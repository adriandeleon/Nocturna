package com.adl.nocturna.settings

import com.adl.nocturna.clock.label
import com.adl.nocturna.data.Alarm
import com.adl.nocturna.data.ClockStyle
import com.adl.nocturna.data.DayMode
import com.adl.nocturna.data.Settings
import com.adl.nocturna.text.PlatformLocale
import com.adl.nocturna.text.capitalizedFirst
import com.adl.nocturna.text.formatTime
import com.adl.nocturna.text.shortName
import com.adl.nocturna.text.strings
import kotlinx.datetime.DayOfWeek
import kotlinx.datetime.LocalTime

// One-line summaries for the rows of the Settings hub, so each page says what it is set to before you open it.

/** E.g. "Digital · Thin · 24-hour" or "Analog · 12-hour · seconds". */
fun clockSummary(s: Settings, locale: PlatformLocale): String {
    val t = strings(locale)
    return buildList {
        if (s.style == ClockStyle.DIGITAL) {
            add(t.styleDigital)
            add(s.font.label(locale))
        } else {
            add(t.styleAnalog)
        }
        add(if (s.use24h) t.hour24 else t.hour12)
        if (s.showSeconds) add(t.seconds)
    }.joinToString(" · ")
}

/** What the line under the clock shows, e.g. "Date, weather, battery and next alarm". */
fun infoLineSummary(s: Settings, locale: PlatformLocale): String {
    val t = strings(locale)
    val shown = buildList {
        if (s.showDate) add(t.infoDate)
        if (s.showWeather) add(t.infoWeather)
        if (s.showBattery) add(t.infoBattery)
        if (s.showNextAlarm) add(t.infoNextAlarm)
        if (s.showMoon) add(t.infoMoon)
        if (s.showNotifications) add(t.infoNotifications)
    }
    if (shown.isEmpty()) return t.justTheTime
    return t.andList(shown).capitalizedFirst(locale)
}

/** E.g. "Follows room light · night mode" or "Always full brightness". */
fun brightnessSummary(s: Settings, locale: PlatformLocale): String {
    val t = strings(locale)
    if (!s.dimEnabled) return t.dimmingOff
    return buildList {
        add(if (s.autoBrightness) t.followsRoomLight else t.phoneBrightness)
        if (s.nightModeEnabled) add(t.nightMode)
    }.joinToString(" · ")
}

/** E.g. "Off", "Always on" or "Weekdays, 09:00–18:00". */
fun dayModeSummary(s: Settings, locale: PlatformLocale): String = when (s.dayMode) {
    DayMode.OFF -> strings(locale).off
    DayMode.ON -> strings(locale).dayModeAlwaysOn
    DayMode.SCHEDULED -> {
        val from = formatTime(LocalTime(s.dayModeStart / 60, s.dayModeStart % 60), s.use24h, locale)
        val until = formatTime(LocalTime(s.dayModeEnd / 60, s.dayModeEnd % 60), s.use24h, locale)
        "${daysLabel(s.dayModeDays, locale)}, $from–$until"
    }
}

/** A weekday bitmask (bit 0 = Monday) in words: "Every day", "Weekdays", "Weekends" or "Mon, Wed, Fri". */
fun daysLabel(days: Int, locale: PlatformLocale): String = when (days and Alarm.ALL_DAYS) {
    0 -> strings(locale).noDays
    Alarm.ALL_DAYS -> strings(locale).everyDay
    Settings.WEEKDAYS -> strings(locale).weekdays
    Alarm.ALL_DAYS and Settings.WEEKDAYS.inv() -> strings(locale).weekends
    else -> DayOfWeek.entries.filterIndexed { i, _ -> days and (1 shl i) != 0 }
        .joinToString(", ") { it.shortName(locale) }
}

/** Where the weather comes from and in which unit, e.g. "Monterrey · °C" or "Your location · °F". */
fun weatherSummary(s: Settings, locale: PlatformLocale): String {
    val place = when {
        s.weatherUseDeviceLocation -> strings(locale).yourLocation
        s.weatherCity.isNotBlank() -> s.weatherCity
        else -> strings(locale).noPlaceSet
    }
    return "$place · " + if (s.weatherFahrenheit) "°F" else "°C"
}

/** How the clock comes up on its own. */
fun startSummary(s: Settings, locale: PlatformLocale): String =
    if (s.autoStartOnCharge) strings(locale).startOnCharge else strings(locale).startYourself

/** E.g. "2 of 3 on" or "No alarms yet". */
fun alarmsSummary(alarms: List<Alarm>, locale: PlatformLocale): String =
    if (alarms.isEmpty()) strings(locale).noAlarmsYet
    else strings(locale).alarmsOn(alarms.count { it.enabled }, alarms.size)
