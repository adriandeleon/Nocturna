package com.adl.nocturna.data

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.time.Clock
import kotlin.time.Instant

class StoresTest {
    private val now = Instant.parse("2026-11-14T22:00:00Z")
    private val clock = object : Clock {
        override fun now() = this@StoresTest.now
    }
    private var reschedules = 0

    private fun alarms(store: MemoryStore = MemoryStore()) = store.also {
        AlarmStore.init(it, clock) { reschedules++ }
        reschedules = 0
    }

    @Test fun settingsLoadWhatWasSaved() {
        val store = MemoryStore("settings" to SettingsCodec.encodeToString(Settings(use24h = false, snoozeMinutes = 5)))
        SettingsStore.init(store)
        assertEquals(Settings(use24h = false, snoozeMinutes = 5), SettingsStore.current)
    }

    @Test fun missingOrDamagedSettingsGiveTheDefaults() {
        SettingsStore.init(MemoryStore())
        assertEquals(Settings(), SettingsStore.current)
        SettingsStore.init(MemoryStore("settings" to "{not json"))
        assertEquals(Settings(), SettingsStore.current)
    }

    @Test fun settingsUpdatesAreSaved() {
        val store = MemoryStore()
        SettingsStore.init(store)
        SettingsStore.update { it.copy(showSeconds = true) }
        assertEquals(true, SettingsStore.settings.value.showSeconds)
        assertEquals(Settings(showSeconds = true), SettingsCodec.decodeFromString(store.getString("settings")!!))
    }

    @Test fun addedAlarmsAreSavedSortedAndRescheduled() {
        val store = alarms()
        AlarmStore.add(9, 0)
        val early = AlarmStore.add(6, 30)
        assertEquals(now.toEpochMilliseconds(), early.id)
        assertEquals(listOf(6, 9), AlarmStore.alarms.value.map { it.hour })
        assertEquals(AlarmStore.alarms.value, AlarmCodec.decode(store.getString("alarms")!!))
        assertEquals(2, reschedules)
    }

    @Test fun deleteAndRestore() {
        alarms()
        val alarm = AlarmStore.add(7, 0)
        val removed = AlarmStore.delete(alarm.id)
        assertEquals(emptyList(), AlarmStore.alarms.value)
        AlarmStore.restore(removed!!)
        assertEquals(listOf(alarm), AlarmStore.alarms.value)
    }

    @Test fun anUnchangedUpdateSavesNothing() {
        alarms()
        val alarm = AlarmStore.add(7, 0)
        reschedules = 0
        AlarmStore.update(alarm.id) { it }
        assertEquals(0, reschedules)
    }

    @Test fun aPendingSnoozeSurvivesARestart() {
        val until = now.toEpochMilliseconds() + 5 * 60_000
        alarms(MemoryStore("snoozeId" to 42L, "snoozeUntil" to until))
        assertEquals(Snooze(42, until), AlarmStore.snooze.value)
    }

    @Test fun aSnoozeThatRanOutIsDropped() {
        alarms(MemoryStore("snoozeId" to 42L, "snoozeUntil" to now.toEpochMilliseconds() - 1))
        assertNull(AlarmStore.snooze.value)
    }

    @Test fun turningASnoozedAlarmOffCancelsTheSnooze() {
        val store = alarms()
        val alarm = AlarmStore.add(7, 0)
        AlarmStore.setSnooze(alarm.id, now.toEpochMilliseconds() + 60_000)
        assertEquals(alarm.id, store.getLong("snoozeId", 0))
        AlarmStore.update(alarm.id) { it.copy(enabled = false) }
        assertNull(AlarmStore.snooze.value)
        assertNull(store.values["snoozeUntil"])
    }

    @Test fun damagedAlarmsLoadAsNone() {
        alarms(MemoryStore("alarms" to "[{\"id\":1}]"))
        assertEquals(emptyList(), AlarmStore.alarms.value)
    }
}
