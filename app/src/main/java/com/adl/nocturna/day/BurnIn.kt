package com.adl.nocturna.day

import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin

/*
 * Day mode stays on screen for a whole working day, so on OLED screens nothing in it may stay put:
 * the layout drifts (ClockScreen's DriftLayout), the clock, the page and the note each drift on their
 * own as well (DayScreen), the two sides swap every hour in landscape, and everything in the
 * background keeps moving: the sun and the moon cross the sky, the clouds float up and down, the
 * hills scroll and rise and fall, and the flowers are replanted every hour. These are the timings,
 * kept here so they can be tested.
 */

/** Whether the clock and the calendar page trade places: every other hour, when burn-in protection is on. */
fun sidesSwapped(hour: Int, burnInProtection: Boolean): Boolean = burnInProtection && hour % 2 == 1

/** Seconds for the near hill to scroll one screen width; farther hills go slower, for depth. */
private const val HILL_PERIOD = 4 * 3600f

/** How far hill [layer] (0 = farthest) has scrolled, in screen widths, [seconds] into the day. */
fun hillShift(seconds: Float, layer: Int): Float = seconds / HILL_PERIOD * (layer + 1) / 3f

/** The background photo is drawn this much larger than the screen, so it can pan without showing an edge. */
const val PHOTO_SCALE = 1.08f

/**
 * Where the background photo has panned to [seconds] into the day, as fractions of the screen
 * (x, y): a slow loop every two hours that never reaches past the extra room [PHOTO_SCALE] leaves.
 */
fun photoPan(seconds: Float): Pair<Float, Float> {
    val reach = (PHOTO_SCALE - 1f) / 2f * 0.85f
    val t = 2 * PI * seconds / 7200.0
    return (reach * sin(t)).toFloat() to (reach * cos(t * 0.5)).toFloat()
}

/** The sun's (and the moon's) small circle around its place on its way, as fractions of its radius, once an hour. */
fun sunWobble(seconds: Float): Pair<Float, Float> {
    val t = 2 * PI * seconds / 3600.0
    return (0.35 * cos(t)).toFloat() to (0.35 * sin(t)).toFloat()
}

private const val DAY = 86_400f

/** The sun is up from dawn (05:30) to dusk (20:00), as the sky's looks have it (see [skyMix]). */
const val SUNRISE = 5.5f * 3600
const val SUNSET = 20f * 3600

/** The moon is up from early dusk (18:30) to after dawn (07:00), so it fades in and out with the night. */
const val MOONRISE = 18.5f * 3600
const val MOONSET = 7f * 3600

/**
 * How far along its trip across the sky a body that rises at [rise] and sets at [set] (seconds of
 * the day, possibly across midnight) is [seconds] into the day: 0 rising, 1 setting. Held at 1
 * while it is down.
 */
fun skyTrip(seconds: Float, rise: Float, set: Float): Float {
    val span = ((set - rise) % DAY + DAY) % DAY
    val since = ((seconds - rise) % DAY + DAY) % DAY
    return (since / span).coerceIn(0f, 1f)
}

/**
 * Where the sun or the moon is [trip] of the way across the sky (see [skyTrip]): x as a fraction of
 * the width, from the left corner to the right one; y as its radii below the top edge, low in the
 * corners and peeking over the top in the middle of its trip.
 */
fun skyArc(trip: Float): Pair<Float, Float> =
    (0.06f + 0.88f * trip) to (0.35f - 0.6f * sin(PI * trip).toFloat())

/** How far cloud [index] has floated up (-) or down (+), as a fraction of the screen height. */
fun cloudBob(seconds: Float, index: Int): Float =
    (0.035 * sin(2 * PI * seconds / (2400.0 + 700 * index) + index * 2.1)).toFloat()

/**
 * How far hill [layer] has risen (-) or sunk (+), as a fraction of the screen height: slow swells,
 * so the hilltops' edges sweep a band of pixels and not a line.
 */
fun hillRise(seconds: Float, layer: Int): Float =
    (0.012 * sin(2 * PI * seconds / (5400.0 + 1800 * layer) + layer)).toFloat()

/** Degrees the starry sky has turned around its pole [seconds] into the day: six an hour. */
fun starTurn(seconds: Float): Float = seconds / 600f

/** The flowers and trees are replanted every hour, fading from one field to the next over five minutes. */
private const val FIELD_PERIOD = 3600f
private const val FIELD_FADE = 300f

/** Which field is planted [seconds] into the day, the one before it, and how far it has grown in (0..1). */
data class Planting(val now: Int, val before: Int, val grown: Float)

fun planting(seconds: Float): Planting {
    val count = (DAY / FIELD_PERIOD).toInt()
    val now = (seconds / FIELD_PERIOD).toInt().mod(count)
    return Planting(now, (now + count - 1) % count, ((seconds % FIELD_PERIOD) / FIELD_FADE).coerceIn(0f, 1f))
}
