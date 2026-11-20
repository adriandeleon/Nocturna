package com.adl.nocturna.sound

import com.adl.nocturna.day.ReminderKind
import kotlin.math.abs
import kotlin.math.sqrt
import kotlin.test.Test
import kotlin.test.assertContentEquals
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class ReminderSoundSynthTest {
    private val rate = ReminderSoundSynth.SAMPLE_RATE
    private fun render(kind: ReminderKind) = ReminderSoundSynth.render(kind, rate)

    /** How many separate bursts of sound there are: 10 ms windows rising above a fifth of the loudest. */
    private fun FloatArray.bursts(): Int {
        val window = rate / 100
        val levels = (0 until size / window).map { w ->
            sqrt((w * window until (w + 1) * window).sumOf { this[it].toDouble() * this[it] } / window)
        }
        val threshold = levels.max() / 5
        var count = 0
        var quietFor = Int.MAX_VALUE
        for (l in levels) {
            if (l >= threshold) {
                // Under 100 ms of quiet is the same burst going on.
                if (quietFor >= 10) count++
                quietFor = 0
            } else if (quietFor != Int.MAX_VALUE) {
                quietFor++
            }
        }
        return count
    }

    @Test fun theSameEveryTime() = ReminderKind.entries.forEach {
        assertContentEquals(render(it), render(it), it.name)
    }

    @Test fun peaksAtTheSameLevel() = ReminderKind.entries.forEach { kind ->
        val samples = render(kind)
        assertEquals(ReminderSoundSynth.PEAK, samples.maxOf { abs(it) }, 1e-6f, kind.name)
    }

    @Test fun startsAndEndsInSilence() = ReminderKind.entries.forEach { kind ->
        val samples = render(kind)
        assertTrue(abs(samples.first()) < 0.01f, kind.name)
        assertEquals(0f, samples.last(), kind.name)
    }

    @Test fun shortEnoughForANudge() = ReminderKind.entries.forEach { kind ->
        val seconds = render(kind).size.toDouble() / rate
        assertTrue(seconds in 0.5..2.5, "${kind.name}: $seconds s")
    }

    @Test fun waterIsAFewDrops() = assertEquals(ReminderSoundSynth.DROPS, render(ReminderKind.WATER).bursts())

    @Test fun stretchIsAFewSteps() = assertEquals(ReminderSoundSynth.STEPS, render(ReminderKind.STRETCH).bursts())

    @Test fun togetherTheStepsComeFirstThenTheWater() {
        val steps = render(ReminderKind.STRETCH)
        val water = render(ReminderKind.WATER)
        val both = ReminderSoundSynth.render(setOf(ReminderKind.WATER, ReminderKind.STRETCH), rate)
        val gap = both.size - steps.size - water.size
        assertTrue(gap > 0)
        assertContentEquals(steps, both.copyOfRange(0, steps.size))
        assertTrue(both.copyOfRange(steps.size, steps.size + gap).all { it == 0f })
        assertContentEquals(water, both.copyOfRange(steps.size + gap, both.size))
    }

    @Test fun oneKindAloneIsJustItsSound() =
        assertContentEquals(render(ReminderKind.WATER), ReminderSoundSynth.render(setOf(ReminderKind.WATER), rate))

    @Test fun noKindsIsSilence() = assertEquals(0, ReminderSoundSynth.render(emptySet(), rate).size)
}
