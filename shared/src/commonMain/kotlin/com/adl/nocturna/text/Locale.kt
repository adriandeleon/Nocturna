package com.adl.nocturna.text

import kotlinx.datetime.DayOfWeek
import kotlinx.datetime.LocalTime

/** A language and region, for words and formats: `java.util.Locale` on Android, `NSLocale` on iOS. */
expect class PlatformLocale

/** [time] as "18:00" ([use24h]) or "6:00 PM", with [locale]'s words for AM and PM. */
expect fun formatTime(time: LocalTime, use24h: Boolean, locale: PlatformLocale): String

/** The day's short name in [locale]'s language: "Mon", "lun.", "月"… */
expect fun DayOfWeek.shortName(locale: PlatformLocale): String
