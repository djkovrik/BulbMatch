# BulbMatch: завершение iOS-реализации на macOS

## Назначение

Этот документ — исполнимый hand-off для coding-агента, который продолжит работу
с репозиторием BulbMatch на macOS. Его задача — завершить нативный iOS host,
подключить platform SDK, проверить проект в Xcode и закрыть доступные iOS quality
gates.

Текущая граница реализации:

- common KMP-код, Compose UI, Decompose/MVIKotlin, SQLDelight и ads graph созданы;
- `iosSimulatorArm64` Kotlin target компилируется;
- `iosApp/Podfile` содержит Yandex Mobile Ads, PaddleOCR/ONNX Runtime OCR и
  Firebase Crashlytics;
- нативные Swift-реализации камеры, PHPicker, OCR и Crashlytics ещё не подключены;
- до их подключения iOS camera flow намеренно возвращает `Unavailable`, не
  подставляя demo data.

## Обязательные инструкции агенту

1. Используй `$vibe-developer` как основной workflow.
2. Для Swift/platform bridge используй `$vibe-platform-engineer`, для рекламы —
   `$vibe-monetization-engineer`, для тестов — `$vibe-test-engineer`.
3. Прочитай `spec/app-spec` и ближайший `AGENTS.md` до изменений.
4. Не меняй требования AppSpec молча. Любой конфликт с SDK, Xcode или
   существующей реализацией сначала опиши пользователю.
5. Не ослабляй release-gates, чтобы получить зелёную сборку.
6. Не добавляй Firebase Analytics, ATT prompt, запрос полной Photo Library,
   `AD_ID`, location tracking или хранение фото/OCR.
7. Сохраняй пользовательские изменения. Не используй `git reset --hard`,
   destructive checkout или массовую перезапись проекта.
8. Не утверждай, что device QA пройден, если проверялся только Simulator.
9. Не коммить `GoogleService-Info.plist`, signing certificates, provisioning
   profiles или другие credentials.

## Критерий завершения

Работа завершена, только если:

- Swift hosts реально подключены к Kotlin composition root;
- камера и PHPicker работают на физическом iPhone;
- bundled PaddleOCR работает для Latin/Cyrillic маркировок в airplane mode;
- iOS Gradle tests и framework link проходят;
- Xcode Simulator build проходит;
- device permission/cancellation/settings-return сценарии пройдены;
- Yandex integration проверена с test ads;
- контролируемый Crashlytics report доставлен и проверен на запрещённые данные;
- все недоступные проверки перечислены в финальном отчёте.

Production archive не должен обходить отдельные блокеры production-каталога,
Yandex SDK alignment и release configuration.

## Этап 0. Preflight

### 0.1. Проверить рабочее дерево и инструкции

Из корня репозитория:

```bash
pwd
git status --short
find .. -name AGENTS.md -print
```

Прочитать:

- `AGENTS.md`;
- `spec/app-spec/app-spec.json`;
- `spec/app-spec/product.md`;
- `spec/app-spec/domain.md`;
- `spec/app-spec/data.md`;
- `spec/app-spec/quality.md`;
- `spec/app-spec/flows/FLOW-002.md`;
- `spec/app-spec/flows/FLOW-007.md`;
- `shared/platform/src/iosMain/kotlin/com/sedsoftware/bulbmatch/platform/IosPlatformBridges.kt`;
- `shared/compose/src/iosMain/kotlin/main.kt`;
- `iosApp/iosApp/iosApp.swift`;
- `iosApp/Podfile`;
- `.github/workflows/ci.yml`.

Валидировать AppSpec:

```bash
python3 /path/to/vibe-developer/scripts/validate-app-spec.py spec/app-spec
```

Если расположение skill pack отличается, сначала найти
`vibe-developer/scripts/validate-app-spec.py`. Ошибки валидатора блокируют
реализацию. Предупреждения нужно отдельно перечислить.

### 0.2. Проверить toolchain

```bash
xcodebuild -version
xcode-select -p
java -version
pod --version
uname -m
chmod +x gradlew
```

Ожидается:

- Xcode 16.4 или новее;
- JDK 17;
- актуальный CocoaPods;
- Apple Silicon предпочтителен, но не является продуктовым требованием;
- deployment target проекта — iOS 16.2.

Если Xcode ниже требуемого текущей версией SDK — остановиться и сообщить
пользователю, не понижать SDK молча.

## Этап 1. Проверить и согласовать версии SDK

