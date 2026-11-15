package com.adl.nocturna.settings

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.adl.nocturna.data.Settings
import com.adl.nocturna.data.SettingsStore
import com.adl.nocturna.data.WorldZone
import com.adl.nocturna.day.ZoneChoice
import com.adl.nocturna.day.searchZones
import com.adl.nocturna.resources.*
import kotlinx.datetime.TimeZone
import org.jetbrains.compose.resources.stringResource

/** Day mode's stringResource(Res.string.zones_title): the zones the desk clock shows under its time, each removable, and a way to add one. */
@Composable
fun WorldZonesGroup(zones: List<WorldZone>) {
    var picking by remember { mutableStateOf(false) }
    if (picking) {
        ZonePicker(onDismiss = { picking = false }) { choice ->
            picking = false
            SettingsStore.update { it.copy(worldZones = (it.worldZones + choice.toWorldZone()).take(Settings.MAX_WORLD_ZONES)) }
        }
    }
    SettingsGroup(stringResource(Res.string.zones_title)) {
        zones.forEachIndexed { i, zone ->
            Row(Modifier.fillMaxWidth().padding(start = 16.dp, end = 8.dp, top = 4.dp, bottom = 4.dp), verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Text(zone.label, style = MaterialTheme.typography.bodyLarge)
                    Text(
                        zone.zoneId.replace('_', ' '), style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
                    )
                }
                TextButton(onClick = {
                    SettingsStore.update { s -> s.copy(worldZones = s.worldZones.filterIndexed { j, _ -> j != i }) }
                }) { Text(stringResource(Res.string.zones_remove), color = MaterialTheme.colorScheme.error) }
            }
        }
        if (zones.size < Settings.MAX_WORLD_ZONES) {
            ActionButton(if (zones.isEmpty()) stringResource(Res.string.zones_add) else stringResource(Res.string.zones_add_another)) { picking = true }
        }
        Hint(
            stringResource(Res.string.zones_hint, Settings.MAX_WORLD_ZONES),
            Modifier.padding(bottom = 6.dp),
        )
    }
}

/** A searchable list of cities, and of time zones by ID for places that aren't on it. */
@Composable
private fun ZonePicker(onDismiss: () -> Unit, onPick: (ZoneChoice) -> Unit) {
    var query by remember { mutableStateOf("") }
    // Only zones this phone's time zone data knows, so every one picked can be shown.
    val known = remember { TimeZone.availableZoneIds }
    val results = remember(query, known) { searchZones(query, known).filter { it.zoneId in known } }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(Res.string.zones_add)) },
        confirmButton = { TextButton(onClick = onDismiss) { Text(stringResource(Res.string.cancel)) } },
        text = {
            Column {
                OutlinedTextField(
                    query, { query = it }, label = { Text(stringResource(Res.string.zones_search)) }, singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                )
                LazyColumn(Modifier.fillMaxWidth().height(360.dp).padding(top = 8.dp)) {
                    items(results, key = { it.label + "|" + it.zoneId }) { choice ->
                        Column(Modifier.fillMaxWidth().clickable { onPick(choice) }.padding(vertical = 10.dp)) {
                            Text(choice.label, style = MaterialTheme.typography.bodyLarge)
                            Text(
                                choice.detail, style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
                            )
                        }
                    }
                    if (results.isEmpty()) item { Hint(stringResource(Res.string.zones_no_match)) }
                }
            }
        },
    )
}
