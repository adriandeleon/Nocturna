package com.adl.nocturna.clock

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.adl.nocturna.data.Settings
import com.adl.nocturna.data.SettingsStore
import com.adl.nocturna.radio.RADIO_VOLUME_STEPS
import com.adl.nocturna.radio.dialPosition
import com.adl.nocturna.radio.nearestSlot
import com.adl.nocturna.radio.presetIndices
import com.adl.nocturna.radio.presetSlot
import com.adl.nocturna.radio.radioVolumeStep
import com.adl.nocturna.radio.tuned
import com.adl.nocturna.radio.tunedTo
import com.adl.nocturna.resources.*
import com.adl.nocturna.sound.displayName
import com.adl.nocturna.ui.ControlButton
import com.adl.nocturna.ui.ControlColors
import com.adl.nocturna.ui.DeskNavy
import com.adl.nocturna.ui.Glyph
import com.adl.nocturna.ui.currentLocale
import org.jetbrains.compose.resources.stringResource

/**
 * The clock radio's quick controls, above the clock's buttons while the radio plays or just after a station
 * failed: the previous and next preset either side of the station's name, which play it (a tap on the name opens the [TunerPanel]), and the volume.
 */
@Composable
internal fun RadioStrip(settings: Settings, colors: ControlColors, onInteract: () -> Unit, onOpenTuner: () -> Unit) {
    val radio = LocalClockPlatform.current.radio
    val locale = currentLocale()
    val name = settings.currentStation?.displayName(locale).orEmpty()
    FlowRow(
        horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.CenterHorizontally),
        verticalArrangement = Arrangement.spacedBy(8.dp),
        itemVerticalAlignment = Alignment.CenterVertically,
    ) {
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
            ControlButton(
                "◀", colors, description = stringResource(Res.string.tuner_previous),
                onClick = { onInteract(); radio.tune(start = true) { it.tuned(-1) } },
            )
            ControlButton(
                name, colors, glyph = Glyph.RADIO, modifier = Modifier.widthIn(max = 260.dp),
                description = stringResource(Res.string.tuner_open, name),
                onClick = { onInteract(); onOpenTuner() },
            )
            ControlButton(
                "▶", colors, description = stringResource(Res.string.tuner_next),
                onClick = { onInteract(); radio.tune(start = true) { it.tuned(1) } },
            )
        }
        VolumeButtons(settings, colors, onInteract)
    }
}

@Composable
private fun VolumeButtons(settings: Settings, colors: ControlColors, onInteract: () -> Unit) {
    val volume = stringResource(Res.string.tuner_volume, settings.radioVolume, RADIO_VOLUME_STEPS)
    Row(
        horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier.semantics { stateDescription = volume },
    ) {
        ControlButton(
            "−", colors, description = stringResource(Res.string.tuner_quieter), enabled = settings.radioVolume > 1,
            onClick = { onInteract(); SettingsStore.update { it.copy(radioVolume = radioVolumeStep(it.radioVolume, -1)) } },
        )
        Text(
            "${settings.radioVolume}", color = colors.content, fontSize = 18.sp, textAlign = TextAlign.Center,
            modifier = Modifier.width(28.dp).semantics { contentDescription = volume },
        )
        ControlButton(
            "+", colors, description = stringResource(Res.string.tuner_louder), enabled = settings.radioVolume < RADIO_VOLUME_STEPS,
            onClick = { onInteract(); SettingsStore.update { it.copy(radioVolume = radioVolumeStep(it.radioVolume, 1)) } },
        )
    }
}

/**
 * A clock radio's tuning dial: the presets spread along a band like stations on the FM scale, and a needle on
 * the one playing. Tap a preset or drag the needle to tune; it snaps to the nearest preset when let go. Below
 * it, the station and song, the arrows, the volume and the power.
 */
