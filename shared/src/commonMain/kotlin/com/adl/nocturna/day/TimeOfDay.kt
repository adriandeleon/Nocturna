package com.adl.nocturna.day

import com.adl.nocturna.weather.DayForecast
import kotlinx.datetime.LocalDate
import kotlinx.datetime.LocalTime
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime

/** The four looks of day mode's illustrated sky. */
enum class TimeOfDay { NIGHT, MORNING, MIDDAY, AFTERNOON }

/**
 * One day of the sky: when each look is fully on (minutes of the day, in order, from 0 to 1440), and when
 * the sun and the moon are up (seconds of the day). Between two keyframes the sky blends from one look to
 * the next, so it never jumps. Made by [skyDay].
 */
class SkyDay internal constructor(
    internal val keyframes: List<Pair<Float, TimeOfDay>>,
    /** The sun rises as dawn starts and sets as the night is fully on. */
    val sunUp: Float,
    val sunDown: Float,
    /** The moon rises as dusk starts and sets as the morning is fully on. */
    val moonUp: Float,
    val moonDown: Float,
)

/** The day the sky keeps without the real sunrise and sunset: dawn 05:30–07:00, dusk 18:30–20:00. */
private const val FIXED_SUNRISE = 6 * 60 + 15
private const val FIXED_SUNSET = 19 * 60 + 15

/** Minutes either side of sunrise that dawn takes, and of sunset that dusk takes. */
private const val TWILIGHT = 45

/** From the end of dawn: morning holds 3 hours, then 90 minutes into midday. */
private const val MORNING_HOLD = 180
private const val TO_MIDDAY = 90

/** Before dusk: 90 minutes from midday into the afternoon, which holds 2 hours. */
private const val TO_AFTERNOON = 90
private const val AFTERNOON_HOLD = 120

/** Days shorter than this (minutes of daylight) keep the fixed day: the looks would have no room. */
private const val SHORTEST_DAY = 4 * 60

/** The day the sky keeps when the real sunrise and sunset aren't known. */
val FIXED_SKY_DAY: SkyDay = build(FIXED_SUNRISE, FIXED_SUNSET)

/**
 * The sky's day for a [sunrise] and [sunset]: night until 45 minutes before sunrise, dawn until 45
 * minutes after, three hours of morning, midday, two hours of golden afternoon and dusk from 45 minutes
 * before sunset to 45 after. Short days squeeze the morning and afternoon, keeping a quarter for midday.
 * Without both times, or with a day too short or too close to midnight to fit, it is [FIXED_SKY_DAY].
 */
fun skyDay(sunrise: LocalTime?, sunset: LocalTime?): SkyDay {
    if (sunrise == null || sunset == null) return FIXED_SKY_DAY
    val rise = sunrise.toSecondOfDay() / 60
    val set = sunset.toSecondOfDay() / 60
    if (set - rise < SHORTEST_DAY || rise - TWILIGHT < 0 || set + TWILIGHT > 24 * 60) return FIXED_SKY_DAY
    return build(rise, set)
}

/** The sky's day from today's sunrise and sunset in the forecast [days], read in [zone], or the fixed day. */
fun skyDay(days: List<DayForecast>, date: LocalDate, zone: TimeZone): SkyDay {
    val today = days.firstOrNull { it.date == date } ?: return FIXED_SKY_DAY
    return skyDay(today.sunrise?.toLocalDateTime(zone)?.time, today.sunset?.toLocalDateTime(zone)?.time)
}

private fun build(rise: Int, set: Int): SkyDay {
    val dawnEnd = (rise + TWILIGHT).toFloat()
    val duskStart = (set - TWILIGHT).toFloat()
    // What the morning, the afternoon and the blends between them need, shrunk on a short day so that
    // midday keeps at least a quarter of the time between dawn and dusk.
    val k = minOf(1f, (duskStart - dawnEnd) * 0.75f / (MORNING_HOLD + TO_MIDDAY + TO_AFTERNOON + AFTERNOON_HOLD))
    val keyframes = listOf(
        0f to TimeOfDay.NIGHT,
        (rise - TWILIGHT).toFloat() to TimeOfDay.NIGHT,
        dawnEnd to TimeOfDay.MORNING,
        dawnEnd + MORNING_HOLD * k to TimeOfDay.MORNING,
        dawnEnd + (MORNING_HOLD + TO_MIDDAY) * k to TimeOfDay.MIDDAY,
        duskStart - (AFTERNOON_HOLD + TO_AFTERNOON) * k to TimeOfDay.MIDDAY,
        duskStart - AFTERNOON_HOLD * k to TimeOfDay.AFTERNOON,
        duskStart to TimeOfDay.AFTERNOON,
        (set + TWILIGHT).toFloat() to TimeOfDay.NIGHT,
        24f * 60 to TimeOfDay.NIGHT,
    )
    return SkyDay(
        keyframes,
        sunUp = (rise - TWILIGHT) * 60f, sunDown = (set + TWILIGHT) * 60f,
        moonUp = (set - TWILIGHT) * 60f, moonDown = (rise + TWILIGHT) * 60f,
    )
}

/** The sky at a moment: on its way [from] one look [to] the next, [t] of the way there (0..1). */
data class SkyMix(val from: TimeOfDay, val to: TimeOfDay, val t: Float) {
    /** How much of [look] is in the sky, 0..1; the weights of all four add up to 1. */
    fun weight(look: TimeOfDay): Float =
        (if (from == look) 1f - t else 0f) + (if (to == look) t else 0f)
}

/** The sky at [time] on [day]. */
fun skyMix(time: LocalTime, day: SkyDay = FIXED_SKY_DAY): SkyMix {
    val keyframes = day.keyframes
    val minute = time.toSecondOfDay() / 60f
    val i = keyframes.indexOfLast { it.first <= minute }.coerceIn(0, keyframes.size - 2)
    val (startMinute, from) = keyframes[i]
    val (endMinute, to) = keyframes[i + 1]
    if (from == to || endMinute <= startMinute) return SkyMix(to, to, 0f)
    return SkyMix(from, to, ((minute - startMinute) / (endMinute - startMinute)).coerceIn(0f, 1f))
}
