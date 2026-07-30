# BulbMatch data, platform, and integration contract

## Data ownership

Core data is local. The only network SDKs are Yandex Mobile Ads and Firebase Crashlytics; neither participates in matching. There is no product API, account, catalog download, remote database, or cloud backup in MVP.

## Bundled catalog

Ship a read-only UTF-8 catalog resource with:

- `schemaVersion`;
- `catalogVersion`;
- `rulesetVersion`;
- `publishedAt`;
- `sourceManifestVersion`;
- SHA-256 content hash;
- localized common-base entries, aliases, diagram IDs, and reviewed rule codes.

The app validates schema and hash at startup. A failure disables assessment and reference browsing with a local fatal-data error and manual safety message; it must not fall back to an unversioned default. Catalog changes arrive only in an app release.

The initial catalog is deliberately common, not exhaustive. Candidate base families include E27, E14, B22d, GU10, G9, G4, GU5.3, G13, R7s, and 2G11, but inclusion in production requires documented sources, original diagrams, and reviewer sign-off. Presence in this candidate list is not approval.

## Local database

Use SQLDelight for saved history. Generated rows remain behind repository mappers.

### `saved_match`

| Column | Contract |
|---|---|
| `id` | Stable generated string primary key |
| `created_at_epoch_ms` | UTC instant, indexed descending |
| `display_name` | Nullable, trimmed, maximum 80 Unicode code points |
| `status_code` | Stable enum code |
| `base_code` | Nullable known canonical code |
| `raw_base_text` | Nullable confirmed unknown-base text, maximum 80 code points |
| `voltage_min_v` / `voltage_max_v` | Nullable validated decimal values |
| `catalog_version` | Required |
| `ruleset_version` | Required |
| `snapshot_schema_version` | Required integer |
| `snapshot_json` | Required complete immutable confirmed-input and assessment snapshot |

The indexed summary columns make the history list cheap; `snapshot_json` is the source of truth for reopening. JSON decoding is versioned, rejects malformed required fields, and supports explicit forward migrations. A bad record is isolated and shown as unavailable; it never crashes or contaminates other rows.

Database operations:

- observe summaries ordered by `created_at_epoch_ms DESC, id DESC`;
- get one snapshot by ID;
- insert one immutable result transactionally;
- delete one by ID;
- clear all in a transaction.

Duplicate saves create separate records because they represent explicit user actions. No photo, thumbnail, bounding box, OCR confidence, rejected OCR text, or full recognized text is stored.

## Settings

Use Multiplatform Settings with centralized keys and typed defaults:

| Key | Type/default | Notes |
|---|---|---|
| `locale_override_v1` | `SYSTEM` | `SYSTEM`, `EN`, or `RU`; malformed falls back to `SYSTEM` |
| `theme_override_v1` | `SYSTEM` | `SYSTEM`, `LIGHT`, or `DARK` |
| `completed_compatible_matches_v1` | `0` | Local ad eligibility counter; not analytics |
| `last_interstitial_epoch_ms_v1` | absent | Local frequency cap only |
| `compatible_matches_since_interstitial_v1` | `0` | Reset only after a confirmed impression |

Clear local data deletes saved matches and resets ad-frequency counters. It keeps language and theme so a destructive privacy action does not make the UI unreadable. The confirmation copy states this behavior.

## Ephemeral capture data

An image handle, decoded pixels, OCR geometry, observations, and unfinished draft live only in memory. They are released on finish, cancel, root replacement, or process death. Do not copy picker content into app storage, cache, logs, saved-state serialization, crash keys, or screenshots.

Within the same live process, navigation back from review to image preview may retain the handle. After process recreation, route to Match home with the neutral message “Unfinished scan was not saved / Незавершённое сканирование не сохранено.”

## Camera and system picker

- Camera access is requested only after the Camera action.
- Permission states are `Unknown`, `Checking`, `Granted`, `DeniedCanAsk`, `DeniedOpenSettings`, and `Unavailable`.
- Recheck on foreground resume after the user opens system Settings.
- Android image import uses the system Photo Picker contract with platform fallback where the OS provides it; do not request broad media permission.
- iOS image import uses the system PHPicker/PhotosPicker presentation and does not request library-wide access.
- A unavailable camera still leaves photo picker and manual entry available.
- Capture metadata such as EXIF location is neither read nor stored.

