package com.adl.nocturna.day

import com.adl.nocturna.weather.DayForecast
import kotlin.test.assertEquals
import kotlin.test.assertSame
import kotlin.test.assertTrue
import kotlin.test.Test
import kotlin.time.Instant
import kotlinx.datetime.LocalDate
import kotlinx.datetime.LocalTime
import kotlinx.datetime.TimeZone

class TimeOfDayTest {
    private fun at(h: Int, m: Int = 0) = skyMix(LocalTime(h, m))

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
            val mix = skyMix(LocalTime(minute / 60, minute % 60))
            assertEquals(1f, TimeOfDay.entries.sumOf { mix.weight(it).toDouble() }.toFloat(), 0.001f, "at minute $minute")
        }
    }

    @Test fun lastMinuteOfTheDayIsNight() = assertEquals(1f, at(23, 59).weight(TimeOfDay.NIGHT))

    // Monterrey on 17 November: sunrise 07:10, sunset 18:00.
    private val november = skyDay(LocalTime(7, 10), LocalTime(18, 0))
    private fun nov(h: Int, m: Int = 0) = skyMix(LocalTime(h, m), november)

    @Test fun theRealSunMovesDawnAndDusk() {
        // Still night where the fixed day would be at dawn; dawn is half way at sunrise.
        assertEquals(1f, nov(6, 0).weight(TimeOfDay.NIGHT))
        assertEquals(0.5f, nov(7, 10).weight(TimeOfDay.MORNING), 0.001f)
        assertEquals(1f, nov(9, 0).weight(TimeOfDay.MORNING))
        assertEquals(1f, nov(13, 0).weight(TimeOfDay.MIDDAY))
        assertEquals(1f, nov(16, 30).weight(TimeOfDay.AFTERNOON))
        // Half way into the night at sunset, and fully dark 45 minutes later, not at 20:00.
        assertEquals(0.5f, nov(18, 0).weight(TimeOfDay.NIGHT), 0.001f)
        assertEquals(1f, nov(18, 45).weight(TimeOfDay.NIGHT))
    }

    @Test fun theSunAndMoonFollowTheRealDay() {
        assertEquals((7 * 60 + 10 - 45) * 60f, november.sunUp)
        assertEquals((18 * 60 + 45) * 60f, november.sunDown)
        assertEquals((17 * 60 + 15) * 60f, november.moonUp)
        assertEquals((7 * 60 + 55) * 60f, november.moonDown)
    }

    @Test fun withoutSunTimesTheDayIsFixed() {
        assertSame(FIXED_SKY_DAY, skyDay(null, LocalTime(18, 0)))
        // Too short a day, or twilight running past midnight, keeps the fixed day too.
        assertSame(FIXED_SKY_DAY, skyDay(LocalTime(11, 0), LocalTime(13, 0)))
        assertSame(FIXED_SKY_DAY, skyDay(LocalTime(0, 20), LocalTime(23, 50)))
        assertEquals(SUNRISE, FIXED_SKY_DAY.sunUp)
        assertEquals(SUNSET, FIXED_SKY_DAY.sunDown)
        assertEquals(MOONRISE, FIXED_SKY_DAY.moonUp)
        assertEquals(MOONSET, FIXED_SKY_DAY.moonDown)
    }

    @Test fun aShortWinterDayStillHasEveryLook() {
        val short = skyDay(LocalTime(9, 0), LocalTime(15, 0))
        for (look in TimeOfDay.entries) {
            assertTrue((0 until 24 * 60).any { skyMix(LocalTime(it / 60, it % 60), short).weight(look) == 1f }, "$look")
        }
        for (minute in 0 until 24 * 60 step 7) {
            val mix = skyMix(LocalTime(minute / 60, minute % 60), short)
            assertEquals(1f, TimeOfDay.entries.sumOf { mix.weight(it).toDouble() }.toFloat(), 0.001f)
        }
    }

    @Test fun theForecastGivesTodaysSun() {
        val zone = TimeZone.of("America/Monterrey")
        val days = listOf(
            DayForecast(LocalDate(2026, 11, 17), 0, 20.0, 10.0, Instant.parse("2026-11-17T13:10:00Z"), Instant.parse("2026-11-18T00:00:00Z")),
        )
        assertEquals(1f, skyMix(LocalTime(18, 45), skyDay(days, LocalDate(2026, 11, 17), zone)).weight(TimeOfDay.NIGHT))
        assertSame(FIXED_SKY_DAY, skyDay(days, LocalDate(2026, 11, 18), zone))
    }
}
