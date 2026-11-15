package com.adl.nocturna.settings

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.adl.nocturna.clock.Moment
import com.adl.nocturna.clock.rememberNow
import com.adl.nocturna.data.CalendarView
import com.adl.nocturna.data.DayMode
import com.adl.nocturna.data.Settings
import com.adl.nocturna.data.SettingsStore
import com.adl.nocturna.day.label
import com.adl.nocturna.day.inDaySchedule
import com.adl.nocturna.day.isDayModeActive
import com.adl.nocturna.day.nextDayScheduleEdge
import com.adl.nocturna.text.DateTimePattern
import com.adl.nocturna.text.PlatformLocale
import com.adl.nocturna.text.bestDateTimePattern
import com.adl.nocturna.text.formatTime
import com.adl.nocturna.ui.currentLocale
import kotlinx.datetime.LocalTime
import kotlinx.datetime.toLocalDateTime
import kotlin.time.Clock
import kotlin.time.Instant
import kotlin.math.roundToInt

@Composable
fun DayModePage(padding: PaddingValues) {
    val platform = LocalSettingsPlatform.current
    val s by SettingsStore.settings.collectAsStateWithLifecycle()
    val access = platform.access()
    val locale = currentLocale()
    fun set(block: (Settings) -> Settings) = SettingsStore.update(block)

    var photoFailed by remember { mutableStateOf(false) }
    // The photo is copied in, so it lasts after the original is moved or deleted.
    val pickPhoto = platform.rememberPhotoPicker { saved ->
        photoFailed = !saved
        if (saved) set { it.copy(dayPhotoVersion = Clock.System.now().toEpochMilliseconds()) }
    }
    val askCalendar = platform.rememberPermissionRequest(Permission.CALENDAR) { granted ->
        set { it.copy(showCalendarEvents = granted) }
    }

    var editingTime by remember { mutableStateOf<DayScheduleTime?>(null) }
    editingTime?.let { which ->
        val minute = if (which == DayScheduleTime.START) s.dayModeStart else s.dayModeEnd
        TimeDialog(minute / 60, minute % 60, s.use24h, onDismiss = { editingTime = null }) { h, m ->
            editingTime = null
            // Changing the schedule drops a switch made from the clock, which was tied to the old one.
            set {
                if (which == DayScheduleTime.START) it.copy(dayModeStart = h * 60 + m, dayModeOverrideUntil = 0L)
                else it.copy(dayModeEnd = h * 60 + m, dayModeOverrideUntil = 0L)
            }
        }
    }

    SettingsPage(padding) {
        Hint(
            "The clock beside a calendar, for a desk stand. Also on the clock: tap it, then Day mode",
            Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
        )
        SettingsGroup {
            ChoiceRow("Show day mode", DayMode.entries, s.dayMode, { it.label() }) { v ->
                set { it.copy(dayMode = v, dayModeOverrideUntil = 0L) }
            }
        }
        if (s.dayMode == DayMode.SCHEDULED) {
            SettingsGroup("Schedule") {
                NavRow("From", formatTime(LocalTime(s.dayModeStart / 60, s.dayModeStart % 60), s.use24h, locale)) {
                    editingTime = DayScheduleTime.START
                }
                val endsNextDay = if (s.dayModeEnd <= s.dayModeStart) " the next day" else ""
                NavRow("Until", formatTime(LocalTime(s.dayModeEnd / 60, s.dayModeEnd % 60), s.use24h, locale) + endsNextDay) {
                    editingTime = DayScheduleTime.END
                }
                DayToggles(s.dayModeDays, locale, Modifier.padding(horizontal = 16.dp, vertical = 6.dp)) { days ->
                    set { it.copy(dayModeDays = days, dayModeOverrideUntil = 0L) }
                }
                // Only this line ticks every second, not the whole page.
                val now by rememberNow()
                Hint(dayScheduleStatus(s, now, locale), Modifier.padding(bottom = 6.dp))
            }
        }
        SettingsGroup("Calendar") {
            SwitchRow(
                "Today's events", s.showCalendarEvents,
                if (s.showCalendarEvents && !access.calendar) "Needs calendar access" else "From the calendars on this phone",
            ) { v ->
                if (v && !access.calendar) askCalendar()
                else set { it.copy(showCalendarEvents = v) }
            }
            if (s.showCalendarEvents && !access.calendar) {
                ActionButton("Allow calendar access") { askCalendar() }
            }
            ChoiceRow("Calendar view", CalendarView.entries, s.dayCalendarView, { it.label() }) { v ->
                set { it.copy(dayCalendarView = v) }
            }
            Hint("On the clock, swipe the calendar page to switch between month, week and day", Modifier.padding(bottom = 6.dp))
            if (s.showCalendarEvents) {
                ChoiceRow("Chime before meetings", Settings.MEETING_CHIME_MINUTES, s.meetingChimeMinutes, { if (it == 0) "Off" else "$it min" }) { v ->
                    set { it.copy(meetingChimeMinutes = v) }
                }
                Hint(
                    "The note counts down to meetings in the next hour. The chime is one soft tone at the notification " +
                        "volume, only while day mode is showing, and not on silent or Do Not Disturb",
                    Modifier.padding(bottom = 6.dp),
                )
            }
        }
        SettingsGroup("Focus timer") {
            ChoiceRow("Focus", Settings.FOCUS_MINUTES, s.focusMinutes, { "$it min" }) { v -> set { it.copy(focusMinutes = v) } }
            ChoiceRow("Short break", Settings.FOCUS_BREAK_MINUTES, s.focusBreakMinutes, { "$it min" }) { v ->
                set { it.copy(focusBreakMinutes = v) }
            }
            ChoiceRow("Long break", Settings.FOCUS_LONG_BREAK_MINUTES, s.focusLongBreakMinutes, { "$it min" }) { v ->
                set { it.copy(focusLongBreakMinutes = v) }
            }
            Hint(
                "Tap the clock and pick Focus to start. After every fourth focus block the break is a long one. " +
                    "A soft chime marks each switch, at the notification volume. Leaving day mode stops the timer",
                Modifier.padding(bottom = 6.dp),
            )
        }
        SettingsGroup("Screen care") {
            if (s.dimEnabled && s.autoBrightness) {
                SliderRow("Brightest", s.dayMaxBrightness, Settings.DAY_BRIGHTNESS_RANGE, { "${(it * 100).roundToInt()}%" }) { v ->
                    set { it.copy(dayMaxBrightness = v) }
                }
                Hint(
                    "Day mode follows the room's light up to this, never above the clock's own brightest. " +
                        "A dimmer screen wears much more slowly",
                    Modifier.padding(bottom = 6.dp),
                )
            } else {
                Hint(
                    "Day mode uses the phone's own brightness. To keep it lower, turn on Adjust to room light " +
                        "in Brightness and night mode",
                    Modifier.padding(vertical = 6.dp),
                )
            }
        }
        SettingsGroup("Background") {
            NavRow(
                if (s.dayPhotoVersion == 0L) "Monthly pictures" else "Your photo",
                if (s.dayPhotoVersion == 0L) "A cheerful picture for each month that follows the time of day. Tap to use your own photo"
                else "Tap to change it",
            ) { pickPhoto() }
            if (photoFailed) Hint("That photo couldn't be opened. Try another one")
            if (s.dayPhotoVersion != 0L) {
                ActionButton("Use the monthly pictures") {
                    platform.clearDayPhoto()
                    set { it.copy(dayPhotoVersion = 0L) }
                }
            }
        }
    }
}

