package com.adl.nocturna.data

import com.adl.nocturna.alarm.SUM_COUNTS
import com.adl.nocturna.radio.RADIO_VOLUME_STEPS
import kotlinx.datetime.LocalDate
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.buildJsonArray
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.put

/** [Settings] as the one JSON object they are stored as. */
object SettingsCodec {
    fun encodeToString(s: Settings): String = encode(s).toString()

    /** Reads what [encodeToString] wrote; throws when [json] isn't a JSON object at all. */
    fun decodeFromString(json: String): Settings = decode(Json.parseToJsonElement(json).jsonObject)

    fun encode(s: Settings): JsonObject = buildJsonObject {
        put("style", s.style.name)
        put("use24h", s.use24h)
        put("showSeconds", s.showSeconds)
        put("font", s.font.name)
        put("color", s.color)
        put("colorFromWallpaper", s.colorFromWallpaper)
        put("showDate", s.showDate)
        put("showBattery", s.showBattery)
        put("showNextAlarm", s.showNextAlarm)
        put("showTimeToAlarm", s.showTimeToAlarm)
        put("showMoon", s.showMoon)
        put("showWeather", s.showWeather)
        put("showForecast", s.showForecast)
        put("showNotifications", s.showNotifications)
        put("dimEnabled", s.dimEnabled)
        put("autoBrightness", s.autoBrightness)
        put("minBrightness", s.minBrightness.toDouble())
        put("maxBrightness", s.maxBrightness.toDouble())
        put("nightModeEnabled", s.nightModeEnabled)
        put("nightModeLux", s.nightModeLux.toDouble())
        put("tapToPeek", s.tapToPeek)
        put("waveToPeek", s.waveToPeek)
        put("burnInProtection", s.burnInProtection)
        put("dayMode", s.dayMode.name)
        put("dayModeStart", s.dayModeStart)
        put("dayModeEnd", s.dayModeEnd)
        put("dayModeDays", s.dayModeDays)
        put("dayModeOverrideUntil", s.dayModeOverrideUntil)
        put("showCalendarEvents", s.showCalendarEvents)
        put("dayCalendarView", s.dayCalendarView.name)
        put("dayMaxBrightness", s.dayMaxBrightness.toDouble())
        put("dayPhotoVersion", s.dayPhotoVersion)
        put("meetingChimeMinutes", s.meetingChimeMinutes)
        put("focusMinutes", s.focusMinutes)
        put("focusBreakMinutes", s.focusBreakMinutes)
        put("focusLongBreakMinutes", s.focusLongBreakMinutes)
        put("worldZones", buildJsonArray {
            s.worldZones.forEach { add(buildJsonObject { put("label", it.label); put("zone", it.zoneId) }) }
        })
        put("dayHourlyWeather", s.dayHourlyWeather)
        put("dayRainAlert", s.dayRainAlert)
        put("daySunTimes", s.daySunTimes)
        put("dayUvIndex", s.dayUvIndex)
        put("dayAirQuality", s.dayAirQuality)
        put("dayWeatherScene", s.dayWeatherScene)
        put("dayHolidays", s.dayHolidays)
        put("birthday", s.birthday)
        put("dayWeekNumbers", s.dayWeekNumbers)
        put("dayBirthdays", s.dayBirthdays)
        put("countdowns", buildJsonArray {
            s.countdowns.forEach { add(buildJsonObject { put("label", it.label); put("date", it.date) }) }
        })
        put("countdownDaysOff", s.countdownDaysOff)
        put("dayTodos", s.dayTodos)
        put("todos", buildJsonArray {
            s.todos.forEach { add(buildJsonObject { put("text", it.text); put("doneOn", it.doneOn) }) }
        })
        put("stretchReminderMinutes", s.stretchReminderMinutes)
        put("waterReminderMinutes", s.waterReminderMinutes)
        put("batteryCarePercent", s.batteryCarePercent)
        put("waveForFocus", s.waveForFocus)
        put("autoStartOnCharge", s.autoStartOnCharge)
        put("dndWhileShowing", s.dndWhileShowing)
        put("weatherUseDeviceLocation", s.weatherUseDeviceLocation)
        put("weatherCity", s.weatherCity)
        // JSON has no NaN: no place is stored as no keys.
        if (s.hasWeatherLocation) {
            put("weatherLat", s.weatherLat)
            put("weatherLon", s.weatherLon)
        }
        put("weatherFahrenheit", s.weatherFahrenheit)
        put("snoozeMinutes", s.snoozeMinutes)
        put("alarmRampSeconds", s.alarmRampSeconds)
        put("alarmVibrate", s.alarmVibrate)
        put("waveToSnooze", s.waveToSnooze)
        put("alarmSums", s.alarmSums)
        put("sunriseMinutes", s.sunriseMinutes)
        put("alarmShowWeather", s.alarmShowWeather)
        put("dayOffKeywords", s.dayOffKeywords)
        put("bedtimeSleepHours", s.bedtimeSleepHours.toDouble())
        put("stations", buildJsonArray {
            s.stations.forEach { add(buildJsonObject { put("name", it.name); put("url", it.url) }) }
        })
        put("selectedStation", s.selectedStation)
        put("selectedSound", s.selectedSound)
        put("radioSleepMinutes", s.radioSleepMinutes)
        put("favoriteStations", buildJsonArray { s.favoriteStations.sorted().forEach { add(JsonPrimitive(it)) } })
        put("radioVolume", s.radioVolume)
        put("stationsVersion", Settings.STATIONS_VERSION)
    }

