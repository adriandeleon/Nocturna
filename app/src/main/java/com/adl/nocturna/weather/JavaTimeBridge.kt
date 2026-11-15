package com.adl.nocturna.weather

import kotlinx.datetime.toKotlinLocalDateTime
import java.time.LocalDateTime

// java.time overload of the shared forecast logic, for the clock's UI (see time/JavaTime.kt).

fun pickForecast(days: List<DayForecast>, now: LocalDateTime): ForecastPick? = pickForecast(days, now.toKotlinLocalDateTime())
