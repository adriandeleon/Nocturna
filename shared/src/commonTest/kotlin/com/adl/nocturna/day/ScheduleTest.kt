package com.adl.nocturna.day

import com.adl.nocturna.data.DayMode
import com.adl.nocturna.data.Settings
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue
import kotlin.test.Test
import kotlinx.datetime.LocalDateTime
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toInstant
import kotlin.time.Duration.Companion.minutes
import kotlin.time.Instant

class ScheduleTest {
    private val monterrey = TimeZone.of("America/Monterrey") // UTC-6, no daylight saving
    private val berlin = TimeZone.of("Europe/Berlin")

    // 2026-11-13 is a Friday.
    private fun at(text: String, zone: TimeZone = monterrey): Instant = LocalDateTime.parse(text).toInstant(zone)

    private fun scheduled(start: String, end: String, days: Int = Settings.WEEKDAYS) = Settings(
        dayMode = DayMode.SCHEDULED,
        dayModeStart = minutes(start),
        dayModeEnd = minutes(end),
        dayModeDays = days,
    )

    private fun minutes(hhmm: String) = hhmm.substring(0, 2).toInt() * 60 + hhmm.substring(3).toInt()

    private val office = scheduled("09:00", "18:00")

    @Test fun offAndOnIgnoreTheSchedule() {
        assertFalse(isDayModeActive(office.copy(dayMode = DayMode.OFF), at("2026-11-13T10:00"), monterrey))
        assertTrue(isDayModeActive(office.copy(dayMode = DayMode.ON), at("2026-11-14T03:00"), monterrey))
    }

    @Test fun startIsInsideAndEndIsOutside() {
        assertFalse(isDayModeActive(office, at("2026-11-13T08:59:59"), monterrey))
        assertTrue(isDayModeActive(office, at("2026-11-13T09:00"), monterrey))
        assertTrue(isDayModeActive(office, at("2026-11-13T17:59:59"), monterrey))
        assertFalse(isDayModeActive(office, at("2026-11-13T18:00"), monterrey))
    }

    @Test fun daysOffStayOnTheClock() {
        assertFalse(isDayModeActive(office, at("2026-11-14T10:00"), monterrey)) // Saturday
        assertFalse(isDayModeActive(office, at("2026-11-15T10:00"), monterrey)) // Sunday
        assertTrue(isDayModeActive(office, at("2026-11-16T10:00"), monterrey)) // Monday
        assertFalse(isDayModeActive(office.copy(dayModeDays = 0), at("2026-11-16T10:00"), monterrey))
    }

    @Test fun windowAcrossMidnightBelongsToTheDayItStarts() {
        val nights = scheduled("22:00", "06:00", days = 1 shl 4) // Fridays only
        assertTrue(isDayModeActive(nights, at("2026-11-13T22:00"), monterrey)) // Friday night
        assertTrue(isDayModeActive(nights, at("2026-11-14T05:59"), monterrey)) // into Saturday
        assertFalse(isDayModeActive(nights, at("2026-11-14T06:00"), monterrey))
        assertFalse(isDayModeActive(nights, at("2026-11-14T23:00"), monterrey)) // Saturday night is not Friday's
        assertFalse(isDayModeActive(nights, at("2026-11-13T05:00"), monterrey)) // Thursday's night wasn't set
    }

    @Test fun sameStartAndEndKeepsItOnAllDay() {
        val allDay = scheduled("00:00", "00:00", days = 0b110_0000) // weekends
        assertTrue(isDayModeActive(allDay, at("2026-11-14T00:00"), monterrey))
        assertTrue(isDayModeActive(allDay, at("2026-11-15T23:59"), monterrey))
        assertFalse(isDayModeActive(allDay, at("2026-11-16T00:00"), monterrey))
    }

    @Test fun readsTheTimesInTheZoneItIsGiven() {
        val instant = at("2026-11-13T10:00") // 17:00 in Berlin
        assertTrue(isDayModeActive(office, instant, berlin))
        assertFalse(isDayModeActive(office, instant, TimeZone.of("Asia/Tokyo"))) // 01:00 Saturday
    }

    @Test fun nextEdgeIsTheEndInsideAndTheNextStartOutside() {
        assertEquals(at("2026-11-13T18:00"), nextDayScheduleEdge(office, at("2026-11-13T10:00"), monterrey))
        assertEquals(at("2026-11-16T09:00"), nextDayScheduleEdge(office, at("2026-11-13T18:00"), monterrey)) // over the weekend
        assertEquals(at("2026-11-13T09:00"), nextDayScheduleEdge(office, at("2026-11-13T08:00"), monterrey))
    }

