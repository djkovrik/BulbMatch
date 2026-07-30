# SCREEN-004 — Confirm detected data / manual entry

Linked flows: FLOW-002, FLOW-003, FLOW-004, FLOW-006  
Linked requirements: REQ-004, REQ-005, REQ-006, REQ-007, REQ-010, REQ-024

## Purpose and information hierarchy

Make every fact and its origin inspectable. Order: top bar; “Review every detected field” message when OCR exists; unresolved summary; Base; Electrical; Light output; Appearance; Fixture; safety note; Assess replacement profile.

Each field shows label, value/input, origin chip (`Detected`, `Edited`, `Manual`), validation, and an explicit Confirm/Edit/Reject decision when sourced from OCR. Optional absent fields use “Not provided,” not zero.

## States

- **Content:** editable grouped form; assessment enabled only when every observation is handled and required construction is valid.
- **Loading:** only for catalog-backed base search; retain typed query and other fields.
- **Empty:** manual mode starts with blank fields and instructional examples; OCR mode with no candidates routes to the recoverable no-text state rather than pretending detection succeeded.
- **Error:** inline parse errors plus top unresolved summary; database/network errors are not relevant.
- **Offline:** identical to Content because catalog is bundled.
- **Permission:** not applicable; any camera recovery occurs before this screen.

## Actions, outputs, navigation, and validation

Base picker searches canonical codes and EN/RU aliases and supports Unknown base raw text. Voltage accepts a nominal value or inclusive range with unit. Fixture maximum is in a visibly separate section and has Manual origin only. Assess sends immutable confirmed input to FLOW-004. Back with edits asks to discard.

Validation never autocorrects a materially different value. OCR confidence may influence a subtle review cue but is never described as compatibility confidence.

## Responsive layout and insets

Compact: one scrolling column with a sticky bottom assessment action that moves above keyboard and never covers validation. Expanded: maximum two panes—section index/summary and form—while preserving one reading order. Respect top/bottom/keyboard insets.

## Accessibility and localization

Screen-reader order follows unresolved summary then visual form order, with each field announcing label, value, origin, decision, error, and required/optional status. Focus moves to the first unresolved field after failed assessment. EN/RU examples are localized; unit identifiers remain canonical. At 200% font scale chips wrap below labels and the bottom action remains reachable. Statuses use icon/text in light/dark themes.

## Allowed ad slots

None.

## Reference assets

Original base mini-diagrams may appear in the picker with equivalent text labels.
