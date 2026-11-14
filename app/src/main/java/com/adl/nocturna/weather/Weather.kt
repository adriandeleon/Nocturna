package com.adl.nocturna.weather

import android.Manifest
import android.annotation.SuppressLint
import android.content.Context
import android.content.pm.PackageManager
import android.location.Location
import android.location.LocationManager
import android.os.CancellationSignal
import androidx.core.location.LocationManagerCompat
import androidx.core.content.ContextCompat
import com.adl.nocturna.data.Settings
import com.adl.nocturna.ui.Glyph
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeoutOrNull
import kotlin.coroutines.resume
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL
import java.net.URLEncoder
import java.time.LocalDate
import java.time.LocalDateTime
import java.util.Locale
import kotlin.math.roundToInt

data class Weather(
    val temperatureC: Double,
    /** WMO weather interpretation code, as returned by Open-Meteo. */
    val code: Int,
    val isDay: Boolean,
    val fetchedAt: Long,
    val lat: Double,
    val lon: Double,
    /** Daily forecasts by the forecast location's calendar date, today first. */
    val days: List<DayForecast> = emptyList(),
) {
    fun temperatureText(fahrenheit: Boolean): String = temperatureText(temperatureC, fahrenheit)

    val glyph: Glyph get() = weatherGlyph(code, isDay)
}

/** One day's forecast: WMO weather [code] and the high and low in °C. */
data class DayForecast(val date: LocalDate, val code: Int, val highC: Double, val lowC: Double) {
    val glyph: Glyph get() = weatherGlyph(code, isDay = true)

    /** "↑24° ↓12°" */
    fun highLowText(fahrenheit: Boolean) = "↑${temperatureText(highC, fahrenheit)} ↓${temperatureText(lowC, fahrenheit)}"
}

/** Which day's forecast the clock shows, and whether it is tomorrow's. */
data class ForecastPick(val day: DayForecast, val tomorrow: Boolean)

/** Hour from which the clock shows tomorrow's forecast instead of today's: what you want before bed. */
const val TOMORROW_FROM_HOUR = 18

/**
 * Today's forecast until [TOMORROW_FROM_HOUR], tomorrow's after it. Null when that day isn't in [days],
 * for example data fetched before midnight that doesn't reach far enough yet.
 */
fun pickForecast(days: List<DayForecast>, now: LocalDateTime): ForecastPick? {
    val tomorrow = now.hour >= TOMORROW_FROM_HOUR
    val date = if (tomorrow) now.toLocalDate().plusDays(1) else now.toLocalDate()
    return days.firstOrNull { it.date == date }?.let { ForecastPick(it, tomorrow) }
}

/**
 * Reads Open-Meteo's `daily` block: parallel arrays of dates, weather codes, highs and lows. Days with a
 * missing value (Open-Meteo sends null) are left out rather than shown wrong.
 */
fun parseDaily(response: JSONObject): List<DayForecast> {
    val daily = response.optJSONObject("daily") ?: return emptyList()
    val time = daily.optJSONArray("time") ?: return emptyList()
    val codes = daily.optJSONArray("weather_code") ?: return emptyList()
    val highs = daily.optJSONArray("temperature_2m_max") ?: return emptyList()
    val lows = daily.optJSONArray("temperature_2m_min") ?: return emptyList()
    return (0 until time.length()).mapNotNull { i ->
        if (codes.isNull(i) || highs.isNull(i) || lows.isNull(i)) return@mapNotNull null
        runCatching {
            DayForecast(LocalDate.parse(time.getString(i)), codes.getInt(i), highs.getDouble(i), lows.getDouble(i))
        }.getOrNull()
    }
}

private fun temperatureText(celsius: Double, fahrenheit: Boolean): String {
    val t = if (fahrenheit) celsius * 9 / 5 + 32 else celsius
    return "${t.roundToInt()}°"
}

private fun weatherGlyph(code: Int, isDay: Boolean): Glyph = when (code) {
    0 -> if (isDay) Glyph.SUN else Glyph.MOON
    1, 2 -> if (isDay) Glyph.PARTLY_DAY else Glyph.PARTLY_NIGHT
    3 -> Glyph.CLOUD
    45, 48 -> Glyph.FOG
    in 51..67, in 80..82 -> Glyph.RAIN
    in 71..77, 85, 86 -> Glyph.SNOW
    in 95..99 -> Glyph.STORM
    else -> Glyph.CLOUD
}

data class Place(val name: String, val lat: Double, val lon: Double)

