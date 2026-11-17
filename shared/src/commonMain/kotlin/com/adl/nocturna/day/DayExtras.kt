package com.adl.nocturna.day

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import com.adl.nocturna.data.Settings
import com.adl.nocturna.data.TodoItem
import com.adl.nocturna.resources.*
import kotlin.math.PI
import kotlin.math.sin
import kotlin.random.Random
import org.jetbrains.compose.resources.stringResource

/*
 * The smaller pieces of day mode added around the desk clock and the note: the glance line, the reminder
 * and battery tags, the to-do list, and the weather and holiday overlay over the background.
 */

/** Rain on the way, sunset, the UV index, the air quality and the countdowns, in the desk clock under its info line. */
@Composable
internal fun GlanceLine(items: List<String>, fontSize: TextUnit) {
    val line = with(LocalDensity.current) { fontSize.toDp() }
    FlowRow(
        horizontalArrangement = Arrangement.spacedBy(line * 0.9f, Alignment.CenterHorizontally),
        modifier = Modifier.padding(top = 4.dp),
    ) {
        items.forEach { text ->
            Text(
                text,
                style = TextStyle(color = Color.White.copy(alpha = 0.85f), fontSize = fontSize * 0.85f, fontFeatureSettings = "tnum", textAlign = TextAlign.Center),
                maxLines = 1,
            )
        }
    }
}

/** A nudge in the desk clock, e.g. "Time for a glass of water"; a tap puts it away. */
@Composable
internal fun NudgeTag(text: String, fontSize: TextUnit, onDismiss: (() -> Unit)?) {
    Text(
        text,
        style = TextStyle(color = NudgeInk, fontSize = fontSize * 0.85f, fontWeight = FontWeight.Medium, textAlign = TextAlign.Center),
        maxLines = 2,
        overflow = TextOverflow.Ellipsis,
        modifier = Modifier
            .padding(top = 6.dp)
            .clip(RoundedCornerShape(50))
            .background(NudgeColor)
            .clickable(enabled = onDismiss != null, onClickLabel = stringResource(Res.string.day_dismiss)) { onDismiss?.invoke() }
            .padding(horizontal = 14.dp, vertical = 5.dp),
    )
}

private val NudgeColor = Color(0xFFFFE08A)
private val NudgeInk = Color(0xFF3A2F10)

/** A reminder's state: whether it's showing, and how to put it away. */
@Stable
internal class ReminderSlot(val showing: Boolean, val dismiss: () -> Unit)

/**
 * A reminder every [intervalMinutes] (0 is off) while day mode is up, counted from when it last showed or,
 * at first, from when day mode came up. While [held] it waits. When it comes due it shows for a few
 * minutes and [onDue] plays its chime. Kept across rotation.
 */
@Composable
internal fun rememberReminder(intervalMinutes: Int, held: Boolean, now: Long, onDue: () -> Unit): ReminderSlot {
    // Changing how often starts the count again.
    var since by rememberSaveable(intervalMinutes) { mutableLongStateOf(now) }
    var shownAt by rememberSaveable { mutableStateOf<Long?>(null) }
    val due = reminderDue(since, now, intervalMinutes, held)
    val currentOnDue by rememberUpdatedState(onDue)
    LaunchedEffect(due) {
        if (due) {
            since = now
            shownAt = now
            currentOnDue()
        }
    }
    return ReminderSlot(intervalMinutes > 0 && reminderShowing(shownAt, now)) { shownAt = null }
}

/**
 * The to-do list on the sticky note: a box to tick and the text, crossed off once done. [onToggle] ticks an
 * item; [onAdd] adds one through a small dialog. Both are null where nothing can be tapped.
 */
