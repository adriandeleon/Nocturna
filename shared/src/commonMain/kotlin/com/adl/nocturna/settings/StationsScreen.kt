package com.adl.nocturna.settings

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.adl.nocturna.data.SettingsStore
import com.adl.nocturna.data.Station
import com.adl.nocturna.clock.LocalClockPlatform
import com.adl.nocturna.clock.RadioControl
import com.adl.nocturna.sound.SleepSound

@Composable
fun StationsScreen(padding: PaddingValues) {
    val radio = LocalClockPlatform.current.radio
    val s by SettingsStore.settings.collectAsStateWithLifecycle()
    val radioOn by radio.isOn.collectAsStateWithLifecycle()
    val error by radio.error.collectAsStateWithLifecycle()
    var name by remember { mutableStateOf("") }
    var url by remember { mutableStateOf("") }

    LazyColumn(Modifier.fillMaxSize(), contentPadding = padding) {
        item {
            Text(
                "Pick what the clock's Radio button plays: a web station (any MP3, AAC or HLS stream URL works) or " +
                    "one of the sleep sounds below.",
                style = MaterialTheme.typography.bodySmall,
                modifier = Modifier.padding(16.dp),
            )
        }
        error?.let { item { Text(it, color = MaterialTheme.colorScheme.error, modifier = Modifier.padding(horizontal = 16.dp)) } }
        itemsIndexed(s.stations) { i, station ->
            Row(
                Modifier.fillMaxWidth().clickable { select(radio, i, station, radioOn) }.padding(horizontal = 8.dp, vertical = 4.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                RadioButton(selected = s.currentSleepSound == null && i == s.selectedStation, onClick = { select(radio, i, station, radioOn) })
                Column(Modifier.weight(1f)) {
                    Text(station.name, style = MaterialTheme.typography.bodyLarge)
                    Text(
                        station.url, style = MaterialTheme.typography.bodySmall, maxLines = 1, overflow = TextOverflow.Ellipsis,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
                    )
                }
                TextButton(onClick = {
                    SettingsStore.update {
                        val list = it.stations.filterIndexed { j, _ -> j != i }
                        val sel = when {
                            it.selectedStation > i -> it.selectedStation - 1
                            it.selectedStation == i -> 0
                            else -> it.selectedStation
                        }
                        it.copy(stations = list, selectedStation = sel.coerceAtMost((list.size - 1).coerceAtLeast(0)))
                    }
                }) { Text("Remove", color = MaterialTheme.colorScheme.error) }
            }
        }
        item { SectionHeader("Sleep sounds") }
        item {
            Text(
                "Made on your phone, so they play without the internet. They loop for as long as you like, " +
                    "and the sleep timer fades them out like a station.",
                style = MaterialTheme.typography.bodySmall,
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp),
            )
        }
        items(SleepSound.entries) { sound ->
            Row(
                Modifier.fillMaxWidth().clickable { select(radio, sound, radioOn) }.padding(horizontal = 8.dp, vertical = 4.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                RadioButton(selected = sound == s.currentSleepSound, onClick = { select(radio, sound, radioOn) })
                Column(Modifier.weight(1f)) {
                    Text(sound.label, style = MaterialTheme.typography.bodyLarge)
                    Text(
                        sound.description, style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
                    )
                }
            }
        }
        item {
            Row(Modifier.fillMaxWidth().padding(horizontal = 16.dp)) {
                TextButton(onClick = { radio.play(s.currentStation ?: return@TextButton) }, enabled = !radioOn && s.currentStation != null) {
                    Text("▶  Play")
                }
                TextButton(onClick = { radio.stop() }, enabled = radioOn) { Text("■  Stop") }
            }
        }
        item { SectionHeader("Add a station") }
        item {
            Column(Modifier.padding(horizontal = 16.dp)) {
                OutlinedTextField(name, { name = it }, label = { Text("Name") }, singleLine = true, modifier = Modifier.fillMaxWidth())
                OutlinedTextField(
                    url, { url = it }, label = { Text("Stream URL") }, singleLine = true,
                    placeholder = { Text("https://…") }, modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
                )
                val valid = name.isNotBlank() && (url.startsWith("http://") || url.startsWith("https://"))
                Button(
                    enabled = valid,
                    onClick = {
                        SettingsStore.update { it.copy(stations = it.stations + Station(name.trim(), url.trim())) }
                        name = ""
                        url = ""
                    },
                    modifier = Modifier.padding(vertical = 12.dp),
                ) { Text("Add") }
            }
        }
    }
}

private fun select(radio: RadioControl, index: Int, station: Station, radioOn: Boolean) {
    SettingsStore.update { it.copy(selectedStation = index, selectedSound = "") }
    if (radioOn) radio.play(station)
}

private fun select(radio: RadioControl, sound: SleepSound, radioOn: Boolean) {
    SettingsStore.update { it.copy(selectedSound = sound.id) }
    if (radioOn) radio.play(sound.station)
}
