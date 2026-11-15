package com.adl.nocturna.day

import com.adl.nocturna.text.DateTimePattern
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime
import kotlin.time.Instant

/** How far ahead day mode counts down to the next event: an hour. */
const val COUNTDOWN_WINDOW_MILLIS = 60 * 60_000L

/** How long after its moment a chime may still play, so a late tick doesn't skip it. */
private const val CHIME_GRACE_MILLIS = 60_000L

/**
 * The next timed event that starts within the hour after [now], for the countdown on day mode's
 * note; null when there is none. All-day events and ones already under way never count down. The
 * events are the ones the note shows, so hidden calendars and declined invitations are already out.
 */
fun upcomingMeeting(events: List<CalendarEvent>, now: Instant): CalendarEvent? {
    val t = now.toEpochMilliseconds()
    return events
        .filter { !it.allDay && it.begin > t && it.begin - t <= COUNTDOWN_WINDOW_MILLIS }
        .minWithOrNull(compareBy<CalendarEvent> { it.begin }.thenBy { it.title })
}

/** Whole minutes until [event] starts at [now], rounded up: 12 at 11 min 30 s, 1 in its last minute. */
fun minutesUntil(event: CalendarEvent, now: Instant): Int {
    val left = event.begin - now.toEpochMilliseconds()
    return ((left + 59_999) / 60_000).toInt().coerceAtLeast(0)
}

/**
 * The countdown over the event's title on the sticky note, with its start time in [time] read in
 * [zone], e.g. "In 12 min · 10:00".
 */
fun countdownLabel(event: CalendarEvent, now: Instant, zone: TimeZone, time: DateTimePattern): String =
    "In ${minutesUntil(event, now)} min · ${time.format(Instant.fromEpochMilliseconds(event.begin).toLocalDateTime(zone))}"

/** The countdown on its own line, where the note isn't showing, e.g. "Standup in 12 min". */
fun countdownLine(event: CalendarEvent, now: Instant): String =
    "${event.title.trim().ifEmpty { "Next event" }} in ${minutesUntil(event, now)} min"

/** One occurrence of an event, so its chime plays only once. */
fun chimeKey(event: CalendarEvent): String = "${event.eventId}@${event.begin}"

/**
 * Whether the soft chime for [event] should play at [now]: [minutesBefore] minutes before it starts
 * (0 is off), or within a minute after that point, and only if [chimed] (the [chimeKey] of the last
 * chime played) isn't this event. Opening day mode later than that doesn't chime.
 */
fun shouldChime(event: CalendarEvent?, now: Instant, minutesBefore: Int, chimed: String?): Boolean {
    if (event == null || event.allDay || minutesBefore <= 0 || chimeKey(event) == chimed) return false
    val at = event.begin - minutesBefore * 60_000L
    val t = now.toEpochMilliseconds()
    return t >= at && t < at + CHIME_GRACE_MILLIS && t < event.begin
}
