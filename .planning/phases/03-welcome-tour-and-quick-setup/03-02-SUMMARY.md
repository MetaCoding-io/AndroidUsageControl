---
phase: 03-welcome-tour-and-quick-setup
plan: 02
subsystem: onboarding
tags: [onboarding, quick-setup, night-mode, settings, navigation, hilt]
dependency_graph:
  requires: [03-01]
  provides: [TOUR-03, TOUR-04]
  affects: [feature-onboarding, feature-settings, app-navigation]
tech_stack:
  added:
    - core-database dependency in feature-onboarding (for SoundPresetDao)
    - core-model dependency in feature-onboarding (for SoundPreset/RingerMode/PresetTrigger)
  patterns:
    - HiltViewModel injecting DAO for one-tap preset creation
    - Standalone tour route inside main nav graph (bottom bar auto-hides via AppScreen allowlist)
    - Default lambda parameter pattern for onRevisitTour to avoid breaking existing call sites
key_files:
  created:
    - feature/feature-onboarding/src/main/java/com/usagecontrol/feature/onboarding/QuickSetupViewModel.kt
    - feature/feature-onboarding/src/main/java/com/usagecontrol/feature/onboarding/QuickSetupScreen.kt
  modified:
    - feature/feature-onboarding/build.gradle.kts
    - app/src/main/java/com/usagecontrol/MainActivity.kt
    - feature/feature-settings/src/main/java/com/usagecontrol/feature/settings/SettingsScreen.kt
  deleted:
    - feature/feature-onboarding/src/main/java/com/usagecontrol/feature/onboarding/OnboardingPlaceholderScreen.kt
decisions:
  - "QuickSetupScreen does NOT write onboarding_complete to DataStore — PermissionWalkthroughViewModel already does that; QuickSetupScreen only optionally inserts Night Mode preset"
  - "Standalone tour route (tour/standalone) lives in main nav graph so bottom bar auto-hides via AppScreen allowlist — no extra code needed"
  - "onRevisitTour parameter defaults to empty lambda so existing SettingsScreen() call sites without parameter compile without change"
  - "OnboardingPlaceholderScreen.kt deleted — Phase 1 placeholder with no remaining usages"
metrics:
  duration: 2 min
  completed_date: "2026-02-28"
  tasks_completed: 2
  tasks_total: 3
  files_created: 2
  files_modified: 3
  files_deleted: 1
---

# Phase 3 Plan 02: Quick Setup and Settings Revisit Tour Summary

**One-liner:** One-tap Night Mode preset at onboarding end + "Revisit tour" Settings entry point using HiltViewModel DAO injection and standalone nav route.

## What Was Built

QuickSetupViewModel and QuickSetupScreen added as the final onboarding step after permission walkthrough. Users can enable a pre-configured Night Mode sound preset (silent 10 PM - 7 AM, DND on, alarms ring) or skip. The "Revisit tour" Settings item routes returning users to a standalone FeatureTourScreen without re-entering the full onboarding flow. The Phase 1 OnboardingPlaceholderScreen was deleted.

## Tasks Completed

| Task | Name | Commit | Files |
|------|------|--------|-------|
| 1 | QuickSetupViewModel and QuickSetupScreen | bd103d1 | build.gradle.kts, QuickSetupViewModel.kt, QuickSetupScreen.kt |
| 2 | Wire routes, Settings revisit tour, delete placeholder | fe9065b | MainActivity.kt, SettingsScreen.kt, OnboardingPlaceholderScreen.kt (deleted) |
| 3 | Human verification on device | (pending) | - |

## Navigation Route Chain

```
onboarding/welcome
  -> onboarding/tour
    -> onboarding/permissions
      -> onboarding/quicksetup  [NEW]
        -> main (popUpTo onboarding inclusive)

main/settings
  -> tour/standalone  [NEW]
    (popBackStack on onNext/onSkip)
```

## Key Decisions

1. **QuickSetupScreen does not write DataStore** — `PermissionWalkthroughViewModel.completeOnboarding()` already marks onboarding complete before navigating to quicksetup. QuickSetupScreen only inserts the optional Night Mode preset.

2. **Standalone tour in main nav graph** — `tour/standalone` lives inside the `main` nested navigation. Since `"tour/standalone"` is not in the `AppScreen` enum, `showBottomBar` evaluates to `false` automatically — no extra code needed.

3. **Default lambda for onRevisitTour** — `onRevisitTour: () -> Unit = {}` ensures the existing `SettingsScreen()` call site without the parameter still compiles.

4. **Placeholder deleted** — `OnboardingPlaceholderScreen.kt` was confirmed unreferenced in any other file and safely removed.

## Deviations from Plan

None — plan executed exactly as written. Build compiled without errors on first attempt.

## Self-Check

### Files created/modified:
- [x] `feature/feature-onboarding/build.gradle.kts` — has core-model and core-database deps
- [x] `QuickSetupViewModel.kt` — has `soundPresetDao.insert` and Night Mode preset
- [x] `QuickSetupScreen.kt` — has Enable Night Mode and Skip buttons
- [x] `MainActivity.kt` — has `onboarding/quicksetup`, `tour/standalone`, QuickSetupScreen, onRevisitTour wiring
- [x] `SettingsScreen.kt` — has onRevisitTour parameter and "Revisit tour" item
- [x] `OnboardingPlaceholderScreen.kt` — deleted

### Commits:
- [x] bd103d1 — feat(03-02): add QuickSetupViewModel and QuickSetupScreen
- [x] fe9065b — feat(03-02): wire quicksetup and standalone tour routes, add Settings revisit tour

### Build:
- [x] `:feature:feature-onboarding:compileDebugKotlin` — BUILD SUCCESSFUL
- [x] `:feature:feature-settings:compileDebugKotlin` — BUILD SUCCESSFUL
- [x] `:app:compileDebugKotlin` — BUILD SUCCESSFUL

## Self-Check: PASSED
