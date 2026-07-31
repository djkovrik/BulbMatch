# BulbMatch reviewed ruleset 2026.08.01

Status: `APPROVED`

Electrical target: `220–240 V / 50 Hz`

Reviewer: `Sergey V.`

Reviewed at: `2026-07-31T07:21:40Z`

Review decision: `APPROVED`

Values not fully contained by exactly one listed voltage family are `Ambiguous`. Cap type,
popularity, typical use, lamp shape, and manufacturer examples never supply voltage.

## CATALOG_KNOWN_BASE_ENABLED

```text
ruleCode: CATALOG_KNOWN_BASE_ENABLED
description: Compatible requires an exact known base enabled in the signed catalog.
inputs: ConfirmedBase.Known; immutable CatalogSnapshot.enabledBaseCodes
outcome: Continue only when the exact code is enabled; otherwise NeedClarification.
reasonCode: UnsupportedBase
requirements: REQ-006, REQ-025
acceptanceScenarios: AC-006, AC-025
positiveFixtureIds: BASE-E27-230, BASE-E14-230, BASE-B22D-230, BASE-GU10-230, BASE-G9-230, BASE-R7S-230
negativeFixtureIds: BASE-MISSING, BASE-UNKNOWN, BASE-UNSUPPORTED-GX53
sourceRecordIds: SRC-CAT-E27, SRC-CAT-E14, SRC-CAT-B22D, SRC-CAT-GU10, SRC-CAT-G9, SRC-CAT-R7S
reviewer: Sergey V.
reviewDecision: APPROVED
```

## BASE_NO_SUBSTITUTION

```text
ruleCode: BASE_NO_SUBSTITUTION
description: No visually or textually similar cap may replace the exact confirmed code.
inputs: Confirmed base code and exact enabled codes
outcome: Unsupported exact code returns NeedClarification; no nearest-code fallback.
reasonCode: UnsupportedBase
requirements: REQ-007, REQ-025
acceptanceScenarios: AC-007, AC-025
positiveFixtureIds: BASE-E27-230
negativeFixtureIds: BASE-UNSUPPORTED-GX53
sourceRecordIds: SRC-001
reviewer: Sergey V.
reviewDecision: APPROVED
```

## VOLTAGE_TARGET_220_240

```text
ruleCode: VOLTAGE_TARGET_220_240
description: A confirmed marking fully contained in inclusive 220.0–240.0 V is in scope.
inputs: One user-confirmed nominal voltage or inclusive voltage range
outcome: VoltageDisposition.InScope
reasonCode: VOLTAGE_TARGET_220_240
requirements: REQ-006, REQ-009, REQ-025
acceptanceScenarios: AC-006, AC-009, AC-025
positiveFixtureIds: VOLTAGE-220-240, VOLTAGE-230
negativeFixtureIds: VOLTAGE-PARTIAL-200-240, VOLTAGE-250
sourceRecordIds: SRC-002
reviewer: Sergey V.
reviewDecision: APPROVED
```

## VOLTAGE_OTHER_MAINS_100_127

```text
ruleCode: VOLTAGE_OTHER_MAINS_100_127
description: A confirmed marking fully contained in inclusive 100.0–127.0 V is another mains family.
inputs: One user-confirmed nominal voltage or inclusive voltage range
outcome: VoltageDisposition.OutsideScope; PotentialConflict
reasonCode: OutsideElectricalScope
requirements: REQ-009, REQ-025
acceptanceScenarios: AC-009, AC-025
positiveFixtureIds: none
negativeFixtureIds: VOLTAGE-110-120
sourceRecordIds: SRC-002
reviewer: Sergey V.
reviewDecision: APPROVED
```

## VOLTAGE_LOW_1_48

```text
ruleCode: VOLTAGE_LOW_1_48
description: A confirmed marking fully contained in inclusive 1.0–48.0 V is explicitly low voltage.
inputs: One user-confirmed nominal voltage or inclusive voltage range
outcome: VoltageDisposition.OutsideScope; PotentialConflict
reasonCode: OutsideElectricalScope
requirements: REQ-009, REQ-025
acceptanceScenarios: AC-009, AC-025
positiveFixtureIds: none
negativeFixtureIds: VOLTAGE-12, VOLTAGE-24
sourceRecordIds: SRC-002, SRC-005
reviewer: Sergey V.
reviewDecision: APPROVED
```

## VOLTAGE_UNCOVERED_AMBIGUOUS

```text
ruleCode: VOLTAGE_UNCOVERED_AMBIGUOUS
description: A value not fully contained by exactly one reviewed family is not guessed.
inputs: One user-confirmed nominal voltage or inclusive voltage range
outcome: VoltageDisposition.Ambiguous; NeedClarification
reasonCode: AmbiguousVoltage
requirements: REQ-009, REQ-025
acceptanceScenarios: AC-009, AC-025
positiveFixtureIds: none
negativeFixtureIds: VOLTAGE-PARTIAL-200-240, VOLTAGE-250
sourceRecordIds: SRC-002
reviewer: Sergey V.
reviewDecision: APPROVED
```

