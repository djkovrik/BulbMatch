# BulbMatch: подготовка, проверка и approval production-каталога

## 1. Что представляет собой каталог

BulbMatch не является каталогом товаров и не выбирает конкретную лампу,
артикул, бренд или магазин. Встроенный каталог — это небольшая, неизменяемая
для конкретной версии приложения база распознаваемых типов цоколей и
проверенных правил. На её основе приложение строит консервативный
`Compatible profile / Совместимый профиль` для самостоятельного поиска
замены.

Каталог поставляется внутри Android- и iOS-приложения, работает офлайн и
обновляется только вместе с новой версией приложения. При старте
`BundledCatalogLoader`:

1. строго декодирует UTF-8 JSON;
2. проверяет `schemaVersion`;
3. проверяет контракт SHA-256;
4. заново вычисляет canonical content hash;
5. проверяет обязательные metadata, уникальность кодов и aliases;
6. в production-режиме требует зафиксированный human approval;
7. передаёт разрешённые entries и проверенные voltage rules в
   `CatalogSnapshot`.

Если JSON повреждён, hash не совпадает, approval отсутствует или ruleset не
подключён, assessment и справочник должны оставаться недоступными. Приложение
не скачивает замену из сети и не подставляет непроверенный fallback.

Основные контракты находятся здесь:

- `spec/app-spec/domain.md` — алгоритм assessment и catalog contract;
- `spec/app-spec/data.md` — формат и offline-поведение bundled catalog;
- `spec/app-spec/quality.md` — safety fixture suite и release guardrails;
- `spec/app-spec/assets/source-licensing-manifest.md` — правила источников;
- `shared/data/src/commonMain/kotlin/com/sedsoftware/bulbmatch/data/catalog/BundledCatalog.kt`
  — текущая JSON-схема, canonical hash и runtime validation;
- `shared/data/src/commonMain/kotlin/com/sedsoftware/bulbmatch/data/DomainRepositories.kt`
  — преобразование JSON в доменный каталог;
- `shared/domain/src/commonMain/kotlin/com/sedsoftware/bulbmatch/domain/CompatibilityEngine.kt`
  — фактический подбор профиля замены.

### Как подбирается аналог

Подбор идёт только от подтверждённых пользователем фактов и в строгом порядке.

1. Пользователь должен подтвердить точный известный цоколь. `E27` не
   заменяется на «похожий резьбовой», `GU10` — на «похожий двухштырьковый».
2. Entry должна быть одновременно `APPROVED` и
   `enabledForAssessment: true` в подписанной версии каталога.
3. Пользователь должен отдельно подтвердить напряжение маркировки. Форма
   цоколя, популярность или «обычное применение» не доказывают напряжение.
4. Проверенный ruleset классифицирует напряжение. Подтверждённые 220–240 V
   относятся к целевой сети; 100–127 V и явно низковольтные значения дают
   `PotentialConflict`; неоднозначные значения дают `NeedClarification`.
5. Положительный результат содержит точный цоколь и требование искать лампу
   для 220–240 V / 50 Hz. Это не сертификат светильника, проводки, патрона,
   enclosure или dimmer.
6. Максимальная мощность светильника учитывается только тогда, когда
   пользователь отдельно вручную прочитал её на светильнике. Мощность старой
   лампы и OCR не могут создать fixture maximum.
7. Люмены, Kelvin, форма и dimmability добавляются как подтверждённые
   shopping preferences. Они не заменяют hard checks.

Иными словами, `Compatible` означает «из подтверждённых данных можно составить
консервативный профиль покупки», а не «эта конкретная лампа гарантированно
подойдёт и безопасна».

### Как читать development-пример

Исходный кандидат находится в:

```text
shared/data/src/commonMain/resources/catalog/bulbmatch-catalog-development.json
```

Он содержит десять candidate entries: `E27`, `E14`, `B22d`, `GU10`, `G9`,
`G4`, `GU5.3`, `G13`, `R7s` и `2G11`. Это стартовый список из AppSpec, а не
утверждённый production-набор. Сейчас у всех entries:

```json
"reviewState": "PENDING_HUMAN_SIGNOFF",
"enabledForAssessment": false
```

`sourceRecordIds` в нём ссылаются на candidate manifests, а не на
индивидуальные production source records. Названия содержат слово
`candidate`, `reviewedRuleCodes` пуст, а release metadata запрещает выпуск.

Примеры правильной интерпретации:

- `E27`: источник может подтвердить каноническое обозначение и резьбовой тип.
  Из `E27` нельзя вывести напряжение, допустимую мощность или размеры
  светильника.
- `GU10`: original hint может описывать два коротких twist-lock контакта.
  Нельзя автоматически заменить его на другой двухконтактный цоколь.
