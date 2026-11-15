package com.adl.nocturna.clock

import com.adl.nocturna.data.Settings
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class ClockColorTest {
    private val amber = 0xFFFFD27A.toInt()
    private val wallpaper = 0xFFB5C4FF.toInt()

    @Test fun aFixedColourIgnoresTheWallpaper() {
        val s = Settings(color = amber)
        assertEquals(amber, clockColor(s, wallpaper))
        assertEquals(amber, clockColor(s, null))
        assertEquals(amber, selectedSwatch(s, wallpaper))
    }

    @Test fun followingTheWallpaperUsesItsAccent() {
        val s = Settings(color = amber, colorFromWallpaper = true)
        assertEquals(wallpaper, clockColor(s, wallpaper))
        assertNull(selectedSwatch(s, wallpaper))
    }

    @Test fun withoutAWallpaperPaletteItFallsBackToTheDefault() {
        val s = Settings(color = amber, colorFromWallpaper = true)
        assertEquals(Settings().color, clockColor(s, null))
        assertEquals(Settings().color, selectedSwatch(s, null))
    }
}
