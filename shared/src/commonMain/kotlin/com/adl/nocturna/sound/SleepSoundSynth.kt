package com.adl.nocturna.sound

import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.cos
import kotlin.math.exp
import kotlin.math.pow
import kotlin.math.roundToInt
import kotlin.math.sin
import kotlin.math.sqrt
import kotlin.math.tanh

/**
 * Makes the [SleepSound]s: a few tens of seconds of mono audio that loop without a click. The music comes
 * from [AmbientMusic]; the rest of this file makes the noises. Pure Kotlin, so the
 * same seed gives the same samples on every platform.
 *
 * Each sound is a noise generator run for one loop plus [FADE_SECONDS] more; the extra tail is crossfaded
 * into the start, so the last sample of the loop leads straight into the first one, as if the generator had
 * kept running ([seamlessLoop]).
 */
object SleepSoundSynth {
    const val SAMPLE_RATE = 44_100
    const val FADE_SECONDS = 1.0

    /** Filters settle for this long before the loop starts, so it doesn't open on a ramp. */
    private const val WARM_UP_SECONDS = 0.5

    /** Waves in one [SleepSound.SURF] loop. */
    const val SURF_WAVES = 4

    /** The loop for [sound], in -1..1. */
    fun render(
        sound: SleepSound,
        sampleRate: Int = SAMPLE_RATE,
        seconds: Double = sound.loopSeconds.toDouble(),
        seed: Long = sound.seed,
    ): FloatArray {
        require(sampleRate > 0) { "Sample rate must be positive" }
        require(seconds >= 2 * FADE_SECONDS) { "A loop must be at least ${2 * FADE_SECONDS} s long" }
        val n = (seconds * sampleRate).roundToInt()
        val fade = (FADE_SECONDS * sampleRate).roundToInt()
        val random = SeededRandom(seed)
        fun loop(source: () -> Float) = seamlessLoop(n, fade, (WARM_UP_SECONDS * sampleRate).roundToInt(), source)

        val out = when (sound) {
            SleepSound.WHITE -> loop(white(random))
            SleepSound.PINK -> loop(pink(random))
            SleepSound.BROWN -> loop(brown(random, sampleRate, cutoffHz = 50.0))
            SleepSound.RAIN -> {
                // Hiss of distant rain, close drops pattering on top, and a little low rumble beneath.
                val hiss = loop(highPass(lowPass(pink(random), sampleRate, 7_000.0), sampleRate, 500.0)).normalized()
                val drops = loop(drops(random, sampleRate)).normalized()
                val rumble = loop(brown(random, sampleRate, cutoffHz = 150.0)).normalized()
                FloatArray(n) { hiss[it] + 0.7f * drops[it] + 0.4f * rumble[it] }
            }
            SleepSound.CHORDS, SleepSound.CHIMES, SleepSound.DRONE -> AmbientMusic.render(sound, sampleRate, n, random)
            SleepSound.SURF -> {
                // Brown noise swells with each wave; a brighter wash of foam comes in at the crest.
                val body = loop(brown(random, sampleRate, cutoffHz = 90.0)).normalized()
                val foam = loop(highPass(pink(random), sampleRate, 400.0)).normalized()
                val swell = surfEnvelope(n, SURF_WAVES, random)
                FloatArray(n) {
                    val e = swell[it]
                    body[it] * (0.25f + 0.75f * e) + foam[it] * 0.6f * e * e
                }
            }
        }
        return out.leveled(sound.targetRms)
    }

    /** How loud each sound plays; noise with more high end sounds louder at the same level. */
    private val SleepSound.targetRms: Float
        get() = when (this) {
            SleepSound.WHITE -> 0.09f
            SleepSound.PINK -> 0.12f
            SleepSound.BROWN -> 0.16f
            SleepSound.RAIN -> 0.12f
            SleepSound.SURF -> 0.15f
            // Music has peaks well above its average, so it sits lower to sound as loud as the noises.
            SleepSound.CHORDS -> 0.11f
            SleepSound.CHIMES -> 0.07f
            SleepSound.DRONE -> 0.12f
        }

    /**
     * [n] samples of [source] that loop seamlessly. The source runs for [warmUp] samples, then [n] + [fade];
     * the first [fade] samples of the loop are an equal-power crossfade from the [fade] samples that followed
     * the loop's end into the loop's own start, so the loop point joins two samples the source made in a row.
     */
    internal fun seamlessLoop(n: Int, fade: Int, warmUp: Int, source: () -> Float): FloatArray {
        require(fade in 1..n) { "The crossfade must fit in the loop" }
        repeat(warmUp) { source() }
        val raw = FloatArray(n + fade) { source() }
        val out = raw.copyOf(n)
        for (i in 0 until fade) {
            val t = (i + 0.5) / fade * (PI / 2)
            out[i] = (raw[i] * sin(t) + raw[n + i] * cos(t)).toFloat()
        }
        return out
    }