- `GU5.3`: даже если конкретные товары часто бывают низковольтными, сам код
  цоколя не доказывает 12 V. Напряжение всегда читается и подтверждается
  отдельно.

Для пополнения каталога development JSON можно использовать как структурный
пример, но нельзя механически переносить его candidate metadata в production.

## 2. Что в этом проекте означает «подписать каталог»

Текущая реализация не содержит асимметричной криптографической подписи,
сертификата или закрытого ключа. Здесь используются два разных механизма:

- SHA-256 `contentHash` обеспечивает контроль целостности exact canonical
  payload;
- human approval фиксирует решение именованного reviewer `Sergey V.` для
  конкретных catalog/ruleset/fixture versions.

SHA-256 сам по себе не доказывает, кто одобрил каталог. Поля
`requiredReviewer`, `reviewedAt` и `decision` сами по себе также не являются
криптографической подписью. Production accountability создаётся совокупностью:

1. versioned source records;
2. versioned ruleset;
3. versioned safety fixture suite;
4. автоматических отчётов;
5. финального sign-off record;
6. неизменяемого commit/tag;
7. canonical content hash, проверяемого приложением и release tooling.

Если в будущем потребуется юридически значимая или криптографическая подпись,
это отдельное изменение AppSpec и формата. Нельзя называть существующий
SHA-256 цифровой подписью.

## 3. Release bundle, который утверждает reviewer

Reviewer утверждает не отдельно взятый JSON, а один frozen bundle:

```text
spec/catalog/releases/<catalogVersion>/
├── README.md
├── sources/
│   ├── SRC-CAT-E27.md
│   ├── SRC-CAT-GU10.md
│   └── ...
├── rules/
│   └── ruleset.md
├── fixtures/
│   └── safety-fixtures.json
├── approval/
│   └── catalog-signoff.json
└── reports/
    ├── automated-checks.md
    ├── ai-review.md
    └── catalog-diff.md
```

Это рекомендуемое постоянное расположение production evidence. Файлы должны
находиться в Git, не содержать секретов и не включаться в приложение как
runtime resources. Сам shipping JSON хранится отдельно:

```text
shared/data/src/commonMain/resources/catalog/bulbmatch-catalog-production.json
```

Полный skeleton текущей schema v1:

```json
{
  "schemaVersion": 1,
  "catalogVersion": "<immutable catalog version>",
  "rulesetVersion": "<immutable ruleset version>",
  "publishedAt": "<YYYY-MM-DD>",
  "sourceManifestVersion": "<manifest version>",
  "contentHashAlgorithm": "SHA-256",
  "contentHashScope": "kotlinx-serialization canonical UnsignedCatalogPayload v1",
  "contentHash": "<canonical hash>",
  "release": {
    "state": "APPROVED",
    "releaseEligible": true,
    "requiredReviewer": "Sergey V.",
    "reviewedAt": "<ISO-8601 UTC timestamp>",
    "decision": "APPROVED"
  },
  "reviewedRuleCodes": [
    "<stable reviewed rule code>"
  ],
  "entries": [
    {
      "canonicalCode": "<exact base code>",
      "commonNameEn": "<reviewed original EN name>",
      "commonNameRu": "<reviewed original RU name>",
      "aliasesEn": ["<reviewed alias>"],
      "aliasesRu": ["<reviewed alias>"],
      "diagramId": "<original shipping diagram ID>",
      "distinguishingHintEn": "<reviewed original EN hint>",
      "distinguishingHintRu": "<reviewed original RU hint>",
      "sourceRecordIds": ["<existing production source record ID>"],
      "reviewState": "APPROVED",
      "enabledForAssessment": true
    }
  ]
}
```

`catalog-signoff.json` должен фиксировать минимум:

```json
{
  "catalogVersion": "2026.08.0",
  "rulesetVersion": "2026.08.0",
  "safetyFixtureSuiteVersion": "2026.08.0",
  "sourceManifestVersion": "1",
  "catalogContentHashAlgorithm": "SHA-256",
  "catalogContentHash": "<canonical catalog hash>",
  "rulesetReviewFileSha256": "<raw SHA-256 of frozen ruleset.md>",
  "runtimeRulesSourceSha256": "<raw SHA-256 of BundledCatalogRules.kt>",
  "safetyFixtureSuiteFileSha256": "<raw SHA-256 of frozen fixture file>",
  "reviewedCommit": "<full Git commit SHA>",
  "reviewer": "Sergey V.",
  "reviewedAt": "2026-08-15T12:30:00Z",
  "decision": "APPROVED"
}
```

Sign-off record является audit evidence. Runtime JSON продолжает
использовать собственные поля `release` и canonical `contentHash`.

