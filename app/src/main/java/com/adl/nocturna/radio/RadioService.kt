package com.adl.nocturna.radio

import android.content.Intent
import androidx.annotation.OptIn
import androidx.media3.common.AudioAttributes
import androidx.media3.common.C
import androidx.media3.common.MediaItem
import androidx.media3.common.Metadata
import androidx.media3.common.Player
import androidx.media3.common.util.UnstableApi
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.extractor.metadata.icy.IcyInfo
import com.adl.nocturna.data.SettingsStore
import androidx.media3.session.MediaSession
import androidx.media3.session.MediaSessionService
import com.google.common.util.concurrent.Futures
import com.google.common.util.concurrent.ListenableFuture
import com.google.common.util.concurrent.MoreExecutors
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch

/**
 * Plays internet radio streams, the built-in sleep sounds and music on the phone in the background with a
 * media notification.
 */
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
                if (!on || events.contains(Player.EVENT_MEDIA_ITEM_TRANSITION)) songState.value = null
            }

            // Internet radio sends the song as ICY "StreamTitle" metadata; the item's own title stays the station's.
            // Metadata entries and IcyInfo are still marked unstable, though they have been there since Media3 1.0.
            @OptIn(UnstableApi::class)
            override fun onMetadata(metadata: Metadata) {
                for (i in 0 until metadata.length()) {
                    val icy = metadata[i] as? IcyInfo ?: continue
                    songState.value = cleanStreamTitle(icy.title, player.mediaMetadata.title?.toString())
                }
            }
        })

        session = MediaSession.Builder(this, player)
            .setCallback(object : MediaSession.Callback {
                override fun onAddMediaItems(
                    mediaSession: MediaSession,
                    controller: MediaSession.ControllerInfo,
                    mediaItems: MutableList<MediaItem>,
                ): ListenableFuture<MutableList<MediaItem>> = resolve(mediaSession.player, mediaItems)

                // A folder on the phone becomes all its tracks, so the list that plays can be longer than the one asked for.
                // MediaItemsWithStartPosition is still marked unstable, though onSetMediaItems is built on it.
                @OptIn(UnstableApi::class)
                override fun onSetMediaItems(
                    mediaSession: MediaSession,
                    controller: MediaSession.ControllerInfo,
                    mediaItems: MutableList<MediaItem>,
                    startIndex: Int,
                    startPositionMs: Long,
                ): ListenableFuture<MediaSession.MediaItemsWithStartPosition> = Futures.transform(
                    resolve(mediaSession.player, mediaItems),
                    { items ->
                        val same = items.size == mediaItems.size
                        MediaSession.MediaItemsWithStartPosition(
                            items, if (same) startIndex else 0, if (same) startPositionMs else C.TIME_UNSET,
                        )
                    },
                    MoreExecutors.directExecutor(),
                )
            })
            .build()

        // The clock radio's volume, and the sleep timer's fade from it down to silence.
        val gain = SettingsStore.settings.map { radioGain(it.radioVolume) }.distinctUntilChanged()
        scope.launch {
            combine(Radio.sleepAt, gain) { at, g -> at to g }.collectLatest { (at, g) ->
                player.volume = g
                if (at == null) return@collectLatest
                delay((at - FADE_MS - System.currentTimeMillis()).coerceAtLeast(0))
                try {
                    val steps = 40
                    for (i in steps downTo 0) {
                        player.volume = g * i / steps
                        delay(FADE_MS / steps)
                    }
                    player.stop()
                    player.clearMediaItems()
                } finally {
                    player.volume = g
                }
                Radio.sleepAt.value = null
            }
        }
    }

    /**
     * Controllers send only request metadata across the binder; rebuild playable items from it. Sleep sounds
     * become their cached loop, repeated seamlessly; music on the phone repeats, a folder from its first track
     * again; streams play once.
     */
    private fun resolve(player: Player, items: List<MediaItem>): ListenableFuture<MutableList<MediaItem>> = when {
        items.any(SleepSoundFiles::isSleepSound) -> {
            player.repeatMode = Player.REPEAT_MODE_ONE
            SleepSoundFiles.playable(this, items)
        }
        items.any(PhoneMusicFiles::isOnPhone) -> {
            player.repeatMode = Player.REPEAT_MODE_ALL
            PhoneMusicFiles.playable(this, items)
        }
        else -> {
            player.repeatMode = Player.REPEAT_MODE_OFF
            Futures.immediateFuture(items.map { it.buildUpon().setUri(it.requestMetadata.mediaUri).build() }.toMutableList())
        }
    }

    override fun onGetSession(controllerInfo: MediaSession.ControllerInfo): MediaSession? = session

    override fun onTaskRemoved(rootIntent: Intent?) {
        val p = session?.player
        if (p == null || !p.playWhenReady || p.mediaItemCount == 0) stopSelf()
    }

    override fun onDestroy() {
        playing.value = null
        songState.value = null
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

        private val songState = MutableStateFlow<String?>(null)

        /** The song the station says is playing, tidied by [cleanStreamTitle]; null when it doesn't say. */
        val song: StateFlow<String?> = songState.asStateFlow()
    }
}
