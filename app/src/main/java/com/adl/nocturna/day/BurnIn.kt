package com.adl.nocturna.day

import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin

/*
 * Day mode stays on screen for a whole working day, so on OLED screens nothing in it may stay put:
 * the layout drifts (ClockScreen's DriftLayout), swaps sides every hour in landscape, and the
 * background keeps moving slowly. These are the timings, kept here so they can be tested.
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

/** The sun's small circle around its corner, as fractions of its radius, once an hour. */
fun sunWobble(seconds: Float): Pair<Float, Float> {
    val t = 2 * PI * seconds / 3600.0
    return (0.35 * cos(t)).toFloat() to (0.35 * sin(t)).toFloat()
}
