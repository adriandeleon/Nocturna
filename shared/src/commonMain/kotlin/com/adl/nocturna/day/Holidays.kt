package com.adl.nocturna.day

import com.adl.nocturna.alarm.matchesDayOffKeyword
import com.adl.nocturna.alarm.parseDayOffKeywords
import com.adl.nocturna.text.PlatformLocale
import com.adl.nocturna.text.strings
import kotlinx.datetime.DateTimeUnit
import kotlinx.datetime.DayOfWeek
import kotlinx.datetime.LocalDate
import kotlinx.datetime.Month
import kotlinx.datetime.daysUntil
import kotlinx.datetime.isoDayNumber
import kotlinx.datetime.minus
import kotlinx.datetime.number

/*
 * Holiday touches for day mode: a few days of the year get decorations in the scene and their own greeting
 * on the sticky note, and so does the user's birthday. Some holidays decorate the days around them too,
 * more lightly and without the greeting. Holidays that belong to some countries show only with the phone's
 * region ([holidayOn]'s country) set to one of them.
 */

/** The days day mode decorates. */
enum class Holiday {
    BIRTHDAY, NEW_YEAR, VALENTINES, MEXICAN_INDEPENDENCE, HALLOWEEN, DAY_OF_THE_DEAD, CHRISTMAS,
    POSADAS, THREE_KINGS, CHILDRENS_DAY, MOTHERS_DAY, FATHERS_DAY, EASTER, ST_PATRICKS, US_INDEPENDENCE,
    CANADA_DAY, THANKSGIVING, LUNAR_NEW_YEAR, DIWALI, HANUKKAH,
}

/** [holiday] on some day: [theDay] itself (the greeting, the full decorations) or a day around it. */
data class HolidayDay(val holiday: Holiday, val theDay: Boolean)

/**
 * The holiday on [date], if any: the user's [birthday] (month * 100 + day, 0 for none) first, then the
 * holidays of the phone's region ([country], an ISO code such as "MX") that follow the moon, then those on
 * fixed dates and last those that move (Easter, Mother's and Father's Day, Thanksgiving). A 29 February
 * birthday is kept on 28 February in other years.
 *
 * Everywhere: New Year's Eve and Day; Valentine's Day (14 February, from the 10th more lightly); Easter
 * Sunday; Halloween (31 October, from the 24th more lightly); Christmas (24 and 25 December, lightly from
 * 1 to 30 December). Mexico: Independence (15 and 16 September, from the 1st), Day of the Dead (1 and 2
 * November), Las Posadas (16 to 23 December), Christmas lightly until 5 January, Día de Reyes (6 January),
 * Día del Niño (30 April) and Mother's Day (10 May). See [mothersDay], [fathersDay] and the region sets
 * below for the rest.
 */
fun holidayOn(date: LocalDate, birthday: Int, country: String): HolidayDay? {
    val md = date.month.number * 100 + date.day
    val leap = date.year % 4 == 0 && (date.year % 100 != 0 || date.year % 400 == 0)
    if (birthday != 0 && (md == birthday || (birthday == 229 && !leap && md == 228))) return HolidayDay(Holiday.BIRTHDAY, true)
    val region = country.uppercase()
    return lunarHoliday(date, region) ?: fixedHoliday(md, region) ?: movingHoliday(date, region)
}

private fun day(h: Holiday) = HolidayDay(h, true)
private fun around(h: Holiday) = HolidayDay(h, false)

/** Lunar New Year, Diwali and Hanukkah, from the tables below, in the regions that keep them. */
private fun lunarHoliday(date: LocalDate, region: String): HolidayDay? {
    if (region in LUNAR_NEW_YEAR_REGIONS) {
        tableDate(LUNAR_NEW_YEAR, date.year)?.let { newYear ->
            // The eve and the day itself, then lanterns until the Lantern Festival two weeks on.
            when (newYear.daysUntil(date)) {
                -1, 0 -> return day(Holiday.LUNAR_NEW_YEAR)
                in 1..14 -> return around(Holiday.LUNAR_NEW_YEAR)
            }
        }
    }
    if (region in DIWALI_REGIONS) {
        tableDate(DIWALI, date.year)?.let { diwali ->
            // The five days of the festival, with Lakshmi Puja in the middle.
            when (diwali.daysUntil(date)) {
                0 -> return day(Holiday.DIWALI)
                -2, -1, 1, 2 -> return around(Holiday.DIWALI)
            }
        }
    }
    if (region in HANUKKAH_REGIONS) {
        // The eight days from the first candle; a late Hanukkah runs into January.
        for (year in listOf(date.year, date.year - 1)) {
            val first = tableDate(HANUKKAH_FIRST_CANDLE, year) ?: continue
            if (first.daysUntil(date) in 0..7) return day(Holiday.HANUKKAH)
        }
    }
    return null
}

