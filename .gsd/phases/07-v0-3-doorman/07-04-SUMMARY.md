---
id: S04
parent: M007
milestone: M007
provides:
  - (none)
requires:
  []
affects:
  []
key_files:
  - core/core-common/src/main/java/com/usagecontrol/core/common/SoundProfileApplier.kt
  - service/service-context/src/main/java/com/usagecontrol/service/context/SoundProfileApplierImpl.kt
  - feature/feature-notifications/src/main/java/com/usagecontrol/feature/notifications/apps/AppStats.kt
  - feature/feature-lockcontrol/src/main/java/com/usagecontrol/feature/lockcontrol/modes/ModesScreen.kt
  - app/src/main/java/com/usagecontrol/MainActivity.kt
key_decisions: []
patterns_established:
  - (none)
observability_surfaces:
  - logcat tag SoundProfileApplier: preset id plus outcome applied/permission-denied/error
  - Modes snackbar on PermissionDenied/Error
drill_down_paths:
  []
duration: ""
verification_result: passed
completed_at: 2026-11-04T06:57:34.588Z
blocker_discovered: false
---

# S04: Apps and Modes v1

**Real Apps list (sorted by alerts/day, sparklines, dismiss hint), app profile (ring, median time to act, hourly bars, channels, screen time, limit, suggestions), and Modes preset cards that apply sound profiles via a core-common SoundProfileApplier bridge; placeholders removed.**

## What Happened

T01 added per-app Flow aggregate queries to NotificationDao and a pure AppStats object with JVM tests. T02 built AppsScreen/AppsViewModel and AppProfileScreen/AppProfileViewModel, added the Sparkline chart and the AppIcon composable, kept sorting and limit-rule logic in AppsLogic (tested), and wired the APPS and apps/{packageName} routes. T03 added the SoundProfileApplier interface in core-common (sealed ApplyResult), implemented it in service-context as SoundProfileApplierImpl (logcat tag SoundProfileApplier) and bound it in ActionHandlersModule. It also built ModesScreen/ModesViewModel in feature-lockcontrol, which seeds Focus/Sleep/Normal MANUAL presets, routed MODES to ModesScreen, and deleted PlaceholderScreens.kt.

## Verification

gsd_exec 77633033: ./gradlew assembleDebug check :app:assembleDebugAndroidTest exit 0. The testTags apps_list, app_profile, modes_list and mode_toggle_ are all present. PlaceholderScreens.kt is absent. The only feature-to-service dependency, feature-settings to service-notification, dates from commit 123e096 (S02), so S04 added none. The SoundProfileApplier bridge files are present. The on-device ringer change is still pending human UAT.

## Requirements Advanced

- R013 — APPS-01/02 and MODE-01 screens built and wired; awaiting on-device UAT

## Requirements Validated

None.

## New Requirements Surfaced

None.

## Requirements Invalidated or Re-scoped

None.

## Operational Readiness

None.

## Deviations

None.

## Known Limitations

Auto-applied (non-MANUAL) presets may override a manual mode pick. The screens and the DAO instrumented tests have not run on a device. Profile 'today' does not roll over at midnight while the screen stays open.

## Follow-ups

None.

## Files Created/Modified

None.
<!-- gsd:state-version=156:1 -->
