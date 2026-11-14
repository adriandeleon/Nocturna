package com.adl.nocturna.day

import android.text.format.DateFormat
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.SubcomposeLayout
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.constrainHeight
import androidx.compose.ui.unit.constrainWidth
import androidx.compose.ui.unit.dp
import com.adl.nocturna.data.Settings
import com.adl.nocturna.ui.currentLocale
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.YearMonth
import java.time.ZonedDateTime
import java.time.format.DateTimeFormatter
import java.time.temporal.WeekFields

private val Paper = Color(0xFFFFFCF5)
private val Ink = Color(0xFF3B3A36)
private val NoteYellow = Color(0xFFFFF09A)
private val NoteInk = Color(0xFF4A3F1F)

/**
 * Day mode's background: the photo the user picked ([photoVersion] > 0), or else this month's
 * illustrated scene. Falls back to the scene when the photo can't be read.
 */
@Composable
fun DayBackground(now: ZonedDateTime, photoVersion: Long, modifier: Modifier = Modifier) {
    val context = LocalContext.current
    val photo by produceState<ImageBitmap?>(null, photoVersion) {
        value = if (photoVersion == 0L) null else DayPhoto.load(context)
    }
    Box(modifier) {
        val p = photo
        if (p != null) {
            Image(p, contentDescription = null, contentScale = ContentScale.Crop, modifier = Modifier.fillMaxSize())
            // Shade the top and bottom a little so the white clock and the page stand out on any photo.
            Box(
                Modifier.fillMaxSize().background(
                    Brush.verticalGradient(0f to Color.Black.copy(alpha = 0.35f), 0.45f to Color.Transparent, 1f to Color.Black.copy(alpha = 0.25f))
                )
            )
        } else {
            SceneBackground(monthTheme(now.month), now.toLocalTime(), seed = now.year * 12 + now.monthValue, modifier = Modifier.fillMaxSize())
        }
    }
}

/**
 * Day mode: the clock over a cheerful background, beside a desk-calendar page for this month and a
 * sticky note with the rest of today's events. Side by side in landscape, stacked in portrait.
 * [clock] draws the clock and its info line.
 */
@Composable
fun DayLayout(
    landscape: Boolean,
    now: ZonedDateTime,
    settings: Settings,
    modifier: Modifier = Modifier,
    clock: @Composable () -> Unit,
) {
    val today = now.toLocalDate()
    val locale = currentLocale()
    val firstDay = WeekFields.of(locale).firstDayOfWeek
    val weeks = remember(today.year, today.monthValue, firstDay) { monthGrid(YearMonth.from(today), firstDay) }
    val events by rememberCalendarEvents(settings.showCalendarEvents, weeks.first().first(), weeks.last().last(), now.zone)
    val dots = remember(events, weeks, now.zone) { events?.let { daysWithEvents(it, weeks.flatten(), now.zone) } ?: emptySet() }
    val theme = monthTheme(now.month)

    BoxWithConstraints(modifier) {
        val noteFont = with(LocalDensity.current) { (minOf(maxWidth, maxHeight) * 0.042f).coerceIn(14.dp, 22.dp).toSp() }
        if (landscape) {
            Row(Modifier.fillMaxSize(), verticalAlignment = Alignment.CenterVertically) {
                Column(
                    Modifier.weight(1f).fillMaxHeight(),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(20.dp, Alignment.CenterVertically),
                ) {
                    ClockPanel(clock)
                    if (settings.showCalendarEvents) {
                        StickyNote(now, events, settings.use24h, theme, noteFont, Modifier.widthIn(max = 380.dp).weight(1f, fill = false))
                    }
                }
                Spacer(Modifier.width(24.dp))
                DeskPage(today, weeks, dots, theme, Modifier.weight(1f).fillMaxHeight())
            }
        } else {
            Column(Modifier.fillMaxSize(), horizontalAlignment = Alignment.CenterHorizontally) {
                Box(Modifier.padding(vertical = 16.dp)) { ClockPanel(clock) }
                DeskPage(today, weeks, dots, theme, Modifier.fillMaxWidth().weight(1.6f))
                if (settings.showCalendarEvents) {
                    Spacer(Modifier.height(20.dp))
                    StickyNote(now, events, settings.use24h, theme, noteFont, Modifier.fillMaxWidth(0.9f).weight(1f, fill = false))
                }
            }
        }
    }
}

/** The clock sits in a navy desk clock, solid so clouds or a busy photo never wash out its digits. */
@Composable
private fun ClockPanel(clock: @Composable () -> Unit) {
    val shape = RoundedCornerShape(28.dp)
    Box(
        Modifier
            .shadow(10.dp, shape)
            .clip(shape)
            .background(Brush.verticalGradient(listOf(Color(0xFF2B3A55), Color(0xFF1C263A))))
            .padding(horizontal = 28.dp, vertical = 12.dp)
    ) { clock() }
}

