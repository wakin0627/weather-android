package com.yoho.weather

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.delay

/* ---------------- Apple 天气式城市卡片列表 ---------------- */

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun CityCardsOverlay(
    current: City,
    locating: Boolean,
    onLocate: () -> Unit,
    onPick: (City) -> Unit,
    onClose: () -> Unit
) {
    val ctx = androidx.compose.ui.platform.LocalContext.current
    val favs = remember { mutableStateListOf<City>() }
    val cards = remember { mutableStateMapOf<String, CityCardInfo?>() }
    var showSearch by remember { mutableStateOf(false) }
    var deleteTarget by remember { mutableStateOf<City?>(null) }

    LaunchedEffect(Unit) {
        favs.clear()
        favs.addAll(CityStore.favorites(ctx))
    }

    // 逐张取数据（当前城市优先）
    LaunchedEffect(favs.toList(), current.name) {
        val list = buildList {
            if (current.located) add(current)
            addAll(favs)
        }.distinctBy { it.name }
        for (c in list) {
            if (cards.containsKey(c.name)) continue
            cards[c.name] = Api.quickCard(c.lng, c.lat)
        }
    }

    Box(Modifier.fillMaxSize().background(Color(0xFF080C12))) {
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 52.dp, bottom = 100.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            item {
                Row(
                    Modifier.fillMaxWidth().padding(bottom = 4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("城市", color = Color.White, fontSize = 26.sp, fontWeight = FontWeight.Bold)
                    Spacer(Modifier.weight(1f))
                    if (locating) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(18.dp),
                            strokeWidth = 2.dp,
                            color = Color.White.copy(alpha = 0.8f)
                        )
                        Spacer(Modifier.width(10.dp))
                    }
                    if (!current.located) {
                        CircleBtn("◎") { onLocate() }
                        Spacer(Modifier.width(8.dp))
                    }
                    CircleBtn("＋") { showSearch = true }
                    Spacer(Modifier.width(8.dp))
                    CircleBtn("✓") { onClose() }
                }
            }

            if (current.located) {
                item(key = "loc-" + current.name) {
                    CityWeatherCard(
                        city = current,
                        info = cards[current.name],
                        active = true,
                        badge = "我的位置",
                        onLongPress = null
                    ) { onPick(current) }
                }
            }

            items(favs) { c ->
                CityWeatherCard(
                    city = c,
                    info = cards[c.name],
                    active = !current.located && c.name == current.name,
                    badge = null,
                    onLongPress = { deleteTarget = c }
                ) { onPick(c) }
            }

            if (favs.isEmpty() && !current.located) {
                item {
                    Column(Modifier.fillMaxWidth().padding(top = 40.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                        Text("还没有收藏城市", color = Color.White.copy(alpha = 0.5f), fontSize = 14.sp)
                        Text(
                            "点右上角 ＋ 搜索添加",
                            color = Color.White.copy(alpha = 0.35f), fontSize = 12.5.sp,
                            modifier = Modifier.padding(top = 6.dp)
                        )
                    }
                }
            }
        }
    }

    if (showSearch) {
        CitySearchSheet(
            onPick = { c ->
                CityStore.addFavorite(ctx, c)
                favs.removeAll { it.name == c.name }
                favs.add(0, c)
                showSearch = false
                cards[c.name] = null
                onPick(c)
            },
            onDismiss = { showSearch = false }
        )
    }

    deleteTarget?.let { t ->
        AlertDialog(
            onDismissRequest = { deleteTarget = null },
            title = { Text("删除城市") },
            text = { Text("从列表移除「${t.name}」？") },
            confirmButton = {
                TextButton(onClick = {
                    CityStore.removeFavorite(ctx, t)
                    favs.removeAll { it.name == t.name }
                    deleteTarget = null
                }) { Text("删除", color = Color(0xFFFF6B6B)) }
            },
            dismissButton = { TextButton(onClick = { deleteTarget = null }) { Text("取消") } }
        )
    }
}

