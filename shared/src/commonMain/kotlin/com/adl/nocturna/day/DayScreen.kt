package com.adl.nocturna.day

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.Crossfade
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.AnimationVector1D
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
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
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
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
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.SubcomposeLayout
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.constrainHeight
import androidx.compose.ui.unit.constrainWidth
import androidx.compose.ui.unit.dp
import com.adl.nocturna.clock.LocalClockPlatform
import com.adl.nocturna.clock.Moment
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.adl.nocturna.alarm.DAYS_OFF_LOOK_AHEAD
import com.adl.nocturna.alarm.daysOff
import com.adl.nocturna.alarm.parseDayOffKeywords
import com.adl.nocturna.data.CalendarView
import com.adl.nocturna.data.Settings
import com.adl.nocturna.data.SettingsStore
import com.adl.nocturna.data.TodoItem
import com.adl.nocturna.text.countryCode
import com.adl.nocturna.weather.WeatherRepository
import com.adl.nocturna.resources.*
import com.adl.nocturna.text.DateTimePattern
import com.adl.nocturna.text.bestDateTimePattern
import com.adl.nocturna.text.capitalizedFirst
import com.adl.nocturna.text.firstDayOfWeek
import com.adl.nocturna.text.fullName
import com.adl.nocturna.text.narrowName
import com.adl.nocturna.text.shortName
import com.adl.nocturna.text.weekOfYear
import com.adl.nocturna.ui.currentLocale
import kotlin.math.abs
import kotlin.random.Random
import kotlin.time.Clock
import kotlin.time.Instant
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.datetime.DateTimeUnit
import kotlinx.datetime.DayOfWeek
import kotlinx.datetime.LocalDate
import kotlinx.datetime.YearMonth
import kotlinx.datetime.atTime
import kotlinx.datetime.number
import kotlinx.datetime.plus
import kotlinx.datetime.toLocalDateTime
import org.jetbrains.compose.resources.stringResource

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
fun DayBackground(
    now: Moment,
    photoVersion: Long,
    modifier: Modifier = Modifier,
    /** The weather the illustrated scene shows; null for its own fair weather. */
    weather: SceneWeather? = null,
    /** The holiday whose decorations go over the background, or null. */
    holiday: HolidayDay? = null,
    /** How cold the illustrated scene looks, 0..1 (see [sceneCold]). */
    cold: Float = 0f,
    /** When the illustrated scene's dawn, day, dusk and night are (see [skyDay]). */
    skyDay: SkyDay = FIXED_SKY_DAY,
) {
    val platform = LocalClockPlatform.current
    val photo by produceState<ImageBitmap?>(null, photoVersion) {
        value = if (photoVersion == 0L) null else platform.loadDayPhoto()
    }
    Box(modifier) {
        val p = photo
        if (p != null) {
            // A slow pan, so no part of the photo stays on the same pixels all day.
            val (panX, panY) = photoPan(now.time.toSecondOfDay().toFloat())
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
            SceneBackground(
                monthTheme(now.local.month), now.time, seed = now.local.year * 12 + now.local.month.number,
                modifier = Modifier.fillMaxSize(), weather = weather, holiday = holiday?.holiday, cold = cold, skyDay = skyDay,
                moonPhase = moonPhase(now.instant),
            )
        }
        // Rain and shooting stars go over the illustration only; a holiday's decorations go over a photo too,
        // with fireworks once the sky is dark.
        val night = skyMix(now.time, skyDay).weight(TimeOfDay.NIGHT)
        val meteors = p == null && (weather == null || weather.showsSun()) && meteorNight(now.date, now.time)
        DayOverlay(if (p == null) weather else null, holiday, Modifier.fillMaxSize(), night, meteors)
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
    now: Moment,
    settings: Settings,
    modifier: Modifier = Modifier,
    /** Opens the calendar app; null where nothing can be tapped (the screen saver). */
    onOpen: ((CalendarLink) -> Unit)? = null,
    /** The focus timer, shown in the desk clock while it runs. */
    focus: FocusTimer? = null,
    /** Changes or (with null) stops the focus timer; null where it can't be tapped. */
    onFocus: ((FocusTimer?) -> Unit)? = null,
    /** Draws the clock and its info line. */
    clock: @Composable () -> Unit,
) {
    val today = now.date
    val locale = currentLocale()
    val platform = LocalClockPlatform.current
    val firstDay = firstDayOfWeek(locale)
    val weeks = remember(today.year, today.month, firstDay) { monthGrid(YearMonth(today.year, today.month), firstDay) }
    val calendarEvents = platform.calendarEvents(settings.showCalendarEvents, weeks.first().first(), weeks.last().last(), now.zone)
    // Birthdays get a cake: before their titles, and in place of the month view's dot.
    val events = remember(calendarEvents, settings.dayBirthdays) {
        if (!settings.dayBirthdays) calendarEvents
        else calendarEvents?.map { if (isBirthday(it)) it.copy(title = "$CAKE ${it.title}") else it }
    }
    val dots = remember(events, weeks, now.zone) { events?.let { daysWithEvents(it, weeks.flatten(), now.zone) } ?: emptySet() }
    val cakes = remember(calendarEvents, weeks, now.zone, settings.dayBirthdays) {
        if (!settings.dayBirthdays) emptySet() else calendarEvents?.let { daysWithEvents(it.filter(::isBirthday), weeks.flatten(), now.zone) } ?: emptySet()
    }
    val theme = monthTheme(now.local.month)
    val openDay: ((LocalDate) -> Unit)? = onOpen?.let { open -> { day -> open(CalendarLink.Day(dayOpenTime(day, now.instant, now.zone))) } }
    val openEvent: ((CalendarEvent) -> Unit)? = onOpen?.let { open -> { event -> open(CalendarLink.Event(event)) } }
    // The next meeting within the hour: picked once a minute, dropped the second it starts.
    val minute = now.epochMillis / 60_000L
    val next = remember(events, minute, settings.showCalendarEvents) {
        if (!settings.showCalendarEvents) null
        else events?.let { upcomingMeeting(it, Instant.fromEpochMilliseconds(minute * 60_000L)) }
    }
    val upcoming = next?.takeIf { it.begin > now.epochMillis }
    MeetingChime(upcoming, now, settings.meetingChimeMinutes)
    val focusLengths = FocusLengths(settings.focusMinutes, settings.focusBreakMinutes, settings.focusLongBreakMinutes)
    FocusChime(focus, now.epochMillis, focusLengths, onFocus)

    // Reminders to stretch and drink some water, held during focus blocks and meetings. Two coming due
    // together chime once.
    val held = remindersHeld(
        focus?.status(now.epochMillis, focusLengths),
        inMeeting = settings.showCalendarEvents && events?.any { it.isOngoing(now.instant) } == true,
    )
    var lastNudgeChime by remember { mutableLongStateOf(0L) }
    val nudgeChime = {
        val t = Clock.System.now().toEpochMilliseconds()
        if (t - lastNudgeChime > 10_000L) {
            lastNudgeChime = t
            platform.playReminderChime()
        }
    }
    val stretch = rememberReminder(settings.stretchReminderMinutes, held, now.epochMillis, nudgeChime)
    val water = rememberReminder(settings.waterReminderMinutes, held, now.epochMillis, nudgeChime)
    val reminders = buildList {
        if (stretch.showing) add(ReminderKind.STRETCH)
        if (water.showing) add(ReminderKind.WATER)
    }
    // Battery care: once charged far enough, until it's unplugged or the tag is tapped away.
    val battery = platform.battery()
    var batteryCareDismissed by remember { mutableStateOf(false) }
    LaunchedEffect(battery?.charging) { if (battery?.charging != true) batteryCareDismissed = false }
    val batteryCare = battery?.takeIf { batteryCareDue(it, settings.batteryCarePercent) && !batteryCareDismissed }

    // The weather's details over the sky (rain on the way, the sun, the UV index, the air quality), and
    // the countdowns in the desk clock.
    val weather by WeatherRepository.weather.collectAsStateWithLifecycle()
    val air by WeatherRepository.airQuality.collectAsStateWithLifecycle()
    LaunchedEffect(settings.dayAirQuality, settings.showWeather, settings.weatherUseDeviceLocation, settings.weatherLat, settings.weatherLon) {
        if (!settings.dayAirQuality || !settings.showWeather) return@LaunchedEffect
        while (true) {
            WeatherRepository.refreshAirQualityIfStale(settings) { platform.deviceLocation() }
            delay(10 * 60_000L)
        }
    }
    val lookAhead = today.plus(DAYS_OFF_LOOK_AHEAD, DateTimeUnit.DAY)
    val aheadEvents = platform.calendarEvents(settings.countdownDaysOff, today, lookAhead, now.zone)
    val daysOff = remember(aheadEvents, today, settings.dayOffKeywords) {
        aheadEvents?.let { daysOff(it, parseDayOffKeywords(settings.dayOffKeywords), today, lookAhead) }
    }
    val timeFormat = rememberTimeFormat(settings.use24h)
    val details = remember(minute, weather, air, settings, locale, now.zone) {
        weatherGlanceItems(settings, weather, air, Instant.fromEpochMilliseconds(minute * 60_000L), now.zone, timeFormat, locale)
    }
    val glance = remember(minute, daysOff, settings, locale, now.zone) {
        countdownItems(settings, daysOff, Instant.fromEpochMilliseconds(minute * 60_000L), now.zone, locale)
    }
    val skyWeather = weather?.takeIf { settings.showWeather }

    // The to-do list on the note, and the holiday's greeting over it.
    val todos = remember(settings.dayTodos, settings.todos, today) { if (settings.dayTodos) todosForDay(settings.todos, today) else null }
    val onTodo: ((Int) -> Unit)? = onOpen?.let { { i -> SettingsStore.update { s -> s.copy(todos = toggleTodo(todosForDay(s.todos, today), i, today)) } } }
    val onAddTodo: ((String) -> Unit)? = onOpen?.let { { text -> SettingsStore.update { s -> s.copy(todos = addTodo(s.todos, text, today)) } } }
    val holiday = if (settings.dayHolidays) holidayOn(today, settings.birthday, locale.countryCode) else null
    val greetingText = holiday?.takeIf { it.theDay }?.let { holidayGreeting(it.holiday, locale) } ?: greeting(now.hour, locale)

    // The page starts on the view chosen in Settings; a swipe (or the switch under the page)
    // changes it for as long as the clock stays open. The screen saver can't be swiped.
    var view by rememberSaveable(settings.dayCalendarView) { mutableStateOf(settings.dayCalendarView) }
    val onView: ((CalendarView) -> Unit)? = if (onOpen != null) { v -> view = v } else null
    // The day view lists the whole day on the page itself, so the sticky note would only repeat it; with
    // the to-do list, the note stays for that (and the next meeting's countdown).
    val agendaOnNote = settings.showCalendarEvents && view != CalendarView.DAY
    val showNote = agendaOnNote || todos != null
    // Burn-in protection: besides the whole layout's drift, the clock, the page and the note each
    // drift on their own, at different moments, so their edges don't keep the same distance all day.
    // Kept out here so swapping sides doesn't send them back to the middle.
    val burnIn = settings.burnInProtection
    val clockDrift = rememberDrift(burnIn, phase = 0)
    val pageDrift = rememberDrift(burnIn, phase = 1)
    val noteDrift = rememberDrift(burnIn, phase = 2)
    // The page is the brightest thing on screen, so its paper slowly dims the longer it stays up.
    val shownSince = rememberSaveable { now.epochMillis }
    val minutesShown = (now.epochMillis - shownSince) / 60_000f
    val paper = if (burnIn) lerp(Paper, DimPaper, paperDim(minutesShown)) else Paper
    val page: @Composable (Modifier) -> Unit = { m ->
        DeskPage(view, now, weeks, dots, cakes, settings.dayWeekNumbers, events, settings.showCalendarEvents, settings.use24h, theme, paper, openDay, openEvent, onView, m)
    }

    // Other time zones under the desk clock's time, worked out once a minute.
    val worldText = remember(settings.worldZones, minute, now.zone, settings.use24h, locale) {
        worldClockLine(settings.worldZones, Instant.fromEpochMilliseconds(minute * 60_000L), now.zone, settings.use24h, locale)
    }

    BoxWithConstraints(modifier) {
        val noteFont = with(LocalDensity.current) { (minOf(maxWidth, maxHeight) * 0.042f).coerceIn(14.dp, 22.dp).toSp() }
        val worldLine: (@Composable () -> Unit)? = worldText?.let { text -> { WorldLine(text, noteFont) } }
        // The countdowns, the focus timer and the nudges sit in the desk clock under its info line, so
        // they drift with it and never take the meeting countdown's place under the clock or on the note.
        // [glanceShown] of the countdowns: fewer where the note needs the room (see deskClockFit).
        fun extras(glanceShown: Int): (@Composable () -> Unit)? =
            if (glance.size.coerceAtMost(glanceShown) == 0 && focus == null && reminders.isEmpty() && batteryCare == null) null else {
            {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    if (glanceShown > 0 && glance.isNotEmpty()) GlanceLine(glance.take(glanceShown), noteFont)
                    focus?.let { timer -> FocusLine(timer, timer.status(now.epochMillis, focusLengths), focusLengths, noteFont, onFocus) }
                    if (reminders.isNotEmpty()) {
                        NudgeTag(reminderText(reminders, locale), noteFont, onOpen?.let { { stretch.dismiss(); water.dismiss() } })
                    }
                    batteryCare?.let { b -> NudgeTag(batteryCareText(b, locale), noteFont, onOpen?.let { { batteryCareDismissed = true } }) }
                }
            }
        }
        // The weather over the sky and the desk clock under it, drifting together. [shown] of the weather's
        // details and, with [hourly], the next hours, or with [slim] the weather on one line; [world] the
        // world clock line.
        fun deskClock(shown: Int, hourly: Boolean, world: Boolean, rangeX: Dp, rangeY: Dp, slim: Boolean = false): @Composable () -> Unit = {
            Column(Modifier.drift(clockDrift, rangeX, rangeY), horizontalAlignment = Alignment.CenterHorizontally) {
                skyWeather?.let { w ->
                    SkyWeather(w, details.take(shown), hourly && settings.dayHourlyWeather, slim, now, settings, noteFont)
                    Spacer(Modifier.height(SKY_GAP))
                }
                ClockPanel(clock, extras(shown), world = if (world) worldLine else null)
            }
        }
        val note: @Composable (Modifier) -> Unit = { m ->
            StickyNote(now, events, upcoming, agendaOnNote, greetingText, todos, onTodo, onAddTodo, settings.use24h, theme, noteFont, openDay, openEvent, m)
        }
        if (landscape) {
            // Every other hour the clock and the page trade places, so the bright page and the dark
            // clock don't sit on the same pixels all day.
            Crossfade(sidesSwapped(now.hour, settings.burnInProtection), animationSpec = tween(1500), label = "sides") { swapped ->
                Row(Modifier.fillMaxSize(), verticalAlignment = Alignment.CenterVertically) {
                    val clockSide = @Composable {
                        // On a phone the clock takes most of the column's height; the note shows only
                        // when its greeting and at least one event fit under it.
                        ClockOverNote(
                            gap = 32.dp,
                            // Fullest first: without the world clock line, then with one weather detail and one
                            // countdown, then none, then without the next hours, for when they leave no room for the note.
                            clocks = buildList {
                                val most = maxOf(details.size, glance.size)
                                add(deskClock(most, hourly = true, world = true, 28.dp, 14.dp))
                                if (worldLine != null) add(deskClock(most, hourly = true, world = false, 28.dp, 14.dp))
                                if (most > 1) add(deskClock(1, hourly = true, world = false, 28.dp, 14.dp))
                                if (most > 0) add(deskClock(0, hourly = true, world = false, 28.dp, 14.dp))
                                // Then the next hours' weather, and last of all the weather shrinks to one line.
                                add(deskClock(0, hourly = false, world = false, 28.dp, 14.dp))
                                if (skyWeather != null) add(deskClock(0, hourly = false, world = false, 28.dp, 14.dp, slim = true))
                            },
                            note = if (!showNote) null else {
                                { note(Modifier.widthIn(max = 380.dp).drift(noteDrift, 28.dp, 14.dp)) }
                            },
                            smallestNote = { SmallestNote(greetingText, noteFont, Modifier.widthIn(max = 380.dp)) },
                            // Without the note, the countdown hangs under the desk clock instead.
                            countdown = upcoming?.let { e ->
                                { CountdownTag(countdownLine(e, now.instant, currentLocale()), noteFont, openEvent?.let { open -> { open(e) } }, Modifier.drift(clockDrift, 28.dp, 14.dp)) }
                            },
                            modifier = Modifier.weight(1f).fillMaxHeight(),
                        )
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
                        Column(Modifier.padding(vertical = 16.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                            PortraitDeskClock(
                                full = deskClock(Int.MAX_VALUE, hourly = true, world = true, 24.dp, 8.dp),
                                bare = worldLine?.let { deskClock(Int.MAX_VALUE, hourly = true, world = false, 24.dp, 8.dp) },
                            )
                            // Without the note (the day view), the countdown hangs under the desk clock.
                            if (!showNote && upcoming != null) {
                                Spacer(Modifier.height(COUNTDOWN_GAP))
                                CountdownTag(
                                    countdownLine(upcoming, now.instant, currentLocale()), noteFont, openEvent?.let { open -> { open(upcoming) } },
                                    Modifier.drift(clockDrift, 24.dp, 8.dp),
                                )
                            }
                        }
                    }
                    if (!swapped) clockEnd()
                    page(Modifier.fillMaxWidth().weight(1.6f).drift(pageDrift, 16.dp, 8.dp))
                    if (showNote) {
                        Spacer(Modifier.height(20.dp))
                        note(Modifier.fillMaxWidth(0.9f).weight(1f, fill = false).drift(noteDrift, 20.dp, 8.dp))
                    }
                    if (swapped) clockEnd()
                }
            }
        }
    }
}

/**
 * The clock with the sticky note under it, the pair centred in the space. [clocks] are the desk clock's
 * versions, fullest first, each leaving out a little more (see [deskClockFit]): the fullest that leaves
 * room for [smallestNote] (the note with its greeting and one event) shows with [note], which gets all
 * the height the clock leaves. When even the barest leaves no room, the note is left out and [countdown]
 * (if any) goes under the clock instead, no wider than it, and only if it fits too.
 */
@Composable
private fun ClockOverNote(
    gap: Dp,
    clocks: List<@Composable () -> Unit>,
    note: (@Composable () -> Unit)?,
    smallestNote: @Composable () -> Unit,
    countdown: (@Composable () -> Unit)?,
    modifier: Modifier,
) {
    SubcomposeLayout(modifier) { constraints ->
        val loose = constraints.copy(minWidth = 0, minHeight = 0)
        val gapPx = gap.roundToPx()
        val noteMin = note?.let { subcompose("smallest", smallestNote).maxOfOrNull { it.measure(loose).height } ?: 0 }
        // The versions are measured one by one, and only as far as needed: the fullest usually fits.
        val measured = mutableListOf<List<androidx.compose.ui.layout.Placeable>>()
        fun fits(height: Int) = if (noteMin != null) noteFits(height, gapPx, noteMin, constraints.maxHeight) else height <= constraints.maxHeight
        for ((i, version) in clocks.withIndex()) {
            val placeables = subcompose("clock$i", version).map { it.measure(loose) }
            measured += placeables
            if (fits(placeables.maxOfOrNull { it.height } ?: 0)) break
        }
        val fit = deskClockFit(measured.map { p -> p.maxOfOrNull { it.height } ?: 0 }, gapPx, noteMin, constraints.maxHeight)
        val clockPlaceables = measured[fit.level]
        val clockHeight = clockPlaceables.maxOfOrNull { it.height } ?: 0
        val notePlaceables = if (note == null || !fit.note) emptyList() else {
            subcompose("note", note).map { it.measure(loose.copy(maxHeight = constraints.maxHeight - clockHeight - gapPx)) }
        }
        val noteHeight = notePlaceables.maxOfOrNull { it.height } ?: 0
        val clockWidth = clockPlaceables.maxOfOrNull { it.width } ?: 0
        val tagGapPx = COUNTDOWN_GAP.roundToPx()
        val tagPlaceables = if (notePlaceables.isNotEmpty() || countdown == null) emptyList() else {
            val tag = subcompose("countdown", countdown).map { it.measure(loose.copy(maxWidth = clockWidth)) }
            if (clockHeight + tagGapPx + (tag.maxOfOrNull { it.height } ?: 0) <= constraints.maxHeight) tag else emptyList()
        }
        val tagHeight = tagPlaceables.maxOfOrNull { it.height } ?: 0
        val total = clockHeight + when {
            notePlaceables.isNotEmpty() -> gapPx + noteHeight
            tagPlaceables.isNotEmpty() -> tagGapPx + tagHeight
            else -> 0
        }
        val width = constraints.maxWidth
        val height = constraints.maxHeight
        layout(width, height) {
            val top = (height - total) / 2
            clockPlaceables.forEach { it.place((width - it.width) / 2, top) }
            notePlaceables.forEach { it.place((width - it.width) / 2, top + clockHeight + gapPx) }
            tagPlaceables.forEach { it.place((width - it.width) / 2, top + clockHeight + tagGapPx) }
        }
    }
}

/**
 * The clock sits in a navy desk clock, solid so clouds or a busy photo never wash out its digits.
 * [world] (the other time zones) and then [below] (the focus timer) go under the clock, inside the panel.
 */
@Composable
private fun ClockPanel(
    clock: @Composable () -> Unit,
    below: (@Composable () -> Unit)?,
    modifier: Modifier = Modifier,
    world: (@Composable () -> Unit)? = null,
) {
    val shape = RoundedCornerShape(28.dp)
    Box(
        modifier
            .shadow(10.dp, shape)
            .clip(shape)
            .background(Brush.verticalGradient(DeskClockNavy))
            .padding(horizontal = 28.dp, vertical = 12.dp)
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            clock()
            world?.invoke()
            below?.invoke()
        }
    }
}

