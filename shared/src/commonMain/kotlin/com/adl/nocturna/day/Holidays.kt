package com.adl.nocturna.day

import com.adl.nocturna.alarm.matchesDayOffKeyword
import com.adl.nocturna.alarm.parseDayOffKeywords
import com.adl.nocturna.text.PlatformLocale
import com.adl.nocturna.text.strings
import kotlinx.datetime.LocalDate
import kotlinx.datetime.Month
import kotlinx.datetime.number

/*
 * Holiday touches for day mode: a few days of the year get decorations in the scene and their own greeting
 * on the sticky note, and so does the user's birthday.
 */

/** The days day mode decorates. [MEXICAN_INDEPENDENCE] and [DAY_OF_THE_DEAD] only with a Mexican region. */
enum class Holiday { BIRTHDAY, NEW_YEAR, VALENTINES, MEXICAN_INDEPENDENCE, HALLOWEEN, DAY_OF_THE_DEAD, CHRISTMAS }

/**
 * The holiday on [date], if any: the user's [birthday] (month * 100 + day, 0 for none) first, then the
 * holidays everyone gets, and Mexico's own when the phone's region ([country]) is Mexico. A 29 February
 * birthday is kept on 28 February in other years.
 */
fun holidayOn(date: LocalDate, birthday: Int, country: String): Holiday? {
    val md = date.month.number * 100 + date.day
    val leap = date.year % 4 == 0 && (date.year % 100 != 0 || date.year % 400 == 0)
    if (birthday != 0 && (md == birthday || (birthday == 229 && !leap && md == 228))) return Holiday.BIRTHDAY
    val mexico = country.equals("MX", ignoreCase = true)
    return when {
        md == 101 || md == 1231 -> Holiday.NEW_YEAR
        md == 214 -> Holiday.VALENTINES
        mexico && (md == 915 || md == 916) -> Holiday.MEXICAN_INDEPENDENCE
        md == 1031 -> Holiday.HALLOWEEN
        mexico && (md == 1101 || md == 1102) -> Holiday.DAY_OF_THE_DEAD
        md == 1224 || md == 1225 -> Holiday.CHRISTMAS
        else -> null
    }
}

/** The sticky note's greeting on [holiday], e.g. "Happy birthday!". */
fun holidayGreeting(holiday: Holiday, locale: PlatformLocale): String {
    val t = strings(locale)
    return when (holiday) {
        Holiday.BIRTHDAY -> t.happyBirthday
        Holiday.NEW_YEAR -> t.happyNewYear
        Holiday.VALENTINES -> t.happyValentines
        Holiday.MEXICAN_INDEPENDENCE -> t.vivaMexico
        Holiday.HALLOWEEN -> t.happyHalloween
        Holiday.DAY_OF_THE_DEAD -> t.happyDayOfTheDead
        Holiday.CHRISTMAS -> t.merryChristmas
    }
}

/** [monthDay] (month * 100 + day) as a date in [year], for the birthday picker; 29 February in a leap year. */
fun monthDayDate(monthDay: Int, year: Int = 2024): LocalDate? =
    runCatching { LocalDate(year, Month(monthDay / 100), monthDay % 100) }.getOrNull()

/** Words in an event's title that make it a birthday, matched like the day-off words. */
const val BIRTHDAY_KEYWORDS = "birthday, bday, b-day, cumpleaños, cumple"

private val birthdayWords = parseDayOffKeywords(BIRTHDAY_KEYWORDS)

/** Whether [event] is someone's birthday: an all-day event with a birthday word in its title. */
fun isBirthday(event: CalendarEvent): Boolean = event.allDay && matchesDayOffKeyword(event.title, birthdayWords)
