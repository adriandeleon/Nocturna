package com.adl.nocturna.day

import android.text.format.DateFormat
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.SubcomposeLayout
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.constrainHeight
import androidx.compose.ui.unit.constrainWidth
import androidx.compose.ui.unit.dp
import com.adl.nocturna.data.Settings
import com.adl.nocturna.ui.currentLocale
import java.time.LocalDate
import java.time.YearMonth
import java.time.ZonedDateTime
import java.time.format.DateTimeFormatter
import java.time.temporal.WeekFields

/**
 * Day mode: the clock beside this month's calendar and today's events, for a phone on a desk stand.
 * Side by side in landscape, stacked in portrait. [clock] draws the clock and its info line.
 */
@Composable
fun DayLayout(
    landscape: Boolean,
    now: ZonedDateTime,
    settings: Settings,
    color: Color,
    modifier: Modifier = Modifier,
    clock: @Composable () -> Unit,
) {
    if (landscape) {
        Row(modifier, verticalAlignment = Alignment.CenterVertically) {
            Box(Modifier.weight(1f).fillMaxHeight(), contentAlignment = Alignment.Center) { clock() }
            Spacer(Modifier.width(24.dp))
            CalendarPane(now, settings, color, Modifier.weight(1f).fillMaxHeight())
        }
    } else {
        Column(modifier, horizontalAlignment = Alignment.CenterHorizontally) {
            Box(Modifier.fillMaxWidth().padding(vertical = 24.dp), contentAlignment = Alignment.Center) { clock() }
            CalendarPane(now, settings, color, Modifier.weight(1f).fillMaxWidth())
        }
    }
}

@Composable
private fun CalendarPane(now: ZonedDateTime, settings: Settings, color: Color, modifier: Modifier) {
    val today = now.toLocalDate()
    val locale = currentLocale()
    val firstDay = WeekFields.of(locale).firstDayOfWeek
    val weeks = remember(today.year, today.monthValue, firstDay) { monthGrid(YearMonth.from(today), firstDay) }

    BoxWithConstraints(modifier, contentAlignment = Alignment.Center) {
        // Title + weekday row + up to six weeks; leave the rest of the height to the events.
        val share = if (settings.showCalendarEvents) 0.62f else 0.9f
        val cell = minOf(maxWidth / 7, maxHeight * share / (weeks.size * ROW + 2), 64.dp)
        val density = LocalDensity.current
        val dayFont = with(density) { (cell * 0.4f).toSp() }

        Column(Modifier.width(cell * 7).fillMaxHeight(), verticalArrangement = Arrangement.Center) {
            MonthView(today, weeks, color, cell, dayFont)
            if (settings.showCalendarEvents) {
                Spacer(Modifier.height(cell * 0.4f))
                val events by rememberCalendarEvents(true, today, now.zone)
                Agenda(now, events, settings.use24h, color, dayFont, cell * 0.25f, Modifier.weight(1f, fill = false))
            }
        }
    }
}

@Composable
private fun MonthView(today: LocalDate, weeks: List<List<LocalDate>>, color: Color, cell: Dp, fontSize: TextUnit) {
    val locale = currentLocale()
    val title = remember(today.year, today.monthValue, locale) {
        today.format(DateTimeFormatter.ofPattern(DateFormat.getBestDateTimePattern(locale, "MMMMyyyy"), locale))
    }
    val base = TextStyle(color = color, fontSize = fontSize, fontFeatureSettings = "tnum")

    Text(
        title.replaceFirstChar { it.titlecase(locale) },
        style = base.copy(fontSize = fontSize * 1.2f, fontWeight = FontWeight.Medium),
        modifier = Modifier.padding(start = cell * 0.15f, bottom = cell * 0.15f),
    )
    Row {
        weeks.first().forEach { d ->
            Box(Modifier.size(cell, cell * 0.8f), contentAlignment = Alignment.Center) {
                Text(
                    d.dayOfWeek.getDisplayName(java.time.format.TextStyle.NARROW, locale),
                    style = base.copy(color = color.copy(alpha = 0.5f), fontSize = fontSize * 0.85f),
                )
            }
        }
    }
    weeks.forEach { week ->
        Row {
            week.forEach { d ->
                val isToday = d == today
                Box(Modifier.size(cell, cell * ROW), contentAlignment = Alignment.Center) {
                    if (isToday) Box(Modifier.size(cell * ROW * 0.95f).clip(CircleShape).background(color))
                    Text(
                        d.dayOfMonth.toString(),
                        style = when {
                            isToday -> base.copy(color = Color.Black, fontWeight = FontWeight.Bold)
                            d.month != today.month -> base.copy(color = color.copy(alpha = 0.25f))
                            else -> base.copy(color = color.copy(alpha = 0.85f))
                        },
                    )
                }
            }
        }
    }
}

