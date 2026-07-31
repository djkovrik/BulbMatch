# SCREEN-001 — Match home

Linked flows: FLOW-001, FLOW-002, FLOW-003  
Linked requirements: REQ-001, REQ-002, REQ-003, REQ-004, REQ-016, REQ-017

## Purpose and information hierarchy

The default root screen starts a match without onboarding. Order: top app bar with BulbMatch and Settings; short promise; safety-scope line; primary Camera; secondary Choose photo; tertiary Enter manually; optional neutral “unfinished scan was not saved” message; bottom navigation.

Suggested copy:

- EN: “Match a replacement from what is printed on your old bulb.”
- RU: “Подберите замену по маркировке на старой лампе.”
- Scope: “For 220–240 V / 50 Hz regions. Always check the fixture label.” / “Для регионов 220–240 В / 50 Гц. Всегда проверяйте маркировку светильника.”

## States

- **Content:** all three start actions enabled; Match selected in bottom navigation.
- **Loading:** not used for first content; network SDK initialization has no visible blocker.
- **Empty:** same as Content; no onboarding or recent-result carousel.
- **Error:** catalog integrity failure disables Camera/Photo assessment paths but leaves manual/reference explanation and support/source details; avoid generic “Something went wrong.”
- **Offline:** no warning badge because core work is offline; failed advertising/crash delivery is invisible here.
- **Permission:** camera permission is not requested on this screen. A returned denied state may show one-line guidance beside Camera without hiding picker/manual actions.

## Actions, outputs, navigation, and validation

Camera opens SCREEN-002 through FLOW-002. Choose photo opens the system picker then SCREEN-003. Enter manually opens SCREEN-004 through FLOW-003. Bottom navigation opens SCREEN-007 or SCREEN-009. Settings opens SCREEN-011.

## Responsive layout and insets

Single-column compact layout; at expanded width, place explanatory copy and the three-action panel in a centered maximum-width container, not a stretched row. Respect status bar, display cutout, gesture/navigation bar, keyboard, and bottom-navigation insets.

## Accessibility and localization

EN/RU strings must reflow. At 200% font scale actions stack and remain fully labeled. Screen-reader order follows title, promise, scope, Camera, Choose photo, Enter manually, transient message, bottom navigation, Settings. Camera is not the only emphasized path. Warm yellow accent meets contrast requirements in both light/dark themes.

## Allowed ad slots

None.

## Reference assets

Original small lamp-outline illustration is optional and decorative; hide it from accessibility. No stock bulb photo.
