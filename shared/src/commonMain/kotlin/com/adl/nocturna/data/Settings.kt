package com.adl.nocturna.data

import com.adl.nocturna.alarm.DEFAULT_DAY_OFF_KEYWORDS
import com.adl.nocturna.alarm.SUM_COUNTS
import com.adl.nocturna.sound.SleepSound

enum class ClockStyle { DIGITAL, ANALOG }

enum class ClockFont { THIN, LIGHT, REGULAR, CONDENSED, MONO, SERIF }

/** When day mode shows: never, always, or inside the schedule in [Settings]. */
enum class DayMode { OFF, ON, SCHEDULED }

/** What day mode's calendar page shows; swiping the page moves between them. */
enum class CalendarView { MONTH, WEEK, DAY }

data class Station(val name: String, val url: String)

/** Another time zone on day mode's desk clock: a short city [label] and an IANA [zoneId], e.g. "Europe/London". */
data class WorldZone(val label: String, val zoneId: String)

/**
 * A line of the to-do list on day mode's sticky note. [doneOn] is the day it was ticked off, as
 * ISO "2026-11-16", or blank while it is still to do: a done item stays crossed off for the rest of that
 * day and is dropped the next (see `todosForDay`).
 */
data class TodoItem(val text: String, val doneOn: String = "") {
    val done: Boolean get() = doneOn.isNotEmpty()
}

/** A day day mode counts down to, e.g. "Vacation" on 2026-12-20, with [date] as ISO "2026-12-20". */
data class Countdown(val label: String, val date: String)

