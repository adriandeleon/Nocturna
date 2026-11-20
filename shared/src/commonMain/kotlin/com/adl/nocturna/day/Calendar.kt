package com.adl.nocturna.day

import com.adl.nocturna.data.CalendarView
import com.adl.nocturna.text.PlatformLocale
import com.adl.nocturna.text.strings
import kotlinx.datetime.DateTimeUnit
import kotlinx.datetime.DayOfWeek
import kotlinx.datetime.LocalDate
import kotlinx.datetime.TimeZone
import kotlinx.datetime.YearMonth
import kotlinx.datetime.atStartOfDayIn
import kotlinx.datetime.isoDayNumber
import kotlinx.datetime.minus
import kotlinx.datetime.plus
import kotlinx.datetime.toLocalDateTime
import kotlin.time.Instant

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
    val first = month.firstDay
    val lead = (first.dayOfWeek.isoDayNumber - firstDayOfWeek.isoDayNumber + 7) % 7
    val weeks = (lead + month.numberOfDays + 6) / 7
    val start = first.minus(lead, DateTimeUnit.DAY)
    return List(weeks) { w -> List(7) { d -> start.plus(w * 7 + d, DateTimeUnit.DAY) } }
}

/**
 * The events of [day] still worth showing at [now]: all-day events first, then timed events by start
 * time. Timed events that have already ended are left out; ones in progress stay.
 */
fun eventsForDay(events: List<CalendarEvent>, day: LocalDate, zone: TimeZone, now: Instant): List<CalendarEvent> {
    val nowMillis = now.toEpochMilliseconds()
    return events
        .filter { it.covers(day, zone) && (it.allDay || it.end > nowMillis) }
        .sortedWith(compareByDescending<CalendarEvent> { it.allDay }.thenBy { it.begin }.thenBy { it.title })
}

/** Which of [days] have at least one event, for the dots on the calendar page. */
fun daysWithEvents(events: List<CalendarEvent>, days: List<LocalDate>, zone: TimeZone): Set<LocalDate> =
    days.filterTo(mutableSetOf()) { day -> events.any { it.covers(day, zone) } }

/** Whether the event takes up any of [day] in [zone]. */
fun CalendarEvent.covers(day: LocalDate, zone: TimeZone): Boolean {
    if (allDay) {
        // All-day events are dates, not instants: read them in UTC, whatever the local zone.
        val first = Instant.fromEpochMilliseconds(begin).toLocalDateTime(TimeZone.UTC).date
        val last = Instant.fromEpochMilliseconds(end).toLocalDateTime(TimeZone.UTC).date // exclusive
        return day >= first && day < maxOf(last, first.plus(1, DateTimeUnit.DAY))
    }
    val dayStart = day.atStartOfDayIn(zone).toEpochMilliseconds()
    val dayEnd = day.plus(1, DateTimeUnit.DAY).atStartOfDayIn(zone).toEpochMilliseconds()
    return begin < dayEnd && maxOf(end, begin + 1) > dayStart
}

/** A friendly hello for the sticky note, by the hour of the day. */
fun greeting(hour: Int, locale: PlatformLocale): String = when (hour) {
    in 5..11 -> strings(locale).goodMorning
    in 12..17 -> strings(locale).goodAfternoon
    else -> strings(locale).goodEvening
}

/** Whether a timed event is happening at [now]. */
fun CalendarEvent.isOngoing(now: Instant): Boolean = !allDay && now.toEpochMilliseconds() in begin until end

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

/**
 * Whether the sticky note fits under the clock in landscape: the clock [clock] tall, a [gap], then the
 * note at its smallest ([noteMin], its greeting and one event), all within [max]. When it doesn't, the
 * note is left out instead of being squashed into a strip.
 */
fun noteFits(clock: Int, gap: Int, noteMin: Int, max: Int): Boolean = clock + gap + noteMin <= max

/** The seven days of the week that holds [day], starting on [firstDayOfWeek]. */
fun weekOf(day: LocalDate, firstDayOfWeek: DayOfWeek): List<LocalDate> {
    val start = day.minus((day.dayOfWeek.isoDayNumber - firstDayOfWeek.isoDayNumber + 7) % 7, DateTimeUnit.DAY)
    return List(7) { start.plus(it, DateTimeUnit.DAY) }
}

/** All of [day]'s events, finished ones included: all-day events first, then by start time. */
fun eventsOnDay(events: List<CalendarEvent>, day: LocalDate, zone: TimeZone): List<CalendarEvent> =
    events.filter { it.covers(day, zone) }
        .sortedWith(compareByDescending<CalendarEvent> { it.allDay }.thenBy { it.begin }.thenBy { it.title })

/**
 * What the calendar's Day view lists for [day] at [now]: every all-day event, then the timed events
 * around the time. That is the last event to have finished, then the ones in progress or still to come
 * by start time, [timed] timed events in all. Older finished events never fill in, so late in the day the
 * list gets shorter; before any event has finished, all [timed] are ones still to come.
 */
fun dayViewEvents(events: List<CalendarEvent>, day: LocalDate, zone: TimeZone, now: Instant, timed: Int = 4): List<CalendarEvent> {
    val all = eventsOnDay(events, day, zone)
    val nowMillis = now.toEpochMilliseconds()
    val (allDay, scheduled) = all.partition { it.allDay }
    val (finished, left) = scheduled.partition { it.end <= nowMillis }
    val previous = finished.maxWithOrNull(compareBy<CalendarEvent> { it.end }.thenBy { it.begin })
    return allDay + listOfNotNull(previous) + left.take(timed - if (previous == null) 0 else 1)
}

/**
 * The view a swipe on the calendar page moves to: towards more detail (month, week, day) or back.
 * It stops at either end rather than wrapping round.
 */
fun swipeView(view: CalendarView, towardsDetail: Boolean): CalendarView {
    val next = view.ordinal + if (towardsDetail) 1 else -1
    return CalendarView.entries[next.coerceIn(0, CalendarView.entries.size - 1)]
}

/** How a calendar view is called on the page and in Settings. */
fun CalendarView.label(locale: PlatformLocale): String = when (this) {
    CalendarView.MONTH -> strings(locale).calendarMonth
    CalendarView.WEEK -> strings(locale).calendarWeek
    CalendarView.DAY -> strings(locale).calendarDay
}
