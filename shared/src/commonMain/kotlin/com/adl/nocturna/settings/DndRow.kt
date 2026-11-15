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
import com.adl.nocturna.resources.*
import org.jetbrains.compose.resources.stringResource

/**
 * The stringResource(Res.string.dnd_title) switch (Settings.dndWhileShowing). It needs Do Not Disturb access, which
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
        enabled && !access -> stringResource(Res.string.dnd_not_allowed)
        enabled && !alarmsAllowed -> stringResource(Res.string.dnd_not_turned_on)
        else -> stringResource(Res.string.dnd_detail)
    }
    SwitchRow(stringResource(Res.string.dnd_title), enabled, subtitle) { v ->
        if (v && !access) explain = true
        else SettingsStore.update { it.copy(dndWhileShowing = v) }
    }
    // Where the system has no such screen (some watches and TVs), the switch keeps saying it isn't allowed.
    if (enabled && !access) ActionButton(stringResource(Res.string.dnd_allow_access)) { platform.open(SystemPage.DND_ACCESS) }
    if (enabled && alarmsAllowed) {
        Hint(
            stringResource(
                Res.string.dnd_hours,
                "$NIGHT_START_HOUR:00",
                "${MORNING_HOUR.toString().padStart(2, '0')}:00",
            ),
        )
    }
    if (enabled && access && !alarmsAllowed) {
        Hint(stringResource(Res.string.dnd_allow_alarms))
    }

    if (explain) {
        AlertDialog(
            onDismissRequest = { explain = false },
            title = { Text(stringResource(Res.string.dnd_allow_access)) },
            text = {
                Text(stringResource(Res.string.dnd_explain))
            },
            confirmButton = {
                TextButton(onClick = {
                    explain = false
                    SettingsStore.update { it.copy(dndWhileShowing = true) }
                    platform.open(SystemPage.DND_ACCESS)
                }) { Text(stringResource(Res.string.dnd_continue)) }
            },
            dismissButton = { TextButton(onClick = { explain = false }) { Text(stringResource(Res.string.cancel)) } },
        )
    }
}
