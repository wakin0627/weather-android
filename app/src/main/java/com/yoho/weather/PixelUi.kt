package com.yoho.weather

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/* ---------------- 像素风基础件 ---------------- */

val PixelFont = FontFamily(Font(R.font.ark_pixel))

/**
 * 像素风主题：白天走明亮色（浅天空 + 浅面板 + 深墨字），夜里换成深蓝但仍保留对比，
 * 避免整屏黑压压。
 */
object Px {
    var panel = Color(0xF7F3F8FC)
    var panelDeep = Color(0xFFE8F0F7)
    var border = Color(0xFF9DB8CC)
    var borderDim = Color(0x809DB8CC)
    var ink = Color(0xFF16222E)
    var dim = Color(0xFF4E6579)
    var accent = Color(0xFFD9772F)
    var blue = Color(0xFF2E7FB0)

    fun apply(isDay: Boolean) {
        if (isDay) {
            panel = Color(0xF7F3F8FC); panelDeep = Color(0xFFE8F0F7)
            border = Color(0xFF9DB8CC); borderDim = Color(0x809DB8CC)
            ink = Color(0xFF16222E); dim = Color(0xFF33475C)
            accent = Color(0xFFD9772F); blue = Color(0xFF2E7FB0)
        } else {
            panel = Color(0xF21B2A3C); panelDeep = Color(0xFF16222F)
            border = Color(0xFF3E5C77); borderDim = Color(0x803E5C77)
            ink = Color(0xFFE9F2FA); dim = Color(0xFF9DB3C7)
            accent = Color(0xFFE8A55E); blue = Color(0xFF7FB3D5)
        }
    }
}



/** 像素卡片：直角 + 1px 描边 + 内侧投影线，经典 8-bit 面板 */
@Composable
fun PixelCard(
    modifier: Modifier = Modifier,
    accent: Color = Px.borderDim,
    content: @Composable ColumnScope.() -> Unit
) {
    Column(
        modifier
            .fillMaxWidth()
            .background(Px.panel)
            .border(1.dp, accent)
            .padding(horizontal = 12.dp, vertical = 10.dp)
    ) {
        content()
    }
}

