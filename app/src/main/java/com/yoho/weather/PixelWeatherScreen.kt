package com.yoho.weather

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/* ---------------- 像素天空 ---------------- */

@Composable
fun PixelSky(group: String, isDay: Boolean, cloud: Int, warm: Float) {
    val colors = pxSkyColors(group, isDay)
    val anim = rememberInfiniteTransition(label = "pxsky")
    val t by anim.animateFloat(
        initialValue = 0f,
        targetValue = 512f,
        animationSpec = infiniteRepeatable(tween(240_000, easing = LinearEasing)),
        label = "t"
    )

    Canvas(Modifier.fillMaxSize()) {
        val w = size.width
        val h = size.height
        val cell = (w / 90f).coerceAtLeast(3f)   // 像素块尺寸
        val bands = 64
        val top = colors.first()
        val mid = colors[1]
        val bottom = colors.last()

        fun lerp(a: Color, b: Color, f: Float) = Color(
            a.red + (b.red - a.red) * f,
            a.green + (b.green - a.green) * f,
            a.blue + (b.blue - a.blue) * f
        )

        // 分带渐变（硬边 + 隔行抖动）
        for (i in 0 until bands) {
            val f = i.toFloat() / (bands - 1)
            val c = if (f < 0.65f) lerp(top, mid, f / 0.65f) else lerp(mid, bottom, (f - 0.65f) / 0.35f)
            val y = h * f
            val bandH = h / (bands - 1f) + 2f   // 与步长同分母，并重叠 2px 消除缝隙
            drawRect(c, Offset(0f, y), Size(w, bandH))
            // 4x4 有序抖动：把下一档颜色按棋盘比例掺一点，消除色带台阶
            val nextC = if (f < 0.65f) lerp(top, mid, (f + 1f / (bands - 1f)) / 0.65f)
                        else lerp(mid, bottom, (f + 1f / (bands - 1f) - 0.65f) / 0.35f)
            val mixC = lerp(c, nextC, 0.5f)
            val bayer = intArrayOf(0, 8, 2, 10, 12, 4, 14, 6, 3, 11, 1, 9, 15, 7, 13, 5)
            val rows = ((h / (bands - 1f)) / cell).toInt().coerceAtLeast(1)
            for (r in 0 until rows) {
                for (cc in 0 until ((w / cell).toInt())) {
                    val idx = (r % 4) * 4 + (cc % 4)
                    if (bayer[idx] >= 8) {
                        drawRect(mixC, Offset(cc * cell, y + r * cell), Size(cell, cell))
                    }
                }
            }
        }

        // 暖光：靠近日落时底部泛橘
        if (warm > 0.02f) {
            val warmColor = Color(0xFFD8A85A)
            var y = h * 0.55f
            var a = 0f
            while (y < h) {
                a += 0.035f
                drawRect(warmColor.copy(alpha = a * warm * 0.22f), Offset(0f, y), Size(w, h / (bands - 1f) + 2f))
                y += h / (bands - 1f)
            }
        }

        // 太阳 / 月亮：像素方块 + 十字光芒
        val sx = w * 0.72f
        val sy = if (isDay) h * 0.16f else h * 0.13f
        val sunColor = if (isDay) Color(0xFFD8A85A) else Color(0xFFDCE6F2)
        fun block(x: Float, y: Float, n: Int = 1, c: Color = sunColor, alpha: Float = 1f) {
            drawRect(c.copy(alpha = alpha), Offset(x, y), Size(cell * n, cell * n))
        }
        if (isDay) {
            block(sx, sy, 5)
            block(sx + cell, sy - cell, 3, Color(0xFFE0BC7E))
            block(sx - cell, sy + cell, 2, Color(0xFFE0BC7E), 0.9f)
            block(sx + cell * 5, sy + cell, 2, Color(0xFFE0BC7E), 0.85f)
            for (i in 1..3) {
                block(sx + cell * 2, sy - cell * (i + 1) - cell, 1, Color(0xFFE0BC7E), 0.55f - i * 0.12f)
                block(sx + cell * 2, sy + cell * 6 + (i - 1) * cell, 1, Color(0xFFE0BC7E), 0.5f - i * 0.12f)
                block(sx + cell * 6 + (i - 1) * cell, sy + cell * 2, 1, Color(0xFFE0BC7E), 0.5f - i * 0.12f)
            }
        } else {
            block(sx, sy, 5, Color(0xFFDCE6F2))
            block(sx + cell * 3, sy + cell, 2, Color(0xFF0A121A))
            block(sx + cell, sy + cell * 3, 2, Color(0xFF0A121A))
            // 星星
            val stars = 26
            for (i in 0 until stars) {
                val bx = ((i * 37) % 60) * (w / 60f)
                val by = ((i * 53) % 45) * (h / 100f)
                val tw = 0.45f + 0.55f * kotlin.math.abs(kotlin.math.sin((t + i * 9f) * 0.05f))
                block(bx, by, 1, Color.White, tw * 0.9f)
            }
        }

        // 云：几块像素团，按块步进漂移
        val cloudN = (cloud / 100f * 4).toInt().coerceIn(if (group == "clear") 1 else 0, 4)
        for (i in 0 until cloudN) {
            val speed = 6f + i * 3f
            val cx = ((t * speed / 8f + i * 31f) % (w + cell * 30)) - cell * 30
            val cy = h * (0.10f + 0.11f * i)
            val white = if (isDay) Color(0xFFF2F8FF) else Color(0xFF7C8FA6)
            for (k in 0 until 3) {
                drawRect(white.copy(alpha = 0.9f), Offset(cx + cell * k * 3, cy), Size(cell * 7, cell * 2))
                drawRect(white.copy(alpha = 0.75f), Offset(cx + cell * (k * 3 + 1), cy - cell), Size(cell * 5, cell))
            }
            drawRect(white.copy(alpha = 0.55f), Offset(cx + cell, cy + cell * 2), Size(cell * 8, cell))
        }

        // 雨 / 雪
        if (group == "rain" || group == "storm" || group == "snow") {
            val drops = if (group == "snow") 26 else 40
            for (i in 0 until drops) {
                val dx = ((i * 71) % 90) * (w / 90f)
                val phase = (t * (group.let { if (it == "snow") 12f else 30f }) + i * 17f) % (h + 40f)
                val dy = phase
                if (group == "snow") {
                    drawRect(Color.White.copy(alpha = 0.8f), Offset(dx, dy), Size(cell, cell))
                } else {
                    drawRect(Color(0xFF9FD8FF).copy(alpha = 0.55f), Offset(dx, dy), Size(cell * 0.6f, cell * 3f))
                }
            }
        }
    }
}

