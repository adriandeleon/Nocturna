package com.adl.nocturna.day

import com.adl.nocturna.text.spanishTestLocale
import com.adl.nocturna.text.testLocale
import kotlinx.datetime.LocalDate
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

class HolidaysTest {
    private fun on(month: Int, day: Int, country: String = "GB") = holidayOn(LocalDate(2026, month, day), 0, country)

    @Test fun holidaysAreDecoratedOnTheDaysLeadingUpToThem() {
        assertEquals(HolidayDay(Holiday.VALENTINES, false), on(2, 10))
        assertEquals(HolidayDay(Holiday.VALENTINES, false), on(2, 13))
        assertNull(on(2, 9))
        assertNull(on(2, 15))
        assertEquals(HolidayDay(Holiday.HALLOWEEN, false), on(10, 24))
        assertEquals(HolidayDay(Holiday.HALLOWEEN, false), on(10, 30))
        assertNull(on(10, 23))
        assertEquals(HolidayDay(Holiday.MEXICAN_INDEPENDENCE, false), on(9, 1, "MX"))
        assertNull(on(9, 1, "GB"))
    }

    @Test fun christmasLastsAllDecemberUntilNewYearsEve() {
        assertEquals(HolidayDay(Holiday.CHRISTMAS, false), on(12, 1))
        assertEquals(HolidayDay(Holiday.CHRISTMAS, false), on(12, 23))
        assertEquals(HolidayDay(Holiday.CHRISTMAS, true), on(12, 24))
        assertEquals(HolidayDay(Holiday.CHRISTMAS, false), on(12, 26))
        assertEquals(HolidayDay(Holiday.CHRISTMAS, false), on(12, 30))
        assertEquals(HolidayDay(Holiday.NEW_YEAR, true), on(12, 31))
        assertNull(on(11, 30))
    }

    private fun day(h: Holiday) = HolidayDay(h, true)
    private fun around(h: Holiday) = HolidayDay(h, false)
    private fun on(y: Int, m: Int, d: Int, country: String) = holidayOn(LocalDate(y, m, d), 0, country)

    @Test fun easterFollowsTheChurchCalendar() {
        assertEquals(LocalDate(2008, 3, 23), easterSunday(2008))
        assertEquals(LocalDate(2011, 4, 24), easterSunday(2011))
        assertEquals(LocalDate(2024, 3, 31), easterSunday(2024))
        assertEquals(LocalDate(2025, 4, 20), easterSunday(2025))
        assertEquals(LocalDate(2026, 4, 5), easterSunday(2026))
        assertEquals(day(Holiday.EASTER), on(2026, 4, 5, "MX"))
        assertEquals(day(Holiday.EASTER), on(2026, 4, 5, ""))
    }

    @Test fun mothersAndFathersDayByRegion() {
        assertEquals(day(Holiday.MOTHERS_DAY), on(2026, 5, 10, "MX"))
        assertEquals(day(Holiday.MOTHERS_DAY), on(2027, 5, 10, "MX"))
        assertEquals(LocalDate(2026, 5, 10), mothersDay(2026, "US"))
        assertEquals(LocalDate(2027, 5, 9), mothersDay(2027, "US"))
        assertEquals(LocalDate(2026, 3, 15), mothersDay(2026, "GB"))
        assertEquals(LocalDate(2026, 5, 3), mothersDay(2026, "ES"))
        assertNull(mothersDay(2026, ""))
        assertNull(on(2026, 5, 10, ""))
        assertEquals(LocalDate(2026, 6, 21), fathersDay(2026, "MX"))
        assertEquals(LocalDate(2026, 3, 19), fathersDay(2026, "ES"))
        assertEquals(LocalDate(2026, 9, 6), fathersDay(2026, "AU"))
        assertEquals(day(Holiday.FATHERS_DAY), on(2026, 6, 21, "US"))
    }

    @Test fun nationalDays() {
        assertEquals(day(Holiday.US_INDEPENDENCE), on(2026, 7, 4, "US"))
        assertNull(on(2026, 7, 4, "MX"))
        assertEquals(day(Holiday.CANADA_DAY), on(2026, 7, 1, "CA"))
        assertEquals(day(Holiday.ST_PATRICKS), on(2026, 3, 17, "IE"))
        assertNull(on(2026, 3, 17, "MX"))
        assertEquals(LocalDate(2026, 11, 26), thanksgiving(2026, "US"))
        assertEquals(LocalDate(2026, 10, 12), thanksgiving(2026, "CA"))
        assertEquals(day(Holiday.THANKSGIVING), on(2026, 11, 26, "US"))
        assertNull(on(2026, 11, 26, "GB"))
    }

