package com.adl.nocturna.settings

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.adl.nocturna.data.SettingsStore
import com.adl.nocturna.resources.*
import org.jetbrains.compose.resources.stringResource

@Composable
fun StartPage(padding: PaddingValues) {
    val platform = LocalSettingsPlatform.current
    val s by SettingsStore.settings.collectAsStateWithLifecycle()
    val access = platform.access()

    SettingsPage(padding) {
        SettingsGroup(stringResource(Res.string.start_while_charging)) {
            NavRow(stringResource(Res.string.start_screen_saver), stringResource(Res.string.start_screen_saver_detail)) {
                platform.open(SystemPage.SCREEN_SAVER)
            }
            SwitchRow(stringResource(Res.string.start_open_charging), s.autoStartOnCharge, stringResource(Res.string.start_open_charging_detail)) { v ->
                SettingsStore.update { it.copy(autoStartOnCharge = v) }
                platform.setOpenOnCharge(v)
            }
            if (s.autoStartOnCharge && !access.overlay) {
                ActionButton(stringResource(Res.string.start_allow_overlay)) { platform.open(SystemPage.OVERLAY) }
            }
        }
        SettingsGroup(stringResource(Res.string.start_at_night)) {
            DndRow(s.dndWhileShowing)
        }
        if (!access.fullScreen) {
            SettingsGroup(stringResource(Res.string.settings_alarms)) {
                Hint(stringResource(Res.string.start_full_screen_hint))
                ActionButton(stringResource(Res.string.start_allow_full_screen)) { platform.open(SystemPage.FULL_SCREEN_ALARMS) }
            }
        }
    }
}
