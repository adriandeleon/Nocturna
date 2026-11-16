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
import com.adl.nocturna.clock.LocalClockPlatform
import com.adl.nocturna.clock.RadioControl
import com.adl.nocturna.data.AlarmStore
import com.adl.nocturna.data.Settings
import com.adl.nocturna.data.SettingsStore
import com.adl.nocturna.data.Station
import com.adl.nocturna.resources.*
import com.adl.nocturna.sound.SleepSound
import com.adl.nocturna.sound.isOnPhone
import com.adl.nocturna.text.PlatformLocale
import com.adl.nocturna.text.strings
import com.adl.nocturna.ui.currentLocale
import org.jetbrains.compose.resources.stringResource

@Composable
fun StationsScreen(padding: PaddingValues) {
    val radio = LocalClockPlatform.current.radio
    val platform = LocalSettingsPlatform.current
    val s by SettingsStore.settings.collectAsStateWithLifecycle()
    val radioOn by radio.isOn.collectAsStateWithLifecycle()
    val error by radio.error.collectAsStateWithLifecycle()
    var name by remember { mutableStateOf("") }
    var url by remember { mutableStateOf("") }
    val locale = currentLocale()
    val failed = stringResource(Res.string.stations_music_failed)
    val addMusic: (List<Station>) -> Unit = { picked ->
        if (picked.isEmpty()) platform.showMessage(failed)
        SettingsStore.update { st -> st.copy(stations = st.stations + picked.filter { p -> st.stations.none { it.url == p.url } }) }
    }
    val pickFiles = platform.rememberMusicPicker(folder = false, onPicked = addMusic)
    val pickFolder = platform.rememberMusicPicker(folder = true, onPicked = addMusic)
    val web = s.stations.withIndex().filter { !it.value.isOnPhone }
    val onPhone = s.stations.withIndex().filter { it.value.isOnPhone }
    val here = stringResource(Res.string.stations_on_phone)

    LazyColumn(Modifier.fillMaxSize(), contentPadding = padding) {
        item {
            Text(
                stringResource(Res.string.stations_hint),
                style = MaterialTheme.typography.bodySmall,
                modifier = Modifier.padding(16.dp),
            )
        }
        error?.let { item { Text(it, color = MaterialTheme.colorScheme.error, modifier = Modifier.padding(horizontal = 16.dp)) } }
        items(web, key = { "web-${it.index}" }) { (i, station) ->
            StationRow(station, station.url, s.currentSleepSound == null && i == s.selectedStation, { select(radio, i, station, radioOn) }) {
                remove(platform, i)
            }
        }
        item { SectionHeader(stringResource(Res.string.stations_phone_music)) }
        item { Hint(stringResource(Res.string.stations_phone_music_hint)) }
        items(onPhone, key = { "phone-${it.index}" }) { (i, station) ->
            StationRow(station, here, s.currentSleepSound == null && i == s.selectedStation, { select(radio, i, station, radioOn) }) {
                remove(platform, i)
            }
        }
        item {
            Row(Modifier.fillMaxWidth().padding(horizontal = 16.dp)) {
                TextButton(onClick = pickFiles) { Text(stringResource(Res.string.stations_add_files)) }
                TextButton(onClick = pickFolder) { Text(stringResource(Res.string.stations_add_folder)) }
            }
        }
        item { SectionHeader(stringResource(Res.string.stations_sleep_sounds)) }
        item { Hint(stringResource(Res.string.stations_sounds_hint)) }
        items(SleepSound.entries.filter { !it.isMusic }) { SoundRow(it, it == s.currentSleepSound, locale) { select(radio, it, radioOn) } }
        item { SectionHeader(stringResource(Res.string.stations_ambient_music)) }
        item { Hint(stringResource(Res.string.stations_music_hint)) }
        items(SleepSound.entries.filter { it.isMusic }) { SoundRow(it, it == s.currentSleepSound, locale) { select(radio, it, radioOn) } }
        item {
            Row(Modifier.fillMaxWidth().padding(horizontal = 16.dp)) {
                TextButton(onClick = { radio.play(s.currentStation ?: return@TextButton, s.radioSleepMinutes) }, enabled = !radioOn && s.currentStation != null) {
                    Text(stringResource(Res.string.stations_play))
                }
                TextButton(onClick = { radio.stop() }, enabled = radioOn) { Text(stringResource(Res.string.stations_stop)) }
            }
        }
        item {
            ChoiceRow(
                stringResource(Res.string.stations_sleep_timer), Settings.RADIO_SLEEP_MINUTES, s.radioSleepMinutes,
                { if (it == 0) strings(locale).off else strings(locale).minutesShort(it) },
            ) { v -> SettingsStore.update { it.copy(radioSleepMinutes = v) } }
            Text(
                stringResource(Res.string.stations_sleep_timer_hint),
                style = MaterialTheme.typography.bodySmall,
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp),
            )
        }
        item { SectionHeader(stringResource(Res.string.stations_add_title)) }
        item {
            Column(Modifier.padding(horizontal = 16.dp)) {
                OutlinedTextField(name, { name = it }, label = { Text(stringResource(Res.string.stations_name)) }, singleLine = true, modifier = Modifier.fillMaxWidth())
                OutlinedTextField(
                    url, { url = it }, label = { Text(stringResource(Res.string.stations_url)) }, singleLine = true,
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
                ) { Text(stringResource(Res.string.stations_add)) }
            }
        }
    }
}

