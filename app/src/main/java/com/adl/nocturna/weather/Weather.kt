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
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeoutOrNull
import kotlin.coroutines.resume
import java.net.HttpURLConnection
import java.net.URL
import java.net.URLEncoder
import java.util.Locale

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
        parseWeather(get(url), lat, lon, fetchedAt = System.currentTimeMillis())
    }

    /** Looks up a city by name; returns the best match. */
    suspend fun geocode(query: String): Place? = withContext(Dispatchers.IO) {
        val q = URLEncoder.encode(query.trim(), "UTF-8")
        val lang = Locale.getDefault().language
        parsePlace(get("https://geocoding-api.open-meteo.com/v1/search?name=$q&count=1&language=$lang"))
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
