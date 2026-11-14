package com.adl.nocturna.day

import org.junit.Assert.assertEquals
import org.junit.Test
import java.time.LocalTime

class TimeOfDayTest {
    private fun at(h: Int, m: Int = 0) = skyMix(LocalTime.of(h, m))

    @Test fun eachLookHoldsInItsOwnHours() {
        assertEquals(1f, at(3).weight(TimeOfDay.NIGHT))
        assertEquals(1f, at(8, 30).weight(TimeOfDay.MORNING))
        assertEquals(1f, at(13).weight(TimeOfDay.MIDDAY))
        assertEquals(1f, at(17, 30).weight(TimeOfDay.AFTERNOON))
        assertEquals(1f, at(22).weight(TimeOfDay.NIGHT))
    }

    @Test fun dawnBlendsNightIntoMorning() {
        val dawn = at(6, 15)
        assertEquals(0.5f, dawn.weight(TimeOfDay.NIGHT), 0.001f)
        assertEquals(0.5f, dawn.weight(TimeOfDay.MORNING), 0.001f)
    }

    @Test fun duskBlendsAfternoonIntoNight() {
        val dusk = at(19, 0)
        assertEquals(TimeOfDay.AFTERNOON, dusk.from)
        assertEquals(TimeOfDay.NIGHT, dusk.to)
        assertEquals(1f / 3f, dusk.t, 0.001f)
    }

    @Test fun weightsAlwaysAddUpToOne() {
        for (minute in 0 until 24 * 60 step 7) {
            val mix = skyMix(LocalTime.of(minute / 60, minute % 60))
            assertEquals("at minute $minute", 1f, TimeOfDay.entries.sumOf { mix.weight(it).toDouble() }.toFloat(), 0.001f)
        }
    }

    @Test fun lastMinuteOfTheDayIsNight() = assertEquals(1f, at(23, 59).weight(TimeOfDay.NIGHT))
}
