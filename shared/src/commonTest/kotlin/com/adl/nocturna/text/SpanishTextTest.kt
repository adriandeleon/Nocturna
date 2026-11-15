package com.adl.nocturna.text

import com.adl.nocturna.alarm.DismissChallenge
import com.adl.nocturna.alarm.Sum
import com.adl.nocturna.alarm.bedtimeReminderText
import com.adl.nocturna.alarm.sleepHoursLabel
import com.adl.nocturna.alarm.timeToAlarmText
import com.adl.nocturna.clock.MoonPhase
import com.adl.nocturna.data.Alarm
import com.adl.nocturna.data.Backup
import com.adl.nocturna.data.Settings
import com.adl.nocturna.day.CalendarEvent
import com.adl.nocturna.day.FocusStatus
import com.adl.nocturna.day.countdownLine
import com.adl.nocturna.day.focusLabel
import com.adl.nocturna.day.greeting
import com.adl.nocturna.settings.alarmsSummary
import com.adl.nocturna.settings.infoLineSummary
import kotlinx.datetime.TimeZone
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertSame
import kotlin.time.Duration.Companion.hours
import kotlin.time.Duration.Companion.minutes
import kotlin.time.Duration.Companion.seconds
import kotlin.time.Instant

/** The text plain functions build, in Mexican Spanish: picked by the locale, with Spanish word order and plurals. */
class SpanishTextTest {
    private val es = spanishTestLocale
    private val alarm = Instant.parse("2026-11-16T12:30:00Z") // 6:30 in Monterrey
    private val zone = TimeZone.of("America/Monterrey")

    @Test fun spanishPicksTheSpanishWords() {
        assertSame(SpanishStrings, strings(es))
        assertSame(EnglishStrings, strings(testLocale))
    }

    @Test fun countdownToTheAlarm() {
        assertEquals("en 7 h 20 min", timeToAlarmText(alarm, alarm - 7.hours - 20.minutes, es))
        assertEquals("en 7 h", timeToAlarmText(alarm, alarm - 7.hours, es))
        assertEquals("en 45 min", timeToAlarmText(alarm, alarm - 45.minutes, es))
        assertEquals("en < 5 min", timeToAlarmText(alarm, alarm - 1.seconds, es))
    }

    @Test fun focusTimer() {
        val status = FocusStatus(step = 0, left = (18 * 60 + 42) * 1000L, length = 25 * 60_000L, paused = false)
        assertEquals("Concentración · quedan 18:42", focusLabel(status, es))
        assertEquals("Concentración en pausa · quedan 18:42", focusLabel(status.copy(paused = true), es))
        assertEquals("Descanso · quedan 3:10", focusLabel(FocusStatus(1, 190_000L, 300_000L, false), es))
        assertEquals("Descanso largo · quedan 15:00", focusLabel(FocusStatus(7, 900_000L, 900_000L, false), es))
    }

    @Test fun pluralsAndAgreement() {
        val station = Settings().stations.first()
        assertEquals("1 alarma, 1 estación de radio", Backup("", Settings(stations = listOf(station)), listOf(Alarm(1, 7, 0))).summary(es))
        assertEquals("0 alarmas, 0 estaciones de radio", Backup("", Settings(stations = emptyList()), emptyList()).summary(es))
        val on = Alarm(1, 7, 0)
        assertEquals("1 de 2 activada", alarmsSummary(listOf(on, on.copy(id = 2, enabled = false)), es))
        assertEquals("2 de 2 activadas", alarmsSummary(listOf(on, on.copy(id = 2)), es))
        assertEquals("Todavía no hay alarmas", alarmsSummary(emptyList(), es))
    }

    @Test fun bedtimeReminderUsesTheRightArticle() {
        assertEquals("Hora de dormir: 8 h antes de tu alarma de las 06:30", bedtimeReminderText(alarm, 8f, zone, true, es))
        val oneThirty = Instant.parse("2026-11-16T07:30:00Z")
        assertEquals("Hora de dormir: 7 h 30 min antes de tu alarma de la 01:30", bedtimeReminderText(oneThirty, 7.5f, zone, true, es))
        assertEquals("Desactivado", sleepHoursLabel(0f, es))
    }

    @Test fun listsJoinWithY() = assertEquals(
        "Fecha, clima, batería y próxima alarma",
        infoLineSummary(Settings(showNotifications = false), es),
    )

    @Test fun meetingCountdown() {
        val standup = CalendarEvent("Standup", begin = alarm.toEpochMilliseconds(), end = alarm.toEpochMilliseconds() + 900_000L, allDay = false)
        assertEquals("Standup en 12 min", countdownLine(standup, alarm - 12.minutes, es))
        assertEquals("Próximo evento en 12 min", countdownLine(standup.copy(title = " "), alarm - 12.minutes, es))
    }

    @Test fun namesAndGreetings() {
        assertEquals("Gibosa creciente", MoonPhase.WAXING_GIBBOUS.label(es))
        assertEquals("Mantener el botón 3 s", DismissChallenge.HOLD.label(es))
        assertEquals("37 más 48", Sum(37, 48).spoken(es))
        assertEquals("¡Buenos días!", greeting(8, es))
        assertEquals("¡Buenas noches!", greeting(22, es))
    }
}
