package com.adl.nocturna.data

import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse

class SettingsJsonTest {
    /** The stored JSON for [s], changed the way an older or a damaged version would have left it. */
    private fun stored(s: Settings = Settings(), remove: List<String> = emptyList(), put: Map<String, JsonElement> = emptyMap()) =
        JsonObject(SettingsCodec.encode(s) - remove.toSet() + put)

    private fun roundTrip(s: Settings) = SettingsCodec.decodeFromString(SettingsCodec.encodeToString(s))

    @Test fun dayModeSurvivesRoundTrip() {
        val settings = Settings(
            dayMode = DayMode.SCHEDULED,
            dayModeStart = 8 * 60 + 30,
            dayModeEnd = 2 * 60,
            dayModeDays = 0b100_0001,
            dayModeOverrideUntil = 1_789_000_000_000L,
            showCalendarEvents = true,
            dayCalendarView = CalendarView.WEEK,
            dayPhotoVersion = 1_794_590_985_000L,
            dayMaxBrightness = 0.35f,
        )
        assertEquals(settings, roundTrip(settings))
    }

    @Test fun settingsSavedBeforeDayModeLoadWithItOff() {
        val loaded = SettingsCodec.decode(stored(remove = listOf("dayMode", "showCalendarEvents", "dayCalendarView", "dayPhotoVersion")))
        assertEquals(DayMode.OFF, loaded.dayMode)
        assertFalse(loaded.showCalendarEvents)
        assertEquals(CalendarView.MONTH, loaded.dayCalendarView)
        assertEquals(0L, loaded.dayPhotoVersion)
    }

    @Test fun settingsSavedBeforeTheDayBrightnessLimitGetTheDefault() =
        assertEquals(Settings().dayMaxBrightness, SettingsCodec.decode(stored(remove = listOf("dayMaxBrightness"))).dayMaxBrightness)

    @Test fun outOfRangeDayBrightnessIsClamped() {
        fun saved(v: JsonElement) = SettingsCodec.decode(stored(put = mapOf("dayMaxBrightness" to v))).dayMaxBrightness
        assertEquals(0.2f, saved(JsonPrimitive(0.0)))
        assertEquals(1f, saved(JsonPrimitive(3.0)))
        assertEquals(Settings().dayMaxBrightness, saved(JsonPrimitive("NaN")))
    }

    @Test fun settingsSavedBeforeTheScheduleKeepTheirSwitch() {
        fun saved(dayMode: Boolean) = stored(
            remove = listOf("dayModeStart", "dayModeEnd", "dayModeDays", "dayModeOverrideUntil"),
            put = mapOf("dayMode" to JsonPrimitive(dayMode)),
        )
        val on = SettingsCodec.decode(saved(true))
        assertEquals(DayMode.ON, on.dayMode)
        assertEquals(Settings().copy(dayMode = DayMode.ON), on)
        assertEquals(DayMode.OFF, SettingsCodec.decode(saved(false)).dayMode)
    }

    @Test fun outOfRangeScheduleValuesAreClamped() {
        val json = stored(
            put = mapOf(
                "dayModeStart" to JsonPrimitive(-5),
                "dayModeEnd" to JsonPrimitive(5000),
                "dayModeDays" to JsonPrimitive(0xFFFF),
                "dayMode" to JsonPrimitive("SOMETIMES"),
            ),
        )
        val loaded = SettingsCodec.decode(json)
        assertEquals(0, loaded.dayModeStart)
        assertEquals(24 * 60 - 1, loaded.dayModeEnd)
        assertEquals(Alarm.ALL_DAYS, loaded.dayModeDays)
        assertEquals(DayMode.OFF, loaded.dayMode)
    }

    @Test fun forecastSwitchSurvivesRoundTrip() {
        val settings = Settings(showForecast = false)
        assertEquals(settings, roundTrip(settings))
    }

    @Test fun settingsSavedBeforeTheForecastShowIt() =
        assertEquals(true, SettingsCodec.decode(stored(remove = listOf("showForecast"))).showForecast)

    @Test fun noWeatherPlaceSurvivesRoundTrip() = assertFalse(roundTrip(Settings()).hasWeatherLocation)
}
