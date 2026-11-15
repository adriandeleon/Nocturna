package com.adl.nocturna.settings

import android.os.Build
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalContext
import com.adl.nocturna.ui.NocturnaTheme

/**
 * The settings screens' theme: Material You's dark colours from the wallpaper on Android 12 and
 * later, Nocturna's own night blues before it. The clock itself keeps [NocturnaTheme].
 */
@Composable
fun SettingsTheme(content: @Composable () -> Unit) {
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
        MaterialTheme(colorScheme = dynamicDarkColorScheme(LocalContext.current), content = content)
    } else {
        NocturnaTheme(content)
    }
}
