# BulbMatch: macOS validation after the PaddleOCR migration

## Purpose

This is an executable hand-off for an agent validating the iOS side of the
PaddleOCR migration on macOS. It applies while:

- iOS is not a release target;
- no physical iPhone is available;
- the available local Mac uses an Intel `x86_64` processor;
- Apple Silicon validation is temporarily owned by the GitHub Actions
  `Test and link iOS` job and will later also be repeated locally.

The goal of this stage is integration confidence, not iOS release sign-off.
Do not claim physical-device, performance, offline, signing, archive, or App
Store gates from these checks.

## Sources of truth

Use, in order:

1. `AGENTS.md` and the approved AppSpec in `spec/app-spec`;
2. `.github/workflows/AnalysisAndTest.yml`, job `Test and link iOS`;
3. `iosApp/Podfile`, `iosApp/Podfile.lock`, and the Xcode workspace;
4. `shared/platform/ocr-model-manifest.json`;
5. `docs/OCR-MIGRATION-REPORT.md`.

Always build `iosApp/iosApp.xcworkspace`, never the `.xcodeproj`, after CocoaPods
resolution.

## Architecture boundary

The project declares `iosArm64` and `iosSimulatorArm64`; it does not declare an
`iosX64` Kotlin target. On an Intel simulator, the application therefore uses a
deliberate Swift-only host. That build verifies the Xcode project, Swift and
Objective-C++ sources, CocoaPods graph, resources, and the Intel simulator
slices, but it does not run the Compose application or production Kotlin/Swift
bridge.

`PaddleOCRQualificationTests` invokes the PaddleOCR engine directly and may be
run on an Intel simulator. A successful test is useful OCR integration evidence,
but it remains an x86_64 simulator result.

The CI `Test and link iOS` job is the required source of truth for:

- all declared `iosSimulatorArm64Test` tasks;
- the ARM64 Compose framework link;
- the CocoaPods workspace simulator build;
- compilation of the iOS OCR XCTest target.

CI currently uses `macos-15`, JDK 17, and Xcode 26.2. Do not weaken dependency
versions, deployment targets, or build settings merely to accommodate an older
local Intel toolchain. Record a toolchain incompatibility as `BLOCKED_LOCAL` and
use the CI result for the canonical ARM64 build decision.

## Result vocabulary

Record every step with its command, exit code, and one of these statuses:

- `PASS` — the exact command completed with exit code 0;
- `FAIL` — the command ran and failed because of a project or test problem;
- `BLOCKED_LOCAL` — the Intel host, installed Xcode, simulator runtime, network,
  or credentials prevented the command from running;
- `CI_COVERED` — the check is intentionally delegated to a green
  `Test and link iOS` job;
- `NOT_RUN_DEVICE` — a physical iPhone is required and none is available;
- `DEFERRED_RELEASE` — the check is relevant only when iOS release work starts.

Never convert `BLOCKED_LOCAL`, `CI_COVERED`, `NOT_RUN_DEVICE`, or
`DEFERRED_RELEASE` into `PASS`.

## 1. Repository and toolchain preflight

Run from the repository root:

```bash
pwd
uname -m
sw_vers
java -version
xcodebuild -version
xcrun --sdk iphoneos --show-sdk-version
xcrun --sdk iphonesimulator --show-sdk-version
pod --version
git lfs version
git status --short
```

Expected local architecture is `x86_64`. Record all versions. JDK 17 is
required. If local Xcode is older than the CI Xcode and a failure appears tied
to that difference, report `BLOCKED_LOCAL`; do not edit the dependency graph.

Fetch the model and fixture payloads:

```bash
git lfs pull
git lfs status
```

Verify that the following are real non-empty files, not unresolved LFS
pointers:

```text
shared/platform/src/androidMain/assets/Models/det/inference.onnx
shared/platform/src/androidMain/assets/Models/det/inference.yml
shared/platform/src/androidMain/assets/Models/rec/inference.onnx
shared/platform/src/androidMain/assets/Models/rec/inference.yml
iosApp/iosAppTests/Fixtures/cyrillic_e27_clean.png
```

Optional repository-level validation, if the Android SDK needed by Gradle is
available on the Mac:

```bash
./gradlew :shared:platform:validateOcrModelRelease --stacktrace
python3 spec/ocr-fixtures/v1/tools/validate_ocr_fixtures.py
```

Expected result: model validation exits 0 and the corpus validator reports 96
fixtures. These checks are also covered by the Android CI job.

## 2. Resolve and inspect CocoaPods

Run:

```bash
cd iosApp
pod install --repo-update
cd ..
```

Require exit code 0. This is the first real resolver validation of the
Windows-prepared `Podfile.lock`; inspect any resulting diff instead of
discarding it:

