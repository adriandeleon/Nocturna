package com.adl.nocturna.radio

import com.adl.nocturna.data.Settings
import com.adl.nocturna.data.Station
import kotlin.math.roundToInt
import kotlin.math.sign

/*
 * The clock radio's tuning: presets, stepping between them, the dial and the volume.
 */

/**
 * The stations the tuner steps through, as indices into [Settings.stations]: the starred ones
 * ([Settings.favoriteStations]), or every saved station when none is starred.
 */
val Settings.presetIndices: List<Int>
    get() {
        val starred = stations.indices.filter { stations[it].url in favoriteStations }
        return starred.ifEmpty { stations.indices.toList() }
    }

/** The presets themselves, in list order. */
val Settings.presets: List<Station> get() = presetIndices.map { stations[it] }

/**
 * Which preset is tuned in, as a position in [presetIndices], or null when a sleep sound is chosen or the
 * selected station isn't a preset.
 */
val Settings.presetSlot: Int?
    get() = if (currentSleepSound != null) null else presetIndices.indexOf(selectedStation).takeIf { it >= 0 }

/**
 * The settings tuned [step] presets on (negative steps go back), wrapping round the ends. From a sleep sound
 * or a station that isn't a preset, forward goes to the next preset below it in the list and back to the one
 * above. Unchanged with no stations.
 */
fun Settings.tuned(step: Int): Settings {
    val presets = presetIndices
    if (presets.isEmpty() || step == 0) return this
    val slot = presetSlot
    val target = if (slot != null) {
        slot + step
    } else {
        // The first step lands on the neighbouring preset; the rest move on from there.
        val from = if (currentSleepSound != null) -1 else selectedStation
        val first = if (step > 0) {
            presets.indexOfFirst { it > from }.takeIf { it >= 0 } ?: 0
        } else {
            presets.indexOfLast { it < from }.takeIf { it >= 0 } ?: presets.lastIndex
        }
        first + step - step.sign
    }
    return copy(selectedStation = presets[target.mod(presets.size)], selectedSound = "")
}

/** The settings with [preset] (an index into [Settings.stations]) tuned in. */
fun Settings.tunedTo(preset: Int): Settings =
    if (preset in stations.indices) copy(selectedStation = preset, selectedSound = "") else this

/** Stars [url] when it isn't starred, unstars it when it is. */
fun Settings.withFavoriteToggled(url: String): Settings =
    copy(favoriteStations = if (url in favoriteStations) favoriteStations - url else favoriteStations + url)

/** Where preset [slot] of [count] sits on the dial, 0 at the left end to 1 at the right, spaced evenly. */
fun dialPosition(slot: Int, count: Int): Float = if (count <= 0) 0.5f else (slot + 0.5f) / count

/** The preset slot nearest a point on the dial ([fraction], 0 to 1), or null with no presets. */
fun nearestSlot(fraction: Float, count: Int): Int? =
    if (count <= 0) null else (fraction.coerceIn(0f, 1f) * count - 0.5f).roundToInt().coerceIn(0, count - 1)

/** The steps of the clock's radio volume: 1 (a whisper) to [RADIO_VOLUME_STEPS] (full). */
const val RADIO_VOLUME_STEPS = 10

/**
 * The player's gain for volume [step]: squared, so the low steps the night needs are fine-grained (step 1
 * is about -40 dB) and each step up sounds like a similar change.
 */
fun radioGain(step: Int): Float {
    val s = step.coerceIn(1, RADIO_VOLUME_STEPS) / RADIO_VOLUME_STEPS.toFloat()
    return s * s
}

/** [step] moved by [by], kept between 1 and [RADIO_VOLUME_STEPS]. */
fun radioVolumeStep(step: Int, by: Int): Int = (step + by).coerceIn(1, RADIO_VOLUME_STEPS)

/**
 * The song a stream says is playing (its ICY "StreamTitle"), tidied for the clock: trimmed, without quotes,
 * a dangling " - " or doubled spaces. Null when there's nothing worth showing: blank, the station's own name,
 * a web address, or a placeholder such as "Unknown".
 */
fun cleanStreamTitle(raw: String?, stationName: String?): String? {
    var t = raw?.trim()?.replace(Regex("\\s+"), " ") ?: return null
    t = t.trim('"', '\'', ' ')
    t = t.removePrefix("-").removeSuffix("-").trim()
    if (t.isEmpty() || t.length < 2) return null
    if (stationName != null && t.equals(stationName.trim(), ignoreCase = true)) return null
    if (t.startsWith("http://") || t.startsWith("https://") || t.startsWith("www.")) return null
    if (t.lowercase() in PLACEHOLDERS) return null
    return t.take(MAX_TITLE)
}

private val PLACEHOLDERS = setOf("unknown", "unknown - unknown", "n/a", "none", "null", "untitled", "advert", "commercial")
private const val MAX_TITLE = 120
