package com.adl.nocturna.day

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.DayOfWeek
import java.time.Instant
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.YearMonth
import java.time.ZoneId
import java.time.ZoneOffset

class CalendarTest {
    private val monterrey = ZoneId.of("America/Monterrey") // UTC-6, no daylight saving
    private val today = LocalDate.of(2026, 11, 13)

    private fun at(hour: Int, minute: Int = 0, day: LocalDate = today) =
        LocalDateTime.of(day, java.time.LocalTime.of(hour, minute)).atZone(monterrey).toInstant().toEpochMilli()

    private fun allDay(title: String, from: LocalDate, until: LocalDate = from.plusDays(1)) = CalendarEvent(
        title,
        from.atStartOfDay(ZoneOffset.UTC).toInstant().toEpochMilli(),
        until.atStartOfDay(ZoneOffset.UTC).toInstant().toEpochMilli(),
        allDay = true,
    )

    // November 2026 starts on a Sunday.
    @Test fun mondayFirstMonthPadsWithTheDaysAround() {
        val grid = monthGrid(YearMonth.of(2026, 11), DayOfWeek.MONDAY)
        assertEquals(6, grid.size)
        assertEquals(LocalDate.of(2026, 10, 26), grid.first().first())
        assertEquals(LocalDate.of(2026, 12, 6), grid.last().last())
        assertTrue(grid.all { it.size == 7 && it.first().dayOfWeek == DayOfWeek.MONDAY })
    }

    @Test fun sundayFirstMonthStartsOnTheFirst() {
        val grid = monthGrid(YearMonth.of(2026, 11), DayOfWeek.SUNDAY)
        assertEquals(5, grid.size)
        assertEquals(LocalDate.of(2026, 11, 1), grid.first().first())
        assertEquals(LocalDate.of(2026, 12, 5), grid.last().last())
    }

    @Test fun februaryThatFitsExactlyHasFourWeeks() {
        val grid = monthGrid(YearMonth.of(2026, 2), DayOfWeek.SUNDAY)
        assertEquals(4, grid.size)
        assertEquals(LocalDate.of(2026, 2, 28), grid.last().last())
    }

    @Test fun eventsAreSortedAllDayFirstThenByStart() {
        val events = listOf(
            CalendarEvent("Lunch", at(13), at(14), allDay = false),
            CalendarEvent("Stand-up", at(9, 30), at(9, 45), allDay = false),
            allDay("Holiday", today),
        )
        val shown = eventsForDay(events, today, monterrey, Instant.ofEpochMilli(at(8)))
        assertEquals(listOf("Holiday", "Stand-up", "Lunch"), shown.map { it.title })
    }

    @Test fun endedEventsAreLeftOutButOngoingOnesStay() {
        val events = listOf(
            CalendarEvent("Stand-up", at(9, 30), at(9, 45), allDay = false),
            CalendarEvent("Review", at(10), at(11), allDay = false),
        )
        val now = Instant.ofEpochMilli(at(10, 15))
        val shown = eventsForDay(events, today, monterrey, now)
        assertEquals(listOf("Review"), shown.map { it.title })
        assertTrue(shown.single().isOngoing(now))
    }

    @Test fun otherDaysEventsAreLeftOut() {
        val events = listOf(
            CalendarEvent("Yesterday", at(15, day = today.minusDays(1)), at(16, day = today.minusDays(1)), allDay = false),
            CalendarEvent("Tomorrow", at(9, day = today.plusDays(1)), at(10, day = today.plusDays(1)), allDay = false),
        )
        assertEquals(emptyList<CalendarEvent>(), eventsForDay(events, today, monterrey, Instant.ofEpochMilli(at(0))))
    }

    @Test fun allDayEventsAreReadAsDatesNotAsUtcInstants() {
        // Tomorrow's all-day event starts at 00:00 UTC, which is still today (18:00) in Monterrey.
        val events = listOf(allDay("Tomorrow", today.plusDays(1)), allDay("Yesterday", today.minusDays(1)))
        assertEquals(emptyList<CalendarEvent>(), eventsForDay(events, today, monterrey, Instant.ofEpochMilli(at(20))))
    }

    @Test fun multiDayAllDayEventCoversEveryDay() {
        val trip = allDay("Trip", today.minusDays(2), today.plusDays(2))
        assertEquals(listOf(trip), eventsForDay(listOf(trip), today, monterrey, Instant.ofEpochMilli(at(23))))
        assertEquals(emptyList<CalendarEvent>(), eventsForDay(listOf(trip), today.plusDays(2), monterrey, Instant.ofEpochMilli(at(0))))
    }

    @Test fun eventFromLastNightStillRunningIsShown() {
        val night = CalendarEvent("Deploy", at(22, day = today.minusDays(1)), at(9), allDay = false)
        val now = Instant.ofEpochMilli(at(8))
        assertEquals(listOf(night), eventsForDay(listOf(night), today, monterrey, now))
        assertTrue(night.isOngoing(now))
        assertFalse(night.isOngoing(Instant.ofEpochMilli(at(9))))
    }

    @Test fun allRowsShownWhenTheyFit() = assertEquals(3, rowsThatFit(listOf(10, 10, 10), gap = 5, max = 40, moreHeight = 8))

    @Test fun roomIsKeptForTheMoreLine() {
        // Three rows would fit in 40 with nothing after them, but not four; with the "+N more" line only two fit.
        assertEquals(2, rowsThatFit(listOf(10, 10, 10, 10), gap = 5, max = 40, moreHeight = 8))
    }

    @Test fun noRowsWhenNothingFits() = assertEquals(0, rowsThatFit(listOf(50), gap = 5, max = 40, moreHeight = 8))
}
