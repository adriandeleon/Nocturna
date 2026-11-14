package com.adl.nocturna.settings

import android.content.Intent
import android.os.Build
import android.provider.Settings.ACTION_DREAM_SETTINGS
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.adl.nocturna.charging.ChargingService
import com.adl.nocturna.data.SettingsStore

@Composable
fun StartPage(padding: PaddingValues) {
    val context = LocalContext.current
    val s by SettingsStore.settings.collectAsStateWithLifecycle()
    val access = rememberSystemAccess()

    SettingsPage(padding) {
        SettingsGroup("While charging") {
            NavRow("Screen saver", "Choose Nocturna and set it to start while charging") {
                context.startActivity(Intent(ACTION_DREAM_SETTINGS))
            }
            SwitchRow("Open when charging", s.autoStartOnCharge, "Opens the clock when you plug in a locked phone") { v ->
                SettingsStore.update { it.copy(autoStartOnCharge = v) }
                if (v) ChargingService.start(context) else ChargingService.stop(context)
            }
            if (s.autoStartOnCharge && !access.overlay) {
                ActionButton("Allow “Display over other apps”") { openOverlaySettings(context) }
            }
        }
        if (!access.fullScreen && Build.VERSION.SDK_INT >= 34) {
            SettingsGroup("Alarms") {
                Hint("Android needs your OK before a ringing alarm can fill the locked screen")
                ActionButton("Allow full-screen alarms") { openFullScreenSettings(context) }
            }
        }
    }
}