/** The other time zones in the desk clock, e.g. "London 19:42 · Tokyo 04:42 +1", a little dimmer than the info line. */
@Composable
private fun WorldLine(text: String, fontSize: TextUnit) {
    Text(
        text,
        style = TextStyle(
            color = Color.White.copy(alpha = 0.78f),
            fontSize = fontSize * 0.85f,
            fontFeatureSettings = "tnum",
            textAlign = TextAlign.Center,
        ),
        modifier = Modifier.padding(top = 4.dp),
    )
}

/**
 * Portrait's desk clock: [full], with the world clock line, while it leaves the calendar page most of
 * the screen (see [worldLineFitsPortrait]), and [bare] without it otherwise.
 */
@Composable
private fun PortraitDeskClock(full: @Composable () -> Unit, bare: (@Composable () -> Unit)?) {
    if (bare == null) return full()
    SubcomposeLayout { constraints ->
        val loose = constraints.copy(minWidth = 0, minHeight = 0)
        val withLine = subcompose("full", full).map { it.measure(loose) }
        val shown = if (worldLineFitsPortrait(withLine.maxOfOrNull { it.height } ?: 0, constraints.maxHeight)) withLine
        else subcompose("bare", bare).map { it.measure(loose) }
        val width = shown.maxOfOrNull { it.width } ?: 0
        layout(width, shown.maxOfOrNull { it.height } ?: 0) {
            shown.forEach { it.place((width - it.width) / 2, 0) }
        }
    }
}

