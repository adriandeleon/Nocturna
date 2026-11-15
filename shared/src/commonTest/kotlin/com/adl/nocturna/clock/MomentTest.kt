package com.adl.nocturna.clock

import kotlinx.datetime.LocalDate
import kotlinx.datetime.LocalTime
import kotlinx.datetime.TimeZone
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.time.Instant

class MomentTest {
    private val monterrey = TimeZone.of("America/Monterrey")

    @Test fun readsTheClockInItsZone() {
        val now = Moment(Instant.parse("2026-11-16T04:30:15Z"), monterrey)
        assertEquals(LocalDate(2026, 11, 15), now.date)
        assertEquals(LocalTime(22, 30, 15), now.time)
        assertEquals(22, now.hour)
        assertEquals(1_794_803_415_000L, now.epochMillis)
    }

    @Test fun theSameMomentElsewhereIsAnotherDay() =
        assertEquals(LocalDate(2026, 11, 16), Moment(Instant.parse("2026-11-16T04:30:15Z"), TimeZone.UTC).date)
}
