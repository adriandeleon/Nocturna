package com.adl.nocturna.day

import com.adl.nocturna.weather.DayForecast
import kotlinx.datetime.LocalDate
import kotlinx.datetime.LocalTime
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime

/** The four looks of day mode's illustrated sky. */
enum class TimeOfDay { NIGHT, MORNING, MIDDAY, AFTERNOON }

/**
 * When the sun rises and sets, as minutes of the day on the clock. Dawn runs from [TWILIGHT] minutes
 * before [rise] to as many after it, and dusk likewise around [set]; the sun is drawn in the sky from the
 * start of dawn to the end of dusk, and the moon from the start of dusk to the end of dawn.
 */
data class SunTimes(val rise: Float, val set: Float) {
    val dawnStart: Float get() = rise - TWILIGHT
    val dawnEnd: Float get() = rise + TWILIGHT
    val duskStart: Float get() = set - TWILIGHT
    val duskEnd: Float get() = set + TWILIGHT

    companion object {
        /** Half of dawn's (and dusk's) blend, in minutes. */
        const val TWILIGHT = 45f

        /** For when the forecast doesn't say: dawn 05:30–07:00 and dusk 18:30–20:00. */
        val DEFAULT = SunTimes(6 * 60 + 15f, 19 * 60 + 15f)
    }
}

/**
 * Today's sunrise and sunset from the forecast [days], read in [zone], or [SunTimes.DEFAULT] when the
 * forecast doesn't have them. A day whose dawn would start before midnight, whose dusk would end after
 * it, or whose daylight is too short to hold the sky's looks (near the poles) gets the default too.
 */
fun sunTimes(days: List<DayForecast>, today: LocalDate, zone: TimeZone): SunTimes {
    val sun = days.firstNotNullOfOrNull { d ->
        val rise = d.sunrise?.toLocalDateTime(zone)
        val set = d.sunset?.toLocalDateTime(zone)
        if (rise == null || set == null || rise.date != today || set.date != today) null
        else SunTimes(rise.time.toSecondOfDay() / 60f, set.time.toSecondOfDay() / 60f)
    } ?: return SunTimes.DEFAULT
    val fits = sun.dawnStart >= 0f && sun.duskEnd <= 24 * 60f && sun.duskStart - sun.dawnEnd >= MIN_DAYLIGHT
    return if (fits) sun else SunTimes.DEFAULT
}

/** The least time between dawn and dusk, in minutes, that leaves room for morning, midday and afternoon. */
private const val MIN_DAYLIGHT = 4 * 60f

/*
 * Between the end of dawn and the start of dusk the morning blends into midday, and midday into the
 * afternoon, at these fractions of the way: with the default sun, 10:00–11:30 and 15:00–16:30.
 */
private const val MIDDAY_FROM = 180f / 690
private const val MIDDAY_AT = 270f / 690
private const val AFTERNOON_FROM = 480f / 690
private const val AFTERNOON_AT = 570f / 690

/** When each look is fully on, as minutes of the day. Between two keyframes the sky blends from one look to the next. */
private fun keyframes(sun: SunTimes): List<Pair<Float, TimeOfDay>> {
    val day = sun.duskStart - sun.dawnEnd
    return listOf(
        0f to TimeOfDay.NIGHT,
        sun.dawnStart to TimeOfDay.NIGHT,
        sun.dawnEnd to TimeOfDay.MORNING,
        sun.dawnEnd + day * MIDDAY_FROM to TimeOfDay.MORNING,
        sun.dawnEnd + day * MIDDAY_AT to TimeOfDay.MIDDAY,
        sun.dawnEnd + day * AFTERNOON_FROM to TimeOfDay.MIDDAY,
        sun.dawnEnd + day * AFTERNOON_AT to TimeOfDay.AFTERNOON,
        sun.duskStart to TimeOfDay.AFTERNOON,
        sun.duskEnd to TimeOfDay.NIGHT,
        24 * 60f to TimeOfDay.NIGHT,
    )
}

/** The sky at a moment: on its way [from] one look [to] the next, [t] of the way there (0..1). */
data class SkyMix(val from: TimeOfDay, val to: TimeOfDay, val t: Float) {
    /** How much of [look] is in the sky, 0..1; the weights of all four add up to 1. */
    fun weight(look: TimeOfDay): Float =
        (if (from == look) 1f - t else 0f) + (if (to == look) t else 0f)
}

/**
 * The sky's look at [time]: night until dawn, a peachy morning, the month's own sky at midday, a golden
 * afternoon and night again after dusk, with dawn and dusk around [sun]'s rise and set. It never jumps.
 */
fun skyMix(time: LocalTime, sun: SunTimes = SunTimes.DEFAULT): SkyMix {
    val keyframes = keyframes(sun)
    val minute = time.toSecondOfDay() / 60f
    val i = keyframes.indexOfLast { it.first <= minute }.coerceAtMost(keyframes.size - 2)
    val (startMinute, from) = keyframes[i]
    val (endMinute, to) = keyframes[i + 1]
    if (from == to || endMinute <= startMinute) return SkyMix(to, to, 0f)
    return SkyMix(from, to, ((minute - startMinute) / (endMinute - startMinute)).coerceIn(0f, 1f))
}
