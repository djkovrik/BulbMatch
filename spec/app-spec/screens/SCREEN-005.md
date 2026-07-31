# SCREEN-005 — Replacement result

Linked flows: FLOW-004, FLOW-005, FLOW-007  
Linked requirements: REQ-008, REQ-009, REQ-010, REQ-011, REQ-012, REQ-020, REQ-021, REQ-025

## Purpose and information hierarchy

Render the safety-first evidence ladder. Order: back; outcome icon/title; “not fixture certification” scope line; electrical warning; reasons/confirmed facts; unresolved or conflicting checks; replacement profile; shopping checklist; Save and Match another; optional inline advertisement.

Outcome language:

- `Compatible`: “Compatible profile / Совместимый профиль”
- `NeedClarification`: “Need clarification / Нужно уточнение”
- `PotentialConflict`: “Potential conflict / Возможен конфликт”

## States

- **Content:** one of the three complete outcome variants. Only `Compatible` may include `RESULT_INLINE`, after actions.
- **Loading:** assessment is local and normally imperceptible; if catalog initialization is pending, show a short skeleton without ads and retain back navigation.
- **Empty:** not applicable; missing assessment returns to SCREEN-004.
- **Error:** corrupted catalog/ruleset or unexpected engine failure shows no recommendation, a safety-first explanation, Edit details, and source/support route.
- **Offline:** full result remains available; inline ad collapses.
- **Permission:** not applicable.

## Actions, outputs, navigation, and validation

Save opens SCREEN-006. Edit details returns to SCREEN-004. Match another clears ephemeral result/draft and opens SCREEN-001. Reference opens SCREEN-009/010 where relevant. Back/exit is the only possible interstitial trigger and only under FLOW-007 eligibility.

The status and checklist are derived only from the immutable assessment. UI code cannot upgrade severity or hide unresolved checks.

## Responsive layout and insets

Compact uses one maximum-width scrolling column. Expanded may place confirmed facts and shopping profile side by side only after the full-width outcome/warning, and actions remain after both. Inline ad uses content width, natural bounded height, and no reserved space on failure. Respect safe areas and bottom navigation of the destination after exit.

## Accessibility and localization

Screen-reader order exactly follows the evidence ladder, with advertisement last. Outcome announces icon meaning and text; green/amber/red are not the sole cue. Collapsible details, if used, default expanded for safety and expose state. At 200% font scale tables become labeled rows, not horizontal scroll. EN/RU copy is written independently, not concatenated. Light/dark semantic containers meet contrast.

## Allowed ad slots

`RESULT_INLINE` only for `Compatible`, after complete checklist and actions. No sticky or interstitial inside the rendered screen; interstitial may occur only after an eligible explicit exit.

## Reference assets

Original base/shape line diagrams. Lazyweb prototype direction: safety-first evidence ladder.
