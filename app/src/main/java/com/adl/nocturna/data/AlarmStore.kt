package com.adl.nocturna.data

import android.content.Context
import android.content.SharedPreferences
import androidx.core.content.edit
import com.adl.nocturna.alarm.AlarmScheduler
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

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
        state.value = runCatching { AlarmCodec.decode(prefs.getString("alarms", "[]")!!) }.getOrDefault(emptyList())
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
        prefs.edit { putString("alarms", AlarmCodec.encode(sorted)) }
        AlarmScheduler.reschedule(appContext)
    }
}
