# SCREEN-003 — Image review and OCR processing

Linked flow: FLOW-002  
Linked requirements: REQ-003, REQ-015, REQ-018, REQ-024

## Purpose and information hierarchy

Let the user judge image legibility before recognition. Order: back; heading; fitted image preview; “Can you read the marking?” hint; Use photo; Retake/Choose another; Enter manually.

## States

- **Content:** preview and all actions.
- **Loading:** after Use photo, show determinate stage text “Reading text on this device / Распознаём текст на устройстве”; keep Cancel available; never imply network upload.
- **Empty:** cancelled picker/camera returns to previous screen instead of rendering an empty preview.
- **Error:** unreadable decode, no text, or recognizer failure names the cause where known and offers Try again, Choose another, and Enter manually.
- **Offline:** identical; optional copy can say “Works offline / Работает офлайн.”
- **Permission:** no new permission. If the ephemeral URI becomes unavailable, treat it as an image error and return to picker/manual choices.

## Actions, outputs, navigation, and validation

Use photo starts bundled recognition once. Retake returns to SCREEN-002; Choose another invokes the system picker; Enter manually releases the image and opens SCREEN-004. Cancel during recognition releases work and returns to Content if the image remains available.

## Responsive layout and insets

Contain the image without stretching and reserve at least half the compact screen for actions/instructions when text is scaled. Expanded width uses preview beside an action panel. Respect safe areas, system bars, and keyboard.

## Accessibility and localization

Do not expose raw pixels as recognized text. Preview label says it is the selected marking photo. Screen-reader order: back, heading, preview, hint, Use photo, alternate actions, progress/error. EN/RU progress, error, and action copy must reflow independently. At 200% font scale the image may shrink; actions never clip. Light/dark themes use neutral preview backing.

## Allowed ad slots

None.

## Reference assets

The user's ephemeral image only; never cached as an app asset.
