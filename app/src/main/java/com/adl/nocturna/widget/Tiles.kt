package com.adl.nocturna.widget

import android.annotation.SuppressLint
import android.app.PendingIntent
import android.content.Intent
import android.os.Build
import android.service.quicksettings.Tile
import android.service.quicksettings.TileService
import com.adl.nocturna.R
import com.adl.nocturna.clock.ClockActivity
import com.adl.nocturna.data.SettingsStore
import com.adl.nocturna.radio.RadioService
import com.adl.nocturna.sound.displayName
import java.util.Locale
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.MainScope
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.launch

/** Quick Settings tile *Nocturna*: opens the clock. */
class ClockTileService : TileService() {
    override fun onStartListening() {
        val tile = qsTile ?: return
        tile.state = Tile.STATE_INACTIVE
        tile.updateTile()
    }

    @SuppressLint("StartActivityAndCollapseDeprecated")
    override fun onClick() {
        val intent = Intent(this, ClockActivity::class.java).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
            startActivityAndCollapse(PendingIntent.getActivity(this, 0, intent, PendingIntent.FLAG_IMMUTABLE))
        } else {
            @Suppress("DEPRECATION")
            startActivityAndCollapse(intent)
        }
    }
}

/**
 * Quick Settings tile *Sleep radio*: shows the selected station and lights up while the radio plays.
 * A tap plays the station with a [SleepRadio.MINUTES]-minute sleep timer; another tap stops it.
 */
class SleepRadioTileService : TileService() {
    private var scope: CoroutineScope? = null

    override fun onStartListening() {
        scope?.cancel()
        scope = MainScope().apply {
            launch {
                combine(RadioService.nowPlaying, SettingsStore.settings) { playing, s -> playing to s.currentStation?.displayName(Locale.getDefault()) }
                    .collect { (playing, station) -> render(playing, station) }
            }
        }
    }

    override fun onStopListening() {
        scope?.cancel()
        scope = null
    }

    override fun onDestroy() {
        scope?.cancel()
        super.onDestroy()
    }

    override fun onClick() = SleepRadio.toggle(this)

    private fun render(playing: String?, station: String?) {
        val tile = qsTile ?: return
        tile.label = getString(R.string.tile_sleep_radio)
        tile.state = when {
            playing != null -> Tile.STATE_ACTIVE
            station == null -> Tile.STATE_UNAVAILABLE
            else -> Tile.STATE_INACTIVE
        }
        val subtitle = playing?.ifEmpty { station } ?: station ?: getString(R.string.tile_no_station)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) tile.subtitle = subtitle
        tile.contentDescription = getString(R.string.tile_description, tile.label, subtitle)
        tile.updateTile()
    }
}
