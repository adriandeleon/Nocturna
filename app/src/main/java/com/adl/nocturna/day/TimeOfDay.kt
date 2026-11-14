package com.adl.nocturna.day

import java.time.LocalTime

/** The four looks of day mode's illustrated sky. */
enum class TimeOfDay { NIGHT, MORNING, MIDDAY, AFTERNOON }

/**
 * When each look is fully on, as minutes of the day. Between two keyframes the sky blends from one
 * look to the next (dawn 05:30–07:00, late morning 10:00–11:30, 15:00–16:30, dusk 18:30–20:00), so
 * it never jumps.
 */
private val KEYFRAMES = listOf(
    0 to TimeOfDay.NIGHT,
    5 * 60 + 30 to TimeOfDay.NIGHT,
    7 * 60 to TimeOfDay.MORNING,
    10 * 60 to TimeOfDay.MORNING,
    11 * 60 + 30 to TimeOfDay.MIDDAY,
    15 * 60 to TimeOfDay.MIDDAY,
    16 * 60 + 30 to TimeOfDay.AFTERNOON,
    18 * 60 + 30 to TimeOfDay.AFTERNOON,
    20 * 60 to TimeOfDay.NIGHT,
    24 * 60 to TimeOfDay.NIGHT,
)

/** The sky at a moment: on its way [from] one look [to] the next, [t] of the way there (0..1). */
data class SkyMix(val from: TimeOfDay, val to: TimeOfDay, val t: Float) {
    /** How much of [look] is in the sky, 0..1; the weights of all four add up to 1. */
    fun weight(look: TimeOfDay): Float =
        (if (from == look) 1f - t else 0f) + (if (to == look) t else 0f)
}

fun skyMix(time: LocalTime): SkyMix {
    val minute = time.toSecondOfDay() / 60f
    val i = KEYFRAMES.indexOfLast { it.first <= minute }.coerceAtMost(KEYFRAMES.size - 2)
    val (startMinute, from) = KEYFRAMES[i]
    val (endMinute, to) = KEYFRAMES[i + 1]
    if (from == to) return SkyMix(from, to, 0f)
    return SkyMix(from, to, ((minute - startMinute) / (endMinute - startMinute)).coerceIn(0f, 1f))
}
