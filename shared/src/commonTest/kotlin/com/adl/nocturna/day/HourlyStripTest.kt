package com.adl.nocturna.day

import kotlin.test.Test
import kotlin.test.assertEquals

class HourlyStripTest {
    @Test fun allFitWhenThereIsRoom() = assertEquals(6, fittingCount(List(6) { 100 }, 600))

    @Test fun keepsTheFirstHoursThatFit() {
        assertEquals(4, fittingCount(listOf(80, 120, 120, 120, 120, 120), 460))
        assertEquals(0, fittingCount(listOf(80), 79))
    }

    @Test fun nothingToFit() = assertEquals(0, fittingCount(emptyList(), 300))
}
