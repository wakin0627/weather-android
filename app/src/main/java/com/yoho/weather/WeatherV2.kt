package com.yoho.weather

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/* 精简线条天气图标（替代像素图标，贴合 Linear 的 hairline 语言） */
@Composable
fun WxIcon(kind: String, tint: Color, size: Dp = 22.dp) {
    Canvas(Modifier.size(size)) {
        val w = this.size.width
        val h = this.size.height
        val sw = w * 0.068f
        val st = Stroke(width = sw, cap = StrokeCap.Round)
        fun l(x1: Float, y1: Float, x2: Float, y2: Float, c: Color = tint) =
            drawLine(c, Offset(x1 * w, y1 * h), Offset(x2 * w, y2 * h), sw, StrokeCap.Round)

        when (kind) {
            "sun" -> {
                drawCircle(tint, radius = w * 0.19f, center = Offset(w * 0.5f, h * 0.5f), style = st)
                for (i in 0 until 8) {
                    val a = Math.toRadians(i * 45.0)
                    val cx = (w * 0.5f + kotlin.math.cos(a).toFloat() * w * 0.32f)
                    val cy = (h * 0.5f + kotlin.math.sin(a).toFloat() * h * 0.32f)
                    val dx = (w * 0.5f + kotlin.math.cos(a).toFloat() * w * 0.45f)
                    val dy = (h * 0.5f + kotlin.math.sin(a).toFloat() * h * 0.45f)
                    l(cx / w, cy / h, dx / w, dy / h)
                }
            }
            "moon" -> {
                val p = Path().apply {
                    moveTo(w * 0.62f, h * 0.16f)
                    cubicTo(w * 0.24f, h * 0.24f, w * 0.24f, h * 0.78f, w * 0.62f, h * 0.86f)
                    cubicTo(w * 0.40f, h * 0.66f, w * 0.40f, h * 0.36f, w * 0.62f, h * 0.16f)
                    close()
                }
                drawPath(p, tint, style = st)
            }
            "cloud" -> {
                val p = Path().apply {
                    moveTo(w * 0.22f, h * 0.68f)
                    cubicTo(w * 0.08f, h * 0.68f, w * 0.08f, h * 0.44f, w * 0.28f, h * 0.44f)
                    cubicTo(w * 0.32f, h * 0.22f, w * 0.68f, h * 0.20f, w * 0.72f, h * 0.44f)
                    cubicTo(w * 0.92f, h * 0.42f, w * 0.96f, h * 0.68f, w * 0.78f, h * 0.68f)
                    close()
                }
                drawPath(p, tint, style = st)
            }
            "rain" -> {
                val p = Path().apply {
                    moveTo(w * 0.24f, h * 0.58f)
                    cubicTo(w * 0.10f, h * 0.58f, w * 0.10f, h * 0.36f, w * 0.30f, h * 0.36f)
                    cubicTo(w * 0.34f, h * 0.16f, w * 0.68f, h * 0.14f, w * 0.72f, h * 0.36f)
                    cubicTo(w * 0.90f, h * 0.34f, w * 0.94f, h * 0.58f, w * 0.76f, h * 0.58f)
                    close()
                }
                drawPath(p, tint, style = st)
                l(0.34f, 0.74f, 0.30f, 0.90f); l(0.52f, 0.74f, 0.48f, 0.90f); l(0.70f, 0.74f, 0.66f, 0.90f)
            }
            "snow" -> {
                val p = Path().apply {
                    moveTo(w * 0.24f, h * 0.56f)
                    cubicTo(w * 0.10f, h * 0.56f, w * 0.10f, h * 0.34f, w * 0.30f, h * 0.34f)
                    cubicTo(w * 0.34f, h * 0.14f, w * 0.68f, h * 0.12f, w * 0.72f, h * 0.34f)
                    cubicTo(w * 0.90f, h * 0.32f, w * 0.94f, h * 0.56f, w * 0.76f, h * 0.56f)
                    close()
                }
                drawPath(p, tint, style = st)
                drawCircle(tint, sw * 0.9f, Offset(w * 0.36f, h * 0.76f))
                drawCircle(tint, sw * 0.9f, Offset(w * 0.54f, h * 0.86f))
                drawCircle(tint, sw * 0.9f, Offset(w * 0.70f, h * 0.74f))
            }
            "storm" -> {
                val p = Path().apply {
                    moveTo(w * 0.24f, h * 0.52f)
                    cubicTo(w * 0.10f, h * 0.52f, w * 0.10f, h * 0.30f, w * 0.30f, h * 0.30f)
                    cubicTo(w * 0.34f, h * 0.10f, w * 0.68f, h * 0.08f, w * 0.72f, h * 0.30f)
                    cubicTo(w * 0.90f, h * 0.28f, w * 0.94f, h * 0.52f, w * 0.76f, h * 0.52f)
                    close()
                }
                drawPath(p, tint, style = st)
                val b = Path().apply {
                    moveTo(w * 0.54f, h * 0.60f); lineTo(w * 0.42f, h * 0.80f); lineTo(w * 0.54f, h * 0.80f)
                    lineTo(w * 0.46f, h * 0.96f)
                }
                drawPath(b, tint, style = st)
            }
            "fog" -> {
                l(0.18f, 0.34f, 0.78f, 0.34f); l(0.30f, 0.52f, 0.90f, 0.52f); l(0.16f, 0.70f, 0.72f, 0.70f)
            }
            else -> drawCircle(tint, w * 0.3f, Offset(w * 0.5f, h * 0.5f), style = st)
        }
    }
}

