package com.adl.nocturna.text

import kotlinx.datetime.DayOfWeek
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
}
