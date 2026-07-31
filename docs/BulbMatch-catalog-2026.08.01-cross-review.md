# Independent cross-review: BulbMatch production catalog candidate `2026.08.01`

**Verdict: `READY_FOR_SERGEY_REVIEW`**

This is advisory only. It is **not** human approval, does not enable entries, does not write approval metadata, and does not authorize a release tag or production sign-off.

**Review date:** 2026-07-31

**Repository state:** Attached BulbMatch archive (candidate bundle `spec/catalog/releases/2026.08.01`)

**Canonical candidate hash (as stated in tree):** `7d83a43a4aa6e6da6eb90e038a22d80b8ea6142f2a10795035e36b864b35470a`

---

## Compatibility decision order (reconstructed)

From `CompatibilityEngine.assess` + `CatalogSnapshot.classify` + `BundledCatalogRules` + production loader:

1. **Unhandled OCR fields** → `NeedClarification(UnreviewedField)` (never `Compatible`).
2. **Base**
   - `Missing` → `NeedClarification(MissingBase)`
   - `Unknown(raw)` → `NeedClarification(UnknownBase)` (no substitution)
   - `Known(code)` not in `catalog.enabledBaseCodes` → `NeedClarification(UnsupportedBase)`
3. **Voltage evidence**
   - `Missing` → `NeedClarification(MissingVoltage)`
   - `Contradictory` → `PotentialConflict(ContradictoryVoltage)`
   - `Marking` → `catalog.classify(marking)`:
     - must be **fully contained** by **exactly one** `VoltageFamilyRule`
     - `OutsideScope` (100–127 or 1–48) → `PotentialConflict(OutsideElectricalScope)`
     - 0 or 2+ matches / uncovered range → `Ambiguous` → `NeedClarification(AmbiguousVoltage)`
     - single `InScope` (220–240 inclusive) → continue
4. **Fixture power** (manual only): if both source watts and fixture max present and source > max → `PotentialConflict(FixturePowerConflict)`
5. Else → **`Compatible`** with exact base, **shopping** target `220–240 V / 50 Hz` from catalog (not inferred from base), optional preferences, and mandatory advisory checks (`ThisDoesNotCertifyFixture`, switch-off, dimensions/enclosure, dimmer/brightness as applicable).

**False-positive path search:** No path was found that yields `Compatible` without (a) an **enabled** known base and (b) a **user-confirmed** voltage fully inside 220–240. Source watts, lumens, equivalent watts, dimmability, OCR decisions, and typical-use wording do not bypass base/voltage gates.

**Runtime gate:** Android/iOS roots load with `CatalogValidationMode.Production` and `BundledCatalogRules.ruleset`. Pending release metadata → `ProductionApprovalRequired` → catalog **Invalid** → assessment/reference stay unavailable until Sergey V. approval. That matches the intended pre-sign-off posture.

---

## Findings (by severity)

### High

*None that create a false-positive `Compatible` or redistribute prohibited IEC content.*

### Medium (process / residual product risk — not blockers for human review)

1. **User-confirmed frequency is not a hard assessment input**
   - **Where:** `CompatibilityEngine` only stamps `catalog.targetFrequency` (50 Hz); `ConfirmedMatchInput` has no frequency conflict branch.
   - **AppSpec:** domain shopping profile is “220–240 V / 50 Hz”; product excludes other supply families as *recommendation targets*.
   - **Scenario:** User confirms base E27 + 230 V on a lamp also marked 60 Hz → still `Compatible` with requiredFrequency 50 Hz.
   - **REQ:** soft alignment with electrical-scope wording; not a clear `REQ-009` violation because out-of-scope is defined on **voltage families**.
   - **Remediation (optional, for Sergey):** Decide whether confirmed 60 Hz / dual 50–60 Hz should be advisory-only (current) or a clarification/conflict rule with fixtures.

2. **Automated checks could not be executed in this review environment**
   - No network → Gradle wrapper cannot download the distribution; `./gradlew` fails before tests.
   - **Remediation:** Re-run focused tasks on the project host (see Commands). Do not treat green CI elsewhere as this review’s execution.

### Low / informational

3. **All six entries and all 37 fixtures remain `PENDING_HUMAN_SIGNOFF` / disabled** — intentional; production loader correctly rejects until `APPROVED` + `enabledForAssessment: true`.

4. **No `spec/catalog/releases/2026.08.01/approval/catalog-signoff.json`** — expected pre-approval; must appear only after content commit + final hash.

5. **Source records have empty `contentHash` / `reviewedAt`** — expected until post-approval hash step.

