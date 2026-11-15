package com.adl.nocturna.clock

import androidx.compose.ui.graphics.Color
import com.adl.nocturna.data.Alarm
import com.adl.nocturna.ui.NightRed
import kotlinx.datetime.LocalDate
import kotlinx.datetime.LocalDateTime
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toInstant
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue
import kotlin.time.Instant

class SunriseTest {
    private val zone = TimeZone.of("Europe/Berlin")
    private fun at(s: String): Instant = LocalDateTime.parse(s).toInstant(zone)

    private val ring = at("2026-11-16T07:00")

    @Test fun nextRingIsTheSoonestAlarmThatIsOn() {
        val alarms = listOf(Alarm(1, 6, 30, enabled = false), Alarm(2, 7, 0), Alarm(3, 8, 0))
        assertEquals(ring, nextAlarmRing(alarms, at("2026-11-16T05:00"), zone))
    }

    @Test fun noRingWithoutAnAlarmOn() {
        assertNull(nextAlarmRing(emptyList(), at("2026-11-16T05:00"), zone))
        assertNull(nextAlarmRing(listOf(Alarm(1, 7, 0, enabled = false)), at("2026-11-16T05:00"), zone))
    }

    @Test fun aSkippedRingHasNoSunrise() {
        // Monday's ring of a weekday alarm is skipped: the next one is on Tuesday.
        val skipped = Alarm(1, 7, 0, days = Alarm.ALL_DAYS, skipDate = LocalDate(2026, 11, 16))
        val now = at("2026-11-16T06:50")
        val next = nextAlarmRing(listOf(skipped), now, zone)
        assertEquals(at("2026-11-17T07:00"), next)
        assertEquals(0f, sunriseProgress(now, next, 20))
    }

    @Test fun onceTheAlarmRingsTheNextSunriseIsTomorrow() {
        val daily = listOf(Alarm(1, 7, 0, days = Alarm.ALL_DAYS))
        val next = nextAlarmRing(daily, ring, zone)
        assertEquals(at("2026-11-17T07:00"), next)
        assertEquals(0f, sunriseProgress(ring, next, 20))
    }

    @Test fun progressRunsThroughTheWindowEasedIn() {
        assertEquals(0f, sunriseProgress(at("2026-11-16T06:30"), ring, 20))
        assertEquals(0f, sunriseProgress(at("2026-11-16T06:40"), ring, 20))
        val half = sunriseProgress(at("2026-11-16T06:50"), ring, 20)
        assertTrue(half > 0f && half < 0.5f, "slow at first: $half")
        assertEquals(0.125f, half, 0.0001f)
        assertTrue(sunriseProgress(at("2026-11-16T06:59:59"), ring, 20) > 0.99f)
        assertEquals(0f, sunriseProgress(ring, ring, 20))
        assertEquals(0f, sunriseProgress(at("2026-11-16T07:05"), ring, 20))
    }

    @Test fun progressRisesSteadily() {
        var last = 0f
        for (minute in 41..59) {
            val p = sunriseProgress(at("2026-11-16T06:$minute"), ring, 20)
            assertTrue(p > last, "06:$minute")
            last = p
        }
    }

    @Test fun windowLengthFollowsTheSetting() {
        val now = at("2026-11-16T06:45")
        assertEquals(0f, sunriseProgress(now, ring, 10))
        assertTrue(sunriseProgress(now, ring, 30) > 0f)
    }

    @Test fun noSunriseWhenOffOrWithoutAnAlarm() {
        assertEquals(0f, sunriseProgress(at("2026-11-16T06:55"), ring, 0))
        assertEquals(0f, sunriseProgress(at("2026-11-16T06:55"), null, 20))
    }

    @Test fun brightnessRisesFromTheRoomToTheBrightest() {
        assertEquals(0.02f, sunriseBrightness(0.02f, 0.8f, 0f), 0.0001f)
        assertEquals(0.41f, sunriseBrightness(0.02f, 0.8f, 0.5f), 0.0001f)
        assertEquals(0.8f, sunriseBrightness(0.02f, 0.8f, 1f), 0.0001f)
    }

    @Test fun brightnessNeverDropsBelowTheRoom() =
        assertEquals(0.9f, sunriseBrightness(0.9f, 0.8f, 0.5f), 0.0001f)

    @Test fun phoneBrightnessIsLeftAlone() =
        assertEquals(SYSTEM_BRIGHTNESS, sunriseBrightness(SYSTEM_BRIGHTNESS, 0.8f, 0.7f))

    @Test fun nightRedTurnsAmberThenWarmWhite() {
        assertEquals(NightRed, sunriseColor(NightRed, night = true, progress = 0f))
        assertEquals(SunriseAmber, sunriseColor(NightRed, night = true, progress = 0.5f))
        assertEquals(SunriseWhite, sunriseColor(NightRed, night = true, progress = 1f))
        val early = sunriseColor(NightRed, night = true, progress = 0.25f)
        assertTrue(early.green > NightRed.green && early.green < SunriseAmber.green)
    }

    @Test fun anyOtherColourGoesStraightToWarmWhite() {
        val blue = Color(0xFF8FD3FF)
        assertEquals(blue, sunriseColor(blue, night = false, progress = 0f))
        assertEquals(SunriseWhite, sunriseColor(blue, night = false, progress = 1f))
        assertTrue(sunriseColor(blue, night = false, progress = 0.5f).blue < blue.blue)
    }
}