@Composable
internal fun TodoList(
    items: List<TodoItem>,
    ink: Color,
    accent: Color,
    fontSize: TextUnit,
    onToggle: ((Int) -> Unit)?,
    onAdd: ((String) -> Unit)?,
    modifier: Modifier = Modifier,
) {
    var adding by remember { mutableStateOf(false) }
    if (adding && onAdd != null) {
        AddTodoDialog(onDismiss = { adding = false }) { text -> adding = false; onAdd(text) }
    }
    val line = with(LocalDensity.current) { fontSize.toDp() }
    val faint = TextStyle(color = ink.copy(alpha = 0.7f), fontSize = fontSize * 0.9f)
    // Only whole rows, the add button last of all: a short note says how many more there are instead.
    val canAdd = onAdd != null && items.size < Settings.MAX_TODOS
    FitColumn(0.dp, modifier, more = { n ->
        // The add button is the last row: left out, it isn't one more to-do.
        val hidden = n - if (canAdd) 1 else 0
        if (hidden > 0) Text(stringResource(Res.string.day_more, hidden), style = faint)
    }) {
        if (items.isEmpty()) {
            Text(stringResource(Res.string.day_todo_empty), style = faint)
        }
        items.forEachIndexed { i, item ->
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(6.dp))
                    .clickable(
                        enabled = onToggle != null,
                        onClickLabel = stringResource(if (item.done) Res.string.day_todo_untick else Res.string.day_todo_tick),
                    ) { onToggle?.invoke(i) }
                    .padding(vertical = 2.dp),
            ) {
                TickBox(item.done, ink, accent, Modifier.size(line * 0.85f))
                Spacer(Modifier.width(line * 0.5f))
                Text(
                    item.text,
                    style = TextStyle(
                        color = ink.copy(alpha = if (item.done) 0.45f else 1f),
                        fontSize = fontSize * 0.95f,
                        textDecoration = if (item.done) TextDecoration.LineThrough else null,
                    ),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        }
        if (canAdd) {
            Text(
                stringResource(Res.string.day_todo_add),
                style = TextStyle(color = accent, fontSize = fontSize * 0.85f, fontWeight = FontWeight.Bold),
                modifier = Modifier
                    .padding(top = 2.dp)
                    .clip(RoundedCornerShape(6.dp))
                    .clickable { adding = true }
                    .padding(vertical = 2.dp),
            )
        }
    }
}

/** A hand-drawn box, with a tick in it once [done]. */
@Composable
private fun TickBox(done: Boolean, ink: Color, accent: Color, modifier: Modifier) {
    Canvas(modifier) {
        val s = size.minDimension
        val stroke = s * 0.11f
        drawRoundRect(ink.copy(alpha = 0.6f), Offset(stroke / 2, stroke / 2), Size(s - stroke, s - stroke), CornerRadius(s * 0.2f), style = Stroke(stroke))
        if (done) {
            val tick = Path().apply {
                moveTo(s * 0.22f, s * 0.52f)
                lineTo(s * 0.43f, s * 0.74f)
                lineTo(s * 0.86f, s * 0.18f)
            }
            drawPath(tick, accent, style = Stroke(stroke * 1.4f, cap = StrokeCap.Round))
        }
    }
}

/** Asks for a new to-do's text. */
@Composable
internal fun AddTodoDialog(onDismiss: () -> Unit, onAdd: (String) -> Unit) {
    var text by remember { mutableStateOf("") }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(Res.string.day_todo_new)) },
        text = {
            OutlinedTextField(
                text, { text = it.take(Settings.MAX_TODO_LENGTH) },
                singleLine = true,
                placeholder = { Text(stringResource(Res.string.day_todo_placeholder)) },
                modifier = Modifier.fillMaxWidth(),
            )
        },
        confirmButton = { TextButton(onClick = { onAdd(text) }, enabled = text.isNotBlank()) { Text(stringResource(Res.string.day_todo_add_button)) } },
        dismissButton = { TextButton(onClick = onDismiss) { Text(stringResource(Res.string.cancel)) } },
    )
}

/**
 * What moves over day mode's background, behind the clock and the page: rain for the scene's weather,
 * and the holiday's confetti, hearts, papel picado, bats or lights (fewer hearts and bats on the days around
 * the holiday than on the day itself). Drawn on a canvas of its own, so the scene
 * under it isn't redrawn every frame; nothing is drawn, and no frames run, when there is nothing to show.
 */
@Composable
internal fun DayOverlay(weather: SceneWeather?, holiday: HolidayDay?, modifier: Modifier = Modifier) {
    val rain = weather == SceneWeather.RAIN || weather == SceneWeather.STORM
    val decoration = holiday?.holiday?.decoration()
    val full = holiday?.theDay ?: true
    if (!rain && decoration == null) return
    val transition = rememberInfiniteTransition(label = "overlay")
    // One slow clock for everything: a minute round, in seconds.
    val t by transition.animateFloat(0f, OVERLAY_LOOP_SECONDS, infiniteRepeatable(tween((OVERLAY_LOOP_SECONDS * 1000).toInt(), easing = LinearEasing), RepeatMode.Restart), label = "t")
    Canvas(modifier) {
        if (rain) rain(t, heavy = weather == SceneWeather.STORM)
        when (decoration) {
            Decoration.CONFETTI -> confetti(t)
            Decoration.HEARTS -> hearts(t, if (full) 18 else 7)
            Decoration.PAPEL_PICADO_MEXICO -> papelPicado(t, listOf(Color(0xFF006847), Color.White, Color(0xFFCE1126)))
            Decoration.PAPEL_PICADO_MUERTOS -> papelPicado(t, listOf(Color(0xFFE6007E), Color(0xFFFF8C00), Color(0xFF7B2CBF), Color(0xFFFFD000), Color(0xFF00A86B)))
            Decoration.LIGHTS -> lights(t)
            Decoration.BATS -> bats(t, if (full) 6 else 2)
            null -> Unit
        }
    }
}

