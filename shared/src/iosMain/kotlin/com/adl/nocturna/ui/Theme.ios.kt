package com.adl.nocturna.ui

import androidx.compose.runtime.Composable
import com.adl.nocturna.text.PlatformLocale
import platform.Foundation.NSLocale
import platform.Foundation.currentLocale

// iOS restarts the app when the language changes, so there is nothing to watch.
@Composable
actual fun currentLocale(): PlatformLocale = NSLocale.currentLocale