private fun fixedHoliday(md: Int, region: String): HolidayDay? {
    val mexico = region == "MX"
    return when (md) {
        101, 1231 -> day(Holiday.NEW_YEAR)
        in 102..105 -> if (mexico) around(Holiday.CHRISTMAS) else null
        106 -> if (mexico) day(Holiday.THREE_KINGS) else null
        214 -> day(Holiday.VALENTINES)
        in 210..213 -> around(Holiday.VALENTINES)
        317 -> if (region in ST_PATRICKS_REGIONS) day(Holiday.ST_PATRICKS) else null
        430 -> if (mexico) day(Holiday.CHILDRENS_DAY) else null
        510 -> if (mexico) day(Holiday.MOTHERS_DAY) else null
        701 -> if (region == "CA") day(Holiday.CANADA_DAY) else null
        704 -> if (region == "US") day(Holiday.US_INDEPENDENCE) else null
        915, 916 -> if (mexico) day(Holiday.MEXICAN_INDEPENDENCE) else null
        in 901..914 -> if (mexico) around(Holiday.MEXICAN_INDEPENDENCE) else null
        1031 -> day(Holiday.HALLOWEEN)
        in 1024..1030 -> around(Holiday.HALLOWEEN)
        1101, 1102 -> if (mexico) day(Holiday.DAY_OF_THE_DEAD) else null
        1224, 1225 -> day(Holiday.CHRISTMAS)
        in 1216..1223 -> if (mexico) day(Holiday.POSADAS) else around(Holiday.CHRISTMAS)
        in 1201..1215, in 1226..1230 -> around(Holiday.CHRISTMAS)
        else -> null
    }
}

private fun movingHoliday(date: LocalDate, region: String): HolidayDay? = when (date) {
    easterSunday(date.year) -> day(Holiday.EASTER)
    mothersDay(date.year, region) -> day(Holiday.MOTHERS_DAY)
    fathersDay(date.year, region) -> day(Holiday.FATHERS_DAY)
    thanksgiving(date.year, region) -> day(Holiday.THANKSGIVING)
    else -> null
}

/** Easter Sunday in [year], in the Gregorian calendar (the Meeus/Jones/Butcher method). */
fun easterSunday(year: Int): LocalDate {
    val a = year % 19
    val b = year / 100
    val c = year % 100
    val h = (19 * a + b - b / 4 - (b - (b + 8) / 25 + 1) / 3 + 15) % 30
    val l = (32 + 2 * (b % 4) + 2 * (c / 4) - h - c % 4) % 7
    val m = (a + 11 * h + 22 * l) / 451
    val month = (h + l - 7 * m + 114) / 31
    val day = (h + l - 7 * m + 114) % 31 + 1
    return LocalDate(year, month, day)
}

/** The [n]th [weekday] of [month] in [year], e.g. the second Sunday of May. */
fun nthWeekday(year: Int, month: Int, weekday: DayOfWeek, n: Int): LocalDate {
    val first = LocalDate(year, month, 1)
    val offset = (weekday.isoDayNumber - first.dayOfWeek.isoDayNumber + 7) % 7
    return LocalDate(year, month, 1 + offset + (n - 1) * 7)
}

/**
 * Mother's Day in [year] for [region], or null where it isn't known: 10 May in Mexico, Mothering Sunday
 * (three weeks before Easter) in the UK and Ireland, the first Sunday of May in Spain and Portugal, and the
 * second Sunday of May in the countries of [SECOND_SUNDAY_OF_MAY].
 */
fun mothersDay(year: Int, region: String): LocalDate? = when (region) {
    "MX" -> LocalDate(year, 5, 10)
    "GB", "IE" -> easterSunday(year).minus(21, DateTimeUnit.DAY)
    "ES", "PT" -> nthWeekday(year, 5, DayOfWeek.SUNDAY, 1)
    in SECOND_SUNDAY_OF_MAY -> nthWeekday(year, 5, DayOfWeek.SUNDAY, 2)
    else -> null
}

