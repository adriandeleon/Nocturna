package com.adl.nocturna.data

import android.content.SharedPreferences
import androidx.core.content.edit

/** A [KeyValueStore] over Android's SharedPreferences; see [Storage.loadStores] for which ones. */
class SharedPreferencesStore(private val prefs: SharedPreferences) : KeyValueStore {
    override fun getString(key: String): String? = prefs.getString(key, null)

    override fun getLong(key: String, default: Long): Long = prefs.getLong(key, default)

    override fun edit(changes: KeyValueStore.Editor.() -> Unit) = prefs.edit {
        val editor = this
        object : KeyValueStore.Editor {
            override fun putString(key: String, value: String) {
                editor.putString(key, value)
            }

            override fun putLong(key: String, value: Long) {
                editor.putLong(key, value)
            }

            override fun remove(key: String) {
                editor.remove(key)
            }
        }.changes()
    }
}
