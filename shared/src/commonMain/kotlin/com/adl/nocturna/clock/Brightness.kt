package com.adl.nocturna.clock

import kotlin.math.log10

/** Means "use the phone's own brightness" (see ClockActivity's setBrightness). */
const val SYSTEM_BRIGHTNESS = -1f

/**
 * The window brightness (0..1) for the clock, or [SYSTEM_BRIGHTNESS].
 *
 * Night mode is always at [min]. Following the room ([dimEnabled] and [autoBrightness], with a light
 * sensor reading [lux]), it goes from [min] in the dark to [max] in a bright room, on a log scale as
 * eyes see light. Day mode stays up all day, so on an OLED screen its top is [dayMax] when that is
 * lower than [max]. Otherwise the phone's own brightness applies.
 */
fun screenBrightness(
    lux: Float?,
    night: Boolean,
    day: Boolean,
    dimEnabled: Boolean,
    autoBrightness: Boolean,
    min: Float,
    max: Float,
    dayMax: Float,
): Float = when {
    night -> min
    !dimEnabled || !autoBrightness || lux == null -> SYSTEM_BRIGHTNESS
    else -> {
        val top = if (day) minOf(dayMax, max).coerceAtLeast(min) else max
        val f = (log10(1f + lux) / log10(1f + 1000f)).coerceIn(0f, 1f)
        min + (top - min) * f
    }
}
