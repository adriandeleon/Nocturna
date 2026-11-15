package com.adl.nocturna.data

import com.adl.nocturna.alarm.DismissChallenge
import kotlinx.datetime.LocalDate
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

    @Test fun dateSurvivesRoundTrip() {
        val alarms = listOf(Alarm(1, 7, 0, date = LocalDate(2026, 11, 21)), Alarm(2, 9, 0))
        assertEquals(alarms, AlarmCodec.decode(AlarmCodec.encode(alarms)))
    }

    @Test fun alarmsSavedBeforeDatesLoadWithoutOne() {
        val old = """[{"id":1,"hour":7,"minute":0,"days":0,"enabled":true,"label":"","skipDaysOff":false}]"""
        assertNull(AlarmCodec.decode(old).single().date)
    }

    @Test fun aDateIsDroppedFromARepeatingAlarm() {
        val json = """[{"id":1,"hour":7,"minute":0,"days":31,"date":"2026-11-21"}]"""
        assertNull(AlarmCodec.decode(json).single().date)
        assertFalse(AlarmCodec.encode(listOf(Alarm(1, 7, 0, days = 31, date = LocalDate(2026, 11, 21)))).contains("date\""))
    }

    @Test fun anUnreadableDateIsDropped() =
        assertNull(AlarmCodec.decode("""[{"id":1,"hour":7,"minute":0,"date":"Friday"}]""").single().date)

    @Test fun dismissChallengeSurvivesRoundTrip() {
        val alarms = listOf(Alarm(1, 6, 40, challenge = DismissChallenge.SUM), Alarm(2, 7, 0, challenge = DismissChallenge.HOLD), Alarm(3, 8, 0))
        assertEquals(alarms, AlarmCodec.decode(AlarmCodec.encode(alarms)))
    }

    @Test fun alarmsSavedBeforeChallengesStopWithATap() {
        val old = """[{"id":1,"hour":7,"minute":0,"days":31,"enabled":true,"label":"","skipDaysOff":true}]"""
        assertEquals(DismissChallenge.TAP, AlarmCodec.decode(old).single().challenge)
    }

    @Test fun anUnknownChallengeStopsWithATap() =
        assertEquals(DismissChallenge.TAP, AlarmCodec.decode("""[{"id":1,"hour":7,"minute":0,"challenge":"riddle"}]""").single().challenge)
}
