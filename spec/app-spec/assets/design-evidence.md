# Design evidence handoff

## Recommended direction

Build the result as a **safety-first evidence ladder**:

1. outcome;
2. reasons;
3. unresolved checks/warning;
4. replacement profile;
5. shopping checklist;
6. actions;
7. at most one advertisement.

This direction attacks the central product risk: a user acting on a bare green badge without understanding which facts were confirmed and which fixture properties remain unknown.

## Evidence limits

- Lazyweb searches found strong adjacent patterns in scanner confirmation, structured result cards, safety acknowledgments, and task-completion screens.
- Direct bulb-replacement UI coverage was sparse.
- Monetization searches did not yield relevant measured A/B results for these utility placements.
- Therefore the direction is an evidence-informed hypothesis, not a measured uplift claim.

## Alternative concepts

- **Store ticket:** optimized for carrying a concise checklist into a shop. Prefer only if usability testing shows the evidence ladder feels too long; skip if it weakens warnings.
- **Fit passport:** pairs old-lamp facts and replacement constraints visually. Prefer only if users confuse source facts with recommendations; skip if lines/diagrams become visually authoritative.

## Implementation instruction

Use `SCREEN-004` and `SCREEN-005` as the source of truth. After deterministic Compose previews exist, run a fresh Lazyweb improvement pass against screenshots rather than treating the exploratory JPGs as pixel-perfect specifications.
