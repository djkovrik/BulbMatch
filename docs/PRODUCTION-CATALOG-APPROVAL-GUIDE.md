# BulbMatch: подпись и включение production-каталога

## Назначение

Этот документ описывает человеческий процесс подготовки, проверки, подписи и
включения production-каталога BulbMatch.

Финальный reviewer — **Sergey V.** Автоматические тесты и AI могут находить
ошибки, расширять fixtures и предлагать исправления, но не могут поставить
production-подпись вместо него.

Главный release guardrail:

> Если приложение не может установить известный цоколь и подтверждённое
> напряжение в поддерживаемом диапазоне, оно не должно показывать
> `Compatible`.

## Текущее состояние

Кандидат находится в:

```text
shared/data/src/commonMain/resources/catalog/bulbmatch-catalog-development.json
```

Сейчас он намеренно содержит:

- development-версии;
- `DEVELOPMENT_PENDING_HUMAN_SIGNOFF`;
- `releaseEligible: false`;
- `decision: PENDING`;
- пустой `reviewedRuleCodes`;
- `PENDING_HUMAN_SIGNOFF` для entries;
- `enabledForAssessment: false`.

Runtime production validation находится в:

```text
shared/data/src/commonMain/kotlin/com/sedsoftware/bulbmatch/data/catalog/BundledCatalog.kt
```

Gradle release gate находится в:

```text
shared/data/build.gradle.kts
```

Простая замена `PENDING` на `APPROVED` запрещена. Она не подтверждает
происхождение данных и нарушает content hash.

## Что должно быть подписано

Подпись относится не только к JSON-файлу каталога. Sergey V. утверждает единый
неизменяемый review bundle:

1. `catalogVersion`;
2. `rulesetVersion`;
3. `safetyFixtureSuiteVersion`;
4. source/licensing records;
5. список `reviewedRuleCodes`;
6. exact catalog content;
7. результаты автоматических тестов;
8. ожидаемые исходы safety fixtures;
9. content hash;
10. итоговое решение и дату.

Изменение любого элемента после подписи требует новой версии, нового hash и
новой подписи.

## Этап 1. Усилить механизм подписи до ревью

До подписания production-каталога нужно закрыть два технических пробела.

### 1.1. Версионированный safety fixture suite

Текущие unit-тесты compatibility engine полезны, но AppSpec требует отдельную
версию полного safety fixture suite.

Нужно создать версионированный набор, который покрывает как минимум:

- missing base;
- unknown base;
- unsupported base;
- missing voltage;
- contradictory voltage;
- ambiguous voltage;
- напряжение вне 220–240 V;
- отсутствие/наличие frequency;
- unknown/missing/known dimmability;
- source rated power и fixture maximum power как разные факты;
- `60 W equivalent` отдельно от реальной мощности;
- OCR-кандидат rejected/edited/confirmed/manual;
- aliases каждого поддерживаемого цоколя на EN/RU;
- положительные случаи для каждого разрешённого base;
- пограничные и ошибочные числовые форматы.

Каждый fixture должен содержать:

```text
fixtureId
suiteVersion
input
expectedOutcome
expectedReasonCodes
coveredRuleCodes
reviewState
```

Ноль известных false-positive `Compatible` является обязательным условием.

### 1.2. Отдельная sign-off record

Создать, например:

```text
spec/catalog-signoffs/catalog-2026.08.0.json
```

Обязательные поля:

```json
{
  "catalogVersion": "2026.08.0",
  "rulesetVersion": "2026.08.0",
  "safetyFixtureSuiteVersion": "2026.08.0",
  "sourceManifestVersion": "1",
  "contentHashAlgorithm": "SHA-256",
  "contentHashScope": "kotlinx-serialization canonical UnsignedCatalogPayload v1",
  "contentHash": "<exact hash>",
  "reviewer": "Sergey V.",
  "reviewedAt": "<ISO-8601 timestamp>",
  "decision": "APPROVED"
}
```

