package com.yoho.weather

import android.content.Context
import coil.ImageLoader
import coil.decode.SvgDecoder
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL

/* ---------------- 数据模型 ---------------- */

data class Place(
    val city: String,
    val district: String,
    val township: String,
    val spot: String,       // 小区/POI 名
    val spotDist: Int,
    val address: String
)

data class Now(
    val time: String,
    val temp: Int,
    val feels: Int,
    val humidity: Int,
    val dew: Int,
    val pressure: Int,
    val windDir: String,
    val windLevel: Int,
    val windKmh: Int,
    val visibility: Double,
    val cloud: Int,
    val text: String,
    val icon: String,
    val group: String,
    val isDay: Boolean,
    val uv: Int = 0,
    val sunrise: String = "",
    val sunset: String = ""
)

data class Hour(val t: String, val temp: Int, val pop: Int, val icon: String)

data class Day(
    val date: String,
    val hi: Int,
    val lo: Int,
    val icon: String,
    val text: String,
    val pop: Int,
    val precip: Double,
    val uv: Double,
    val sunrise: String,
    val sunset: String
)

data class Advice(val icon: String, val title: String, val sub: String)

data class WeatherBundle(
    val place: Place?,
    val now: Now,
    val hours: List<Hour>,
    val days: List<Day>,
    val advice: List<Advice>,
    val updated: Long
)

data class CityCardInfo(
    val temp: Int,
    val text: String,
    val icon: String,
    val group: String,
    val isDay: Boolean,
    val hi: Int,
    val lo: Int,
    val pop: Int
)

data class CityHit(
    val name: String,
    val sub: String,
    val lng: Double,
    val lat: Double
)

/* ---------------- 网络 ---------------- */

object Api {

    // 演示版地址：正式版换成自己的域名
    val BASE_URL: String = BuildConfig.API_BASE

    val APP_VERSION: String = BuildConfig.VERSION_NAME

    const val ICON_CDN = "https://cdn.jsdelivr.net/gh/basmilius/weather-icons@dev/production/fill/svg/"

    var imageLoader: ImageLoader? = null

    fun initImageLoader(ctx: Context) {
        if (imageLoader != null) return
        imageLoader = ImageLoader.Builder(ctx)
            .components { add(SvgDecoder.Factory()) }
            .crossfade(true)
            .build()
    }

    private suspend fun get(path: String): JSONObject = withContext(Dispatchers.IO) {
        try {
            require(BASE_URL.isNotBlank()) { "未配置服务端地址" }
            val conn = URL(BASE_URL + path).openConnection() as HttpURLConnection
            conn.connectTimeout = 8000
            conn.readTimeout = 15000
            conn.requestMethod = "GET"
            val text = conn.inputStream.bufferedReader().use { it.readText() }
            conn.disconnect()
            JSONObject(text)
        } catch (e: Exception) {
            JSONObject().put("ok", false).put("error", e.message ?: "网络异常")
        }
    }

    /** 轻量取一张城市卡片的数据（只打天气接口） */
    suspend fun quickCard(lng: Double, lat: Double): CityCardInfo? = withContext(Dispatchers.IO) {
        val wx = get("/api/weather?lng=$lng&lat=$lat")
        if (!wx.optBoolean("ok", false)) return@withContext null
        val n = wx.optJSONObject("now") ?: return@withContext null
        val d0 = wx.optJSONArray("days")?.optJSONObject(0)
        CityCardInfo(
            temp = n.optInt("temp"),
            text = n.optString("text"),
            icon = n.optString("icon", "cloudy"),
            group = n.optString("group", "cloudy"),
            isDay = n.optInt("is_day", 1) == 1,
            hi = d0?.optInt("hi") ?: n.optInt("temp"),
            lo = d0?.optInt("lo") ?: n.optInt("temp"),
            pop = d0?.optInt("pop") ?: 0
        )
    }

    /** 逆地理：坐标 -> 城市名（用于定位后命名） */
    suspend fun reverse(lng: Double, lat: Double): City? = withContext(Dispatchers.IO) {
        val o = get("/api/where?lng=$lng&lat=$lat")
        if (!o.optBoolean("ok", false)) return@withContext null
        val name = o.optString("city").replace("市", "").ifEmpty { "当前位置" }
        val district = o.optString("district")
        val township = o.optString("township")
        val sub = listOf(district, township).filter { it.isNotEmpty() }.joinToString(" · ")
        City(name, lng, lat, sub, located = true)
    }