## 4. Обязательная одноразовая подготовка репозитория

До первого production approval нужно один раз закрыть технические пробелы
текущей реализации. Пропуск этого раздела создаст JSON, который выглядит
approved, но не является доказанно production-ready.

### 4.1. Отделить production resource от development candidate

В release-ветке выполнить:

```powershell
git mv `
  shared/data/src/commonMain/resources/catalog/bulbmatch-catalog-development.json `
  shared/data/src/commonMain/resources/catalog/bulbmatch-catalog-production.json
```

Затем обновить все runtime и build references:

1. В `BundledCatalog.kt` переименовать
   `BUNDLED_DEVELOPMENT_CATALOG_RESOURCE_PATH` в
   `BUNDLED_CATALOG_RESOURCE_PATH` и установить значение:

   ```text
   catalog/bulbmatch-catalog-production.json
   ```

2. В `androidApp/src/main/kotlin/com/sedsoftware/bulbmatch/AndroidRootHolder.kt`
   использовать новый constant.
3. В `shared/compose/src/iosMain/kotlin/main.kt` изменить
   `pathForResource`:

   ```kotlin
   name = "bulbmatch-catalog-production"
   ```

4. В `shared/data/build.gradle.kts` направить
   `validateProductionCatalog` на production filename.
5. Переименовать
   `BundledDevelopmentCatalogResourceTest.kt` в
   `BundledProductionCatalogResourceTest.kt`.
6. Resource-test должен всегда проверять schema/hash в
   `CatalogValidationMode.Development`, чтобы pending candidate можно было
   готовить без ложного зелёного approval. Он больше не должен навсегда
   ожидать `ProductionApprovalRequired`. Production-режим exact resource
   проверяет отдельный `validateProductionCatalog` gate.

Development-копию нельзя оставлять в `commonMain/resources`: иначе она тоже
может попасть в shipping artifact. При необходимости пример сохраняют в Git
history или вне runtime resources.

### 4.2. Подключить реальный reviewed voltage ruleset

Сейчас Android и iOS передают в `DefaultCatalogProvider`:

```kotlin
voltageRules = emptyList()
```

Поэтому даже JSON с `APPROVED` будет давать
`ReviewedVoltageRulesMissing`. До review нужно:

1. создать одну commonMain-реализацию reviewed rules, например:

   ```text
   shared/data/src/commonMain/kotlin/com/sedsoftware/bulbmatch/data/catalog/BundledCatalogRules.kt
   ```

2. хранить в ней единственный общий для Android/iOS набор
   `VoltageFamilyRule`, target 220–240 V и 50 Hz;
3. присвоить каждому правилу стабильный code;
4. связать набор с точным `rulesetVersion`;
5. использовать его в обеих composition roots вместо `emptyList()`;
6. проверить, что `reviewedRuleCodes` в JSON содержит exact codes
   утверждённого ruleset;
7. добавить тест, который отклоняет расхождение code/version между JSON и
   common rules.

Минимальные смысловые ветки заданы AppSpec и тестами:

- 220–240 V — `InScope`;
- 100–127 V — `OutsideScope`;
- явно низковольтный диапазон — `OutsideScope`;
- непокрытое или перекрывающееся значение — `Ambiguous`.

Точные границы, reason codes и expected outcomes должен утвердить Sergey V.
Их нельзя принять только потому, что они встречаются в unit test.

### 4.3. Создать canonical hash tooling

В текущем репозитории нет задачи, которая печатает или обновляет canonical
hash. Перед release нужно добавить две стабильные задачи в
`shared/data/build.gradle.kts`:

```text
:shared:data:updateProductionCatalogHash
:shared:data:validateProductionCatalog
```

`updateProductionCatalogHash` должна:

1. прочитать production JSON как UTF-8;
2. декодировать его тем же strict `kotlinx.serialization` contract;
3. вызвать тот же алгоритм, что и
   `BundledCatalogLoader.contentHash(document)`;
4. изменить только top-level `contentHash`;
5. сохранить UTF-8 без BOM;
6. всегда повторно загрузить файл в `CatalogValidationMode.Development`;
7. если `release.state == "APPROVED"`, дополнительно загрузить его в
   `CatalogValidationMode.Production`;
8. завершиться с ненулевым exit code, если применимая validation не прошла;
9. напечатать catalog version и итоговый hash без пользовательских данных.

`validateProductionCatalog` нужно заменить с текущего поиска строк через
`contains` на typed validation. Она должна:

1. запустить runtime production loader;
2. проверить hash, metadata, reviewer, дату, `reviewedRuleCodes`, entries и
   aliases;
3. сверить catalog/ruleset/fixture versions с
   `spec/catalog/releases/<version>/approval/catalog-signoff.json`;
