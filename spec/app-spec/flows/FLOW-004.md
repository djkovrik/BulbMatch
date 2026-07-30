# FLOW-004 — Assess and use a replacement profile

Linked requirements: REQ-008, REQ-009, REQ-010, REQ-011, REQ-025  
Linked screens: SCREEN-004, SCREEN-005, SCREEN-006

## Goal

Turn confirmed facts into an auditable outcome and a conservative shopping checklist.

## Entry and exit conditions

Enter after valid confirmed input. Exit by saving, starting another match, or leaving the result.

## Ordered steps

1. Run the pure engine with confirmed input and the current bundled catalog snapshot.
2. Render outcome plus regional electrical warning.
3. Render confirmed facts and reason codes.
4. Render unresolved/conflicting checks before any shopping profile.
5. For a positive outcome, render exact base, supply family, confirmed optional preferences, fixture limit if manually provided, and explicit unchecked items.
6. Render Save and Match another actions.
7. Only then, and only for a positive outcome, allow one inline ad container.

## Branches and resume

- Need clarification returns to SCREEN-004 with retained confirmed values.
- Potential conflict uses blocking language and no ad; it offers Edit details and Reference.
- Closing the result without saving loses only the result snapshot.
- Same-process back returns to the reviewed draft; process death does not restore it.
- Network/ad failure has no effect on the outcome or navigation.

## AC-008

Given the same confirmed input and catalog/ruleset version  
When assessment runs repeatedly  
Then it returns the same Compatible, Need clarification, or Potential conflict outcome, reasons, and replacement profile.

## AC-009

Given confirmed voltage is contradictory, clearly low-voltage, or belongs to a 100–127 V supply family  
When assessment runs for the 220–240 V / 50 Hz scope  
Then the result is Potential conflict and no compatible shopping profile or result ad is shown.

## AC-010

Given the source lamp has a confirmed wattage but the fixture maximum was not entered separately  
When the result is built  
Then no fixture maximum is inferred and the checklist instructs the user to read the fixture label.

## AC-011

Given a positive assessment with some optional fields missing  
When SCREEN-005 renders  
Then it shows status, reasons, warning, known replacement constraints, explicit unknowns, checklist, actions, and only afterward the optional ad.

## AC-025

Given any required fact is missing, unknown, unsupported, contradictory, or outside scope  
When all engine branches are exercised by the approved safety fixture suite  
Then none produces a Compatible result, and any failure blocks release.
