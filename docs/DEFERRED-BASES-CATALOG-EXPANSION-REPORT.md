# Отчёт о приоритете поддержки deferred-цоколей

Дата анализа: `2026-07-31`

Статус: рекомендательный отчёт. Он не изменяет AppSpec, production catalog,
ruleset, safety fixtures или human approval и не считается решением о включении
новых цоколей.

## Краткий вывод

Для ближайшей новой версии каталога проще и безопаснее всего добавить **GX53**.
В текущем frozen evidence прямо зафиксировано, что для GX53 существуют изделия
220–240 V, а причиной deferred-статуса были отсутствие в первоначальном наборе
кандидатов и диаграмм, а не неучтённое электрическое условие. Это не будет
изменением алгоритма совместимости, но всё равно потребует отдельной диаграммы,
source record, fixtures, новой release-папки, хешей и одобрения Sergey V.

Следующий эшелон — **G4 и GU5.3**, примерно одинаковые по трудоёмкости. Код уже
умеет безопасно отклонять подтверждённые 12 V, а оба обозначения присутствуют в
исходном AppSpec и visual asset manifest. Однако имеющееся evidence для текущего
релиза подтверждает именно 12-вольтовые семейства, то есть они находятся вне
целевого сценария 220–240 V. Их стоит включать только после отдельного решения о
ценности такой поддержки и нового source review; техническая простота сама по
себе недостаточна.

**G13 и 2G11 не следует добавлять catalog-only изменением.** Для них одного
точного цоколя и маркировки напряжения недостаточно: текущая модель не учитывает
ПРА, режим подключения, direct-wire retrofit и возможную переделку проводки.
При простом включении entry движок сможет вернуть `Compatible`, не установив эти
факты, что конфликтует с `REQ-025` / `AC-025`. Из этой пары G13 несколько проще,
а 2G11 сложнее из-за зафиксированной dual-mode/control-gear неоднозначности.

Итоговый порядок безопасной реализуемости:

1. **GX53** — рекомендуемый единственный кандидат следующей catalog release.
2. **G4 / GU5.3** — условный второй эшелон после нового evidence и продуктового
   решения; G4 лишь немного проще по aliases/normalization.
3. **G13** — только после расширения доменной модели.
4. **2G11** — после того же расширения, с дополнительным моделированием режимов.

## Границы и проверенная исходная точка

Анализ основан только на текущем репозитории и его frozen evidence; свежий
внешний market/source review не выполнялся. Перед реальным включением каждого
цоколя официальные источники необходимо открыть повторно на дату нового релиза.

AppSpec валиден: validator завершился с `ERRORS (0)`, `RESULT: VALID` и тремя
неблокирующими предупреждениями:

- legacy AppSpec не содержит отдельного `uiQuality` contract;
- legacy AppSpec не содержит отдельного localization contract;
- `capabilities.sync=false`, но в data/flow тексте встречаются sync-термины.

Предупреждение про sync не разрешает сетевое обновление каталога. Новые entries
по-прежнему должны поставляться только вместе с релизом приложения.

Текущий production catalog содержит шесть enabled entries: `E27`, `E14`,
`B22d`, `GU10`, `G9`, `R7s`. На текущем checkout успешно выполнены:

- `:shared:data:catalogToolsTest` — exit `0`;
- `:shared:data:validateProductionCatalog` — exit `0`.

Следовательно, новую версию нужно создавать отдельно, не изменяя уже
подписанный bundle `2026.08.01`. Некоторые status-тексты внутри release reports
всё ещё говорят `awaiting final sign-off`, но фактический
`catalog-signoff.json` заполнен и production validation проходит; для оценки
использован работающий gate.

Основные источники внутри репозитория:

- [AppSpec data contract](../spec/app-spec/data.md);
- [AppSpec domain invariants](../spec/app-spec/domain.md);
- [AppSpec quality contract](../spec/app-spec/quality.md);
- [FLOW-006 / AC-014 / AC-023](../spec/app-spec/flows/FLOW-006.md);
- [frozen release README](../spec/catalog/releases/2026.08.01/README.md);
- [catalog diff](../spec/catalog/releases/2026.08.01/reports/catalog-diff.md);
- [AI-assisted review](../spec/catalog/releases/2026.08.01/reports/ai-review.md);
- [source/licensing manifest](../spec/app-spec/assets/source-licensing-manifest.md);
- [visual asset manifest](../spec/app-spec/assets/visual-asset-manifest.md);
- [production catalog](../shared/data/src/commonMain/resources/catalog/bulbmatch-catalog-production.json).

