package com.adl.nocturna.alarm

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import com.adl.nocturna.data.AlarmStore
import com.adl.nocturna.settings.SettingsActivity
import java.time.ZonedDateTime

/**
 * Keeps exactly one system alarm registered: the soonest of all enabled alarms and any pending snooze.
 * Uses [AlarmManager.setAlarmClock], so the system shows it as the next alarm and wakes the device.
 */
object AlarmScheduler {
    const val EXTRA_ALARM_ID = "alarm_id"
    const val EXTRA_SNOOZE = "snooze"

    fun reschedule(context: Context) {
        val am = context.getSystemService(AlarmManager::class.java)
        val now = ZonedDateTime.now()

        data class Candidate(val at: Long, val alarmId: Long, val snooze: Boolean)

        val candidates = AlarmStore.alarms.value.filter { it.enabled }.map {
            Candidate(it.nextTrigger(now).toInstant().toEpochMilli(), it.id, false)
        }.toMutableList()
        AlarmStore.snooze.value?.let { if (it.until > System.currentTimeMillis()) candidates += Candidate(it.until, it.alarmId, true) }

        val next = candidates.minByOrNull { it.at }
        if (next == null) {
            am.cancel(operation(context, 0L, false))
            return
        }
        val show = PendingIntent.getActivity(
            context, 1,
            Intent(context, SettingsActivity::class.java).putExtra(SettingsActivity.EXTRA_SCREEN, SettingsActivity.SCREEN_ALARMS),
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT,
        )
        try {
            am.setAlarmClock(AlarmManager.AlarmClockInfo(next.at, show), operation(context, next.alarmId, next.snooze))
        } catch (e: SecurityException) {
            // Exact alarms revoked by the user; fall back to an inexact alarm rather than crash.
            am.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, next.at, operation(context, next.alarmId, next.snooze))
        }
    }

    private fun operation(context: Context, alarmId: Long, snooze: Boolean): PendingIntent =
        PendingIntent.getBroadcast(
            context, 0,
            Intent(context, AlarmReceiver::class.java)
                .putExtra(EXTRA_ALARM_ID, alarmId)
                .putExtra(EXTRA_SNOOZE, snooze),
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT,
        )
}
