package com.adl.nocturna.data

import kotlinx.atomicfu.locks.SynchronizedObject
import kotlinx.atomicfu.locks.synchronized
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/** App-wide settings, persisted as one JSON blob and exposed as a [StateFlow]. */
object SettingsStore {
    private const val KEY = "settings"
    private lateinit var store: KeyValueStore
    private val lock = SynchronizedObject()
    private val state = MutableStateFlow(Settings())

    val settings: StateFlow<Settings> = state.asStateFlow()
    val current: Settings get() = state.value

    /** Loads (or reloads) the settings from [store]; unreadable or missing ones give the defaults. */
    fun init(store: KeyValueStore) {
        this.store = store
        state.value = store.getString(KEY)?.let { runCatching { SettingsCodec.decodeFromString(it) }.getOrNull() } ?: Settings()
    }

    fun update(block: (Settings) -> Settings) {
        synchronized(lock) {
            val next = block(state.value)
            state.value = next
            store.edit { putString(KEY, SettingsCodec.encodeToString(next)) }
        }
    }
}