private enum class DayScheduleTime { START, END }

private fun DayMode.label() = when (this) {
    DayMode.OFF -> "Off"
    DayMode.ON -> "Always"
    DayMode.SCHEDULED -> "On a schedule"
}

/** What the schedule is doing at [now] and when that changes, e.g. "Day mode now, until Fri 18:00". */
private fun dayScheduleStatus(s: Settings, now: Moment, locale: PlatformLocale): String {
    val active = isDayModeActive(s, now.instant, now.zone)
    val state = if (active) "Day mode now" else "Clock only now"
    if (s.dayModeDays == 0) return "$state. Pick the days day mode starts on."
    val overridden = active != inDaySchedule(s, now.instant, now.zone)
    val until = if (overridden) s.dayModeOverrideUntil.takeIf { it != Long.MAX_VALUE }?.let { Instant.fromEpochMilliseconds(it) }
    else nextDayScheduleEdge(s, now.instant, now.zone)
    val pattern = DateTimePattern(bestDateTimePattern(if (s.use24h) "EEEHHmm" else "EEEhmma", locale), locale)
    val whenText = until?.let { pattern.format(it.toLocalDateTime(now.zone)) }
    return when {
        overridden && whenText != null -> "$state, switched from the clock until $whenText"
        overridden -> "$state, switched from the clock"
        whenText != null -> "$state, until $whenText"
        else -> state
    }
}
