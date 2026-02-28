---
phase: 02-permission-walkthrough
plan: 01
subsystem: ui
tags: [android, compose, hilt, viewmodel, permissions, onboarding, navigation]

# Dependency graph
requires:
  - phase: 01-foundation
    provides: PermissionChecker, OnboardingRepository, splash routing pattern, Hilt modules

provides:
  - PermissionStep enum with index mapping and TOTAL_STEPS constant
  - PermissionWalkthroughViewModel with recheckAll/advanceStep/autoAdvancePastGranted
  - PermissionWalkthroughScreen with ON_RESUME lifecycle observer, step counter, priming content
  - Settings intent helpers for Notification Listener, Usage Access, and DND
  - NavGraph wired to PermissionWalkthroughScreen (replaces OnboardingPlaceholderScreen)

affects:
  - 02-permission-walkthrough (plan 02 — degraded mode builds on this screen)
  - 03-welcome-tour (welcome tour prepended to this walkthrough)

# Tech tracking
tech-stack:
  added: []
  patterns:
    - ON_RESUME permission re-check via DisposableEffect + LifecycleEventObserver
    - NLS requestRebind called on Notification Access grant transition (AOSP bug workaround)
    - Settings intents with try/catch fallback to ACTION_APPLICATION_DETAILS_SETTINGS
    - ViewModel writes DataStore and sets isComplete; composable observes isComplete via LaunchedEffect

key-files:
  created:
    - feature/feature-onboarding/src/main/java/com/usagecontrol/feature/onboarding/PermissionStep.kt
    - feature/feature-onboarding/src/main/java/com/usagecontrol/feature/onboarding/PermissionWalkthroughViewModel.kt
    - feature/feature-onboarding/src/main/java/com/usagecontrol/feature/onboarding/PermissionWalkthroughScreen.kt
  modified:
    - app/src/main/java/com/usagecontrol/MainActivity.kt

key-decisions:
  - "LocalLifecycleOwner lives in androidx.compose.ui.platform (not androidx.lifecycle.compose) in lifecycle-runtime-compose 2.7.0"
  - "PermissionWalkthroughViewModel writes onboarding complete to DataStore; MainActivity onComplete lambda only navigates (no double-write)"
  - "Settings intent helpers are private extension functions in Screen file -- only used there and require Activity Context"
  - "autoAdvancePastGranted() on init skips already-granted steps for kill+relaunch mid-walkthrough correctness"
  - "Only Continue button shown when permission is granted (no Skip when already granted)"

patterns-established:
  - "ON_RESUME re-check: DisposableEffect(lifecycleOwner) + LifecycleEventObserver pattern for returning-from-Settings detection"
  - "String ComponentName for cross-module service reference: avoids importing service-notification into feature-onboarding"
  - "Settings intent with fallback: wrap startActivity in try/catch with ACTION_APPLICATION_DETAILS_SETTINGS fallback"

requirements-completed: [PERM-01, PERM-02, PERM-03, PERM-04]

# Metrics
duration: 3min
completed: 2026-02-28
---

# Phase 2 Plan 01: Permission Walkthrough Summary

**Three-step permission walkthrough composable with ON_RESUME grant detection, NLS requestRebind, and step counter replacing OnboardingPlaceholderScreen in the NavGraph**

## Performance

- **Duration:** 3 min
- **Started:** 2026-02-28T20:20:02Z
- **Completed:** 2026-02-28T20:22:30Z
- **Tasks:** 2
- **Files modified:** 4

## Accomplishments

- PermissionStep enum and PermissionWalkthroughViewModel providing step state management with NLS rebind workaround
- PermissionWalkthroughScreen with priming content (title/explanation/hint) per step, ON_RESUME re-check, green checkmark on grant, and Settings launch
- App NavGraph updated: OnboardingPlaceholderScreen replaced by PermissionWalkthroughScreen, markOnboardingComplete() double-write eliminated

## Task Commits

Each task was committed atomically:

