package com.adl.nocturna.day

import com.adl.nocturna.data.CalendarView
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue
import kotlin.test.Test
import kotlinx.datetime.DateTimeUnit
import kotlinx.datetime.DayOfWeek
import kotlinx.datetime.LocalDate
import kotlinx.datetime.TimeZone
import kotlinx.datetime.YearMonth
import kotlinx.datetime.atStartOfDayIn
import kotlinx.datetime.atTime
import kotlinx.datetime.minus
import kotlinx.datetime.plus
import kotlinx.datetime.toInstant
import kotlin.time.Instant

class CalendarTest {
    private val monterrey = TimeZone.of("America/Monterrey") // UTC-6, no daylight saving
    private val today = LocalDate(2026, 11, 13)

    private fun at(hour: Int, minute: Int = 0, day: LocalDate = today) =
        day.atTime(hour, minute).toInstant(monterrey).toEpochMilliseconds()

    private fun allDay(title: String, from: LocalDate, until: LocalDate = from.plus(1, DateTimeUnit.DAY)) = CalendarEvent(
        title,
        from.atStartOfDayIn(TimeZone.UTC).toEpochMilliseconds(),
        until.atStartOfDayIn(TimeZone.UTC).toEpochMilliseconds(),
        allDay = true,
    )

    // November 2026 starts on a Sunday.
    @Test fun mondayFirstMonthPadsWithTheDaysAround() {
        val grid = monthGrid(YearMonth(2026, 11), DayOfWeek.MONDAY)
        assertEquals(6, grid.size)
        assertEquals(LocalDate(2026, 10, 26), grid.first().first())
        assertEquals(LocalDate(2026, 12, 6), grid.last().last())
        assertTrue(grid.all { it.size == 7 && it.first().dayOfWeek == DayOfWeek.MONDAY })
    }

    @Test fun sundayFirstMonthStartsOnTheFirst() {
        val grid = monthGrid(YearMonth(2026, 11), DayOfWeek.SUNDAY)
        assertEquals(5, grid.size)
        assertEquals(LocalDate(2026, 11, 1), grid.first().first())
        assertEquals(LocalDate(2026, 12, 5), grid.last().last())
    }

    @Test fun februaryThatFitsExactlyHasFourWeeks() {
        val grid = monthGrid(YearMonth(2026, 2), DayOfWeek.SUNDAY)
        assertEquals(4, grid.size)
        assertEquals(LocalDate(2026, 2, 28), grid.last().last())
    }

    @Test fun eventsAreSortedAllDayFirstThenByStart() {
        val events = listOf(
            CalendarEvent("Lunch", at(13), at(14), allDay = false),
            CalendarEvent("Stand-up", at(9, 30), at(9, 45), allDay = false),
            allDay("Holiday", today),
        )
        val shown = eventsForDay(events, today, monterrey, Instant.fromEpochMilliseconds(at(8)))
        assertEquals(listOf("Holiday", "Stand-up", "Lunch"), shown.map { it.title })
    }

    @Test fun endedEventsAreLeftOutButOngoingOnesStay() {
        val events = listOf(
            CalendarEvent("Stand-up", at(9, 30), at(9, 45), allDay = false),
            CalendarEvent("Review", at(10), at(11), allDay = false),
        )
        val now = Instant.fromEpochMilliseconds(at(10, 15))
        val shown = eventsForDay(events, today, monterrey, now)
        assertEquals(listOf("Review"), shown.map { it.title })
        assertTrue(shown.single().isOngoing(now))
    }

    @Test fun otherDaysEventsAreLeftOut() {
        val events = listOf(
            CalendarEvent("Yesterday", at(15, day = today.minus(1, DateTimeUnit.DAY)), at(16, day = today.minus(1, DateTimeUnit.DAY)), allDay = false),
            CalendarEvent("Tomorrow", at(9, day = today.plus(1, DateTimeUnit.DAY)), at(10, day = today.plus(1, DateTimeUnit.DAY)), allDay = false),
        )
        assertEquals(emptyList<CalendarEvent>(), eventsForDay(events, today, monterrey, Instant.fromEpochMilliseconds(at(0))))
    }

    @Test fun allDayEventsAreReadAsDatesNotAsUtcInstants() {
        // Tomorrow's all-day event starts at 00:00 UTC, which is still today (18:00) in Monterrey.
        val events = listOf(allDay("Tomorrow", today.plus(1, DateTimeUnit.DAY)), allDay("Yesterday", today.minus(1, DateTimeUnit.DAY)))
        assertEquals(emptyList<CalendarEvent>(), eventsForDay(events, today, monterrey, Instant.fromEpochMilliseconds(at(20))))
    }

    @Test fun multiDayAllDayEventCoversEveryDay() {
        val trip = allDay("Trip", today.minus(2, DateTimeUnit.DAY), today.plus(2, DateTimeUnit.DAY))
        assertEquals(listOf(trip), eventsForDay(listOf(trip), today, monterrey, Instant.fromEpochMilliseconds(at(23))))
        assertEquals(emptyList<CalendarEvent>(), eventsForDay(listOf(trip), today.plus(2, DateTimeUnit.DAY), monterrey, Instant.fromEpochMilliseconds(at(0))))
    }

