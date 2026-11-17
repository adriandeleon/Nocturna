package com.adl.nocturna.clock

import com.adl.nocturna.text.PlatformLocale
import com.adl.nocturna.text.strings
import kotlin.math.roundToInt
import kotlin.time.Duration.Companion.hours
import kotlin.time.Instant
import kotlinx.datetime.LocalDate
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime

/**
 * One night's battery use while the night clock showed and the phone wasn't charging: [millis] of it, and
 * [percent] of the charge used in that time. A night is named by the evening it began ([nightOf]).
 */
data class NightUse(val night: LocalDate, val millis: Long, val percent: Int)

/** Where tracking last left off: when, in epoch millis, and the battery level then, in whole percent. */
data class NightBatteryMark(val millis: Long, val level: Int)

/** How many nights the log keeps. */
const val NIGHT_BATTERY_NIGHTS = 14

/** How many of the latest nights the estimate averages. */
const val NIGHT_BATTERY_AVERAGED = 7

private const val HOUR_MILLIS = 3_600_000L

/** The night [instant] belongs to: the evening it began, so 02:00 on the 16th is the night of the 15th. */
fun nightOf(instant: Instant, zone: TimeZone): LocalDate = (instant - 12.hours).toLocalDateTime(zone).date

/**
 * One step of tracking the battery at night. [tracking] is whether the night clock shows now with the phone off
 * its charger, and [level] the battery in whole percent (null while it's unknown). The time and charge since
 * [mark] go into [night]'s entry when the level has moved or tracking stops; between two readings of the same
 * level nothing is written, so the log changes about once per percent. A level that went up (the phone was
 * charged meanwhile) counts as none used.
 *
 * @return the log (the same list when nothing changed) and where tracking now stands.
 */
fun trackNightBattery(
    log: List<NightUse>,
    mark: NightBatteryMark?,
    tracking: Boolean,
    millis: Long,
    level: Int?,
    night: LocalDate,
): Pair<List<NightUse>, NightBatteryMark?> {
    if (mark == null) return log to if (tracking && level != null) NightBatteryMark(millis, level) else null
    if (tracking && (level == null || level == mark.level)) return log to mark
    val end = level ?: mark.level
    val next = addNightUse(log, night, (millis - mark.millis).coerceAtLeast(0), (mark.level - end).coerceAtLeast(0))
    return next to if (tracking) NightBatteryMark(millis, end) else null
}

private fun addNightUse(log: List<NightUse>, night: LocalDate, millis: Long, percent: Int): List<NightUse> {
    if (millis == 0L && percent == 0) return log
    val old = log.firstOrNull { it.night == night }
    val entry = old?.let { it.copy(millis = it.millis + millis, percent = it.percent + percent) } ?: NightUse(night, millis, percent)
    return (log.filterNot { it.night == night } + entry).sortedBy { it.night }.takeLast(NIGHT_BATTERY_NIGHTS)
}

/** The battery used in [hours] at the rate of [uses], in percent; null with under an hour tracked, too little to tell. */
fun nightBatteryEstimate(uses: List<NightUse>, hours: Int = 8): Int? {
    val millis = uses.sumOf { it.millis }
    if (millis < HOUR_MILLIS) return null
    return (uses.sumOf { it.percent } * hours * HOUR_MILLIS.toDouble() / millis).roundToInt()
}

/**
 * What Settings says about the battery at night: the latest night ("Sun 15 Nov: 12% in 7 h 40 m") and, with
 * an hour or more tracked over the last [NIGHT_BATTERY_AVERAGED] nights, what 8 hours take ("About 13% for
 * 8 hours, over the last 5 nights"). Null when nothing is logged. [dayLabel] names a night's date.
 */
fun nightBatterySummary(uses: List<NightUse>, locale: PlatformLocale, dayLabel: (LocalDate) -> String): String? {
    val last = uses.maxByOrNull { it.night } ?: return null
    val t = strings(locale)
    val minutes = last.millis / 60_000
    val lastLine = t.nightBatteryNight(dayLabel(last.night), last.percent, t.hoursMinutes(minutes / 60, minutes % 60))
    val recent = uses.sortedBy { it.night }.takeLast(NIGHT_BATTERY_AVERAGED)
    val estimate = nightBatteryEstimate(recent) ?: return lastLine
    return lastLine + "\n" + t.nightBatteryEstimate(estimate, 8, recent.size)
}
