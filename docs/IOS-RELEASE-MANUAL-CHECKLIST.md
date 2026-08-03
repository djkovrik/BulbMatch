# BulbMatch: ручные шаги перед iOS-релизом

## Назначение

Этот чек-лист содержит только действия, которые нельзя безопасно завершить
автоматически без Firebase-конфигурации, Apple Team, физического iPhone и доступа
к консолям провайдеров.

Основная реализация и технические инструкции находятся в
[`MACOS-IOS-COMPLETION-GUIDE.md`](MACOS-IOS-COMPLETION-GUIDE.md).

Не считать iOS-релиз готовым, пока каждый обязательный пункт ниже не выполнен,
а результат не записан в раздел «Финальный release record».

## Текущее состояние и блокеры

- Xcode: `26.1.1` (`17B100`), iOS SDK: `26.1`.
- CocoaPods: `1.16.2`.
- Yandex Mobile Ads CMP/native: `8.1.0`.
- PaddleOCR: `PP-OCRv5_mobile_det` + `eslav_PP-OCRv5_mobile_rec` through
  `onnxruntime-objc 1.24.3`; model hashes are pinned in
  `shared/platform/ocr-model-manifest.json`.
- Firebase Crashlytics: `12.11.0`, зафиксирован как совместимый с Xcode
  `26.1.1`.
- Generic iOS Device Debug build проходит без signing.
- Intel x86_64 Simulator build проходит со Swift-only заглушкой, потому что
  Compose Multiplatform `1.11.1` не публикует `iosX64`.
- Фактический Simulator smoke-test не завершён: iOS `26.1` CoreSimulator зависал
  в состоянии `Waiting on System App`.
- Kotlin/Native ARM64 test binaries компилируются и линкуются, но не могут
  выполняться на Intel host.

Текущие release-блокеры:

1. Production catalog не имеет human approval от `Sergey V.`.
2. Отсутствует локальный `iosApp/iosApp/GoogleService-Info.plist`.
3. Не настроены Apple Team, provisioning и device signing.
4. Не пройдены physical-device OCR performance/accuracy acceptance, Yandex
   integration check и controlled Crashlytics verification.
5. Не создан Archive, поэтому не записан размер IPA.

## 1. Подключить Firebase Apple app

- [ ] В Firebase Console создать или проверить Apple app с bundle ID:

  ```text
  com.sedsoftware.bulbmatch.iosApp
  ```

- [ ] Скачать `GoogleService-Info.plist`.
- [ ] Убедиться, что `BUNDLE_ID` внутри файла совпадает с
  `com.sedsoftware.bulbmatch.iosApp`.
- [ ] Поместить файл локально:

  ```text
  iosApp/iosApp/GoogleService-Info.plist
  ```

- [ ] Открыть только workspace:

  ```bash
  open iosApp/iosApp.xcworkspace
  ```

- [ ] В Xcode добавить `GoogleService-Info.plist` в группу `iosApp`.
- [ ] В File Inspector включить `Target Membership` для target `iosApp`.
- [ ] Убедиться, что файл присутствует в `Build Phases → Copy Bundle Resources`.
- [ ] Проверить, что файл остаётся проигнорирован Git:

  ```bash
  git status --short --ignored iosApp/iosApp/GoogleService-Info.plist
  ```

- [ ] Не добавлять Firebase Analytics.

Ожидаемый результат: Release-сборка видит plist в app bundle, а Crashlytics
upload phase больше не завершается ошибкой о недостающей конфигурации.

## 2. Настроить Apple signing

- [ ] В Xcode открыть `iosApp` project → target `iosApp` →
  `Signing & Capabilities`.
- [ ] Включить `Automatically manage signing`.
- [ ] Выбрать правильный Apple Team.
- [ ] Не менять bundle ID:

  ```text
  com.sedsoftware.bulbmatch.iosApp
  ```

- [ ] Проверить, что этот bundle ID совпадает с Firebase и App Store Connect.
- [ ] Подключить поддерживаемый iPhone с iOS `16.2` или новее.
- [ ] Подтвердить Trust и включить Developer Mode, если iOS этого потребует.
- [ ] Выбрать физическое устройство как run destination.
- [ ] Выполнить Debug build и запуск из Xcode.

Ожидаемый результат: приложение устанавливается и запускается на физическом
iPhone без signing/provisioning ошибок.

## 3. Пройти physical-device acceptance

Записывать результат каждого сценария отдельно: `PASS`, `FAIL` или `BLOCKED`,
версию iOS и модель устройства.

### Camera permission и lifecycle

- [ ] Clean install: permission ещё не запрошен на стартовом экране.
- [ ] Camera permission granted.
- [ ] Camera permission denied.
- [ ] Denied → Open Settings → grant → возврат в приложение.
- [ ] После foreground resume состояние permission обновилось без перезапуска.
- [ ] Permission отозван между запусками и корректно обнаружен.
- [ ] Camera unavailable не скрывает picker и ручной ввод.

