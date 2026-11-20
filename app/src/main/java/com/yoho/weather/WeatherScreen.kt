package com.yoho.weather

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import coil.request.ImageRequest
import java.time.LocalDate

fun iconUrl(name: String) = Api.ICON_CDN + name + ".svg"

@Composable
fun WeatherIcon(name: String, size: Int) {
    AsyncImage(
        model = ImageRequest.Builder(LocalContext.current).data(iconUrl(name)).crossfade(true).build(),
        contentDescription = null,
        modifier = Modifier.size(size.dp),
        imageLoader = Api.imageLoader!!,
        contentScale = ContentScale.Fit
    )
}

@Composable
fun Pill(text: String, onClick: () -> Unit) {
    Box(
        Modifier
            .clip(RoundedCornerShape(999.dp))
            .background(Color.White.copy(alpha = 0.16f))
            .border(0.6.dp, Color.White.copy(alpha = 0.22f), RoundedCornerShape(999.dp))
            .clickable { onClick() }
            .padding(horizontal = 12.dp, vertical = 6.dp)
    ) {
        Text(text, color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
    }
}

@OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)
@Composable
fun WeatherScreen(
    data: WeatherBundle?,
    loading: Boolean,
    error: String?,
    onRefresh: () -> Unit,
    onOpenDays: () -> Unit,
    onToast: (String) -> Unit,
    cityName: String = "",
    onOpenCity: () -> Unit = {}
) {
    val now = data?.now
    val group = now?.group ?: "cloudy"
    val isDay = now?.isDay ?: true

    Box(Modifier.fillMaxSize()) {
        SkyBackdrop(group, isDay, now?.cloud ?: 40, now?.windKmh ?: 5)
        // 底部压暗，保证白字可读
        Box(
            Modifier
                .fillMaxSize()
                .background(
                    Brush.verticalGradient(
                        0.40f to Color.Transparent,
                        0.78f to Color(0x5710161A),
                        1f to Color(0x8C10161A)
                    )
                )
        )

        val pullState = androidx.compose.material3.pulltorefresh.rememberPullToRefreshState()
        androidx.compose.material3.pulltorefresh.PullToRefreshBox(
            isRefreshing = loading,
            onRefresh = onRefresh,
            state = pullState,
            modifier = Modifier.fillMaxSize(),
            indicator = {
                androidx.compose.material3.pulltorefresh.PullToRefreshDefaults.Indicator(
                    state = pullState,
                    isRefreshing = loading,
                    modifier = Modifier
                        .align(Alignment.TopCenter)
                        .padding(top = 54.dp),
                    containerColor = Color(0xCC15202B),
                    color = Color.White
                )
            }
        ) {
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 44.dp, bottom = 96.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            item {
                Row(
                    Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Pill(if (cityName.isBlank()) "城市" else cityName) { onOpenCity() }
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            now?.time?.takeLast(5) ?: "--:--",
                            color = Color.White.copy(alpha = 0.75f),
                            fontSize = 12.5.sp,
                            fontWeight = FontWeight.SemiBold,
                            modifier = Modifier.padding(end = 8.dp)
                        )
                        Pill(if (loading) "刷新中" else "刷新") { onRefresh() }
                    }
                }
            }

            item {
                Column(
                    Modifier.fillMaxWidth().padding(top = 6.dp, bottom = 6.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        data?.place?.city?.ifEmpty { "当前位置" } ?: "定位中",
                        color = Color.White.copy(alpha = 0.95f),
                        fontSize = 19.sp,
                        fontWeight = FontWeight.Medium
                    )
                    val addr = buildString {
                        data?.place?.let { p ->
                            if (p.district.isNotEmpty()) append(p.district)
                            if (p.township.isNotEmpty()) { if (isNotEmpty()) append(" · "); append(p.township) }
                            if (p.spot.isNotEmpty()) { if (isNotEmpty()) append(" · "); append(p.spot).append(" ").append(p.spotDist).append("m") }
                        }
                    }
                    if (addr.isNotEmpty()) {
                        Text(
                            addr,
                            color = Color.White.copy(alpha = 0.68f),
                            fontSize = 11.5.sp,
                            textAlign = TextAlign.Center,
                            modifier = Modifier.padding(top = 5.dp, start = 12.dp, end = 12.dp)
                        )
                    }
                    Text(
                        if (now != null) "${now.temp}°" else "--°",
                        color = Color.White,
                        fontSize = 92.sp,
                        fontWeight = FontWeight.ExtraLight,
                        letterSpacing = (-4).sp,
                        lineHeight = 96.sp
                    )
                    Text(now?.text ?: (error ?: "加载中"), color = Color.White, fontSize = 20.sp)
                    if (data != null) {
                        Text(
                            "最高 ${data.days.first().hi}°　最低 ${data.days.first().lo}°",
                            color = Color.White.copy(alpha = 0.72f),
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Medium,
                            modifier = Modifier.padding(top = 4.dp)
                        )
                    }
                }
            }

            if (data != null) {
                item { AdviceCard(data, onToast) }
                item { HourlyCard(data) }
                item { DaysCard(data, onOpenDays) }
                item { MiniGrid(data) }
                item {
                    Text(
                        "数据来自 Open-Meteo / 高德 · 演示版 v${Api.APP_VERSION}",
                        color = Color.White.copy(alpha = 0.45f),
                        fontSize = 10.5.sp,
                        modifier = Modifier.fillMaxWidth().padding(top = 2.dp),
                        textAlign = TextAlign.Center
                    )
                }
            } else if (error != null) {
                item {
                    GlassCard {
                        Text("加载失败", color = Color.White, fontSize = 15.sp, fontWeight = FontWeight.SemiBold)
                        Text(error, color = Color.White.copy(alpha = 0.7f), fontSize = 12.5.sp)
                        Text(
                            "点右上刷新重试",
                            color = Color.White.copy(alpha = 0.7f),
                            fontSize = 12.5.sp,
                            modifier = Modifier.padding(top = 4.dp)
                        )
                    }
                }
            }
        }
        }
    }
}

