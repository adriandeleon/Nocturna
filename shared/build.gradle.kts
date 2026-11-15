import org.jetbrains.kotlin.gradle.dsl.JvmTarget

/*
 * Code shared by the Android app and a future iOS app: plain Kotlin and Compose Multiplatform only.
 * Anything that needs an Android or Java API stays in :app until it sits behind an interface.
 */
plugins {
    alias(libs.plugins.kotlin.multiplatform)
    alias(libs.plugins.android.kotlin.multiplatform.library)
    alias(libs.plugins.kotlin.compose)
    // Compose Multiplatform resources: the Res class for the text in composeResources.
    alias(libs.plugins.compose.multiplatform)
    // The multiplatform library has no Android Lint of its own; this adds :shared:lint.
    alias(libs.plugins.android.lint)
}

kotlin {
    compilerOptions {
        // PlatformLocale is an expect class: java.util.Locale on Android, NSLocale on iOS.
        freeCompilerArgs.add("-Xexpect-actual-classes")
    }

    android {
        namespace = "com.adl.nocturna.shared"
        compileSdk = 37
        minSdk = 27

        compilerOptions {
            jvmTarget.set(JvmTarget.JVM_17)
        }

        // commonTest runs on the JVM as Android host tests: ./gradlew :shared:testAndroidHostTest
        withHostTest {}

        // Packs composeResources into the APK's assets, where Res reads them on Android.
        androidResources { enable = true }
    }

    // Kotlin/Native only builds these on macOS. Elsewhere Gradle skips them, but commonMain is still
    // compiled on its own (compileCommonMainKotlinMetadata), which fails on any Java or Android API.
    listOf(iosArm64(), iosSimulatorArm64()).forEach {
        it.binaries.framework {
            baseName = "Shared"
            isStatic = true
        }
    }

    sourceSets {
        commonMain.dependencies {
            implementation(libs.compose.mp.runtime)
            implementation(libs.compose.mp.foundation)
            implementation(libs.compose.mp.ui)
            implementation(libs.compose.mp.material3)
            implementation(libs.compose.mp.components.resources)
            implementation(libs.lifecycle.mp.runtime.compose)
            api(libs.kotlinx.datetime)
            implementation(libs.kotlinx.serialization.json)
            api(libs.kotlinx.coroutines.core)
            implementation(libs.atomicfu)
        }
        commonTest.dependencies {
            implementation(libs.kotlin.test)
            implementation(libs.kotlinx.coroutines.test)
        }
    }
}

// The text of the shared screens: shared/src/commonMain/composeResources, read through Res.string and Res.plurals.
compose.resources {
    packageOfResClass = "com.adl.nocturna.resources"
    publicResClass = false
}
