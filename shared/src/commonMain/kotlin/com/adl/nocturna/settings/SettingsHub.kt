package com.adl.nocturna.settings

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.adl.nocturna.data.AlarmStore
import com.adl.nocturna.data.SettingsStore
import com.adl.nocturna.ui.Glyph
import com.adl.nocturna.ui.currentLocale

/** The first Settings page: what needs fixing, then one row per page with what it is set to. */
@Composable
fun SettingsHub(padding: PaddingValues, onNavigate: (String) -> Unit) {
    val platform = LocalSettingsPlatform.current
    val s by SettingsStore.settings.collectAsStateWithLifecycle()
    val alarms by AlarmStore.alarms.collectAsStateWithLifecycle()
    val access = platform.access()
    val locale = currentLocale()
    val askCalendar = platform.rememberPermissionRequest(Permission.CALENDAR) { granted ->
        if (!granted) SettingsStore.update { it.copy(showCalendarEvents = false) }
    }

    val issues = buildList {
        if (!access.notifications && alarms.any { it.enabled }) add(
            SettingsIssue(
                "Notifications are off",
                "Alarms still sound, but a locked phone won't show a screen to stop them",
                "Turn on",
            ) { platform.open(SystemPage.NOTIFICATIONS) }
        )
        if (!access.fullScreen) add(
            SettingsIssue("Full-screen alarms are off", "A ringing alarm can't fill the locked screen", "Allow") {
                platform.open(SystemPage.FULL_SCREEN_ALARMS)
            }
        )
        if (s.showNotifications && !access.notificationAccess) add(
            SettingsIssue("Notification icons need access", "Or switch them off in Show on the clock", "Grant") {
                platform.open(SystemPage.NOTIFICATION_ACCESS)
            }
        )
        if (s.showCalendarEvents && !access.calendar) add(
            SettingsIssue("Today's events need calendar access", "Day mode shows the calendar page alone until then", "Allow") {
                askCalendar()
            }
        )
        if (s.autoStartOnCharge && !access.overlay) add(
            SettingsIssue("Opening when charging needs a permission", "Allow “Display over other apps”", "Allow") {
                platform.open(SystemPage.OVERLAY)
            }
        )
    }

    SettingsPage(padding) {
        AttentionCard(issues)

        SettingsGroup("Clock") {
            HubRow(Glyph.CLOCK, "Clock face", clockSummary(s)) { onNavigate(SettingsPages.CLOCK) }
            HubRow(Glyph.LIST, "Show on the clock", infoLineSummary(s)) { onNavigate(SettingsPages.INFO_LINE) }
            HubRow(Glyph.MOON, "Brightness and night mode", brightnessSummary(s)) { onNavigate(SettingsPages.BRIGHTNESS) }
            HubRow(Glyph.SUN, "Day mode", dayModeSummary(s, locale)) { onNavigate(SettingsPages.DAY_MODE) }
        }
        SettingsGroup("Alarms and radio") {
            HubRow(Glyph.ALARM, "Alarms", alarmsSummary(alarms)) { onNavigate(SettingsPages.ALARMS) }
            HubRow(Glyph.RADIO, "Radio stations", s.currentStation?.name ?: "None") { onNavigate(SettingsPages.STATIONS) }
        }
        SettingsGroup("More") {
            HubRow(Glyph.PARTLY_DAY, "Weather", weatherSummary(s)) { onNavigate(SettingsPages.WEATHER) }
            HubRow(Glyph.BOLT, "Starting the clock", startSummary(s)) { onNavigate(SettingsPages.START) }
            HubRow(Glyph.INFO, "About Nocturna", "${platform.versionLabel} · Backup") {
                onNavigate(SettingsPages.ABOUT)
            }
        }
    }
}
