package com.adl.nocturna.radio

import com.adl.nocturna.clock.minutesLeft
import com.adl.nocturna.data.Settings
import com.adl.nocturna.data.Station
import com.adl.nocturna.sound.SleepSound
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue

class TuningTest {
    private val five = (0..4).map { Station("S$it", "https://radio.example/$it") }
    private fun settings(selected: Int = 0, stars: List<Int> = emptyList(), sound: String = "") =
        Settings(stations = five, selectedStation = selected, selectedSound = sound, favoriteStations = stars.map { five[it].url }.toSet())

    @Test fun withNoStarsEveryStationIsAPreset() = assertEquals(listOf(0, 1, 2, 3, 4), settings().presetIndices)

    @Test fun starredStationsAreThePresetsInListOrder() = assertEquals(listOf(1, 3), settings(stars = listOf(3, 1)).presetIndices)

    @Test fun nextAndPreviousWrapRound() {
        assertEquals(1, settings(selected = 0).tuned(1).selectedStation)
        assertEquals(0, settings(selected = 4).tuned(1).selectedStation)
        assertEquals(4, settings(selected = 0).tuned(-1).selectedStation)
        assertEquals(2, settings(selected = 4).tuned(3).selectedStation)
    }

    @Test fun stepsGoOnlyThroughStarredStations() {
        val s = settings(selected = 1, stars = listOf(1, 3))
        assertEquals(3, s.tuned(1).selectedStation)
        assertEquals(1, s.tuned(1).tuned(1).selectedStation)
        assertEquals(3, s.tuned(-1).selectedStation)
    }

    @Test fun fromAStationThatIsNotAPresetTheFirstStepGoesToItsNeighbour() {
        val s = settings(selected = 2, stars = listOf(0, 1, 3))
        assertNull(s.presetSlot)
        assertEquals(3, s.tuned(1).selectedStation)
        assertEquals(1, s.tuned(-1).selectedStation)
        assertEquals(0, s.tuned(2).selectedStation)
        assertEquals(0, settings(selected = 4, stars = listOf(0, 1)).tuned(1).selectedStation)
        // Nothing above the first station: back wraps round to the last preset.
        assertEquals(3, settings(selected = 0, stars = listOf(1, 3)).tuned(-1).selectedStation)
    }

    @Test fun fromASleepSoundTuningGoesBackToTheStations() {
        val s = settings(selected = 2, sound = SleepSound.RAIN.id)
        assertEquals(0, s.tuned(1).selectedStation)
        assertEquals("", s.tuned(1).selectedSound)
        assertEquals(4, s.tuned(-1).selectedStation)
    }

    @Test fun tuningWithNoStationsChangesNothing() {
        val s = Settings(stations = emptyList())
        assertEquals(s, s.tuned(1))
    }

    @Test fun starringTogglesAndTunedToSelects() {
        val s = settings().withFavoriteToggled(five[2].url)
        assertEquals(setOf(five[2].url), s.favoriteStations)
        assertTrue(s.withFavoriteToggled(five[2].url).favoriteStations.isEmpty())
        assertEquals(3, settings(sound = SleepSound.RAIN.id).tunedTo(3).selectedStation)
        assertEquals("", settings(sound = SleepSound.RAIN.id).tunedTo(3).selectedSound)
        assertEquals(0, settings().tunedTo(9).selectedStation)
    }

    @Test fun dialSpacesPresetsEvenlyAndSnapsToTheNearest() {
        assertEquals(0.1f, dialPosition(0, 5))
        assertEquals(0.5f, dialPosition(2, 5))
        assertEquals(0.9f, dialPosition(4, 5))
        assertEquals(0, nearestSlot(0f, 5))
        assertEquals(2, nearestSlot(0.45f, 5))
        assertEquals(4, nearestSlot(1f, 5))
        assertEquals(0, nearestSlot(0.7f, 1))
        assertNull(nearestSlot(0.5f, 0))
        for (slot in 0 until 7) assertEquals(slot, nearestSlot(dialPosition(slot, 7), 7))
    }

    @Test fun volumeIsQuietAtTheBottomAndFullAtTheTop() {
        assertEquals(0.01f, radioGain(1), 1e-6f)
        assertEquals(1f, radioGain(RADIO_VOLUME_STEPS))
        assertEquals(radioGain(1), radioGain(-3))
        for (step in 1 until RADIO_VOLUME_STEPS) assertTrue(radioGain(step) < radioGain(step + 1))
        assertEquals(1, radioVolumeStep(1, -1))
        assertEquals(RADIO_VOLUME_STEPS, radioVolumeStep(RADIO_VOLUME_STEPS, 1))
        assertEquals(5, radioVolumeStep(4, 1))
    }

    @Test fun streamTitlesAreTidied() {
        assertEquals("Miles Davis - So What", cleanStreamTitle("  Miles Davis  -  So What ", "Jazz FM"))
        assertEquals("Song", cleanStreamTitle("'Song'", null))
        assertEquals("Artist", cleanStreamTitle("Artist - ", null))
        assertNull(cleanStreamTitle("", null))
        assertNull(cleanStreamTitle(null, null))
        assertNull(cleanStreamTitle("jazz fm", "Jazz FM"))
        assertNull(cleanStreamTitle("https://jazz.example", null))
        assertNull(cleanStreamTitle("Unknown", null))
        assertEquals(120, cleanStreamTitle("x".repeat(500), null)?.length)
    }

    @Test fun minutesLeftRoundsUp() {
        assertEquals(1, minutesLeft(1_000, 0))
        assertEquals(2, minutesLeft(60_001, 0))
        assertEquals(1, minutesLeft(0, 5_000))
    }
}
