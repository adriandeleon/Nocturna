package com.adl.nocturna.day

import com.adl.nocturna.text.DateTimePattern
import com.adl.nocturna.text.testLocale
import kotlinx.datetime.LocalDate
import kotlinx.datetime.TimeZone
import kotlinx.datetime.atStartOfDayIn
import kotlinx.datetime.atTime
import kotlinx.datetime.toInstant
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue
import kotlin.time.Instant

class MeetingCountdownTest {
    private val monterrey = TimeZone.of("America/Monterrey") // UTC-6, no daylight saving
    private val today = LocalDate(2026, 11, 13)

    private fun at(hour: Int, minute: Int = 0, second: Int = 0, day: LocalDate = today): Instant =
        day.atTime(hour, minute, second).toInstant(monterrey)

    private fun timed(title: String, hour: Int, minute: Int = 0, lengthMin: Int = 30, id: Long = 1L) = CalendarEvent(
        title,
        at(hour, minute).toEpochMilliseconds(),
        at(hour, minute).toEpochMilliseconds() + lengthMin * 60_000L,
        allDay = false,
        eventId = id,
    )

    private val standup = timed("Standup", 10, 0)

    @Test fun picksTheNextTimedEventWithinTheHour() {
        val events = listOf(timed("Lunch", 13), standup, timed("Review", 10, 30))
        assertEquals(standup, upcomingMeeting(events, at(9, 15)))
    }

    @Test fun nothingMoreThanAnHourAhead() {
        assertNull(upcomingMeeting(listOf(standup), at(8, 59, 59)))
        assertEquals(standup, upcomingMeeting(listOf(standup), at(9, 0)))
    }

    @Test fun allDayEventsNeverCountDown() {
        val tomorrow = LocalDate(2026, 11, 14)
        val holiday = CalendarEvent(
            "Holiday",
            tomorrow.atStartOfDayIn(TimeZone.UTC).toEpochMilliseconds(),
            LocalDate(2026, 11, 15).atStartOfDayIn(TimeZone.UTC).toEpochMilliseconds(),
            allDay = true,
        )
        // 17:30 in Monterrey is 23:30 UTC, half an hour before the holiday's UTC midnight.
        assertNull(upcomingMeeting(listOf(holiday), at(17, 30)))
    }

    @Test fun anEventUnderWayStopsCountingDown() {
        assertNull(upcomingMeeting(listOf(standup), at(10, 0)))
        assertNull(upcomingMeeting(listOf(standup), at(10, 5)))
        // The next one takes over while the first is still on.
        val review = timed("Review", 10, 30)
        assertEquals(review, upcomingMeeting(listOf(standup, review), at(10, 5)))
    }

    @Test fun eventsAtTheSameTimeGoByTitle() {
        val b = timed("Budget", 10, 0, id = 2)
        assertEquals(b, upcomingMeeting(listOf(standup, b), at(9, 30)))
    }

    @Test fun minutesRoundUp() {
        assertEquals(60, minutesUntil(standup, at(9, 0)))
        assertEquals(12, minutesUntil(standup, at(9, 48)))
        assertEquals(12, minutesUntil(standup, at(9, 48, 30)))
        assertEquals(2, minutesUntil(standup, at(9, 58, 0)))
        assertEquals(2, minutesUntil(standup, at(9, 58, 59)))
        assertEquals(1, minutesUntil(standup, at(9, 59, 0)))
        assertEquals(1, minutesUntil(standup, at(9, 59, 59)))
    }

    @Test fun lineNamesTheEvent() {
        assertEquals("Standup in 12 min", countdownLine(standup, at(9, 48), testLocale))
        assertEquals("Standup in 1 min", countdownLine(standup, at(9, 59, 30), testLocale))
        assertEquals("Next event in 5 min", countdownLine(standup.copy(title = "  "), at(9, 55), testLocale))
    }

    @Test fun noteLabelHasTheStartTimeInTheClocksZone() {
        val time = DateTimePattern("HH:mm", testLocale)
        assertEquals("In 12 min · 10:00", countdownLabel(standup, at(9, 48), monterrey, time, testLocale))
        // New York is an hour ahead of Monterrey in November.
        assertEquals("In 12 min · 11:00", countdownLabel(standup, at(9, 48), TimeZone.of("America/New_York"), time, testLocale))
        val twelveHour = DateTimePattern("h:mm a", testLocale)
        assertEquals("in 1 min · 10:00 am", countdownLabel(standup, at(9, 59, 30), monterrey, twelveHour, testLocale).lowercase())
    }

    @Test fun chimesAtItsMinuteOnly() {
        assertFalse(shouldChime(standup, at(9, 57, 59), 2, null))
        assertTrue(shouldChime(standup, at(9, 58), 2, null))
        assertTrue(shouldChime(standup, at(9, 58, 59), 2, null))
        // Day mode opened later than that: no chime.
        assertFalse(shouldChime(standup, at(9, 59), 2, null))
        assertTrue(shouldChime(standup, at(9, 55, 10), 5, null))
        assertTrue(shouldChime(standup, at(9, 59, 30), 1, null))
    }

    @Test fun chimesOncePerEvent() {
        assertFalse(shouldChime(standup, at(9, 58, 1), 2, chimeKey(standup)))
        // The same event on another day is another occurrence.
        val tomorrow = standup.copy(begin = standup.begin + 86_400_000L, end = standup.end + 86_400_000L)
        assertTrue(shouldChime(tomorrow, at(9, 58, day = LocalDate(2026, 11, 14)), 2, chimeKey(standup)))
    }

    @Test fun offNeverChimes() {
        assertFalse(shouldChime(standup, at(9, 58), 0, null))
        assertFalse(shouldChime(null, at(9, 58), 2, null))
    }
}
