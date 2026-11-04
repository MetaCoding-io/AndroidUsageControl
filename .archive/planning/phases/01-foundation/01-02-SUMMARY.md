---
phase: 01-foundation
plan: 02
subsystem: navigation
tags: [android, compose, navigation, hilt, viewmodel, onboarding, routing]

# Dependency graph
requires:
  - OnboardingRepository (core-common, Plan 01)
  - PermissionChecker (core-common, Plan 01)
  - feature-onboarding Gradle module (Plan 01)
provides:
  - MainViewModel with startDestination StateFlow and markOnboardingComplete()
  - OnboardingPlaceholderScreen with Skip/Continue button
  - Nested NavHost with dynamic startDestination (onboarding vs main graph)
  - v0.1 upgrader auto-skip logic (all permissions granted => skip onboarding)
affects: [02-permissions, onboarding, back-stack, bottom-nav]

# Tech tracking
tech-stack:
  added: []
  patterns:
    - Splash routing pattern: startDestination null until ViewModel resolves, renders empty Box to prevent flicker
    - Nested navigation graphs: onboarding graph (route=onboarding) + main graph (route=main)
    - popUpTo("onboarding") { inclusive = true } on onboarding completion cleans back stack
    - Bottom bar exclusion: AppScreen enum routes naturally exclude onboarding routes (no extra logic needed)

key-files:
  created:
    - app/src/main/java/com/usagecontrol/MainViewModel.kt
    - feature/feature-onboarding/src/main/java/com/usagecontrol/feature/onboarding/OnboardingPlaceholderScreen.kt
  modified:
    - app/src/main/java/com/usagecontrol/MainActivity.kt

key-decisions:
  - "Splash routing pattern: startDestination is null until DataStore coroutine resolves, empty Box prevents NavHost from flashing wrong destination"
  - "Nested navigation graphs (onboarding/main) over flat routing: back press from main exits app, never returns to onboarding; clean separation of concerns"
  - "markOnboardingComplete() called before navigate() in onComplete lambda: DataStore write happens even if navigation is fast (fire-and-forget via viewModelScope)"

patterns-established:
  - "Dynamic NavHost startDestination: read from StateFlow, null-guard with empty Box, unwrap with !! only after null check"
  - "Onboarding clean-exit: popUpTo graph route inclusive=true removes entire onboarding graph from back stack on completion or skip"

requirements-completed: [ONBD-01, ONBD-02, ONBD-03, ONBD-04]

# Metrics
duration: 3min
completed: 2026-02-28
---

# Phase 1 Plan 02: Navigation Routing Spine Summary

**Nested NavHost with MainViewModel splash routing, OnboardingPlaceholderScreen with Skip button, dynamic startDestination from DataStore — fresh install routes to onboarding, returning user goes straight to main app, v0.1 upgrader auto-skips if all permissions granted**

## Performance

- **Duration:** 3 min
- **Started:** 2026-02-28T20:14:56Z
- **Completed:** 2026-02-28T20:17:39Z
- **Tasks:** 3 of 3 (including human-verify checkpoint, approved)
- **Files modified:** 3

## Accomplishments

- MainViewModel created with StateFlow<String?> startDestination, null until DataStore read completes
- v0.1 upgrader logic: if onboarding not complete AND all permissions granted, write DataStore flag and route to main
- markOnboardingComplete() exposed for use by Skip and normal completion paths (identical code path, ONBD-03)
- OnboardingPlaceholderScreen with Surface, centered Column, headlineMedium title, bodyMedium description, Skip/Continue button
- MainActivity restructured: UsageControlAppContent now package-level (not private), accepts MainViewModel via hiltViewModel()
- Empty Box rendered while startDestination==null prevents NavHost flicker
- Nested nav graphs: "onboarding" graph with "onboarding/start" composable; "main" graph with all 5 tabs + rules routes
- popUpTo("onboarding") { inclusive = true } in onComplete removes onboarding from back stack
- Bottom bar logic unchanged — AppScreen enum routes do not include onboarding routes, so exclusion is automatic
- Full assembleDebug succeeds; verification checks pass (no checkSelfPermission in source, onboardingDataStore exactly 2 source hits)

## Task Commits

Each task was committed atomically:

1. **Task 1: Create MainViewModel and OnboardingPlaceholderScreen** - `7cbe9f2` (feat)
2. **Task 2: Restructure MainActivity with nested nav graphs and dynamic startDestination** - `1f3b31b` (feat)

3. **Task 3: Verify onboarding routing on device** - human-verify checkpoint, **approved** by user

## Files Created/Modified

- `app/src/main/java/com/usagecontrol/MainViewModel.kt` - New: routing logic, DataStore read, v0.1 upgrader auto-skip
- `feature/feature-onboarding/src/main/java/com/usagecontrol/feature/onboarding/OnboardingPlaceholderScreen.kt` - New: placeholder screen with Skip/Continue button
- `app/src/main/java/com/usagecontrol/MainActivity.kt` - Restructured: nested nav graphs, dynamic startDestination, hiltViewModel injection

## Decisions Made

- Splash routing pattern chosen: empty Box shown during DataStore read to prevent NavHost rendering wrong start destination
- Nested navigation graphs preserve back-stack integrity: popUpTo("onboarding") { inclusive = true } means back press from main tab exits app, never returns to onboarding
- markOnboardingComplete() fires before navigate() in the onComplete lambda; DataStore write is fire-and-forget via viewModelScope

## Deviations from Plan

None - plan executed exactly as written.

## Issues Encountered

None.

## User Setup Required

None - no external service configuration required.

## Next Phase Readiness

- Routing spine is complete and device-verified; Phase 2 can replace OnboardingPlaceholderScreen with real permission walkthrough screens without touching MainViewModel or MainActivity navigation logic
- Phase 3 can prepend welcome/tour screens by adding composable routes to the onboarding nested graph
- All four ONBD requirements (ONBD-01 through ONBD-04) fully verified on device: fresh install shows onboarding, skip navigates to main with clean back stack, second launch goes directly to main app
- Remaining concern: verify no v0.1 build wrote onboarding_complete=false to DataStore (would prevent upgrader auto-skip) — low probability, validate during Phase 2 device testing
- NLS rebind bug (AOSP #226411020) must still be verified on physical device after Notification Access grant in Phase 2

---
*Phase: 01-foundation*
*Completed: 2026-02-28*
