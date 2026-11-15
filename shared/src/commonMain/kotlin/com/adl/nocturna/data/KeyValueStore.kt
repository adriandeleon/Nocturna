package com.adl.nocturna.data

/**
 * A small file of named values that outlives the app: SharedPreferences on Android, NSUserDefaults on iOS.
 * The stores keep their JSON in one.
 */
interface KeyValueStore {
    fun getString(key: String): String?

    fun getLong(key: String, default: Long): Long

    /** Makes [changes] together. Like `SharedPreferences.edit { }`, they may reach the disk a moment later. */
    fun edit(changes: Editor.() -> Unit)

    interface Editor {
        fun putString(key: String, value: String)

        fun putLong(key: String, value: Long)

        fun remove(key: String)
    }
}
