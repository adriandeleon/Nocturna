package com.adl.nocturna.alarm

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.adl.nocturna.charging.ChargingService
import com.adl.nocturna.data.AlarmStore
import com.adl.nocturna.data.SettingsStore

/** Fired by the system at the scheduled alarm time. */
class AlarmReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val id = intent.getLongExtra(AlarmScheduler.EXTRA_ALARM_ID, 0L)
        val wasSnooze = intent.getBooleanExtra(AlarmScheduler.EXTRA_SNOOZE, false)

        if (wasSnooze) {
            AlarmStore.clearSnooze(reschedule = false)
        } else {
            // A one-shot alarm switches itself off once it has rung.
            AlarmStore.get(id)?.takeIf { !it.repeats }?.let { AlarmStore.update(it.copy(enabled = false)) }
        }
        AlarmScheduler.reschedule(context)
        AlarmService.start(context, id)
    }
}

/** Re-registers alarms after reboot, app update or clock changes. */
class BootReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        // NocturnaApp.onCreate has already loaded the stores and rescheduled.
        AlarmScheduler.reschedule(context)
        if (SettingsStore.current.autoStartOnCharge &&
            intent.action in setOf(Intent.ACTION_BOOT_COMPLETED, Intent.ACTION_MY_PACKAGE_REPLACED)
        ) {
            ChargingService.start(context)
        }
    }
}
