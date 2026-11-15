package com.adl.nocturna.weather

import kotlin.test.Test
import kotlin.test.assertEquals

class OpenMeteoTest {
    @Test fun fourDecimals() {
        assertEquals("25.6866", fixed4(25.68661))
        assertEquals("-100.3161", fixed4(-100.31609))
        assertEquals("0.0000", fixed4(0.0))
        assertEquals("-0.5000", fixed4(-0.5))
        assertEquals("12.0001", fixed4(12.00005))
    }

    @Test fun queriesAreEncodedLikeUrlEncoder() {
        assertEquals("S%C3%A3o+Paulo", formEncode("São Paulo"))
        assertEquals("a%26b%3Dc%2Fd%3F", formEncode("a&b=c/d?"))
        assertEquals("keep.-*_", formEncode("keep.-*_"))
        assertEquals("%E6%9D%B1%E4%BA%AC", formEncode("東京"))
    }

    @Test fun forecastUrl() = assertEquals(
        "https://api.open-meteo.com/v1/forecast?latitude=25.6866&longitude=-100.3161" +
            "&current=temperature_2m,weather_code,is_day" +
            "&daily=weather_code,temperature_2m_max,temperature_2m_min&forecast_days=3&timezone=auto",
        forecastUrl(25.6866, -100.3161),
    )

    @Test fun geocodeUrlTrimsTheQuery() = assertEquals(
        "https://geocoding-api.open-meteo.com/v1/search?name=San+Pedro&count=1&language=es",
        geocodeUrl("  San Pedro ", "es"),
    )
}
