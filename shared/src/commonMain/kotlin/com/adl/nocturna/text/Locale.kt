package com.adl.nocturna.text

import kotlinx.datetime.DayOfWeek
import kotlinx.datetime.LocalDate
import kotlinx.datetime.LocalDateTime
import kotlinx.datetime.LocalTime

/** A language and region, for words and formats: `java.util.Locale` on Android, `NSLocale` on iOS. */
expect class PlatformLocale

/** [time] as "18:00" ([use24h]) or "6:00 PM", with [locale]'s words for AM and PM. */
expect fun formatTime(time: LocalTime, use24h: Boolean, locale: PlatformLocale): String

/** The day's short name in [locale]'s language: "Mon", "lun.", "月"… */
expect fun DayOfWeek.shortName(locale: PlatformLocale): String

/** [locale]'s language as an ISO 639 code: "en", "es"… */
expect val PlatformLocale.languageCode: String

/** [value] with [fractionDigits] decimals, written the way [locale] writes numbers: "2.5", "2,5"… */
expect fun formatNumber(value: Double, fractionDigits: Int, locale: PlatformLocale): String

/** The day's one-letter name for a calendar's header row: "M", "L", "月"… */
expect fun DayOfWeek.narrowName(locale: PlatformLocale): String

/** The day's full name in [locale]'s language: "Monday", "lundi"… */
expect fun DayOfWeek.fullName(locale: PlatformLocale): String

/** The day [locale]'s weeks start on: Sunday in the US, Monday in most of Europe. */
expect fun firstDayOfWeek(locale: PlatformLocale): DayOfWeek

/** [date]'s week number by [locale]'s rules for when weeks start and which one is the first. */
expect fun weekOfYear(date: LocalDate, locale: PlatformLocale): Int

/** This text with its first letter capitalised, for names written in lowercase mid-sentence: "lundi" → "Lundi". */
expect fun String.capitalizedFirst(locale: PlatformLocale): String

/**
 * [locale]'s own pattern for the fields in [skeleton], in its order and with its punctuation:
 * "EEEEdMMMM" is "EEEE, MMMM d" in English and "EEEE d MMMM" in French.
 */
expect fun bestDateTimePattern(skeleton: String, locale: PlatformLocale): String

/**
 * Formats dates and times with a Unicode (CLDR) date [pattern], whose letters both platforms read the
 * same way, in [locale]'s language. Making one takes a moment, so keep it rather than make one per frame.
 */
expect class DateTimePattern(pattern: String, locale: PlatformLocale) {
    fun format(dateTime: LocalDateTime): String
}
