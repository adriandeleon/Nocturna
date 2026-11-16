package com.adl.nocturna.sound

import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.exp
import kotlin.math.max
import kotlin.math.pow
import kotlin.math.roundToInt
import kotlin.math.sin
import kotlin.math.sqrt

/**
 * The ambient music among the [SleepSound]s: slow chords, chimes and a drone, played by a small synthesizer
 * of sine oscillators. Pure Kotlin, so a seed gives the same music on every platform.
 *
 * Everything is written into a [Loop], a buffer one loop long that wraps around: a chord still ringing at the
 * loop's end carries on at its start, so the loop is seamless by construction, with no crossfade. Filters and
 * the reverb run round the loop twice and keep the second lap, so their tails wrap the same way.
 */
internal object AmbientMusic {
    fun render(sound: SleepSound, sampleRate: Int, n: Int, random: SeededRandom): FloatArray {
        val loop = Loop(n, sampleRate)
        when (sound) {
            SleepSound.CHORDS -> chords(loop, random)
            SleepSound.CHIMES -> chimes(loop, random)
            SleepSound.DRONE -> drone(loop, random)
            else -> error("${sound.name} is not music")
        }
        return FloatArray(n) { loop.buf[it].toFloat() }
    }

    /** MIDI note number to hertz, A4 (69) = 440 Hz. */
    fun hz(midi: Int): Double = 440.0 * 2.0.pow((midi - 69) / 12.0)

    /**
     * Four slow chords in D major (I, vi, IV, V), a few seconds of swell and fade each, with a bass note under
     * them and one or two soft glassy notes from the D major pentatonic on top.
     */
    private fun chords(loop: Loop, random: SeededRandom) {
        val chords = listOf(
            Chord(bass = 38, pad = intArrayOf(50, 57, 64, 66), top = intArrayOf(74, 76, 78, 81)), // Dmaj9
            Chord(bass = 35, pad = intArrayOf(54, 57, 62, 64), top = intArrayOf(74, 76, 78, 83)), // Bm11
            Chord(bass = 43, pad = intArrayOf(50, 54, 59, 62), top = intArrayOf(74, 78, 81, 83)), // Gmaj7
            Chord(bass = 45, pad = intArrayOf(52, 57, 59, 66), top = intArrayOf(76, 78, 81, 83)), // A6sus2
        )
        val len = loop.seconds / chords.size
        chords.forEachIndexed { i, chord ->
            val at = i * len
            loop.note(at, len * 1.2, hz(chord.bass), 0.6, BASS, attack = len * 0.25, release = len * 0.45, phase = random)
            chord.pad.forEachIndexed { j, midi ->
                // A slow strum: each note comes in a little after the one below it.
                val start = at + len * (0.03 * j + 0.04 * random.nextDouble())
                val gain = if (j == chord.pad.lastIndex) 0.35 else 0.5
                for (cents in doubleArrayOf(-5.0, 5.0)) {
                    loop.note(start, len * 1.4, hz(midi) * 2.0.pow(cents / 1200), gain, PAD, len * 0.35, len * 0.45, random)
                }
            }
            val tops = if (random.nextDouble() < 0.5) 1 else 2
            var last = -1
            repeat(tops) { k ->
                val start = at + len * (0.25 + 0.3 * k + 0.2 * random.nextDouble())
                var midi = chord.top[(random.nextDouble() * chord.top.size).toInt()]
                if (midi == last) midi = chord.top[(chord.top.indexOf(midi) + 1) % chord.top.size]
                last = midi
                loop.note(start, minOf(6.0, len * 0.9), hz(midi), 0.3, GLASS, attack = 0.02, release = 0.4, phase = random)
            }
        }
        loop.lowPass(3_000.0)
        loop.reverb(room = 0.86, damp = 0.3, wet = 0.6)
    }

    /**
     * Bells struck one at a time, wandering up and down the A minor pentatonic, over a low hum of A and E that
     * swells and beats slowly.
     */
    private fun chimes(loop: Loop, random: SeededRandom) {
        hum(loop, random, 45, 0.30, HUM, cycles = 2, depth = 0.4)
        hum(loop, random, 52, 0.15, HUM, cycles = 3, depth = 0.6)
        hum(loop, random, 57, 0.08, SINE, cycles = 1, depth = 0.8)

        val scale = intArrayOf(69, 72, 74, 76, 79, 81, 84, 86, 88)
        val count = max(2, (loop.seconds / 2.8).roundToInt())
        val spacing = loop.seconds / count
        var step = 3
        repeat(count) { i ->
            val at = (i + 0.35 * random.nextSigned()) * spacing
            step = (step + STEPS[(random.nextDouble() * STEPS.size).toInt()]).coerceIn(0, scale.lastIndex)
            val velocity = 0.45 + 0.55 * random.nextDouble()
            loop.note(at, minOf(7.0, loop.seconds * 0.8), hz(scale[step]), velocity, BELL, attack = 0.003, release = 0.5, phase = random)
            // Now and then a softer neighbour answers, as a wind chime's bars knock on.
            if (random.nextDouble() < 0.2) {
                val answer = scale[(step + if (random.nextDouble() < 0.5) -1 else 2).coerceIn(0, scale.lastIndex)]
                val later = at + 0.25 + 0.4 * random.nextDouble()
                loop.note(later, minOf(6.0, loop.seconds * 0.7), hz(answer), velocity * 0.5, BELL, 0.003, 0.5, random)
            }
        }
        loop.lowPass(6_000.0)
        loop.reverb(room = 0.9, damp = 0.25, wet = 0.9)
    }

