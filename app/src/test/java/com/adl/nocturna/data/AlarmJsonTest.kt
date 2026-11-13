package com.adl.nocturna.data

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class AlarmJsonTest {
    @Test fun radioAlarmSurvivesRoundTrip() {
        val alarms = listOf(
            Alarm(1, 6, 40, days = 0b0011111, label = "Work", station = Station("D99", "https://example.com/d99")),
            Alarm(2, 9, 0),
        )
        assertEquals(alarms, AlarmStore.decode(AlarmStore.encode(alarms)))
    }

    @Test fun alarmsSavedBeforeStationsLoadWithTheAlarmTone() {
        val old = """[{"id":1,"hour":7,"minute":0,"days":0,"enabled":true,"label":""}]"""
        val alarm = AlarmStore.decode(old).single()
        assertEquals(Alarm(1, 7, 0), alarm)
        assertNull(alarm.station)
    }

    @Test fun stationWithoutUrlFallsBackToTheAlarmTone() =
        assertNull(AlarmStore.decode("""[{"id":1,"hour":7,"minute":0,"station":{"name":"Gone"}}]""").single().station)

    @Test fun stationWithoutNameIsNamedAfterItsUrl() =
        assertEquals(
            Station("https://example.com/s", "https://example.com/s"),
            AlarmStore.decode("""[{"id":1,"hour":7,"minute":0,"station":{"url":"https://example.com/s"}}]""").single().station,
        )
}
