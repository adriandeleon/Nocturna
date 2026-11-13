package com.adl.nocturna.clock

import android.graphics.Typeface
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
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
import java.time.ZonedDateTime
import java.time.format.DateTimeFormatter
import com.adl.nocturna.ui.currentLocale

fun ClockFont.family(): FontFamily = when (this) {
    ClockFont.THIN, ClockFont.LIGHT, ClockFont.REGULAR -> FontFamily.SansSerif
    ClockFont.CONDENSED -> FontFamily(Typeface.create("sans-serif-condensed", Typeface.NORMAL))
    ClockFont.MONO -> FontFamily.Monospace
    ClockFont.SERIF -> FontFamily.Serif
}

fun ClockFont.weight(): FontWeight = when (this) {
    ClockFont.THIN -> FontWeight.Thin
    ClockFont.LIGHT -> FontWeight.Light
    else -> FontWeight.Normal
}

fun ClockFont.label(): String = when (this) {
    ClockFont.THIN -> "Thin"
    ClockFont.LIGHT -> "Light"
    ClockFont.REGULAR -> "Regular"
    ClockFont.CONDENSED -> "Condensed"
    ClockFont.MONO -> "Mono"
    ClockFont.SERIF -> "Serif"
}

private val tight = LineHeightStyle(LineHeightStyle.Alignment.Center, LineHeightStyle.Trim.Both)

/** Large digital time with optional small seconds and AM/PM marker. */
@Composable
fun DigitalClock(
    now: ZonedDateTime,
    use24h: Boolean,
    showSeconds: Boolean,
    font: ClockFont,
    color: Color,
    fontSize: TextUnit,
) {
    val locale = currentLocale()
    val hm = now.format(DateTimeFormatter.ofPattern(if (use24h) "HH:mm" else "h:mm", locale))
    val style = TextStyle(
        color = color,
        fontSize = fontSize,
        fontFamily = font.family(),
        fontWeight = font.weight(),
        fontFeatureSettings = "tnum",
        lineHeight = 1.em,
        lineHeightStyle = tight,
        letterSpacing = (-0.02).em,
    )
    Row(verticalAlignment = Alignment.Bottom) {
        Text(hm, style = style)
        if (showSeconds || !use24h) {
            val small = style.copy(fontSize = fontSize * 0.28f, letterSpacing = 0.em)
            androidx.compose.foundation.layout.Column(
                horizontalAlignment = Alignment.Start,
                modifier = Modifier,
            ) {
                if (!use24h) Text(now.format(DateTimeFormatter.ofPattern("a", locale)), style = small)
                if (showSeconds) Text(now.format(DateTimeFormatter.ofPattern("ss")), style = small)
            }
        }
    }
}

/** A minimal analog face: hour ticks, three hands. */
@Composable
fun AnalogClock(now: ZonedDateTime, showSeconds: Boolean, color: Color, size: Dp) {
    Canvas(Modifier.size(size)) {
        val r = this.size.minDimension / 2
        val c = center
        val w = r * 0.025f
        for (i in 0 until 60) {
            val hour = i % 5 == 0
            if (!hour && r < 120f) continue
            tick(c, r, i * 6f, if (hour) r * 0.86f else r * 0.93f, color.copy(alpha = if (hour) 1f else 0.4f), if (hour) w * 1.4f else w * 0.6f)
        }
        val sec = now.second + now.nano / 1e9f
        val min = now.minute + sec / 60f
        val hr = (now.hour % 12) + min / 60f
        hand(c, hr * 30f, r * 0.5f, color, w * 2.6f)
        hand(c, min * 6f, r * 0.78f, color, w * 1.6f)
        if (showSeconds) hand(c, now.second * 6f, r * 0.86f, color.copy(alpha = 0.7f), w * 0.7f, tail = r * 0.12f)
        drawCircle(color, w * 2.6f, c)
    }
}

private fun DrawScope.tick(c: Offset, r: Float, deg: Float, inner: Float, color: Color, width: Float) {
    rotate(deg, c) { drawLine(color, Offset(c.x, c.y - inner), Offset(c.x, c.y - r), width, StrokeCap.Round) }
}

private fun DrawScope.hand(c: Offset, deg: Float, length: Float, color: Color, width: Float, tail: Float = 0f) {
    rotate(deg, c) { drawLine(color, Offset(c.x, c.y + tail), Offset(c.x, c.y - length), width, StrokeCap.Round) }
}