@Composable
private fun AdviceCard(data: WeatherBundle, onToast: (String) -> Unit) {
    val now = data.now
    GlassCard {
        CardLabel("生活指南", "全部 ›")
        data.advice.take(2).forEach { a ->
            Row(Modifier.fillMaxWidth().padding(vertical = 6.dp), verticalAlignment = Alignment.CenterVertically) {
                Box(
                    Modifier.size(22.dp).clip(RoundedCornerShape(6.dp))
                        .background(Color.White.copy(alpha = 0.14f))
                )
                Spacer(Modifier.width(10.dp))
                Column {
                    Text(a.title, color = Color.White, fontSize = 14.sp, fontWeight = FontWeight.Medium)
                    Text(a.sub, color = Color.White.copy(alpha = 0.62f), fontSize = 12.sp)
                }
            }
        }
        Text(
            "体感 ${now.feels}° · 湿度 ${now.humidity}% · 能见度 ${now.visibility}km",
            color = Color.White.copy(alpha = 0.5f),
            fontSize = 11.5.sp,
            modifier = Modifier.padding(top = 4.dp)
        )
    }
}

@Composable
private fun HourlyCard(data: WeatherBundle) {
    GlassCard {
        CardLabel("每小时预报")
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            data.hours.take(8).forEachIndexed { i, h ->
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        if (i == 0) "现在" else h.t.takeLast(5).take(2) + "时",
                        color = Color.White.copy(alpha = if (i == 0) 1f else 0.82f),
                        fontSize = 12.5.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                    Spacer(Modifier.height(6.dp))
                    WeatherIcon(h.icon, 28)
                    Spacer(Modifier.height(6.dp))
                    Text("${h.temp}°", color = Color.White, fontSize = 16.sp, fontWeight = FontWeight.SemiBold)
                    Text(
                        "${h.pop}%",
                        color = if (h.pop >= 30) Color(0xFF8FD0FF) else Color.White.copy(alpha = 0.42f),
                        fontSize = 10.5.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }
        }
    }
}

