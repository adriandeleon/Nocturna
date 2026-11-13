package com.adl.nocturna.day

import java.time.DayOfWeek
import java.time.Instant
import java.time.LocalDate
import java.time.YearMonth
import java.time.ZoneId
import java.time.ZoneOffset

/**
 * One occurrence of a calendar event. [begin] and [end] are epoch millis; for an all-day event they
 * are UTC midnights, the way Android's calendar provider stores them.
 */
data class CalendarEvent(
    val title: String,
    val begin: Long,
    val end: Long,
    val allDay: Boolean,
    val color: Int? = null,
)

/**
 * The weeks to draw for [month], each seven days long and starting on [firstDayOfWeek]. The first and
 * last weeks are filled out with days of the months around it, so there are four to six weeks.
 */
fun monthGrid(month: YearMonth, firstDayOfWeek: DayOfWeek): List<List<LocalDate>> {
    val first = month.atDay(1)
    val lead = (first.dayOfWeek.value - firstDayOfWeek.value + 7) % 7
    val weeks = (lead + month.lengthOfMonth() + 6) / 7
    val start = first.minusDays(lead.toLong())
    return List(weeks) { w -> List(7) { d -> start.plusDays(w * 7L + d) } }
}

/**
 * The events of [day] still worth showing at [now]: all-day events first, then timed events by start
 * time. Timed events that have already ended are left out; ones in progress stay.
 */
fun eventsForDay(events: List<CalendarEvent>, day: LocalDate, zone: ZoneId, now: Instant): List<CalendarEvent> {
    val dayStart = day.atStartOfDay(zone).toInstant().toEpochMilli()
    val dayEnd = day.plusDays(1).atStartOfDay(zone).toInstant().toEpochMilli()
    val nowMillis = now.toEpochMilli()
    return events
        .filter { e ->
            if (e.allDay) {
                // All-day events are dates, not instants: read them in UTC, whatever the local zone.
                val first = Instant.ofEpochMilli(e.begin).atZone(ZoneOffset.UTC).toLocalDate()
                val last = Instant.ofEpochMilli(e.end).atZone(ZoneOffset.UTC).toLocalDate() // exclusive
                !day.isBefore(first) && day.isBefore(maxOf(last, first.plusDays(1)))
            } else {
                e.begin < dayEnd && maxOf(e.end, e.begin + 1) > dayStart && e.end > nowMillis
            }
        }
        .sortedWith(compareByDescending<CalendarEvent> { it.allDay }.thenBy { it.begin }.thenBy { it.title })
}

/** Whether a timed event is happening at [now]. */
fun CalendarEvent.isOngoing(now: Instant): Boolean = !allDay && now.toEpochMilli() in begin until end

/**
 * How many rows of the given [heights], [gap] apart, fit whole in [max]. When they don't all fit,
 * room is kept for a "+N more" line [moreHeight] tall below the ones shown.
 */
fun rowsThatFit(heights: List<Int>, gap: Int, max: Int, moreHeight: Int): Int {
    fun fit(limit: Int): Int {
        var y = 0
        var n = 0
        for (h in heights) {
            if (y + h > limit) break
            y += h + gap
            n++
        }
        return n
    }
    val all = fit(max)
    return if (all == heights.size) all else fit(max - moreHeight - gap)
}
