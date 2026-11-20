package com.yoho.weather

import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import dev.chrisbanes.haze.HazeState
import dev.chrisbanes.haze.hazeSource
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.io.File

private val DEFAULT_CITY = City("杭州", 120.1499, 30.2850, "默认城市")

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        Api.initImageLoader(this)
        org.osmdroid.config.Configuration.getInstance().apply {
            userAgentValue = packageName
            osmdroidBasePath = java.io.File(cacheDir, "osmdroid")
            osmdroidTileCache = java.io.File(cacheDir, "osmdroid/tiles")
        }
        enableEdgeToEdge()
        setContent {
            MaterialTheme(colorScheme = darkColorScheme(primary = Color(0xFF4AA8FF))) {
                App()
            }
        }
    }
}

@Composable
private fun App() {
    val ctx = LocalContext.current
    val scope = rememberCoroutineScope()

    var tab by remember { mutableIntStateOf(0) }
    var data by remember { mutableStateOf<WeatherBundle?>(null) }
    var loading by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<String?>(null) }
    var showDays by remember { mutableStateOf(false) }
    var showName by remember { mutableStateOf(false) }
    var nickname by remember { mutableStateOf("天气用户") }
    var reqCount by remember { mutableIntStateOf(0) }
    var daysUsed by remember { mutableIntStateOf(1) }

    // 城市与定位
    var city by remember { mutableStateOf(CityStore.current(ctx) ?: DEFAULT_CITY) }
    var locating by remember { mutableStateOf(false) }
    var showCity by remember { mutableStateOf(false) }
    var showAdvice by remember { mutableStateOf(false) }
    var favCount by remember { mutableIntStateOf(CityStore.favorites(ctx).size) }

    // 应用内更新
    var release by remember { mutableStateOf<AppRelease?>(null) }
    var showUpdate by remember { mutableStateOf(false) }
    var downloading by remember { mutableStateOf(false) }
    var progress by remember { mutableFloatStateOf(0f) }
    var readyFile by remember { mutableStateOf<File?>(null) }

    fun toast(msg: String) = Toast.makeText(ctx, msg, Toast.LENGTH_SHORT).show()

    fun refresh() {
        if (loading) return
        loading = true
        scope.launch {
            Api.load(city.lng, city.lat)
                .onSuccess {
                    data = it
                    error = null
                    reqCount += 1
                }
                .onFailure { error = it.message }
            loading = false
        }
    }

    fun applyLocation(fromUser: Boolean) {
        if (locating) return
        locating = true
        scope.launch {
            val p = locateOnce(ctx)
            locating = false
            if (p == null) {
                toast(if (fromUser) "定位失败，请确认系统定位已开启" else "没取到定位，先显示杭州")
                if (fromUser) return@launch
                refresh()
                return@launch
            }
            val c = Api.reverse(p.first, p.second) ?: City("当前位置", p.first, p.second, "", true)
            city = c
            CityStore.setCurrent(ctx, c)
            if (fromUser) toast("已定位到 ${c.name}")
            refresh()
        }
    }

    val permLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { res ->
        if (res.values.any { it }) {
            applyLocation(false)
        } else {
            toast("未授权定位，默认杭州；可在「城市」里手动选择")
            refresh()
        }
    }

    fun doInstall(f: File) {
        if (!canInstallPackages(ctx)) {
            toast("请先允许安装未知来源应用")
            openInstallPermission(ctx)
        } else {
            installApk(ctx, f)
        }
    }

    fun startDownload() {
        val r = release ?: return
        readyFile?.let { doInstall(it); return }
        if (downloading) return
        scope.launch {
            downloading = true
            progress = 0f
            val f = downloadApkResilient(ctx, r) { progress = it }
            downloading = false
            if (f == null) {
                toast("下载失败，请稍后重试")
            } else {
                readyFile = f
                doInstall(f)
            }
        }
    }

    fun checkUpdate(manual: Boolean) {
        scope.launch {
            val r = checkRelease()
            if (r == null) {
                if (manual) toast("检查更新失败，请检查网络")
                return@launch
            }
            if (r.versionCode > BuildConfig.VERSION_CODE) {
                release = r
                showUpdate = true
            } else if (manual) {
                toast("已是最新版本 v${BuildConfig.VERSION_NAME}")
            }
        }
    }

    LaunchedEffect(Unit) {
        val saved = CityStore.current(ctx)
        if (saved == null) {
            if (hasLocationPermission(ctx)) applyLocation(false)
            else permLauncher.launch(LOCATION_PERMISSIONS)
        } else if (saved.located && hasLocationPermission(ctx)) {
            applyLocation(false)      // 跟随定位，静默更新
        } else {
            refresh()
        }
        delay(2500)
        checkUpdate(false)
    }

    val hazeState = remember { HazeState() }

    // 返回键：先关浮层，再回首页 tab，最后才退出
    BackHandler(enabled = showAdvice) { showAdvice = false }
    BackHandler(enabled = showCity) { showCity = false }
    BackHandler(enabled = showName) { showName = false }
    BackHandler(enabled = showDays) { showDays = false }
    BackHandler(enabled = tab != 0) { tab = 0 }

    Box(Modifier.fillMaxSize().background(Color(0xFF0A0F16))) {
      Box(Modifier.fillMaxSize().hazeSource(hazeState)) {
        when (tab) {
            0 -> WeatherScreen(
                data = data,
                loading = loading,
                error = error,
                onRefresh = { refresh() },
                onOpenDays = { if (data != null) showDays = true },
                onToast = { toast(it) },
                cityName = data?.place?.city?.ifEmpty { city.name } ?: city.name,
                onOpenCity = { showCity = true },
                onOpenAdvice = { if (data != null) showAdvice = true }
            )
            1 -> androidx.compose.runtime.key(city.lng, city.lat) { MapScreen(city.lng, city.lat) }
            else -> MeScreen(
                cityCount = favCount,
                days = daysUsed,
                requests = reqCount,
                nickname = nickname,
                onNickname = { showName = true },
                onCheckUpdate = { checkUpdate(true) },
                onToast = { toast(it) }
            )
        }
      }

        Box(Modifier.align(Alignment.BottomCenter)) {
            LiquidGlassBar(tab, hazeState) { tab = it }
        }

        if (showAdvice && data != null) {
            AdviceScreen(data!!) { showAdvice = false }
        }

        // Apple 天气式城市卡片列表（全屏覆盖）
        if (showCity) {
            CityCardsOverlay(
                current = city,
                locating = locating,
                onLocate = { applyLocation(true) },
                onPick = { c ->
                    city = c
                    CityStore.setCurrent(ctx, c)
                    favCount = CityStore.favorites(ctx).size
                    showCity = false
                    refresh()
                },
                onClose = { showCity = false }
            )
        }
    }

    if (showDays && data != null) {
        DaysDialog(data!!) { showDays = false }
    }

    if (showUpdate && release != null) {
        val r = release!!
        AlertDialog(
            onDismissRequest = { if (!downloading) showUpdate = false },
            title = { Text("发现新版本 v${r.versionName}") },
            text = {
                Column {
                    Text(r.notes.ifEmpty { "修复若干问题" }, fontSize = 13.sp)
                    if (downloading) {
                        Spacer(Modifier.height(14.dp))
                        LinearProgressIndicator(
                            progress = { progress },
                            modifier = Modifier.fillMaxWidth()
                        )
                        Text(
                            "下载中 ${(progress * 100).toInt()}%",
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
                            fontSize = 12.sp,
                            modifier = Modifier.padding(top = 6.dp)
                        )
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { startDownload() }, enabled = !downloading) {
                    Text(if (readyFile != null) "安装" else "立即更新")
                }
            },
            dismissButton = {
                TextButton(onClick = { showUpdate = false }, enabled = !downloading) { Text("以后") }
            }
        )
    }

    if (showName) {
        var input by remember { mutableStateOf(nickname) }
        AlertDialog(
            onDismissRequest = { showName = false },
            title = { Text("修改昵称") },
            text = {
                OutlinedTextField(
                    value = input,
                    onValueChange = { input = it.take(12) },
                    singleLine = true,
                    label = { Text("昵称") }
                )
            },
            confirmButton = {
                TextButton(onClick = {
                    if (input.isNotBlank()) nickname = input.trim()
                    showName = false
                }) { Text("保存") }
            },
            dismissButton = { TextButton(onClick = { showName = false }) { Text("取消") } }
        )
    }
}

