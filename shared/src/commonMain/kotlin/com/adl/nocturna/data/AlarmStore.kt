package com.adl.nocturna.data

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlin.time.Clock

object AlarmStore {
    private lateinit var store: KeyValueStore
    private lateinit var clock: Clock
    private var reschedule: () -> Unit = {}
    private val state = MutableStateFlow<List<Alarm>>(emptyList())
    private val snoozeState = MutableStateFlow<Snooze?>(null)

    val alarms: StateFlow<List<Alarm>> = state.asStateFlow()
    val snooze: StateFlow<Snooze?> = snoozeState.asStateFlow()

    /**
     * Loads (or reloads) alarms and any pending snooze from [store]. [reschedule] is called after every
     * change that can move the next ring, so the platform can set its system alarm again.
     */
    fun init(store: KeyValueStore, clock: Clock = Clock.System, reschedule: () -> Unit) {
        this.store = store
        this.clock = clock
        this.reschedule = reschedule
        state.value = runCatching { AlarmCodec.decode(store.getString("alarms") ?: "[]") }.getOrDefault(emptyList())
        val until = store.getLong("snoozeUntil", 0L)
        snoozeState.value = if (until > now()) Snooze(store.getLong("snoozeId", 0L), until) else null
    }

    fun get(id: Long): Alarm? = state.value.firstOrNull { it.id == id }

    fun add(hour: Int, minute: Int): Alarm {
        val alarm = Alarm(id = now(), hour = hour, minute = minute)
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
        store.edit { putLong("snoozeId", alarmId); putLong("snoozeUntil", until) }
        reschedule()
    }

    fun clearSnooze(reschedule: Boolean = true) {
        snoozeState.value = null
        store.edit { remove("snoozeId"); remove("snoozeUntil") }
        if (reschedule) this.reschedule()
    }

    private fun save(list: List<Alarm>) {
        val sorted = list.sortedWith(compareBy({ it.hour }, { it.minute }))
        state.value = sorted
        store.edit { putString("alarms", AlarmCodec.encode(sorted)) }
        reschedule()
    }

    private fun now(): Long = clock.now().toEpochMilliseconds()
}
