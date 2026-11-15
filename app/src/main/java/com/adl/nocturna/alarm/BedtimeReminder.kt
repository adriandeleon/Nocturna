package com.adl.nocturna.alarm

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import com.adl.nocturna.NocturnaApp
import com.adl.nocturna.R
import com.adl.nocturna.clock.ClockActivity
import com.adl.nocturna.clock.ClockVisibility
import com.adl.nocturna.data.AlarmStore
import com.adl.nocturna.data.SettingsStore
import kotlinx.datetime.TimeZone
import java.util.Locale
import kotlin.time.Clock
import kotlin.time.Instant

/**
 * The bedtime reminder: one inexact system alarm at [bedtimeReminderAt] the next alarm, which posts a quiet
 * notification on [NocturnaApp.CHANNEL_BEDTIME]. [AlarmScheduler.reschedule] keeps it in step with the
 * alarms, the setting, the clock and reboots; it needs no exact-alarm access.
 */
object BedtimeReminder {
    private const val NOTIFICATION_ID = 3

    /** Sets the reminder for the alarms' next ring, [nextRing], or cancels it when there's none to send. */
    fun schedule(context: Context, am: AlarmManager, nextRing: Instant?, now: Instant, zone: TimeZone) {
        val operation = operation(context)
        val at = bedtimeReminderAt(nextRing, SettingsStore.current.bedtimeSleepHours, now, zone)
        if (at == null || !NotificationManagerCompat.from(context).areNotificationsEnabled()) {
            am.cancel(operation)
            return
        }
        am.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, at.toEpochMilliseconds(), operation)
    }

    /** Posts the reminder if it is still bedtime for the next alarm and the clock isn't already showing. */
    internal fun fire(context: Context) {
        val now = Clock.System.now()
        val zone = TimeZone.currentSystemDefault()
        val settings = SettingsStore.current
        val next = nextAlarmRing(context, now, zone)
        val nm = NotificationManagerCompat.from(context)
        if (next == null || !nm.areNotificationsEnabled() ||
            !shouldRemindBedtime(next, settings.bedtimeSleepHours, now, zone, ClockVisibility.showing)
        ) return
        val text = bedtimeReminderText(next, settings.bedtimeSleepHours, zone, settings.use24h, Locale.getDefault())
        val open = PendingIntent.getActivity(
            context, 3,
            Intent(context, ClockActivity::class.java).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK),
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT,
        )
        val n = NotificationCompat.Builder(context, NocturnaApp.CHANNEL_BEDTIME)
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setContentTitle(text)
            .setContentText("Tap to open the clock")
            .setCategory(NotificationCompat.CATEGORY_REMINDER)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .setSilent(true)
            .setAutoCancel(true)
            // Gone by the time the alarm rings, if it was never seen.
            .setTimeoutAfter((next - now).inWholeMilliseconds)
            .setContentIntent(open)
            .build()
        try {
            nm.notify(NOTIFICATION_ID, n)
        } catch (e: SecurityException) {
            // Notifications refused between the check and here: nothing to show.
        }
    }

    /** The next ring of Nocturna's own alarms, as [AlarmScheduler] works it out (days off included). */
    private fun nextAlarmRing(context: Context, now: Instant, zone: TimeZone): Instant? {
        val enabled = AlarmStore.alarms.value.filter { it.enabled }
        val daysOff = if (enabled.any { it.skipDaysOff && it.repeats }) DaysOffCalendar.read(context, now, zone) else emptyMap()
        return enabled.minOfOrNull { it.nextRing(now, zone, daysOff) }
    }

    private fun operation(context: Context): PendingIntent =
        PendingIntent.getBroadcast(
            context, 3,
            Intent(context, BedtimeReceiver::class.java),
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT,
        )
}

/** Fires at bedtime: posts the reminder, then sets the next night's. */
class BedtimeReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        BedtimeReminder.fire(context)
        AlarmScheduler.reschedule(context)
    }
}
