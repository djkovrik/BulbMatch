# FLOW-008 — Settings, disclosures, and local reset

Linked requirements: REQ-026  
Linked screens: SCREEN-011, SCREEN-007

## Goal

Give the user understandable display controls, privacy/source disclosures, and a complete local history reset.

## Entry and exit conditions

Enter from the top app bar on a root destination. Exit back to that destination.

## Ordered steps

1. Show Language with System, English, and Русский.
2. Show Theme with System, Light, and Dark.
3. Show Privacy summary and the production policy at `https://sedsoftware.com/apps/bulbmatch/policy.html`.
4. Show catalog/ruleset/source-manifest versions and licenses link.
5. Show app version, publisher `Sergey V.`, support `info@sedsoftware.com`, and safety disclaimer.
6. Clear local data opens a confirmation that names what is deleted and retained.

## Branches and resume

- Language/theme changes apply immediately and persist.
- If the configured privacy URL differs from the approved release metadata or cannot be opened during release review, store submission is blocked; local Settings remain usable.
- Clear failure preserves data and offers retry.
- Offline network links explain that connectivity is required; all local settings still work.
- Settings contains no advertising.

## AC-026

Given the user changes language/theme or confirms Clear local data  
When the operation completes and the app restarts  
Then display choices persist, saved matches and ad-frequency counters are cleared when requested, language/theme remain, and privacy/source/version information is still accessible.