private const val OVERLAY_LOOP_SECONDS = 60f

/** What a holiday puts over the background. */
internal enum class Decoration { CONFETTI, HEARTS, PAPEL_PICADO_MEXICO, PAPEL_PICADO_MUERTOS, LIGHTS, BATS }

internal fun Holiday.decoration(): Decoration = when (this) {
    Holiday.BIRTHDAY, Holiday.NEW_YEAR -> Decoration.CONFETTI
    Holiday.VALENTINES -> Decoration.HEARTS
    Holiday.MEXICAN_INDEPENDENCE -> Decoration.PAPEL_PICADO_MEXICO
    Holiday.DAY_OF_THE_DEAD -> Decoration.PAPEL_PICADO_MUERTOS
    Holiday.CHRISTMAS -> Decoration.LIGHTS
    Holiday.HALLOWEEN -> Decoration.BATS
}

/** Slanted streaks falling across the screen, more of them in a storm. */
private fun DrawScope.rain(t: Float, heavy: Boolean) {
    val rnd = Random(11)
    val count = if (heavy) 180 else 110
    val len = size.minDimension * 0.035f
    repeat(count) {
        val x0 = rnd.nextFloat()
        val speed = 0.9f + rnd.nextFloat() * 0.5f
        val phase = rnd.nextFloat()
        // About a second and a half from top to bottom.
        val y = ((phase + t * speed * 0.7f) % 1f) * (size.height + len) - len
        val x = x0 * size.width - (y / size.height) * size.width * 0.05f
        drawLine(Color(0xFFDDE7F2), Offset(x, y), Offset(x - len * 0.18f, y + len), size.minDimension * 0.0025f, StrokeCap.Round, alpha = 0.55f)
    }
}

private val ConfettiColors = listOf(Color(0xFFFF4F79), Color(0xFFFFC21C), Color(0xFF2FBF71), Color(0xFF3D8BFF), Color(0xFFB06BFF), Color(0xFFFF8A3D))

/** Little paper squares tumbling down slowly. */
private fun DrawScope.confetti(t: Float) {
    val rnd = Random(23)
    val piece = size.minDimension * 0.012f
    repeat(70) {
        val x0 = rnd.nextFloat()
        val phase = rnd.nextFloat()
        val speed = 0.6f + rnd.nextFloat() * 0.6f
        val color = ConfettiColors[rnd.nextInt(ConfettiColors.size)]
        val spin = rnd.nextFloat() * 360f
        // A trip down the screen takes about 20 seconds; the pieces flutter side to side on the way.
        val y = ((phase + t / 20f * speed) % 1f) * (size.height + piece * 4) - piece * 2
        val x = x0 * size.width + sin((t * 0.8f + phase * 10f).toDouble()).toFloat() * piece * 2
        rotate(spin + t * 90f * speed, Offset(x, y)) {
            drawRect(color, Offset(x - piece / 2, y - piece * 0.3f), Size(piece, piece * 0.6f), alpha = 0.9f)
        }
    }
}

/** [count] hearts floating up slowly from the bottom. */
private fun DrawScope.hearts(t: Float, count: Int) {
    val rnd = Random(37)
    repeat(count) {
        val x0 = rnd.nextFloat()
        val phase = rnd.nextFloat()
        val r = size.minDimension * (0.012f + rnd.nextFloat() * 0.012f)
        val y = size.height - ((phase + t / 30f) % 1f) * (size.height + r * 4) + r * 2
        val x = x0 * size.width + sin((t * 0.5f + phase * 8f).toDouble()).toFloat() * r * 2
        heart(Offset(x, y), r, if (it % 3 == 0) Color(0xFFFF8FB5) else Color(0xFFE8457A))
    }
}

private fun DrawScope.heart(c: Offset, r: Float, color: Color) {
    drawCircle(color, r * 0.55f, Offset(c.x - r * 0.45f, c.y - r * 0.2f), alpha = 0.8f)
    drawCircle(color, r * 0.55f, Offset(c.x + r * 0.45f, c.y - r * 0.2f), alpha = 0.8f)
    val p = Path().apply {
        moveTo(c.x - r * 0.98f, c.y - r * 0.05f)
        lineTo(c.x, c.y + r * 1.0f)
        lineTo(c.x + r * 0.98f, c.y - r * 0.05f)
        close()
    }
    drawPath(p, color, alpha = 0.8f)
}

