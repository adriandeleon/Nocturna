package com.adl.nocturna.sound

import com.adl.nocturna.data.Station
import com.adl.nocturna.text.PlatformLocale
import com.adl.nocturna.text.Strings
import com.adl.nocturna.text.EnglishStrings
import com.adl.nocturna.text.strings

/**
 * The built-in sleep sounds: noises, and a few pieces of ambient music ([isMusic]). They are made on the phone
 * by [SleepSoundSynth], so they play offline and the app carries no recordings. Each one plays as a [Station] whose URL starts with [URL_PREFIX]; the radio
 * service turns that URL into a looping WAV file.
 */
enum class SleepSound(
    val id: String,
    private val title: (Strings) -> String,
    private val about: (Strings) -> String,
    /** Length of the seamless loop; long enough that the repeat can't be heard. */
    val loopSeconds: Int,
    /** Fixed, so the sound is the same on every phone and every time it is made. */
    val seed: Long,
    /** Ambient music ([AmbientMusic]) rather than a noise; the two are listed apart. */
    val isMusic: Boolean = false,
) {
    WHITE("white", Strings::soundWhite, Strings::soundWhiteAbout, 30, 0x5EED_0001),
    PINK("pink", Strings::soundPink, Strings::soundPinkAbout, 30, 0x5EED_0002),
    BROWN("brown", Strings::soundBrown, Strings::soundBrownAbout, 30, 0x5EED_0003),
    RAIN("rain", Strings::soundRain, Strings::soundRainAbout, 30, 0x5EED_0004),
    SURF("surf", Strings::soundSurf, Strings::soundSurfAbout, 44, 0x5EED_0005),
    CHORDS("chords", Strings::soundChords, Strings::soundChordsAbout, 64, 0x5EED_0006, isMusic = true),
    CHIMES("chimes", Strings::soundChimes, Strings::soundChimesAbout, 48, 0x5EED_0007, isMusic = true),
    DRONE("drone", Strings::soundDrone, Strings::soundDroneAbout, 60, 0x5EED_0008, isMusic = true),
    ;

    /** Its name, e.g. "Pink noise". */
    fun label(locale: PlatformLocale): String = title(strings(locale))

    /** What it sounds like, e.g. "Softer and warmer, like a steady wind". */
    fun description(locale: PlatformLocale): String = about(strings(locale))

    val url: String get() = URL_PREFIX + id

    /**
     * The sound as a station for the radio player. Its stored name stays in English; where a station's name
     * is shown, [Station.displayName] gives the sound's name in the phone's language.
     */
    val station: Station get() = Station(title(EnglishStrings), url)

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

/** The station's name to show: a built-in sound's name in [locale]'s language, a web stream's name as it is. */
fun Station.displayName(locale: PlatformLocale): String = sleepSound?.label(locale) ?: name
