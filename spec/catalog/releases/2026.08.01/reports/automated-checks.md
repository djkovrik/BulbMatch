# Automated checks — catalog 2026.08.01

Status: `CONTENT APPROVED / AWAITING FINAL SIGN-OFF`

This report records checks against the exact content approved by Sergey V. at
`2026-07-31T07:21:40Z`. It does not replace the separate final sign-off on the
frozen content commit and hashes.

## Completed checks

| Check | Result | Evidence |
|---|---|---|
| AppSpec validator | Exit `0` | `python D:\Sources\vibe-skills\vibe-developer\scripts\validate-app-spec.py D:\Sources\Android\BulbMatch\spec\app-spec` |
| Approved canonical hash update | Exit `0` | `build/logs/catalog-approved-content-hash.log`; SHA-256 `881d91ec1c8a3ebd494bac0dfa615e109c25e573548d325ec90d5ba98f32ce7b` |
| Android CI-equivalent checks, including the versioned 39-fixture suite | Exit `0` | `build/logs/catalog-approved-content-android.log` |
| Compose preview report render | Exit `0`; 43 tests, 0 failures, 0 errors | `build/logs/catalog-frequency-preview-tests.log` and `screenshot-tests/build/test-results/testDebugUnitTest/TEST-com.sedsoftware.bulbmatch.screenshottests.generated.GeneratedComposablePreviewPaparazziTest.xml` |
| Debug APK catalog comparison | Pass | Exactly one `catalog/bulbmatch-catalog-production.json`; 5,013 bytes; byte-equal to approved source; no development catalog |

The AppSpec validator reported two non-blocking warnings:

1. the legacy AppSpec has no separate `uiQuality` contract;
2. `capabilities.sync=false`, while sync-related words occur in descriptive text.

Neither warning authorizes synchronization, network catalog delivery, accounts,
analytics, or any expansion of the MVP.

The independent cross-review finding about confirmed frequency was adopted:
confirmed `50 Hz` passes, confirmed `60 Hz` returns
`PotentialConflict(OutsideFrequencyScope)`, and missing frequency remains
optional. The runtime ruleset now has 12 reviewed codes and the release suite
has 39 fixtures.

Fixture distribution: 20 `COMPATIBLE`, 7 `NEED_CLARIFICATION`,
6 `POTENTIAL_CONFLICT`, and 6 alias `MATCH` cases. There were no failures or
skips in the focused catalog/domain/data checks.

## Frozen hash evidence

- canonical catalog SHA-256:
  `881d91ec1c8a3ebd494bac0dfa615e109c25e573548d325ec90d5ba98f32ce7b`;
- reviewed `ruleset.md` raw SHA-256:
  `04f177b455001c36933f0a46d2ecc281658e0b5bf2d3d5c9fa90d0b90b10ef10`;
- runtime `BundledCatalogRules.kt` raw SHA-256:
  `61242f331bbebb36f84cbe8b2258081e818f0017834deb96baf60a8052fac569`;
- `safety-fixtures.json` raw SHA-256:
  `70d54ed133bc71484753cefd418a10b24db00a20ccee719bb8af929d32168f00`;
- debug APK SHA-256:
  `c499b524ac72ccd72fb0ac8efca7bd13decf7733713bfdbbbc7609e70e44ad8e`
  (69,209,131 bytes).

The content commit cannot contain its own SHA without a circular dependency.
Its full SHA will be recorded in the separate `catalog-signoff.json` after
Sergey V. approves the frozen commit and hashes.

## Checks intentionally pending

- `:shared:data:validateProductionCatalog` remains pending until
  `approval/catalog-signoff.json` is authorized and committed separately.
- The Android CI-equivalent task set passed for the approved content and will
  be rerun after the sign-off record is added.
- iOS Gradle, CocoaPods workspace, and Xcode checks require macOS and will be
  reported as not run on this Windows host.
- Physical-device, permission, offline OCR, advertising, Crashlytics, signing,
  archive, and accessibility checks remain manual release gates.
- No Paparazzi golden was recorded or updated before product and diagram review.

## Required interpretation

A green automated check proves only the tested invariant. It does not replace
Sergey V.'s review of sources, wording, diagram distinctions, voltage rules,
fixture outcomes, the exact final hash, or the content commit.
