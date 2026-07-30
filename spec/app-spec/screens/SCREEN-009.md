# SCREEN-009 — Base reference list

Linked flows: FLOW-001, FLOW-003, FLOW-004, FLOW-006, FLOW-007  
Linked requirements: REQ-001, REQ-007, REQ-014, REQ-017, REQ-020, REQ-022, REQ-023

## Purpose and information hierarchy

Help identify a common base offline. Order: top app bar with Settings; search; category filters; result list; optional sticky advertisement; bottom navigation.

Each entry shows canonical code, localized common name, original diagram, and one distinguishing hint. Typical-use text is explicitly informational.

## States

- **Content:** searchable grouped entries; `REFERENCE_STICKY` may render.
- **Loading:** bundled catalog validation/indexing; no ad until complete.
- **Empty:** no query result with Clear search and Browse all; not “unsupported base” until the user explicitly chooses that in Match.
- **Error:** catalog integrity/schema failure disables entries and offers source/support details; no network fetch.
- **Offline:** full reference works; sticky ad collapses.
- **Permission:** not applicable.

## Actions, outputs, navigation, and validation

Search accepts code/EN/RU aliases. Entry opens SCREEN-010. Bottom navigation changes root. Settings opens SCREEN-011.

## Responsive layout and insets

Compact list; expanded grid with consistent reading order. Sticky ad sits within safe areas above bottom navigation and never covers a row or search result. Search remains visible with keyboard/insets.

## Accessibility and localization

Screen-reader order: title/actions, search, filters, results, sticky ad, bottom navigation. Diagram alternative includes code and distinguishing physical pattern without promising scale. At 200% font scale filters wrap/scroll accessibly and entries grow. EN/RU aliases do not translate canonical identifiers. Light/dark themes render line art and focus clearly.

## Allowed ad slots

`REFERENCE_STICKY` only.

## Reference assets

Original base diagrams listed in `assets/visual-asset-manifest.md`.
