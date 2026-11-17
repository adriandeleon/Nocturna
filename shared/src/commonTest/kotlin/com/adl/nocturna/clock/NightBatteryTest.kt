package com.adl.nocturna.clock

import com.adl.nocturna.data.MemoryStore
import com.adl.nocturna.data.NightBatteryStore
import com.adl.nocturna.text.spanishTestLocale
import com.adl.nocturna.text.testLocale
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertSame
import kotlin.time.Instant
import kotlinx.datetime.LocalDate
import kotlinx.datetime.TimeZone

class NightBatteryTest {
    private val zone = TimeZone.of("America/Mexico_City")
    private val night = LocalDate(2026, 11, 15)
    private val min = 60_000L
    private val hour = 60 * min

    @Test fun aNightIsNamedByItsEvening() {
        assertEquals(night, nightOf(Instant.parse("2026-11-16T02:00:00-06:00"), zone))
        assertEquals(night, nightOf(Instant.parse("2026-11-15T23:00:00-06:00"), zone))
        assertEquals(LocalDate(2026, 11, 16), nightOf(Instant.parse("2026-11-16T22:00:00-06:00"), zone))
    }

    @Test fun startingOnlyMarks() {
        val (log, mark) = trackNightBattery(emptyList(), null, tracking = true, millis = 0, level = 80, night = night)
        assertEquals(emptyList(), log)
        assertEquals(NightBatteryMark(0, 80), mark)
    }

    @Test fun nothingStartsOnTheCharger() {
        assertEquals(emptyList<NightUse>() to null, trackNightBattery(emptyList(), null, tracking = false, millis = 0, level = 80, night = night))
    }

    @Test fun theSameLevelWritesNothing() {
        val log = listOf(NightUse(night, hour, 2))
        val mark = NightBatteryMark(0, 80)
        val (next, nextMark) = trackNightBattery(log, mark, tracking = true, millis = 20 * min, level = 80, night = night)
        assertSame(log, next)
        assertSame(mark, nextMark)
    }

    @Test fun eachDropAddsItsTimeAndCharge() {
        var log = emptyList<NightUse>()
        var mark: NightBatteryMark? = null
        for ((at, level) in listOf(0L to 80, 30 * min to 79, 40 * min to 79, 70 * min to 78)) {
            trackNightBattery(log, mark, tracking = true, millis = at, level = level, night = night).let { log = it.first; mark = it.second }
        }
        assertEquals(listOf(NightUse(night, 70 * min, 2)), log)
        assertEquals(NightBatteryMark(70 * min, 78), mark)
    }

    @Test fun stoppingAddsTheTimeSinceTheLastDrop() {
        val (log, mark) = trackNightBattery(
            listOf(NightUse(night, hour, 2)), NightBatteryMark(0, 78), tracking = false, millis = 15 * min, level = 78, night = night,
        )
        assertEquals(listOf(NightUse(night, hour + 15 * min, 2)), log)
        assertNull(mark)
    }

    @Test fun stoppingWithoutALevelKeepsTheTime() {
        val (log, _) = trackNightBattery(emptyList(), NightBatteryMark(0, 78), tracking = false, millis = 15 * min, level = null, night = night)
        assertEquals(listOf(NightUse(night, 15 * min, 0)), log)
    }

    @Test fun aChargeInBetweenUsesNone() {
        val (log, _) = trackNightBattery(emptyList(), NightBatteryMark(0, 60), tracking = false, millis = hour, level = 90, night = night)
        assertEquals(listOf(NightUse(night, hour, 0)), log)
    }

    @Test fun theLogKeepsTwoWeeksInOrder() {
        val old = (1..NIGHT_BATTERY_NIGHTS).map { NightUse(LocalDate(2026, 10, it), hour, 2) }.reversed()
        val (log, _) = trackNightBattery(old, NightBatteryMark(0, 80), tracking = false, millis = hour, level = 78, night = night)
        assertEquals(NIGHT_BATTERY_NIGHTS, log.size)
        assertEquals(LocalDate(2026, 10, 2), log.first().night)
        assertEquals(NightUse(night, hour, 2), log.last())
    }

    @Test fun estimateNeedsAnHour() {
        assertNull(nightBatteryEstimate(listOf(NightUse(night, 59 * min, 1))))
        assertEquals(16, nightBatteryEstimate(listOf(NightUse(night, hour, 2))))
        assertEquals(14, nightBatteryEstimate(listOf(NightUse(night, 7 * hour, 12), NightUse(night.plusDays(1), 8 * hour, 14))))
    }

    @Test fun summary() {
        val label = { d: LocalDate -> "Night ${d.day}" }
        assertNull(nightBatterySummary(emptyList(), testLocale, label))
        assertEquals("Night 15: 0% in 20 m", nightBatterySummary(listOf(NightUse(night, 20 * min, 0)), testLocale, label))
        assertEquals(
            "Night 15: 12% in 7 h 30 m\nAbout 13% for 8 hours, from one night",
            nightBatterySummary(listOf(NightUse(night, 7 * hour + 30 * min, 12)), testLocale, label),
        )
        assertEquals(
            "Night 16: 14% in 8 h\nAbout 14% for 8 hours, over the last 2 nights",
            nightBatterySummary(listOf(NightUse(night, 7 * hour, 12), NightUse(night.plusDays(1), 8 * hour, 14)), testLocale, label),
        )
        assertEquals(
            "Night 15: 12% en 7 h 30 min\nUnos 13% en 8 horas, según una noche",
            nightBatterySummary(listOf(NightUse(night, 7 * hour + 30 * min, 12)), spanishTestLocale, label),
        )
    }

    @Test fun storeKeepsWhatItTracked() {
        val store = MemoryStore()
        NightBatteryStore.init(store)
        val start = Instant.parse("2026-11-15T23:00:00-06:00")
        NightBatteryStore.track(true, 80, start, zone)
        NightBatteryStore.track(true, 79, Instant.fromEpochMilliseconds(start.toEpochMilliseconds() + 40 * min), zone)
        NightBatteryStore.init(store)
        assertEquals(listOf(NightUse(night, 40 * min, 1)), NightBatteryStore.uses.value)
    }

    @Test fun storeSkipsWhatItCannotRead() {
        assertEquals(
            listOf(NightUse(night, hour, 2)),
            NightBatteryStore.decode("""[{"night":"2026-11-15","millis":3600000,"percent":2},{"night":"soon"},7]"""),
        )
        NightBatteryStore.init(MemoryStore("nights" to "{not json"))
        assertEquals(emptyList(), NightBatteryStore.uses.value)
    }

    private fun LocalDate.plusDays(days: Int) = LocalDate.fromEpochDays(toEpochDays() + days)
}
