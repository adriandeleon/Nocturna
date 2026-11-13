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
import android.os.Build
import android.os.Handler
import android.os.IBinder
import android.os.Looper
import android.os.PowerManager
import android.os.SystemClock
import android.os.VibrationAttributes
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import androidx.core.app.NotificationCompat
import androidx.core.app.ServiceCompat
import androidx.core.content.ContextCompat
import androidx.core.net.toUri
import androidx.media3.common.C
import androidx.media3.common.MediaItem
import androidx.media3.common.PlaybackException
import androidx.media3.common.Player
import androidx.media3.exoplayer.ExoPlayer
import com.adl.nocturna.NocturnaApp
import com.adl.nocturna.R
import com.adl.nocturna.data.AlarmStore
import com.adl.nocturna.data.SettingsStore
import com.adl.nocturna.data.Station
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import java.time.LocalTime
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle

/**
 * What is ringing right now, if anything. [station] is the radio station playing, or null while the
 * alarm tone plays (chosen, or because the station failed).
 */
data class Ringing(val alarmId: Long, val label: String, val startedAt: Long, val station: Station? = null)

/**
 * Rings the alarm: plays the alarm's radio station, or the default alarm sound, on the alarm stream with
 * a gradual volume ramp, vibrates, and shows a full-screen notification that opens [AlarmActivity].
 * A station that fails, ends, or stays silent for [STREAM_TIMEOUT_MS] is replaced by the alarm sound.
 */
class AlarmService : Service() {
    private var tone: MediaPlayer? = null
    private var stream: ExoPlayer? = null
    private var volume = 1f
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
        val alarm = AlarmStore.get(alarmId)
        val label = alarm?.label.orEmpty()
        val station = alarm?.station
        ringingState.value = Ringing(alarmId, label, System.currentTimeMillis(), station)

