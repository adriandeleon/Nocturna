package com.adl.nocturna.ui

import androidx.compose.runtime.Composable
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.ui.platform.LocalConfiguration
import com.adl.nocturna.text.PlatformLocale

@Composable
@ReadOnlyComposable
actual fun currentLocale(): PlatformLocale = LocalConfiguration.current.locales[0]
