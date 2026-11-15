package com.adl.nocturna.day

import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue
import kotlin.test.Test
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
            assertTrue(abs(x) < room, "x=$x at $s")
            assertTrue(abs(y) < room, "y=$y at $s")
        }
    }

    @Test fun photoKeepsMoving() {
        assertTrue(photoPan(0f) != photoPan(600f))
    }

    @Test fun sunCrossesTheSkyFromDawnToDusk() {
        assertEquals(0f, skyTrip(SUNRISE, SUNRISE, SUNSET), 0.0001f)
        assertEquals(0.5f, skyTrip((SUNRISE + SUNSET) / 2, SUNRISE, SUNSET), 0.0001f)
        assertEquals(1f, skyTrip(SUNSET, SUNRISE, SUNSET), 0.0001f)
        // Held at the far side while it's down.
        assertEquals(1f, skyTrip(23 * 3600f, SUNRISE, SUNSET), 0.0001f)
    }

    @Test fun moonCrossesTheSkyOverMidnight() {
        assertEquals(0f, skyTrip(MOONRISE, MOONRISE, MOONSET), 0.0001f)
        val midnight = skyTrip(0f, MOONRISE, MOONSET)
        assertTrue(midnight > 0.3f && midnight < 0.7f)
        assertTrue(skyTrip(3 * 3600f, MOONRISE, MOONSET) > midnight)
        assertEquals(1f, skyTrip(MOONSET, MOONRISE, MOONSET), 0.0001f)
    }

    @Test fun sunGoesLeftToRightAndPeaksInTheMiddle() {
        val (x0, y0) = skyArc(0f)
        val (xm, ym) = skyArc(0.5f)
        val (x1, y1) = skyArc(1f)
        assertTrue(x0 < xm && xm < x1)
        assertTrue(x0 > 0f && x1 < 1f)
        assertTrue(ym < y0 && ym < y1, "higher at noon")
        // Always at least partly on screen: its centre is less than a radius above the top edge.
        assertTrue(ym > -1f)
    }

    @Test fun sunKeepsMovingAcrossTheDay() {
        val positions = (6..19).map { skyArc(skyTrip(it * 3600f, SUNRISE, SUNSET)).first }
        assertTrue(positions.zipWithNext().all { (a, b) -> b > a })
    }

    @Test fun cloudsFloatUpAndDownWithinABand() {
        for (i in 0..2) {
            for (s in 0 until 86_400 step 60) assertTrue(abs(cloudBob(s.toFloat(), i)) <= 0.035f)
            assertTrue(cloudBob(0f, i) != cloudBob(600f, i))
        }
    }

    @Test fun hillsRiseAndSinkALittle() {
        for (layer in 0..2) {
            for (s in 0 until 86_400 step 60) assertTrue(abs(hillRise(s.toFloat(), layer)) <= 0.012f)
            assertTrue(hillRise(0f, layer) != hillRise(900f, layer))
        }
    }

    @Test fun starsTurnSixDegreesAnHour() = assertEquals(6f, starTurn(3600f) - starTurn(0f), 0.0001f)

    @Test fun fieldIsReplantedEveryHour() {
        val ten = planting(10 * 3600f)
        assertEquals(10, ten.now)
        assertEquals(9, ten.before)
        assertEquals(0f, ten.grown, 0.0001f)
        assertEquals(0.5f, planting(10 * 3600f + 150f).grown, 0.0001f)
        assertEquals(1f, planting(10 * 3600f + 1800f).grown, 0.0001f)
        assertTrue(planting(10 * 3600f).now != planting(11 * 3600f).now)
    }

    @Test fun fieldCarriesOverMidnight() {
        val midnight = planting(0f)
        assertEquals(planting(86_399f).now, midnight.before)
    }
}
