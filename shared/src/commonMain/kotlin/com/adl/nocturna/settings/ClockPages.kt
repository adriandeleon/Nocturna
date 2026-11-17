package com.adl.nocturna.settings

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.adl.nocturna.clock.AnalogClock
import com.adl.nocturna.clock.DigitalClock
import com.adl.nocturna.clock.LocalClockPlatform
import com.adl.nocturna.clock.clockColor
import com.adl.nocturna.clock.label
import com.adl.nocturna.clock.nightBatterySummary
import com.adl.nocturna.clock.rememberNow
import com.adl.nocturna.clock.selectedSwatch
import com.adl.nocturna.data.ClockFont
import com.adl.nocturna.data.ClockStyle
import com.adl.nocturna.data.Settings
import com.adl.nocturna.data.NightBatteryStore
import com.adl.nocturna.data.SettingsStore
import com.adl.nocturna.resources.*
import com.adl.nocturna.text.DateTimePattern
import com.adl.nocturna.text.PlatformLocale
import com.adl.nocturna.text.bestDateTimePattern
import com.adl.nocturna.text.formatNumber
import com.adl.nocturna.text.strings
import com.adl.nocturna.ui.currentLocale
import kotlin.math.roundToInt
import kotlinx.datetime.LocalDate
import kotlinx.datetime.atTime
import org.jetbrains.compose.resources.stringResource

private fun set(block: (Settings) -> Settings) = SettingsStore.update(block)

@Composable
fun ClockFacePage(padding: PaddingValues) {
    val s by SettingsStore.settings.collectAsStateWithLifecycle()
    val locale = currentLocale()
    SettingsPage(padding) {
        ClockPreview(s)
        SettingsGroup(stringResource(Res.string.clock_look)) {
            ChoiceRow(stringResource(Res.string.clock_style), ClockStyle.entries, s.style, { if (it == ClockStyle.DIGITAL) strings(locale).styleDigital else strings(locale).styleAnalog }) { v ->
                set { it.copy(style = v) }
            }
            if (s.style == ClockStyle.DIGITAL) {
                ChoiceRow(stringResource(Res.string.clock_font), ClockFont.entries, s.font, { it.label(locale) }) { v -> set { it.copy(font = v) } }
            }
        }
        SettingsGroup(stringResource(Res.string.clock_colour)) {
            val wallpaper = LocalClockPlatform.current.wallpaperColor()
            ColorRow(
                Settings.COLORS, selectedSwatch(s, wallpaper),
                wallpaper = wallpaper, wallpaperSelected = s.colorFromWallpaper && wallpaper != null,
                onSelectWallpaper = { set { it.copy(colorFromWallpaper = true) } },
            ) { v -> set { it.copy(color = v, colorFromWallpaper = false) } }
        }
        SettingsGroup(stringResource(Res.string.clock_time)) {
            SwitchRow(stringResource(Res.string.clock_24h), s.use24h) { v -> set { it.copy(use24h = v) } }
            SwitchRow(stringResource(Res.string.clock_show_seconds), s.showSeconds) { v -> set { it.copy(showSeconds = v) } }
        }
    }
}

/** The clock as it will look, on black like the real thing, so changes show as you make them. */
@Composable
private fun ClockPreview(s: Settings) {
    val now by rememberNow()
    Box(
        Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp)
            .padding(top = 12.dp)
            .height(170.dp)
            .clip(RoundedCornerShape(20.dp))
            .background(Color.Black),
        contentAlignment = Alignment.Center,
    ) {
        val color = Color(clockColor(s, LocalClockPlatform.current.wallpaperColor()))
        if (s.style == ClockStyle.DIGITAL) DigitalClock(now, s.use24h, s.showSeconds, s.font, color, 76.sp)
        else AnalogClock(now, s.showSeconds, color, 140.dp)
    }
}

