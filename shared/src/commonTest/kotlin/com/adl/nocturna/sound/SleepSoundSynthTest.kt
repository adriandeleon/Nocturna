package com.adl.nocturna.sound

import com.adl.nocturna.data.Station
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.sin
import kotlin.math.sqrt
import kotlin.test.Test
import kotlin.test.assertContentEquals
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

class SleepSoundSynthTest {
    // Short, low-rate loops keep the tests fast; the code paths are the same as the full ones.
    private val rate = 8_000
    private val seconds = 4.0
    private fun render(sound: SleepSound, seed: Long = sound.seed) = SleepSoundSynth.render(sound, rate, seconds, seed)

    private fun FloatArray.rms(from: Int = 0, to: Int = size) =
        sqrt((from until to).sumOf { this[it].toDouble() * this[it] } / (to - from))

    private fun FloatArray.maxStep() = (1 until size).maxOf { abs(this[it] - this[it - 1]) }

    @Test fun sameSeedGivesTheSameSamples() = SleepSound.entries.forEach {
        assertContentEquals(render(it), render(it), it.name)
    }

    @Test fun anotherSeedGivesOtherSamples() = SleepSound.entries.forEach {
        assertFalse(render(it).contentEquals(render(it, it.seed + 1)), it.name)
    }

    @Test fun firstSamplesAreFixed() {
        // Pins the generator, so a change to it is a deliberate one (and bumps the cached files' version).
        assertEquals(-0x1ddf57c684e23251L, SeededRandom(0).nextLong())
        assertEquals(-0x6ef5d21376fda33fL, SeededRandom(1).nextLong())
    }

    @Test fun samplesAreInRangeAndAudible() = SleepSound.entries.forEach { sound ->
        val loop = render(sound)
        assertEquals((seconds * rate).toInt(), loop.size, sound.name)
        assertTrue(loop.all { it.isFinite() && it in -1f..1f }, "${sound.name} has a sample outside -1..1")
        assertTrue(loop.rms() in 0.05..0.3, "${sound.name} RMS ${loop.rms()}")
    }

    @Test fun loopPointIsNoBiggerAStepThanAnyInsideTheLoop() = SleepSound.entries.forEach { sound ->
        val loop = render(sound)
        val seam = abs(loop.first() - loop.last())
        assertTrue(seam <= loop.maxStep(), "${sound.name}: step of $seam at the loop point, at most ${loop.maxStep()} inside")
    }

    @Test fun steadyNoisesKeepTheirLevelThroughTheCrossfade() =
        listOf(SleepSound.WHITE, SleepSound.PINK, SleepSound.BROWN, SleepSound.RAIN).forEach { sound ->
            val loop = render(sound)
            val fade = (SleepSoundSynth.FADE_SECONDS * rate).toInt()
            val ratio = loop.rms(0, fade) / loop.rms(fade, loop.size)
            assertTrue(ratio in 0.75..1.33, "${sound.name}: the crossfade is $ratio times as loud as the rest")
        }

    @Test fun crossfadeJoinsTheEndToWhatTheSourceMadeNext() {
        // A sine that doesn't fit the loop a whole number of times would jump at a plain loop point.
        var i = 0
        val loop = SleepSoundSynth.seamlessLoop(n = 1000, fade = 100, warmUp = 0) { sin(2 * PI * i++ / 37.3).toFloat() }
        val plainJump = abs(sin(2 * PI * 999 / 37.3) - sin(0.0))
        val seam = abs(loop.first() - loop.last())
        assertTrue(plainJump > 0.5)
        assertTrue(seam <= 1.05f * loop.maxStep(), "step of $seam at the loop point")
    }

    @Test fun surfSwellsAndIsQuietAtTheLoopPoint() {
        val n = 40_000
        val e = SleepSoundSynth.surfEnvelope(n, SleepSoundSynth.SURF_WAVES, SeededRandom(5))
        assertTrue(e.all { it in 0f..1f })
        assertTrue(e.first() < 0.01f && e.last() < 0.01f)
        // One crest per wave: count the rises through half height.
        val crests = (1 until n).count { e[it - 1] < 0.5f && e[it] >= 0.5f }
        assertEquals(SleepSoundSynth.SURF_WAVES, crests)
    }

    @Test fun wavHeaderDescribesTheSamples() {
        val samples = floatArrayOf(0f, 1f, -1f, 0.5f, 2f)
        val wav = Wav.encode(samples, 22_050)
        fun ascii(at: Int) = (at until at + 4).map { wav[it].toInt().toChar() }.joinToString("")
        fun u16(at: Int) = (wav[at].toInt() and 0xFF) or ((wav[at + 1].toInt() and 0xFF) shl 8)
        fun s16(at: Int) = u16(at).toShort().toInt()
        fun i32(at: Int) = u16(at) or (u16(at + 2) shl 16)

        assertEquals(Wav.HEADER_BYTES + 2 * samples.size, wav.size)
        assertEquals("RIFF", ascii(0))
        assertEquals(wav.size - 8, i32(4))
        assertEquals("WAVE", ascii(8))
        assertEquals("fmt ", ascii(12))
        assertEquals(16, i32(16))
        assertEquals(1, u16(20)) // PCM
        assertEquals(1, u16(22)) // mono
        assertEquals(22_050, i32(24))
        assertEquals(44_100, i32(28))
        assertEquals(2, u16(32))
        assertEquals(16, u16(34))
        assertEquals("data", ascii(36))
        assertEquals(2 * samples.size, i32(40))
        assertEquals(listOf(0, 32767, -32767, 16384, 32767), samples.indices.map { s16(44 + 2 * it) })
    }

    @Test fun fullLengthLoopMakesTheExpectedFile() {
        val loop = SleepSoundSynth.render(SleepSound.SURF)
        assertEquals(SleepSound.SURF.loopSeconds * SleepSoundSynth.SAMPLE_RATE, loop.size)
        assertEquals(Wav.HEADER_BYTES + 2 * loop.size, Wav.encode(loop, SleepSoundSynth.SAMPLE_RATE).size)
    }

    @Test fun stationsKnowTheirSound() {
        SleepSound.entries.forEach { assertEquals(it, it.station.sleepSound) }
        assertNull(Station("D99", "https://example.com/d99").sleepSound)
        assertNull(Station("Odd", "nocturna-sound:thunder").sleepSound)
    }
}
