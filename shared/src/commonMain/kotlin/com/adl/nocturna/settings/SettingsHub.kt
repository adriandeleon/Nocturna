package com.adl.nocturna.settings

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.adl.nocturna.data.AlarmStore
import com.adl.nocturna.data.SettingsStore
import com.adl.nocturna.resources.*
import com.adl.nocturna.sound.displayName
import com.adl.nocturna.ui.Glyph
import com.adl.nocturna.ui.currentLocale
import org.jetbrains.compose.resources.stringResource

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
                stringResource(Res.string.settings_issue_notifications_title),
                stringResource(Res.string.settings_issue_notifications_detail),
                stringResource(Res.string.settings_issue_turn_on),
            ) { platform.open(SystemPage.NOTIFICATIONS) }
        )
        if (!access.fullScreen) add(
            SettingsIssue(
                stringResource(Res.string.settings_issue_full_screen_title),
                stringResource(Res.string.settings_issue_full_screen_detail),
                stringResource(Res.string.settings_issue_allow),
            ) {
                platform.open(SystemPage.FULL_SCREEN_ALARMS)
            }
        )
        if (s.showNotifications && !access.notificationAccess) add(
            SettingsIssue(
                stringResource(Res.string.settings_issue_notification_access_title),
                stringResource(Res.string.settings_issue_notification_access_detail),
                stringResource(Res.string.settings_issue_grant),
            ) {
                platform.open(SystemPage.NOTIFICATION_ACCESS)
            }
        )
        if (s.showCalendarEvents && !access.calendar) add(
            SettingsIssue(
                stringResource(Res.string.settings_issue_calendar_title),
                stringResource(Res.string.settings_issue_calendar_detail),
                stringResource(Res.string.settings_issue_allow),
            ) {
                askCalendar()
            }
        )
        if (s.autoStartOnCharge && !access.overlay) add(
            SettingsIssue(
                stringResource(Res.string.settings_issue_overlay_title),
                stringResource(Res.string.settings_issue_overlay_detail),
                stringResource(Res.string.settings_issue_allow),
            ) {
                platform.open(SystemPage.OVERLAY)
            }
        )
    }

    SettingsPage(padding) {
        AttentionCard(issues)

        SettingsGroup(stringResource(Res.string.settings_group_clock)) {
            HubRow(Glyph.CLOCK, stringResource(Res.string.settings_clock_face), clockSummary(s, locale)) { onNavigate(SettingsPages.CLOCK) }
            HubRow(Glyph.LIST, stringResource(Res.string.settings_show_on_clock), infoLineSummary(s, locale)) { onNavigate(SettingsPages.INFO_LINE) }
            HubRow(Glyph.MOON, stringResource(Res.string.settings_brightness), brightnessSummary(s, locale)) { onNavigate(SettingsPages.BRIGHTNESS) }
            HubRow(Glyph.SUN, stringResource(Res.string.settings_day_mode), dayModeSummary(s, locale)) { onNavigate(SettingsPages.DAY_MODE) }
        }
        SettingsGroup(stringResource(Res.string.settings_group_alarms_radio)) {
            HubRow(Glyph.ALARM, stringResource(Res.string.settings_alarms), alarmsSummary(alarms, locale)) { onNavigate(SettingsPages.ALARMS) }
            HubRow(Glyph.RADIO, stringResource(Res.string.settings_radio_stations), s.currentStation?.displayName(locale) ?: stringResource(Res.string.settings_none)) { onNavigate(SettingsPages.STATIONS) }
        }
        SettingsGroup(stringResource(Res.string.settings_group_more)) {
            HubRow(Glyph.PARTLY_DAY, stringResource(Res.string.settings_weather), weatherSummary(s, locale)) { onNavigate(SettingsPages.WEATHER) }
            HubRow(Glyph.BOLT, stringResource(Res.string.settings_starting), startSummary(s, locale)) { onNavigate(SettingsPages.START) }
            HubRow(Glyph.INFO, stringResource(Res.string.settings_about), stringResource(Res.string.settings_about_summary, platform.versionLabel)) {
                onNavigate(SettingsPages.ABOUT)
            }
        }
    }
}