4. сверить hashes review ruleset, runtime rules source и fixture suite;
5. проверить, что все `sourceRecordIds` существуют;
6. проверить, что production resource является единственным shipping catalog;
7. завершаться только с `exit 0` при полном совпадении frozen bundle.

Raw file hash не заменяет canonical hash. Команды вида:

```powershell
Get-FileHash .\bulbmatch-catalog-production.json -Algorithm SHA256
```

или:

```bash
sha256sum bulbmatch-catalog-production.json
```

считают hash байтов целого файла. Runtime contract считает SHA-256 от
`UnsignedCatalogPayload`, исключая поле `contentHash`, но включая release
metadata, `reviewedRuleCodes` и entries.

### 4.4. Добавить versioned safety fixture suite

Создать:

```text
spec/catalog/releases/<catalogVersion>/fixtures/safety-fixtures.json
```

и test runner, например:

```text
shared/domain/src/commonTest/kotlin/com/sedsoftware/bulbmatch/domain/CatalogSafetyFixtureTest.kt
```

Fixture должен содержать:

```json
{
  "fixtureId": "VOLTAGE-LOW-12V",
  "suiteVersion": "2026.08.0",
  "catalogVersion": "2026.08.0",
  "rulesetVersion": "2026.08.0",
  "input": {},
  "expectedOutcome": "POTENTIAL_CONFLICT",
  "expectedReasonCodes": ["OUTSIDE_ELECTRICAL_SCOPE"],
  "coveredRuleCodes": ["VOLTAGE_LOW_OUTSIDE_SCOPE"],
  "reviewState": "APPROVED"
}
```

Полный набор обязан покрывать минимум из `spec/app-spec/quality.md`:

- known base + 220–240 V;
- отдельный reviewed case для 230 V;
- missing, unknown и unsupported base;
- missing voltage;
- 110–120 V, 12 V и 24 V;
- contradictory voltage tokens;
- source watts без fixture limit;
- вручную введённый fixture limit ниже source watts;
- lumens present/absent;
- `60 W equivalent` отдельно от actual `8 W`;
- dimmable yes/no/unknown;
- OCR rejected/edited/manual;
- EN/RU aliases каждого включённого цоколя.

Для `REQ-025` / `AC-025` любой missing, unknown, unsupported,
contradictory или outside-scope факт должен доказанно не давать `Compatible`.

### 4.5. Усилить тесты release bundle

До первого sign-off добавить проверки:

- valid production bundle проходит;
- изменение одного поля без обновления hash даёт `HashMismatch`;
- другой reviewer, пустой `reviewedAt` или `PENDING` блокируют production;
- пустой `reviewedRuleCodes` блокирует production;
- pending/disabled entry блокирует production;
- duplicate normalized code или alias блокирует production;
- неизвестное JSON-поле блокирует strict decoding;
- отсутствующий source record блокирует production;
- несовпадающий catalog/ruleset/fixture version блокирует production;
- несовпадающий review ruleset/runtime rules/fixture file hash блокирует
  production;
- Android и iOS используют один ruleset и один production resource;
- ни одна ветка approved safety suite не создаёт false-positive.

Только после merge этой одноразовой инфраструктуры можно выполнять
повторяемую процедуру ниже.

## 5. Пошаговая процедура выпуска каталога

### Шаг 1. Выполнить preflight и провалидировать AppSpec

Из корня репозитория:

```powershell
python `
  D:\Sources\vibe-skills\vibe-developer\scripts\validate-app-spec.py `
  .\spec\app-spec

git status --short
```

Продолжать можно только при `ERRORS (0)` и `RESULT: VALID`. Warnings не
скрывать: записать их в `reports/automated-checks.md` и оценить влияние на
catalog release. На момент написания гайда валидатор возвращает два
неблокирующих warning:

- legacy AppSpec не содержит машинного `uiQuality` contract;
- `capabilities.sync=false`, хотя sync-related terms встречаются в data/flows.

Ни один warning не разрешает catalog network updates или sync.

Рабочее дерево перед началом должно быть чистым. Если `git status --short`
показывает изменения, сначала определить их владельца и не смешивать
постороннюю работу с catalog release.

### Шаг 2. Создать release branch и зарезервировать версии

Начинать из проверенного commit:

```powershell
git status --short
git switch -c catalog/2026.08.0
```

Рабочее дерево должно быть чистым. Выбрать и записать:

```text
catalogVersion: 2026.08.0
rulesetVersion: 2026.08.0
safetyFixtureSuiteVersion: 2026.08.0
sourceManifestVersion: 1
publishedAt: YYYY-MM-DD
```

Версии не обязаны совпадать, но после начала финального review они
неизменяемы. Любая правка после approval получает новую версию.

