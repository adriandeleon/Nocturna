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
import com.adl.nocturna.day.inDaySchedule
import com.adl.nocturna.day.isDayModeActive
import com.adl.nocturna.day.label
import com.adl.nocturna.day.nextDayScheduleEdge
import com.adl.nocturna.resources.*
import com.adl.nocturna.text.DateTimePattern
import com.adl.nocturna.text.PlatformLocale
import com.adl.nocturna.text.bestDateTimePattern
import com.adl.nocturna.text.formatTime
import com.adl.nocturna.text.strings
import com.adl.nocturna.ui.currentLocale
import kotlin.math.roundToInt
import kotlin.time.Clock
import kotlin.time.Instant
import kotlinx.datetime.LocalTime
import kotlinx.datetime.toLocalDateTime
import org.jetbrains.compose.resources.stringResource

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
            stringResource(Res.string.daypage_hint),
            Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
        )
        SettingsGroup {
            ChoiceRow(stringResource(Res.string.daypage_show), DayMode.entries, s.dayMode, { it.label(locale) }) { v ->
                set { it.copy(dayMode = v, dayModeOverrideUntil = 0L) }
            }
        }
        if (s.dayMode == DayMode.SCHEDULED) {
            SettingsGroup(stringResource(Res.string.daypage_schedule)) {
                NavRow(stringResource(Res.string.daypage_from), formatTime(LocalTime(s.dayModeStart / 60, s.dayModeStart % 60), s.use24h, locale)) {
                    editingTime = DayScheduleTime.START
                }
                val until = formatTime(LocalTime(s.dayModeEnd / 60, s.dayModeEnd % 60), s.use24h, locale)
                NavRow(
                    stringResource(Res.string.daypage_until),
                    if (s.dayModeEnd <= s.dayModeStart) stringResource(Res.string.daypage_until_next_day, until) else until,
                ) {
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
        SettingsGroup(stringResource(Res.string.daypage_calendar)) {
            SwitchRow(
                stringResource(Res.string.daypage_events), s.showCalendarEvents,
                if (s.showCalendarEvents && !access.calendar) stringResource(Res.string.daypage_needs_calendar) else stringResource(Res.string.daypage_events_detail),
            ) { v ->
                if (v && !access.calendar) askCalendar()
                else set { it.copy(showCalendarEvents = v) }
            }
            if (s.showCalendarEvents && !access.calendar) {
                ActionButton(stringResource(Res.string.daypage_allow_calendar)) { askCalendar() }
            }
            ChoiceRow(stringResource(Res.string.daypage_calendar_view), CalendarView.entries, s.dayCalendarView, { it.label(locale) }) { v ->
                set { it.copy(dayCalendarView = v) }
            }
            Hint(stringResource(Res.string.daypage_calendar_swipe), Modifier.padding(bottom = 6.dp))
            if (s.showCalendarEvents) {
                ChoiceRow(stringResource(Res.string.daypage_chime), Settings.MEETING_CHIME_MINUTES, s.meetingChimeMinutes, { if (it == 0) strings(locale).off else strings(locale).minutesShort(it) }) { v ->
                    set { it.copy(meetingChimeMinutes = v) }
                }
                Hint(
                    stringResource(Res.string.daypage_chime_detail),
                    Modifier.padding(bottom = 6.dp),
                )
            }
        }
        // The strip needs the weather on and a place to fetch it for.
        if (s.showWeather && (s.weatherUseDeviceLocation || s.hasWeatherLocation)) {
            SettingsGroup(stringResource(Res.string.settings_weather)) {
                SwitchRow(stringResource(Res.string.daypage_next_hours), s.dayHourlyWeather, stringResource(Res.string.daypage_next_hours_detail)) { v ->
                    set { it.copy(dayHourlyWeather = v) }
                }
            }
        }
        SettingsGroup(stringResource(Res.string.daypage_focus_timer)) {
            ChoiceRow(stringResource(Res.string.daypage_focus), Settings.FOCUS_MINUTES, s.focusMinutes, { strings(locale).minutesShort(it) }) { v -> set { it.copy(focusMinutes = v) } }
            ChoiceRow(stringResource(Res.string.daypage_short_break), Settings.FOCUS_BREAK_MINUTES, s.focusBreakMinutes, { strings(locale).minutesShort(it) }) { v ->
                set { it.copy(focusBreakMinutes = v) }
            }
            ChoiceRow(stringResource(Res.string.daypage_long_break), Settings.FOCUS_LONG_BREAK_MINUTES, s.focusLongBreakMinutes, { strings(locale).minutesShort(it) }) { v ->
                set { it.copy(focusLongBreakMinutes = v) }
            }
            Hint(
                stringResource(Res.string.daypage_focus_detail),
                Modifier.padding(bottom = 6.dp),
            )
        }
        WorldZonesGroup(s.worldZones)
        SettingsGroup(stringResource(Res.string.brightness_screen_care)) {
            if (s.dimEnabled && s.autoBrightness) {
                SliderRow(stringResource(Res.string.brightness_brightest), s.dayMaxBrightness, Settings.DAY_BRIGHTNESS_RANGE, { "${(it * 100).roundToInt()}%" }) { v ->
                    set { it.copy(dayMaxBrightness = v) }
                }
                Hint(
                    stringResource(Res.string.daypage_brightest_detail),
                    Modifier.padding(bottom = 6.dp),
                )
            } else {
                Hint(
                    stringResource(Res.string.daypage_phone_brightness),
                    Modifier.padding(vertical = 6.dp),
                )
            }
        }
        SettingsGroup(stringResource(Res.string.daypage_background)) {
            NavRow(
                if (s.dayPhotoVersion == 0L) stringResource(Res.string.daypage_monthly_pictures) else stringResource(Res.string.daypage_your_photo),
                if (s.dayPhotoVersion == 0L) stringResource(Res.string.daypage_monthly_detail)
                else stringResource(Res.string.daypage_change_photo),
            ) { pickPhoto() }
            if (photoFailed) Hint(stringResource(Res.string.daypage_photo_failed))
            if (s.dayPhotoVersion != 0L) {
                ActionButton(stringResource(Res.string.daypage_use_monthly)) {
                    platform.clearDayPhoto()
                    set { it.copy(dayPhotoVersion = 0L) }
                }
            }
        }
    }
}

private enum class DayScheduleTime { START, END }

private fun DayMode.label(locale: PlatformLocale) = when (this) {
    DayMode.OFF -> strings(locale).off
    DayMode.ON -> strings(locale).dayModeAlways
    DayMode.SCHEDULED -> strings(locale).dayModeScheduled
}

/** What the schedule is doing at [now] and when that changes, e.g. "Day mode now, until Fri 18:00". */
private fun dayScheduleStatus(s: Settings, now: Moment, locale: PlatformLocale): String {
    val active = isDayModeActive(s, now.instant, now.zone)
    val t = strings(locale)
    val state = t.dayModeNow(active)
    if (s.dayModeDays == 0) return t.pickScheduleDays(state)
    val overridden = active != inDaySchedule(s, now.instant, now.zone)
    val until = if (overridden) s.dayModeOverrideUntil.takeIf { it != Long.MAX_VALUE }?.let { Instant.fromEpochMilliseconds(it) }
    else nextDayScheduleEdge(s, now.instant, now.zone)
    val pattern = DateTimePattern(bestDateTimePattern(if (s.use24h) "EEEHHmm" else "EEEhmma", locale), locale)
    val whenText = until?.let { pattern.format(it.toLocalDateTime(now.zone)) }
    return when {
        overridden -> t.switchedFromClock(state, whenText)
        whenText != null -> t.stateUntil(state, whenText)
        else -> state
    }
}
