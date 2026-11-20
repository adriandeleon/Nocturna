package com.adl.nocturna.day

import android.content.Context
import android.media.AudioAttributes
import android.media.AudioManager
import android.media.MediaPlayer
import android.os.Handler
import android.os.Looper
import com.adl.nocturna.R

/**
 * Day mode's reminder sounds: a few footsteps on concrete to stand up and stretch, water poured into a glass to
 * drink some. Short public-domain (CC0) recordings from Freesound, credited in `NOTICE` and on the About screen.
 * They play on the notification stream with the meeting chime's rules ([MeetingChime.mayPlay]): at the
 * notification volume, never the alarm's, and not on silent, vibrate or Do Not Disturb.
 */
object ReminderSound {
    /** Silence between the sounds of two reminders coming due together. */
    private const val GAP_MS = 250L

    private val attributes = AudioAttributes.Builder()
        .setUsage(AudioAttributes.USAGE_NOTIFICATION)
        .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
        .build()

    /** The players still sounding, held so they aren't collected before they finish. Main thread only. */
    private val playing = mutableSetOf<MediaPlayer>()

    private fun soundOf(kind: ReminderKind): Int = when (kind) {
        ReminderKind.STRETCH -> R.raw.reminder_steps
        ReminderKind.WATER -> R.raw.reminder_water
    }

    /** Called on the main thread, like the rest of the clock's sounds. */
    fun play(context: Context, kinds: Set<ReminderKind>) {
        if (!MeetingChime.mayPlay(context)) return
        playInTurn(context.applicationContext, reminderSoundOrder(kinds).map(::soundOf))
    }

    /** Plays [sounds] one after the other, [GAP_MS] apart, each player released as it finishes. */
    private fun playInTurn(context: Context, sounds: List<Int>) {
        val sound = sounds.firstOrNull() ?: return
        val rest = sounds.drop(1)
        val next = { Handler(Looper.getMainLooper()).postDelayed({ playInTurn(context, rest) }, GAP_MS) }
        // create() gives null, or throws, when the audio system can't play it; then that sound is skipped.
        val player = runCatching {
            MediaPlayer.create(context, sound, attributes, AudioManager.AUDIO_SESSION_ID_GENERATE)
        }.getOrNull()
        if (player == null) {
            next()
            return
        }
        val done = { mp: MediaPlayer ->
            playing -= mp
            mp.release()
            next()
        }
        player.setOnCompletionListener { done(it) }
        player.setOnErrorListener { mp, _, _ -> done(mp); true }
        playing += player
        player.start()
    }
}
