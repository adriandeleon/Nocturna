package com.adl.nocturna.day

import android.content.Context
import android.media.AudioAttributes
import android.media.AudioFormat
import android.media.AudioTrack
import com.adl.nocturna.sound.ReminderSoundSynth
import com.adl.nocturna.sound.Wav
import java.util.concurrent.ConcurrentHashMap
import kotlin.concurrent.thread

/**
 * Day mode's reminder sounds, footsteps and drops of water from [ReminderSoundSynth], played on the
 * notification stream with the meeting chime's rules ([MeetingChime.mayPlay]): at the notification volume,
 * never the alarm's, and not on silent, vibrate or Do Not Disturb.
 */
object ReminderSound {
    /** Each sound is made once per process: it's the same every time. */
    private val made = ConcurrentHashMap<Set<ReminderKind>, ShortArray>()

    fun play(context: Context, kinds: Set<ReminderKind>) {
        if (kinds.isEmpty() || !MeetingChime.mayPlay(context)) return
        thread(name = "ReminderSound") {
            val pcm = made.getOrPut(kinds) {
                ReminderSoundSynth.render(kinds).let { s -> ShortArray(s.size) { Wav.toPcm16(s[it]).toShort() } }
            }
            // AudioTrack throws when the audio system can't give it a track; then there's no sound.
            runCatching { playOnce(pcm) }
        }
    }

    private fun playOnce(pcm: ShortArray) {
        val track = AudioTrack.Builder()
            .setAudioAttributes(
                AudioAttributes.Builder()
                    .setUsage(AudioAttributes.USAGE_NOTIFICATION)
                    .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                    .build(),
            )
            .setAudioFormat(
                AudioFormat.Builder()
                    .setEncoding(AudioFormat.ENCODING_PCM_16BIT)
                    .setSampleRate(ReminderSoundSynth.SAMPLE_RATE)
                    .setChannelMask(AudioFormat.CHANNEL_OUT_MONO)
                    .build(),
            )
            .setTransferMode(AudioTrack.MODE_STATIC)
            .setBufferSizeInBytes(pcm.size * 2)
            .build()
        try {
            track.write(pcm, 0, pcm.size)
            track.play()
            Thread.sleep(pcm.size * 1000L / ReminderSoundSynth.SAMPLE_RATE + 200L)
            track.stop()
        } finally {
            track.release()
        }
    }
}
