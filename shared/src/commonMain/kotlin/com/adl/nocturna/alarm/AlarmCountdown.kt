package com.adl.nocturna.alarm

import com.adl.nocturna.text.PlatformLocale
import com.adl.nocturna.text.formatTime
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime
import kotlin.time.Duration
import kotlin.time.Duration.Companion.hours
import kotlin.time.Instant

/** How far ahead the clock's info line counts down to the next alarm. */
val ALARM_COUNTDOWN_WINDOW: Duration = 12.hours

/** The countdown steps in this many minutes, so it doesn't change every minute. */
private const val COUNTDOWN_STEP_MINUTES = 5L

/**
 * How long until [alarmAt] from [now], rounded down to [COUNTDOWN_STEP_MINUTES]: "in 7 h 20 m", "in 7 h",
 * "in 45 m", and "in < 5 m" in the last five minutes. Null when the alarm has gone off or is more than
 * [ALARM_COUNTDOWN_WINDOW] away.
 */
fun timeToAlarmText(alarmAt: Instant, now: Instant): String? {
    val left = alarmAt - now
    if (left <= Duration.ZERO || left > ALARM_COUNTDOWN_WINDOW) return null
    val minutes = left.inWholeMinutes / COUNTDOWN_STEP_MINUTES * COUNTDOWN_STEP_MINUTES
    if (minutes == 0L) return "in < $COUNTDOWN_STEP_MINUTES m"
    return "in ${hoursAndMinutes(minutes)}"
}

/**
 * The info line's next-alarm item with the countdown, e.g. "6:30 am · in 7 h 20 m", the time read in
 * [zone]; null when [timeToAlarmText] has no countdown, and the item shows the day and time as before.
 */
fun alarmCountdownLabel(
    alarmAt: Instant,
    now: Instant,
    zone: TimeZone,
    use24h: Boolean,
    locale: PlatformLocale,
): String? {
    val left = timeToAlarmText(alarmAt, now) ?: return null
    return "${formatTime(alarmAt.toLocalDateTime(zone).time, use24h, locale)} · $left"
}

/** [minutes] as "7 h 20 m", "7 h" or "45 m". */
internal fun hoursAndMinutes(minutes: Long): String {
    val h = minutes / 60
    val m = minutes % 60
    return when {
        h == 0L -> "$m m"
        m == 0L -> "$h h"
        else -> "$h h $m m"
    }
}