### 1.1. Yandex Mobile Ads

Текущее состояние проекта:

- Gradle/CMP dependency зафиксирована на `8.1.0`;
- `iosApp/Podfile` зафиксирован на `YandexMobileAds 8.1.0`;
- release-задача намеренно ожидает документированную CMP-версию `8.2.0`.

Официальный quick start:

<https://ads.yandex.com/helpcenter/en/dev/compose-multiplatform/quick-start>

Перед изменением проверить одновременно:

1. доступность `com.yandex.ads.multiplatform:mobileads-compose:8.2.0` в Gradle;
2. доступность `YandexMobileAds 8.2.0` в CocoaPods;
3. совместимость их iOS native API;
4. требования к `use_frameworks`/static linkage.

Если обе стороны доступны, обновить одной атомарной правкой:

- `gradle/libs.versions.toml`;
- `iosApp/Podfile`;
- `shared/ads/build.gradle.kts`;
- при необходимости lockfiles.

После обновления запустить targeted build и tests. Если хотя бы одна сторона
не разрешает `8.2.0`, оставить gate красным и зафиксировать фактический resolver
output. Не подменять SDK локальным артефактом и не удалять gate.

### 1.2. PaddleOCR / ONNX Runtime

Разрешить зафиксированные offline-зависимости:

```ruby
pod 'onnxruntime-objc', '1.24.3'
pod 'Yams', '~> 5.0'
pod 'OpenCV', '~> 4.3.0'
```

Модели, словарь и SHA-256 уже зафиксированы в
`shared/platform/ocr-model-manifest.json`; не скачивать модели при запуске.
Актуальный migration/qualification record: `docs/OCR-MIGRATION-REPORT.md`.
Официальное руководство по iOS deployment:

<https://www.paddleocr.ai/main/version3.x/inference_deployment/cross_platform/ios_deployment.html>

### 1.3. Firebase

В Firebase Console зарегистрировать Apple app с bundle ID:

```text
com.sedsoftware.bulbmatch.iosApp
```

Скачать файл и разместить локально:

```text
iosApp/iosApp/GoogleService-Info.plist
```

Добавить его в target membership `iosApp`. Файл уже должен игнорироваться Git.
Firebase Analytics не подключать, даже если документация рекомендует его для
breadcrumbs.

### 1.4. Установить pods

```bash
cd iosApp
pod install --repo-update
cd ..
open iosApp/iosApp.xcworkspace
```

Всегда открывать `.xcworkspace`, не `.xcodeproj`.

Если `pod install` требует изменить linkage, сначала проверить требования всех
трёх SDK. Не переключать static framework на dynamic молча.

## Этап 2. Спроектировать connection boundary

Перед кодом зафиксировать небольшой план файлов и ownership.

Kotlin contracts уже определены в:

```text
shared/platform/src/iosMain/kotlin/com/sedsoftware/bulbmatch/platform/IosPlatformBridges.kt
```

Нужно создать Swift implementations:

- `IosImageSourceHost`;
- `IosTextRecognitionHost`;
- `IosCrashReportingHost`.

Рекомендуемые Swift-файлы:

```text
iosApp/iosApp/platform/IOSImageSourceHost.swift
iosApp/iosApp/platform/IOSTextRecognitionHost.swift
iosApp/iosApp/platform/IOSCrashReportingHost.swift
iosApp/iosApp/platform/IOSPlatformComposition.swift
```

Их точные имена могут быть изменены, но каждый bridge должен иметь одного
владельца и отдельную ответственность.

Изменить iOS factory так, чтобы реальные hosts передавались в
`MainViewController`/`IosRootHolder`. После подключения удалить
`IosUnavailablePlatformBridge` только когда реальная реализация компилируется и
все fallback-сценарии сохранены.

Не добавлять global singleton с пользовательскими данными. Retention Swift
объектов должен совпадать с жизненным циклом root controller.

## Этап 3. Реализовать image source host

### 3.1. Camera permission

Использовать AVFoundation:

- `.notDetermined` → запрос `requestAccess(for: .video)`;
- `.authorized` → granted;
- `.denied`/`.restricted` → denied;
- отсутствие camera device → unavailable.

Все completion callbacks переводить на main queue перед UIKit presentation или
обновлением UI.

`openApplicationSettings()` должен открывать
`UIApplication.openSettingsURLString` и возвращать честный success/failure.

После foreground resume Kotlin layer должен повторно проверить permission.

### 3.2. Camera capture

