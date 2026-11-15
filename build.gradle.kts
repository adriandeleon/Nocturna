plugins {
    alias(libs.plugins.android.application) apply false
    alias(libs.plugins.android.kotlin.multiplatform.library) apply false
    alias(libs.plugins.android.lint) apply false
    alias(libs.plugins.kotlin.multiplatform) apply false
    // AGP 9 has Kotlin built in; declaring KGP here only pins the Kotlin version it uses.
    alias(libs.plugins.kotlin.android) apply false
    alias(libs.plugins.kotlin.compose) apply false
}
