package com.adl.nocturna.day

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class SceneWeatherTest {
    @Test fun weatherCodes() {
        assertEquals(SceneWeather.CLEAR, sceneWeather(0))
        assertEquals(SceneWeather.PARTLY, sceneWeather(2))
        assertEquals(SceneWeather.CLOUDY, sceneWeather(3))
        assertEquals(SceneWeather.FOG, sceneWeather(45))
        assertEquals(SceneWeather.RAIN, sceneWeather(53))
        assertEquals(SceneWeather.RAIN, sceneWeather(81))
        assertEquals(SceneWeather.SNOW, sceneWeather(75))
        assertEquals(SceneWeather.STORM, sceneWeather(95))
    }

    @Test fun fairWeatherKeepsTheSceneAsItWas() {
        assertEquals(0f, SceneWeather.CLEAR.overcast())
        assertEquals(3, SceneWeather.CLEAR.cloudCount())
        assertTrue(SceneWeather.PARTLY.showsSun())
        assertFalse(SceneWeather.RAIN.showsSun())
        assertTrue(SceneWeather.STORM.overcast() > SceneWeather.CLOUDY.overcast())
    }
}
