package com.adl.nocturna.settings

import android.Manifest
import android.app.NotificationManager
import android.content.Intent
import android.content.pm.PackageManager
import androidx.core.net.toUri
import android.os.Build
import android.provider.Settings.ACTION_DREAM_SETTINGS
import android.provider.Settings.ACTION_MANAGE_OVERLAY_PERMISSION
import android.provider.Settings.ACTION_NOTIFICATION_LISTENER_SETTINGS
import android.provider.Settings.canDrawOverlays
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LifecycleEventEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.adl.nocturna.BuildConfig
import com.adl.nocturna.charging.ChargingService
import com.adl.nocturna.clock.label
import com.adl.nocturna.clock.rememberAmbientLux
import com.adl.nocturna.data.AlarmStore
import com.adl.nocturna.data.ClockFont
import com.adl.nocturna.data.ClockStyle
import com.adl.nocturna.data.Settings
import com.adl.nocturna.data.SettingsStore
import com.adl.nocturna.day.CalendarEvents
import com.adl.nocturna.notify.NotificationIconService
import com.adl.nocturna.weather.WeatherRepository
import kotlinx.coroutines.launch
import kotlin.math.roundToInt

@Composable
fun MainSettings(padding: PaddingValues, onNavigate: (String) -> Unit) {
    val context = LocalContext.current
    val s by SettingsStore.settings.collectAsStateWithLifecycle()
    val alarms by AlarmStore.alarms.collectAsStateWithLifecycle()
    val lux by rememberAmbientLux()
    fun set(block: (Settings) -> Settings) = SettingsStore.update(block)

    // Permission states that the user changes in system settings; re-check when we come back.
    var refresh by remember { mutableIntStateOf(0) }
    LifecycleEventEffect(Lifecycle.Event.ON_RESUME) { refresh++ }
    val notificationsAllowed = rememberNotificationsAllowed()
    val notificationAccess = remember(refresh) { NotificationIconService.isEnabled(context) }
    val overlayAllowed = remember(refresh) { canDrawOverlays(context) }
    val fullScreenAllowed = remember(refresh) {
        Build.VERSION.SDK_INT < 34 || context.getSystemService(NotificationManager::class.java).canUseFullScreenIntent()
    }
    val locationPermission = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
        set { it.copy(weatherUseDeviceLocation = granted) }
        WeatherRepository.invalidate()
    }
    val calendarAccess = remember(refresh) { CalendarEvents.hasPermission(context) }
    val calendarPermission = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
        set { it.copy(showCalendarEvents = granted) }
        refresh++
    }

    LazyColumn(Modifier.fillMaxSize(), contentPadding = padding) {
        item { SectionHeader("Clock") }
        item {
            ChoiceRow("Style", ClockStyle.entries, s.style, { if (it == ClockStyle.DIGITAL) "Digital" else "Analog" }) { v ->
                set { it.copy(style = v) }
            }
        }
        item { SwitchRow("24-hour time", s.use24h) { v -> set { it.copy(use24h = v) } } }
        item { SwitchRow("Show seconds", s.showSeconds) { v -> set { it.copy(showSeconds = v) } } }
        if (s.style == ClockStyle.DIGITAL) {
            item { ChoiceRow("Font", ClockFont.entries, s.font, { it.label() }) { v -> set { it.copy(font = v) } } }
        }
        item { ColorRow(Settings.COLORS, s.color) { v -> set { it.copy(color = v) } } }

        item { SectionHeader("Show on the clock") }
        item { SwitchRow("Date", s.showDate) { v -> set { it.copy(showDate = v) } } }
        item { SwitchRow("Battery", s.showBattery) { v -> set { it.copy(showBattery = v) } } }
        item { SwitchRow("Next alarm", s.showNextAlarm, "Within the next 24 hours") { v -> set { it.copy(showNextAlarm = v) } } }
        item { SwitchRow("Weather", s.showWeather) { v -> set { it.copy(showWeather = v) } } }
        item {
            SwitchRow(
                "Notification icons", s.showNotifications,
                if (notificationAccess) null else "Needs notification access — tap below",
            ) { v -> set { it.copy(showNotifications = v) } }
        }
        if (s.showNotifications && !notificationAccess) {
            item {
                ActionButton("Grant notification access") {
                    context.startActivity(Intent(ACTION_NOTIFICATION_LISTENER_SETTINGS))
                }
            }
        }

        item { SectionHeader("Day mode") }
        item {
            SwitchRow(
                "Day mode", s.dayMode,
                "The clock beside a calendar, for a desk stand. Also on the clock: tap it, then Day mode",
            ) { v -> set { it.copy(dayMode = v) } }
        }
        item {
            SwitchRow(
                "Today's events", s.showCalendarEvents,
                if (s.showCalendarEvents && !calendarAccess) "Needs calendar access — tap below" else "From the calendars on this phone",
            ) { v ->
                if (v && !calendarAccess) calendarPermission.launch(Manifest.permission.READ_CALENDAR)
                else set { it.copy(showCalendarEvents = v) }
            }
        }
        if (s.showCalendarEvents && !calendarAccess) {
            item { ActionButton("Allow calendar access") { calendarPermission.launch(Manifest.permission.READ_CALENDAR) } }
        }

        item { SectionHeader("Brightness and night mode") }
        item {
            Text(
                lux?.let { "Light sensor: ${"%.1f".format(it)} lux" } ?: "No light sensor — night mode follows the clock (22:00–07:00)",
                style = MaterialTheme.typography.bodySmall,
                modifier = Modifier.padding(horizontal = 16.dp),
            )
        }
        item {
            SwitchRow("Dimming", s.dimEnabled, "Also on the clock: tap it, then the moon/sun button") { v ->
                set { it.copy(dimEnabled = v) }
            }
        }
        item { SwitchRow("Adjust to room light", s.autoBrightness) { v -> set { it.copy(autoBrightness = v) } } }
        item {
            SliderRow("Darkest", s.minBrightness, 0.01f..0.3f, { "${(it * 100).roundToInt()}%" }) { v ->
                set { it.copy(minBrightness = v) }
            }
        }
        item {
            SliderRow("Brightest", s.maxBrightness, 0.3f..1f, { "${(it * 100).roundToInt()}%" }) { v ->
                set { it.copy(maxBrightness = v) }
            }
        }
        item {
            SwitchRow("Night mode", s.nightModeEnabled, "Dim red clock when the room is dark") { v ->
                set { it.copy(nightModeEnabled = v) }
            }
        }
        if (s.nightModeEnabled) {
            item {
                SliderRow("Turn on below", s.nightModeLux, 0.5f..30f, { "${"%.1f".format(it)} lux" }) { v ->
                    set { it.copy(nightModeLux = v) }
                }
            }
        }
        item {
            SwitchRow("Burn-in protection", s.burnInProtection, "Slowly moves the clock around the screen") { v ->
                set { it.copy(burnInProtection = v) }
            }
        }

        item { SectionHeader("Weather") }
        item {
            SwitchRow("Use my location", s.weatherUseDeviceLocation, "Approximate location only") { v ->
                if (v && ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
                    locationPermission.launch(Manifest.permission.ACCESS_COARSE_LOCATION)
                } else {
                    set { it.copy(weatherUseDeviceLocation = v) }
                    WeatherRepository.invalidate()
                }
            }
        }
        item { CitySearch(s) }
        item { SwitchRow("Fahrenheit", s.weatherFahrenheit) { v -> set { it.copy(weatherFahrenheit = v) } } }
        item { WeatherAttribution() }

        item { SectionHeader("Alarms and radio") }
        if (!notificationsAllowed && alarms.any { it.enabled }) item { NotificationsOffWarning() }
        item {
            val on = alarms.count { it.enabled }
            NavRow("Alarms", if (alarms.isEmpty()) "No alarms yet" else "$on of ${alarms.size} on") { onNavigate(SettingsActivity.SCREEN_ALARMS) }
        }
        item { NavRow("Radio stations", s.currentStation?.name ?: "None") { onNavigate(SettingsActivity.SCREEN_STATIONS) } }

        item { SectionHeader("Starting the clock") }
        item {
            NavRow("Screen saver", "Choose Nocturna and set it to start while charging") {
                context.startActivity(Intent(ACTION_DREAM_SETTINGS))
            }
        }
        item {
            SwitchRow("Open when charging", s.autoStartOnCharge, "Opens the clock when you plug in a locked phone") { v ->
                set { it.copy(autoStartOnCharge = v) }
                if (v) ChargingService.start(context) else ChargingService.stop(context)
            }
        }
        if (s.autoStartOnCharge && !overlayAllowed) {
            item {
                ActionButton("Allow “Display over other apps”") {
                    context.startActivity(
                        Intent(ACTION_MANAGE_OVERLAY_PERMISSION, "package:${context.packageName}".toUri())
                    )
                }
            }
        }
        if (!fullScreenAllowed && Build.VERSION.SDK_INT >= 34) {
            item {
                ActionButton("Allow full-screen alarms") {
                    context.startActivity(
                        Intent(
                            android.provider.Settings.ACTION_MANAGE_APP_USE_FULL_SCREEN_INTENT,
                            "package:${context.packageName}".toUri(),
                        )
                    )
                }
            }
        }

        item { SectionHeader("About") }
        item {
            NavRow("About Nocturna", versionLabel(BuildConfig.VERSION_NAME, BuildConfig.VERSION_CODE, BuildConfig.DEBUG)) {
                onNavigate(SettingsActivity.SCREEN_ABOUT)
            }
        }
        item { Spacer(Modifier.height(32.dp)) }
    }
}