По AppSpec использовать `UIImagePickerController` для одноразового фото:

- source type `.camera`;
- только изображение;
- cancel возвращает `Cancelled`, не failure;
- captured image кодируется в memory;
- файл не записывается в Documents, Library, tmp или caches;
- original/decoded image освобождается после передачи bytes.

Simulator без камеры должен возвращать `CameraUnavailable`.

### 3.3. System picker

Использовать `PHPickerViewController`:

- filter `.images`;
- `selectionLimit = 1`;
- не запрашивать `PHPhotoLibrary` authorization;
- загружать только выбранный item;
- cancel возвращает `Cancelled`;
- не копировать picker item в постоянное или cache-хранилище.

Ошибки должны маппиться только на закрытые `ImageFailureCode`.

## Этап 4. Реализовать bundled OCR host

Использовать bundled PaddleOCR detection + East-Slavic recognition через ONNX Runtime:

1. Получить encoded bytes только на время операции.
2. Декодировать `UIImage`.
3. Сохранить ориентацию изображения.
4. Уменьшить слишком большое изображение перед OCR; не обрабатывать
   full-resolution 12+ MP на main thread.
5. Лениво создать один сериализованный PaddleOCR pipeline.
6. Выполнить recognition вне UI-critical path.
7. Вернуть line-level observations:
   - text;
   - normalized или документированно преобразованные bounds.
8. Не возвращать и не логировать полный transcript отдельно от observations.
9. На cancel/finish освободить decoded image, input bytes и callbacks.
10. `close()` должен прекратить дальнейшие операции и освободить ORT sessions.

Результаты:

- пустой набор → `NO_TEXT_FOUND`;
- unsupported/corrupt image → `UNSUPPORTED_IMAGE`;
- SDK failure → `RECOGNITION_FAILED`;
- cancellation не превращать в неизвестную ошибку.

Kotlin parser и подтверждение пользователя остаются в common code. Swift OCR
не должен решать совместимость и не должен автоматически доверять полям.

## Этап 5. Реализовать Crashlytics host

Официальное руководство:

<https://firebase.google.com/docs/crashlytics/ios/get-started>

Требования:

1. Вызвать `FirebaseApp.configure()` до использования Crashlytics.
2. В debug/preview/test не отправлять reports.
3. В release передавать только:
   - sanitized exception type;
   - `ScreenCode`, если есть;
   - `OperationCode`.
4. Не передавать:
   - фото или image metadata;
   - OCR text;
   - base/raw text;
   - подтверждённые значения;
   - result name;
   - database contents;
   - stable user ID;
   - пользовательские breadcrumbs.
5. Не подключать Firebase Analytics.

В Xcode:

- установить `Debug Information Format = DWARF with dSYM File`;
- добавить Crashlytics upload script последней Build Phase;
- для CocoaPods использовать соответствующий скрипт из установленного pod;
- добавить официально требуемые Input Files для dSYM,
  `GoogleService-Info.plist` и executable;
- если включён User Script Sandboxing, добавить необходимые дополнительные
  input paths.

Точный script path брать из установленной версии Firebase/CocoaPods и текущей
официальной документации, а не копировать SPM path вслепую.

## Этап 6. Проверить Info.plist, privacy и ads

Проверить:

- локализованные `NSCameraUsageDescription` на EN/RU;
- отсутствие `NSUserTrackingUsageDescription`;
- отсутствие location purpose strings;
- отсутствие ненужного Photo Library purpose string;
- актуальные SKAdNetwork IDs из официальной документации Yandex;
- privacy configuration применяется до `YandexAds.initialize()`;
- location tracking выключен;
- app ad analytics reporting выключен;
- UI не ждёт ads initialization;
- banner failure сворачивается;
- interstitial failure продолжает navigation.

В Xcode Console проверить Yandex integration по:

```text
Subsystem = com.mobile.ads.ads.sdk
Category = Integration
```

## Этап 7. Targeted verification

Сначала Kotlin/Native tests и framework:

```bash
./gradlew \
  :shared:domain:iosSimulatorArm64Test \
  :shared:data:iosSimulatorArm64Test \
  :shared:platform:iosSimulatorArm64Test \
  :shared:app:iosSimulatorArm64Test \
  :shared:compose:linkDebugFrameworkIosSimulatorArm64 \
  --stacktrace
```

Compose common state tests запускаются через
`:shared:compose:testAndroidHostTest` в Android CI. Не линковать standalone iOS
test binaries для `shared:ads` или `shared:compose`: native Yandex graph
проверяется следующей сборкой CocoaPods workspace.

