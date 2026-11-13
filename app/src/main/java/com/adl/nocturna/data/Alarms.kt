package com.adl.nocturna.data

import android.content.Context
import android.content.SharedPreferences
import androidx.core.content.edit
import com.adl.nocturna.alarm.AlarmScheduler
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import org.json.JSONArray
import org.json.JSONObject
import java.time.LocalDate
import java.time.ZonedDateTime

/**
 * One alarm. [days] is a bitmask with bit 0 = Monday ... bit 6 = Sunday; 0 means "once".
 * [station] is the radio station to wake up to, or null for the alarm tone. It is a copy, not an index
 * into the station list, so editing or deleting stations never changes what an alarm plays.
 * [skipDate] is the date of one occurrence of a repeating alarm that won't ring; a date already past
 * does nothing.
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
) {
    val repeats: Boolean get() = days != 0

    fun repeatsOn(dayIndex: Int) = days and (1 shl dayIndex) != 0

    /**
     * The next time this alarm rings strictly after [now].
     *
     * Each candidate is built from its own calendar date, so a daylight-saving change only affects
     * the day it happens on (a time inside the spring-forward gap rings at the first valid moment).
     */
    fun nextTrigger(now: ZonedDateTime): ZonedDateTime = nextTrigger(now, skipDate)

    private fun nextTrigger(now: ZonedDateTime, skip: LocalDate?): ZonedDateTime {
        var date = now.toLocalDate()
        // Two weeks and a day: a weekly alarm whose next occurrence is skipped rings a week later.
        repeat(15) {
            if ((days == 0 || repeatsOn(date.dayOfWeek.value - 1)) && date != skip) {
                val t = date.atTime(hour, minute).atZone(now.zone)
                if (t.isAfter(now)) return t
            }
            date = date.plusDays(1)
        }
        // Only reachable with no valid day bits; treat as "once" rather than loop forever.
        return copy(days = 0, skipDate = null).nextTrigger(now)
    }

    /** The occurrence being skipped, or null when none is still ahead of [now]. */
    fun skipping(now: ZonedDateTime): LocalDate? =
        skipDate?.takeIf { repeats && nextTrigger(now, null).toLocalDate() <= it }

    /**
     * Skips the next occurrence of a repeating alarm, or brings it back if it is already skipped.
     * A one-time alarm has nothing after it to ring, so it is returned unchanged: turn it off instead.
     */
    fun toggleSkip(now: ZonedDateTime): Alarm = when {
        !repeats -> this
        skipping(now) != null -> copy(skipDate = null)
        else -> copy(skipDate = nextTrigger(now, null).toLocalDate())
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

object AlarmStore {
    private lateinit var prefs: SharedPreferences
    private lateinit var appContext: Context
    private val state = MutableStateFlow<List<Alarm>>(emptyList())
    private val snoozeState = MutableStateFlow<Snooze?>(null)

    val alarms: StateFlow<List<Alarm>> = state.asStateFlow()
    val snooze: StateFlow<Snooze?> = snoozeState.asStateFlow()

    /** Loads (or reloads) alarms from device-protected storage, which is readable before the first unlock. */
    fun init(context: Context) {
        appContext = context.applicationContext
        prefs = Storage.prefs(context, "nocturna_alarms")
        state.value = runCatching { decode(prefs.getString("alarms", "[]")!!) }.getOrDefault(emptyList())
        val until = prefs.getLong("snoozeUntil", 0L)
        snoozeState.value = if (until > System.currentTimeMillis()) Snooze(prefs.getLong("snoozeId", 0L), until) else null
    }

    fun get(id: Long): Alarm? = state.value.firstOrNull { it.id == id }

    fun add(hour: Int, minute: Int): Alarm {
        val alarm = Alarm(id = System.currentTimeMillis(), hour = hour, minute = minute)
        save(state.value + alarm)
        return alarm
    }

    /**
     * Changes one alarm based on its *current* stored value, so quick successive edits don't overwrite
     * each other. Turning an alarm off or moving it also cancels a pending snooze of it.
     */
    fun update(id: Long, transform: (Alarm) -> Alarm) {
        val old = get(id) ?: return
        val new = transform(old).withSkipCheckedAgainst(old)
        if (new == old) return
        val snoozed = snoozeState.value?.alarmId == id
        if (snoozed && (!new.enabled || new.hour != old.hour || new.minute != old.minute)) clearSnooze(reschedule = false)
        save(state.value.map { if (it.id == id) new else it })
    }

    /** Removes an alarm and returns it, so the caller can offer undo via [restore]. */
    fun delete(id: Long): Alarm? {
        val removed = get(id) ?: return null
        if (snoozeState.value?.alarmId == id) clearSnooze(reschedule = false)
        save(state.value.filterNot { it.id == id })
        return removed
    }

    fun restore(alarm: Alarm) {
        if (get(alarm.id) == null) save(state.value + alarm)
    }

    fun setSnooze(alarmId: Long, until: Long) {
        snoozeState.value = Snooze(alarmId, until)
        prefs.edit { putLong("snoozeId", alarmId); putLong("snoozeUntil", until) }
        AlarmScheduler.reschedule(appContext)
    }

    fun clearSnooze(reschedule: Boolean = true) {
        snoozeState.value = null
        prefs.edit { remove("snoozeId"); remove("snoozeUntil") }
        if (reschedule) AlarmScheduler.reschedule(appContext)
    }

    private fun save(list: List<Alarm>) {
        val sorted = list.sortedWith(compareBy({ it.hour }, { it.minute }))
        state.value = sorted
        prefs.edit { putString("alarms", encode(sorted)) }
        AlarmScheduler.reschedule(appContext)
    }

    internal fun encode(list: List<Alarm>) = JSONArray().apply {
        list.forEach {
            put(
                JSONObject()
                    .put("id", it.id).put("hour", it.hour).put("minute", it.minute)
                    .put("days", it.days).put("enabled", it.enabled).put("label", it.label)
                    .apply { it.station?.let { st -> put("station", JSONObject().put("name", st.name).put("url", st.url)) } }
                    .apply { it.skipDate?.let { d -> put("skipDate", d.toString()) } }
            )
        }
    }.toString()

    /** Reads what [encode] wrote, including JSON from versions that had fewer fields. */
    internal fun decode(json: String): List<Alarm> {
        val arr = JSONArray(json)
        return (0 until arr.length()).map { i ->
            val o = arr.getJSONObject(i)
            Alarm(
                id = o.getLong("id"),
                hour = o.getInt("hour").coerceIn(0, 23),
                minute = o.getInt("minute").coerceIn(0, 59),
                days = o.optInt("days", 0) and Alarm.ALL_DAYS,
                enabled = o.optBoolean("enabled", true),
                label = o.optString("label", ""),
                station = o.optJSONObject("station")?.let { st ->
                    st.optString("url").takeIf { it.isNotBlank() }?.let { Station(st.optString("name").ifBlank { it }, it) }
                },
                skipDate = o.optString("skipDate").takeIf { it.isNotBlank() }?.let { runCatching { LocalDate.parse(it) }.getOrNull() },
            )
        }
    }
}
