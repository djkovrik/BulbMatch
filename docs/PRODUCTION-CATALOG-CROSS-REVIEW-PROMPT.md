# Prompt for independent cross-review of the BulbMatch production catalog

Copy the prompt below into another coding agent with read access to the
repository. Ask each reviewer to work independently and not reuse another
agent's conclusions.

---

You are performing an independent, safety-critical cross-review of BulbMatch's
first production lamp-base catalog. Do not edit files, approve the catalog,
create commits/tags, or weaken any gate. Review the exact repository state I
provide (working tree or full content-commit SHA) and return evidence-backed
findings only.

Repository scope:

- AppSpec: `spec/app-spec`
- product guardrails: `AGENTS.md`
- approval procedure: `docs/PRODUCTION-CATALOG-APPROVAL-GUIDE.md`
- shipping catalog:
  `shared/data/src/commonMain/resources/catalog/bulbmatch-catalog-production.json`
- runtime rules:
  `shared/data/src/commonMain/kotlin/com/sedsoftware/bulbmatch/data/catalog/BundledCatalogRules.kt`
- loader/provider and release tooling: `shared/data`
- assessment engine: `shared/domain`
- Android/iOS resource wiring: `androidApp` and `shared/compose/src/iosMain`
- reference UI/diagrams: `shared/compose`
- frozen candidate bundle: `spec/catalog/releases/2026.08.01`

The product scope is 220–240 V / 50 Hz. A cap/base designation is always
voltage-neutral. `Compatible` is permitted only for an exact known enabled base
plus separately user-confirmed in-scope voltage. Frequency is optional, but a
confirmed value must be exactly 50 Hz; any other confirmed value must produce
`PotentialConflict`. Missing, unknown, contradictory, unsupported, or
out-of-scope facts must never produce a false positive. Fixture maximum wattage
is manual only. Photos, OCR transcripts, and drafts are ephemeral. Core
behavior is offline. Do not authorize sync, remote catalog updates, accounts,
analytics, or tracking.

Authoritative starting sources:

1. IEC 60061 preview/index:
   https://webstore.iec.ch/en/iec_catalog/product/preview/?id=L3B1Yi9wZGYvcHJldmlldy9pbmZvX2llYzYwMDYxe2VkMS4wfWIucGRm
2. Commission Regulation (EU) 2019/2020:
   https://eur-lex.europa.eu/legal-content/EN/TXT/?uri=CELEX%3A32019R2020
3. Commission Delegated Regulation (EU) 2019/2015:
   https://eur-lex.europa.eu/legal-content/EN/ALL/?uri=CELEX%3A32019R2015
4. European Commission light-sources page:
   https://energy-efficient-products.ec.europa.eu/product-list/light-sources_en

You may use additional current primary/official sources, but record URL,
publisher, version/date, access date, exact fact supported, and licensing or
redistribution implications. Treat manufacturer pages only as model-level
evidence. Never generalize one product's voltage, dimensions, power, or
control-gear requirements to an entire cap designation.

Review tasks:

1. Validate the AppSpec first and report errors and warnings separately.
2. Reconstruct the exact compatibility decision order from requirements,
   domain code, runtime rules, and fixtures. Identify any path to a
   false-positive `Compatible`.
3. Check whether `E27`, `E14`, `B22d`, `GU10`, `G9`, and `R7s` are justified
   for this first release and whether deferring `G4`, `GU5.3`, `G13`, `2G11`,
   and `GX53` leaves a material coverage gap.
4. Verify every canonical code, EN/RU name, alias, distinguishing hint,
   source-record locator, diagram distinction, and text alternative. Flag
   confusable Cyrillic/Latin aliases and misleading typical-use wording.
5. Review the exact voltage boundaries `220–240`, `100–127`, and `1–48`,
   uncovered ranges, contradictions, and the confirmed-frequency hard rule.
   Test voltage boundary values, ambiguous ranges, missing frequency, 50 Hz,
   and 60 Hz mentally and against fixtures.
6. Verify that source-lamp watts cannot become fixture maximum, and that
   lumens, equivalent watts, dimmability, OCR state, and typical use cannot
   bypass hard checks.
7. Check strict JSON decoding, canonical hash scope, approval metadata,
   ruleset-code lock, duplicate normalization, source-record resolution,
   one-shipping-resource rule, and content/sign-off commit separation.
8. Execute the focused automated checks through the checked-in Gradle wrapper
   using the project's Windows runner when on Windows. Record every command
   and exit code. Do not claim macOS, iOS, physical-device, signing, archive,
   or manual accessibility checks unless actually performed.
9. Inspect the complete diff against the stated baseline/content commit.
   Report unrelated changes and any post-review mutation risk.
10. Confirm that no copyrighted IEC sheets, dimensions, gauges, manufacturer
    copy, or images are redistributed; original prose and conceptual diagrams
    must remain distinguishable and safely licensed.

Required output:

- Verdict: `BLOCK`, `CHANGES_REQUIRED`, or `READY_FOR_SERGEY_REVIEW`.
- Findings ordered by severity, each with file and line, violated
  `REQ-NNN`/`AC-NNN`, concrete failure scenario, and minimal remediation.
- Coverage table for every included and deferred designation.
- Rule/fixture traceability table.
- Source and licensing audit table.
- Commands and exact exit codes.
- Checks not run and why.
- Residual risks and explicit questions for Sergey V.
- A statement that your verdict is advisory and is not human approval.

Do not write `APPROVED`, enable entries, fill reviewer timestamps, create
`catalog-signoff.json`, commit, or tag. Those actions are reserved for Sergey
V.'s explicit review of the exact frozen bundle and final hash.

---