6. **Latin vs Cyrillic aliases are distinct after normalize** (e.g. `E27` vs `Е27`) — correct; avoids false alias collision. Search fixtures cover RU lookalikes.

7. **Diagram `else` → generic pin** for any non-six code — safe for deferred designations; included six have dedicated drawings.

---

## Coverage table (designations)

| Code | Status | Justification in bundle | Coverage gap if deferred? |
|------|--------|-------------------------|---------------------------|
| E27 | Include (pending) | IEC designation + original wording; common 220–240 V product family | Core screw — required |
| E14 | Include (pending) | Same family, distinct narrow shell | Core small screw — required |
| B22d | Include (pending) | IEC bayonet; market examples 220–240 V | Regional bayonet — justified |
| GU10 | Include (pending) | IEC twist-lock; voltage-neutral entry | Common mains reflector — justified |
| G9 | Include (pending) | IEC loop-pin; voltage-neutral | Common capsule — justified |
| R7s | Include (pending) | IEC double-ended; length called out in hint, not as compatibility fact | Linear — justified with length caveat |
| G4 | Deferred | Reviewed product-family evidence often 12 V | Acceptable for first 220–240 V shopping scope |
| GU5.3 | Deferred | Same LV product-family concern | Acceptable |
| G13 | Deferred | Control-gear / retrofit not modelled | Acceptable; avoids false “fit” |
| 2G11 | Deferred | Control-gear / dual-mode complexity | Acceptable |
| GX53 | Deferred | Not in initial diagram/candidate set | Acceptable; unsupported → `UnsupportedBase` |

Deferral does **not** leave a material *safety* gap: deferred codes must not substitute. Coverage gap is **product breadth**, not false-positive risk.

---

## Rule / fixture traceability

| Rule code | Disposition / role | Example positive fixtures | Example negative fixtures |
|-----------|--------------------|---------------------------|---------------------------|
| CATALOG_KNOWN_BASE_ENABLED | Exact enabled base | BASE-E27-230 … BASE-R7S-230 | BASE-MISSING, BASE-UNSUPPORTED-GX53 |
| BASE_NO_SUBSTITUTION | No nearest-base | BASE-E27-230 | BASE-UNSUPPORTED-GX53, BASE-UNKNOWN |
| VOLTAGE_TARGET_220_240 | InScope 220–240 | VOLTAGE-220-240, VOLTAGE-230 | VOLTAGE-PARTIAL-200-240, VOLTAGE-250 |
| VOLTAGE_OTHER_MAINS_100_127 | OutsideScope | — | VOLTAGE-110-120 |
| VOLTAGE_LOW_1_48 | OutsideScope | — | VOLTAGE-12, VOLTAGE-24 |
| VOLTAGE_UNCOVERED_AMBIGUOUS | Ambiguous | — | VOLTAGE-PARTIAL-200-240, VOLTAGE-250 |
| VOLTAGE_CONTRADICTORY | Conflict | — | VOLTAGE-CONTRADICTORY-120-230 |
| FIXTURE_MAXIMUM_MANUAL_ONLY | Manual limit only | POWER-MANUAL-FIXTURE-LIMIT | POWER-MANUAL-LIMIT-BELOW-SOURCE |
| SOURCE_WATTS_NOT_FIXTURE_MAXIMUM | Source ≠ fixture max | POWER-SOURCE-WITHOUT-FIXTURE-LIMIT | — |
| LUMENS_SEPARATE_FROM_EQUIVALENT_WATTS | Preferences only | BRIGHTNESS-*, POWER-EQUIVALENT-SEPARATE | — |
| FIXTURE_SAFETY_DISCLAIMER | Advisories on Compatible | BASE-E27-230, DIMMABLE-* | — |

Fixture suite: **37** fixtures (31 ASSESSMENT, 6 ALIAS_SEARCH); outcomes mix COMPATIBLE / NEED_CLARIFICATION / POTENTIAL_CONFLICT / MATCH. All still `PENDING_HUMAN_SIGNOFF`. Suite version and catalog/ruleset versions align on `2026.08.01`.

Boundary mental checks vs `fullyContains`:

- 220, 230, 240 → InScope
- 219, 241, 250, 200–240, 220–250 → Ambiguous
- 100–127, 110–120 → OutsideScope
- 1–48, 12, 24 → OutsideScope
- 49–99, 128–219 → Ambiguous

---

## Source and licensing audit

