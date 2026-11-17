package com.adl.nocturna.day

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shadow
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import com.adl.nocturna.clock.Moment
import com.adl.nocturna.data.Settings
import com.adl.nocturna.text.shortName
import com.adl.nocturna.ui.GlyphIcon
import com.adl.nocturna.ui.currentLocale
import com.adl.nocturna.weather.Weather
import com.adl.nocturna.weather.pickForecast

/**
 * A pane of the desk clock's navy, light enough that the sky shows through: the weather belongs to the
 * background, yet stays readable over a bright sky or a busy photo. Under a third, warm hills behind it
 * turned it a muddy grey; at about two-thirds it stays blue over them. A faint light rim makes it read as glass.
 */
private val SkyGlass = Brush.verticalGradient(listOf(Color(0xFF2B3A55).copy(alpha = 0.66f), Color(0xFF1C263A).copy(alpha = 0.74f)))
private val SkyRim = Color.White.copy(alpha = 0.22f)

private val SkyShadow = Shadow(Color.Black.copy(alpha = 0.35f), Offset(0f, 2f), 10f)

/**
 * Day mode's weather, over the sky above the desk clock: the glyph and temperature, today's high and low
 * (tomorrow's in the evening), then [details] (rain on the way, the sun, UV, air) and, with [hourly], the
 * next hours. Sized from [fontSize], the note's text size; [compact] is a single slim line, for when the
 * sticky note needs the room.
 */
@Composable
internal fun SkyWeather(
    weather: Weather,
    details: List<String>,
    hourly: Boolean,
    compact: Boolean,
    now: Moment,
    settings: Settings,
    fontSize: TextUnit,
    modifier: Modifier = Modifier,
) {
    val locale = currentLocale()
    val line = with(LocalDensity.current) { fontSize.toDp() }
    val style = TextStyle(color = Color.White, fontSize = fontSize, shadow = SkyShadow)
    val dim = style.copy(color = Color.White.copy(alpha = 0.85f))
    val big = if (compact) 1.15f else 1.6f
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = modifier
            .clip(RoundedCornerShape(22.dp))
            .background(SkyGlass)
            .border(1.dp, SkyRim, RoundedCornerShape(22.dp))
            .padding(horizontal = 18.dp, vertical = if (compact) 2.dp else 6.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            GlyphIcon(weather.glyph, Color.White, line * big)
            Spacer(Modifier.width(line * 0.4f))
            Text(
                weather.temperatureText(settings.weatherFahrenheit),
                style = style.copy(fontSize = fontSize * big, fontWeight = FontWeight.Light),
                softWrap = false,
            )
            val pick = if (settings.showForecast) pickForecast(weather.days, now.local) else null
            if (pick != null) {
                // Today: "↑24° ↓12°". Evening: "Sat ☁ ↑20° ↓11°" for tomorrow.
                Spacer(Modifier.width(line * 0.8f))
                if (pick.tomorrow) {
                    Text(pick.day.date.dayOfWeek.shortName(locale), style = dim, softWrap = false)
                    Spacer(Modifier.width(line * 0.3f))
                    GlyphIcon(pick.day.glyph, dim.color, line)
                    Spacer(Modifier.width(line * 0.3f))
                }
                Text(pick.day.highLowText(settings.weatherFahrenheit), style = dim, softWrap = false)
            }
        }
        if (!compact && details.isNotEmpty()) GlanceLine(details, fontSize)
        if (!compact && hourly && weather.hours.isNotEmpty()) {
            Spacer(Modifier.height(line * 0.3f))
            HourlyStrip(
                weather.hours, now, settings.use24h, settings.weatherFahrenheit, locale,
                dim.copy(fontSize = fontSize * 0.85f), line * 0.85f,
            )
        }
    }
}
