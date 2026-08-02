# PaddleOCR migration and qualification record

## Implemented production boundary

- Android minimum is API 26, centralized in `gradle/libs.versions.toml`.
- Production OCR is `PP-OCRv5_mobile_det` plus
  `eslav_PP-OCRv5_mobile_rec`, executed locally through ONNX Runtime.
- Android and iOS package the same ONNX and YAML files from
  `shared/platform/src/androidMain/assets/Models`.
- `shared/platform/ocr-model-manifest.json` pins source archives, upstream
  PaddleOCR commit, SHA-256, byte sizes, scripts, languages, and runtimes.
- ML Kit has been removed from Gradle, CocoaPods, Kotlin, and Swift production
  code. Apple Vision remains a qualification control only and is not a runtime
  fallback.
- OCR transcripts, image bytes, and geometry remain memory-only; neither
  platform enables model downloads, profiling files, transcript logging, or
  recognition caches.

## Model decision

The `eslav` recognizer is selected because its official contract explicitly
covers East Slavic languages, English, and digits. The smaller universal
`cyrillic` candidate does not promise Latin recognition and therefore cannot
meet BulbMatch's combined Latin/Cyrillic product boundary without a second
recognizer. Device-corpus accuracy and latency are still release gates below.

## Automated evidence

- Model release task: `:shared:platform:validateOcrModelRelease`.
- Domain tests cover Cyrillic units, Russian unit words, unchanged raw text,
  direct `aliasesRu` matching, and mixed-script/confusable negatives.
- Android device-test APK includes a real-model offline smoke test:
  `:shared:platform:assembleAndroidDeviceTest`; execution requires an API 26+
  emulator or physical device.
- Corpus: 96 project-owned synthetic PNGs in `spec/ocr-fixtures/v1`, with six
  transformations and SHA-256 manifest validation.
- Production catalog and signed catalog safety fixtures are unchanged.

## Package-size comparison (Windows debug artifacts)

| Artifact | Before | PaddleOCR | Delta |
|---|---:|---:|---:|
| Universal debug APK | 69,378,467 B (66.16 MiB) | 251,385,062 B (239.74 MiB) | +182,006,595 B (+173.58 MiB) |
| Debug AAB | not recorded | 117,007,736 B (111.59 MiB) | n/a |

The universal APK contains ORT and OpenCV native libraries for four ABIs and is
not representative of a Play-delivered split. Record release AAB download size
and iOS IPA size before sign-off; do not infer either from these debug files.

## Required device/macOS release evidence

The executable Intel Mac, CI, future Apple Silicon, and physical-device hand-off
is documented in `docs/MACOS-IOS-OCR-VALIDATION-GUIDE.md`.

The following cannot be claimed from this Windows host and remain mandatory:

- run the 96-image corpus on API 26 and a current physical Android device, and
  on an iOS 16.2+ physical device;
- compare the iOS corpus against Apple Vision as a test-only control;
- record zero false base/voltage candidates on negative/ambiguous cases,
  Cyrillic field recall, complete supported-corpus recall, and Latin delta from
  the archived ML Kit baseline;
- record cold/warm p50/p95, peak memory, repeated-run memory stability, final
  split/package sizes, and clean-install Airplane Mode behavior;
- run `pod install`, the iOS Gradle tasks, and the Xcode workspace build on
  macOS, then perform the privacy/cache/log inspection on physical devices.