    @Test fun mexicosChristmasRunsFromThePosadasToTheKings() {
        assertEquals(around(Holiday.CHRISTMAS), on(2026, 12, 15, "MX"))
        assertEquals(day(Holiday.POSADAS), on(2026, 12, 16, "MX"))
        assertEquals(day(Holiday.POSADAS), on(2026, 12, 23, "MX"))
        assertEquals(around(Holiday.CHRISTMAS), on(2026, 12, 16, "GB"))
        assertEquals(around(Holiday.CHRISTMAS), on(2027, 1, 3, "MX"))
        assertNull(on(2027, 1, 3, "GB"))
        assertEquals(day(Holiday.THREE_KINGS), on(2027, 1, 6, "MX"))
        assertEquals(day(Holiday.CHILDRENS_DAY), on(2026, 4, 30, "MX"))
        assertNull(on(2026, 4, 30, "US"))
    }

    @Test fun lunarNewYearRunsFromTheEveToTheLanternFestival() {
        assertEquals(day(Holiday.LUNAR_NEW_YEAR), on(2026, 2, 16, "CN"))
        assertEquals(day(Holiday.LUNAR_NEW_YEAR), on(2026, 2, 17, "SG"))
        assertEquals(around(Holiday.LUNAR_NEW_YEAR), on(2026, 2, 18, "KR"))
        assertEquals(around(Holiday.LUNAR_NEW_YEAR), on(2026, 3, 3, "TW"))
        assertNull(on(2026, 3, 4, "CN"))
        assertNull(on(2026, 2, 17, "GB"))
        // In 2029 it falls in Valentine's week; where it's kept, it comes first.
        assertEquals(day(Holiday.LUNAR_NEW_YEAR), on(2029, 2, 13, "CN"))
        assertEquals(around(Holiday.VALENTINES), on(2029, 2, 13, "GB"))
        assertNull(on(2036, 1, 28, "CN"))
    }

    @Test fun diwaliIsFiveDays() {
        assertEquals(day(Holiday.DIWALI), on(2026, 11, 8, "IN"))
        assertEquals(around(Holiday.DIWALI), on(2026, 11, 6, "IN"))
        assertEquals(around(Holiday.DIWALI), on(2026, 11, 10, "NP"))
        assertNull(on(2026, 11, 11, "IN"))
        assertNull(on(2026, 11, 8, "US"))
        assertEquals(day(Holiday.DIWALI), on(2031, 11, 14, "IN"))
        // Past the end of the table there's none, rather than a wrong date.
        assertNull(on(2032, 11, 2, "IN"))
    }

    @Test fun hanukkahIsEightDaysAndCanRunIntoTheNewYear() {
        assertEquals(day(Holiday.HANUKKAH), on(2026, 12, 4, "IL"))
        assertEquals(day(Holiday.HANUKKAH), on(2026, 12, 11, "IL"))
        assertEquals(around(Holiday.CHRISTMAS), on(2026, 12, 3, "IL"))
        assertEquals(day(Holiday.HANUKKAH), on(2027, 12, 31, "IL"))
        assertEquals(day(Holiday.HANUKKAH), on(2036, 1, 1, "IL"))
        assertEquals(around(Holiday.CHRISTMAS), on(2026, 12, 4, "US"))
    }

    @Test fun newGreetings() {
        assertEquals("Happy Posadas!", holidayGreeting(Holiday.POSADAS, testLocale))
        assertEquals("¡Feliz Día de Reyes!", holidayGreeting(Holiday.THREE_KINGS, spanishTestLocale))
        assertEquals("Happy Lunar New Year!", holidayGreeting(Holiday.LUNAR_NEW_YEAR, testLocale))
        assertEquals("¡Feliz Janucá!", holidayGreeting(Holiday.HANUKKAH, spanishTestLocale))
        for (h in Holiday.entries) assertTrue(holidayGreeting(h, testLocale).isNotBlank() && holidayGreeting(h, spanishTestLocale).isNotBlank())
    }

