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
