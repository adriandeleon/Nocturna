package com.adl.nocturna.alarm

import com.adl.nocturna.text.testLocale
import kotlinx.datetime.LocalDateTime
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toInstant
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue
import kotlin.time.Duration.Companion.hours
import kotlin.time.Duration.Companion.minutes
import kotlin.time.Duration.Companion.seconds
import kotlin.time.Instant

class BedtimeTest {
    private val zone = TimeZone.of("Europe/Madrid")
    private fun at(s: String, z: TimeZone = zone): Instant = LocalDateTime.parse(s).toInstant(z)

    private val alarm = at("2026-11-16T06:30")

    @Test fun remindsSleepHoursAndAQuarterBeforeTheAlarm() {
        assertEquals(at("2026-11-15T22:15"), bedtimeReminderAt(alarm, 8f, at("2026-11-15T18:00"), zone))
        assertEquals(at("2026-11-15T22:45"), bedtimeReminderAt(alarm, 7.5f, at("2026-11-15T18:00"), zone))
        assertEquals(at("2026-11-15T21:15"), bedtimeReminderAt(alarm, 9f, at("2026-11-15T18:00"), zone))
    }

    @Test fun offGivesNoReminder() = assertNull(bedtimeReminderAt(alarm, 0f, at("2026-11-15T18:00"), zone))

    @Test fun noAlarmGivesNoReminder() = assertNull(bedtimeReminderAt(null, 8f, at("2026-11-15T18:00"), zone))

    @Test fun notOnceTheTimeHasPassed() {
        assertNull(bedtimeReminderAt(alarm, 8f, at("2026-11-15T22:15"), zone))
        assertNull(bedtimeReminderAt(alarm, 8f, at("2026-11-15T23:30"), zone))
        assertNull(bedtimeReminderAt(alarm, 8f, alarm + 1.minutes, zone))
    }

    @Test fun onlyBeforeAMorningAlarm() {
        assertNull(bedtimeReminderAt(at("2026-11-16T14:00"), 8f, at("2026-11-15T18:00"), zone))
        assertNull(bedtimeReminderAt(at("2026-11-16T12:00"), 8f, at("2026-11-15T18:00"), zone))
        assertEquals(at("2026-11-16T03:15"), bedtimeReminderAt(at("2026-11-16T11:30"), 8f, at("2026-11-15T18:00"), zone))
    }

    @Test fun anAlarmDaysAwayIsRemindedTheEveningBefore() =
        assertEquals(at("2026-11-17T22:15"), bedtimeReminderAt(at("2026-11-18T06:30"), 8f, at("2026-11-16T07:00"), zone))

    @Test fun morningIsReadInTheGivenZone() {
        // 06:30 in Madrid is 23:30 the evening before in Monterrey: not a morning alarm there.
        val monterrey = TimeZone.of("America/Monterrey")
        assertNull(bedtimeReminderAt(alarm, 8f, at("2026-11-15T18:00"), monterrey))
    }

    @Test fun acrossTheClockChange() {
        // Clocks go back an hour at 03:00 on 25 October 2026 in Madrid: 8 h of sleep is still 8 h.
        val alarm = at("2026-10-25T06:30")
        assertEquals(alarm - 8.hours - 15.minutes, bedtimeReminderAt(alarm, 8f, at("2026-10-24T18:00"), zone))
        assertEquals(at("2026-10-24T23:15"), bedtimeReminderAt(alarm, 8f, at("2026-10-24T18:00"), zone))
    }

    @Test fun firesAroundItsTime() {
        val bedtime = at("2026-11-15T22:15")
        assertTrue(shouldRemindBedtime(alarm, 8f, bedtime, zone, clockShowing = false))
        assertTrue(shouldRemindBedtime(alarm, 8f, bedtime - 30.seconds, zone, false))
        assertTrue(shouldRemindBedtime(alarm, 8f, bedtime + 29.minutes, zone, false))
        assertFalse(shouldRemindBedtime(alarm, 8f, bedtime + 30.minutes, zone, false))
        assertFalse(shouldRemindBedtime(alarm, 8f, bedtime - 2.minutes, zone, false))
    }

    @Test fun notWhenTheClockIsShowing() =
        assertFalse(shouldRemindBedtime(alarm, 8f, at("2026-11-15T22:15"), zone, clockShowing = true))

    @Test fun notForAnAlarmThatMoved() {
        // The alarm was moved to 07:30 after the reminder was set for 22:15.
        assertFalse(shouldRemindBedtime(at("2026-11-16T07:30"), 8f, at("2026-11-15T22:15"), zone, false))
        assertFalse(shouldRemindBedtime(null, 8f, at("2026-11-15T22:15"), zone, false))
        assertFalse(shouldRemindBedtime(alarm, 0f, at("2026-11-15T22:15"), zone, false))
    }

    @Test fun textNamesTheSleepAndTheAlarm() {
        assertEquals("Bedtime: 8 h before your 06:30 alarm", bedtimeReminderText(alarm, 8f, zone, true, testLocale))
        assertEquals(
            "bedtime: 6 h 30 m before your 6:30 am alarm",
            bedtimeReminderText(alarm, 6.5f, zone, false, testLocale).lowercase(),
        )
    }

    @Test fun sleepLabels() {
        assertEquals("Off", sleepHoursLabel(0f, testLocale))
        assertEquals("7 h", sleepHoursLabel(7f, testLocale))
        assertEquals("8 h 30 m", sleepHoursLabel(8.5f, testLocale))
    }
}
