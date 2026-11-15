package com.adl.nocturna.clock

import com.adl.nocturna.ui.NightRed
import kotlin.math.abs
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue
import kotlin.time.Duration.Companion.milliseconds
import kotlin.time.Duration.Companion.seconds

class PeekTest {
    private fun near(expected: Float, actual: Float) = assertTrue(abs(expected - actual) < 1e-4f, "expected $expected, was $actual")

    @Test fun noPeekWithoutATap() {
        assertEquals(0f, peekLevel(null))
        assertEquals(0f, peekLevel((-1).seconds))
    }

    @Test fun aPeekHoldsForSixSecondsThenFadesOverTwo() {
        assertEquals(1f, peekLevel(0.seconds))
        assertEquals(1f, peekLevel(3.seconds))
        assertEquals(1f, peekLevel(6.seconds))
        near(0.5f, peekLevel(7.seconds))
        assertEquals(0f, peekLevel(8.seconds))
        assertEquals(0f, peekLevel(60.seconds))
    }

    @Test fun theFadeOnlyGoesDown() {
        var last = 1f
        for (ms in 6_000..8_000 step 50) {
            val level = peekLevel(ms.milliseconds)
            assertTrue(level <= last, "rose at $ms ms")
            last = level
        }
        assertEquals(0f, last)
    }

    @Test fun aPeekBringsTheScreenToFortyPercentOfTheBrightest() {
        near(0.32f, peekBrightness(room = 0.02f, sunrise = 0.02f, max = 0.8f, level = 1f))
        near(0.17f, peekBrightness(room = 0.02f, sunrise = 0.02f, max = 0.8f, level = 0.5f))
        assertEquals(0.02f, peekBrightness(room = 0.02f, sunrise = 0.02f, max = 0.8f, level = 0f))
    }

    @Test fun theBrighterOfSunriseAndPeekWins() {
        // Early in a sunrise the peek is brighter; late in one the sunrise is.
        near(0.32f, peekBrightness(room = 0.02f, sunrise = 0.1f, max = 0.8f, level = 1f))
        assertEquals(0.6f, peekBrightness(room = 0.02f, sunrise = 0.6f, max = 0.8f, level = 1f))
    }

    @Test fun aPeekNeverDimsTheScreen() =
        assertEquals(0.3f, peekBrightness(room = 0.3f, sunrise = 0.3f, max = 0.5f, level = 1f))

    @Test fun thePhonesOwnBrightnessStaysSo() =
        assertEquals(SYSTEM_BRIGHTNESS, peekBrightness(SYSTEM_BRIGHTNESS, SYSTEM_BRIGHTNESS, 0.8f, 1f))

    @Test fun aPeekWarmsTheColourPartWayAlongTheSunrise() {
        assertEquals(PEEK_SUNRISE_PROGRESS, peekProgress(sunrise = 0f, level = 1f))
        assertEquals(0f, peekProgress(sunrise = 0f, level = 0f))
        assertEquals(0.9f, peekProgress(sunrise = 0.9f, level = 1f))
        // Not night red any more, and not yet the sunrise's warm white.
        val peek = sunriseColor(NightRed, night = true, peekProgress(0f, 1f))
        assertTrue(peek != NightRed && peek != SunriseWhite)
    }

    @Test fun peeksOnlyWhenTheClockIsDim() {
        // Night mode: always at the darkest level.
        assertTrue(canPeek(enabled = true, day = false, room = 0.02f, current = 0.02f, min = 0.02f, max = 0.8f))
        // A dark room, just above the darkest level.
        assertTrue(canPeek(enabled = true, day = false, room = 0.05f, current = 0.05f, min = 0.02f, max = 0.8f))
        // A lit room.
        assertFalse(canPeek(enabled = true, day = false, room = 0.3f, current = 0.3f, min = 0.02f, max = 0.8f))
    }

    @Test fun noPeekWhenOffInDayModeOrOnThePhonesBrightness() {
        assertFalse(canPeek(enabled = false, day = false, room = 0.02f, current = 0.02f, min = 0.02f, max = 0.8f))
        assertFalse(canPeek(enabled = true, day = true, room = 0.02f, current = 0.02f, min = 0.02f, max = 0.8f))
        assertFalse(canPeek(true, false, SYSTEM_BRIGHTNESS, SYSTEM_BRIGHTNESS, 0.02f, 0.8f))
    }

    @Test fun noPeekWhenTheSunriseIsAlreadyBrighter() =
        assertFalse(canPeek(enabled = true, day = false, room = 0.02f, current = 0.5f, min = 0.02f, max = 0.8f))

    @Test fun theFirstTapPeeksAndTheSecondShowsTheControls() {
        assertEquals(ClockTap.PEEK, clockTap(controlsVisible = false, peeking = false, canPeek = true))
        assertEquals(ClockTap.SHOW_CONTROLS, clockTap(controlsVisible = false, peeking = true, canPeek = true))
        assertEquals(ClockTap.HIDE_CONTROLS, clockTap(controlsVisible = true, peeking = true, canPeek = true))
    }

    @Test fun withoutAPeekATapTogglesTheControlsAsBefore() {
        assertEquals(ClockTap.SHOW_CONTROLS, clockTap(controlsVisible = false, peeking = false, canPeek = false))
        assertEquals(ClockTap.HIDE_CONTROLS, clockTap(controlsVisible = true, peeking = false, canPeek = false))
    }
}
