package com.adl.nocturna.weather

import com.adl.nocturna.data.Settings
import com.adl.nocturna.ui.Glyph
import kotlinx.datetime.LocalDate
import kotlinx.datetime.TimeZone
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue
import kotlin.time.Duration
import kotlin.time.Duration.Companion.hours
import kotlin.time.Duration.Companion.minutes
import kotlin.time.Instant

class WakeWeatherTest {
    private val zone = TimeZone.of("America/Monterrey")
    /** 07:00 on 15 November in Monterrey. */
    private val now = Instant.parse("2026-11-15T13:00:00Z")
    private val monterrey = Settings(weatherCity = "Monterrey", weatherLat = 25.67, weatherLon = -100.31)

    private fun weather(age: Duration = 5.minutes, days: List<DayForecast> = forecast) = Weather(
        temperatureC = 14.2, code = 0, isDay = true, fetchedAt = (now - age).toEpochMilliseconds(),
        lat = 25.67, lon = -100.31, days = days,
    )

    private val forecast = listOf(
        DayForecast(LocalDate.parse("2026-11-14"), 61, 18.0, 11.0),
        DayForecast(LocalDate.parse("2026-11-15"), 0, 22.4, 8.6),
        DayForecast(LocalDate.parse("2026-11-16"), 3, 25.0, 12.0),
    )

    @Test fun showsCurrentConditionsAndTodaysHighAndLow() =
        assertEquals(WakeWeather(Glyph.SUN, "14° · 22° / 9°"), wakeWeather(monterrey, weather(), now, zone))

    @Test fun usesTheInfoLineUnits() =
        assertEquals("58° · 72° / 47°", wakeWeather(monterrey.copy(weatherFahrenheit = true), weather(), now, zone)?.text)

    @Test fun todayIsTheDateInTheGivenZone() {
        // 13:00 UTC is already 03:00 on the 16th in Kiritimati (UTC+14).
        assertEquals("14° · 25° / 12°", wakeWeather(monterrey, weather(), now, TimeZone.of("Pacific/Kiritimati"))?.text)
    }

    @Test fun withoutTodaysForecastShowsJustTheTemperature() =
        assertEquals(WakeWeather(Glyph.SUN, "14°"), wakeWeather(monterrey, weather(days = forecast.take(1)), now, zone))

    @Test fun nothingWhenSwitchedOffWithoutAPlaceOrWithoutData() {
        assertNull(wakeWeather(monterrey.copy(alarmShowWeather = false), weather(), now, zone))
        assertNull(wakeWeather(Settings(), weather(), now, zone))
        assertNull(wakeWeather(monterrey, null, now, zone))
    }

    @Test fun deviceLocationCountsAsAPlace() =
        assertEquals("14° · 22° / 9°", wakeWeather(Settings(weatherUseDeviceLocation = true), weather(), now, zone)?.text)

    @Test fun oldWeatherStillShowsWhileRefreshingButNotLastNights() {
        assertEquals("14° · 22° / 9°", wakeWeather(monterrey, weather(age = 2.hours), now, zone)?.text)
        assertNull(wakeWeather(monterrey, weather(age = 7.hours), now, zone))
    }

    @Test fun refreshesOnlyWhenWantedAndMissingOrOld() {
        assertTrue(wakeWeatherNeedsRefresh(monterrey, null, now))
        assertTrue(wakeWeatherNeedsRefresh(monterrey, weather(age = 30.minutes), now))
        assertFalse(wakeWeatherNeedsRefresh(monterrey, weather(age = 29.minutes), now))
        assertFalse(wakeWeatherNeedsRefresh(monterrey.copy(alarmShowWeather = false), null, now))
        assertFalse(wakeWeatherNeedsRefresh(Settings(), null, now))
    }
}
