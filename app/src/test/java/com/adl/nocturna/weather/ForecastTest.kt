package com.adl.nocturna.weather

import org.json.JSONObject
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate
import java.time.LocalDateTime

class ForecastTest {
    private fun day(date: String, high: Double = 24.0, low: Double = 12.0, code: Int = 3) =
        DayForecast(LocalDate.parse(date), code, high, low)

    private val days = listOf(day("2026-11-13"), day("2026-11-14", 20.0, 11.0, 61), day("2026-11-15"))

    @Test fun showsTodayDuringTheDay() {
        val pick = pickForecast(days, LocalDateTime.parse("2026-11-13T09:00"))!!
        assertEquals(LocalDate.parse("2026-11-13"), pick.day.date)
        assertFalse(pick.tomorrow)
    }

    @Test fun switchesToTomorrowAtSixInTheEvening() {
        assertFalse(pickForecast(days, LocalDateTime.parse("2026-11-13T17:59"))!!.tomorrow)
        val pick = pickForecast(days, LocalDateTime.parse("2026-11-13T18:00"))!!
        assertEquals(LocalDate.parse("2026-11-14"), pick.day.date)
        assertTrue(pick.tomorrow)
    }

    @Test fun afterMidnightTheNewDayIsToday() {
        val pick = pickForecast(days, LocalDateTime.parse("2026-11-14T02:00"))!!
        assertEquals(LocalDate.parse("2026-11-14"), pick.day.date)
        assertFalse(pick.tomorrow)
    }

    @Test fun nothingWhenTheDayIsMissing() {
        assertNull(pickForecast(days, LocalDateTime.parse("2026-11-15T20:00")))
        assertNull(pickForecast(emptyList(), LocalDateTime.parse("2026-11-13T09:00")))
    }

    @Test fun highLowInCelsiusAndFahrenheit() {
        val d = day("2026-11-13", high = 23.6, low = -0.4)
        assertEquals("↑24° ↓0°", d.highLowText(fahrenheit = false))
        assertEquals("↑74° ↓31°", d.highLowText(fahrenheit = true))
    }

    @Test fun parsesOpenMeteoDaily() {
        val json = JSONObject(
            """
            {"daily": {
              "time": ["2026-11-13", "2026-11-14", "2026-11-15"],
              "weather_code": [3, 61, null],
              "temperature_2m_max": [24.1, 19.8, 21.0],
              "temperature_2m_min": [12.3, 10.9, 9.0]
            }}
            """
        )
        assertEquals(
            listOf(day("2026-11-13", 24.1, 12.3, 3), day("2026-11-14", 19.8, 10.9, 61)),
            parseDaily(json),
        )
    }

    @Test fun noDailyBlockMeansNoForecast() {
        assertEquals(emptyList<DayForecast>(), parseDaily(JSONObject("""{"current": {"temperature_2m": 18}}""")))
        assertEquals(emptyList<DayForecast>(), parseDaily(JSONObject("""{"daily": {"time": ["2026-11-13"]}}""")))
    }

    @Test fun badDateIsSkipped() =
        assertEquals(
            listOf(day("2026-11-14", 20.0, 11.0, 61)),
            parseDaily(
                JSONObject(
                    """{"daily": {"time": ["soon", "2026-11-14"], "weather_code": [3, 61],
                    "temperature_2m_max": [24, 20], "temperature_2m_min": [12, 11]}}"""
                )
            ),
        )
}
