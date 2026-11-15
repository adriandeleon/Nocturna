package com.adl.nocturna.clock

import kotlin.time.Duration
import kotlin.time.Duration.Companion.seconds

/*
 * Tap to peek: a tap on the dim night clock brightens it for a few seconds, to a comfortable level and a soft
 * warm colour, then lets it fade back, so you can read the time in the dark without getting blinded. A second
 * tap during the peek shows the controls as usual.
 */

/** How long a peek stays up at full before it fades. */
val PEEK_HOLD: Duration = 6.seconds

/** How long a peek takes to fade back to the dim clock. */
val PEEK_FADE: Duration = 2.seconds

/** A peek's brightness, as a share of the brightest the user allows: comfortable, never blinding. */
const val PEEK_SHARE = 0.4f

/**
 * How far along a sunrise the peek's colour is: from night red past amber to a soft warm colour, not yet the
 * sunrise's warm white.
 */
const val PEEK_SUNRISE_PROGRESS = 0.6f

/** Where the room-light brightness counts as "at its minimum": within this share of the range above it. */
private const val NEAR_MIN_SHARE = 0.05f

/**
 * How much a peek is showing at [sinceTap] after the tap, from 1 (at full) to 0: held at 1 for [PEEK_HOLD],
 * then easing down over [PEEK_FADE]. It is 0 with no peek (null), before the tap or after the fade.
 */
fun peekLevel(sinceTap: Duration?): Float {
    if (sinceTap == null || sinceTap.isNegative()) return 0f
    if (sinceTap <= PEEK_HOLD) return 1f
    val t = ((sinceTap - PEEK_HOLD) / PEEK_FADE).toFloat()
    if (t >= 1f) return 0f
    // Smoothstep down: no jump as the fade starts or ends.
    return 1f - t * t * (3f - 2f * t)
}

/** The brightness a peek brings the screen up to: [PEEK_SHARE] of [max]. */
fun peekTarget(max: Float): Float = PEEK_SHARE * max

/**
 * Whether a tap on the clock peeks rather than showing the controls: [enabled] (the setting), not in Day mode
 * ([day]), and the clock is dim, its room-light level [room] at (or within a hair of) [min], as it always is in
 * night mode. A peek also has to make the screen brighter than it is ([current], the room level with any
 * sunrise): late in a sunrise, the tap just shows the controls. When the phone sets the brightness
 * ([SYSTEM_BRIGHTNESS]) there is nothing to peek from.
 */
fun canPeek(enabled: Boolean, day: Boolean, room: Float, current: Float, min: Float, max: Float): Boolean {
    if (!enabled || day || room == SYSTEM_BRIGHTNESS || current == SYSTEM_BRIGHTNESS) return false
    val nearMin = min + (max - min).coerceAtLeast(0f) * NEAR_MIN_SHARE
    return room <= nearMin && current < peekTarget(max)
}

/**
 * The window brightness with a peek at [level] on top: from [room], the room-light level, towards
 * [peekTarget] of [max], and never below [sunrise], what a sunrise gives (or [room] without one), so
 * whichever is brighter wins. [SYSTEM_BRIGHTNESS] stays so.
 */
fun peekBrightness(room: Float, sunrise: Float, max: Float, level: Float): Float {
    if (room == SYSTEM_BRIGHTNESS || sunrise == SYSTEM_BRIGHTNESS || level <= 0f) return sunrise
    val l = level.coerceAtMost(1f)
    val peek = room + (peekTarget(max) - room).coerceAtLeast(0f) * l
    return maxOf(sunrise, peek)
}

/**
 * How far along the sunrise the clock's colour (and night-mode opacity) is, with a peek at [level]: the
 * peek counts as [PEEK_SUNRISE_PROGRESS] of the way, and whichever of it and [sunrise] is further along wins.
 * Feed the result to [sunriseColor].
 */
fun peekProgress(sunrise: Float, level: Float): Float =
    maxOf(sunrise, PEEK_SUNRISE_PROGRESS * level.coerceIn(0f, 1f))

/** What a tap on the clock does. */
enum class ClockTap { PEEK, SHOW_CONTROLS, HIDE_CONTROLS }

/**
 * What a tap does: hides the controls when they show; during a peek ([peeking]) shows them; otherwise peeks
 * when [canPeek] allows it, and shows the controls when it doesn't, as before.
 */
fun clockTap(controlsVisible: Boolean, peeking: Boolean, canPeek: Boolean): ClockTap = when {
    controlsVisible -> ClockTap.HIDE_CONTROLS
    peeking -> ClockTap.SHOW_CONTROLS
    canPeek -> ClockTap.PEEK
    else -> ClockTap.SHOW_CONTROLS
}
