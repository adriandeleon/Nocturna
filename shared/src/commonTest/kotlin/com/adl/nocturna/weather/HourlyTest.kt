package com.adl.nocturna.weather

import com.adl.nocturna.text.testLocale
import com.adl.nocturna.ui.Glyph
import kotlinx.datetime.LocalDate
import kotlinx.datetime.LocalDateTime
import kotlinx.datetime.TimeZone
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.jsonObject
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue
import kotlin.time.Duration.Companion.hours
import kotlin.time.Instant

class HourlyTest {
    private fun json(text: String) = Json.parseToJsonElement(text).jsonObject

    private val recorded = parseWeather(NEW_YORK_DST_RESPONSE, 40.7128, -74.006, fetchedAt = 0L)
    private val newYork = TimeZone.of("America/New_York")

    private fun labels(slots: List<HourSlot>) = slots.map { hourLabel(it.local, use24h = true, testLocale) }

    @Test fun parsesEveryHourOfTheRecordedResponse() {
        assertEquals(72, recorded.hours.size)
        // Open-Meteo's local times carry its one offset, EST, even on the last day of summer time.
        assertEquals(Instant.parse("2026-10-31T05:00:00Z"), recorded.hours.first().time)
        assertEquals(Instant.parse("2026-11-03T04:00:00Z"), recorded.hours.last().time)
        assertTrue(recorded.hours.zipWithNext().all { (a, b) -> b.time - a.time == 1.hours })
        assertEquals(10.2, recorded.hours.first().temperatureC)
        assertEquals(Glyph.MOON, recorded.hours.first().glyph)
    }

    @Test fun theNightClocksDataIsUnchanged() {
        assertEquals(8.2, recorded.temperatureC)
        assertEquals(0, recorded.code)
        assertEquals(
            listOf(LocalDate(2026, 10, 31), LocalDate(2026, 11, 1), LocalDate(2026, 11, 2)),
            recorded.days.map { it.date },
        )
    }

    @Test fun nextSixHoursStartAfterNow() {
        // 9:20 EST on 2 November.
        val slots = nextHours(recorded.hours, Instant.parse("2026-11-02T14:20:00Z"), newYork, 6)
        assertEquals(listOf("10h", "11h", "12h", "13h", "14h", "15h"), labels(slots))
        assertEquals(Instant.parse("2026-11-02T15:00:00Z"), slots.first().forecast.time)
    }

    @Test fun anHourOnTheDotIsAlreadyGone() {
        val slots = nextHours(recorded.hours, Instant.parse("2026-11-02T15:00:00Z"), newYork, 2)
        assertEquals(listOf("11h", "12h"), labels(slots))
    }

    @Test fun rollsOverAtMidnight() {
        // 22:30 on 1 November in Bogotá, which keeps UTC-5 all year.
        val slots = nextHours(recorded.hours, Instant.parse("2026-11-02T03:30:00Z"), TimeZone.of("America/Bogota"), 6)
        assertEquals(listOf("23h", "0h", "1h", "2h", "3h", "4h"), labels(slots))
        assertEquals(LocalDate(2026, 11, 1), slots[0].local.date)
        assertEquals(LocalDate(2026, 11, 2), slots[1].local.date)
        // "2026-11-02T00:00" in the response.
        assertEquals(8.9, slots[1].forecast.temperatureC)
    }

    @Test fun summerTimeEndingShowsOneOClockTwice() {
        // 0:30 EDT on 1 November; at 2:00 EDT the clocks go back to 1:00 EST.
        val slots = nextHours(recorded.hours, Instant.parse("2026-11-01T04:30:00Z"), newYork, 6)
        assertEquals(listOf("1h", "1h", "2h", "3h", "4h", "5h"), labels(slots))
        // "2026-11-01T00:00" and "T01:00" in the response: one real hour apart, not the same hour.
        assertEquals(listOf(7.3, 7.0), slots.take(2).map { it.forecast.temperatureC })
        assertTrue(slots.zipWithNext().all { (a, b) -> b.forecast.time - a.forecast.time == 1.hours })
    }

    @Test fun summerTimeStartingSkipsTwoOClock() {
        // Madrid on 29 March 2026: at 2:00 CET the clocks go forward to 3:00 CEST.
        val response = json(
            """
            {"utc_offset_seconds": 3600, "hourly": {
              "time": ["2026-03-29T01:00", "2026-03-29T02:00", "2026-03-29T03:00"],
              "temperature_2m": [9.0, 8.5, 8.1], "weather_code": [0, 0, 0], "is_day": [0, 0, 0],
              "precipitation_probability": [0, 0, 0]
            }}
            """
        )
        val slots = nextHours(parseHourly(response), Instant.parse("2026-03-28T23:30:00Z"), TimeZone.of("Europe/Madrid"), 6)
        assertEquals(listOf("1h", "3h", "4h"), labels(slots))
    }

    @Test fun twelveHourLabels() {
        val slots = nextHours(recorded.hours, Instant.parse("2026-11-02T03:30:00Z"), TimeZone.of("America/Bogota"), 3)
        assertEquals(
            listOf("11 pm", "12 am", "1 am"),
            slots.map { hourLabel(it.local, use24h = false, testLocale).lowercase() },
        )
        assertEquals("12 pm", hourLabel(LocalDateTime(2026, 11, 2, 12, 0), use24h = false, testLocale).lowercase())
    }

    @Test fun rainShowsFromThirtyPercent() {
        val response = json(
            """
            {"utc_offset_seconds": 0, "hourly": {
              "time": ["2026-11-15T10:00", "2026-11-15T11:00", "2026-11-15T12:00", "2026-11-15T13:00"],
              "temperature_2m": [12, 12, 13, 13], "weather_code": [3, 61, 80, 3],
              "precipitation_probability": [29, 30, 85, null]
            }}
            """
        )
        assertEquals(listOf(null, "30%", "85%", null), parseHourly(response).map { it.rainText })
    }

    @Test fun missingValuesDropTheHour() {
        val response = json(
            """
            {"utc_offset_seconds": 0, "hourly": {
              "time": ["2026-11-15T10:00", "later", "2026-11-15T12:00", "2026-11-15T13:00"],
              "temperature_2m": [12, 12, null, 13], "weather_code": [3, 3, 3, 3]
            }}
            """
        )
        val hours = parseHourly(response)
        assertEquals(listOf(Instant.parse("2026-11-15T10:00:00Z"), Instant.parse("2026-11-15T13:00:00Z")), hours.map { it.time })
        // Without is_day an hour counts as day; without the chance of rain it isn't shown.
        assertTrue(hours.all { it.isDay && it.rainText == null })
    }

    @Test fun noOffsetOrNoHourlyBlockMeansNoHours() {
        assertEquals(emptyList(), parseHourly(json("""{"hourly": {"time": ["2026-11-15T10:00"], "temperature_2m": [1], "weather_code": [0]}}""")))
        assertEquals(emptyList(), parseHourly(json("""{"utc_offset_seconds": 0, "current": {"temperature_2m": 18}}""")))
    }

    @Test fun fewerHoursWhenTheForecastRunsOut() {
        val slots = nextHours(recorded.hours, Instant.parse("2026-11-03T01:10:00Z"), newYork, 6)
        assertEquals(3, slots.size)
        assertNull(nextHours(recorded.hours, Instant.parse("2026-11-04T00:00:00Z"), newYork, 6).firstOrNull())
    }
}
