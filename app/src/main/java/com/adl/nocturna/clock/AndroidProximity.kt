package com.adl.nocturna.clock

import android.content.Context
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.util.Log
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlin.time.Duration.Companion.nanoseconds

/**
 * The phone's proximity sensor for wave to snooze and wave to peek. The sensor is registered only while
 * [readings] is collected, which the shared [WaveEffect] does only while its screen shows and is started.
 * Registering and unregistering are logged under [TAG], so `adb logcat -s NocturnaWave` shows when it's on.
 */
class AndroidProximity private constructor(private val sensors: SensorManager, private val sensor: Sensor) : Proximity {
    override fun readings(): Flow<ProximityReading> = callbackFlow {
        val listener = object : SensorEventListener {
            override fun onSensorChanged(event: SensorEvent) {
                // The event's time is on the monotonic clock (elapsed realtime), in nanoseconds.
                trySend(ProximityReading(event.timestamp.nanoseconds, proximityIsNear(event.values[0], sensor.maximumRange)))
            }

            override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) = Unit
        }
        val registered = sensors.registerListener(listener, sensor, SensorManager.SENSOR_DELAY_NORMAL)
        Log.d(TAG, "Proximity sensor registered: $registered")
        awaitClose {
            sensors.unregisterListener(listener)
            Log.d(TAG, "Proximity sensor unregistered")
        }
    }

    companion object {
        const val TAG = "NocturnaWave"

        /** The phone's proximity sensor, or null when it has none. */
        fun of(context: Context): AndroidProximity? {
            val sensors = context.getSystemService(SensorManager::class.java) ?: return null
            val sensor = sensors.getDefaultSensor(Sensor.TYPE_PROXIMITY) ?: return null
            return AndroidProximity(sensors, sensor)
        }
    }
}