/** A focus block's ring and a break's, so the two read apart at a glance. */
private val FocusRingColor = Color.White
private val BreakRingColor = Color(0xFF9BE3B0)

/**
 * The focus timer in the desk clock, e.g. "Focus · 18:42 left" beside a thin ring of the time gone.
 * A tap shows Pause, Skip and Stop under it for a few seconds; [onFocus] gets the changed timer.
 */
@Composable
private fun FocusLine(
    timer: FocusTimer,
    status: FocusStatus,
    lengths: FocusLengths,
    fontSize: TextUnit,
    onFocus: ((FocusTimer?) -> Unit)?,
) {
    var open by remember { mutableStateOf(false) }
    var touched by remember { mutableIntStateOf(0) }
    LaunchedEffect(open, touched) {
        if (open) {
            delay(FOCUS_ACTIONS_MS)
            open = false
        }
    }
    val line = with(LocalDensity.current) { fontSize.toDp() }
    val style = TextStyle(color = Color.White, fontSize = fontSize * 0.85f, fontWeight = FontWeight.Medium, fontFeatureSettings = "tnum")
    Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.padding(top = 6.dp)) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier
                .clip(RoundedCornerShape(50))
                .clickable(enabled = onFocus != null, onClickLabel = stringResource(Res.string.day_focus_actions)) { open = !open }
                .padding(horizontal = 10.dp, vertical = 4.dp),
        ) {
            FocusRing(status.progress, if (status.phase == FocusPhase.FOCUS) FocusRingColor else BreakRingColor, Modifier.size(line * 0.9f))
            Spacer(Modifier.width(line * 0.5f))
            Text(focusLabel(status, currentLocale()), style = style.copy(color = Color.White.copy(alpha = if (status.paused) 0.7f else 1f)), maxLines = 1)
        }
        if (open && onFocus != null) {
            fun act(change: (Long) -> FocusTimer?) {
                touched++
                onFocus(change(Clock.System.now().toEpochMilliseconds()))
            }
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.padding(top = 4.dp)) {
                FocusAction(if (timer.paused) stringResource(Res.string.day_resume) else stringResource(Res.string.day_pause), style) { act { t -> if (timer.paused) timer.resume(t) else timer.pause(t) } }
                FocusAction(stringResource(Res.string.day_skip), style) { act { t -> timer.skip(t, lengths) } }
                FocusAction(stringResource(Res.string.day_stop), style) { open = false; onFocus(null) }
            }
        }
    }
}

