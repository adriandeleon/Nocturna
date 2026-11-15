package com.adl.nocturna.clock

import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.LifecycleRegistry
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import kotlin.time.Duration.Companion.milliseconds
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

private const val N = true
private const val F = false

class WaveTest {
    /** Feeds (millis, near) readings to a new detector and returns the millis at which waves came out. */
    private fun waves(vararg readings: Pair<Int, Boolean>): List<Int> {
        val detector = WaveDetector()
        return readings.filter { (ms, near) -> detector.feed(ms.milliseconds, near) != null }.map { it.first }
    }

    /** A quick wave: near at [start], far [heldMs] later. */
    private fun wave(start: Int, heldMs: Int = 300) = listOf(start to N, start + heldMs to F)

    @Test fun aQuickWaveIsAWave() = assertEquals(listOf(1300), waves(0 to F, 1000 to N, 1300 to F))

    @Test fun aWaveAtTheLimitStillCounts() = assertEquals(listOf(2500), waves(0 to F, 1000 to N, 2500 to F))

    @Test fun aSlowCoverIsNotAWave() = assertEquals(emptyList(), waves(0 to F, 1000 to N, 2700 to F))

    @Test fun aGlitchOfNearIsNotAWave() = assertEquals(emptyList(), waves(0 to F, 1000 to N, 1010 to F))

    @Test fun farOnItsOwnIsNothing() = assertEquals(emptyList(), waves(0 to F, 500 to F, 1000 to F))

    @Test fun aLongCoverThenItsReleaseIsNotAWave() =
        assertEquals(emptyList(), waves(0 to F, 1000 to N, 9000 to F))

    @Test fun aRepeatedNearDuringALongCoverChangesNothing() =
        assertEquals(emptyList(), waves(0 to F, 1000 to N, 3500 to N, 3600 to F))

    @Test fun afterALongCoverWavesCountOnceItHasSettled() {
        val readings = listOf(0 to F, 1000 to N, 9000 to F) + wave(9300) + wave(10500)
        // The first wave ends 600 ms after the cover lifted: picking the phone up, not a wave.
        assertEquals(listOf(10800), waves(*readings.toTypedArray()))
    }

    @Test fun nearWhenTheSensorStartsIsACoverNotAWave() {
        // Face down when the alarm went off: near first, then lifted 300 ms later.
        assertEquals(emptyList(), waves(0 to N, 300 to F))
        assertEquals(listOf(2800), waves(*(listOf(0 to N, 300 to F) + wave(2500)).toTypedArray()))
    }

    @Test fun repeatedWavesEachCount() {
        val readings = listOf(0 to F) + wave(1000) + wave(2600) + wave(4200)
        assertEquals(listOf(1300, 2900, 4500), waves(*readings.toTypedArray()))
    }

    @Test fun aSecondWaveTooSoonIsDebounced() {
        val readings = listOf(0 to F) + wave(1000) + wave(1500) + wave(3000)
        assertEquals(listOf(1300, 3300), waves(*readings.toTypedArray()))
    }

    @Test fun noisyFlappingIsOneWave() {
        // The sensor flaps near and far every 50 ms for two seconds, then a real wave a second later.
        val flapping = (1000 until 3000 step 50).map { it to ((it / 50) % 2 == 0) } + (3000 to F)
        val readings = listOf(0 to F) + flapping + wave(4500)
        assertEquals(listOf(1050, 4800), waves(*readings.toTypedArray()))
    }

    @Test fun timeGoingBackwardsIsNotAWave() = assertEquals(emptyList(), waves(0 to F, 1000 to N, 900 to F))

    @Test fun nearIsUnderTheRangeOrFiveCentimetres() {
        assertTrue(proximityIsNear(0f, 5f))
        assertFalse(proximityIsNear(5f, 5f))
        assertTrue(proximityIsNear(0f, 1f))
        assertFalse(proximityIsNear(1f, 1f))
        // A sensor that measures up to 10 cm: near is under 5.
        assertTrue(proximityIsNear(3f, 10f))
        assertFalse(proximityIsNear(6f, 10f))
        // A sensor with no sensible range still works.
        assertTrue(proximityIsNear(0f, 0f))
    }

    @Test fun aWavePeeksOnlyWhereATapWould() {
        assertTrue(clockWave(controlsVisible = false, peeking = false, canPeek = true))
        assertFalse(clockWave(controlsVisible = false, peeking = false, canPeek = false))
        // During a peek, a wave starts it again instead of showing the controls.
        assertTrue(clockWave(controlsVisible = false, peeking = true, canPeek = false))
        assertFalse(clockWave(controlsVisible = true, peeking = false, canPeek = true))
    }

    private class FakeProximity : Proximity {
        val flow = MutableSharedFlow<ProximityReading>(extraBufferCapacity = 16)
        val listening get() = flow.subscriptionCount.value > 0
        override fun readings() = flow

        fun send(ms: Int, near: Boolean) = check(flow.tryEmit(ProximityReading(ms.milliseconds, near)))
    }

    @OptIn(ExperimentalCoroutinesApi::class)
    @Test fun theSensorIsOnOnlyWhileTheScreenIsStarted() = runTest {
        Dispatchers.setMain(UnconfinedTestDispatcher(testScheduler))
        try {
            val owner = object : LifecycleOwner {
                override val lifecycle = LifecycleRegistry.createUnsafe(this)
            }
            val registry = owner.lifecycle
            registry.handleLifecycleEvent(Lifecycle.Event.ON_CREATE)
            val proximity = FakeProximity()
            var count = 0
            val job = launch(UnconfinedTestDispatcher(testScheduler)) { listenForWaves(registry, proximity) { count++ } }
            runCurrent()
            assertFalse(proximity.listening, "off before the screen starts")

            registry.handleLifecycleEvent(Lifecycle.Event.ON_START)
            runCurrent()
            assertTrue(proximity.listening, "on while started")
            proximity.send(0, false)
            proximity.send(1000, true)
            proximity.send(1300, false)
            runCurrent()
            assertEquals(1, count)

            registry.handleLifecycleEvent(Lifecycle.Event.ON_STOP)
            runCurrent()
            assertFalse(proximity.listening, "off once stopped")

            registry.handleLifecycleEvent(Lifecycle.Event.ON_START)
            runCurrent()
            assertTrue(proximity.listening, "on again once started again")

            job.cancel()
            runCurrent()
            assertFalse(proximity.listening, "off once the screen leaves")
        } finally {
            Dispatchers.resetMain()
        }
    }
}
