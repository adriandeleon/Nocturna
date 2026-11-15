package com.adl.nocturna.weather

import com.adl.nocturna.data.Settings
import com.adl.nocturna.ui.Glyph
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime
import kotlin.time.Instant

/** How old the weather may get before it is fetched again, on the clock and on the wake-up screen. */
const val WEATHER_MAX_AGE_MS = 30 * 60_000L

/**
 * Older weather isn't shown on the wake-up screen at all, while it is refreshed: last night's temperature
 * isn't this morning's.
 */
const val WAKE_WEATHER_SHOW_MAX_AGE_MS = 6 * 3600_000L

/** Whether this weather is recent enough not to fetch again at [now]. */
fun Weather.isFresh(now: Instant): Boolean = now.toEpochMilliseconds() - fetchedAt < WEATHER_MAX_AGE_MS

/** The weather line on the wake-up screen: the clock's [glyph] for the current conditions, then [text]. */
data class WakeWeather(val glyph: Glyph, val text: String)

/** Whether the wake-up screen shows weather at all: switched on, and there is a place to have weather for. */
private val Settings.wakeWeatherWanted: Boolean
    get() = alarmShowWeather && (hasWeatherLocation || weatherUseDeviceLocation)

/**
 * What the wake-up screen shows of [weather] at [now] in [zone]: "14° · 22° / 9°", the current temperature
 * and today's high and low, in the info line's units; just "14°" when today isn't in the forecast. Null,
 * so the screen shows nothing, when the setting is off, no place is set, or there's no recent weather.
 */
fun wakeWeather(settings: Settings, weather: Weather?, now: Instant, zone: TimeZone): WakeWeather? {
    if (!settings.wakeWeatherWanted || weather == null) return null
    if (now.toEpochMilliseconds() - weather.fetchedAt > WAKE_WEATHER_SHOW_MAX_AGE_MS) return null
    val f = settings.weatherFahrenheit
    val today = now.toLocalDateTime(zone).date
    val day = weather.days.firstOrNull { it.date == today }
    val current = weather.temperatureText(f)
    val text = if (day == null) current else "$current · ${temperatureText(day.highC, f)} / ${temperatureText(day.lowC, f)}"
    return WakeWeather(weather.glyph, text)
}

/**
 * Whether the wake-up screen should fetch the weather: it is wanted and what we have is missing or older
 * than [WEATHER_MAX_AGE_MS]. The caller also checks that the phone is online.
 */
fun wakeWeatherNeedsRefresh(settings: Settings, weather: Weather?, now: Instant): Boolean =
    settings.wakeWeatherWanted && (weather == null || !weather.isFresh(now))
