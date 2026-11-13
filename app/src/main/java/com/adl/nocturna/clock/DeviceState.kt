package com.adl.nocturna.clock

import android.app.AlarmManager
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.os.BatteryManager
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.State
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
import androidx.core.content.ContextCompat
import kotlinx.coroutines.delay
import java.time.ZonedDateTime
import kotlin.math.abs

/** Current time, updated exactly on each second boundary. */
@Composable
fun rememberNow(): State<ZonedDateTime> = produceState(ZonedDateTime.now()) {
    while (true) {
        val now = ZonedDateTime.now()
        value = now
        delay(1000L - now.nano / 1_000_000 + 5)
    }
}

/**
 * Smoothed ambient light in lux, or null when the device has no light sensor.
 * Only publishes changes larger than ~10% so the clock doesn't recompose on every sensor event.
 */
@Composable
fun rememberAmbientLux(): State<Float?> {
    val context = LocalContext.current
    val state = remember { mutableStateOf<Float?>(null) }
    DisposableEffect(context) {
        val sm = context.getSystemService(SensorManager::class.java)
        val sensor = sm?.getDefaultSensor(Sensor.TYPE_LIGHT)
        if (sensor == null) {
            state.value = null
            return@DisposableEffect onDispose { }
        }
        var smoothed = -1f
        val listener = object : SensorEventListener {
            override fun onSensorChanged(event: SensorEvent) {
                val lux = event.values[0].coerceAtLeast(0f)
                smoothed = if (smoothed < 0) lux else smoothed + (lux - smoothed) * 0.25f
                val prev = state.value
                if (prev == null || abs(smoothed - prev) > 0.1f * prev + 0.3f) state.value = smoothed
            }

            override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) = Unit
        }
        sm.registerListener(listener, sensor, SensorManager.SENSOR_DELAY_NORMAL)
        onDispose { sm.unregisterListener(listener) }
    }
    return state
}

data class BatteryInfo(val level: Float, val charging: Boolean)

@Composable
fun rememberBattery(): State<BatteryInfo?> {
    val context = LocalContext.current
    val state = remember { mutableStateOf<BatteryInfo?>(null) }
    DisposableEffect(context) {
        fun read(intent: Intent?) {
            intent ?: return
            val level = intent.getIntExtra(BatteryManager.EXTRA_LEVEL, -1)
            val scale = intent.getIntExtra(BatteryManager.EXTRA_SCALE, 100)
            val status = intent.getIntExtra(BatteryManager.EXTRA_STATUS, -1)
            if (level >= 0) {
                state.value = BatteryInfo(
                    level / scale.toFloat(),
                    status == BatteryManager.BATTERY_STATUS_CHARGING || status == BatteryManager.BATTERY_STATUS_FULL,
                )
            }
        }
        val receiver = object : BroadcastReceiver() {
            override fun onReceive(c: Context, intent: Intent) = read(intent)
        }
        read(ContextCompat.registerReceiver(context, receiver, IntentFilter(Intent.ACTION_BATTERY_CHANGED), ContextCompat.RECEIVER_NOT_EXPORTED))
        onDispose { context.unregisterReceiver(receiver) }
    }
    return state
}

/** Trigger time of the next alarm set on the device (by any app), in epoch millis. */
@Composable
fun rememberNextAlarm(): State<Long?> {
    val context = LocalContext.current
    val state = remember { mutableStateOf<Long?>(null) }
    DisposableEffect(context) {
        val am = context.getSystemService(AlarmManager::class.java)
        fun read() {
            state.value = am.nextAlarmClock?.triggerTime
        }
        val receiver = object : BroadcastReceiver() {
            override fun onReceive(c: Context, intent: Intent) = read()
        }
        ContextCompat.registerReceiver(
            context, receiver, IntentFilter(AlarmManager.ACTION_NEXT_ALARM_CLOCK_CHANGED), ContextCompat.RECEIVER_EXPORTED,
        )
        read()
        onDispose { context.unregisterReceiver(receiver) }
    }
    return state
}
