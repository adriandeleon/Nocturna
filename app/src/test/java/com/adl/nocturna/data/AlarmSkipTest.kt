package com.adl.nocturna.data

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.ZoneId
import java.time.ZonedDateTime

class AlarmSkipTest {
    private val zone = ZoneId.of("Europe/Berlin")
    private fun at(s: String): ZonedDateTime = LocalDateTime.parse(s).atZone(zone)
    private val weekdays = Alarm(1, 6, 40, days = 0b0011111)

    @Test fun skipNextMovesToTheFollowingOccurrence() {
        // Thursday evening: next is Friday 13th; skipping it moves to Monday 16th.
        val now = at("2026-11-12T22:00")
        val skipped = weekdays.toggleSkip(now)
        assertEquals(LocalDate.parse("2026-11-13"), skipped.skipDate)
        assertEquals(at("2026-11-16T06:40"), skipped.nextTrigger(now))
    }

    @Test fun skippingTodaysAlarmBeforeItRings() {
        val now = at("2026-11-13T05:00")
        val skipped = weekdays.toggleSkip(now)
        assertEquals(LocalDate.parse("2026-11-13"), skipped.skipping(now))
        assertEquals(at("2026-11-16T06:40"), skipped.nextTrigger(now))
    }

    @Test fun toggleTwiceUndoesTheSkip() {
        val now = at("2026-11-12T22:00")
        val undone = weekdays.toggleSkip(now).toggleSkip(now)
        assertNull(undone.skipDate)
        assertEquals(at("2026-11-13T06:40"), undone.nextTrigger(now))
    }

    @Test fun weeklyAlarmSkipsAWholeWeek() {
        val sundays = Alarm(1, 9, 0, days = 1 shl 6)
        // Sunday 15th after 09:00: next is the 22nd; skipping it means the 29th.
        val now = at("2026-11-15T10:00")
        assertEquals(at("2026-11-29T09:00"), sundays.toggleSkip(now).nextTrigger(now))
    }

    @Test fun skipEndsOnceTheSkippedDayHasPassed() {
        val skipped = weekdays.copy(skipDate = LocalDate.parse("2026-11-13"))
        val later = at("2026-11-13T07:00")
        assertNull(skipped.skipping(later))
        assertEquals(at("2026-11-16T06:40"), skipped.nextTrigger(later))
        // So "Skip next" now skips Monday, not Friday again.
        assertEquals(LocalDate.parse("2026-11-16"), skipped.toggleSkip(later).skipDate)
    }

    @Test fun oneTimeAlarmCannotBeSkipped() {
        val once = Alarm(1, 7, 0)
        assertEquals(once, once.toggleSkip(at("2026-11-12T22:00")))
    }

    @Test fun changingTheTimeDropsTheSkip() {
        val skipped = weekdays.copy(skipDate = LocalDate.parse("2026-11-13"))
        assertNull(skipped.copy(hour = 7).withSkipCheckedAgainst(skipped).skipDate)
        assertNull(skipped.copy(days = Alarm.ALL_DAYS).withSkipCheckedAgainst(skipped).skipDate)
        assertNull(skipped.copy(enabled = false).withSkipCheckedAgainst(skipped).skipDate)
    }

    @Test fun otherEditsKeepTheSkip() {
        val skipped = weekdays.copy(skipDate = LocalDate.parse("2026-11-13"))
        assertEquals(skipped.skipDate, skipped.copy(label = "Work").withSkipCheckedAgainst(skipped).skipDate)
    }

    @Test fun skipSurvivesJsonRoundTrip() {
        val alarms = listOf(weekdays.copy(skipDate = LocalDate.parse("2026-11-13")))
        assertEquals(alarms, AlarmStore.decode(AlarmStore.encode(alarms)))
    }

    @Test fun badSkipDateInStoredJsonIsIgnored() =
        assertNull(AlarmStore.decode("""[{"id":1,"hour":7,"minute":0,"days":31,"skipDate":"soon"}]""").single().skipDate)
}
