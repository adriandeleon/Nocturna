package com.adl.nocturna.radio

import android.content.ComponentName
import android.content.Context
import androidx.core.net.toUri
import androidx.core.content.ContextCompat
import androidx.media3.common.MediaItem
import androidx.media3.common.MediaMetadata
import androidx.media3.common.PlaybackException
import androidx.media3.common.Player
import androidx.media3.session.MediaController
import androidx.media3.session.SessionToken
import com.adl.nocturna.data.Station
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/** App-side handle on [RadioService]: play/stop a station and run the sleep timer. */
object Radio {
    private var controller: MediaController? = null
    private var connecting = false
    private val pending = mutableListOf<(MediaController) -> Unit>()

    private val onState = MutableStateFlow(false)
    private val titleState = MutableStateFlow<String?>(null)
    private val errorState = MutableStateFlow<String?>(null)

    /** True while a stream is playing or buffering. */
    val isOn: StateFlow<Boolean> = onState.asStateFlow()
    val title: StateFlow<String?> = titleState.asStateFlow()
    val error: StateFlow<String?> = errorState.asStateFlow()

    /** Epoch millis at which playback fades out and stops, or null. */
    val sleepAt = MutableStateFlow<Long?>(null)

    private val listener = object : Player.Listener {
        override fun onEvents(player: Player, events: Player.Events) = sync(player)
        override fun onPlayerError(error: PlaybackException) {
            errorState.value = "Can't play this station (${error.errorCodeName})"
        }
    }

    private fun sync(p: Player) {
        val on = p.playWhenReady && p.playbackState != Player.STATE_IDLE && p.playbackState != Player.STATE_ENDED
        onState.value = on
        titleState.value = if (on) p.mediaMetadata.title?.toString() else null
        if (!on) sleepAt.value = null
    }

    fun connect(context: Context, then: (MediaController) -> Unit = {}) {
        controller?.takeIf { it.isConnected }?.let { then(it); return }
        pending += then
        if (connecting) return
        connecting = true
        val app = context.applicationContext
        val future = MediaController.Builder(app, SessionToken(app, ComponentName(app, RadioService::class.java))).buildAsync()
        future.addListener({
            connecting = false
            val c = runCatching { future.get() }.getOrNull()
            if (c == null) {
                pending.clear()
                errorState.value = "Radio service unavailable"
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

    fun play(context: Context, station: Station) {
        errorState.value = null
        connect(context) { c ->
            val item = MediaItem.Builder()
                .setMediaId(station.url)
                .setRequestMetadata(MediaItem.RequestMetadata.Builder().setMediaUri(station.url.toUri()).build())
                .setMediaMetadata(MediaMetadata.Builder().setTitle(station.name).setArtist("Nocturna").build())
                .build()
            c.setMediaItem(item)
            c.prepare()
            c.play()
        }
    }

    fun stop(context: Context) {
        sleepAt.value = null
        connect(context) { c ->
            c.stop()
            c.clearMediaItems()
        }
    }

    fun toggle(context: Context, station: Station?) {
        if (onState.value) stop(context) else if (station != null) play(context, station)
    }

    fun setSleepTimer(minutes: Int) {
        sleepAt.value = if (minutes > 0 && onState.value) System.currentTimeMillis() + minutes * 60_000L else null
    }
}
