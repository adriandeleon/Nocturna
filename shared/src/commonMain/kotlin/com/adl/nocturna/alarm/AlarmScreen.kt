package com.adl.nocturna.alarm

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.adl.nocturna.clock.DigitalClock
import com.adl.nocturna.clock.Proximity
import com.adl.nocturna.clock.WaveEffect
import com.adl.nocturna.clock.rememberNow
import com.adl.nocturna.data.SettingsStore
import com.adl.nocturna.data.Station
import com.adl.nocturna.resources.*
import com.adl.nocturna.ui.GlyphIcon
import com.adl.nocturna.weather.WeatherRepository
import com.adl.nocturna.weather.wakeWeather
import org.jetbrains.compose.resources.stringResource

/**
 * The ringing alarm: the time, its [label] and [station], with big Snooze and Dismiss buttons. With a
 * [station], stringResource(Res.string.alarm_keep_listening) dismisses the alarm and carries on with the station in the radio player.
 * Under the time it shows today's weather from [WeatherRepository]; the host refreshes that when it is old.
 *
 * Dismissing, and keeping on listening, take the alarm's [challenge]: a tap, the button held for
 * [HOLD_TO_STOP_MILLIS], or the sums from [seed] on a page of their own. Snoozing is always one tap. When the
 * challenge fails it calls [onChallengeFailed]; the host then passes [DismissChallenge.TAP] instead. With
 * wave to snooze on, a wave of the hand over the [proximity] sensor snoozes too.
 */
@Composable
fun AlarmScreen(
    label: String,
    station: Station?,
    onSnooze: () -> Unit,
    onDismiss: () -> Unit,
    onKeepListening: (Station) -> Unit,
    challenge: DismissChallenge = DismissChallenge.TAP,
    seed: Long = 0L,
    onChallengeFailed: (Throwable) -> Unit = {},
    /** The proximity sensor, for wave to snooze; null without one. */
    proximity: Proximity? = null,
) {
    val settings = SettingsStore.current
    // Wave to snooze: a wave does what the Snooze button does, never more, so the challenge stands.
    WaveEffect(proximity, settings.waveToSnooze, onSnooze)
    val now by rememberNow()
    val accent = Color(settings.color)
    val weather by WeatherRepository.weather.collectAsStateWithLifecycle()
    val wake = wakeWeather(settings, weather, now.instant, now.zone)
    // Set while the sums are showing: what solving them goes on to do.
    var solving by remember { mutableStateOf<(() -> Unit)?>(null) }
    fun startStop(stop: () -> Unit) {
        if (challenge == DismissChallenge.SUM) solving = stop else stop()
    }
    val snooze = @Composable {
        OutlinedButton(
            onClick = onSnooze,
            modifier = Modifier.size(width = 150.dp, height = 72.dp),
            colors = ButtonDefaults.outlinedButtonColors(contentColor = accent),
        ) { Text(stringResource(Res.string.alarm_snooze, settings.snoozeMinutes), fontSize = 18.sp) }
    }

    val afterSums = solving
    if (afterSums != null && challenge == DismissChallenge.SUM) {
        Column(
            Modifier.fillMaxSize().background(Color.Black).safeDrawingPadding().verticalScroll(rememberScrollState()).padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
        ) {
            SumChallenge(
                settings.alarmSums, seed, accent,
                onSolved = afterSums,
                onFailed = {
                    solving = null
                    onChallengeFailed(it)
                },
            )
            Spacer(Modifier.height(24.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(24.dp), verticalAlignment = Alignment.CenterVertically) {
                snooze()
                TextButton(onClick = { solving = null }, colors = ButtonDefaults.textButtonColors(contentColor = accent)) {
                    Text(stringResource(Res.string.alarm_back), fontSize = 18.sp)
                }
            }
        }
    } else Column(
        Modifier.fillMaxSize().background(Color.Black).safeDrawingPadding().padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        DigitalClock(now, settings.use24h, false, settings.font, accent, 96.sp)
        Spacer(Modifier.height(12.dp))
        Text(label.ifBlank { stringResource(Res.string.alarm_default_label) }, style = MaterialTheme.typography.headlineSmall, color = accent.copy(alpha = 0.8f))
        station?.let {
            Spacer(Modifier.height(4.dp))
            Text(it.name, style = MaterialTheme.typography.bodyLarge, color = accent.copy(alpha = 0.6f))
        }
        wake?.let { w ->
            Spacer(Modifier.height(12.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                val color = accent.copy(alpha = 0.7f)
                GlyphIcon(w.glyph, color, 26.dp)
                Spacer(Modifier.width(8.dp))
                Text(w.text, style = MaterialTheme.typography.titleLarge, color = color, softWrap = false)
            }
        }
        Spacer(Modifier.height(64.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(24.dp)) {
            snooze()
            if (challenge == DismissChallenge.HOLD) {
                HoldButton(stringResource(Res.string.alarm_hold_to_stop), accent, onDone = onDismiss)
            } else {
                Button(
                    onClick = { startStop(onDismiss) },
                    modifier = Modifier.size(width = 150.dp, height = 72.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = accent, contentColor = Color.Black),
                ) { Text(stringResource(Res.string.alarm_dismiss), fontSize = 18.sp) }
            }
        }
        station?.let {
            Spacer(Modifier.height(24.dp))
            if (challenge == DismissChallenge.HOLD) {
                HoldButton(stringResource(Res.string.alarm_keep_listening), accent, onDone = { onKeepListening(it) }, width = 200.dp, height = 56.dp, filled = false)
            } else {
                TextButton(
                    onClick = { startStop { onKeepListening(it) } },
                    colors = ButtonDefaults.textButtonColors(contentColor = accent),
                ) { Text(stringResource(Res.string.alarm_keep_listening), fontSize = 18.sp) }
            }
        }
    }
}
