package com.adl.nocturna

import android.app.Application
import android.app.NotificationChannel
import android.app.NotificationManager
import com.adl.nocturna.alarm.AlarmScheduler
import com.adl.nocturna.charging.ChargingService
import com.adl.nocturna.clock.DndWhileShowing
import com.adl.nocturna.data.SettingsStore
import com.adl.nocturna.data.Storage
import com.adl.nocturna.widget.ClockWidget

class NocturnaApp : Application() {
    override fun onCreate() {
        super.onCreate()
        Storage.loadStores(this)
        createChannels()
        AlarmScheduler.reschedule(this)
        ClockWidget.watch(this)
        // Puts back Do Not Disturb that a clock which went away with the app had turned on.
        DndWhileShowing.check(this)
        // Before the first unlock after a reboot only alarm components may run.
        if (SettingsStore.current.autoStartOnCharge && Storage.isUnlocked(this)) ChargingService.start(this)
    }

    private fun createChannels() {
        val nm = getSystemService(NotificationManager::class.java)
        nm.createNotificationChannels(
            listOf(
                NotificationChannel(CHANNEL_ALARM, getString(R.string.channel_alarm), NotificationManager.IMPORTANCE_HIGH).apply {
                    setSound(null, null) // AlarmService plays the sound itself, with a volume ramp.
                    enableVibration(false)
                },
                NotificationChannel(CHANNEL_SERVICE, getString(R.string.channel_service), NotificationManager.IMPORTANCE_MIN),
                NotificationChannel(CHANNEL_LAUNCH, getString(R.string.channel_launch), NotificationManager.IMPORTANCE_HIGH).apply {
                    setSound(null, null)
                },
                NotificationChannel(CHANNEL_BEDTIME, getString(R.string.channel_bedtime), NotificationManager.IMPORTANCE_LOW),
            )
        )
    }

    companion object {
        const val CHANNEL_ALARM = "alarm"
        const val CHANNEL_SERVICE = "service"
        const val CHANNEL_LAUNCH = "launch"
        const val CHANNEL_BEDTIME = "bedtime"
    }
}
