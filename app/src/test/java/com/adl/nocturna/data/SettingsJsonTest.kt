package com.adl.nocturna.data

import org.json.JSONObject
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Test

class SettingsJsonTest {
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
        assertEquals(settings, SettingsStore.decode(JSONObject(SettingsStore.encode(settings).toString())))
    }

    @Test fun settingsSavedBeforeDayModeLoadWithItOff() {
        val old = SettingsStore.encode(Settings()).apply {
            remove("dayMode")
            remove("showCalendarEvents")
            remove("dayCalendarView")
            remove("dayPhotoVersion")
        }
        val loaded = SettingsStore.decode(JSONObject(old.toString()))
        assertEquals(DayMode.OFF, loaded.dayMode)
        assertFalse(loaded.showCalendarEvents)
        assertEquals(CalendarView.MONTH, loaded.dayCalendarView)
        assertEquals(0L, loaded.dayPhotoVersion)
    }

    @Test fun settingsSavedBeforeTheDayBrightnessLimitGetTheDefault() {
        val old = SettingsStore.encode(Settings()).apply { remove("dayMaxBrightness") }
        assertEquals(Settings().dayMaxBrightness, SettingsStore.decode(JSONObject(old.toString())).dayMaxBrightness)
    }

    @Test fun outOfRangeDayBrightnessIsClamped() {
        fun saved(v: Double) = SettingsStore.decode(SettingsStore.encode(Settings()).apply { put("dayMaxBrightness", v) })
        assertEquals(0.2f, saved(0.0).dayMaxBrightness)
        assertEquals(1f, saved(3.0).dayMaxBrightness)
    }

    @Test fun settingsSavedBeforeTheScheduleKeepTheirSwitch() {
        fun saved(dayMode: Boolean) = JSONObject(
            SettingsStore.encode(Settings()).apply {
                put("dayMode", dayMode)
                listOf("dayModeStart", "dayModeEnd", "dayModeDays", "dayModeOverrideUntil").forEach { remove(it) }
            }.toString()
        )
        val on = SettingsStore.decode(saved(true))
        assertEquals(DayMode.ON, on.dayMode)
        assertEquals(Settings().copy(dayMode = DayMode.ON), on)
        assertEquals(DayMode.OFF, SettingsStore.decode(saved(false)).dayMode)
    }

    @Test fun outOfRangeScheduleValuesAreClamped() {
        val json = SettingsStore.encode(Settings()).apply {
            put("dayModeStart", -5)
            put("dayModeEnd", 5000)
            put("dayModeDays", 0xFFFF)
            put("dayMode", "SOMETIMES")
        }
        val loaded = SettingsStore.decode(json)
        assertEquals(0, loaded.dayModeStart)
        assertEquals(24 * 60 - 1, loaded.dayModeEnd)
        assertEquals(Alarm.ALL_DAYS, loaded.dayModeDays)
        assertEquals(DayMode.OFF, loaded.dayMode)
    }

    @Test fun forecastSwitchSurvivesRoundTrip() {
        val settings = Settings(showForecast = false)
        assertEquals(settings, SettingsStore.decode(JSONObject(SettingsStore.encode(settings).toString())))
    }

    @Test fun settingsSavedBeforeTheForecastShowIt() {
        val old = SettingsStore.encode(Settings()).apply { remove("showForecast") }
        assertEquals(true, SettingsStore.decode(JSONObject(old.toString())).showForecast)
    }
}
