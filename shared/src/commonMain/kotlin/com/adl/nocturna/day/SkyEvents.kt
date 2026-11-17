package com.adl.nocturna.day

import kotlinx.datetime.DateTimeUnit
import kotlinx.datetime.LocalDate
import kotlinx.datetime.LocalTime
import kotlinx.datetime.minus
import kotlinx.datetime.number
import kotlin.math.PI
import kotlin.math.cos
import kotlin.time.Instant

/*
 * What happens in day mode's night sky besides the stars turning: the moon's phase, and shooting stars on
 * the peak nights of the year's main meteor showers.
 */

/**
 * The evenings (month * 100 + day) that start the peak nights of the main meteor showers, which come back
 * on about the same dates every year: the Quadrantids (3 January), Lyrids (22 April), Eta Aquariids
 * (6 May), Perseids (11 to 13 August), Orionids (21 October), Leonids (17 November) and Geminids (13 and
 * 14 December).
 */
private val METEOR_EVENINGS = setOf(103, 422, 506, 811, 812, 813, 1021, 1117, 1213, 1214)

/**
 * Whether shooting stars may cross the sky at [time] on [date]: on a meteor shower's peak night, which
 * starts on the evening of its date and runs until the next morning. Only the dark part of that shows them.
 */
fun meteorNight(date: LocalDate, time: LocalTime): Boolean {
    val evening = if (time.hour < 12) date.minus(1, DateTimeUnit.DAY) else date
    return evening.month.number * 100 + evening.day in METEOR_EVENINGS
}

/** Days from one new moon to the next, on average. */
private const val SYNODIC_MONTH_DAYS = 29.530588853

/** A new moon to count from: 6 January 2000, 18:14 UTC. */
private val NEW_MOON = Instant.parse("2000-01-06T18:14:00Z")

/**
 * How far through its cycle the moon is at [at]: 0 new, 0.25 first quarter, 0.5 full, 0.75 last quarter.
 * From the average month, so it can be off by up to about half a day, which the drawing doesn't show.
 */
fun moonPhase(at: Instant): Float {
    val days = (at - NEW_MOON).inWholeSeconds / 86_400.0
    return (((days / SYNODIC_MONTH_DAYS) % 1.0 + 1.0) % 1.0).toFloat()
}

/** How much of the moon's face is lit at [phase] (see [moonPhase]), 0 at new moon to 1 at full. */
fun moonLit(phase: Float): Float = ((1 - cos(2 * PI * phase)) / 2).toFloat()
