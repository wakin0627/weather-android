package com.yoho.weather

import android.graphics.RuntimeShader
import android.os.Build
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ShaderBrush

/* ----------------
   天空引擎：Android 13+ 走 GPU 着色器（AGSL），低版本回退 Canvas
   - fbm 分形噪声云：会自己流动、堆叠、撕边
   - 太阳/月亮光晕：随距离指数衰减
   - 雨帘、夜间降饱和、随时间变化的整体色彩
   ---------------- */

private const val SKY_AGSL = """
uniform float2 uSize;
uniform float uTime;
uniform float uCloud;
uniform float uRain;
uniform float uNight;
uniform float3 uTop;
uniform float3 uBottom;
uniform float3 uSun;
uniform float2 uSunPos;
uniform float uWarm;

float hash21(float2 p) {
    p = fract(p * float2(123.34, 456.21));
    p += dot(p, p + 45.32);
    return fract(p.x * p.y);
}

float vnoise(float2 p) {
    float2 i = floor(p);
    float2 f = fract(p);
    float2 u = f * f * (3.0 - 2.0 * f);
    float a = hash21(i);
    float b = hash21(i + float2(1.0, 0.0));
    float c = hash21(i + float2(0.0, 1.0));
    float d = hash21(i + float2(1.0, 1.0));
    return mix(mix(a, b, u.x), mix(c, d, u.x), u.y);
}

float fbm(float2 p) {
    float v = 0.0;
    float amp = 0.5;
    for (int i = 0; i < 5; i++) {
        v += amp * vnoise(p);
        p *= 2.03;
        amp *= 0.5;
    }
    return v;
}

half4 main(float2 fragCoord) {
    float2 uv = fragCoord / uSize;
    float aspect = uSize.x / max(uSize.y, 1.0);

    // 垂直渐变天空
    half3 sky = mix(half3(uTop), half3(uBottom), half(pow(uv.y, 0.9)));

    // 柔和日照：宽衰减散射 + 地平线方向光 + 极淡光束，峰值压住不刺眼
    float2 sunPos = uSunPos + float2(sin(uTime * 0.020) * 0.010, cos(uTime * 0.017) * 0.006);
    float2 sunVec = (uv - sunPos) * float2(aspect, 1.0);
    float d = length(sunVec);
    float breath = 0.94 + 0.06 * sin(uTime * 0.030);
    float broad = 0.17 * exp(-d * 1.9) + 0.07 * exp(-d * 0.9);
    float sideGlow = pow(max(0.0, 1.0 - abs(uv.x - sunPos.x) * 1.15), 2.0) * (1.0 - uv.y) * 0.045;
    float glow = min((broad + sideGlow) * breath, 0.34);
    sky += half3(uSun) * half(glow * (1.0 - uNight * 0.85));

    // 光束：用缓慢旋转的方向向量 + 点积，避免依赖 atan
    float2 rayDir = float2(cos(uTime * 0.045), sin(uTime * 0.032));
    float aligned = max(0.0, dot(normalize(sunVec + float2(1e-4)), rayDir));
    float rays = pow(aligned, 5.0) * exp(-d * 1.6) * 0.07;
    sky += half3(uSun) * half(rays * (1.0 - uNight));

    // 薄雾光带：晴天也保持缓慢流动
    float haze = fbm(float2(uv.x * 1.35 + uTime * 0.016, uv.y * 3.4 - uTime * 0.005));
    sky += half3(1.0, 0.98, 0.95) * half(0.055 * smoothstep(0.32, 0.92, haze));

    // 云层：两层噪声叠加出体积感
    float2 p = float2(uv.x * aspect, uv.y) * 2.3;
    float2 drift = float2(uTime * 0.021, -uTime * 0.0045);
    float base = fbm(p + drift + fbm(p * 0.55 + uTime * 0.008) * 0.9);
    float lo = 0.52 - uCloud * 0.26;
    float c = smoothstep(lo, 0.98, base) * clamp(uCloud, 0.0, 1.0);

    half3 cloudColor = mix(half3(1.0), half3(0.62, 0.68, 0.78), half(uNight));
    cloudColor = mix(cloudColor, half3(0.99, 0.96, 0.90), half(glow * 1.2));
    sky = mix(sky, cloudColor, half(clamp(c * 0.9, 0.0, 0.92)));

    // 星空 + 月亮 + 城市灯光（夜间）
    if (uNight > 0.01) {
        float2 sp = float2(uv.x * aspect, uv.y) * 78.0;
        float2 cell = floor(sp);
        float2 jitter = float2(hash21(cell + 3.1), hash21(cell + 7.7)) - 0.5;
        float2 fr = fract(sp) - 0.5 - jitter * 0.7;
        float h = hash21(cell);
        float rstar = 0.040 + 0.045 * fract(h * 17.0);
        float twinkle = 0.5 + 0.5 * sin(uTime * 2.2 + h * 6.2831);
        float star = smoothstep(rstar, 0.0, length(fr)) * step(0.78, h)
                     * twinkle * uNight * smoothstep(0.90, 0.10, uv.y);
        sky += half3(star * 1.15);

        float md = length((uv - sunPos) * float2(aspect, 1.0));
        float moonDisc = smoothstep(0.030, 0.020, md);
        sky = mix(sky, half3(0.90, 0.92, 0.86), half(moonDisc * 0.92 * uNight));
        sky += half3(0.32, 0.40, 0.62) * half(exp(-md * 3.2) * 0.15 * uNight);

        float cityGlow = exp(-pow((uv.y - 0.97) * 9.0, 2.0)) * uNight;
        sky += half3(1.0, 0.70, 0.40) * half(cityGlow * 0.14);
    }

    // 雨帘
    if (uRain > 0.02) {
        float2 rp = float2(uv.x * 90.0, uv.y * 9.0 - uTime * 3.4);
        float streak = step(0.982, hash21(floor(rp)));
        sky = mix(sky, half3(0.72, 0.80, 0.90), half(streak * uRain * 0.55));
        sky *= half(1.0 - 0.12 * uRain);
    }

    // 早晚霞光：贴近日出/日落时，地平线附近染橘粉
    float horizon = pow(uv.y, 0.7);
    half3 warmColor = half3(1.0, 0.46, 0.26);
    sky = mix(sky, warmColor, half(uWarm * horizon * 0.62 * (1.0 - uNight * 0.80)));
    sky += half3(1.0, 0.58, 0.32) * half(uWarm * glow * 0.55);

    // 暗角，让底部文字更稳
    float vig = smoothstep(0.15, 1.15, uv.y);
    sky *= half(1.0 - 0.22 * vig);

    return half4(sky, 1.0);
}
"""

