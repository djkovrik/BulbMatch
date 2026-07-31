# BulbMatch agent guide

## Project state and source of truth

BulbMatch is a Kotlin Multiplatform mobile application for Android and iOS. The
baseline implementation for both platforms was created from the approved Vibe
AppSpec in `spec/app-spec` and is already present in the current branch. Treat
this as an implemented product that is being evolved, not as a greenfield
scaffold.

Use this order of authority:

1. Explicit user decisions.
2. The current AppSpec in `spec/app-spec`.
3. This file, the current code and tests, build configuration, and CI.
4. Current official platform and library documentation.
5. General engineering judgment.

Do not silently change product behavior, safety rules, scope, or acceptance
criteria. Read the relevant `REQ-NNN`, `AC-NNN`, flow, and screen definitions
before changing behavior, and preserve their identifiers in plans, tests, and
reports. If code and AppSpec conflict in a way that changes the product outcome,
surface the conflict and ask for a decision.

The AppSpec currently validates with zero errors and one known warning:
`capabilities.sync=false`, while sync-related terms occur in data/flow text.
This does not authorize synchronization. Accounts, cloud backup, cross-device
transfer, catalog network updates, and product analytics remain outside MVP.

The AppSpec is the product contract, but current code and recent commits are the
authority for implementation status. In particular,
`docs/MACOS-IOS-COMPLETION-GUIDE.md` was written as a pre-completion hand-off;
its statements that native iOS bridges are not connected are historical. Verify
the current Kotlin/Swift sources before using any status statement from that
guide.

## Required Vibe workflow

All implementation changes must use the installed `vibe-*` skills.

- Use `$vibe-developer` for AppSpec-driven, repository-wide, cross-cutting, or
  multi-module work.
- For a narrow task, invoke the owning specialist directly:
  - modules, Gradle, source sets, CI, signing/release contracts:
    `$vibe-project-architect`
  - domain types, invariants, parsing, and compatibility rules:
    `$vibe-domain-engineer`
  - Decompose components and navigation: `$vibe-decompose-engineer`
  - MVIKotlin Stores and observable orchestration: `$vibe-mvikotlin-engineer`
  - SQLDelight, settings, migrations, and local repositories:
    `$vibe-persistence-engineer`
  - camera, picker, OCR bridges, lifecycle, permissions, and native services:
    `$vibe-platform-engineer`
  - remote transport, if a future AppSpec explicitly adds it:
    `$vibe-network-engineer`
  - synchronization, only after an explicit AppSpec change:
    `$vibe-sync-engineer`
  - product UI, UX, accessibility, localization, and Compose design:
    `$vibe-product-designer`
  - Yandex ads, privacy-safe SDK setup, and ad lifecycle:
    `$vibe-monetization-engineer`
  - non-visual tests and acceptance coverage: `$vibe-test-engineer`
  - previews, screenshot tests, and goldens: `$vibe-visual-testing`

For UI design or critique, start with `$vibe-product-designer`, use Lazyweb
evidence before making design decisions, and route Compose API implementation
through Compose Expert as directed by the product-design skill. Do not update
goldens until the visual change has been reviewed and approved.

Before editing:

1. Read the selected skill and every required reference it names.
2. Run the `vibe-developer` AppSpec validator against `spec/app-spec`. Errors
   block work; report warnings separately.
3. Read the nearest `AGENTS.md`, inspect `git status`, and preserve user changes.
4. Inspect the smallest affected module graph and assign one owner to each
   file/change. Sequence explicit hand-offs instead of overlapping ownership.

After editing, run focused checks first, then the applicable platform/CI checks.
Report requirement and acceptance coverage, changed modules, checks with exit
codes, checks not run, remaining risks, and any AppSpec deviations.

## Product guardrails

BulbMatch turns user-confirmed lamp markings into a conservative replacement
shopping profile for 220–240 V / 50 Hz regions. It never certifies a fixture,
wiring, enclosure, dimmer, or installation.

These invariants are release-critical:

- `Compatible` requires a known catalog base and confirmed in-scope voltage.
- Missing, unknown, contradictory, unsupported, or out-of-scope facts must not
  produce a false-positive compatible result.
- Every OCR-derived field remains untrusted until the user confirms, edits, or
  rejects it.
- Fixture maximum wattage is a separate manual fact and must never be inferred
  from source-lamp wattage or OCR.
- Photos, thumbnails, full OCR transcripts, and unfinished drafts are
  ephemeral; do not persist or transmit them.
- Crash reports must not contain photos, OCR/user text, confirmed lamp values,
  saved-result names, or stable user identifiers.
- Core capture, bundled OCR, manual entry, assessment, reference, save, and
  history flows work offline. Ads and crash delivery must never gate them.
- Do not add Firebase Analytics, ATT prompts, Android advertising-ID
  permission, location targeting, custom ad targeting, product analytics, or
  background/user-tracking behavior.
- Yandex ads may appear only in the AppSpec-approved placements and must obey
  the eligibility, frequency, failure-collapse, accessibility, and privacy
  contracts.
- English and Russian, light and dark themes, safe areas, screen-reader
  semantics, minimum touch targets, and 200% text scaling are product
  requirements, not optional polish.

