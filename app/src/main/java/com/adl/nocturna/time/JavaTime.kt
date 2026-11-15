package com.adl.nocturna.time

import kotlinx.datetime.TimeZone
import kotlinx.datetime.toKotlinTimeZone
import java.time.ZonedDateTime
import kotlin.time.Instant
import kotlin.time.toJavaInstant
import kotlin.time.toKotlinInstant

/*
 * The shared module works in kotlinx-datetime; the app's UI still uses java.time. The JavaTimeBridge
 * files in each package give the shared functions java.time overloads, and go away as the UI moves to
 * the shared module.
 */

/** The moment, as the shared module takes it. */
val ZonedDateTime.instant: Instant get() = toInstant().toKotlinInstant()

/** The zone, as the shared module takes it. */
val ZonedDateTime.timeZone: TimeZone get() = zone.toKotlinTimeZone()

/** [instant] in this zone, as the app takes it. */
fun ZonedDateTime.withMoment(instant: Instant): ZonedDateTime = instant.toJavaInstant().atZone(zone)
