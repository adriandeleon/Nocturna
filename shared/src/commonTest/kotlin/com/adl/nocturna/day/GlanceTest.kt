package com.adl.nocturna.day

import com.adl.nocturna.data.Countdown
import com.adl.nocturna.data.Settings
import com.adl.nocturna.text.DateTimePattern
import com.adl.nocturna.text.spanishTestLocale
import com.adl.nocturna.text.testLocale
import com.adl.nocturna.weather.AirQuality
import com.adl.nocturna.weather.DayForecast
import com.adl.nocturna.weather.HourForecast
import com.adl.nocturna.weather.Weather
import kotlinx.datetime.LocalDate
import kotlinx.datetime.TimeZone
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.time.Duration.Companion.hours
import kotlin.time.Duration.Companion.minutes
import kotlin.time.Instant

class GlanceTest {
    private val now = Instant.parse("2026-11-16T12:10:00Z")
    private val zone = TimeZone.UTC

    private fun hour(h: Int, code: Int = 1, rain: Int? = 0) =
        HourForecast(Instant.parse("2026-11-16T00:00:00Z") + h.hours, code, isDay = true, temperatureC = 20.0, precipitationPercent = rain)

    private val dryDay = (13..20).map { hour(it) }

    @Test fun rainOnTheWay() {
        val hours = dryDay.map { if (it.time == hour(15).time) hour(15, code = 61) else it }
        assertEquals(RainAlert.Starts(hour(15).time, snow = false), rainAlert(currentCode = 2, hours, now))
    }

    @Test fun aLikelyChanceCountsAsRain() {
        val hours = dryDay.map { if (it.time == hour(14).time) hour(14, rain = 60) else it }
        assertEquals(RainAlert.Starts(hour(14).time, snow = false), rainAlert(2, hours, now))
    }

    @Test fun rainBeyondSixHoursIsNotMentioned() {
        val hours = dryDay.map { if (it.time == hour(19).time) hour(19, code = 63) else it }
        assertNull(rainAlert(2, hours, now))
    }

    @Test fun rainingNowSaysWhenItStops() {
        val hours = listOf(hour(13, 61), hour(14, 61), hour(15, 3, rain = 10))
        assertEquals(RainAlert.Stops(hour(15).time, snow = false), rainAlert(currentCode = 63, hours, now))
    }

    @Test fun rainAllAfternoonSaysNothingMore() = assertNull(rainAlert(63, (13..18).map { hour(it, 63) }, now))

    @Test fun snow() {
        val hours = dryDay.map { if (it.time == hour(16).time) hour(16, code = 73) else it }
        val alert = rainAlert(3, hours, now)
        assertEquals(RainAlert.Starts(hour(16).time, snow = true), alert)
        assertEquals("Snow from 16:00", rainAlertText(alert!!, "16:00", testLocale))
    }

    @Test fun rainWords() {
        assertEquals("Rain from 15:00", rainAlertText(RainAlert.Starts(now, false), "15:00", testLocale))
        assertEquals("Rain until 17:00", rainAlertText(RainAlert.Stops(now, false), "17:00", testLocale))
        assertEquals("Lluvia desde las 15:00", rainAlertText(RainAlert.Starts(now, false), "15:00", spanishTestLocale))
        assertEquals("Lluvia hasta la 1:00 p.m.", rainAlertText(RainAlert.Stops(now, false), "1:00 p.m.", spanishTestLocale))
    }

    private val days = listOf(
        DayForecast(LocalDate(2026, 11, 16), 1, 20.0, 10.0, Instant.parse("2026-11-16T07:00:00Z"), Instant.parse("2026-11-16T17:48:00Z")),
        DayForecast(LocalDate(2026, 11, 17), 1, 20.0, 10.0, Instant.parse("2026-11-17T07:01:00Z"), Instant.parse("2026-11-17T17:47:00Z")),
    )

    @Test fun sunsetWithTheLightLeft() {
        val event = nextSunEvent(days, now)
        assertEquals(SunEvent.Sunset(Instant.parse("2026-11-16T17:48:00Z"), (5.hours + 38.minutes).inWholeMilliseconds), event)
        assertEquals("Sunset 17:48 · 5 h 38 m of light", sunText(event!!, "17:48", testLocale))
        assertEquals("Atardecer 17:48 · 5 h 38 min de luz", sunText(event, "17:48", spanishTestLocale))
    }

