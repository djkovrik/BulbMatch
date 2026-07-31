# Sergey V. review request — catalog 2026.08.01

Decision status: `APPROVED_FOR_CONTENT_COMMIT`

Recorded reviewer: `Sergey V.`

Recorded at: `2026-07-31T07:21:40Z`

Canonical candidate hash:

`881d91ec1c8a3ebd494bac0dfa615e109c25e573548d325ec90d5ba98f32ce7b`

This hash was recomputed after writing the approved release, entry, rule,
fixture, and source metadata. It is the canonical hash to be presented with
the frozen content commit for separate final sign-off.

## Exact review scope

Please approve, reject, or request changes to each group:

1. **Entry set:** include `E27`, `E14`, `B22d`, `GU10`, `G9`, `R7s`;
   defer `G4`, `GU5.3`, `G13`, `2G11`, and `GX53`.
2. **Electrical rules:** only confirmed `220–240 V` is in scope; confirmed
   `100–127 V` and `1–48 V` are outside scope; other uncovered or
   contradictory values cannot produce `Compatible`; frequency is optional,
   but a confirmed value must be exactly `50 Hz`, otherwise the outcome is
   `PotentialConflict`; the shopping target is `220–240 V / 50 Hz`.
3. **Catalog copy:** every canonical code, EN/RU name, EN/RU alias, and EN/RU
   distinguishing hint in the shipping JSON.
4. **Sources and licensing:** all six source records, their exact facts,
   locators, permitted transformations, and no-generalization limits.
5. **Rules and fixtures:** all 12 reviewed rule codes and all 39 expected
   safety-fixture outcomes.
6. **Reference UI:** the six original conceptual diagrams, their text
   alternatives, the “not to scale” caption, and the unchanged printed-voltage
   warning.
7. **Reports and diff:** automated checks, advisory AI review, catalog diff,
   and the exact complete Git diff before the content commit.

## Required approval meaning

An approval means that Sergey V. has reviewed the exact pending bundle and
authorizes the preparation of the content commit with:

- catalog release state `APPROVED`;
- all six entries `APPROVED` and enabled;
- all source records, rules, and fixtures marked `APPROVED`;
- one shared ISO-8601 UTC review timestamp;
- the canonical hash recomputed and presented once more before final sign-off.

Approval here does not authorize weakening production validation, faking an
unavailable release gate, or claiming unrun macOS/physical-device checks.

## Recorded approval

`Одобряю все 7 групп каталога 2026.08.01 с указанным набором, правилами,
39 fixture outcomes, текстами, источниками и схемами. Разрешаю подготовить
content commit и предъявить финальный hash для sign-off.`