## VOLTAGE_CONTRADICTORY

```text
ruleCode: VOLTAGE_CONTRADICTORY
description: Two confirmed incompatible voltage tokens are a conflict, not a best guess.
inputs: VoltageEvidence.Contradictory with at least two markings
outcome: PotentialConflict
reasonCode: ContradictoryVoltage
requirements: REQ-009, REQ-025
acceptanceScenarios: AC-009, AC-025
positiveFixtureIds: none
negativeFixtureIds: VOLTAGE-CONTRADICTORY-120-230
sourceRecordIds: SRC-002
reviewer: Sergey V.
reviewDecision: APPROVED
```

## FREQUENCY_CONFIRMED_TARGET_50

```text
ruleCode: FREQUENCY_CONFIRMED_TARGET_50
description: When a frequency marking is confirmed, it must equal the reviewed 50 Hz target; an absent frequency remains optional.
inputs: ConfirmedMatchInput.frequency; CatalogSnapshot.targetFrequency
outcome: Continue for absent or exactly 50 Hz; any other confirmed value returns PotentialConflict.
reasonCode: OutsideFrequencyScope
requirements: REQ-009, REQ-025
acceptanceScenarios: AC-009, AC-025
positiveFixtureIds: FREQUENCY-50
negativeFixtureIds: FREQUENCY-60
sourceRecordIds: SRC-002
reviewer: Sergey V.
reviewDecision: APPROVED
```

## FIXTURE_MAXIMUM_MANUAL_ONLY

```text
ruleCode: FIXTURE_MAXIMUM_MANUAL_ONLY
description: Fixture maximum wattage can be constructed only from dedicated manual input.
inputs: FixtureMaximumPower.manual(Watts)
outcome: Use the value only as a fixture limit; a lower limit than source watts is blocking.
reasonCode: FixturePowerConflict
requirements: REQ-010, REQ-025
acceptanceScenarios: AC-010, AC-025
positiveFixtureIds: POWER-MANUAL-FIXTURE-LIMIT
negativeFixtureIds: POWER-MANUAL-LIMIT-BELOW-SOURCE
sourceRecordIds: SRC-002
reviewer: Sergey V.
reviewDecision: APPROVED
```

## SOURCE_WATTS_NOT_FIXTURE_MAXIMUM

```text
ruleCode: SOURCE_WATTS_NOT_FIXTURE_MAXIMUM
description: Source-lamp rated watts never populate fixture maximum watts.
inputs: Confirmed sourceRatedPower with fixtureMaximumPower absent
outcome: Compatible may continue, but fixture limit remains unresolved and the checklist requests the fixture label.
reasonCode: FixtureLimitUnresolved
requirements: REQ-010, REQ-011
acceptanceScenarios: AC-010, AC-011
positiveFixtureIds: POWER-SOURCE-WITHOUT-FIXTURE-LIMIT
negativeFixtureIds: none
sourceRecordIds: SRC-002
reviewer: Sergey V.
reviewDecision: APPROVED
```

## LUMENS_SEPARATE_FROM_EQUIVALENT_WATTS

```text
ruleCode: LUMENS_SEPARATE_FROM_EQUIVALENT_WATTS
description: Confirmed lumens create brightness preference; printed equivalent watts remain separate from actual watts.
inputs: luminousFlux, sourceRatedPower, printedEquivalentPower
outcome: No watts-to-lumens inference and no equivalent-watts fixture limit.
reasonCode: BrightnessUnresolved when lumens are absent
requirements: REQ-011, REQ-025
acceptanceScenarios: AC-011, AC-025
positiveFixtureIds: BRIGHTNESS-LUMENS-PRESENT, POWER-EQUIVALENT-SEPARATE
negativeFixtureIds: BRIGHTNESS-LUMENS-ABSENT
sourceRecordIds: SRC-003, SRC-004
reviewer: Sergey V.
reviewDecision: APPROVED
```

## FIXTURE_SAFETY_DISCLAIMER

```text
ruleCode: FIXTURE_SAFETY_DISCLAIMER
description: A positive profile never certifies fixture, wiring, enclosure, dimensions, dimmer, or installation.
inputs: Any Compatible assessment
outcome: Include ThisDoesNotCertifyFixture and applicable verification checks.
reasonCode: ThisDoesNotCertifyFixture
requirements: REQ-011, REQ-025
acceptanceScenarios: AC-011, AC-025
positiveFixtureIds: BASE-E27-230
negativeFixtureIds: none
sourceRecordIds: SRC-002
reviewer: Sergey V.
reviewDecision: APPROVED
```
