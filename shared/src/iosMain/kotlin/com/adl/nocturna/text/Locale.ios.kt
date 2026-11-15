package com.adl.nocturna.text

import kotlinx.datetime.DayOfWeek
import kotlinx.datetime.LocalDate
import kotlinx.datetime.LocalDateTime
import kotlinx.datetime.LocalTime
import kotlinx.datetime.TimeZone
import kotlinx.datetime.atTime
import kotlinx.datetime.isoDayNumber
import kotlinx.datetime.toInstant
import platform.Foundation.NSCalendar
import platform.Foundation.NSCalendarIdentifierGregorian
import platform.Foundation.NSCalendarUnitWeekOfYear
import platform.Foundation.NSDate
import platform.Foundation.dateWithTimeIntervalSince1970
import platform.Foundation.NSDateComponents
import platform.Foundation.NSDateFormatter
import platform.Foundation.NSLocale
import platform.Foundation.NSTimeZone
import platform.Foundation.timeZoneForSecondsFromGMT

actual typealias PlatformLocale = NSLocale

actual fun formatTime(time: LocalTime, use24h: Boolean, locale: PlatformLocale): String {
    // A fixed date in UTC on the Gregorian calendar, so only the time of day can show.
    val utc = NSTimeZone.timeZoneForSecondsFromGMT(0)
    val calendar = NSCalendar(calendarIdentifier = NSCalendarIdentifierGregorian).apply { timeZone = utc }
    val parts = NSDateComponents().apply {
        year = 2001
        month = 1
        day = 1
        hour = time.hour.toLong()
        minute = time.minute.toLong()
    }
    val date = calendar.dateFromComponents(parts) ?: return time.toString()
    val formatter = NSDateFormatter().apply {
        this.locale = locale
        timeZone = utc
        dateFormat = if (use24h) "HH:mm" else "h:mm a"
    }
    return formatter.stringFromDate(date)
}

actual fun DayOfWeek.shortName(locale: PlatformLocale): String {
    // Foundation's list starts on Sunday; ISO numbers run Monday 1 to Sunday 7.
    val names = NSDateFormatter().apply { this.locale = locale }.shortWeekdaySymbols
    return names[isoDayNumber % 7] as String
}

actual fun DayOfWeek.narrowName(locale: PlatformLocale): String =
    NSDateFormatter().apply { this.locale = locale }.veryShortStandaloneWeekdaySymbols[isoDayNumber % 7] as String

actual fun DayOfWeek.fullName(locale: PlatformLocale): String =
    NSDateFormatter().apply { this.locale = locale }.weekdaySymbols[isoDayNumber % 7] as String

actual fun firstDayOfWeek(locale: PlatformLocale): DayOfWeek {
    // Foundation numbers weekdays from Sunday 1 to Saturday 7.
    val first = gregorian(locale).firstWeekday.toInt()
    return DayOfWeek((first + 5) % 7 + 1)
}

actual fun weekOfYear(date: LocalDate, locale: PlatformLocale): Int =
    gregorian(locale).component(NSCalendarUnitWeekOfYear, fromDate = nsDate(date.atTime(12, 0))).toInt()

// Foundation has no single-letter title case by locale; this differs from Android's only in a few
// languages' dotted and dotless i.
actual fun String.capitalizedFirst(locale: PlatformLocale): String = replaceFirstChar { it.titlecase() }

actual fun bestDateTimePattern(skeleton: String, locale: PlatformLocale): String =
    NSDateFormatter.dateFormatFromTemplate(skeleton, 0u, locale) ?: skeleton

actual class DateTimePattern actual constructor(pattern: String, locale: PlatformLocale) {
    private val formatter = NSDateFormatter().apply {
        this.locale = locale
        calendar = gregorian(locale)
        timeZone = utc
        dateFormat = pattern
    }

    actual fun format(dateTime: LocalDateTime): String = formatter.stringFromDate(nsDate(dateTime))
}

private val utc = NSTimeZone.timeZoneForSecondsFromGMT(0)

/** The Gregorian calendar in UTC, with [locale]'s first day of the week and week numbering. */
private fun gregorian(locale: PlatformLocale) = NSCalendar(calendarIdentifier = NSCalendarIdentifierGregorian).apply {
    this.locale = locale
    timeZone = utc
}

/** [dateTime] read in UTC, the zone the formatters above use, so it shows exactly as given. */
private fun nsDate(dateTime: LocalDateTime): NSDate =
    NSDate.dateWithTimeIntervalSince1970(dateTime.toInstant(TimeZone.UTC).toEpochMilliseconds() / 1000.0)
