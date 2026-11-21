package com.adl.nocturna.radio

import com.adl.nocturna.data.Station
import com.adl.nocturna.data.optInt
import com.adl.nocturna.data.optString
import com.adl.nocturna.weather.fixed4
import com.adl.nocturna.weather.formEncode
import com.adl.nocturna.weather.httpGet
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonObject

/*
 * Radio Browser (radio-browser.info), a free, community-run directory of internet radio stations with no key.
 * Its data is public domain; the NOTICE file names it.
 */

private const val API = "https://all.api.radio-browser.info/json/stations"

/** How many stations one search lists. */
const val RADIO_BROWSER_LIMIT = 40

/** How far "Near me" looks, in metres: a city and the towns around it. */
const val NEARBY_METRES = 60_000

/** A station found in the directory, before it is saved. */
data class FoundStation(
    val name: String,
    val url: String,
    /** ISO 3166-1 code such as "MX", or blank. */
    val countryCode: String,
    /** The directory's genre tags, the first few, e.g. "jazz, smooth jazz". */
    val tags: List<String>,
    /** "MP3", "AAC" or blank. */
    val codec: String,
    /** Kilobits a second, 0 when not known. */
    val bitrate: Int,
) {
    val station: Station get() = Station(name, url)

    /** "MX · jazz, smooth jazz · 128 kbps": what's known, for the line under its name. */
    val detail: String
        get() = listOfNotNull(
            countryCode.ifBlank { null },
            tags.take(3).joinToString(", ").ifBlank { null },
            if (bitrate > 0) "$bitrate kbps" else codec.ifBlank { null },
        ).joinToString(" · ")
}

/** Stations whose name, genre tag or country matches [query], the most listened to first. */
internal fun radioSearchUrl(query: String): String =
    "$API/search?name=${formEncode(query.trim())}&order=clickcount&reverse=true&hidebroken=true&limit=$RADIO_BROWSER_LIMIT"

internal fun radioTagUrl(query: String): String =
    "$API/search?tag=${formEncode(query.trim().lowercase())}&order=clickcount&reverse=true&hidebroken=true&limit=$RADIO_BROWSER_LIMIT"

internal fun radioCountryUrl(query: String): String =
    "$API/search?country=${formEncode(query.trim())}&order=clickcount&reverse=true&hidebroken=true&limit=$RADIO_BROWSER_LIMIT"

/** Stations within [NEARBY_METRES] of a place, the most listened to first. */
internal fun radioNearbyUrl(lat: Double, lon: Double): String =
    "$API/search?geo_lat=${fixed4(lat)}&geo_long=${fixed4(lon)}&geo_distance=$NEARBY_METRES" +
        "&order=clickcount&reverse=true&hidebroken=true&limit=$RADIO_BROWSER_LIMIT"

/**
 * The stations in a Radio Browser reply that Nocturna can play: with a name and an http(s) stream, not
 * failing the directory's last check, once per stream.
 */
fun parseRadioBrowser(body: String): List<FoundStation> {
    val array = runCatching { Json.parseToJsonElement(body) as? JsonArray }.getOrNull() ?: return emptyList()
    return array.mapNotNull { element ->
        val o = element as? JsonObject ?: return@mapNotNull null
        val name = o.optString("name").trim().replace(Regex("\\s+"), " ")
        val url = o.optString("url_resolved").trim().ifBlank { o.optString("url").trim() }
        if (name.isEmpty() || !(url.startsWith("http://") || url.startsWith("https://"))) return@mapNotNull null
        if (o.optInt("lastcheckok", 1) == 0) return@mapNotNull null
        FoundStation(
            name = name,
            url = url,
            countryCode = o.optString("countrycode").trim().uppercase(),
            tags = o.optString("tags").split(',').map { it.trim() }.filter { it.isNotEmpty() }.take(3),
            codec = o.optString("codec").trim().let { if (it.equals("unknown", ignoreCase = true)) "" else it },
            bitrate = o.optInt("bitrate", 0).coerceAtLeast(0),
        )
    }.distinctBy { it.url }
}

/**
 * Merges the lists from a name search, a genre search and a country search into one, matches by name first,
 * each station once.
 */
fun mergeSearches(vararg lists: List<FoundStation>): List<FoundStation> =
    lists.toList().flatten().distinctBy { it.url }.take(RADIO_BROWSER_LIMIT)

/** Searches and lists from Radio Browser; [get] is the HTTP fetch, swapped out in tests. */
class RadioBrowser(private val get: suspend (String) -> String = { httpGet(it) }) {
    /** Stations by name, genre or country; throws when the directory can't be reached. */
    suspend fun search(query: String): List<FoundStation> {
        if (query.isBlank()) return emptyList()
        val byName = parseRadioBrowser(get(radioSearchUrl(query)))
        val byTag = runCatching { parseRadioBrowser(get(radioTagUrl(query))) }.getOrDefault(emptyList())
        val byCountry = runCatching { parseRadioBrowser(get(radioCountryUrl(query))) }.getOrDefault(emptyList())
        return mergeSearches(byName, byTag, byCountry)
    }

    /** Popular stations around [lat], [lon]; throws when the directory can't be reached. */
    suspend fun nearby(lat: Double, lon: Double): List<FoundStation> = parseRadioBrowser(get(radioNearbyUrl(lat, lon)))
}
