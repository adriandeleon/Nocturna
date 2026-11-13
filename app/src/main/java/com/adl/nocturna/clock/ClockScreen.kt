package com.adl.nocturna.clock

import android.text.format.DateFormat
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.BorderStroke
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
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.ButtonDefaults
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.Layout
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.graphics.drawable.toBitmap
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.adl.nocturna.data.ClockStyle
import com.adl.nocturna.data.Settings
import com.adl.nocturna.data.SettingsStore
import com.adl.nocturna.notify.NotifIcon
import com.adl.nocturna.notify.NotificationIconService
import com.adl.nocturna.radio.Radio
import com.adl.nocturna.ui.Glyph
import com.adl.nocturna.ui.GlyphIcon
import com.adl.nocturna.ui.NightRed
import com.adl.nocturna.weather.WeatherRepository
import kotlinx.coroutines.delay
import java.time.Instant
import java.time.ZoneId
import java.time.ZonedDateTime
import java.time.format.DateTimeFormatter
import com.adl.nocturna.ui.currentLocale
import kotlin.math.log10
import kotlin.random.Random

/**
 * The bedside clock. Shared by [ClockActivity] (interactive, with a tap-to-show control bar)
 * and the screen saver (display only).
 *
 * @param onBrightness receives the window brightness to apply (0..1), or -1 for "system default".
 */
@Composable
fun ClockScreen(
    interactive: Boolean,
    onBrightness: (Float) -> Unit,
    onOpenSettings: () -> Unit = {},
    onOpenAlarms: () -> Unit = {},
) {
    val context = LocalContext.current
    val settings by SettingsStore.settings.collectAsStateWithLifecycle()
    val now by rememberNow()
    val lux by rememberAmbientLux()

    // Night mode with hysteresis so it doesn't flicker around the threshold.
    var night by remember { mutableStateOf(false) }
    val currentLux = lux
    night = when {
        !settings.dimEnabled || !settings.nightModeEnabled -> false
        currentLux == null -> now.hour >= 22 || now.hour < 7 // no light sensor: go by the clock
        night -> currentLux < settings.nightModeLux * 2.5f
        else -> currentLux < settings.nightModeLux
    }

    val brightness = when {
        night -> settings.minBrightness
        !settings.dimEnabled || !settings.autoBrightness -> -1f
        currentLux == null -> -1f
        else -> {
            val f = (log10(1f + currentLux) / log10(1f + 1000f)).coerceIn(0f, 1f)
            settings.minBrightness + (settings.maxBrightness - settings.minBrightness) * f
        }
    }
    LaunchedEffect(brightness) { onBrightness(brightness) }

    val color = if (night) NightRed else Color(settings.color)
    val contentAlpha by animateFloatAsState(if (night) 0.55f else 1f, tween(2000), label = "alpha")

    LaunchedEffect(settings.showWeather, settings.weatherUseDeviceLocation, settings.weatherLat, settings.weatherLon) {
        if (!settings.showWeather) return@LaunchedEffect
        while (true) {
            WeatherRepository.refreshIfStale(context, settings)
            delay(10 * 60_000L)
        }
    }

    var controlsVisible by remember { mutableStateOf(false) }
    LaunchedEffect(controlsVisible) {
        if (controlsVisible) {
            delay(8_000)
            controlsVisible = false
        }
    }

    BoxWithConstraints(
        Modifier
            .fillMaxSize()
            .background(Color.Black)
            .then(
                if (interactive) Modifier.clickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = null,
                ) { controlsVisible = !controlsVisible } else Modifier
            )
    ) {
        val landscape = maxWidth > maxHeight
        val clockFontDp: Dp = minOf(
            maxWidth * (if (landscape) 0.62f else 0.85f) / (if (settings.showSeconds || !settings.use24h) 3.3f else 2.8f),
            maxHeight * (if (landscape) 0.5f else 0.3f),
        )
        val analogSize = minOf(maxWidth, maxHeight) * 0.55f
        val infoFontDp = (clockFontDp * 0.13f).coerceIn(14.dp, 30.dp)
        val density = LocalDensity.current
        val clockSize = with(density) { clockFontDp.toSp() }
        val infoSize = with(density) { infoFontDp.toSp() }

        DriftLayout(enabled = settings.burnInProtection, modifier = Modifier.safeDrawingPadding().padding(16.dp)) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.alpha(contentAlpha),
            ) {
                when (settings.style) {
                    ClockStyle.DIGITAL -> DigitalClock(now, settings.use24h, settings.showSeconds, settings.font, color, clockSize)
                    ClockStyle.ANALOG -> AnalogClock(now, settings.showSeconds, color, analogSize)
                }
                Spacer(Modifier.height(infoFontDp * 0.6f))
                InfoBlock(settings, now, color, infoSize, infoFontDp)
            }
        }

        if (interactive) {
            AnimatedVisibility(
                visible = controlsVisible,
                enter = fadeIn(),
                exit = fadeOut(),
                modifier = Modifier.align(Alignment.BottomCenter).safeDrawingPadding().padding(16.dp),
            ) {
                Controls(settings, color, onOpenSettings, onOpenAlarms, onInteract = { controlsVisible = true })
            }
        }
    }
}