Обновить в production JSON top-level поля `catalogVersion`,
`rulesetVersion`, `publishedAt` и `sourceManifestVersion`. Сохранить pending
release metadata, pending/disabled entries и provisional `contentHash`.
После любых content-правок до human review запускать
`:shared:data:updateProductionCatalogHash`: в pending-состоянии задача
проверяет Development mode и не выдаёт production approval.

Создать release evidence directory:

```powershell
$releaseRoot = 'spec/catalog/releases/2026.08.0'
New-Item -ItemType Directory -Force `
  "$releaseRoot/sources", `
  "$releaseRoot/rules", `
  "$releaseRoot/fixtures", `
  "$releaseRoot/approval", `
  "$releaseRoot/reports" | Out-Null
```

### Шаг 3. Проверить и зафиксировать источники

Для каждой entry создать отдельный файл:

```text
spec/catalog/releases/2026.08.0/sources/SRC-CAT-<CODE>.md
```

Шаблон:

```yaml
entryId: SRC-CAT-E27
canonicalBaseCode: E27
facts:
  - canonical designation E27
  - original identification wording used by BulbMatch
primarySourceId: SRC-001
primarySourceUrl: https://...
sourceAuthority: IEC
sourcePublicationOrVersion: ...
secondaryVerificationUrl: https://...
accessedAt: YYYY-MM-DD
transformation: >
  Canonical identifier retained; consumer text and diagram created
  independently; no standard sheet, gauge or dimensions copied.
redistributionDecision: IDENTIFIER_AND_ORIGINAL_PROSE_ONLY
reviewer: Sergey V.
reviewedAt:
reviewDecision: PENDING
aiAssistanceSummary: >
  Advisory cross-check only; no approval delegated to AI.
catalogVersion: 2026.08.0
contentHash:
```

Операции reviewer для каждого source record:

1. открыть primary source;
2. записать точную publication/version date;
3. записать текущую `accessedAt`;
4. проверить canonical code;
5. проверить, что извлечённый факт действительно есть в источнике;
6. отдельно проверить EN/RU wording и aliases;
7. описать transformation своими словами;
8. зафиксировать license/redistribution decision;
9. проверить secondary source, если без него остаётся неоднозначность;
10. не переносить стандартные sheets, gauges, размеры, drawings, logos,
    packaging, фотографии и marketing copy.

IEC 60061 используется для проверки обозначений и семейств цоколей. Его
database/subscription restrictions не разрешают копировать стандартные листы
или существенные части базы. EUR-Lex и European Commission допустимы как
источники терминологии для voltage, power, lumens и cap type, но не создают
универсальное fixture compatibility rule.

Официальные entry points из AppSpec, которые нужно открывать заново на дату
каждого release:

- [IEC 60061 database overview](https://webstore.iec.ch/en/iec_catalog/product/preview/?id=L3B1Yi9wZGYvcHJldmlldy9pbmZvX2llYzYwMDYxe2VkMS4wfWIucGRm);
- [Commission Regulation (EU) 2019/2020](https://eur-lex.europa.eu/legal-content/EN/TXT/?uri=CELEX%3A32019R2020);
- [Commission Delegated Regulation (EU) 2019/2015](https://eur-lex.europa.eu/legal-content/EN/ALL/?uri=CELEX%3A32019R2015);
- [European Commission: Light Sources](https://energy-efficient-products.ec.europa.eu/product-list/light-sources_en).

Manufacturer datasheet подтверждает только факты конкретного изделия. Нельзя
обобщать его напряжение, мощность или назначение на весь тип цоколя.

### Шаг 4. Сформировать entries

В production JSON для каждой entry заполнить:

```json
{
  "canonicalCode": "E27",
  "commonNameEn": "E27 screw base",
  "commonNameRu": "Резьбовой цоколь E27",
  "aliasesEn": ["E27"],
  "aliasesRu": ["E27"],
  "diagramId": "base_e27",
  "distinguishingHintEn": "Threaded base identified by the E27 marking",
  "distinguishingHintRu": "Резьбовой цоколь с маркировкой E27",
  "sourceRecordIds": ["SRC-CAT-E27"],
  "reviewState": "PENDING_HUMAN_SIGNOFF",
  "enabledForAssessment": false
}
```

Тексты выше иллюстрируют структуру, а не заранее утверждённый wording.

Для каждой entry выполнить содержательную проверку:

1. `canonicalCode` совпадает с source record;
2. normalized code уникален;
3. каждый alias принадлежит только одной entry;
4. EN/RU names и hints не обещают совместимость;
5. hint не выводит напряжение из формы цоколя;
6. `diagramId` существует в shipping assets;
7. diagram оригинален, не является standard drawing/gauge и имеет EN/RU text
   alternative;
8. каждый `sourceRecordId` разрешается в реальный файл;
9. есть positive и negative fixtures;
10. добавление entry не создаёт implicit substitution.

Normalization в текущем loader:

- `trim`;
- uppercase;
- удаление spaces, `-`, `_` и `.`.

Поэтому, например, `GU5.3` и `GU53` нормализуются одинаково и могут быть
aliases только одной entry.

### Шаг 5. Зафиксировать ruleset

Создать:

```text
spec/catalog/releases/2026.08.0/rules/ruleset.md
```

Для каждого stable rule code записать:

```text
ruleCode:
description:
inputs:
outcome:
reasonCode:
requirements:
acceptanceScenarios:
positiveFixtureIds:
negativeFixtureIds:
sourceRecordIds:
reviewer:
reviewDecision:
```

Набор должен покрывать как минимум:

- known enabled base;
- confirmed voltage;
- target voltage family;
- other mains family;
- explicit low voltage;
- ambiguous and contradictory voltage;
- no base substitution;
- fixture maximum manual origin;
- source watts versus fixture maximum;
- lumens and printed-equivalent separation;
- fixture safety disclaimer.

Связать правила с `REQ-006`, `REQ-007`, `REQ-009`, `REQ-010`, `REQ-011`,
`REQ-023`, `REQ-025` и соответствующими `AC-NNN`.

После содержательного review скопировать exact stable codes в
`reviewedRuleCodes` production JSON. Список не может быть пустым.

### Шаг 6. Заполнить и прогнать safety fixture suite

Заполнить
`spec/catalog/releases/2026.08.0/fixtures/safety-fixtures.json`, затем запустить
focused checks. На Windows команды Gradle выполняются через установленный
Vibe runner:

```powershell
$projectRoot = (Resolve-Path '.').Path
$runner = 'D:\Sources\vibe-skills\vibe-developer\scripts\run-gradle.ps1'
$logDir = Join-Path $projectRoot 'build\logs'
New-Item -ItemType Directory -Force $logDir | Out-Null