Поле `safetyFixtureSuiteVersion` требуется AppSpec, но сейчас отсутствует в
catalog metadata. Его нужно хранить в типизированной sign-off record или добавить
в schema каталога. Выбранный вариант должен проверяться автоматически.

### 1.3. Усилить Gradle gate

Текущий `validateProductionCatalog` проверяет строки через `contains`. Перед
реальной подписью его нужно заменить типизированной проверкой, которая:

- декодирует catalog JSON;
- проверяет runtime hash;
- декодирует sign-off record;
- сверяет exact catalog/ruleset/fixture versions;
- сверяет reviewer и reviewedAt;
- требует `decision == APPROVED`;
- требует непустой `reviewedRuleCodes`;
- требует, чтобы все entries были approved и enabled;
- отклоняет лишние/неизвестные поля;
- отклоняет несовпадающие hash или версии.

Добавить тесты:

- валидный подписанный production bundle проходит;
- изменение одного символа ломает hash;
- отсутствующий sign-off блокирует release;
- несовпадающая версия fixtures блокирует release;
- другой reviewer блокирует release;
- `PENDING` entry блокирует release;
- disabled entry блокирует release;
- пустой `reviewedRuleCodes` блокирует release.

## Этап 2. Зафиксировать release versions

До содержательного ревью выбрать версии. Например:

```text
catalogVersion: 2026.08.0
rulesetVersion: 2026.08.0
safetyFixtureSuiteVersion: 2026.08.0
```

Версии не обязаны совпадать по формату, но каждая должна быть уникальной,
неизменяемой и одинаково указана:

- в catalog;
- в ruleset;
- в fixtures;
- в sign-off record;
- в Settings;
- в release notes.

После начала финального ревью версии замораживаются.

Рекомендуется переименовать shipping resource:

```text
bulbmatch-catalog-development.json
```

в:

```text
bulbmatch-catalog-production.json
```

При переименовании нужно обновить Android/iOS resource loading, тесты и Gradle
validation task.

## Этап 3. Подготовить source records

Источник правил:

```text
spec/app-spec/assets/source-licensing-manifest.md
```

Для каждой production entry создать запись:

```text
entryId:
canonicalBaseCode:
facts:
primarySourceId:
secondaryVerificationUrl:
accessedAt:
transformation:
redistributionDecision:
reviewer:
reviewedAt:
reviewDecision:
aiAssistanceSummary:
catalogVersion:
contentHash:
```

Reviewer должен проверить:

1. canonical base code;
2. источник и его актуальность;
3. EN/RU common names;
4. EN/RU aliases;
5. distinguishing hints;
6. original diagram и text alternative;
7. source record IDs;
8. отсутствие неподтверждённых выводов;
9. лицензионную допустимость shipping content.

Нельзя переносить в приложение:

- стандартные таблицы и листы IEC;
- gauges, нормативные размеры и защищённые чертежи;
- длинные фрагменты стандартов;
- логотипы и manufacturer photography;
- proprietary diagrams;
- marketing copy.

Manufacturer datasheet может подтверждать характеристики конкретного товара, но
не может автоматически становиться универсальным правилом совместимости.

Если redistribution rights неясны, использовать только проверенный
неохраняемый идентификатор/факт и оригинальные тексты/диаграммы.

## Этап 4. Проверить каждую catalog entry

Для каждой entry заполнить review sheet.

### Идентификация

- [ ] `canonicalCode` соответствует проверенному идентификатору.
- [ ] Нет дубликата canonical code после normalization.
- [ ] Aliases не принадлежат другому canonical code.
- [ ] EN/RU search fixtures находят правильную entry.

### Тексты

- [ ] `commonNameEn` проверено.
- [ ] `commonNameRu` проверено носителем/редактором.
- [ ] `distinguishingHintEn` оригинален и не обещает совместимость.
- [ ] `distinguishingHintRu` соответствует EN-смыслу.
- [ ] Нет mojibake или неверной UTF-8 кодировки.

