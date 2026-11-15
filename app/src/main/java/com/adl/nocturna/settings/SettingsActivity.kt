package com.adl.nocturna.settings

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import com.adl.nocturna.clock.AndroidClockPlatform
import com.adl.nocturna.clock.LocalClockPlatform
import com.adl.nocturna.ui.enableDarkEdgeToEdge

/** Hosts the shared [SettingsScreen]; [EXTRA_SCREEN] opens it on one of the [SettingsPages]. */
class SettingsActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableDarkEdgeToEdge()
        val start = intent.getStringExtra(EXTRA_SCREEN)?.takeIf { SettingsPages.exists(it) } ?: SettingsPages.MAIN
        // Opened straight into a page (e.g. from the clock's Alarms button): back closes the activity.
        val directEntry = start != SettingsPages.MAIN

        setContent {
            SettingsTheme {
                val clockPlatform = remember { AndroidClockPlatform(this) }
                val settingsPlatform = remember { AndroidSettingsPlatform(this) }
                var page by rememberSaveable { mutableStateOf(start) }
                val goBack = { if (page == SettingsPages.MAIN || directEntry) finish() else page = SettingsPages.MAIN }
                BackHandler(onBack = goBack)

                CompositionLocalProvider(
                    LocalClockPlatform provides clockPlatform,
                    LocalSettingsPlatform provides settingsPlatform,
                ) {
                    SettingsScreen(page, onNavigate = { page = it }, onBack = goBack)
                }
            }
        }
    }

    companion object {
        const val EXTRA_SCREEN = "screen"
    }
}
