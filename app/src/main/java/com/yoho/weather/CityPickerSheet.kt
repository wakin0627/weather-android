package com.yoho.weather

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.delay

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CityPickerSheet(
    current: City?,
    locating: Boolean,
    onLocate: () -> Unit,
    onPick: (City) -> Unit,
    onDelete: (City) -> Unit,
    onDismiss: () -> Unit
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val ctx = androidx.compose.ui.platform.LocalContext.current
    val favs = remember { mutableStateListOf<City>() }

    var query by remember { mutableStateOf("") }
    var hits by remember { mutableStateOf<List<CityHit>>(emptyList()) }
    var searching by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        favs.clear()
        favs.addAll(CityStore.favorites(ctx))
    }

    LaunchedEffect(query) {
        val q = query.trim()
        if (q.length < 1) {
            hits = emptyList()
            searching = false
            return@LaunchedEffect
        }
        searching = true
        delay(380)
        hits = Api.search(q)
        searching = false
    }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = Color(0xFF101720),
        dragHandle = { BottomSheetDefaultsHandle() }
    ) {
        Column(Modifier.fillMaxWidth().padding(horizontal = 18.dp).padding(bottom = 20.dp)) {
            Text("城市", color = Color.White, fontSize = 20.sp, fontWeight = FontWeight.SemiBold)
            Spacer(Modifier.height(12.dp))

            // 当前定位
            Row(
                Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(14.dp))
                    .background(if (current?.located == true) Color(0xFF2A6BD4).copy(alpha = 0.28f) else Color.White.copy(alpha = 0.07f))
                    .clickable(enabled = !locating) { onLocate() }
                    .padding(horizontal = 14.dp, vertical = 13.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("◎", color = Color(0xFF6FB6FF), fontSize = 16.sp)
                Spacer(Modifier.width(10.dp))
                Column(Modifier.weight(1f)) {
                    Text(
                        if (current?.located == true) "当前位置 · ${current.name}" else "使用当前位置",
                        color = Color.White, fontSize = 14.sp, fontWeight = FontWeight.Medium
                    )
                    if (current?.located == true && current.sub.isNotEmpty()) {
                        Text(current.sub, color = Color.White.copy(alpha = 0.5f), fontSize = 11.5.sp)
                    }
                }
                if (locating) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(16.dp),
                        strokeWidth = 2.dp,
                        color = Color(0xFF6FB6FF)
                    )
                }
            }

            Spacer(Modifier.height(14.dp))

            OutlinedTextField(
                value = query,
                onValueChange = { query = it },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                placeholder = { Text("搜索城市 / 小区 / 地标", fontSize = 14.sp) },
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search)
            )

            // 搜索结果
            if (query.isNotBlank()) {
                Spacer(Modifier.height(8.dp))
                if (searching && hits.isEmpty()) {
                    Row(Modifier.fillMaxWidth().padding(vertical = 18.dp), horizontalArrangement = Arrangement.Center) {
                        CircularProgressIndicator(modifier = Modifier.size(18.dp), strokeWidth = 2.dp, color = Color(0xFF6FB6FF))
                    }
                } else if (hits.isEmpty()) {
                    Text(
                        "没找到「$query」",
                        color = Color.White.copy(alpha = 0.45f), fontSize = 13.sp,
                        modifier = Modifier.padding(vertical = 16.dp)
                    )
                } else {
                    LazyColumn(Modifier.heightIn(max = 300.dp)) {
                        items(hits) { hit ->
                            Row(
                                Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(12.dp))
                                    .clickable {
                                        onPick(City(hit.name, hit.lng, hit.lat, hit.sub))
                                    }
                                    .padding(horizontal = 10.dp, vertical = 11.dp)
                            ) {
                                Column(Modifier.weight(1f)) {
                                    Text(hit.name, color = Color.White, fontSize = 14.sp)
                                    if (hit.sub.isNotEmpty()) {
                                        Text(hit.sub, color = Color.White.copy(alpha = 0.45f), fontSize = 11.5.sp)
                                    }
                                }
                                Text("＋", color = Color(0xFF6FB6FF), fontSize = 15.sp)
                            }
                        }
                    }
                }
            }

            // 收藏
            if (query.isBlank()) {
                Spacer(Modifier.height(16.dp))
                Text(
                    "已收藏", color = Color.White.copy(alpha = 0.45f), fontSize = 12.sp,
                    modifier = Modifier.padding(bottom = 6.dp)
                )
                if (favs.isEmpty()) {
                    Text("还没有收藏城市，搜索后添加", color = Color.White.copy(alpha = 0.3f), fontSize = 12.5.sp)
                } else {
                    LazyColumn(Modifier.heightIn(max = 320.dp)) {
                        items(favs) { c ->
                            val active = current != null && current.name == c.name
                            Row(
                                Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(12.dp))
                                    .background(if (active) Color.White.copy(alpha = 0.08f) else Color.Transparent)
                                    .clickable { onPick(c) }
                                    .padding(horizontal = 10.dp, vertical = 12.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column(Modifier.weight(1f)) {
                                    Text(c.name, color = Color.White, fontSize = 14.sp, fontWeight = if (active) FontWeight.SemiBold else FontWeight.Normal)
                                    if (c.sub.isNotEmpty()) {
                                        Text(c.sub, color = Color.White.copy(alpha = 0.45f), fontSize = 11.5.sp)
                                    }
                                }
                                Text(
                                    "删除",
                                    color = Color.White.copy(alpha = 0.35f),
                                    fontSize = 11.5.sp,
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(8.dp))
                                        .clickable {
                                            CityStore.removeFavorite(ctx, c)
                                            favs.removeAll { it.name == c.name }
                                            onDelete(c)
                                        }
                                        .padding(horizontal = 8.dp, vertical = 4.dp)
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
private fun BottomSheetDefaultsHandle() {
    Box(Modifier.fillMaxWidth().padding(vertical = 10.dp), contentAlignment = Alignment.Center) {
        Box(
            Modifier
                .width(34.dp)
                .height(4.dp)
                .clip(RoundedCornerShape(2.dp))
                .background(Color.White.copy(alpha = 0.28f))
        )
    }
}
