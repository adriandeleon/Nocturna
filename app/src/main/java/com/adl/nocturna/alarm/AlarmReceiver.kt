package com.adl.nocturna.alarm

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.adl.nocturna.charging.ChargingService
import com.adl.nocturna.data.AlarmStore
import com.adl.nocturna.data.SettingsStore
import com.adl.nocturna.data.Storage

/** Fired by the system at the scheduled alarm time. */
class AlarmReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val id = intent.getLongExtra(AlarmScheduler.EXTRA_ALARM_ID, 0L)
        val wasSnooze = intent.getBooleanExtra(AlarmScheduler.EXTRA_SNOOZE, false)

        if (wasSnooze) {
            AlarmStore.clearSnooze(reschedule = false)
        } else {
            // A one-shot alarm switches itself off once it has rung (update() also reschedules).
            AlarmStore.update(id) { if (it.repeats) it else it.copy(enabled = false) }
        }
        AlarmScheduler.reschedule(context)
        AlarmService.start(context, id)
    }
}

/** Re-registers alarms after reboot, app update or clock changes. */
class BootReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        // NocturnaApp.onCreate has already loaded the stores. After the first unlock, load them again:
        // that is when data from older versions (in normal storage) can be moved to device storage.
        if (intent.action == Intent.ACTION_BOOT_COMPLETED || intent.action == Intent.ACTION_MY_PACKAGE_REPLACED) {
            SettingsStore.init(context)
            AlarmStore.init(context)
        }
        AlarmScheduler.reschedule(context)
        if (SettingsStore.current.autoStartOnCharge && Storage.isUnlocked(context) &&
            intent.action in setOf(Intent.ACTION_BOOT_COMPLETED, Intent.ACTION_MY_PACKAGE_REPLACED)
        ) {
            ChargingService.start(context)
        }
    }
}
