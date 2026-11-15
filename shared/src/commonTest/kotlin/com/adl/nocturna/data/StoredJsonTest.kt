package com.adl.nocturna.data

import kotlinx.datetime.LocalDate
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

/**
 * What's already on people's phones: JSON written by the versions that stored settings and alarms with
 * Android's org.json, which escapes "/" as "\/" and writes floats as their double values.
 */
class StoredJsonTest {
    private val settingsWrittenByOrgJson = """{"dayPhotoVersion":1794590985000,"alarmRampSeconds":45,"weatherFahrenheit":true,""" +
        """"color":-30107,"showNextAlarm":true,"dayModeStart":510,"alarmVibrate":true,"dayCalendarView":"WEEK",""" +
        """"burnInProtection":true,"showDate":true,"snoozeMinutes":5,"weatherLat":-23.5475,"stationsVersion":2,""" +
        """"showForecast":false,"showSeconds":true,"maxBrightness":0.8999999761581421,"use24h":false,""" +
        """"dayModeOverrideUntil":1789000000000,"selectedStation":0,"dayModeEnd":120,"autoStartOnCharge":false,""" +
        """"minBrightness":0.05000000074505806,"dayMode":"SCHEDULED","showCalendarEvents":true,"autoBrightness":true,""" +
        """"showNotifications":true,"nightModeLux":2.5,"stations":[{"name":"Café \/ Radio",""" +
        """"url":"https:\/\/example.com\/a?b=1&c=2"}],"dimEnabled":true,"dayModeDays":65,""" +
        """"weatherUseDeviceLocation":false,"weatherLon":-46.63611,"showBattery":true,"showWeather":true,""" +
        """"weatherCity":"São Paulo, \"Brasil\"","style":"ANALOG","nightModeEnabled":true,"font":"SERIF"}"""

    private val alarmsWrittenByOrgJson = """[{"hour":6,"skipDate":"2026-11-16","station":{"name":"D99",""" +
        """"url":"https:\/\/mdstrm.com\/audio\/x\/icecast.audio"},"days":31,"id":1731600000000,""" +
        """"label":"Work \"early\" \/ gym","enabled":true,"minute":40},""" +
        """{"hour":9,"days":0,"id":1731600000001,"label":"","enabled":false,"minute":0}]"""

    @Test fun settingsFromOrgJsonLoadUnchanged() = assertEquals(
        Settings(
            style = ClockStyle.ANALOG, use24h = false, showSeconds = true, font = ClockFont.SERIF,
            color = 0xFFFF8A65.toInt(), showForecast = false, minBrightness = 0.05f, maxBrightness = 0.9f,
            nightModeLux = 2.5f, dayMode = DayMode.SCHEDULED, dayModeStart = 8 * 60 + 30, dayModeEnd = 2 * 60,
            dayModeDays = 0b100_0001, dayModeOverrideUntil = 1_789_000_000_000L, showCalendarEvents = true,
            dayCalendarView = CalendarView.WEEK, dayPhotoVersion = 1_794_590_985_000L, weatherCity = "São Paulo, \"Brasil\"",
            weatherLat = -23.5475, weatherLon = -46.63611, weatherFahrenheit = true, snoozeMinutes = 5,
            stations = listOf(Station("Café / Radio", "https://example.com/a?b=1&c=2")), selectedStation = 0,
        ),
        SettingsCodec.decodeFromString(settingsWrittenByOrgJson),
    )

    @Test fun alarmsFromOrgJsonLoadUnchanged() = assertEquals(
        listOf(
            Alarm(
                1_731_600_000_000L, 6, 40, days = 0b0011111, label = "Work \"early\" / gym",
                station = Station("D99", "https://mdstrm.com/audio/x/icecast.audio"), skipDate = LocalDate(2026, 11, 16),
            ),
            Alarm(1_731_600_000_001L, 9, 0, enabled = false),
        ),
        AlarmCodec.decode(alarmsWrittenByOrgJson),
    )

    @Test fun valuesOfTheWrongTypeAreReadAsOrgJsonDid() {
        val loaded = SettingsCodec.decodeFromString(
            """{"use24h":"false","snoozeMinutes":"7","alarmRampSeconds":30.9,"showDate":null,"weatherCity":12}""",
        )
        assertEquals(false, loaded.use24h)
        assertEquals(7, loaded.snoozeMinutes)
        assertEquals(30, loaded.alarmRampSeconds)
        assertEquals(Settings().showDate, loaded.showDate)
        assertEquals("12", loaded.weatherCity)
    }

    @Test fun anAlarmWithoutItsTimeIsNotGuessed() {
        val error = runCatching { AlarmCodec.decode("""[{"id":1,"minute":0}]""") }.exceptionOrNull()
        assertEquals(IllegalArgumentException::class, error?.let { it::class })
    }

    @Test fun aStringDayModeIsNotTheOldSwitch() =
        assertEquals(DayMode.OFF, SettingsCodec.decodeFromString("""{"dayMode":"true"}""").dayMode)

    @Test fun aMissingStationListKeepsTheDefaults() =
        assertEquals(Settings().stations, SettingsCodec.decodeFromString("""{"stationsVersion":2}""").stations)

    @Test fun noSkipDateMeansNone() = assertNull(AlarmCodec.decode("""[{"id":1,"hour":7,"minute":0}]""").single().skipDate)
}
