# BulbMatch: первый Android-релиз

Дата фиксации scope: 2026-07-31.

## Release scope

Первый публичный релиз BulbMatch выпускается только для Android с package name
`com.sedsoftware.bulbmatch`.

Обязательны для этого релиза:

- Android Gradle release build, подписанные APK и AAB;
- production-каталог, manifest/privacy gates, Firebase Crashlytics и Yandex Ads;
- публикация сначала в Google Play Internal testing;
- ручные Android device, offline OCR, accessibility, ads и Crashlytics проверки.

Следующие проверки имеют статус `DEFERRED — future iOS release` и не считаются
пройденными для Android-релиза:

- iOS Gradle compile/test/link;
- CocoaPods resolution и `Podfile.lock` alignment;
- Xcode workspace build и archive;
- physical iPhone acceptance;
- IPA/signing/App Store Connect проверки.

Существующий iOS CI можно продолжать использовать как ранний сигнал регрессий,
но его результат не является Android release gate и не меняет статус `DEFERRED`.

## Текущее состояние репозитория

- `androidApp/google-services.json` хранится в репозитории и содержит client
  для `com.sedsoftware.bulbmatch`.
- Google Services и Crashlytics Gradle plugins применяются при наличии этого
  файла. Debug-сборки не отправляют отчёты; release host включает collection.
- Firebase Analytics не подключён. Release verifier запрещает
  `firebase-analytics` и Google `play-services-measurement` в APK/AAB.
- Android production Yandex IDs совпадают с AppSpec:
  - result inline: `R-M-19664981-1`;
  - history sticky: `R-M-19664981-2`;
  - reference sticky: `R-M-19664981-3`;
  - match-exit interstitial: `R-M-19664981-4`.
- Debug Yandex IDs поступают только из debug `BuildConfig`; release archive
  verifier запрещает `demo-banner-yandex` и `demo-interstitial-yandex`.
- `AD_ID`, install-referrer binding, location и broad media permissions, а также
  Yandex debug/preinstall providers удаляются из merged release manifest.
- Yandex location, affirmative consent и automatic app-ad analytics выключены
  до SDK initialization. Сам Yandex Ads SDK содержит AppMetrica runtime для
  разрешённой рекламы и Yandex revenue/impression reporting; это не заменяет
  обязательное store disclosure и legal/privacy review.
- Production-каталог одобрен Sergey V.; canonical hash:
  `881d91ec1c8a3ebd494bac0dfa615e109c25e573548d325ec90d5ba98f32ce7b`.

### Утверждённая SDK-пара для закрытого тестирования

AppSpec утверждает Yandex Compose Multiplatform и native iOS SDK `8.1.0` как
опубликованную и выровненную baseline-пару для закрытого тестирования.
`:shared:ads:validateAdSdkReleaseVersion` проверяет это решение одновременно в
AppSpec, Gradle version catalog, Podfile и Podfile.lock.

Нельзя выключать этот gate в publish workflow. Для перехода на более новую
версию сначала принять явное AppSpec-решение, затем одной атомарной правкой
обновить CMP/native зависимости и повторить Android dependency resolution,
CocoaPods/Xcode linkage, privacy checks и physical-device Ads QA.

## GitHub release automation

Добавлены workflows:

- `CreateAndroidRelease.yml` — только repository owner и только `master`;
  проверяет EN/RU changelog до 500 символов, создаёт SemVer tag и GitHub
prerelease. При отсутствии SemVer tags первая версия — `v0.1.0` с
`versionCode=1000`.
- `PublishAndroidRelease.yml` — проверяет Firebase config из репозитория и восстанавливает upload key,
  строит signed/R8 APK+AAB, проверяет manifest, каталог, test IDs, Analytics,
  подписи и mapping, сохраняет artifacts и публикует AAB в Google Play
  `internal` track.

Повторная ручная публикация существующего tag запускается из `master` через
`Publish Android release`. Обычный путь — запуск `Create Android release`.

## 1. Создать Android upload key

Включить Google Play App Signing. Создать отдельный upload key, не использовать
debug keystore и не хранить файл/пароли в Git.

Пример:

```text
keytool -genkeypair -v -keystore bulbmatch-upload.jks -alias bulbmatch-upload -keyalg RSA -keysize 4096 -validity 10000
```

Сохранить минимум две зашифрованные резервные копии keystore и отдельно
зафиксировать alias, срок действия сертификата и SHA-1/SHA-256 fingerprints.
Потеря upload key требует процедуры reset через Play Console; потеря исходных
паролей нельзя компенсировать изменением workflow.

## 2. Настроить Firebase

1. В Firebase Console проверить Android app с package name
   `com.sedsoftware.bulbmatch` в project `bulbmatch-49a3b`.
2. Включить Crashlytics. Не включать Google Analytics при создании/связывании
   проекта и не добавлять Firebase Analytics dependency.
3. Скачать свежий `google-services.json`, положить его в
   `androidApp/google-services.json` и закоммитить. Workflow проверяет, что файл
   отслеживается Git, является корректным JSON и содержит Android client для
   `com.sedsoftware.bulbmatch`.
4. После internal release выполнить controlled synthetic Crashlytics test на
   физическом устройстве. Не использовать фото, OCR-текст, значения лампы,
   имена сохранённых результатов, email или стабильный user ID.
5. В Firebase Console дождаться символизированного события и проверить keys,
   message, breadcrumbs и attachments на запрещённые данные. Записать issue ID
   и дату проверки в release evidence.
6. Проверить, что mapping upload для того же `versionName/versionCode` прошёл в
   GitHub Actions и stack trace символизируется.

Controlled crash/non-fatal trigger должен существовать только во временной
internal-test сборке и быть удалён до release tag. После удаления повторить
production gates; доставку события не переносить как доказательство на другой
versionCode.

