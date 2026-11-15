package com.adl.nocturna.data

/** A [KeyValueStore] in memory, for tests. */
class MemoryStore(vararg initial: Pair<String, Any>) : KeyValueStore {
    val values = mutableMapOf(*initial)

    override fun getString(key: String): String? = values[key] as? String

    override fun getLong(key: String, default: Long): Long = values[key] as? Long ?: default

    override fun edit(changes: KeyValueStore.Editor.() -> Unit) {
        object : KeyValueStore.Editor {
            override fun putString(key: String, value: String) {
                values[key] = value
            }

            override fun putLong(key: String, value: Long) {
                values[key] = value
            }

            override fun remove(key: String) {
                values.remove(key)
            }
        }.changes()
    }
}
