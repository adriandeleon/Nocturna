package com.adl.nocturna.alarm

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class SilenceWatchdogTest {
    @Test fun streamThatNeverStartsExpiresAfterTimeout() {
        val w = SilenceWatchdog(15_000, start = 1_000)
        w.update(playing = false, now = 15_999)
        assertFalse(w.expired(15_999))
        w.update(playing = false, now = 16_000)
        assertTrue(w.expired(16_000))
    }

    @Test fun playingStreamNeverExpires() {
        val w = SilenceWatchdog(15_000, start = 0)
        w.update(playing = true, now = 2_000)
        assertFalse(w.expired(60_000))
    }

    @Test fun stallMidStreamCountsFromWhenPlaybackStopped() {
        val w = SilenceWatchdog(15_000, start = 0)
        w.update(playing = true, now = 3_000)
        w.update(playing = false, now = 40_000)
        assertFalse(w.expired(54_999))
        w.update(playing = false, now = 55_000)
        assertTrue(w.expired(55_000))
    }

    @Test fun recoveringBeforeTimeoutResetsTheCount() {
        val w = SilenceWatchdog(15_000, start = 0)
        w.update(playing = false, now = 10_000)
        w.update(playing = true, now = 12_000)
        w.update(playing = false, now = 20_000)
        assertFalse(w.expired(34_000))
        assertTrue(w.expired(35_000))
    }
}
