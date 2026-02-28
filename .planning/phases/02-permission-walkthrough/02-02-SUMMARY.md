---
phase: 02-permission-walkthrough
plan: "02"
subsystem: ui
tags: [android, compose, material3, permissions, degraded-mode, hilt]

# Dependency graph
requires:
  - phase: 02-permission-walkthrough
    provides: PermissionChecker in core-common; PermissionWalkthroughScreen built in plan 02-01
  - phase: 01-foundation
    provides: core-common, core-ui, core-database, Hilt DI wiring

provides:
  - DegradedPermissionBanner composable in core-ui/CommonComponents.kt (Material3 errorContainer)
  - showPermissionBanner StateFlow + recheckPermission() in NotificationsViewModel, ScreenTimeViewModel, LockControlViewModel
  - ON_RESUME banner re-check via DisposableEffect in all three feature screens

affects:
  - feature/feature-notifications
  - feature/feature-screentime
  - feature/feature-lockcontrol
  - core/core-ui

# Tech tracking
tech-stack:
  added: []
  patterns:
    - "DegradedPermissionBanner shared composable in core-ui for permission warning reuse across feature modules"
    - "showPermissionBanner StateFlow + recheckPermission() pattern for reactive permission state in HiltViewModels"
    - "DisposableEffect(lifecycleOwner) with LifecycleEventObserver for ON_RESUME re-check — same pattern as PermissionWalkthroughScreen"

key-files:
  created: []
  modified:
    - core/core-ui/src/main/java/com/usagecontrol/core/ui/component/CommonComponents.kt
    - feature/feature-notifications/src/main/java/com/usagecontrol/feature/notifications/NotificationsViewModel.kt
    - feature/feature-notifications/src/main/java/com/usagecontrol/feature/notifications/NotificationsScreen.kt
    - feature/feature-screentime/src/main/java/com/usagecontrol/feature/screentime/ScreenTimeViewModel.kt
    - feature/feature-screentime/src/main/java/com/usagecontrol/feature/screentime/ScreenTimeScreen.kt
    - feature/feature-lockcontrol/src/main/java/com/usagecontrol/feature/lockcontrol/LockControlViewModel.kt
    - feature/feature-lockcontrol/src/main/java/com/usagecontrol/feature/lockcontrol/LockControlScreen.kt

key-decisions:
  - "DegradedPermissionBanner takes only message: String — feature-specific text stays in feature modules, not core-ui (anti-pattern avoidance)"
  - "No dismiss button on banner — it persists as long as permission is missing (dismissing hides a real problem)"
  - "recheckPermission() calls PermissionChecker synchronously on main thread — all three check methods are synchronous system API calls with negligible latency"
  - "LocalLifecycleOwner imported from androidx.compose.ui.platform (not androidx.lifecycle.compose) — consistent with PermissionWalkthroughScreen decision from 02-01"

patterns-established:
  - "Permission banner pattern: ViewModel holds showPermissionBanner StateFlow + recheckPermission(); Screen adds DisposableEffect for ON_RESUME; banner shown conditionally after TopAppBar"

requirements-completed: [PERM-05]

# Metrics
duration: 4min
completed: 2026-02-28
---

# Phase 2 Plan 02: Degraded Mode Permission Banners Summary

**DegradedPermissionBanner composable (Material3 errorContainer) with PermissionChecker injection and ON_RESUME re-check in all three feature ViewModels and Screens**

## Performance

- **Duration:** 4 min
- **Started:** 2026-02-28T20:25:17Z
- **Completed:** 2026-02-28T20:29:00Z
- **Tasks:** 1 of 2 auto-executed (Task 2 is human-verify checkpoint)
- **Files modified:** 7

## Accomplishments
- Added shared `DegradedPermissionBanner` composable to `core-ui/CommonComponents.kt` using Material3 `errorContainer`/`onErrorContainer` colors
- Injected `PermissionChecker` into all three feature ViewModels and exposed `showPermissionBanner: StateFlow<Boolean>` + `recheckPermission()` in each
- Integrated banners and ON_RESUME lifecycle observers into `NotificationsScreen`, `ScreenTimeScreen`, and `LockControlScreen`

## Task Commits

Each task was committed atomically:

1. **Task 1: Add DegradedPermissionBanner to core-ui and integrate into three feature screens** - `ca16867` (feat)

**Plan metadata:** (pending — created after checkpoint)

## Files Created/Modified
- `core/core-ui/src/main/java/com/usagecontrol/core/ui/component/CommonComponents.kt` - Added DegradedPermissionBanner composable
- `feature/feature-notifications/src/main/java/com/usagecontrol/feature/notifications/NotificationsViewModel.kt` - Added PermissionChecker injection and showPermissionBanner StateFlow
- `feature/feature-notifications/src/main/java/com/usagecontrol/feature/notifications/NotificationsScreen.kt` - Added banner, ON_RESUME DisposableEffect
- `feature/feature-screentime/src/main/java/com/usagecontrol/feature/screentime/ScreenTimeViewModel.kt` - Added PermissionChecker injection and showPermissionBanner StateFlow
- `feature/feature-screentime/src/main/java/com/usagecontrol/feature/screentime/ScreenTimeScreen.kt` - Added banner, ON_RESUME DisposableEffect
- `feature/feature-lockcontrol/src/main/java/com/usagecontrol/feature/lockcontrol/LockControlViewModel.kt` - Added PermissionChecker injection and showPermissionBanner StateFlow
- `feature/feature-lockcontrol/src/main/java/com/usagecontrol/feature/lockcontrol/LockControlScreen.kt` - Added banner, ON_RESUME DisposableEffect

## Decisions Made
- DegradedPermissionBanner accepts only `message: String` — feature-specific text stays in feature modules (anti-pattern avoidance)
- No dismiss button on banner — it must persist as long as permission is missing (hiding it would hide a real problem)
- `recheckPermission()` is synchronous — PermissionChecker methods are system API calls with negligible latency, no coroutine needed
- `LocalLifecycleOwner` imported from `androidx.compose.ui.platform` — consistent with the decision established in plan 02-01

## Deviations from Plan

None - plan executed exactly as written.

## Issues Encountered

None.

## User Setup Required

None - no external service configuration required.

## Next Phase Readiness
- Task 2 (human-verify checkpoint) requires device/emulator testing to validate full Phase 2 flow
- After checkpoint approval: Phase 2 is complete, Phase 3 can begin
- Concerns from STATE.md still pending: NLS rebind bug verification on physical device

---
*Phase: 02-permission-walkthrough*
*Completed: 2026-02-28*
