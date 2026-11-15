package com.adl.nocturna.radio

import android.content.Context
import android.net.Uri
import android.util.Log
import androidx.media3.common.MediaItem
import com.adl.nocturna.sound.SleepSound
import com.adl.nocturna.sound.SleepSoundSynth
import com.adl.nocturna.sound.Wav
import com.google.common.util.concurrent.ListenableFuture
import com.google.common.util.concurrent.MoreExecutors
import java.io.File
import java.io.IOException
import java.util.concurrent.Executors

/**
 * The built-in sleep sounds as WAV loops in the app's cache. Each is made the first time it plays (well under
 * a second on a phone) and kept until the system clears the cache, when it is simply made again.
 */
object SleepSoundFiles {
    /** Bump whenever [SleepSoundSynth] changes what it makes, so phones replace their cached loops. */
    private const val VERSION = 1
    private const val TAG = "SleepSoundFiles"

    // One thread, so two requests for the same sound never write the same file at once.
    private val executor = MoreExecutors.listeningDecorator(Executors.newSingleThreadExecutor())

    /**
     * [items] with each sleep sound's `nocturna-sound:` URI swapped for its cached file, made off the main
     * thread when needed. Web stations pass through unchanged. A sound that can't be made keeps its URI, so
     * the player reports it as a station that can't play.
     */
    fun playable(context: Context, items: List<MediaItem>): ListenableFuture<MutableList<MediaItem>> {
        val app = context.applicationContext
        return executor.submit<MutableList<MediaItem>> {
            items.map { item ->
                val uri = item.requestMetadata.mediaUri
                val sound = uri?.toString()?.let(SleepSound::ofUrl)
                val target = if (sound == null) uri else runCatching { Uri.fromFile(file(app, sound)) }
                    .onFailure { Log.w(TAG, "Couldn't make ${sound.id}", it) }
                    .getOrDefault(uri)
                item.buildUpon().setUri(target).build()
            }.toMutableList()
        }
    }

    fun isSleepSound(item: MediaItem): Boolean =
        item.requestMetadata.mediaUri?.toString()?.let(SleepSound::ofUrl) != null

    private fun file(context: Context, sound: SleepSound): File {
        val dir = File(context.cacheDir, "sleep-sounds")
        val file = File(dir, "${sound.id}-v$VERSION.wav")
        if (file.isFile) return file
        if (!dir.isDirectory && !dir.mkdirs()) throw IOException("Can't create $dir")
        dir.listFiles { f -> f.name.startsWith("${sound.id}-") }?.forEach { it.delete() } // older versions
        val tmp = File(dir, "${file.name}.tmp")
        tmp.writeBytes(Wav.encode(SleepSoundSynth.render(sound), SleepSoundSynth.SAMPLE_RATE))
        if (!tmp.renameTo(file)) throw IOException("Can't save $file")
        return file
    }
}