@Composable
internal fun TunerPanel(
    settings: Settings,
    day: Boolean,
    color: Color,
    colors: ControlColors,
    onInteract: () -> Unit,
    onClose: () -> Unit,
) {
    val radio = LocalClockPlatform.current.radio
    val radioOn by radio.isOn.collectAsStateWithLifecycle()
    val song by radio.song.collectAsStateWithLifecycle()
    val locale = currentLocale()
    val presets = settings.presetIndices
    val slot = settings.presetSlot
    val ink = if (day) Color.White else color
    val station = settings.currentStation?.displayName(locale)

    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(12.dp),
        modifier = Modifier
            .widthIn(max = 640.dp)
            .fillMaxWidth()
            .background(if (day) DeskNavy else Color.Black, RoundedCornerShape(20.dp))
            .border(1.dp, ink.copy(alpha = if (day) 0f else 0.3f), RoundedCornerShape(20.dp))
            .padding(16.dp),
    ) {
        Text(
            station ?: stringResource(Res.string.tuner_no_station), color = ink, fontSize = 22.sp,
            maxLines = 1, overflow = TextOverflow.Ellipsis,
        )
        Text(
            if (radioOn) song ?: " " else stringResource(Res.string.tuner_off),
            color = ink.copy(alpha = 0.6f), fontSize = 15.sp, maxLines = 1, overflow = TextOverflow.Ellipsis,
        )
        if (presets.isNotEmpty()) {
            Dial(
                names = presets.map { settings.stations[it].displayName(locale) },
                slot = slot, ink = ink,
                onTune = { s -> onInteract(); radio.tune(start = true) { it.tunedTo(presets[s]) } },
            )
        }
        FlowRow(
            horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.CenterHorizontally),
            verticalArrangement = Arrangement.spacedBy(8.dp),
            itemVerticalAlignment = Alignment.CenterVertically,
        ) {
            ControlButton(
                "◀", colors, description = stringResource(Res.string.tuner_previous),
                onClick = { onInteract(); radio.tune { it.tuned(-1) } },
            )
            ControlButton(
                stringResource(Res.string.clock_radio), colors, glyph = Glyph.RADIO, checked = radioOn,
                onClick = { onInteract(); radio.toggle(settings.currentStation, settings.radioSleepMinutes) },
            )
            ControlButton(
                "▶", colors, description = stringResource(Res.string.tuner_next),
                onClick = { onInteract(); radio.tune { it.tuned(1) } },
            )
            VolumeButtons(settings, colors, onInteract)
            ControlButton(stringResource(Res.string.tuner_close), colors, onClick = onClose)
        }
    }
}

/** The band, its ticks, the presets' names under their marks and the needle. */
@Composable
private fun Dial(names: List<String>, slot: Int?, ink: Color, onTune: (Int) -> Unit) {
    val count = names.size
    // While dragged, the needle follows the finger; otherwise it glides to the tuned preset.
    var dragAt by remember { mutableStateOf<Float?>(null) }
    val target = slot?.let { dialPosition(it, count) }
    val needle by animateFloatAsState(dragAt ?: target ?: 0f, label = "needle")
    val tuned = slot?.let { names[it] }
    val dialDescription = stringResource(Res.string.tuner_dial, tuned ?: "-")

    BoxWithConstraints(Modifier.fillMaxWidth().semantics { contentDescription = dialDescription }) {
        val width = maxWidth
        Column {
            Canvas(
                Modifier
                    .fillMaxWidth()
                    .height(56.dp)
                    .pointerInput(count) {
                        detectTapGestures { p -> nearestSlot(p.x / size.width, count)?.let(onTune) }
                    }
                    .pointerInput(count) {
                        detectHorizontalDragGestures(
                            onDragStart = { p -> dragAt = (p.x / size.width).coerceIn(0f, 1f) },
                            onDragEnd = {
                                dragAt?.let { at -> nearestSlot(at, count)?.let(onTune) }
                                dragAt = null
                            },
                            onDragCancel = { dragAt = null },
                        ) { change, dx ->
                            change.consume()
                            dragAt = ((dragAt ?: 0f) + dx / size.width).coerceIn(0f, 1f)
                        }
                    },
            ) {
                val w = size.width
                val h = size.height
                val base = h * 0.75f
                // The scale: a fine tick every 2 %, a taller one every 10 %, a long one at each preset.
                drawLine(ink.copy(alpha = 0.35f), Offset(0f, base), Offset(w, base), 2f)
                for (i in 0..50) {
                    val x = w * i / 50f
                    val tall = if (i % 5 == 0) h * 0.22f else h * 0.12f
                    drawLine(ink.copy(alpha = 0.3f), Offset(x, base), Offset(x, base - tall), 2f)
                }
                for (s in 0 until count) {
                    val x = w * dialPosition(s, count)
                    val a = if (s == slot) 0.9f else 0.55f
                    drawLine(ink.copy(alpha = a), Offset(x, base), Offset(x, base - h * 0.4f), 4f, StrokeCap.Round)
                }
                if (target != null || dragAt != null) {
                    val x = w * needle
                    drawLine(ink, Offset(x, h * 0.05f), Offset(x, h * 0.95f), 6f, StrokeCap.Round)
                }
            }
            // The presets' names, centred under their marks, as wide as the gap between them allows.
            val labelWidth = (width / count).coerceAtMost(140.dp)
            Box(Modifier.fillMaxWidth().height(22.dp)) {
                names.forEachIndexed { s, name ->
                    val x = width * dialPosition(s, count) - labelWidth / 2
                    Text(
                        name, color = ink.copy(alpha = if (s == slot) 0.95f else 0.55f), fontSize = 12.sp,
                        maxLines = 1, overflow = TextOverflow.Ellipsis, textAlign = TextAlign.Center,
                        modifier = Modifier.offset(x = x).width(labelWidth),
                    )
                }
            }
        }
    }
}
