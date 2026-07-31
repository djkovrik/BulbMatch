# AI-assisted catalog review — 2026.08.01

Status: `ADVISORY FINDINGS DISPOSITIONED / CONTENT APPROVED BY SERGEY V.`

## Scope reviewed

- AppSpec requirements and acceptance criteria: `REQ-006`, `REQ-007`,
  `REQ-009`, `REQ-010`, `REQ-011`, `REQ-014`, `REQ-015`, `REQ-016`,
  `REQ-023`, `REQ-025`, and `AC-014`, `AC-023`.
- Production approval workflow in
  `docs/PRODUCTION-CATALOG-APPROVAL-GUIDE.md`.
- Exact candidate entries, EN/RU aliases and hints, source records, runtime
  rules, safety fixtures, canonical hash contract, and offline resource wiring.
- SCREEN-009/010 reference behavior and diagram accessibility.

## Source assessment

Primary product-domain evidence remains the official IEC 60061 preview/index.
The EU regulatory sources establish the target market context and terminology:

- IEC 60061 preview/index:
  https://webstore.iec.ch/en/iec_catalog/product/preview/?id=L3B1Yi9wZGYvcHJldmlldy9pbmZvX2llYzYwMDYxe2VkMS4wfWIucGRm
- Commission Regulation (EU) 2019/2020:
  https://eur-lex.europa.eu/legal-content/EN/TXT/?uri=CELEX%3A32019R2020
- Commission Delegated Regulation (EU) 2019/2015:
  https://eur-lex.europa.eu/legal-content/EN/ALL/?uri=CELEX%3A32019R2015
- European Commission light-sources page:
  https://energy-efficient-products.ec.europa.eu/product-list/light-sources_en

Official LEDVANCE product pages were added as `SRC-005` supplemental,
model-level evidence that the selected cap types occur in current 220–240 V
products. They are not used to infer that every lamp with the same cap has the
same voltage, frequency, power, dimensions, control-gear requirements, or
compatibility. No manufacturer image or product copy is shipped.

## Inclusion recommendation

Recommend the following exact designations for the first production candidate:

- `E27`
- `E14`
- `B22d`
- `GU10`
- `G9`
- `R7s`

The set covers common screw, bayonet, twist-lock, loop-pin, and double-ended
families while remaining small enough for exact source, wording, diagram, and
safety-fixture review.

Recommend deferring:

- `G4` and `GU5.3`: the currently reviewed product-family examples are 12 V;
- `G13` and `2G11`: control-gear and retrofit distinctions are not modelled;
- `GX53`: plausible later addition, but outside the initial candidate and
  diagram inventory.

Deferred codes remain unsupported. No similar base may be substituted.

## Safety findings

1. A catalog entry is voltage-neutral. The code or diagram never proves
   voltage or frequency.
2. `Compatible` requires both an exact enabled canonical base and a separately
   user-confirmed voltage covered by the reviewed 220–240 V rule.
3. Confirmed `100–127 V` and `1–48 V` are outside the product scope and must not
   produce a positive result.
4. Missing, contradictory, unsupported, or uncovered voltage facts remain
   clarification/conflict outcomes.
5. Frequency remains optional, but confirmed 50 Hz is the only accepted value;
   any other confirmed frequency produces `PotentialConflict`.
6. Source-lamp wattage never creates fixture maximum wattage.
7. Typical use, lumens, equivalent watts, dimmability, and visual similarity
   never weaken hard checks.
8. The candidate stays entirely bundled and offline; no remote catalog or
   analytics behavior was added.

## Independent cross-review disposition

The advisory review in
`docs/BulbMatch-catalog-2026.08.01-cross-review.md` found no high-severity
issue and judged the six-entry candidate ready for Sergey review. Its one
material product-risk finding was accepted: confirmed frequency is now a hard
scope input. The historical review remains unchanged and therefore refers to
the earlier 11-rule, 37-fixture hash; this revised request covers 12 rules,
39 fixtures, and the new canonical hash.

## UI evidence

The existing SCREEN-010 was rendered through Paparazzi and reviewed through
Lazyweb's `lazyweb-design` workflow:

https://www.lazyweb.com/report/lazyweb/0504d222-5c03-478a-801c-2fe37af35d22/?source=create

The applied, safety-compatible findings are:

- keep one large conceptual line diagram and a clear stroke hierarchy;
- distinguish the actual identifying contact form for every included family;
- add a visible caption that the diagram is for identification and not to
  scale;
- retain a precise screen-reader alternative and the separate printed-voltage
  warning.

The resulting six-diagram board and SCREEN-010 light/dark/200% previews were
rendered without updating goldens. Visual correctness remains part of Sergey
V.'s manual approval.

## Recorded human decision and remaining sign-off

Sergey V. approved:

- the six-entry inclusion set and every deferred designation;
- every EN/RU name, alias, hint, and original diagram;
- the exact `220–240`, `100–127`, and `1–48` voltage boundaries;
- the optional-frequency rule: exactly `50 Hz` passes when confirmed and every
  other confirmed value conflicts;
- all rule codes and all expected safety-fixture outcomes;
- the permitted source transformations and redistribution decisions.

Sergey V. approved all seven review groups at
`2026-07-31T07:21:40Z` and authorized preparation of the content commit. AI
assistance did not create that decision and cannot authorize the separate final
sign-off record. The remaining human decision is whether the frozen content
commit and the final canonical/raw hashes receive that sign-off.
