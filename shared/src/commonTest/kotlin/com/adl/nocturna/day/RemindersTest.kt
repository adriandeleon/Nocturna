package com.adl.nocturna.day

import com.adl.nocturna.clock.BatteryInfo
import com.adl.nocturna.text.spanishTestLocale
import com.adl.nocturna.text.testLocale
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class RemindersTest {
    private val minute = 60_000L

    @Test fun dueOnceTheIntervalHasPassed() {
        assertFalse(reminderDue(since = 0L, now = 44 * minute, intervalMinutes = 45, held = false))
        assertTrue(reminderDue(since = 0L, now = 45 * minute, intervalMinutes = 45, held = false))
    }

    @Test fun offIsNeverDue() = assertFalse(reminderDue(0L, 1_000 * minute, intervalMinutes = 0, held = false))

    @Test fun heldWaitsAndThenComesAtOnce() {
        assertFalse(reminderDue(0L, 90 * minute, 45, held = true))
        assertTrue(reminderDue(0L, 90 * minute, 45, held = false))
    }

    @Test fun heldDuringAFocusBlockOrAMeeting() {
        val focus = FocusStatus(step = 0, left = 10 * minute, length = 25 * minute, paused = false)
        assertTrue(remindersHeld(focus, inMeeting = false))
        assertFalse(remindersHeld(focus.copy(paused = true), inMeeting = false))
        // A break is the time to stretch.
        assertFalse(remindersHeld(focus.copy(step = 1), inMeeting = false))
        assertTrue(remindersHeld(null, inMeeting = true))
        assertFalse(remindersHeld(null, inMeeting = false))
    }

    @Test fun showsForFiveMinutes() {
        assertTrue(reminderShowing(shownAt = 0L, now = 4 * minute))
        assertFalse(reminderShowing(shownAt = 0L, now = 5 * minute))
        assertFalse(reminderShowing(shownAt = null, now = 0L))
    }

    @Test fun theFootstepsPlayBeforeTheWater() {
        assertEquals(listOf(ReminderKind.STRETCH, ReminderKind.WATER), reminderSoundOrder(setOf(ReminderKind.WATER, ReminderKind.STRETCH)))
        assertEquals(listOf(ReminderKind.WATER), reminderSoundOrder(setOf(ReminderKind.WATER)))
        assertEquals(emptyList(), reminderSoundOrder(emptySet()))
    }

    @Test fun reminderWords() {
        assertEquals("Time to stand up and stretch", reminderText(listOf(ReminderKind.STRETCH), testLocale))
        assertEquals("Time for a glass of water", reminderText(listOf(ReminderKind.WATER), testLocale))
        assertEquals("Time to stretch and drink some water", reminderText(listOf(ReminderKind.WATER, ReminderKind.STRETCH), testLocale))
        assertEquals("Hora de un vaso de agua", reminderText(listOf(ReminderKind.WATER), spanishTestLocale))
    }

    @Test fun batteryCareWhileChargingPastTheMark() {
        assertTrue(batteryCareDue(BatteryInfo(0.85f, charging = true), 85))
        assertFalse(batteryCareDue(BatteryInfo(0.84f, charging = true), 85))
        assertFalse(batteryCareDue(BatteryInfo(0.95f, charging = false), 85))
        assertFalse(batteryCareDue(BatteryInfo(1f, charging = true), 0))
        assertFalse(batteryCareDue(null, 80))
        assertEquals("Charged to 86% · you can unplug", batteryCareText(BatteryInfo(0.86f, true), testLocale))
    }

    @Test fun aWaveStartsPausesAndResumesTheFocusTimer() {
        val started = waveFocus(null, 1_000L)
        assertEquals(FocusTimer.start(1_000L), started)
        val paused = waveFocus(started, 61_000L)
        assertTrue(paused.paused)
        val resumed = waveFocus(paused, 121_000L)
        assertFalse(resumed.paused)
        // The minute spent paused doesn't count.
        assertEquals(61_000L, resumed.since)
    }
}
