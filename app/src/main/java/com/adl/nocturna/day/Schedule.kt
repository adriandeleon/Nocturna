package com.adl.nocturna.day

import com.adl.nocturna.data.DayMode
import com.adl.nocturna.data.Settings
import java.time.LocalDate
import java.time.ZonedDateTime

/*
 * The day mode schedule: a window from Settings.dayModeStart to Settings.dayModeEnd that opens on each
 * of Settings.dayModeDays. An end at or before the start closes the next day, so 22:00–06:00 on Friday
 * runs into Saturday morning, and an end equal to the start keeps day mode on for 24 hours. The start
 * is included and the end is not. Times are read in the zone of the `now` passed in.
 */

/** Whether day mode shows at [now]: its mode, and when scheduled, the schedule and any override. */
fun isDayModeActive(settings: Settings, now: ZonedDateTime): Boolean = when (settings.dayMode) {
    DayMode.OFF -> false
    DayMode.ON -> true
    DayMode.SCHEDULED -> inDaySchedule(settings, now) != isOverridden(settings, now)
}

/** Whether the schedule covers [now], whatever the mode and the override. */
fun inDaySchedule(settings: Settings, now: ZonedDateTime): Boolean {
    val today = now.toLocalDate()
    return datesFrom(today.minusDays(1), today).any { date ->
        val (start, end) = window(settings, date, now) ?: return@any false
        !now.isBefore(start) && now.isBefore(end)
    }
}

/** The first moment after [now] when the schedule turns day mode on or off, or null when it never does. */
fun nextDayScheduleEdge(settings: Settings, now: ZonedDateTime): ZonedDateTime? {
    val inside = inDaySchedule(settings, now)
    val today = now.toLocalDate()
    // A window opens within a week if any day is set, and one that is open closes within about a day.
    return datesFrom(today.minusDays(1), today.plusDays(7))
        .flatMap { date -> window(settings, date, now)?.toList().orEmpty() }
        .filter { it.isAfter(now) }
        .sortedBy { it.toInstant() }
        .firstOrNull { inDaySchedule(settings, it) != inside }
}

/**
 * The settings after the clock's Day mode / Clock only button is pressed at [now]. Off and on swap. On a
 * schedule the button flips day mode until the schedule's next edge, or ends a flip already running.
 */
fun Settings.withDayModeToggled(now: ZonedDateTime): Settings = when (dayMode) {
    DayMode.OFF -> copy(dayMode = DayMode.ON)
    DayMode.ON -> copy(dayMode = DayMode.OFF)
    DayMode.SCHEDULED -> copy(
        dayModeOverrideUntil = if (isOverridden(this, now)) 0L
        else nextDayScheduleEdge(this, now)?.toInstant()?.toEpochMilli() ?: Long.MAX_VALUE,
    )
}

private fun isOverridden(settings: Settings, now: ZonedDateTime) =
    now.toInstant().toEpochMilli() < settings.dayModeOverrideUntil

/** The window opening on [date], or null when the schedule skips that day. */
private fun window(settings: Settings, date: LocalDate, now: ZonedDateTime): Pair<ZonedDateTime, ZonedDateTime>? {
    if (settings.dayModeDays and (1 shl (date.dayOfWeek.value - 1)) == 0) return null
    val endDate = if (settings.dayModeEnd <= settings.dayModeStart) date.plusDays(1) else date
    // atZone moves a time inside a spring-forward gap to just after it.
    val start = date.atTime(settings.dayModeStart / 60, settings.dayModeStart % 60).atZone(now.zone)
    val end = endDate.atTime(settings.dayModeEnd / 60, settings.dayModeEnd % 60).atZone(now.zone)
    return start to end
}

private fun datesFrom(first: LocalDate, last: LocalDate): List<LocalDate> =
    generateSequence(first) { it.plusDays(1) }.takeWhile { !it.isAfter(last) }.toList()