When uncertain, prefer `NeedClarification` or `PotentialConflict`; never weaken
a hard check to make a test or release gate pass.

## Repository map

- `spec/app-spec/` — approved product, domain, data, quality, flow, screen, and
  acceptance contracts.
- `shared/domain/` — pure common domain model, marking parser, immutable catalog
  contracts, and deterministic `CompatibilityEngine`. No Android, iOS, Compose,
  SQL, OCR SDK, or advertising types belong here.
- `shared/data/` — bundled catalog loading/validation, SQLDelight history,
  snapshot codecs, and Multiplatform Settings repositories.
- `shared/platform/` — platform-neutral image/OCR/crash contracts plus Android
  implementations and Swift-facing iOS bridges.
- `shared/app/` — Decompose component tree, navigation, MVIKotlin state, and
  interstitial eligibility/orchestration.
- `shared/ads/` — Yandex Compose Multiplatform configuration, banners,
  interstitial controller, and release validation.
- `shared/compose/` — shared Compose UI, localization, theme, previews, root
  adapter, and Android/iOS composition code.
- `androidApp/` — Android application host and composition root. The app ID is
  `com.sedsoftware.bulbmatch`; min SDK 24, target/compile SDK 36.
- `iosApp/` — Swift host, native camera/picker/OCR/Crashlytics implementations,
  Xcode project/workspace, and CocoaPods integration. The bundle ID is
  `com.sedsoftware.bulbmatch.iosApp`; deployment target is iOS 16.2. Open and
  build `iosApp/iosApp.xcworkspace`, not the `.xcodeproj`, after pod resolution.
- `.github/workflows/ci.yml` — Android shared tests/build and macOS iOS
  simulator tests/framework link.
- `docs/IOS-RELEASE-MANUAL-CHECKLIST.md` — current manual iOS release and device
  acceptance checklist.

The main composition boundaries are
`androidApp/.../AndroidRootHolder.kt`,
`shared/compose/src/iosMain/kotlin/main.kt`, and
`iosApp/iosApp/platform/IOSPlatformComposition.swift`. Keep native SDK details
behind platform/ads boundaries and keep business decisions in common domain
code.

## Toolchain and verification

Use the checked-in Gradle wrapper and JDK 17. Versions are centralized in
`gradle/libs.versions.toml`; do not update one platform side of a paired native
dependency without checking the other side.

On Windows, run Gradle through the installed
`vibe-developer/scripts/run-gradle.ps1`, with an absolute project root and a
UTF-8 log path. Require exit code 0 for every claimed check. On failure, inspect
the log tail and targeted error matches before increasing verbosity.

The current Android CI-equivalent task set is:

```text
:shared:domain:testAndroidHostTest
:shared:data:testAndroidHostTest
:shared:data:verifySqlDelightMigration
:shared:platform:testAndroidHostTest
:shared:app:testAndroidHostTest
:shared:ads:testAndroidHostTest
:shared:compose:compileAndroidMain
:androidApp:assembleDebug
```

The macOS iOS CI-equivalent task set, after `pod install` in `iosApp`, is:

```text
:shared:domain:iosSimulatorArm64Test
:shared:data:iosSimulatorArm64Test
:shared:platform:iosSimulatorArm64Test
:shared:app:iosSimulatorArm64Test
:shared:compose:iosSimulatorArm64Test
:shared:compose:linkDebugFrameworkIosSimulatorArm64
xcodebuild -workspace iosApp/iosApp.xcworkspace -scheme iosApp -configuration Debug -destination "generic/platform=iOS Simulator" CODE_SIGNING_ALLOWED=NO build
```

The ads module's pure common tests run on the Android host through Kover.
Validate the native iOS Yandex SDK graph by building the CocoaPods workspace;
do not link its transitive frameworks into a standalone Kotlin/Native test
binary.

Run Android checks after changes to common code. Run iOS Gradle and Xcode checks
on macOS after changes that affect Apple targets, Swift bridges, CocoaPods, or
the framework boundary. Simulator success does not substitute for the physical
device, permission, airplane-mode OCR, ad, Crashlytics, signing, archive, and
accessibility checks in the release checklist.

## Release boundary and credentials

The Android and iOS baseline is implemented, but do not describe the project as
production-release-ready unless every applicable release gate has actually
passed.

Known intentional release boundaries include:

- The bundled catalog is still the development candidate and requires the
  versioned human approval by Sergey V. described in the production catalog
  guide. AI findings are advisory and cannot create that sign-off.
- Yandex Compose/native SDKs are pinned to the currently aligned resolvable
  version, while `validateAdSdkReleaseVersion` intentionally blocks release
  until the documented required version is available and aligned. Do not remove
  or relax this gate.
- Android `androidApp/google-services.json` is committed release configuration;
  CI validates its package client directly from the checkout. The iOS
  `GoogleService-Info.plist`, signing keys, certificates, provisioning profiles,
  service-account credentials, and CI secrets must remain out of Git.
- Production validation tasks are expected to fail while required approval or
  configuration is absent. Do not replace a truthful red gate with a mock,
  demo data, a test ad ID, or weaker validation.

Never claim an unavailable platform/device/manual check passed. Record it as not
run with the reason and the exact remaining hand-off.