private fun wxKind(group: String, isDay: Boolean): String = when (group) {
    "clear", "few" -> if (isDay) "sun" else "moon"
    "rain" -> "rain"
    "snow" -> "snow"
    "storm" -> "storm"
    "fog" -> "fog"
    else -> "cloud"
}

private fun hourKind(icon: String, isDay: Boolean): String = when {
    icon.contains("thunder") -> "storm"
    icon.contains("snow") || icon.contains("sleet") -> "snow"
    icon.contains("rain") || icon.contains("drizzle") -> "rain"
    icon.contains("fog") || icon.contains("haze") -> "fog"
    icon.contains("clear") || icon.contains("sun") -> if (isDay) "sun" else "moon"
    else -> "cloud"
}

/* ---------------- 天气页（Linear 设计系统） ---------------- */

@Composable
fun WeatherV2Screen(
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
    val isDay = now?.isDay ?: true

    Box(Modifier.fillMaxSize()) {
        LnBackdrop(isNight = !isDay)
        LazyColumn(
            Modifier.fillMaxSize(),
            contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 40.dp, bottom = 124.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // 顶部工具行
            item {
                Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                    Row(
                        Modifier
                            .background(Ln.surface1, RoundedCornerShape(8.dp))
                            .border(1.dp, Ln.hairline, RoundedCornerShape(8.dp))
                            .clickable { onOpenCity() }
                            .padding(horizontal = 10.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            cityName.ifBlank { "选择城市" },
                            color = Ln.inkMuted, fontSize = 13.sp, fontWeight = FontWeight.Medium
                        )
                    }
                    Spacer(Modifier.weight(1f))
                    Text(
                        now?.time?.takeLast(5)?.let { "更新 $it" } ?: "",
                        color = Ln.inkTertiary, fontSize = 12.sp
                    )
                    Spacer(Modifier.width(10.dp))
                    Text(
                        if (loading) "刷新中" else "刷新",
                        color = if (loading) Ln.inkTertiary else Ln.inkMuted,
                        fontSize = 13.sp,
                        modifier = Modifier.clickable { if (!loading) onRefresh() }
                    )
                }
            }

            // 主视觉
            item {
                Column(Modifier.fillMaxWidth().padding(top = 10.dp, bottom = 2.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            data?.place?.city?.ifEmpty { cityName } ?: cityName,
                            color = Ln.inkSubtle, fontSize = 14.sp, letterSpacing = 0.2.sp
                        )
                        Spacer(Modifier.width(8.dp))
                        val sub = data?.place?.let { p ->
                            listOf(p.district, p.township).filter { it.isNotEmpty() }.joinToString(" · ")
                        } ?: ""
                        if (sub.isNotEmpty()) Text(sub, color = Ln.inkTertiary, fontSize = 13.sp)
                    }
                    Spacer(Modifier.height(6.dp))
                    Row(verticalAlignment = Alignment.Bottom) {
                        Text(
                            if (now != null) "${now.temp}" else "--",
                            color = Ln.ink, fontSize = 84.sp, fontWeight = FontWeight.SemiBold,
                            letterSpacing = (-3).sp, lineHeight = 86.sp,
                            style = androidx.compose.ui.text.TextStyle(fontFeatureSettings = "tnum")
                        )
                        Text(
                            "°", color = Ln.inkSubtle, fontSize = 30.sp, fontWeight = FontWeight.Light,
                            modifier = Modifier.padding(bottom = 16.dp)
                        )
                        Spacer(Modifier.width(14.dp))
                        Column(Modifier.padding(bottom = 14.dp)) {
                            Text(now?.text ?: "…", color = Ln.ink, fontSize = 16.sp)
                            if (data != null) Text(
                                "最高 ${data.days.first().hi}°  最低 ${data.days.first().lo}°",
                                color = Ln.inkSubtle, fontSize = 13.sp,
                                modifier = Modifier.padding(top = 2.dp)
                            )
                        }
                    }
                    if (error != null) Text(error, color = Color(0xFFE5484D), fontSize = 12.sp)
                }
            }

            // 关键指标条
            if (data != null) {
                item {
                    Row(
                        Modifier
                            .fillMaxWidth()
                            .background(Ln.surface1, RoundedCornerShape(12.dp))
                            .border(1.dp, Ln.hairline, RoundedCornerShape(12.dp))
                            .padding(vertical = 14.dp)
                    ) {
                        V2Stat("紫外线", "${now?.uv ?: 0}", when {
                            (now?.uv ?: 0) <= 2 -> "弱"; (now?.uv ?: 0) <= 5 -> "中等"
                            (now?.uv ?: 0) <= 7 -> "强"; else -> "很强"
                        })
                        V2Divider()
                        V2Stat("湿度", "${now?.humidity ?: 0}%", when {
                            (now?.humidity ?: 0) < 30 -> "干燥"; (now?.humidity ?: 0) < 60 -> "舒适"; else -> "偏湿"
                        })
                        V2Divider()
                        V2Stat("风", "${now?.windLevel ?: 0} 级", now?.windDir ?: "-")
                        V2Divider()
                        V2Stat("空气", "${data.air?.aqi ?: 0}", data.air?.level ?: "-")
                    }
                }

                // 降水提示
                item {
                    val soon = data.hours.take(3).any { it.pop >= 40 }
                    Row(
                        Modifier
                            .fillMaxWidth()
                            .background(if (soon) Ln.accentSoft else Ln.surface1, RoundedCornerShape(10.dp))
                            .border(1.dp, if (soon) Ln.accent.copy(alpha = 0.45f) else Ln.hairline, RoundedCornerShape(10.dp))
                            .padding(horizontal = 14.dp, vertical = 11.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        WxIcon(if (soon) "rain" else "sun", if (soon) Ln.accent else Ln.inkSubtle, 18.dp)
                        Spacer(Modifier.width(10.dp))
                        Text(
                            if (soon) "未来 2 小时可能有雨，记得带伞" else "未来 2 小时内无降雨",
                            color = if (soon) Ln.ink else Ln.inkMuted, fontSize = 13.sp
                        )
                    }
                }

                // 生活指南
                item {
                    LnCard {
                        LnLabel("生活指南", "全部", onOpenAdvice)
                        data.advice.take(2).forEachIndexed { i, a ->
                            if (i > 0) Box(Modifier.fillMaxWidth().height(1.dp).background(Ln.hairline))
                            Row(Modifier.fillMaxWidth().padding(vertical = 11.dp), verticalAlignment = Alignment.CenterVertically) {
                                Glyph(glyphOf(a.icon, a.title), Ln.inkMuted, 20.dp)
                                Spacer(Modifier.width(12.dp))
                                Column {
                                    Text(a.title, color = Ln.ink, fontSize = 14.sp, fontWeight = FontWeight.Medium)
                                    Text(a.sub, color = Color(0xFF888D96), fontSize = 12.sp, modifier = Modifier.padding(top = 2.dp))
                                }
                            }
                        }
                    }
                }

                // 每小时
                item {
                    LnCard {
                        LnLabel("每小时")
                        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            data.hours.take(6).forEachIndexed { i, hr ->
                                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                    Text(
                                        if (i == 0) "现在" else "${hr.t.takeLast(5).take(2)}时",
                                        color = if (i == 0) Ln.inkMuted else Ln.inkSubtle, fontSize = 11.sp
                                    )
                                    Spacer(Modifier.height(8.dp))
                                    WxIcon(hourKind(hr.icon, isDay), Ln.inkMuted, 20.dp)
                                    Spacer(Modifier.height(8.dp))
                                    Text("${hr.temp}°", color = Ln.ink, fontSize = 13.sp, fontWeight = FontWeight.Medium,
                                        style = androidx.compose.ui.text.TextStyle(fontFeatureSettings = "tnum"))
                                    if (hr.pop >= 20) Text("${hr.pop}%", color = Ln.accent, fontSize = 10.sp)
                                }
                            }
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Text("日落", color = Ln.inkSubtle, fontSize = 11.sp)
                                Spacer(Modifier.height(8.dp))
                                WxIcon("sun", Ln.inkSubtle, 20.dp)
                                Spacer(Modifier.height(8.dp))
                                Text(now?.sunset?.take(5) ?: "--", color = Ln.inkMuted, fontSize = 12.sp)
                            }
                        }
                    }
                }

                // 15 天
                item {
                    LnCard(padding = 16.dp) {
                        LnLabel("未来 15 天", "全部", onOpenDays)
                        val lo = data.days.minOf { it.lo }
                        val hi = data.days.maxOf { it.hi }
                        val span = (hi - lo).coerceAtLeast(1)
                        data.days.take(7).forEachIndexed { i, d ->
                            if (i > 0) Box(Modifier.fillMaxWidth().height(1.dp).background(Ln.hairline))
                            Row(
                                Modifier.fillMaxWidth().padding(vertical = 9.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    dayLabel(d.date, i),
                                    color = Ln.ink, fontSize = 13.sp,
                                    modifier = Modifier.width(42.dp)
                                )
                                Text(
                                    d.date.takeLast(5).replace("-", "/"),
                                    color = Ln.inkTertiary, fontSize = 11.sp,
                                    modifier = Modifier.width(38.dp)
                                )
                                WxIcon(hourKind(d.icon, true), Ln.inkSubtle, 18.dp)
                                Spacer(Modifier.width(10.dp))
                                Text(
                                    "${d.lo}°", color = Ln.inkTertiary, fontSize = 12.sp,
                                    modifier = Modifier.width(26.dp)
                                )
                                Box(Modifier.weight(1f).height(4.dp).background(Ln.surface3, RoundedCornerShape(2.dp))) {
                                    Row(Modifier.fillMaxSize()) {
                                        Spacer(Modifier.weight(((d.lo - lo).toFloat() / span).coerceAtLeast(0f) + 0.02f))
                                        Box(
                                            Modifier
                                                .weight(((d.hi - d.lo).toFloat() / span).coerceAtLeast(0.1f))
                                                .fillMaxHeight()
                                                .background(Ln.inkSubtle, RoundedCornerShape(2.dp))
                                        )
                                        Spacer(Modifier.weight(((hi - d.hi).toFloat() / span).coerceAtLeast(0f) + 0.02f))
                                    }
                                }
                                Spacer(Modifier.width(10.dp))
                                Text(
                                    "${d.hi}°", color = Ln.ink, fontSize = 12.sp,
                                    modifier = Modifier.width(26.dp)
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
private fun RowScope.V2Stat(label: String, value: String, note: String) {
    Column(Modifier.weight(1f), horizontalAlignment = Alignment.CenterHorizontally) {
        Text(value, color = Ln.ink, fontSize = 16.sp, fontWeight = FontWeight.Medium)
        Text(label, color = Ln.inkSubtle, fontSize = 11.sp, modifier = Modifier.padding(top = 3.dp))
        Text(note, color = Color(0xFF7E838C), fontSize = 10.sp, modifier = Modifier.padding(top = 1.dp))
    }
}

@Composable
private fun V2Divider() {
    Box(Modifier.width(1.dp).height(28.dp).background(Ln.hairline.copy(alpha = 0.7f)))
}
