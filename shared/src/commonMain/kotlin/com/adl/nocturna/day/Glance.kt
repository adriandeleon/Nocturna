package com.adl.nocturna.day

import com.adl.nocturna.alarm.DaysOff
import com.adl.nocturna.data.Countdown
import com.adl.nocturna.data.Settings
import com.adl.nocturna.text.DateTimePattern
import com.adl.nocturna.text.PlatformLocale
import com.adl.nocturna.text.strings
import com.adl.nocturna.weather.AirQuality
import com.adl.nocturna.weather.DayForecast
import com.adl.nocturna.weather.HourForecast
import com.adl.nocturna.weather.Weather
import com.adl.nocturna.weather.airQualityText
import com.adl.nocturna.weather.uvText
import kotlinx.datetime.LocalDate
import kotlinx.datetime.TimeZone
import kotlinx.datetime.daysUntil
import kotlinx.datetime.toLocalDateTime
import kotlin.time.Instant

/*
 * The glance line under day mode's desk clock: rain on the way, sunset, the UV index, the air quality and
 * the days counted down to. Each is worked out here from data the clock already has.
 */

/** How far ahead the rain alert looks. */
const val RAIN_LOOK_AHEAD_HOURS = 6

/** The chance of rain (%) from which an hour counts as wet, whatever its weather code says. */
const val RAIN_LIKELY_PERCENT = 50

/** Rain or snow on the way or ending: what the rain alert says. */
sealed interface RainAlert {
    /** When the rain starts or stops. */
    val at: Instant

    /** Rain (or snow, when [snow]) is expected from [at], within the next hours. */
    data class Starts(override val at: Instant, val snow: Boolean) : RainAlert

    /** It's raining (or snowing) now and should stop at [at]. */
    data class Stops(override val at: Instant, val snow: Boolean) : RainAlert
}

/** Whether WMO weather [code] is rain, drizzle or a storm. */
fun isRainCode(code: Int): Boolean = code in 51..67 || code in 80..82 || code in 95..99

/** Whether WMO weather [code] is snow. */
fun isSnowCode(code: Int): Boolean = code in 71..77 || code == 85 || code == 86

private fun HourForecast.wet(): Boolean =
    isRainCode(code) || isSnowCode(code) || (precipitationPercent ?: 0) >= RAIN_LIKELY_PERCENT

/**
 * What the rain alert says at [now], given the current weather [currentCode] and the [hours] forecast:
 * when it's dry, the first wet hour in the next [RAIN_LOOK_AHEAD_HOURS]; when it's raining or snowing, the
 * first dry hour in them. Null when nothing changes in that time, since the weather glyph already shows it.
 */
fun rainAlert(currentCode: Int, hours: List<HourForecast>, now: Instant): RainAlert? {
    val ahead = hours.filter { it.time > now && it.time.toEpochMilliseconds() - now.toEpochMilliseconds() <= RAIN_LOOK_AHEAD_HOURS * 3_600_000L }
        .sortedBy { it.time }
    val wetNow = isRainCode(currentCode) || isSnowCode(currentCode)
    return if (wetNow) {
        ahead.firstOrNull { !it.wet() }?.let { RainAlert.Stops(it.time, isSnowCode(currentCode)) }
    } else {
        ahead.firstOrNull { it.wet() }?.let { RainAlert.Starts(it.time, isSnowCode(it.code)) }
    }
}

/** "Rain from 15:00", "Snow until 17:00": [time] is the alert's hour as the clock shows it. */
fun rainAlertText(alert: RainAlert, time: String, locale: PlatformLocale): String {
    val t = strings(locale)
    return when (alert) {
        is RainAlert.Starts -> if (alert.snow) t.snowFrom(time) else t.rainFrom(time)
        is RainAlert.Stops -> if (alert.snow) t.snowUntil(time) else t.rainUntil(time)
    }
}

/** The sun's next rise or set: what the sun line shows. */
sealed interface SunEvent {
    val at: Instant

    /** The sun sets at [at], [daylightMillis] from now. */
    data class Sunset(override val at: Instant, val daylightMillis: Long) : SunEvent

    data class Sunrise(override val at: Instant) : SunEvent
}

