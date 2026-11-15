package com.adl.nocturna.data

import com.adl.nocturna.alarm.DEFAULT_DAY_OFF_KEYWORDS
import com.adl.nocturna.sound.SleepSound
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

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

    @Test fun wallpaperColourSurvivesRoundTrip() {
        val settings = Settings(color = 0xFFFFD27A.toInt(), colorFromWallpaper = true)
        assertEquals(settings, roundTrip(settings))
    }

    @Test fun settingsSavedBeforeTheWallpaperColourKeepTheirSwatch() {
        val loaded = SettingsCodec.decode(stored(Settings(color = 0xFFFFD27A.toInt()), remove = listOf("colorFromWallpaper")))
        assertFalse(loaded.colorFromWallpaper)
        assertEquals(0xFFFFD27A.toInt(), loaded.color)
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

    @Test fun tapToPeekSurvivesRoundTrip() = assertFalse(roundTrip(Settings(tapToPeek = false)).tapToPeek)

    @Test fun settingsSavedBeforeTapToPeekHaveItOn() =
        assertTrue(SettingsCodec.decode(stored(Settings(tapToPeek = false), remove = listOf("tapToPeek"))).tapToPeek)

    @Test fun sunriseSurvivesRoundTrip() {
        for (minutes in Settings.SUNRISE_CHOICES) assertEquals(minutes, roundTrip(Settings(sunriseMinutes = minutes)).sunriseMinutes)
    }

    @Test fun settingsSavedBeforeTheSunriseGetTheDefault() =
        assertEquals(20, SettingsCodec.decode(stored(remove = listOf("sunriseMinutes"))).sunriseMinutes)

    @Test fun outOfRangeSunriseIsClamped() {
        assertEquals(0, SettingsCodec.decode(stored(put = mapOf("sunriseMinutes" to JsonPrimitive(-5)))).sunriseMinutes)
        assertEquals(30, SettingsCodec.decode(stored(put = mapOf("sunriseMinutes" to JsonPrimitive(600)))).sunriseMinutes)
    }

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

    @Test fun meetingChimeSurvivesRoundTrip() {
        val settings = Settings(meetingChimeMinutes = 2)
        assertEquals(settings, roundTrip(settings))
    }

    @Test fun settingsSavedBeforeTheMeetingChimeLoadWithItOff() =
        assertEquals(0, SettingsCodec.decode(stored(Settings(meetingChimeMinutes = 5), remove = listOf("meetingChimeMinutes"))).meetingChimeMinutes)

    @Test fun anUnknownMeetingChimeIsOff() =
        assertEquals(0, SettingsCodec.decode(stored(put = mapOf("meetingChimeMinutes" to JsonPrimitive(7)))).meetingChimeMinutes)

    @Test fun focusLengthsSurviveRoundTrip() {
        val settings = Settings(focusMinutes = 50, focusBreakMinutes = 10, focusLongBreakMinutes = 30)
        assertEquals(settings, roundTrip(settings))
    }

    @Test fun settingsSavedBeforeTheFocusTimerLoadWithItsDefaults() {
        val old = Settings(focusMinutes = 50, focusBreakMinutes = 10, focusLongBreakMinutes = 30)
        val loaded = SettingsCodec.decode(stored(old, remove = listOf("focusMinutes", "focusBreakMinutes", "focusLongBreakMinutes")))
        assertEquals(25, loaded.focusMinutes)
        assertEquals(5, loaded.focusBreakMinutes)
        assertEquals(15, loaded.focusLongBreakMinutes)
    }

    @Test fun unknownFocusLengthsFallBackToTheDefaults() {
        val loaded = SettingsCodec.decode(
            stored(put = mapOf("focusMinutes" to JsonPrimitive(0), "focusBreakMinutes" to JsonPrimitive(-5), "focusLongBreakMinutes" to JsonPrimitive(999))),
        )
        assertEquals(Settings().focusMinutes, loaded.focusMinutes)
        assertEquals(Settings().focusBreakMinutes, loaded.focusBreakMinutes)
        assertEquals(Settings().focusLongBreakMinutes, loaded.focusLongBreakMinutes)
    }

    @Test fun forecastSwitchSurvivesRoundTrip() {
        val settings = Settings(showForecast = false)
        assertEquals(settings, roundTrip(settings))
    }

    @Test fun settingsSavedBeforeTheForecastShowIt() =
        assertEquals(true, SettingsCodec.decode(stored(remove = listOf("showForecast"))).showForecast)

    @Test fun noWeatherPlaceSurvivesRoundTrip() = assertFalse(roundTrip(Settings()).hasWeatherLocation)

    @Test fun sleepSoundSurvivesRoundTripAndIsWhatTheRadioPlays() {
        val settings = Settings(selectedStation = 2, selectedSound = SleepSound.RAIN.id)
        val loaded = roundTrip(settings)
        assertEquals(settings, loaded)
        assertEquals(SleepSound.RAIN.station, loaded.currentStation)
        // Sleep sounds are not stored with the stations, which stay the user's own list.
        assertEquals(Settings().stations, loaded.stations)
    }

    @Test fun settingsSavedBeforeSleepSoundsKeepTheirStation() {
        val old = Settings(selectedStation = 3)
        val loaded = SettingsCodec.decode(stored(old, remove = listOf("selectedSound")))
        assertEquals(old, loaded)
        assertNull(loaded.currentSleepSound)
        assertEquals(old.stations[3], loaded.currentStation)
    }

    @Test fun unknownSleepSoundFallsBackToTheStation() {
        val loaded = SettingsCodec.decode(stored(Settings(selectedStation = 1), put = mapOf("selectedSound" to JsonPrimitive("thunder"))))
        assertNull(loaded.currentSleepSound)
        assertEquals(Settings().stations[1], loaded.currentStation)
    }
    @Test fun wakeWeatherSwitchSurvivesRoundTrip() {
        val settings = Settings(alarmShowWeather = false)
        assertEquals(settings, roundTrip(settings))
    }

    @Test fun settingsSavedBeforeTheWakeWeatherShowIt() =
        assertEquals(true, SettingsCodec.decode(stored(remove = listOf("alarmShowWeather"))).alarmShowWeather)
    @Test fun dayOffKeywordsSurviveRoundTrip() {
        val settings = Settings(dayOffKeywords = "Feriado, puente")
        assertEquals(settings, roundTrip(settings))
    }

    @Test fun settingsSavedBeforeDayOffKeywordsGetTheDefaults() =
        assertEquals(DEFAULT_DAY_OFF_KEYWORDS, SettingsCodec.decode(stored(remove = listOf("dayOffKeywords"))).dayOffKeywords)

    @Test fun alarmCountdownAndBedtimeSurviveRoundTrip() {
        val settings = Settings(showTimeToAlarm = false, bedtimeSleepHours = 7.5f)
        assertEquals(settings, roundTrip(settings))
    }

    @Test fun settingsSavedBeforeTheCountdownShowItWithNoBedtimeReminder() {
        val loaded = SettingsCodec.decode(stored(remove = listOf("showTimeToAlarm", "bedtimeSleepHours")))
        assertEquals(true, loaded.showTimeToAlarm)
        assertEquals(0f, loaded.bedtimeSleepHours)
    }

    @Test fun unknownSleepHoursTurnTheBedtimeReminderOff() =
        assertEquals(0f, SettingsCodec.decode(stored(put = mapOf("bedtimeSleepHours" to JsonPrimitive(7.25)))).bedtimeSleepHours)
    @Test fun dndWhileShowingSurvivesRoundTrip() =
        assertEquals(true, roundTrip(Settings(dndWhileShowing = true)).dndWhileShowing)

    @Test fun settingsSavedBeforeDndWhileShowingLoadWithItOff() {
        val loaded = SettingsCodec.decode(stored(Settings(dndWhileShowing = true, autoStartOnCharge = true), remove = listOf("dndWhileShowing")))
        assertFalse(loaded.dndWhileShowing)
        assertEquals(true, loaded.autoStartOnCharge)
    }

    @Test fun worldZonesSurviveRoundTrip() {
        val settings = Settings(worldZones = listOf(WorldZone("London", "Europe/London"), WorldZone("Tokyo", "Asia/Tokyo")))
        assertEquals(settings, roundTrip(settings))
    }

    @Test fun settingsSavedBeforeWorldZonesLoadWithNone() {
        val loaded = SettingsCodec.decode(stored(Settings(focusMinutes = 50), remove = listOf("worldZones")))
        assertEquals(emptyList(), loaded.worldZones)
        assertEquals(50, loaded.focusMinutes)
    }

    @Test fun damagedWorldZonesAreSkippedAndCapped() {
        fun zone(label: String, zone: String) = JsonObject(mapOf("label" to JsonPrimitive(label), "zone" to JsonPrimitive(zone)))
        val zones = JsonArray(
            listOf(JsonPrimitive("Paris"), zone(" ", "Europe/Paris"), zone("Lima", "America/Lima"), zone("Seoul", "Asia/Seoul"), zone("Oslo", "Europe/Oslo")),
        )
        val loaded = SettingsCodec.decode(stored(put = mapOf("worldZones" to zones)))
        assertEquals(listOf(WorldZone("Lima", "America/Lima"), WorldZone("Seoul", "Asia/Seoul")), loaded.worldZones)
    }

    @Test fun alarmSumsSurviveRoundTrip() = assertEquals(3, roundTrip(Settings(alarmSums = 3)).alarmSums)

    @Test fun settingsSavedBeforeAlarmSumsAskForOne() =
        assertEquals(1, SettingsCodec.decode(stored(Settings(alarmSums = 3), remove = listOf("alarmSums"))).alarmSums)

    @Test fun alarmSumsAreHeldToOneToThree() =
        assertEquals(3, SettingsCodec.decode(stored(put = mapOf("alarmSums" to JsonPrimitive(7)))).alarmSums)

    @Test fun dayHourlyWeatherSurvivesRoundTrip() =
        assertFalse(roundTrip(Settings(dayHourlyWeather = false)).dayHourlyWeather)

    @Test fun settingsSavedBeforeDayHourlyWeatherShowTheStrip() {
        val loaded = SettingsCodec.decode(stored(Settings(dayHourlyWeather = false, focusMinutes = 50), remove = listOf("dayHourlyWeather")))
        assertEquals(true, loaded.dayHourlyWeather)
        assertEquals(50, loaded.focusMinutes)
    }

    @Test fun moonPhaseSwitchSurvivesRoundTrip() =
        assertEquals(true, roundTrip(Settings(showMoon = true)).showMoon)

    @Test fun settingsSavedBeforeTheMoonPhaseLoadWithItOff() {
        val loaded = SettingsCodec.decode(stored(Settings(showMoon = true, showBattery = false), remove = listOf("showMoon")))
        assertFalse(loaded.showMoon)
        assertFalse(loaded.showBattery)
    }

    @Test fun waveSwitchesSurviveRoundTrip() {
        val loaded = roundTrip(Settings(waveToSnooze = true, waveToPeek = true))
        assertTrue(loaded.waveToSnooze)
        assertTrue(loaded.waveToPeek)
    }

    @Test fun settingsSavedBeforeWavesLoadWithThemOff() {
        val loaded = SettingsCodec.decode(
            stored(Settings(waveToSnooze = true, waveToPeek = true, tapToPeek = false, alarmVibrate = false), remove = listOf("waveToSnooze", "waveToPeek")),
        )
        assertFalse(loaded.waveToSnooze)
        assertFalse(loaded.waveToPeek)
        assertFalse(loaded.tapToPeek)
        assertFalse(loaded.alarmVibrate)
    }
}
