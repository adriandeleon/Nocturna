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

/** At this temperature (°C) and above, the scene doesn't show the cold at all. */
const val CHILLY_FROM_C = 20.0

/** At this temperature (°C) and below, the scene is fully frosted over. */
const val FROSTED_AT_C = 5.0

/**
 * How cold the scene looks at [temperatureC]: 0 from [CHILLY_FROM_C] up, rising to 1 (fully frosted) at
 * [FROSTED_AT_C] and below, quickly at first so a chilly day already shows. The hills frost over, the sky
 * turns icy and the flowers thin out.
 */
fun sceneCold(temperatureC: Double): Float {
    if (temperatureC.isNaN()) return 0f
    val x = ((CHILLY_FROM_C - temperatureC) / (CHILLY_FROM_C - FROSTED_AT_C)).coerceIn(0.0, 1.0)
    return (1 - (1 - x) * (1 - x)).toFloat()
}
