package com.adl.nocturna.text

/**
 * The words that plain functions put together: summaries, countdowns, the focus timer, moon phases and so on.
 * Text written straight into a composable lives in `composeResources` instead and is read with
 * `stringResource`; text that a plain function builds comes from here, picked by [PlatformLocale], so those
 * functions stay plain Kotlin that `commonTest` checks without a Compose runtime or resource files, on
 * Android and iOS alike.
 *
 * One object per language: [EnglishStrings] and [SpanishStrings]. A language that has none reads English.
 */
interface Strings {
    // Lists and units

    /** "date, weather and battery": [items] joined as a sentence lists them. */
    fun andList(items: List<String>): String

    /** A length of time on the clock: "7 h 20 m", "7 h" or "45 m". */
    fun hoursMinutes(hours: Long, minutes: Long): String

    val off: String

    /** A number of minutes on a setting: "10 min". */
    fun minutesShort(minutes: Int): String

    // The clock's info line

    /** Before the time left to the next alarm: "in 7 h 20 m". */
    fun inTime(time: String): String

    /** The last minutes before the alarm: "in < 5 m". */
    fun inUnderMinutes(minutes: Long): String

    val moonNew: String
    val moonWaxingCrescent: String
    val moonFirstQuarter: String
    val moonWaxingGibbous: String
    val moonFull: String
    val moonWaningGibbous: String
    val moonLastQuarter: String
    val moonWaningCrescent: String

    // The alarm list

    val once: String
    val repeats: String

    /** A one-time alarm whose day has gone by: "Fri 21 Nov · passed". */
    fun datePassed(day: String): String

    /** Under an alarm's time: "Weekdays · skipping Mon 24 Nov · next Tue 25 Nov 07:00", [skipping] null when it isn't. */
    fun alarmDescription(repeat: String, skipping: String?, next: String): String

    /** The gentle wake-up setting: "off" or "30 s to full volume". */
    fun rampLabel(seconds: Int): String

    /** How many sums stop an alarm: "1 (easy)", "2", "3 (hard)". */
    fun sumsLabel(count: Int, easiest: Boolean, hardest: Boolean): String

    // Bedtime reminder

    /** "Bedtime: 8 h before your 6:30 am alarm". */
    fun bedtimeReminder(sleep: String, alarmTime: String): String

    // Dismiss challenge

    val challengeTap: String
    val challengeSum: String

    /** "Hold the button 3 s". */
    fun challengeHold(seconds: Long): String

    /** As TalkBack reads a sum: "37 plus 48". */
    fun sumSpoken(a: Int, b: Int): String

    // Day mode

    val goodMorning: String
    val goodAfternoon: String
    val goodEvening: String
    val calendarMonth: String
    val calendarWeek: String
    val calendarDay: String

    /** Over the event on the sticky note: "In 12 min · 10:00". */
    fun meetingIn(minutes: Int, time: String): String

    /** Under the clock: "Standup in 12 min". */
    fun meetingLine(title: String, minutes: Int): String

    val nextEvent: String
    val focus: String
    val focusBreak: String
    val focusLongBreak: String

    /** "Focus · 18:42 left", "Break paused · 3:10 left". */
    fun focusLabel(phase: String, paused: Boolean, left: String): String

    // Settings summaries

    val styleDigital: String
    val styleAnalog: String
    val hour24: String
    val hour12: String
    val seconds: String
    val fontThin: String
    val fontLight: String
    val fontRegular: String
    val fontCondensed: String
    val fontMono: String
    val fontSerif: String
    val infoDate: String
    val infoWeather: String
    val infoBattery: String
    val infoNextAlarm: String
    val infoMoon: String
    val infoNotifications: String
    val justTheTime: String
    val dimmingOff: String
    val followsRoomLight: String
    val phoneBrightness: String
    val nightMode: String
    val dayModeAlways: String
    val dayModeAlwaysOn: String
    val dayModeScheduled: String
    val noDays: String
    val everyDay: String
    val weekdays: String
    val weekends: String
    val yourLocation: String
    val noPlaceSet: String
    val startOnCharge: String
    val startYourself: String
    val noAlarmsYet: String

    /** "2 of 3 on". */
    fun alarmsOn(on: Int, total: Int): String

    /** The schedule's state: "Day mode now" or "Clock only now". */
    fun dayModeNow(active: Boolean): String

    /** "Day mode now. Pick the days day mode starts on." */
    fun pickScheduleDays(state: String): String

    /** "Day mode now, switched from the clock until Fri 18:00", with [until] null when it has no end. */
    fun switchedFromClock(state: String, until: String?): String

    /** "Day mode now, until Fri 18:00". */
    fun stateUntil(state: String, until: String): String

    // Sleep sounds

    val soundWhite: String
    val soundWhiteAbout: String
    val soundPink: String
    val soundPinkAbout: String
    val soundBrown: String
    val soundBrownAbout: String
    val soundRain: String
    val soundRainAbout: String
    val soundSurf: String
    val soundSurfAbout: String

    /** On the About page: "Version 0.1.0 (1)". */
    fun version(name: String, code: Int): String

    // Backups

    /** "3 alarms, 6 radio stations". */
    fun backupSummary(alarms: Int, stations: Int): String
    val backupNotABackup: String
    val backupNewerFormat: String
    val backupDamaged: String
}

/** The [Strings] for [locale]'s language. */
fun strings(locale: PlatformLocale): Strings = when (locale.languageCode) {
    "es" -> SpanishStrings
    else -> EnglishStrings
}

