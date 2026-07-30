# SCREEN-002 — Camera capture

Linked flow: FLOW-002  
Linked requirements: REQ-002, REQ-018, REQ-024

## Purpose and information hierarchy

Capture a sharp view of printed markings. Order: close action; live preview; concise framing guide; shutter; torch toggle when supported; Choose photo and Enter manually alternatives.

## States

- **Content:** live preview with non-authoritative framing overlay and shutter.
- **Loading:** capability/permission check or camera binding; announce “Opening camera / Открываем камеру” without indefinite spinner.
- **Empty:** not applicable; absence of a camera is `Unavailable`.
- **Error:** binding/capture failure shows Retry, Choose photo, and Enter manually over a neutral surface, not behind an unusable preview.
- **Offline:** identical to Content.
- **Permission:** rationale before OS request; `DeniedCanAsk` offers Try again; `DeniedOpenSettings` offers Open Settings; `Unavailable` omits permission action. All states retain picker/manual alternatives and recheck on resume.

## Actions, outputs, navigation, and validation

Shutter captures one ephemeral image and opens SCREEN-003. Close returns to SCREEN-001. Torch state is session-only. Never start continuous OCR or infer fields over the camera.

## Responsive layout and insets

Preview fills available safe content; controls occupy a solid inset-aware bottom panel and never sit under gesture areas. Landscape may letterbox rather than crop controls.

## Accessibility and localization

Preview has a short description, not fake recognized content. Controls have EN/RU names and state. Screen-reader order: Close, instructions, preview description, torch, shutter, Choose photo, Enter manually. At 200% font scale, guidance may scroll in the bottom panel while shutter remains reachable. Light/dark theme applies to chrome; preview contrast overlay is visible in both.

## Allowed ad slots

None.

## Reference assets

Original corner/frame overlay only; no copied camera artwork.