### Camera, picker и OCR

- [ ] Camera capture → image review → OCR → review fields.
- [ ] Отмена камеры возвращает назад без error toast.
- [ ] PHPicker возвращает ровно одно изображение.
- [ ] PHPicker не запрашивает full-library permission.
- [ ] Отмена PHPicker возвращает назад без error toast.
- [ ] Corrupt/unsupported image показывает recoverable error.
- [ ] Изображение без текста показывает `NoTextFound`.
- [ ] После любой camera/OCR ошибки остаётся доступен ручной ввод.
- [ ] OCR-кандидаты нельзя использовать для assessment без явного
  confirm/edit/reject.
- [ ] Прогнать `spec/ocr-fixtures/v1` для Latin/Cyrillic/mixed, rotated,
  low-contrast, small, curved, blurred и no-text случаев.
- [ ] Зафиксировать field recall, false base/voltage count, p95 и peak memory;
  пороги берутся из `spec/app-spec/quality.md`.

### Offline и ephemeral data

- [ ] После clean install включить Airplane Mode.
- [ ] В Airplane Mode проверить camera и picker.
- [ ] В Airplane Mode проверить bundled Latin и Cyrillic OCR без загрузки модели.
- [ ] В Airplane Mode проверить catalog/reference и assessment.
- [ ] В Airplane Mode проверить save/history.
- [ ] После finish изображение и OCR-данные больше не доступны.
- [ ] После cancel изображение и OCR-данные больше не доступны.
- [ ] После принудительного завершения процесса unfinished scan не
  восстанавливается.
- [ ] В persistence, cache и логах нет изображения, OCR transcript и geometry.

### UI и accessibility

- [ ] Английская локализация.
- [ ] Русская локализация.
- [ ] Light theme.
- [ ] Dark theme.
- [ ] Portrait и landscape.
- [ ] Safe areas, display cutout и системные bars.
- [ ] Dynamic Type 200%: safety result и основные действия доступны без
  горизонтального скролла и обрезания.
- [ ] VoiceOver: safety result, действия и рекламные контейнеры имеют понятные
  labels и порядок фокуса.

## 4. Проверить Yandex Mobile Ads

Для Debug-сборки должны использоваться только официальные demo IDs.

- [ ] Проверить result inline banner.
- [ ] Проверить history sticky banner.
- [ ] Проверить reference sticky banner.
- [ ] Проверить match-exit interstitial после выполнения frequency policy.
- [ ] Проверить, что failed/offline banner сворачивается до нулевой высоты.
- [ ] Проверить, что ошибка interstitial не блокирует уже запрошенную навигацию.
- [ ] Проверить, что реклама не появляется на camera/OCR review/conflict
  экранах.
- [ ] Проверить отсутствие ATT prompt.
- [ ] Проверить отсутствие location permission.
- [ ] Проверить отсутствие custom targeting и affirmative user consent.
- [ ] В Xcode Console проверить Yandex integration с фильтрами:

  ```text
  Subsystem = com.mobile.ads.ads.sdk
  Category = Integration
  ```

- [ ] Записать результат integration check и обнаруженные SDK warnings.

### Yandex SDK release gate

Перед релизом проверить утверждённую AppSpec пару `8.1.0` одновременно в Gradle
version catalog, Podfile и Podfile.lock.

```bash
./gradlew :shared:ads:validateAdSdkReleaseVersion --stacktrace
```

Не удалять и не ослаблять gate. Более новая версия требует отдельного
AppSpec-решения и атомарного обновления Gradle/CocoaPods пары.

- [ ] Убедиться, что AppSpec, `gradle/libs.versions.toml`, `iosApp/Podfile` и
  `iosApp/Podfile.lock` фиксируют `8.1.0`.
- [ ] Убедиться, что `validateAdSdkReleaseVersion` завершается с `exit 0`.
- [ ] Выполнить iOS Gradle matrix, generic device build и physical Ads QA.

## 5. Подписать production catalog

- [ ] Вручную проверить точный bundled catalog и все его источники.
- [ ] Убедиться, что типичные значения не используются как compatibility facts.
- [ ] Убедиться, что каталог не создаёт возможный false-positive `Compatible`.
- [ ] После проверки обновить approval metadata только реальным решением
  `Sergey V.`.
- [ ] Выполнить:

  ```bash
  ./gradlew :shared:data:validateProductionCatalog --stacktrace
  ```

- [ ] Gate должен завершиться с `exit 0`.

Не менять approval metadata только ради зелёной сборки.

## 6. Провести controlled Crashlytics verification

Проверку выполнять на отдельной локальной Release-test сборке. В Debug отправка
Crashlytics намеренно выключена.