1. **Task 1: Create PermissionStep enum and PermissionWalkthroughViewModel** - `f354777` (feat)
2. **Task 2: Create PermissionWalkthroughScreen and wire into NavGraph** - `b6a3f40` (feat)

## Files Created/Modified

- `feature/feature-onboarding/src/main/java/com/usagecontrol/feature/onboarding/PermissionStep.kt` - Enum of 3 permission steps with index mapping and TOTAL_STEPS constant
- `feature/feature-onboarding/src/main/java/com/usagecontrol/feature/onboarding/PermissionWalkthroughViewModel.kt` - State (currentStep, permissionStates, isComplete), recheckAll with NLS rebind, advanceStep, autoAdvancePastGranted
- `feature/feature-onboarding/src/main/java/com/usagecontrol/feature/onboarding/PermissionWalkthroughScreen.kt` - Full 3-step walkthrough composable with lifecycle observer, step counter, priming UI, Settings helpers
- `app/src/main/java/com/usagecontrol/MainActivity.kt` - Replaced OnboardingPlaceholderScreen with PermissionWalkthroughScreen; removed markOnboardingComplete() from onComplete lambda

## Decisions Made

- ViewModel handles DataStore write for onboarding completion; MainActivity onComplete lambda is navigation-only to avoid double-write
- Settings intent helpers are private extension functions in PermissionWalkthroughScreen.kt — they require Activity Context and are only used in this file
- ComponentName for NLS service is passed as string to avoid importing service-notification module into feature-onboarding
- autoAdvancePastGranted() on ViewModel init ensures correct behavior when app is killed and relaunched mid-walkthrough
- Only Continue button shown when permission is already granted (no Skip offered when already granted per plan)

## Deviations from Plan

### Auto-fixed Issues

**1. [Rule 1 - Bug] Fixed incorrect LocalLifecycleOwner import package**

- **Found during:** Task 2 (PermissionWalkthroughScreen compilation)
- **Issue:** Plan specified `import androidx.lifecycle.compose.LocalLifecycleOwner` but in lifecycle-runtime-compose 2.7.0, `LocalLifecycleOwner` is in `androidx.compose.ui.platform`, not `androidx.lifecycle.compose`. Build failed with "Unresolved reference: LocalLifecycleOwner".
- **Fix:** Changed import to `import androidx.compose.ui.platform.LocalLifecycleOwner`
- **Files modified:** `feature/feature-onboarding/src/main/java/com/usagecontrol/feature/onboarding/PermissionWalkthroughScreen.kt`
- **Verification:** `./gradlew :app:compileDebugKotlin` succeeded after fix
- **Committed in:** `b6a3f40` (Task 2 commit)

---

**Total deviations:** 1 auto-fixed (1 bug — wrong import package)
**Impact on plan:** Auto-fix necessary for compilation. No scope creep.

## Issues Encountered

None beyond the auto-fixed import issue above.

## User Setup Required

None - no external service configuration required.

## Next Phase Readiness

- Permission walkthrough screen is fully functional and wired into the NavGraph
- Phase 2 Plan 02 can build degraded mode (banner/warning when permissions skipped) on top of this PermissionWalkthroughScreen
- Phase 3 welcome tour can prepend welcome steps before the walkthrough
- Physical device test required: NLS requestRebind behavior (noted as blocker in STATE.md — AOSP bug #226411020)
- Physical device test required: DND and Usage Access Settings navigation on different OEM ROMs

---
*Phase: 02-permission-walkthrough*
*Completed: 2026-02-28*

## Self-Check: PASSED

- FOUND: feature/feature-onboarding/.../PermissionStep.kt
- FOUND: feature/feature-onboarding/.../PermissionWalkthroughViewModel.kt
- FOUND: feature/feature-onboarding/.../PermissionWalkthroughScreen.kt
- FOUND: .planning/phases/02-permission-walkthrough/02-01-SUMMARY.md
- FOUND commit: f354777 (Task 1)
- FOUND commit: b6a3f40 (Task 2)
