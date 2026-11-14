package com.adl.nocturna.settings

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
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
        val start = intent.getStringExtra(EXTRA_SCREEN)?.takeIf { it in TITLES } ?: SCREEN_MAIN
        // Opened straight into a page (e.g. from the clock's Alarms button): back closes the activity.
        val directEntry = start != SCREEN_MAIN

        setContent {
            NocturnaTheme {
                var screen by rememberSaveable { mutableStateOf(start) }
                val goBack = { if (screen == SCREEN_MAIN || directEntry) finish() else screen = SCREEN_MAIN }
                BackHandler(onBack = goBack)

                Scaffold(
                    topBar = {
                        TopAppBar(
                            title = { Text(TITLES.getValue(screen)) },
                            navigationIcon = { TextButton(onClick = goBack) { Text("‹ Back") } },
                        )
                    },
                ) { padding ->
                    // Pages slide in from the right and back out to it, like the system's own settings.
                    AnimatedContent(
                        targetState = screen,
                        transitionSpec = {
                            val sign = if (targetState == SCREEN_MAIN) -1 else 1
                            (slideInHorizontally { sign * it / 4 } + fadeIn()) togetherWith
                                (slideOutHorizontally { -sign * it / 4 } + fadeOut())
                        },
                        label = "settings page",
                    ) { page ->
                        val open: (String) -> Unit = { screen = it }
                        when (page) {
                            SCREEN_CLOCK -> ClockFacePage(padding)
                            SCREEN_INFO_LINE -> InfoLinePage(padding, open)
                            SCREEN_BRIGHTNESS -> BrightnessPage(padding)
                            SCREEN_DAY_MODE -> DayModePage(padding)
                            SCREEN_WEATHER -> WeatherPage(padding, open)
                            SCREEN_START -> StartPage(padding)
                            SCREEN_ALARMS -> AlarmsScreen(padding)
                            SCREEN_STATIONS -> StationsScreen(padding)
                            SCREEN_ABOUT -> AboutScreen(padding)
                            else -> SettingsHub(padding, onNavigate = open)
                        }
                    }
                }
            }
        }
    }

    companion object {
        const val EXTRA_SCREEN = "screen"
        const val SCREEN_MAIN = "main"
        const val SCREEN_CLOCK = "clock"
        const val SCREEN_INFO_LINE = "info_line"
        const val SCREEN_BRIGHTNESS = "brightness"
        const val SCREEN_DAY_MODE = "day_mode"
        const val SCREEN_WEATHER = "weather"
        const val SCREEN_START = "start"
        const val SCREEN_ALARMS = "alarms"
        const val SCREEN_STATIONS = "stations"
        const val SCREEN_ABOUT = "about"

        private val TITLES = mapOf(
            SCREEN_MAIN to "Settings",
            SCREEN_CLOCK to "Clock face",
            SCREEN_INFO_LINE to "Show on the clock",
            SCREEN_BRIGHTNESS to "Brightness and night mode",
            SCREEN_DAY_MODE to "Day mode",
            SCREEN_WEATHER to "Weather",
            SCREEN_START to "Starting the clock",
            SCREEN_ALARMS to "Alarms",
            SCREEN_STATIONS to "Radio stations",
            SCREEN_ABOUT to "About",
        )
    }
}