/** How long Pause, Skip and Stop stay up after the last tap. */
private const val FOCUS_ACTIONS_MS = 8_000L

@Composable
private fun FocusAction(text: String, style: TextStyle, onClick: () -> Unit) {
    Text(
        text,
        style = style,
        maxLines = 1,
        modifier = Modifier
            .clip(RoundedCornerShape(50))
            .background(Color.White.copy(alpha = 0.14f))
            .clickable(onClick = onClick)
            .padding(horizontal = 14.dp, vertical = 6.dp),
    )
}

/** A thin ring, filled clockwise from the top by [progress] (0 to 1). */
@Composable
private fun FocusRing(progress: Float, color: Color, modifier: Modifier) {
    Canvas(modifier) {
        val stroke = size.minDimension * 0.16f
        val inset = stroke / 2
        val arcSize = Size(size.width - stroke, size.height - stroke)
        drawArc(color.copy(alpha = 0.25f), 0f, 360f, false, Offset(inset, inset), arcSize, style = Stroke(stroke))
        drawArc(color, -90f, 360f * progress, false, Offset(inset, inset), arcSize, style = Stroke(stroke, cap = StrokeCap.Round))
    }
}

/**
 * Plays the soft chime as the focus timer moves between focus and a break, once per switch, and
 * marks it heard in the timer so a rotation doesn't chime again. A skip doesn't chime.
 */