### Визуальный материал

- [ ] `diagramId` существует.
- [ ] Диаграмма оригинальная или имеет явное право на использование.
- [ ] Диаграмма не является gauge/standard drawing.
- [ ] Text alternative содержит code и отличительные признаки.

### Источники

- [ ] `sourceRecordIds` не пуст.
- [ ] Каждый ID существует в source registry.
- [ ] Указаны access date и review date.
- [ ] Transformation и license decision записаны.
- [ ] Присутствует secondary verification, где она нужна.

### Безопасность

- [ ] Entry не подразумевает напряжение только из формы цоколя.
- [ ] Entry не утверждает безопасность fixture/wiring/enclosure/dimmer.
- [ ] Unknown или unsupported данные не дают `Compatible`.
- [ ] Для entry есть положительные и отрицательные fixtures.

## Этап 5. Проверить ruleset

Создать явный список стабильных rule codes. Например, фактические названия
должны соответствовать коду проекта, но набор обязан покрывать:

- known-base requirement;
- confirmed-voltage requirement;
- voltage range;
- contradictory voltage;
- frequency handling;
- reviewed OCR fields;
- fixture maximum power origin;
- dimmability advisory;
- scope disclaimer.

Каждый code должен иметь:

- описание;
- входные условия;
- ожидаемый outcome/reason;
- связанные requirement и acceptance IDs;
- positive fixture;
- negative fixture;
- reviewer decision.

После ревью заполнить `reviewedRuleCodes`. Пустой список запрещает production.

## Этап 6. Автоматическая проверка перед подписью

До человеческого sign-off запустить:

```bash
./gradlew \
  :shared:domain:testAndroidHostTest \
  :shared:data:testAndroidHostTest \
  :shared:data:verifySqlDelightMigration \
  --rerun-tasks
```

Дополнительно должны быть выполнены:

- весь safety fixture suite;
- property tests для unknown/missing facts;
- negative/mutation tests для hard checks;
- catalog schema и duplicate validation;
- EN/RU alias fixtures;
- hash tampering tests;
- production approval negative tests.

Результат review bundle должен содержать:

- commit SHA;
- версии;
- число fixtures;
- число положительных и отрицательных cases;
- test commands и exit codes;
- failures/skips;
- diff относительно предыдущего каталога;
- AI findings и их disposition.

AI findings должны быть помечены как advisory:

```text
accepted
rejected
requires human review
```

## Этап 7. Человеческий review bundle

Sergey V. получает единый пакет:

1. exact catalog JSON;
2. exact ruleset;
3. exact safety fixture suite;
4. source/licensing records;
5. diagrams и text alternatives;
6. список `reviewedRuleCodes`;
7. automated test report;
8. AI-assisted review report;
9. diff;
10. proposed versions;
11. proposed content hash;
12. список известных ограничений.

Решение:

- `REJECTED` — замечания фиксируются, версия остаётся неподписанной;
- `CHANGES_REQUIRED` — bundle меняется, проверки запускаются заново;
- `APPROVED` — утверждается exact frozen bundle.

Устное согласование без версии, даты и hash не является production-подписью.

## Этап 8. Записать approval

Только после решения Sergey V. установить:

```json
"release": {
  "state": "APPROVED",
  "releaseEligible": true,
  "requiredReviewer": "Sergey V.",
  "reviewedAt": "<ISO-8601 timestamp>",
  "decision": "APPROVED"
}
```

Для каждой утверждённой entry:

```json
"reviewState": "APPROVED",
"enabledForAssessment": true
```

Также:

- заполнить `reviewedRuleCodes`;
- заполнить sign-off record;
- проверить одинаковые версии;
- проверить UTF-8;
- не менять пользовательские тексты после финального review.

