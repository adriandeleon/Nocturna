package com.adl.nocturna.settings

import android.text.format.DateFormat
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
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.FilledIconToggleButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TimePicker
import androidx.compose.material3.rememberTimePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.adl.nocturna.data.Alarm
import com.adl.nocturna.data.AlarmStore
import com.adl.nocturna.data.SettingsStore
import com.adl.nocturna.ui.currentLocale
import java.time.DayOfWeek
import java.time.Instant
import java.time.LocalTime
import java.time.ZoneId
import java.time.ZonedDateTime
import java.time.format.DateTimeFormatter
import java.time.format.TextStyle
import java.util.Locale

@Composable
fun AlarmsScreen(padding: PaddingValues) {
    val alarms by AlarmStore.alarms.collectAsStateWithLifecycle()
    val snooze by AlarmStore.snooze.collectAsStateWithLifecycle()
    val s by SettingsStore.settings.collectAsStateWithLifecycle()
    var editing by remember { mutableStateOf<Alarm?>(null) }
    var adding by remember { mutableStateOf(false) }

    Box(Modifier.fillMaxSize().padding(padding)) {
        LazyColumn(Modifier.fillMaxSize(), contentPadding = PaddingValues(bottom = 96.dp)) {
            snooze?.let { sn ->
                item {
                    Row(Modifier.fillMaxWidth().padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                        val t = Instant.ofEpochMilli(sn.until).atZone(ZoneId.systemDefault()).toLocalTime()
                        Text("Snoozed until ${formatTime(t, s.use24h)}", modifier = Modifier.weight(1f))
                        TextButton(onClick = { AlarmStore.clearSnooze() }) { Text("Cancel snooze") }
                    }
                }
            }
            if (alarms.isEmpty()) {
                item {
                    Text(
                        "No alarms yet. Tap “Add alarm” to set one.",
                        modifier = Modifier.padding(24.dp),
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f),
                    )
                }
            }
            items(alarms, key = { it.id }) { alarm ->
                AlarmCard(alarm, s.use24h, onEditTime = { editing = alarm })
            }
            item { SectionHeader("Alarm behaviour") }
            item {
                SliderRow("Snooze length", s.snoozeMinutes.toFloat(), 1f..30f, "${s.snoozeMinutes} min", steps = 28) { v ->
                    SettingsStore.update { it.copy(snoozeMinutes = v.toInt()) }
                }
            }
            item {
                SliderRow(
                    "Gentle wake-up", s.alarmRampSeconds.toFloat(), 0f..120f,
                    if (s.alarmRampSeconds == 0) "off" else "${s.alarmRampSeconds} s to full volume",
                    steps = 11,
                ) { v -> SettingsStore.update { it.copy(alarmRampSeconds = v.toInt()) } }
            }
            item { SwitchRow("Vibrate", s.alarmVibrate) { v -> SettingsStore.update { it.copy(alarmVibrate = v) } } }
        }

        ExtendedFloatingActionButton(
            onClick = { adding = true },
            modifier = Modifier.align(Alignment.BottomEnd).padding(16.dp),
        ) { Text("+  Add alarm") }
    }

    if (adding) {
        val now = LocalTime.now().plusHours(1)
        TimeDialog(now.hour, 0, s.use24h, onDismiss = { adding = false }) { h, m ->
            AlarmStore.add(h, m)
            adding = false
        }
    }
    editing?.let { alarm ->
        TimeDialog(alarm.hour, alarm.minute, s.use24h, onDismiss = { editing = null }) { h, m ->
            AlarmStore.update(alarm.copy(hour = h, minute = m, enabled = true))
            editing = null
        }
    }
}

@Composable
private fun AlarmCard(alarm: Alarm, use24h: Boolean, onEditTime: () -> Unit) {
    Card(Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 6.dp)) {
        Column(Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    formatTime(LocalTime.of(alarm.hour, alarm.minute), use24h),
                    fontSize = 44.sp,
                    color = if (alarm.enabled) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.4f),
                    modifier = Modifier.weight(1f).clickable(onClick = onEditTime),
                )
                Switch(checked = alarm.enabled, onCheckedChange = { AlarmStore.update(alarm.copy(enabled = it)) })
            }
            Text(
                describe(alarm, use24h),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
            )
            Spacer(Modifier.height(8.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                val locale = currentLocale()
                DayOfWeek.entries.forEachIndexed { i, day ->
                    FilledIconToggleButton(
                        checked = alarm.repeatsOn(i),
                        onCheckedChange = { on ->
                            val days = if (on) alarm.days or (1 shl i) else alarm.days and (1 shl i).inv()
                            AlarmStore.update(alarm.copy(days = days))
                        },
                        shape = CircleShape,
                        modifier = Modifier.size(36.dp),
                    ) { Text(day.getDisplayName(TextStyle.NARROW, locale), fontSize = 13.sp) }
                }
            }
            Row(verticalAlignment = Alignment.CenterVertically) {
                var label by remember(alarm.id) { mutableStateOf(alarm.label) }
                OutlinedTextField(
                    value = label,
                    onValueChange = {
                        label = it
                        AlarmStore.update(alarm.copy(label = it))
                    },
                    placeholder = { Text("Label") },
                    singleLine = true,
                    modifier = Modifier.weight(1f).padding(top = 8.dp),
                )
                TextButton(onClick = { AlarmStore.delete(alarm.id) }) {
                    Text("Delete", color = MaterialTheme.colorScheme.error)
                }
            }
        }
    }
}

private fun describe(alarm: Alarm, use24h: Boolean): String {
    if (!alarm.enabled) return "Off"
    val next = alarm.nextTrigger(ZonedDateTime.now())
    val pattern = DateFormat.getBestDateTimePattern(Locale.getDefault(), if (use24h) "EEEdMMMHHmm" else "EEEdMMMhmma")
    val repeat = when (alarm.days) {
        0 -> "Once"
        0b1111111 -> "Every day"
        0b0011111 -> "Weekdays"
        0b1100000 -> "Weekends"
        else -> "Repeats"
    }
    return "$repeat · next ${next.format(DateTimeFormatter.ofPattern(pattern, Locale.getDefault()))}"
}

private fun formatTime(t: LocalTime, use24h: Boolean): String =
    t.format(DateTimeFormatter.ofPattern(if (use24h) "HH:mm" else "h:mm a", Locale.getDefault()))

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun TimeDialog(hour: Int, minute: Int, use24h: Boolean, onDismiss: () -> Unit, onConfirm: (Int, Int) -> Unit) {
    val state = rememberTimePickerState(initialHour = hour, initialMinute = minute, is24Hour = use24h)
    AlertDialog(
        onDismissRequest = onDismiss,
        confirmButton = { TextButton(onClick = { onConfirm(state.hour, state.minute) }) { Text("OK") } },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } },
        text = { TimePicker(state) },
    )
}
