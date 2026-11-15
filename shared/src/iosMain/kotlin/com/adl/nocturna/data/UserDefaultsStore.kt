package com.adl.nocturna.data

import platform.Foundation.NSUserDefaults

/** A [KeyValueStore] in its own NSUserDefaults suite, named [suiteName]. */
class UserDefaultsStore(suiteName: String) : KeyValueStore {
    private val defaults = NSUserDefaults(suiteName = suiteName)

    override fun getString(key: String): String? = defaults.stringForKey(key)

    override fun getLong(key: String, default: Long): Long =
        if (defaults.objectForKey(key) == null) default else defaults.integerForKey(key)

    override fun edit(changes: KeyValueStore.Editor.() -> Unit) {
        object : KeyValueStore.Editor {
            override fun putString(key: String, value: String) = defaults.setObject(value, forKey = key)

            override fun putLong(key: String, value: Long) = defaults.setInteger(value, forKey = key)

            override fun remove(key: String) = defaults.removeObjectForKey(key)
        }.changes()
    }
}
