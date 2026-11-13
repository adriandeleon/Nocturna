package com.adl.nocturna.settings

/** Where the code lives; the About screen links to it, its issues and its licence. */
const val REPOSITORY_URL = "https://github.com/adriandeleon/Nocturna"

const val COPYRIGHT = "Copyright © 2026 Adrián Arturo De León Saldivar"

/** One line of credit on the About screen. [url] matches the one in `NOTICE`. */
data class Credit(val name: String, val detail: String, val licence: String, val url: String)

/** Every library the APK carries. Kept in step with `NOTICE` (a unit test checks it). */
val LIBRARY_CREDITS = listOf(
    Credit(
        "Android Jetpack (AndroidX)", "Core, Activity, Lifecycle and SavedState", "Apache License 2.0",
        "https://developer.android.com/jetpack/androidx",
    ),
    Credit("Jetpack Compose", "UI and Material 3", "Apache License 2.0", "https://developer.android.com/jetpack/compose"),
    Credit("AndroidX Media3", "ExoPlayer, HLS and media session", "Apache License 2.0", "https://github.com/androidx/media"),
    Credit("Kotlin standard library", "JetBrains and Kotlin contributors", "Apache License 2.0", "https://kotlinlang.org"),
)

/** Outside services whose data the app shows. Their CC BY 4.0 licence asks for this credit. */
val DATA_CREDITS = listOf(
    Credit("Open-Meteo", "Weather and city search", "CC BY 4.0", "https://open-meteo.com"),
    Credit("GeoNames", "Place names behind the city search", "CC BY 4.0", "https://www.geonames.org"),
)

/** "Version 0.1.0 (1)", with "· debug" for a debug build. */
fun versionLabel(versionName: String, versionCode: Int, debug: Boolean): String =
    "Version $versionName ($versionCode)" + if (debug) " · debug" else ""