/** A page of a spiral-bound desk calendar: a header in the month's colour and the month's days. */
@Composable
private fun DeskPage(today: LocalDate, weeks: List<List<LocalDate>>, dots: Set<LocalDate>, theme: MonthTheme, modifier: Modifier) {
    val locale = currentLocale()
    val month = remember(today.year, today.monthValue, locale) {
        today.format(DateTimeFormatter.ofPattern("LLLL", locale)).replaceFirstChar { it.titlecase(locale) }
    }
    BoxWithConstraints(modifier, contentAlignment = Alignment.Center) {
        // Heights in cells: rings, header, weekday row, the weeks and the bottom margin.
        val cell = minOf(maxWidth / PAGE_WIDTH, maxHeight / (0.3f + 1.6f + 0.7f + weeks.size * ROW + 0.4f), 72.dp)
        val font = with(LocalDensity.current) { (cell * 0.4f).toSp() }
        val shape = RoundedCornerShape(cell * 0.28f)
        val base = TextStyle(color = Ink, fontSize = font, fontFeatureSettings = "tnum")

        Box(Modifier.width(cell * PAGE_WIDTH)) {
            Column(
                Modifier
                    .padding(top = cell * 0.3f)
                    .shadow(cell * 0.3f, shape)
                    .clip(shape)
                    .background(Paper)
                    .fillMaxWidth()
            ) {
                Row(
                    Modifier
                        .fillMaxWidth()
                        .background(Brush.verticalGradient(listOf(theme.accent, theme.accent.copy(alpha = 0.85f))))
                        .padding(start = cell * 0.45f, end = cell * 0.45f, top = cell * 0.55f, bottom = cell * 0.25f),
                    verticalAlignment = Alignment.Bottom,
                ) {
                    Text(month, style = base.copy(color = Color.White, fontSize = font * 1.7f, fontWeight = FontWeight.Bold))
                    Spacer(Modifier.weight(1f))
                    Text(today.year.toString(), style = base.copy(color = Color.White.copy(alpha = 0.85f), fontSize = font * 1.2f))
                }
                Column(Modifier.padding(horizontal = cell * 0.3f).padding(top = cell * 0.1f, bottom = cell * 0.3f)) {
                    Row {
                        weeks.first().forEach { d ->
                            Box(Modifier.size(cell, cell * 0.7f), contentAlignment = Alignment.Center) {
                                Text(
                                    d.dayOfWeek.getDisplayName(java.time.format.TextStyle.NARROW, locale),
                                    style = base.copy(
                                        color = if (d.dayOfWeek in WEEKEND) theme.accent else Ink.copy(alpha = 0.5f),
                                        fontSize = font * 0.8f,
                                        fontWeight = FontWeight.Bold,
                                    ),
                                )
                            }
                        }
                    }
                    weeks.forEach { week ->
                        Row {
                            week.forEach { d -> DayCell(d, today, d in dots, theme.accent, cell, base) }
                        }
                    }
                }
            }
            Rings(Modifier.width(cell * PAGE_WIDTH).height(cell * 0.75f), cell)
        }
    }
}

@Composable
private fun DayCell(day: LocalDate, today: LocalDate, hasEvents: Boolean, accent: Color, cell: Dp, base: TextStyle) {
    val isToday = day == today
    val inMonth = day.month == today.month
    Box(Modifier.size(cell, cell * ROW), contentAlignment = Alignment.Center) {
        if (isToday) Box(Modifier.size(cell * ROW * 0.95f).shadow(3.dp, CircleShape).clip(CircleShape).background(accent))
        Text(
            day.dayOfMonth.toString(),
            style = when {
                isToday -> base.copy(color = Color.White, fontWeight = FontWeight.Bold)
                !inMonth -> base.copy(color = Ink.copy(alpha = 0.2f))
                day.dayOfWeek in WEEKEND -> base.copy(color = accent)
                else -> base
            },
        )
        if (hasEvents && inMonth) {
            Box(
                Modifier
                    .align(Alignment.BottomCenter)
                    .padding(bottom = cell * 0.06f)
                    .size(cell * 0.1f)
                    .clip(CircleShape)
                    .background(if (isToday) Color.White else accent)
            )
        }
    }
}

