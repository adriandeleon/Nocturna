package com.adl.nocturna.day

import com.adl.nocturna.data.WorldZone
import com.adl.nocturna.text.testLocale
import kotlinx.datetime.TimeZone
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue
import kotlin.time.Instant

class WorldClockTest {
    private val monterrey = TimeZone.of("America/Monterrey") // UTC-6, no daylight saving
    private val london = TimeZone.of("Europe/London")
    private val tokyo = TimeZone.of("Asia/Tokyo")
    private val londonZone = WorldZone("London", "Europe/London")
    private val tokyoZone = WorldZone("Tokyo", "Asia/Tokyo")

    private fun at(iso: String) = Instant.parse(iso)

    private fun time(zone: WorldZone, now: String, home: TimeZone = monterrey, use24h: Boolean = true) =
        worldTime(zone, at(now), home, use24h, testLocale)

    @Test fun sameDayHasNoMark() = assertEquals("London 19:42", time(londonZone, "2026-11-13T19:42:00Z"))

    @Test fun tomorrowThereIsPlusOne() = assertEquals("Tokyo 04:42 +1", time(tokyoZone, "2026-11-13T19:42:00Z"))

    @Test fun yesterdayThereIsMinusOne() =
        assertEquals("Los Angeles 17:00 −1", time(WorldZone("Los Angeles", "America/Los_Angeles"), "2026-11-14T01:00:00Z", home = tokyo))

    @Test fun followsTheTwelveHourClock() =
        assertEquals("tokyo 4:42 am +1", time(tokyoZone, "2026-11-13T19:42:00Z", use24h = false)?.lowercase())

    @Test fun dayOffsetFlipsAtMidnightThereNotHere() {
        // 17:59 in Monterrey: still the 13th in London a minute before midnight, the 14th a minute later.
        assertEquals(0, dayOffset(at("2026-11-13T23:59:00Z"), monterrey, london))
        assertEquals(1, dayOffset(at("2026-11-14T00:00:00Z"), monterrey, london))
        // And back to the same day when midnight reaches Monterrey too.
        assertEquals(1, dayOffset(at("2026-11-14T05:59:00Z"), monterrey, london))
        assertEquals(0, dayOffset(at("2026-11-14T06:00:00Z"), monterrey, london))
    }

    @Test fun dayOffsetAcrossTheEndOfTheYear() =
        assertEquals(1, dayOffset(at("2026-12-31T16:00:00Z"), monterrey, tokyo))

    @Test fun differencesFollowDaylightSavingOnEachSide() {
        val newYork = WorldZone("New York", "America/New_York")
        // Five hours apart, then four in the weeks between the US (8 March) and the UK (29 March) changing clocks.
        assertEquals("New York 07:00", time(newYork, "2026-03-02T12:00:00Z", home = london))
        assertEquals("New York 08:00", time(newYork, "2026-03-16T12:00:00Z", home = london))
        assertEquals("New York 07:00", time(newYork, "2026-04-06T11:00:00Z", home = london))
    }

    @Test fun southernSummerMovesTheDayOffset() {
        val sydney = WorldZone("Sydney", "Australia/Sydney")
        // At 13:30 in London Sydney is past midnight in its summer (UTC+11), but not in its winter (UTC+10).
        assertEquals("Sydney 00:30 +1", time(sydney, "2026-01-15T13:30:00Z", home = london))
        assertEquals("Sydney 23:30", time(sydney, "2026-07-15T13:30:00Z", home = london))
    }

    @Test fun halfAndQuarterHourZones() {
        assertEquals("Kathmandu 18:15", time(WorldZone("Kathmandu", "Asia/Kathmandu"), "2026-11-13T12:30:00Z", home = london))
        assertEquals("St. John's 09:00", time(WorldZone("St. John's", "America/St_Johns"), "2026-11-13T12:30:00Z", home = london))
    }

    @Test fun unknownZoneShowsNothing() = assertNull(time(WorldZone("Atlantis", "Atlantic/Atlantis"), "2026-11-13T19:42:00Z"))

    @Test fun lineJoinsZonesAndKeepsEachOnOneLine() {
        val line = worldClockLine(
            listOf(WorldZone("New York", "America/New_York"), WorldZone("Nowhere", "Nowhere/Here"), tokyoZone),
            at("2026-11-13T19:42:00Z"), monterrey, use24h = true, testLocale,
        )
        assertEquals("New York 14:42 · Tokyo 04:42 +1", line)
    }

