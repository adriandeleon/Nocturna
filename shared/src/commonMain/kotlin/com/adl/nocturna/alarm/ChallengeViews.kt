package com.adl.nocturna.alarm

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.waitForUpOrCancellation
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.runtime.withFrameMillis
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.ProgressBarRangeInfo
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.progressBarRangeInfo
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/*
 * The wake-up screen's challenges (see [DismissChallenge]). Both report a problem through `onFailed`
 * rather than throw, so the screen can fall back to a plain Dismiss button and the alarm can always be stopped.
 */

/**
 * *Solve a sum*: [needed] sums from [seed], one at a time, typed on a numeric keypad. A wrong answer brings a
 * new sum, never a wait. TalkBack reads each sum as it appears ("37 plus 48 equals?").
 */
@Composable
fun SumChallenge(
    needed: Int,
    seed: Long,
    accent: Color,
    onSolved: () -> Unit,
    onFailed: (Throwable) -> Unit,
    modifier: Modifier = Modifier,
) {
    var progress by remember(seed, needed) { mutableStateOf(SumProgress.start(needed)) }
    var typed by remember(seed, needed) { mutableStateOf("") }
    val shown = remember(seed, progress.round) { runCatching { progress.sum(seed) } }
    val sum = shown.getOrNull()
    if (sum == null) {
        LaunchedEffect(shown) { onFailed(shown.exceptionOrNull() ?: IllegalStateException("No sum")) }
        return
    }
    val muted = accent.copy(alpha = 0.7f)

    fun submit() {
        runCatching { progress.answer(seed, typed) }
            .onSuccess { next ->
                typed = ""
                if (next.done) onSolved() else progress = next
            }
            .onFailure(onFailed)
    }

    Column(modifier, horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(8.dp)) {
        if (progress.needed > 1) {
            Text("Sum ${progress.solved + 1} of ${progress.needed}", style = MaterialTheme.typography.titleMedium, color = muted)
        }
        Text(
            "${sum.text} =",
            fontSize = 48.sp,
            fontWeight = FontWeight.Medium,
            color = accent,
            modifier = Modifier.semantics {
                heading()
                contentDescription = "${sum.spoken} equals?"
                liveRegion = LiveRegionMode.Assertive
            },
        )
        Box(
            Modifier.size(width = 160.dp, height = 64.dp).border(2.dp, muted, RoundedCornerShape(12.dp))
                .clearAndSetSemantics { contentDescription = if (typed.isEmpty()) "Your answer, empty" else "Your answer, $typed" },
            contentAlignment = Alignment.Center,
        ) { Text(typed, fontSize = 36.sp, color = accent) }
        Text(
            if (progress.lastWrong) "Not quite. Here's a new one." else " ",
            style = MaterialTheme.typography.bodyLarge,
            color = muted,
            modifier = Modifier.semantics { liveRegion = LiveRegionMode.Polite },
        )
        val rows = listOf(listOf("1", "2", "3"), listOf("4", "5", "6"), listOf("7", "8", "9"))
        rows.forEach { row ->
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                row.forEach { d -> Key(d, accent, onClick = { if (typed.length < MAX_DIGITS) typed += d }) }
            }
        }
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            Key("⌫", accent, description = "Delete", enabled = typed.isNotEmpty(), onClick = { typed = typed.dropLast(1) })
            Key("0", accent, onClick = { if (typed.length < MAX_DIGITS) typed += "0" })
            Key("OK", accent, description = "Check answer", enabled = typed.isNotEmpty(), onClick = ::submit)
        }
    }
}

private const val MAX_DIGITS = 3

@Composable
private fun Key(text: String, accent: Color, description: String? = null, enabled: Boolean = true, onClick: () -> Unit) {
    OutlinedButton(
        onClick = onClick,
        enabled = enabled,
        modifier = Modifier.size(width = 88.dp, height = 60.dp)
            .then(if (description != null) Modifier.semantics { contentDescription = description } else Modifier),
        colors = ButtonDefaults.outlinedButtonColors(contentColor = accent),
    ) { Text(text, fontSize = 24.sp) }
}

/**
 * *Hold the button*: a button that fills up while it is held and calls [onDone] once it has been held for
 * [HOLD_TO_STOP_MILLIS]. Letting go early empties it again.
 */
@Composable
fun HoldButton(
    label: String,
    accent: Color,
    onDone: () -> Unit,
    modifier: Modifier = Modifier,
    width: Dp = 150.dp,
    height: Dp = 72.dp,
    filled: Boolean = true,
) {
    var holding by remember { mutableStateOf(false) }
    var progress by remember { mutableFloatStateOf(0f) }
    val done by rememberUpdatedState(onDone)
    LaunchedEffect(holding) {
        if (!holding) {
            progress = 0f
            return@LaunchedEffect
        }
        val start = withFrameMillis { it }
        while (true) {
            val now = withFrameMillis { it }
            progress = holdProgress(now - start)
            if (progress >= 1f) {
                holding = false
                done()
                break
            }
        }
    }
    val shape = RoundedCornerShape(height / 2)
    val textColor = if (filled) Color.Black else accent
    Box(
        modifier.size(width = width, height = height).clip(shape)
            .background(if (filled) accent.copy(alpha = 0.55f) else Color.Transparent)
            .border(2.dp, accent, shape)
            .pointerInput(Unit) {
                awaitEachGesture {
                    awaitFirstDown()
                    holding = true
                    waitForUpOrCancellation()
                    holding = false
                }
            }
            .semantics {
                role = Role.Button
                contentDescription = "$label: touch and hold for ${HOLD_TO_STOP_MILLIS / 1000} seconds"
                stateDescription = if (holding) "Keep holding" else ""
                progressBarRangeInfo = ProgressBarRangeInfo(progress, 0f..1f)
            },
        contentAlignment = Alignment.Center,
    ) {
        Box(Modifier.align(Alignment.CenterStart).fillMaxHeight().fillMaxWidth(progress).background(accent))
        Text(if (holding) "Keep holding…" else label, fontSize = 18.sp, color = if (progress > 0.5f) Color.Black else textColor)
    }
}
