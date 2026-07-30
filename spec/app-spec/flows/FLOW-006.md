# FLOW-006 — Browse the base reference

Linked requirements: REQ-014, REQ-023  
Linked screens: SCREEN-009, SCREEN-010, SCREEN-004, SCREEN-011

## Goal

Help users identify common bases from a transparent, versioned offline reference without turning typicals into compatibility facts.

## Entry and exit conditions

Enter from the Reference root or a result clarification action. Exit to root navigation or start manual Match with an explicitly chosen base.

## Ordered steps

1. SCREEN-009 loads bundled groups and localized search aliases.
2. Search by canonical code or EN/RU common name.
3. SCREEN-010 shows original line diagram, code, distinguishing features, common-use hints, and safety caveat.
4. Use this base starts FLOW-003 with a visible manual prefill.
5. Source/catalog details in Settings expose version, date, and source manifest.

## Branches and resume

- Empty search offers clear query and browse groups.
- Catalog validation failure disables entries and gives a local-data error; it never fetches a replacement.
- A reference typical voltage or use is never copied into confirmed Match fields.
- Sticky advertising may appear only after reference content is present and never covers the diagram.

## AC-014

Given the device is offline and the catalog is valid  
When the user searches or browses a supported base  
Then localized reference content and the original diagram are available, and Use this base creates only an explicit manual base value.

## AC-023

Given any catalog entry or rule can affect displayed guidance  
When its production bundle is reviewed  
Then it resolves to a manifest record, catalog/ruleset version, access date, permitted transformation, reviewer, and content hash visible to release tooling.
