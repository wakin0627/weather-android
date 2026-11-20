package com.yoho.weather

import android.graphics.Bitmap
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.FilterQuality
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext
import org.json.JSONObject
import org.osmdroid.tileprovider.tilesource.OnlineTileSourceBase
import org.osmdroid.util.GeoPoint
import org.osmdroid.util.MapTileIndex
import org.osmdroid.views.MapView
import java.net.HttpURLConnection
import java.net.URL
import kotlin.math.cos
import kotlin.math.sin
import kotlin.random.Random

/* ---------------- 数据 ---------------- */

data class WindFrame(
    val t: String,
    val u: Array<DoubleArray>,
    val v: Array<DoubleArray>,
    val speed: Array<DoubleArray>,
    val avg: Double,
    val max: Double,
    val maxGust: Double
)

data class WindData(
    val n: Int,
    val west: Double,
    val east: Double,
    val south: Double,
    val north: Double,
    val frames: List<WindFrame>
)

data class ScalarGrid(
    val kind: String,
    val n: Int,
    val unit: String,
    val values: Array<DoubleArray>,
    val min: Double,
    val max: Double
)

private fun httpJson(url: String): JSONObject? = try {
    val conn = URL(url).openConnection() as HttpURLConnection
    conn.connectTimeout = 8000
    conn.readTimeout = 15000
    val text = conn.inputStream.bufferedReader().use { it.readText() }
    conn.disconnect()
    JSONObject(text)
} catch (e: Exception) {
    null
}

private fun dgrid(o: JSONObject, key: String): Array<DoubleArray> {
    val arr = o.getJSONArray(key)
    return Array(arr.length()) { j ->
        val row = arr.getJSONArray(j)
        DoubleArray(row.length()) { i -> row.getDouble(i) }
    }
}

suspend fun fetchWind(west: Double, east: Double, south: Double, north: Double, n: Int = 6): WindData? =
    withContext(Dispatchers.IO) {
        val url = "${Api.BASE_URL}/api/windgrid?west=$west&east=$east&south=$south&north=$north&n=$n&frames=5&step=3"
        val o = httpJson(url) ?: return@withContext null
        if (!o.optBoolean("ok", false)) return@withContext null
        val frames = mutableListOf<WindFrame>()
        val arr = o.optJSONArray("frames")
        if (arr != null) {
            for (i in 0 until arr.length()) {
                val f = arr.getJSONObject(i)
                frames.add(
                    WindFrame(
                        t = f.optString("t"),
                        u = dgrid(f, "u"), v = dgrid(f, "v"), speed = dgrid(f, "speed"),
                        avg = f.optDouble("avg", 0.0),
                        max = f.optDouble("max", 0.0),
                        maxGust = f.optDouble("max_gust", 0.0)
                    )
                )
            }
        }
        if (frames.isEmpty()) return@withContext null
        WindData(
            n = o.optInt("n", n),
            west = west, east = east, south = south, north = north,
            frames = frames
        )
    }

suspend fun fetchScalar(kind: String, west: Double, east: Double, south: Double, north: Double, n: Int = 7): ScalarGrid? =
    withContext(Dispatchers.IO) {
        val url = "${Api.BASE_URL}/api/scalargrid?kind=$kind&west=$west&east=$east&south=$south&north=$north&n=$n"
        val o = httpJson(url) ?: return@withContext null
        if (!o.optBoolean("ok", false)) return@withContext null
        ScalarGrid(
            kind = o.optString("kind", kind),
            n = o.optInt("n", n),
            unit = o.optString("unit"),
            values = dgrid(o, "values"),
            min = o.optDouble("min", 0.0),
            max = o.optDouble("max", 0.0)
        )
    }

/* ---------------- 高德瓦片（GCJ-02） ---------------- */

private class AmapSource : OnlineTileSourceBase("AMap", 3, 18, 256, "", arrayOf("")) {
    override fun getTileURLString(pMapTileIndex: Long): String {
        val z = MapTileIndex.getZoom(pMapTileIndex)
        val x = MapTileIndex.getX(pMapTileIndex)
        val y = MapTileIndex.getY(pMapTileIndex)
        val s = (x + y) % 4 + 1
        return "https://webrd0$s.is.autonavi.com/appmaptile?lang=zh_cn&size=1&scale=1&style=7&x=$x&y=$y&z=$z"
    }
}

/* ---------------- WGS84 -> GCJ02 ---------------- */

