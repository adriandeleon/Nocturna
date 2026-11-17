package com.adl.nocturna.data

import com.adl.nocturna.clock.NightBatteryMark
import com.adl.nocturna.clock.NightUse
import com.adl.nocturna.clock.nightOf
import com.adl.nocturna.clock.trackNightBattery
import kotlinx.atomicfu.locks.SynchronizedObject
import kotlinx.atomicfu.locks.synchronized
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.datetime.LocalDate
import kotlinx.datetime.TimeZone
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.buildJsonArray
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put
import kotlin.time.Instant

/**
 * The battery used at night, a [NightUse] per night, kept as JSON. The clock reports to [track]; where it left
 * off stays in memory, so a stretch the app didn't live through isn't counted.
 */
object NightBatteryStore {
    private const val KEY = "nights"
    private lateinit var store: KeyValueStore
    private val lock = SynchronizedObject()
    private val state = MutableStateFlow<List<NightUse>>(emptyList())
    private var mark: NightBatteryMark? = null

    val uses: StateFlow<List<NightUse>> = state.asStateFlow()

    /** Loads (or reloads) the log from [store]; an unreadable or missing one is empty. */
    fun init(store: KeyValueStore) {
        synchronized(lock) {
            this.store = store
            mark = null
            state.value = store.getString(KEY)?.let { runCatching { decode(it) }.getOrNull() } ?: emptyList()
        }
    }

    /** See [trackNightBattery]: [tracking] is whether the night clock shows with the phone off its charger. */
    fun track(tracking: Boolean, level: Int?, now: Instant, zone: TimeZone) {
        synchronized(lock) {
            val (next, nextMark) = trackNightBattery(state.value, mark, tracking, now.toEpochMilliseconds(), level, nightOf(now, zone))
            mark = nextMark
            if (next != state.value) {
                state.value = next
                store.edit { putString(KEY, encode(next)) }
            }
        }
    }

    fun encode(list: List<NightUse>): String = buildJsonArray {
        list.forEach {
            add(buildJsonObject { put("night", it.night.toString()); put("millis", it.millis); put("percent", it.percent) })
        }
    }.toString()

    /** Reads what [encode] wrote, leaving out entries it can't read. */
    fun decode(json: String): List<NightUse> {
        val arr = Json.parseToJsonElement(json) as? JsonArray ?: return emptyList()
        return arr.mapNotNull { element ->
            val o = element as? JsonObject ?: return@mapNotNull null
            runCatching { NightUse(LocalDate.parse(o.getString("night")), o.getLong("millis"), o.getInt("percent")) }.getOrNull()
        }.sortedBy { it.night }
    }
}
