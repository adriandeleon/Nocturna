package com.adl.nocturna.day

import android.content.Intent
import android.text.format.DateFormat
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.Crossfade
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.AnimationVector1D
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.produceState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.lerp
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
import com.adl.nocturna.data.CalendarView
import com.adl.nocturna.data.Settings
import com.adl.nocturna.ui.currentLocale
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.YearMonth
import java.time.ZonedDateTime
import java.time.format.DateTimeFormatter
import java.time.temporal.WeekFields
import kotlin.math.abs
import kotlin.random.Random
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

// Warm and a touch below white: kinder to OLED pixels lit all day, and it still reads as paper.
private val Paper = Color(0xFFF8F2E4)
// Where the paper dims to after hours on screen (see paperDim): about a quarter less light, still paper.
private val DimPaper = Color(0xFFDDD3BE)
private val Ink = Color(0xFF3B3A36)
private val NoteYellow = Color(0xFFF8E68C)
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
            // A slow pan, so no part of the photo stays on the same pixels all day.
            val (panX, panY) = photoPan(now.toLocalTime().toSecondOfDay().toFloat())
            Image(
                p,
                contentDescription = null,
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize().graphicsLayer {
                    scaleX = PHOTO_SCALE
                    scaleY = PHOTO_SCALE
                    translationX = panX * size.width
                    translationY = panY * size.height
                },
            )
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
    /** Opens the calendar app; null where nothing can be tapped (the screen saver). */
    onOpen: ((Intent) -> Unit)? = null,
    clock: @Composable () -> Unit,
) {
    val today = now.toLocalDate()
    val locale = currentLocale()
    val firstDay = WeekFields.of(locale).firstDayOfWeek
    val weeks = remember(today.year, today.monthValue, firstDay) { monthGrid(YearMonth.from(today), firstDay) }
    val events by rememberCalendarEvents(settings.showCalendarEvents, weeks.first().first(), weeks.last().last(), now.zone)
    val dots = remember(events, weeks, now.zone) { events?.let { daysWithEvents(it, weeks.flatten(), now.zone) } ?: emptySet() }
    val theme = monthTheme(now.month)
    val openDay: ((LocalDate) -> Unit)? = onOpen?.let { open -> { day -> open(OpenCalendar.day(day, now)) } }
    val openEvent: ((CalendarEvent) -> Unit)? = onOpen?.let { open -> { event -> open(OpenCalendar.event(event)) } }

    // The page starts on the view chosen in Settings; a swipe (or the switch under the page)
    // changes it for as long as the clock stays open. The screen saver can't be swiped.
    var view by rememberSaveable(settings.dayCalendarView) { mutableStateOf(settings.dayCalendarView) }
    val onView: ((CalendarView) -> Unit)? = if (onOpen != null) { v -> view = v } else null
    // The day view lists the whole day on the page itself, so the sticky note would only repeat it.
    val showNote = settings.showCalendarEvents && view != CalendarView.DAY
    // Burn-in protection: besides the whole layout's drift, the clock, the page and the note each
    // drift on their own, at different moments, so their edges don't keep the same distance all day.
    // Kept out here so swapping sides doesn't send them back to the middle.
    val burnIn = settings.burnInProtection
    val clockDrift = rememberDrift(burnIn, phase = 0)
    val pageDrift = rememberDrift(burnIn, phase = 1)
    val noteDrift = rememberDrift(burnIn, phase = 2)
    // The page is the brightest thing on screen, so its paper slowly dims the longer it stays up.
    val shownSince = rememberSaveable { now.toInstant().toEpochMilli() }
    val minutesShown = (now.toInstant().toEpochMilli() - shownSince) / 60_000f
    val paper = if (burnIn) lerp(Paper, DimPaper, paperDim(minutesShown)) else Paper
    val page: @Composable (Modifier) -> Unit = { m ->
        DeskPage(view, now, weeks, dots, events, settings.showCalendarEvents, settings.use24h, theme, paper, openDay, openEvent, onView, m)
    }

    BoxWithConstraints(modifier) {
        val noteFont = with(LocalDensity.current) { (minOf(maxWidth, maxHeight) * 0.042f).coerceIn(14.dp, 22.dp).toSp() }
        if (landscape) {
            // Every other hour the clock and the page trade places, so the bright page and the dark
            // clock don't sit on the same pixels all day.
            Crossfade(sidesSwapped(now.hour, settings.burnInProtection), animationSpec = tween(1500), label = "sides") { swapped ->
                Row(Modifier.fillMaxSize(), verticalAlignment = Alignment.CenterVertically) {
                    val clockSide = @Composable {
                        Column(
                            Modifier.weight(1f).fillMaxHeight(),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(32.dp, Alignment.CenterVertically),
                        ) {
                            ClockPanel(clock, Modifier.drift(clockDrift, 28.dp, 14.dp))
                            if (showNote) {
                                StickyNote(
                                    now, events, settings.use24h, theme, noteFont, openDay, openEvent,
                                    Modifier.widthIn(max = 380.dp).weight(1f, fill = false).drift(noteDrift, 28.dp, 14.dp),
                                )
                            }
                        }
                    }
                    val pageSide = @Composable { page(Modifier.weight(1f).fillMaxHeight().drift(pageDrift, 20.dp, 16.dp)) }
                    if (swapped) pageSide() else clockSide()
                    Spacer(Modifier.width(40.dp))
                    if (swapped) clockSide() else pageSide()
                }
            }
        } else {
            // Every other hour the clock moves from the top to the bottom, so the page and the note
            // move up by its height and nothing keeps the same rows of pixels all day.
            Crossfade(sidesSwapped(now.hour, settings.burnInProtection), animationSpec = tween(1500), label = "ends") { swapped ->
                Column(Modifier.fillMaxSize(), horizontalAlignment = Alignment.CenterHorizontally) {
                    val clockEnd = @Composable {
                        Box(Modifier.padding(vertical = 16.dp)) { ClockPanel(clock, Modifier.drift(clockDrift, 24.dp, 8.dp)) }
                    }
                    if (!swapped) clockEnd()
                    page(Modifier.fillMaxWidth().weight(1.6f).drift(pageDrift, 16.dp, 8.dp))
                    if (showNote) {
                        Spacer(Modifier.height(20.dp))
                        StickyNote(
                            now, events, settings.use24h, theme, noteFont, openDay, openEvent,
                            Modifier.fillMaxWidth(0.9f).weight(1f, fill = false).drift(noteDrift, 20.dp, 8.dp),
                        )
                    }
                    if (swapped) clockEnd()
                }
            }
        }
    }
}

