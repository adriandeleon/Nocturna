package com.adl.nocturna.text

import kotlinx.datetime.DayOfWeek
import kotlinx.datetime.LocalTime
import kotlinx.datetime.toJavaDayOfWeek
import kotlinx.datetime.toJavaLocalTime
import java.time.format.DateTimeFormatter
import java.time.format.TextStyle

actual typealias PlatformLocale = java.util.Locale

actual fun formatTime(time: LocalTime, use24h: Boolean, locale: PlatformLocale): String =
    time.toJavaLocalTime().format(DateTimeFormatter.ofPattern(if (use24h) "HH:mm" else "h:mm a", locale))

actual fun DayOfWeek.shortName(locale: PlatformLocale): String = toJavaDayOfWeek().getDisplayName(TextStyle.SHORT, locale)
