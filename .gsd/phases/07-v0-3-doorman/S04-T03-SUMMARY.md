---
id: T03
parent: S04
milestone: M007
key_files:
  - core/core-common/src/main/java/com/usagecontrol/core/common/SoundProfileApplier.kt
  - service/service-context/src/main/java/com/usagecontrol/service/context/SoundProfileApplierImpl.kt
  - service/service-context/src/main/java/com/usagecontrol/service/context/ActionHandlersModule.kt
  - feature/feature-lockcontrol/src/main/java/com/usagecontrol/feature/lockcontrol/modes/ModesLogic.kt
  - feature/feature-lockcontrol/src/main/java/com/usagecontrol/feature/lockcontrol/modes/ModesViewModel.kt
  - feature/feature-lockcontrol/src/main/java/com/usagecontrol/feature/lockcontrol/modes/ModesScreen.kt
  - feature/feature-lockcontrol/src/test/java/com/usagecontrol/feature/lockcontrol/modes/ModesLogicTest.kt
  - app/src/main/java/com/usagecontrol/MainActivity.kt
  - core/core-common/build.gradle.kts
  - feature/feature-lockcontrol/build.gradle.kts
key_decisions:
  - SoundProfileApplierImpl pre-checks notification policy access (DND or SILENT presets) because SoundProfileExecutor swallows SecurityException; otherwise PermissionDenied could never be observed
  - core-common now depends on core-model so the bridge can take SoundPreset directly
  - Turning the active mode off reverts to the preset named Normal when present; active id is in-memory only (v1)
duration:
verification_result: passed
completed_at:
blocker_discovered: false
---

# T03: Added the core-common SoundProfileApplier bridge (sealed Applied/PermissionDenied/Error result, service-context impl logging under tag SoundProfileApplier) and a Modes tab of preset cards (modes_list, mode_toggle_<id>) with ContextStrip, a 7-day screen-time summary, default Focus/Sleep/Normal seeding and snackbars on failure; deleted PlaceholderScreens.kt

**Added the core-common SoundProfileApplier bridge (sealed Applied/PermissionDenied/Error result, service-context impl logging under tag SoundProfileApplier) and a Modes tab of preset cards (modes_list, mode_toggle_<id>) with ContextStrip, a 7-day screen-time summary, default Focus/Sleep/Normal seeding and snackbars on failure; deleted PlaceholderScreens.kt**

## What Happened

Bridge: added core/core-common/.../SoundProfileApplier.kt with suspend apply(SoundPreset): ApplyResult, a sealed interface with Applied, PermissionDenied and Error(message). core-common had no project deps, so I added implementation(project(":core:core-model")). There is no cycle because core-model has no project deps. SoundProfileApplierImpl in service-context delegates to SoundProfileExecutor.applyPreset on Dispatchers.Default. The executor swallows SecurityException per setting, so the impl checks NotificationManager.isNotificationPolicyAccessGranted up front when the preset touches DND or enters SILENT, and returns PermissionDenied without changing the device. It also catches SecurityException (→ PermissionDenied) and other exceptions (→ Error). Every call logs `preset=<id> outcome=applied|permission-denied|error` under tag SoundProfileApplier. The binding goes through a new Hilt @Binds SoundProfileApplierModule in ActionHandlersModule.kt. That file previously held only ActionHandlerRegistrar.

Modes (feature-lockcontrol/modes):
- ModesLogic is a pure, JVM-tested object. It holds the default MANUAL presets (Focus = vibrate with notifications at 0, Sleep = silent with DND on, Normal = ring 70/70 with DND off), toggle resolution, snackbar text and the screen-time summary. Turning a mode on applies it. Turning the active mode off reverts to the Normal preset if one exists; otherwise it just clears the active mark.
- ModesViewModel combines SoundPresetDao.getAllFlow() (seeding the defaults once when the table is empty) with in-memory activeId and applyingId and a 7-day UsageDao.getDailyTotals summary. activeId only changes when the result is Applied. Failure text goes out through a Channel-backed snackbar flow. Toggles are ignored while an apply is in flight. The v1 limitation (auto-applied TIME_RANGE/WIFI presets from SoundPresetApplier may override a manual pick) is documented in the ViewModel KDoc.
- ModesScreen is a Scaffold with a SnackbarHost and a LazyColumn tagged modes_list. It shows a ContextStrip (active mode plus today's screen time), a screen-time card (today and the 7-day average), and preset cards, each with a Switch tagged mode_toggle_<id>.

Wiring: MainActivity's MODES route now renders ModesScreen. I deleted PlaceholderScreens.kt and the coming_in_s04 string resource, which was now unused.

## Verification

Full `./gradlew assembleDebug check :app:assembleDebugAndroidTest` exited 0, including lint and all unit tests. ModesLogicTest ran 10 tests with 0 failures (from build/test-results XML). The modes_list and mode_toggle_ greps passed, and PlaceholderScreens.kt is gone. On-device toggle behaviour was not exercised; there is no emulator run in this unit.

## Verification Evidence

| # | Command | Exit Code | Verdict | Duration |
|---|---------|-----------|---------|----------|
| 1 | `./gradlew assembleDebug check :app:assembleDebugAndroidTest --console=plain -q` | 0 | pass | 3865ms |
| 2 | `grep -rq "modes_list" feature/feature-lockcontrol/src/main` | 0 | pass | 6ms |
| 3 | `grep -rq "mode_toggle_" feature/feature-lockcontrol/src/main` | 0 | pass | 6ms |
| 4 | `test ! -e app/src/main/java/com/usagecontrol/navigation/PlaceholderScreens.kt` | 0 | pass | 5ms |

## Deviations

- Added ModesLogic.kt and ModesLogicTest.kt, which were not in the plan's file list, to keep the rules JVM-testable.
- Added the core-model dependency to core-common and junit to feature-lockcontrol.
- Removed the now-unused coming_in_s04 string.
- Put the Hilt binding in a new module class inside ActionHandlersModule.kt; the file had no Hilt module before.

## Known Issues

- The active mode is not persisted and is not re-derived from device state; it resets when the ViewModel is recreated.
- Auto presets may override a manual pick (documented v1 limitation).
- The Normal preset sets DND off, so it also needs policy access and reports PermissionDenied without it.
- Toggle behaviour has not been checked on a device.

## Files Created/Modified

- `core/core-common/src/main/java/com/usagecontrol/core/common/SoundProfileApplier.kt`
- `service/service-context/src/main/java/com/usagecontrol/service/context/SoundProfileApplierImpl.kt`
- `service/service-context/src/main/java/com/usagecontrol/service/context/ActionHandlersModule.kt`
- `feature/feature-lockcontrol/src/main/java/com/usagecontrol/feature/lockcontrol/modes/ModesLogic.kt`
- `feature/feature-lockcontrol/src/main/java/com/usagecontrol/feature/lockcontrol/modes/ModesViewModel.kt`
- `feature/feature-lockcontrol/src/main/java/com/usagecontrol/feature/lockcontrol/modes/ModesScreen.kt`
- `feature/feature-lockcontrol/src/test/java/com/usagecontrol/feature/lockcontrol/modes/ModesLogicTest.kt`
- `app/src/main/java/com/usagecontrol/MainActivity.kt`
- `core/core-common/build.gradle.kts`
- `feature/feature-lockcontrol/build.gradle.kts`
<!-- gsd:state-version=161:1 -->
