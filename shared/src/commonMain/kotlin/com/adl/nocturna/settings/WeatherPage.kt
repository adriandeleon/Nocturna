package com.adl.nocturna.settings

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.adl.nocturna.data.Settings
import com.adl.nocturna.data.SettingsStore
import com.adl.nocturna.resources.*
import com.adl.nocturna.text.languageCode
import com.adl.nocturna.ui.currentLocale
import com.adl.nocturna.weather.WeatherRepository
import kotlinx.coroutines.launch
import org.jetbrains.compose.resources.stringResource

@Composable
fun WeatherPage(padding: PaddingValues, onNavigate: (String) -> Unit) {
    val platform = LocalSettingsPlatform.current
    val s by SettingsStore.settings.collectAsStateWithLifecycle()
    val askLocation = platform.rememberPermissionRequest(Permission.LOCATION) { granted ->
        SettingsStore.update { it.copy(weatherUseDeviceLocation = granted) }
        WeatherRepository.invalidate()
    }

    SettingsPage(padding) {
        if (!s.showWeather) {
            SettingsGroup {
                NavRow(stringResource(Res.string.weather_hidden), stringResource(Res.string.weather_hidden_detail)) { onNavigate(SettingsPages.INFO_LINE) }
            }
        }
        SettingsGroup(stringResource(Res.string.weather_place)) {
            SwitchRow(stringResource(Res.string.weather_use_location), s.weatherUseDeviceLocation, stringResource(Res.string.weather_use_location_detail)) { v ->
                if (v && !platform.isGranted(Permission.LOCATION)) {
                    askLocation()
                } else {
                    SettingsStore.update { it.copy(weatherUseDeviceLocation = v) }
                    WeatherRepository.invalidate()
                }
            }
            CitySearch(s)
        }
        SettingsGroup(stringResource(Res.string.weather_units)) {
            val fahrenheit = stringResource(Res.string.weather_fahrenheit)
            val celsius = stringResource(Res.string.weather_celsius)
            ChoiceRow(stringResource(Res.string.weather_temperature), listOf(false, true), s.weatherFahrenheit, { if (it) fahrenheit else celsius }) { v ->
                SettingsStore.update { it.copy(weatherFahrenheit = v) }
            }
        }
        WeatherAttribution()
    }
}

/** Open-Meteo's data (and the GeoNames data behind its city search) is CC BY 4.0, which asks for credit. */
@Composable
private fun WeatherAttribution() {
    val uriHandler = LocalUriHandler.current
    Text(
        stringResource(Res.string.weather_attribution),
        style = MaterialTheme.typography.bodySmall,
        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 12.dp)
            .clickable { runCatching { uriHandler.openUri("https://open-meteo.com/") } }
            .padding(horizontal = 32.dp, vertical = 8.dp),
    )
}

@Composable
private fun CitySearch(s: Settings) {
    val scope = rememberCoroutineScope()
    val language = currentLocale().languageCode
    var query by remember { mutableStateOf("") }
    var status by remember { mutableStateOf<String?>(null) }
    var busy by remember { mutableStateOf(false) }
    val noMatch = stringResource(Res.string.weather_no_match)

    Column(Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 6.dp)) {
        Text(
            if (s.weatherCity.isNotBlank()) stringResource(Res.string.weather_city, s.weatherCity) else stringResource(Res.string.weather_city_not_set),
            style = MaterialTheme.typography.bodyLarge,
        )
        if (s.weatherUseDeviceLocation) {
            Text(
                stringResource(Res.string.weather_city_fallback),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
            )
        }
        Row(
            Modifier.padding(top = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            OutlinedTextField(
                value = query,
                onValueChange = { query = it },
                singleLine = true,
                placeholder = { Text(stringResource(Res.string.weather_search_city)) },
                modifier = Modifier.weight(1f),
            )
            Button(
                enabled = query.isNotBlank() && !busy,
                onClick = {
                    busy = true
                    status = null
                    scope.launch {
                        val place = runCatching { WeatherRepository.geocode(query, language) }.getOrNull()
                        busy = false
                        if (place == null) {
                            status = noMatch
                        } else {
                            SettingsStore.update { it.copy(weatherCity = place.name, weatherLat = place.lat, weatherLon = place.lon) }
                            WeatherRepository.invalidate()
                            query = ""
                        }
                    }
                },
            ) { Text(if (busy) "…" else stringResource(Res.string.weather_find)) }
        }
        status?.let { Text(it, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.error) }
    }
}
