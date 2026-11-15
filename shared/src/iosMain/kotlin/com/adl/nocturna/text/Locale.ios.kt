package com.adl.nocturna.text

import kotlinx.datetime.DayOfWeek
import kotlinx.datetime.LocalTime
import kotlinx.datetime.isoDayNumber
import platform.Foundation.NSCalendar
import platform.Foundation.NSCalendarIdentifierGregorian
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
