package com.adl.nocturna.clock

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.Saver
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.runtime.withFrameMillis
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.Shadow
import androidx.compose.ui.layout.Layout
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LifecycleEventEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.adl.nocturna.alarm.alarmCountdownLabel
import com.adl.nocturna.data.AlarmStore
import com.adl.nocturna.data.ClockStyle
import com.adl.nocturna.data.Settings
import com.adl.nocturna.data.SettingsStore
import com.adl.nocturna.day.CalendarLink
import com.adl.nocturna.day.DayBackground
import com.adl.nocturna.day.DayLayout
import com.adl.nocturna.day.FocusTimer
import com.adl.nocturna.day.HourlyStrip
import com.adl.nocturna.day.focusTimerFromSaved
import com.adl.nocturna.day.isDayModeActive
import com.adl.nocturna.day.toSaved
import com.adl.nocturna.day.withDayModeToggled
import com.adl.nocturna.resources.*
import com.adl.nocturna.text.DateTimePattern
import com.adl.nocturna.text.bestDateTimePattern
import com.adl.nocturna.text.shortName
import com.adl.nocturna.ui.ControlButton
import com.adl.nocturna.ui.ControlDefaults
import com.adl.nocturna.ui.DeskNavy
import com.adl.nocturna.ui.Glyph
import com.adl.nocturna.ui.GlyphIcon
import com.adl.nocturna.ui.NightRed
import com.adl.nocturna.ui.currentLocale
import com.adl.nocturna.weather.WeatherRepository
import com.adl.nocturna.weather.pickForecast
import kotlin.random.Random
import kotlin.time.Clock
import kotlin.time.Instant
import kotlin.time.TimeSource
import kotlinx.coroutines.delay
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime
import org.jetbrains.compose.resources.stringResource

/**
 * The bedside clock: interactive in the app, with a tap-to-show control bar, and display only in the
 * screen saver. In day mode it sits beside a calendar ([DayLayout]). It reads the phone through
 * [LocalClockPlatform]. A tap on the dim clock peeks first (see [clockTap]), and so can a wave ([clockWave]).
 *
 * @param onBrightness receives the window brightness to apply (0..1), or -1 for "system default".
 */