@Composable
private fun FocusChime(focus: FocusTimer?, now: Long, lengths: FocusLengths, onFocus: ((FocusTimer?) -> Unit)?) {
    val platform = LocalClockPlatform.current
    val timer by rememberUpdatedState(focus)
    val change by rememberUpdatedState(onFocus)
    val due = focus?.chimeDue(now, lengths)
    LaunchedEffect(due) {
        val t = timer
        val c = change
        if (due != null && t != null && c != null) {
            c(t.copy(chimed = due))
            platform.playFocusChime()
        }
    }
}

private val DeskClockNavy = listOf(Color(0xFF2B3A55), Color(0xFF1C263A))

/** Space between the weather over the sky and the desk clock under it. */
private val SKY_GAP = 12.dp

/** Space between the desk clock and the countdown hung under it. */
private val COUNTDOWN_GAP = 10.dp

/**
 * The next meeting's countdown, e.g. "Standup in 12 min", in a small navy tag under the desk clock:
 * where it goes when the sticky note isn't showing. A tap opens the event.
 */
@Composable
private fun CountdownTag(text: String, fontSize: TextUnit, onClick: (() -> Unit)?, modifier: Modifier) {
    val shape = RoundedCornerShape(50)
    Text(
        text,
        style = TextStyle(color = Color.White, fontSize = fontSize * 0.85f, fontWeight = FontWeight.Medium, fontFeatureSettings = "tnum"),
        maxLines = 1,
        overflow = TextOverflow.Ellipsis,
        modifier = modifier
            .shadow(6.dp, shape)
            .clip(shape)
            .background(Brush.verticalGradient(DeskClockNavy))
            .clickable(enabled = onClick != null, onClickLabel = stringResource(Res.string.day_open_calendar)) { onClick?.invoke() }
            .padding(horizontal = 16.dp, vertical = 6.dp),
    )
}

