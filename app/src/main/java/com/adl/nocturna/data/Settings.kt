package com.adl.nocturna.data

import android.content.Context
import android.content.SharedPreferences
import androidx.core.content.edit
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import org.json.JSONArray
import org.json.JSONObject

enum class ClockStyle { DIGITAL, ANALOG }

enum class ClockFont { THIN, LIGHT, REGULAR, CONDENSED, MONO, SERIF }

data class Station(val name: String, val url: String)

data class Settings(
    // Clock
    val style: ClockStyle = ClockStyle.DIGITAL,
    val use24h: Boolean = true,
    val showSeconds: Boolean = false,
    val font: ClockFont = ClockFont.THIN,
    val color: Int = 0xFF8FD3FF.toInt(),
    // Info line
    val showDate: Boolean = true,
    val showBattery: Boolean = true,
    val showNextAlarm: Boolean = true,
    val showWeather: Boolean = true,
    val showNotifications: Boolean = true,
    // Brightness and night mode
    val autoBrightness: Boolean = true,
    val minBrightness: Float = 0.02f,
    val maxBrightness: Float = 0.8f,
    val nightModeEnabled: Boolean = true,
    /** Ambient light (lux) below which the clock switches to the dim red night look. */
    val nightModeLux: Float = 4f,
    val burnInProtection: Boolean = true,
    // Launching
    val autoStartOnCharge: Boolean = false,
    // Weather
    val weatherUseDeviceLocation: Boolean = false,
    val weatherCity: String = "",
    val weatherLat: Double = Double.NaN,
    val weatherLon: Double = Double.NaN,
    val weatherFahrenheit: Boolean = false,
    // Alarms
    val snoozeMinutes: Int = 9,
    val alarmRampSeconds: Int = 45,
    val alarmVibrate: Boolean = true,
    // Radio
    val stations: List<Station> = DEFAULT_STATIONS,
    val selectedStation: Int = 0,
) {
    val hasWeatherLocation: Boolean get() = !weatherLat.isNaN() && !weatherLon.isNaN()
    val currentStation: Station? get() = stations.getOrNull(selectedStation) ?: stations.firstOrNull()

    companion object {
        val DEFAULT_STATIONS = listOf(
            Station("SomaFM Groove Salad", "https://ice1.somafm.com/groovesalad-128-mp3"),
            Station("SomaFM Drone Zone", "https://ice1.somafm.com/dronezone-128-mp3"),
            Station("SomaFM Deep Space One", "https://ice1.somafm.com/deepspaceone-128-mp3"),
        )
        val COLORS = listOf(
            0xFF8FD3FF, 0xFFFFFFFF, 0xFFFFD27A, 0xFFFF8A65,
            0xFFFF5252, 0xFFB388FF, 0xFF69F0AE, 0xFF80CBC4,
        ).map { it.toInt() }
    }
}

/** App-wide settings, persisted as one JSON blob and exposed as a [StateFlow]. */
object SettingsStore {
    private const val KEY = "settings"
    private lateinit var prefs: SharedPreferences
    private val state = MutableStateFlow(Settings())

    val settings: StateFlow<Settings> = state.asStateFlow()
    val current: Settings get() = state.value

    fun init(context: Context) {
        prefs = context.getSharedPreferences("nocturna", Context.MODE_PRIVATE)
        state.value = prefs.getString(KEY, null)?.let { runCatching { decode(JSONObject(it)) }.getOrNull() } ?: Settings()
    }

    fun update(block: (Settings) -> Settings) {
        synchronized(this) {
            val next = block(state.value)
            state.value = next
            prefs.edit { putString(KEY, encode(next).toString()) }
        }
    }

