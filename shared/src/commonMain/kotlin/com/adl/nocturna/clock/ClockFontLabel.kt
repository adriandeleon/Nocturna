package com.adl.nocturna.clock

import com.adl.nocturna.data.ClockFont
import com.adl.nocturna.text.PlatformLocale
import com.adl.nocturna.text.strings

/** How a clock font is called in Settings. */
fun ClockFont.label(locale: PlatformLocale): String = with(strings(locale)) {
    when (this@label) {
        ClockFont.THIN -> fontThin
        ClockFont.LIGHT -> fontLight
        ClockFont.REGULAR -> fontRegular
        ClockFont.CONDENSED -> fontCondensed
        ClockFont.MONO -> fontMono
        ClockFont.SERIF -> fontSerif
    }
}