/** The next sunrise or sunset after [now] in the forecast [days], or null when they don't say. */
fun nextSunEvent(days: List<DayForecast>, now: Instant): SunEvent? {
    val events = days.flatMap { d ->
        listOfNotNull(d.sunrise?.let { SunEvent.Sunrise(it) }, d.sunset?.let { SunEvent.Sunset(it, 0L) })
    }
    val next = events.filter { it.at > now }.minByOrNull { it.at } ?: return null
    return if (next is SunEvent.Sunset) next.copy(daylightMillis = (next.at - now).inWholeMilliseconds) else next
}

/** "Sunset 17:48 · 2 h 10 m of light" or "Sunrise 07:02"; [time] is the event's time as the clock shows it. */
fun sunText(event: SunEvent, time: String, locale: PlatformLocale): String {
    val t = strings(locale)
    return when (event) {
        is SunEvent.Sunrise -> t.sunrise(time)
        is SunEvent.Sunset -> {
            val minutes = event.daylightMillis / 60_000
            t.sunset(time, t.hoursMinutes(minutes / 60, minutes % 60))
        }
    }
}

/** A day counted down to: its [label] and how many [days] away it is (0 is today). */
data class CountdownDay(val label: String, val days: Int)

/** How many countdowns the desk clock shows at a time: the nearest ones. */
const val COUNTDOWNS_SHOWN = 2

/**
 * The days [today] counts down to, nearest first and at most [COUNTDOWNS_SHOWN]: the user's own
 * [countdowns], and with [daysOff] (read from the calendar) the next day off, unless it's today or one of
 * the user's own already counts down to it. Days gone by are left out.
 */
fun countdownDays(countdowns: List<Countdown>, daysOff: DaysOff?, today: LocalDate): List<CountdownDay> {
    val own = countdowns.mapNotNull { c ->
        val date = runCatching { LocalDate.parse(c.date) }.getOrNull() ?: return@mapNotNull null
        val days = today.daysUntil(date)
        if (days < 0) null else CountdownDay(c.label, days) to date
    }
    val dayOff = daysOff?.entries
        ?.filter { it.key > today && own.none { o -> o.second == it.key } }
        ?.minByOrNull { it.key }
        ?.let { CountdownDay(it.value.trim(), today.daysUntil(it.key)) to it.key }
    return (own + listOfNotNull(dayOff)).sortedWith(compareBy({ it.first.days }, { it.first.label }))
        .map { it.first }
        .take(COUNTDOWNS_SHOWN)
}

/** "Vacation in 12 days", "Vacation tomorrow" or "Vacation today!". */
fun countdownText(day: CountdownDay, locale: PlatformLocale): String = strings(locale).daysUntil(day.label, day.days)

/**
 * The glance line's items at [now], in order: rain on the way, the sun, the UV index (while the sun is up and
 * it rounds to 1 or more),
 * the air quality and the countdowns, each as its setting asks. The weather ones need the weather on.
 * Times are read in [zone] and written with [time].
 */
fun glanceItems(
    settings: Settings,
    weather: Weather?,
    air: AirQuality?,
    daysOff: DaysOff?,
    now: Instant,
    zone: TimeZone,
    time: DateTimePattern,
    locale: PlatformLocale,
): List<String> = buildList {
    fun at(i: Instant) = time.format(i.toLocalDateTime(zone))
    if (settings.showWeather && weather != null) {
        if (settings.dayRainAlert) rainAlert(weather.code, weather.hours, now)?.let { add(rainAlertText(it, at(it.at), locale)) }
        if (settings.daySunTimes) nextSunEvent(weather.days, now)?.let { add(sunText(it, at(it.at), locale)) }
        if (settings.dayUvIndex && weather.isDay) weather.uvIndex?.takeIf { it >= 0.5 }?.let { add(uvText(it, locale)) }
    }
    if (settings.showWeather && settings.dayAirQuality && air != null) add(airQualityText(air, locale))
    countdownDays(settings.countdowns, if (settings.countdownDaysOff) daysOff else null, now.toLocalDateTime(zone).date)
        .forEach { add(countdownText(it, locale)) }
}
