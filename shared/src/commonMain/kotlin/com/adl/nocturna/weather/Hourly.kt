package com.adl.nocturna.weather

import com.adl.nocturna.data.asLong
import com.adl.nocturna.data.optArray
import com.adl.nocturna.data.optObject
import com.adl.nocturna.data.primitive
import com.adl.nocturna.data.primitiveAt
import com.adl.nocturna.text.DateTimePattern
import com.adl.nocturna.text.PlatformLocale
import com.adl.nocturna.ui.Glyph
import kotlinx.datetime.LocalDateTime
import kotlinx.datetime.TimeZone
import kotlinx.datetime.UtcOffset
import kotlinx.datetime.toInstant
import kotlinx.datetime.toLocalDateTime
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.doubleOrNull
import kotlin.time.Instant

/** Precipitation probability (%) from which the hourly strip shows it. */
const val SHOW_RAIN_FROM_PERCENT = 30

/** One hour's forecast: the temperature at [time], its WMO weather [code] and the chance of rain, if known. */
data class HourForecast(
    val time: Instant,
    val code: Int,
    val isDay: Boolean,
    val temperatureC: Double,
    val precipitationPercent: Int? = null,
) {
    val glyph: Glyph get() = weatherGlyph(code, isDay)

    fun temperatureText(fahrenheit: Boolean): String = temperatureText(temperatureC, fahrenheit)

    /** "40%" when rain is likely enough to mention, otherwise null. */
    val rainText: String? get() = precipitationPercent?.takeIf { it >= SHOW_RAIN_FROM_PERCENT }?.let { "$it%" }
}

/** An hour of the strip: its forecast and the time it falls on in the clock's zone. */
data class HourSlot(val local: LocalDateTime, val forecast: HourForecast)

/**
 * Reads Open-Meteo's `hourly` block. Its times are local to the place but carry no offset: Open-Meteo
 * applies the one `utc_offset_seconds` of the response to every hour, even across a change to or from
 * summer time, so that offset (not the place's time zone rules) turns them back into instants. Hours
 * with a missing value are left out; without the offset there is nothing to place them by.
 */
fun parseHourly(response: JsonObject): List<HourForecast> {
    val offset = responseOffset(response) ?: return emptyList()
    val hourly = response.optObject("hourly") ?: return emptyList()
    val time = hourly.optArray("time") ?: return emptyList()
    val temps = hourly.optArray("temperature_2m") ?: return emptyList()
    val codes = hourly.optArray("weather_code") ?: return emptyList()
    val rain = hourly.optArray("precipitation_probability")
    val isDay = hourly.optArray("is_day")
    return time.indices.mapNotNull { i ->
        val at = time.primitiveAt(i)?.content ?: return@mapNotNull null
        val temp = temps.primitiveAt(i)?.doubleOrNull ?: return@mapNotNull null
        val code = codes.primitiveAt(i)?.asLong()?.toInt() ?: return@mapNotNull null
        val instant = runCatching { LocalDateTime.parse(at).toInstant(offset) }.getOrNull() ?: return@mapNotNull null
        HourForecast(
            time = instant,
            code = code,
            isDay = isDay?.primitiveAt(i)?.asLong()?.let { it == 1L } ?: true,
            temperatureC = temp,
            precipitationPercent = rain?.primitiveAt(i)?.asLong()?.toInt(),
        )
    }
}

/** The one UTC offset Open-Meteo gives all of a response's local times, or null when it's missing. */
internal fun responseOffset(response: JsonObject): UtcOffset? {
    val seconds = response.primitive("utc_offset_seconds")?.asLong() ?: return null
    return runCatching { UtcOffset(seconds = seconds.toInt()) }.getOrNull()
}

/**
 * The first [count] hours after [now], each with the time it falls on in [zone]: the clock's zone, so
 * the strip's hours match the clock's even when the weather is for a city elsewhere. Fewer when the
 * forecast doesn't reach that far.
 */
fun nextHours(hours: List<HourForecast>, now: Instant, zone: TimeZone, count: Int): List<HourSlot> =
    hours.asSequence()
        .filter { it.time > now }
        .sortedBy { it.time }
        .take(count)
        .map { HourSlot(it.time.toLocalDateTime(zone), it) }
        .toList()

/** The hour's label: "14h" on a 24-hour clock, "2 pm" (in [locale]'s words) on a 12-hour one. */
fun hourLabel(local: LocalDateTime, use24h: Boolean, locale: PlatformLocale): String =
    if (use24h) "${local.hour}h" else DateTimePattern("h a", locale).format(local)