@Composable
private fun Agenda(
    now: ZonedDateTime,
    events: List<CalendarEvent>?,
    use24h: Boolean,
    color: Color,
    fontSize: TextUnit,
    gap: Dp,
    modifier: Modifier,
) {
    val context = LocalContext.current
    val locale = currentLocale()
    val access = remember(events) { CalendarEvents.hasPermission(context) }
    val faint = TextStyle(color = color.copy(alpha = 0.5f), fontSize = fontSize * 0.85f)
    val instant = now.toInstant()
    // Minute resolution is enough to drop finished events; avoids refiltering every second.
    val minute = now.withSecond(0).withNano(0)
    val shown = remember(events, minute) { events?.let { eventsForDay(it, now.toLocalDate(), now.zone, minute.toInstant()) } }
    val time = remember(use24h, locale) {
        DateTimeFormatter.ofPattern(DateFormat.getBestDateTimePattern(locale, if (use24h) "Hm" else "hma"), locale)
    }
    fun fmt(millis: Long) = java.time.Instant.ofEpochMilli(millis).atZone(now.zone).format(time)

    when {
        !access -> Text("Allow calendar access in Settings to see today's events", style = faint, modifier = modifier)
        shown == null -> Unit // still loading
        shown.isEmpty() -> Text("Nothing else on your calendar today", style = faint, modifier = modifier)
        else -> FitColumn(gap, modifier, more = { n -> Text("+$n more today", style = faint) }) {
            shown.forEach { e ->
                val ongoing = e.isOngoing(instant)
                val whenText = when {
                    e.allDay -> "All day"
                    ongoing -> "Now · until ${fmt(e.end)}"
                    else -> "${fmt(e.begin)} – ${fmt(e.end)}"
                }
                EventRow(e.title, whenText, e.color?.let { Color(it) } ?: color, color, fontSize, emphasised = ongoing)
            }
        }
    }
}

@Composable
private fun EventRow(title: String, whenText: String, accent: Color, color: Color, fontSize: TextUnit, emphasised: Boolean) {
    val density = LocalDensity.current
    val line = with(density) { fontSize.toDp() }
    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
        Box(Modifier.size(width = 4.dp, height = line * 2.3f).clip(RoundedCornerShape(2.dp)).background(accent))
        Spacer(Modifier.width(line * 0.6f))
        Column {
            Text(
                whenText,
                style = TextStyle(color = color.copy(alpha = if (emphasised) 0.95f else 0.6f), fontSize = fontSize * 0.8f, fontFeatureSettings = "tnum"),
                maxLines = 1,
            )
            Text(
                title,
                style = TextStyle(color = color, fontSize = fontSize, fontWeight = if (emphasised) FontWeight.Medium else FontWeight.Normal),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
    }
}

/** Height of a week row, as a share of a day cell's width: a little flatter than square. */
private const val ROW = 0.82f

/**
 * A column that shows only the children that fit whole, so a long day never shows half an event;
 * when some are left out, [more] says how many.
 */
@Composable
private fun FitColumn(gap: Dp, modifier: Modifier, more: @Composable (Int) -> Unit, content: @Composable () -> Unit) {
    SubcomposeLayout(modifier) { constraints ->
        val loose = constraints.copy(minWidth = 0, minHeight = 0)
        val gapPx = gap.roundToPx()
        val rows = subcompose("rows", content).map { it.measure(loose) }
        val moreHeight = subcompose("probe") { more(rows.size) }.maxOfOrNull { it.measure(loose).height } ?: 0
        val count = rowsThatFit(rows.map { it.height }, gapPx, constraints.maxHeight, moreHeight)
        val label = if (count < rows.size) subcompose("more") { more(rows.size - count) }.map { it.measure(loose) } else emptyList()
        val shown = rows.take(count) + label
        val height = shown.sumOf { it.height } + gapPx * (shown.size - 1).coerceAtLeast(0)
        layout(constraints.constrainWidth(shown.maxOfOrNull { it.width } ?: 0), constraints.constrainHeight(height)) {
            var y = 0
            shown.forEach { it.place(0, y); y += it.height + gapPx }
        }
    }
}