@Composable
private fun CircleBtn(label: String, onClick: () -> Unit) {
    Box(
        Modifier
            .size(34.dp)
            .clip(RoundedCornerShape(999.dp))
            .background(Color.White.copy(alpha = 0.14f))
            .border(0.7.dp, Color.White.copy(alpha = 0.2f), RoundedCornerShape(999.dp))
            .clickable { onClick() },
        contentAlignment = Alignment.Center
    ) {
        Text(label, color = Color.White, fontSize = 14.sp, fontWeight = FontWeight.Medium)
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun CityWeatherCard(
    city: City,
    info: CityCardInfo?,
    active: Boolean,
    badge: String?,
    onLongPress: (() -> Unit)?,
    onClick: () -> Unit
) {
    val shape = RoundedCornerShape(22.dp)
    val colors = skyColors(info?.group ?: "cloudy", info?.isDay ?: true)

    Box(
        Modifier
            .fillMaxWidth()
            .height(166.dp)
            .clip(shape)
            .background(Brush.verticalGradient(colors))
            .then(
                if (active) Modifier.border(2.dp, Color.White.copy(alpha = 0.85f), shape)
                else Modifier.border(0.7.dp, Color.White.copy(alpha = 0.16f), shape)
            )
            .combinedClickable(onClick = onClick, onLongClick = onLongPress)
    ) {
        // 压暗底部，保证文字
        Box(
            Modifier
                .fillMaxSize()
                .background(
                    Brush.verticalGradient(
                        0.45f to Color.Transparent,
                        1f to Color(0x5910161A)
                    )
                )
        )

        Column(Modifier.fillMaxSize().padding(18.dp)) {
            Row(verticalAlignment = Alignment.Top) {
                Column(Modifier.weight(1f)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            city.name,
                            color = Color.White,
                            fontSize = 19.sp,
                            fontWeight = FontWeight.SemiBold,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        if (active) {
                            Spacer(Modifier.width(6.dp))
                            Text("•", color = Color(0xFF7FD4FF), fontSize = 18.sp)
                        }
                    }
                    if (!city.sub.isNullOrEmpty()) {
                        Text(
                            city.sub,
                            color = Color.White.copy(alpha = 0.72f),
                            fontSize = 11.sp,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }
                Column(horizontalAlignment = Alignment.End) {
                    Text(
                        if (info != null) "${info.temp}°" else "--°",
                        color = Color.White,
                        fontSize = 42.sp,
                        fontWeight = FontWeight.Light,
                        lineHeight = 44.sp
                    )
                }
            }

            Spacer(Modifier.weight(1f))

            Row(verticalAlignment = Alignment.Bottom) {
                Column(Modifier.weight(1f)) {
                    Text(
                        info?.text ?: "加载中…",
                        color = Color.White.copy(alpha = 0.92f),
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Medium
                    )
                    if (info != null) {
                        Text(
                            "最高 ${info.hi}°　最低 ${info.lo}°" + if (info.pop > 0) "　降水 ${info.pop}%" else "",
                            color = Color.White.copy(alpha = 0.82f),
                            fontSize = 11.5.sp,
                            modifier = Modifier.padding(top = 2.dp)
                        )
                    }
                    if (badge != null) {
                        Text(
                            badge,
                            color = Color(0xFF8FE0FF),
                            fontSize = 10.5.sp,
                            fontWeight = FontWeight.SemiBold,
                            modifier = Modifier.padding(top = 4.dp)
                        )
                    }
                }
                if (info != null) WeatherIcon(info.icon, 42)
            }
        }
    }
}

/* ---------------- 搜索添加 ---------------- */

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun CitySearchSheet(onPick: (City) -> Unit, onDismiss: () -> Unit) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    var query by remember { mutableStateOf("") }
    var hits by remember { mutableStateOf<List<CityHit>>(emptyList()) }
    var searching by remember { mutableStateOf(false) }

    LaunchedEffect(query) {
        val q = query.trim()
        if (q.isEmpty()) {
            hits = emptyList(); searching = false; return@LaunchedEffect
        }
        searching = true
        delay(380)
        hits = Api.search(q)
        searching = false
    }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = Color(0xFF101720)
    ) {
        Column(Modifier.fillMaxWidth().padding(horizontal = 18.dp).padding(bottom = 24.dp)) {
            Text("添加城市", color = Color.White, fontSize = 19.sp, fontWeight = FontWeight.SemiBold)
            Spacer(Modifier.height(12.dp))
            OutlinedTextField(
                value = query,
                onValueChange = { query = it },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                placeholder = { Text("搜索城市 / 小区 / 地标", fontSize = 14.sp) },
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search)
            )
            Spacer(Modifier.height(10.dp))
            if (searching && hits.isEmpty()) {
                Box(Modifier.fillMaxWidth().padding(20.dp), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator(modifier = Modifier.size(18.dp), strokeWidth = 2.dp, color = Color(0xFF6FB6FF))
                }
            } else if (query.isNotBlank() && hits.isEmpty()) {
                Text("没找到「$query」", color = Color.White.copy(alpha = 0.45f), fontSize = 13.sp, modifier = Modifier.padding(vertical = 16.dp))
            } else {
                LazyColumn(Modifier.heightIn(max = 340.dp)) {
                    items(hits) { h ->
                        Row(
                            Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(12.dp))
                                .clickable { onPick(City(h.name, h.lng, h.lat, h.sub)) }
                                .padding(horizontal = 10.dp, vertical = 11.dp)
                        ) {
                            Column(Modifier.weight(1f)) {
                                Text(h.name, color = Color.White, fontSize = 14.sp)
                                if (h.sub.isNotEmpty()) {
                                    Text(h.sub, color = Color.White.copy(alpha = 0.45f), fontSize = 11.5.sp)
                                }
                            }
                            Text("＋", color = Color(0xFF6FB6FF), fontSize = 15.sp)
                        }
                    }
                }
            }
        }
    }
}
