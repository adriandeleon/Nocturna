package com.adl.nocturna.day

import com.adl.nocturna.clock.BatteryInfo
import com.adl.nocturna.text.PlatformLocale
import com.adl.nocturna.text.strings

/*
 * Day mode's gentle nudges: to stand up and stretch, to drink some water, and to take the phone off the
 * charger once it's charged enough. They show as a tag in the desk clock; the first two also play a sound (footsteps, drops of water).
 */

/** What a reminder is for. */
enum class ReminderKind { STRETCH, WATER }

/** How long a reminder stays up in the desk clock unless it's tapped away. */
const val REMINDER_SHOW_MILLIS = 5 * 60_000L

/**
 * Whether the reminder that was last shown (or started counting) at [since] is due at [now], every
 * [intervalMinutes] (0 is off). While [held] it waits, and shows as soon as it's let go.
 */
fun reminderDue(since: Long, now: Long, intervalMinutes: Int, held: Boolean): Boolean =
    intervalMinutes > 0 && !held && now - since >= intervalMinutes * 60_000L

/**
 * Whether reminders wait: during a running focus block, so they don't break it (the break that follows is
 * the time to stretch), and during a meeting.
 */
fun remindersHeld(focus: FocusStatus?, inMeeting: Boolean): Boolean =
    inMeeting || (focus != null && !focus.paused && focus.phase == FocusPhase.FOCUS)

/** Whether a reminder shown at [shownAt] is still up at [now]. */
fun reminderShowing(shownAt: Long?, now: Long): Boolean = shownAt != null && now - shownAt in 0 until REMINDER_SHOW_MILLIS

/** "Time to stand up and stretch", "Time for a glass of water", or both at once. */
fun reminderText(kinds: Collection<ReminderKind>, locale: PlatformLocale): String {
    val t = strings(locale)
    return when {
        ReminderKind.STRETCH in kinds && ReminderKind.WATER in kinds -> t.reminderBoth
        ReminderKind.STRETCH in kinds -> t.reminderStretch
        else -> t.reminderWater
    }
}

/** Whether day mode says the phone can come off the charger: charging and at [percent] or more (0 is off). */
fun batteryCareDue(battery: BatteryInfo?, percent: Int): Boolean =
    percent > 0 && battery != null && battery.charging && battery.level * 100 >= percent - 0.5f

/** "Charged to 85% · you can unplug". */
fun batteryCareText(battery: BatteryInfo, locale: PlatformLocale): String =
    strings(locale).batteryCare((battery.level * 100).toInt())
