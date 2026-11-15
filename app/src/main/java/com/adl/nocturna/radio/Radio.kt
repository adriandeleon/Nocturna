package com.adl.nocturna.radio

import android.content.ComponentName
import android.content.Context
import androidx.core.content.ContextCompat
import androidx.core.net.toUri
import androidx.media3.common.MediaItem
import androidx.media3.common.MediaMetadata
import androidx.media3.common.PlaybackException
import androidx.media3.common.Player
import androidx.media3.session.MediaController
import androidx.media3.session.SessionToken
import com.adl.nocturna.R
import com.adl.nocturna.data.Station
import com.adl.nocturna.sound.displayName
import java.util.Locale
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/** App-side handle on [RadioService]: play/stop a station and run the sleep timer. */
object Radio {
    private var controller: MediaController? = null
    private var connecting = false
    private val pending = mutableListOf<(MediaController) -> Unit>()

    /** For the error messages' text; set on the first [connect]. */
    private var appContext: Context? = null

    private val onState = MutableStateFlow(false)
    private val titleState = MutableStateFlow<String?>(null)
    private val errorState = MutableStateFlow<String?>(null)

    /** True while a stream is playing or buffering. */
    val isOn: StateFlow<Boolean> = onState.asStateFlow()
    val title: StateFlow<String?> = titleState.asStateFlow()
    val error: StateFlow<String?> = errorState.asStateFlow()

    /** Epoch millis at which playback fades out and stops, or null. */
    val sleepAt = MutableStateFlow<Long?>(null)

    /** Sleep timer asked for with [play], started once the stream is on (the timer needs a stream to stop). */
    private var pendingSleepMinutes = 0

    private val listener = object : Player.Listener {
        override fun onEvents(player: Player, events: Player.Events) = sync(player)
        override fun onPlayerError(error: PlaybackException) {
            errorState.value = appContext?.getString(R.string.radio_cant_play, error.errorCodeName)
        }
    }

    private fun sync(p: Player) {
        val on = p.playWhenReady && p.playbackState != Player.STATE_IDLE && p.playbackState != Player.STATE_ENDED
        onState.value = on
        titleState.value = if (on) p.mediaMetadata.title?.toString() else null
        if (!on) sleepAt.value = null
        if (on && pendingSleepMinutes > 0) {
            sleepAt.value = System.currentTimeMillis() + pendingSleepMinutes * 60_000L
            pendingSleepMinutes = 0
        }
    }

    fun connect(context: Context, then: (MediaController) -> Unit = {}) {
        controller?.takeIf { it.isConnected }?.let { then(it); return }
        pending += then
        if (connecting) return
        connecting = true
        val app = context.applicationContext
        appContext = app
        val future = MediaController.Builder(app, SessionToken(app, ComponentName(app, RadioService::class.java))).buildAsync()
        future.addListener({
            connecting = false
            val c = runCatching { future.get() }.getOrNull()
            if (c == null) {
                pending.clear()
                errorState.value = app.getString(R.string.radio_unavailable)
                return@addListener
            }
            controller = c
            c.addListener(listener)
            sync(c)
            val callbacks = pending.toList()
            pending.clear()
            callbacks.forEach { it(c) }
        }, ContextCompat.getMainExecutor(app))
    }

    /** Plays [station]; with [sleepMinutes] it fades out and stops after that long. */
    fun play(context: Context, station: Station, sleepMinutes: Int = 0) {
        errorState.value = null
        pendingSleepMinutes = sleepMinutes
        connect(context) { c ->
            val item = MediaItem.Builder()
                .setMediaId(station.url)
                .setRequestMetadata(MediaItem.RequestMetadata.Builder().setMediaUri(station.url.toUri()).build())
                .setMediaMetadata(MediaMetadata.Builder().setTitle(station.displayName(Locale.getDefault())).setArtist("Nocturna").build())
                .build()
            c.setMediaItem(item)
            c.prepare()
            c.play()
        }
    }

    fun stop(context: Context) {
        sleepAt.value = null
        pendingSleepMinutes = 0
        connect(context) { c ->
            c.stop()
            c.clearMediaItems()
        }
    }

    fun setSleepTimer(minutes: Int) {
        sleepAt.value = if (minutes > 0 && onState.value) System.currentTimeMillis() + minutes * 60_000L else null
    }
}
