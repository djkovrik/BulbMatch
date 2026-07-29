[![GitHub Actions](https://github.com/djkovrik/BulbMatch/workflows/AnalysisAndTest/badge.svg)](https://github.com/djkovrik/BulbMatch/actions/workflows/AnalysisAndTest.yml)
[![Coverage](https://img.shields.io/endpoint?url=https://gist.githubusercontent.com/djkovrik/ee9bfb6d698aba0842a299dc0b55238c/raw/bulbmatch-coverage-badge.json)](https://github.com/djkovrik/BulbMatch/actions/workflows/CodeCoverageBadge.yml)
[![Last Commit](https://img.shields.io/github/last-commit/djkovrik/BulbMatch/master.svg)](https://github.com/djkovrik/BulbMatch/commits/master)
![License](https://img.shields.io/badge/license-MIT-blue.svg)

# BulbMatch

BulbMatch is an offline-first Kotlin Multiplatform app for Android and iOS. It
turns user-confirmed lamp markings into a conservative replacement shopping
profile for 220–240 V / 50 Hz regions. The app can capture or import a label,
run bundled OCR, require confirmation of every extracted field, assess
compatibility, and save immutable results locally.

BulbMatch does not certify fixtures, wiring, dimmers, enclosures, or
installations. Unknown, contradictory, unsupported, or out-of-scope data is
reported as a clarification need or potential conflict rather than a compatible
result.

## Technical overview

- Kotlin Multiplatform with shared domain, data, platform, application, ads,
  and Compose Multiplatform modules.
- Decompose component navigation and MVIKotlin state management.
- SQLDelight history, Multiplatform Settings preferences, and a versioned
  bundled catalog; core flows work offline.
- Native Android and iOS camera, image picker, OCR, and crash-delivery bridges
  behind shared contracts.
- English and Russian localization, light and dark themes, accessibility, and
  responsive text scaling.
- Detekt static analysis and Kover JVM line coverage with a 70% minimum gate.

## Project structure

- `shared/domain` — pure models, parsing, catalog contracts, and compatibility.
- `shared/data` — catalog validation, SQLDelight persistence, and settings.
- `shared/platform` — shared native-service contracts and platform bridges.
- `shared/app` — Decompose components, navigation, MVIKotlin, and orchestration.
- `shared/ads` — privacy-constrained banner and interstitial integration.
- `shared/compose` — shared Compose UI, themes, localization, and previews.
- `androidApp` and `iosApp` — platform hosts and composition roots.

## Build and quality checks

Use JDK 17 and the checked-in Gradle wrapper:

```shell
./gradlew detekt
./gradlew koverVerify
./gradlew -q printLineCoverage
./gradlew :androidApp:assembleDebug
```

The last command produces
`androidApp/build/outputs/apk/debug/androidApp-debug.apk`.

CI runs Android analysis, tests, migration checks, and assembly on Linux, plus
iOS simulator tests and framework linking on macOS.

## Running the apps

For Android, import the project in Android Studio and run the `androidApp`
configuration.

For iOS, run `pod install` in `iosApp`, then open
`iosApp/iosApp.xcworkspace` in Xcode. The workspace, not the `.xcodeproj`, is
the supported build entry point after CocoaPods resolution.

Local Firebase configuration, signing credentials, and production catalog
approval are intentionally not stored in Git. Their absence must remain a
truthful release gate rather than being replaced with test production data.
