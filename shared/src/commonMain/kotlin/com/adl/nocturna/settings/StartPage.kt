package com.adl.nocturna.settings

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.adl.nocturna.data.SettingsStore

@Composable
fun StartPage(padding: PaddingValues) {
    val platform = LocalSettingsPlatform.current
    val s by SettingsStore.settings.collectAsStateWithLifecycle()
    val access = platform.access()

    SettingsPage(padding) {
        SettingsGroup("While charging") {
            NavRow("Screen saver", "Choose Nocturna and set it to start while charging") {
                platform.open(SystemPage.SCREEN_SAVER)
            }
            SwitchRow("Open when charging", s.autoStartOnCharge, "Opens the clock when you plug in a locked phone") { v ->
                SettingsStore.update { it.copy(autoStartOnCharge = v) }
                platform.setOpenOnCharge(v)
            }
            if (s.autoStartOnCharge && !access.overlay) {
                ActionButton("Allow “Display over other apps”") { platform.open(SystemPage.OVERLAY) }
            }
        }
        if (!access.fullScreen) {
            SettingsGroup("Alarms") {
                Hint("Android needs your OK before a ringing alarm can fill the locked screen")
                ActionButton("Allow full-screen alarms") { platform.open(SystemPage.FULL_SCREEN_ALARMS) }
            }
        }
    }
}