/** 章节标题：左侧加一个方块标记 */
@Composable
fun PixelLabel(text: String, trailing: String? = null, onClick: (() -> Unit)? = null) {
    Row(
        Modifier.fillMaxWidth().padding(bottom = 6.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(Modifier.size(6.dp).background(Px.accent))
        Spacer(Modifier.width(6.dp))
        Text(text, color = Px.dim, fontSize = 11.sp, fontFamily = PixelFont, fontWeight = FontWeight.Normal)
        Spacer(Modifier.weight(1f))
        if (trailing != null) {
            Text(
                trailing,
                color = Px.blue,
                fontSize = 11.sp,
                fontFamily = PixelFont,
                modifier = Modifier
                    .then(if (onClick != null) Modifier.clickable { onClick() } else Modifier)
                    .padding(2.dp)
            )
        }
    }
}

/** 像素胶囊按钮（直角版本） */
@Composable
fun PixelPill(text: String, filled: Boolean = false, onClick: () -> Unit) {
    Box(
        Modifier
            .background(if (filled) Px.border else Color(0x33101A24))
            .border(1.dp, if (filled) Px.border else Px.borderDim)
            .clickable { onClick() }
            .padding(horizontal = 8.dp, vertical = 4.dp)
    ) {
        Text(
            text,
            color = if (filled) Color(0xFF08111A) else Px.ink,
            fontSize = 11.sp,
            fontFamily = PixelFont
        )
    }
}

/** 像素分割线 */
@Composable
fun PixelDivider() {
    Canvas(Modifier.fillMaxWidth().height(2.dp).padding(vertical = 1.dp)) {
        val step = 4f
        var x = 0f
        while (x < size.width) {
            drawRect(Px.borderDim.copy(alpha = 0.5f), Offset(x, 0f), Size(step / 2, size.height))
            x += step
        }
    }
}

/** 16×16 网格绘制的像素图标：所有坐标都对齐到格心，保证硬边 */
@Composable
fun PixelGlyph(kind: String, tint: Color, size: Dp = 18.dp) {
    Canvas(Modifier.size(size)) {
        val cell = this.size.minDimension / 16f
        fun px(x: Int, y: Int, w: Int = 1, h: Int = 1, c: Color = tint) {
            drawRect(c, Offset(x * cell, y * cell), Size(w * cell, h * cell))
        }
        when (kind) {
            "sun" -> {
                px(7, 7, 2, 2, Px.accent)
                px(6, 6, 4, 1, Px.accent.copy(alpha = 0.62f))
                px(6, 9, 4, 1, Px.accent.copy(alpha = 0.62f))
                px(5, 7, 1, 2, Px.accent.copy(alpha = 0.62f))
                px(10, 7, 1, 2, Px.accent.copy(alpha = 0.62f))
                px(7, 3, 2, 1)
                px(7, 12, 2, 1)
                px(3, 7, 1, 2)
                px(12, 7, 1, 2)
                px(4, 4, 1, 1); px(11, 4, 1, 1); px(4, 11, 1, 1); px(11, 11, 1, 1)
            }
            "cloud" -> {
                px(4, 8, 8, 4, Color(0xFFDCE9F5))
                px(5, 6, 5, 2, Color(0xFFEDF6FF))
                px(6, 5, 3, 1, Color(0xFFEDF6FF))
                px(4, 8, 8, 1, Color(0xFFF7FBFF))
            }
            "rain" -> {
                px(4, 7, 8, 3, Color(0xFFDCE9F5))
                px(5, 5, 4, 2, Color(0xFFEDF6FF))
                px(5, 11, 1, 3, Px.blue)
                px(8, 11, 1, 4, Px.blue)
                px(11, 11, 1, 3, Px.blue)
            }
            "snow" -> {
                px(4, 7, 8, 3, Color(0xFFDCE9F5))
                px(6, 11, 1, 1, Color.White); px(8, 13, 1, 1, Color.White)
                px(10, 11, 1, 1, Color.White); px(7, 14, 1, 1, Color.White)
            }
            "storm" -> {
                px(4, 6, 8, 3, Color(0xFFB9C9D8))
                px(7, 9, 2, 3, Px.accent)
                px(6, 12, 2, 2, Px.accent)
                px(5, 9, 1, 2, Px.blue)
                px(11, 9, 1, 3, Px.blue)
            }
            "fog" -> {
                px(3, 5, 10, 1, Color(0xFFDCE9F5))
                px(5, 8, 9, 1, Color(0xFFDCE9F5))
                px(3, 11, 8, 1, Color(0xFFDCE9F5))
            }
            "moon" -> {
                px(6, 3, 5, 1, Color(0xFFE7F0FF))
                px(5, 4, 2, 8, Color(0xFFE7F0FF))
                px(7, 12, 4, 1, Color(0xFFE7F0FF))
                px(9, 5, 2, 2, Color(0xFF0A121A))
                px(8, 9, 2, 2, Color(0xFF0A121A))
            }
            "jacket" -> {
                px(5, 3, 6, 2); px(4, 5, 2, 8); px(10, 5, 2, 8)
                px(6, 5, 4, 1); px(7, 5, 2, 7)
            }
            "umbrella" -> {
                px(3, 6, 10, 1); px(4, 5, 8, 1); px(5, 4, 6, 1)
                px(7, 7, 2, 6); px(9, 13, 2, 1)
            }
            "car" -> {
                px(2, 9, 12, 3); px(4, 6, 8, 3); px(3, 12, 2, 2); px(11, 12, 2, 2)
                px(5, 7, 2, 2, Color(0xFF9FD8FF)); px(9, 7, 2, 2, Color(0xFF9FD8FF))
            }
            "dry" -> {
                px(2, 4, 12, 1); px(5, 5, 4, 7); px(5, 5, 1, 7); px(8, 5, 1, 7)
            }
            "run" -> {
                px(8, 3, 2, 2); px(6, 6, 3, 1); px(5, 7, 2, 4); px(7, 9, 3, 1)
                px(4, 11, 2, 2); px(9, 10, 2, 4)
            }
            "hike" -> {
                px(2, 12, 12, 2); px(5, 7, 2, 5); px(9, 5, 2, 7); px(7, 9, 5, 2)
                px(11, 3, 2, 3)
            }
            "uv", "air" -> {
                px(7, 6, 2, 4); px(5, 7, 6, 2); px(7, 2, 2, 2); px(7, 12, 2, 2)
                px(2, 7, 2, 2); px(12, 7, 2, 2)
            }
            "hum" -> {
                px(7, 2, 2, 3); px(6, 5, 4, 3); px(5, 8, 6, 4); px(6, 12, 4, 2)
            }
            "wind" -> {
                px(2, 5, 9, 1); px(2, 8, 12, 1); px(2, 11, 7, 1)
            }
            "press" -> {
                px(5, 3, 6, 1); px(4, 4, 8, 8); px(5, 12, 6, 1); px(7, 8, 1, 3); px(8, 7, 2, 1)
            }
            "vis" -> {
                px(2, 8, 12, 1); px(4, 6, 8, 1); px(4, 10, 8, 1); px(7, 7, 2, 2)
            }
            "sunset" -> {
                px(7, 6, 2, 3, Px.accent); px(5, 7, 6, 2, Color(0xFFFFB36B))
                px(2, 10, 12, 1)
                px(7, 12, 2, 1); px(6, 13, 4, 1)
            }
            "sunrise" -> {
                px(7, 7, 2, 3, Px.accent); px(5, 8, 6, 2, Color(0xFFFFB36B))
                px(2, 11, 12, 1)
                px(7, 3, 2, 1); px(6, 4, 4, 1)
            }
            else -> {
                px(6, 6, 4, 4)
            }
        }
    }
}

/** 天气码 -> 像素图标类型 */
fun pixelIconOf(group: String, isDay: Boolean): String = when (group) {
    "clear" -> if (isDay) "sun" else "moon"
    "few" -> if (isDay) "sun" else "moon"
    "rain" -> "rain"
    "snow" -> "snow"
    "storm" -> "storm"
    "fog" -> "fog"
    else -> "cloud"
}


/** 像素风底部导航：直角格子 + 选中反色 */
@Composable
fun PixelBar(tab: Int, onTab: (Int) -> Unit) {
    val items = listOf("天气" to "sun", "地图" to "vis", "我的" to "air")
    Column(
        Modifier
            .fillMaxWidth()
            .background(Px.panel)
            .border(1.dp, Px.border)
            .navigationBarsPadding()
    ) {
        Row(Modifier.fillMaxWidth().height(46.dp)) {
            items.forEachIndexed { i, (label, glyph) ->
                val on = tab == i
                Column(
                    Modifier
                        .weight(1f)
                        .fillMaxHeight()
                        .background(if (on) Px.panelDeep else Color.Transparent)
                        .clickable { onTab(i) },
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    PixelGlyph(glyph, if (on) Px.accent else Px.dim, 15.dp)
                    Spacer(Modifier.height(3.dp))
                    Text(
                        label,
                        color = if (on) Px.accent else Px.dim,
                        fontSize = 9.sp,
                        fontFamily = PixelFont
                    )
                }
            }
        }
    }
}


/** 像素天空配色：低饱和、三阶以内，避免多色打架 */
fun pxSkyColors(group: String, isDay: Boolean): List<Color> {
    val day = when (group) {
        "clear" -> listOf(0xFF5EA7D6, 0xFF8CC4E6, 0xFFBFE0F4)
        "few" -> listOf(0xFF6BAFD8, 0xFF97C8E6, 0xFFC9E3F4)
        "cloudy" -> listOf(0xFF7EA6BF, 0xFFA3C0D2, 0xFFD2E1EB)
        "rain" -> listOf(0xFF6E8FA6, 0xFF8FAEBF, 0xFFC0D3DF)
        "snow" -> listOf(0xFF87A6BC, 0xFFAFC6D6, 0xFFE2EDF4)
        "storm" -> listOf(0xFF5C7386, 0xFF7D94A5, 0xFFB4C5D0)
        else -> listOf(0xFF7EA6BF, 0xFFA3C0D2, 0xFFD2E1EB)
    }
    val night = when (group) {
        "clear" -> listOf(0xFF16273D, 0xFF20374F, 0xFF334F6B)
        "rain" -> listOf(0xFF141F2C, 0xFF1E2C3C, 0xFF2E4054)
        "storm" -> listOf(0xFF101822, 0xFF19222E, 0xFF2A3543)
        else -> listOf(0xFF152335, 0xFF1F3044, 0xFF31465C)
    }
    return (if (isDay) day else night).map { Color(it) }
}
