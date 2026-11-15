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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.adl.nocturna.clock.AnalogClock
import com.adl.nocturna.clock.DigitalClock
import com.adl.nocturna.clock.clockColor
import com.adl.nocturna.clock.label
import com.adl.nocturna.clock.rememberAmbientLux
import com.adl.nocturna.clock.rememberNow
import com.adl.nocturna.clock.rememberWallpaperColor
import com.adl.nocturna.clock.selectedSwatch
import com.adl.nocturna.data.ClockFont
import com.adl.nocturna.data.ClockStyle
import com.adl.nocturna.data.Settings
import com.adl.nocturna.data.SettingsStore
import kotlin.math.roundToInt

private fun set(block: (Settings) -> Settings) = SettingsStore.update(block)

@Composable
fun ClockFacePage(padding: PaddingValues) {
    val s by SettingsStore.settings.collectAsStateWithLifecycle()
    SettingsPage(padding) {
        ClockPreview(s)
        SettingsGroup("Look") {
            ChoiceRow("Style", ClockStyle.entries, s.style, { if (it == ClockStyle.DIGITAL) "Digital" else "Analog" }) { v ->
                set { it.copy(style = v) }
            }
            if (s.style == ClockStyle.DIGITAL) {
                ChoiceRow("Font", ClockFont.entries, s.font, { it.label() }) { v -> set { it.copy(font = v) } }
            }
        }
        SettingsGroup("Colour") {
            val wallpaper = rememberWallpaperColor()
            ColorRow(
                Settings.COLORS, selectedSwatch(s, wallpaper),
                wallpaper = wallpaper, wallpaperSelected = s.colorFromWallpaper && wallpaper != null,
                onSelectWallpaper = { set { it.copy(colorFromWallpaper = true) } },
            ) { v -> set { it.copy(color = v, colorFromWallpaper = false) } }
        }
        SettingsGroup("Time") {
            SwitchRow("24-hour time", s.use24h) { v -> set { it.copy(use24h = v) } }
            SwitchRow("Show seconds", s.showSeconds) { v -> set { it.copy(showSeconds = v) } }
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
        val color = Color(clockColor(s, rememberWallpaperColor()))
        if (s.style == ClockStyle.DIGITAL) DigitalClock(now, s.use24h, s.showSeconds, s.font, color, 76.sp)
        else AnalogClock(now, s.showSeconds, color, 140.dp)
    }
}

@Composable
fun InfoLinePage(padding: PaddingValues, onNavigate: (String) -> Unit) {
    val context = LocalContext.current
    val s by SettingsStore.settings.collectAsStateWithLifecycle()
    val access = rememberSystemAccess()
    SettingsPage(padding) {
        Hint("The line of small details under the time", Modifier.padding(horizontal = 16.dp, vertical = 8.dp))
        SettingsGroup {
            SwitchRow("Date", s.showDate) { v -> set { it.copy(showDate = v) } }
            SwitchRow("Battery", s.showBattery) { v -> set { it.copy(showBattery = v) } }
            SwitchRow("Next alarm", s.showNextAlarm, "Within the next 24 hours") { v -> set { it.copy(showNextAlarm = v) } }
            if (s.showNextAlarm) {
                SwitchRow("Time until the alarm", s.showTimeToAlarm, "\"in 7 h 20 m\" when it's within 12 hours") { v ->
                    set { it.copy(showTimeToAlarm = v) }
                }
            }
            SwitchRow("Moon phase", s.showMoon, "\"Waxing gibbous\" at night; Day mode draws its own moon") { v ->
                set { it.copy(showMoon = v) }
            }
        }
        SettingsGroup("Weather") {
            SwitchRow("Weather", s.showWeather) { v -> set { it.copy(showWeather = v) } }
            if (s.showWeather) {
                SwitchRow("Forecast", s.showForecast, "Today's high and low; tomorrow's in the evening") { v ->
                    set { it.copy(showForecast = v) }
                }
                NavRow("Place and units", weatherSummary(s)) { onNavigate(SettingsActivity.SCREEN_WEATHER) }
            }
        }
        SettingsGroup("Notifications") {
            SwitchRow(
                "Notification icons", s.showNotifications,
                if (access.notificationAccess) "Icons of the apps with notifications" else "Needs notification access",
            ) { v -> set { it.copy(showNotifications = v) } }
            if (s.showNotifications && !access.notificationAccess) {
                ActionButton("Grant notification access") { openNotificationAccessSettings(context) }
            }
        }
    }
}

@Composable
fun BrightnessPage(padding: PaddingValues) {
    val s by SettingsStore.settings.collectAsStateWithLifecycle()
    val lux by rememberAmbientLux()
    SettingsPage(padding) {
        Hint(
            lux?.let { "Light sensor reads ${"%.1f".format(it)} lux now" }
                ?: "No light sensor: night mode follows the clock (22:00–07:00)",
            Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
        )
        SettingsGroup {
            SwitchRow("Dimming", s.dimEnabled, "Also on the clock: tap it, then the moon/sun button") { v ->
                set { it.copy(dimEnabled = v) }
            }
        }
        if (s.dimEnabled) {
            SettingsGroup("Brightness") {
                SwitchRow(
                    "Adjust to room light", s.autoBrightness,
                    if (s.autoBrightness) null else "Off: the phone's own brightness",
                ) { v -> set { it.copy(autoBrightness = v) } }
                if (s.autoBrightness) {
                SliderRow("Darkest", s.minBrightness, 0.01f..0.3f, { "${(it * 100).roundToInt()}%" }) { v ->
                    set { it.copy(minBrightness = v) }
                }
                SliderRow("Brightest", s.maxBrightness, 0.3f..1f, { "${(it * 100).roundToInt()}%" }) { v ->
                    set { it.copy(maxBrightness = v) }
                }
                }
                // Night mode dims the clock even without the room light, so this shows either way.
                SwitchRow(
                    "Tap to peek", s.tapToPeek,
                    "A tap on the dim clock brightens it for a few seconds; tap again for the controls",
                ) { v -> set { it.copy(tapToPeek = v) } }
            }
            SettingsGroup("Night mode") {
                SwitchRow("Night mode", s.nightModeEnabled, "Dim red clock when the room is dark") { v ->
                    set { it.copy(nightModeEnabled = v) }
                }
                if (s.nightModeEnabled) {
                    SliderRow("Turn on below", s.nightModeLux, 0.5f..30f, { "${"%.1f".format(it)} lux" }) { v ->
                        set { it.copy(nightModeLux = v) }
                    }
                }
            }
        }
        SettingsGroup("Screen care") {
            SwitchRow("Burn-in protection", s.burnInProtection, "Slowly moves the clock around the screen") { v ->
                set { it.copy(burnInProtection = v) }
            }
        }
    }
}