    @Test fun eventFromLastNightStillRunningIsShown() {
        val night = CalendarEvent("Deploy", at(22, day = today.minus(1, DateTimeUnit.DAY)), at(9), allDay = false)
        val now = Instant.fromEpochMilliseconds(at(8))
        assertEquals(listOf(night), eventsForDay(listOf(night), today, monterrey, now))
        assertTrue(night.isOngoing(now))
        assertFalse(night.isOngoing(Instant.fromEpochMilliseconds(at(9))))
    }

    @Test fun allRowsShownWhenTheyFit() = assertEquals(3, rowsThatFit(listOf(10, 10, 10), gap = 5, max = 40, moreHeight = 8))

    @Test fun roomIsKeptForTheMoreLine() {
        // Three rows would fit in 40 with nothing after them, but not four; with the "+N more" line only two fit.
        assertEquals(2, rowsThatFit(listOf(10, 10, 10, 10), gap = 5, max = 40, moreHeight = 8))
    }

    @Test fun noRowsWhenNothingFits() = assertEquals(0, rowsThatFit(listOf(50), gap = 5, max = 40, moreHeight = 8))

    @Test fun noteFitsUnderTheClockWithRoomToSpare() = assertTrue(noteFits(clock = 200, gap = 32, noteMin = 120, max = 400))

    @Test fun noteFitsExactly() = assertTrue(noteFits(clock = 248, gap = 32, noteMin = 120, max = 400))

    @Test fun noteIsLeftOutUnderATallClock() {
        // A landscape phone: the analog clock and its info line take nearly the whole column.
        assertFalse(noteFits(clock = 300, gap = 32, noteMin = 120, max = 340))
    }

    @Test fun dotsMarkDaysWithTimedAndAllDayEvents() {
        val days = monthGrid(YearMonth(2026, 11), DayOfWeek.SUNDAY).flatten()
        val events = listOf(
            CalendarEvent("Stand-up", at(9, 30), at(9, 45), allDay = false),
            allDay("Trip", LocalDate(2026, 11, 20), LocalDate(2026, 11, 22)),
            // Ends exactly at midnight: belongs to the 24th only.
            CalendarEvent("Late", at(23, day = LocalDate(2026, 11, 24)), at(0, day = LocalDate(2026, 11, 25)), allDay = false),
        )
        assertEquals(
            setOf(today, LocalDate(2026, 11, 20), LocalDate(2026, 11, 21), LocalDate(2026, 11, 24)),
            daysWithEvents(events, days, monterrey),
        )
    }

    @Test fun greetingFollowsTheTimeOfDay() {
        assertEquals("Good morning!", greeting(8))
        assertEquals("Good afternoon!", greeting(12))
        assertEquals("Good evening!", greeting(20))
        assertEquals("Good evening!", greeting(2))
    }

    @Test fun weekStartsOnTheLocalesFirstDay() {
        // Saturday 14 November 2026.
        val sat = LocalDate(2026, 11, 14)
        assertEquals(LocalDate(2026, 11, 8), weekOf(sat, DayOfWeek.SUNDAY).first())
        assertEquals(LocalDate(2026, 11, 9), weekOf(sat, DayOfWeek.MONDAY).first())
        assertEquals(LocalDate(2026, 11, 15), weekOf(sat, DayOfWeek.MONDAY).last())
        assertEquals(sat, weekOf(sat, DayOfWeek.SATURDAY).first())
    }

    @Test fun dayViewKeepsFinishedEvents() {
        val events = listOf(
            CalendarEvent("Lunch", at(13), at(14), allDay = false),
            CalendarEvent("Stand-up", at(9, 30), at(9, 45), allDay = false),
            allDay("Holiday", today),
            CalendarEvent("Tomorrow", at(9, day = today.plus(1, DateTimeUnit.DAY)), at(10, day = today.plus(1, DateTimeUnit.DAY)), allDay = false),
        )
        assertEquals(listOf("Holiday", "Stand-up", "Lunch"), eventsOnDay(events, today, monterrey).map { it.title })
    }

    @Test fun swipesMoveThroughTheViewsAndStopAtTheEnds() {
        assertEquals(CalendarView.WEEK, swipeView(CalendarView.MONTH, towardsDetail = true))
        assertEquals(CalendarView.DAY, swipeView(CalendarView.WEEK, towardsDetail = true))
        assertEquals(CalendarView.DAY, swipeView(CalendarView.DAY, towardsDetail = true))
        assertEquals(CalendarView.WEEK, swipeView(CalendarView.DAY, towardsDetail = false))
        assertEquals(CalendarView.MONTH, swipeView(CalendarView.MONTH, towardsDetail = false))
    }
}