fun wgs2gcj(lng: Double, lat: Double): Pair<Double, Double> {
    if (lng < 73.66 || lng > 135.05 || lat < 3.86 || lat > 53.55) return lng to lat
    val a = 6378245.0
    val ee = 0.00669342162296594323
    fun tLat(x: Double, y: Double): Double {
        var r = -100 + 2 * x + 3 * y + 0.2 * y * y + 0.1 * x * y + 0.2 * Math.sqrt(Math.abs(x))
        r += (20 * sin(6 * x * Math.PI) + 20 * sin(2 * x * Math.PI)) * 2 / 3
        r += (20 * sin(y * Math.PI) + 40 * sin(y / 3 * Math.PI)) * 2 / 3
        r += (160 * sin(y / 12 * Math.PI) + 320 * sin(y * Math.PI / 30)) * 2 / 3
        return r
    }
    fun tLng(x: Double, y: Double): Double {
        var r = 300 + x + 2 * y + 0.1 * x * x + 0.1 * x * y + 0.1 * Math.sqrt(Math.abs(x))
        r += (20 * sin(6 * x * Math.PI) + 20 * sin(2 * x * Math.PI)) * 2 / 3
        r += (20 * sin(x * Math.PI) + 40 * sin(x / 3 * Math.PI)) * 2 / 3
        r += (150 * sin(x / 12 * Math.PI) + 300 * sin(x / 30 * Math.PI)) * 2 / 3
        return r
    }
    var dLat = tLat(lng - 105, lat - 35)
    var dLng = tLng(lng - 105, lat - 35)
    val rad = lat / 180 * Math.PI
    val magic = 1 - ee * sin(rad) * sin(rad)
    val sq = Math.sqrt(magic)
    dLat = (dLat * 180) / ((a * (1 - ee)) / (magic * sq) * Math.PI)
    dLng = (dLng * 180) / (a / sq * cos(rad) * Math.PI)
    return (lng + dLng) to (lat + dLat)
}

/* ---------------- 粒子 ---------------- */

private class Particle {
    var x = 0f
    var y = 0f
    var life = 0f
    val hx = FloatArray(9)
    val hy = FloatArray(9)
    var hi = 0
    var filled = 0
}

/* ---------------- 页面 ---------------- */

