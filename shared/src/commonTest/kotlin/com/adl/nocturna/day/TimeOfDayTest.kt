package com.adl.nocturna.day

import com.adl.nocturna.weather.DayForecast
import kotlin.test.assertEquals
import kotlin.test.Test
import kotlin.time.Instant
import kotlinx.datetime.LocalDate
import kotlinx.datetime.LocalTime
import kotlinx.datetime.TimeZone

class TimeOfDayTest {
    private fun at(h: Int, m: Int = 0, sun: SunTimes = SunTimes.DEFAULT) = skyMix(LocalTime(h, m), sun)

    @Test fun eachLookHoldsInItsOwnHours() {
        assertEquals(1f, at(3).weight(TimeOfDay.NIGHT))
        assertEquals(1f, at(8, 30).weight(TimeOfDay.MORNING))
        assertEquals(1f, at(13).weight(TimeOfDay.MIDDAY))
        assertEquals(1f, at(17, 30).weight(TimeOfDay.AFTERNOON))
        assertEquals(1f, at(22).weight(TimeOfDay.NIGHT))
    }

    @Test fun dawnBlendsNightIntoMorning() {
        val dawn = at(6, 15)
        assertEquals(0.5f, dawn.weight(TimeOfDay.NIGHT), 0.001f)
        assertEquals(0.5f, dawn.weight(TimeOfDay.MORNING), 0.001f)
    }

    @Test fun duskBlendsAfternoonIntoNight() {
        val dusk = at(19, 0)
        assertEquals(TimeOfDay.AFTERNOON, dusk.from)
        assertEquals(TimeOfDay.NIGHT, dusk.to)
        assertEquals(1f / 3f, dusk.t, 0.001f)
    }

    @Test fun weightsAlwaysAddUpToOne() {
        for (sun in listOf(SunTimes.DEFAULT, NOVEMBER, SunTimes(4 * 60f, 22 * 60f))) {
            for (minute in 0 until 24 * 60 step 7) {
                val mix = skyMix(LocalTime(minute / 60, minute % 60), sun)
                assertEquals(1f, TimeOfDay.entries.sumOf { mix.weight(it).toDouble() }.toFloat(), 0.001f, "at minute $minute, $sun")
            }
        }
    }

    @Test fun lastMinuteOfTheDayIsNight() = assertEquals(1f, at(23, 59).weight(TimeOfDay.NIGHT))

    // Sunrise 07:05, sunset 17:50: a November day.
    private val NOVEMBER = SunTimes(7 * 60 + 5f, 17 * 60 + 50f)

    @Test fun duskComesWithAnEarlySunset() {
        assertEquals(1f, at(17, 0, NOVEMBER).weight(TimeOfDay.AFTERNOON))
        assertEquals(0.5f, at(17, 50, NOVEMBER).weight(TimeOfDay.NIGHT), 0.001f)
        assertEquals(1f, at(18, 35, NOVEMBER).weight(TimeOfDay.NIGHT))
    }

    @Test fun dawnComesWithALateSunrise() {
        assertEquals(1f, at(6, 20, NOVEMBER).weight(TimeOfDay.NIGHT))
        assertEquals(0.5f, at(7, 5, NOVEMBER).weight(TimeOfDay.MORNING), 0.001f)
        assertEquals(1f, at(8, 0, NOVEMBER).weight(TimeOfDay.MORNING))
    }

    @Test fun middayFallsBetweenSunriseAndSunset() {
        assertEquals(1f, at(12, 30, NOVEMBER).weight(TimeOfDay.MIDDAY))
        assertEquals(1f, at(16, 0, NOVEMBER).weight(TimeOfDay.AFTERNOON))
    }

    private val zone = TimeZone.of("America/Monterrey")
    private val today = LocalDate(2026, 11, 19)
    private fun day(date: LocalDate, rise: String?, set: String?) =
        DayForecast(date, 1, 20.0, 10.0, rise?.let(Instant::parse), set?.let(Instant::parse))

    @Test fun sunTimesComeFromTodaysForecastInTheClocksZone() {
        val days = listOf(
            day(LocalDate(2026, 11, 18), "2026-11-18T13:04:00Z", "2026-11-18T23:51:00Z"),
            day(today, "2026-11-19T13:05:00Z", "2026-11-19T23:50:00Z"),
        )
        assertEquals(NOVEMBER, sunTimes(days, today, zone))
    }

    @Test fun sunTimesFallBackWhenTheForecastDoesNotSay() {
        assertEquals(SunTimes.DEFAULT, sunTimes(emptyList(), today, zone))
        assertEquals(SunTimes.DEFAULT, sunTimes(listOf(day(today, null, null)), today, zone))
        // Only yesterday's: stale data.
        assertEquals(SunTimes.DEFAULT, sunTimes(listOf(day(LocalDate(2026, 11, 18), "2026-11-18T13:04:00Z", "2026-11-18T23:51:00Z")), today, zone))
    }

    @Test fun sunTimesFallBackWhenTheDayIsTooShortOrRunsPastMidnight() {
        val arctic = TimeZone.of("Europe/Oslo")
        // Two hours of daylight, too short for the sky's looks.
        assertEquals(SunTimes.DEFAULT, sunTimes(listOf(day(today, "2026-11-19T10:00:00Z", "2026-11-19T12:00:00Z")), today, arctic))
        // Sunset at 23:50: dusk would run into tomorrow.
        assertEquals(SunTimes.DEFAULT, sunTimes(listOf(day(today, "2026-11-19T04:00:00Z", "2026-11-19T22:50:00Z")), today, arctic))
    }
}
