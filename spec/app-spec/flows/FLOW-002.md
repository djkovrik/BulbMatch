# FLOW-002 — Capture or import and recognize markings

Linked requirements: REQ-002, REQ-003, REQ-005, REQ-015, REQ-018, REQ-024, REQ-027
Linked screens: SCREEN-001, SCREEN-002, SCREEN-003, SCREEN-004

## Goal

Produce editable OCR observations from an ephemeral image without turning recognition into a decision.

## Entry and exit conditions

Enter from SCREEN-001 by Camera or Choose photo. Exit to SCREEN-004 with observations, back to SCREEN-001, or to manual mode after a recoverable failure.

## Ordered steps

1. Camera path checks capability and permission, requests only after the user's action, then opens SCREEN-002.
2. Picker path opens the system single-image picker without broad library access.
3. A selected/captured image appears on SCREEN-003 with Retake/Choose another and Use photo.
4. Use photo downsamples in memory and runs bundled on-device recognition.
5. Parsed observations open SCREEN-004. Every observation starts as undecided.
6. The user confirms, edits, or rejects every observation before assessment can be requested.
7. Release image and OCR memory when the flow finishes or is abandoned.

## Branches and resume

- Denied camera access shows rationale, Open Settings when appropriate, and picker/manual alternatives.
- Recheck camera access after foreground resume from Settings.
- Unavailable camera never disables picker/manual entry.
- Blur, no text, unsupported image, or recognition failure offers retry, choose another, and manual entry.
- Cancelled camera or picker returns without an error toast.
- Process death drops image, observations, and draft; next launch returns to SCREEN-001 with a neutral one-time message.
- Network absence does not alter capture or recognition.

## AC-002

Given camera capability is available and the user grants permission  
When the user captures a marking and accepts the preview  
Then the image is processed on device and SCREEN-004 receives editable observations without writing a photo to app storage.

## AC-003

Given the user chooses Choose photo  
When one image is returned by the system picker  
Then BulbMatch processes only that image without requesting broad photo-library or media permission.

## AC-005

Given OCR returns one or more candidate fields  
When at least one candidate is neither confirmed, edited, nor rejected  
Then the assessment action remains disabled and the first undecided field is identified accessibly.

## AC-015

Given a clean install is in airplane mode  
When the user captures or imports a legible supported label  
Then bundled OCR and the bundled catalog are usable without a model or catalog download.

## AC-018

Given an image, OCR observations, or unfinished draft exists  
When the flow ends or the app process is destroyed  
Then none of those ephemeral artifacts is persisted or restored, while explicitly saved results remain intact.

## AC-024

Given permission denial, unavailable camera, unreadable blur, no text, or OCR failure  
When the failure is presented  
Then the user receives a specific explanation and at least one valid recovery path, with manual entry always available.

## AC-027

Given a legible supported lamp label contains Latin, Cyrillic, or mixed electrical markings
When bundled on-device recognition processes it without network access
Then BulbMatch produces only editable untrusted field candidates, preserves the original recognized text for review, and requires the same confirm, edit, or reject decision as every other OCR candidate.
