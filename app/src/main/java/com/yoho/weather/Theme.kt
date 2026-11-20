package com.yoho.weather

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlin.math.sin

val CardAlpha = 0.17f
val CardBorder = 0.22f
val DimText = 0.68f

val Radius = 19.dp

fun skyColors(group: String, isDay: Boolean): List<Color> {
    val day = mapOf(
        "clear" to listOf(0xFF1A5FA8, 0xFF4F97CF, 0xFFCFE4F3),
        "few" to listOf(0xFF20476B, 0xFF5B8BB2, 0xFFCFDDE8),
        "cloudy" to listOf(0xFF2C4C67, 0xFF68889F, 0xFFC2CDD6),
        "overcast" to listOf(0xFF3F4F5C, 0xFF7A8791, 0xFFB6BEC4),
        "rain" to listOf(0xFF1E2A36, 0xFF445464, 0xFF8B98A5),
        "storm" to listOf(0xFF121824, 0xFF343D4E, 0xFF69717F),
        "snow" to listOf(0xFF3F4F64, 0xFF7B8B9D, 0xFFC8D2DB),
        "fog" to listOf(0xFF505A62, 0xFF828B92, 0xFFBFC5C9)
    )
    val night = mapOf(
        "clear" to listOf(0xFF03060D, 0xFF0A1322, 0xFF1B2739),
        "few" to listOf(0xFF050910, 0xFF0E1624, 0xFF22303F),
        "cloudy" to listOf(0xFF070B12, 0xFF121A26, 0xFF28313D),
        "overcast" to listOf(0xFF080B10, 0xFF151B23, 0xFF2A3038),
        "rain" to listOf(0xFF05080E, 0xFF101722, 0xFF212B38),
        "storm" to listOf(0xFF03060B, 0xFF0C121C, 0xFF1D2531),
        "snow" to listOf(0xFF080C13, 0xFF161E2B, 0xFF2D3746),
        "fog" to listOf(0xFF0A0D11, 0xFF181D23, 0xFF2E3339)
    )
    val pal = (if (isDay) day else night)[group] ?: (if (isDay) day["cloudy"]!! else night["cloudy"]!!)
    return pal.map { Color(it) }
}

/** 全屏天空：纵向渐变 + 缓慢漂移的云团 + 雨雪粒子 */
@Composable
fun SkyBackdrop(group: String, isDay: Boolean, cloud: Int, windKmh: Int) {
    val colors = skyColors(group, isDay)
    val rain = group == "rain" || group == "storm"
    val snow = group == "snow"

    val transition = rememberInfiniteTransition(label = "sky")
    val drift by transition.animateFloat(
        initialValue = 0f, targetValue = 1f,
        animationSpec = infiniteRepeatable(tween(42000, easing = LinearEasing), RepeatMode.Restart),
        label = "drift"
    )
    val fall by transition.animateFloat(
        initialValue = 0f, targetValue = 1f,
        animationSpec = infiniteRepeatable(tween(1100, easing = LinearEasing), RepeatMode.Restart),
        label = "fall"
    )

    Box(
        Modifier
            .fillMaxSize()
            .background(Brush.verticalGradient(listOf(colors[0], colors[1], colors[2])))
    ) {
        Canvas(Modifier.fillMaxSize()) {
            val w = size.width
            val h = size.height

            // 云
            val cloudCount = (cloud / 100f * 5).toInt()
            val tint = if (isDay) Color(0xFFEBF3FA) else Color(0xFF96AAC3)
            for (i in 0 until cloudCount) {
                val baseX = (i * 0.31f + 0.12f) * w
                val x = ((baseX + drift * w * 1.4f * (if (i % 2 == 0) 1 else -1)) % (w * 1.6f)) - w * 0.3f
                val y = h * (0.10f + 0.13f * i)
                val r = w * (0.42f + 0.1f * (i % 3))
                drawCircle(
                    brush = Brush.radialGradient(
                        colors = listOf(tint.copy(alpha = 0.16f), tint.copy(alpha = 0.05f), Color.Transparent),
                        center = Offset(x, y),
                        radius = r
                    ),
                    radius = r,
                    center = Offset(x, y)
                )
            }

            // 雨
            if (rain) {
                val slant = (windKmh / 40f).coerceIn(-0.5f, 0.5f)
                val count = 120
                for (i in 0 until count) {
                    val seedX = ((i * 97) % 1000) / 1000f
                    val seedY = ((i * 37) % 1000) / 1000f
                    val y = ((seedY + fall) % 1f) * (h + 80f) - 40f
                    val x = (seedX * w + y * slant) % w
                    val len = 12f + (i % 5) * 4f
                    drawLine(
                        color = Color(0xFFC6E0FF).copy(alpha = 0.32f),
                        start = Offset(x, y),
                        end = Offset(x + len * slant * 0.6f, y + len),
                        strokeWidth = 1.6f,
                        cap = StrokeCap.Round
                    )
                }
            }

            // 雪
            if (snow) {
                for (i in 0 until 90) {
                    val seedX = ((i * 71) % 1000) / 1000f
                    val seedY = ((i * 43) % 1000) / 1000f
                    val y = ((seedY + fall * 0.45f) % 1f) * (h + 20f) - 10f
                    val x = seedX * w + sin((fall * 6.28f + i).toDouble()).toFloat() * 14f
                    drawCircle(
                        color = Color.White.copy(alpha = 0.65f),
                        radius = 1.2f + (i % 3) * 0.8f,
                        center = Offset(x, y)
                    )
                }
            }
        }
    }
}

/** 毛玻璃卡片 */
@Composable
fun GlassCard(
    modifier: Modifier = Modifier,
    content: @Composable ColumnScope.() -> Unit
) {
    Column(
        modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(Radius))
            .background(Color.White.copy(alpha = CardAlpha))
            .border(0.6.dp, Color.White.copy(alpha = CardBorder), RoundedCornerShape(Radius))
            .padding(horizontal = 14.dp, vertical = 11.dp),
        content = content
    )
}

@Composable
fun CardLabel(text: String, trailing: String? = null) {
    Row(
        Modifier.fillMaxWidth().padding(bottom = 8.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text,
            color = Color.White.copy(alpha = DimText),
            fontSize = 12.sp,
            fontWeight = FontWeight.SemiBold
        )
        if (trailing != null) {
            Text(
                trailing,
                color = Color.White.copy(alpha = 0.9f),
                fontSize = 11.5.sp,
                fontWeight = FontWeight.SemiBold
            )
        }
    }
}

@Composable
fun Hairline() {
    Box(
        Modifier
            .fillMaxWidth()
            .height(0.6.dp)
            .background(Color.White.copy(alpha = 0.15f))
    )
}