    /** A low C with its fifths and octaves, and a D and E high above; each part swells on its own slow cycle. */
    private fun drone(loop: Loop, random: SeededRandom) {
        hum(loop, random, 36, 0.70, DEEP, cycles = 1, depth = 0.3)
        hum(loop, random, 43, 0.55, HUM, cycles = 2, depth = 0.5)
        hum(loop, random, 48, 0.45, PAD, cycles = 3, depth = 0.6)
        hum(loop, random, 55, 0.22, PAD, cycles = 5, depth = 0.8)
        hum(loop, random, 62, 0.10, SINE, cycles = 4, depth = 0.9)
        hum(loop, random, 64, 0.06, SINE, cycles = 7, depth = 0.9)
        loop.lowPass(2_000.0)
        loop.reverb(room = 0.88, damp = 0.4, wet = 0.5)
    }

    /**
     * A tone that lasts the whole loop: two oscillators a few beats per loop apart, so it wavers slowly,
     * swelling [cycles] times per loop by up to [depth].
     */
    private fun hum(loop: Loop, random: SeededRandom, midi: Int, gain: Double, partials: List<Partial>, cycles: Int, depth: Double) {
        val beats = 2 + (random.nextDouble() * 4).toInt()
        val lfoPhase = 2 * PI * random.nextDouble()
        val base = loop.wholeCycles(hz(midi))
        for (hz in doubleArrayOf(base, base + beats / loop.seconds)) {
            loop.hum(hz, gain / 2, partials, cycles, depth, lfoPhase, random)
        }
    }

    private class Chord(val bass: Int, val pad: IntArray, val top: IntArray)

    /** Steps through the scale between one bell and the next: mostly to a neighbour, sometimes a leap. */
    private val STEPS = intArrayOf(-2, -1, -1, 0, 1, 1, 2)

    private val SINE = listOf(Partial(1.0, 1.0))
    private val BASS = listOf(Partial(1.0, 1.0), Partial(2.0, 0.4))
    private val PAD = listOf(Partial(1.0, 1.0), Partial(2.0, 0.3), Partial(3.0, 0.1), Partial(4.0, 0.04))
    private val HUM = listOf(Partial(1.0, 1.0), Partial(2.0, 0.35), Partial(3.0, 0.12))
    /**
     * A low note with strong overtones: a phone's speaker barely plays the fundamental, but from the overtones
     * the ear still hears the low note.
     */
    private val DEEP = listOf(Partial(1.0, 0.6), Partial(2.0, 0.6), Partial(3.0, 0.4), Partial(4.0, 0.2), Partial(5.0, 0.08))

    /** A soft glassy note: almost a pure tone, the overtones gone in a moment. */
    private val GLASS = listOf(Partial(1.0, 1.0, decay = 3.0), Partial(2.0, 0.15, decay = 1.2), Partial(3.0, 0.05, decay = 0.6))

    /** A struck metal bar: the overtones of a free bar (not whole multiples), the higher ones dying sooner. */
    private val BELL = listOf(
        Partial(1.0, 1.0, decay = 2.6),
        Partial(2.756, 0.30, decay = 1.1),
        Partial(5.404, 0.10, decay = 0.45),
        Partial(8.933, 0.04, decay = 0.25),
    )
}

/** One sine in a voice: [ratio] times the note's frequency, at [amp], dying away over [decay] seconds (time constant). */
internal class Partial(val ratio: Double, val amp: Double, val decay: Double = Double.POSITIVE_INFINITY)

/** [n] samples of music at [rate] that wrap around: what is written past the end lands at the start. */
internal class Loop(val n: Int, val rate: Int) {
    val buf = DoubleArray(n)
    val seconds: Double get() = n.toDouble() / rate

    /** The frequency nearest [hz] that fits a whole number of times into the loop, so it joins up at the loop point. */
    fun wholeCycles(hz: Double): Double = max(1.0, (hz * seconds).roundToInt().toDouble()) / seconds

    /**
     * Adds a note at [hz] of [partials] (those above the Nyquist limit are left out), starting [start] s into
     * the loop and lasting [length] s, but never longer than the loop. It fades in over [attack] s and out over
     * the last [release] s; each partial starts at a phase from [phase].
     */
    fun note(
        start: Double, length: Double, hz: Double, gain: Double, partials: List<Partial>,
        attack: Double, release: Double, phase: SeededRandom,
    ) {
        val from = (start * rate).roundToInt()
        val len = (length * rate).roundToInt().coerceIn(1, n)
        val a = (attack * rate).roundToInt().coerceIn(1, len)
        val r = (release * rate).roundToInt().coerceIn(1, len)
        val voice = Voice(hz, partials, phase, rate)
        var at = from.mod(n)
        for (k in 0 until len) {
            val env = minOf(smooth(k.toDouble() / a), smooth((len - k).toDouble() / r))
            buf[at] += gain * env * voice.next()
            if (++at == n) at = 0
        }
    }