@Composable
private fun DaysCard(data: WeatherBundle, onOpenDays: () -> Unit) {
    val days = data.days
    val lo = days.minOf { it.lo }
    val hi = days.maxOf { it.hi }
    val span = (hi - lo).coerceAtLeast(1)

    GlassCard(Modifier.clickable { onOpenDays() }) {
        CardLabel("未来 15 天", "全部趋势 ›")
        days.take(6).forEachIndexed { i, d ->
            if (i > 0) Hairline()
            Row(
                Modifier.fillMaxWidth().padding(vertical = 5.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    dayLabel(d.date, i),
                    color = Color.White,
                    fontSize = 15.sp,
                    fontWeight = FontWeight.SemiBold,
                    modifier = Modifier.width(50.dp)
                )
                WeatherIcon(d.icon, 22)
                Text(
                    "${d.lo}°",
                    color = Color.White.copy(alpha = 0.62f),
                    fontSize = 15.sp,
                    modifier = Modifier.width(34.dp).padding(start = 4.dp),
                    textAlign = TextAlign.End
                )
                // 温度条：按全区间比例定位
                Row(Modifier.weight(1f).padding(horizontal = 10.dp).height(5.dp)) {
                    Spacer(Modifier.weight(((d.lo - lo).toFloat() / span).coerceAtLeast(0f) + 0.02f))
                    Box(
                        Modifier
                            .weight(((d.hi - d.lo).toFloat() / span).coerceAtLeast(0.08f))
                            .fillMaxHeight()
                            .clip(RoundedCornerShape(3.dp))
                            .background(
                                Brush.horizontalGradient(
                                    listOf(Color(0xFF6FB3D9), Color(0xFF9ECDB6), Color(0xFFE8C778), Color(0xFFE89A5C))
                                )
                            )
                    )
                    Spacer(Modifier.weight(1f))
                }
                Text(
                    "${d.hi}°",
                    color = Color.White,
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Medium,
                    modifier = Modifier.width(34.dp),
                    textAlign = TextAlign.End
                )
            }
        }
    }
}

@Composable
private fun MiniGrid(data: WeatherBundle) {
    val d0 = data.days.first()
    val now = data.now
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            MiniCell("紫外线", "${d0.uv.toInt()}", uvText(d0.uv), Modifier.weight(1f))
            MiniCell("日出", d0.sunrise, "日落 ${d0.sunset}", Modifier.weight(1f))
        }
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            MiniCell("风", "${now.windDir} ${now.windLevel}级", "${now.windKmh} km/h", Modifier.weight(1f))
            MiniCell("体感", "${now.feels}°", "湿度 ${now.humidity}%", Modifier.weight(1f))
        }
    }
}

@Composable
private fun MiniCell(label: String, value: String, sub: String, modifier: Modifier = Modifier) {
    Column(
        modifier
            .clip(RoundedCornerShape(Radius))
            .background(Color.White.copy(alpha = CardAlpha))
            .border(0.6.dp, Color.White.copy(alpha = CardBorder), RoundedCornerShape(Radius))
            .padding(horizontal = 13.dp, vertical = 10.dp)
    ) {
        Text(label, color = Color.White.copy(alpha = 0.68f), fontSize = 11.sp, fontWeight = FontWeight.SemiBold)
        Text(
            value,
            color = Color.White,
            fontSize = 22.sp,
            fontWeight = FontWeight.Light,
            modifier = Modifier.padding(top = 4.dp)
        )
        Text(sub, color = Color.White.copy(alpha = 0.72f), fontSize = 12.sp)
    }
}

fun uvText(uv: Double) = when {
    uv < 3 -> "低"
    uv < 6 -> "中等"
    uv < 8 -> "高"
    uv < 11 -> "很高"
    else -> "极高"
}

fun dayLabel(date: String, index: Int): String {
    if (index == 0) return "今天"
    if (index == 1) return "明天"
    return try {
        val d = LocalDate.parse(date)
        val names = arrayOf("周一", "周二", "周三", "周四", "周五", "周六", "周日")
        names[d.dayOfWeek.value - 1]
    } catch (e: Exception) {
        date.takeLast(5)
    }
}
