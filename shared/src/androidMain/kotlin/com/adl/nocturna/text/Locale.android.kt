package com.adl.nocturna.text

import android.text.format.DateFormat
import kotlinx.datetime.DayOfWeek
import kotlinx.datetime.LocalDate
import kotlinx.datetime.LocalDateTime
import kotlinx.datetime.LocalTime
import kotlinx.datetime.toJavaDayOfWeek
import kotlinx.datetime.toJavaLocalDate
import kotlinx.datetime.toJavaLocalDateTime
import kotlinx.datetime.toJavaLocalTime
import kotlinx.datetime.toKotlinDayOfWeek
import java.time.format.DateTimeFormatter
import java.time.format.TextStyle
import java.time.temporal.WeekFields

actual typealias PlatformLocale = java.util.Locale

actual fun formatTime(time: LocalTime, use24h: Boolean, locale: PlatformLocale): String =
    time.toJavaLocalTime().format(DateTimeFormatter.ofPattern(if (use24h) "HH:mm" else "h:mm a", locale))

actual fun DayOfWeek.shortName(locale: PlatformLocale): String = toJavaDayOfWeek().getDisplayName(TextStyle.SHORT, locale)

actual val PlatformLocale.languageCode: String get() = language

actual val PlatformLocale.countryCode: String get() = country

actual fun formatNumber(value: Double, fractionDigits: Int, locale: PlatformLocale): String =
    String.format(locale, "%.${fractionDigits}f", value)

actual fun DayOfWeek.narrowName(locale: PlatformLocale): String = toJavaDayOfWeek().getDisplayName(TextStyle.NARROW, locale)

actual fun DayOfWeek.fullName(locale: PlatformLocale): String = toJavaDayOfWeek().getDisplayName(TextStyle.FULL, locale)

actual fun firstDayOfWeek(locale: PlatformLocale): DayOfWeek = WeekFields.of(locale).firstDayOfWeek.toKotlinDayOfWeek()

actual fun weekOfYear(date: LocalDate, locale: PlatformLocale): Int =
    date.toJavaLocalDate().get(WeekFields.of(locale).weekOfWeekBasedYear())

actual fun String.capitalizedFirst(locale: PlatformLocale): String = replaceFirstChar { it.titlecase(locale) }

actual fun bestDateTimePattern(skeleton: String, locale: PlatformLocale): String = DateFormat.getBestDateTimePattern(locale, skeleton)

actual class DateTimePattern actual constructor(pattern: String, locale: PlatformLocale) {
    private val formatter = DateTimeFormatter.ofPattern(pattern, locale)

    actual fun format(dateTime: LocalDateTime): String = formatter.format(dateTime.toJavaLocalDateTime())
}