    /**
     * Adds a tone at [hz] that lasts the whole loop, its loudness swelling [cycles] times per loop between
     * 1 - [depth] and 1, from [lfoPhase]. [hz] should come from [wholeCycles], or the tone jumps at the loop point.
     */
    fun hum(hz: Double, gain: Double, partials: List<Partial>, cycles: Int, depth: Double, lfoPhase: Double, phase: SeededRandom) {
        val whole = partials.map { Partial(it.ratio.roundToInt().coerceAtLeast(1).toDouble(), it.amp) }
        val voice = Voice(hz, whole, phase, rate)
        val lfo = Sine(2 * PI * cycles / n, lfoPhase)
        for (k in 0 until n) {
            val swell = 1 - depth * (0.5 - 0.5 * lfo.next())
            buf[k] += gain * swell * voice.next()
        }
    }

    /** One-pole low pass at [cutoffHz], round the loop twice so its state at the loop point is the steady one. */
    fun lowPass(cutoffHz: Double) {
        val a = exp(-2 * PI * cutoffHz / rate)
        var y = 0.0
        repeat(2) {
            for (i in 0 until n) {
                y = a * y + (1 - a) * buf[i]
                if (it == 1) buf[i] = y
            }
        }
    }

    /**
     * Adds reverb: a mono Freeverb (eight damped combs, then four all-passes) with [room] as the combs'
     * feedback and [damp] how much high end each echo loses. The reverb is mixed in at [wet] times the dry
     * signal's level. It runs round the loop twice, so the tail of the end rings on into the start.
     */
    fun reverb(room: Double, damp: Double, wet: Double) {
        val scale = rate / 44_100.0
        val combs = intArrayOf(1557, 1617, 1491, 1422, 1277, 1356, 1188, 1116)
            .map { Comb((it * scale).roundToInt().coerceAtLeast(1), room, damp) }
        val allPasses = intArrayOf(556, 441, 341, 225).map { AllPass((it * scale).roundToInt().coerceAtLeast(1)) }
        val out = DoubleArray(n)
        repeat(2) { lap ->
            for (i in 0 until n) {
                var s = 0.0
                for (c in combs) s += c.process(buf[i])
                for (p in allPasses) s = p.process(s)
                if (lap == 1) out[i] = s
            }
        }
        val gain = wet * rms(buf) / rms(out).coerceAtLeast(1e-12)
        for (i in 0 until n) buf[i] += gain * out[i]
    }

    private class Comb(size: Int, val feedback: Double, val damp: Double) {
        private val line = DoubleArray(size)
        private var at = 0
        private var store = 0.0

        fun process(x: Double): Double {
            val y = line[at]
            store = y * (1 - damp) + store * damp
            line[at] = x + store * feedback
            if (++at == line.size) at = 0
            return y
        }
    }

    private class AllPass(size: Int) {
        private val line = DoubleArray(size)
        private var at = 0

        fun process(x: Double): Double {
            val delayed = line[at]
            line[at] = x + delayed * 0.5
            if (++at == line.size) at = 0
            return delayed - x
        }
    }

    private companion object {
        /** 0 at 0, 1 from 1 on, an S-curve between. */
        fun smooth(x: Double): Double = if (x >= 1) 1.0 else if (x <= 0) 0.0 else x * x * (3 - 2 * x)

        fun rms(x: DoubleArray): Double = sqrt(x.sumOf { it * it } / x.size)
    }
}

/** The partials of one note as running [Sine]s, each with its amplitude dying away by its own decay. */
private class Voice(hz: Double, partials: List<Partial>, phase: SeededRandom, rate: Int) {
    private val sines: Array<Sine>
    private val amps: DoubleArray
    private val decays: DoubleArray

    init {
        val audible = partials.filter { hz * it.ratio < 0.45 * rate }
        sines = Array(audible.size) { Sine(2 * PI * hz * audible[it].ratio / rate, 2 * PI * phase.nextDouble()) }
        amps = DoubleArray(audible.size) { audible[it].amp }
        decays = DoubleArray(audible.size) { val d = audible[it].decay; if (d.isFinite()) exp(-1 / (d * rate)) else 1.0 }
    }

    fun next(): Double {
        var s = 0.0
        for (i in sines.indices) {
            s += amps[i] * sines[i].next()
            amps[i] *= decays[i]
        }
        return s
    }
}

/**
 * sin(phase + k·[step]) for k = 0, 1, 2…, by the recurrence y(k) = 2·cos(step)·y(k-1) - y(k-2): one multiply
 * and one subtract a sample instead of a call to sin, and accurate over millions of samples in doubles.
 */
private class Sine(step: Double, phase: Double) {
    private val c = 2 * cos(step)
    private var y1 = sin(phase - step)
    private var y2 = sin(phase - 2 * step)

    fun next(): Double {
        val y = c * y1 - y2
        y2 = y1
        y1 = y
        return y
    }
}
