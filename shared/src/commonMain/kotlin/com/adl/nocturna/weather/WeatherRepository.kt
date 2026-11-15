package com.adl.nocturna.weather

import com.adl.nocturna.data.Settings
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlin.math.abs
import kotlin.time.Clock

/** Current conditions and a short daily forecast from the free Open-Meteo API (no key needed). */
object WeatherRepository {
    private const val MAX_AGE_MS = 30 * 60_000L
    private val state = MutableStateFlow<Weather?>(null)
    val weather: StateFlow<Weather?> = state.asStateFlow()

    /** Fetches a page as text; tests swap in their own. */
    internal var get: suspend (url: String) -> String = { httpGet(it) }
    internal var clock: Clock = Clock.System

    /**
     * Fetches the weather unless what we have is under 30 minutes old and for about the same place. The
     * place is the phone's, from [deviceLocation], when the settings ask for it and it is known, otherwise
     * the city in the settings. A failed fetch keeps what we had.
     */
    suspend fun refreshIfStale(settings: Settings, deviceLocation: suspend () -> Pair<Double, Double>?) {
        val (lat, lon) = location(settings, deviceLocation) ?: return
        val cur = state.value
        val moved = cur == null || abs(cur.lat - lat) > 0.05 || abs(cur.lon - lon) > 0.05
        if (!moved && clock.now().toEpochMilliseconds() - cur.fetchedAt < MAX_AGE_MS) return
        runCatching { parseWeather(get(forecastUrl(lat, lon)), lat, lon, fetchedAt = clock.now().toEpochMilliseconds()) }
            .onSuccess { state.value = it }
    }

    private suspend fun location(settings: Settings, deviceLocation: suspend () -> Pair<Double, Double>?): Pair<Double, Double>? {
        if (settings.weatherUseDeviceLocation) deviceLocation()?.let { return it }
        return if (settings.hasWeatherLocation) settings.weatherLat to settings.weatherLon else null
    }

    /** Looks up a city by name, with its name in [language]; the best match, or null. Throws when offline. */
    suspend fun geocode(query: String, language: String): Place? = parsePlace(get(geocodeUrl(query, language)))

    /** Forgets the weather, so the next refresh fetches it whatever its age: after the place changes. */
    fun invalidate() {
        state.value = null
    }
}