@Composable
fun InfoLinePage(padding: PaddingValues, onNavigate: (String) -> Unit) {
    val platform = LocalSettingsPlatform.current
    val locale = currentLocale()
    val s by SettingsStore.settings.collectAsStateWithLifecycle()
    val access = platform.access()
    SettingsPage(padding) {
        Hint(stringResource(Res.string.info_hint), Modifier.padding(horizontal = 16.dp, vertical = 8.dp))
        SettingsGroup {
            SwitchRow(stringResource(Res.string.info_date), s.showDate) { v -> set { it.copy(showDate = v) } }
            SwitchRow(stringResource(Res.string.info_battery), s.showBattery) { v -> set { it.copy(showBattery = v) } }
            SwitchRow(stringResource(Res.string.info_next_alarm), s.showNextAlarm, stringResource(Res.string.info_next_alarm_detail)) { v -> set { it.copy(showNextAlarm = v) } }
            if (s.showNextAlarm) {
                SwitchRow(stringResource(Res.string.info_time_to_alarm), s.showTimeToAlarm, stringResource(Res.string.info_time_to_alarm_detail)) { v ->
                    set { it.copy(showTimeToAlarm = v) }
                }
            }
            SwitchRow(stringResource(Res.string.info_moon), s.showMoon, stringResource(Res.string.info_moon_detail)) { v ->
                set { it.copy(showMoon = v) }
            }
        }
        SettingsGroup(stringResource(Res.string.settings_weather)) {
            SwitchRow(stringResource(Res.string.settings_weather), s.showWeather) { v -> set { it.copy(showWeather = v) } }
            if (s.showWeather) {
                SwitchRow(stringResource(Res.string.info_forecast), s.showForecast, stringResource(Res.string.info_forecast_detail)) { v ->
                    set { it.copy(showForecast = v) }
                }
                NavRow(stringResource(Res.string.info_place_units), weatherSummary(s, locale)) { onNavigate(SettingsPages.WEATHER) }
            }
        }
        SettingsGroup(stringResource(Res.string.info_notifications)) {
            SwitchRow(
                stringResource(Res.string.info_notification_icons), s.showNotifications,
                if (access.notificationAccess) stringResource(Res.string.info_notification_icons_detail) else stringResource(Res.string.info_needs_notification_access),
            ) { v -> set { it.copy(showNotifications = v) } }
            if (s.showNotifications && !access.notificationAccess) {
                ActionButton(stringResource(Res.string.info_grant_notification_access)) { platform.open(SystemPage.NOTIFICATION_ACCESS) }
            }
        }
    }
}

@Composable
fun BrightnessPage(padding: PaddingValues) {
    val s by SettingsStore.settings.collectAsStateWithLifecycle()
    val lux = LocalClockPlatform.current.ambientLux()
    val locale = currentLocale()
    SettingsPage(padding) {
        Hint(
            lux?.let { stringResource(Res.string.brightness_light_sensor, formatNumber(it.toDouble(), 1, locale)) }
                ?: stringResource(Res.string.brightness_no_sensor),
            Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
        )
        SettingsGroup {
            SwitchRow(stringResource(Res.string.brightness_dimming), s.dimEnabled, stringResource(Res.string.brightness_dimming_detail)) { v ->
                set { it.copy(dimEnabled = v) }
            }
        }
        if (s.dimEnabled) {
            SettingsGroup(stringResource(Res.string.brightness_title)) {
                SwitchRow(
                    stringResource(Res.string.brightness_room_light), s.autoBrightness,
                    if (s.autoBrightness) null else stringResource(Res.string.brightness_room_light_off),
                ) { v -> set { it.copy(autoBrightness = v) } }
                if (s.autoBrightness) {
                SliderRow(stringResource(Res.string.brightness_darkest), s.minBrightness, 0.01f..0.3f, { "${(it * 100).roundToInt()}%" }) { v ->
                    set { it.copy(minBrightness = v) }
                }
                SliderRow(stringResource(Res.string.brightness_brightest), s.maxBrightness, 0.3f..1f, { "${(it * 100).roundToInt()}%" }) { v ->
                    set { it.copy(maxBrightness = v) }
                }
                }
                // Night mode dims the clock even without the room light, so this shows either way.
                SwitchRow(
                    stringResource(Res.string.brightness_tap_to_peek), s.tapToPeek,
                    stringResource(Res.string.brightness_tap_to_peek_detail),
                ) { v -> set { it.copy(tapToPeek = v) } }
                if (s.tapToPeek && LocalClockPlatform.current.proximity != null) {
                    SwitchRow(
                        stringResource(Res.string.brightness_wave_to_peek), s.waveToPeek,
                        stringResource(Res.string.brightness_wave_to_peek_detail),
                    ) { v -> set { it.copy(waveToPeek = v) } }
                }
            }
            SettingsGroup(stringResource(Res.string.brightness_night_mode)) {
                SwitchRow(stringResource(Res.string.brightness_night_mode), s.nightModeEnabled, stringResource(Res.string.brightness_night_mode_detail)) { v ->
                    set { it.copy(nightModeEnabled = v) }
                }
                if (s.nightModeEnabled) {
                    SliderRow(stringResource(Res.string.brightness_turn_on_below), s.nightModeLux, 0.5f..30f, { "${formatNumber(it.toDouble(), 1, locale)} lux" }) { v ->
                        set { it.copy(nightModeLux = v) }
                    }
                    val nights by NightBatteryStore.uses.collectAsStateWithLifecycle()
                    TextRow(
                        stringResource(Res.string.brightness_night_battery),
                        nightBatterySummary(nights, locale) { dayLabel(it, locale) } ?: stringResource(Res.string.brightness_night_battery_none),
                    )
                }
            }
        }
        SettingsGroup(stringResource(Res.string.brightness_screen_care)) {
            SwitchRow(stringResource(Res.string.brightness_burn_in), s.burnInProtection, stringResource(Res.string.brightness_burn_in_detail)) { v ->
                set { it.copy(burnInProtection = v) }
            }
        }
    }
}

/** A night's date in the battery log: "Sun 15 Nov". */
private fun dayLabel(date: LocalDate, locale: PlatformLocale): String =
    DateTimePattern(bestDateTimePattern("EEEdMMM", locale), locale).format(date.atTime(0, 0))
