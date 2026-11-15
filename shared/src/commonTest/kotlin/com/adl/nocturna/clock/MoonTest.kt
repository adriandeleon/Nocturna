package com.adl.nocturna.clock

import com.adl.nocturna.text.testLocale
import kotlin.math.abs
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue
import kotlin.time.Instant

class MoonTest {
    /** Days between [at]'s computed phase and [target] (0 new, 0.5 full), the short way round the cycle. */
    private fun daysOff(at: String, target: Double): Double {
        val diff = moonCycle(Instant.parse(at)) - target
        return abs(diff - kotlin.math.round(diff)) * SYNODIC_MONTH_DAYS
    }

    // New and full moons from the published tables (eclipses, so easy to check).
    private val newMoons = listOf(
        "2017-08-21T18:30:00Z",
        "2024-04-08T18:21:00Z",
        "2025-09-21T19:54:00Z",
        "2026-08-12T17:37:00Z",
    )
    private val fullMoons = listOf(
        "2000-01-21T04:40:00Z",
        "2022-11-08T11:02:00Z",
        "2024-01-25T17:54:00Z",
        "2025-03-14T06:55:00Z",
        "2026-03-03T11:38:00Z",
    )

    @Test fun newMoonsAreWithinADay() = newMoons.forEach { assertTrue(daysOff(it, 0.0) < 1.0, it) }

    @Test fun fullMoonsAreWithinADay() = fullMoons.forEach { assertTrue(daysOff(it, 0.5) < 1.0, it) }

    @Test fun knownDatesGetTheirNames() {
        newMoons.forEach { assertEquals(MoonPhase.NEW, MoonPhase.at(Instant.parse(it)), it) }
        fullMoons.forEach { assertEquals(MoonPhase.FULL, MoonPhase.at(Instant.parse(it)), it) }
    }

    @Test fun cycleStaysInRangeBeforeTheReference() {
        val c = moonCycle(Instant.parse("1969-07-20T20:17:00Z"))
        // Apollo 11 landed under a waxing moon, about six days after the new moon and before the first quarter.
        assertTrue(c > 0.15 && c < 0.25, "$c")
    }

    @Test fun eachEighthHasItsPhase() {
        val expected = MoonPhase.entries
        for (i in 0 until 8) assertEquals(expected[i], MoonPhase.of(i / 8.0), "cycle ${i / 8.0}")
        assertEquals(MoonPhase.NEW, MoonPhase.of(0.97))
        assertEquals(MoonPhase.NEW, MoonPhase.of(0.06))
        assertEquals(MoonPhase.WAXING_CRESCENT, MoonPhase.of(0.07))
        assertEquals(MoonPhase.FULL, MoonPhase.of(0.56))
        assertEquals(MoonPhase.WANING_GIBBOUS, MoonPhase.of(0.57))
    }

    @Test fun labels() {
        assertEquals("New moon", MoonPhase.NEW.label(testLocale))
        assertEquals("Waxing crescent", MoonPhase.WAXING_CRESCENT.label(testLocale))
        assertEquals("First quarter", MoonPhase.FIRST_QUARTER.label(testLocale))
        assertEquals("Waxing gibbous", MoonPhase.WAXING_GIBBOUS.label(testLocale))
        assertEquals("Full moon", MoonPhase.FULL.label(testLocale))
        assertEquals("Waning gibbous", MoonPhase.WANING_GIBBOUS.label(testLocale))
        assertEquals("Last quarter", MoonPhase.LAST_QUARTER.label(testLocale))
        assertEquals("Waning crescent", MoonPhase.WANING_CRESCENT.label(testLocale))
    }

    @Test fun illumination() {
        assertEquals(0.0, moonIllumination(0.0), 1e-9)
        assertEquals(0.5, moonIllumination(0.25), 1e-9)
        assertEquals(1.0, moonIllumination(0.5), 1e-9)
        assertEquals(0.5, moonIllumination(0.75), 1e-9)
    }

    @Test fun shapeGrowsOnTheRightThenShrinksOnTheLeft() {
        val crescent = moonShape(0.1)
        assertTrue(crescent.litOnRight)
        assertTrue(crescent.bulge < 0f)
        assertEquals(0f, moonShape(0.25).bulge, 1e-6f)
        val gibbous = moonShape(0.4)
        assertTrue(gibbous.litOnRight)
        assertTrue(gibbous.bulge > 0f)
        assertEquals(1f, moonShape(0.5).bulge, 1e-6f)
        assertFalse(moonShape(0.5).litOnRight)
        val waning = moonShape(0.9)
        assertFalse(waning.litOnRight)
        assertTrue(waning.bulge < 0f)
        assertEquals(-1f, moonShape(0.0).bulge, 1e-6f)
    }
}
