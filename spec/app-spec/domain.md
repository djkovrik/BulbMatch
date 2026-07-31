# BulbMatch domain contract

## Ubiquitous language

- **Source lamp** — the old lamp whose printed markings the user is recording.
- **Fixture fact** — a value read from the fixture itself; in MVP only optional maximum wattage is modeled.
- **Observed field** — an OCR candidate plus origin and confidence. It is never trusted domain data.
- **Confirmed field** — a user-confirmed, edited, manually entered, or explicitly absent value.
- **Base code** — a canonical catalog identifier such as `E27` or `GU10`.
- **Raw base text** — user-confirmed text that cannot be mapped to a supported base.
- **Electrical scope** — the MVP target `220–240 V / 50 Hz`.
- **Replacement profile** — conservative shopping constraints derived only from confirmed facts and reviewed rules.
- **Assessment** — one of `Compatible`, `NeedClarification`, or `PotentialConflict`.
- **Hard check** — a condition that must pass before `Compatible`.
- **Advisory check** — useful information that cannot establish safety by itself.

## Core value objects

All domain types live in common code and contain no Android, iOS, OCR SDK, SQL, Compose, or advertising types.

### Confirmed input

`MatchDraft` contains:

- `base: ConfirmedBase` — `Known(BaseCode)`, `Unknown(rawText)`, or `Missing`;
- `voltage: VoltageMarking?` — single nominal value or inclusive range in volts;
- `frequency: FrequencyMarking?`;
- `sourceRatedPower: Watts?`;
- `printedEquivalentPower: Watts?`;
- `luminousFlux: Lumens?`;
- `colorTemperature: Kelvin?`;
- `shape: ShapeCode?`;
- `dimmability: Dimmability` — `Yes`, `No`, or `Unknown`;
- `fixtureMaximumPower: Watts?` — manual source only;
- `reviewedFields: Set<FieldKey>`;
- `rejectedObservations: Set<FieldKey>`.

Value objects reject non-finite, zero, negative, reversed, or physically nonsensical ranges. UI parsing may temporarily hold incomplete strings; domain construction occurs only after validation.

### Observation

`ObservedField` contains `fieldKey`, raw recognized text, parsed candidate, optional SDK confidence, and bounding geometry. It exists only in the in-memory capture flow. SDK confidence may order review suggestions but never affects compatibility.

### Saved result

`SavedMatch` has a generated stable ID, optional trimmed display name, creation instant, immutable confirmed input snapshot, immutable assessment snapshot, catalog/ruleset version, and snapshot schema version. It contains no image, crop, bounding box, or full OCR transcript.

## Invariants

1. Every OCR-produced `FieldKey` must be present in either `reviewedFields` or `rejectedObservations` before assessment. (`REQ-005`, `AC-005`)
2. `Compatible` requires a known catalog base and a confirmed voltage. (`REQ-006`, `AC-006`)
3. `Unknown(rawText)` can never produce `Compatible`; the raw text is retained only as an explanation and optional saved value. (`REQ-007`, `AC-007`)
4. A voltage that is unsupported, internally contradictory, or outside the target supply family can never produce `Compatible`. A confirmed frequency other than exactly 50 Hz also can never produce `Compatible`; an absent frequency remains optional. (`REQ-009`, `AC-009`)
5. `fixtureMaximumPower` can be created only from a dedicated manual input origin. It cannot be derived from `sourceRatedPower`, OCR, base defaults, or catalog typicals. (`REQ-010`, `AC-010`)
6. A numeric brightness target is derived from confirmed lumens. Source wattage alone does not imply lumens unless a reviewed rule explicitly has every required technology input; MVP ships no such fallback rule by default. (`REQ-011`, `AC-011`)
7. A printed “W equivalent” is stored separately from actual rated watts and is never used as a fixture limit.
8. Ads, network state, locale, theme, and crash reporting never change an assessment. (`REQ-025`, `AC-025`)
9. Saved assessments are immutable historical snapshots. Catalog updates shipped in later app versions do not silently recalculate them.
10. Cancellation is never mapped to an OCR, database, or unknown domain error.

## Assessment algorithm

`CompatibilityEngine.assess(ConfirmedMatchInput, CatalogSnapshot): Assessment` is pure and deterministic.

Evaluation order:

