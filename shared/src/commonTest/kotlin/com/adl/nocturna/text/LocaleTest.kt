package com.adl.nocturna.text

import kotlinx.datetime.DayOfWeek
import kotlinx.datetime.LocalDate
import kotlinx.datetime.LocalDateTime
import kotlinx.datetime.LocalTime
import kotlin.test.Test
import kotlin.test.assertEquals

class LocaleTest {
    @Test fun twentyFourHourTime() = assertEquals("07:05", formatTime(LocalTime(7, 5), use24h = true, testLocale))

    @Test fun twelveHourTime() = assertEquals("7:05 pm", formatTime(LocalTime(19, 5), use24h = false, testLocale).lowercase())

    @Test fun midnightInTwelveHours() = assertEquals("12:00 am", formatTime(LocalTime(0, 0), use24h = false, testLocale).lowercase())

    @Test fun shortDayNames() = assertEquals(
        listOf("Mon", "Tue", "Wed", "Thu", "Fri", "Sat", "Sun"),
        DayOfWeek.entries.map { it.shortName(testLocale) },
    )

    @Test fun narrowDayNames() = assertEquals(
        listOf("M", "T", "W", "T", "F", "S", "S"),
        DayOfWeek.entries.map { it.narrowName(testLocale) },
    )

    @Test fun fullDayNames() = assertEquals("Monday", DayOfWeek.MONDAY.fullName(testLocale))

    @Test fun britishWeeksStartOnMonday() = assertEquals(DayOfWeek.MONDAY, firstDayOfWeek(testLocale))

    @Test fun weekNumbers() {
        // British weeks are ISO weeks: week 1 holds the year's first Thursday.
        assertEquals(1, weekOfYear(LocalDate(2026, 1, 1), testLocale))
        assertEquals(46, weekOfYear(LocalDate(2026, 11, 15), testLocale))
        assertEquals(53, weekOfYear(LocalDate(2027, 1, 1), testLocale))
    }

    @Test fun capitalizesTheFirstLetterOnly() {
        assertEquals("Lundi 3", "lundi 3".capitalizedFirst(testLocale))
        assertEquals("", "".capitalizedFirst(testLocale))
    }

    @Test fun formatsWithAPattern() {
        val at = LocalDateTime(2026, 11, 15, 18, 5)
        assertEquals("Sunday 15 November", DateTimePattern("EEEE d MMMM", testLocale).format(at))
        assertEquals("November", DateTimePattern("LLLL", testLocale).format(at))
        assertEquals("18:05", DateTimePattern("HH:mm", testLocale).format(at))
        assertEquals("6:05 pm", DateTimePattern("h:mm a", testLocale).format(at).lowercase())
    }

    @Test fun numbersWithDecimals() {
        assertEquals("2.5", formatNumber(2.5, 1, testLocale))
        assertEquals("0.0", formatNumber(0.04, 1, testLocale))
        assertEquals("1234.6", formatNumber(1234.56, 1, testLocale))
    }

    @Test fun languageCode() = assertEquals("en", testLocale.languageCode)
}
