package com.adl.nocturna.alarm

import android.os.Bundle
import android.view.WindowManager
import androidx.activity.ComponentActivity
import androidx.activity.addCallback
import androidx.activity.compose.setContent
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
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.adl.nocturna.clock.DigitalClock
import com.adl.nocturna.clock.rememberNow
import com.adl.nocturna.data.SettingsStore
import com.adl.nocturna.radio.Radio
import com.adl.nocturna.ui.NocturnaTheme
import com.adl.nocturna.ui.enableDarkEdgeToEdge

/**
 * Full-screen "wake up" screen with big Snooze and Dismiss buttons. Shows over the lock screen.
 * While a radio alarm plays, "Keep listening" dismisses the alarm and carries on with the station in the
 * normal radio player.
 */
class AlarmActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableDarkEdgeToEdge()
        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        window.attributes = window.attributes.apply { screenBrightness = 1f }
        // Back would leave the alarm ringing out of sight; treat it as snooze.
        onBackPressedDispatcher.addCallback(this) { AlarmService.send(this@AlarmActivity, AlarmService.ACTION_SNOOZE) }

        setContent {
            NocturnaTheme {
                val ringing by AlarmService.ringing.collectAsStateWithLifecycle()
                LaunchedEffect(ringing) { if (ringing == null) finish() }
                val settings = SettingsStore.current
                val now by rememberNow()
                val accent = Color(settings.color)

                Column(
                    Modifier.fillMaxSize().background(Color.Black).safeDrawingPadding().padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center,
                ) {
                    DigitalClock(now, settings.use24h, false, settings.font, accent, 96.sp)
                    Spacer(Modifier.height(12.dp))
                    Text(
                        ringing?.label?.ifBlank { null } ?: "Alarm",
                        style = MaterialTheme.typography.headlineSmall,
                        color = accent.copy(alpha = 0.8f),
                    )
                    ringing?.station?.let { station ->
                        Spacer(Modifier.height(4.dp))
                        Text(station.name, style = MaterialTheme.typography.bodyLarge, color = accent.copy(alpha = 0.6f))
                    }
                    Spacer(Modifier.height(64.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(24.dp)) {
                        OutlinedButton(
                            onClick = { AlarmService.send(this@AlarmActivity, AlarmService.ACTION_SNOOZE) },
                            modifier = Modifier.size(width = 150.dp, height = 72.dp),
                            colors = ButtonDefaults.outlinedButtonColors(contentColor = accent),
                        ) { Text("Snooze ${settings.snoozeMinutes}m", fontSize = 18.sp) }
                        Button(
                            onClick = { AlarmService.send(this@AlarmActivity, AlarmService.ACTION_DISMISS) },
                            modifier = Modifier.size(width = 150.dp, height = 72.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = accent, contentColor = Color.Black),
                        ) { Text("Dismiss", fontSize = 18.sp) }
                    }
                    ringing?.station?.let { station ->
                        Spacer(Modifier.height(24.dp))
                        TextButton(
                            onClick = {
                                AlarmService.send(this@AlarmActivity, AlarmService.ACTION_DISMISS)
                                Radio.play(this@AlarmActivity, station)
                            },
                            colors = ButtonDefaults.textButtonColors(contentColor = accent),
                        ) { Text("Keep listening", fontSize = 18.sp) }
                    }
                }
            }
        }
    }
}
