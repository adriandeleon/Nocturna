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
    @Test fun holidaysEveryoneGets() {
        assertEquals(Holiday.NEW_YEAR, holidayOn(LocalDate(2027, 1, 1), 0, "GB"))
        assertEquals(Holiday.NEW_YEAR, holidayOn(LocalDate(2026, 12, 31), 0, "GB"))
        assertEquals(Holiday.VALENTINES, holidayOn(LocalDate(2026, 2, 14), 0, "GB"))
        assertEquals(Holiday.HALLOWEEN, holidayOn(LocalDate(2026, 10, 31), 0, "US"))
        assertEquals(Holiday.CHRISTMAS, holidayOn(LocalDate(2026, 12, 24), 0, "GB"))
        assertNull(holidayOn(LocalDate(2026, 11, 16), 0, "GB"))
    }

    @Test fun mexicosOwnOnlyWithAMexicanRegion() {
        assertEquals(Holiday.DAY_OF_THE_DEAD, holidayOn(LocalDate(2026, 11, 2), 0, "MX"))
        assertEquals(Holiday.MEXICAN_INDEPENDENCE, holidayOn(LocalDate(2026, 9, 16), 0, "mx"))
        assertNull(holidayOn(LocalDate(2026, 11, 2), 0, "GB"))
        assertNull(holidayOn(LocalDate(2026, 9, 16), 0, ""))
    }

    @Test fun yourBirthdayComesFirst() {
        assertEquals(Holiday.BIRTHDAY, holidayOn(LocalDate(2026, 12, 25), 1225, "GB"))
        assertEquals(Holiday.BIRTHDAY, holidayOn(LocalDate(2026, 3, 7), 307, "GB"))
    }

    @Test fun aLeapDayBirthdayIsKeptOnThe28thInOtherYears() {
        assertEquals(Holiday.BIRTHDAY, holidayOn(LocalDate(2027, 2, 28), 229, "GB"))
        assertNull(holidayOn(LocalDate(2028, 2, 28), 229, "GB"))
        assertEquals(Holiday.BIRTHDAY, holidayOn(LocalDate(2028, 2, 29), 229, "GB"))
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
