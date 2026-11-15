package com.adl.nocturna.alarm

import com.adl.nocturna.data.Alarm
import com.adl.nocturna.day.CalendarEvent
import kotlinx.datetime.LocalDate
import kotlinx.datetime.LocalDateTime
import kotlinx.datetime.TimeZone
import kotlinx.datetime.atStartOfDayIn
import kotlinx.datetime.toInstant
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue
import kotlin.time.Instant

class DaysOffTest {
    private val zone = TimeZone.of("America/Monterrey")
    private fun at(s: String): Instant = LocalDateTime.parse(s).toInstant(zone)
    private fun date(s: String) = LocalDate.parse(s)
    private val keywords = parseDayOffKeywords(DEFAULT_DAY_OFF_KEYWORDS)

    /** Weekdays at 06:40, skipping days off. */
    private val weekdays = Alarm(1, 6, 40, days = 0b0011111, skipDaysOff = true)

    /** An all-day event from [first] to [last] inclusive, stored the provider's way, at UTC midnights. */
    private fun allDay(title: String, first: String, last: String = first) = CalendarEvent(
        title = title,
        begin = date(first).atStartOfDayIn(TimeZone.UTC).toEpochMilliseconds(),
        end = date(last).let { LocalDate.fromEpochDays(it.toEpochDays() + 1) }.atStartOfDayIn(TimeZone.UTC).toEpochMilliseconds(),
        allDay = true,
    )

    // Keyword matching

    @Test fun keywordsAreSplitOnCommasTrimmedAndNormalised() =
        assertEquals(listOf("holiday", "day off", "dia libre"), parseDayOffKeywords(" Holiday ,day   off,, Día Libre ,holiday"))

    @Test fun blankKeywordsAreNone() = assertEquals(emptyList(), parseDayOffKeywords(" , ,"))

    @Test fun matchingIgnoresCaseAndAccents() {
        assertTrue(matchesDayOffKeyword("DIA LIBRE", keywords))
        assertTrue(matchesDayOffKeyword("Día libre 🎉", keywords))
        assertTrue(matchesDayOffKeyword("Festivo: Revolución", keywords))
        assertTrue(matchesDayOffKeyword("Thanksgiving holiday", keywords))
        assertTrue(matchesDayOffKeyword("pto", keywords))
        assertTrue(matchesDayOffKeyword("OOO - dentist", keywords))
        assertTrue(matchesDayOffKeyword("Vacaciones en Cancún", keywords))
    }

    @Test fun keywordsMatchWholeWordsOnly() {
        assertFalse(matchesDayOffKeyword("Buy a laptop", keywords))
        assertFalse(matchesDayOffKeyword("Holidays planning", keywords))
        assertFalse(matchesDayOffKeyword("Gym", keywords))
        assertTrue(matchesDayOffKeyword("Holidays planning", parseDayOffKeywords("holidays")))
    }

    @Test fun keywordsWithAccentsMatchTitlesWithout() =
        assertTrue(matchesDayOffKeyword("dia libre", parseDayOffKeywords("día libre")))

    // Days off from events

    @Test fun onlyAllDayEventsWithAKeywordMakeDaysOff() {
        val events = listOf(
            allDay("Holiday", "2026-11-16"),
            allDay("Birthday", "2026-11-17"),
            CalendarEvent("PTO", at("2026-11-18T09:00").toEpochMilliseconds(), at("2026-11-18T17:00").toEpochMilliseconds(), allDay = false),
        )
        assertEquals(mapOf(date("2026-11-16") to "Holiday"), daysOff(events, keywords, date("2026-11-15"), date("2026-11-30")))
    }

    @Test fun aMultiDayEventCoversEachDay() {
        val off = daysOff(listOf(allDay("Vacation", "2026-12-21", "2026-12-23")), keywords, date("2026-12-01"), date("2026-12-31"))
        assertEquals(setOf(date("2026-12-21"), date("2026-12-22"), date("2026-12-23")), off.keys)
    }

    @Test fun daysOffStayInsideTheRange() {
        val off = daysOff(listOf(allDay("Vacation", "2026-12-21", "2026-12-23")), keywords, date("2026-12-22"), date("2026-12-22"))
        assertEquals(setOf(date("2026-12-22")), off.keys)
    }

    @Test fun noKeywordsMeansNoDaysOff() =
        assertEquals(emptyMap(), daysOff(listOf(allDay("Holiday", "2026-11-16")), emptyList(), date("2026-11-15"), date("2026-11-30")))

