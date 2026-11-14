package com.adl.nocturna.settings

import android.Manifest
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.adl.nocturna.BuildConfig
import com.adl.nocturna.data.AlarmStore
import com.adl.nocturna.data.SettingsStore
import com.adl.nocturna.ui.Glyph
import com.adl.nocturna.ui.currentLocale

/** The first Settings page: what needs fixing, then one row per page with what it is set to. */
@Composable
fun SettingsHub(padding: PaddingValues, onNavigate: (String) -> Unit) {
    val context = LocalContext.current
    val s by SettingsStore.settings.collectAsStateWithLifecycle()
    val alarms by AlarmStore.alarms.collectAsStateWithLifecycle()
    val access = rememberSystemAccess()
    val notificationsAllowed = rememberNotificationsAllowed()
    val locale = currentLocale()
    val calendarPermission = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
        if (!granted) SettingsStore.update { it.copy(showCalendarEvents = false) }
    }

    val issues = buildList {
        if (!notificationsAllowed && alarms.any { it.enabled }) add(
            SettingsIssue(
                "Notifications are off",
                "Alarms still sound, but a locked phone won't show a screen to stop them",
                "Turn on",
            ) { openNotificationSettings(context) }
        )
        if (!access.fullScreen && Build.VERSION.SDK_INT >= 34) add(
            SettingsIssue("Full-screen alarms are off", "A ringing alarm can't fill the locked screen", "Allow") {
                openFullScreenSettings(context)
            }
        )
        if (s.showNotifications && !access.notificationAccess) add(
            SettingsIssue("Notification icons need access", "Or switch them off in Show on the clock", "Grant") {
                openNotificationAccessSettings(context)
            }
        )
        if (s.showCalendarEvents && !access.calendar) add(
            SettingsIssue("Today's events need calendar access", "Day mode shows the calendar page alone until then", "Allow") {
                calendarPermission.launch(Manifest.permission.READ_CALENDAR)
            }
        )
        if (s.autoStartOnCharge && !access.overlay) add(
            SettingsIssue("Opening when charging needs a permission", "Allow “Display over other apps”", "Allow") {
                openOverlaySettings(context)
            }
        )
    }

    SettingsPage(padding) {
        AttentionCard(issues)

        SettingsGroup("Clock") {
            HubRow(Glyph.CLOCK, "Clock face", clockSummary(s)) { onNavigate(SettingsActivity.SCREEN_CLOCK) }
            HubRow(Glyph.LIST, "Show on the clock", infoLineSummary(s)) { onNavigate(SettingsActivity.SCREEN_INFO_LINE) }
            HubRow(Glyph.MOON, "Brightness and night mode", brightnessSummary(s)) { onNavigate(SettingsActivity.SCREEN_BRIGHTNESS) }
            HubRow(Glyph.SUN, "Day mode", dayModeSummary(s, locale)) { onNavigate(SettingsActivity.SCREEN_DAY_MODE) }
        }
        SettingsGroup("Alarms and radio") {
            HubRow(Glyph.ALARM, "Alarms", alarmsSummary(alarms)) { onNavigate(SettingsActivity.SCREEN_ALARMS) }
            HubRow(Glyph.RADIO, "Radio stations", s.currentStation?.name ?: "None") { onNavigate(SettingsActivity.SCREEN_STATIONS) }
        }
        SettingsGroup("More") {
            HubRow(Glyph.PARTLY_DAY, "Weather", weatherSummary(s)) { onNavigate(SettingsActivity.SCREEN_WEATHER) }
            HubRow(Glyph.BOLT, "Starting the clock", startSummary(s)) { onNavigate(SettingsActivity.SCREEN_START) }
            HubRow(Glyph.INFO, "About Nocturna", versionLabel(BuildConfig.VERSION_NAME, BuildConfig.VERSION_CODE, BuildConfig.DEBUG)) {
                onNavigate(SettingsActivity.SCREEN_ABOUT)
            }
        }
    }
}
