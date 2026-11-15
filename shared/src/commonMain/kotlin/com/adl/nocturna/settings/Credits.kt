package com.adl.nocturna.settings

import com.adl.nocturna.resources.*
import com.adl.nocturna.text.PlatformLocale
import com.adl.nocturna.text.strings
import org.jetbrains.compose.resources.StringResource

/** Where the code lives; the About screen links to it, its issues and its licence. */
const val REPOSITORY_URL = "https://github.com/adriandeleon/Nocturna"

/** The author's homepage, linked from the About screen. */
const val HOMEPAGE_URL = "https://www.adeleon.dev"

const val COPYRIGHT = "Copyright © 2026 Adrián Arturo De León Saldivar"

/** One line of credit on the About screen. [url] matches the one in `NOTICE`. */
data class Credit(val name: String, val detail: StringResource, val licence: String, val url: String)

/** Every library the APK carries. Kept in step with `NOTICE` (a unit test checks it). */
val LIBRARY_CREDITS = listOf(
    Credit(
        "Android Jetpack (AndroidX)", Res.string.credit_androidx, "Apache License 2.0",
        "https://developer.android.com/jetpack/androidx",
    ),
    Credit("Jetpack Compose", Res.string.credit_compose, "Apache License 2.0", "https://developer.android.com/jetpack/compose"),
    Credit(
        "Compose Multiplatform", Res.string.credit_compose_mp, "Apache License 2.0",
        "https://github.com/JetBrains/compose-multiplatform",
    ),
    Credit("AndroidX Media3", Res.string.credit_media3, "Apache License 2.0", "https://github.com/androidx/media"),
    Credit("kotlinx-datetime", Res.string.credit_datetime, "Apache License 2.0", "https://github.com/Kotlin/kotlinx-datetime"),
    Credit(
        "kotlinx.serialization", Res.string.credit_serialization, "Apache License 2.0",
        "https://github.com/Kotlin/kotlinx.serialization",
    ),
    Credit("kotlinx.coroutines", Res.string.credit_coroutines, "Apache License 2.0", "https://github.com/Kotlin/kotlinx.coroutines"),
    Credit("atomicfu", Res.string.credit_atomicfu, "Apache License 2.0", "https://github.com/Kotlin/kotlinx-atomicfu"),
    Credit("Kotlin standard library", Res.string.credit_kotlin, "Apache License 2.0", "https://kotlinlang.org"),
)

/** Outside services whose data the app shows. Their CC BY 4.0 licence asks for this credit. */
val DATA_CREDITS = listOf(
    Credit("Open-Meteo", Res.string.credit_open_meteo, "CC BY 4.0", "https://open-meteo.com"),
    Credit("GeoNames", Res.string.credit_geonames, "CC BY 4.0", "https://www.geonames.org"),
)

/** "Version 0.1.0 (1)", with "· debug" for a debug build. */
fun versionLabel(versionName: String, versionCode: Int, debug: Boolean, locale: PlatformLocale): String =
    strings(locale).version(versionName, versionCode) + if (debug) " · debug" else ""