Затем список Xcode schemes:

```bash
xcodebuild -list -workspace iosApp/iosApp.xcworkspace
```

Simulator build:

```bash
xcodebuild \
  -workspace iosApp/iosApp.xcworkspace \
  -scheme iosApp \
  -sdk iphonesimulator \
  -destination 'platform=iOS Simulator,name=iPhone 16' \
  CODE_SIGNING_ALLOWED=NO \
  build
```

Если указанного Simulator нет, выбрать реально установленный через:

```bash
xcrun simctl list devices available
```

Не изменять deployment target только ради существующего Simulator.

После iOS изменений повторно проверить Android shared graph:

```bash
./gradlew \
  :shared:domain:testAndroidHostTest \
  :shared:data:testAndroidHostTest \
  :shared:data:verifySqlDelightMigration \
  :shared:platform:testAndroidHostTest \
  :shared:app:testAndroidHostTest \
  :shared:ads:testAndroidHostTest \
  :androidApp:assembleDebug \
  --stacktrace
```

## Этап 8. Physical-device acceptance

На поддерживаемом iPhone выполнить и записать результат каждого сценария:

1. Clean install.
2. Camera permission granted.
3. Camera permission denied.
4. Denied → Open Settings → grant → foreground resume.
5. Permission revoked между запусками.
6. Camera unavailable fallback.
7. Camera capture → preview → OCR → review.
8. Camera cancellation без error toast.
9. PHPicker success без full-library permission.
10. PHPicker cancellation без error toast.
11. Corrupt/unsupported image.
12. No-text image.
13. Manual entry остаётся доступным при любой camera/OCR ошибке.
14. Airplane mode после clean install:
    - camera;
    - picker;
    - bundled OCR;
    - catalog/reference;
    - assessment;
    - save/history.
15. После finish/cancel/process death нет сохранённого image/cache/OCR.
16. EN/RU.
17. Light/dark.
18. 200% Dynamic Type и VoiceOver на safety result.
19. Safe areas, rotation и системные bars.
20. Yandex test banners/interstitial и failure-collapse behavior.

## Этап 9. Crashlytics controlled verification

Проводить только на отдельной release-test сборке:

1. Использовать синтетические canary strings для OCR и result name.
2. Запустить приложение на устройстве из Xcode.
3. Остановить debugger.
4. Открыть установленное приложение вручную.
5. Вызвать контролируемый test crash.
6. Повторно открыть приложение для отправки отчёта.
7. Дождаться отчёта в Firebase Console.
8. Проверить:
   - stack trace символизирован;
   - нет canary strings;
   - нет фото/OCR/confirmed values/result name;
   - нет stable user ID;
   - есть только разрешённые технические enum-коды.
9. Удалить test-crash trigger до release.

Не считать проверку завершённой только по факту появления события в Console.

## Этап 10. Archive и финальный отчёт

Перед Archive:

- выбрать правильный Apple Team;
- проверить automatic signing/provisioning;
- убедиться, что bundle ID совпадает с Firebase и App Store Connect;
- проверить release configuration files;
- убедиться, что production catalog gate отдельно закрыт;
- запустить все release validation tasks;
- проверить отсутствие test ad IDs в release artifact;
- записать размер IPA.

Финальный отчёт агента должен содержать:

- изменённые файлы;
- реализованные Swift hosts;
- версии Xcode, iOS SDK, CocoaPods, Yandex, PaddleOCR, ONNX Runtime и Firebase;
- команды с exit code;
- Simulator и device matrix;
- permission/OCR/picker результаты;
- Yandex integration result;
- Crashlytics report ID и privacy inspection result без пользовательских данных;
- размер IPA;
- непройденные или недоступные проверки;
- отклонения от AppSpec и явные решения пользователя.

## Stop conditions

Агент обязан остановиться и запросить решение пользователя, если:

- Yandex CMP и native pod нельзя согласовать одной поддерживаемой версией;
- SDK требует добавить ATT, location или Analytics вопреки AppSpec;
- bundle ID/Firebase app не совпадают;
- требуется изменить product behavior или privacy contract;
- production catalog всё ещё не подписан;
- signing credentials или Apple Team недоступны;
- физического устройства нет, а задача требует заявить device acceptance;
- тест обнаружил возможный false-positive `Compatible`;
- Crashlytics report содержит запрещённые данные.

