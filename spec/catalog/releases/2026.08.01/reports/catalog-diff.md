# Catalog diff — development candidate to 2026.08.01 production candidate

Status: `APPROVED CONTENT / AWAITING FINAL SIGN-OFF`

## Resource and runtime

- Shipping resource renamed from
  `bulbmatch-catalog-development.json` to
  `bulbmatch-catalog-production.json`.
- Android and iOS composition roots now load the production filename.
- Per-platform voltage rule construction was replaced by one shared
  `BundledCatalogRules` definition.
- Catalog loading now rejects a ruleset version or reviewed-code mismatch.
- Typed JVM release tools now update the canonical hash and validate the frozen
  release selected from the catalog version.

## Entry set

Retained for exact human review:

`E27`, `E14`, `B22d`, `GU10`, `G9`, `R7s`.

Removed from the first production payload:

`G4`, `GU5.3`, `G13`, `2G11`.

`GX53` was researched but not added.

No removed or deferred code is mapped to another base. All six retained entries
were approved by Sergey V. and are enabled in the content-approved production
catalog.

## Evidence and tests

- Added six versioned production source records.
- Added a reviewed-ruleset document and a strict versioned safety-fixture suite.
- Added supplemental official manufacturer evidence to AppSpec source manifest
  version `2`, limited to model-level market verification.
- Added strict unknown-field, duplicate-code/alias, approval metadata,
  ruleset-lock, and fixture-execution tests.
- Added a hard assessment rule and stable saved-snapshot reason code so a
  confirmed frequency other than 50 Hz cannot produce `Compatible`.
- No database, network, sync, account, analytics, advertising placement, or
  saved-snapshot schema change was introduced.

## Visual reference

SCREEN-010 keeps the existing hierarchy and explicit printed-voltage warning.
The candidate requires distinct original line diagrams for screw, bayonet,
GU10 twist-lock, G9 loop-pin, and R7s double-ended forms. No standardized IEC
dimensions, gauges, manufacturer artwork, or lamp-length claims are copied.

## Final diff gate

This report must be checked again against the frozen content commit before
final sign-off. Any content change after Sergey V.'s review invalidates the
approval and requires a new version, hash, review, and sign-off.
