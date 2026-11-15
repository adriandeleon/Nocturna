package com.adl.nocturna.day

import kotlinx.datetime.LocalDate
import kotlinx.datetime.LocalTime
import kotlinx.datetime.TimeZone
import kotlinx.datetime.atTime
import kotlinx.datetime.toInstant
import kotlinx.datetime.toLocalDateTime
import kotlin.time.Instant

/** The hour a day view opens at: the current time for today, the start of the working day otherwise. */
private val WORKDAY_START = LocalTime(9, 0)

/**
 * The moment to open the calendar at for [day]: now if it's today in [zone] (so the view shows what's
 * next), otherwise 09:00 that day in [zone]. In epoch millis, as the calendar app takes it.
 */
fun dayOpenTime(day: LocalDate, now: Instant, zone: TimeZone): Long =
    if (day == now.toLocalDateTime(zone).date) now.toEpochMilliseconds()
    else day.atTime(WORKDAY_START).toInstant(zone).toEpochMilliseconds()
