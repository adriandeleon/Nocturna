package com.adl.nocturna.sound

import com.adl.nocturna.data.Station
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class PhoneMusicTest {
    private fun file(path: String, type: String? = "audio/mpeg") = FolderFile(path, type, "content://music/$path")

    @Test fun stationsOnThePhoneAreTheOnesThatAreNeitherStreamsNorSounds() {
        assertTrue(Station("Rain", "content://com.android.providers.media.documents/document/audio%3A42").isOnPhone)
        assertTrue(Station("Sleep", "content://com.android.externalstorage.documents/tree/primary%3AMusic").isOnPhone)
        assertFalse(Station("D99", "https://example.com/d99").isOnPhone)
        assertFalse(Station("Old", "http://example.com/old").isOnPhone)
        assertFalse(SleepSound.CHORDS.station.isOnPhone)
    }

    @Test fun audioIsKnownByItsTypeOrElseItsEnding() {
        assertTrue(PhoneMusic.isAudio("a.mp3", "audio/mpeg"))
        assertTrue(PhoneMusic.isAudio("a.ogg", "application/ogg"))
        assertTrue(PhoneMusic.isAudio("a.FLAC", null))
        assertTrue(PhoneMusic.isAudio("Disc 1/a.opus", "application/octet-stream"))
        assertTrue(PhoneMusic.isAudio("no ending", "audio/x-wav"))
        assertFalse(PhoneMusic.isAudio("cover.jpg", "image/jpeg"))
        assertFalse(PhoneMusic.isAudio("cover.mp3", "image/jpeg")) // its type says it isn't
        assertFalse(PhoneMusic.isAudio("notes.txt", null))
        assertFalse(PhoneMusic.isAudio("mp3", null))
    }

    @Test fun folderPlaysItsAudioInNameOrder() {
        val files = listOf(
            file("10 Night.mp3"), file("cover.jpg", "image/jpeg"), file("2 Dusk.mp3"),
            file("b/1 Later.ogg", "application/ogg"), file("1 Evening.mp3"), file("B/0 Upper.mp3"), file("a/9.flac", null),
        )
        assertEquals(
            listOf("1 Evening.mp3", "2 Dusk.mp3", "10 Night.mp3", "a/9.flac", "B/0 Upper.mp3", "b/1 Later.ogg"),
            PhoneMusic.playlist(files).map { it.path },
        )
    }

    @Test fun naturalOrderComparesNumbersAsNumbers() {
        val names = listOf("track 10", "Track 9", "track 009b", "track", "track 1", "track 01")
        assertEquals(
            listOf("track", "track 01", "track 1", "Track 9", "track 009b", "track 10"),
            names.sortedWith(PhoneMusic::naturalCompare),
        )
        assertEquals(0, PhoneMusic.naturalCompare("same", "same"))
        assertTrue(PhoneMusic.naturalCompare("99999999999999999999 a", "100000000000000000000 a") < 0)
    }

    @Test fun aHugeFolderPlaysOnlyTheFirstTracks() {
        val files = (1..PhoneMusic.MAX_TRACKS + 50).map { file("$it.mp3") }
        val list = PhoneMusic.playlist(files)
        assertEquals(PhoneMusic.MAX_TRACKS, list.size)
        assertEquals("1.mp3", list.first().path)
    }

    @Test fun stationIsNamedAfterTheFileWithoutItsEnding() {
        assertEquals("03 Night Rain", PhoneMusic.stationName("03 Night Rain.mp3"))
        assertEquals("Lullaby.v2", PhoneMusic.stationName("Lullaby.v2.ogg"))
        assertEquals(".hidden", PhoneMusic.stationName(".hidden"))
        assertEquals("No ending", PhoneMusic.stationName("No ending"))
    }
}