@Composable
fun MapScreen(lng: Double, lat: Double) {
    val (gLng, gLat) = remember(lng, lat) { wgs2gcj(lng, lat) }
    var mapView by remember { mutableStateOf<MapView?>(null) }
    var box by remember { mutableStateOf<List<Double>?>(null) }   // west east south north
    var wind by remember { mutableStateOf<WindData?>(null) }
    var frameIdx by remember { mutableIntStateOf(0) }
    var layer by remember { mutableStateOf("wind") }
    var scalar by remember { mutableStateOf<ScalarGrid?>(null) }
    var playing by remember { mutableStateOf(false) }
    var tick by remember { mutableLongStateOf(0L) }
    var canvasSize by remember { mutableStateOf(IntSize.Zero) }

    // 相机变化后重新取网格
    LaunchedEffect(box, layer) {
        val b = box ?: return@LaunchedEffect
        if (layer == "wind") {
            wind = fetchWind(b[0], b[1], b[2], b[3])
            frameIdx = 0
        } else {
            scalar = fetchScalar(layer, b[0], b[1], b[2], b[3])
        }
    }

    // 播放
    LaunchedEffect(playing, wind) {
        if (!playing) return@LaunchedEffect
        while (true) {
            delay(900)
            val n = wind?.frames?.size ?: 0
            if (n > 0) frameIdx = (frameIdx + 1) % n
        }
    }

    val parts = remember { MutableList(320) { Particle() } }
    val rnd = remember { Random(11) }
    fun spawn(p: Particle) {
        p.x = rnd.nextFloat(); p.y = rnd.nextFloat()
        p.life = 0.7f + rnd.nextFloat() * 1.5f
        p.hi = 0; p.filled = 0
    }

    LaunchedEffect(Unit) {
        parts.forEach { spawn(it) }
        var last = 0L
        while (true) {
            withFrameNanos { now ->
                val dt = if (last == 0L) 0.016f else ((now - last) / 1_000_000_000.0).toFloat().coerceAtMost(0.05f)
                last = now
                val w = wind
                if (layer == "wind" && w != null) {
                    val f = w.frames.getOrNull(frameIdx) ?: w.frames[0]
                    for (p in parts) {
                        val gi = (p.x * (w.n - 1)).toInt().coerceIn(0, w.n - 2)
                        val gj = (p.y * (w.n - 1)).toInt().coerceIn(0, w.n - 2)
                        p.hx[p.hi] = p.x; p.hy[p.hi] = p.y
                        p.hi = (p.hi + 1) % p.hx.size
                        if (p.filled < p.hx.size) p.filled++
                        p.x += (f.u[gj][gi] * 0.05).toFloat() * dt
                        p.y -= (f.v[gj][gi] * 0.05).toFloat() * dt
                        p.life -= dt
                        if (p.life <= 0f || p.x < 0f || p.x > 1f || p.y < 0f || p.y > 1f) spawn(p)
                    }
                }
                tick++
            }
        }
    }

    fun refreshBox() {
        val m = mapView ?: return
        try {
            val bb = m.boundingBox
            box = listOf(bb.lonWest, bb.lonEast, bb.latSouth, bb.latNorth)
        } catch (e: Exception) {
        }
    }

    Box(Modifier.fillMaxSize().background(Color(0xFF0A1018))) {
        AndroidView(
            modifier = Modifier.fillMaxSize().onSizeChanged { canvasSize = it },
            factory = { ctx ->
                MapView(ctx).apply {
                    setTileSource(AmapSource())
                    setMultiTouchControls(true)
                    setUseDataConnection(true)
                    zoomController.setVisibility(
                        org.osmdroid.views.CustomZoomButtonsController.Visibility.NEVER
                    )
                    controller.setZoom(8.0)
                    controller.setCenter(GeoPoint(gLat, gLng))
                    addMapListener(object : org.osmdroid.events.MapListener {
                        override fun onScroll(event: org.osmdroid.events.ScrollEvent?): Boolean {
                            refreshBox()
                            return false
                        }

                        override fun onZoom(event: org.osmdroid.events.ZoomEvent?): Boolean {
                            refreshBox()
                            return false
                        }
                    })
                    post { refreshBox() }
                    mapView = this
                }
            }
        )

        // 压暗底图
        Box(Modifier.fillMaxSize().background(Color(0x9C060B12)))

        val wd = wind
        val cur = wd?.frames?.getOrNull(frameIdx)
        val t = tick
        Canvas(Modifier.fillMaxSize()) {
            val b = box ?: return@Canvas
            val w = size.width
            val h = size.height
            val west = b[0]; val east = b[1]; val south = b[2]; val north = b[3]
            if (east <= west || north <= south) return@Canvas

            // 当前位置
            val px = ((gLng - west) / (east - west) * w).toFloat()
            val py = ((north - gLat) / (north - south) * h).toFloat()
            if (px in 0f..w && py in 0f..h) {
                drawCircle(Color.White.copy(alpha = 0.35f), radius = 13f, center = Offset(px, py))
                drawCircle(Color.White, radius = 8f, center = Offset(px, py))
                drawCircle(Color(0xFF58D0FF), radius = 5.5f, center = Offset(px, py))
            }

            if (layer == "wind" && wd != null && cur != null) {
                val n = wd.n
                for (p in parts) {
                    if (p.filled < 3) continue
                    var idx = (p.hi - 1 + p.hx.size) % p.hx.size
                    var prevX = p.hx[idx] * w
                    var prevY = p.hy[idx] * h
                    val gi = (p.x * (n - 1)).toInt().coerceIn(0, n - 1)
                    val gj = (p.y * (n - 1)).toInt().coerceIn(0, n - 1)
                    val col = windColor(cur.speed[gj][gi])
                    for (step in 1 until p.filled) {
                        idx = (idx - 1 + p.hx.size) % p.hx.size
                        val cx = p.hx[idx] * w
                        val cy = p.hy[idx] * h
                        drawLine(
                            color = col.copy(alpha = (1f - step.toFloat() / p.filled) * 0.8f),
                            start = Offset(cx, cy), end = Offset(prevX, prevY),
                            strokeWidth = 1.9f, cap = StrokeCap.Round
                        )
                        prevX = cx; prevY = cy
                    }
                }
            } else if (layer != "wind" && scalar != null) {
                val sg: ScalarGrid = scalar ?: return@Canvas
                val bw = (w / 6).toInt().coerceAtLeast(24)
                val bh = (h / 6).toInt().coerceAtLeast(24)
                val bmp = buildScalarBitmap(sg, bw, bh)
                bmp?.let {
                    drawImage(
                        image = it,
                        dstSize = IntSize(w.toInt(), h.toInt()),
                        filterQuality = FilterQuality.Medium
                    )
                }
            }
            t.hashCode()
        }

        // 顶部：图层切换
        Column(Modifier.fillMaxWidth().padding(start = 16.dp, end = 16.dp, top = 44.dp)) {
            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                LayerChip("风场", layer == "wind") { layer = "wind" }
                LayerChip("温度", layer == "temp") { layer = "temp" }
                LayerChip("云量", layer == "cloud") { layer = "cloud" }
            }
            Spacer(Modifier.height(8.dp))
            Column(
                Modifier
                    .clip(RoundedCornerShape(Radius))
                    .background(Color(0xB3141A22))
                    .border(0.6.dp, Color.White.copy(alpha = 0.18f), RoundedCornerShape(Radius))
                    .padding(horizontal = 14.dp, vertical = 10.dp)
            ) {
                Text(
                    when {
                        layer == "wind" && cur != null ->
                            "平均 ${cur.avg} m/s · 阵风 ${cur.maxGust} m/s"
                        layer == "temp" && scalar != null ->
                            "气温 ${scalar!!.min.toInt()}° ~ ${scalar!!.max.toInt()}°"
                        layer == "cloud" && scalar != null ->
                            "云量 ${scalar!!.min.toInt()}% ~ ${scalar!!.max.toInt()}%"
                        else -> "加载中…"
                    },
                    color = Color.White, fontSize = 14.sp, fontWeight = FontWeight.Medium
                )
                Text(
                    when (layer) {
                        "wind" -> if (cur != null) "10m 风场 · ${cur.t.takeLast(5)}" else "正在取风场数据"
                        "temp" -> "近地面 2m 气温 · 1–2km 网格"
                        else -> "总云量 · 1–2km 网格"
                    },
                    color = Color.White.copy(alpha = 0.6f), fontSize = 11.5.sp,
                    modifier = Modifier.padding(top = 2.dp)
                )
            }
        }

        // 底部：时间轴（仅风场）
        Column(
            Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
                .padding(horizontal = 14.dp)
                .padding(bottom = 86.dp)
        ) {
            val wdB = wind
            if (layer == "wind" && wdB != null) {
                val frames = wdB.frames
                Column(
                    Modifier
                        .clip(RoundedCornerShape(Radius))
                        .background(Color(0xB3141A22))
                        .border(0.6.dp, Color.White.copy(alpha = 0.18f), RoundedCornerShape(Radius))
                        .padding(horizontal = 14.dp, vertical = 10.dp)
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            Modifier
                                .size(32.dp)
                                .clip(RoundedCornerShape(999.dp))
                                .background(Color.White.copy(alpha = 0.16f))
                                .clickable { playing = !playing },
                            contentAlignment = Alignment.Center
                        ) {
                            Text(if (playing) "❚❚" else "▶", color = Color.White, fontSize = 12.sp)
                        }
                        Spacer(Modifier.width(12.dp))
                        Column(Modifier.weight(1f)) {
                            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                frames.forEachIndexed { i, f ->
                                    Box(
                                        Modifier
                                            .weight(1f)
                                            .height(26.dp)
                                            .clip(RoundedCornerShape(8.dp))
                                            .background(
                                                if (i == frameIdx) Color.White.copy(alpha = 0.9f)
                                                else Color.White.copy(alpha = 0.12f)
                                            )
                                            .clickable { frameIdx = i; playing = false },
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Text(
                                            f.t.takeLast(5),
                                            color = if (i == frameIdx) Color(0xFF10161F) else Color.White.copy(alpha = 0.75f),
                                            fontSize = 10.5.sp,
                                            fontWeight = FontWeight.SemiBold
                                        )
                                    }
                                }
                            }
                            Text(
                                "未来 12 小时逐 3 小时风场",
                                color = Color.White.copy(alpha = 0.45f), fontSize = 10.sp,
                                modifier = Modifier.padding(top = 5.dp)
                            )
                        }
                    }
                }
            } else {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text("小", color = Color.White.copy(alpha = 0.6f), fontSize = 10.sp)
                    Box(
                        Modifier.weight(1f).height(5.dp).padding(horizontal = 6.dp)
                            .background(
                                Brush.horizontalGradient(
                                    if (layer == "temp")
                                        listOf(Color(0xFF2C7BE5), Color(0xFF4AA8FF), Color(0xFF59E0B0), Color(0xFFFFD166), Color(0xFFFF8A5C))
                                    else
                                        listOf(Color(0x00FFFFFF), Color(0x80FFFFFF), Color(0xFFFFFFFF))
                                ),
                                RoundedCornerShape(3.dp)
                            )
                    )
                    Text("大", color = Color.White.copy(alpha = 0.6f), fontSize = 10.sp)
                }
            }
        }
    }
}