data class Settings(
    // Clock
    val style: ClockStyle = ClockStyle.DIGITAL,
    val use24h: Boolean = true,
    val showSeconds: Boolean = false,
    val font: ClockFont = ClockFont.THIN,
    val color: Int = 0xFF8FD3FF.toInt(),
    /** Material You: the clock takes a light accent from the wallpaper instead of [color], where the phone has one. */
    val colorFromWallpaper: Boolean = false,
    // Info line
    val showDate: Boolean = true,
    val showBattery: Boolean = true,
    val showNextAlarm: Boolean = true,
    /** "in 7 h 20 m" after the next alarm's time when it is within 12 hours. */
    val showTimeToAlarm: Boolean = true,
    /** The moon's phase on the night clock, e.g. "Waxing gibbous" with a little drawing of it. */
    val showMoon: Boolean = false,
    val showWeather: Boolean = true,
    /** Today's high and low next to the weather, or tomorrow's in the evening. */
    val showForecast: Boolean = true,
    val showNotifications: Boolean = true,
    // Brightness and night mode
    /** Master switch for all dimming: room-light brightness and night mode. */
    val dimEnabled: Boolean = true,
    val autoBrightness: Boolean = true,
    val minBrightness: Float = 0.02f,
    val maxBrightness: Float = 0.8f,
    val nightModeEnabled: Boolean = true,
    /** Ambient light (lux) below which the clock switches to the dim red night look. */
    val nightModeLux: Float = 4f,
    /** A tap on the dim clock brightens it for a few seconds before a second tap shows the controls. */
    val tapToPeek: Boolean = true,
    /** With [tapToPeek], a wave of the hand over the top of the phone peeks too; needs a proximity sensor. */
    val waveToPeek: Boolean = false,
    val burnInProtection: Boolean = true,
    // Day mode
    /** Clock beside a month calendar, for a desk during the day; never turns night red. */
    val dayMode: DayMode = DayMode.OFF,
    /** Scheduled day mode starts at this minute of the day (0..1439) on [dayModeDays]. */
    val dayModeStart: Int = 9 * 60,
    /** Scheduled day mode ends at this minute of the day; at or before [dayModeStart], the next day. */
    val dayModeEnd: Int = 18 * 60,
    /** Days the schedule starts on: bit 0 = Monday ... bit 6 = Sunday, as in [Alarm.days]. */
    val dayModeDays: Int = WEEKDAYS,
    /**
     * Epoch millis until which the clock's Day mode button overrides the schedule, flipping it; 0 for none.
     * It is set to the schedule's next edge, so the schedule takes over again on its own.
     */
    val dayModeOverrideUntil: Long = 0L,
    /** Today's events from the phone's calendars in day mode; needs READ_CALENDAR. */
    val showCalendarEvents: Boolean = false,
    /**
     * Day mode's brightest, when the brightness follows the room: lower than [maxBrightness], since
     * day mode stays up all day and a bright OLED screen wears fastest.
     */
    val dayMaxBrightness: Float = 0.5f,
    /** The calendar page's view when day mode starts. */
    val dayCalendarView: CalendarView = CalendarView.MONTH,
    /** When the day mode photo was picked (epoch millis); 0 means no photo, use the illustration. */
    val dayPhotoVersion: Long = 0L,
    /** Minutes before a meeting that day mode plays a soft chime: one of [MEETING_CHIME_MINUTES]; 0 is off. */
    val meetingChimeMinutes: Int = 0,
    /** Day mode's focus timer: minutes in a focus block, one of [FOCUS_MINUTES]. */
    val focusMinutes: Int = 25,
    /** Minutes in the focus timer's short break, one of [FOCUS_BREAK_MINUTES]. */
    val focusBreakMinutes: Int = 5,
    /** Minutes in the long break after every fourth focus block, one of [FOCUS_LONG_BREAK_MINUTES]. */
    val focusLongBreakMinutes: Int = 15,
    /** Up to [MAX_WORLD_ZONES] other time zones whose time day mode's desk clock shows under its own. */
    val worldZones: List<WorldZone> = emptyList(),
    /** The next few hours' weather under day mode's weather line, when the weather shows. */
    val dayHourlyWeather: Boolean = true,
    /** "Rain from 15:00" under day mode's desk clock when the next hours' forecast has rain. */
    val dayRainAlert: Boolean = false,
    /** "Sunset 17:48 · 2 h 10 m of light" under day mode's desk clock. */
    val daySunTimes: Boolean = false,
    /** The UV index under day mode's desk clock while the sun is up, e.g. "UV 7 · High". */
    val dayUvIndex: Boolean = false,
    /** The air quality under day mode's desk clock, e.g. "Air: good (42)"; one more Open-Meteo request. */
    val dayAirQuality: Boolean = false,
    /** Day mode's illustrated scene shows the weather: clouds, rain, snow, fog or a storm. */
    val dayWeatherScene: Boolean = true,
    /** Holiday touches in day mode's scene and greeting: confetti on New Year's Day, papel picado, lights. */
    val dayHolidays: Boolean = true,
    /** The user's birthday as month * 100 + day (1225 for 25 December), for confetti in day mode; 0 for none. */
    val birthday: Int = 0,
    /** Week numbers down the side of the calendar page's month view. */
    val dayWeekNumbers: Boolean = false,
    /** A cake on birthdays on the calendar page and the sticky note, from events titled as birthdays. */
    val dayBirthdays: Boolean = false,
    /** Up to [MAX_COUNTDOWNS] days the desk clock counts down to, e.g. "Vacation in 12 days". */
    val countdowns: List<Countdown> = emptyList(),
    /** Also count down to the next day off on the calendar (see [dayOffKeywords]). */
    val countdownDaysOff: Boolean = false,
    /** A to-do list on day mode's sticky note. */
    val dayTodos: Boolean = false,
    /** The to-do list's items, at most [MAX_TODOS]. */
    val todos: List<TodoItem> = emptyList(),
    /** Minutes between day mode's reminders to stand up and stretch: one of [REMINDER_MINUTES]; 0 is off. */
    val stretchReminderMinutes: Int = 0,
    /** Minutes between day mode's reminders to drink some water: one of [REMINDER_MINUTES]; 0 is off. */
    val waterReminderMinutes: Int = 0,
    /** Charge (%) at which day mode says the phone can come off the charger: one of [BATTERY_CARE_PERCENT]; 0 is off. */
    val batteryCarePercent: Int = 0,
    /** A wave over the top of the phone starts, pauses and resumes day mode's focus timer; needs a proximity sensor. */
    val waveForFocus: Boolean = false,
    // Launching
    val autoStartOnCharge: Boolean = false,
    /** Priority-only Do Not Disturb while the clock shows at night; needs Do Not Disturb access. */
    val dndWhileShowing: Boolean = false,
    // Weather
    val weatherUseDeviceLocation: Boolean = false,
    val weatherCity: String = "",
    val weatherLat: Double = Double.NaN,
    val weatherLon: Double = Double.NaN,
    val weatherFahrenheit: Boolean = false,
    // Alarms
    val snoozeMinutes: Int = 9,
    val alarmRampSeconds: Int = 45,
    val alarmVibrate: Boolean = true,
    /** A wave of the hand over the top of the phone snoozes a ringing alarm; needs a proximity sensor. */
    val waveToSnooze: Boolean = false,
    /** How many sums an alarm set to *Solve a sum* asks for, in [SUM_COUNTS]: the difficulty. */
    val alarmSums: Int = 1,
    /** Minutes before an alarm over which the clock brightens and warms up; 0 turns it off. */
    val sunriseMinutes: Int = 20,
    /** Today's weather under the time on the wake-up screen. */
    val alarmShowWeather: Boolean = true,
    /** Comma-separated words that make an all-day calendar event a day off, for alarms that skip them. */
    val dayOffKeywords: String = DEFAULT_DAY_OFF_KEYWORDS,
    /** Hours of sleep the bedtime reminder leaves before a morning alarm: one of [BEDTIME_SLEEP_HOURS]; 0 is off. */
    val bedtimeSleepHours: Float = 0f,
    // Radio
    val stations: List<Station> = DEFAULT_STATIONS,
    val selectedStation: Int = 0,
    /** Id of the [SleepSound] the Radio button plays instead of [selectedStation]; blank for a web station. */
    val selectedSound: String = "",
    /**
     * Minutes the radio or sleep sound plays before it fades out and stops, when started from the clock's Radio
     * button, the *Sleep radio* tile or the widget: one of [RADIO_SLEEP_MINUTES]; 0 plays until stopped.
     */
    val radioSleepMinutes: Int = 30,
) {
    val hasWeatherLocation: Boolean get() = !weatherLat.isNaN() && !weatherLon.isNaN()
    val currentSleepSound: SleepSound? get() = SleepSound.byId(selectedSound)
    val currentStation: Station?
        get() = currentSleepSound?.station ?: stations.getOrNull(selectedStation) ?: stations.firstOrNull()

    companion object {
        /** Stations added in later versions; merged once into lists saved by older versions. */
        val MONTERREY_STATIONS = listOf(
            Station("Classic 106.9", "https://mdstrm.com/audio/673788019b826f0dc1842750/icecast.audio"),
            Station("D99", "https://mdstrm.com/audio/662800a9c30cbb3e2e61f44b/icecast.audio"),
            Station("Imagen Monterrey", "https://monterrey.radio.wnsol.com/;stream.mp3"),
        )
        const val STATIONS_VERSION = 2
        const val WEEKDAYS = 0b001_1111
        /** What [dayMaxBrightness] may be set to. */
        val DAY_BRIGHTNESS_RANGE = 0.2f..1f
        /** What [sunriseMinutes] may be set to; 0 is off. */
        val SUNRISE_CHOICES = listOf(0, 10, 20, 30)
        /** What [meetingChimeMinutes] may be set to; 0 is off. */
        val MEETING_CHIME_MINUTES = listOf(0, 1, 2, 5)
        /** What [radioSleepMinutes] may be set to; 0 is off. */
        val RADIO_SLEEP_MINUTES = listOf(0, 15, 30, 45, 60, 90, 120)
        /** What [bedtimeSleepHours] may be set to; 0 is off. */
        val BEDTIME_SLEEP_HOURS = listOf(0f, 6f, 6.5f, 7f, 7.5f, 8f, 8.5f, 9f)
        /** What [focusMinutes], [focusBreakMinutes] and [focusLongBreakMinutes] may be set to. */
        val FOCUS_MINUTES = listOf(15, 20, 25, 30, 45, 50, 60, 90)
        val FOCUS_BREAK_MINUTES = listOf(3, 5, 10, 15)
        val FOCUS_LONG_BREAK_MINUTES = listOf(10, 15, 20, 30)
        /** How many [worldZones] the desk clock has room for. */
        const val MAX_WORLD_ZONES = 2
        /** What [stretchReminderMinutes] and [waterReminderMinutes] may be set to; 0 is off. */
        val REMINDER_MINUTES = listOf(0, 30, 45, 60, 90, 120)
        /** What [batteryCarePercent] may be set to; 0 is off. */
        val BATTERY_CARE_PERCENT = listOf(0, 80, 85, 90)
        /** How many [todos] the list keeps. */
        const val MAX_TODOS = 8
        /** The longest a to-do may be, in characters. */
        const val MAX_TODO_LENGTH = 80
        /** How many [countdowns] the desk clock keeps. */
        const val MAX_COUNTDOWNS = 3

        val DEFAULT_STATIONS = MONTERREY_STATIONS + listOf(
            Station("SomaFM Groove Salad", "https://ice1.somafm.com/groovesalad-128-mp3"),
            Station("SomaFM Drone Zone", "https://ice1.somafm.com/dronezone-128-mp3"),
            Station("SomaFM Deep Space One", "https://ice1.somafm.com/deepspaceone-128-mp3"),
        )
        val COLORS = listOf(
            0xFF8FD3FF, 0xFFFFFFFF, 0xFFFFD27A, 0xFFFF8A65,
            0xFFFF5252, 0xFFB388FF, 0xFF69F0AE, 0xFF80CBC4,
        ).map { it.toInt() }
    }
}
