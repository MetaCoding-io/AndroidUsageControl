---
phase: 01-foundation
plan: 01
subsystem: infra
tags: [android, gradle, hilt, ksp, datastore, kotlin, coroutines]

# Dependency graph
requires: []
provides:
  - DataStore dependency wired into libs.versions.toml and core-common/app build files
  - PermissionChecker utility in core-common with AppOpsManager/NotificationManager/NotificationManagerCompat APIs
  - OnboardingDataStore preferencesDataStore delegate + OnboardingPreferencesKeys
  - OnboardingRepository with Flow<Boolean> isOnboardingComplete and suspend setOnboardingComplete()
  - DataStoreModule Hilt @Singleton provider in app module
  - feature-onboarding Gradle module (minimal, ready for screens in Plan 02)
affects: [01-02, onboarding, settings, permissions]

# Tech tracking
tech-stack:
  added:
    - androidx.datastore:datastore-preferences:1.1.1
    - com.google.dagger:hilt-android:2.50 (now in core-common)
    - com.google.devtools.ksp (now in core-common)
  patterns:
    - Hilt @Singleton DataStore provided from app module via DataStoreModule (one-instance guarantee)
    - preferencesDataStore delegate declared as Context extension in core-common; never called outside DI module
    - Special Android permissions checked via non-standard APIs (AppOpsManager, NotificationManagerCompat, NotificationManager.isNotificationPolicyAccessGranted) -- never checkSelfPermission

key-files:
  created:
    - core/core-common/src/main/java/com/usagecontrol/core/common/PermissionChecker.kt
    - core/core-common/src/main/java/com/usagecontrol/core/common/onboarding/OnboardingDataStore.kt
    - core/core-common/src/main/java/com/usagecontrol/core/common/onboarding/OnboardingRepository.kt
    - app/src/main/java/com/usagecontrol/di/DataStoreModule.kt
    - feature/feature-onboarding/build.gradle.kts
    - feature/feature-onboarding/src/main/AndroidManifest.xml
  modified:
    - gradle/libs.versions.toml
    - core/core-common/build.gradle.kts
    - settings.gradle.kts
    - app/build.gradle.kts

key-decisions:
  - "DataStore provided as @Singleton from app module via DataStoreModule; preferencesDataStore delegate in core-common called exactly once -- prevents multiple-instance corruption"
  - "PermissionChecker uses AppOpsManager.unsafeCheckOpNoThrow (minSdk 29+) for usage access -- checkSelfPermission always returns DENIED for PACKAGE_USAGE_STATS"
  - "feature-onboarding starts with minimal manifest and no source files; screens added in Plan 02"

patterns-established:
  - "Special permission checks: always use platform-specific APIs (AppOpsManager, NotificationManagerCompat, NotificationManager), never checkSelfPermission"
  - "DataStore singleton: declare preferencesDataStore delegate once in core module, provide via Hilt in app module"

requirements-completed: [ONBD-01, ONBD-04]

# Metrics
duration: 2min
completed: 2026-02-28
---

# Phase 1 Plan 01: Foundation — Gradle Infrastructure and Data Layer Summary

**DataStore + Hilt wired into core-common, PermissionChecker using AppOpsManager/NotificationManager APIs, OnboardingRepository with Flow<Boolean>, and feature-onboarding Gradle module scaffolded**

## Performance

- **Duration:** 2 min
- **Started:** 2026-02-28T19:49:15Z
- **Completed:** 2026-02-28T19:51:29Z
- **Tasks:** 2
- **Files modified:** 10

## Accomplishments

- DataStore 1.1.1 added to version catalog; core-common and app wired with Hilt + KSP + DataStore
- PermissionChecker created with three correct API-specific permission checks (no checkSelfPermission anti-pattern)
- OnboardingRepository exposes Flow<Boolean> with IOException recovery and suspend write via DataStore
- DataStoreModule provides single @Singleton DataStore instance in app module via Hilt
- feature-onboarding module scaffolded with build.gradle.kts (Compose + Hilt) and minimal AndroidManifest.xml

## Task Commits

Each task was committed atomically:

1. **Task 1: Add DataStore dependency and wire Gradle modules** - `440fb3b` (chore)
2. **Task 2: Create PermissionChecker, OnboardingDataStore, OnboardingRepository, DataStoreModule** - `5c23359` (feat)

**Plan metadata:** `8abcd8a` (docs: complete plan)

## Files Created/Modified

- `gradle/libs.versions.toml` - Added datastore version and library entry
- `core/core-common/build.gradle.kts` - Added Hilt, KSP plugins and DataStore/Hilt dependencies
- `settings.gradle.kts` - Added :feature:feature-onboarding include
- `app/build.gradle.kts` - Added feature-onboarding and datastore-preferences dependencies
- `feature/feature-onboarding/build.gradle.kts` - New feature module with Compose + Hilt setup
- `feature/feature-onboarding/src/main/AndroidManifest.xml` - Minimal manifest
- `core/core-common/src/main/java/com/usagecontrol/core/common/PermissionChecker.kt` - Three special-permission check methods + allPermissionsGranted()
- `core/core-common/src/main/java/com/usagecontrol/core/common/onboarding/OnboardingDataStore.kt` - preferencesDataStore delegate and OnboardingPreferencesKeys
- `core/core-common/src/main/java/com/usagecontrol/core/common/onboarding/OnboardingRepository.kt` - Flow<Boolean> read and suspend write for onboarding flag
- `app/src/main/java/com/usagecontrol/di/DataStoreModule.kt` - Singleton DataStore<Preferences> Hilt provision

## Decisions Made

- DataStore provided as @Singleton from app module; delegate declared in core-common but called only once via DI to prevent multiple-instance file corruption
- AppOpsManager.unsafeCheckOpNoThrow used for usage access (minSdk 29 guarantees availability); eliminates deprecated checkOpNoThrow fallback
- feature-onboarding module starts empty (only manifest); Plan 02 adds screens to keep this plan focused on infrastructure

## Deviations from Plan

None - plan executed exactly as written.

## Issues Encountered

None.

## User Setup Required

None - no external service configuration required.

## Next Phase Readiness

- core-common, app, and feature-onboarding all compile successfully
- PermissionChecker ready for use in onboarding screens (Plan 02) and settings
- OnboardingRepository injectable anywhere via Hilt; DataStore singleton guaranteed
- feature-onboarding module ready for UI code in Plan 02
- Concern (from STATE.md): verify no v0.1 code created a DataStore file with onboarding_complete = false (would break upgrader migration) — addressed at runtime by checking existing DataStore values in Plan 02 routing logic

---
*Phase: 01-foundation*
*Completed: 2026-02-28*
