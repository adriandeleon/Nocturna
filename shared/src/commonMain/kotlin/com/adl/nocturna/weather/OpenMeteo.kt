package com.adl.nocturna.weather

import kotlin.math.abs
import kotlin.math.roundToLong

/*
 * Open-Meteo's two endpoints, free and without a key: the forecast and the city search.
 */

/** Current conditions and three days of forecast, in the place's own time zone, so "today" and "tomorrow" are there. */
internal fun forecastUrl(lat: Double, lon: Double): String =
    "https://api.open-meteo.com/v1/forecast?latitude=${fixed4(lat)}&longitude=${fixed4(lon)}" +
        "&current=temperature_2m,weather_code,is_day" +
        "&daily=weather_code,temperature_2m_max,temperature_2m_min&forecast_days=3&timezone=auto"

/** The best match for [query], with place names in [language] (an ISO 639 code such as "es"). */
internal fun geocodeUrl(query: String, language: String): String =
    "https://geocoding-api.open-meteo.com/v1/search?name=${formEncode(query.trim())}&count=1&language=${formEncode(language)}"

/** [x] with four decimals, "25.6866" or "-100.3161": about 10 m, plenty for the weather. */
internal fun fixed4(x: Double): String {
    val scaled = (abs(x) * 10_000).roundToLong()
    val sign = if (x < 0 && scaled != 0L) "-" else ""
    return "$sign${scaled / 10_000}.${(scaled % 10_000).toString().padStart(4, '0')}"
}

/** [text] for a URL query, as Java's URLEncoder does it: UTF-8, "+" for spaces, "%XX" for the rest. */
internal fun formEncode(text: String): String = buildString {
    for (byte in text.encodeToByteArray()) {
        val b = byte.toInt() and 0xFF
        val c = b.toChar()
        when {
            c in 'a'..'z' || c in 'A'..'Z' || c in '0'..'9' || c in ".-*_" -> append(c)
            c == ' ' -> append('+')
            else -> append('%').append(HEX[b shr 4]).append(HEX[b and 0xF])
        }
    }
}

private const val HEX = "0123456789ABCDEF"
