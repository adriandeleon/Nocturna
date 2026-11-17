package com.adl.nocturna.day

import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin

/*
 * Day mode stays on screen for a whole working day, so on OLED screens nothing in it may stay put:
 * the layout drifts (ClockScreen's DriftLayout), the clock, the page and the note each drift on their
 * own as well (DayScreen), the clock and the page trade places every other hour (sides in
 * landscape, top and bottom in portrait), the page's bright paper slowly dims the longer it stays up,
 * and everything in the background keeps moving: the sun and the moon cross the sky, the clouds float up and down, the
 * hills scroll, rise and fall and change shape, and the flowers and trees sway, wander, grow and shrink,
 * and are replanted every hour. These are the timings,
 * kept here so they can be tested.
 */

/**
 * Whether the clock and the calendar page trade places: every other hour, when burn-in protection is
 * on. In landscape they swap sides; in portrait the clock moves from the top to the bottom.
 */
fun sidesSwapped(hour: Int, burnInProtection: Boolean): Boolean = burnInProtection && hour % 2 == 1

/** Seconds for the near hill to scroll one screen width; farther hills go slower, for depth. */
private const val HILL_PERIOD = 2 * 3600f

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

/** Without the real sunrise and sunset, the sun is up from dawn (05:30) to dusk (20:00) (see [FIXED_SKY_DAY]). */
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
    (HILL_SWELL * sin(2 * PI * seconds / (2700.0 + 900 * layer) + layer)).toFloat()

/** How far, as a fraction of the screen height, a hill rises or sinks at most. */
const val HILL_SWELL = 0.025f

/**
 * How far the smaller bumps on hill [layer] have rolled along it, in radians: once around every one
 * to two hours, so the hills' outline changes shape and not just place.
 */
fun hillMorph(seconds: Float, layer: Int): Float =
    (2 * PI * seconds / (3600.0 + 1800 * layer)).toFloat() * if (layer % 2 == 0) 1f else -1f

/** How far, in degrees, plant [index] leans in the breeze at most. */
const val SWAY_DEGREES = 8f

/** How far plant [index] leans in the breeze [seconds] into the day, in degrees: each sways at its own pace. */
fun plantSway(seconds: Float, index: Int): Float =
    (SWAY_DEGREES * sin(2 * PI * seconds / (24.0 + 7 * (index % 5)) + index * 1.7)).toFloat()

/** How far, as a fraction of the screen width, a plant wanders from its spot at most. */
const val WANDER = 0.03f

/**
 * Where plant [index] has wandered from its spot [seconds] into the day, as fractions of the screen's
 * width (across) and of the field's depth (down), each within [WANDER]: a slow loop of its own.
 */
fun plantWander(seconds: Float, index: Int): Pair<Float, Float> {
    val t = 2 * PI * seconds / (900.0 + 120 * (index % 7))
    return (WANDER * sin(t + index)).toFloat() to (WANDER * 1.5 * sin(1.3 * t + index * 2.3)).toFloat()
}

/** How big plant [index] has grown [seconds] into the day, compared with its usual size: 0.85 to 1.15. */
fun plantGrowth(seconds: Float, index: Int): Float =
    (1.0 + 0.15 * sin(2 * PI * seconds / (1500.0 + 200 * (index % 4)) + index * 0.9)).toFloat()

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

/** The page's paper starts dimming after half an hour on screen and is as dim as it goes after four hours. */
private const val DIM_FROM_MINUTES = 30f
private const val DIM_FULL_MINUTES = 240f

/**
 * How far the calendar page's paper has dimmed (0 = fresh, 1 = as dim as it goes) after [minutesShown]
 * minutes on screen: so slowly that nobody sees it happen, since the bright page is what wears an
 * OLED screen most.
 */
fun paperDim(minutesShown: Float): Float =
    ((minutesShown - DIM_FROM_MINUTES) / (DIM_FULL_MINUTES - DIM_FROM_MINUTES)).coerceIn(0f, 1f)