& $runner `
  -ProjectRoot $projectRoot `
  -Tasks @(':shared:data:updateProductionCatalogHash') `
  -LogPath (Join-Path $logDir 'catalog-candidate-hash.log')

& $runner `
  -ProjectRoot $projectRoot `
  -Tasks @(
    ':shared:domain:testAndroidHostTest',
    ':shared:data:testAndroidHostTest'
  ) `
  -LogPath (Join-Path $logDir 'catalog-preapproval-tests.log')
```

Требуется `exit 0`. В `reports/automated-checks.md` записать:

- full commit SHA;
- команды;
- exit codes;
- catalog/ruleset/fixture versions;
- количество fixtures;
- количество positive/negative cases;
- failures, skips и known limitations.

AI/multi-model review можно использовать для поиска пропусков. Все findings
записать в `reports/ai-review.md` со статусом `accepted`, `rejected` или
`requires human review`. AI не меняет `reviewDecision` и не ставит
`APPROVED`.

### Шаг 7. Подготовить diff для human review

Создать:

```text
spec/catalog/releases/2026.08.0/reports/catalog-diff.md
```

Приложить:

1. diff относительно предыдущей approved version или development candidate;
2. добавленные/удалённые entries;
3. aliases;
4. изменения EN/RU wording;
5. diagram changes;
6. rule changes;
7. fixture changes;
8. known limitations;
9. все unresolved findings.

До решения reviewer metadata остаётся pending:

```json
"release": {
  "state": "PRODUCTION_CANDIDATE_PENDING_HUMAN_SIGNOFF",
  "releaseEligible": false,
  "requiredReviewer": "Sergey V.",
  "reviewedAt": null,
  "decision": "PENDING"
}
```

Entries также остаются pending и disabled.

### Шаг 8. Провести human review

Sergey V. проверяет exact bundle:

1. production JSON;
2. все source records;
3. ruleset;
4. safety fixtures и expected outcomes;
5. diagrams и text alternatives;
6. automated report;
7. AI findings и их disposition;
8. catalog diff;
9. отсутствие известного false-positive `Compatible`.

Возможны три решения:

- `REJECTED` — release version не выпускается;
- `CHANGES_REQUIRED` — изменения вносятся при pending metadata, проверки
  повторяются с начала;
- `APPROVED` — reviewer разрешает записать approval metadata для exact
  frozen content.

Устное согласование без versions, времени, hash и sign-off record не является
production approval.

### Шаг 9. Записать approval metadata

Только после явного решения Sergey V. авторизованный оператор изменяет:

```json
"release": {
  "state": "APPROVED",
  "releaseEligible": true,
  "requiredReviewer": "Sergey V.",
  "reviewedAt": "2026-08-15T12:30:00Z",
  "decision": "APPROVED"
}
```

Для каждой действительно утверждённой entry:

```json
"reviewState": "APPROVED",
"enabledForAssessment": true
```

Если entry не утверждена, безопасный вариант — удалить её из production
catalog и выпустить новую candidate version позже. Текущий production loader
отклоняет весь документ, если хотя бы одна entry pending или disabled.

Одновременно заполнить `reviewer`, `reviewedAt` и `reviewDecision` в source
records, ruleset и fixtures. После этого никакие content fields больше не
редактировать.

### Шаг 10. Пересчитать canonical content hash

Hash пересчитывается после versions, approval metadata, reviewed rule codes и
entries, потому что все они входят в `UnsignedCatalogPayload`.

Запустить:

```powershell
& $runner `
  -ProjectRoot $projectRoot `
  -Tasks @(':shared:data:updateProductionCatalogHash') `
  -LogPath (Join-Path $logDir 'catalog-update-hash.log')
```