```bash
git diff -- iosApp/Podfile iosApp/Podfile.lock
grep -E 'onnxruntime-objc|OpenCV|Yams|MLKit' iosApp/Podfile.lock
```

Expected dependency boundary:

- `onnxruntime-objc 1.24.3` is present;
- OpenCV and Yams are present;
- no Google ML Kit pod is present;
- `iosApp/iosApp.xcworkspace` exists.

If CocoaPods legitimately rewrites the lockfile, preserve the resolved diff for
review. Do not manually restore the old lockfile and do not update unrelated pod
versions without approval.

## 3. Intel simulator build

Use an isolated DerivedData directory inside the ignored `build` directory:

```bash
export BULBMATCH_IOS_DERIVED_DATA="$PWD/build/ios-intel-derived-data"

xcodebuild \
  -workspace iosApp/iosApp.xcworkspace \
  -scheme iosApp \
  -configuration Debug \
  -destination "generic/platform=iOS Simulator" \
  -derivedDataPath "$BULBMATCH_IOS_DERIVED_DATA" \
  ARCHS=x86_64 \
  ONLY_ACTIVE_ARCH=YES \
  CODE_SIGNING_ALLOWED=NO \
  build
```

Require exit code 0 to record `PASS`. The resulting app is the intentional
Swift-only Intel simulator host. Do not report this as a Compose runtime or
production OCR-flow test.

After the build, verify the bundled model resources:

```bash
find "$BULBMATCH_IOS_DERIVED_DATA/Build/Products" \
  -path '*/BulbMatch.app/Models/*' \
  -type f \
  -print | sort
```

Expected files:

```text
Models/det/inference.onnx
Models/det/inference.yml
Models/rec/inference.onnx
Models/rec/inference.yml
```

There must be one packaged copy of each model file and no downloaded model.

## 4. Compile the OCR XCTest target on Intel

Run:

```bash
xcodebuild \
  -workspace iosApp/iosApp.xcworkspace \
  -scheme iosApp \
  -configuration Debug \
  -destination "generic/platform=iOS Simulator" \
  -derivedDataPath "$BULBMATCH_IOS_DERIVED_DATA" \
  ARCHS=x86_64 \
  ONLY_ACTIVE_ARCH=YES \
  CODE_SIGNING_ALLOWED=NO \
  build-for-testing
```

This must compile `iosAppTests/PaddleOCRQualificationTests.swift`, the bundled
fixture, PaddleOCR Swift/Objective-C++ sources, ONNX Runtime, OpenCV, and the
Apple Vision test-only control. Require exit code 0 for `PASS`.

## 5. Run the OCR XCTest on an Intel simulator when available

List installed simulator runtimes and devices:

```bash
xcrun simctl list runtimes
xcrun simctl list devices available
```

Choose an available iPhone simulator name and run, replacing
`<SIMULATOR_NAME>`:

```bash
xcodebuild \
  -workspace iosApp/iosApp.xcworkspace \
  -scheme iosApp \
  -configuration Debug \
  -destination "platform=iOS Simulator,name=<SIMULATOR_NAME>,OS=latest" \
  -derivedDataPath "$BULBMATCH_IOS_DERIVED_DATA" \
  ARCHS=x86_64 \
  ONLY_ACTIVE_ARCH=YES \
  CODE_SIGNING_ALLOWED=NO \
  test
```

The qualification test must recognize `220` and the E27 base with PaddleOCR;
Apple Vision is only a comparison control. Preserve the XCTest result bundle or
failure attachment when the test fails.

If the simulator cannot boot or remains in a CoreSimulator system-app wait,
record `BLOCKED_LOCAL`. A successful `build-for-testing` proves compilation but
does not prove test execution.

## 6. Compile the device architecture without a device

An Intel Mac can still attempt a non-signed generic `iphoneos` build. This
cross-compiles the production `iosArm64` Compose framework and the real
Kotlin/Swift bridge rather than the Intel simulator stub:

```bash
xcodebuild \
  -workspace iosApp/iosApp.xcworkspace \
  -scheme iosApp \
  -configuration Debug \
  -sdk iphoneos \
  -destination "generic/platform=iOS" \
  -derivedDataPath "$BULBMATCH_IOS_DERIVED_DATA" \
  CODE_SIGNING_ALLOWED=NO \
  build
```

Record `PASS` only for exit code 0. This is compile/link evidence; it is not a
physical-device run and does not require an Apple Team or provisioning profile.
If the installed Intel-era Xcode cannot resolve the current SDK or pod graph,
record `BLOCKED_LOCAL` and retain the build log.

## 7. Required CI hand-off

