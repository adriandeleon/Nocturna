package com.adl.nocturna.day

import com.adl.nocturna.data.CalendarView

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
    /** The event this is an occurrence of, to open it in the calendar app; 0 when unknown. */
    val eventId: Long = 0L,
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
    val nowMillis = now.toEpochMilli()
    return events
        .filter { it.covers(day, zone) && (it.allDay || it.end > nowMillis) }
        .sortedWith(compareByDescending<CalendarEvent> { it.allDay }.thenBy { it.begin }.thenBy { it.title })
}

/** Which of [days] have at least one event, for the dots on the calendar page. */
fun daysWithEvents(events: List<CalendarEvent>, days: List<LocalDate>, zone: ZoneId): Set<LocalDate> =
    days.filterTo(mutableSetOf()) { day -> events.any { it.covers(day, zone) } }

/** Whether the event takes up any of [day] in [zone]. */
fun CalendarEvent.covers(day: LocalDate, zone: ZoneId): Boolean {
    if (allDay) {
        // All-day events are dates, not instants: read them in UTC, whatever the local zone.
        val first = Instant.ofEpochMilli(begin).atZone(ZoneOffset.UTC).toLocalDate()
        val last = Instant.ofEpochMilli(end).atZone(ZoneOffset.UTC).toLocalDate() // exclusive
        return !day.isBefore(first) && day.isBefore(maxOf(last, first.plusDays(1)))
    }
    val dayStart = day.atStartOfDay(zone).toInstant().toEpochMilli()
    val dayEnd = day.plusDays(1).atStartOfDay(zone).toInstant().toEpochMilli()
    return begin < dayEnd && maxOf(end, begin + 1) > dayStart
}

/** A friendly hello for the sticky note, by the hour of the day. */
fun greeting(hour: Int): String = when (hour) {
    in 5..11 -> "Good morning!"
    in 12..17 -> "Good afternoon!"
    else -> "Good evening!"
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

/** The seven days of the week that holds [day], starting on [firstDayOfWeek]. */
fun weekOf(day: LocalDate, firstDayOfWeek: DayOfWeek): List<LocalDate> {
    val start = day.minusDays(((day.dayOfWeek.value - firstDayOfWeek.value + 7) % 7).toLong())
    return List(7) { start.plusDays(it.toLong()) }
}

/** All of [day]'s events, finished ones included: all-day events first, then by start time. */
fun eventsOnDay(events: List<CalendarEvent>, day: LocalDate, zone: ZoneId): List<CalendarEvent> =
    events.filter { it.covers(day, zone) }
        .sortedWith(compareByDescending<CalendarEvent> { it.allDay }.thenBy { it.begin }.thenBy { it.title })

/**
 * The view a swipe on the calendar page moves to: towards more detail (month, week, day) or back.
 * It stops at either end rather than wrapping round.
 */
fun swipeView(view: CalendarView, towardsDetail: Boolean): CalendarView {
    val next = view.ordinal + if (towardsDetail) 1 else -1
    return CalendarView.entries[next.coerceIn(0, CalendarView.entries.size - 1)]
}

/** How a calendar view is called on the page and in Settings. */
fun CalendarView.label(): String = when (this) {
    CalendarView.MONTH -> "Month"
    CalendarView.WEEK -> "Week"
    CalendarView.DAY -> "Day"
}
