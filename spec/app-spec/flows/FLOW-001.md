# FLOW-001 — First launch and root navigation

Linked requirements: REQ-001, REQ-016, REQ-017  
Linked screens: SCREEN-001, SCREEN-007, SCREEN-009, SCREEN-011

## Goal

Enter the useful product immediately, in a readable locale/theme, without mandatory onboarding.

## Entry and exit conditions

Enter on cold or warm launch. Exit into Match, History, Reference, or Settings. Match is selected by default on first launch.

## Ordered steps

1. Resolve locale from override, then system locale, then English fallback.
2. Resolve theme from override, then system appearance.
3. Render SCREEN-001 before network SDK readiness.
4. Let the user switch among the three root destinations.
5. Open SCREEN-011 from the top app bar and return to the previous root destination.

## Branches and resume

- Empty History renders its own empty state; it does not trigger onboarding.
- Locale/theme changes apply immediately and survive restart.
- Root destinations retain in-process scroll state.
- After process recreation, durable settings and history restore; an unfinished match does not.

## AC-001

Given a clean install with no saved results  
When the user launches BulbMatch  
Then SCREEN-001 appears in the system-resolved locale with Camera, Choose photo, and Enter manually actions, and no onboarding blocks interaction.

## AC-016

Given the system locale is unsupported or the user has chosen a locale override  
When the app starts or the override changes  
Then every app-owned string uses English fallback or the selected EN/RU locale immediately, while unit and base identifiers remain canonical.

## AC-017

Given light or dark system appearance and font scale up to 200%  
When the user navigates all root destinations with a screen reader  
Then content respects safe areas, keeps safety/actions reachable without horizontal scrolling, and announces destinations in visual order.
