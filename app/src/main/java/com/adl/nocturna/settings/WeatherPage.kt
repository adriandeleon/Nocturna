package com.adl.nocturna.settings

import android.Manifest
import android.content.pm.PackageManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.adl.nocturna.data.Settings
import com.adl.nocturna.data.SettingsStore
import com.adl.nocturna.weather.WeatherRepository
import kotlinx.coroutines.launch
import java.util.Locale

@Composable
fun WeatherPage(padding: PaddingValues, onNavigate: (String) -> Unit) {
    val context = LocalContext.current
    val s by SettingsStore.settings.collectAsStateWithLifecycle()
    val locationPermission = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
        SettingsStore.update { it.copy(weatherUseDeviceLocation = granted) }
        WeatherRepository.invalidate()
    }

    SettingsPage(padding) {
        if (!s.showWeather) {
            SettingsGroup {
                NavRow("The weather is hidden", "Tap to show it under the clock") { onNavigate(SettingsActivity.SCREEN_INFO_LINE) }
            }
        }
        SettingsGroup("Place") {
            SwitchRow("Use my location", s.weatherUseDeviceLocation, "Approximate location only") { v ->
                if (v && ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
                    locationPermission.launch(Manifest.permission.ACCESS_COARSE_LOCATION)
                } else {
                    SettingsStore.update { it.copy(weatherUseDeviceLocation = v) }
                    WeatherRepository.invalidate()
                }
            }
            CitySearch(s)
        }
        SettingsGroup("Units") {
            ChoiceRow("Temperature", listOf(false, true), s.weatherFahrenheit, { if (it) "Fahrenheit (°F)" else "Celsius (°C)" }) { v ->
                SettingsStore.update { it.copy(weatherFahrenheit = v) }
            }
        }
        WeatherAttribution()
    }
}

/** Open-Meteo's data (and the GeoNames data behind its city search) is CC BY 4.0, which asks for credit. */
@Composable
private fun WeatherAttribution() {
    val context = LocalContext.current
    Text(
        "Weather data by Open-Meteo.com (CC BY 4.0). City search based on GeoNames.",
        style = MaterialTheme.typography.bodySmall,
        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 12.dp)
            .clickable { context.openUrl("https://open-meteo.com/") }
            .padding(horizontal = 32.dp, vertical = 8.dp),
    )
}

@Composable
private fun CitySearch(s: Settings) {
    val scope = rememberCoroutineScope()
    var query by remember { mutableStateOf("") }
    var status by remember { mutableStateOf<String?>(null) }
    var busy by remember { mutableStateOf(false) }

    Column(Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 6.dp)) {
        Text(
            if (s.weatherCity.isNotBlank()) "City: ${s.weatherCity}" else "City: not set",
            style = MaterialTheme.typography.bodyLarge,
        )
        if (s.weatherUseDeviceLocation) {
            Text(
                "Used when your location isn't available",
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
                placeholder = { Text("Search a city") },
                modifier = Modifier.weight(1f),
            )
            Button(
                enabled = query.isNotBlank() && !busy,
                onClick = {
                    busy = true
                    status = null
                    scope.launch {
                        val place = runCatching { WeatherRepository.geocode(query, Locale.getDefault().language) }.getOrNull()
                        busy = false
                        if (place == null) {
                            status = "No match (or no connection)"
                        } else {
                            SettingsStore.update { it.copy(weatherCity = place.name, weatherLat = place.lat, weatherLon = place.lon) }
                            WeatherRepository.invalidate()
                            query = ""
                        }
                    }
                },
            ) { Text(if (busy) "…" else "Find") }
        }
        status?.let { Text(it, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.error) }
    }
}
