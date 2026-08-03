# BulbMatch quality and release contract

## Quality priorities

1. Prevent a false-positive compatible result.
2. Preserve user control over OCR-derived facts.
3. Keep the core flow available offline.
4. Make warnings and recovery accessible.
5. Keep monetization subordinate and failure-tolerant.

## Acceptance traceability

Every `REQ-NNN` owns exactly one `AC-NNN` in the flow files. Implementation tests may cover multiple acceptance scenarios, but each scenario has one canonical flow location. Screen files link the requirements and flows they render.

## Test pyramid

### Pure common tests

- table-driven value-object boundaries for voltage, watts, lumens, Kelvin, names, and ranges;
- every ordered branch of `CompatibilityEngine`;
- a property invariant that any unknown/missing base, missing/out-of-scope voltage, or confirmed non-50 Hz frequency never yields `Compatible`;
- exact-base matching with no implicit substitutions;
- fixture maximum origin enforcement and proof that source watts cannot populate it;
- brightness and printed-equivalent separation;
- deterministic explanations and shopping-profile output for the same input/catalog version;
- locale-independent parsing fixtures for comma/dot decimals and OCR punctuation.
- exact Cyrillic numeric-unit parsing with immutable raw text and negative confusable-token coverage;
- property-style checks proving OCR normalization cannot invent a base or voltage candidate. (`REQ-027`, `AC-027`)

### Store/component tests

- use real state-machine/component factories and deterministic fakes;
- all camera permission transitions, settings-return resume recheck, and camera-unavailable alternatives;
- OCR candidates cannot advance while any candidate is undecided;
- process recreation drops the draft and preserves only the neutral message;
- result Save, Match another, back, and parent navigation outputs;
- history observation, stable ordering, reopen, delete-one, and clear-all confirmation;
- locale/theme changes update visible models without losing durable data;
- ads never gate content/navigation and interstitial eligibility follows the exact counter/cooldown rules.

### Persistence tests

- real SQLDelight schema CRUD, ordering, transaction rollback, and driver close;
- snapshot codec round-trip and malformed/legacy snapshot isolation;
- every released migration from its original schema;
- settings missing/malformed/default/update/reset paths;
- Android and iOS repository compilation.

### Platform tests

- Android and iOS camera permission denied/granted/revoked/settings-return behavior on devices;
- Android system photo picker without broad media permission;
- iOS picker without library-wide authorization;
- bundled/offline OCR after a clean install with airplane mode;
- legible, blurred, rotated, low-contrast, numeric-only, Latin, Cyrillic, and no-text fixture images;
- Android API 26 and a current Android API; iOS 16.2 or newer;
- every packaged Android 64-bit native library has 16 KB-compatible ELF LOAD alignment, the APK passes 16 KB ZIP alignment validation, and OCR runs on a 16 KB Android environment;
- controlled Crashlytics test report with forbidden-data inspection;
- Yandex test banners/interstitials, SDK privacy setup before initialization, ad failure collapse, and release-ID guard.

### Visual tests

Deterministic Compose previews and screenshot tests cover each screen's content, loading, empty, error, offline, permission, and ad-present/ad-absent states where applicable:

- EN and RU;
- compact phone and expanded phone/tablet width;
- light and dark themes;
- 100% and 200% font scale;
- LTR screen-reader semantic ordering;
- navigation bars, display cutouts, and iOS safe areas.

Golden updates require human review. Safety text clipping, ad overlap, hidden actions, and color-only statuses block approval.

## Safety fixture suite

Maintain a versioned, source-linked fixture set with expected outcomes. It contains only synthetic or licensed label text/images and no user photos.

Minimum cases:

- known base + clear 220–240 V marking;
- known base + single 230 V marking reviewed under an explicitly documented rule;
- missing base;
- unknown raw base;
- missing voltage;
- 110–120 V marking;
- 12 V and 24 V markings;
- contradictory voltage tokens;
- confirmed 50 Hz and confirmed 60 Hz;
- source watts with no fixture limit;
- separately entered fixture limit below source watts;
- lumens present/absent;
- “60 W equivalent” separated from “8 W” actual power;
- dimmable yes/no/unknown;
- OCR candidate rejected, edited, and manually supplied;
- supported known base aliases in EN/RU search.

Sergey V. is the named final catalog/ruleset reviewer and approves expected outcomes and rule provenance. Automated checks and multiple AI models should expand coverage and challenge assumptions, but AI output is advisory and cannot replace the recorded human release decision.

