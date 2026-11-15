package com.adl.nocturna.alarm

import com.adl.nocturna.text.PlatformLocale
import com.adl.nocturna.text.formatTime
import com.adl.nocturna.text.strings
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime
import kotlin.math.roundToLong
import kotlin.time.Duration
import kotlin.time.Duration.Companion.minutes
import kotlin.time.Instant

/*
 * Bedtime reminder: a quiet notification the evening before a morning alarm, early enough to get the
 * chosen hours of sleep.
 */

/** The reminder goes out this long before the sleep has to start, to get ready for bed. */
val BEDTIME_WIND_DOWN: Duration = 15.minutes

/** Alarms at or after this hour aren't morning alarms, and get no reminder. */
const val MORNING_ENDS_HOUR = 12

/** How late a delayed reminder may still go out; any later and it isn't bedtime any more. */
val BEDTIME_GRACE: Duration = 30.minutes

/**
 * When the reminder for [alarmAt] goes out: [sleepHours] plus [BEDTIME_WIND_DOWN] before it. Null when
 * the reminder is off ([sleepHours] 0) or the alarm, read in [zone], isn't in the morning.
 */
fun bedtimeFor(alarmAt: Instant, sleepHours: Float, zone: TimeZone): Instant? {
    if (sleepHours <= 0f) return null
    if (alarmAt.toLocalDateTime(zone).hour >= MORNING_ENDS_HOUR) return null
    return alarmAt - sleepMinutes(sleepHours).minutes - BEDTIME_WIND_DOWN
}

/**
 * When to schedule the reminder for the next alarm, [nextAlarm], at [now]: [bedtimeFor] it, so the
 * reminder goes out the evening before the alarm's morning. Null when there is no alarm, no reminder
 * for it, or its time has already passed.
 */
fun bedtimeReminderAt(nextAlarm: Instant?, sleepHours: Float, now: Instant, zone: TimeZone): Instant? {
    if (nextAlarm == null || nextAlarm <= now) return null
    return bedtimeFor(nextAlarm, sleepHours, zone)?.takeIf { it > now }
}

/**
 * Whether the reminder that fired at [now] should still be shown for [nextAlarm]: it is that alarm's
 * bedtime, give or take a minute early or [BEDTIME_GRACE] late (an inexact alarm can be held back), and
 * the clock isn't already showing ([clockShowing]), which means the user is in bed.
 */
fun shouldRemindBedtime(
    nextAlarm: Instant?,
    sleepHours: Float,
    now: Instant,
    zone: TimeZone,
    clockShowing: Boolean,
): Boolean {
    if (clockShowing || nextAlarm == null || nextAlarm <= now) return false
    val at = bedtimeFor(nextAlarm, sleepHours, zone) ?: return false
    return now >= at - 1.minutes && now < at + BEDTIME_GRACE
}

/** The reminder's text, e.g. "Bedtime: 8 h before your 6:30 am alarm", the alarm read in [zone]. */
fun bedtimeReminderText(
    alarmAt: Instant,
    sleepHours: Float,
    zone: TimeZone,
    use24h: Boolean,
    locale: PlatformLocale,
): String {
    val time = formatTime(alarmAt.toLocalDateTime(zone).time, use24h, locale)
    return strings(locale).bedtimeReminder(sleepHoursLabel(sleepHours, locale), time)
}

/** A sleep length as on the setting: "8 h", "7 h 30 m"; "Off" for 0. */
fun sleepHoursLabel(sleepHours: Float, locale: PlatformLocale): String =
    if (sleepHours <= 0f) strings(locale).off else hoursAndMinutes(sleepMinutes(sleepHours), locale)

private fun sleepMinutes(sleepHours: Float): Long = (sleepHours * 60).roundToLong()
