package com.adl.nocturna.settings

import com.adl.nocturna.data.Alarm
import com.adl.nocturna.data.ClockFont
import com.adl.nocturna.data.ClockStyle
import com.adl.nocturna.data.DayMode
import com.adl.nocturna.data.Settings
import kotlin.test.assertEquals
import kotlin.test.Test
import com.adl.nocturna.text.testLocale

class SummariesTest {
    private val en = testLocale // British English

    @Test fun digitalClock() = assertEquals("Digital · Thin · 24-hour", clockSummary(Settings(), testLocale))

    @Test fun analogClockWithSeconds() = assertEquals(
        "Analog · 12-hour · seconds",
        clockSummary(Settings(style = ClockStyle.ANALOG, font = ClockFont.SERIF, use24h = false, showSeconds = true), testLocale),
    )

    @Test fun infoLineListsWhatIsShown() = assertEquals(
        "Date, weather, battery, next alarm and notifications",
        infoLineSummary(Settings(), testLocale),
    )

    @Test fun infoLineWithOneItem() = assertEquals(
        "Battery",
        infoLineSummary(Settings(showDate = false, showWeather = false, showNextAlarm = false, showNotifications = false), testLocale),
    )

    @Test fun infoLineWithTheMoon() = assertEquals(
        "Battery and moon",
        infoLineSummary(Settings(showDate = false, showWeather = false, showNextAlarm = false, showNotifications = false, showMoon = true), testLocale),
    )

    @Test fun infoLineWithNothing() = assertEquals(
        "Just the time",
        infoLineSummary(Settings(showDate = false, showWeather = false, showBattery = false, showNextAlarm = false, showNotifications = false), testLocale),
    )

    @Test fun brightness() {
        assertEquals("Follows room light · night mode", brightnessSummary(Settings(), testLocale))
        assertEquals("Phone brightness", brightnessSummary(Settings(autoBrightness = false, nightModeEnabled = false), testLocale))
        assertEquals("Dimming off", brightnessSummary(Settings(dimEnabled = false), testLocale))
    }

    @Test fun dayModeOffAndOn() {
        assertEquals("Off", dayModeSummary(Settings(dayMode = DayMode.OFF), en))
        assertEquals("Always on", dayModeSummary(Settings(dayMode = DayMode.ON), en))
    }

    @Test fun dayModeSchedule() = assertEquals(
        "Weekdays, 09:00–18:00",
        dayModeSummary(Settings(dayMode = DayMode.SCHEDULED), en),
    )

    @Test fun dayModeScheduleIn12Hours() = assertEquals(
        "weekends, 8:30 am–1:00 pm",
        dayModeSummary(
            Settings(dayMode = DayMode.SCHEDULED, use24h = false, dayModeStart = 8 * 60 + 30, dayModeEnd = 13 * 60, dayModeDays = 0b110_0000),
            en,
        ).lowercase(),
    )

    @Test fun days() {
        assertEquals("No days", daysLabel(0, en))
        assertEquals("Every day", daysLabel(Alarm.ALL_DAYS, en))
        assertEquals("Weekdays", daysLabel(Settings.WEEKDAYS, en))
        assertEquals("Weekends", daysLabel(0b110_0000, en))
        assertEquals("Mon, Wed, Fri", daysLabel(0b001_0101, en))
    }

    @Test fun weather() {
        assertEquals("No place set · °C", weatherSummary(Settings(), testLocale))
        assertEquals("Monterrey · °F", weatherSummary(Settings(weatherCity = "Monterrey", weatherFahrenheit = true), testLocale))
        assertEquals("Your location · °C", weatherSummary(Settings(weatherCity = "Monterrey", weatherUseDeviceLocation = true), testLocale))
    }

    @Test fun alarms() {
        assertEquals("No alarms yet", alarmsSummary(emptyList(), testLocale))
        val on = Alarm(id = 1, hour = 7, minute = 0, enabled = true)
        assertEquals("1 of 2 on", alarmsSummary(listOf(on, on.copy(id = 2, enabled = false)), testLocale))
    }
}
