package com.adl.nocturna.clock

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.ui.layout.LastBaseline
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shadow
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.LineHeightStyle
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.em
import com.adl.nocturna.data.ClockFont
import com.adl.nocturna.text.DateTimePattern
import com.adl.nocturna.ui.currentLocale

fun ClockFont.family(): FontFamily = when (this) {
    ClockFont.THIN, ClockFont.LIGHT, ClockFont.REGULAR -> FontFamily.SansSerif
    ClockFont.CONDENSED -> condensedFontFamily()
    ClockFont.MONO -> FontFamily.Monospace
    ClockFont.SERIF -> FontFamily.Serif
}

internal expect fun condensedFontFamily(): FontFamily

fun ClockFont.weight(): FontWeight = when (this) {
    ClockFont.THIN -> FontWeight.Thin
    ClockFont.LIGHT -> FontWeight.Light
    else -> FontWeight.Normal
}

private val tight = LineHeightStyle(LineHeightStyle.Alignment.Center, LineHeightStyle.Trim.Both)

/** Large digital time with optional small seconds and AM/PM marker. */
@Composable
fun DigitalClock(
    now: Moment,
    use24h: Boolean,
    showSeconds: Boolean,
    font: ClockFont,
    color: Color,
    fontSize: TextUnit,
    shadow: Shadow? = null,
) {
    val locale = currentLocale()
    val hm = remember(use24h, locale) { DateTimePattern(if (use24h) "HH:mm" else "h:mm", locale) }.format(now.local)
    val style = TextStyle(
        color = color,
        fontSize = fontSize,
        fontFamily = font.family(),
        fontWeight = font.weight(),
        fontFeatureSettings = "tnum",
        lineHeight = 1.em,
        lineHeightStyle = tight,
        letterSpacing = (-0.02).em,
        shadow = shadow,
    )
    // ":ss" sits on the same baseline as the big digits; AM/PM sits above it, near the top of the digits.
    Row {
        Text(hm, style = style, modifier = Modifier.alignBy(LastBaseline))
        if (showSeconds || !use24h) {
            val small = style.copy(fontSize = fontSize * 0.26f, letterSpacing = 0.em)
            val gap = with(LocalDensity.current) { (fontSize * 0.06f).toDp() }
            Column(Modifier.alignBy(LastBaseline).padding(start = gap)) {
                if (!use24h) Text(remember(locale) { DateTimePattern("a", locale) }.format(now.local), style = small, softWrap = false)
                if (!use24h && showSeconds) Spacer(Modifier.height(with(LocalDensity.current) { (fontSize * 0.2f).toDp() }))
                if (showSeconds) {
                    Text(":" + now.local.second.toString().padStart(2, '0'), style = small.copy(color = color.copy(alpha = color.alpha * 0.6f)), softWrap = false)
                }
            }
        }
    }
}

/** A minimal analog face: hour ticks, three hands. */
@Composable
fun AnalogClock(now: Moment, showSeconds: Boolean, color: Color, size: Dp) {
    Canvas(Modifier.size(size)) {
        val r = this.size.minDimension / 2
        val c = center
        val w = r * 0.025f
        for (i in 0 until 60) {
            val hour = i % 5 == 0
            if (!hour && r < 120f) continue
            tick(c, r, i * 6f, if (hour) r * 0.86f else r * 0.93f, color.copy(alpha = if (hour) 1f else 0.4f), if (hour) w * 1.4f else w * 0.6f)
        }
        val sec = now.local.second + now.local.nanosecond / 1e9f
        val min = now.local.minute + sec / 60f
        val hr = (now.hour % 12) + min / 60f
        hand(c, hr * 30f, r * 0.5f, color, w * 2.6f)
        hand(c, min * 6f, r * 0.78f, color, w * 1.6f)
        if (showSeconds) hand(c, now.local.second * 6f, r * 0.86f, color.copy(alpha = 0.7f), w * 0.7f, tail = r * 0.12f)
        drawCircle(color, w * 2.6f, c)
    }
}

private fun DrawScope.tick(c: Offset, r: Float, deg: Float, inner: Float, color: Color, width: Float) {
    rotate(deg, c) { drawLine(color, Offset(c.x, c.y - inner), Offset(c.x, c.y - r), width, StrokeCap.Round) }
}

private fun DrawScope.hand(c: Offset, deg: Float, length: Float, color: Color, width: Float, tail: Float = 0f) {
    rotate(deg, c) { drawLine(color, Offset(c.x, c.y + tail), Offset(c.x, c.y - length), width, StrokeCap.Round) }
}
