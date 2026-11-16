package com.adl.nocturna.weather

import com.adl.nocturna.text.spanishTestLocale
import com.adl.nocturna.text.testLocale
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class AirQualityTest {
    @Test fun readsTheCurrentUsAqi() {
        val body = """{"latitude":40.4,"utc_offset_seconds":3600,"current":{"time":"2026-11-16T22:00","interval":3600,"us_aqi":25}}"""
        assertEquals(AirQuality(25, 1000L, 40.4, -3.7), parseAirQuality(body, 40.4, -3.7, 1000L))
    }

    @Test fun noIndexNoAirQuality() =
        assertNull(parseAirQuality("""{"current":{"time":"2026-11-16T22:00","us_aqi":null}}""", 0.0, 0.0, 0L))

    @Test fun bandsFollowTheUsIndex() {
        assertEquals(AirLevel.GOOD, airLevel(0))
        assertEquals(AirLevel.GOOD, airLevel(50))
        assertEquals(AirLevel.MODERATE, airLevel(51))
        assertEquals(AirLevel.SENSITIVE, airLevel(150))
        assertEquals(AirLevel.UNHEALTHY, airLevel(151))
        assertEquals(AirLevel.VERY_UNHEALTHY, airLevel(300))
        assertEquals(AirLevel.HAZARDOUS, airLevel(301))
    }

    @Test fun airText() {
        assertEquals("Air: good (42)", airQualityText(AirQuality(42, 0L, 0.0, 0.0), testLocale))
        assertEquals("Aire: moderado (87)", airQualityText(AirQuality(87, 0L, 0.0, 0.0), spanishTestLocale))
    }

    @Test fun uvBandsAndText() {
        assertEquals(UvLevel.LOW, uvLevel(2.4))
        assertEquals(UvLevel.MODERATE, uvLevel(2.5))
        assertEquals(UvLevel.HIGH, uvLevel(7.2))
        assertEquals(UvLevel.VERY_HIGH, uvLevel(10.4))
        assertEquals(UvLevel.EXTREME, uvLevel(11.0))
        assertEquals("UV 7 · High", uvText(7.2, testLocale))
        assertEquals("UV 0 · Low", uvText(0.0, testLocale))
        assertEquals("UV 9 · Muy alto", uvText(8.6, spanishTestLocale))
    }
}
