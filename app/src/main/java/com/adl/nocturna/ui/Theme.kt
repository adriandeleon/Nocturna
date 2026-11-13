package com.adl.nocturna.ui

import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.enableEdgeToEdge
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.ui.platform.LocalConfiguration
import java.util.Locale
import androidx.compose.ui.graphics.Color

private val scheme = darkColorScheme(
    primary = Color(0xFF8FD3FF),
    onPrimary = Color(0xFF00213A),
    primaryContainer = Color(0xFF1C3D5A),
    onPrimaryContainer = Color(0xFFCDE8FF),
    secondary = Color(0xFFFFD27A),
    onSecondary = Color(0xFF3D2E00),
    secondaryContainer = Color(0xFF243049),
    onSecondaryContainer = Color(0xFFD9E2FF),
    tertiary = Color(0xFFFFD27A),
    onTertiary = Color(0xFF3D2E00),
    tertiaryContainer = Color(0xFF4A3B12),
    onTertiaryContainer = Color(0xFFFFE6AE),
    surfaceVariant = Color(0xFF1C2338),
    onSurfaceVariant = Color(0xFFC3C8D6),
    surfaceContainerLowest = Color(0xFF05070D),
    surfaceContainerLow = Color(0xFF0D1324),
    surfaceContainerHighest = Color(0xFF1C2338),
    outline = Color(0xFF5A6378),
    outlineVariant = Color(0xFF2E3549),
    inverseSurface = Color(0xFFE3E6EE),
    inverseOnSurface = Color(0xFF111729),
    inversePrimary = Color(0xFF1F6491),
    background = Color(0xFF05070D),
    surface = Color(0xFF0B1020),
    surfaceContainer = Color(0xFF111729),
    surfaceContainerHigh = Color(0xFF161D33),
    onBackground = Color(0xFFE3E6EE),
    onSurface = Color(0xFFE3E6EE),
)

@Composable
fun NocturnaTheme(content: @Composable () -> Unit) {
    MaterialTheme(colorScheme = scheme, content = content)
}

/** Edge-to-edge with light system-bar icons; all our screens are dark whatever the system theme. */
fun ComponentActivity.enableDarkEdgeToEdge() {
    val transparent = android.graphics.Color.TRANSPARENT
    enableEdgeToEdge(SystemBarStyle.dark(transparent), SystemBarStyle.dark(transparent))
}

/** The current locale, read so that composables recompose when it changes. */
@Composable
@ReadOnlyComposable
fun currentLocale(): Locale = LocalConfiguration.current.locales[0]

/** The dim red used in night mode; red light disturbs night vision and sleep the least. */
val NightRed = Color(0xFFC62828)
