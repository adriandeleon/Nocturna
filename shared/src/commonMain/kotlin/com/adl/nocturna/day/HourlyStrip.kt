package com.adl.nocturna.day

import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.Layout
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.Dp
import com.adl.nocturna.clock.Moment
import com.adl.nocturna.text.PlatformLocale
import com.adl.nocturna.ui.GlyphIcon
import com.adl.nocturna.weather.HourForecast
import com.adl.nocturna.weather.hourLabel
import com.adl.nocturna.weather.nextHours

/** Hours day mode's strip shows when there's room for them all. */
const val HOURLY_STRIP_HOURS = 6

/** Fewer hours than this and the strip isn't shown at all, rather than squeezed. */
const val HOURLY_STRIP_MIN_HOURS = 3

/** The chance of rain, a soft blue that reads as rain on the desk clock's navy. */
private val RainBlue = Color(0xFFA8D4FF)

/**
 * Day mode's next hours under the weather line: "14h ☀ 22° · 15h ⛅ 23° 40% · …", the chance of rain
 * only when it's [com.adl.nocturna.weather.SHOW_RAIN_FROM_PERCENT] or more. It shows as many hours as
 * fit in its width, from [HOURLY_STRIP_HOURS] down to [HOURLY_STRIP_MIN_HOURS], and nothing when even
 * those don't fit, so a narrow screen loses the strip rather than crowding the clock.
 */
@Composable
internal fun HourlyStrip(
    hours: List<HourForecast>,
    now: Moment,
    use24h: Boolean,
    fahrenheit: Boolean,
    locale: PlatformLocale,
    style: TextStyle,
    glyphSize: Dp,
    modifier: Modifier = Modifier,
) {
    // Picked again each minute, so an hour drops off as soon as it starts.
    val minute = now.epochMillis / 60_000L
    val slots = remember(hours, minute, now.zone) { nextHours(hours, now.instant, now.zone, HOURLY_STRIP_HOURS) }
    if (slots.size < HOURLY_STRIP_MIN_HOURS) return
    Layout(
        content = {
            slots.forEachIndexed { i, slot ->
                Row(verticalAlignment = Alignment.CenterVertically) {
                    if (i > 0) Text("  ·  ", style = style, softWrap = false)
                    Text(hourLabel(slot.local, use24h, locale), style = style, softWrap = false)
                    Spacer(Modifier.width(glyphSize * 0.25f))
                    GlyphIcon(slot.forecast.glyph, style.color, glyphSize)
                    Spacer(Modifier.width(glyphSize * 0.2f))
                    Text(slot.forecast.temperatureText(fahrenheit), style = style, softWrap = false)
                    slot.forecast.rainText?.let { rain ->
                        Spacer(Modifier.width(glyphSize * 0.25f))
                        Text(rain, style = style.copy(color = RainBlue.copy(alpha = style.color.alpha)), softWrap = false)
                    }
                }
            }
        },
        modifier = modifier,
    ) { measurables, constraints ->
        val placeables = measurables.map { it.measure(Constraints()) }
        val shown = fittingCount(placeables.map { it.width }, constraints.maxWidth)
            .takeIf { it >= HOURLY_STRIP_MIN_HOURS } ?: 0
        val visible = placeables.take(shown)
        val width = visible.sumOf { it.width }
        val height = visible.maxOfOrNull { it.height } ?: 0
        layout(width, height) {
            var x = 0
            visible.forEach {
                it.place(x, (height - it.height) / 2)
                x += it.width
            }
        }
    }
}

/** How many of [widths], from the first, fit side by side in [maxWidth]. */
internal fun fittingCount(widths: List<Int>, maxWidth: Int): Int {
    var used = 0
    widths.forEachIndexed { i, w ->
        if (used + w > maxWidth) return i
        used += w
    }
    return widths.size
}