@Composable
fun SkyEngineBackdrop(
    group: String,
    isDay: Boolean,
    cloud: Int,
    windKmh: Int,
    warm: Float = 0f
) {
    if (Build.VERSION.SDK_INT < 33) {
        // 低版本走原来的 Canvas 引擎
        SkyBackdrop(group, isDay, cloud, windKmh)
        return
    }

    val colors = skyColors(group, isDay)
    val shaderOrNull = remember {
        runCatching { RuntimeShader(SKY_AGSL) }
            .onFailure { android.util.Log.w("SkyEngine", "AGSL 编译失败: ${it.message}") }
            .getOrNull()
    }
    if (shaderOrNull == null) {
        SkyBackdrop(group, isDay, cloud, windKmh)
        return
    }
    val shader = shaderOrNull

    val night = if (isDay) 0f else 1f
    val top = colors.first()
    val bottom = colors.last()
    val sunColor = when {
        !isDay -> Color(0xFFB9CDF0)
        group == "clear" -> Color(0xFFFFE6A8)
        group == "rain" || group == "storm" -> Color(0xFFC9D8E6)
        else -> Color(0xFFF3E3C0)
    }
    // 晴天也给一点点薄云，天空才有流动感
    val cloudF = maxOf((cloud / 100f).coerceIn(0f, 1f), if (group == "clear") 0.22f else 0.12f)
    val rainF = when (group) {
        "rain" -> 0.85f
        "storm" -> 1f
        "snow" -> 0.35f
        "fog" -> 0.15f
        else -> 0f
    }

    val anim = rememberInfiniteTransition(label = "sky")
    val time by anim.animateFloat(
        initialValue = 0f,
        targetValue = 1000f,
        animationSpec = infiniteRepeatable(tween(900_000, easing = LinearEasing)),
        label = "t"
    )

    Canvas(Modifier.fillMaxSize()) {
        try {
            shader.setFloatUniform("uSize", size.width, size.height)
            shader.setFloatUniform("uTime", time)
            shader.setFloatUniform("uCloud", cloudF)
            shader.setFloatUniform("uRain", rainF)
            shader.setFloatUniform("uNight", night)
            shader.setFloatUniform("uWarm", warm)
            shader.setFloatUniform("uTop", top.red, top.green, top.blue)
            shader.setFloatUniform("uBottom", bottom.red, bottom.green, bottom.blue)
            shader.setFloatUniform("uSun", sunColor.red, sunColor.green, sunColor.blue)
            shader.setFloatUniform(
                "uSunPos",
                if (isDay) 0.74f else 0.78f,
                if (isDay) 0.20f else 0.16f
            )
            drawRect(ShaderBrush(shader))
        } catch (e: Throwable) {
            android.util.Log.w("SkyEngine", "AGSL 渲染失败，回退渐变: ${e.message}")
            drawRect(Brush.verticalGradient(skyColors(group, isDay)))
        }
    }
}
