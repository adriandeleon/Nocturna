package com.adl.nocturna.data

import org.json.JSONObject
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Test

class SettingsJsonTest {
    @Test fun dayModeSurvivesRoundTrip() {
        val settings = Settings(dayMode = true, showCalendarEvents = true)
        assertEquals(settings, SettingsStore.decode(SettingsStore.encode(settings)))
    }

    @Test fun settingsSavedBeforeDayModeLoadWithItOff() {
        val old = SettingsStore.encode(Settings()).apply {
            remove("dayMode")
            remove("showCalendarEvents")
        }
        val loaded = SettingsStore.decode(JSONObject(old.toString()))
        assertFalse(loaded.dayMode)
        assertFalse(loaded.showCalendarEvents)
    }
}
