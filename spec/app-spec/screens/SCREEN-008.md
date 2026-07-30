# SCREEN-008 — Saved result detail

Linked flow: FLOW-005  
Linked requirements: REQ-013, REQ-017, REQ-023

## Purpose and information hierarchy

Reopen the immutable result as it was saved. Order: back; saved name/default and date; stored outcome/scope warning; stored reasons; stored profile/checklist; catalog/ruleset version; Delete.

## States

- **Content:** snapshot renders without recalculation.
- **Loading:** local record lookup.
- **Empty:** missing/deleted ID returns to History with an announcement.
- **Error:** malformed/unsupported snapshot shows unavailable record metadata and Delete; never reconstructs a positive result from summary columns.
- **Offline:** identical to Content.
- **Permission:** not applicable.

## Actions, outputs, navigation, and validation

Back returns to SCREEN-007. Delete requires confirmation. Optional “Start a new match with these confirmed values” is out of MVP to avoid silently reusing stale facts.

## Responsive layout and insets

Use SCREEN-005's readable result width but omit match actions and ads. Expanded layout never moves version/provenance before safety content. Respect safe areas.

## Accessibility and localization

Announce that this is a saved historical snapshot and state its catalog version. Screen-reader order follows the stored evidence ladder then Delete. At 200% font scale no clipping/horizontal tables. EN/RU UI surrounds preserved canonical values. Light/dark themes do not alter stored status meaning.

## Allowed ad slots

None.

## Reference assets

Original diagrams referenced by stable asset IDs; if unavailable, use localized text alternative.
