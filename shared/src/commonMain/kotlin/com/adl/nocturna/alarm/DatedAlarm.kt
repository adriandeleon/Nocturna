package com.adl.nocturna.alarm

import com.adl.nocturna.data.Alarm
import kotlinx.datetime.LocalDate
import kotlinx.datetime.TimeZone
import kotlinx.datetime.atTime
import kotlinx.datetime.toInstant
import kotlinx.datetime.toLocalDateTime
import kotlin.time.Instant

/*
 * One-time alarms on a date: an alarm with no repeat days can name the day it rings on ([Alarm.date]).
 * It rings once, at its clock time on that day, and is then switched off like any one-time alarm.
 * Days off and *Skip next* don't apply to it.
 */

/**
 * When [hour]:[minute] on [date] is, read in [zone]. On a daylight-saving day a time inside the
 * spring-forward gap moves later by the length of the gap, and a time that happens twice when the clocks
 * go back is the first of the two.
 */
fun ringInstant(date: LocalDate, hour: Int, minute: Int, zone: TimeZone): Instant =
    date.atTime(hour, minute).toInstant(zone)

/** When this alarm rings on its [Alarm.ringDate], if that is still after [now]; null without a date or once it has passed. */
fun Alarm.datedRing(now: Instant, zone: TimeZone): Instant? =
    ringDate?.let { ringInstant(it, hour, minute, zone) }?.takeIf { it > now }

/** Whether this alarm has a [Alarm.ringDate] whose time is not after [now]. */
fun Alarm.datePassed(now: Instant, zone: TimeZone): Boolean =
    ringDate != null && datedRing(now, zone) == null

/** Whether an alarm at [hour]:[minute] can be set for [date]: that time must still be ahead of [now]. */
fun canRingOn(date: LocalDate, hour: Int, minute: Int, now: Instant, zone: TimeZone): Boolean =
    ringInstant(date, hour, minute, zone) > now

/**
 * This alarm set to ring on [date], or with its date taken off when [date] is null. Null when [date] can't
 * be saved, being in the past at the alarm's time. Setting a date takes off the repeat days, and switches
 * the alarm on.
 */
fun Alarm.withDate(date: LocalDate?, now: Instant, zone: TimeZone): Alarm? = when {
    date == null -> copy(date = null)
    !canRingOn(date, hour, minute, now, zone) -> null
    else -> copy(date = date, days = 0, skipDate = null, enabled = true)
}

/** This alarm with repeat [days]: repeating takes off its date, since the two don't go together. */
fun Alarm.withDays(days: Int): Alarm = copy(days = days, date = if (days != 0) null else date)

/**
 * This alarm moved to [hour]:[minute] and switched on, or null when it has a date on which that time has
 * already passed. A date before today is left over from a ring that's done, so it is taken off and the
 * alarm rings the next time its clock time comes.
 */
fun Alarm.withTime(hour: Int, minute: Int, now: Instant, zone: TimeZone): Alarm? {
    val moved = copy(hour = hour, minute = minute, enabled = true)
    val day = moved.ringDate ?: return moved
    return when {
        day < now.toLocalDateTime(zone).date -> moved.copy(date = null)
        moved.datePassed(now, zone) -> null
        else -> moved
    }
}

/**
 * This alarm switched on or off. Switching on an alarm whose date has passed takes the date off, so it
 * rings the next time its clock time comes, as a one-time alarm without a date does.
 */
fun Alarm.switched(on: Boolean, now: Instant, zone: TimeZone): Alarm =
    if (on && datePassed(now, zone)) copy(enabled = true, date = null) else copy(enabled = on)