object EnglishStrings : Strings {
    override fun andList(items: List<String>) =
        if (items.size <= 1) items.joinToString() else items.dropLast(1).joinToString(", ") + " and " + items.last()

    override fun hoursMinutes(hours: Long, minutes: Long) = when {
        hours == 0L -> "$minutes m"
        minutes == 0L -> "$hours h"
        else -> "$hours h $minutes m"
    }

    override val off = "Off"
    override fun minutesShort(minutes: Int) = "$minutes min"

    override fun inTime(time: String) = "in $time"
    override fun inUnderMinutes(minutes: Long) = "in < $minutes m"

    override val moonNew = "New moon"
    override val moonWaxingCrescent = "Waxing crescent"
    override val moonFirstQuarter = "First quarter"
    override val moonWaxingGibbous = "Waxing gibbous"
    override val moonFull = "Full moon"
    override val moonWaningGibbous = "Waning gibbous"
    override val moonLastQuarter = "Last quarter"
    override val moonWaningCrescent = "Waning crescent"

    override val once = "Once"
    override val repeats = "Repeats"
    override fun datePassed(day: String) = "$day · passed"
    override fun alarmDescription(repeat: String, skipping: String?, next: String) =
        repeat + skipping?.let { " · skipping $it" }.orEmpty() + " · next $next"
    override fun rampLabel(seconds: Int) = if (seconds == 0) "off" else "$seconds s to full volume"
    override fun sumsLabel(count: Int, easiest: Boolean, hardest: Boolean) = when {
        easiest -> "$count (easy)"
        hardest -> "$count (hard)"
        else -> "$count"
    }

    override fun bedtimeReminder(sleep: String, alarmTime: String) = "Bedtime: $sleep before your $alarmTime alarm"

    override val challengeTap = "Just tap"
    override val challengeSum = "Solve a sum"
    override fun challengeHold(seconds: Long) = "Hold the button $seconds s"
    override fun sumSpoken(a: Int, b: Int) = "$a plus $b"

    override val goodMorning = "Good morning!"
    override val goodAfternoon = "Good afternoon!"
    override val goodEvening = "Good evening!"
    override val calendarMonth = "Month"
    override val calendarWeek = "Week"
    override val calendarDay = "Day"
    override fun meetingIn(minutes: Int, time: String) = "In $minutes min · $time"
    override fun meetingLine(title: String, minutes: Int) = "$title in $minutes min"
    override val nextEvent = "Next event"
    override val focus = "Focus"
    override val focusBreak = "Break"
    override val focusLongBreak = "Long break"
    override fun focusLabel(phase: String, paused: Boolean, left: String) =
        "$phase${if (paused) " paused" else ""} · $left left"

    override val styleDigital = "Digital"
    override val styleAnalog = "Analog"
    override val hour24 = "24-hour"
    override val hour12 = "12-hour"
    override val seconds = "seconds"
    override val fontThin = "Thin"
    override val fontLight = "Light"
    override val fontRegular = "Regular"
    override val fontCondensed = "Condensed"
    override val fontMono = "Mono"
    override val fontSerif = "Serif"
    override val infoDate = "date"
    override val infoWeather = "weather"
    override val infoBattery = "battery"
    override val infoNextAlarm = "next alarm"
    override val infoMoon = "moon"
    override val infoNotifications = "notifications"
    override val justTheTime = "Just the time"
    override val dimmingOff = "Dimming off"
    override val followsRoomLight = "Follows room light"
    override val phoneBrightness = "Phone brightness"
    override val nightMode = "night mode"
    override val dayModeAlways = "Always"
    override val dayModeAlwaysOn = "Always on"
    override val dayModeScheduled = "On a schedule"
    override val noDays = "No days"
    override val everyDay = "Every day"
    override val weekdays = "Weekdays"
    override val weekends = "Weekends"
    override val yourLocation = "Your location"
    override val noPlaceSet = "No place set"
    override val startOnCharge = "Opens when you plug in a locked phone"
    override val startYourself = "As the screen saver, or open it yourself"
    override val noAlarmsYet = "No alarms yet"
    override fun alarmsOn(on: Int, total: Int) = "$on of $total on"
    override fun dayModeNow(active: Boolean) = if (active) "Day mode now" else "Clock only now"
    override fun pickScheduleDays(state: String) = "$state. Pick the days day mode starts on."
    override fun switchedFromClock(state: String, until: String?) =
        if (until == null) "$state, switched from the clock" else "$state, switched from the clock until $until"
    override fun stateUntil(state: String, until: String) = "$state, until $until"

    override val soundWhite = "White noise"
    override val soundWhiteAbout = "An even hiss at every pitch"
    override val soundPink = "Pink noise"
    override val soundPinkAbout = "Softer and warmer, like a steady wind"
    override val soundBrown = "Brown noise"
    override val soundBrownAbout = "A deep rumble, like a distant waterfall"
    override val soundRain = "Rain"
    override val soundRainAbout = "Steady rain with drops pattering close by"
    override val soundSurf = "Surf"
    override val soundSurfAbout = "Slow waves rolling in to the shore"

    override fun version(name: String, code: Int) = "Version $name ($code)"

    override fun backupSummary(alarms: Int, stations: Int) =
        "${if (alarms == 1) "1 alarm" else "$alarms alarms"}, ${if (stations == 1) "1 radio station" else "$stations radio stations"}"
    override val backupNotABackup = "This file isn't a Nocturna backup."
    override val backupNewerFormat = "This backup was made by a newer version of Nocturna. Update the app, then import it again."
    override val backupDamaged = "This backup is damaged and can't be imported."
}