@Composable
fun ClockScreen(
    interactive: Boolean,
    onBrightness: (Float) -> Unit,
    onOpenSettings: () -> Unit = {},
    onOpenAlarms: () -> Unit = {},
    /** Opens day mode's calendar in the phone's calendar app; only used when [interactive]. */
    onOpenCalendar: (CalendarLink) -> Unit = {},
    /** The screen saver's way out: a tap that doesn't peek, or a second one during a peek, ends it. */
    onDismiss: (() -> Unit)? = null,
    /** Told when night mode (the dim red look) goes on or off. */
    onNight: (Boolean) -> Unit = {},
) {
    val platform = LocalClockPlatform.current
    val settings by SettingsStore.settings.collectAsStateWithLifecycle()
    val now by rememberNow()
    val lux = platform.ambientLux()
    // Follows the schedule on its own: now ticks every second.
    val day = isDayModeActive(settings, now.instant, now.zone)

    // Sunrise before alarms: in the minutes before the next alarm (not a snooze) the clock brightens and
    // warms up. Day mode keeps its own look.
    val alarms by AlarmStore.alarms.collectAsStateWithLifecycle()
    val sunrise = if (day) 0f
    else sunriseProgress(now.instant, nextAlarmRing(alarms, now.instant, now.zone), settings.sunriseMinutes)

    // Tap to peek: a tap on the dim clock brightens it for a few seconds, then it fades back.
    var peekMark by remember { mutableStateOf<TimeSource.Monotonic.ValueTimeMark?>(null) }
    var peekShown by remember { mutableFloatStateOf(0f) }
    LaunchedEffect(peekMark) {
        val mark = peekMark ?: return@LaunchedEffect
        peekShown = 1f
        delay(PEEK_HOLD)
        do {
            peekShown = withFrameMillis { peekLevel(mark.elapsedNow()) }
        } while (peekShown > 0f)
        peekMark = null
    }
    val peek = if (day || !settings.tapToPeek) 0f else peekShown

    // Night mode with hysteresis so it doesn't flicker around the threshold. A sunrise or a peek that starts
    // from night red keeps to it, since the brightening screen can lift the light sensor's own reading.
    var night by remember { mutableStateOf(false) }
    val currentLux = lux
    night = when {
        !settings.dimEnabled || !settings.nightModeEnabled || day -> false
        night && (sunrise > 0f || peek > 0f) -> true
        currentLux == null -> now.hour >= 22 || now.hour < 7 // no light sensor: go by the clock
        night -> currentLux < settings.nightModeLux * 2.5f
        else -> currentLux < settings.nightModeLux
    }
    LaunchedEffect(night) { onNight(night) }

    val room = screenBrightness(
        currentLux, night, day, settings.dimEnabled, settings.autoBrightness,
        settings.minBrightness, settings.maxBrightness, settings.dayMaxBrightness,
    )
    val withSunrise = sunriseBrightness(room, settings.maxBrightness, sunrise)
    val brightness = peekBrightness(room, withSunrise, settings.maxBrightness, peek)
    LaunchedEffect(brightness) { onBrightness(brightness) }
    val peekAllowed = canPeek(settings.tapToPeek, day, room, withSunrise, settings.minBrightness, settings.maxBrightness)

    val wallpaperColor = platform.wallpaperColor()
    // Day mode's clock is white with a soft shadow, to read well over its colourful background.
    val color = when {
        day -> Color.White
        // A peek warms night red part way along the sunrise; a clock in its own colour keeps it.
        night -> sunriseColor(NightRed, night = true, peekProgress(sunrise, peek))
        else -> sunriseColor(Color(clockColor(settings, wallpaperColor)), night = false, sunrise)
    }
    val shadow = if (day) Shadow(Color.Black.copy(alpha = 0.35f), Offset(0f, 3f), 14f) else null
    val nightAlpha by animateFloatAsState(if (night) 0.55f + 0.45f * sunrise else 1f, tween(2000), label = "alpha")
    // A peek lifts the opacity at once, not over the night fade's two seconds.
    val contentAlpha = if (night) maxOf(nightAlpha, 0.55f + 0.45f * peekProgress(sunrise, peek)) else nightAlpha

    LaunchedEffect(settings.showWeather, settings.weatherUseDeviceLocation, settings.weatherLat, settings.weatherLon) {
        if (!settings.showWeather) return@LaunchedEffect
        while (true) {
            WeatherRepository.refreshIfStale(settings) { platform.deviceLocation() }
            delay(10 * 60_000L)
        }
    }

    // Day mode's focus timer, kept across rotation: only where it started, so it follows the clock.
    var focus by rememberSaveable(stateSaver = FocusTimerSaver) { mutableStateOf<FocusTimer?>(null) }
    // Leaving day mode stops it, and so does leaving the app, except for the clock's own Settings and alarms.
    var focusOutlivesStop by remember { mutableStateOf(false) }
    LaunchedEffect(day, interactive) { if (!day || !interactive) focus = null }
    LifecycleEventEffect(Lifecycle.Event.ON_STOP) {
        if (!focusOutlivesStop) focus = null
        focusOutlivesStop = false
    }
    // Back on the clock without having left it (say, an unlock prompt was cancelled): the next stop counts.
    LifecycleEventEffect(Lifecycle.Event.ON_RESUME) { focusOutlivesStop = false }
    val openSettings = { focusOutlivesStop = true; onOpenSettings() }
    val openAlarms = { focusOutlivesStop = true; onOpenAlarms() }

    var controlsVisible by remember { mutableStateOf(false) }
    var lastInteraction by remember { mutableIntStateOf(0) }
    LaunchedEffect(controlsVisible, lastInteraction) {
        if (controlsVisible) {
            delay(8_000)
            controlsVisible = false
        }
    }

    val onTap: () -> Unit = {
        when (clockTap(controlsVisible, peeking = peek > 0f, canPeek = peekAllowed)) {
            ClockTap.PEEK -> peekMark = TimeSource.Monotonic.markNow()
            ClockTap.SHOW_CONTROLS -> if (interactive) controlsVisible = true else onDismiss?.invoke()
            ClockTap.HIDE_CONTROLS -> controlsVisible = false
        }
    }
    // Wave to peek: the night clock only, and only with tap to peek on. The sensor is off otherwise.
    WaveEffect(platform.proximity, enabled = settings.waveToPeek && settings.tapToPeek && !day) {
        if (clockWave(controlsVisible, peeking = peek > 0f, canPeek = peekAllowed)) peekMark = TimeSource.Monotonic.markNow()
    }

    BoxWithConstraints(
        Modifier
            .fillMaxSize()
            .background(Color.Black)
            .then(
                if (interactive || onDismiss != null) Modifier.clickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = null,
                    onClick = onTap,
                ) else Modifier
            )
    ) {
        val landscape = maxWidth > maxHeight
        // In day mode the clock gets half the width (landscape) or the top of the screen (portrait),
        // less the burn-in margin and the desk clock panel's padding.
        val clockWidth = if (day) maxWidth * (if (landscape) 0.5f else 1f) * 0.9f - 72.dp else maxWidth
        val clockFontDp: Dp = minOf(
            clockWidth * (if (landscape && !day) 0.62f else 0.85f) / (if (settings.showSeconds || !settings.use24h) 3.3f else 2.8f),
            maxHeight * when {
                day && !landscape -> 0.15f
                day -> if (settings.showCalendarEvents) 0.2f else 0.28f
                landscape -> 0.5f
                else -> 0.3f
            },
        )
        val analogSize = if (day) minOf(clockWidth * 0.7f, maxHeight * (if (landscape) 0.55f else 0.25f))
        else minOf(maxWidth, maxHeight) * 0.55f
        val infoFontDp = (clockFontDp * 0.13f).coerceIn(14.dp, if (day) 22.dp else 30.dp)
        val density = LocalDensity.current
        val clockSize = with(density) { clockFontDp.toSp() }
        val infoSize = with(density) { infoFontDp.toSp() }

        val clock = @Composable {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.alpha(contentAlpha),
            ) {
                when (settings.style) {
                    ClockStyle.DIGITAL -> DigitalClock(now, settings.use24h, settings.showSeconds, settings.font, color, clockSize, shadow)
                    ClockStyle.ANALOG -> AnalogClock(now, settings.showSeconds, color, analogSize)
                }
                Spacer(Modifier.height(infoFontDp * 0.6f))
                InfoBlock(
                    settings, now, color, infoSize, infoFontDp, shadow,
                    hourlyWidth = if (day) clockWidth else null,
                    night = !day,
                )
            }
        }

        if (day) DayBackground(now, settings.dayPhotoVersion, Modifier.fillMaxSize())

        DriftLayout(enabled = settings.burnInProtection, modifier = Modifier.safeDrawingPadding().padding(16.dp)) {
            // Day mode fills the screen and stays up all day: with burn-in protection it leaves a tenth
            // of the screen free, so the drift moves it noticeably.
            if (day) DayLayout(
                landscape, now, settings,
                Modifier.fillMaxSize(if (settings.burnInProtection) 0.9f else 0.96f),
                onOpen = if (interactive) onOpenCalendar else null,
                focus = focus,
                onFocus = if (interactive) { f -> focus = f } else null,
                clock = clock,
            )
            else clock()
        }

        if (interactive) {
            AnimatedVisibility(
                visible = controlsVisible,
                enter = fadeIn(),
                exit = fadeOut(),
                modifier = Modifier.align(Alignment.BottomCenter).safeDrawingPadding().padding(16.dp),
            ) {
                Controls(
                    settings, day, color, openSettings, openAlarms,
                    onInteract = { controlsVisible = true; lastInteraction++ },
                    focusOn = focus != null,
                    onFocus = { focus = if (focus == null) FocusTimer.start(Clock.System.now().toEpochMilliseconds()) else null },
                )
            }
        }
    }
}