/** The clock sits in a navy desk clock, solid so clouds or a busy photo never wash out its digits. */
@Composable
private fun ClockPanel(clock: @Composable () -> Unit, modifier: Modifier = Modifier) {
    val shape = RoundedCornerShape(28.dp)
    Box(
        modifier
            .shadow(10.dp, shape)
            .clip(shape)
            .background(Brush.verticalGradient(listOf(Color(0xFF2B3A55), Color(0xFF1C263A))))
            .padding(horizontal = 28.dp, vertical = 12.dp)
    ) { clock() }
}

/**
 * Where one panel of day mode has drifted to, as fractions (-1..1) of its range on each axis. Like the
 * night clock's drift, it glides to a new random spot every couple of minutes; [phase] staggers the
 * panels so they move at different moments. Stays in the middle while [enabled] is false.
 */
@Stable
private class Drift(val x: Animatable<Float, AnimationVector1D>, val y: Animatable<Float, AnimationVector1D>)

@Composable
private fun rememberDrift(enabled: Boolean, phase: Int): Drift {
    val drift = remember { Drift(Animatable(0f), Animatable(0f)) }
    LaunchedEffect(enabled) {
        if (!enabled) {
            launch { drift.x.animateTo(0f, tween(DRIFT_GLIDE_MS)) }
            launch { drift.y.animateTo(0f, tween(DRIFT_GLIDE_MS)) }
            return@LaunchedEffect
        }
        delay(DRIFT_EVERY_MS / 3 * phase)
        while (true) {
            launch { drift.x.animateTo(Random.nextFloat() * 2 - 1, tween(DRIFT_GLIDE_MS)) }
            launch { drift.y.animateTo(Random.nextFloat() * 2 - 1, tween(DRIFT_GLIDE_MS)) }
            delay(DRIFT_EVERY_MS)
        }
    }
    return drift
}