## Что уже является универсальным

Добавление нового canonical code не требует нового доменного enum:
`BaseCode` принимает любое непустое обозначение длиной до 32 символов без
пробелов. `G4`, `GU5.3`, `G13`, `2G11` и `GX53` укладываются в этот контракт.

Catalog loader и provider также data-driven:

- canonical code, EN/RU names, aliases, hint и `diagramId` читаются из JSON;
- поиск нормализует регистр и удаляет пробелы, `-`, `_`, `.`;
- поэтому `GU5.3` и `GU53` считаются одним поисковым токеном и должны
  принадлежать только одной entry;
- compatibility engine проверяет точное присутствие code в
  `enabledBaseCodes`; похожий цоколь не подставляется;
- отдельно подтверждённые `1–48 V` уже дают
  `PotentialConflict(OutsideElectricalScope)`;
- отдельно подтверждённые `220–240 V` проходят существующую voltage gate;
- отсутствующее, противоречивое или неподдержанное значение не даёт
  `Compatible`.

Это покрывает базовую семантику `REQ-006`, `REQ-007`, `REQ-009` и `REQ-025`
без специальной ветки на каждый новый pin-base.

## Что сейчас не является catalog-only

### Диаграммы

Catalog entry хранит `diagramId`, но Compose presentation сейчас его не
использует. `BaseDiagram` выбирает рисунок по canonical code и имеет отдельные
ветки только для текущих шести цоколей; для любого другого code рисуется
generic two-pin fallback:

- [BaseDiagram implementation](../shared/compose/src/commonMain/kotlin/com/sedsoftware/bulbmatch/compose/components/Components.kt).

Такой fallback не доказывает выполнение `REQ-014` / `AC-014`, которые требуют
оригинальную распознаваемую диаграмму для каждого поддерживаемого цоколя.
Особенно нельзя выпускать через generic fallback GX53, G13 или четырёхконтактный
2G11. Для новой версии потребуется добавить проверенную Compose/vector
диаграмму и связать presentation с `diagramId` либо с явным исчерпывающим
реестром code-to-diagram.

Текущий loader проверяет только непустой `diagramId`, а release tool не
проверяет его наличие в shipping asset registry. Поэтому JSON может пройти
автоматический gate с несуществующим или фактически неиспользуемым asset ID.
Перед расширением каталога стоит добавить автоматическую проверку этого
соответствия.

### Production entry нельзя сделать reference-only

Production validation отклоняет любую entry, у которой
`reviewState != APPROVED` или `enabledForAssessment == false`. Поэтому в
текущей schema нельзя добавить G4/GU5.3 только в справочник, оставив их
недоступными для assessment. Такая двухуровневая поддержка потребовала бы
отдельного изменения AppSpec/schema/runtime gate; ослаблять существующий
production validator без продуктового решения нельзя.

### Новая версия затрагивает весь frozen bundle

Release tool выбирает evidence directory по точному `catalogVersion` и требует
в нём ruleset, safety fixtures, sign-off и source record для каждой entry.
Source records должны содержать новый `catalogVersion` и новый catalog
`contentHash`. Поэтому даже одна новая entry означает новый полный bundle, а
не добавление одного JSON-фрагмента.

Кроме того, `CatalogSafetyFixtureTest` сейчас жёстко ссылается на
`spec/catalog/releases/2026.08.01` и на конкретные suite version/review time.
Этот тест придётся обновить либо предварительно сделать metadata-driven.

## Сравнение deferred-цоколей

