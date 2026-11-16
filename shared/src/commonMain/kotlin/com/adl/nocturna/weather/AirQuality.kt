package com.adl.nocturna.weather

import com.adl.nocturna.data.asLong
import com.adl.nocturna.data.getObject
import com.adl.nocturna.data.primitive
import com.adl.nocturna.text.PlatformLocale
import com.adl.nocturna.text.strings
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.jsonObject

/** The air quality at [lat], [lon] as the US Air Quality Index (0 and up), from Open-Meteo's CAMS data. */
data class AirQuality(val usAqi: Int, val fetchedAt: Long, val lat: Double, val lon: Double) {
    val level: AirLevel get() = airLevel(usAqi)
}

/** The US AQI's bands, from good (0–50) to hazardous (301 and up). */
enum class AirLevel { GOOD, MODERATE, SENSITIVE, UNHEALTHY, VERY_UNHEALTHY, HAZARDOUS }

fun airLevel(usAqi: Int): AirLevel = when {
    usAqi <= 50 -> AirLevel.GOOD
    usAqi <= 100 -> AirLevel.MODERATE
    usAqi <= 150 -> AirLevel.SENSITIVE
    usAqi <= 200 -> AirLevel.UNHEALTHY
    usAqi <= 300 -> AirLevel.VERY_UNHEALTHY
    else -> AirLevel.HAZARDOUS
}

/** Open-Meteo's air quality response: its current US AQI, or null when it has none (out at sea, say). */
fun parseAirQuality(body: String, lat: Double, lon: Double, fetchedAt: Long): AirQuality? {
    val current = Json.parseToJsonElement(body).jsonObject.getObject("current")
    val aqi = current.primitive("us_aqi")?.asLong()?.toInt() ?: return null
    return AirQuality(aqi.coerceAtLeast(0), fetchedAt, lat, lon)
}

/** What day mode's desk clock shows, e.g. "Air: good (42)". */
fun airQualityText(air: AirQuality, locale: PlatformLocale): String {
    val t = strings(locale)
    val level = when (air.level) {
        AirLevel.GOOD -> t.airGood
        AirLevel.MODERATE -> t.airModerate
        AirLevel.SENSITIVE -> t.airSensitive
        AirLevel.UNHEALTHY -> t.airUnhealthy
        AirLevel.VERY_UNHEALTHY -> t.airVeryUnhealthy
        AirLevel.HAZARDOUS -> t.airHazardous
    }
    return t.airQuality(level, air.usAqi)
}

/** The UV index's bands, from low (0–2) to extreme (11 and up), as the WHO names them. */
enum class UvLevel { LOW, MODERATE, HIGH, VERY_HIGH, EXTREME }

fun uvLevel(uv: Double): UvLevel = when {
    uv < 2.5 -> UvLevel.LOW
    uv < 5.5 -> UvLevel.MODERATE
    uv < 7.5 -> UvLevel.HIGH
    uv < 10.5 -> UvLevel.VERY_HIGH
    else -> UvLevel.EXTREME
}

/** "UV 7 · High", rounded to a whole number as the index is usually given. */
fun uvText(uv: Double, locale: PlatformLocale): String {
    val t = strings(locale)
    val level = when (uvLevel(uv)) {
        UvLevel.LOW -> t.uvLow
        UvLevel.MODERATE -> t.uvModerate
        UvLevel.HIGH -> t.uvHigh
        UvLevel.VERY_HIGH -> t.uvVeryHigh
        UvLevel.EXTREME -> t.uvExtreme
    }
    return t.uvIndex(kotlin.math.round(uv).toInt().coerceAtLeast(0), level)
}