@Composable
private fun BoxScope.BottomBar(tab: Int, onTab: (Int) -> Unit) {
    val items = listOf("天气", "地图", "我的")
    Row(
        Modifier
            .fillMaxWidth()
            .align(Alignment.BottomCenter)
            .background(
                Brush.verticalGradient(
                    listOf(Color.Transparent, Color(0xCC0A0F16), Color(0xF20A0F16))
                )
            )
            .navigationBarsPadding()
            .padding(horizontal = 12.dp, vertical = 8.dp)
    ) {
        items.forEachIndexed { i, label ->
            Column(
                Modifier
                    .weight(1f)
                    .clip(RoundedCornerShape(14.dp))
                    .clickable { onTab(i) }
                    .padding(vertical = 6.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    label,
                    color = if (tab == i) Color.White else Color.White.copy(alpha = 0.5f),
                    fontSize = 12.sp,
                    fontWeight = FontWeight.SemiBold
                )
                Box(
                    Modifier
                        .padding(top = 4.dp)
                        .size(width = if (tab == i) 16.dp else 0.dp, height = 2.dp)
                        .background(Color.White, RoundedCornerShape(2.dp))
                )
            }
        }
    }
}

@Composable
private fun DaysDialog(data: WeatherBundle, onDismiss: () -> Unit) {
    AlertDialog(
        onDismissRequest = onDismiss,
        confirmButton = { TextButton(onClick = onDismiss) { Text("关闭") } },
        title = { Text("未来 15 天") },
        text = {
            LazyColumn(Modifier.heightIn(max = 420.dp)) {
                itemsIndexed(data.days) { i, d ->
                    Row(
                        Modifier.fillMaxWidth().padding(vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            dayLabel(d.date, i),
                            color = MaterialTheme.colorScheme.onSurface,
                            fontSize = 14.sp,
                            modifier = Modifier.width(54.dp)
                        )
                        WeatherIcon(d.icon, 22)
                        Spacer(Modifier.weight(1f))
                        Text(
                            "${d.text} ${d.pop}%",
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
                            fontSize = 12.sp,
                            textAlign = TextAlign.End,
                            modifier = Modifier.weight(1.2f)
                        )
                        Text(
                            "${d.lo}° / ${d.hi}°",
                            color = MaterialTheme.colorScheme.onSurface,
                            fontSize = 13.sp,
                            modifier = Modifier.width(72.dp),
                            textAlign = TextAlign.End
                        )
                    }
                }
            }
        }
    )
}