## OCR fixture suite

Maintain OCR images separately from the signed catalog safety fixtures under `spec/ocr-fixtures/v1/`. The set contains 60–100 synthetic or explicitly licensed images and a manifest with provenance, visible source text, expected raw observations, expected parsed candidates, script, difficulty, and negative/ambiguous tags. It contains no user photographs.

The release OCR gate requires:

- zero false base or voltage candidates on the complete negative/ambiguous corpus;
- at least 95% exact field recall on controlled legible Cyrillic fixtures;
- at least 95% exact field recall across the complete controlled legible supported-script corpus;
- Latin field recall no more than two percentage points below the recorded ML Kit baseline;
- stable output and no retained-image or unbounded-memory growth across twenty repeated recognitions;
- a recorded package-size and peak-memory comparison before removing the previous OCR engine.

## Release guardrails

- `validate-app-spec.py` returns zero errors before implementation begins.
- All common and platform build/test tasks pass for Android; available iOS compile/test tasks pass on macOS CI before iOS release.
- Zero false-positive `Compatible` results across the full safety fixture suite.
- Mutation or negative tests prove each hard-check removal would be caught.
- No known test ad ID is present in a release artifact; all eight production keys are supplied.
- The release ad keys exactly match the approved mapping in `app-spec.json`.
- No Crashlytics report inspected in the release test contains forbidden product/user data.
- The packaged catalog hash matches its manifest, and catalog/ruleset versions are visible in Settings.
- Sergey V. records approval of the exact catalog, ruleset, and safety-fixture-suite versions after reviewing automated and AI-assisted evidence.
- App operates from clean install in airplane mode for the full core flow.
- The packaged OCR model set matches `ocr-model-manifest.json`, appears exactly once per platform bundle, and no ML Kit dependency remains after migration.
- Android APK/AAB native libraries pass automated 16 KB ELF and ZIP alignment gates.
- Accessibility audit finds no critical/blocking issue.
- The final APK/AAB/IPA size is recorded and accepted; bundled OCR size is intentional.

## Performance budgets

Measure on a documented mid-range Android reference device and a supported iPhone:

- first meaningful Match content: under 1.0 s p95 warm and under 2.0 s p95 cold, excluding OS splash;
- OCR candidates after image confirmation: under 2.5 s p95 for a legible 12 MP-or-smaller image after downsampling;
- assessment: under 100 ms p95;
- history first 100 rows: under 300 ms p95;
- reference search first result update: under 100 ms p95;
- no main-thread database or full-resolution OCR decode work;
- no product interaction waits for ad or crash SDK initialization.

## Accessibility gates

- semantic status heading is announced before reasons, warning, checklist, actions, and advertisement;
- all fields expose label, value, source (`Detected`, `Edited`, `Manual`), error, and decision state;
- diagrams have text alternatives containing the canonical base code and distinguishing features;
- minimum 48 dp targets and visible focus;
- status does not rely on hue;
- 200% font scale has no clipped safety copy, overlapped banner, or unreachable button;
- destructive actions require confirmation and focus returns predictably after cancel;
- reduced-motion preference removes nonessential transitions.

## Privacy and security review

- inspect merged Android manifest for broad media, advertising ID, and unexpected SDK permissions;
- inspect iOS `Info.plist` purpose strings, SKAdNetwork entries, and absence of ATT usage description;
- verify provider initialization order and non-personalized configuration;
- scan logs and crash reports with synthetic canary OCR/name strings and assert none leave the device;
- confirm no photo/cache files remain after completion, cancellation, and process death;
- keep true secrets such as signing credentials and provider configuration files in ignored/local or CI secret storage; the approved Yandex placement IDs may remain in the AppSpec and build configuration.

## Manual product review

Before release, a novice EN reviewer and a novice RU reviewer complete:

1. camera success;
2. picker success;
3. manual success;
4. unknown-base clarification;
5. out-of-scope voltage conflict;
6. save/reopen/delete;
7. offline reference;
8. 200% font scale with screen reader;
9. ad-present and ad-failed paths.

Reviewers must correctly explain what the positive result does and does not guarantee. If they interpret it as fixture certification, copy/design must change.

## Observability without product analytics

Use only provider operational consoles:

- Firebase Crashlytics for crash/non-fatal stability;
- Yandex reporting for impressions and revenue.

Do not add custom product events, funnels, attribution, session replay, or user profiling. Local counters exist solely to enforce frequency and are never transmitted by BulbMatch.
