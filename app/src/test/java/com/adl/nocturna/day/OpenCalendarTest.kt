package com.adl.nocturna.day

import org.junit.Assert.assertEquals
import org.junit.Test
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.ZoneId

class OpenCalendarTest {
    private val monterrey = ZoneId.of("America/Monterrey")
    private val now = LocalDateTime.of(2026, 11, 14, 15, 42).atZone(monterrey)

    @Test fun todayOpensAtTheCurrentTime() =
        assertEquals(now.toInstant().toEpochMilli(), dayOpenTime(LocalDate.of(2026, 11, 14), now))

    @Test fun otherDaysOpenAtNineInTheMorning() =
        assertEquals(
            LocalDateTime.of(2026, 11, 20, 9, 0).atZone(monterrey).toInstant().toEpochMilli(),
            dayOpenTime(LocalDate.of(2026, 11, 20), now),
        )

    @Test fun nineInTheMorningIsLocalAcrossAClockChange() {
        // New York moves its clocks forward on 8 March 2026; 09:00 that day is 13:00 UTC, not 14:00.
        val newYork = ZoneId.of("America/New_York")
        val march = LocalDateTime.of(2026, 3, 1, 12, 0).atZone(newYork)
        assertEquals(
            java.time.Instant.parse("2026-03-08T13:00:00Z").toEpochMilli(),
            dayOpenTime(LocalDate.of(2026, 3, 8), march),
        )
    }
}
