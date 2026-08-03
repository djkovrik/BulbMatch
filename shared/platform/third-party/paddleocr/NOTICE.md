# PaddleOCR-derived on-device pipeline

BulbMatch adapts the Android and iOS on-device OCR reference implementations from
PaddlePaddle/PaddleOCR commit `2661c7c0ef5c613e8f93c6e93b2e052399f0f854`.

- Upstream: https://github.com/PaddlePaddle/PaddleOCR
- Android reference: `deploy/ppocr-android/ppocr-sdk`
- iOS reference: `deploy/ios_demo/PaddleOCRDemo/Engine`
- License: Apache License 2.0; the full text is stored beside this notice.
- Local adaptations: BulbMatch service contracts, model bundle paths, lifecycle,
  privacy-safe production defaults, and removal of demo/profiling UI.

The iOS DB polygon implementation also contains Clipper 6.4.2 source under the
Boost Software License 1.0. Its license is retained beside that source.

ONNX Runtime, OpenCV, Yams, and platform dependency versions remain governed by
their upstream licenses and the resolved Gradle/CocoaPods lockfiles.
