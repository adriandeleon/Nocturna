package com.adl.nocturna.alarm

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log
import com.adl.nocturna.data.SettingsStore
import com.adl.nocturna.day.CalendarEvents
import kotlinx.datetime.DateTimeUnit
import kotlinx.datetime.LocalDate
import kotlinx.datetime.TimeZone
import kotlinx.datetime.plus
import kotlinx.datetime.toLocalDateTime
import kotlin.time.Instant

/**
 * Days off from the phone's calendars, for alarms with *Skip on days off*. Fails open: without calendar
 * access, or when the calendar can't be read (as before the first unlock after a reboot), there are no
 * days off and every alarm rings.
 */
object DaysOffCalendar {
    /** Days off from today to [DAYS_OFF_LOOK_AHEAD] days ahead. */
    fun read(context: Context, now: Instant, zone: TimeZone): DaysOff {
        val first = now.toLocalDateTime(zone).date
        return read(context, first, first.plus(DAYS_OFF_LOOK_AHEAD, DateTimeUnit.DAY), zone)
    }

    fun read(context: Context, first: LocalDate, last: LocalDate, zone: TimeZone): DaysOff {
        if (!CalendarEvents.hasPermission(context)) return emptyMap()
        val keywords = parseDayOffKeywords(SettingsStore.current.dayOffKeywords)
        if (keywords.isEmpty()) return emptyMap()
        return try {
            daysOff(CalendarEvents.between(context, first, last, zone), keywords, first, last)
        } catch (e: Exception) {
            Log.w("Nocturna", "Couldn't read days off from the calendar; alarms ring as usual", e)
            emptyMap()
        }
    }
}

/**
 * Reschedules when the calendars change (the provider's PROVIDER_CHANGED broadcast), so a day off
 * added or removed moves the next alarm, and on the check [AlarmScheduler] sets shortly before a ring
 * it skips, in case that broadcast never came.
 */
class CalendarChangeReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        AlarmScheduler.reschedule(context)
    }
}