    fun decode(j: JsonObject): Settings {
        val d = Settings()
        var stations = j.optArray("stations")?.map { element ->
            val o = element as? JsonObject ?: throw IllegalArgumentException("Station is not a JSON object")
            Station(o.getString("name"), o.getString("url"))
        } ?: d.stations
        var selected = j.optInt("selectedStation", d.selectedStation)
        // Lists saved before version 2 don't have the Monterrey stations yet: put them on top.
        if (j.optInt("stationsVersion", 1) < 2) {
            val added = Settings.MONTERREY_STATIONS.filter { new -> stations.none { it.url == new.url } }
            stations = added + stations
            selected += added.size
        }
        return Settings(
            style = enumOr(j.optString("style"), d.style),
            use24h = j.optBoolean("use24h", d.use24h),
            showSeconds = j.optBoolean("showSeconds", d.showSeconds),
            font = enumOr(j.optString("font"), d.font),
            color = j.optInt("color", d.color),
            colorFromWallpaper = j.optBoolean("colorFromWallpaper", d.colorFromWallpaper),
            showDate = j.optBoolean("showDate", d.showDate),
            showBattery = j.optBoolean("showBattery", d.showBattery),
            showNextAlarm = j.optBoolean("showNextAlarm", d.showNextAlarm),
            showTimeToAlarm = j.optBoolean("showTimeToAlarm", d.showTimeToAlarm),
            showMoon = j.optBoolean("showMoon", d.showMoon),
            showWeather = j.optBoolean("showWeather", d.showWeather),
            showForecast = j.optBoolean("showForecast", d.showForecast),
            showNotifications = j.optBoolean("showNotifications", d.showNotifications),
            dimEnabled = j.optBoolean("dimEnabled", d.dimEnabled),
            autoBrightness = j.optBoolean("autoBrightness", d.autoBrightness),
            minBrightness = j.optDouble("minBrightness", d.minBrightness.toDouble()).toFloat(),
            maxBrightness = j.optDouble("maxBrightness", d.maxBrightness.toDouble()).toFloat(),
            nightModeEnabled = j.optBoolean("nightModeEnabled", d.nightModeEnabled),
            nightModeLux = j.optDouble("nightModeLux", d.nightModeLux.toDouble()).toFloat(),
            tapToPeek = j.optBoolean("tapToPeek", d.tapToPeek),
            waveToPeek = j.optBoolean("waveToPeek", d.waveToPeek),
            burnInProtection = j.optBoolean("burnInProtection", d.burnInProtection),
            dayMode = when {
                // Versions before the schedule stored a plain switch.
                j.isBoolean("dayMode") -> if (j.optBoolean("dayMode", false)) DayMode.ON else DayMode.OFF
                else -> enumOr(j.optString("dayMode"), d.dayMode)
            },
            dayModeStart = j.optInt("dayModeStart", d.dayModeStart).coerceIn(0, MINUTES_PER_DAY - 1),
            dayModeEnd = j.optInt("dayModeEnd", d.dayModeEnd).coerceIn(0, MINUTES_PER_DAY - 1),
            dayModeDays = j.optInt("dayModeDays", d.dayModeDays) and Alarm.ALL_DAYS,
            dayModeOverrideUntil = j.optLong("dayModeOverrideUntil", d.dayModeOverrideUntil),
            showCalendarEvents = j.optBoolean("showCalendarEvents", d.showCalendarEvents),
            dayCalendarView = enumOr(j.optString("dayCalendarView"), d.dayCalendarView),
            dayMaxBrightness = j.optDouble("dayMaxBrightness", d.dayMaxBrightness.toDouble()).toFloat()
                .let { if (it.isNaN()) d.dayMaxBrightness else it.coerceIn(Settings.DAY_BRIGHTNESS_RANGE) },
            dayPhotoVersion = j.optLong("dayPhotoVersion", d.dayPhotoVersion),
            meetingChimeMinutes = j.optInt("meetingChimeMinutes", d.meetingChimeMinutes)
                .let { if (it in Settings.MEETING_CHIME_MINUTES) it else d.meetingChimeMinutes },
            focusMinutes = j.optInt("focusMinutes", d.focusMinutes)
                .let { if (it in Settings.FOCUS_MINUTES) it else d.focusMinutes },
            focusBreakMinutes = j.optInt("focusBreakMinutes", d.focusBreakMinutes)
                .let { if (it in Settings.FOCUS_BREAK_MINUTES) it else d.focusBreakMinutes },
            focusLongBreakMinutes = j.optInt("focusLongBreakMinutes", d.focusLongBreakMinutes)
                .let { if (it in Settings.FOCUS_LONG_BREAK_MINUTES) it else d.focusLongBreakMinutes },
            worldZones = j.optArray("worldZones")?.let(::decodeWorldZones) ?: d.worldZones,
            dayHourlyWeather = j.optBoolean("dayHourlyWeather", d.dayHourlyWeather),
            dayRainAlert = j.optBoolean("dayRainAlert", d.dayRainAlert),
            daySunTimes = j.optBoolean("daySunTimes", d.daySunTimes),
            dayUvIndex = j.optBoolean("dayUvIndex", d.dayUvIndex),
            dayAirQuality = j.optBoolean("dayAirQuality", d.dayAirQuality),
            dayWeatherScene = j.optBoolean("dayWeatherScene", d.dayWeatherScene),
            dayHolidays = j.optBoolean("dayHolidays", d.dayHolidays),
            birthday = j.optInt("birthday", d.birthday).let { if (isMonthDay(it)) it else d.birthday },
            dayWeekNumbers = j.optBoolean("dayWeekNumbers", d.dayWeekNumbers),
            dayBirthdays = j.optBoolean("dayBirthdays", d.dayBirthdays),
            countdowns = j.optArray("countdowns")?.let(::decodeCountdowns) ?: d.countdowns,
            countdownDaysOff = j.optBoolean("countdownDaysOff", d.countdownDaysOff),
            dayTodos = j.optBoolean("dayTodos", d.dayTodos),
            todos = j.optArray("todos")?.let(::decodeTodos) ?: d.todos,
            stretchReminderMinutes = j.optInt("stretchReminderMinutes", d.stretchReminderMinutes)
                .let { if (it in Settings.REMINDER_MINUTES) it else d.stretchReminderMinutes },
            waterReminderMinutes = j.optInt("waterReminderMinutes", d.waterReminderMinutes)
                .let { if (it in Settings.REMINDER_MINUTES) it else d.waterReminderMinutes },
            batteryCarePercent = j.optInt("batteryCarePercent", d.batteryCarePercent)
                .let { if (it in Settings.BATTERY_CARE_PERCENT) it else d.batteryCarePercent },
            waveForFocus = j.optBoolean("waveForFocus", d.waveForFocus),
            autoStartOnCharge = j.optBoolean("autoStartOnCharge", d.autoStartOnCharge),
            dndWhileShowing = j.optBoolean("dndWhileShowing", d.dndWhileShowing),
            weatherUseDeviceLocation = j.optBoolean("weatherUseDeviceLocation", d.weatherUseDeviceLocation),
            weatherCity = j.optString("weatherCity", d.weatherCity),
            weatherLat = j.optDouble("weatherLat", Double.NaN),
            weatherLon = j.optDouble("weatherLon", Double.NaN),
            weatherFahrenheit = j.optBoolean("weatherFahrenheit", d.weatherFahrenheit),
            snoozeMinutes = j.optInt("snoozeMinutes", d.snoozeMinutes),
            alarmRampSeconds = j.optInt("alarmRampSeconds", d.alarmRampSeconds),
            alarmVibrate = j.optBoolean("alarmVibrate", d.alarmVibrate),
            waveToSnooze = j.optBoolean("waveToSnooze", d.waveToSnooze),
            alarmSums = j.optInt("alarmSums", d.alarmSums).coerceIn(SUM_COUNTS),
            sunriseMinutes = j.optInt("sunriseMinutes", d.sunriseMinutes).coerceIn(0, Settings.SUNRISE_CHOICES.max()),
            alarmShowWeather = j.optBoolean("alarmShowWeather", d.alarmShowWeather),
            dayOffKeywords = j.optString("dayOffKeywords", d.dayOffKeywords),
            bedtimeSleepHours = j.optDouble("bedtimeSleepHours", d.bedtimeSleepHours.toDouble()).toFloat()
                .let { if (it in Settings.BEDTIME_SLEEP_HOURS) it else d.bedtimeSleepHours },
            stations = stations,
            selectedStation = selected,
            selectedSound = j.optString("selectedSound", d.selectedSound),
            radioSleepMinutes = j.optInt("radioSleepMinutes", d.radioSleepMinutes)
                .let { if (it in Settings.RADIO_SLEEP_MINUTES) it else d.radioSleepMinutes },
            // Stars on stations no longer in the list are dropped.
            favoriteStations = j.optArray("favoriteStations")
                ?.mapNotNull { (it as? JsonPrimitive)?.contentOrNull?.takeIf { url -> stations.any { st -> st.url == url } } }
                ?.toSet() ?: d.favoriteStations,
            radioVolume = j.optInt("radioVolume", d.radioVolume).coerceIn(1, RADIO_VOLUME_STEPS),
        )
    }

