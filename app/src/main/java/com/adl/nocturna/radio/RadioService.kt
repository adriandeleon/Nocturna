package com.adl.nocturna.radio

import android.content.Intent
import androidx.media3.common.AudioAttributes
import androidx.media3.common.C
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.session.MediaSession
import androidx.media3.session.MediaSessionService
import com.google.common.util.concurrent.Futures
import com.google.common.util.concurrent.ListenableFuture
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

/** Plays internet radio streams and the built-in sleep sounds in the background with a media notification. */
class RadioService : MediaSessionService() {
    private var session: MediaSession? = null
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main)

    override fun onCreate() {
        super.onCreate()
        val player = ExoPlayer.Builder(this)
            .setAudioAttributes(
                AudioAttributes.Builder().setUsage(C.USAGE_MEDIA).setContentType(C.AUDIO_CONTENT_TYPE_MUSIC).build(),
                /* handleAudioFocus = */ true,
            )
            .setWakeMode(C.WAKE_MODE_NETWORK)
            .setHandleAudioBecomingNoisy(true)
            .build()
        player.addListener(object : Player.Listener {
            override fun onEvents(player: Player, events: Player.Events) {
                val on = player.playWhenReady && player.playbackState != Player.STATE_IDLE &&
                    player.playbackState != Player.STATE_ENDED
                playing.value = if (on) player.mediaMetadata.title?.toString().orEmpty() else null
            }
        })

        session = MediaSession.Builder(this, player)
            .setCallback(object : MediaSession.Callback {
                // Controllers send only request metadata across the binder; rebuild playable items from it.
                // Sleep sounds become their cached loop, repeated seamlessly; streams play once.
                override fun onAddMediaItems(
                    mediaSession: MediaSession,
                    controller: MediaSession.ControllerInfo,
                    mediaItems: MutableList<MediaItem>,
                ): ListenableFuture<MutableList<MediaItem>> {
                    val loops = mediaItems.any(SleepSoundFiles::isSleepSound)
                    mediaSession.player.repeatMode = if (loops) Player.REPEAT_MODE_ONE else Player.REPEAT_MODE_OFF
                    return if (loops) SleepSoundFiles.playable(this@RadioService, mediaItems) else Futures.immediateFuture(
                        mediaItems.map { it.buildUpon().setUri(it.requestMetadata.mediaUri).build() }.toMutableList()
                    )
                }
            })
            .build()

        scope.launch {
            Radio.sleepAt.collectLatest { at ->
                player.volume = 1f
                if (at == null) return@collectLatest
                delay((at - FADE_MS - System.currentTimeMillis()).coerceAtLeast(0))
                try {
                    val steps = 40
                    for (i in steps downTo 0) {
                        player.volume = i / steps.toFloat()
                        delay(FADE_MS / steps)
                    }
                    player.stop()
                    player.clearMediaItems()
                } finally {
                    player.volume = 1f
                }
                Radio.sleepAt.value = null
            }
        }
    }

    override fun onGetSession(controllerInfo: MediaSession.ControllerInfo): MediaSession? = session

    override fun onTaskRemoved(rootIntent: Intent?) {
        val p = session?.player
        if (p == null || !p.playWhenReady || p.mediaItemCount == 0) stopSelf()
    }

    override fun onDestroy() {
        playing.value = null
        scope.cancel()
        session?.run {
            player.release()
            release()
        }
        session = null
        super.onDestroy()
    }

    companion object {
        private const val FADE_MS = 15_000L
        private val playing = MutableStateFlow<String?>(null)

        /**
         * The station playing or buffering, null when the radio is off. Read from the player itself, so the
         * tile and the widget see the real state without connecting a controller.
         */
        val nowPlaying: StateFlow<String?> = playing.asStateFlow()
    }
}
