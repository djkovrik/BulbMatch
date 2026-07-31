# SCREEN-011 — Settings and privacy

Linked flows: FLOW-001, FLOW-006, FLOW-007, FLOW-008  
Linked requirements: REQ-016, REQ-017, REQ-019, REQ-023, REQ-026

## Purpose and information hierarchy

Expose product controls and accountability. Order: back; Language; Theme; Privacy and network services; Catalog and sources; Safety disclaimer; About/support; Clear local data.

## States

- **Content:** typed settings and version/disclosure rows.
- **Loading:** only while reading local settings/version metadata; preserve back action.
- **Empty:** not applicable; required metadata is packaged.
- **Error:** malformed preference uses documented default and a non-blocking message; catalog metadata failure is visible and links to support.
- **Offline:** local controls work; privacy/source web links are disabled or open later with “Internet required.”
- **Permission:** no permission request. Camera permission status may be shown only as an informational link to system Settings, not a custom toggle.

## Actions, outputs, navigation, and validation

Language: English/Русский, with English as the default. Theme: System/Light/Dark. Privacy opens the app-owned summary and `https://sedsoftware.com/apps/bulbmatch/policy.html`. About shows publisher `Sergey V.` and support `info@sedsoftware.com`. Sources opens the packaged manifest summary. Clear local data uses destructive confirmation and preserves language/theme. Back returns to the prior root destination.

## Responsive layout and insets

Centered settings list with section headers; expanded width limits line length. Destructive row remains in normal scroll, never pinned beside navigation. Respect all safe areas and keyboard if confirmations include text (normally they do not).

## Accessibility and localization

Rows expose name, current value, role, and destination. Screen-reader order follows visual sections, with destructive action last. Locale changes re-announce title and retain focus logically. At 200% font scale rows wrap rather than truncate. Light/dark preview is not required; selection applies immediately. Privacy/provider names remain accurate in EN/RU.

## Allowed ad slots

None.

## Reference assets

No external logos. Provider names are text.