/**
 * Father's Day in [year] for [region], or null where it isn't known: 19 March (Saint Joseph's day) in Spain,
 * Italy and Portugal, the first Sunday of September in Australia and New Zealand, and the third Sunday of
 * June in the countries of [THIRD_SUNDAY_OF_JUNE].
 */
fun fathersDay(year: Int, region: String): LocalDate? = when (region) {
    "ES", "IT", "PT" -> LocalDate(year, 3, 19)
    "AU", "NZ" -> nthWeekday(year, 9, DayOfWeek.SUNDAY, 1)
    in THIRD_SUNDAY_OF_JUNE -> nthWeekday(year, 6, DayOfWeek.SUNDAY, 3)
    else -> null
}

/** Thanksgiving in [year]: the fourth Thursday of November in the US, the second Monday of October in Canada. */
fun thanksgiving(year: Int, region: String): LocalDate? = when (region) {
    "US" -> nthWeekday(year, 11, DayOfWeek.THURSDAY, 4)
    "CA" -> nthWeekday(year, 10, DayOfWeek.MONDAY, 2)
    else -> null
}

private val SECOND_SUNDAY_OF_MAY = setOf(
    "US", "CA", "AU", "NZ", "DE", "AT", "CH", "IT", "BE", "NL", "DK", "FI", "BR", "CL", "CO", "PE", "VE", "EC",
    "UY", "JP", "CN", "HK", "TW", "SG", "MY", "PH", "IN", "ZA", "TR", "GR",
)
private val THIRD_SUNDAY_OF_JUNE = setOf(
    "US", "CA", "MX", "GB", "IE", "FR", "NL", "JP", "CN", "HK", "SG", "MY", "PH", "IN", "ZA", "CL", "CO", "PE",
    "VE", "EC", "AR", "TR", "GR",
)
private val ST_PATRICKS_REGIONS = setOf("IE", "GB", "US", "CA", "AU", "NZ")
private val LUNAR_NEW_YEAR_REGIONS = setOf("CN", "TW", "HK", "MO", "SG", "MY", "KR", "VN")
private val DIWALI_REGIONS = setOf("IN", "NP")
private val HANUKKAH_REGIONS = setOf("IL")

/*
 * Holidays that follow a lunar calendar, as month * 100 + day by year. Years past the end of a table have
 * none until it is extended. Lunar New Year: the first day of the Spring Festival. Diwali: India's gazetted
 * Diwali/Deepavali (timeanddate.com). Hanukkah: the day of the first candle, lit that evening (Hebcal).
 */
private val LUNAR_NEW_YEAR = mapOf(
    2026 to 217, 2027 to 206, 2028 to 126, 2029 to 213, 2030 to 203,
    2031 to 123, 2032 to 211, 2033 to 131, 2034 to 219, 2035 to 208,
)
private val DIWALI = mapOf(2026 to 1108, 2027 to 1029, 2028 to 1017, 2029 to 1105, 2030 to 1026, 2031 to 1114)
private val HANUKKAH_FIRST_CANDLE = mapOf(
    2026 to 1204, 2027 to 1224, 2028 to 1212, 2029 to 1201, 2030 to 1220,
    2031 to 1209, 2032 to 1127, 2033 to 1216, 2034 to 1206, 2035 to 1225,
)

private fun tableDate(table: Map<Int, Int>, year: Int): LocalDate? =
    table[year]?.let { LocalDate(year, it / 100, it % 100) }

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
        Holiday.POSADAS -> t.happyPosadas
        Holiday.THREE_KINGS -> t.happyThreeKings
        Holiday.CHILDRENS_DAY -> t.happyChildrensDay
        Holiday.MOTHERS_DAY -> t.happyMothersDay
        Holiday.FATHERS_DAY -> t.happyFathersDay
        Holiday.EASTER -> t.happyEaster
        Holiday.ST_PATRICKS -> t.happyStPatricks
        Holiday.US_INDEPENDENCE -> t.happyFourthOfJuly
        Holiday.CANADA_DAY -> t.happyCanadaDay
        Holiday.THANKSGIVING -> t.happyThanksgiving
        Holiday.LUNAR_NEW_YEAR -> t.happyLunarNewYear
        Holiday.DIWALI -> t.happyDiwali
        Holiday.HANUKKAH -> t.happyHanukkah
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
