package com.yoho.weather

import android.content.Context
import org.json.JSONArray
import org.json.JSONObject

/* ---------------- 城市 ---------------- */

data class City(
    val name: String,
    val lng: Double,
    val lat: Double,
    val sub: String = "",
    val located: Boolean = false
)

object CityStore {
    private const val FILE = "weather_city"
    private const val K_CURRENT = "current"
    private const val K_FAVS = "favorites"

    private fun sp(ctx: Context) = ctx.getSharedPreferences(FILE, Context.MODE_PRIVATE)

    private fun encode(c: City) = JSONObject()
        .put("name", c.name).put("lng", c.lng).put("lat", c.lat)
        .put("sub", c.sub).put("located", c.located).toString()

    private fun decode(s: String): City? = try {
        val o = JSONObject(s)
        City(
            name = o.optString("name"),
            lng = o.optDouble("lng"),
            lat = o.optDouble("lat"),
            sub = o.optString("sub"),
            located = o.optBoolean("located", false)
        )
    } catch (e: Exception) {
        null
    }

    fun current(ctx: Context): City? =
        sp(ctx).getString(K_CURRENT, null)?.let { decode(it) }

    fun setCurrent(ctx: Context, c: City) {
        sp(ctx).edit().putString(K_CURRENT, encode(c)).apply()
        if (!c.located) addFavorite(ctx, c)
    }

    fun favorites(ctx: Context): List<City> {
        val raw = sp(ctx).getString(K_FAVS, null) ?: return emptyList()
        return try {
            val arr = JSONArray(raw)
            (0 until arr.length()).mapNotNull { decode(arr.getString(it)) }
        } catch (e: Exception) {
            emptyList()
        }
    }

    fun addFavorite(ctx: Context, c: City) {
        val list = favorites(ctx).toMutableList()
        list.removeAll { it.name == c.name && kotlin.math.abs(it.lng - c.lng) < 0.01 }
        list.add(0, c)
        save(ctx, list.take(12))
    }

    fun removeFavorite(ctx: Context, c: City) {
        val list = favorites(ctx).filterNot { it.name == c.name && kotlin.math.abs(it.lng - c.lng) < 0.01 }
        save(ctx, list)
    }

    private fun save(ctx: Context, list: List<City>) {
        val arr = JSONArray()
        list.forEach { arr.put(encode(it)) }
        sp(ctx).edit().putString(K_FAVS, arr.toString()).apply()
    }
}
