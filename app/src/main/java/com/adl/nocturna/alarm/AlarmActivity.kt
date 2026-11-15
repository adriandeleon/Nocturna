package com.adl.nocturna.alarm

import android.content.Context
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.os.Bundle
import android.view.WindowManager
import androidx.activity.ComponentActivity
import androidx.activity.addCallback
import androidx.activity.compose.setContent
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.lifecycleScope
import com.adl.nocturna.data.SettingsStore
import com.adl.nocturna.radio.Radio
import com.adl.nocturna.ui.NocturnaTheme
import com.adl.nocturna.ui.enableDarkEdgeToEdge
import com.adl.nocturna.weather.DeviceLocation
import com.adl.nocturna.weather.WeatherRepository
import com.adl.nocturna.weather.wakeWeatherNeedsRefresh
import kotlinx.coroutines.launch
import kotlin.time.Clock

/**
 * Full-screen "wake up" screen ([AlarmScreen]) with big Snooze and Dismiss buttons. Shows over the lock
 * screen. While a radio alarm plays, "Keep listening" dismisses the alarm and carries on with the station
 * in the normal radio player. The weather under the time is refreshed in the background when it is old;
 * the ringing itself is [AlarmService]'s and never waits for it.
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
                AlarmScreen(
                    label = ringing?.label.orEmpty(),
                    station = ringing?.station,
                    onSnooze = { AlarmService.send(this@AlarmActivity, AlarmService.ACTION_SNOOZE) },
                    onDismiss = { AlarmService.send(this@AlarmActivity, AlarmService.ACTION_DISMISS) },
                    onKeepListening = { station ->
                        AlarmService.send(this@AlarmActivity, AlarmService.ACTION_DISMISS)
                        Radio.play(this@AlarmActivity, station)
                    },
                )
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