- [ ] Подготовить синтетические canary strings для OCR и result name.
- [ ] Добавить временный контролируемый test-crash trigger.
- [ ] Убедиться, что trigger недоступен в обычной production-сборке.
- [ ] Запустить Release-test сборку на физическом iPhone из Xcode.
- [ ] Остановить debugger.
- [ ] Открыть уже установленное приложение вручную.
- [ ] Вызвать controlled crash.
- [ ] Повторно открыть приложение, чтобы Crashlytics отправил отчёт.
- [ ] Дождаться отчёта в Firebase Console.
- [ ] Проверить, что stack trace символизирован.
- [ ] Проверить отсутствие canary strings.
- [ ] Проверить отсутствие:
  - фото и image metadata;
  - OCR text и raw/base text;
  - confirmed values;
  - result name;
  - database contents;
  - stable user ID;
  - пользовательских breadcrumbs.
- [ ] Проверить, что custom keys содержат только разрешённые технические
  значения:
  - sanitized exception type;
  - `ScreenCode`;
  - `OperationCode`.
- [ ] Записать Crashlytics report ID.
- [ ] Полностью удалить test-crash trigger и временную build condition.
- [ ] Проверить удаление через `git diff` и повторную Release build.

Появление события в Xcode Console не заменяет проверку фактического отчёта в
Firebase Console.

## 7. Повторить финальные проверки

### iOS Gradle matrix

На Apple Silicon Mac выполнить:

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

Compose common state tests выполняются отдельно через
`:shared:compose:testAndroidHostTest` в Android CI. Не добавлять
`shared:ads` или `shared:compose` iOS test binary в эту матрицу: native Yandex
graph проверяется сборкой CocoaPods workspace ниже.

- [ ] Все test binaries не только слинкованы, но и выполнены.
- [ ] Все тесты завершились с `exit 0`, без `SKIPPED` из-за host architecture.

### Xcode

```bash
cd iosApp
pod install --repo-update
cd ..

xcodebuild -list -workspace iosApp/iosApp.xcworkspace

xcodebuild \
  -workspace iosApp/iosApp.xcworkspace \
  -scheme iosApp \
  -sdk iphoneos \
  -destination 'generic/platform=iOS' \
  CODE_SIGNING_ALLOWED=NO \
  build
```

- [ ] `pod install` завершился с `exit 0`.
- [ ] Workspace содержит scheme `iosApp`.
- [ ] Generic iOS Device build завершился с `exit 0`.
- [ ] На Apple Silicon дополнительно прошёл реальный ARM64 Simulator build.

### Privacy и конфигурация

- [ ] `Info.plist` не содержит `NSUserTrackingUsageDescription`.
- [ ] Нет location purpose strings.
- [ ] Нет ненужного Photo Library purpose string.
- [ ] `NSCameraUsageDescription` локализован на EN/RU.
- [ ] SKAdNetwork IDs повторно сверены с текущим официальным списком Yandex.
- [ ] В Pods нет Firebase Analytics.
- [ ] `GoogleService-Info.plist` и signing credentials не отслеживаются Git.

## 8. Создать Archive и экспортировать IPA

- [ ] Проверить, что все release-gates зелёные.
- [ ] Выбрать правильный Apple Team.
- [ ] Выбрать `Any iOS Device (arm64)`.
- [ ] Выполнить `Product → Archive`.
- [ ] Убедиться, что Crashlytics upload phase успешно обработала dSYM.
- [ ] В Organizer выполнить Validate App.
- [ ] Экспортировать IPA подходящим методом распространения.
- [ ] Проверить отсутствие test ad IDs в Release artifact.
- [ ] Записать размер IPA.
- [ ] Выполнить финальный `git status` и убедиться, что secrets и test-crash
  trigger не попадут в коммит.

## Финальный release record

Заполнить перед отправкой в App Store Connect:

```text
Дата:
Git commit:
Xcode:
iOS SDK:
CocoaPods:
Yandex CMP/native:
PaddleOCR models / SHA-256:
ONNX Runtime:
Firebase Crashlytics:

Устройство:
Версия iOS:
Physical-device acceptance:
Offline bundled OCR:
Yandex integration:
Crashlytics report ID:
Crashlytics privacy inspection:
Production catalog approval:
Release validation tasks:
Archive validation:
IPA size:

Оставшиеся warnings:
Принятые отклонения:
Release owner:
```

## Критерий готовности

iOS-релиз готов только когда:

- physical-device acceptance полностью пройден;
- bundled OCR проверен после clean install в Airplane Mode;
- Yandex test ads и failure behavior проверены;
- Yandex SDK release gate зелёный;
- production catalog подписан и gate зелёный;
- controlled Crashlytics report доставлен, символизирован и не содержит
  запрещённых данных;
- signing, Archive и Validate App успешны;
- размер IPA записан;
- в Git нет secrets, временного crash trigger и других release-test артефактов.