    @Test fun noZonesNoLine() {
        assertNull(worldClockLine(emptyList(), at("2026-11-13T19:42:00Z"), monterrey, true, testLocale))
        assertNull(worldClockLine(listOf(WorldZone("X", "Bad/Zone")), at("2026-11-13T19:42:00Z"), monterrey, true, testLocale))
    }

    @Test fun longLabelsAreCut() {
        assertEquals("Ho Chi Minh City", shortLabel("  Ho Chi Minh City "))
        assertEquals("Santa Cruz de T…", shortLabel("Santa Cruz de Tenerife"))
        assertEquals(WORLD_LABEL_MAX, shortLabel("Llanfairpwllgwyngyll").length)
    }

    @Test fun marks() {
        assertEquals("", dayOffsetMark(0))
        assertEquals("+1", dayOffsetMark(1))
        assertEquals("−1", dayOffsetMark(-1))
    }

    @Test fun everyCityHasAZoneAndAShortName() {
        assertTrue(CITIES.size >= 150, "${CITIES.size} cities")
        CITIES.forEach { city ->
            assertNotNull(timeZoneOrNull(city.zoneId), city.zoneId)
            assertTrue(city.name.length <= WORLD_LABEL_MAX, city.name)
        }
        assertEquals(CITIES.size, CITIES.map { it.name }.toSet().size)
    }

    @Test fun searchFindsCitiesByTheStartOfTheirName() {
        assertEquals("Tokyo", searchZones("tok", emptyList()).first().label)
        assertEquals("New York", searchZones("york", emptyList()).first().label)
    }

    @Test fun searchIgnoresAccentsAndCase() {
        assertEquals("São Paulo", searchZones("SAO pa", emptyList()).single().label)
        assertEquals("Bogotá", searchZones("bogota", emptyList()).single().label)
    }

    @Test fun searchFindsCitiesByCountry() =
        assertEquals(listOf("Osaka", "Tokyo"), searchZones("japan", emptyList()).map { it.label })

    @Test fun searchFallsBackToZoneIds() {
        val ids = listOf("America/Argentina/Cordoba", "Etc/GMT+5", "Europe/London", "GMT")
        val found = searchZones("cordoba", ids)
        assertEquals(listOf(ZoneChoice("Cordoba", "America/Argentina/Cordoba", "America/Argentina/Cordoba")), found)
        assertEquals(WorldZone("Cordoba", "America/Argentina/Cordoba"), found.single().toWorldZone())
        // Zones already listed under a city, and the Etc/ and bare IDs, aren't offered by ID.
        assertEquals(listOf("London"), searchZones("london", ids).map { it.label })
        assertTrue(searchZones("gmt", ids).isEmpty())
        assertEquals(listOf("Beijing", "Shanghai", "Shenzhen"), searchZones("asia/shang", ids).map { it.label })
    }

    @Test fun blankSearchListsEveryCityByName() {
        val all = searchZones("  ", listOf("America/Argentina/Cordoba"))
        assertEquals(CITIES.size, all.size)
        assertEquals("Abu Dhabi", all.first().label)
    }

    @Test fun landscapeDropsTheLineBeforeTheNote() {
        assertEquals(DeskFit(worldLine = true, note = true), deskFit(withLine = 300, without = 260, gap = 32, noteMin = 100, max = 440))
        assertEquals(DeskFit(worldLine = false, note = true), deskFit(withLine = 300, without = 260, gap = 32, noteMin = 100, max = 400))
        assertEquals(DeskFit(worldLine = true, note = false), deskFit(withLine = 300, without = 260, gap = 32, noteMin = 100, max = 380))
        assertEquals(DeskFit(worldLine = true, note = false), deskFit(withLine = 300, without = 260, gap = 32, noteMin = null, max = 300))
        assertEquals(DeskFit(worldLine = false, note = false), deskFit(withLine = 300, without = 260, gap = 32, noteMin = null, max = 280))
    }

    @Test fun portraitKeepsTheLineWhileTheClockStaysSmall() {
        assertTrue(worldLineFitsPortrait(withLine = 280, max = 700))
        assertFalse(worldLineFitsPortrait(withLine = 281, max = 700))
    }
}
