package com.adl.nocturna.data

import kotlin.test.assertEquals
import kotlin.test.Test
import kotlinx.datetime.LocalDateTime
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toInstant
import kotlin.time.Instant

class AlarmTest {
    private val zone = TimeZone.of("Europe/Berlin") // DST: 2026-03-29 02:00 -> 03:00
    private fun at(s: String): Instant = LocalDateTime.parse(s).toInstant(zone)

    @Test fun onceLaterToday() =
        assertEquals(at("2026-11-12T23:00"), Alarm(1, 23, 0).nextTrigger(at("2026-11-12T22:00"), zone))

    @Test fun onceAlreadyPassedMovesToTomorrow() =
        assertEquals(at("2026-11-13T06:40"), Alarm(1, 6, 40).nextTrigger(at("2026-11-12T22:00"), zone))

    @Test fun exactlyNowIsNotNext() =
        assertEquals(at("2026-11-13T07:00"), Alarm(1, 7, 0).nextTrigger(at("2026-11-12T07:00"), zone))

    @Test fun weekdaysSkipTheWeekend() {
        val weekdays = Alarm(1, 6, 40, days = 0b0011111)
        // Friday 2026-11-13 after 06:40 -> Monday 2026-11-16
        assertEquals(at("2026-11-16T06:40"), weekdays.nextTrigger(at("2026-11-13T07:00"), zone))
    }

    @Test fun sundayOnly() =
        assertEquals(at("2026-11-15T09:00"), Alarm(1, 9, 0, days = 1 shl 6).nextTrigger(at("2026-11-12T10:00"), zone))

    @Test fun timeInsideDstGapRingsAtFirstValidMoment() =
        assertEquals(at("2026-03-29T03:30"), Alarm(1, 2, 30, days = Alarm.ALL_DAYS).nextTrigger(at("2026-03-29T01:00"), zone))

    @Test fun dayAfterDstGapIsBackToNormalTime() =
        // Rescheduled right after ringing at 03:30 on the change day: must be 02:30 next day, not 03:30.
        assertEquals(at("2026-03-30T02:30"), Alarm(1, 2, 30, days = Alarm.ALL_DAYS).nextTrigger(at("2026-03-29T03:30:00.100"), zone))

    @Test fun invalidDayBitsDoNotHang() =
        assertEquals(at("2026-11-13T06:00"), Alarm(1, 6, 0, days = 1 shl 9).nextTrigger(at("2026-11-12T22:00"), zone))
}
