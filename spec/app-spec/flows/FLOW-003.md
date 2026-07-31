# FLOW-003 — Manual entry and field confirmation

Linked requirements: REQ-004, REQ-006, REQ-007  
Linked screens: SCREEN-001, SCREEN-004, SCREEN-009, SCREEN-010

## Goal

Create a fully reviewed input without trusting OCR or guessing unsupported values.

## Entry and exit conditions

Enter from SCREEN-001, from OCR observations, or from Use this base in the Reference. Exit to assessment or cancel to SCREEN-001.

## Ordered steps

1. Render source sections for Base, Electrical, Light output, Appearance, and Fixture.
2. Distinguish Detected, Edited, Manual, and Not provided values.
3. Require a base choice from the catalog or an explicit Unknown base raw string.
4. Require voltage for assessment; validate nominal/range syntax.
5. Offer optional source rated watts, printed equivalent watts, lumens, Kelvin, shape, and dimmability.
6. Offer fixture maximum wattage only in a separately labeled Fixture section and only as manual input.
7. Validate inline, summarize unresolved required fields, and request assessment only after all detected fields are handled.

## Branches and resume

- A reference-selected base is prefilled as Manual and remains editable.
- Unknown base preserves a trimmed raw value for explanation.
- Leaving with edits asks whether to discard only within the live process.
- Process recreation drops the draft.
- Invalid optional fields are either corrected or explicitly cleared; they are never silently ignored.

## AC-004

Given the user has no usable photo  
When the user chooses Enter manually and supplies valid fields  
Then the same assessment engine receives confirmed input with Manual origins and no OCR dependency.

## AC-006

Given all OCR observations are handled  
When known base or voltage is missing  
Then SCREEN-004 prevents a positive assessment and explains exactly which required fact is absent.

## AC-007

Given the user confirms a raw base marking that has no supported catalog mapping  
When assessment runs  
Then the outcome is Need clarification, the raw text is shown, and no substitute base is guessed.
