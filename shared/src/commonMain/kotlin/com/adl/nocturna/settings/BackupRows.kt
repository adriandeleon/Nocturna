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
import com.adl.nocturna.resources.*
import com.adl.nocturna.ui.currentLocale
import kotlin.time.Clock
import kotlinx.datetime.TimeZone
import kotlinx.datetime.todayIn
import org.jetbrains.compose.resources.stringResource

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

    val locale = currentLocale()
    val savedMessage = stringResource(Res.string.backup_saved)
    val importedMessage = stringResource(Res.string.backup_imported)
    val cantExport = stringResource(Res.string.backup_cant_export)
    val cantExportDetail = stringResource(Res.string.backup_cant_export_detail)
    val cantImport = stringResource(Res.string.backup_cant_import)
    val cantImportDetail = stringResource(Res.string.backup_cant_import_detail)
    val export = platform.rememberSaveFile(MIME_TYPE) { saved ->
        if (saved) platform.showMessage(savedMessage)
        else error = cantExport to cantExportDetail
    }
    val import = platform.rememberOpenFile(MAX_BYTES) { file ->
        val read = when (file) {
            is OpenedFile.Text -> runCatching { BackupCodec.decode(file.text) }
            OpenedFile.TooBig -> Result.failure(BackupException(BackupException.Problem.NOT_A_BACKUP))
            OpenedFile.Unreadable -> Result.failure(IllegalStateException("Unreadable file"))
        }
        read.onSuccess { pending = it }.onFailure {
            error = cantImport to ((it as? BackupException)?.problem?.message(locale) ?: cantImportDetail)
        }
    }

    Column {
        SectionHeader(stringResource(Res.string.backup_title))
        NavRow(stringResource(Res.string.backup_export), stringResource(Res.string.backup_export_detail)) {
            val backup = Backup(platform.versionName, SettingsStore.current, AlarmStore.alarms.value)
            export(BackupCodec.fileName(Clock.System.todayIn(TimeZone.currentSystemDefault())), BackupCodec.encode(backup))
        }
        NavRow(stringResource(Res.string.backup_import), stringResource(Res.string.backup_import_detail)) { import() }
        Hint(stringResource(Res.string.backup_no_photo))
    }

    pending?.let { backup ->
        AlertDialog(
            onDismissRequest = { pending = null },
            title = { Text(stringResource(Res.string.backup_import_question)) },
            text = { Text(stringResource(Res.string.backup_import_detail_summary, backup.summary(locale))) },
            confirmButton = {
                TextButton(onClick = {
                    pending = null
                    restore(platform, backup)
                    platform.showMessage(importedMessage)
                }) { Text(stringResource(Res.string.backup_replace)) }
            },
            dismissButton = { TextButton(onClick = { pending = null }) { Text(stringResource(Res.string.cancel)) } },
        )
    }
    error?.let { (title, message) ->
        AlertDialog(
            onDismissRequest = { error = null },
            title = { Text(title) },
            text = { Text(message) },
            confirmButton = { TextButton(onClick = { error = null }) { Text(stringResource(Res.string.ok)) } },
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
