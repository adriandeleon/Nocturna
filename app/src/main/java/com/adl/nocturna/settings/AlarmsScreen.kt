package com.adl.nocturna.settings

import android.Manifest
import android.os.Build
import android.text.format.DateFormat
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SnackbarResult
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.adl.nocturna.alarm.AlarmService
import com.adl.nocturna.data.Alarm
import com.adl.nocturna.data.AlarmStore
import com.adl.nocturna.data.SettingsStore
import com.adl.nocturna.data.Station
import com.adl.nocturna.ui.currentLocale
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.time.Instant
import java.time.LocalTime
import java.time.ZoneId
import java.time.ZonedDateTime
import java.time.format.DateTimeFormatter
import java.util.Locale
import kotlin.math.roundToInt

@Composable
fun AlarmsScreen(padding: PaddingValues) {
    val alarms by AlarmStore.alarms.collectAsStateWithLifecycle()
    val snooze by AlarmStore.snooze.collectAsStateWithLifecycle()
    val s by SettingsStore.settings.collectAsStateWithLifecycle()
    val notificationsAllowed = rememberNotificationsAllowed()
    val askNotifications = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { }
    val snackbar = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()
    val locale = currentLocale()
    var editing by remember { mutableStateOf<Alarm?>(null) }
    var adding by remember { mutableStateOf(false) }

    Box(Modifier.fillMaxSize().padding(padding)) {
        LazyColumn(Modifier.fillMaxSize(), contentPadding = PaddingValues(bottom = 32.dp)) {
            if (!notificationsAllowed) item { NotificationsOffWarning() }
            item {
                Button(
                    onClick = {
                        adding = true
                        if (!notificationsAllowed && Build.VERSION.SDK_INT >= 33) {
                            askNotifications.launch(Manifest.permission.POST_NOTIFICATIONS)
                        }
                    },
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp),
                ) { Text("+  Add alarm") }
            }
            snooze?.let { sn ->
                item {
                    Row(Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 4.dp), verticalAlignment = Alignment.CenterVertically) {
                        val t = Instant.ofEpochMilli(sn.until).atZone(ZoneId.systemDefault()).toLocalTime()
                        Text("Snoozed until ${formatTime(t, s.use24h, locale)}", modifier = Modifier.weight(1f))
                        TextButton(onClick = { AlarmStore.clearSnooze() }) { Text("Cancel snooze") }
                    }
                }
            }
            if (alarms.isEmpty()) {
                item {
                    Text(
                        "No alarms yet.",
                        modifier = Modifier.padding(24.dp),
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f),
                    )
                }
            }
            items(alarms, key = { it.id }) { alarm ->
                AlarmCard(
                    alarm, s.use24h, locale, s.stations,
                    onEditTime = { editing = alarm },
                    onDelete = {
                        val removed = AlarmStore.delete(alarm.id) ?: return@AlarmCard
                        scope.launch {
                            snackbar.currentSnackbarData?.dismiss()
                            val result = snackbar.showSnackbar(
                                "Alarm ${formatTime(LocalTime.of(removed.hour, removed.minute), s.use24h, locale)} deleted",
                                actionLabel = "Undo",
                                duration = SnackbarDuration.Long,
                            )
                            if (result == SnackbarResult.ActionPerformed) AlarmStore.restore(removed)
                        }
                    },
                )
            }
            item { SectionHeader("Alarm behaviour") }
            item {
                SliderRow("Snooze length", s.snoozeMinutes.toFloat(), 1f..30f, { "${it.roundToInt()} min" }, steps = 28) { v ->
                    SettingsStore.update { it.copy(snoozeMinutes = v.roundToInt()) }
                }
            }
            item {
                SliderRow(
                    "Gentle wake-up", s.alarmRampSeconds.toFloat(), 0f..120f,
                    { if (it.roundToInt() == 0) "off" else "${it.roundToInt()} s to full volume" },
                    steps = 11,
                ) { v -> SettingsStore.update { it.copy(alarmRampSeconds = v.roundToInt()) } }
            }
            item { SwitchRow("Vibrate", s.alarmVibrate) { v -> SettingsStore.update { it.copy(alarmVibrate = v) } } }
        }

        SnackbarHost(snackbar, Modifier.align(Alignment.BottomCenter).padding(16.dp))
    }

    if (adding) {
        val next = LocalTime.now().plusHours(1)
        TimeDialog(next.hour, 0, s.use24h, onDismiss = { adding = false }) { h, m ->
            AlarmStore.add(h, m)
            adding = false
        }
    }
    editing?.let { alarm ->
        TimeDialog(alarm.hour, alarm.minute, s.use24h, onDismiss = { editing = null }) { h, m ->
            AlarmStore.update(alarm.id) { it.copy(hour = h, minute = m, enabled = true) }
            editing = null
        }
    }
}