| Цоколь | Семантика в текущем engine | Evidence readiness | Диаграмма/UI | Итог |
|---|---|---|---|---|
| **GX53** | Новой электрической ветки не видно: точный base + отдельно подтверждённые 220–240 V укладываются в действующие rules | Лучший задел: frozen README говорит о существовании 220–240 V products, но versioned `SRC-CAT-GX53` отсутствует | Нет в initial asset inventory и нет dedicated renderer; нужен новый original diagram | **Самый простой безопасный кандидат** |
| **G4** | 12 V уже безопасно блокируется; hidden control-gear gap в текущем evidence не заявлен | Текущее evidence отрицательное для target: рассмотрены 12 V families | `base_g4` запланирован в AppSpec, но dedicated renderer отсутствует | **Условно второй эшелон** после нового evidence/решения |
| **GU5.3** | Как у G4; текущая normalization уже поддерживает точку | Текущее evidence отрицательное для target: рассмотрены 12 V families | `base_gu5_3` запланирован, renderer отсутствует; нужен отдельный review близких обозначений и aliases | **Условно второй эшелон**, чуть больше alias-risk |
| **G13** | Base + voltage недостаточны: ПРА/retrofit/rewiring не моделируются | Нужны источники не только на designation, но и на безопасную границу поддерживаемых режимов | Planned asset есть, dedicated renderer отсутствует | **Не catalog-only**; требуется доменная работа |
| **2G11** | Та же проблема плюс зафиксированная control-gear-specific/dual-mode неоднозначность | Наибольший evidence и wording gap | Planned four-pin asset есть, generic fallback явно недостаточен | **Самый сложный** из пяти |

### 1. GX53

Почему первый:

- current release прямо отделяет его от G13/2G11: причина deferral — отсутствие
  в initial candidate/diagram inventory;
- в том же документе отмечено наличие 220–240 V products;
- существующие voltage/frequency, exact-base и no-substitution rules подходят
  без ослабления;
- нет заявленной необходимости моделировать control gear или wiring mode.

Что всё ещё блокирует выпуск:

- нет versioned source record и повторной проверки official primary/secondary
  sources;
- GX53 отсутствует в initial AppSpec candidate list и visual asset manifest;
- нет оригинальной диаграммы и dedicated Compose renderer;
- текущий negative fixture `BASE-UNSUPPORTED-GX53` перестанет быть валиден и
  должен быть заменён другим намеренно unsupported code;
- нужны positive assessment, no-substitution, EN/RU alias/search и diagram
  fixtures/previews;
- нужен новый полный review и sign-off Sergey V.

### 2. G4 и GU5.3

Почему технически близки к готовности:

- оба уже перечислены как AppSpec candidates;
- оба имеют planned asset IDs;
- low-voltage rule `VOLTAGE_LOW_1_48` уже предотвращает positive outcome для
  подтверждённых 12 V;
- parser, BaseCode, alias index и catalog schema не требуют расширения.

Почему не рекомендуются раньше GX53:

- текущий release review специально отложил их как 12 V product families вне
  recommendation target;
- source manifest разрешает использовать manufacturer evidence только для
  конкретной модели и запрещает переносить её voltage/compatibility на весь
  base type;
- production schema не позволяет ограничиться справочной, assessment-disabled
  entry;
- без нового evidence и явного продуктового решения расширение даст мало
  target-value, хотя потребует полный release/sign-off cycle.

G4 немного проще механически: canonical token не содержит точки и имеет меньше
риска пересечения aliases. Для GU5.3 нужно особенно проверить, что `GU5.3`,
`GU 5.3`, `GU-5.3` и `GU53` принадлежат только этой entry, а похожие G5.3/GX5.3
не становятся неявными aliases. Это не основание признать G4 безопаснее — только
небольшая разница в тестовой работе.

### 3. G13 и 2G11

Текущий `CompatibilityEngine` знает только exact enabled base, voltage,
frequency и общие confirmed lamp facts. Он не спрашивает и не сохраняет:

- тип/наличие control gear;
- EM/HF/direct-wire или другой operating mode;
- необходимость rewiring или удаления/обхода ПРА;
- подтверждение, что retrofit рассчитан на существующую схему.

Поэтому catalog-only entry превратит base + 230 V в потенциальный
`Compatible`, хотя ключевая совместимость не установлена. Общая advisory-фраза
«проверьте светильник» не заменяет hard gate, если current evidence уже
указывает на систематическую неоднозначность.

Для G13/2G11 сначала требуется отдельное изменение AppSpec и доменной модели с
новыми confirmed inputs, outcomes/reasons, safety fixtures и UI confirmation.
Если безопасная модель потребует инструкций по rewiring, это дополнительно
конфликтует с текущей границей продукта, который не сертифицирует проводку и
монтаж; такое расширение требует явного решения пользователя, а не только
catalog review.

