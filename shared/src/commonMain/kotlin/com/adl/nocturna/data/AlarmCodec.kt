package com.adl.nocturna.data

import kotlinx.datetime.LocalDate
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.buildJsonArray
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put

/** Alarms as the JSON array they are stored as. */
object AlarmCodec {
    fun encode(list: List<Alarm>): String = buildJsonArray {
        list.forEach {
            add(
                buildJsonObject {
                    put("id", it.id)
                    put("hour", it.hour)
                    put("minute", it.minute)
                    put("days", it.days)
                    put("enabled", it.enabled)
                    put("label", it.label)
                    it.station?.let { st -> put("station", buildJsonObject { put("name", st.name); put("url", st.url) }) }
                    it.skipDate?.let { d -> put("skipDate", d.toString()) }
                    if (it.skipDaysOff) put("skipDaysOff", true)
                    it.ringDate?.let { d -> put("date", d.toString()) }
                }
            )
        }
    }.toString()

    /** Reads what [encode] wrote, including JSON from versions that had fewer fields. */
    fun decode(json: String): List<Alarm> {
        val arr = Json.parseToJsonElement(json) as? JsonArray ?: throw IllegalArgumentException("Not a JSON array")
        return arr.map { element ->
            val o = element as? JsonObject ?: throw IllegalArgumentException("Not a JSON object")
            val days = o.optInt("days", 0) and Alarm.ALL_DAYS
            Alarm(
                id = o.getLong("id"),
                hour = o.getInt("hour").coerceIn(0, 23),
                minute = o.getInt("minute").coerceIn(0, 59),
                days = days,
                enabled = o.optBoolean("enabled", true),
                label = o.optString("label", ""),
                station = o.optObject("station")?.let { st ->
                    st.optString("url").takeIf { it.isNotBlank() }?.let { Station(st.optString("name").ifBlank { it }, it) }
                },
                skipDate = o.optDate("skipDate"),
                skipDaysOff = o.optBoolean("skipDaysOff", false),
                // A date and repeat days never go together: the days win.
                date = if (days != 0) null else o.optDate("date"),
            )
        }
    }

    private fun JsonObject.optDate(key: String): LocalDate? =
        optString(key).takeIf { it.isNotBlank() }?.let { runCatching { LocalDate.parse(it) }.getOrNull() }
}
