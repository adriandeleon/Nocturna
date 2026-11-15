package com.adl.nocturna.clock

import androidx.compose.ui.text.font.FontFamily

// iOS's system font has no condensed family that Compose can name; the regular one stands in.
internal actual fun condensedFontFamily(): FontFamily = FontFamily.SansSerif
