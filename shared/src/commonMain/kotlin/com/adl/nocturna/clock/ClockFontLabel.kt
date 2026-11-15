package com.adl.nocturna.clock

import com.adl.nocturna.data.ClockFont

/** How a clock font is called in Settings. */
fun ClockFont.label(): String = when (this) {
    ClockFont.THIN -> "Thin"
    ClockFont.LIGHT -> "Light"
    ClockFont.REGULAR -> "Regular"
    ClockFont.CONDENSED -> "Condensed"
    ClockFont.MONO -> "Mono"
    ClockFont.SERIF -> "Serif"
}