@Composable
private fun Hint(text: String) {
    Text(text, style = MaterialTheme.typography.bodySmall, modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp))
}

/** A station in the list: its name, [detail] under it, and a button to remove it. */
@Composable
private fun StationRow(station: Station, detail: String, selected: Boolean, onSelect: () -> Unit, onRemove: () -> Unit) {
    Row(
        Modifier.fillMaxWidth().clickable(onClick = onSelect).padding(horizontal = 8.dp, vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        RadioButton(selected = selected, onClick = onSelect)
        Column(Modifier.weight(1f)) {
            Text(station.name, style = MaterialTheme.typography.bodyLarge)
            Text(
                detail, style = MaterialTheme.typography.bodySmall, maxLines = 1, overflow = TextOverflow.Ellipsis,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
            )
        }
        TextButton(onClick = onRemove) { Text(stringResource(Res.string.zones_remove), color = MaterialTheme.colorScheme.error) }
    }
}

@Composable
private fun SoundRow(sound: SleepSound, selected: Boolean, locale: PlatformLocale, onSelect: () -> Unit) {
    Row(
        Modifier.fillMaxWidth().clickable(onClick = onSelect).padding(horizontal = 8.dp, vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        RadioButton(selected = selected, onClick = onSelect)
        Column(Modifier.weight(1f)) {
            Text(sound.label(locale), style = MaterialTheme.typography.bodyLarge)
            Text(
                sound.description(locale), style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
            )
        }
    }
}

/**
 * Removes the station at [index], keeping the same station selected. Music on the phone that no other station
 * or alarm plays is given up too.
 */
private fun remove(platform: SettingsPlatform, index: Int) {
    val removed = SettingsStore.current.stations.getOrNull(index) ?: return
    SettingsStore.update {
        val list = it.stations.filterIndexed { j, _ -> j != index }
        val sel = when {
            it.selectedStation > index -> it.selectedStation - 1
            it.selectedStation == index -> 0
            else -> it.selectedStation
        }
        it.copy(stations = list, selectedStation = sel.coerceAtMost((list.size - 1).coerceAtLeast(0)))
    }
    val stillUsed = SettingsStore.current.stations.any { it.url == removed.url } ||
        AlarmStore.alarms.value.any { it.station?.url == removed.url }
    if (removed.isOnPhone && !stillUsed) platform.forgetMusic(removed)
}

private fun select(radio: RadioControl, index: Int, station: Station, radioOn: Boolean) {
    SettingsStore.update { it.copy(selectedStation = index, selectedSound = "") }
    if (radioOn) radio.play(station)
}

private fun select(radio: RadioControl, sound: SleepSound, radioOn: Boolean) {
    SettingsStore.update { it.copy(selectedSound = sound.id) }
    if (radioOn) radio.play(sound.station)
}
