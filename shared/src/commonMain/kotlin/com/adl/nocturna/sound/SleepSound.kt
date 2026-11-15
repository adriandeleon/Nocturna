package com.adl.nocturna.sound

import com.adl.nocturna.data.Station

/**
 * The built-in sleep sounds. They are made on the phone by [SleepSoundSynth], so they play offline and the
 * app carries no recordings. Each one plays as a [Station] whose URL starts with [URL_PREFIX]; the radio
 * service turns that URL into a looping WAV file.
 */
enum class SleepSound(
    val id: String,
    val label: String,
    val description: String,
    /** Length of the seamless loop; long enough that the repeat can't be heard. */
    val loopSeconds: Int,
    /** Fixed, so the sound is the same on every phone and every time it is made. */
    val seed: Long,
) {
    WHITE("white", "White noise", "An even hiss at every pitch", 30, 0x5EED_0001),
    PINK("pink", "Pink noise", "Softer and warmer, like a steady wind", 30, 0x5EED_0002),
    BROWN("brown", "Brown noise", "A deep rumble, like a distant waterfall", 30, 0x5EED_0003),
    RAIN("rain", "Rain", "Steady rain with drops pattering close by", 30, 0x5EED_0004),
    SURF("surf", "Surf", "Slow waves rolling in to the shore", 44, 0x5EED_0005),
    ;

    val url: String get() = URL_PREFIX + id
    val station: Station get() = Station(label, url)

    companion object {
        const val URL_PREFIX = "nocturna-sound:"

        fun byId(id: String): SleepSound? = entries.firstOrNull { it.id == id }

        /** The sound a station URL plays, or null for a web stream. */
        fun ofUrl(url: String): SleepSound? =
            if (url.startsWith(URL_PREFIX)) byId(url.removePrefix(URL_PREFIX)) else null
    }
}

/** The built-in sound this station plays, or null for a web stream. */
val Station.sleepSound: SleepSound? get() = SleepSound.ofUrl(url)