    // Next ring

    @Test fun aDayOffMovesTheRingToTheNextWorkday() {
        // Saturday: the next weekday is Monday 16th, a holiday, so Tuesday 17th.
        val now = at("2026-11-14T12:00")
        val off = mapOf(date("2026-11-16") to "Revolution Day holiday")
        assertEquals(at("2026-11-17T06:40"), weekdays.nextRing(now, zone, off))
        assertEquals(DayOffSkip(date("2026-11-16"), "Revolution Day holiday"), weekdays.nextDayOffSkip(now, zone, off))
        assertEquals(at("2026-11-16T06:40"), weekdays.firstSkippedRing(now, zone, off))
    }

    @Test fun severalDaysOffInARowAreAllSkipped() {
        val now = at("2026-12-20T12:00")
        val off = (21..25).associate { date("2026-12-$it") to "Vacation" }
        assertEquals(at("2026-12-28T06:40"), weekdays.nextRing(now, zone, off))
    }

    @Test fun withTheOptionOffDaysOffAreIgnored() {
        val now = at("2026-11-14T12:00")
        val off = mapOf(date("2026-11-16") to "Holiday")
        val plain = weekdays.copy(skipDaysOff = false)
        assertEquals(at("2026-11-16T06:40"), plain.nextRing(now, zone, off))
        assertNull(plain.nextDayOffSkip(now, zone, off))
        assertNull(plain.firstSkippedRing(now, zone, off))
    }

    @Test fun aOneTimeAlarmAlwaysRings() {
        val once = Alarm(1, 6, 40, skipDaysOff = true)
        val now = at("2026-11-15T12:00")
        assertEquals(at("2026-11-16T06:40"), once.nextRing(now, zone, mapOf(date("2026-11-16") to "Holiday")))
    }

    @Test fun aDayOffThatIsNotARingDayChangesNothing() {
        val now = at("2026-11-14T12:00")
        val off = mapOf(date("2026-11-15") to "Holiday") // a Sunday
        assertEquals(at("2026-11-16T06:40"), weekdays.nextRing(now, zone, off))
        assertNull(weekdays.nextDayOffSkip(now, zone, off))
    }

    @Test fun noDaysOffIsTheUsualNextTrigger() {
        val now = at("2026-11-14T12:00")
        assertEquals(weekdays.nextTrigger(now, zone), weekdays.nextRing(now, zone, emptyMap()))
    }

    @Test fun lookAheadIsCapped() {
        // Every day for a year is a day off: the ring after the look-ahead rings anyway.
        val now = at("2026-11-14T12:00")
        val daily = Alarm(1, 6, 40, days = Alarm.ALL_DAYS, skipDaysOff = true)
        val off = (0 until 365).associate { LocalDate.fromEpochDays(date("2026-11-15").toEpochDays() + it) to "Sabbatical holiday" }
        assertEquals(at("2027-01-14T06:40"), daily.nextRing(now, zone, off))
    }

    // Together with Skip next

    @Test fun skipNextAndADayOffAddUp() {
        // Monday is a holiday; Skip next then skips Tuesday, the next ring that would happen.
        val now = at("2026-11-14T12:00")
        val off = mapOf(date("2026-11-16") to "Holiday")
        val skipped = weekdays.toggleSkip(now, zone, off)
        assertEquals(date("2026-11-17"), skipped.skipDate)
        assertEquals(date("2026-11-17"), skipped.skipping(now, zone, off))
        assertEquals(at("2026-11-18T06:40"), skipped.nextRing(now, zone, off))
        assertNull(skipped.toggleSkip(now, zone, off).skipDate)
    }

    @Test fun aSkipNextThatLandsOnADayOffMakesWayForTheNextRing() {
        // Monday was skipped before it became a holiday: that skip changes nothing now, so Skip next
        // offers Tuesday, the next ring that would happen.
        val now = at("2026-11-14T12:00")
        val skipped = weekdays.toggleSkip(now, zone)
        assertEquals(date("2026-11-16"), skipped.skipDate)
        val off = mapOf(date("2026-11-16") to "Holiday")
        assertEquals(at("2026-11-17T06:40"), skipped.nextRing(now, zone, off))
        assertNull(skipped.skipping(now, zone, off))
        assertEquals(date("2026-11-17"), skipped.toggleSkip(now, zone, off).skipDate)
    }
}