| Record | Canonical | Primary | Secondary | Redistribution claim | Review state |
|--------|-----------|---------|-----------|----------------------|--------------|
| SRC-CAT-E27 | E27 | IEC 60061 preview index | LEDVANCE model (220–240 V example) | Identifier + original prose only | PENDING |
| SRC-CAT-E14 | E14 | IEC 60061 | LEDVANCE model | Same | PENDING |
| SRC-CAT-B22D | B22d | IEC 60061 | LEDVANCE model | Same | PENDING |
| SRC-CAT-GU10 | GU10 | IEC 60061 | LEDVANCE model | Same | PENDING |
| SRC-CAT-G9 | G9 | IEC 60061 | LEDVANCE model | Same | PENDING |
| SRC-CAT-R7S | R7s | IEC 60061 | LEDVANCE model | Same; no lamp lengths shipped | PENDING |

- No IEC sheets, gauges, dimensions, or manufacturer copy/images appear in shipping JSON or Compose diagrams (conceptual line art + original hints).
- Manufacturer pages are documented as **model-level** evidence, not type-wide voltage proof — consistent with voltage-neutral entries.
- Authoritative EU/IEC entry points are cited in reports; this review did not re-fetch external URLs (sandbox has no reliable outbound package/network for Gradle; content review is from the frozen tree).

EN/RU names and hints: identification-focused; require confirming the marking; R7s explicitly separates **length**. No wording that a base “implies” 230 V or “fits any fixture.”

---

## Hash, JSON, and release tooling

- Strict `Json { ignoreUnknownKeys = false; isLenient = false }`.
- Canonical hash over `UnsignedCatalogPayload` (excludes `contentHash`, includes release + rules + entries). Stated candidate hash:
  `7d83a43a4aa6e6da6eb90e038a22d80b8ea6142f2a10795035e36b864b35470a`
  (not recomputed here without running project hash tooling).
- `reviewedRuleCodes` lock matches `BundledCatalogRules` (11 codes).
- Duplicate normalized code/alias detection present.
- Single shipping resource: `catalog/bulbmatch-catalog-production.json`; Android `BUNDLED_CATALOG_RESOURCE_PATH` and iOS `pathForResource("bulbmatch-catalog-production")` aligned; shared `BundledCatalogRules`.
- Content vs sign-off commit separation described in docs; sign-off file not present (correct for pending).

---

## Commands and exit codes

| Command | Exit | Notes |
|---------|------|--------|
| Extract / static inspection of catalog, rules, engine, fixtures, sources, diagrams | N/A | Completed |
| `./gradlew :shared:domain:testAndroidHostTest` (and related) | **Not run** | Wrapper could not fetch Gradle distribution (no network). CRLF on `gradlew` was fixed locally only for the attempt. |
| macOS / iOS / physical device / signing / archive / a11y | **Not run** | Not available in this environment |

Host should re-run at minimum:

```text
./gradlew :shared:domain:testAndroidHostTest :shared:data:testAndroidHostTest
# plus catalog tools / safety fixture execution as wired in shared/data
./gradlew :shared:data:validateProductionCatalog   # expected non-zero until approval
```

---

## Checks not run (and why)

- Full Gradle JVM/Android host tests — no Gradle distro download
- `validateProductionCatalog` — expected red until approval; not executed
- iOS simulator / Xcode / CocoaPods — not a macOS host
- Physical device, Airplane Mode OCR, ads, Crashlytics, signing, IPA — out of scope for catalog cross-review and unavailable here
- Live re-fetch of IEC/EUR-Lex pages — not required for tree integrity; licensing claims reviewed from frozen source records

---

## Residual risks & questions for Sergey V.

1. Confirm the **six-entry** set and explicit deferrals (especially R7s length-only advisory, G9/GU10 voltage neutrality).
2. Confirm exact voltage bands **220–240 / 100–127 / 1–48** and that gaps stay **Ambiguous** (not OutsideScope).
3. Confirm whether **frequency markings** (50 vs 60 vs 50/60) need a hard rule or stay profile-stamped + advisory.
4. Approve or revise every EN/RU name, alias (including Cyrillic lookalikes), hint, and diagram alternative text.
5. Confirm all **37** fixture expected outcomes before any `APPROVED` metadata.
6. After approval: content commit → recompute canonical hash → only then `catalog-signoff.json` + sign-off commit/tag — do not change content after `APPROVED`.

---

## Statement

This verdict is **advisory**. Only **Sergey V.** may approve the exact frozen bundle, enable entries, set timestamps, create `catalog-signoff.json`, commit, or tag. Automated checks and this review cannot create human approval.