@Composable
private fun ActionButton(text: String, onClick: () -> Unit) {
    OutlinedButton(onClick = onClick, modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp)) { Text(text) }
}

/** Open-Meteo's data (and the GeoNames data behind its city search) is CC BY 4.0, which asks for credit. */
@Composable
private fun WeatherAttribution() {
    val context = LocalContext.current
    Text(
        "Weather data by Open-Meteo.com (CC BY 4.0). City search based on GeoNames.",
        style = MaterialTheme.typography.bodySmall,
        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
        modifier = Modifier
            .fillMaxWidth()
            .clickable { context.openUrl("https://open-meteo.com/") }
            .padding(horizontal = 16.dp, vertical = 8.dp),
    )
}

@Composable
private fun CitySearch(s: Settings) {
    val scope = rememberCoroutineScope()
    var query by remember { mutableStateOf("") }
    var status by remember { mutableStateOf<String?>(null) }
    var busy by remember { mutableStateOf(false) }

    androidx.compose.foundation.layout.Column(Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 6.dp)) {
        Text(
            if (s.weatherCity.isNotBlank()) "City: ${s.weatherCity}" else "City: not set",
            style = MaterialTheme.typography.bodyLarge,
        )
        if (s.weatherUseDeviceLocation) {
            Text(
                "Used when your location isn't available",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
            )
        }
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            OutlinedTextField(
                value = query,
                onValueChange = { query = it },
                singleLine = true,
                placeholder = { Text("Search a city") },
                modifier = Modifier.weight(1f),
            )
            Button(
                enabled = query.isNotBlank() && !busy,
                onClick = {
                    busy = true
                    status = null
                    scope.launch {
                        val place = runCatching { WeatherRepository.geocode(query) }.getOrNull()
                        busy = false
                        if (place == null) {
                            status = "No match (or no connection)"
                        } else {
                            SettingsStore.update { it.copy(weatherCity = place.name, weatherLat = place.lat, weatherLon = place.lon) }
                            WeatherRepository.invalidate()
                            query = ""
                        }
                    }
                },
            ) { Text(if (busy) "…" else "Find") }
        }
        status?.let { Text(it, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.error) }
    }
}
