package com.adl.nocturna.alarm

import android.content.Context
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.os.Bundle
import android.view.WindowManager
import androidx.activity.ComponentActivity
import androidx.activity.addCallback
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.lifecycleScope
import com.adl.nocturna.clock.DigitalClock
import com.adl.nocturna.clock.rememberNow
import com.adl.nocturna.data.SettingsStore
import com.adl.nocturna.radio.Radio
import com.adl.nocturna.ui.GlyphIcon
import com.adl.nocturna.ui.NocturnaTheme
import com.adl.nocturna.ui.enableDarkEdgeToEdge
import com.adl.nocturna.weather.DeviceLocation
import com.adl.nocturna.weather.WeatherRepository
import com.adl.nocturna.weather.wakeWeather
import com.adl.nocturna.weather.wakeWeatherNeedsRefresh
import kotlinx.coroutines.launch
import kotlin.time.Clock

/**
 * Full-screen "wake up" screen with big Snooze and Dismiss buttons. Shows over the lock screen.
 * While a radio alarm plays, "Keep listening" dismisses the alarm and carries on with the station in the
 * normal radio player. Under the time it shows today's weather, from the cache at once and refreshed in the
 * background when that is old; the ringing itself is [AlarmService]'s and never waits for it.
 */
class AlarmActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableDarkEdgeToEdge()
        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        window.attributes = window.attributes.apply { screenBrightness = 1f }
        // Back would leave the alarm ringing out of sight; treat it as snooze.
        onBackPressedDispatcher.addCallback(this) { AlarmService.send(this@AlarmActivity, AlarmService.ACTION_SNOOZE) }
        refreshWeather()

        setContent {
            NocturnaTheme {
                val ringing by AlarmService.ringing.collectAsStateWithLifecycle()
                LaunchedEffect(ringing) { if (ringing == null) finish() }
                val settings = SettingsStore.current
                val now by rememberNow()
                val accent = Color(settings.color)
                val weather by WeatherRepository.weather.collectAsStateWithLifecycle()
                val wake = wakeWeather(settings, weather, now.instant, now.zone)

                Column(
                    Modifier.fillMaxSize().background(Color.Black).safeDrawingPadding().padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center,
                ) {
                    DigitalClock(now, settings.use24h, false, settings.font, accent, 96.sp)
                    Spacer(Modifier.height(12.dp))
                    Text(
                        ringing?.label?.ifBlank { null } ?: "Alarm",
                        style = MaterialTheme.typography.headlineSmall,
                        color = accent.copy(alpha = 0.8f),
                    )
                    ringing?.station?.let { station ->
                        Spacer(Modifier.height(4.dp))
                        Text(station.name, style = MaterialTheme.typography.bodyLarge, color = accent.copy(alpha = 0.6f))
                    }
                    wake?.let { w ->
                        Spacer(Modifier.height(12.dp))
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            val color = accent.copy(alpha = 0.7f)
                            GlyphIcon(w.glyph, color, 26.dp)
                            Spacer(Modifier.width(8.dp))
                            Text(w.text, style = MaterialTheme.typography.titleLarge, color = color, softWrap = false)
                        }
                    }
                    Spacer(Modifier.height(64.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(24.dp)) {
                        OutlinedButton(
                            onClick = { AlarmService.send(this@AlarmActivity, AlarmService.ACTION_SNOOZE) },
                            modifier = Modifier.size(width = 150.dp, height = 72.dp),
                            colors = ButtonDefaults.outlinedButtonColors(contentColor = accent),
                        ) { Text("Snooze ${settings.snoozeMinutes}m", fontSize = 18.sp) }
                        Button(
                            onClick = { AlarmService.send(this@AlarmActivity, AlarmService.ACTION_DISMISS) },
                            modifier = Modifier.size(width = 150.dp, height = 72.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = accent, contentColor = Color.Black),
                        ) { Text("Dismiss", fontSize = 18.sp) }
                    }
                    ringing?.station?.let { station ->
                        Spacer(Modifier.height(24.dp))
                        TextButton(
                            onClick = {
                                AlarmService.send(this@AlarmActivity, AlarmService.ACTION_DISMISS)
                                Radio.play(this@AlarmActivity, station)
                            },
                            colors = ButtonDefaults.textButtonColors(contentColor = accent),
                        ) { Text("Keep listening", fontSize = 18.sp) }
                    }
                }
            }
        }
    }

    /** Fetches the weather in the background when it is wanted, old or missing, and the phone is online. */
    private fun refreshWeather() {
        val settings = SettingsStore.current
        if (!wakeWeatherNeedsRefresh(settings, WeatherRepository.weather.value, Clock.System.now()) || !online()) return
        val app = applicationContext
        lifecycleScope.launch { WeatherRepository.refreshIfStale(settings) { DeviceLocation.get(app) } }
    }

    private fun online(): Boolean {
        val cm = getSystemService(Context.CONNECTIVITY_SERVICE) as? ConnectivityManager ?: return false
        val caps = runCatching { cm.getNetworkCapabilities(cm.activeNetwork) }.getOrNull() ?: return false
        return caps.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
    }
}