@Composable
private fun InfoBlock(
    settings: Settings,
    now: Moment,
    color: Color,
    fontSize: androidx.compose.ui.unit.TextUnit,
    glyphSize: Dp,
    shadow: Shadow? = null,
    /** Day mode's width for the next hours' weather under the weather line; null leaves it out. */
    hourlyWidth: Dp? = null,
    /** The night clock, not Day mode, which draws its own moon. */
    night: Boolean = true,
) {
    val style = TextStyle(color = color.copy(alpha = if (shadow != null) 1f else 0.85f), fontSize = fontSize, shadow = shadow)
    val platform = LocalClockPlatform.current
    val battery = platform.battery()
    val nextAlarm = platform.nextAlarm()
    val weather by WeatherRepository.weather.collectAsStateWithLifecycle()
    val notifications = platform.notificationIcons()
    val radioTitle by platform.radio.title.collectAsStateWithLifecycle()

    val locale = currentLocale()
    if (settings.showDate) {
        val pattern = remember(locale) { DateTimePattern(bestDateTimePattern("EEEEdMMMM", locale), locale) }
        Text(pattern.format(now.local), style = style)
        Spacer(Modifier.height(glyphSize * 0.4f))
    }

    // Weather, forecast, battery and next alarm wrap onto a second line when they don't fit; in a Row
    // the last item was squeezed until it stacked one letter per line.
    FlowRow(
        horizontalArrangement = Arrangement.spacedBy(glyphSize * 1.2f, Alignment.CenterHorizontally),
        verticalArrangement = Arrangement.spacedBy(glyphSize * 0.3f),
        itemVerticalAlignment = Alignment.CenterVertically,
    ) {
        val w = weather
        if (settings.showWeather && w != null) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Item(w.glyph, w.temperatureText(settings.weatherFahrenheit), color, style, glyphSize)
                val pick = if (settings.showForecast) pickForecast(w.days, now.local) else null
                if (pick != null) {
                    // Today: "↑24° ↓12°" after the current temperature. Evening: "Sat ☁ ↑20° ↓11°" for tomorrow.
                    Spacer(Modifier.width(glyphSize * 0.6f))
                    val forecastStyle = style.copy(color = style.color.copy(alpha = style.color.alpha * 0.75f))
                    if (pick.tomorrow) {
                        Text(pick.day.date.dayOfWeek.shortName(locale), style = forecastStyle, softWrap = false)
                        Spacer(Modifier.width(glyphSize * 0.3f))
                        GlyphIcon(pick.day.glyph, forecastStyle.color, glyphSize)
                        Spacer(Modifier.width(glyphSize * 0.3f))
                    }
                    Text(pick.day.highLowText(settings.weatherFahrenheit), style = forecastStyle, softWrap = false)
                }
            }
        }
        val b = battery
        if (settings.showBattery && b != null) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                GlyphIcon(Glyph.BATTERY, style.color, glyphSize, level = b.level)
                if (b.charging) GlyphIcon(Glyph.BOLT, style.color, glyphSize * 0.8f)
                Spacer(Modifier.width(glyphSize * 0.2f))
                Text("${(b.level * 100).toInt()}%", style = style, softWrap = false)
            }
        }
        val a = nextAlarm
        if (settings.showNextAlarm && a != null && a - now.epochMillis < 24 * 3600_000L) {
            val t = Instant.fromEpochMilliseconds(a).toLocalDateTime(now.zone)
            val pattern = remember(locale, settings.use24h) {
                DateTimePattern(bestDateTimePattern(if (settings.use24h) "EEEHHmm" else "EEEhmma", locale), locale)
            }
            // Within 12 hours: "6:30 · in 7 h 20 m" instead of the day and time.
            val countdown = if (settings.showTimeToAlarm) {
                alarmCountdownLabel(Instant.fromEpochMilliseconds(a), now.instant, now.zone, settings.use24h, locale)
            } else null
            Item(Glyph.ALARM, countdown ?: pattern.format(t), color, style, glyphSize)
        }
        if (settings.showMoon && night) {
            val cycle = moonCycle(now.instant)
            Row(verticalAlignment = Alignment.CenterVertically) {
                GlyphIcon(Glyph.MOON_PHASE, style.color, glyphSize, level = cycle.toFloat())
                Spacer(Modifier.width(glyphSize * 0.3f))
                Text(MoonPhase.of(cycle).label(locale), style = style, softWrap = false)
            }
        }
    }

    val hours = weather?.hours.orEmpty()
    if (hourlyWidth != null && settings.showWeather && settings.dayHourlyWeather && hours.isNotEmpty()) {
        Spacer(Modifier.height(glyphSize * 0.4f))
        HourlyStrip(
            hours, now, settings.use24h, settings.weatherFahrenheit, locale,
            style.copy(color = style.color.copy(alpha = style.color.alpha * 0.75f), fontSize = fontSize * 0.85f),
            glyphSize * 0.85f, Modifier.widthIn(max = hourlyWidth),
        )
    }

    if (radioTitle != null) {
        Spacer(Modifier.height(glyphSize * 0.4f))
        Text("♪ $radioTitle", style = style.copy(fontSize = fontSize * 0.8f))
    }

    if (settings.showNotifications && notifications.isNotEmpty()) {
        Spacer(Modifier.height(glyphSize * 0.6f))
        NotificationRow(notifications, style.color, glyphSize)
    }
}

