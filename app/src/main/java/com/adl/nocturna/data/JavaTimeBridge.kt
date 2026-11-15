package com.adl.nocturna.data

import com.adl.nocturna.time.instant
import com.adl.nocturna.time.timeZone
import com.adl.nocturna.time.withMoment
import kotlinx.datetime.toJavaLocalDate
import java.time.LocalDate
import java.time.ZonedDateTime

// java.time overloads of the shared alarm logic, for the app's UI and services (see time/JavaTime.kt).

fun Alarm.nextTrigger(now: ZonedDateTime): ZonedDateTime = now.withMoment(nextTrigger(now.instant, now.timeZone))

fun Alarm.skipping(now: ZonedDateTime): LocalDate? = skipping(now.instant, now.timeZone)?.toJavaLocalDate()

fun Alarm.toggleSkip(now: ZonedDateTime): Alarm = toggleSkip(now.instant, now.timeZone)
