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
import java.time.ZonedDateTime

/**
 * One alarm. [days] is a bitmask with bit 0 = Monday ... bit 6 = Sunday; 0 means "once".
 */
data class Alarm(
    val id: Long,
    val hour: Int,
    val minute: Int,
    val days: Int = 0,
    val enabled: Boolean = true,
    val label: String = "",
) {
    val repeats: Boolean get() = days != 0

    fun repeatsOn(dayIndex: Int) = days and (1 shl dayIndex) != 0

    /** The next time this alarm rings strictly after [now]. */
    fun nextTrigger(now: ZonedDateTime): ZonedDateTime {
        var t = now.withHour(hour).withMinute(minute).withSecond(0).withNano(0)
        if (!t.isAfter(now)) t = t.plusDays(1)
        if (days != 0) {
            while (!repeatsOn(t.dayOfWeek.value - 1)) t = t.plusDays(1)
        }
        return t
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

    fun init(context: Context) {
        appContext = context.applicationContext
        prefs = context.getSharedPreferences("nocturna_alarms", Context.MODE_PRIVATE)
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

    fun update(alarm: Alarm) = save(state.value.map { if (it.id == alarm.id) alarm else it })

    fun delete(id: Long) {
        if (snoozeState.value?.alarmId == id) clearSnooze(reschedule = false)
        save(state.value.filterNot { it.id == id })
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

    private fun encode(list: List<Alarm>) = JSONArray().apply {
        list.forEach {
            put(
                JSONObject()
                    .put("id", it.id).put("hour", it.hour).put("minute", it.minute)
                    .put("days", it.days).put("enabled", it.enabled).put("label", it.label)
            )
        }
    }.toString()

    private fun decode(json: String): List<Alarm> {
        val arr = JSONArray(json)
        return (0 until arr.length()).map { i ->
            val o = arr.getJSONObject(i)
            Alarm(
                id = o.getLong("id"),
                hour = o.getInt("hour"),
                minute = o.getInt("minute"),
                days = o.optInt("days", 0),
                enabled = o.optBoolean("enabled", true),
                label = o.optString("label", ""),
            )
        }
    }
}
