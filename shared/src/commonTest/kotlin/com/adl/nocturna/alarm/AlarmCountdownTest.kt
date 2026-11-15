package com.adl.nocturna.alarm

import com.adl.nocturna.text.testLocale
import kotlinx.datetime.LocalDateTime
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toInstant
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.time.Duration.Companion.hours
import kotlin.time.Duration.Companion.minutes
import kotlin.time.Duration.Companion.seconds
import kotlin.time.Instant

class AlarmCountdownTest {
    private val zone = TimeZone.of("America/Monterrey") // UTC-6, no daylight saving
    private fun at(s: String): Instant = LocalDateTime.parse(s).toInstant(zone)

    private val alarm = at("2026-11-16T06:30")

    @Test fun hoursAndMinutesRoundDownToFive() {
        assertEquals("in 7 h 20 m", timeToAlarmText(alarm, at("2026-11-15T23:10")))
        assertEquals("in 7 h 20 m", timeToAlarmText(alarm, at("2026-11-15T23:06")))
        assertEquals("in 7 h 15 m", timeToAlarmText(alarm, at("2026-11-15T23:14:59")))
        assertEquals("in 7 h 10 m", timeToAlarmText(alarm, at("2026-11-15T23:15:01")))
    }

    @Test fun wholeHoursHaveNoMinutes() = assertEquals("in 7 h", timeToAlarmText(alarm, at("2026-11-15T23:30")))

    @Test fun underAnHourShowsOnlyMinutes() {
        assertEquals("in 45 m", timeToAlarmText(alarm, at("2026-11-16T05:45")))
        assertEquals("in 45 m", timeToAlarmText(alarm, at("2026-11-16T05:41")))
        assertEquals("in 5 m", timeToAlarmText(alarm, at("2026-11-16T06:25")))
    }

    @Test fun lastFiveMinutesDontCountToZero() {
        assertEquals("in < 5 m", timeToAlarmText(alarm, alarm - 4.minutes - 59.seconds))
        assertEquals("in < 5 m", timeToAlarmText(alarm, alarm - 1.seconds))
    }

    @Test fun onlyWithinTwelveHours() {
        assertEquals("in 12 h", timeToAlarmText(alarm, alarm - 12.hours))
        assertNull(timeToAlarmText(alarm, alarm - 12.hours - 1.seconds))
    }

    @Test fun nothingOnceItHasRung() {
        assertNull(timeToAlarmText(alarm, alarm))
        assertNull(timeToAlarmText(alarm, alarm + 1.minutes))
    }

    @Test fun labelHasTheAlarmTimeInTheClocksZone() {
        val now = at("2026-11-15T23:10")
        assertEquals("06:30 · in 7 h 20 m", alarmCountdownLabel(alarm, now, zone, true, testLocale))
        assertEquals("6:30 am · in 7 h 20 m", alarmCountdownLabel(alarm, now, zone, false, testLocale)?.lowercase())
        // New York is an hour ahead of Monterrey in November; the countdown is the same.
        assertEquals("07:30 · in 7 h 20 m", alarmCountdownLabel(alarm, now, TimeZone.of("America/New_York"), true, testLocale))
    }

    @Test fun noLabelFurtherOff() = assertNull(alarmCountdownLabel(alarm, at("2026-11-15T18:00"), zone, true, testLocale))
}