/* ---------------- 像素天气页 ---------------- */

@Composable
fun PixelWeatherScreen(
    data: WeatherBundle?,
    loading: Boolean,
    error: String?,
    cityName: String,
    onRefresh: () -> Unit,
    onOpenCity: () -> Unit,
    onOpenAdvice: () -> Unit,
    onOpenDays: () -> Unit
) {
    val now = data?.now
    val group = now?.group ?: "cloudy"
    val isDay = now?.isDay ?: true
    val warm = remember(now?.sunrise, now?.sunset) {
        val rise = runCatching { now?.sunrise?.let { it.substring(0, 2).toInt() * 60 + it.substring(3, 5).toInt() } }.getOrNull()
        val set = runCatching { now?.sunset?.let { it.substring(0, 2).toInt() * 60 + it.substring(3, 5).toInt() } }.getOrNull()
        if (rise == null || set == null) 0f else {
            val m = java.time.LocalTime.now().let { it.hour * 60 + it.minute }
            (1f - minOf(Math.abs(m - rise), Math.abs(m - set)) / 70f).coerceIn(0f, 1f)
        }
    }

    Box(Modifier.fillMaxSize().background(Color(0xFF08111A))) {
        PixelSky(group, isDay, now?.cloud ?: 30, warm)

        LazyColumn(
            Modifier.fillMaxSize(),
            contentPadding = PaddingValues(start = 12.dp, end = 12.dp, top = 40.dp, bottom = 108.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            item {
                Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                    PixelPill(cityName.ifBlank { "城市" }) { onOpenCity() }
                    Spacer(Modifier.weight(1f))
                    Text(
                        "更新 ${now?.time?.takeLast(5) ?: "--:--"}",
                        color = PxTextDim, fontSize = 10.sp, fontFamily = PixelFont
                    )
                    Spacer(Modifier.width(8.dp))
                    PixelPill(if (loading) "…" else "刷新", filled = loading) { onRefresh() }
                }
            }

            // 主信息
            item {
                Column(
                    Modifier.fillMaxWidth().padding(top = 4.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    PixelGlyph(pixelIconOf(group, isDay), PxText, 40.dp)
                    Spacer(Modifier.height(6.dp))
                    Text(
                        data?.place?.city?.ifEmpty { cityName } ?: cityName,
                        color = PxText, fontSize = 18.sp, fontFamily = PixelFont
                    )
                    Text(
                        buildString {
                            data?.place?.let { p ->
                                if (p.district.isNotEmpty()) append(p.district)
                                if (p.township.isNotEmpty()) { if (isNotEmpty()) append(" · "); append(p.township) }
                            }
                        }.ifEmpty { error ?: "读取中" },
                        color = PxTextDim, fontSize = 10.sp, fontFamily = PixelFont,
                        modifier = Modifier.padding(top = 2.dp)
                    )
                    Spacer(Modifier.height(2.dp))
                    Text(
                        if (now != null) "${now.temp}°" else "--°",
                        color = PxText, fontSize = 56.sp, fontFamily = PixelFont, lineHeight = 60.sp
                    )
                    Spacer(Modifier.height(2.dp))
                    Text(now?.text ?: "…", color = PxText, fontSize = 13.sp, fontFamily = PixelFont)
                    if (data != null) {
                        Text(
                            "最高 ${data.days.first().hi}°  最低 ${data.days.first().lo}°",
                            color = PxTextDim, fontSize = 11.sp, fontFamily = PixelFont,
                            modifier = Modifier.padding(top = 3.dp)
                        )
                    }
                    data?.air?.let { air ->
                        Spacer(Modifier.height(5.dp))
                        Row(
                            Modifier.border(1.dp, PxBorderDim).padding(horizontal = 6.dp, vertical = 2.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            val tone = when {
                                air.aqi <= 50 -> Color(0xFF93B49C)
                                air.aqi <= 100 -> Color(0xFFC9B27A)
                                air.aqi <= 150 -> Color(0xFFBE9070)
                                else -> Color(0xFFB87A76)
                            }
                            Box(Modifier.size(6.dp).background(tone))
                            Spacer(Modifier.width(5.dp))
                            Text("空气 ${air.level} ${air.aqi}", color = PxText, fontSize = 10.sp, fontFamily = PixelFont)
                        }
                    }
                }
            }

            // 降水提示
            if (data != null) {
                item { Spacer(Modifier.height(6.dp)) }
                item {
                    val next3 = data.hours.take(3)
                    val soon = next3.any { it.pop >= 40 }
                    Row(
                        Modifier.fillMaxWidth().padding(horizontal = 4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        PixelGlyph(if (soon) "umbrella" else "sun", PxAccent, 14.dp)
                        Spacer(Modifier.width(6.dp))
                        Text(
                            if (soon) "未来 2 小时可能有雨，带伞" else "未来 2 小时内无降雨",
                            color = PxText, fontSize = 11.sp, fontFamily = PixelFont
                        )
                    }
                }

                // 生活指南
                item {
                    PixelCard(accent = PxBorderDim) {
                        PixelLabel("生活指南", "全部 >", onOpenAdvice)
                        data.advice.take(2).forEach { a ->
                            Row(Modifier.padding(vertical = 4.dp), verticalAlignment = Alignment.CenterVertically) {
                                PixelGlyph(glyphOf(a.icon, a.title), PxAccent, 16.dp)
                                Spacer(Modifier.width(8.dp))
                                Column {
                                    Text(a.title, color = PxText, fontSize = 12.sp, fontFamily = PixelFont)
                                    Text(a.sub, color = PxTextDim, fontSize = 10.sp, fontFamily = PixelFont)
                                }
                            }
                        }
                        PixelDivider()
                        Text(
                            "湿度 ${now?.humidity ?: 0}%  能见度 ${now?.visibility ?: 0}km  气压 ${now?.pressure ?: 0}hPa",
                            color = PxTextDim, fontSize = 10.sp, fontFamily = PixelFont,
                            modifier = Modifier.padding(top = 5.dp)
                        )
                    }
                }

                // 每小时
                item {
                    PixelCard {
                        PixelLabel("每小时预报")
                        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            data.hours.take(6).forEachIndexed { i, h ->
                                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                    Text(
                                        if (i == 0) "现在" else h.t.takeLast(5).take(2) + "时",
                                        color = PxTextDim, fontSize = 9.sp, fontFamily = PixelFont
                                    )
                                    Spacer(Modifier.height(4.dp))
                                    PixelGlyph(pixelIconOf(h.icon.let { iconGroup(it) }, isDay), PxText, 16.dp)
                                    Spacer(Modifier.height(4.dp))
                                    Text("${h.temp}°", color = PxText, fontSize = 11.sp, fontFamily = PixelFont)
                                    if (h.pop >= 20) {
                                        Text("${h.pop}%", color = PxBlue, fontSize = 8.sp, fontFamily = PixelFont)
                                    }
                                }
                            }
                            // 日落
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Text("日落", color = PxAccent, fontSize = 9.sp, fontFamily = PixelFont)
                                Spacer(Modifier.height(4.dp))
                                PixelGlyph("sunset", PxAccent, 16.dp)
                                Spacer(Modifier.height(4.dp))
                                Text(now?.sunset?.take(5) ?: "--", color = PxAccent, fontSize = 9.sp, fontFamily = PixelFont)
                            }
                        }
                    }
                }

                // 指标
                item {
                    PixelCard {
                        Row(Modifier.fillMaxWidth()) {
                            val uv = now?.uv ?: 0
                            PxMetric("uv", "紫外线", "$uv", when {
                                uv <= 2 -> "弱"; uv <= 5 -> "中等"; uv <= 7 -> "强"; else -> "很强"
                            })
                            PxMetric("hum", "湿度", "${now?.humidity ?: 0}%", when {
                                (now?.humidity ?: 0) < 30 -> "干燥"
                                (now?.humidity ?: 0) < 60 -> "舒适"
                                else -> "偏湿"
                            })
                            PxMetric("wind", "风力", "${now?.windLevel ?: 0} 级", now?.windDir ?: "-")
                        }
                    }
                }

                // 15 天
                item {
                    PixelCard {
                        PixelLabel("未来 15 天", "全部 >", onOpenDays)
                        val lo = data.days.minOf { it.lo }
                        val hi = data.days.maxOf { it.hi }
                        val span = (hi - lo).coerceAtLeast(1)
                        data.days.take(5).forEachIndexed { i, d ->
                            Row(
                                Modifier.fillMaxWidth().padding(vertical = 5.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    dayLabel(d.date, i),
                                    color = PxText, fontSize = 10.sp, fontFamily = PixelFont,
                                    modifier = Modifier.width(38.dp)
                                )
                                Text(
                                    d.date.takeLast(5).replace("-", "/"),
                                    color = PxTextDim, fontSize = 8.sp, fontFamily = PixelFont,
                                    modifier = Modifier.width(34.dp)
                                )
                                PixelGlyph(pixelIconOf(iconGroup(d.icon), true), PxText, 14.dp)
                                Spacer(Modifier.width(6.dp))
                                Text(
                                    "${d.lo}°",
                                    color = PxTextDim, fontSize = 9.sp, fontFamily = PixelFont,
                                    modifier = Modifier.width(24.dp)
                                )
                                // 像素温度条
                                Row(Modifier.weight(1f).height(6.dp).background(Color(0x22FFFFFF))) {
                                    Spacer(Modifier.weight(((d.lo - lo).toFloat() / span).coerceAtLeast(0f) + 0.01f))
                                    Box(
                                        Modifier
                                            .weight(((d.hi - d.lo).toFloat() / span).coerceAtLeast(0.08f))
                                            .fillMaxHeight()
                                            .background(PxAccent.copy(alpha = 0.75f))
                                    )
                                    Spacer(Modifier.weight(((hi - d.hi).toFloat() / span).coerceAtLeast(0f) + 0.01f))
                                }
                                Spacer(Modifier.width(6.dp))
                                Text(
                                    "${d.hi}°",
                                    color = PxText, fontSize = 9.sp, fontFamily = PixelFont,
                                    modifier = Modifier.width(24.dp)
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun androidx.compose.foundation.layout.RowScope.PxMetric(glyph: String, label: String, value: String, note: String) {
    Column(Modifier.weight(1f), horizontalAlignment = Alignment.CenterHorizontally) {
        PixelGlyph(glyph, PxTextDim, 16.dp)
        Spacer(Modifier.height(4.dp))
        Text(value, color = PxText, fontSize = 14.sp, fontFamily = PixelFont)
        Text(label, color = PxTextDim, fontSize = 9.sp, fontFamily = PixelFont)
        Text(note, color = PxTextDim.copy(alpha = 0.7f), fontSize = 8.sp, fontFamily = PixelFont)
    }
}

/** 小时图标的 group 名映射（服务端给的是像素图标名） */
private fun iconGroup(icon: String): String = when {
    icon.startsWith("clear") -> "clear"
    icon.contains("rain") || icon.contains("drizzle") -> "rain"
    icon.contains("snow") || icon.contains("sleet") -> "snow"
    icon.contains("thunder") -> "storm"
    icon.contains("fog") || icon.contains("haze") -> "fog"
    else -> "cloudy"
}
