package com.adl.nocturna.radio

import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class RadioBrowserTest {
    private fun station(
        name: String, url: String, resolved: String = url, check: Int = 1,
        tags: String = "jazz,smooth jazz", country: String = "mx", codec: String = "MP3", bitrate: Int = 128,
    ) = """{"name":"$name","url":"$url","url_resolved":"$resolved","lastcheckok":$check,"tags":"$tags",""" +
        """"countrycode":"$country","codec":"$codec","bitrate":$bitrate}"""

    @Test fun readsPlayableStations() {
        val found = parseRadioBrowser("[${station("  Jazz   FM ", "http://a.example/pls", "https://a.example/stream")}]")
        assertEquals(1, found.size)
        val f = found[0]
        assertEquals("Jazz FM", f.name)
        assertEquals("https://a.example/stream", f.url)
        assertEquals("MX", f.countryCode)
        assertEquals(listOf("jazz", "smooth jazz"), f.tags)
        assertEquals("MX · jazz, smooth jazz · 128 kbps", f.detail)
        assertEquals("Jazz FM", f.station.name)
    }

    @Test fun skipsBrokenNamelessAndDuplicateStations() {
        val body = "[" + listOf(
            station("Broken", "https://b.example/", check = 0),
            station("", "https://c.example/"),
            station("Not http", "rtmp://d.example/", resolved = ""),
            station("One", "https://e.example/"),
            station("One again", "https://e.example/"),
            "42",
        ).joinToString(",") + "]"
        assertEquals(listOf("One"), parseRadioBrowser(body).map { it.name })
    }

    @Test fun fallsBackToTheUrlWhenThereIsNoResolvedOne() =
        assertEquals("https://f.example/", parseRadioBrowser("[${station("F", "https://f.example/", resolved = "")}]").single().url)

    @Test fun detailLeavesOutWhatIsUnknown() {
        val f = parseRadioBrowser("[${station("G", "https://g.example/", tags = "", country = "", codec = "unknown", bitrate = 0)}]").single()
        assertEquals("", f.detail)
        val aac = parseRadioBrowser("[${station("H", "https://h.example/", tags = "", country = "", codec = "AAC", bitrate = 0)}]").single()
        assertEquals("AAC", aac.detail)
    }

    @Test fun anythingButAnArrayIsNoStations() {
        assertTrue(parseRadioBrowser("").isEmpty())
        assertTrue(parseRadioBrowser("{\"error\":1}").isEmpty())
        assertTrue(parseRadioBrowser("<html>").isEmpty())
    }

    @Test fun urlsAskForWorkingStationsMostPlayedFirst() {
        val search = radioSearchUrl(" café jazz ")
        assertTrue(search.startsWith("https://all.api.radio-browser.info/json/stations/search?name=caf%C3%A9+jazz&"))
        assertTrue("hidebroken=true" in search && "order=clickcount" in search && "reverse=true" in search)
        assertTrue("tag=jazz" in radioTagUrl("Jazz"))
        assertTrue("country=Mexico" in radioCountryUrl("Mexico"))
        assertTrue("geo_lat=25.6866&geo_long=-100.3161&geo_distance=$NEARBY_METRES" in radioNearbyUrl(25.68661, -100.31611))
    }

    @Test fun searchMergesNameGenreAndCountryMatchesNameFirst() = runTest {
        val replies = mapOf(
            "name=" to "[${station("By name", "https://1.example/")},${station("Both", "https://2.example/")}]",
            "tag=" to "[${station("Both", "https://2.example/")},${station("By tag", "https://3.example/")}]",
            "country=" to "boom",
        )
        val browser = RadioBrowser { url -> replies.entries.first { it.key in url }.value.also { if (it == "boom") error("down") } }
        assertEquals(listOf("By name", "Both", "By tag"), browser.search("jazz").map { it.name })
        assertTrue(browser.search("  ").isEmpty())
    }

    @Test fun nearbyAsksAroundThePlace() = runTest {
        var asked = ""
        val browser = RadioBrowser { url -> asked = url; "[${station("Local", "https://l.example/")}]" }
        assertEquals("Local", browser.nearby(25.0, -100.0).single().name)
        assertTrue("geo_lat=25.0000&geo_long=-100.0000" in asked)
    }
}
