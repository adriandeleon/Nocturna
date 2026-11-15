package com.adl.nocturna.settings

import android.content.Context
import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Column
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import com.adl.nocturna.BuildConfig
import com.adl.nocturna.charging.ChargingService
import com.adl.nocturna.data.AlarmStore
import com.adl.nocturna.data.Backup
import com.adl.nocturna.data.BackupCodec
import com.adl.nocturna.data.BackupException
import com.adl.nocturna.data.SettingsStore
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.datetime.TimeZone
import kotlinx.datetime.todayIn
import java.io.ByteArrayOutputStream
import java.io.IOException
import java.io.InputStream
import kotlin.time.Clock

/**
 * Export and Import of settings, radio stations and alarms, through files the user picks with the system's
 * file picker, so no storage permission is needed.
 */
@Composable
fun BackupRows() {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var pending by remember { mutableStateOf<Backup?>(null) }
    // The title and text of a dialog saying what went wrong.
    var error by remember { mutableStateOf<Pair<String, String>?>(null) }

    val export = rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument(MIME_TYPE)) { uri ->
        if (uri == null) return@rememberLauncherForActivityResult
        val json = BackupCodec.encode(Backup(BuildConfig.VERSION_NAME, SettingsStore.current, AlarmStore.alarms.value))
        scope.launch {
            val saved = withContext(Dispatchers.IO) { runCatching { write(context, uri, json) }.isSuccess }
            if (saved) Toast.makeText(context, "Backup saved", Toast.LENGTH_SHORT).show()
            else error = "Can't export" to "The backup couldn't be saved there. Try another place."
        }
    }
    val import = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        if (uri == null) return@rememberLauncherForActivityResult
        scope.launch {
            val read = withContext(Dispatchers.IO) {
                runCatching { BackupCodec.decode(read(context, uri) ?: throw BackupException(BackupException.Problem.NOT_A_BACKUP)) }
            }
            read.onSuccess { pending = it }.onFailure {
                error = "Can't import" to ((it as? BackupException)?.message ?: "The file couldn't be opened. Nothing was changed.")
            }
        }
    }

    Column {
        SectionHeader("Backup")
        NavRow("Export…", "Saves your settings, radio stations and alarms to a file") {
            export.launch(BackupCodec.fileName(Clock.System.todayIn(TimeZone.currentSystemDefault())))
        }
        // Some file managers don't know .json as application/json, so any file can be picked and is checked.
        NavRow("Import…", "Replaces them with the ones in a backup file") { import.launch(arrayOf("*/*")) }
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
                    restore(context, backup)
                    Toast.makeText(context, "Backup imported", Toast.LENGTH_SHORT).show()
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
private fun restore(context: Context, backup: Backup) {
    val wasAutoStart = SettingsStore.current.autoStartOnCharge
    SettingsStore.update { backup.settingsOver(it) }
    AlarmStore.replaceAll(backup.alarms)
    val autoStart = SettingsStore.current.autoStartOnCharge
    if (autoStart != wasAutoStart) {
        if (autoStart) ChargingService.start(context) else ChargingService.stop(context)
    }
}

private fun write(context: Context, uri: Uri, json: String) {
    val out = context.contentResolver.openOutputStream(uri, "w") ?: throw IOException("No stream for $uri")
    out.use { it.write(json.encodeToByteArray()) }
}

/** The file's text, or null when it is far bigger than any backup, so a wrong pick can't run out of memory. */
private fun read(context: Context, uri: Uri): String? {
    val input = context.contentResolver.openInputStream(uri) ?: throw IOException("No stream for $uri")
    return input.use { it.readAtMost(MAX_BYTES) }?.decodeToString()
}

private fun InputStream.readAtMost(limit: Int): ByteArray? {
    val out = ByteArrayOutputStream()
    val buffer = ByteArray(8 * 1024)
    while (true) {
        val n = read(buffer)
        if (n < 0) return out.toByteArray()
        out.write(buffer, 0, n)
        if (out.size() > limit) return null
    }
}

private const val MIME_TYPE = "application/json"
private const val MAX_BYTES = 1024 * 1024
