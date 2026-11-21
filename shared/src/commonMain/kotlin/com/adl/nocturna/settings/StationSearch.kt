package com.adl.nocturna.settings

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.adl.nocturna.clock.ClockPlatform
import com.adl.nocturna.clock.LocalClockPlatform
import com.adl.nocturna.data.Settings
import com.adl.nocturna.data.SettingsStore
import com.adl.nocturna.radio.FoundStation
import com.adl.nocturna.radio.RadioBrowser
import com.adl.nocturna.resources.*
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import org.jetbrains.compose.resources.stringResource

/** What the station search shows: nothing yet, a search under way, its results, or why it failed. */
sealed interface SearchState {
    data object Idle : SearchState
    data object Busy : SearchState
    data class Found(val stations: List<FoundStation>) : SearchState
    data object NoLocation : SearchState
    data object Failed : SearchState
}

/** The station search's query and results, kept while the Stations page is open. */
@Stable
class StationSearch(private val scope: CoroutineScope, private val platform: ClockPlatform) {
    private val browser = RadioBrowser()
    private var job: Job? = null
    var query by mutableStateOf("")
    var state by mutableStateOf<SearchState>(SearchState.Idle)
        private set

    fun search() {
        val q = query.trim()
        if (q.isEmpty()) return
        run { browser.search(q) }
    }

    /** Stations around the weather's place when it's set by hand, else around the phone. */
    fun nearby() {
        job?.cancel()
        state = SearchState.Busy
        job = scope.launch {
            val s = SettingsStore.current
            val place = if (s.hasWeatherLocation) s.weatherLat to s.weatherLon else runCatching { platform.deviceLocation() }.getOrNull()
            state = if (place == null) SearchState.NoLocation
            else runCatching { SearchState.Found(browser.nearby(place.first, place.second)) }.getOrDefault(SearchState.Failed)
        }
    }

    private fun run(block: suspend () -> List<FoundStation>) {
        job?.cancel()
        state = SearchState.Busy
        job = scope.launch { state = runCatching { SearchState.Found(block()) }.getOrDefault(SearchState.Failed) }
    }
}

@Composable
fun rememberStationSearch(): StationSearch {
    val scope = rememberCoroutineScope()
    val platform = LocalClockPlatform.current
    return remember { StationSearch(scope, platform) }
}

/**
 * The top of the Stations page: a search of Radio Browser by name, genre or country, and the stations near
 * you. A found station plays to try it, and *Add* saves it to the list.
 */
fun LazyListScope.stationSearch(search: StationSearch, settings: Settings, onTry: (FoundStation) -> Unit) {
    item { SectionHeader(stringResource(Res.string.search_title)) }
    item {
        Column(Modifier.padding(horizontal = 16.dp)) {
            OutlinedTextField(
                search.query, { search.query = it },
                label = { Text(stringResource(Res.string.search_hint)) }, singleLine = true,
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                keyboardActions = KeyboardActions(onSearch = { search.search() }),
                modifier = Modifier.fillMaxWidth(),
            )
            Row(Modifier.padding(vertical = 8.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Button(onClick = search::search, enabled = search.query.isNotBlank()) { Text(stringResource(Res.string.search_go)) }
                OutlinedButton(onClick = search::nearby) { Text(stringResource(Res.string.search_nearby)) }
            }
        }
    }
    when (val st = search.state) {
        SearchState.Idle -> {}
        SearchState.Busy -> item {
            Row(Modifier.fillMaxWidth().padding(16.dp), horizontalArrangement = Arrangement.Center) { CircularProgressIndicator() }
        }
        SearchState.Failed -> item { Message(stringResource(Res.string.search_failed)) }
        SearchState.NoLocation -> item { Message(stringResource(Res.string.search_no_location)) }
        is SearchState.Found -> {
            if (st.stations.isEmpty()) item { Message(stringResource(Res.string.search_none)) }
            items(st.stations, key = { "found-${it.url}" }) { found ->
                FoundRow(found, saved = settings.stations.any { it.url == found.url }, onTry = { onTry(found) })
            }
            item {
                Text(
                    stringResource(Res.string.search_credit), style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                )
            }
        }
    }
}

@Composable
private fun Message(text: String) {
    Text(text, color = MaterialTheme.colorScheme.error, modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp))
}

/** A found station: tap to try it; *Add* saves it to the list. */
@Composable
private fun FoundRow(found: FoundStation, saved: Boolean, onTry: () -> Unit) {
    Row(
        Modifier.fillMaxWidth().clickable(onClick = onTry).padding(horizontal = 16.dp, vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(Modifier.weight(1f)) {
            Text(found.name, style = MaterialTheme.typography.bodyLarge, maxLines = 1, overflow = TextOverflow.Ellipsis)
            Text(
                found.detail, style = MaterialTheme.typography.bodySmall, maxLines = 1, overflow = TextOverflow.Ellipsis,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
            )
        }
        TextButton(
            enabled = !saved,
            onClick = {
                SettingsStore.update {
                    if (it.stations.any { s -> s.url == found.url }) it
                    else it.copy(stations = it.stations + found.station)
                }
            },
        ) { Text(stringResource(if (saved) Res.string.search_added else Res.string.stations_add)) }
    }
}
