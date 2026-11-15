package com.adl.nocturna.day

import android.app.NotificationManager
import android.content.Context
import android.media.AudioManager
import android.media.ToneGenerator
import android.os.Handler
import android.os.Looper

/**
 * Day mode's soft chime before a meeting: one short, low tone on the notification stream, so it
 * follows the notification volume and never the alarm's. It stays quiet when the phone is on silent
 * or vibrate, or in Do Not Disturb.
 */
object MeetingChime {
    /** ToneGenerator's volume, 0..100 of the notification volume: well under it. */
    private const val VOLUME = 35
    private const val DURATION_MS = 300

    fun play(context: Context) {
        val audio = context.getSystemService(AudioManager::class.java) ?: return
        if (audio.ringerMode != AudioManager.RINGER_MODE_NORMAL) return
        val notifications = context.getSystemService(NotificationManager::class.java)
        if (notifications != null && notifications.currentInterruptionFilter > NotificationManager.INTERRUPTION_FILTER_ALL) return
        // The constructor throws when the audio system can't give it a track.
        val tone = runCatching { ToneGenerator(AudioManager.STREAM_NOTIFICATION, VOLUME) }.getOrNull() ?: return
        tone.startTone(ToneGenerator.TONE_PROP_PROMPT, DURATION_MS)
        Handler(Looper.getMainLooper()).postDelayed({ tone.release() }, DURATION_MS + 200L)
    }
}
