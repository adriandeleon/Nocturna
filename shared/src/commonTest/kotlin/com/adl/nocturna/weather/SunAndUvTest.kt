package com.adl.nocturna.weather

import kotlinx.datetime.LocalDate
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.time.Instant

class SunAndUvTest {
    private val body = """
        {"utc_offset_seconds":3600,
         "current":{"temperature_2m":12.3,"weather_code":2,"is_day":1,"uv_index":3.15},
         "daily":{"time":["2026-11-16","2026-11-17"],"weather_code":[2,3],
                  "temperature_2m_max":[18.0,16.0],"temperature_2m_min":[7.0,6.0],
                  "sunrise":["2026-11-16T08:02","2026-11-17T08:03"],"sunset":["2026-11-16T17:56",null]}}
    """.trimIndent()

    @Test fun sunriseAndSunsetAreReadWithTheResponsesOffset() {
        val days = parseWeather(body, 40.4, -3.7, 0L).days
        assertEquals(LocalDate(2026, 11, 16), days[0].date)
        assertEquals(Instant.parse("2026-11-16T07:02:00Z"), days[0].sunrise)
        assertEquals(Instant.parse("2026-11-16T16:56:00Z"), days[0].sunset)
        // A missing sunset leaves the day in, without it.
        assertEquals(Instant.parse("2026-11-17T07:03:00Z"), days[1].sunrise)
        assertNull(days[1].sunset)
    }

    @Test fun uvIndexIsReadFromTheCurrentConditions() = assertEquals(3.15, parseWeather(body, 40.4, -3.7, 0L).uvIndex)

    @Test fun olderResponsesWithoutThemStillRead() {
        val w = parseWeather("""{"current":{"temperature_2m":18,"weather_code":3,"is_day":1}}""", 0.0, 0.0, 0L)
        assertNull(w.uvIndex)
    }
}
