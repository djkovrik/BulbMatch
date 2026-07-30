# SCREEN-006 — Save result sheet

Linked flow: FLOW-005  
Linked requirements: REQ-012, REQ-018

## Purpose and information hierarchy

Confirm explicit persistence. Order: title; optional Name; compact “Saved locally” data summary; Save; Cancel.

## States

- **Content:** optional name empty by default; Save enabled.
- **Loading:** transactional save disables duplicate submission, shows progress in Save, and keeps Cancel disabled only for the brief commit.
- **Empty:** empty name is valid and uses an automatic localized summary in History.
- **Error:** preserve name and result, announce failure, offer Retry/Cancel.
- **Offline:** identical to Content.
- **Permission:** not applicable.

## Actions, outputs, navigation, and validation

Trim name; maximum 80 Unicode code points; reject control characters. Confirm persists the immutable confirmed-input/assessment snapshot only. Success closes the sheet and announces Saved. Cancel persists nothing.

## Responsive layout and insets

Compact bottom sheet respects safe areas, keyboard, and gesture inset; expanded width uses a centered dialog. Content scrolls before actions clip.

## Accessibility and localization

Screen-reader order and initial focus are title, Name, data summary, Save, Cancel. Error focus/announcement is immediate. EN/RU summary preserves canonical units/codes. At 200% font scale the sheet expands/scrolls. Light/dark scrim and surface maintain contrast.

## Allowed ad slots

None.

## Reference assets

None.
