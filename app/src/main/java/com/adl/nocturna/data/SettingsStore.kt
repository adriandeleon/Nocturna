package com.adl.nocturna.data

import android.content.Context
import android.content.SharedPreferences
import androidx.core.content.edit
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/** App-wide settings, persisted as one JSON blob and exposed as a [StateFlow]. */
object SettingsStore {
    private const val KEY = "settings"
    private lateinit var prefs: SharedPreferences
    private val state = MutableStateFlow(Settings())

    val settings: StateFlow<Settings> = state.asStateFlow()
    val current: Settings get() = state.value

    fun init(context: Context) {
        prefs = Storage.prefs(context, "nocturna")
        state.value = prefs.getString(KEY, null)?.let { runCatching { SettingsCodec.decodeFromString(it) }.getOrNull() } ?: Settings()
    }

    fun update(block: (Settings) -> Settings) {
        synchronized(this) {
            val next = block(state.value)
            state.value = next
            prefs.edit { putString(KEY, SettingsCodec.encodeToString(next)) }
        }
    }
}