private const val DRIFT_EVERY_MS = 120_000L
private const val DRIFT_GLIDE_MS = 20_000

/** Moves this panel [drift] of the way to [rangeX] and [rangeY] off its place; taps follow it. */
private fun Modifier.drift(drift: Drift, rangeX: Dp, rangeY: Dp): Modifier = graphicsLayer {
    translationX = drift.x.value * rangeX.toPx()
    translationY = drift.y.value * rangeY.toPx()
}

/**
 * A page of a spiral-bound desk calendar: a header in the month's colour, then the month, this week
 * or today, and a switch between them. Swiping the page sideways moves between the views too.
 */
@Composable
private fun DeskPage(
    view: CalendarView,
    now: ZonedDateTime,
    weeks: List<List<LocalDate>>,
    dots: Set<LocalDate>,
    events: List<CalendarEvent>?,
    showEvents: Boolean,
    use24h: Boolean,
    theme: MonthTheme,
    paper: Color,
    onOpenDay: ((LocalDate) -> Unit)?,
    onOpenEvent: ((CalendarEvent) -> Unit)?,
    onView: ((CalendarView) -> Unit)?,
    modifier: Modifier,
) {
    val today = now.toLocalDate()
    val locale = currentLocale()
    val weekFields = WeekFields.of(locale)
    val month = remember(today.year, today.monthValue, locale) {
        today.format(DateTimeFormatter.ofPattern("LLLL", locale)).replaceFirstChar { it.titlecase(locale) }
    }
    val currentView by rememberUpdatedState(view)
    val currentOnView by rememberUpdatedState(onView)
    // Swipe left for more detail (month, week, day), right to go back.
    val swipe = if (onView == null) Modifier else Modifier.pointerInput(Unit) {
        val threshold = 48.dp.toPx()
        var total = 0f
        detectHorizontalDragGestures(
            onDragStart = { total = 0f },
            onDragEnd = {
                if (abs(total) > threshold) currentOnView?.invoke(swipeView(currentView, towardsDetail = total < 0))
            },
        ) { change, dx ->
            change.consume()
            total += dx
        }
    }

    BoxWithConstraints(modifier, contentAlignment = Alignment.Center) {
        // Heights in cells: rings, header, the body (as tall as the month's weekday row and weeks, so
        // the page keeps its size in every view), the view switch and the bottom margin.
        val bodyCells = 0.7f + weeks.size * ROW
        val cell = minOf(maxWidth / PAGE_WIDTH, maxHeight / (0.3f + 1.6f + 0.1f + bodyCells + SWITCH + 0.15f), 72.dp)
        val font = with(LocalDensity.current) { (cell * 0.4f).toSp() }
        val shape = RoundedCornerShape(cell * 0.28f)
        val base = TextStyle(color = Ink, fontSize = font, fontFeatureSettings = "tnum")

        Box(Modifier.width(cell * PAGE_WIDTH)) {
            Column(
                Modifier
                    .padding(top = cell * 0.3f)
                    .shadow(cell * 0.3f, shape)
                    .clip(shape)
                    .background(paper)
                    .fillMaxWidth()
                    .then(swipe)
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
                    Text(
                        if (view == CalendarView.WEEK) "Week ${today.get(weekFields.weekOfWeekBasedYear())}" else today.year.toString(),
                        style = base.copy(color = Color.White.copy(alpha = 0.85f), fontSize = font * 1.2f),
                    )
                }
                AnimatedContent(
                    view,
                    transitionSpec = {
                        val dir = if (targetState.ordinal > initialState.ordinal) 1 else -1
                        (slideInHorizontally { it * dir / 3 } + fadeIn()) togetherWith (slideOutHorizontally { -it * dir / 3 } + fadeOut())
                    },
                    label = "view",
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = cell * 0.3f)
                        .padding(top = cell * 0.1f)
                        .height(cell * bodyCells),
                ) { v ->
                    when (v) {
                        CalendarView.MONTH -> MonthBody(today, weeks, dots, theme.accent, cell, base, onOpenDay)
                        CalendarView.WEEK ->
                            WeekBody(now, weekOf(today, weekFields.firstDayOfWeek), events, showEvents, use24h, theme.accent, cell, base, onOpenDay, onOpenEvent)
                        CalendarView.DAY -> DayBody(now, events, showEvents, use24h, theme.accent, cell, base, onOpenDay, onOpenEvent)
                    }
                }
                ViewSwitch(view, theme.accent, cell, base, onView)
            }
            Rings(Modifier.width(cell * PAGE_WIDTH).height(cell * 0.75f), cell)
        }
    }
}

