package com.adl.nocturna.settings

import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import com.adl.nocturna.clock.MORNING_HOUR
import com.adl.nocturna.clock.NIGHT_START_HOUR
import com.adl.nocturna.data.SettingsStore

/**
 * The "Do Not Disturb at night" switch (Settings.dndWhileShowing). It needs Do Not Disturb access, which
 * only the user can give, on a system screen; without it the switch says so and nothing happens.
 */
@Composable
fun DndRow(enabled: Boolean) {
    val platform = LocalSettingsPlatform.current
    val systemAccess = platform.access()
    val access = systemAccess.dndAccess
    val alarmsAllowed = systemAccess.dndAllowsAlarms
    var explain by remember { mutableStateOf(false) }

    val subtitle = when {
        enabled && !access -> "Not allowed: Nocturna doesn't have Do Not Disturb access"
        enabled && !alarmsAllowed -> "Not turned on: your Do Not Disturb settings would silence alarms"
        else -> "Turn on Do Not Disturb while the clock is on screen at night"
    }
    SwitchRow("Do Not Disturb at night", enabled, subtitle) { v ->
        if (v && !access) explain = true
        else SettingsStore.update { it.copy(dndWhileShowing = v) }
    }
    // Where the system has no such screen (some watches and TVs), the switch keeps saying it isn't allowed.
    if (enabled && !access) ActionButton("Allow Do Not Disturb access") { platform.open(SystemPage.DND_ACCESS) }
    if (enabled && alarmsAllowed) {
        Hint(
            "From $NIGHT_START_HOUR:00 to ${MORNING_HOUR.toString().padStart(2, '0')}:00, or from when night mode " +
                "starts in the evening. Priority only, so alarms still ring.",
        )
    }
    if (enabled && access && !alarmsAllowed) {
        Hint("Allow alarms in your phone's Do Not Disturb settings and Nocturna will turn it on at night.")
    }

    if (explain) {
        AlertDialog(
            onDismissRequest = { explain = false },
            title = { Text("Allow Do Not Disturb access") },
            text = {
                Text(
                    "To silence notifications at night, Nocturna needs your OK to change Do Not Disturb. " +
                        "On the next screen, find Nocturna and allow it. It only sets priority mode, so " +
                        "alarms still ring, and it puts things back when the clock leaves the screen or " +
                        "in the morning.",
                )
            },
            confirmButton = {
                TextButton(onClick = {
                    explain = false
                    SettingsStore.update { it.copy(dndWhileShowing = true) }
                    platform.open(SystemPage.DND_ACCESS)
                }) { Text("Continue") }
            },
            dismissButton = { TextButton(onClick = { explain = false }) { Text("Cancel") } },
        )
    }
}
