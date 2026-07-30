# SCREEN-007 — History

Linked flows: FLOW-001, FLOW-005, FLOW-007, FLOW-008  
Linked requirements: REQ-001, REQ-013, REQ-017, REQ-020, REQ-022, REQ-026

## Purpose and information hierarchy

Show explicitly saved results newest first. Order: top app bar with Settings and overflow Clear all; list/empty state; optional sticky advertisement; bottom navigation.

Each row exposes optional name or localized default, status text/icon, base/raw unknown base, voltage summary, date, and Open. Destructive actions are not gesture-only.

## States

- **Content:** ordered rows; `HISTORY_STICKY` may render above bottom navigation.
- **Loading:** short local database load/skeleton; banner request starts only after content/empty state is known.
- **Empty:** original neutral illustration, “Saved results appear here,” and Start a match.
- **Error:** preserve bottom navigation; explain local read failure and offer Retry. Never replace it with an ad.
- **Offline:** history unchanged; sticky ad collapses.
- **Permission:** not applicable.

## Actions, outputs, navigation, and validation

Tap row opens SCREEN-008. Row overflow offers Delete with confirmation. Clear all uses a confirmation naming saved results and ad counters while retaining language/theme. Start a match opens SCREEN-001.

## Responsive layout and insets

Compact one-column list; expanded width may use two-column cards while preserving chronological reading order. Sticky ad is inset within safe areas above bottom navigation, does not overlay rows, and disappears with zero-height on failure. Lists account for ad/nav height.

## Accessibility and localization

Screen-reader order: title/actions, list newest-first, sticky ad, bottom navigation. Each row forms one meaningful group and exposes a separate Delete action. EN/RU dates and default names are locale formatted; canonical values are not translated. At 200% font scale row metadata wraps and never truncates status. Light/dark themes keep semantic status contrast.

## Allowed ad slots

`HISTORY_STICKY` only. Hide while delete/clear confirmation is visible.

## Reference assets

Original empty-history line illustration.
