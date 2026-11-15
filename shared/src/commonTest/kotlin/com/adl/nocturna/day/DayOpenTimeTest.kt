package com.adl.nocturna.day

import kotlinx.datetime.LocalDate
import kotlinx.datetime.LocalDateTime
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toInstant
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.time.Instant

class DayOpenTimeTest {
    private val monterrey = TimeZone.of("America/Monterrey")
    private val now = LocalDateTime(2026, 11, 14, 15, 42).toInstant(monterrey)

    @Test fun todayOpensAtTheCurrentTime() =
        assertEquals(now.toEpochMilliseconds(), dayOpenTime(LocalDate(2026, 11, 14), now, monterrey))

    @Test fun otherDaysOpenAtNineInTheMorning() =
        assertEquals(
            LocalDateTime(2026, 11, 20, 9, 0).toInstant(monterrey).toEpochMilliseconds(),
            dayOpenTime(LocalDate(2026, 11, 20), now, monterrey),
        )

    @Test fun nineInTheMorningIsLocalAcrossAClockChange() {
        // New York moves its clocks forward on 8 March 2026; 09:00 that day is 13:00 UTC, not 14:00.
        val newYork = TimeZone.of("America/New_York")
        val march = LocalDateTime(2026, 3, 1, 12, 0).toInstant(newYork)
        assertEquals(
            Instant.parse("2026-03-08T13:00:00Z").toEpochMilliseconds(),
            dayOpenTime(LocalDate(2026, 3, 8), march, newYork),
        )
    }
}