@Composable
private fun InfoBlock(settings: Settings, now: ZonedDateTime, color: Color, fontSize: androidx.compose.ui.unit.TextUnit, glyphSize: Dp) {
    val style = TextStyle(color = color.copy(alpha = 0.85f), fontSize = fontSize)
    val battery by rememberBattery()
    val nextAlarm by rememberNextAlarm()
    val weather by WeatherRepository.weather.collectAsStateWithLifecycle()
    val notifications by NotificationIconService.icons.collectAsStateWithLifecycle()
    val radioTitle by Radio.title.collectAsStateWithLifecycle()

    val locale = currentLocale()
    if (settings.showDate) {
        val pattern = DateFormat.getBestDateTimePattern(locale, "EEEEdMMMM")
        Text(now.format(DateTimeFormatter.ofPattern(pattern, locale)), style = style)
        Spacer(Modifier.height(glyphSize * 0.4f))
    }

    Row(horizontalArrangement = Arrangement.spacedBy(glyphSize * 1.2f), verticalAlignment = Alignment.CenterVertically) {
        val w = weather
        if (settings.showWeather && w != null) {
            Item(w.glyph, w.temperatureText(settings.weatherFahrenheit), color, style, glyphSize)
        }
        val b = battery
        if (settings.showBattery && b != null) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                GlyphIcon(Glyph.BATTERY, style.color, glyphSize, level = b.level)
                if (b.charging) GlyphIcon(Glyph.BOLT, style.color, glyphSize * 0.8f)
                Spacer(Modifier.width(glyphSize * 0.2f))
                Text("${(b.level * 100).toInt()}%", style = style)
            }
        }
        val a = nextAlarm
        if (settings.showNextAlarm && a != null && a - System.currentTimeMillis() < 24 * 3600_000L) {
            val t = Instant.ofEpochMilli(a).atZone(ZoneId.systemDefault())
            val pattern = DateFormat.getBestDateTimePattern(locale, if (settings.use24h) "EEEHHmm" else "EEEhmma")
            Item(Glyph.ALARM, t.format(DateTimeFormatter.ofPattern(pattern, locale)), color, style, glyphSize)
        }
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
        Text(text, style = style)
    }
}

@Composable
private fun NotificationRow(icons: List<NotifIcon>, color: Color, size: Dp) {
    val context = LocalContext.current
    val px = with(LocalDensity.current) { size.roundToPx() }
    Row(horizontalArrangement = Arrangement.spacedBy(size * 0.5f)) {
        icons.forEach { n ->
            val bitmap = remember(n.key, px) {
                runCatching { n.icon.loadDrawable(context)?.toBitmap(px, px)?.asImageBitmap() }.getOrNull()
            }
            if (bitmap != null) {
                Image(bitmap, contentDescription = null, colorFilter = ColorFilter.tint(color), modifier = Modifier.size(size))
            }
        }
    }
}

private val SLEEP_STEPS = listOf(0, 15, 30, 60, 90)

@Composable
private fun Controls(
    settings: Settings,
    color: Color,
    onOpenSettings: () -> Unit,
    onOpenAlarms: () -> Unit,
    onInteract: () -> Unit,
) {
    val context = LocalContext.current
    val radioOn by Radio.isOn.collectAsStateWithLifecycle()
    val sleepAt by Radio.sleepAt.collectAsStateWithLifecycle()
    val error by Radio.error.collectAsStateWithLifecycle()
    val buttonColors = ButtonDefaults.outlinedButtonColors(contentColor = color)
    val border = BorderStroke(1.dp, color.copy(alpha = 0.5f))

    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        error?.let { Text(it, color = color.copy(alpha = 0.8f), fontSize = 14.sp, modifier = Modifier.padding(bottom = 8.dp)) }
        FlowRow(
            horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.CenterHorizontally),
            itemVerticalAlignment = Alignment.CenterVertically,
        ) {
            // Quick dimming switch: moon = dims with the room and goes red at night; sun = normal brightness.
            OutlinedButton(
                onClick = {
                    onInteract()
                    SettingsStore.update { it.copy(dimEnabled = !it.dimEnabled) }
                },
                colors = buttonColors,
                border = border,
                shape = CircleShape,
                contentPadding = PaddingValues(0.dp),
                modifier = Modifier
                    .size(40.dp)
                    .semantics { contentDescription = if (settings.dimEnabled) "Dimming on" else "Dimming off" },
            ) { GlyphIcon(if (settings.dimEnabled) Glyph.MOON else Glyph.SUN, color, 24.dp) }
            OutlinedButton(onClick = onOpenAlarms, colors = buttonColors, border = border) { Text("Alarms") }
            OutlinedButton(
                onClick = { onInteract(); Radio.toggle(context, settings.currentStation) },
                colors = buttonColors, border = border,
            ) { Text(if (radioOn) "■  Radio" else "▶  Radio") }
            if (radioOn) {
                val remaining = sleepAt?.let { ((it - System.currentTimeMillis()) / 60_000L).coerceAtLeast(0) + 1 }
                OutlinedButton(
                    onClick = {
                        onInteract()
                        // Cycle off -> 15 -> 30 -> 60 -> 90 -> off.
                        val idx = remaining?.let { r -> SLEEP_STEPS.indexOfFirst { it >= r } } ?: 0
                        Radio.setSleepTimer(SLEEP_STEPS[(idx + 1) % SLEEP_STEPS.size])
                    },
                    colors = buttonColors, border = border,
                ) { Text(if (remaining == null) "Sleep: off" else "Sleep: ${remaining}m") }
            }
            OutlinedButton(onClick = onOpenSettings, colors = buttonColors, border = border) { Text("Settings") }
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
