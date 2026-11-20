package com.yoho.weather

import androidx.compose.animation.core.CubicBezierEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.material3.Text
import dev.chrisbanes.haze.HazeState
import dev.chrisbanes.haze.HazeStyle
import dev.chrisbanes.haze.HazeTint
import dev.chrisbanes.haze.hazeEffect
import androidx.compose.foundation.layout.BoxWithConstraints

/**
 * iOS 26/27 风格的液态玻璃底栏：
 * 双层渐变 + 边缘高光 + 缓慢流动的镜面扫光 + 弹簧跟随的选中胶囊。
 */
@Composable
fun LiquidGlassBar(tab: Int, hazeState: HazeState, onTab: (Int) -> Unit) {
    val items = listOf("天气", "地图", "我的")
    val shape = RoundedCornerShape(999.dp)
    val easing = CubicBezierEasing(0.22f, 1f, 0.36f, 1f)

    // 镜面扫光
    val sheen = rememberInfiniteTransition(label = "sheen")
    val phase by sheen.animateFloat(
        initialValue = -0.6f,
        targetValue = 1.6f,
        animationSpec = infiniteRepeatable(
            animation = tween(5200, easing = CubicBezierEasing(0.4f, 0f, 0.2f, 1f)),
            repeatMode = RepeatMode.Restart
        ),
        label = "phase"
    )

    Box(
        Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp)
            .navigationBarsPadding()
            .padding(bottom = 10.dp)
    ) {
        BoxWithConstraints(
            Modifier
                .fillMaxWidth()
                .height(64.dp)
                .shadow(
                    elevation = 20.dp,
                    shape = shape,
                    ambientColor = Color(0x66000000),
                    spotColor = Color(0x99000000)
                )
                .clip(shape)
                // 真实背景模糊（Haze 采样下层内容）
                .hazeEffect(
                    hazeState,
                    style = HazeStyle(
                        backgroundColor = Color(0xFF0B1119).copy(alpha = 0.34f),
                        tints = listOf(HazeTint(Color(0x1FFFFFFF))),
                        blurRadius = 26.dp,
                        noiseFactor = 0.015f
                    )
                )
                // 玻璃面高光
                .background(
                    Brush.verticalGradient(
                        listOf(
                            Color(0x33FFFFFF),
                            Color(0x12FFFFFF),
                            Color(0x0AFFFFFF)
                        )
                    )
                )
                // 冷色反光，避免一片死白
                .background(
                    Brush.linearGradient(
                        listOf(Color(0x1A8FD8FF), Color(0x00000000), Color(0x14A98CFF)),
                        start = Offset.Zero,
                        end = Offset(Float.POSITIVE_INFINITY, Float.POSITIVE_INFINITY)
                    )
                )
                .border(
                    width = 1.dp,
                    brush = Brush.verticalGradient(
                        listOf(Color(0x66FFFFFF), Color(0x1AFFFFFF), Color(0x33FFFFFF))
                    ),
                    shape = shape
                )
        ) {
            val itemW = maxWidth / items.size

            // 扫光
            Canvas(Modifier.fillMaxSize()) {
                val w = size.width
                val x = phase * w
                val band = w * 0.42f
                drawRect(
                    brush = Brush.linearGradient(
                        colors = listOf(Color(0x00FFFFFF), Color(0x12FFFFFF), Color(0x00FFFFFF)),
                        start = Offset(x, 0f),
                        end = Offset(x + band, size.height)
                    ),
                    size = size
                )
                // 顶部内高光
                drawLine(
                    color = Color(0x59FFFFFF),
                    start = Offset(18f, 1.5f),
                    end = Offset(w - 18f, 1.5f),
                    strokeWidth = 1.4f
                )
            }

            // 选中胶囊
            val selX by animateDpAsState(
                targetValue = itemW * tab,
                animationSpec = tween(420, easing = easing),
                label = "sel"
            )
            Box(
                Modifier
                    .offset(x = selX)
                    .width(itemW)
                    .fillMaxHeight()
                    .padding(horizontal = 6.dp, vertical = 7.dp)
                    .clip(RoundedCornerShape(999.dp))
                    .background(
                        Brush.verticalGradient(
                            listOf(Color(0x3DFFFFFF), Color(0x1FFFFFFF))
                        )
                    )
                    .border(
                        0.9.dp,
                        Brush.verticalGradient(
                            listOf(Color(0x8CFFFFFF), Color(0x1FFFFFFF))
                        ),
                        RoundedCornerShape(999.dp)
                    )
            )

            Row(Modifier.fillMaxSize()) {
                items.forEachIndexed { i, label ->
                    val on = tab == i
                    Column(
                        Modifier
                            .weight(1f)
                            .fillMaxHeight()
                            .clickable { onTab(i) },
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        TabGlyph(
                            kind = i,
                            tint = if (on) Color.White else Color.White.copy(alpha = 0.55f)
                        )
                        Spacer(Modifier.height(3.dp))
                        Text(
                            label,
                            color = if (on) Color.White else Color.White.copy(alpha = 0.55f),
                            fontSize = 10.5.sp,
                            fontWeight = if (on) FontWeight.SemiBold else FontWeight.Normal,
                            letterSpacing = 0.3.sp
                        )
                    }
                }
            }
        }
    }
}

/** 手绘小图标，避免为三个 tab 引入整套 material-icons */
@Composable
private fun TabGlyph(kind: Int, tint: Color) {
    Canvas(Modifier.size(19.dp)) {
        val c = Offset(size.width / 2, size.height / 2)
        val r = size.minDimension / 2
        when (kind) {
            // 天气：太阳
            0 -> {
                drawCircle(tint, radius = r * 0.36f, center = c)
                repeat(8) { i ->
                    val a = Math.toRadians(i * 45.0)
                    val sx = c.x + (r * 0.62f) * kotlin.math.cos(a).toFloat()
                    val sy = c.y + (r * 0.62f) * kotlin.math.sin(a).toFloat()
                    val ex = c.x + (r * 0.92f) * kotlin.math.cos(a).toFloat()
                    val ey = c.y + (r * 0.92f) * kotlin.math.sin(a).toFloat()
                    drawLine(tint, Offset(sx, sy), Offset(ex, ey), strokeWidth = size.minDimension * 0.075f, cap = androidx.compose.ui.graphics.StrokeCap.Round)
                }
            }
            // 地图：定位针
            1 -> {
                val path = Path().apply {
                    moveTo(c.x, size.height * 0.96f)
                    cubicTo(
                        size.width * 0.18f, size.height * 0.56f,
                        size.width * 0.16f, size.height * 0.10f,
                        c.x, size.height * 0.10f
                    )
                    cubicTo(
                        size.width * 0.84f, size.height * 0.10f,
                        size.width * 0.82f, size.height * 0.56f,
                        c.x, size.height * 0.96f
                    )
                    close()
                }
                drawPath(path, tint, style = Stroke(width = size.minDimension * 0.10f))
                drawCircle(tint, radius = r * 0.17f, center = Offset(c.x, size.height * 0.36f))
            }
            // 我的：人像
            else -> {
                drawCircle(tint, radius = r * 0.30f, center = Offset(c.x, size.height * 0.32f))
                val p = Path().apply {
                    moveTo(size.width * 0.14f, size.height * 1.02f)
                    cubicTo(
                        size.width * 0.16f, size.height * 0.62f,
                        size.width * 0.84f, size.height * 0.62f,
                        size.width * 0.86f, size.height * 1.02f
                    )
                }
                drawPath(p, tint, style = Stroke(width = size.minDimension * 0.10f))
            }
        }
    }
}
