package com.adl.nocturna.clock

import com.adl.nocturna.text.PlatformLocale
import com.adl.nocturna.text.capitalizedFirst
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.floor
import kotlin.time.Instant

/** The mean time from one new moon to the next, in days. */
const val SYNODIC_MONTH_DAYS = 29.530588853

/** A new moon to count from: 6 January 2000, 18:14 UTC. */
private val REFERENCE_NEW_MOON = Instant.parse("2000-01-06T18:14:00Z")

private const val SECONDS_PER_DAY = 86_400.0

/**
 * How far through its cycle the moon is at [at], from 0 up to (not including) 1: 0 is new, 0.25 first
 * quarter, 0.5 full and 0.75 last quarter. It counts mean synodic months from a known new moon, so it's
 * within about a day of the real phase, which is plenty for a clock.
 */
fun moonCycle(at: Instant): Double {
    val days = (at - REFERENCE_NEW_MOON).inWholeSeconds / SECONDS_PER_DAY
    val cycles = days / SYNODIC_MONTH_DAYS
    return cycles - floor(cycles)
}

/** The share of the moon's disc that's lit at [cycle] (see [moonCycle]): 0 at new moon, 1 at full. */
fun moonIllumination(cycle: Double): Double = (1 - cos(2 * PI * cycle)) / 2

/** The eight named phases, each an eighth of the cycle centred on its own point. */
enum class MoonPhase(private val words: String) {
    NEW("new moon"),
    WAXING_CRESCENT("waxing crescent"),
    FIRST_QUARTER("first quarter"),
    WAXING_GIBBOUS("waxing gibbous"),
    FULL("full moon"),
    WANING_GIBBOUS("waning gibbous"),
    LAST_QUARTER("last quarter"),
    WANING_CRESCENT("waning crescent"),
    ;

    /** The info line's name for it: "Waxing gibbous". */
    fun label(locale: PlatformLocale): String = words.capitalizedFirst(locale)

    companion object {
        /** The phase at [cycle] (see [moonCycle]): new moon from 15/16 to 1/16, and so on round. */
        fun of(cycle: Double): MoonPhase = entries[floor(cycle * 8 + 0.5).toInt().mod(8)]

        fun at(at: Instant): MoonPhase = of(moonCycle(at))
    }
}

/**
 * How to draw the moon at [cycle] as seen from the northern hemisphere: the lit half of the disc is on the
 * right while it waxes and on the left while it wanes, and the terminator is a half-ellipse whose width is
 * [bulge] times the radius: below 0 it eats into the lit half (a crescent), above 0 it adds to it (gibbous).
 * -1 is a new moon, 0 a quarter and 1 full.
 */
data class MoonShape(val litOnRight: Boolean, val bulge: Float)

fun moonShape(cycle: Double): MoonShape = MoonShape(litOnRight = cycle < 0.5, bulge = -cos(2 * PI * cycle).toFloat())
