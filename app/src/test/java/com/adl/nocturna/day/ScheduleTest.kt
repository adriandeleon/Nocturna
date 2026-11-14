package com.adl.nocturna.day

import com.adl.nocturna.data.DayMode
import com.adl.nocturna.data.Settings
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDateTime
import java.time.ZoneId
import java.time.ZonedDateTime

class ScheduleTest {
    private val monterrey = ZoneId.of("America/Monterrey") // UTC-6, no daylight saving
    private val berlin = ZoneId.of("Europe/Berlin")

    // 2026-11-13 is a Friday.
    private fun at(text: String, zone: ZoneId = monterrey): ZonedDateTime = LocalDateTime.parse(text).atZone(zone)

    private fun scheduled(start: String, end: String, days: Int = Settings.WEEKDAYS) = Settings(
        dayMode = DayMode.SCHEDULED,
        dayModeStart = minutes(start),
        dayModeEnd = minutes(end),
        dayModeDays = days,
    )

    private fun minutes(hhmm: String) = hhmm.substring(0, 2).toInt() * 60 + hhmm.substring(3).toInt()

    private val office = scheduled("09:00", "18:00")

    @Test fun offAndOnIgnoreTheSchedule() {
        assertFalse(isDayModeActive(office.copy(dayMode = DayMode.OFF), at("2026-11-13T10:00")))
        assertTrue(isDayModeActive(office.copy(dayMode = DayMode.ON), at("2026-11-14T03:00")))
    }

    @Test fun startIsInsideAndEndIsOutside() {
        assertFalse(isDayModeActive(office, at("2026-11-13T08:59:59")))
        assertTrue(isDayModeActive(office, at("2026-11-13T09:00")))
        assertTrue(isDayModeActive(office, at("2026-11-13T17:59:59")))
        assertFalse(isDayModeActive(office, at("2026-11-13T18:00")))
    }

    @Test fun daysOffStayOnTheClock() {
        assertFalse(isDayModeActive(office, at("2026-11-14T10:00"))) // Saturday
        assertFalse(isDayModeActive(office, at("2026-11-15T10:00"))) // Sunday
        assertTrue(isDayModeActive(office, at("2026-11-16T10:00"))) // Monday
        assertFalse(isDayModeActive(office.copy(dayModeDays = 0), at("2026-11-16T10:00")))
    }

    @Test fun windowAcrossMidnightBelongsToTheDayItStarts() {
        val nights = scheduled("22:00", "06:00", days = 1 shl 4) // Fridays only
        assertTrue(isDayModeActive(nights, at("2026-11-13T22:00"))) // Friday night
        assertTrue(isDayModeActive(nights, at("2026-11-14T05:59"))) // into Saturday
        assertFalse(isDayModeActive(nights, at("2026-11-14T06:00")))
        assertFalse(isDayModeActive(nights, at("2026-11-14T23:00"))) // Saturday night is not Friday's
        assertFalse(isDayModeActive(nights, at("2026-11-13T05:00"))) // Thursday's night wasn't set
    }

    @Test fun sameStartAndEndKeepsItOnAllDay() {
        val allDay = scheduled("00:00", "00:00", days = 0b110_0000) // weekends
        assertTrue(isDayModeActive(allDay, at("2026-11-14T00:00")))
        assertTrue(isDayModeActive(allDay, at("2026-11-15T23:59")))
        assertFalse(isDayModeActive(allDay, at("2026-11-16T00:00")))
    }

    @Test fun readsTheTimesInTheZoneItIsGiven() {
        val instant = at("2026-11-13T10:00").toInstant() // 17:00 in Berlin
        assertTrue(isDayModeActive(office, instant.atZone(berlin)))
        assertFalse(isDayModeActive(office, instant.atZone(ZoneId.of("Asia/Tokyo")))) // 01:00 Saturday
    }

    @Test fun nextEdgeIsTheEndInsideAndTheNextStartOutside() {
        assertEquals(at("2026-11-13T18:00"), nextDayScheduleEdge(office, at("2026-11-13T10:00")))
        assertEquals(at("2026-11-16T09:00"), nextDayScheduleEdge(office, at("2026-11-13T18:00"))) // over the weekend
        assertEquals(at("2026-11-13T09:00"), nextDayScheduleEdge(office, at("2026-11-13T08:00")))
    }

    @Test fun nextEdgeSkipsWindowsThatTouch() {
        val everyDay = scheduled("06:00", "06:00", days = 0b111_1111)
        assertNull(nextDayScheduleEdge(everyDay, at("2026-11-13T10:00")))
        val twoDays = scheduled("06:00", "06:00", days = 0b001_1000) // Thursday and Friday
        assertEquals(at("2026-11-14T06:00"), nextDayScheduleEdge(twoDays, at("2026-11-12T10:00")))
    }

    @Test fun noDaysMeansNoEdge() {
        assertNull(nextDayScheduleEdge(office.copy(dayModeDays = 0), at("2026-11-13T10:00")))
    }

    @Test fun windowEdgesFollowDaylightSaving() {
        // Berlin springs forward on 2026-03-29 at 02:00; a 02:30 start lands at 03:30.
        val early = scheduled("02:30", "08:00", days = 1 shl 6)
        assertEquals(at("2026-03-29T03:30", berlin), nextDayScheduleEdge(early, at("2026-03-29T01:00", berlin)))
        assertTrue(isDayModeActive(early, at("2026-03-29T03:30", berlin)))
    }

    @Test fun buttonOverridesTheScheduleUntilItsNextEdge() {
        val now = at("2026-11-13T10:00")
        val toggled = office.withDayModeToggled(now)
        assertEquals(at("2026-11-13T18:00").toInstant().toEpochMilli(), toggled.dayModeOverrideUntil)
        assertFalse(isDayModeActive(toggled, now))
        assertFalse(isDayModeActive(toggled, at("2026-11-13T17:59")))
        // The schedule says off from 18:00 and the override has run out: still off, and on again Monday.
        assertFalse(isDayModeActive(toggled, at("2026-11-13T18:00")))
        assertTrue(isDayModeActive(toggled, at("2026-11-16T09:00")))
    }

    @Test fun buttonOutsideTheWindowShowsDayModeUntilItOpens() {
        val saturday = at("2026-11-14T11:00")
        val toggled = office.withDayModeToggled(saturday)
        assertTrue(isDayModeActive(toggled, saturday))
        assertTrue(isDayModeActive(toggled, at("2026-11-16T08:59")))
        assertTrue(isDayModeActive(toggled, at("2026-11-16T09:00"))) // the schedule takes over
        assertFalse(isDayModeActive(toggled, at("2026-11-16T18:00")))
    }

    @Test fun secondPressEndsTheOverride() {
        val now = at("2026-11-13T10:00")
        val back = office.withDayModeToggled(now).withDayModeToggled(now.plusMinutes(5))
        assertEquals(0L, back.dayModeOverrideUntil)
        assertTrue(isDayModeActive(back, now.plusMinutes(5)))
    }

    @Test fun buttonWithoutEdgesOverridesUntilPressedAgain() {
        val never = office.copy(dayModeDays = 0)
        val toggled = never.withDayModeToggled(at("2026-11-13T10:00"))
        assertTrue(isDayModeActive(toggled, at("2027-06-01T10:00")))
    }

    @Test fun buttonSwapsOffAndOn() {
        val now = at("2026-11-13T10:00")
        assertEquals(DayMode.ON, Settings().withDayModeToggled(now).dayMode)
        assertEquals(DayMode.OFF, Settings(dayMode = DayMode.ON).withDayModeToggled(now).dayMode)
    }
}
