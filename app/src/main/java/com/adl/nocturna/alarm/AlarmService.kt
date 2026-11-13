package com.adl.nocturna.alarm

import android.app.Notification
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.media.AudioAttributes
import android.media.AudioFocusRequest
import android.media.AudioManager
import android.media.MediaPlayer
import android.media.RingtoneManager
import android.os.Handler
import android.os.IBinder
import android.os.Looper
import android.os.PowerManager
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import androidx.core.app.NotificationCompat
import androidx.core.app.ServiceCompat
import androidx.core.content.ContextCompat
import com.adl.nocturna.NocturnaApp
import com.adl.nocturna.R
import com.adl.nocturna.data.AlarmStore
import com.adl.nocturna.data.SettingsStore
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.time.LocalTime
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle

/** What is ringing right now, if anything. */
data class Ringing(val alarmId: Long, val label: String, val startedAt: Long)

/**
 * Rings the alarm: plays the default alarm sound on the alarm stream with a gradual volume ramp,
 * vibrates, and shows a full-screen notification that opens [AlarmActivity].
 */
class AlarmService : Service() {
    private var player: MediaPlayer? = null
    private var focusRequest: AudioFocusRequest? = null
    private var wakeLock: PowerManager.WakeLock? = null
    private val handler = Handler(Looper.getMainLooper())

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            ACTION_SNOOZE -> snooze()
            ACTION_DISMISS -> finishRinging()
            else -> ring(intent?.getLongExtra(AlarmScheduler.EXTRA_ALARM_ID, 0L) ?: 0L)
        }
        return START_NOT_STICKY
    }

    private fun ring(alarmId: Long) {
        stopSound()
        val label = AlarmStore.get(alarmId)?.label.orEmpty()
        ringingState.value = Ringing(alarmId, label, System.currentTimeMillis())

        ServiceCompat.startForeground(
            this, NOTIFICATION_ID, buildNotification(label),
            ServiceInfo.FOREGROUND_SERVICE_TYPE_MEDIA_PLAYBACK,
        )
        wakeLock = getSystemService(PowerManager::class.java)
            .newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, "nocturna:alarm")
            .apply { acquire(MAX_RING_MS + 10_000) }

        // When Nocturna is the visible app, Android shows full-screen intents only as a heads-up
        // notification, so open the wake-up screen ourselves. (Blocked harmlessly when in background.)
        runCatching { startActivity(Intent(this, AlarmActivity::class.java).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)) }

        startSound()
        if (SettingsStore.current.alarmVibrate) startVibration()
        handler.postDelayed({ finishRinging() }, MAX_RING_MS)
    }

    private fun buildNotification(label: String): Notification {
        val fullScreen = PendingIntent.getActivity(
            this, 0, Intent(this, AlarmActivity::class.java).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK),
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT,
        )
        val time = LocalTime.now().format(DateTimeFormatter.ofLocalizedTime(FormatStyle.SHORT))
        return NotificationCompat.Builder(this, NocturnaApp.CHANNEL_ALARM)
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setContentTitle(label.ifBlank { "Alarm" })
            .setContentText(time)
            .setCategory(NotificationCompat.CATEGORY_ALARM)
            .setPriority(NotificationCompat.PRIORITY_MAX)
            .setOngoing(true)
            .setFullScreenIntent(fullScreen, true)
            .setContentIntent(fullScreen)
            .addAction(0, "Snooze", serviceIntent(this, ACTION_SNOOZE))
            .addAction(0, "Dismiss", serviceIntent(this, ACTION_DISMISS))
            .build()
    }

    private fun startSound() {
        val attrs = AudioAttributes.Builder()
            .setUsage(AudioAttributes.USAGE_ALARM)
            .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
            .build()
        // Ask other audio (e.g. the radio) to pause while we ring.
        val am = getSystemService(AudioManager::class.java)
        focusRequest = AudioFocusRequest.Builder(AudioManager.AUDIOFOCUS_GAIN_TRANSIENT)
            .setAudioAttributes(attrs).build().also { am.requestAudioFocus(it) }

        val uri = RingtoneManager.getActualDefaultRingtoneUri(this, RingtoneManager.TYPE_ALARM)
            ?: RingtoneManager.getDefaultUri(RingtoneManager.TYPE_ALARM)
            ?: RingtoneManager.getDefaultUri(RingtoneManager.TYPE_RINGTONE)
        player = runCatching {
            MediaPlayer().apply {
                setAudioAttributes(attrs)
                setDataSource(this@AlarmService, uri)
                isLooping = true
                setVolume(START_VOLUME, START_VOLUME)
                prepare()
                start()
            }
        }.getOrNull()

        val rampMs = SettingsStore.current.alarmRampSeconds * 1000L
        if (rampMs > 0) {
            val started = System.currentTimeMillis()
            handler.post(object : Runnable {
                override fun run() {
                    val p = player ?: return
                    val f = ((System.currentTimeMillis() - started).toFloat() / rampMs).coerceIn(0f, 1f)
                    // Perceived loudness is roughly logarithmic, so ramp on a curve.
                    val v = START_VOLUME + (1f - START_VOLUME) * f * f
                    p.setVolume(v, v)
                    if (f < 1f) handler.postDelayed(this, 250)
                }
            })
        } else {
            player?.setVolume(1f, 1f)
        }
    }

    private fun startVibration() {
        val vibrator = if (android.os.Build.VERSION.SDK_INT >= 31) {
            getSystemService(VibratorManager::class.java).defaultVibrator
        } else {
            @Suppress("DEPRECATION") getSystemService(Vibrator::class.java)
        }
        vibrator.vibrate(VibrationEffect.createWaveform(longArrayOf(0, 600, 900), 0))
    }

    private fun stopSound() {
        handler.removeCallbacksAndMessages(null)
        player?.runCatching { stop(); release() }
        player = null
        focusRequest?.let { getSystemService(AudioManager::class.java).abandonAudioFocusRequest(it) }
        focusRequest = null
        if (android.os.Build.VERSION.SDK_INT >= 31) {
            getSystemService(VibratorManager::class.java).cancel()
        } else {
            @Suppress("DEPRECATION") getSystemService(Vibrator::class.java).cancel()
        }
    }

    private fun snooze() {
        val r = ringingState.value
        if (r != null) {
            AlarmStore.setSnooze(r.alarmId, System.currentTimeMillis() + SettingsStore.current.snoozeMinutes * 60_000L)
        }
        finishRinging()
    }

    private fun finishRinging() {
        stopSound()
        ringingState.value = null
        wakeLock?.takeIf { it.isHeld }?.release()
        wakeLock = null
        ServiceCompat.stopForeground(this, ServiceCompat.STOP_FOREGROUND_REMOVE)
        stopSelf()
    }

    override fun onDestroy() {
        stopSound()
        ringingState.value = null
        wakeLock?.takeIf { it.isHeld }?.release()
        super.onDestroy()
    }

    companion object {
        private const val NOTIFICATION_ID = 42
        private const val START_VOLUME = 0.05f
        private const val MAX_RING_MS = 10 * 60_000L
        const val ACTION_SNOOZE = "com.adl.nocturna.SNOOZE"
        const val ACTION_DISMISS = "com.adl.nocturna.DISMISS"

        private val ringingState = MutableStateFlow<Ringing?>(null)
        val ringing: StateFlow<Ringing?> = ringingState.asStateFlow()

        fun start(context: Context, alarmId: Long) {
            ContextCompat.startForegroundService(
                context,
                Intent(context, AlarmService::class.java).putExtra(AlarmScheduler.EXTRA_ALARM_ID, alarmId),
            )
        }

        fun send(context: Context, action: String) {
            context.startService(Intent(context, AlarmService::class.java).setAction(action))
        }

        private fun serviceIntent(context: Context, action: String): PendingIntent =
            PendingIntent.getService(
                context, action.hashCode(),
                Intent(context, AlarmService::class.java).setAction(action),
                PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT,
            )
    }
}
