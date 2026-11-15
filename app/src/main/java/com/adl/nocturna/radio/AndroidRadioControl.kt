package com.adl.nocturna.radio

import android.content.Context
import com.adl.nocturna.clock.RadioControl
import com.adl.nocturna.data.Station

/** [Radio] for the shared screens, which have no Context to pass it. */
class AndroidRadioControl(context: Context) : RadioControl {
    private val app = context.applicationContext

    override val isOn = Radio.isOn
    override val title = Radio.title
    override val error = Radio.error
    override val sleepAt = Radio.sleepAt

    override fun play(station: Station) = Radio.play(app, station)

    override fun stop() = Radio.stop(app)

    override fun setSleepTimer(minutes: Int) = Radio.setSleepTimer(minutes)
}
