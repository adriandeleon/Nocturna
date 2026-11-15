package com.adl.nocturna.data

import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.Test

class AlarmJsonTest {
    @Test fun radioAlarmSurvivesRoundTrip() {
        val alarms = listOf(
            Alarm(1, 6, 40, days = 0b0011111, label = "Work", station = Station("D99", "https://example.com/d99")),
            Alarm(2, 9, 0),
        )
        assertEquals(alarms, AlarmCodec.decode(AlarmCodec.encode(alarms)))
    }

    @Test fun alarmsSavedBeforeStationsLoadWithTheAlarmTone() {
        val old = """[{"id":1,"hour":7,"minute":0,"days":0,"enabled":true,"label":""}]"""
        val alarm = AlarmCodec.decode(old).single()
        assertEquals(Alarm(1, 7, 0), alarm)
        assertNull(alarm.station)
    }

    @Test fun stationWithoutUrlFallsBackToTheAlarmTone() =
        assertNull(AlarmCodec.decode("""[{"id":1,"hour":7,"minute":0,"station":{"name":"Gone"}}]""").single().station)

    @Test fun stationWithoutNameIsNamedAfterItsUrl() =
        assertEquals(
            Station("https://example.com/s", "https://example.com/s"),
            AlarmCodec.decode("""[{"id":1,"hour":7,"minute":0,"station":{"url":"https://example.com/s"}}]""").single().station,
        )

    @Test fun skipOnDaysOffSurvivesRoundTrip() {
        val alarms = listOf(Alarm(1, 6, 40, days = 0b0011111, skipDaysOff = true))
        assertEquals(alarms, AlarmCodec.decode(AlarmCodec.encode(alarms)))
    }

    @Test fun alarmsSavedBeforeSkipOnDaysOffLoadWithItOff() {
        val old = """[{"id":1,"hour":7,"minute":0,"days":31,"enabled":true,"label":"","skipDate":"2026-11-16"}]"""
        assertFalse(AlarmCodec.decode(old).single().skipDaysOff)
    }
}