## 3. Настроить Google Play Console

1. Создать приложение BulbMatch с package name
   `com.sedsoftware.bulbmatch`, default language и владельцем, совпадающим с
   production account.
2. Включить Play App Signing и зарегистрировать сертификат upload key.
3. Заполнить store listing EN/RU: название, short/full description, icon,
   feature graphic, phone screenshots и support contacts.
4. Убедиться, что `https://sedsoftware.com/apps/bulbmatch/policy.html` публично
   доступна, соответствует текущим Firebase/Yandex данным и указана в Console.
5. Заполнить App access, Content rating, Target audience, Ads declaration
   (`Yes`), Data safety и все текущие policy forms.
6. В Data safety не заявлять, что SDK ничего не собирают только потому, что
   BulbMatch не отправляет product events. Сверить актуальные disclosure pages
   Firebase Crashlytics и Yandex/AppMetrica: crash/device diagnostics и
   рекламные/impression identifiers могут требовать декларации даже без
   `AD_ID`, location и custom targeting.
7. Создать Internal testing track и список тестировщиков.
8. Первый AAB для нового package загрузить вручную через Play Console. После
   появления package в Play Developer API следующие версии может загружать
   workflow.
9. Убедиться, что target API 36 принят Console и нет policy blockers.

## 4. Настроить Google Play API service account

1. В Google Cloud project включить Google Play Android Developer API.
2. Создать отдельный service account для release automation и JSON key.
3. В Play Console → Users and permissions пригласить service-account email.
4. Выдать только app-scoped разрешения BulbMatch, достаточные для просмотра app
   information и создания/управления releases в internal testing. Не давать
   финансовые или account-wide admin permissions.
5. Полный JSON сохранить в GitHub secret
   `GOOGLE_PLAY_SERVICE_ACCOUNT_JSON`.

## 5. Проверить Yandex Advertising Network

1. В Yandex Advertising Network проверить Android app с точным package name.
2. Проверить moderation/store linkage и четыре unit IDs/формата:
   inline, sticky, sticky, interstitial соответственно списку выше.
3. Убедиться, что ID принадлежат production app, активны и не являются demo/test.
4. На debug build проверить официальные test banners/interstitial и failure
   collapse. На internal release после moderation проверить реальные units,
   не создавая искусственные impressions/clicks.
5. В Logcat проверить `Yandex Ads` integration/initialization messages.
6. Проверить отсутствие location/AD_ID signals и отсутствие передаваемых
   targeting parameters через разрешённые SDK diagnostics/network review.
7. Решения о consent, возрасте, странах и персонализации подтвердить с legal.
   Текущий AppSpec устанавливает `userConsent=false`, `locationTracking=false`,
   `ageRestricted=false`; изменение любого решения требует AppSpec change.

## 6. Создать GitHub environment и secrets

Создать Environment `google-play-internal`. Рекомендуется добавить required
reviewer, запретить неподходящие branches/tags и разрешить deployment только из
`master`/SemVer tags.

Environment или repository secrets, необходимые workflow:

- `ANDROID_KEYSTORE_BASE64`;
- `ANDROID_KEYSTORE_PASSWORD`;
- `ANDROID_KEY_ALIAS`;
- `ANDROID_KEY_PASSWORD`;
- `GOOGLE_PLAY_SERVICE_ACCOUNT_JSON`.

Для создания base64 в PowerShell:

```text
[Convert]::ToBase64String([IO.File]::ReadAllBytes('bulbmatch-upload.jks'))
```

Не вставлять значения secrets в issues, Actions logs, changelog или release
notes. После настройки выполнить ручной workflow retry только на тестовом tag,
пока internal track не подтвердит end-to-end доступ.

## 7. Android manual release acceptance

На физическом поддерживаемом Android-устройстве проверить подписанную internal
release сборку:

1. clean install и upgrade с предыдущего internal versionCode;
2. камера: grant, deny, deny permanently, возврат из Settings;
3. system photo picker без broad media permission;
4. bundled OCR в airplane mode на EN/RU, rotated, blurred, numeric-only и
   no-text fixtures;
5. manual flow, Compatible/Need clarification/Potential conflict, save/reopen/
   delete и offline reference;
6. light/dark, EN/RU, screen reader, 200% font scale, cutout/navigation insets;
7. Yandex ad-present/ad-failed paths, отсутствие перекрытия контента и точный
   interstitial cooldown;
8. отсутствие ожидания рекламы/Crashlytics в core flow;
9. controlled Crashlytics privacy inspection;
10. итоговый installed/download size и startup/OCR performance budgets.

Результаты фиксировать отдельно от автоматических checks. Невыполненную ручную
проверку отмечать `NOT RUN`, а не `PASS`.

## 8. Порядок выпуска

1. Убедиться, что release gate подтверждает утверждённую Yandex CMP/native пару
   `8.1.0` без `-x` и без ослабления проверки.
2. Слить release-prep изменения в `master`; убедиться, что Android CI зелёный.
3. Завершить Console/Firebase/Yandex/manual prerequisites выше.
4. Запустить `Create Android release`, выбрать bump и заполнить EN/RU notes.
5. Проверить signed APK/AAB, mapping и GitHub prerelease artifacts.
6. Проверить версию в Google Play Internal testing и выполнить manual acceptance.
7. Только после принятия internal build продвигать тот же AAB/versionCode в
   closed/open/production track через контролируемый Console release.

Если publish упал после создания tag, не создавать новый tag без изменения
версии. Исправить внешний blocker и запустить `Publish Android release` для того
же tag. Если artifact или source изменились, нужен новый SemVer tag/versionCode.
