package com.adl.nocturna.alarm

import com.adl.nocturna.data.Alarm
import com.adl.nocturna.day.CalendarEvent
import com.adl.nocturna.day.covers
import kotlinx.datetime.DateTimeUnit
import kotlinx.datetime.LocalDate
import kotlinx.datetime.TimeZone
import kotlinx.datetime.plus
import kotlinx.datetime.toLocalDateTime
import kotlin.time.Instant

/**
 * Days off: dates whose all-day calendar events have a title matching one of the user's keywords, each
 * with the title of the event that made it one. Alarms with [Alarm.skipDaysOff] don't ring on them.
 */
typealias DaysOff = Map<LocalDate, String>

/** One ring an alarm skips because it falls on a day off, and the event that made it one. */
data class DayOffSkip(val date: LocalDate, val title: String)

/** The keywords a new install starts with, as the user edits them: comma-separated. */
const val DEFAULT_DAY_OFF_KEYWORDS = "holiday, vacation, day off, PTO, OOO, vacaciones, día libre, festivo"

/**
 * How many days ahead the calendar is read for days off. A ring further away than this is never skipped,
 * since the calendar wasn't read that far.
 */
const val DAYS_OFF_LOOK_AHEAD = 60

/** The keywords in [text], comma-separated, normalised for [matchesDayOffKeyword]; blanks dropped. */
fun parseDayOffKeywords(text: String): List<String> =
    text.split(',').map { normalizeForMatch(it) }.filter { it.isNotEmpty() }.distinct()

/**
 * Whether [title] contains one of [keywords] (from [parseDayOffKeywords]) as whole words, ignoring case
 * and accents: "Día Libre" and "dia libre" match "día libre", but "laptop" doesn't match "PTO".
 */
fun matchesDayOffKeyword(title: String, keywords: List<String>): Boolean {
    val t = normalizeForMatch(title)
    return keywords.any { k -> containsWord(t, k) }
}

/**
 * The days off from [first] to [last]: the dates covered by an all-day event in [events] whose title
 * matches one of [keywords]. Timed events never make a day off.
 */
fun daysOff(events: List<CalendarEvent>, keywords: List<String>, first: LocalDate, last: LocalDate): DaysOff {
    if (keywords.isEmpty()) return emptyMap()
    val matching = events.filter { it.allDay && matchesDayOffKeyword(it.title, keywords) }
    if (matching.isEmpty()) return emptyMap()
    val result = linkedMapOf<LocalDate, String>()
    var day = first
    while (day <= last) {
        // An all-day event's dates don't depend on the zone.
        matching.firstOrNull { it.covers(day, TimeZone.UTC) }?.let { result[day] = it.title }
        day = day.plus(1, DateTimeUnit.DAY)
    }
    return result
}

/** Whether [daysOff] can change when this alarm rings. */
private fun Alarm.skipsOn(daysOff: DaysOff) = skipDaysOff && repeats && daysOff.isNotEmpty()

/**
 * The next time this alarm rings strictly after [now]: [Alarm.nextTrigger], which already leaves out a
 * *Skip next* date, moved past any [daysOff] when [Alarm.skipDaysOff] is on. A ring more than
 * [DAYS_OFF_LOOK_AHEAD] days away is never skipped, so this always ends.
 */
fun Alarm.nextRing(now: Instant, zone: TimeZone, daysOff: DaysOff): Instant =
    nextRingAndSkips(now, zone, daysOff).first

/** The first ring this alarm skips because of [daysOff] before its [nextRing], or null for none. */
fun Alarm.nextDayOffSkip(now: Instant, zone: TimeZone, daysOff: DaysOff): DayOffSkip? =
    nextRingAndSkips(now, zone, daysOff).second.firstOrNull()

/** When the first ring skipped because of [daysOff] would have rung, or null for none. */
fun Alarm.firstSkippedRing(now: Instant, zone: TimeZone, daysOff: DaysOff): Instant? {
    if (!skipsOn(daysOff)) return null
    val t = nextTrigger(now, zone)
    return t.takeIf { it.toLocalDateTime(zone).date in daysOff && it < nextRing(now, zone, daysOff) }
}

private fun Alarm.nextRingAndSkips(now: Instant, zone: TimeZone, daysOff: DaysOff): Pair<Instant, List<DayOffSkip>> {
    var t = nextTrigger(now, zone)
    if (!skipsOn(daysOff)) return t to emptyList()
    val limit = now.toLocalDateTime(zone).date.plus(DAYS_OFF_LOOK_AHEAD, DateTimeUnit.DAY)
    val skips = mutableListOf<DayOffSkip>()
    while (true) {
        val date = t.toLocalDateTime(zone).date
        val title = daysOff[date]
        if (title == null || date > limit) return t to skips
        skips += DayOffSkip(date, title)
        t = nextTrigger(t, zone)
    }
}

/**
 * The date *Skip next* is skipping, counting [daysOff]: like [Alarm.skipping], but measured from the next
 * ring that isn't a day off, so the button never offers to skip a ring that won't happen anyway.
 */
fun Alarm.skipping(now: Instant, zone: TimeZone, daysOff: DaysOff): LocalDate? =
    skipDate?.takeIf { repeats && copy(skipDate = null).nextRing(now, zone, daysOff).toLocalDateTime(zone).date <= it }

/** *Skip next* counting [daysOff]: skips the next ring that isn't a day off, or brings back a skipped one. */
fun Alarm.toggleSkip(now: Instant, zone: TimeZone, daysOff: DaysOff): Alarm = when {
    !repeats -> this
    skipping(now, zone, daysOff) != null -> copy(skipDate = null)
    else -> copy(skipDate = copy(skipDate = null).nextRing(now, zone, daysOff).toLocalDateTime(zone).date)
}

private fun containsWord(text: String, word: String): Boolean {
    var from = 0
    while (true) {
        val i = text.indexOf(word, from)
        if (i < 0) return false
        val before = text.getOrNull(i - 1)
        val after = text.getOrNull(i + word.length)
        if ((before == null || !before.isLetterOrDigit()) && (after == null || !after.isLetterOrDigit())) return true
        from = i + 1
    }
}

/** Lower case, accents taken off the letters that have them, and runs of spaces made one. */
internal fun normalizeForMatch(s: String): String {
    val sb = StringBuilder(s.length)
    var space = false
    for (c in s.trim().lowercase()) {
        if (c.isWhitespace()) {
            space = true
            continue
        }
        if (space && sb.isNotEmpty()) sb.append(' ')
        space = false
        sb.append(PLAIN_LETTERS[c] ?: c)
    }
    return sb.toString()
}

private val PLAIN_LETTERS: Map<Char, Char> = buildMap {
    fun put(accented: String, plain: Char) = accented.forEach { put(it, plain) }
    put("àáâãäåāăą", 'a')
    put("çćĉċč", 'c')
    put("ďđ", 'd')
    put("èéêëēĕėęě", 'e')
    put("ĝğġģ", 'g')
    put("ĥħ", 'h')
    put("ìíîïĩīĭįı", 'i')
    put("ĵ", 'j')
    put("ķ", 'k')
    put("ĺļľŀł", 'l')
    put("ñńņňŉ", 'n')
    put("òóôõöøōŏő", 'o')
    put("ŕŗř", 'r')
    put("śŝşšș", 's')
    put("ţťŧț", 't')
    put("ùúûüũūŭůűų", 'u')
    put("ŵ", 'w')
    put("ýÿŷ", 'y')
    put("źżž", 'z')
}
