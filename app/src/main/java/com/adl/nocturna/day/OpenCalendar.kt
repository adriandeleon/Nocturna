package com.adl.nocturna.day

import android.content.ContentUris
import android.content.Intent
import android.provider.CalendarContract

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

    /** What day mode asked to open. */
    fun of(link: CalendarLink): Intent = when (link) {
        is CalendarLink.Day -> at(link.millis)
        is CalendarLink.Event -> event(link.event)
    }

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
