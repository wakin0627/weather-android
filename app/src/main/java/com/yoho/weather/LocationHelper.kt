package com.yoho.weather

import android.Manifest
import android.annotation.SuppressLint
import android.content.Context
import android.content.pm.PackageManager
import android.location.Location
import android.location.LocationListener
import android.location.LocationManager
import android.os.Build
import android.os.Bundle
import android.os.Looper
import androidx.core.content.ContextCompat
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeoutOrNull
import kotlin.coroutines.resume

/* ---------------- 定位 ---------------- */

val LOCATION_PERMISSIONS = arrayOf(
    Manifest.permission.ACCESS_FINE_LOCATION,
    Manifest.permission.ACCESS_COARSE_LOCATION
)

fun hasLocationPermission(ctx: Context): Boolean =
    ContextCompat.checkSelfPermission(ctx, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED ||
            ContextCompat.checkSelfPermission(ctx, Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED

private fun bestLastKnown(ctx: Context, lm: LocationManager): Location? {
    if (!hasLocationPermission(ctx)) return null
    val providers = listOf(LocationManager.GPS_PROVIDER, LocationManager.NETWORK_PROVIDER, LocationManager.PASSIVE_PROVIDER)
    return try {
        providers.mapNotNull { p ->
            runCatching { lm.getLastKnownLocation(p) }.getOrNull()
        }.maxByOrNull { it.time }
    } catch (e: SecurityException) {
        null
    }
}

/**
 * 取当前位置（一次性）。优先用系统缓存的最近位置，太旧再单次请求定位。
 * 无权限或超时返回 null，调用方自行兜底。
 */
@SuppressLint("MissingPermission")
suspend fun locateOnce(ctx: Context, timeoutMs: Long = 12000): Pair<Double, Double>? =
    withContext(Dispatchers.Main) {
        if (!hasLocationPermission(ctx)) return@withContext null
        val lm = ctx.getSystemService(Context.LOCATION_SERVICE) as? LocationManager
            ?: return@withContext null

        // 1) 5 分钟内的缓存位置直接用
        bestLastKnown(ctx, lm)?.let { loc ->
            if (System.currentTimeMillis() - loc.time < 5 * 60 * 1000) {
                return@withContext loc.longitude to loc.latitude
            }
        }

        // 2) 所有可用 provider 一起请求，谁先给结果用谁
        val providers = listOf(LocationManager.GPS_PROVIDER, LocationManager.NETWORK_PROVIDER)
            .filter { p -> runCatching { lm.isProviderEnabled(p) }.getOrDefault(false) }
        if (providers.isEmpty()) {
            return@withContext bestLastKnown(ctx, lm)?.let { it.longitude to it.latitude }
        }

        withTimeoutOrNull(timeoutMs) {
            suspendCancellableCoroutine<Pair<Double, Double>?> { cont ->
                val listener = object : LocationListener {
                    override fun onLocationChanged(location: Location) {
                        runCatching { lm.removeUpdates(this) }
                        if (cont.isActive) cont.resume(location.longitude to location.latitude)
                    }

                    override fun onProviderEnabled(provider: String) {}
                    override fun onProviderDisabled(provider: String) {}
                    @Deprecated("兼容旧系统")
                    override fun onStatusChanged(provider: String?, status: Int, extras: Bundle?) {}
                }
                cont.invokeOnCancellation { runCatching { lm.removeUpdates(listener) } }
                providers.forEach { p ->
                    runCatching { lm.requestLocationUpdates(p, 0L, 0f, listener, Looper.getMainLooper()) }
                }
            }
        } ?: bestLastKnown(ctx, lm)?.let { it.longitude to it.latitude }
    }
