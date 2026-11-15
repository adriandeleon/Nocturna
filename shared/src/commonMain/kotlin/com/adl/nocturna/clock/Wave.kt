package com.adl.nocturna.clock

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberUpdatedState
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.repeatOnLifecycle
import kotlinx.coroutines.flow.Flow
import kotlin.time.Duration
import kotlin.time.Duration.Companion.milliseconds
import kotlin.time.Duration.Companion.seconds

/*
 * Wave to snooze and wave to peek: a hand passed over the top of the phone, where the proximity sensor is,
 * snoozes a ringing alarm or peeks at the dim night clock. The sensor only says "near" or "far"; a wave is
 * near, then far again, quickly. A phone lying face down, in a pocket or under a pillow stays near, and
 * that is not a wave.
 */

/** A wave of the hand over the proximity sensor, as [WaveDetector] tells it. */
data object Wave

/** The longest a hand may stay over the sensor for it to count as a wave. */
val WAVE_MAX_NEAR: Duration = 1500.milliseconds

/** Shorter than this, "near" is the sensor's noise, not a hand. */
val WAVE_MIN_NEAR: Duration = 30.milliseconds

/** Near for longer than this, the phone is covered (face down, in a pocket): nothing counts until it is far. */
val WAVE_COVERED: Duration = 2.seconds

/**
 * After a wave, or a cover lifting, no wave counts until the sensor has been quiet this long: one wave is
 * one action, however much the sensor flaps.
 */
val WAVE_DEBOUNCE: Duration = 1.seconds

/** How close, in cm, counts as near on a sensor that measures distance rather than just near or far. */
private const val NEAR_CM = 5f

/**
 * Whether a proximity reading of [distance] (cm) is near, on a sensor whose far reading is [maxRange]. Most
 * sensors only report 0 and [maxRange]; those that measure count anything under [NEAR_CM] as near.
 */
fun proximityIsNear(distance: Float, maxRange: Float): Boolean {
    val limit = if (maxRange > 0f) minOf(maxRange, NEAR_CM) else NEAR_CM
    return distance < limit
}

/**
 * Tells waves from proximity readings, fed in order with the monotonic time each was taken. A pure state
 * machine: it keeps no clock of its own, so a reading's time is all it knows.
 *
 * - A wave is near, then far within [WAVE_MAX_NEAR] (and not under [WAVE_MIN_NEAR], which is noise).
 * - Near for longer than [WAVE_COVERED] is a covered phone; its lifting is not a wave.
 * - A near before any far doesn't count either: the phone may have been covered when the sensor started.
 * - After a wave or a lifted cover, nothing counts until [WAVE_DEBOUNCE] passes with no near ending, so a
 *   sensor flapping near and far gives one wave, not many.
 */
class WaveDetector {
    private enum class State { UNKNOWN, FAR, NEAR, COVERED }

    private var state = State.UNKNOWN
    private var nearSince = Duration.ZERO
    private var quietUntil: Duration? = null

    /** Feeds one reading: [near] or far at [at]. Returns [Wave] when this reading ends one. */
    fun feed(at: Duration, near: Boolean): Wave? {
        when (state) {
            State.UNKNOWN -> state = if (near) State.COVERED else State.FAR
            State.FAR -> if (near) {
                state = State.NEAR
                nearSince = at
            }
            State.NEAR -> {
                val held = at - nearSince
                if (near) {
                    if (held > WAVE_COVERED) state = State.COVERED
                } else {
                    state = State.FAR
                    val quiet = quietUntil.let { it == null || at >= it }
                    when {
                        held > WAVE_COVERED -> quietUntil = at + WAVE_DEBOUNCE
                        held < WAVE_MIN_NEAR || held > WAVE_MAX_NEAR -> {}
                        // A wave during the quiet time: the sensor still flapping. It keeps it quiet longer.
                        !quiet -> quietUntil = at + WAVE_DEBOUNCE
                        else -> {
                            quietUntil = at + WAVE_DEBOUNCE
                            return Wave
                        }
                    }
                }
            }
            State.COVERED -> if (!near) {
                state = State.FAR
                quietUntil = at + WAVE_DEBOUNCE
            }
        }
        return null
    }
}

/** One proximity reading: near or far, at a monotonic time. */
data class ProximityReading(val at: Duration, val near: Boolean)

/** The phone's proximity sensor, at the top of the screen. */
interface Proximity {
    /**
     * The sensor's readings, the current one first. Collecting the flow turns the sensor on; cancelling the
     * collection turns it off again.
     */
    fun readings(): Flow<ProximityReading>
}

/**
 * Whether a wave peeks at the clock: when the controls don't show, and the clock is dim enough to peek
 * ([canPeek]) or peeking already, which starts the peek again. Unlike a tap, a wave never shows the controls
 * or ends the screen saver.
 */
fun clockWave(controlsVisible: Boolean, peeking: Boolean, canPeek: Boolean): Boolean =
    !controlsVisible && (peeking || canPeek)

/**
 * Calls [onWave] for each wave over [proximity], with the sensor on only while [lifecycle] is at least
 * started. Returns when the caller is cancelled, which turns the sensor off.
 */
suspend fun listenForWaves(lifecycle: Lifecycle, proximity: Proximity, onWave: () -> Unit) {
    lifecycle.repeatOnLifecycle(Lifecycle.State.STARTED) {
        val detector = WaveDetector()
        proximity.readings().collect { if (detector.feed(it.at, it.near) != null) onWave() }
    }
}

/**
 * Listens for waves while [enabled], this is on screen and the screen is started; without a [proximity]
 * sensor it does nothing. Leaving the screen, or turning [enabled] off, turns the sensor off.
 */
@Composable
fun WaveEffect(proximity: Proximity?, enabled: Boolean, onWave: () -> Unit) {
    if (proximity == null || !enabled) return
    val lifecycle = LocalLifecycleOwner.current.lifecycle
    val currentOnWave by rememberUpdatedState(onWave)
    LaunchedEffect(proximity, lifecycle) { listenForWaves(lifecycle, proximity) { currentOnWave() } }
}