    private const val MINUTES_PER_DAY = 24 * 60

    /** The zones that have both a label and a zone ID, at most [Settings.MAX_WORLD_ZONES]; damaged ones are skipped. */
    private fun decodeWorldZones(array: JsonArray): List<WorldZone> = array.mapNotNull { element ->
        val o = element as? JsonObject ?: return@mapNotNull null
        val label = o.optString("label").trim()
        val zone = o.optString("zone").trim()
        if (label.isEmpty() || zone.isEmpty()) null else WorldZone(label, zone)
    }.take(Settings.MAX_WORLD_ZONES)

    /** The countdowns with a label and a real date, at most [Settings.MAX_COUNTDOWNS]; damaged ones are skipped. */
    private fun decodeCountdowns(array: JsonArray): List<Countdown> = array.mapNotNull { element ->
        val o = element as? JsonObject ?: return@mapNotNull null
        val label = o.optString("label").trim()
        val date = o.optString("date").trim()
        if (label.isEmpty() || runCatching { LocalDate.parse(date) }.isFailure) null else Countdown(label, date)
    }.take(Settings.MAX_COUNTDOWNS)

    /** The to-dos with some text, at most [Settings.MAX_TODOS]; a damaged done date reads as not done. */
    private fun decodeTodos(array: JsonArray): List<TodoItem> = array.mapNotNull { element ->
        val o = element as? JsonObject ?: return@mapNotNull null
        val text = o.optString("text").trim().take(Settings.MAX_TODO_LENGTH)
        val doneOn = o.optString("doneOn").trim().takeIf { runCatching { LocalDate.parse(it) }.isSuccess }.orEmpty()
        if (text.isEmpty()) null else TodoItem(text, doneOn)
    }.take(Settings.MAX_TODOS)

    /** Whether [monthDay] (month * 100 + day) is a day of the year; 29 February counts. */
    private fun isMonthDay(monthDay: Int): Boolean {
        val month = monthDay / 100
        val day = monthDay % 100
        return month in 1..12 && day in 1..DAYS_IN_MONTH[month - 1]
    }

    private val DAYS_IN_MONTH = intArrayOf(31, 29, 31, 30, 31, 30, 31, 31, 30, 31, 30, 31)

    private inline fun <reified E : Enum<E>> enumOr(name: String, fallback: E): E =
        enumValues<E>().firstOrNull { it.name == name } ?: fallback
}