    @Test fun fireworksWaitForTheDark() {
        assertEquals(listOf(Decoration.CONFETTI), day(Holiday.NEW_YEAR).decorations(dark = false))
        assertEquals(listOf(Decoration.FIREWORKS), day(Holiday.NEW_YEAR).decorations(dark = true))
        assertEquals(listOf(Decoration.BUNTING_US), day(Holiday.US_INDEPENDENCE).decorations(dark = false))
        assertEquals(listOf(Decoration.FIREWORKS), day(Holiday.US_INDEPENDENCE).decorations(dark = true))
        assertEquals(listOf(Decoration.PAPEL_PICADO_MEXICO, Decoration.FIREWORKS), day(Holiday.MEXICAN_INDEPENDENCE).decorations(dark = true))
        // The days around a holiday don't get its fireworks.
        assertEquals(listOf(Decoration.PAPEL_PICADO_MEXICO), around(Holiday.MEXICAN_INDEPENDENCE).decorations(dark = true))
        assertEquals(listOf(Decoration.LANTERNS), around(Holiday.LUNAR_NEW_YEAR).decorations(dark = true))
        assertEquals(listOf(Decoration.LIGHTS, Decoration.PINATAS), day(Holiday.POSADAS).decorations(dark = false))
        assertTrue(day(Holiday.EASTER).decorations(dark = false).isEmpty())
    }

    @Test fun aBirthdayBeatsTheDaysAroundAHoliday() =
        assertEquals(HolidayDay(Holiday.BIRTHDAY, true), holidayOn(LocalDate(2026, 12, 10), 1210, "GB"))
    @Test fun holidaysEveryoneGets() {
        assertEquals(HolidayDay(Holiday.NEW_YEAR, true), holidayOn(LocalDate(2027, 1, 1), 0, "GB"))
        assertEquals(HolidayDay(Holiday.NEW_YEAR, true), holidayOn(LocalDate(2026, 12, 31), 0, "GB"))
        assertEquals(HolidayDay(Holiday.VALENTINES, true), holidayOn(LocalDate(2026, 2, 14), 0, "GB"))
        assertEquals(HolidayDay(Holiday.HALLOWEEN, true), holidayOn(LocalDate(2026, 10, 31), 0, "US"))
        assertEquals(HolidayDay(Holiday.CHRISTMAS, true), holidayOn(LocalDate(2026, 12, 24), 0, "GB"))
        assertNull(holidayOn(LocalDate(2026, 11, 16), 0, "GB"))
    }

    @Test fun mexicosOwnOnlyWithAMexicanRegion() {
        assertEquals(HolidayDay(Holiday.DAY_OF_THE_DEAD, true), holidayOn(LocalDate(2026, 11, 2), 0, "MX"))
        assertEquals(HolidayDay(Holiday.MEXICAN_INDEPENDENCE, true), holidayOn(LocalDate(2026, 9, 16), 0, "mx"))
        assertNull(holidayOn(LocalDate(2026, 11, 2), 0, "GB"))
        assertNull(holidayOn(LocalDate(2026, 9, 16), 0, ""))
    }

    @Test fun yourBirthdayComesFirst() {
        assertEquals(HolidayDay(Holiday.BIRTHDAY, true), holidayOn(LocalDate(2026, 12, 25), 1225, "GB"))
        assertEquals(HolidayDay(Holiday.BIRTHDAY, true), holidayOn(LocalDate(2026, 3, 7), 307, "GB"))
    }

    @Test fun aLeapDayBirthdayIsKeptOnThe28thInOtherYears() {
        assertEquals(HolidayDay(Holiday.BIRTHDAY, true), holidayOn(LocalDate(2027, 2, 28), 229, "GB"))
        assertNull(holidayOn(LocalDate(2028, 2, 28), 229, "GB"))
        assertEquals(HolidayDay(Holiday.BIRTHDAY, true), holidayOn(LocalDate(2028, 2, 29), 229, "GB"))
    }

    @Test fun greetings() {
        assertEquals("Happy birthday!", holidayGreeting(Holiday.BIRTHDAY, testLocale))
        assertEquals("¡Feliz Día de Muertos!", holidayGreeting(Holiday.DAY_OF_THE_DEAD, spanishTestLocale))
    }

    @Test fun birthdayEvents() {
        fun allDay(title: String) = CalendarEvent(title, 0L, 86_400_000L, allDay = true)
        assertTrue(isBirthday(allDay("Ana's birthday")))
        assertTrue(isBirthday(allDay("Cumpleaños de Luis")))
        assertTrue(isBirthday(allDay("Mom's B-Day")))
        assertFalse(isBirthday(allDay("Birthdays planning").copy(allDay = false)))
        assertFalse(isBirthday(allDay("Team offsite")))
    }

    @Test fun birthdayPickerDates() {
        assertEquals(LocalDate(2024, 2, 29), monthDayDate(229))
        assertNull(monthDayDate(0))
        assertNull(monthDayDate(1340))
    }
}
