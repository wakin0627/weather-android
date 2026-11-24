package com.yoho.weather

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/** 生活建议类型 -> emoji（服务端给的是 jacket/car/dry/run 这类 key） */
fun adviceEmoji(icon: String, title: String): String {
    val key = icon.lowercase()
    val t = title
    return when {
        key.contains("jacket") || key.contains("cloth") || t.contains("穿") -> "🧥"
        key.contains("car") || t.contains("洗车") -> "🚗"
        key.contains("dry") || t.contains("晾") -> "🧺"
        key.contains("hike") || t.contains("爬山") || t.contains("徒步") -> "🥾"
        key.contains("run") || key.contains("sport") || t.contains("运动") || t.contains("跑") -> "🏃"
        key.contains("umbrella") || key.contains("rain") || t.contains("伞") || t.contains("雨") -> "☂️"
        key.contains("sun") || key.contains("uv") || t.contains("晒") || t.contains("紫外线") -> "🕶️"
        key.contains("mask") || key.contains("air") || t.contains("空气") || t.contains("霾") -> "😷"
        key.contains("wind") || t.contains("风") -> "🍃"
        key.contains("water") || key.contains("humid") || t.contains("湿") || t.contains("补水") -> "💧"
        key.contains("cold") || t.contains("感冒") || t.contains("温差") -> "🤧"
        else -> "✨"
    }
}