    suspend fun search(q: String): List<CityHit> = withContext(Dispatchers.IO) {
        val o = get("/api/search?q=" + java.net.URLEncoder.encode(q, "UTF-8") + "&limit=10")
        if (!o.optBoolean("ok", false)) return@withContext emptyList()
        val arr = o.optJSONArray("items") ?: return@withContext emptyList()
        (0 until arr.length()).map { i ->
            val it = arr.getJSONObject(i)
            val cityName = it.optString("cityname").replace("市", "")
            val district = it.optString("district")
            val sub = listOf(cityName, district).filter { s -> s.isNotEmpty() }.distinct().joinToString(" · ")
            CityHit(
                name = it.optString("name"),
                sub = sub.ifEmpty { it.optString("address") },
                lng = it.optDouble("lng"),
                lat = it.optDouble("lat")
            )
        }
    }

    suspend fun load(lng: Double, lat: Double): Result<WeatherBundle> = coroutineScope {
        val placeDeferred = async { get("/api/where?lng=$lng&lat=$lat") }
        val wxDeferred = async { get("/api/weather?lng=$lng&lat=$lat") }
        val placeJson = placeDeferred.await()
        val wx = wxDeferred.await()

        if (!wx.optBoolean("ok", false)) {
            return@coroutineScope Result.failure(Exception(wx.optString("error", "天气数据获取失败")))
        }

        val place = if (placeJson.optBoolean("ok", false)) {
            val estate = placeJson.optJSONObject("estate")
            val poi = placeJson.optJSONObject("poi")
            val spot = estate?.optString("name").orEmpty().ifEmpty { poi?.optString("name").orEmpty() }
            val dist = estate?.optInt("distance", 0) ?: poi?.optInt("distance", 0) ?: 0
            Place(
                city = placeJson.optString("city").replace("市", ""),
                district = placeJson.optString("district"),
                township = placeJson.optString("township"),
                spot = spot,
                spotDist = dist,
                address = placeJson.optString("address")
            )
        } else null

        val n = wx.getJSONObject("now")
        val now = Now(
            time = n.optString("time"),
            temp = n.optInt("temp"),
            feels = n.optInt("feels"),
            humidity = n.optInt("humidity"),
            dew = n.optInt("dew"),
            pressure = n.optInt("pressure"),
            windDir = n.optString("wind_dir"),
            windLevel = n.optInt("wind_level"),
            windKmh = n.optInt("wind_kmh"),
            visibility = n.optDouble("visibility", 10.0),
            cloud = n.optInt("cloud"),
            text = n.optString("text"),
            icon = n.optString("icon", "cloudy"),
            group = n.optString("group", "cloudy"),
            isDay = n.optInt("is_day", 1) == 1,
            uv = n.optInt("uv", 0),
            sunrise = n.optString("sunrise"),
            sunset = n.optString("sunset")
        )

        val hoursJson = wx.getJSONArray("hours")
        val hours = (0 until hoursJson.length()).map { i ->
            val o = hoursJson.getJSONObject(i)
            Hour(o.optString("t"), o.optInt("temp"), o.optInt("pop"), o.optString("icon"))
        }

        val daysJson = wx.getJSONArray("days")
        val days = (0 until daysJson.length()).map { i ->
            val o = daysJson.getJSONObject(i)
            Day(
                date = o.optString("date"),
                hi = o.optInt("hi"),
                lo = o.optInt("lo"),
                icon = o.optString("icon"),
                text = o.optString("text"),
                pop = o.optInt("pop"),
                precip = o.optDouble("precip", 0.0),
                uv = o.optDouble("uv", 0.0),
                sunrise = o.optString("sunrise").takeLast(5),
                sunset = o.optString("sunset").takeLast(5)
            )
        }

        val advJson = wx.getJSONArray("advice")
        val advice = (0 until advJson.length()).map { i ->
            val o = advJson.getJSONObject(i)
            Advice(o.optString("icon"), o.optString("title"), o.optString("sub"))
        }

        Result.success(
            WeatherBundle(place, now, hours, days, advice, wx.optLong("updated", 0L))
        )
    }
}
