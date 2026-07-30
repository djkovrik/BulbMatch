# FLOW-005 — Save and manage history

Linked requirements: REQ-012, REQ-013  
Linked screens: SCREEN-005, SCREEN-006, SCREEN-007, SCREEN-008

## Goal

Persist only an explicitly requested immutable result and let the user control its retention.

## Entry and exit conditions

Enter from a result Save action or History root. Exit to the current result, saved detail, or root navigation.

## Ordered steps

1. Save opens SCREEN-006 with an optional name and a concise data summary.
2. Confirm inserts one immutable snapshot transactionally.
3. History observes newest-first summaries.
4. Selecting a row opens SCREEN-008 from the stored snapshot and original catalog/ruleset version.
5. Delete one and Clear all require destructive confirmation.

## Branches and resume

- Empty history explains that results are saved only on explicit request.
- Save failure keeps the result and entered name, offers retry, and never duplicates after one confirmed transaction.
- A malformed legacy record is isolated as unavailable with Delete action.
- Clear all keeps language/theme but resets ad-frequency counters, as stated in confirmation.
- Offline behavior is identical.

## AC-012

Given any completed result and an optional valid name  
When the user confirms Save  
Then exactly one immutable snapshot is stored without image or full OCR text and is visible in History after restart.

## AC-013

Given one or more saved results  
When the user reopens one, deletes one, or confirms Clear all  
Then the database performs the selected action transactionally, updates History, and never modifies unrelated records.