/** Current conditions and a short daily forecast from the free Open-Meteo API (no key needed). */
object WeatherRepository {
    private const val MAX_AGE_MS = 30 * 60_000L
    private val state = MutableStateFlow<Weather?>(null)
    val weather: StateFlow<Weather?> = state.asStateFlow()

    suspend fun refreshIfStale(context: Context, settings: Settings) {
        val (lat, lon) = location(context, settings) ?: return
        val cur = state.value
        val moved = cur == null || Math.abs(cur.lat - lat) > 0.05 || Math.abs(cur.lon - lon) > 0.05
        if (!moved && System.currentTimeMillis() - cur.fetchedAt < MAX_AGE_MS) return
        runCatching { fetch(lat, lon) }.onSuccess { state.value = it }
    }

    private suspend fun location(context: Context, settings: Settings): Pair<Double, Double>? {
        if (settings.weatherUseDeviceLocation) deviceLocation(context)?.let { return it }
        return if (settings.hasWeatherLocation) settings.weatherLat to settings.weatherLon else null
    }

    @SuppressLint("MissingPermission")
    private suspend fun deviceLocation(context: Context): Pair<Double, Double>? {
        val granted = ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_COARSE_LOCATION) ==
            PackageManager.PERMISSION_GRANTED
        if (!granted) return null
        val lm = context.getSystemService(LocationManager::class.java) ?: return null
        val enabled = lm.getProviders(true)
        enabled.mapNotNull { runCatching { lm.getLastKnownLocation(it) }.getOrNull() }
            .filter { System.currentTimeMillis() - it.time < 6 * 3600_000L }
            .maxByOrNull { it.time }
            ?.let { return it.latitude to it.longitude }

        // Nothing cached (common when no other app used location lately): ask for a fresh fix.
        val provider = listOf("fused", "network", LocationManager.GPS_PROVIDER).firstOrNull { it in enabled } ?: return null
        val fix = withTimeoutOrNull(20_000) {
            suspendCancellableCoroutine<Location?> { cont ->
                val cancel = CancellationSignal()
                cont.invokeOnCancellation { cancel.cancel() }
                runCatching {
                    LocationManagerCompat.getCurrentLocation(lm, provider, cancel, ContextCompat.getMainExecutor(context)) {
                        if (cont.isActive) cont.resume(it)
                    }
                }.onFailure { if (cont.isActive) cont.resume(null) }
            }
        }
        return fix?.let { it.latitude to it.longitude }
    }

    private suspend fun fetch(lat: Double, lon: Double): Weather = withContext(Dispatchers.IO) {
        val url = String.format(
            Locale.US,
            "https://api.open-meteo.com/v1/forecast?latitude=%.4f&longitude=%.4f" +
                "&current=temperature_2m,weather_code,is_day" +
                // Three days, in the place's own time zone, so "today" and "tomorrow" are there across midnight.
                "&daily=weather_code,temperature_2m_max,temperature_2m_min&forecast_days=3&timezone=auto",
            lat, lon,
        )
        val response = JSONObject(get(url))
        val current = response.getJSONObject("current")
        Weather(
            temperatureC = current.getDouble("temperature_2m"),
            code = current.getInt("weather_code"),
            isDay = current.optInt("is_day", 1) == 1,
            fetchedAt = System.currentTimeMillis(),
            lat = lat,
            lon = lon,
            days = parseDaily(response),
        )
    }

    /** Looks up a city by name; returns the best match. */
    suspend fun geocode(query: String): Place? = withContext(Dispatchers.IO) {
        val q = URLEncoder.encode(query.trim(), "UTF-8")
        val lang = Locale.getDefault().language
        val results = JSONObject(get("https://geocoding-api.open-meteo.com/v1/search?name=$q&count=1&language=$lang"))
            .optJSONArray("results") ?: return@withContext null
        if (results.length() == 0) return@withContext null
        val r = results.getJSONObject(0)
        val label = listOfNotNull(r.optString("name").takeIf { it.isNotBlank() }, r.optString("country").takeIf { it.isNotBlank() })
            .joinToString(", ")
        Place(label, r.getDouble("latitude"), r.getDouble("longitude"))
    }

    fun invalidate() {
        state.value = null
    }

    private fun get(url: String): String {
        val conn = URL(url).openConnection() as HttpURLConnection
        conn.connectTimeout = 10_000
        conn.readTimeout = 10_000
        conn.setRequestProperty("User-Agent", "Nocturna-Android")
        try {
            if (conn.responseCode !in 200..299) error("HTTP ${conn.responseCode}")
            return conn.inputStream.bufferedReader().use { it.readText() }
        } finally {
            conn.disconnect()
        }
    }
}
