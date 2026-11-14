package com.adl.nocturna.alarm

import com.adl.nocturna.data.Station
import org.junit.Assert.assertEquals
import org.junit.Test
import java.time.LocalDateTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter

class RingingTextTest {
    private val zone = ZoneId.of("America/Monterrey")
    private val hhmm = DateTimeFormatter.ofPattern("HH:mm")
    private val started = LocalDateTime.parse("2026-11-16T06:40:05").atZone(zone).toInstant().toEpochMilli()
    private val radio = Ringing(1, "Work", started, Station("D99", "https://example.com/d99"))

    @Test fun radioAlarmNamesTheStation() =
        assertEquals("06:40 · D99", ringingText(radio, zone, hhmm))

    @Test fun afterTheToneTakesOverTheStationIsGone() =
        assertEquals("06:40", ringingText(radio.copy(station = null), zone, hhmm))

    @Test fun showsWhenTheAlarmStartedNotTheCurrentTime() =
        // Built from startedAt alone: an update minutes later still reads 06:40.
        assertEquals("06:40", ringingText(Ringing(1, "", started), zone, hhmm))
}
