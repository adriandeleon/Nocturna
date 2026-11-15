package com.adl.nocturna.alarm

import com.adl.nocturna.clock.nextAlarmRing
import com.adl.nocturna.data.Alarm
import kotlinx.datetime.LocalDate
import kotlinx.datetime.LocalDateTime
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toInstant
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue
import kotlin.time.Instant

class DatedAlarmTest {
    private val zone = TimeZone.of("America/Monterrey")
    private val london = TimeZone.of("Europe/London")
    private fun at(s: String, z: TimeZone = zone): Instant = LocalDateTime.parse(s).toInstant(z)
    private fun utc(s: String): Instant = Instant.parse(s)
    private fun date(s: String) = LocalDate.parse(s)

    /** 07:00 on Friday 21 November 2026. */
    private val friday = Alarm(1, 7, 0, date = date("2026-11-21"))

    @Test fun ringsOnItsDateNotTheNextTimeItsClockTimeComes() {
        val now = at("2026-11-15T22:00")
        assertEquals(at("2026-11-21T07:00"), friday.nextTrigger(now, zone))
        assertEquals(at("2026-11-21T07:00"), friday.nextRing(now, zone, emptyMap()))
    }

    @Test fun withoutADateItRingsTheNextTimeItsClockTimeComes() =
        assertEquals(at("2026-11-16T07:00"), Alarm(1, 7, 0).nextRing(at("2026-11-15T22:00"), zone, emptyMap()))

    @Test fun ringsLaterTheSameDay() =
        assertEquals(at("2026-11-21T07:00"), friday.nextRing(at("2026-11-21T06:59"), zone, emptyMap()))

    @Test fun hasNothingToRingOnceItsTimeHasPassed() {
        val now = at("2026-11-21T07:00")
        assertNull(friday.nextTrigger(now, zone))
        assertNull(friday.nextRing(now, zone, emptyMap()))
        assertTrue(friday.datePassed(now, zone))
        assertFalse(friday.datePassed(at("2026-11-21T06:59"), zone))
    }

    @Test fun daysOffDontMoveIt() {
        val dated = friday.copy(skipDaysOff = true)
        val now = at("2026-11-15T22:00")
        val off = mapOf(date("2026-11-21") to "Holiday")
        assertEquals(at("2026-11-21T07:00"), dated.nextRing(now, zone, off))
        assertNull(dated.nextDayOffSkip(now, zone, off))
        assertNull(dated.firstSkippedRing(now, zone, off))
    }

    @Test fun skipNextDoesNothing() {
        val now = at("2026-11-15T22:00")
        assertEquals(friday, friday.toggleSkip(now, zone, emptyMap()))
    }

    @Test fun repeatDaysWinOverADate() {
        val both = friday.copy(days = 1 shl 0)
        assertNull(both.ringDate)
        assertEquals(at("2026-11-16T07:00"), both.nextRing(at("2026-11-15T22:00"), zone, emptyMap()))
    }

    @Test fun sunriseCountsOnlyAlarmsWithARingAhead() {
        val now = at("2026-11-22T06:00")
        assertNull(nextAlarmRing(listOf(friday), now, zone))
        assertEquals(at("2026-11-22T06:30"), nextAlarmRing(listOf(friday, Alarm(2, 6, 30)), now, zone))
    }

    // Daylight saving, in London: the clocks go forward at 01:00 on 29 March 2026 and back at 02:00 on 25 October.

    @Test fun onAnOrdinaryDaylightSavingDayItRingsAtItsClockTime() {
        val spring = Alarm(1, 7, 0, date = date("2026-03-29"))
        assertEquals(utc("2026-03-29T06:00:00Z"), spring.nextRing(at("2026-03-28T22:00", london), london, emptyMap()))
        val autumn = Alarm(1, 7, 0, date = date("2026-10-25"))
        assertEquals(utc("2026-10-25T07:00:00Z"), autumn.nextRing(at("2026-10-24T22:00", london), london, emptyMap()))
    }

    @Test fun aTimeInTheSpringForwardGapRingsAfterIt() {
        val gap = Alarm(1, 1, 30, date = date("2026-03-29"))
        assertEquals(utc("2026-03-29T01:30:00Z"), gap.nextRing(at("2026-03-28T22:00", london), london, emptyMap()))
    }

    @Test fun aTimeThatHappensTwiceWhenTheClocksGoBackRingsTheFirstTime() {
        val twice = Alarm(1, 1, 30, date = date("2026-10-25"))
        val now = at("2026-10-24T22:00", london)
        assertEquals(utc("2026-10-25T00:30:00Z"), twice.nextRing(now, london, emptyMap()))
        // Once the first 01:30 has gone, it has rung: the second one isn't another ring.
        assertNull(twice.nextRing(utc("2026-10-25T00:30:00Z"), london, emptyMap()))
    }

    @Test fun aDateInThePastCantBeSaved() {
        val now = at("2026-11-15T08:00")
        val plain = Alarm(1, 7, 0)
        assertNull(plain.withDate(date("2026-11-14"), now, zone))
        assertNull(plain.withDate(date("2026-11-15"), now, zone), "07:00 today has passed at 08:00")
        assertEquals(date("2026-11-16"), plain.withDate(date("2026-11-16"), now, zone)?.date)
        assertTrue(canRingOn(date("2026-11-15"), 9, 0, now, zone))
    }

    @Test fun settingADateTakesOffRepeatDaysAndSwitchesItOn() {
        val now = at("2026-11-15T08:00")
        val weekdays = Alarm(1, 7, 0, days = 0b0011111, enabled = false, skipDate = date("2026-11-16"))
        assertEquals(
            Alarm(1, 7, 0, days = 0, enabled = true, date = date("2026-11-21")),
            weekdays.withDate(date("2026-11-21"), now, zone),
        )
        assertEquals(Alarm(1, 7, 0), friday.withDate(null, now, zone))
    }

    @Test fun repeatDaysTakeOffTheDate() {
        assertEquals(Alarm(1, 7, 0, days = 0b0011111), friday.withDays(0b0011111))
        assertEquals(friday, friday.withDays(0))
    }

    @Test fun movingTheTimeKeepsTheDateWhileItIsAhead() {
        val now = at("2026-11-21T06:00")
        assertEquals(friday.copy(hour = 6, minute = 30), friday.withTime(6, 30, now, zone))
        assertNull(friday.withTime(5, 30, now, zone), "05:30 on its day has passed")
    }

    @Test fun movingTheTimeOfAnAlarmWhoseDayHasGoneTakesTheDateOff() {
        val rung = friday.copy(enabled = false)
        assertEquals(Alarm(1, 6, 30), rung.withTime(6, 30, at("2026-11-23T08:00"), zone))
    }

    @Test fun switchingOnAnAlarmWhoseDateHasPassedTakesTheDateOff() {
        val rung = friday.copy(enabled = false)
        assertEquals(Alarm(1, 7, 0), rung.switched(true, at("2026-11-21T07:01"), zone))
        assertEquals(friday, rung.switched(true, at("2026-11-20T07:00"), zone))
        assertEquals(rung, friday.switched(false, at("2026-11-20T07:00"), zone))
    }
}
