package com.adl.nocturna.weather

import com.adl.nocturna.data.Settings
import kotlinx.coroutines.test.runTest
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.time.Clock
import kotlin.time.Duration
import kotlin.time.Duration.Companion.minutes
import kotlin.time.Instant

class WeatherRepositoryTest {
    private var now = Instant.parse("2026-11-14T22:00:00Z")
    private val fetched = mutableListOf<String>()
    private var temperature = 18.0
    private var fail = false

    private val monterrey = Settings(weatherCity = "Monterrey", weatherLat = 25.6866, weatherLon = -100.3161)

    @BeforeTest fun fakeNetwork() {
        WeatherRepository.invalidate()
        WeatherRepository.clock = object : Clock {
            override fun now() = this@WeatherRepositoryTest.now
        }
        WeatherRepository.get = { url ->
            fetched += url
            if (fail) error("offline")
            """{"current":{"temperature_2m":$temperature,"weather_code":3,"is_day":1}}"""
        }
    }

    @AfterTest fun realNetwork() {
        WeatherRepository.get = { httpGet(it) }
        WeatherRepository.clock = Clock.System
        WeatherRepository.invalidate()
    }

    private fun later(by: Duration) {
        now += by
    }

    @Test fun fetchesOnceThenWaitsHalfAnHour() = runTest {
        WeatherRepository.refreshIfStale(monterrey) { null }
        assertEquals(18.0, WeatherRepository.weather.value?.temperatureC)
        later(29.minutes)
        WeatherRepository.refreshIfStale(monterrey) { null }
        assertEquals(1, fetched.size)
        later(1.minutes)
        temperature = 20.0
        WeatherRepository.refreshIfStale(monterrey) { null }
        assertEquals(2, fetched.size)
        assertEquals(20.0, WeatherRepository.weather.value?.temperatureC)
    }

    @Test fun anotherPlaceFetchesAtOnce() = runTest {
        WeatherRepository.refreshIfStale(monterrey) { null }
        WeatherRepository.refreshIfStale(monterrey.copy(weatherLat = 19.43, weatherLon = -99.13)) { null }
        assertEquals(2, fetched.size)
        assertEquals(19.43, WeatherRepository.weather.value?.lat)
    }

    @Test fun thePhonesLocationWinsWhenAskedFor() = runTest {
        WeatherRepository.refreshIfStale(monterrey.copy(weatherUseDeviceLocation = true)) { 40.4168 to -3.7038 }
        assertEquals(forecastUrl(40.4168, -3.7038), fetched.single())
    }

    @Test fun withoutTheLocationTheCityIsUsed() = runTest {
        WeatherRepository.refreshIfStale(monterrey.copy(weatherUseDeviceLocation = true)) { null }
        assertEquals(forecastUrl(25.6866, -100.3161), fetched.single())
    }

    @Test fun noPlaceMeansNoFetch() = runTest {
        WeatherRepository.refreshIfStale(Settings()) { null }
        assertEquals(emptyList(), fetched)
        assertNull(WeatherRepository.weather.value)
    }

    @Test fun aFailedFetchKeepsWhatWeHad() = runTest {
        WeatherRepository.refreshIfStale(monterrey) { null }
        later(31.minutes)
        fail = true
        WeatherRepository.refreshIfStale(monterrey) { null }
        assertEquals(18.0, WeatherRepository.weather.value?.temperatureC)
    }

    @Test fun geocodeReadsTheBestMatch() = runTest {
        WeatherRepository.get = { """{"results":[{"name":"Monterrey","country":"México","latitude":25.67,"longitude":-100.31}]}""" }
        assertEquals(Place("Monterrey, México", 25.67, -100.31), WeatherRepository.geocode("monterrey", "es"))
    }
}
