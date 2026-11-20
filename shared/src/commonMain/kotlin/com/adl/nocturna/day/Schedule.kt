package com.adl.nocturna.day

import com.adl.nocturna.data.DayMode
import com.adl.nocturna.data.Settings
import kotlinx.datetime.DateTimeUnit
import kotlinx.datetime.LocalDate
import kotlinx.datetime.TimeZone
import kotlinx.datetime.atTime
import kotlinx.datetime.isoDayNumber
import kotlinx.datetime.minus
import kotlinx.datetime.plus
import kotlinx.datetime.toInstant
import kotlinx.datetime.toLocalDateTime
import kotlin.time.Instant

/*
 * The day mode schedule: a window from Settings.dayModeStart to Settings.dayModeEnd that opens on each
 * of Settings.dayModeDays. An end at or before the start closes the next day, so 22:00–06:00 on Friday
 * runs into Saturday morning, and an end equal to the start keeps day mode on for 24 hours. The start
 * is included and the end is not. Times are read in the `zone` passed in.
 */

/** Whether day mode shows at [now]: its mode, and when scheduled, the schedule and any override. */
fun isDayModeActive(settings: Settings, now: Instant, zone: TimeZone): Boolean = when (settings.dayMode) {
    DayMode.OFF -> false
    DayMode.ON -> true
    DayMode.SCHEDULED -> inDaySchedule(settings, now, zone) != isOverridden(settings, now)
}

/** Whether the schedule covers [now], whatever the mode and the override. */
fun inDaySchedule(settings: Settings, now: Instant, zone: TimeZone): Boolean {
    val today = now.toLocalDateTime(zone).date
    return datesFrom(today.minus(1, DateTimeUnit.DAY), today).any { date ->
        val (start, end) = window(settings, date, zone) ?: return@any false
        now >= start && now < end
    }
}

/** The first moment after [now] when the schedule turns day mode on or off, or null when it never does. */
fun nextDayScheduleEdge(settings: Settings, now: Instant, zone: TimeZone): Instant? {
    val inside = inDaySchedule(settings, now, zone)
    val today = now.toLocalDateTime(zone).date
    // A window opens within a week if any day is set, and one that is open closes within about a day.
    return datesFrom(today.minus(1, DateTimeUnit.DAY), today.plus(7, DateTimeUnit.DAY))
        .flatMap { date -> window(settings, date, zone)?.toList().orEmpty() }
        .filter { it > now }
        .sorted()
        .firstOrNull { inDaySchedule(settings, it, zone) != inside }
}

/**
 * The settings after the clock's Day mode / Clock only button is pressed at [now]. Off and on swap. On a
 * schedule the button flips day mode until the schedule's next edge, or ends a flip already running.
 * Going to Clock only turns dimming back on, so a bright Day mode doesn't keep the bedside clock bright;
 * the clock's moon/sun button can still turn it off afterwards.
 */
fun Settings.withDayModeToggled(now: Instant, zone: TimeZone): Settings {
    val toggled = when (dayMode) {
        DayMode.OFF -> copy(dayMode = DayMode.ON)
        DayMode.ON -> copy(dayMode = DayMode.OFF)
        DayMode.SCHEDULED -> copy(
            dayModeOverrideUntil = if (isOverridden(this, now)) 0L
            else nextDayScheduleEdge(this, now, zone)?.toEpochMilliseconds() ?: Long.MAX_VALUE,
        )
    }
    return if (isDayModeActive(toggled, now, zone)) toggled else toggled.copy(dimEnabled = true)
}

private fun isOverridden(settings: Settings, now: Instant) = now.toEpochMilliseconds() < settings.dayModeOverrideUntil

/** The window opening on [date], or null when the schedule skips that day. */
private fun window(settings: Settings, date: LocalDate, zone: TimeZone): Pair<Instant, Instant>? {
    if (settings.dayModeDays and (1 shl (date.dayOfWeek.isoDayNumber - 1)) == 0) return null
    val endDate = if (settings.dayModeEnd <= settings.dayModeStart) date.plus(1, DateTimeUnit.DAY) else date
    // toInstant moves a time inside a spring-forward gap later by the length of the gap.
    val start = date.atTime(settings.dayModeStart / 60, settings.dayModeStart % 60).toInstant(zone)
    val end = endDate.atTime(settings.dayModeEnd / 60, settings.dayModeEnd % 60).toInstant(zone)
    return start to end
}

private fun datesFrom(first: LocalDate, last: LocalDate): List<LocalDate> =
    generateSequence(first) { it.plus(1, DateTimeUnit.DAY) }.takeWhile { it <= last }.toList()
