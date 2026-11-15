package com.adl.nocturna.settings

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
import androidx.compose.runtime.Composable
import com.adl.nocturna.resources.*
import org.jetbrains.compose.resources.StringResource
import org.jetbrains.compose.resources.stringResource

/** The Settings pages, by name. */
object SettingsPages {
    const val MAIN = "main"
    const val CLOCK = "clock"
    const val INFO_LINE = "info_line"
    const val BRIGHTNESS = "brightness"
    const val DAY_MODE = "day_mode"
    const val WEATHER = "weather"
    const val START = "start"
    const val ALARMS = "alarms"
    const val STATIONS = "stations"
    const val ABOUT = "about"

    private val TITLES = mapOf(
        MAIN to Res.string.settings_title,
        CLOCK to Res.string.settings_clock_face,
        INFO_LINE to Res.string.settings_show_on_clock,
        BRIGHTNESS to Res.string.settings_brightness,
        DAY_MODE to Res.string.settings_day_mode,
        WEATHER to Res.string.settings_weather,
        START to Res.string.settings_starting,
        ALARMS to Res.string.settings_alarms,
        STATIONS to Res.string.settings_radio_stations,
        ABOUT to Res.string.settings_about_title,
    )

    /** Whether [page] is one of the pages above. */
    fun exists(page: String): Boolean = page in TITLES

    fun title(page: String): StringResource = TITLES.getValue(page)
}

/**
 * Settings: [page] under a top bar with a back button, which calls [onBack]. Pages navigate with
 * [onNavigate]; the app keeps the current page and handles the system's back gesture.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(page: String, onNavigate: (String) -> Unit, onBack: () -> Unit) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(SettingsPages.title(page))) },
                navigationIcon = { TextButton(onClick = onBack) { Text(stringResource(Res.string.settings_back)) } },
            )
        },
    ) { padding ->
        // Pages slide in from the right and back out to it, like the system's own settings.
        AnimatedContent(
            targetState = page,
            transitionSpec = {
                val sign = if (targetState == SettingsPages.MAIN) -1 else 1
                (slideInHorizontally { sign * it / 4 } + fadeIn()) togetherWith
                    (slideOutHorizontally { -sign * it / 4 } + fadeOut())
            },
            label = "settings page",
        ) { shown ->
            when (shown) {
                SettingsPages.CLOCK -> ClockFacePage(padding)
                SettingsPages.INFO_LINE -> InfoLinePage(padding, onNavigate)
                SettingsPages.BRIGHTNESS -> BrightnessPage(padding)
                SettingsPages.DAY_MODE -> DayModePage(padding)
                SettingsPages.WEATHER -> WeatherPage(padding, onNavigate)
                SettingsPages.START -> StartPage(padding)
                SettingsPages.ALARMS -> AlarmsScreen(padding)
                SettingsPages.STATIONS -> StationsScreen(padding)
                SettingsPages.ABOUT -> AboutScreen(padding)
                else -> SettingsHub(padding, onNavigate)
            }
        }
    }
}
