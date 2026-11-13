package com.adl.nocturna.settings

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import com.adl.nocturna.ui.NocturnaTheme
import com.adl.nocturna.ui.enableDarkEdgeToEdge

class SettingsActivity : ComponentActivity() {
    @OptIn(ExperimentalMaterial3Api::class)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableDarkEdgeToEdge()
        val start = intent.getStringExtra(EXTRA_SCREEN) ?: SCREEN_MAIN
        // Opened straight into a sub-screen (e.g. from the clock's Alarms button): back closes the activity.
        val directEntry = start != SCREEN_MAIN

        setContent {
            NocturnaTheme {
                var screen by rememberSaveable { mutableStateOf(start) }
                val goBack = { if (screen == SCREEN_MAIN || directEntry) finish() else screen = SCREEN_MAIN }
                BackHandler(onBack = goBack)

                Scaffold(
                    topBar = {
                        TopAppBar(
                            title = {
                                Text(
                                    when (screen) {
                                        SCREEN_ALARMS -> "Alarms"
                                        SCREEN_STATIONS -> "Radio stations"
                                        SCREEN_ABOUT -> "About"
                                        else -> "Settings"
                                    }
                                )
                            },
                            navigationIcon = { TextButton(onClick = goBack) { Text("‹ Back") } },
                        )
                    },
                ) { padding ->
                    when (screen) {
                        SCREEN_ALARMS -> AlarmsScreen(padding)
                        SCREEN_STATIONS -> StationsScreen(padding)
                        SCREEN_ABOUT -> AboutScreen(padding)
                        else -> MainSettings(padding, onNavigate = { screen = it })
                    }
                }
            }
        }
    }

    companion object {
        const val EXTRA_SCREEN = "screen"
        const val SCREEN_MAIN = "main"
        const val SCREEN_ALARMS = "alarms"
        const val SCREEN_STATIONS = "stations"
        const val SCREEN_ABOUT = "about"
    }
}