        ServiceCompat.startForeground(
            this, NOTIFICATION_ID, buildNotification(label, station),
            ServiceInfo.FOREGROUND_SERVICE_TYPE_MEDIA_PLAYBACK,
        )
        wakeLock?.takeIf { it.isHeld }?.release()
        wakeLock = getSystemService(PowerManager::class.java)
            .newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, "nocturna:alarm")
            .apply { acquire(MAX_RING_MS + 10_000) }

        // When Nocturna is the visible app, Android shows full-screen intents only as a heads-up
        // notification, so open the wake-up screen ourselves. (Blocked harmlessly when in background.)
        runCatching { startActivity(Intent(this, AlarmActivity::class.java).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)) }

        startSound(station)
        if (SettingsStore.current.alarmVibrate) startVibration()
        handler.postDelayed({ finishRinging() }, MAX_RING_MS)
    }

    private fun buildNotification(label: String, station: Station?): Notification {
        val fullScreen = PendingIntent.getActivity(
            this, 0, Intent(this, AlarmActivity::class.java).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK),
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT,
        )
        val time = LocalTime.now().format(DateTimeFormatter.ofLocalizedTime(FormatStyle.SHORT))
        return NotificationCompat.Builder(this, NocturnaApp.CHANNEL_ALARM)
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setContentTitle(label.ifBlank { "Alarm" })
            .setContentText(listOfNotNull(time, station?.name).joinToString(" · "))
            .setCategory(NotificationCompat.CATEGORY_ALARM)
            .setPriority(NotificationCompat.PRIORITY_MAX)
            .setOngoing(true)
            .setFullScreenIntent(fullScreen, true)
            .setContentIntent(fullScreen)
            .addAction(0, "Snooze", serviceIntent(this, ACTION_SNOOZE))
            .addAction(0, "Dismiss", serviceIntent(this, ACTION_DISMISS))
            .build()
    }

    private fun startSound(station: Station?) {
        // Ask other audio (e.g. the radio) to pause while we ring.
        val am = getSystemService(AudioManager::class.java)
        focusRequest = AudioFocusRequest.Builder(AudioManager.AUDIOFOCUS_GAIN_TRANSIENT)
            .setAudioAttributes(toneAttributes()).build().also { am.requestAudioFocus(it) }

        val rampMs = SettingsStore.current.alarmRampSeconds * 1000L
        volume = if (rampMs > 0) START_VOLUME else 1f
        if (station == null) {
            playTone()
        } else {
            runCatching { playStation(station) }.onFailure {
                releaseStream()
                ringingState.update { r -> r?.copy(station = null) }
                playTone()
            }
        }

        if (rampMs > 0) {
            val started = SystemClock.elapsedRealtime()
            handler.post(object : Runnable {
                override fun run() {
                    if (tone == null && stream == null) return
                    val f = ((SystemClock.elapsedRealtime() - started).toFloat() / rampMs).coerceIn(0f, 1f)
                    // Perceived loudness is roughly logarithmic, so ramp on a curve.
                    setVolume(START_VOLUME + (1f - START_VOLUME) * f * f)
                    if (f < 1f) handler.postDelayed(this, 250)
                }
            })
        }
    }

    /** Sets the volume of whichever player is ringing; one that takes over later starts at the same level. */
    private fun setVolume(v: Float) {
        volume = v
        tone?.setVolume(v, v)
        stream?.volume = v
    }

    private fun toneAttributes(): AudioAttributes = AudioAttributes.Builder()
        .setUsage(AudioAttributes.USAGE_ALARM)
        .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
        .build()

    private fun playTone() {
        val attrs = toneAttributes()
        // Try the system alarm sound first. Its settings URI is served from the system's own cached
        // copy, so it plays even when the user picked a tone from storage we aren't allowed to read.
        // If nothing else works, ring with the chime bundled in the app: an alarm must never be silent.
        val candidates = listOfNotNull(
            RingtoneManager.getDefaultUri(RingtoneManager.TYPE_ALARM),
            RingtoneManager.getActualDefaultRingtoneUri(this, RingtoneManager.TYPE_ALARM),
            "android.resource://$packageName/${R.raw.alarm_fallback}".toUri(),
        )
        tone = candidates.firstNotNullOfOrNull { uri ->
            val mp = MediaPlayer()
            runCatching {
                mp.setAudioAttributes(attrs)
                mp.setDataSource(this, uri)
                mp.isLooping = true
                mp.setVolume(volume, volume)
                mp.prepare()
                mp.start()
                mp
            }.onFailure { mp.release() }.getOrNull()
        }
    }

    /**
     * Streams [station] on the alarm stream (so it follows the alarm volume and Do Not Disturb's alarm
     * rules). Focus is ours already, so the player doesn't handle it. Errors, the stream ending, and
     * silence (not started yet, or stalled) all hand over to [playTone].
     */
    private fun playStation(station: Station) {
        val p = ExoPlayer.Builder(this)
            .setAudioAttributes(
                androidx.media3.common.AudioAttributes.Builder()
                    .setUsage(C.USAGE_ALARM).setContentType(C.AUDIO_CONTENT_TYPE_MUSIC).build(),
                /* handleAudioFocus = */ false,
            )
            .setWakeMode(C.WAKE_MODE_NETWORK)
            .build()
        p.addListener(object : Player.Listener {
            // Post, so the player isn't released from inside its own callback.
            override fun onPlayerError(error: PlaybackException) {
                handler.post { fallBackToTone(p) }
            }

            override fun onPlaybackStateChanged(state: Int) {
                if (state == Player.STATE_ENDED) handler.post { fallBackToTone(p) }
            }
        })
        p.volume = volume
        p.setMediaItem(MediaItem.fromUri(station.url))
        p.prepare()
        p.play()
        stream = p

        val watchdog = SilenceWatchdog(STREAM_TIMEOUT_MS, SystemClock.elapsedRealtime())
        handler.post(object : Runnable {
            override fun run() {
                if (stream !== p) return
                val now = SystemClock.elapsedRealtime()
                watchdog.update(p.isPlaying, now)
                if (watchdog.expired(now)) fallBackToTone(p) else handler.postDelayed(this, 1000)
            }
        })
    }

    private fun fallBackToTone(failed: ExoPlayer) {
        if (stream !== failed) return // already replaced, or the alarm stopped
        releaseStream()
        ringingState.update { it?.copy(station = null) }
        playTone()
    }

    private fun releaseStream() {
        stream?.runCatching { stop(); release() }
        stream = null
    }

    private fun vibrator(): Vibrator = if (Build.VERSION.SDK_INT >= 31) {
        getSystemService(VibratorManager::class.java).defaultVibrator
    } else {
        @Suppress("DEPRECATION") getSystemService(Vibrator::class.java)
    }

    private fun startVibration() {
        val effect = VibrationEffect.createWaveform(longArrayOf(0, 600, 900), 0)
        // Mark it as alarm vibration so "alarm vibration" / Do Not Disturb settings treat it as one.
        if (Build.VERSION.SDK_INT >= 33) {
            vibrator().vibrate(effect, VibrationAttributes.createForUsage(VibrationAttributes.USAGE_ALARM))
        } else {
            @Suppress("DEPRECATION")
            vibrator().vibrate(effect, AudioAttributes.Builder().setUsage(AudioAttributes.USAGE_ALARM).build())
        }
    }

    private fun stopSound() {
        handler.removeCallbacksAndMessages(null)
        tone?.runCatching { stop(); release() }
        tone = null
        releaseStream()
        focusRequest?.let { getSystemService(AudioManager::class.java).abandonAudioFocusRequest(it) }
        focusRequest = null
        vibrator().cancel()
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
        /** How long a radio alarm may stay silent before the alarm tone takes over. */
        const val STREAM_TIMEOUT_MS = 15_000L
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
