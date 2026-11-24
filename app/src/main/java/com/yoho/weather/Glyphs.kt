package com.yoho.weather

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/**
 * 单色线性图标：全站只用这一套，避免 emoji / 彩色插画 / 手绘混搭。
 * 统一 1.7dp 描边、圆头，24dp 视觉尺寸。
 */
@Composable
fun Glyph(kind: String, tint: Color, size: Dp = 20.dp) {
    Canvas(Modifier.size(size)) {
        val w = this.size.width
        val h = this.size.height
        val sw = w * 0.075f
        val stroke = Stroke(width = sw, cap = StrokeCap.Round)
        fun line(x1: Float, y1: Float, x2: Float, y2: Float) =
            drawLine(tint, Offset(x1 * w, y1 * h), Offset(x2 * w, y2 * h), sw, StrokeCap.Round)

        when (kind) {
            "jacket" -> {
                // 衣领 + 两肩 + 身体
                val p = Path().apply {
                    moveTo(0.30f * w, 0.20f * h)
                    lineTo(0.20f * w, 0.34f * h)
                    lineTo(0.20f * w, 0.82f * h)
                    lineTo(0.80f * w, 0.82f * h)
                    lineTo(0.80f * w, 0.34f * h)
                    lineTo(0.70f * w, 0.20f * h)
                }
                drawPath(p, tint, style = stroke)
                line(0.50f, 0.22f, 0.50f, 0.82f)
            }
            "umbrella" -> {
                val p = Path().apply {
                    moveTo(0.12f * w, 0.52f * h)
                    quadraticBezierTo(0.50f * w, 0.10f * h, 0.88f * w, 0.52f * h)
                }
                drawPath(p, tint, style = stroke)
                line(0.50f, 0.52f, 0.50f, 0.84f)
                val hook = Path().apply {
                    moveTo(0.50f * w, 0.84f * h)
                    quadraticBezierTo(0.62f * w, 0.92f * h, 0.66f * w, 0.80f * h)
                }
                drawPath(hook, tint, style = stroke)
            }
            "car" -> {
                val body = Path().apply {
                    moveTo(0.14f * w, 0.62f * h)
                    lineTo(0.22f * w, 0.40f * h)
                    lineTo(0.78f * w, 0.40f * h)
                    lineTo(0.86f * w, 0.62f * h)
                    lineTo(0.86f * w, 0.72f * h)
                    lineTo(0.14f * w, 0.72f * h)
                    close()
                }
                drawPath(body, tint, style = stroke)
                drawCircle(tint, w * 0.055f, Offset(0.30f * w, 0.76f * h), style = stroke)
                drawCircle(tint, w * 0.055f, Offset(0.70f * w, 0.76f * h), style = stroke)
            }
            "dry" -> {
                // 晾衣绳 + 衣物
                line(0.10f, 0.30f, 0.90f, 0.30f)
                val cloth = Path().apply {
                    moveTo(0.30f * w, 0.30f * h)
                    lineTo(0.30f * w, 0.72f * h)
                    lineTo(0.52f * w, 0.72f * h)
                    lineTo(0.52f * w, 0.30f * h)
                }
                drawPath(cloth, tint, style = stroke)
                line(0.66f, 0.30f, 0.66f, 0.56f)
            }
            "run" -> {
                drawCircle(tint, w * 0.085f, Offset(0.56f * w, 0.20f * h), style = stroke)
                line(0.56f, 0.30f, 0.46f, 0.56f)
                line(0.46f, 0.56f, 0.60f, 0.70f)
                line(0.60f, 0.70f, 0.54f, 0.88f)
                line(0.46f, 0.56f, 0.30f, 0.66f)
                line(0.60f, 0.70f, 0.78f, 0.62f)
            }
            "hike" -> {
                // 山 + 旗
                val m = Path().apply {
                    moveTo(0.10f * w, 0.82f * h)
                    lineTo(0.36f * w, 0.40f * h)
                    lineTo(0.54f * w, 0.66f * h)
                    lineTo(0.66f * w, 0.50f * h)
                    lineTo(0.90f * w, 0.82f * h)
                }
                drawPath(m, tint, style = stroke)
                line(0.76f, 0.50f, 0.76f, 0.24f)
                line(0.76f, 0.24f, 0.90f, 0.30f)
            }
            "uv" -> {
                drawCircle(tint, w * 0.17f, Offset(0.5f * w, 0.5f * h), style = stroke)
                repeat(8) { i ->
                    val a = Math.toRadians(i * 45.0)
                    line(
                        (0.5f + 0.28f * kotlin.math.cos(a).toFloat()),
                        (0.5f + 0.28f * kotlin.math.sin(a).toFloat()),
                        (0.5f + 0.42f * kotlin.math.cos(a).toFloat()),
                        (0.5f + 0.42f * kotlin.math.sin(a).toFloat())
                    )
                }
            }
            "hum" -> {
                val drop = Path().apply {
                    moveTo(0.50f * w, 0.14f * h)
                    quadraticBezierTo(0.24f * w, 0.52f * h, 0.50f * w, 0.84f * h)
                    quadraticBezierTo(0.76f * w, 0.52f * h, 0.50f * w, 0.14f * h)
                    close()
                }
                drawPath(drop, tint, style = stroke)
            }
            "wind" -> {
                line(0.12f, 0.36f, 0.70f, 0.36f)
                line(0.12f, 0.52f, 0.84f, 0.52f)
                line(0.20f, 0.68f, 0.62f, 0.68f)
            }
            "press" -> {
                drawCircle(tint, w * 0.32f, Offset(0.5f * w, 0.5f * h), style = stroke)
                line(0.5f, 0.5f, 0.62f, 0.38f)
            }
            "vis" -> {
                val eye = Path().apply {
                    moveTo(0.10f * w, 0.50f * h)
                    quadraticBezierTo(0.50f * w, 0.18f * h, 0.90f * w, 0.50f * h)
                    quadraticBezierTo(0.50f * w, 0.82f * h, 0.10f * w, 0.50f * h)
                }
                drawPath(eye, tint, style = stroke)
                drawCircle(tint, w * 0.11f, Offset(0.5f * w, 0.5f * h), style = stroke)
            }
            "sunset", "sunrise" -> {
                // 地平线 + 半个太阳（日落朝下，日出朝上）
                val cy = if (kind == "sunset") 0.56f else 0.44f
                drawCircle(tint, w * 0.17f, Offset(0.5f * w, cy * h), style = stroke)
                line(0.10f, 0.60f, 0.90f, 0.60f)
                if (kind == "sunset") {
                    line(0.50f, 0.74f, 0.50f, 0.86f)
                    line(0.36f, 0.80f, 0.50f, 0.88f)
                    line(0.64f, 0.80f, 0.50f, 0.88f)
                } else {
                    line(0.50f, 0.30f, 0.50f, 0.18f)
                    line(0.36f, 0.24f, 0.50f, 0.16f)
                    line(0.64f, 0.24f, 0.50f, 0.16f)
                }
            }
            "air" -> {
                line(0.14f, 0.34f, 0.66f, 0.34f)
                line(0.14f, 0.50f, 0.82f, 0.50f)
                line(0.14f, 0.66f, 0.58f, 0.66f)
                line(0.70f, 0.34f, 0.80f, 0.42f)
            }
            else -> {
                drawCircle(tint, w * 0.34f, Offset(0.5f * w, 0.5f * h), style = stroke)
            }
        }
        // 统一视觉重心微调
        drawRect(Color.Transparent, size = Size(w, h))
    }
}

/** 服务端 advice icon -> Glyph 类型 */
fun glyphOf(icon: String, title: String): String = when {
    icon.contains("jacket") || title.contains("穿") -> "jacket"
    icon.contains("umbrella") || title.contains("伞") -> "umbrella"
    icon.contains("car") || title.contains("洗车") -> "car"
    icon.contains("dry") || title.contains("晾") -> "dry"
    icon.contains("hike") || title.contains("爬山") -> "hike"
    icon.contains("run") || title.contains("运动") -> "run"
    else -> "air"
}
