package com.yoho.weather

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
fun MeScreen(
    cityCount: Int,
    days: Int,
    requests: Int,
    nickname: String,
    onNickname: () -> Unit,
    onCheckUpdate: () -> Unit,
    onToast: (String) -> Unit
) {
    Box(
        Modifier
            .fillMaxSize()
            .background(Brush.verticalGradient(listOf(Color(0xFF0C1622), Color(0xFF17222E), Color(0xFF1E2A34))))
    ) {
        Column(
            Modifier
                .fillMaxSize()
                .padding(horizontal = 14.dp)
                .padding(top = 44.dp)
        ) {
            Row(Modifier.fillMaxWidth().padding(horizontal = 2.dp, vertical = 8.dp), verticalAlignment = Alignment.CenterVertically) {
                Box(
                    Modifier
                        .size(58.dp)
                        .clip(RoundedCornerShape(999.dp))
                        .background(Brush.linearGradient(listOf(Color.White.copy(alpha = 0.34f), Color.White.copy(alpha = 0.14f))))
                        .border(0.6.dp, Color.White.copy(alpha = 0.3f), RoundedCornerShape(999.dp)),
                    contentAlignment = Alignment.Center
                ) {
                    Text(nickname.take(1), color = Color.White, fontSize = 23.sp)
                }
                Spacer(Modifier.width(14.dp))
                Column {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(nickname, color = Color.White, fontSize = 19.sp, fontWeight = FontWeight.SemiBold)
                        Spacer(Modifier.width(8.dp))
                        Box(
                            Modifier
                                .clip(RoundedCornerShape(999.dp))
                                .background(Color.White.copy(alpha = 0.14f))
                                .clickable { onNickname() }
                                .padding(horizontal = 8.dp, vertical = 2.dp)
                        ) {
                            Text("修改", color = Color.White.copy(alpha = 0.8f), fontSize = 10.sp)
                        }
                    }
                    Text(
                        "本地账号 · 演示版",
                        color = Color.White.copy(alpha = 0.55f),
                        fontSize = 11.5.sp,
                        modifier = Modifier.padding(top = 4.dp)
                    )
                }
            }

            Row(Modifier.fillMaxWidth().padding(top = 14.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Stat("$cityCount", "收藏城市", Modifier.weight(1f))
                Stat("$days", "使用天数", Modifier.weight(1f))
                Stat("$requests", "接口请求", Modifier.weight(1f))
            }

            Spacer(Modifier.height(14.dp))

            Column(
                Modifier
                    .clip(RoundedCornerShape(Radius))
                    .background(Color.White.copy(alpha = 0.14f))
                    .border(0.6.dp, Color.White.copy(alpha = 0.18f), RoundedCornerShape(Radius))
            ) {
                Item("天气通知") { onToast("通知推送将在正式版开放") }
                Divider()
                Item("桌面组件") { onToast("桌面组件将在正式版开放") }
                Divider()
                Item("数据与隐私") { onToast("定位仅用于取当前城市天气，不上传轨迹") }
            }

            Spacer(Modifier.height(8.dp))

            Column(
                Modifier
                    .clip(RoundedCornerShape(Radius))
                    .background(Color.White.copy(alpha = 0.14f))
                    .border(0.6.dp, Color.White.copy(alpha = 0.18f), RoundedCornerShape(Radius))
            ) {
                Row(
                    Modifier.fillMaxWidth().clickable { onCheckUpdate() }.padding(horizontal = 14.dp, vertical = 13.dp),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text("版本", color = Color.White, fontSize = 14.sp)
                    Text("v${Api.APP_VERSION} · 检查更新 ›", color = Color.White.copy(alpha = 0.55f), fontSize = 12.5.sp)
                }
                Divider()
                Row(
                    Modifier.fillMaxWidth().padding(horizontal = 14.dp, vertical = 13.dp),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text("数据来源", color = Color.White, fontSize = 14.sp)
                    Text("Open-Meteo · 高德", color = Color.White.copy(alpha = 0.55f), fontSize = 12.5.sp)
                }
            }

            Text(
                "天气数据为 1–2km 网格模式预报，地址为街道/社区级，均仅供参考。",
                color = Color.White.copy(alpha = 0.4f),
                fontSize = 10.5.sp,
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth().padding(top = 14.dp, start = 10.dp, end = 10.dp)
            )
        }
    }
}

@Composable
private fun Stat(value: String, key: String, modifier: Modifier = Modifier) {
    Column(
        modifier
            .clip(RoundedCornerShape(17.dp))
            .background(Color.White.copy(alpha = 0.14f))
            .border(0.6.dp, Color.White.copy(alpha = 0.18f), RoundedCornerShape(17.dp))
            .padding(horizontal = 12.dp, vertical = 11.dp)
    ) {
        Text(value, color = Color.White, fontSize = 22.sp, fontWeight = FontWeight.Light)
        Text(key, color = Color.White.copy(alpha = 0.6f), fontSize = 10.5.sp, modifier = Modifier.padding(top = 3.dp))
    }
}

@Composable
private fun Item(text: String, onClick: () -> Unit) {
    Row(
        Modifier.fillMaxWidth().clickable { onClick() }.padding(horizontal = 14.dp, vertical = 13.dp),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(text, color = Color.White, fontSize = 14.sp)
        Text("›", color = Color.White.copy(alpha = 0.35f), fontSize = 16.sp)
    }
}

@Composable
private fun Divider() {
    Box(Modifier.fillMaxWidth().height(0.6.dp).background(Color.White.copy(alpha = 0.13f)))
}