    /**
     * One loop's loudness of the surf, 0..1: [waves] swells of slightly different lengths and heights, each
     * swelling for three or four seconds to its crest and then washing out more slowly. It is 0 at both ends,
     * so it loops too.
     */
    internal fun surfEnvelope(n: Int, waves: Int, random: SeededRandom): FloatArray {
        val lengths = DoubleArray(waves) { 0.8 + 0.4 * random.nextDouble() }
        val heights = DoubleArray(waves) { 0.65 + 0.35 * random.nextDouble() }
        val total = lengths.sum()
        val out = FloatArray(n)
        var sum = 0.0
        for (w in 0 until waves) {
            val start = (n * sum / total).roundToInt()
            sum += lengths[w]
            val end = (n * sum / total).roundToInt()
            for (i in start until end) {
                val p = (i - start).toDouble() / (end - start)
                // p^0.6 puts the crest about a third of the way in, after a slow swell; the longer tail is the wash.
                val s = sin(PI * p.pow(0.6))
                out[i] = (heights[w] * s * s).toFloat()
            }
        }
        return out
    }

    internal fun white(random: SeededRandom): () -> Float = { random.nextSigned() }

    /** Paul Kellet's pink noise filter: within 0.05 dB of -3 dB per octave above 9 Hz at 44.1 kHz. */
    private fun pink(random: SeededRandom): () -> Float {
        var b0 = 0.0; var b1 = 0.0; var b2 = 0.0; var b3 = 0.0; var b4 = 0.0; var b5 = 0.0; var b6 = 0.0
        return {
            val w = random.nextSigned().toDouble()
            b0 = 0.99886 * b0 + w * 0.0555179
            b1 = 0.99332 * b1 + w * 0.0750759
            b2 = 0.96900 * b2 + w * 0.1538520
            b3 = 0.86650 * b3 + w * 0.3104856
            b4 = 0.55000 * b4 + w * 0.5329522
            b5 = -0.7616 * b5 - w * 0.0168980
            val out = b0 + b1 + b2 + b3 + b4 + b5 + b6 + w * 0.5362
            b6 = w * 0.115926
            (out * 0.11).toFloat()
        }
    }

    /** White noise through a one-pole low pass: -6 dB per octave above [cutoffHz], flat below it. */
    private fun brown(random: SeededRandom, sampleRate: Int, cutoffHz: Double): () -> Float =
        lowPass(white(random), sampleRate, cutoffHz)

    /**
     * Raindrops: random taps of high-passed noise that die away in a few milliseconds, many small ones and a
     * few big ones.
     */
    private fun drops(random: SeededRandom, sampleRate: Int): () -> Float {
        val noise = highPass(white(random), sampleRate, 1_800.0)
        val smallDecay = exp(-1.0 / (0.0025 * sampleRate))
        val bigDecay = exp(-1.0 / (0.012 * sampleRate))
        val smallChance = 90.0 / sampleRate
        val bigChance = 4.0 / sampleRate
        var small = 0.0
        var big = 0.0
        return {
            if (random.nextDouble() < smallChance) small += (0.2 + 0.8 * random.nextDouble()).pow(2)
            if (random.nextDouble() < bigChance) big += 0.5 + 0.5 * random.nextDouble()
            small *= smallDecay
            big *= bigDecay
            (noise() * (0.5 * small + big)).toFloat()
        }
    }

    internal fun lowPass(source: () -> Float, sampleRate: Int, cutoffHz: Double): () -> Float {
        val a = exp(-2 * PI * cutoffHz / sampleRate)
        var y = 0.0
        return {
            y = a * y + (1 - a) * source()
            y.toFloat()
        }
    }

    internal fun highPass(source: () -> Float, sampleRate: Int, cutoffHz: Double): () -> Float {
        val a = exp(-2 * PI * cutoffHz / sampleRate)
        var low = 0.0
        return {
            val x = source().toDouble()
            low = a * low + (1 - a) * x
            (x - low).toFloat()
        }
    }

    private fun FloatArray.rms(): Double = if (isEmpty()) 0.0 else sqrt(sumOf { it.toDouble() * it } / size)

    /** Scaled to an RMS of 1. */
    private fun FloatArray.normalized(): FloatArray {
        val r = rms()
        if (r > 0) for (i in indices) this[i] = (this[i] / r).toFloat()
        return this
    }

    /**
     * Scaled to [targetRms], with the rare peaks above [KNEE] rounded off smoothly so that every sample stays
     * inside -1..1 without a hard clip.
     */
    private fun FloatArray.leveled(targetRms: Float): FloatArray {
        val gain = targetRms / rms().coerceAtLeast(1e-9)
        for (i in indices) {
            val x = this[i] * gain
            val m = abs(x)
            val y = if (m <= KNEE) m else KNEE + (1 - KNEE) * tanh((m - KNEE) / (1 - KNEE))
            this[i] = (if (x < 0) -y else y).toFloat()
        }
        return this
    }

    private const val KNEE = 0.8
}

/** SplitMix64: a small, fast generator that gives the same numbers for a seed on every platform. */
class SeededRandom(seed: Long) {
    private var state = seed

    fun nextLong(): Long {
        state += -0x61c8864680b583ebL // 0x9E3779B97F4A7C15
        var z = state
        z = (z xor (z ushr 30)) * -0x40a7b892e31b1a47L // 0xBF58476D1CE4E5B9
        z = (z xor (z ushr 27)) * -0x6b2fb644ecceee15L // 0x94D049BB133111EB
        return z xor (z ushr 31)
    }

    /** Uniform in [0, 1). */
    fun nextDouble(): Double = (nextLong() ushr 11) * (1.0 / (1L shl 53))

    /** Uniform in [-1, 1). */
    fun nextSigned(): Float = (nextDouble() * 2 - 1).toFloat()
}