@Composable
private fun MonthBody(
    today: LocalDate,
    weeks: List<List<LocalDate>>,
    dots: Set<LocalDate>,
    accent: Color,
    cell: Dp,
    base: TextStyle,
    onOpenDay: ((LocalDate) -> Unit)?,
) {
    val locale = currentLocale()
    Column {
        Row {
            weeks.first().forEach { d ->
                Box(Modifier.size(cell, cell * 0.7f), contentAlignment = Alignment.Center) {
                    Text(
                        d.dayOfWeek.getDisplayName(java.time.format.TextStyle.NARROW, locale),
                        style = base.copy(
                            color = if (d.dayOfWeek in WEEKEND) accent else Ink.copy(alpha = 0.5f),
                            fontSize = base.fontSize * 0.8f,
                            fontWeight = FontWeight.Bold,
                        ),
                    )
                }
            }
        }
        weeks.forEach { week ->
            Row {
                week.forEach { d -> DayCell(d, today, d in dots, accent, cell, base, onOpenDay) }
            }
        }
    }
}

/** This week, one row per day: the date and as many of its events as fit, finished ones faded. */
@Composable
private fun WeekBody(
    now: ZonedDateTime,
    days: List<LocalDate>,
    events: List<CalendarEvent>?,
    showEvents: Boolean,
    use24h: Boolean,
    accent: Color,
    cell: Dp,
    base: TextStyle,
    onOpenDay: ((LocalDate) -> Unit)?,
    onOpenEvent: ((CalendarEvent) -> Unit)?,
) {
    val locale = currentLocale()
    val today = now.toLocalDate()
    val time = rememberTimeFormat(use24h)
    val eventFont = base.fontSize * 0.72f
    BoxWithConstraints(Modifier.fillMaxWidth()) {
        val lineHeight = with(LocalDensity.current) { (eventFont * 1.4f).toDp() }
        val lines = ((maxHeight / 7) / lineHeight).toInt().coerceIn(1, 3)
        Column(Modifier.fillMaxSize()) {
            days.forEach { d ->
                val isToday = d == today
                val dayEvents = if (showEvents) events?.let { eventsOnDay(it, d, now.zone) }.orEmpty() else emptyList()
                Row(
                    Modifier
                        .fillMaxWidth()
                        .weight(1f)
                        .clip(RoundedCornerShape(cell * 0.15f))
                        .background(if (isToday) accent.copy(alpha = 0.12f) else Color.Transparent)
                        .clickable(enabled = onOpenDay != null, onClickLabel = "Open in your calendar") { onOpenDay?.invoke(d) }
                        .padding(horizontal = cell * 0.15f),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Row(Modifier.width(cell * 1.9f), verticalAlignment = Alignment.Bottom) {
                        Text(
                            d.dayOfWeek.getDisplayName(java.time.format.TextStyle.SHORT, locale),
                            style = base.copy(
                                fontSize = base.fontSize * 0.62f,
                                color = if (isToday || d.dayOfWeek in WEEKEND) accent else Ink.copy(alpha = 0.55f),
                                fontWeight = FontWeight.Bold,
                            ),
                            maxLines = 1,
                        )
                        Spacer(Modifier.width(cell * 0.12f))
                        Text(
                            d.dayOfMonth.toString(),
                            style = base.copy(
                                fontSize = base.fontSize * 0.95f,
                                color = if (isToday) accent else Ink,
                                fontWeight = if (isToday) FontWeight.Bold else FontWeight.Medium,
                            ),
                            maxLines = 1,
                        )
                    }
                    Column(Modifier.weight(1f)) {
                        val shown = dayEvents.take(lines)
                        shown.forEachIndexed { i, e ->
                            // No room for a line of its own: the count of the rest goes on the last line.
                            val more = if (i == shown.lastIndex) dayEvents.size - shown.size else 0
                            WeekEventLine(e, now, time, eventFont, accent, more, onOpenEvent)
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun WeekEventLine(
    e: CalendarEvent,
    now: ZonedDateTime,
    time: DateTimeFormatter,
    fontSize: TextUnit,
    accent: Color,
    more: Int,
    onOpenEvent: ((CalendarEvent) -> Unit)?,
) {
    val faded = !e.allDay && e.end <= now.toInstant().toEpochMilli()
    val line = with(LocalDensity.current) { fontSize.toDp() }
    Row(
        Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(4.dp))
            .clickable(enabled = onOpenEvent != null, onClickLabel = "Open in your calendar") { onOpenEvent?.invoke(e) },
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(Modifier.size(line * 0.55f).clip(CircleShape).background((e.color?.let { Color(it) } ?: accent).copy(alpha = if (faded) 0.4f else 1f)))
        Spacer(Modifier.width(line * 0.4f))
        val start = if (e.allDay) "" else java.time.Instant.ofEpochMilli(e.begin).atZone(now.zone).format(time) + "  "
        Text(
            start + e.title,
            style = TextStyle(color = Ink.copy(alpha = if (faded) 0.4f else 0.9f), fontSize = fontSize, fontFeatureSettings = "tnum"),
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.weight(1f, fill = false),
        )
        if (more > 0) {
            Spacer(Modifier.width(line * 0.4f))
            Text("+$more", style = TextStyle(color = accent, fontSize = fontSize, fontWeight = FontWeight.Bold), maxLines = 1)
        }
    }
}

/** Today, like a tear-off calendar: the big date beside the whole day's schedule. */
@Composable
private fun DayBody(
    now: ZonedDateTime,
    events: List<CalendarEvent>?,
    showEvents: Boolean,
    use24h: Boolean,
    accent: Color,
    cell: Dp,
    base: TextStyle,
    onOpenDay: ((LocalDate) -> Unit)?,
    onOpenEvent: ((CalendarEvent) -> Unit)?,
) {
    val context = LocalContext.current
    val locale = currentLocale()
    val today = now.toLocalDate()
    val time = rememberTimeFormat(use24h)
    val access = remember(events) { CalendarEvents.hasPermission(context) }
    val dayEvents = remember(events, today) { events?.let { eventsOnDay(it, today, now.zone) } }
    val faint = TextStyle(color = Ink.copy(alpha = 0.55f), fontSize = base.fontSize * 0.7f)
    val instant = now.toInstant()
    fun fmt(millis: Long) = java.time.Instant.ofEpochMilli(millis).atZone(now.zone).format(time)

    Row(Modifier.fillMaxSize(), verticalAlignment = Alignment.CenterVertically) {
        Column(
            Modifier
                .width(cell * 2.5f)
                .fillMaxHeight()
                .clip(RoundedCornerShape(cell * 0.2f))
                .clickable(enabled = onOpenDay != null, onClickLabel = "Open in your calendar") { onOpenDay?.invoke(today) },
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
        ) {
            Text(
                today.dayOfWeek.getDisplayName(java.time.format.TextStyle.FULL, locale).replaceFirstChar { it.titlecase(locale) },
                style = base.copy(color = accent, fontSize = base.fontSize * 0.75f, fontWeight = FontWeight.Bold),
                maxLines = 1,
            )
            Text(
                today.dayOfMonth.toString(),
                style = base.copy(fontSize = base.fontSize * 4f, fontWeight = FontWeight.Bold, lineHeight = base.fontSize * 4.2f),
                maxLines = 1,
            )
        }
        Box(Modifier.width(1.dp).fillMaxHeight(0.8f).background(Ink.copy(alpha = 0.15f)))
        Spacer(Modifier.width(cell * 0.3f))
        Box(Modifier.weight(1f).fillMaxHeight(), contentAlignment = Alignment.CenterStart) {
            when {
                !showEvents -> Text("Turn on Today's events in Settings to see your day here", style = faint)
                !access -> Text("Allow calendar access in Settings to see your day here", style = faint)
                dayEvents == null -> Unit // still loading
                dayEvents.isEmpty() -> Text("Nothing on your calendar today", style = faint)
                else -> FitColumn(cell * 0.12f, Modifier, more = { n -> Text("+$n more", style = faint) }) {
                    dayEvents.forEach { e ->
                        val ongoing = e.isOngoing(instant)
                        val whenText = when {
                            e.allDay -> "All day"
                            ongoing -> "Now · until ${fmt(e.end)}"
                            else -> "${fmt(e.begin)} – ${fmt(e.end)}"
                        }
                        EventRow(
                            e.title, whenText, e.color?.let { Color(it) } ?: accent, base.fontSize * 0.72f,
                            emphasised = ongoing,
                            onClick = onOpenEvent?.let { open -> { open(e) } },
                            ink = Ink,
                            faded = !e.allDay && e.end <= instant.toEpochMilli(),
                        )
                    }
                }
            }
        }
    }
}

/** "Month · Week · Day" under the page: shows the view, and switches it with a tap. */
@Composable
private fun ViewSwitch(view: CalendarView, accent: Color, cell: Dp, base: TextStyle, onView: ((CalendarView) -> Unit)?) {
    Row(
        Modifier.fillMaxWidth().height(cell * SWITCH),
        horizontalArrangement = Arrangement.spacedBy(cell * 0.15f, Alignment.CenterHorizontally),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        CalendarView.entries.forEach { v ->
            val selected = v == view
            Text(
                v.label(),
                style = base.copy(
                    fontSize = base.fontSize * 0.62f,
                    color = if (selected) accent else Ink.copy(alpha = 0.45f),
                    fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium,
                ),
                modifier = Modifier
                    .clip(RoundedCornerShape(50))
                    .background(if (selected) accent.copy(alpha = 0.12f) else Color.Transparent)
                    .clickable(enabled = onView != null, onClickLabel = "Show the ${v.label().lowercase()}") { onView?.invoke(v) }
                    .padding(horizontal = cell * 0.25f, vertical = cell * 0.06f),
            )
        }
    }
}

@Composable
private fun rememberTimeFormat(use24h: Boolean): DateTimeFormatter {
    val locale = currentLocale()
    return remember(use24h, locale) {
        DateTimeFormatter.ofPattern(DateFormat.getBestDateTimePattern(locale, if (use24h) "Hm" else "hma"), locale)
    }
}

@Composable
private fun DayCell(
    day: LocalDate,
    today: LocalDate,
    hasEvents: Boolean,
    accent: Color,
    cell: Dp,
    base: TextStyle,
    onOpen: ((LocalDate) -> Unit)?,
) {
    val isToday = day == today
    val inMonth = day.month == today.month
    Box(
        Modifier
            .size(cell, cell * ROW)
            .clip(RoundedCornerShape(cell * 0.2f))
            .clickable(enabled = onOpen != null, onClickLabel = "Open in your calendar") { onOpen?.invoke(day) },
        contentAlignment = Alignment.Center,
    ) {
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
    onOpenDay: ((LocalDate) -> Unit)?,
    onOpenEvent: ((CalendarEvent) -> Unit)?,
    modifier: Modifier,
) {
    Box(modifier.rotate(-1.5f)) {
        Column(
            Modifier
                .padding(top = 8.dp)
                .shadow(6.dp, RoundedCornerShape(2.dp))
                .background(NoteYellow)
                // Anywhere on the note but an event opens today; an event opens itself.
                .clickable(enabled = onOpenDay != null, onClickLabel = "Open today in your calendar") { onOpenDay?.invoke(now.toLocalDate()) }
                .padding(horizontal = 18.dp, vertical = 14.dp)
        ) {
            Text(
                greeting(now.hour),
                style = TextStyle(color = NoteInk, fontSize = fontSize * 1.6f, fontFamily = FontFamily.Cursive, fontWeight = FontWeight.Bold),
            )
            Spacer(Modifier.height(6.dp))
            Agenda(now, events, use24h, theme.accent, fontSize, onOpenEvent, Modifier.weight(1f, fill = false))
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
private fun Agenda(
    now: ZonedDateTime,
    events: List<CalendarEvent>?,
    use24h: Boolean,
    accent: Color,
    fontSize: TextUnit,
    onOpenEvent: ((CalendarEvent) -> Unit)?,
    modifier: Modifier,
) {
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
                EventRow(e.title, whenText, e.color?.let { Color(it) } ?: accent, fontSize, emphasised = ongoing, onClick = onOpenEvent?.let { open -> { open(e) } })
            }
        }
    }
}

@Composable
private fun EventRow(
    title: String,
    whenText: String,
    dot: Color,
    fontSize: TextUnit,
    emphasised: Boolean,
    onClick: (() -> Unit)?,
    ink: Color = NoteInk,
    faded: Boolean = false,
) {
    val alpha = if (faded) 0.4f else 1f
    val line = with(LocalDensity.current) { fontSize.toDp() }
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(6.dp))
            .clickable(enabled = onClick != null, onClickLabel = "Open in your calendar") { onClick?.invoke() },
    ) {
        Box(Modifier.size(line * 0.7f).clip(CircleShape).background(dot.copy(alpha = dot.alpha * alpha)))
        Spacer(Modifier.width(line * 0.6f))
        Column {
            Text(
                whenText,
                style = TextStyle(
                    color = ink.copy(alpha = (if (emphasised) 1f else 0.65f) * alpha),
                    fontSize = fontSize * 0.8f,
                    fontWeight = if (emphasised) FontWeight.Bold else FontWeight.Normal,
                    fontFeatureSettings = "tnum",
                ),
                maxLines = 1,
            )
            Text(
                title,
                style = TextStyle(color = ink.copy(alpha = alpha), fontSize = fontSize, fontWeight = if (emphasised) FontWeight.Bold else FontWeight.Medium),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
    }
}

private val WEEKEND = setOf(DayOfWeek.SATURDAY, DayOfWeek.SUNDAY)

/** The page is seven day cells wide plus its side margins. */
private const val PAGE_WIDTH = 7.6f

/** Height of the Month · Week · Day switch under the page, in cells. */
private const val SWITCH = 0.75f

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
