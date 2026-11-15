package com.adl.nocturna.settings

import com.adl.nocturna.clock.label
import com.adl.nocturna.data.Alarm
import com.adl.nocturna.data.ClockStyle
import com.adl.nocturna.data.DayMode
import com.adl.nocturna.data.Settings
import com.adl.nocturna.text.PlatformLocale
import com.adl.nocturna.text.formatTime
import com.adl.nocturna.text.shortName
import kotlinx.datetime.DayOfWeek
import kotlinx.datetime.LocalTime

// One-line summaries for the rows of the Settings hub, so each page says what it is set to before you open it.

/** E.g. "Digital · Thin · 24-hour" or "Analog · 12-hour · seconds". */
fun clockSummary(s: Settings): String = buildList {
    if (s.style == ClockStyle.DIGITAL) {
        add("Digital")
        add(s.font.label())
    } else {
        add("Analog")
    }
    add(if (s.use24h) "24-hour" else "12-hour")
    if (s.showSeconds) add("seconds")
}.joinToString(" · ")

/** What the line under the clock shows, e.g. "Date, weather, battery and next alarm". */
fun infoLineSummary(s: Settings): String {
    val shown = buildList {
        if (s.showDate) add("date")
        if (s.showWeather) add("weather")
        if (s.showBattery) add("battery")
        if (s.showNextAlarm) add("next alarm")
        if (s.showMoon) add("moon")
        if (s.showNotifications) add("notifications")
    }
    if (shown.isEmpty()) return "Just the time"
    val text = if (shown.size == 1) shown[0] else shown.dropLast(1).joinToString(", ") + " and " + shown.last()
    return text.replaceFirstChar { it.uppercase() }
}

/** E.g. "Follows room light · night mode" or "Always full brightness". */
fun brightnessSummary(s: Settings): String {
    if (!s.dimEnabled) return "Dimming off"
    return buildList {
        add(if (s.autoBrightness) "Follows room light" else "Phone brightness")
        if (s.nightModeEnabled) add("night mode")
    }.joinToString(" · ")
}

/** E.g. "Off", "Always on" or "Weekdays, 09:00–18:00". */
fun dayModeSummary(s: Settings, locale: PlatformLocale): String = when (s.dayMode) {
    DayMode.OFF -> "Off"
    DayMode.ON -> "Always on"
    DayMode.SCHEDULED -> {
        val from = formatTime(LocalTime(s.dayModeStart / 60, s.dayModeStart % 60), s.use24h, locale)
        val until = formatTime(LocalTime(s.dayModeEnd / 60, s.dayModeEnd % 60), s.use24h, locale)
        "${daysLabel(s.dayModeDays, locale)}, $from–$until"
    }
}

/** A weekday bitmask (bit 0 = Monday) in words: "Every day", "Weekdays", "Weekends" or "Mon, Wed, Fri". */
fun daysLabel(days: Int, locale: PlatformLocale): String = when (days and Alarm.ALL_DAYS) {
    0 -> "No days"
    Alarm.ALL_DAYS -> "Every day"
    Settings.WEEKDAYS -> "Weekdays"
    Alarm.ALL_DAYS and Settings.WEEKDAYS.inv() -> "Weekends"
    else -> DayOfWeek.entries.filterIndexed { i, _ -> days and (1 shl i) != 0 }
        .joinToString(", ") { it.shortName(locale) }
}

/** Where the weather comes from and in which unit, e.g. "Monterrey · °C" or "Your location · °F". */
fun weatherSummary(s: Settings): String {
    val place = when {
        s.weatherUseDeviceLocation -> "Your location"
        s.weatherCity.isNotBlank() -> s.weatherCity
        else -> "No place set"
    }
    return "$place · " + if (s.weatherFahrenheit) "°F" else "°C"
}

/** How the clock comes up on its own. */
fun startSummary(s: Settings): String =
    if (s.autoStartOnCharge) "Opens when you plug in a locked phone" else "As the screen saver, or open it yourself"

fun alarmsSummary(alarms: List<Alarm>): String =
    if (alarms.isEmpty()) "No alarms yet" else "${alarms.count { it.enabled }} of ${alarms.size} on"
