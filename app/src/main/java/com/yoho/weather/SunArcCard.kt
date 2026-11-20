package com.yoho.weather

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.*
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import java.time.LocalTime

/* 日出日落：小米天气那种「弧形轨迹 + 太阳沿弧走」 */

private fun minutesOf(hhmm: String): Int? = try {
    if (hhmm.length < 5) null else {
        val h = hhmm.substring(0, 2).toInt()
        val m = hhmm.substring(3, 5).toInt()
        h * 60 + m
    }
} catch (e: Exception) {
    null
}

@Composable
fun SunArcCard(now: Now?) {
    val rise = remember(now?.sunrise) { minutesOf(now?.sunrise ?: "") }
    val set = remember(now?.sunset) { minutesOf(now?.sunset ?: "") }
    val minuteNow = remember(now?.time) {
        runCatching { LocalTime.now().hour * 60 + LocalTime.now().minute }.getOrDefault(720)
    }

    val progress = when {
        rise == null || set == null || set <= rise -> 0.5f
        else -> ((minuteNow - rise).toFloat() / (set - rise)).coerceIn(-0.2f, 1.2f)
    }
    val isUp = progress in 0f..1f
    val dayLen = if (rise != null && set != null && set > rise) {
        val total = set - rise
        "${total / 60} 小时 ${total % 60} 分"
    } else "--"

    val anim = rememberInfiniteTransition(label = "sun")
    val pulse by anim.animateFloat(
        initialValue = 0.85f,
        targetValue = 1.15f,
        animationSpec = infiniteRepeatable(tween(2600, easing = LinearEasing)),
        label = "pulse"
    )

    GlassCard {
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Text("日出日落", color = Color.White.copy(alpha = DimText), fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
            Text("白昼 $dayLen", color = Color.White.copy(alpha = 0.5f), fontSize = 11.sp)
        }
        Spacer(Modifier.height(6.dp))

        Box(Modifier.fillMaxWidth().height(118.dp)) {
            Canvas(Modifier.fillMaxSize()) {
                val w = size.width
                val h = size.height
                val left = Offset(w * 0.10f, h * 0.90f)
                val right = Offset(w * 0.90f, h * 0.90f)
                val control = Offset(w * 0.50f, -h * 0.32f)

                fun point(t: Float): Offset {
                    val mt = 1 - t
                    val x = mt * mt * left.x + 2 * mt * t * control.x + t * t * right.x
                    val y = mt * mt * left.y + 2 * mt * t * control.y + t * t * right.y
                    return Offset(x, y)
                }

                // 地平线
                drawLine(
                    color = Color.White.copy(alpha = 0.16f),
                    start = Offset(0f, left.y),
                    end = Offset(w, right.y),
                    strokeWidth = 1.2f
                )

                // 整条轨迹（虚线）
                val arc = Path().apply {
                    moveTo(left.x, left.y)
                    quadraticBezierTo(control.x, control.y, right.x, right.y)
                }
                drawPath(
                    path = arc,
                    color = Color.White.copy(alpha = 0.28f),
                    style = Stroke(
                        width = 2f,
                        cap = StrokeCap.Round,
                        pathEffect = PathEffect.dashPathEffect(floatArrayOf(6f, 8f))
                    )
                )

                // 已走过的轨迹（渐变实线）
                val done = progress.coerceIn(0f, 1f)
                if (done > 0.01f) {
                    val trail = Path().apply {
                        moveTo(left.x, left.y)
                        var t = 0f
                        while (t <= done) {
                            val p = point(t)
                            lineTo(p.x, p.y)
                            t += 0.02f
                        }
                        val p = point(done)
                        lineTo(p.x, p.y)
                    }
                    drawPath(
                        path = trail,
                        brush = Brush.horizontalGradient(
                            listOf(Color(0xFFFFD479), Color(0xFFFF9F43))
                        ),
                        style = Stroke(width = 2.6f, cap = StrokeCap.Round)
                    )
                }

                // 太阳
                val pos = when {
                    progress < 0f -> left
                    progress > 1f -> right
                    else -> point(progress)
                }
                val sunColor = if (isUp) Color(0xFFFFD479) else Color(0xFFCBD8E8)
                if (isUp) {
                    drawCircle(
                        brush = Brush.radialGradient(
                            listOf(sunColor.copy(alpha = 0.55f * pulse), Color.Transparent),
                            center = pos,
                            radius = 46f * pulse
                        ),
                        radius = 46f * pulse,
                        center = pos
                    )
                }
                drawCircle(sunColor, radius = 9f, center = pos)
                drawCircle(Color.White.copy(alpha = 0.9f), radius = 4f, center = pos)
            }
        }

        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Column {
                Text("日出", color = Color.White.copy(alpha = 0.45f), fontSize = 10.5.sp)
                Text(now?.sunrise?.ifEmpty { "--:--" } ?: "--:--", color = Color.White, fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
            }
            Column(horizontalAlignment = androidx.compose.ui.Alignment.End) {
                Text("日落", color = Color.White.copy(alpha = 0.45f), fontSize = 10.5.sp)
                Text(now?.sunset?.ifEmpty { "--:--" } ?: "--:--", color = Color.White, fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
            }
        }
    }
}
