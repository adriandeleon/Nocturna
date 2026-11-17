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
 * on the sticky note, and so does the user's birthday. Some holidays decorate the days leading up to them
 * too (Christmas also the days after it), more lightly and without the greeting.
 */

/** The days day mode decorates. [MEXICAN_INDEPENDENCE] and [DAY_OF_THE_DEAD] only with a Mexican region. */
enum class Holiday { BIRTHDAY, NEW_YEAR, VALENTINES, MEXICAN_INDEPENDENCE, HALLOWEEN, DAY_OF_THE_DEAD, CHRISTMAS }

/** [holiday] on some day: [theDay] itself (the greeting, the full decorations) or a day around it. */
data class HolidayDay(val holiday: Holiday, val theDay: Boolean)

/**
 * The holiday on [date], if any: the user's [birthday] (month * 100 + day, 0 for none) first, then the
 * holidays everyone gets, and Mexico's own when the phone's region ([country]) is Mexico. A 29 February
 * birthday is kept on 28 February in other years.
 *
 * The days themselves are New Year's Eve and Day, 14 February, 15 and 16 September (Mexico), 31 October,
 * 1 and 2 November (Mexico), and 24 and 25 December. Around them: 10–13 February, 1–14 September
 * (Mexico, the *mes patrio*), 24–30 October, and 1–23 and 26–30 December.
 */
fun holidayOn(date: LocalDate, birthday: Int, country: String): HolidayDay? {
    val md = date.month.number * 100 + date.day
    val leap = date.year % 4 == 0 && (date.year % 100 != 0 || date.year % 400 == 0)
    if (birthday != 0 && (md == birthday || (birthday == 229 && !leap && md == 228))) return HolidayDay(Holiday.BIRTHDAY, true)
    val mexico = country.equals("MX", ignoreCase = true)
    fun day(h: Holiday) = HolidayDay(h, true)
    fun around(h: Holiday) = HolidayDay(h, false)
    return when (md) {
        101, 1231 -> day(Holiday.NEW_YEAR)
        214 -> day(Holiday.VALENTINES)
        in 210..213 -> around(Holiday.VALENTINES)
        915, 916 -> if (mexico) day(Holiday.MEXICAN_INDEPENDENCE) else null
        in 901..914 -> if (mexico) around(Holiday.MEXICAN_INDEPENDENCE) else null
        1031 -> day(Holiday.HALLOWEEN)
        in 1024..1030 -> around(Holiday.HALLOWEEN)
        1101, 1102 -> if (mexico) day(Holiday.DAY_OF_THE_DEAD) else null
        1224, 1225 -> day(Holiday.CHRISTMAS)
        in 1201..1223, in 1226..1230 -> around(Holiday.CHRISTMAS)
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
