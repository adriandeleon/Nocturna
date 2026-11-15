package com.adl.nocturna.widget

import com.adl.nocturna.text.DateTimePattern
import com.adl.nocturna.text.PlatformLocale
import com.adl.nocturna.text.formatTime
import com.adl.nocturna.text.shortName
import kotlin.time.Instant
import kotlinx.datetime.TimeZone
import kotlinx.datetime.daysUntil
import kotlinx.datetime.toLocalDateTime

// Text and sizes for the home-screen widget, which Android draws from RemoteViews outside the app.

/**
 * The widget's next-alarm line: "Tue 07:00" within the coming week, or with the date ([farDate], e.g.
 * "Tue 22 Dec") further off. It never says "today" or "tomorrow", so it stays right as midnight passes and
 * needs updating only when the alarm changes. Null when there's no alarm, or it's already gone off.
 */
fun nextAlarmText(
    alarmAt: Instant?,
    now: Instant,
    zone: TimeZone,
    use24h: Boolean,
    locale: PlatformLocale,
    farDate: DateTimePattern,
): String? {
    if (alarmAt == null || alarmAt <= now) return null
    val at = alarmAt.toLocalDateTime(zone)
    val days = now.toLocalDateTime(zone).date.daysUntil(at.date)
    val day = if (days < 7) at.date.dayOfWeek.shortName(locale) else farDate.format(at)
    return "$day ${formatTime(at.time, use24h, locale)}"
}

/**
 * The clock's text size in sp for a widget [widthDp] wide and [heightDp] tall: as big as fits beside the
 * alarm line and buttons, which take about [CHROME_DP] of the height, between [MIN_CLOCK_SP] and [MAX_CLOCK_SP].
 */
fun widgetClockSizeSp(widthDp: Int, heightDp: Int): Int {
    // "12:34" is about 3.2 ems wide and its line about 1.2 ems tall; the widget has 12dp of padding a side.
    val byWidth = (widthDp - 24) * 5 / 16
    val byHeight = (heightDp - CHROME_DP) * 5 / 6
    return minOf(byWidth, byHeight).coerceIn(MIN_CLOCK_SP, MAX_CLOCK_SP)
}

const val CHROME_DP = 100
const val MIN_CLOCK_SP = 32
const val MAX_CLOCK_SP = 96
