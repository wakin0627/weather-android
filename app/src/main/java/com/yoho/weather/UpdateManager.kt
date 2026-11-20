package com.yoho.weather

import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.core.content.FileProvider
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.io.File
import java.net.HttpURLConnection
import java.net.URL

/* ---------------- 应用内更新 ---------------- */

data class AppRelease(
    val versionCode: Int,
    val versionName: String,
    val notes: String,
    val apkUrl: String,
    val sha256: String,
    val force: Boolean,
    val tag: String = ""
)

private fun httpGet(url: String, timeoutMs: Int = 12000, headers: Map<String, String> = emptyMap()): String? = try {
    val conn = URL(url).openConnection() as HttpURLConnection
    conn.connectTimeout = 8000
    conn.readTimeout = timeoutMs
    headers.forEach { (k, v) -> conn.setRequestProperty(k, v) }
    val text = conn.inputStream.bufferedReader().use { it.readText() }
    conn.disconnect()
    text
} catch (e: Exception) {
    null
}

/** 更新源：公开仓库的 Releases（版本清单 + 安装包都挂在这里） */
private const val GH_OWNER = "wakin0627"
private const val GH_REPO = "weather-android"

/** 从 GitHub Releases 读最新版本；仓库不可达时返回 null */
suspend fun checkGitHubRelease(): AppRelease? = withContext(Dispatchers.IO) {
    try {
        val txt = httpGet(
            "https://api.github.com/repos/$GH_OWNER/$GH_REPO/releases/latest",
            headers = mapOf(
                "Accept" to "application/vnd.github+json",
                "User-Agent" to "weather-app"
            )
        ) ?: return@withContext null
        val o = JSONObject(txt)
        val tag = o.optString("tag_name").removePrefix("v")
        val body = o.optString("body")
        val assets = o.optJSONArray("assets") ?: return@withContext null
        var apkUrl = ""
        var manifestUrl = ""
        for (i in 0 until assets.length()) {
            val a = assets.getJSONObject(i)
            val n = a.optString("name")
            when {
                n.endsWith(".apk") -> apkUrl = a.optString("browser_download_url")
                n == "version.json" -> manifestUrl = a.optString("browser_download_url")
            }
        }
        if (apkUrl.isEmpty()) return@withContext null

        var code = 0
        var name = tag
        var notes = body
        var force = false
        if (manifestUrl.isNotEmpty()) {
            val m = httpGet(manifestUrl)?.let { runCatching { JSONObject(it) }.getOrNull() }
            if (m != null) {
                code = m.optInt("versionCode", 0)
                name = m.optString("versionName", tag)
                notes = m.optString("notes", body)
                force = m.optBoolean("force", false)
            }
        }
        if (code <= 0) return@withContext null
        AppRelease(code, name, notes, apkUrl, "", force, o.optString("tag_name"))
    } catch (e: Exception) {
        null
    }
}

/** 查询服务器上的最新版本（GitHub 不通时兜底） */
suspend fun checkRelease(): AppRelease? {
    checkGitHubRelease()?.let { return it }
    if (Api.BASE_URL.isBlank()) return null
    return checkServerRelease()
}

private suspend fun checkServerRelease(): AppRelease? = withContext(Dispatchers.IO) {
    val text = try { httpGet("${Api.BASE_URL}/api/app/version") } catch (e: Exception) { null }
        ?: return@withContext null
    try {
        val o = JSONObject(text)
        if (!o.optBoolean("ok", false)) return@withContext null
        val apk = o.optString("apk")
        AppRelease(
            versionCode = o.optInt("versionCode", 0),
            versionName = o.optString("versionName"),
            notes = o.optString("notes"),
            apkUrl = if (apk.startsWith("http")) apk else Api.BASE_URL + apk,
            sha256 = o.optString("sha256"),
            force = o.optBoolean("force", false),
            tag = o.optString("tag")
        )
    } catch (e: Exception) {
        null
    }
}

/**
 * 下载安装包：先直连（GitHub 资产/CDN），失败再走自建服务端中转。
 * 国内网络下 release-assets.githubusercontent.com 经常被污染，必须有这条后路。
 */
suspend fun downloadApkResilient(
    ctx: Context,
    rel: AppRelease,
    onProgress: (Float) -> Unit
): File? {
    val fileName = "weather-${rel.versionName}.apk"
    downloadApk(ctx, rel.apkUrl, fileName) { onProgress(it * 0.5f) }?.let { return it }

    val tag = rel.tag.ifBlank { "v${rel.versionName}" }
    if (Api.BASE_URL.isBlank()) return null
    val proxy = "${Api.BASE_URL}/api/app/apk?tag=${java.net.URLEncoder.encode(tag, "UTF-8")}"
    return downloadApk(ctx, proxy, fileName) { onProgress(0.5f + it * 0.5f) }
}

/** 下载安装包，回调进度 0..1 */
suspend fun downloadApk(
    ctx: Context,
    url: String,
    fileName: String,
    onProgress: (Float) -> Unit
): File? = withContext(Dispatchers.IO) {
    try {
        val dir = File(ctx.getExternalFilesDir(null), "updates").apply { mkdirs() }
        val out = File(dir, fileName)
        if (out.exists()) out.delete()

        val conn = URL(url).openConnection() as HttpURLConnection
        conn.connectTimeout = 10000
        conn.readTimeout = 60000
        conn.instanceFollowRedirects = true
        val total = conn.contentLengthLong
        var read = 0L
        conn.inputStream.use { input ->
            out.outputStream().use { output ->
                val buf = ByteArray(64 * 1024)
                var lastEmit = 0L
                while (true) {
                    val n = input.read(buf)
                    if (n <= 0) break
                    output.write(buf, 0, n)
                    read += n
                    if (total > 0) {
                        val now = System.currentTimeMillis()
                        if (now - lastEmit > 120) {
                            lastEmit = now
                            onProgress((read.toFloat() / total).coerceIn(0f, 1f))
                        }
                    }
                }
            }
        }
        conn.disconnect()
        onProgress(1f)
        if (out.length() < 1024 * 100) return@withContext null
        out
    } catch (e: Exception) {
        null
    }
}

/** 拉起系统安装器 */
fun installApk(ctx: Context, file: File) {
    val uri: Uri = FileProvider.getUriForFile(ctx, "${ctx.packageName}.fileprovider", file)
    val intent = Intent(Intent.ACTION_VIEW).apply {
        setDataAndType(uri, "application/vnd.android.package-archive")
        addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
    }
    ctx.startActivity(intent)
}

fun canInstallPackages(ctx: Context): Boolean =
    android.os.Build.VERSION.SDK_INT < 26 ||
            ctx.packageManager.canRequestPackageInstalls()

fun openInstallPermission(ctx: Context) {
    if (android.os.Build.VERSION.SDK_INT >= 26) {
        try {
            ctx.startActivity(
                Intent(android.provider.Settings.ACTION_MANAGE_UNKNOWN_APP_SOURCES)
                    .setData(Uri.parse("package:${ctx.packageName}"))
                    .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            )
        } catch (e: Exception) {
        }
    }
}