@Composable
private fun AlarmCard(
    alarm: Alarm,
    use24h: Boolean,
    locale: Locale,
    stations: List<Station>,
    onEditTime: () -> Unit,
    onDelete: () -> Unit,
) {
    val muted = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
    Card(Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 6.dp)) {
        Column(Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    formatTime(LocalTime.of(alarm.hour, alarm.minute), use24h, locale),
                    fontSize = 44.sp,
                    color = if (alarm.enabled) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.4f),
                    modifier = Modifier.weight(1f).clickable(onClick = onEditTime),
                )
                Switch(checked = alarm.enabled, onCheckedChange = { on -> AlarmStore.update(alarm.id) { it.copy(enabled = on) } })
            }
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    describe(alarm, use24h, locale),
                    style = MaterialTheme.typography.bodySmall,
                    color = muted,
                    modifier = Modifier.weight(1f),
                )
                if (alarm.enabled && alarm.repeats) {
                    val skipping = alarm.skipping(ZonedDateTime.now()) != null
                    TextButton(onClick = { AlarmStore.update(alarm.id) { it.toggleSkip(ZonedDateTime.now()) } }) {
                        Text(if (skipping) "Undo skip" else "Skip next")
                    }
                }
            }
            Spacer(Modifier.height(8.dp))
            DayToggles(alarm.days, locale) { days -> AlarmStore.update(alarm.id) { it.copy(days = days) } }
            SoundPicker(alarm, stations)
            Row(verticalAlignment = Alignment.CenterVertically) {
                LabelField(alarm, Modifier.weight(1f).padding(top = 8.dp))
                TextButton(onClick = onDelete) { Text("Delete", color = MaterialTheme.colorScheme.error) }
            }
        }
    }
}

/** Chooses what the alarm plays: the alarm tone or one of the radio stations. */
@Composable
private fun SoundPicker(alarm: Alarm, stations: List<Station>) {
    var open by remember { mutableStateOf(false) }
    val muted = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
    fun choose(station: Station?) {
        open = false
        AlarmStore.update(alarm.id) { it.copy(station = station) }
    }
    Row(Modifier.padding(top = 8.dp), verticalAlignment = Alignment.CenterVertically) {
        Text("Sound", style = MaterialTheme.typography.bodyMedium, modifier = Modifier.weight(1f))
        Box {
            TextButton(onClick = { open = true }) { Text(alarm.station?.name ?: "Alarm tone") }
            DropdownMenu(expanded = open, onDismissRequest = { open = false }) {
                DropdownMenuItem(text = { Text("Alarm tone") }, onClick = { choose(null) })
                stations.forEach { st -> DropdownMenuItem(text = { Text(st.name) }, onClick = { choose(st) }) }
            }
        }
    }
    if (alarm.station != null) {
        Text(
            "If the station doesn't play within ${AlarmService.STREAM_TIMEOUT_MS / 1000} s, the alarm tone rings instead.",
            style = MaterialTheme.typography.bodySmall,
            color = muted,
        )
    }
}

/** Edits the label locally and saves once typing pauses, instead of on every keystroke. */
@Composable
private fun LabelField(alarm: Alarm, modifier: Modifier) {
    var label by remember(alarm.id) { mutableStateOf(alarm.label) }
    LaunchedEffect(label) {
        if (label == alarm.label) return@LaunchedEffect
        delay(600)
        AlarmStore.update(alarm.id) { it.copy(label = label) }
    }
    OutlinedTextField(
        value = label,
        onValueChange = { label = it },
        placeholder = { Text("Label") },
        singleLine = true,
        modifier = modifier,
    )
}

private fun describe(alarm: Alarm, use24h: Boolean, locale: Locale): String {
    if (!alarm.enabled) return "Off"
    val now = ZonedDateTime.now()
    val next = alarm.nextTrigger(now)
    val pattern = DateFormat.getBestDateTimePattern(locale, if (use24h) "EEEdMMMHHmm" else "EEEdMMMhmma")
    val skipped = alarm.skipping(now)?.let {
        " · skipping " + it.format(DateTimeFormatter.ofPattern(DateFormat.getBestDateTimePattern(locale, "EEEdMMM"), locale))
    }.orEmpty()
    val repeat = when (alarm.days) {
        0 -> "Once"
        Alarm.ALL_DAYS -> "Every day"
        0b0011111 -> "Weekdays"
        0b1100000 -> "Weekends"
        else -> "Repeats"
    }
    return "$repeat$skipped · next ${next.format(DateTimeFormatter.ofPattern(pattern, locale))}"
}
