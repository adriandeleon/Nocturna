package com.adl.nocturna.day

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.math.abs

class BurnInTest {
    @Test fun sidesSwapEveryOtherHour() {
        assertFalse(sidesSwapped(10, burnInProtection = true))
        assertTrue(sidesSwapped(11, burnInProtection = true))
        assertFalse(sidesSwapped(12, burnInProtection = true))
    }

    @Test fun sidesStayPutWithoutBurnInProtection() =
        assertTrue((0..23).none { sidesSwapped(it, burnInProtection = false) })

    @Test fun nearHillsScrollFasterThanFarOnes() {
        val s = 5_000f
        assertTrue(hillShift(s, 2) > hillShift(s, 1))
        assertTrue(hillShift(s, 1) > hillShift(s, 0))
        assertEquals(1f, hillShift(4 * 3600f, 2), 0.0001f)
    }

    @Test fun photoNeverPansPastItsExtraRoom() {
        val room = (PHOTO_SCALE - 1f) / 2f
        for (s in 0 until 86_400 step 60) {
            val (x, y) = photoPan(s.toFloat())
            assertTrue("x=$x at $s", abs(x) < room)
            assertTrue("y=$y at $s", abs(y) < room)
        }
    }

    @Test fun photoKeepsMoving() {
        assertTrue(photoPan(0f) != photoPan(600f))
    }
}
