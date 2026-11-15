package com.adl.nocturna.data

import kotlinx.datetime.DateTimeUnit
import kotlinx.datetime.LocalDate
import kotlinx.datetime.TimeZone
import kotlinx.datetime.atTime
import kotlinx.datetime.isoDayNumber
import kotlinx.datetime.plus
import kotlinx.datetime.toInstant
import kotlinx.datetime.toLocalDateTime
import kotlin.time.Instant

/**
 * One alarm. [days] is a bitmask with bit 0 = Monday ... bit 6 = Sunday; 0 means "once".
 * [station] is the radio station to wake up to, or null for the alarm tone. It is a copy, not an index
 * into the station list, so editing or deleting stations never changes what an alarm plays.
 * [skipDate] is the date of one occurrence of a repeating alarm that won't ring; a date already past
 * does nothing.
 * [skipDaysOff] makes a repeating alarm stay quiet on days off in the phone's calendars
 * (see `com.adl.nocturna.alarm.nextRing`).
 */
data class Alarm(
    val id: Long,
    val hour: Int,
    val minute: Int,
    val days: Int = 0,
    val enabled: Boolean = true,
    val label: String = "",
    val station: Station? = null,
    val skipDate: LocalDate? = null,
    val skipDaysOff: Boolean = false,
) {
    val repeats: Boolean get() = days != 0

    fun repeatsOn(dayIndex: Int) = days and (1 shl dayIndex) != 0

    /**
     * The next time this alarm rings strictly after [now], with the alarm's time read in [zone].
     *
     * Each candidate is built from its own calendar date, so a daylight-saving change only affects
     * the day it happens on (a time inside the spring-forward gap moves later by the length of the gap).
     */
    fun nextTrigger(now: Instant, zone: TimeZone): Instant = nextTrigger(now, zone, skipDate)

    private fun nextTrigger(now: Instant, zone: TimeZone, skip: LocalDate?): Instant {
        var date = now.toLocalDateTime(zone).date
        // Two weeks and a day: a weekly alarm whose next occurrence is skipped rings a week later.
        repeat(15) {
            if ((days == 0 || repeatsOn(date.dayOfWeek.isoDayNumber - 1)) && date != skip) {
                val t = date.atTime(hour, minute).toInstant(zone)
                if (t > now) return t
            }
            date = date.plus(1, DateTimeUnit.DAY)
        }
        // Only reachable with no valid day bits; treat as "once" rather than loop forever.
        return copy(days = 0, skipDate = null).nextTrigger(now, zone)
    }

    /** The occurrence being skipped, or null when none is still ahead of [now]. */
    fun skipping(now: Instant, zone: TimeZone): LocalDate? =
        skipDate?.takeIf { repeats && nextTrigger(now, zone, null).toLocalDateTime(zone).date <= it }

    /**
     * Skips the next occurrence of a repeating alarm, or brings it back if it is already skipped.
     * A one-time alarm has nothing after it to ring, so it is returned unchanged: turn it off instead.
     */
    fun toggleSkip(now: Instant, zone: TimeZone): Alarm = when {
        !repeats -> this
        skipping(now, zone) != null -> copy(skipDate = null)
        else -> copy(skipDate = nextTrigger(now, zone, null).toLocalDateTime(zone).date)
    }

    /**
     * The alarm after an edit from [old]: changing its time, days or switch drops a skip, since the
     * occurrence it named may no longer exist or be the one the user meant.
     */
    fun withSkipCheckedAgainst(old: Alarm): Alarm {
        val scheduleChanged = hour != old.hour || minute != old.minute || days != old.days || enabled != old.enabled
        return if (scheduleChanged && skipDate == old.skipDate && skipDate != null) copy(skipDate = null) else this
    }

    companion object {
        const val ALL_DAYS = 0b111_1111
    }
}

data class Snooze(val alarmId: Long, val until: Long)
