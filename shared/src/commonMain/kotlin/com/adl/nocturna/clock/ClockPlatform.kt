package com.adl.nocturna.clock

import androidx.compose.runtime.Composable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.ImageBitmap
import com.adl.nocturna.data.Settings
import com.adl.nocturna.data.SettingsStore
import com.adl.nocturna.data.Station
import com.adl.nocturna.day.CalendarEvent
import com.adl.nocturna.day.ReminderKind
import com.adl.nocturna.day.reminderSoundOrder
import kotlinx.coroutines.flow.StateFlow
import kotlinx.datetime.LocalDate
import kotlinx.datetime.TimeZone
import kotlin.time.Clock

/**
 * What the clock and day mode need from the phone they run on. Each app provides one through
 * [LocalClockPlatform] around [ClockScreen].
 */
interface ClockPlatform {
    /** Smoothed ambient light in lux, or null when there is no light sensor. */
    @Composable
    fun ambientLux(): Float?

    /** The battery, or null until it's known. */
    @Composable
    fun battery(): BatteryInfo?

    /** When the next alarm set on the phone (by any app) goes off, in epoch millis, or null. */
    @Composable
    fun nextAlarm(): Long?

    /** Icons of the user's pending notifications, newest first; empty without access to them. */
    @Composable
    fun notificationIcons(): List<NotificationIcon>

    val radio: RadioControl

    /** The phone's location (latitude, longitude) for the weather, or null when it isn't known. */
    suspend fun deviceLocation(): Pair<Double, Double>?

    /**
     * Calendar events from [first] to [last], kept current as the calendars change; null when
     * [enabled] is off or calendar access isn't granted.
     */
    @Composable
    fun calendarEvents(enabled: Boolean, first: LocalDate, last: LocalDate, zone: TimeZone): List<CalendarEvent>?

    /** Whether the app may read the phone's calendars. */
    fun hasCalendarAccess(): Boolean

    /** The photo the user picked as day mode's background, or null when there is none or it can't be read. */
    suspend fun loadDayPhoto(): ImageBitmap?

    /** One short, quiet chime before a meeting, on the notification sound (never the alarm's). */
    fun playMeetingChime() {}

    /** The focus timer's chime as it switches between focus and a break: the meeting chime's soft tone. */
    fun playFocusChime() = playMeetingChime()

    /**
     * Day mode's reminders that just came due, once for those coming due together, in [reminderSoundOrder]:
     * footsteps to stand up and stretch, water poured into a glass to drink some, with the meeting chime's
     * rules. Where a platform has no such sounds, the meeting chime's soft tone.
     */
    fun playReminderSound(kinds: Set<ReminderKind>) = playMeetingChime()

    /**
     * A light accent from the phone's wallpaper palette (Material You) as ARGB, kept current as the
     * wallpaper changes; null where the phone has no such palette.
     */
    @Composable
    fun wallpaperColor(): Int? = null

    /** The proximity sensor at the top of the screen, for wave to peek and wave to snooze; null without one. */
    val proximity: Proximity? get() = null
}

val LocalClockPlatform = staticCompositionLocalOf<ClockPlatform> { error("No ClockPlatform provided") }

/** The battery's charge, 0 to 1, and whether it's charging. */
data class BatteryInfo(val level: Float, val charging: Boolean)

/** A pending notification's small icon; [image] draws it at a size in pixels, or is null when it can't. */
class NotificationIcon(val key: String, val image: (sizePx: Int) -> ImageBitmap?)

/** The radio, as the clock's buttons use it. */
interface RadioControl {
    /** True while a stream is playing or buffering. */
    val isOn: StateFlow<Boolean>
    val title: StateFlow<String?>
    val error: StateFlow<String?>

    /** The song the stream says is playing, already tidied by `cleanStreamTitle`; null when it doesn't say. */
    val song: StateFlow<String?>

    /** Epoch millis at which playback fades out and stops, or null. */
    val sleepAt: StateFlow<Long?>

    /** Plays [station], replacing whatever was playing; with [sleepMinutes] it fades out and stops after that long. */
    fun play(station: Station, sleepMinutes: Int = 0)

    fun stop()

    /** Stops the radio, or starts [station] when it's off, with a [sleepMinutes] sleep timer (0 for none). */
    fun toggle(station: Station?, sleepMinutes: Int = 0) {
        if (isOn.value) stop() else if (station != null) play(station, sleepMinutes)
    }

    /** Stops the radio in [minutes]; 0 turns the timer off. */
    fun setSleepTimer(minutes: Int)

    /**
     * Applies [change] to the settings (tuning to another station) and, when [start] or the radio is on,
     * plays the station it leaves selected, keeping the sleep timer's time left.
     */
    fun tune(start: Boolean = false, change: (Settings) -> Settings) {
        SettingsStore.update(change)
        val station = SettingsStore.current.currentStation ?: return
        if (!start && !isOn.value) return
        val left = sleepAt.value?.let { minutesLeft(it, Clock.System.now().toEpochMilliseconds()) } ?: 0
        play(station, if (isOn.value) left else SettingsStore.current.radioSleepMinutes)
    }
}

/** Whole minutes from [nowMillis] to [atMillis], rounded up, at least 1. */
fun minutesLeft(atMillis: Long, nowMillis: Long): Int = (((atMillis - nowMillis).coerceAtLeast(0) + 59_999) / 60_000).toInt().coerceAtLeast(1)
