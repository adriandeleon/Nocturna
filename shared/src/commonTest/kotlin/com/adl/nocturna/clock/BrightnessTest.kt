package com.adl.nocturna.clock

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class BrightnessTest {
    private fun brightness(lux: Float?, day: Boolean = false, night: Boolean = false, dim: Boolean = true, auto: Boolean = true) =
        screenBrightness(lux, night, day, dim, auto, min = 0.02f, max = 0.8f, dayMax = 0.5f)

    @Test fun followsTheRoomUpToTheClocksLimit() {
        assertEquals(0.02f, brightness(0f), 0.0001f)
        assertEquals(0.8f, brightness(1000f), 0.0001f)
        assertEquals(0.8f, brightness(50_000f), 0.0001f)
        assertTrue(brightness(10f) < brightness(100f))
    }

    @Test fun dayModeStopsAtItsOwnLimit() {
        assertEquals(0.5f, brightness(1000f, day = true), 0.0001f)
        assertEquals(0.5f, brightness(50_000f, day = true), 0.0001f)
        assertEquals(0.02f, brightness(0f, day = true), 0.0001f)
        assertTrue(brightness(100f, day = true) < brightness(100f))
    }

    @Test fun dayLimitNeverRaisesTheClocksLimit() =
        assertEquals(0.4f, screenBrightness(1000f, false, true, true, true, min = 0.02f, max = 0.4f, dayMax = 0.7f), 0.0001f)

    @Test fun dayLimitNeverGoesBelowTheDarkest() =
        assertEquals(0.3f, screenBrightness(1000f, false, true, true, true, min = 0.3f, max = 0.8f, dayMax = 0.2f), 0.0001f)

    @Test fun nightModeIsTheDarkest() = assertEquals(0.02f, brightness(1000f, night = true), 0.0001f)

    @Test fun phoneBrightnessWhenNotFollowingTheRoom() {
        assertEquals(SYSTEM_BRIGHTNESS, brightness(100f, dim = false))
        assertEquals(SYSTEM_BRIGHTNESS, brightness(100f, auto = false))
        assertEquals(SYSTEM_BRIGHTNESS, brightness(null, day = true))
    }
}