1. Reject construction errors as `NeedClarification(InvalidField...)`.
2. If any OCR observation is unhandled, return `NeedClarification(UnreviewedField...)`.
3. If base is missing, return `NeedClarification(MissingBase)`.
4. If base is unknown, return `NeedClarification(UnknownBase(rawText))`.
5. If base is not enabled in the signed catalog version, return `NeedClarification(UnsupportedBase)`.
6. If voltage is missing, return `NeedClarification(MissingVoltage)`.
7. If voltage is malformed or conflicts with another confirmed voltage token, return `PotentialConflict(ContradictoryVoltage)`.
8. Classify the marking against the catalog's reviewed voltage family rules. Values clearly belonging to another family, including 100–127 V mains or explicit low-voltage values, return `PotentialConflict(OutsideElectricalScope)`. Ambiguous values return `NeedClarification`, not a best guess.
9. If frequency is confirmed and is not exactly the catalog target of 50 Hz, return `PotentialConflict(OutsideFrequencyScope)`. An absent frequency does not block assessment.
10. If a manually confirmed fixture maximum is lower than a confirmed source rated power, add a blocking `FixturePowerConflict`; do not claim the existing installation is safe.
11. Otherwise create `CompatibleProfile` with the exact base, required `220–240 V / 50 Hz` shopping scope, and only the confirmed optional targets described below.

The positive UI phrase is “Compatible profile / Совместимый профиль”, followed immediately by “This does not certify the fixture / Это не подтверждает безопасность светильника.”

## Replacement profile rules

- **Base:** exact canonical base code; no “close enough” base substitution.
- **Voltage/frequency:** a replacement label must be suitable for the target 220–240 V / 50 Hz supply family. The app does not recommend transformers or adapters.
- **Power:** if fixture maximum is known, show “do not exceed X W.” If it is unknown, show “check the fixture label”; never invent a number.
- **Source rated power:** show as reference only, explicitly distinguished from the fixture limit.
- **Printed equivalent power:** repeat only when confirmed, prefixed “printed equivalent”; never convert it into actual consumption.
- **Brightness:** when confirmed lumens exist, recommend matching the printed lumen value and present a shopping tolerance of ±10% as preference, not compatibility. Without lumens, state that brightness is unresolved.
- **Color temperature:** repeat a confirmed Kelvin value as a preference; otherwise show optional common-language choices without selecting one.
- **Shape:** repeat a confirmed shape and warn that dimensions/enclosure clearance remain unchecked.
- **Dimmability:** when confirmed `Yes`, require a dimmable replacement; when `No` or `Unknown`, ask the user to verify fixture/dimmer requirements.

## Catalog contract

`CatalogSnapshot` is immutable and versioned. It supplies:

- canonical base codes and normalized aliases;
- localized base names and identification hints;
- base-family grouping and original diagram asset IDs;
- reviewed voltage-family classifications and conflict reason codes;
- catalog version, ruleset version, publication date, schema version, source-manifest version, and content hash.

Typical use, popularity, dimensions, or common voltage are reference hints only. They cannot satisfy a hard check. The catalog must not encode an undocumented compatibility substitution.

## Domain interfaces

- `CompatibilityEngine` — pure assessment and profile generation.
- `CatalogProvider` — returns the bundled immutable snapshot and searchable base entries.
- `SavedMatchRepository` — observe, get, save immutable snapshot, delete one, and clear all.
- `SettingsRepository` — observe/set locale override and theme override; expose ad-frequency state separately.
- `TextRecognitionService` — platform-neutral suspend contract that returns observations or typed failure; owned by platform implementations.
- `ImageSourceService` — platform-neutral camera and system-picker results; it yields an ephemeral image handle, not a persisted path.
- `CrashReporter` — accepts throwable plus a fixed allowlist of technical enums/build metadata; rejects arbitrary strings.
- `AdGateway` — lifecycle-aware load/show callbacks that cannot enter the compatibility engine.

## Typed outcomes and failures

`Assessment`:

- `Compatible(profile, explanations, advisoryChecks)`
- `NeedClarification(reasons, retainedConfirmedInput)`
- `PotentialConflict(reasons, retainedConfirmedInput)`

Recoverable boundary failures:

- image: `PermissionDenied`, `CameraUnavailable`, `CaptureCancelled`, `PickerCancelled`, `UnreadableImage`;
- OCR: `NoTextFound`, `UnsupportedImage`, `RecognitionFailed(cause)`;
- persistence: `ReadFailed(cause)`, `WriteFailed(cause)`;
- ads: `Unavailable`, `LoadFailed(cause)`, `ShowFailed(cause)`; always non-blocking;
- crash reporting: silent-to-user delivery failure, logged locally only in debug.

Components map these to localized presentation and recovery actions. Causes are preserved for technical diagnostics only when they contain no user content.

## Cross-feature outputs

- Match emits `OpenResult(assessment)` or `ExitDraft`.
- Result emits `SaveRequested`, `StartAnotherMatch`, or `ExitResult(outcome, completedMatchOrdinal)`.
- History emits `OpenSavedMatch(id)`.
- Reference emits `UseBase(baseCode)`, which starts manual Match with that base prefilled and visibly marked as user-selected.
- Settings emits `LanguageChanged`, `ThemeChanged`, or `LocalDataCleared`.

Parent navigation interprets outputs. No feature directly manipulates another feature's navigation state.