    private fun encode(s: Settings) = JSONObject().apply {
        put("style", s.style.name)
        put("use24h", s.use24h)
        put("showSeconds", s.showSeconds)
        put("font", s.font.name)
        put("color", s.color)
        put("showDate", s.showDate)
        put("showBattery", s.showBattery)
        put("showNextAlarm", s.showNextAlarm)
        put("showWeather", s.showWeather)
        put("showNotifications", s.showNotifications)
        put("autoBrightness", s.autoBrightness)
        put("minBrightness", s.minBrightness.toDouble())
        put("maxBrightness", s.maxBrightness.toDouble())
        put("nightModeEnabled", s.nightModeEnabled)
        put("nightModeLux", s.nightModeLux.toDouble())
        put("burnInProtection", s.burnInProtection)
        put("autoStartOnCharge", s.autoStartOnCharge)
        put("weatherUseDeviceLocation", s.weatherUseDeviceLocation)
        put("weatherCity", s.weatherCity)
        if (s.hasWeatherLocation) {
            put("weatherLat", s.weatherLat)
            put("weatherLon", s.weatherLon)
        }
        put("weatherFahrenheit", s.weatherFahrenheit)
        put("snoozeMinutes", s.snoozeMinutes)
        put("alarmRampSeconds", s.alarmRampSeconds)
        put("alarmVibrate", s.alarmVibrate)
        put("stations", JSONArray().apply {
            s.stations.forEach { put(JSONObject().put("name", it.name).put("url", it.url)) }
        })
        put("selectedStation", s.selectedStation)
    }

    private fun decode(j: JSONObject): Settings {
        val d = Settings()
        return Settings(
            style = enumOr(j.optString("style"), d.style),
            use24h = j.optBoolean("use24h", d.use24h),
            showSeconds = j.optBoolean("showSeconds", d.showSeconds),
            font = enumOr(j.optString("font"), d.font),
            color = j.optInt("color", d.color),
            showDate = j.optBoolean("showDate", d.showDate),
            showBattery = j.optBoolean("showBattery", d.showBattery),
            showNextAlarm = j.optBoolean("showNextAlarm", d.showNextAlarm),
            showWeather = j.optBoolean("showWeather", d.showWeather),
            showNotifications = j.optBoolean("showNotifications", d.showNotifications),
            autoBrightness = j.optBoolean("autoBrightness", d.autoBrightness),
            minBrightness = j.optDouble("minBrightness", d.minBrightness.toDouble()).toFloat(),
            maxBrightness = j.optDouble("maxBrightness", d.maxBrightness.toDouble()).toFloat(),
            nightModeEnabled = j.optBoolean("nightModeEnabled", d.nightModeEnabled),
            nightModeLux = j.optDouble("nightModeLux", d.nightModeLux.toDouble()).toFloat(),
            burnInProtection = j.optBoolean("burnInProtection", d.burnInProtection),
            autoStartOnCharge = j.optBoolean("autoStartOnCharge", d.autoStartOnCharge),
            weatherUseDeviceLocation = j.optBoolean("weatherUseDeviceLocation", d.weatherUseDeviceLocation),
            weatherCity = j.optString("weatherCity", d.weatherCity),
            weatherLat = j.optDouble("weatherLat", Double.NaN),
            weatherLon = j.optDouble("weatherLon", Double.NaN),
            weatherFahrenheit = j.optBoolean("weatherFahrenheit", d.weatherFahrenheit),
            snoozeMinutes = j.optInt("snoozeMinutes", d.snoozeMinutes),
            alarmRampSeconds = j.optInt("alarmRampSeconds", d.alarmRampSeconds),
            alarmVibrate = j.optBoolean("alarmVibrate", d.alarmVibrate),
            stations = j.optJSONArray("stations")?.let { arr ->
                (0 until arr.length()).map { i ->
                    val o = arr.getJSONObject(i)
                    Station(o.getString("name"), o.getString("url"))
                }
            } ?: d.stations,
            selectedStation = j.optInt("selectedStation", d.selectedStation),
        )
    }

    private inline fun <reified E : Enum<E>> enumOr(name: String, fallback: E): E =
        enumValues<E>().firstOrNull { it.name == name } ?: fallback
}
