package com.adl.nocturna.day

/** Something on day mode's page or note to open in the phone's calendar app. */
sealed interface CalendarLink {
    /** The calendar's day view at [millis] (see [dayOpenTime]). */
    data class Day(val millis: Long) : CalendarLink

    /** The event itself, at this occurrence. */
    data class Event(val event: CalendarEvent) : CalendarLink
}