Trigger or observe `.github/workflows/AnalysisAndTest.yml` and require the job
named `Test and link iOS` to complete successfully for the exact commit under
validation.

The current job performs:

1. Git LFS checkout;
2. JDK 17 and Xcode/iOS Simulator SDK verification;
3. `pod install --repo-update`;
4. these KMP tasks:

   ```text
   :shared:domain:iosSimulatorArm64Test
   :shared:data:iosSimulatorArm64Test
   :shared:platform:iosSimulatorArm64Test
   :shared:app:iosSimulatorArm64Test
   :shared:compose:linkDebugFrameworkIosSimulatorArm64
   ```

5. generic iOS Simulator workspace build;
6. `build-for-testing` for the OCR XCTest target.

Compose common state tests are intentionally not linked as a standalone iOS
test binary because `shared:compose` transitively depends on the native Yandex
framework. The Android CI job preserves their behavioral coverage through
`:shared:compose:testAndroidHostTest`; the production iOS Compose graph remains
covered by the ARM64 framework link and CocoaPods workspace build.

A green job is required before merging this migration. Record the workflow run
URL, commit SHA, job conclusion, Xcode version, and simulator SDK version.

Important limitation: the current CI job compiles the OCR XCTest target but
does not execute `xcodebuild test`. Do not describe the CI job as OCR runtime
execution. Until CI is extended, runtime evidence comes from the optional Intel
simulator test above, a later Apple Silicon simulator test, and eventually a
physical iPhone.

If an ARM64 KMP task is skipped or not executed, the job must not be accepted as
equivalent evidence merely because a later Xcode step is green.

## 8. Later Apple Silicon hand-off

When an Apple Silicon Mac becomes available, repeat the repository/toolchain and
CocoaPods preflight, then run the CI-equivalent KMP matrix locally:

```bash
./gradlew \
  :shared:domain:iosSimulatorArm64Test \
  :shared:data:iosSimulatorArm64Test \
  :shared:platform:iosSimulatorArm64Test \
  :shared:app:iosSimulatorArm64Test \
  :shared:compose:linkDebugFrameworkIosSimulatorArm64 \
  --no-parallel \
  --stacktrace
```

Then repeat the workspace `build`, `build-for-testing`, and `test` commands
without forcing `ARCHS=x86_64`. On Apple Silicon, the simulator must launch the
real Compose application and production Kotlin/Swift bridge.

Apple Silicon simulator success still does not close physical-device gates.

## 9. Explicitly deferred checks

While iOS is not being released and no physical iPhone exists, record these as
`NOT_RUN_DEVICE` or `DEFERRED_RELEASE`:

- camera and PHPicker permission/lifecycle acceptance;
- the full 96-image corpus on iOS hardware;
- OCR p50/p95 latency, peak memory, and repeated-run stability;
- clean-install Airplane Mode and local-only OCR verification;
- post-cancel/post-finish privacy, cache, and log inspection;
- VoiceOver, Dynamic Type, safe-area, rotation, and device UI checks;
- Yandex Ads device integration and failure behavior;
- controlled Crashlytics delivery and privacy inspection;
- Apple Team, signing, provisioning, Archive, Validate App, and IPA size;
- App Store submission and all iOS release-only gates.

These are not blockers for the present Android-focused release boundary, but
they remain mandatory before any future iOS release claim.

## 10. Agent report template

Return a report in this form:

```text
Date:
Commit SHA:
Local architecture: x86_64
macOS:
Xcode / build:
iPhoneOS SDK:
iPhoneSimulator SDK:
JDK:
CocoaPods:

git lfs pull: PASS/FAIL
pod install --repo-update: PASS/FAIL
Podfile.lock resolver diff: NONE/EXPECTED/NEEDS_REVIEW
Intel simulator workspace build: PASS/FAIL/BLOCKED_LOCAL
Packaged model files: PASS/FAIL
Intel OCR build-for-testing: PASS/FAIL/BLOCKED_LOCAL
Intel OCR XCTest execution: PASS/FAIL/BLOCKED_LOCAL
Generic iphoneos build: PASS/FAIL/BLOCKED_LOCAL

CI workflow URL:
CI commit SHA:
Test and link iOS: PASS/FAIL
ARM64 KMP tests/link actually executed: YES/NO
CI OCR XCTest compilation: PASS/FAIL
CI OCR XCTest execution: NOT_RUN (current workflow compiles only)

Physical-device checks: NOT_RUN_DEVICE
iOS release checks: DEFERRED_RELEASE
Unexpected warnings:
Changed files produced by validation:
Remaining hand-off:
```

Every `PASS` must have an exit code 0 in the retained log. Report warnings
separately from failures and leave all unrelated working-tree changes intact.