## Этап 9. Пересчитать content hash

Hash пересчитывается последним, после изменения approval metadata, entries,
versions и reviewed rule codes.

Текущий контракт:

```text
algorithm: SHA-256
scope: kotlinx-serialization canonical UnsignedCatalogPayload v1
```

Hash считается от canonical `UnsignedCatalogPayload`, который исключает само
поле `contentHash`, но включает:

- schema/version metadata;
- release metadata;
- reviewed rule codes;
- entries.

Нельзя использовать:

```bash
sha256sum bulbmatch-catalog-production.json
```

Это hash сырых байтов, а не canonical payload.

Нужно добавить/использовать детерминированную проектную Gradle-задачу, которая:

1. декодирует catalog тем же `kotlinx.serialization`;
2. строит `UnsignedCatalogPayload`;
3. печатает или обновляет exact hash;
4. повторно загружает документ через production validator;
5. не меняет другие поля.

После вычисления hash больше ничего в catalog/sign-off bundle не менять.

## Этап 10. Финальные release gates

Запустить:

```bash
./gradlew \
  :shared:data:testAndroidHostTest \
  :shared:data:validateProductionCatalog \
  :shared:data:verifySqlDelightMigration \
  :shared:domain:testAndroidHostTest \
  --rerun-tasks
```

Затем общий Android build:

```bash
./gradlew \
  :shared:platform:testAndroidHostTest \
  :shared:app:testAndroidHostTest \
  :shared:ads:testAndroidHostTest \
  :androidApp:assembleDebug \
  --stacktrace
```

На macOS дополнительно выполнить iOS test/link tasks из
`docs/MACOS-IOS-COMPLETION-GUIDE.md`.

Проверить artifacts:

- packaged catalog присутствует;
- filename production;
- packaged hash совпадает;
- Settings показывает exact catalog/ruleset versions;
- ни одна entry не pending/disabled;
- development catalog не попал в release;
- core flow работает в airplane mode;
- итоговые APK/AAB/IPA размеры записаны и приняты.

## Этап 11. Зафиксировать release

Каталог, ruleset, fixtures и sign-off record должны попасть в один атомарный
commit.

В release record сохранить:

- commit SHA;
- release tag;
- catalog version;
- ruleset version;
- fixture suite version;
- content hash;
- reviewer;
- reviewedAt;
- test report location;
- artifact hashes и размеры.

После этого:

1. создать immutable release tag;
2. запретить force-update подписанного tag;
3. сохранить review evidence;
4. перейти к release packaging.

## Когда требуется новая подпись

Новая версия, hash и подпись обязательны при любом изменении:

- catalog entry;
- alias;
- EN/RU текста;
- diagram или text alternative;
- source record;
- rule;
- expected fixture outcome;
- fixture suite;
- release metadata;
- content hash contract.

Исправление одной опечатки после подписи тоже создаёт новый bundle.

## Финальный чек-лист Sergey V.

- [ ] Все entries имеют проверенные источники.
- [ ] Все license/usage решения записаны.
- [ ] Все EN/RU тексты проверены.
- [ ] Все diagrams оригинальны или разрешены.
- [ ] Все entries имеют положительные и отрицательные fixtures.
- [ ] Safety fixture suite version зафиксирована.
- [ ] Ноль известных false-positive `Compatible`.
- [ ] Hard-check negative/mutation tests проходят.
- [ ] `reviewedRuleCodes` полный и непустой.
- [ ] Catalog/ruleset/fixture versions совпадают с sign-off.
- [ ] Content hash рассчитан canonical project tool.
- [ ] Automated report приложен.
- [ ] AI findings рассмотрены как advisory.
- [ ] Exact diff просмотрен.
- [ ] Решение `APPROVED` записано с ISO-8601 датой.
- [ ] После hash каталог не менялся.
- [ ] Production validation прошла.
- [ ] Packaged artifact содержит exact approved catalog.

