package com.adl.nocturna.clock

import java.util.concurrent.atomic.AtomicInteger

/**
 * Whether the clock is on screen, as the app or as the screen saver: the user is in bed, so the bedtime
 * reminder stays quiet. Kept in memory only; when the process is gone, the clock isn't showing.
 */
object ClockVisibility {
    private val visible = AtomicInteger(0)

    val showing: Boolean get() = visible.get() > 0

    fun shown() {
        visible.incrementAndGet()
    }

    fun hidden() {
        visible.updateAndGet { (it - 1).coerceAtLeast(0) }
    }
}