@Composable
private fun Item(glyph: Glyph, text: String, color: Color, style: TextStyle, size: Dp) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        GlyphIcon(glyph, style.color, size)
        Spacer(Modifier.width(size * 0.3f))
        Text(text, style = style, softWrap = false)
    }
}

@Composable
private fun NotificationRow(icons: List<NotificationIcon>, color: Color, size: Dp) {
    val px = with(LocalDensity.current) { size.roundToPx() }
    Row(horizontalArrangement = Arrangement.spacedBy(size * 0.5f)) {
        icons.forEach { n ->
            val bitmap = remember(n.key, px) { n.image(px) }
            if (bitmap != null) {
                Image(bitmap, contentDescription = null, colorFilter = ColorFilter.tint(color), modifier = Modifier.size(size))
            }
        }
    }
}

private val SLEEP_STEPS = listOf(0, 15, 30, 60, 90)

/** Keeps the focus timer as a few numbers, which a saved instance state can hold. */
private val FocusTimerSaver = Saver<FocusTimer?, LongArray>(
    save = { it?.toSaved()?.toLongArray() },
    restore = { focusTimerFromSaved(it.toList()) },
)

@Composable
private fun Controls(
    settings: Settings,
    day: Boolean,
    color: Color,
    onOpenSettings: () -> Unit,
    onOpenAlarms: () -> Unit,
    onInteract: () -> Unit,
    /** Whether day mode's focus timer is running; its button shows only in day mode. */
    focusOn: Boolean,
    onFocus: () -> Unit,
) {
    val radio = LocalClockPlatform.current.radio
    val radioOn by radio.isOn.collectAsStateWithLifecycle()
    val sleepAt by radio.sleepAt.collectAsStateWithLifecycle()
    val error by radio.error.collectAsStateWithLifecycle()
    val colors = if (day) ControlDefaults.Day else ControlDefaults.night(color)

    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        error?.let {
            Text(
                it,
                color = if (day) Color.White else color.copy(alpha = 0.8f),
                fontSize = 14.sp,
                modifier = Modifier
                    .padding(bottom = 8.dp)
                    .then(if (day) Modifier.background(DeskNavy, RoundedCornerShape(12.dp)).padding(horizontal = 12.dp, vertical = 6.dp) else Modifier),
            )
        }
        FlowRow(
            horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.CenterHorizontally),
            verticalArrangement = Arrangement.spacedBy(8.dp),
            itemVerticalAlignment = Alignment.CenterVertically,
        ) {
            // Quick dimming switch: moon = dims with the room and goes red at night; sun = normal brightness.
            ControlButton(
                stringResource(Res.string.clock_dimming), colors,
                glyph = if (settings.dimEnabled) Glyph.MOON else Glyph.SUN,
                checked = settings.dimEnabled,
                onClick = {
                    onInteract()
                    SettingsStore.update { it.copy(dimEnabled = !it.dimEnabled) }
                },
            )
            ControlButton(
                if (day) stringResource(Res.string.clock_clock_only) else stringResource(Res.string.settings_day_mode), colors,
                glyph = if (day) Glyph.CLOCK else Glyph.SUN,
                // On a schedule this overrides it until the next start or end.
                onClick = { onInteract(); SettingsStore.update { it.withDayModeToggled(Clock.System.now(), TimeZone.currentSystemDefault()) } },
            )
            if (day) {
                ControlButton(
                    stringResource(Res.string.daypage_focus), colors,
                    glyph = Glyph.HOURGLASS, checked = focusOn,
                    onClick = { onInteract(); onFocus() },
                )
            }
            ControlButton(stringResource(Res.string.settings_alarms), colors, glyph = Glyph.ALARM, onClick = onOpenAlarms)
            ControlButton(
                stringResource(Res.string.clock_radio), colors,
                glyph = Glyph.RADIO, checked = radioOn,
                onClick = { onInteract(); radio.toggle(settings.currentStation, settings.radioSleepMinutes) },
            )
            if (radioOn) {
                val remaining = sleepAt?.let { ((it - Clock.System.now().toEpochMilliseconds()) / 60_000L).coerceAtLeast(0) + 1 }
                ControlButton(
                    if (remaining == null) stringResource(Res.string.clock_sleep_off) else stringResource(Res.string.clock_sleep_minutes, remaining), colors,
                    glyph = Glyph.SLEEP, checked = remaining != null,
                    onClick = {
                        onInteract()
                        // Cycle off -> 15 -> 30 -> 60 -> 90 -> off.
                        val idx = remaining?.let { r -> SLEEP_STEPS.indexOfFirst { it >= r } } ?: 0
                        radio.setSleepTimer(SLEEP_STEPS[(idx + 1) % SLEEP_STEPS.size])
                    },
                )
            }
            ControlButton(stringResource(Res.string.settings_title), colors, glyph = Glyph.GEAR, onClick = onOpenSettings)
        }
    }
}