/** The spiral binding across the top of the page. */
@Composable
private fun Rings(modifier: Modifier, cell: Dp) {
    Canvas(modifier) {
        val c = cell.toPx()
        val count = 12
        val gap = size.width / (count + 1)
        val holeY = c * 0.55f
        for (i in 1..count) {
            val x = gap * i
            drawCircle(Color(0xFF2B2B2B).copy(alpha = 0.55f), c * 0.07f, Offset(x, holeY))
            drawRoundRect(
                Brush.horizontalGradient(listOf(Color(0xFF9A9DA4), Color(0xFFE6E8EC), Color(0xFF8A8D94)), x - c * 0.07f, x + c * 0.07f),
                Offset(x - c * 0.07f, 0f), Size(c * 0.14f, holeY),
                CornerRadius(c * 0.07f),
                style = Stroke(c * 0.045f),
            )
        }
    }
}

/** A yellow sticky note with a hello and the rest of today's events. */
@Composable
private fun StickyNote(
    now: ZonedDateTime,
    events: List<CalendarEvent>?,
    use24h: Boolean,
    theme: MonthTheme,
    fontSize: TextUnit,
    modifier: Modifier,
) {
    Box(modifier.rotate(-1.5f)) {
        Column(
            Modifier
                .padding(top = 8.dp)
                .shadow(6.dp, RoundedCornerShape(2.dp))
                .background(NoteYellow)
                .padding(horizontal = 18.dp, vertical = 14.dp)
        ) {
            Text(
                greeting(now.hour),
                style = TextStyle(color = NoteInk, fontSize = fontSize * 1.6f, fontFamily = FontFamily.Cursive, fontWeight = FontWeight.Bold),
            )
            Spacer(Modifier.height(6.dp))
            Agenda(now, events, use24h, theme.accent, fontSize, Modifier.weight(1f, fill = false))
        }
        // A strip of tape holding the note up.
        Box(
            Modifier
                .align(Alignment.TopCenter)
                .rotate(4f)
                .size(80.dp, 22.dp)
                .background(Color.White.copy(alpha = 0.6f))
        )
    }
}

@Composable
private fun Agenda(now: ZonedDateTime, events: List<CalendarEvent>?, use24h: Boolean, accent: Color, fontSize: TextUnit, modifier: Modifier) {
    val context = LocalContext.current
    val locale = currentLocale()
    val access = remember(events) { CalendarEvents.hasPermission(context) }
    val faint = TextStyle(color = NoteInk.copy(alpha = 0.7f), fontSize = fontSize * 0.9f)
    val instant = now.toInstant()
    // Minute resolution is enough to drop finished events; avoids refiltering every second.
    val minute = now.withSecond(0).withNano(0)
    val shown = remember(events, minute) { events?.let { eventsForDay(it, now.toLocalDate(), now.zone, minute.toInstant()) } }
    val time = remember(use24h, locale) {
        DateTimeFormatter.ofPattern(DateFormat.getBestDateTimePattern(locale, if (use24h) "Hm" else "hma"), locale)
    }
    fun fmt(millis: Long) = java.time.Instant.ofEpochMilli(millis).atZone(now.zone).format(time)

    when {
        !access -> Text("Allow calendar access in Settings to see today's events here", style = faint, modifier = modifier)
        shown == null -> Unit // still loading
        shown.isEmpty() -> Text("Nothing else on your calendar today. Enjoy!", style = faint, modifier = modifier)
        else -> FitColumn(8.dp, modifier, more = { n -> Text("+$n more today", style = faint) }) {
            shown.forEach { e ->
                val ongoing = e.isOngoing(instant)
                val whenText = when {
                    e.allDay -> "All day"
                    ongoing -> "Now · until ${fmt(e.end)}"
                    else -> "${fmt(e.begin)} – ${fmt(e.end)}"
                }
                EventRow(e.title, whenText, e.color?.let { Color(it) } ?: accent, fontSize, emphasised = ongoing)
            }
        }
    }
}

@Composable
private fun EventRow(title: String, whenText: String, dot: Color, fontSize: TextUnit, emphasised: Boolean) {
    val line = with(LocalDensity.current) { fontSize.toDp() }
    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
        Box(Modifier.size(line * 0.7f).clip(CircleShape).background(dot))
        Spacer(Modifier.width(line * 0.6f))
        Column {
            Text(
                whenText,
                style = TextStyle(
                    color = NoteInk.copy(alpha = if (emphasised) 1f else 0.65f),
                    fontSize = fontSize * 0.8f,
                    fontWeight = if (emphasised) FontWeight.Bold else FontWeight.Normal,
                    fontFeatureSettings = "tnum",
                ),
                maxLines = 1,
            )
            Text(
                title,
                style = TextStyle(color = NoteInk, fontSize = fontSize, fontWeight = if (emphasised) FontWeight.Bold else FontWeight.Medium),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
    }
}

private val WEEKEND = setOf(DayOfWeek.SATURDAY, DayOfWeek.SUNDAY)

/** The page is seven day cells wide plus its side margins. */
private const val PAGE_WIDTH = 7.6f

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