Проверить diff:

```powershell
git diff -- `
  shared/data/src/commonMain/resources/catalog/bulbmatch-catalog-production.json
```

Сама hash-задача должна изменить только строку `contentHash`. В общем
`git diff` также будут видны approval fields, осознанно изменённые на
предыдущем шаге. Если задача переформатировала, переупорядочила или изменила
другие значения, остановить процесс и проверить tooling.

Записать final canonical hash в:

- каждый source record;
- `approval/catalog-signoff.json`;
- `reports/automated-checks.md`.

Для review ruleset, фактического runtime rules source и fixture suite отдельно
вычислить raw file SHA-256:

```powershell
Get-FileHash `
  .\spec\catalog\releases\2026.08.0\rules\ruleset.md `
  -Algorithm SHA256

Get-FileHash `
  .\shared\data\src\commonMain\kotlin\com\sedsoftware\bulbmatch\data\catalog\BundledCatalogRules.kt `
  -Algorithm SHA256

Get-FileHash `
  .\spec\catalog\releases\2026.08.0\fixtures\safety-fixtures.json `
  -Algorithm SHA256
```

Эти три raw hashes записать в sign-off record. Они не заменяют canonical
catalog hash.

### Шаг 11. Зафиксировать финальный sign-off

Перед созданием sign-off Sergey V. повторно сверяет final JSON diff,
canonical catalog hash, review/runtime rules hashes, fixture hash и отсутствие
content-правок после решения `APPROVED`. Если final diff отличается от
разрешённых approval metadata и hash, вернуться в pending-состояние и повторить
review.

Так как commit SHA нельзя записать внутрь файла до создания самого commit без
циклической зависимости, использовать двухкоммитную схему:

1. создать content commit с frozen catalog, rules, fixtures, sources и
   reports, но без `catalog-signoff.json`;
2. получить его full SHA командой `git rev-parse HEAD`;
3. создать `approval/catalog-signoff.json` с exact versions, canonical catalog
   hash, review/runtime rules hashes, fixture hash, full content commit SHA,
   `Sergey V.`, ISO-8601 UTC timestamp и `APPROVED`;
4. создать отдельный sign-off commit, содержащий только approval record.

Tag ставится на sign-off commit. После sign-off commit запрещены content
правки. Любая правка создаёт новую catalog version и новый review bundle.

### Шаг 12. Запустить production validation

Focused gate:

```powershell
& $runner `
  -ProjectRoot $projectRoot `
  -Tasks @(
    ':shared:domain:testAndroidHostTest',
    ':shared:data:testAndroidHostTest',
    ':shared:data:validateProductionCatalog',
    ':shared:data:verifySqlDelightMigration'
  ) `
  -LogPath (Join-Path $logDir 'catalog-production-validation.log')
```

Затем Android CI-equivalent checks:

```powershell
& $runner `
  -ProjectRoot $projectRoot `
  -Tasks @(
    ':shared:platform:testAndroidHostTest',
    ':shared:app:testAndroidHostTest',
    ':shared:ads:testAndroidHostTest',
    ':shared:compose:compileAndroidMain',
    ':androidApp:assembleDebug'
  ) `
  -LogPath (Join-Path $logDir 'catalog-android-integration.log')
```

Каждый запуск должен завершиться с `exit 0`. `validateProductionCatalog`
является catalog gate; полноценная release-сборка дополнительно зависит от
отдельных Yandex и Crashlytics gates и может ожидаемо оставаться красной.

На macOS выполнить:

```bash
cd iosApp
pod install --repo-update
cd ..

./gradlew \
  :shared:domain:iosSimulatorArm64Test \
  :shared:data:iosSimulatorArm64Test \
  :shared:platform:iosSimulatorArm64Test \
  :shared:app:iosSimulatorArm64Test \
  :shared:compose:iosSimulatorArm64Test \
  :shared:compose:linkDebugFrameworkIosSimulatorArm64 \
  --stacktrace