## Минимальный пакет для рекомендуемого GX53-релиза

1. Зафиксировать явное продуктовое решение о включении GX53 в новую версию и
   обновить candidate/asset inventory AppSpec, не меняя `REQ-006`, `REQ-007`,
   `REQ-009`, `REQ-014`, `REQ-023`, `REQ-025` или соответствующие safety
   outcomes.
2. Повторно проверить official IEC identifier и конкретное актуальное
   220–240 V product evidence; добавить `SRC-CAT-GX53` с license/transformation
   решением и human review.
3. Создать оригинальную GX53 identification diagram с EN/RU alternative text;
   не копировать IEC gauge/dimensions или manufacturer artwork.
4. Перевести diagram rendering на проверяемый `diagramId` registry и добавить
   gate, что каждая production entry имеет реально используемую shipping
   диаграмму.
5. Добавить catalog entry с точными EN/RU names, hints и aliases; не выводить
   voltage из base form.
6. Создать новый `spec/catalog/releases/<version>` и обновить весь evidence
   bundle. Безопаснее следовать текущей конвенции и версионировать catalog,
   ruleset и fixture suite вместе; если ruleset semantics оставляются прежними,
   это должно быть явно записано и всё равно повторно подписано.
7. Обновить fixtures:
   positive GX53 + 220–240 V, low/out-of-scope voltage, missing voltage,
   no-substitution, EN/RU aliases и новый intentionally unsupported base вместо
   GX53.
8. Обновить metadata-driven/hardcoded catalog tests и SCREEN-009/010 previews.
   Не записывать/обновлять Paparazzi goldens до review диаграммы и визуального
   изменения.
9. Пересчитать canonical/raw hashes, проверить packaged resource и получить
   отдельный sign-off Sergey V. на точные версии, fixtures, diagram и commit.

## Обязательные проверки при реализации

Focused checks:

- AppSpec validator: zero errors, warnings reported separately;
- `:shared:domain:testAndroidHostTest`;
- `:shared:data:catalogToolsTest`;
- `:shared:data:testAndroidHostTest`;
- `:shared:data:validateProductionCatalog` после human sign-off;
- SCREEN-009/010 preview/Paparazzi verification после одобренного visual change;
- packaged APK catalog byte/hash comparison.

Затем требуется полный применимый Android CI-equivalent набор. Изменение общей
Compose/data логики также требует iOS Gradle/framework/Xcode проверок на macOS.
Нельзя утверждать, что physical-device, offline OCR, accessibility или release
manual gates пройдены, пока они не выполнены отдельно.

## Риски и рекомендуемые preparatory improvements

1. **Asset integrity gap:** автоматизировать проверку `diagramId -> shipping
   renderer/asset`, иначе catalog validation может дать ложное ощущение
   готовности `AC-014`.
2. **Hardcoded release version:** получать fixture directory и expected version
   из catalog metadata, оставив строгую проверку exact bundle, чтобы следующая
   catalog release не требовала случайных ручных констант.
3. **Unsupported-base fixture:** не использовать будущий очевидный кандидат как
   вечный negative fixture; завести явно зарезервированный synthetic code либо
   version-local unsupported designation с review rationale.
4. **Reference-only ambiguity:** не ослаблять production gate ради G4/GU5.3.
   Если продукту нужна справочная запись без разрешения assessment, сначала
   определить это как отдельную AppSpec/schema capability и доказать, что UI не
   представляет её как поддерживаемую замену.
5. **Control-gear boundary:** не прятать G13/2G11 complexity в hint/advisory.
   Systematic compatibility condition должно быть либо hard confirmed input,
   либо явной причиной оставить base unsupported.

## Покрытие требований и отклонения

Отчёт оценивал влияние на:

- `REQ-006` / `AC-006` — known base + confirmed voltage;
- `REQ-007` / `AC-007` — unknown/unsupported base;
- `REQ-009` / `AC-009` — electrical/frequency scope;
- `REQ-014` / `AC-014` — offline localized reference и original diagram;
- `REQ-023` / `AC-023` — versioned source/audit bundle;
- `REQ-025` / `AC-025` — отсутствие false-positive `Compatible`.

AppSpec deviations не вносились. Catalog, ruleset, fixtures, diagrams и product
behavior не изменялись.