    @Test fun afterDarkTheNextSunrise() {
        val event = nextSunEvent(days, Instant.parse("2026-11-16T20:00:00Z"))
        assertEquals(SunEvent.Sunrise(Instant.parse("2026-11-17T07:01:00Z")), event)
        assertEquals("Sunrise 07:01", sunText(event!!, "07:01", testLocale))
    }

    @Test fun noSunTimesNoSunLine() = assertNull(nextSunEvent(days.map { it.copy(sunrise = null, sunset = null) }, now))

    private val today = LocalDate(2026, 11, 16)

    @Test fun countdownsNearestFirstAndPastOnesGone() {
        val countdowns = listOf(Countdown("Trip", "2026-12-20"), Countdown("Gone", "2026-11-01"), Countdown("Party", "2026-11-17"))
        assertEquals(listOf(CountdownDay("Party", 1), CountdownDay("Trip", 34)), countdownDays(countdowns, null, today))
    }

    @Test fun theNextDayOffCountsDownToo() {
        val daysOff = mapOf(LocalDate(2026, 11, 16) to "Holiday", LocalDate(2026, 11, 20) to "Vacation")
        assertEquals(listOf(CountdownDay("Vacation", 4)), countdownDays(emptyList(), daysOff, today))
        // Not twice when one of your own counts down to it already.
        assertEquals(listOf(CountdownDay("Trip", 4)), countdownDays(listOf(Countdown("Trip", "2026-11-20")), daysOff, today))
    }

    @Test fun onlyTheNearestTwo() {
        val countdowns = listOf(Countdown("C", "2026-11-30"), Countdown("A", "2026-11-18"), Countdown("B", "2026-11-20"))
        assertEquals(listOf("A", "B"), countdownDays(countdowns, null, today).map { it.label })
    }

    @Test fun countdownWords() {
        assertEquals("Vacation in 12 days", countdownText(CountdownDay("Vacation", 12), testLocale))
        assertEquals("Vacation tomorrow", countdownText(CountdownDay("Vacation", 1), testLocale))
        assertEquals("Vacation today!", countdownText(CountdownDay("Vacation", 0), testLocale))
        assertEquals("Vacaciones en 12 días", countdownText(CountdownDay("Vacaciones", 12), spanishTestLocale))
    }

    @Test fun glanceLineInOrderAndOnlyWhatIsOn() {
        val weather = Weather(
            temperatureC = 20.0, code = 2, isDay = true, fetchedAt = 0L, lat = 0.0, lon = 0.0,
            days = days, hours = dryDay.map { if (it.time == hour(15).time) hour(15, 61) else it }, uvIndex = 6.6,
        )
        val all = Settings(
            dayRainAlert = true, daySunTimes = true, dayUvIndex = true, dayAirQuality = true,
            countdowns = listOf(Countdown("Trip", "2026-11-20")),
        )
        val time = DateTimePattern("HH:mm", testLocale)
        val air = AirQuality(42, 0L, 0.0, 0.0)
        assertEquals(
            listOf("Rain from 15:00", "Sunset 17:48 · 5 h 38 m of light", "UV 7 · High", "Air: good (42)", "Trip in 4 days"),
            glanceItems(all, weather, air, null, now, zone, time, testLocale),
        )
        // With the weather off, only the countdowns are left.
        assertEquals(listOf("Trip in 4 days"), glanceItems(all.copy(showWeather = false), weather, air, null, now, zone, time, testLocale))
        // Nothing turned on, nothing to show.
        assertEquals(emptyList(), glanceItems(Settings(), weather, air, null, now, zone, time, testLocale))
        // No UV of 0.
        assertEquals(false, glanceItems(all, weather.copy(uvIndex = 0.2), null, null, now, zone, time, testLocale).any { it.startsWith("UV") })
        // No UV after dark.
        val night = weather.copy(isDay = false)
        assertEquals(false, glanceItems(all, night, null, null, now, zone, time, testLocale).any { it.startsWith("UV") })
    }
}