/**
 * Plays the soft chime [minutesBefore] minutes before [event], once per event, while day mode is up.
 * Remembered across rotation, so turning the phone in that minute doesn't chime again.
 */
@Composable
private fun MeetingChime(event: CalendarEvent?, now: Moment, minutesBefore: Int) {
    val platform = LocalClockPlatform.current
    var chimed by rememberSaveable { mutableStateOf<String?>(null) }
    val due = event?.takeIf { shouldChime(it, now.instant, minutesBefore, chimed) }?.let(::chimeKey)
    LaunchedEffect(due) {
        if (due != null) {
            chimed = due
            platform.playMeetingChime()
        }
    }
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
    now: Moment,
    weeks: List<List<LocalDate>>,
    dots: Set<LocalDate>,
    cakes: Set<LocalDate>,
    weekNumbers: Boolean,
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
    val today = now.date
    val locale = currentLocale()
    val month = remember(today.year, today.month, locale) {
        DateTimePattern("LLLL", locale).format(today.atTime(0, 0)).capitalizedFirst(locale)
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
        // Week numbers take a narrow column of their own down the month's side.
        val pageWidth = PAGE_WIDTH + if (weekNumbers) WEEK_NUMBER_WIDTH else 0f
        val cell = minOf(maxWidth / pageWidth, maxHeight / (0.3f + 1.6f + 0.1f + bodyCells + SWITCH + 0.15f), 72.dp)
        val font = with(LocalDensity.current) { (cell * 0.4f).toSp() }
        val shape = RoundedCornerShape(cell * 0.28f)
        val base = TextStyle(color = Ink, fontSize = font, fontFeatureSettings = "tnum")

        Box(Modifier.width(cell * pageWidth)) {
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
                        if (view == CalendarView.WEEK) stringResource(Res.string.day_week_number, weekOfYear(today, locale)) else today.year.toString(),
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
                        CalendarView.MONTH -> MonthBody(today, weeks, dots, cakes, weekNumbers, theme.accent, cell, base, onOpenDay)
                        CalendarView.WEEK ->
                            WeekBody(now, weekOf(today, firstDayOfWeek(locale)), events, showEvents, use24h, theme.accent, cell, base, onOpenDay, onOpenEvent)
                        CalendarView.DAY -> DayBody(now, events, showEvents, use24h, theme.accent, cell, base, onOpenDay, onOpenEvent)
                    }
                }
                ViewSwitch(view, theme.accent, cell, base, onView)
            }
            Rings(Modifier.width(cell * pageWidth).height(cell * 0.75f), cell)
        }
    }
}

