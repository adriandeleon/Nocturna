package com.adl.nocturna.data

import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonNull
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.doubleOrNull
import kotlinx.serialization.json.longOrNull

/*
 * Reading stored JSON the way org.json's opt* and get* methods did, since versions that used org.json
 * wrote what is on people's phones: a missing key, a null or a value of the wrong type gives the default
 * (opt*) or fails (get*), and a number or boolean written as a string still counts.
 */

private fun JsonPrimitive.orNull(): JsonPrimitive? = takeUnless { it is JsonNull }

internal fun JsonPrimitive.asLong(): Long? = longOrNull ?: doubleOrNull?.toLong()

private fun JsonPrimitive.asBoolean(): Boolean? = when (content.lowercase()) {
    "true" -> true
    "false" -> false
    else -> null
}

internal fun JsonObject.primitive(key: String): JsonPrimitive? = (this[key] as? JsonPrimitive)?.orNull()

internal fun JsonObject.optString(key: String, default: String = ""): String = primitive(key)?.content ?: default

internal fun JsonObject.optInt(key: String, default: Int): Int = primitive(key)?.asLong()?.toInt() ?: default

internal fun JsonObject.optLong(key: String, default: Long): Long = primitive(key)?.asLong() ?: default

internal fun JsonObject.optDouble(key: String, default: Double): Double = primitive(key)?.doubleOrNull ?: default

internal fun JsonObject.optBoolean(key: String, default: Boolean): Boolean = primitive(key)?.asBoolean() ?: default

internal fun JsonObject.optObject(key: String): JsonObject? = this[key] as? JsonObject

internal fun JsonObject.optArray(key: String): JsonArray? = this[key] as? JsonArray

internal fun JsonObject.getString(key: String): String = primitive(key)?.content ?: missing(key)

internal fun JsonObject.getInt(key: String): Int = primitive(key)?.asLong()?.toInt() ?: missing(key)

internal fun JsonObject.getLong(key: String): Long = primitive(key)?.asLong() ?: missing(key)

internal fun JsonObject.getDouble(key: String): Double = primitive(key)?.doubleOrNull ?: missing(key)

internal fun JsonObject.getObject(key: String): JsonObject = optObject(key) ?: missing(key)

/** Whether the value is a JSON boolean, not a string or number that reads as one. */
internal fun JsonObject.isBoolean(key: String): Boolean =
    primitive(key)?.let { !it.isString && it.asBoolean() != null } ?: false

/** Element [i] of an array, or null when it is missing or a JSON null. */
internal fun JsonArray.primitiveAt(i: Int): JsonPrimitive? = (getOrNull(i) as? JsonPrimitive)?.orNull()

private fun missing(key: String): Nothing = throw IllegalArgumentException("No usable value for \"$key\"")
