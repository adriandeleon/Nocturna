package com.adl.nocturna.clock

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.lerp
import com.adl.nocturna.data.Alarm
import kotlinx.datetime.TimeZone
import kotlin.time.Duration.Companion.minutes
import kotlin.time.Instant

/*
 * Sunrise before alarms: in the minutes before an alarm the clock brightens and warms up, from night red
 * through amber to a warm white, so the room is already light when the alarm rings.
 */

/** Amber, halfway through a sunrise from night red. */
val SunriseAmber = Color(0xFFFFA726)

/** Where a sunrise ends: a warm white, like early sunlight. */
val SunriseWhite = Color(0xFFFFF3E0)

/**
 * When the next of [alarms] rings after [now], or null when none is on. Only the alarms themselves count:
 * a snooze is not a new sunrise, and a skipped occurrence or a one-time alarm that was dismissed (and so
 * turned off) is not ahead any more, nor is one whose date has passed.
 */
fun nextAlarmRing(alarms: List<Alarm>, now: Instant, zone: TimeZone): Instant? =
    alarms.filter { it.enabled }.mapNotNull { it.nextTrigger(now, zone) }.minOrNull()

/**
 * How far into the sunrise [now] is, from 0 when it starts [windowMinutes] before [ring] to 1 when the
 * alarm rings, eased in (slow at first) as the sky brightens at dawn. It is 0 outside that window, with no
 * alarm ahead, or with the sunrise turned off ([windowMinutes] 0).
 */
fun sunriseProgress(now: Instant, ring: Instant?, windowMinutes: Int): Float {
    if (ring == null || windowMinutes <= 0 || now >= ring) return 0f
    val left = ring - now
    val window = windowMinutes.minutes
    if (left >= window) return 0f
    val linear = (1.0 - left / window).toFloat().coerceIn(0f, 1f)
    return linear * linear * linear
}

/**
 * The brightness during a sunrise at [progress]: from [base], what the room light would give, up to
 * [max], the brightest the user allows, never darker than [base]. When the clock leaves the brightness to
 * the phone ([SYSTEM_BRIGHTNESS]) it stays so: only the colour changes.
 */
fun sunriseBrightness(base: Float, max: Float, progress: Float): Float {
    if (base == SYSTEM_BRIGHTNESS || progress <= 0f) return base
    val p = progress.coerceAtMost(1f)
    return (base + (max - base) * p).coerceAtLeast(base)
}

/**
 * The clock's colour during a sunrise at [progress]: from [base], through amber when it starts from night
 * red ([night]), to a warm white.
 */
fun sunriseColor(base: Color, night: Boolean, progress: Float): Color {
    val p = progress.coerceIn(0f, 1f)
    if (p == 0f) return base
    return when {
        !night -> lerp(base, SunriseWhite, p)
        p < 0.5f -> lerp(base, SunriseAmber, p * 2f)
        else -> lerp(SunriseAmber, SunriseWhite, (p - 0.5f) * 2f)
    }
}