@Composable
private fun MonthBody(
    today: LocalDate,
    weeks: List<List<LocalDate>>,
    dots: Set<LocalDate>,
    cakes: Set<LocalDate>,
    weekNumbers: Boolean,
    accent: Color,
    cell: Dp,
    base: TextStyle,
    onOpenDay: ((LocalDate) -> Unit)?,
) {
    val locale = currentLocale()
    val numberStyle = base.copy(color = Ink.copy(alpha = 0.35f), fontSize = base.fontSize * 0.6f, fontWeight = FontWeight.Bold)
    Column(Modifier.fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally) {
        Row {
            if (weekNumbers) Spacer(Modifier.width(cell * WEEK_NUMBER_WIDTH))
            weeks.first().forEach { d ->
                Box(Modifier.size(cell, cell * 0.7f), contentAlignment = Alignment.Center) {
                    Text(
                        d.dayOfWeek.narrowName(locale),
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
                if (weekNumbers) {
                    Box(Modifier.size(cell * WEEK_NUMBER_WIDTH, cell * ROW), contentAlignment = Alignment.Center) {
                        Text(weekOfYear(week.first(), locale).toString(), style = numberStyle, maxLines = 1)
                    }
                }
                week.forEach { d -> DayCell(d, today, d in dots, d in cakes, accent, cell, base, onOpenDay) }
            }
        }
    }
}

/** This week, one row per day: the date and as many of its events as fit, finished ones faded. */
@Composable
private fun WeekBody(
    now: Moment,
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
    val today = now.date
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
                        .clickable(enabled = onOpenDay != null, onClickLabel = stringResource(Res.string.day_open_calendar)) { onOpenDay?.invoke(d) }
                        .padding(horizontal = cell * 0.15f),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Row(Modifier.width(cell * 1.9f), verticalAlignment = Alignment.Bottom) {
                        Text(
                            d.dayOfWeek.shortName(locale),
                            style = base.copy(
                                fontSize = base.fontSize * 0.62f,
                                color = if (isToday || d.dayOfWeek in WEEKEND) accent else Ink.copy(alpha = 0.55f),
                                fontWeight = FontWeight.Bold,
                            ),
                            maxLines = 1,
                        )
                        Spacer(Modifier.width(cell * 0.12f))
                        Text(
                            d.day.toString(),
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
    now: Moment,
    time: DateTimePattern,
    fontSize: TextUnit,
    accent: Color,
    more: Int,
    onOpenEvent: ((CalendarEvent) -> Unit)?,
) {
    val faded = !e.allDay && e.end <= now.epochMillis
    val line = with(LocalDensity.current) { fontSize.toDp() }
    Row(
        Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(4.dp))
            .clickable(enabled = onOpenEvent != null, onClickLabel = stringResource(Res.string.day_open_calendar)) { onOpenEvent?.invoke(e) },
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(Modifier.size(line * 0.55f).clip(CircleShape).background((e.color?.let { Color(it) } ?: accent).copy(alpha = if (faded) 0.4f else 1f)))
        Spacer(Modifier.width(line * 0.4f))
        val start = if (e.allDay) "" else time.format(Instant.fromEpochMilliseconds(e.begin).toLocalDateTime(now.zone)) + "  "
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

/** Today, like a tear-off calendar: the big date beside the day's schedule ([dayViewEvents]). */
@Composable
private fun DayBody(
    now: Moment,
    events: List<CalendarEvent>?,
    showEvents: Boolean,
    use24h: Boolean,
    accent: Color,
    cell: Dp,
    base: TextStyle,
    onOpenDay: ((LocalDate) -> Unit)?,
    onOpenEvent: ((CalendarEvent) -> Unit)?,
) {
    val platform = LocalClockPlatform.current
    val locale = currentLocale()
    val today = now.date
    val time = rememberTimeFormat(use24h)
    val access = remember(events) { platform.hasCalendarAccess() }
    val minute = now.epochMillis / 60_000L
    val dayEvents = remember(events, today, minute) {
        events?.let { dayViewEvents(it, today, now.zone, Instant.fromEpochMilliseconds(minute * 60_000L)) }
    }
    val faint = TextStyle(color = Ink.copy(alpha = 0.55f), fontSize = base.fontSize * 0.7f)
    val instant = now.instant
    fun fmt(millis: Long) = time.format(Instant.fromEpochMilliseconds(millis).toLocalDateTime(now.zone))

    Row(Modifier.fillMaxSize(), verticalAlignment = Alignment.CenterVertically) {
        Column(
            Modifier
                .width(cell * 2.5f)
                .fillMaxHeight()
                .clip(RoundedCornerShape(cell * 0.2f))
                .clickable(enabled = onOpenDay != null, onClickLabel = stringResource(Res.string.day_open_calendar)) { onOpenDay?.invoke(today) },
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
        ) {
            Text(
                today.dayOfWeek.fullName(locale).capitalizedFirst(locale),
                style = base.copy(color = accent, fontSize = base.fontSize * 0.75f, fontWeight = FontWeight.Bold),
                maxLines = 1,
            )
            Text(
                today.day.toString(),
                style = base.copy(fontSize = base.fontSize * 4f, fontWeight = FontWeight.Bold, lineHeight = base.fontSize * 4.2f),
                maxLines = 1,
            )
        }
        Box(Modifier.width(1.dp).fillMaxHeight(0.8f).background(Ink.copy(alpha = 0.15f)))
        Spacer(Modifier.width(cell * 0.3f))
        Box(Modifier.weight(1f).fillMaxHeight(), contentAlignment = Alignment.CenterStart) {
            when {
                !showEvents -> Text(stringResource(Res.string.day_turn_on_events), style = faint)
                !access -> Text(stringResource(Res.string.day_allow_calendar), style = faint)
                dayEvents == null -> Unit // still loading
                dayEvents.isEmpty() -> Text(stringResource(Res.string.day_nothing_today), style = faint)
                else -> FitColumn(cell * 0.12f, Modifier, more = { n -> Text(stringResource(Res.string.day_more, n), style = faint) }) {
                    dayEvents.forEach { e ->
                        val ongoing = e.isOngoing(instant)
                        val whenText = when {
                            e.allDay -> stringResource(Res.string.day_all_day)
                            ongoing -> stringResource(Res.string.day_now_until, fmt(e.end))
                            else -> "${fmt(e.begin)} – ${fmt(e.end)}"
                        }
                        EventRow(
                            e.title, whenText, e.color?.let { Color(it) } ?: accent, base.fontSize * 0.72f,
                            emphasised = ongoing,
                            onClick = onOpenEvent?.let { open -> { open(e) } },
                            ink = Ink,
                            faded = !e.allDay && e.end <= now.epochMillis,
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
    val locale = currentLocale()
    Row(
        Modifier.fillMaxWidth().height(cell * SWITCH),
        horizontalArrangement = Arrangement.spacedBy(cell * 0.15f, Alignment.CenterHorizontally),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        CalendarView.entries.forEach { v ->
            val selected = v == view
            Text(
                v.label(locale),
                style = base.copy(
                    fontSize = base.fontSize * 0.62f,
                    color = if (selected) accent else Ink.copy(alpha = 0.45f),
                    fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium,
                ),
                modifier = Modifier
                    .clip(RoundedCornerShape(50))
                    .background(if (selected) accent.copy(alpha = 0.12f) else Color.Transparent)
                    .clickable(enabled = onView != null, onClickLabel = stringResource(Res.string.day_show_view, v.label(locale).lowercase())) { onView?.invoke(v) }
                    .padding(horizontal = cell * 0.25f, vertical = cell * 0.06f),
            )
        }
    }
}

@Composable
private fun rememberTimeFormat(use24h: Boolean): DateTimePattern {
    val locale = currentLocale()
    return remember(use24h, locale) { DateTimePattern(bestDateTimePattern(if (use24h) "Hm" else "hma", locale), locale) }
}

@Composable
private fun DayCell(
    day: LocalDate,
    today: LocalDate,
    hasEvents: Boolean,
    birthday: Boolean,
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
            .clickable(enabled = onOpen != null, onClickLabel = stringResource(Res.string.day_open_calendar)) { onOpen?.invoke(day) },
        contentAlignment = Alignment.Center,
    ) {
        if (isToday) Box(Modifier.size(cell * ROW * 0.95f).shadow(3.dp, CircleShape).clip(CircleShape).background(accent))
        Text(
            day.day.toString(),
            style = when {
                isToday -> base.copy(color = Color.White, fontWeight = FontWeight.Bold)
                !inMonth -> base.copy(color = Ink.copy(alpha = 0.2f))
                day.dayOfWeek in WEEKEND -> base.copy(color = accent)
                else -> base
            },
        )
        if (birthday && inMonth) {
            Text(
                CAKE,
                style = TextStyle(fontSize = base.fontSize * 0.42f),
                modifier = Modifier.align(Alignment.BottomCenter).padding(bottom = cell * 0.01f),
            )
        } else if (hasEvents && inMonth) {
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

/**
 * A yellow sticky note with a hello, the rest of today's events ([showAgenda]) and the to-do list
 * ([todos], null when it's off). Without the agenda, the next meeting's countdown ([upcoming]) still
 * goes at the top.
 */
@Composable
private fun StickyNote(
    now: Moment,
    events: List<CalendarEvent>?,
    upcoming: CalendarEvent?,
    showAgenda: Boolean,
    greeting: String,
    todos: List<TodoItem>?,
    onTodo: ((Int) -> Unit)?,
    onAddTodo: ((String) -> Unit)?,
    use24h: Boolean,
    theme: MonthTheme,
    fontSize: TextUnit,
    onOpenDay: ((LocalDate) -> Unit)?,
    onOpenEvent: ((CalendarEvent) -> Unit)?,
    modifier: Modifier,
) {
    // Anywhere on the note but an event or a to-do opens today; an event opens itself.
    NotePaper(onOpenDay?.let { open -> { open(now.date) } }, modifier) {
        Greeting(greeting, fontSize)
        if (showAgenda) {
            Agenda(now, events, upcoming, use24h, theme.accent, fontSize, onOpenEvent, Modifier.weight(1f, fill = false))
        } else if (upcoming != null) {
            val time = rememberTimeFormat(use24h)
            EventRow(
                upcoming.title, countdownLabel(upcoming, now.instant, now.zone, time, currentLocale()), upcoming.color?.let { Color(it) } ?: theme.accent,
                fontSize, emphasised = true, onClick = onOpenEvent?.let { open -> { open(upcoming) } },
            )
        }
        if (todos != null) {
            if (showAgenda || upcoming != null) {
                Box(Modifier.padding(vertical = 8.dp).fillMaxWidth().height(1.dp).background(NoteInk.copy(alpha = 0.15f)))
            }
            TodoList(todos, NoteInk, theme.accent, fontSize, onTodo, onAddTodo, Modifier.weight(1f, fill = false))
        }
    }
}

/** The sticky note at its smallest: its greeting and one event. Only measured, never drawn. */
@Composable
private fun SmallestNote(greeting: String, fontSize: TextUnit, modifier: Modifier) {
    NotePaper(null, modifier) {
        Greeting(greeting, fontSize)
        EventRow(stringResource(Res.string.day_event_placeholder), "00:00 – 00:00", NoteInk, fontSize, emphasised = false, onClick = null)
    }
}

@Composable
private fun Greeting(text: String, fontSize: TextUnit) {
    Text(
        text,
        style = TextStyle(color = NoteInk, fontSize = fontSize * 1.6f, fontFamily = FontFamily.Cursive, fontWeight = FontWeight.Bold),
    )
    Spacer(Modifier.height(6.dp))
}

/** The yellow paper of the sticky note, a little crooked, with tape holding it up. */
@Composable
private fun NotePaper(onClick: (() -> Unit)?, modifier: Modifier, content: @Composable ColumnScope.() -> Unit) {
    Box(modifier.rotate(-1.5f)) {
        Column(
            Modifier
                .padding(top = 8.dp)
                .shadow(6.dp, RoundedCornerShape(2.dp))
                .background(NoteYellow)
                .clickable(enabled = onClick != null, onClickLabel = stringResource(Res.string.day_open_today)) { onClick?.invoke() }
                .padding(horizontal = 18.dp, vertical = 14.dp),
            content = content,
        )
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
    now: Moment,
    events: List<CalendarEvent>?,
    /** The next meeting within the hour: first on the note, counting down, and not repeated below. */
    upcoming: CalendarEvent?,
    use24h: Boolean,
    accent: Color,
    fontSize: TextUnit,
    onOpenEvent: ((CalendarEvent) -> Unit)?,
    modifier: Modifier,
) {
    val platform = LocalClockPlatform.current
    val access = remember(events) { platform.hasCalendarAccess() }
    val faint = TextStyle(color = NoteInk.copy(alpha = 0.7f), fontSize = fontSize * 0.9f)
    val instant = now.instant
    // Minute resolution is enough to drop finished events; avoids refiltering every second.
    val minute = now.epochMillis / 60_000L
    val shown = remember(events, minute) {
        events?.let { eventsForDay(it, now.date, now.zone, Instant.fromEpochMilliseconds(minute * 60_000L)) }
    }
    val time = rememberTimeFormat(use24h)
    fun fmt(millis: Long) = time.format(Instant.fromEpochMilliseconds(millis).toLocalDateTime(now.zone))

    when {
        !access -> Text(stringResource(Res.string.day_allow_calendar_note), style = faint, modifier = modifier)
        shown == null -> Unit // still loading
        shown.isEmpty() && upcoming == null -> Text(stringResource(Res.string.day_nothing_else), style = faint, modifier = modifier)
        else -> FitColumn(8.dp, modifier, more = { n -> Text(stringResource(Res.string.day_more_today, n), style = faint) }) {
            if (upcoming != null) {
                EventRow(
                    upcoming.title, countdownLabel(upcoming, instant, now.zone, time, currentLocale()), upcoming.color?.let { Color(it) } ?: accent,
                    fontSize, emphasised = true, onClick = onOpenEvent?.let { open -> { open(upcoming) } },
                )
            }
            shown.forEach { e ->
                if (e == upcoming) return@forEach
                val ongoing = e.isOngoing(instant)
                val whenText = when {
                    e.allDay -> stringResource(Res.string.day_all_day)
                    ongoing -> stringResource(Res.string.day_now_until, fmt(e.end))
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
            .clickable(enabled = onClick != null, onClickLabel = stringResource(Res.string.day_open_calendar)) { onClick?.invoke() },
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

/** The month view's column of week numbers, in cells. */
private const val WEEK_NUMBER_WIDTH = 0.6f

/** Marks a birthday on the page and the note. */
private const val CAKE = "🎂"

/** Height of the Month · Week · Day switch under the page, in cells. */
private const val SWITCH = 0.75f

/** Height of a week row, as a share of a day cell's width: a little flatter than square. */
private const val ROW = 0.82f

/**
 * A column that shows only the children that fit whole, so a long day never shows half an event;
 * when some are left out, [more] says how many.
 */
@Composable
internal fun FitColumn(gap: Dp, modifier: Modifier, more: @Composable (Int) -> Unit, content: @Composable () -> Unit) {
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