/**
 * Burn-in protection: places its content somewhere inside the available space and moves it to a
 * new random spot every couple of minutes, gliding slowly so the movement isn't distracting.
 */
@Composable
private fun DriftLayout(enabled: Boolean, modifier: Modifier = Modifier, content: @Composable () -> Unit) {
    var target by remember { mutableStateOf(0f to 0f) }
    LaunchedEffect(enabled) {
        target = 0f to 0f
        if (!enabled) return@LaunchedEffect
        while (true) {
            delay(120_000)
            target = Random.nextFloat() * 2 - 1 to Random.nextFloat() * 2 - 1
        }
    }
    val fx by animateFloatAsState(target.first, tween(20_000), label = "fx")
    val fy by animateFloatAsState(target.second, tween(20_000), label = "fy")

    Layout(content, modifier.fillMaxSize()) { measurables, constraints ->
        val loose = Constraints(maxWidth = constraints.maxWidth, maxHeight = constraints.maxHeight)
        val placeable = measurables.first().measure(loose)
        layout(constraints.maxWidth, constraints.maxHeight) {
            val slackX = (constraints.maxWidth - placeable.width).coerceAtLeast(0)
            val slackY = (constraints.maxHeight - placeable.height).coerceAtLeast(0)
            // Keep within the middle ~70% of the slack so the clock never hugs the edge.
            val x = slackX / 2 + (slackX / 2 * 0.7f * fx).toInt()
            val y = slackY / 2 + (slackY / 2 * 0.7f * fy).toInt()
            placeable.place(x, y)
        }
    }
}