@Composable
private fun LayerChip(text: String, on: Boolean, onClick: () -> Unit) {
    Box(
        Modifier
            .clip(RoundedCornerShape(999.dp))
            .background(if (on) Color.White.copy(alpha = 0.9f) else Color(0x990F141B))
            .border(0.6.dp, Color.White.copy(alpha = if (on) 0f else 0.22f), RoundedCornerShape(999.dp))
            .clickable { onClick() }
            .padding(horizontal = 14.dp, vertical = 6.dp)
    ) {
        Text(
            text,
            color = if (on) Color(0xFF10161F) else Color.White.copy(alpha = 0.8f),
            fontSize = 12.sp, fontWeight = FontWeight.SemiBold
        )
    }
}

private fun windColor(s: Double): Color {
    val t = (s / 15.0).coerceIn(0.0, 1.0)
    return when {
        t < 0.25 -> lerpColor(Color(0xFF4AA8FF), Color(0xFF59E0B0), t / 0.25)
        t < 0.5 -> lerpColor(Color(0xFF59E0B0), Color(0xFFFFD166), (t - 0.25) / 0.25)
        t < 0.75 -> lerpColor(Color(0xFFFFD166), Color(0xFFFF8A5C), (t - 0.5) / 0.25)
        else -> lerpColor(Color(0xFFFF8A5C), Color(0xFFFF4D6D), (t - 0.75) / 0.25)
    }
}