    @Test fun nextEdgeSkipsWindowsThatTouch() {
        val everyDay = scheduled("06:00", "06:00", days = 0b111_1111)
        assertNull(nextDayScheduleEdge(everyDay, at("2026-11-13T10:00"), monterrey))
        val twoDays = scheduled("06:00", "06:00", days = 0b001_1000) // Thursday and Friday
        assertEquals(at("2026-11-14T06:00"), nextDayScheduleEdge(twoDays, at("2026-11-12T10:00"), monterrey))
    }

    @Test fun noDaysMeansNoEdge() {
        assertNull(nextDayScheduleEdge(office.copy(dayModeDays = 0), at("2026-11-13T10:00"), monterrey))
    }

    @Test fun windowEdgesFollowDaylightSaving() {
        // Berlin springs forward on 2026-03-29 at 02:00; a 02:30 start lands at 03:30.
        val early = scheduled("02:30", "08:00", days = 1 shl 6)
        assertEquals(at("2026-03-29T03:30", berlin), nextDayScheduleEdge(early, at("2026-03-29T01:00", berlin), berlin))
        assertTrue(isDayModeActive(early, at("2026-03-29T03:30", berlin), berlin))
    }

    @Test fun buttonOverridesTheScheduleUntilItsNextEdge() {
        val now = at("2026-11-13T10:00")
        val toggled = office.withDayModeToggled(now, monterrey)
        assertEquals(at("2026-11-13T18:00").toEpochMilliseconds(), toggled.dayModeOverrideUntil)
        assertFalse(isDayModeActive(toggled, now, monterrey))
        assertFalse(isDayModeActive(toggled, at("2026-11-13T17:59"), monterrey))
        // The schedule says off from 18:00 and the override has run out: still off, and on again Monday.
        assertFalse(isDayModeActive(toggled, at("2026-11-13T18:00"), monterrey))
        assertTrue(isDayModeActive(toggled, at("2026-11-16T09:00"), monterrey))
    }

    @Test fun buttonOutsideTheWindowShowsDayModeUntilItOpens() {
        val saturday = at("2026-11-14T11:00")
        val toggled = office.withDayModeToggled(saturday, monterrey)
        assertTrue(isDayModeActive(toggled, saturday, monterrey))
        assertTrue(isDayModeActive(toggled, at("2026-11-16T08:59"), monterrey))
        assertTrue(isDayModeActive(toggled, at("2026-11-16T09:00"), monterrey)) // the schedule takes over
        assertFalse(isDayModeActive(toggled, at("2026-11-16T18:00"), monterrey))
    }

    @Test fun secondPressEndsTheOverride() {
        val now = at("2026-11-13T10:00")
        val back = office.withDayModeToggled(now, monterrey).withDayModeToggled(now + 5.minutes, monterrey)
        assertEquals(0L, back.dayModeOverrideUntil)
        assertTrue(isDayModeActive(back, now + 5.minutes, monterrey))
    }

    @Test fun buttonWithoutEdgesOverridesUntilPressedAgain() {
        val never = office.copy(dayModeDays = 0)
        val toggled = never.withDayModeToggled(at("2026-11-13T10:00"), monterrey)
        assertTrue(isDayModeActive(toggled, at("2027-06-01T10:00"), monterrey))
    }

    @Test fun buttonSwapsOffAndOn() {
        val now = at("2026-11-13T10:00")
        assertEquals(DayMode.ON, Settings().withDayModeToggled(now, monterrey).dayMode)
        assertEquals(DayMode.OFF, Settings(dayMode = DayMode.ON).withDayModeToggled(now, monterrey).dayMode)
    }

    @Test fun clockOnlyTurnsDimmingOn() {
        val now = at("2026-11-13T10:00")
        val clock = Settings(dayMode = DayMode.ON, dimEnabled = false).withDayModeToggled(now, monterrey)
        assertEquals(DayMode.OFF, clock.dayMode)
        assertTrue(clock.dimEnabled)
        // On a schedule, flipping out of day mode does it too.
        assertTrue(office.copy(dimEnabled = false).withDayModeToggled(now, monterrey).dimEnabled)
    }

    @Test fun dayModeLeavesDimmingAsItWas() {
        val now = at("2026-11-13T10:00")
        assertFalse(Settings(dimEnabled = false).withDayModeToggled(now, monterrey).dimEnabled)
        val saturday = at("2026-11-14T11:00")
        assertFalse(office.copy(dimEnabled = false).withDayModeToggled(saturday, monterrey).dimEnabled)
    }
}
