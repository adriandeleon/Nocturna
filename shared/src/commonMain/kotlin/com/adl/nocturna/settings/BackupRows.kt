package com.adl.nocturna.settings

import androidx.compose.foundation.layout.Column
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import com.adl.nocturna.data.AlarmStore
import com.adl.nocturna.data.Backup
import com.adl.nocturna.data.BackupCodec
import com.adl.nocturna.data.BackupException
import com.adl.nocturna.data.SettingsStore
import kotlinx.datetime.TimeZone
import kotlinx.datetime.todayIn
import kotlin.time.Clock

/**
 * Export and Import of settings, radio stations and alarms, through files the user picks with the system's
 * file picker, so no storage permission is needed.
 */
@Composable
fun BackupRows() {
    val platform = LocalSettingsPlatform.current
    var pending by remember { mutableStateOf<Backup?>(null) }
    // The title and text of a dialog saying what went wrong.
    var error by remember { mutableStateOf<Pair<String, String>?>(null) }

    val export = platform.rememberSaveFile(MIME_TYPE) { saved ->
        if (saved) platform.showMessage("Backup saved")
        else error = "Can't export" to "The backup couldn't be saved there. Try another place."
    }
    val import = platform.rememberOpenFile(MAX_BYTES) { file ->
        val read = when (file) {
            is OpenedFile.Text -> runCatching { BackupCodec.decode(file.text) }
            OpenedFile.TooBig -> Result.failure(BackupException(BackupException.Problem.NOT_A_BACKUP))
            OpenedFile.Unreadable -> Result.failure(IllegalStateException("Unreadable file"))
        }
        read.onSuccess { pending = it }.onFailure {
            error = "Can't import" to ((it as? BackupException)?.message ?: "The file couldn't be opened. Nothing was changed.")
        }
    }

    Column {
        SectionHeader("Backup")
        NavRow("Export…", "Saves your settings, radio stations and alarms to a file") {
            val backup = Backup(platform.versionName, SettingsStore.current, AlarmStore.alarms.value)
            export(BackupCodec.fileName(Clock.System.todayIn(TimeZone.currentSystemDefault())), BackupCodec.encode(backup))
        }
        NavRow("Import…", "Replaces them with the ones in a backup file") { import() }
        Hint("The Day mode photo isn't included in a backup.")
    }

    pending?.let { backup ->
        AlertDialog(
            onDismissRequest = { pending = null },
            title = { Text("Import this backup?") },
            text = { Text("${backup.summary}; replaces your current settings, radio stations and alarms.") },
            confirmButton = {
                TextButton(onClick = {
                    pending = null
                    restore(platform, backup)
                    platform.showMessage("Backup imported")
                }) { Text("Replace") }
            },
            dismissButton = { TextButton(onClick = { pending = null }) { Text("Cancel") } },
        )
    }
    error?.let { (title, message) ->
        AlertDialog(
            onDismissRequest = { error = null },
            title = { Text(title) },
            text = { Text(message) },
            confirmButton = { TextButton(onClick = { error = null }) { Text("OK") } },
        )
    }
}

/** Puts [backup] in place of the settings and alarms; saving the alarms sets the system alarm again. */
private fun restore(platform: SettingsPlatform, backup: Backup) {
    val wasAutoStart = SettingsStore.current.autoStartOnCharge
    SettingsStore.update { backup.settingsOver(it) }
    AlarmStore.replaceAll(backup.alarms)
    val autoStart = SettingsStore.current.autoStartOnCharge
    if (autoStart != wasAutoStart) platform.setOpenOnCharge(autoStart)
}

private const val MIME_TYPE = "application/json"

/** Far bigger than any backup, so a wrong pick can't run out of memory. */
private const val MAX_BYTES = 1024 * 1024
