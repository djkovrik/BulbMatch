# SCREEN-010 — Base reference detail

Linked flows: FLOW-003, FLOW-006  
Linked requirements: REQ-007, REQ-014, REQ-017, REQ-020, REQ-023

## Purpose and information hierarchy

Explain one base without asserting hidden electrical compatibility. Order: back; canonical code/name; large original diagram; distinguishing features; common-use/typical hints; “check the printed voltage” warning; Use this base; optional sticky advertisement.

## States

- **Content:** full bundled entry and action.
- **Loading:** brief local lookup.
- **Empty:** missing ID returns to list with announcement.
- **Error:** corrupt entry with no diagram/value guessing; show source/version route.
- **Offline:** identical; ad collapses.
- **Permission:** not applicable.

## Actions, outputs, navigation, and validation

Use this base starts SCREEN-004 with a Manual known-base value only. It does not prefill voltage, power, lumens, or fixture facts. Back returns to the previous reference/search position.

## Responsive layout and insets

Compact single column; expanded width may place diagram beside text but warning and action follow both. Sticky ad remains below content/action and above safe bottom inset; it never covers the diagram.

## Accessibility and localization

Screen-reader order: code/name, diagram alternative, features, typical hints, warning, Use this base, advertisement. At 200% font scale detail scrolls naturally. EN/RU prose is localized; code stays canonical. Diagram strokes and semantic warning meet light/dark contrast.

## Allowed ad slots

`REFERENCE_STICKY`; omit while transitioning into Match.

## Reference assets

One original scalable diagram per catalog asset ID.
