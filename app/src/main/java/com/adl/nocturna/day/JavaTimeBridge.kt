package com.adl.nocturna.day

import com.adl.nocturna.data.Settings
import com.adl.nocturna.time.instant
import com.adl.nocturna.time.timeZone
import com.adl.nocturna.time.withMoment
import kotlinx.datetime.toJavaLocalDate
import kotlinx.datetime.toKotlinDayOfWeek
import kotlinx.datetime.toKotlinLocalDate
import kotlinx.datetime.toKotlinLocalTime
import kotlinx.datetime.toKotlinTimeZone
import kotlinx.datetime.toKotlinYearMonth
import java.time.DayOfWeek
import java.time.Instant
import java.time.LocalDate
import java.time.LocalTime
import java.time.YearMonth
import java.time.ZoneId
import java.time.ZonedDateTime
import kotlin.time.toKotlinInstant

// java.time overloads of the shared day mode logic, for the app's UI (see time/JavaTime.kt).

fun monthGrid(month: YearMonth, firstDayOfWeek: DayOfWeek): List<List<LocalDate>> =
    monthGrid(month.toKotlinYearMonth(), firstDayOfWeek.toKotlinDayOfWeek()).map { week -> week.map { it.toJavaLocalDate() } }

fun weekOf(day: LocalDate, firstDayOfWeek: DayOfWeek): List<LocalDate> =
    weekOf(day.toKotlinLocalDate(), firstDayOfWeek.toKotlinDayOfWeek()).map { it.toJavaLocalDate() }

fun eventsForDay(events: List<CalendarEvent>, day: LocalDate, zone: ZoneId, now: Instant): List<CalendarEvent> =
    eventsForDay(events, day.toKotlinLocalDate(), zone.toKotlinTimeZone(), now.toKotlinInstant())

fun eventsOnDay(events: List<CalendarEvent>, day: LocalDate, zone: ZoneId): List<CalendarEvent> =
    eventsOnDay(events, day.toKotlinLocalDate(), zone.toKotlinTimeZone())

fun daysWithEvents(events: List<CalendarEvent>, days: List<LocalDate>, zone: ZoneId): Set<LocalDate> =
    daysWithEvents(events, days.map { it.toKotlinLocalDate() }, zone.toKotlinTimeZone()).mapTo(mutableSetOf()) { it.toJavaLocalDate() }

fun CalendarEvent.covers(day: LocalDate, zone: ZoneId): Boolean = covers(day.toKotlinLocalDate(), zone.toKotlinTimeZone())

fun CalendarEvent.isOngoing(now: Instant): Boolean = isOngoing(now.toKotlinInstant())

fun isDayModeActive(settings: Settings, now: ZonedDateTime): Boolean = isDayModeActive(settings, now.instant, now.timeZone)

fun inDaySchedule(settings: Settings, now: ZonedDateTime): Boolean = inDaySchedule(settings, now.instant, now.timeZone)

fun nextDayScheduleEdge(settings: Settings, now: ZonedDateTime): ZonedDateTime? =
    nextDayScheduleEdge(settings, now.instant, now.timeZone)?.let { now.withMoment(it) }

fun Settings.withDayModeToggled(now: ZonedDateTime): Settings = withDayModeToggled(now.instant, now.timeZone)

fun skyMix(time: LocalTime): SkyMix = skyMix(time.toKotlinLocalTime())

fun dayOpenTime(day: LocalDate, now: ZonedDateTime): Long = dayOpenTime(day.toKotlinLocalDate(), now.instant, now.timeZone)
