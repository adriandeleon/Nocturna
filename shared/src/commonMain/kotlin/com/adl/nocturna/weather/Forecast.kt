package com.adl.nocturna.weather

import com.adl.nocturna.data.asLong
import com.adl.nocturna.data.getDouble
import com.adl.nocturna.data.getInt
import com.adl.nocturna.data.getObject
import com.adl.nocturna.data.optArray
import com.adl.nocturna.data.optInt
import com.adl.nocturna.data.optObject
import com.adl.nocturna.data.optString
import com.adl.nocturna.data.primitiveAt
import com.adl.nocturna.ui.Glyph
import kotlinx.datetime.DateTimeUnit
import kotlinx.datetime.LocalDate
import kotlinx.datetime.LocalDateTime
import kotlinx.datetime.plus
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.doubleOrNull
import kotlinx.serialization.json.jsonObject
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
    /** Hourly forecasts, earliest first, for day mode's strip of the next few hours. */
    val hours: List<HourForecast> = emptyList(),
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
    val date = if (tomorrow) now.date.plus(1, DateTimeUnit.DAY) else now.date
    return days.firstOrNull { it.date == date }?.let { ForecastPick(it, tomorrow) }
}

/** Open-Meteo's forecast response: current conditions at [lat], [lon], the [parseDaily] days and [parseHourly] hours. */
fun parseWeather(body: String, lat: Double, lon: Double, fetchedAt: Long): Weather {
    val response = Json.parseToJsonElement(body).jsonObject
    val current = response.getObject("current")
    return Weather(
        temperatureC = current.getDouble("temperature_2m"),
        code = current.getInt("weather_code"),
        isDay = current.optInt("is_day", 1) == 1,
        fetchedAt = fetchedAt,
        lat = lat,
        lon = lon,
        days = parseDaily(response),
        hours = parseHourly(response),
    )
}

/**
 * Reads Open-Meteo's `daily` block: parallel arrays of dates, weather codes, highs and lows. Days with a
 * missing value (Open-Meteo sends null) are left out rather than shown wrong.
 */
fun parseDaily(response: JsonObject): List<DayForecast> {
    val daily = response.optObject("daily") ?: return emptyList()
    val time = daily.optArray("time") ?: return emptyList()
    val codes = daily.optArray("weather_code") ?: return emptyList()
    val highs = daily.optArray("temperature_2m_max") ?: return emptyList()
    val lows = daily.optArray("temperature_2m_min") ?: return emptyList()
    return time.indices.mapNotNull { i ->
        val date = time.primitiveAt(i)?.content ?: return@mapNotNull null
        val code = codes.primitiveAt(i)?.asLong()?.toInt() ?: return@mapNotNull null
        val high = highs.primitiveAt(i)?.doubleOrNull ?: return@mapNotNull null
        val low = lows.primitiveAt(i)?.doubleOrNull ?: return@mapNotNull null
        runCatching { DayForecast(LocalDate.parse(date), code, high, low) }.getOrNull()
    }
}

data class Place(val name: String, val lat: Double, val lon: Double)

/** The best match in Open-Meteo's city search response, or null when it found nothing. */
fun parsePlace(body: String): Place? {
    val results = Json.parseToJsonElement(body).jsonObject.optArray("results") ?: return null
    val r = results.firstOrNull() as? JsonObject ?: return null
    val label = listOfNotNull(r.optString("name").takeIf { it.isNotBlank() }, r.optString("country").takeIf { it.isNotBlank() })
        .joinToString(", ")
    return Place(label, r.getDouble("latitude"), r.getDouble("longitude"))
}

internal fun temperatureText(celsius: Double, fahrenheit: Boolean): String {
    val t = if (fahrenheit) celsius * 9 / 5 + 32 else celsius
    return "${t.roundToInt()}°"
}

internal fun weatherGlyph(code: Int, isDay: Boolean): Glyph = when (code) {
    0 -> if (isDay) Glyph.SUN else Glyph.MOON
    1, 2 -> if (isDay) Glyph.PARTLY_DAY else Glyph.PARTLY_NIGHT
    3 -> Glyph.CLOUD
    45, 48 -> Glyph.FOG
    in 51..67, in 80..82 -> Glyph.RAIN
    in 71..77, 85, 86 -> Glyph.SNOW
    in 95..99 -> Glyph.STORM
    else -> Glyph.CLOUD
}
