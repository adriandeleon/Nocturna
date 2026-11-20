package com.adl.nocturna.sound

import com.adl.nocturna.day.ReminderKind
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.exp
import kotlin.math.roundToInt
import kotlin.math.sin

/**
 * The sounds of day mode's reminders, made on the phone like the [SleepSound]s: a few drops of water for
 * [ReminderKind.WATER], and footsteps on gravel for [ReminderKind.STRETCH]. Each is a second or two long,
 * starts and ends in silence, and peaks at [PEAK], so it plays at about the notification volume. Pure
 * Kotlin with fixed seeds, so they sound the same on every phone.
 */
object ReminderSoundSynth {
    const val SAMPLE_RATE = 44_100

    /** The loudest sample of every reminder sound. */
    const val PEAK = 0.5f

    /** Drops of water in [ReminderKind.WATER]. */
    const val DROPS = 3

    /** Footsteps in [ReminderKind.STRETCH]. */
    const val STEPS = 4

    /** Silence between the sounds of two reminders that come due together. */
    private const val GAP_SECONDS = 0.25

    /** The end of each sound fades out over this long, so it never stops on a click. */
    private const val FADE_OUT_SECONDS = 0.01

    /** One sound for [kinds] coming due together: the footsteps, then the water. Empty for none. */
    fun render(kinds: Collection<ReminderKind>, sampleRate: Int = SAMPLE_RATE): FloatArray {
        val parts = ReminderKind.entries.filter { it in kinds }.map { render(it, sampleRate) }
        if (parts.isEmpty()) return FloatArray(0)
        val gap = (GAP_SECONDS * sampleRate).roundToInt()
        val out = FloatArray(parts.sumOf { it.size } + gap * (parts.size - 1))
        var at = 0
        for (p in parts) {
            p.copyInto(out, at)
            at += p.size + gap
        }
        return out
    }

    /** The sound of one kind of reminder, in -[PEAK]..[PEAK]. */
    fun render(kind: ReminderKind, sampleRate: Int = SAMPLE_RATE): FloatArray {
        require(sampleRate > 0) { "Sample rate must be positive" }
        val out = when (kind) {
            ReminderKind.WATER -> water(sampleRate)
            ReminderKind.STRETCH -> footsteps(sampleRate)
        }
        return out.fadedOut(sampleRate).peakedAt(PEAK)
    }

    /**
     * Drops falling into a glass: each one a bubble ringing at a pitch that rises fast as it closes, dying
     * away in a few tens of milliseconds ("plip"). Three drops, a little apart, each at its own pitch.
     */
    private fun water(sampleRate: Int): FloatArray {
        val random = SeededRandom(0x5EED_0101)
        val out = FloatArray((1.0 * sampleRate).roundToInt())
        // When each drop lands (s), its starting pitch (Hz) and how loud it is.
        val drops = listOf(Triple(0.02, 820.0, 1.0), Triple(0.27, 1_050.0, 0.75), Triple(0.43, 700.0, 0.9))
        for ((start, pitch, level) in drops.take(DROPS)) {
            val from = (start * sampleRate).roundToInt()
            val wobble = 1 + 0.05 * random.nextSigned()
            addDrop(out, from, sampleRate, pitch * wobble, level)
        }
        return out
    }

    /** One drop into [out] from sample [from]: a sine gliding up from [pitch] to about twice it. */
    private fun addDrop(out: FloatArray, from: Int, sampleRate: Int, pitch: Double, level: Double) {
        val length = (0.18 * sampleRate).roundToInt()
        val attack = 0.002
        val decay = 0.035
        val glide = 0.04 // seconds for the pitch to double
        var phase = 0.0
        for (i in 0 until length) {
            val at = from + i
            if (at >= out.size) break
            val t = i.toDouble() / sampleRate
            val f = pitch * (1 + (t / glide).coerceAtMost(2.5))
            phase += 2 * PI * f / sampleRate
            val env = (t / attack).coerceAtMost(1.0) * exp(-t / decay)
            out[at] += (level * env * sin(phase)).toFloat()
        }
    }

    /**
     * Unhurried steps on a gravel path: each a soft thud of the heel and the crunch of the stones under it,
     * the second foot a touch lighter than the first.
     */
    private fun footsteps(sampleRate: Int): FloatArray {
        val random = SeededRandom(0x5EED_0102)
        val stride = 0.46
        val out = FloatArray((((STEPS - 1) * stride + 0.3) * sampleRate).roundToInt())
        for (s in 0 until STEPS) {
            // Not quite in time, like a person walking.
            val from = ((0.03 + s * stride + 0.02 * random.nextSigned()) * sampleRate).roundToInt()
            addStep(out, from, sampleRate, level = if (s % 2 == 0) 1.0 else 0.8, random = random)
        }
        return out
    }

    /** One footstep into [out] from sample [from]: the heel's thud, then the gravel crunching. */
    private fun addStep(out: FloatArray, from: Int, sampleRate: Int, level: Double, random: SeededRandom) {
        // The heel: low, muffled noise that dies away fast.
        val thud = SleepSoundSynth.lowPass(SleepSoundSynth.lowPass(SleepSoundSynth.white(random), sampleRate, 500.0), sampleRate, 500.0)
        val thudLength = (0.09 * sampleRate).roundToInt()
        for (i in 0 until thudLength) {
            val at = from + i
            if (at >= out.size) break
            val t = i.toDouble() / sampleRate
            val env = (t / 0.003).coerceAtMost(1.0) * exp(-t / 0.022)
            out[at] += (6.0 * level * env * thud()).toFloat()
        }
        // The gravel: a burst of tiny clicks of bright noise, thick at first and thinning out, as the sole
        // rolls from heel to toe.
        val grit = SleepSoundSynth.highPass(SleepSoundSynth.lowPass(SleepSoundSynth.white(random), sampleRate, 5_000.0), sampleRate, 900.0)
        val crunchLength = (0.17 * sampleRate).roundToInt()
        val clickDecay = exp(-1.0 / (0.0015 * sampleRate))
        var click = 0.0
        for (i in 0 until crunchLength) {
            val at = from + (0.008 * sampleRate).roundToInt() + i
            if (at >= out.size) break
            val t = i.toDouble() / sampleRate
            val density = 1_400.0 * exp(-t / 0.06) / sampleRate
            if (random.nextDouble() < density) click += 0.3 + 0.7 * random.nextDouble()
            click *= clickDecay
            out[at] += (0.9 * level * click * grit()).toFloat()
        }
    }

    /** The last [FADE_OUT_SECONDS] ramped down to silence. */
    private fun FloatArray.fadedOut(sampleRate: Int): FloatArray {
        val fade = (FADE_OUT_SECONDS * sampleRate).roundToInt().coerceAtMost(size)
        for (i in 0 until fade) this[size - 1 - i] *= i.toFloat() / fade
        return this
    }

    /** Scaled so its loudest sample is [peak]. */
    private fun FloatArray.peakedAt(peak: Float): FloatArray {
        val max = maxOfOrNull { abs(it) } ?: 0f
        if (max > 0f) for (i in indices) this[i] = this[i] / max * peak
        return this
    }
}