/** A string of papel picado across the top, swaying in the breeze and drifting along. */
private fun DrawScope.papelPicado(t: Float, colors: List<Color>) {
    val flagW = size.width / 9f
    val flagH = flagW * 1.1f
    val top = size.height * 0.02f
    val sag = flagH * 0.35f
    // The string sags in the middle.
    fun stringY(x: Float): Float {
        val fx = (x / size.width).coerceIn(0f, 1f)
        return top + sag * 4 * fx * (1 - fx)
    }
    val string = Path().apply {
        moveTo(0f, stringY(0f))
        for (i in 1..20) lineTo(size.width * i / 20f, stringY(size.width * i / 20f))
    }
    drawPath(string, Color.White.copy(alpha = 0.7f), style = Stroke(2f))
    // The flags drift along by one round of colours a minute, so none stays on the same pixels.
    val shift = (t / OVERLAY_LOOP_SECONDS) * flagW * colors.size
    for (i in -colors.size..10) {
        val x = i * flagW + shift
        if (x > size.width || x + flagW < 0) continue
        val y = stringY(x + flagW / 2)
        val sway = sin((t * 0.9f + i * 0.7f).toDouble()).toFloat() * 4f
        val color = colors[((i % colors.size) + colors.size) % colors.size]
        rotate(sway, Offset(x + flagW / 2, y)) {
            val flag = Path().apply {
                moveTo(x + flagW * 0.06f, y)
                lineTo(x + flagW * 0.94f, y)
                // A zigzag hem.
                for (z in 0..5) lineTo(x + flagW * (0.94f - z * 0.176f), y + flagH * (if (z % 2 == 0) 0.8f else 0.95f))
                close()
            }
            drawPath(flag, color, alpha = 0.85f)
            // Cut-out holes, as in the real paper.
            for (h in 0 until 3) drawCircle(Color.Black.copy(alpha = 0.18f), flagW * 0.07f, Offset(x + flagW * (0.28f + h * 0.22f), y + flagH * 0.35f))
            drawCircle(Color.Black.copy(alpha = 0.18f), flagW * 0.1f, Offset(x + flagW * 0.5f, y + flagH * 0.6f))
        }
    }
}

private val LightColors = listOf(Color(0xFFFF4040), Color(0xFFFFD23F), Color(0xFF3DDC84), Color(0xFF4DA3FF), Color(0xFFFF8AD8))

/** A garland of coloured lights swooping along the top, twinkling one after another and drifting along. */
private fun DrawScope.lights(t: Float) {
    val gap = size.width / 14f
    val top = size.height * 0.03f
    fun wireY(x: Float): Float {
        val s = sin(x / size.width * PI * 3).toFloat()
        return top + gap * 0.6f * s * s
    }
    val wire = Path().apply {
        moveTo(0f, wireY(0f))
        for (i in 1..42) lineTo(size.width * i / 42f, wireY(size.width * i / 42f))
    }
    drawPath(wire, Color(0xFF2E5E3A), style = Stroke(3f))
    val shift = (t / OVERLAY_LOOP_SECONDS) * gap * LightColors.size
    for (i in -LightColors.size..15) {
        val x = i * gap + shift
        if (x < -gap || x > size.width + gap) continue
        val y = wireY(x)
        val color = LightColors[((i % LightColors.size) + LightColors.size) % LightColors.size]
        val glow = 0.55f + 0.45f * sin((t * 2f + i * 1.3f).toDouble()).toFloat()
        drawCircle(color.copy(alpha = 0.25f * glow), gap * 0.22f, Offset(x, y + gap * 0.2f))
        drawRoundRect(color, Offset(x - gap * 0.07f, y + gap * 0.08f), Size(gap * 0.14f, gap * 0.22f), CornerRadius(gap * 0.07f), alpha = 0.6f + 0.4f * glow)
    }
}

/** [count] bats flapping across the sky. */
private fun DrawScope.bats(t: Float, count: Int) {
    val rnd = Random(53)
    repeat(count) {
        val phase = rnd.nextFloat()
        val y0 = size.height * (0.08f + rnd.nextFloat() * 0.3f)
        val w = size.minDimension * (0.03f + rnd.nextFloat() * 0.02f)
        val x = ((phase + t / 25f) % 1f) * (size.width + w * 4) - w * 2
        val y = y0 + sin((t + phase * 9f).toDouble()).toFloat() * w
        val flap = sin((t * 9f + phase * 20f).toDouble()).toFloat()
        val wing = Path().apply {
            moveTo(x - w, y - w * 0.3f * flap)
            quadraticTo(x - w * 0.5f, y + w * 0.25f, x, y)
            quadraticTo(x + w * 0.5f, y + w * 0.25f, x + w, y - w * 0.3f * flap)
            quadraticTo(x + w * 0.5f, y - w * 0.05f, x, y - w * 0.2f)
            quadraticTo(x - w * 0.5f, y - w * 0.05f, x - w, y - w * 0.3f * flap)
            close()
        }
        drawPath(wing, Color(0xFF2B2233), alpha = 0.85f)
    }
}
