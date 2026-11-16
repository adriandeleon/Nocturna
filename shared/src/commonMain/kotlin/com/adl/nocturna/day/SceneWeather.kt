package com.adl.nocturna.day

/** The weather day mode's illustrated scene shows (Settings › Day mode › Weather in the scene). */
enum class SceneWeather { CLEAR, PARTLY, CLOUDY, FOG, RAIN, SNOW, STORM }

/** The scene's weather for WMO weather [code], as Open-Meteo gives it. */
fun sceneWeather(code: Int): SceneWeather = when {
    code == 0 -> SceneWeather.CLEAR
    code == 1 || code == 2 -> SceneWeather.PARTLY
    code == 45 || code == 48 -> SceneWeather.FOG
    code in 95..99 -> SceneWeather.STORM
    isRainCode(code) -> SceneWeather.RAIN
    isSnowCode(code) -> SceneWeather.SNOW
    else -> SceneWeather.CLOUDY
}

/** How much grey the weather puts in the sky, 0 (none) to 1. */
fun SceneWeather.overcast(): Float = when (this) {
    SceneWeather.CLEAR -> 0f
    SceneWeather.PARTLY -> 0.1f
    SceneWeather.CLOUDY -> 0.45f
    SceneWeather.FOG -> 0.5f
    SceneWeather.RAIN -> 0.55f
    SceneWeather.SNOW -> 0.4f
    SceneWeather.STORM -> 0.75f
}

/** How many clouds cross the sky: fair weather's three, more as it clouds over. */
fun SceneWeather.cloudCount(): Int = when (this) {
    SceneWeather.CLEAR, SceneWeather.PARTLY, SceneWeather.FOG -> 3
    SceneWeather.CLOUDY, SceneWeather.SNOW -> 5
    SceneWeather.RAIN, SceneWeather.STORM -> 6
}

/** Whether the sun or moon shows through: not behind a full cover of cloud. */
fun SceneWeather.showsSun(): Boolean = this == SceneWeather.CLEAR || this == SceneWeather.PARTLY
