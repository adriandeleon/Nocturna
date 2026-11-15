package com.adl.nocturna.alarm

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import com.adl.nocturna.data.AlarmStore
import com.adl.nocturna.settings.SettingsActivity
import kotlinx.datetime.TimeZone
import kotlin.time.Clock
import kotlin.time.Duration.Companion.minutes
import kotlin.time.Instant

/**
 * Keeps exactly one system alarm registered: the soonest of all enabled alarms and any pending snooze.
 * Uses [AlarmManager.setAlarmClock], so the system shows it as the next alarm and wakes the device.
 */
object AlarmScheduler {
    const val EXTRA_ALARM_ID = "alarm_id"
    const val EXTRA_SNOOZE = "snooze"

    fun reschedule(context: Context) {
        val am = context.getSystemService(AlarmManager::class.java)
        val now = Clock.System.now()
        val zone = TimeZone.currentSystemDefault()

        data class Candidate(val at: Long, val alarmId: Long, val snooze: Boolean)

        val enabled = AlarmStore.alarms.value.filter { it.enabled }
        val daysOff = if (enabled.any { it.skipDaysOff && it.repeats }) DaysOffCalendar.read(context, now, zone) else emptyMap()
        val candidates = enabled.map {
            Candidate(it.nextRing(now, zone, daysOff).toEpochMilliseconds(), it.id, false)
        }.toMutableList()
        scheduleDaysOffCheck(context, am, enabled.mapNotNull { it.firstSkippedRing(now, zone, daysOff) }.minOrNull(), now)
        BedtimeReminder.schedule(context, am, candidates.minOfOrNull { it.at }?.let(Instant::fromEpochMilliseconds), now, zone)
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

    /**
     * Checks the calendar again [DAYS_OFF_CHECK_BEFORE] before the first ring skipped for a day off, so an
     * alarm still rings if the day off was taken out of the calendar and the change was never broadcast.
     */
    private fun scheduleDaysOffCheck(context: Context, am: AlarmManager, skipped: Instant?, now: Instant) {
        val check = PendingIntent.getBroadcast(
            context, 2,
            Intent(context, CalendarChangeReceiver::class.java),
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT,
        )
        val at = skipped?.minus(DAYS_OFF_CHECK_BEFORE)?.takeIf { it > now }
        if (at == null) {
            am.cancel(check)
            return
        }
        try {
            am.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, at.toEpochMilliseconds(), check)
        } catch (e: SecurityException) {
            am.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, at.toEpochMilliseconds(), check)
        }
    }

    private val DAYS_OFF_CHECK_BEFORE = 10.minutes

    private fun operation(context: Context, alarmId: Long, snooze: Boolean): PendingIntent =
        PendingIntent.getBroadcast(
            context, 0,
            Intent(context, AlarmReceiver::class.java)
                .putExtra(EXTRA_ALARM_ID, alarmId)
                .putExtra(EXTRA_SNOOZE, snooze),
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT,
        )
}
