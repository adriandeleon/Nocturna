package com.adl.nocturna.widget

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.Handler
import android.os.Looper
import com.adl.nocturna.data.SettingsStore
import com.adl.nocturna.radio.Radio
import com.adl.nocturna.radio.RadioService

/** The one-tap sleep radio behind the Quick Settings tile and the widget's button. */
object SleepRadio {
    /** How long the sleep radio plays before it fades out. */
    const val MINUTES = 30

    /** Stops the radio if it's on; otherwise plays the selected station with a [MINUTES]-minute sleep timer. */
    fun toggle(context: Context) {
        if (RadioService.nowPlaying.value != null) {
            Radio.stop(context)
        } else {
            val station = SettingsStore.current.currentStation ?: return
            Radio.play(context, station, sleepMinutes = MINUTES)
        }
    }
}

/** Receives the widget's *Sleep radio* taps. Not exported, so only Nocturna's own PendingIntent reaches it. */
class SleepRadioReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        // Connecting to RadioService is asynchronous: keep the broadcast open until the command is sent.
        val result = goAsync()
        var done = false
        val finish = {
            if (!done) {
                done = true
                result.finish()
            }
        }
        SleepRadio.toggle(context)
        Radio.connect(context) { finish() }
        // Radio.connect drops its callbacks if the service can't be reached; don't hold the broadcast forever.
        Handler(Looper.getMainLooper()).postDelayed(finish, 5_000)
    }
}
