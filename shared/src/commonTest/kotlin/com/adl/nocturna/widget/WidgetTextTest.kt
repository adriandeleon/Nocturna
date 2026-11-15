package com.adl.nocturna.widget

import com.adl.nocturna.text.DateTimePattern
import com.adl.nocturna.text.testLocale
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.time.Instant
import kotlinx.datetime.LocalDateTime
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toInstant

class WidgetTextTest {
    private val zone = TimeZone.of("America/Monterrey")
    private val farDate = DateTimePattern("EEE d MMM", testLocale)

    // Monday 16 November 2026, 23:30 in Monterrey.
    private val now = at(2026, 11, 16, 23, 30)

    private fun at(y: Int, mo: Int, d: Int, h: Int, mi: Int): Instant = LocalDateTime(y, mo, d, h, mi).toInstant(zone)

    private fun text(alarm: Instant?, use24h: Boolean = true) = nextAlarmText(alarm, now, zone, use24h, testLocale, farDate)

    @Test fun noAlarm() = assertNull(text(null))

    @Test fun alarmAlreadyRang() = assertNull(text(at(2026, 11, 16, 7, 0)))

    @Test fun laterTodayNamesTheDay() = assertEquals("Mon 23:45", text(at(2026, 11, 16, 23, 45)))

    @Test fun tomorrowMorning() = assertEquals("Tue 07:00", text(at(2026, 11, 17, 7, 0)))

    @Test fun twelveHour() = assertEquals("tue 7:00 am", text(at(2026, 11, 17, 7, 0), use24h = false)?.lowercase())

    @Test fun sixDaysAheadIsStillADayName() = assertEquals("Sun 06:30", text(at(2026, 11, 22, 6, 30)))

    @Test fun aWeekAheadGetsTheDate() = assertEquals("Mon 23 Nov 07:00", text(at(2026, 11, 23, 7, 0)))

    @Test fun usesTheGivenZone() = assertEquals(
        "Tue 14:00",
        nextAlarmText(at(2026, 11, 17, 7, 0), now, TimeZone.of("Europe/Madrid"), true, testLocale, farDate),
    )

    @Test fun smallWidgetGetsTheSmallestClock() = assertEquals(MIN_CLOCK_SP, widgetClockSizeSp(110, 90))

    @Test fun narrowWidgetIsLimitedByWidth() = assertEquals(48, widgetClockSizeSp(180, 400))

    @Test fun shortWidgetIsLimitedByHeight() = assertEquals(50, widgetClockSizeSp(400, 160))

    @Test fun bigWidgetStopsAtTheLargestClock() = assertEquals(MAX_CLOCK_SP, widgetClockSizeSp(800, 600))
}
