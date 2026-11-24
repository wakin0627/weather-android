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
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/* ---------------------------------------------------------------
   设计系统取自 awesome-design-md / linear.app
   canvas #010102 · surface #0F1011 · hairline #23252A
   ink #F7F8F8 · ink-subtle #8A8F98 · accent #5E6AD2
   radius 8/12/16 · spacing 4/8/12/16/24
   --------------------------------------------------------------- */

object Ln {
    val canvas = Color(0xFF010102)
    val surface1 = Color(0xFF0F1011)
    val surface2 = Color(0xFF141516)
    val surface3 = Color(0xFF18191A)
    val hairline = Color(0xFF23252A)
    val hairlineStrong = Color(0xFF34343A)
    val ink = Color(0xFFF7F8F8)
    val inkMuted = Color(0xFFD0D6E0)
    val inkSubtle = Color(0xFF8A8F98)
    val inkTertiary = Color(0xFF62666D)
    val accent = Color(0xFF5E6AD2)
    val accentSoft = Color(0x335E6AD2)
    val success = Color(0xFF27A644)
}

/** Linear 式面板：近黑表面 + 一条 hairline，没有阴影没有模糊 */
@Composable
fun LnCard(
    modifier: Modifier = Modifier,
    padding: Dp = 16.dp,
    content: @Composable ColumnScope.() -> Unit
) {
    Column(
        modifier
            .fillMaxWidth()
            .background(Ln.surface1, RoundedCornerShape(12.dp))
            .border(1.dp, Ln.hairline, RoundedCornerShape(12.dp))
            .padding(padding),
        content = content
    )
}

/** 区块标题：13px / 0.4 字距 / ink-subtle，右侧可选动作 */
@Composable
fun LnLabel(text: String, action: String? = null, onAction: (() -> Unit)? = null) {
    Row(
        Modifier.fillMaxWidth().padding(bottom = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text.uppercase().let { if (it == text) text else text },
            color = Ln.inkSubtle,
            fontSize = 13.sp,
            fontWeight = FontWeight.Medium,
            letterSpacing = 0.4.sp
        )
        Spacer(Modifier.weight(1f))
        if (action != null) {
            Text(
                action,
                color = if (onAction != null) Ln.inkMuted else Ln.inkTertiary,
                fontSize = 13.sp,
                modifier = Modifier
                    .then(if (onAction != null) Modifier.clickable { onAction() } else Modifier)
                    .padding(vertical = 2.dp, horizontal = 2.dp)
            )
        }
    }
}

/** 次级按钮：surface + hairline */
@Composable
fun LnButton(text: String, primary: Boolean = false, onClick: () -> Unit) {
    Box(
        Modifier
            .background(
                if (primary) Ln.accent else Ln.surface1,
                RoundedCornerShape(8.dp)
            )
            .border(
                1.dp,
                if (primary) Color.Transparent else Ln.hairline,
                RoundedCornerShape(8.dp)
            )
            .clickable { onClick() }
            .padding(horizontal = 14.dp, vertical = 8.dp)
    ) {
        Text(
            text,
            color = if (primary) Color.White else Ln.inkMuted,
            fontSize = 14.sp,
            fontWeight = FontWeight.Medium
        )
    }
}

/** 顶部的极淡光晕，代替插画式天空，克制但不像纯黑那么死 */
@Composable
fun LnBackdrop(isNight: Boolean) {
    val glow = if (isNight) Color(0xFF2A2F5E) else Color(0xFF3B4470)
    Box(
        Modifier
            .fillMaxSize()
            .background(Ln.canvas)
            .background(
                Brush.radialGradient(
                    colors = listOf(glow.copy(alpha = if (isNight) 0.55f else 0.42f), Color.Transparent),
                    radius = 1400f,
                    center = androidx.compose.ui.geometry.Offset(1200f, -200f)
                )
            )
    )
}

/** 底部导航：线性风格，选中态用 accent 小条 */
@Composable
fun LnBar(tab: Int, onTab: (Int) -> Unit) {
    val items = listOf("天气", "地图", "我的")
    Column(
        Modifier
            .fillMaxWidth()
            .background(Ln.canvas.copy(alpha = 0.94f))
            .navigationBarsPadding()
    ) {
        Box(Modifier.fillMaxWidth().height(1.dp).background(Ln.hairline))
        Row(Modifier.fillMaxWidth().height(52.dp)) {
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
                    Box(
                        Modifier
                            .height(2.dp)
                            .width(if (on) 18.dp else 0.dp)
                            .background(if (on) Ln.accent else Color.Transparent, RoundedCornerShape(1.dp))
                    )
                    Spacer(Modifier.height(6.dp))
                    Text(
                        label,
                        color = if (on) Ln.ink else Ln.inkSubtle,
                        fontSize = 13.sp,
                        fontWeight = if (on) FontWeight.Medium else FontWeight.Normal
                    )
                }
            }
        }
    }
}
