# FLOW-007 — Privacy-safe crash reporting and advertising

Linked requirements: REQ-019, REQ-020, REQ-021, REQ-022  
Linked screens: SCREEN-005, SCREEN-007, SCREEN-009, SCREEN-011

## Goal

Operate approved network SDKs without changing the core task, exposing product data, or interrupting safety-critical work.

## Entry and exit conditions

Crash setup occurs in release platform hosts. Ad setup occurs after privacy configuration and first content readiness. The flow exits when a banner collapses/renders or interstitial show/failure completes the already-requested navigation.

## Ordered steps

1. Apply conservative ad privacy flags before Yandex SDK initialization.
2. Initialize network SDKs without blocking Match content.
3. Request a banner only for an approved visible slot.
4. Compute interstitial eligibility from local counters after a completed positive match.
5. On explicit eligible result exit, show a loaded interstitial or navigate immediately if unavailable.
6. Send crashes/non-fatals only through the fixed technical allowlist.

## Branches and resume

- Offline or failed banner collapses with no placeholder.
- Interstitial is ineligible for first match, clarification/conflict outcomes, cooldown under ten minutes, fewer than three positive matches since the last impression, save in progress, app launch/resume, or destructive dialog.
- Backgrounding cancels pending banner attachment; resume does not open a full-screen ad.
- Tests/previews use fakes; debug device builds use official test IDs.
- Release configuration with blank/known test ID fails before packaging.

## AC-019

Given a controlled release test crash containing synthetic canary OCR and result-name strings in app memory  
When the Crashlytics report is inspected  
Then the crash arrives with allowed technical metadata and none of the canary, photo, OCR, confirmed-value, or stable-user data.

## AC-020

Given each app screen and state is inspected  
When advertisements are requested  
Then only result inline, history sticky, reference sticky, and eligible match-exit interstitial placements exist, and safety/capture/review content is never interrupted or covered.

## AC-021

Given a user has completed positive matches and explicitly exits SCREEN-005  
When eligibility is evaluated  
Then the first match never shows an interstitial, subsequent impressions are at least three positive matches and ten minutes apart, and only a successful impression resets counters.

## AC-022

Given the ad network fails, the process is a preview/test, or release IDs are invalid  
When ad behavior executes or release validation runs  
Then product interaction continues with zero-height failed slots, SDK is absent from previews/tests, and invalid release configuration blocks packaging.