@Composable
fun AdviceScreen(data: WeatherBundle, onClose: () -> Unit) {
    val now = data.now
    Box(Modifier.fillMaxSize().background(Color(0xFF0A0F16))) {
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 48.dp, bottom = 40.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            item {
                Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                    Text("‹", color = Color.White, fontSize = 26.sp, modifier = Modifier.clickable { onClose() })
                    Spacer(Modifier.width(10.dp))
                    Column(Modifier.weight(1f)) {
                        Text("生活指南", color = Color.White, fontSize = 24.sp, fontWeight = FontWeight.Bold)
                        Text(
                            data.place?.let { "${it.city} ${it.district}" } ?: "当前位置",
                            color = Color.White.copy(alpha = 0.5f), fontSize = 12.sp
                        )
                    }
                    Text(
                        "${now.temp}°",
                        color = Color.White, fontSize = 30.sp, fontWeight = FontWeight.Light
                    )
                }
            }

            item {
                GlassCard {
                    CardLabel("今天建议")
                    data.advice.forEachIndexed { i, a ->
                        if (i > 0) Hairline()
                        Row(Modifier.fillMaxWidth().padding(vertical = 8.dp), verticalAlignment = Alignment.CenterVertically) {
                            Box(
                                Modifier
                                    .size(38.dp)
                                    .clip(RoundedCornerShape(12.dp))
                                    .background(Color.White.copy(alpha = 0.14f)),
                                contentAlignment = Alignment.Center
                            ) {
                                Glyph(glyphOf(a.icon, a.title), Color.White.copy(alpha = 0.92f), 19.dp)
                            }
                            Spacer(Modifier.width(12.dp))
                            Column {
                                Text(a.title, color = Color.White, fontSize = 14.5.sp, fontWeight = FontWeight.Medium)
                                Text(a.sub, color = Color.White.copy(alpha = 0.6f), fontSize = 12.sp)
                            }
                        }
                    }
                }
            }

            item {
                GlassCard {
                    CardLabel("当前环境")
                    Row(Modifier.fillMaxWidth()) {
                        Metric("体感", "${now.feels}°", "🌡")
                        Metric("湿度", "${now.humidity}%", "💧")
                        Metric("露点", "${now.dew}°", "🫧")
                    }
                    Spacer(Modifier.height(10.dp))
                    Row(Modifier.fillMaxWidth()) {
                        Metric("能见度", "${now.visibility}km", "👁")
                        Metric("气压", "${now.pressure}hPa", "📊")
                        Metric("风", "${now.windDir}${now.windLevel}级", "🍃")
                    }
                }
            }

            data.air?.let { air ->
                item {
                    GlassCard {
                        CardLabel("空气质量")
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                "${air.aqi}",
                                color = Color.White, fontSize = 30.sp, fontWeight = FontWeight.Light
                            )
                            Spacer(Modifier.width(10.dp))
                            Column(Modifier.weight(1f)) {
                                Text("${air.level} · US AQI", color = Color.White.copy(alpha = 0.85f), fontSize = 13.sp,
                                    fontWeight = FontWeight.SemiBold)
                                Text(
                                    airTip(air.aqi),
                                    color = Color.White.copy(alpha = 0.6f), fontSize = 11.5.sp
                                )
                            }
                        }
                        Spacer(Modifier.height(10.dp))
                        Row(Modifier.fillMaxWidth()) {
                            Poll("PM2.5", air.pm25)
                            Poll("PM10", air.pm10)
                            Poll("O₃", air.o3)
                            Poll("NO₂", air.no2)
                        }
                    }
                }
            }

            item {
                GlassCard {
                    CardLabel("紫外线")
                    val uv = now.uv
                    val (level, tip) = when {
                        uv <= 2 -> "弱" to "日常不用特别防晒"
                        uv <= 5 -> "中等" to "涂 SPF15+，中午戴个帽子"
                        uv <= 7 -> "强" to "SPF30+，尽量避开正午直晒"
                        uv <= 10 -> "很强" to "SPF50+，遮阳帽 + 太阳镜"
                        else -> "极强" to "避免午间外出，皮肤十分钟就会晒伤"
                    }
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text("☀️", fontSize = 22.sp)
                        Spacer(Modifier.width(10.dp))
                        Column(Modifier.weight(1f)) {
                            Row(verticalAlignment = Alignment.Bottom) {
                                Text("$uv", color = Color.White, fontSize = 26.sp, fontWeight = FontWeight.Light)
                                Text("  $level", color = Color.White.copy(alpha = 0.85f), fontSize = 14.sp,
                                    fontWeight = FontWeight.SemiBold, modifier = Modifier.padding(bottom = 4.dp))
                            }
                            Text(tip, color = Color.White.copy(alpha = 0.6f), fontSize = 12.sp)
                        }
                        if (now.sunrise.isNotEmpty() || now.sunset.isNotEmpty()) {
                            Column(horizontalAlignment = Alignment.End) {
                                Text("${now.sunrise} 日出", color = Color.White.copy(alpha = 0.55f), fontSize = 11.sp)
                                Text("${now.sunset} 日落", color = Color.White.copy(alpha = 0.55f), fontSize = 11.sp)
                            }
                        }
                    }
                }
            }

            item {
                GlassCard {
                    CardLabel("未来七天节奏")
                    data.days.take(7).forEachIndexed { i, d ->
                        if (i > 0) Hairline()
                        Row(
                            Modifier.fillMaxWidth().padding(vertical = 8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(Modifier.width(58.dp)) {
                                Text(dayLabel(d.date, i), color = Color.White, fontSize = 13.5.sp, fontWeight = FontWeight.SemiBold)
                                Text(d.date.takeLast(5), color = Color.White.copy(alpha = 0.45f), fontSize = 10.5.sp)
                            }
                            WeatherIcon(d.icon, 22)
                            Spacer(Modifier.weight(1f))
                            Column(horizontalAlignment = Alignment.End) {
                                Text(
                                    buildString {
                                        if (d.pop >= 30) append("降水 ${d.pop}%  ")
                                        if (d.uv >= 6) append("强紫外 ")
                                        append("${d.lo}° / ${d.hi}°")
                                    },
                                    color = Color.White.copy(alpha = 0.8f), fontSize = 12.5.sp
                                )
                                Text(
                                    dayTip(d),
                                    color = Color.White.copy(alpha = 0.5f), fontSize = 11.sp
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
private fun RowScope.Metric(label: String, value: String, emoji: String) {
    Column(Modifier.weight(1f), horizontalAlignment = Alignment.CenterHorizontally) {
        Glyph(if (label == "紫外线") "uv" else if (label == "湿度") "hum" else if (label == "风") "wind" else if (label == "气压") "press" else if (label == "能见度") "vis" else "air", Color.White.copy(alpha = 0.55f), 15.dp)
        Text(value, color = Color.White, fontSize = 15.sp, fontWeight = FontWeight.SemiBold)
        Text(label, color = Color.White.copy(alpha = 0.5f), fontSize = 10.5.sp)
    }
}

private fun airTip(aqi: Int): String = when {
    aqi <= 50 -> "空气很好，开窗通风"
    aqi <= 100 -> "可以正常户外活动"
    aqi <= 150 -> "敏感人群减少户外"
    aqi <= 200 -> "建议戴口罩，少开窗"
    else -> "尽量留在室内"
}

@Composable
private fun RowScope.Poll(label: String, v: Double) {
    Column(Modifier.weight(1f), horizontalAlignment = Alignment.CenterHorizontally) {
        Text(String.format("%.0f", v), color = Color.White, fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
        Text(label, color = Color.White.copy(alpha = 0.45f), fontSize = 10.sp)
    }
}

private fun dayTip(d: Day): String = when {
    d.pop >= 60 -> "记得带伞"
    d.uv >= 7 -> "注意防晒"
    d.hi - d.lo >= 12 -> "温差大，备件外套"
    d.pop >= 30 -> "可能有零星小雨"
    d.hi >= 30 -> "偏热，注意补水"
    d.lo <= 0 -> "夜间结冰，注意保暖"
    else -> "适合外出"
}