xcodebuild \
  -workspace iosApp/iosApp.xcworkspace \
  -scheme iosApp \
  -configuration Debug \
  -destination "generic/platform=iOS Simulator" \
  CODE_SIGNING_ALLOWED=NO \
  build
```

Нельзя заявлять iOS check успешным, если он не запускался на macOS.

### Шаг 13. Проверить packaged artifacts

Для Android:

1. собрать APK/AAB;
2. найти внутри artifact
   `catalog/bulbmatch-catalog-production.json`;
3. извлечь файл во временную директорию;
4. побайтно сравнить его с approved source JSON;
5. загрузить извлечённый JSON production loader;
6. проверить versions/hash в Settings;
7. убедиться, что development JSON не упакован.

Пример поиска ресурса в debug APK:

```powershell
jar tf .\androidApp\build\outputs\apk\debug\androidApp-debug.apk |
  Select-String 'bulbmatch-catalog'
```

Для iOS:

1. build выполнять через `iosApp/iosApp.xcworkspace`;
2. найти production JSON внутри собранного `.app`;
3. убедиться, что `NSBundle.pathForResource` возвращает его;
4. сравнить bytes и canonical hash;
5. проверить Settings и offline reference/assessment.

На физическом Android и iPhone после clean install включить Airplane Mode и
проверить:

- reference search;
- EN/RU aliases;
- выбор каждого approved base;
- known base + 230 V;
- unknown base;
- 110–120 V;
- 12/24 V;
- сохранение и повторное открытие результата;
- отображение exact catalog/ruleset versions.

Simulator/emulator не заменяет обязательные physical-device проверки из
release checklist.

### Шаг 14. Создать immutable release tag

После успешных gates:

```powershell
git status --short
git log -2 --format=fuller
git tag -a catalog-2026.08.0 -m "Approved BulbMatch catalog 2026.08.0"
git show catalog-2026.08.0
```

Tag должен указывать на sign-off commit. Не force-update утверждённый tag.
В release record сохранить:

- tag;
- content commit SHA;
- sign-off commit SHA;
- catalog/ruleset/fixture versions;
- canonical catalog hash;
- review ruleset, runtime rules source и fixture hashes;
- reviewer and reviewedAt;
- exact test logs;
- Android/iOS artifact hashes и размеры;
- не выполненные manual checks.

## 6. Когда approval аннулируется

Новая version, новый hash и новый approval обязательны при изменении любого из
следующих элементов:

- entry, alias, EN/RU name или hint;
- `enabledForAssessment`;
- diagram или text alternative;
- source record, access date или license decision;
- voltage rule, reason code или rule code;
- fixture input или expected outcome;
- catalog/ruleset/fixture version;
- release metadata;
- hash contract или schema;
- runtime mapping, способный изменить assessment.

Даже исправление опечатки после approval создаёт новый bundle. Нельзя
«поправить только текст» в уже подписанном JSON.

## 7. Stop conditions

Немедленно остановить production approval, если:

- AppSpec validator выдаёт error;
- источник не подтверждает факт;
- права на shipping content неясны;
- найден конфликт aliases или canonical codes;
- хотя бы один required fact может дать false-positive `Compatible`;
- voltage rules отсутствуют или расходятся между Android/iOS;
- fixture suite не покрывает `AC-025`;
- hash вычислялся не проектным canonical tooling;
- reviewer не проверил exact final diff;
- approval metadata меняется только ради зелёного Gradle gate;
- production validation или packaged-artifact comparison не прошли;
- после sign-off изменился любой content file.

Безопасный результат при неопределённости — `NeedClarification` или
`PotentialConflict`, а не ослабление правила или фиктивный approval.

## 8. Требования и acceptance coverage

Этот процесс обеспечивает traceability прежде всего для:

- `REQ-006` / `AC-006` — known base и confirmed voltage;
- `REQ-007` / `AC-007` — unknown base не подменяется аналогом;
- `REQ-009` / `AC-009` — conflicting/out-of-scope voltage не даёт positive
  outcome;
- `REQ-010` / `AC-010` — fixture maximum не выводится из source watts;
- `REQ-023` / `AC-023` — каждая entry/rule разрешается в source record,
  versions, access date, transformation, reviewer и hash;
- `REQ-025` / `AC-025` — approved safety fixture suite доказывает отсутствие
  false-positive для missing/unknown/unsupported/contradictory/out-of-scope
  фактов.

Финальным reviewer остаётся только **Sergey V.** Автоматические проверки и AI
предоставляют evidence, но не могут создать human approval.
