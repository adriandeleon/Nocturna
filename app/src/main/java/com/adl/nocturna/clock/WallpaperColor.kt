package com.adl.nocturna.clock

import android.app.WallpaperManager
import android.os.Build
import android.os.Handler
import android.os.Looper
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalResources

/**
 * The system's light wallpaper accent (`system_accent1_200`), which reads well on black, or null
 * below Android 12, which has no dynamic palette. It is read again when the configuration changes
 * (the system applies a new palette that way) and when the wallpaper's colours change, so a new
 * wallpaper shows without restarting the clock.
 */
@Composable
fun rememberWallpaperColor(): Int? {
    if (Build.VERSION.SDK_INT < Build.VERSION_CODES.S) return null
    val context = LocalContext.current
    val resources = LocalResources.current
    val configuration = LocalConfiguration.current
    var wallpaperChanges by remember { mutableIntStateOf(0) }
    DisposableEffect(context) {
        val manager = context.getSystemService(WallpaperManager::class.java)
        val listener = WallpaperManager.OnColorsChangedListener { _, _ -> wallpaperChanges++ }
        manager?.addOnColorsChangedListener(listener, Handler(Looper.getMainLooper()))
        onDispose { manager?.removeOnColorsChangedListener(listener) }
    }
    return remember(resources, configuration, wallpaperChanges) { resources.getColor(android.R.color.system_accent1_200, null) }
}