private fun lerpColor(a: Color, b: Color, t: Double): Color {
    val f = t.toFloat().coerceIn(0f, 1f)
    return Color(
        red = a.red + (b.red - a.red) * f,
        green = a.green + (b.green - a.green) * f,
        blue = a.blue + (b.blue - a.blue) * f,
        alpha = 1f
    )
}

private fun buildScalarBitmap(g: ScalarGrid, bw: Int, bh: Int): ImageBitmap? {
    return try {
        val bmp = Bitmap.createBitmap(bw, bh, Bitmap.Config.ARGB_8888)
        val px = IntArray(bw * bh)
        val n = g.n
        val lo = g.min
        val hi = g.max
        val span = (hi - lo).coerceAtLeast(0.001)
        for (y in 0 until bh) {
            val gy = y.toFloat() / (bh - 1) * (n - 1)
            val j = gy.toInt().coerceIn(0, n - 2)
            val fy = gy - j
            for (x in 0 until bw) {
                val gx = x.toFloat() / (bw - 1) * (n - 1)
                val i = gx.toInt().coerceIn(0, n - 2)
                val fx = gx - i
                val v = g.values[j][i] * (1 - fx) * (1 - fy) +
                        g.values[j][i + 1] * fx * (1 - fy) +
                        g.values[j + 1][i] * (1 - fx) * fy +
                        g.values[j + 1][i + 1] * fx * fy
                val t = ((v - lo) / span).coerceIn(0.0, 1.0)
                val c = if (g.kind == "cloud") {
                    Color(1f, 1f, 1f, (t * 0.6).toFloat())
                } else {
                    tempColor(t)
                }
                px[y * bw + x] = android.graphics.Color.argb(
                    (c.alpha * 255).toInt(), (c.red * 255).toInt(),
                    (c.green * 255).toInt(), (c.blue * 255).toInt()
                )
            }
        }
        bmp.setPixels(px, 0, bw, 0, 0, bw, bh)
        bmp.asImageBitmap()
    } catch (e: Exception) {
        null
    }
}

private fun tempColor(t: Double): Color = when {
    t < 0.25 -> lerpColor(Color(0xFF2C7BE5), Color(0xFF4AA8FF), t / 0.25)
    t < 0.5 -> lerpColor(Color(0xFF4AA8FF), Color(0xFF59E0B0), (t - 0.25) / 0.25)
    t < 0.75 -> lerpColor(Color(0xFF59E0B0), Color(0xFFFFD166), (t - 0.5) / 0.25)
    else -> lerpColor(Color(0xFFFFD166), Color(0xFFFF8A5C), (t - 0.75) / 0.25)
}.copy(alpha = 0.55f)
