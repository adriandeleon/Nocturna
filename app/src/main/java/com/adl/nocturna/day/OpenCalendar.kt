package com.adl.nocturna.day

import android.content.ContentUris
import android.content.Intent
import android.provider.CalendarContract
import java.time.LocalDate
import java.time.LocalTime
import java.time.ZonedDateTime

/**
 * Intents that open the phone's calendar app from day mode. They use the calendar provider's
 * standard URIs, so any calendar app that handles them works (Google Calendar, Samsung, Etar…).
 */
object OpenCalendar {
    /** The calendar app's day view at [millis]. */
    fun at(millis: Long): Intent = Intent(
        Intent.ACTION_VIEW,
        CalendarContract.CONTENT_URI.buildUpon().appendPath("time").also { ContentUris.appendId(it, millis) }.build(),
    )

    /** The calendar app on [day], scrolled to a sensible hour (see [dayOpenTime]). */
    fun day(day: LocalDate, now: ZonedDateTime): Intent = at(dayOpenTime(day, now))

    /**
     * The event itself. The begin and end times pick out the occurrence of a repeating event; without
     * an event id (shouldn't happen) it falls back to the day view at the event's start.
     */
    fun event(event: CalendarEvent): Intent =
        if (event.eventId <= 0L) at(event.begin)
        else Intent(Intent.ACTION_VIEW, ContentUris.withAppendedId(CalendarContract.Events.CONTENT_URI, event.eventId))
            .putExtra(CalendarContract.EXTRA_EVENT_BEGIN_TIME, event.begin)
            .putExtra(CalendarContract.EXTRA_EVENT_END_TIME, event.end)
            .putExtra(CalendarContract.EXTRA_EVENT_ALL_DAY, event.allDay)
}

/** The hour a day view opens at: the current time for today, the start of the working day otherwise. */
private val WORKDAY_START = LocalTime.of(9, 0)

/**
 * The moment to open the calendar at for [day]: now if it's today (so the view shows what's next),
 * otherwise 09:00 that day in the same zone.
 */
fun dayOpenTime(day: LocalDate, now: ZonedDateTime): Long =
    if (day == now.toLocalDate()) now.toInstant().toEpochMilli()
    else day.atTime(WORKDAY_START).atZone(now.zone).toInstant().toEpochMilli()