## OCR

Use a common `TextRecognitionService` contract with platform implementations based on ML Kit Text Recognition v2.

- Android bundles the current supported on-device model artifact so recognition does not require a first-run download.
- iOS statically links the selected script SDK.
- The implementation must verify current ML Kit target requirements and versions at build time rather than pinning this AppSpec's research snapshot.
- The supported recognition target is electrical/numeric and Latin-script label content commonly printed on lamps. Russian UI support does not imply reliable Cyrillic OCR. Unsupported or missed Cyrillic text must remain editable through manual input.
- Parsing recognizes units and separators conservatively, keeps competing candidates visible, and never invents a field from layout alone.
- OCR yields observations only; confirmation and assessment happen in common domain logic.

## Yandex Mobile Ads

Use the current Yandex Mobile Ads Compose Multiplatform library and native iOS SDK with compatible versions. As of AppSpec research on 2026-07-28, Yandex documented Compose Multiplatform plugin `8.2.0`, Android/iOS integration, iOS 13+, and privacy setup before initialization. Re-verify official documentation during implementation.

Network advertising behavior:

- Configure `setUserConsent(false)` before SDK initialization on every launch.
- Disable location signals and never pass age, gender, interests, search terms, lamp values, result status, or other targeting parameters.
- Remove `com.google.android.gms.permission.AD_ID` from the merged Android manifest.
- Do not request App Tracking Transparency on iOS.
- Initialize after first content is renderable; initialization failure is non-blocking.
- The eight approved production ad unit values are recorded in `app-spec.json` and mapped to platform build configuration. They are public placement identifiers, not sensitive credentials; environment/build overrides remain allowed for test variants.
- Debug device builds use provider test IDs. Preview/screenshot/unit/UI-test processes use a fake gateway and do not initialize the network SDK.
- A release task validates that every production key is nonblank and is not a known test ID.
- Banner failure collapses the container. Interstitial load/show failure completes navigation immediately.
- Only a successful impression resets the local frequency counters.

## Firebase Crashlytics

Integrate the native Firebase Crashlytics SDK in both platform hosts behind a minimal common `CrashReporter`. Do not add Firebase Analytics.

Allowed report context:

- app version/build;
- platform/OS/device information automatically supplied by the SDK;
- fixed technical enum such as `screen_code`, `operation_code`, `catalog_version`, and `ruleset_version`;
- sanitized non-fatal exception class and stack trace.

Forbidden context:

- image bytes/path/URI;
- OCR text, raw base text, confirmed field values, result name, database contents;
- user-entered strings, ad identifiers, stable generated user ID;
- navigation breadcrumbs or custom logs containing product data.

Debug, preview, screenshot-test, and unit-test builds do not send reports. Release verification includes one controlled test crash per platform and inspection that the report contains only the allowlist.

## Data retention and deletion

Saved results persist until the user deletes them, clears local data, uninstalls the app, or the OS removes app storage. Ephemeral capture data has session-only retention. Ad frequency settings persist locally and are reset by Clear local data. Crash and ad provider retention is governed by their disclosed privacy terms and must be linked from the production privacy policy at `https://sedsoftware.com/apps/bulbmatch/policy.html`.

## Migrations and failure policy

- Start at database schema 1 and snapshot schema 1.
- Every released schema receives a forward migration and a real-schema migration test.
- Never solve a migration failure by deleting the database silently.
- Preserve a failing record where possible, show a localized unavailable state, and let the user delete it.
- Settings codecs tolerate missing and malformed values by applying documented defaults.
- Database work runs off the UI thread and cancellation is rethrown.

## Source registry

The exact research and redistribution policy is in `assets/source-licensing-manifest.md`. Catalog data may enter the app only through a reviewed source record with URL/document identifier, access date, extracted facts, transformation, license/usage note, reviewer, and content hash.

The final production reviewer is Sergey V. Automated and multi-model AI checks may propose fixtures, identify inconsistencies, and challenge decisions, but cannot mark a catalog/ruleset release approved. Human sign-off records the reviewed versions, date, and decision.
