package com.adl.nocturna.day

import kotlinx.datetime.LocalDate
import kotlinx.datetime.LocalTime
import kotlin.math.min
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue
import kotlin.time.Instant

class SkyEventsTest {
    @Test fun shootingStarsOnAShowersPeakNight() {
        assertTrue(meteorNight(LocalDate(2026, 8, 12), LocalTime(22, 0)))
        // After midnight it's still the night that started on the evening before.
        assertTrue(meteorNight(LocalDate(2026, 8, 14), LocalTime(2, 0)))
        assertFalse(meteorNight(LocalDate(2026, 8, 14), LocalTime(22, 0)))
        assertTrue(meteorNight(LocalDate(2026, 11, 17), LocalTime(23, 0)))
        assertTrue(meteorNight(LocalDate(2026, 11, 18), LocalTime(4, 30)))
        assertFalse(meteorNight(LocalDate(2026, 11, 18), LocalTime(13, 0)))
        assertFalse(meteorNight(LocalDate(2026, 6, 1), LocalTime(23, 0)))
    }

    private fun nearNew(phase: Float) = min(phase, 1 - phase)

    @Test fun eclipsesFallOnNewAndFullMoons() {
        // The total solar eclipse of 8 April 2024 (a new moon) and the total lunar eclipses of
        // 21 January 2000 and 8 November 2022 (full moons).
        assertTrue(nearNew(moonPhase(Instant.parse("2024-04-08T18:18:00Z"))) < 0.03f)
        assertEquals(0.5f, moonPhase(Instant.parse("2000-01-21T04:44:00Z")), 0.03f)
        assertEquals(0.5f, moonPhase(Instant.parse("2022-11-08T10:59:00Z")), 0.03f)
    }

    @Test fun howMuchOfTheMoonIsLit() {
        assertEquals(0f, moonLit(0f), 0.001f)
        assertEquals(0.5f, moonLit(0.25f), 0.001f)
        assertEquals(1f, moonLit(0.5f), 0.001f)
        assertEquals(0.5f, moonLit(0.75f), 0.001f)
    }
}
