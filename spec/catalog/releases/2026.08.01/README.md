# BulbMatch production catalog candidate 2026.08.01

Status: `APPROVED_CONTENT_AWAITING_FINAL_SIGNOFF`

This directory is the frozen-evidence workspace for the first production catalog. Its exact
content was approved by Sergey V. at `2026-07-31T07:21:40Z`. The frozen content commit and
canonical/hash evidence still require Sergey's separate final sign-off before a sign-off record
or immutable tag is created.

## Reserved versions

- catalogVersion: `2026.08.01`
- rulesetVersion: `2026.08.01`
- safetyFixtureSuiteVersion: `2026.08.01`
- sourceManifestVersion: `2`
- publishedAt: `2026-08-01`
- electrical scope: `220–240 V / 50 Hz`

## Candidate inclusion

The pending candidate contains six exact cap designations:

- `E27`
- `E14`
- `B22d`
- `GU10`
- `G9`
- `R7s`

Every entry is voltage-neutral. A cap code never proves voltage or frequency. A positive
assessment additionally requires a user-confirmed marking fully covered by the reviewed
220–240 V rule. Frequency remains optional, but a confirmed value must be exactly 50 Hz;
any other confirmed value produces `PotentialConflict(OutsideFrequencyScope)`. The shopping
profile always retains the 220–240 V / 50 Hz target.

## Deferred candidate designations

- `G4` and `GU5.3`: current official product-family evidence reviewed for this release is
  explicitly 12 V; they are outside the recommendation target.
- `G13`: fluorescent and retrofit products can depend on control gear or rewiring; this first
  consumer release does not model those installation distinctions.
- `2G11`: current products may be control-gear-specific or dual-mode; safe inclusion needs
  additional compatibility wording and fixtures.
- `GX53`: current 220–240 V products exist, but the designation was not in the approved initial
  candidate/diagram inventory and is deferred to a separately reviewed catalog version.

Deferred entries remain unsupported and must return `NeedClarification(UnsupportedBase)` rather
than a substitute.

## Approval boundary

The six entries, rules, sources, and fixture outcomes carry the recorded content approval.
AI-generated research and checks remain advisory. Only Sergey V. may authorize the separate
`catalog-signoff.json` after reviewing the frozen content commit and final hashes.
