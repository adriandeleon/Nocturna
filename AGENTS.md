# Nocturna: rules for coding agents

Nocturna is a bedside clock for Android: Kotlin, Jetpack Compose, Media3, Gradle with a version catalog,
package `com.adl.nocturna`. minSdk 27, targetSdk 36, compileSdk 37, JDK 17+. Two modules:

- `:app`, the Android app: Activities, services, receivers and everything that calls an Android or Java API.
- `:shared`, Kotlin Multiplatform with Compose Multiplatform, built for Android and iOS: code with no Android
  or Java API in it, in `shared/src/commonMain`, with its tests in `shared/src/commonTest`. Code moves here
  as it is freed of those APIs, so that an iOS app can use it later.
This file holds operational rules, not a changelog. The package layout is in the [README](README.md#layout).
When documentation and code disagree, **the code wins**, and the documentation is fixed in the same change.

Nocturna is not one of the JavaFX desktop apps, so it does not follow
[Pauta](https://github.com/adriandeleon/adl-desktop-apps-guidelines). It borrows Pauta's repository
files and CI rules (REPO-02 to REPO-05 and REPO-09, REL-19 and REL-22 to REL-26, REL-31); its UI,
packaging and release rules do not apply.

## Essential commands

Run Gradle from the repository root. `local.properties` must set `sdk.dir` to the Android SDK.

```bash
./gradlew :app:assembleDebug        # debug APK: app/build/outputs/apk/debug/app-debug.apk
./gradlew :app:testDebugUnitTest    # JVM unit tests (app/src/test)
./gradlew :shared:testAndroidHostTest   # shared/src/commonTest, run on the JVM
./gradlew :shared:compileCommonMainKotlinMetadata   # fails on any Java or Android API in commonMain
./gradlew lint                      # Android Lint for both modules; errors fail the build
./gradlew lint testDebugUnitTest testAndroidHostTest compileCommonMainKotlinMetadata assembleDebug   # what CI runs
adb install -r app/build/outputs/apk/debug/app-debug.apk
```

The debug build installs as `com.adl.nocturna.debug`, beside a release build.

## How to work

- **One task, one branch, one worktree.** `git worktree add ../Nocturna-<topic> -b <type>/<topic>`;
  never switch branches under another session's feet, never stash someone else's work.
- **A whole feature is one pull request,** with its tests. `CHANGELOG.md` changes in the same PR, never as a
  follow-up.
- **Before you push:** the CI command above passes locally.
- Commit subjects are short and imperative ("Add dimming toggle and Monterrey radio stations").

## Change checklist

- [ ] `CHANGELOG.md` has an entry under `## [Unreleased]` that opens with a bold one-line summary for users.
- [ ] Logic that can be pulled out of a Composable or a service (time maths, schedules, brightness curves,
      parsing) is a plain function with a JVM unit test in `app/src/test`, or in `shared/src/commonTest`
      (`kotlin.test`, not JUnit) when the code is in `:shared`.
- [ ] Tests do not sleep, touch the network, or depend on the machine's time zone: pass the clock and zone in.
- [ ] A new setting has a default, survives the JSON round trip in `SettingsCodec` (`shared`, `data/`), and old
      stored JSON without it still loads.
- [ ] A new permission is in `AndroidManifest.xml` with the reason in a comment, and the app still works,
      with a degraded path, when the user refuses it.
- [ ] A new dependency goes in `gradle/libs.versions.toml` and gets a `NOTICE` entry with its licence.
- [ ] A new outside service whose data the app shows gets a `NOTICE` entry and whatever attribution its
      licence asks for.

## Things that will bite you

- `local.properties` is not committed; CI writes none, since the runner's `ANDROID_HOME` is enough.
- Exact alarms: `USE_EXACT_ALARM` on API 33+, `SCHEDULE_EXACT_ALARM` below it. Alarms go through
  `AlarmManager.setAlarmClock` and are rescheduled after boot.
- `:shared` builds for iOS too, so `commonMain` takes only Kotlin and multiplatform libraries: no `java.*`
  (`Math`, `java.time`, `String.format`), no `android.*`, no `org.json`. Only the metadata compile above
  catches a slip; the Android build alone does not. Its iOS targets link only on macOS, but
  `./gradlew :shared:compileKotlinIosSimulatorArm64` compiles them on Linux too.
- Shared code uses kotlinx-datetime (`kotlin.time.Instant` plus a `TimeZone`, where the app used
  `ZonedDateTime`) and reads stored JSON with the org.json-style helpers in `data/StoredJson.kt`, which keep
  the old lenient reading of what's already on phones. The app's UI still uses java.time and reaches the
  shared functions through `JavaTimeBridge.kt` overloads in each package; those go as the UI moves.
- Text that depends on the language (`formatTime`, day names) takes a `PlatformLocale`: `java.util.Locale` on
  Android, `NSLocale` on iOS. Tests use `testLocale`, British English, never the machine's.
- Release builds are minified (`isMinifyEnabled`, `isShrinkResources`): check anything reached by reflection
  against `app/proguard-rules.pro`.
