package com.adl.nocturna.clock

import com.adl.nocturna.data.Settings

private val DEFAULT_COLOR = Settings().color

/**
 * The clock's colour as ARGB, before night mode and day mode change it: the wallpaper's accent when
 * [Settings.colorFromWallpaper] is on and the phone has one ([wallpaperColor] not null), the picked
 * swatch otherwise. A phone without a wallpaper palette that was set to follow it (a restored backup,
 * say) gets the default colour.
 */
fun clockColor(settings: Settings, wallpaperColor: Int?): Int = when {
    !settings.colorFromWallpaper -> settings.color
    wallpaperColor != null -> wallpaperColor
    else -> DEFAULT_COLOR
}

/**
 * Which fixed swatch the colour picker marks as chosen: null while the clock follows the wallpaper,
 * which then has its own mark. Where there is no wallpaper colour, that's the swatch [clockColor] falls back to.
 */
fun selectedSwatch(settings: Settings, wallpaperColor: Int?): Int? =
    if (settings.colorFromWallpaper && wallpaperColor != null) null else clockColor(settings, wallpaperColor)
