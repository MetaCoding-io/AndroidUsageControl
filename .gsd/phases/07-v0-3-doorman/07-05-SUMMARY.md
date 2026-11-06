---
id: S05
parent: M007
milestone: M007
provides:
  - (none)
requires:
  []
affects:
  []
key_files:
  - build.gradle.kts
  - gradle.properties
  - feature/feature-settings/src/main/java/com/usagecontrol/feature/settings/SettingsScreen.kt
  - app/src/main/java/com/usagecontrol/MainActivity.kt
  - app/src/androidTest/java/com/usagecontrol/SettingsBackTest.kt
  - app/src/main/res/values-night/themes.xml
key_decisions:
  - Fail build at KSP when an @AndroidEntryPoint lacks a Hilt_ class
  - onBack required param on SettingsScreen
  - Window background via qualified color resource
patterns_established:
  - (none)
observability_surfaces:
  - Gradle guard error 'produced no Hilt_ class for <X>' at :<module>:kspDebugKotlin
  - logcat AndroidRuntime NoClassDefFoundError
drill_down_paths:
  - .gsd/phases/07-v0-3-doorman/S05-T01-SUMMARY.md
  - .gsd/phases/07-v0-3-doorman/S05-T02-SUMMARY.md
  - .gsd/phases/07-v0-3-doorman/S05-T03-SUMMARY.md
duration: ""
verification_result: passed
completed_at: 2026-11-06T01:43:03.778Z
blocker_discovered: false
---

# S05: Hardening: Hilt/KSP clean-build crash, Settings back arrow, dark mode check

**Build-time guard plus ksp.incremental=false against missing Hilt_ classes, a Settings back arrow wired to popBackStack with an instrumented test, and a values-night Theme.Doorman so the window and system bars follow dark mode**

## What Happened

T01: the NoClassDefFoundError for Hilt_UsageMonitorService never reproduced (0 failures in 11 builds). Two changes went in anyway: ksp.incremental=false and explicit Hilt aggregation. A root Gradle guard was also added. It fails kspDebugKotlin when an @AndroidEntryPoint has no generated Hilt_ class, so a bad APK can no longer ship. The root cause is inferred, not proven. 5/5 clean builds contained the class.

T02: SettingsScreen now takes a required onBack parameter. A TopAppBar navigationIcon (testTag settings_back) calls it, and MainActivity wires it to navController.popBackStack(). SettingsBackTest passed on Medium_Phone_API_36.1.

T03: values-night/themes.xml now gives Theme.Doorman a dark Material parent, an Ink window background and light bar icons. The light theme is now explicit with Paper. The window colour lives in one qualified color resource, doorman_window_background. Emulator screenshots confirmed both modes.

## Verification

gsd_exec ff2d08af: clean assembleDebug check :app:assembleDebugAndroidTest exited 0, Hilt_UsageMonitorService.java is present in the KSP output, values-night theme exists, and the settings_back grep passed. Task-level evidence: 5/5 clean builds had the class in the APK dex; SettingsBackTest gave tests=1 failures=0; dark/light screenshots checked; logcat showed no crash.

## Requirements Advanced

None.

## Requirements Validated

None.

## New Requirements Surfaced

None.

## Requirements Invalidated or Re-scoped

None.

## Operational Readiness

None.

## Deviations

T01 could not bisect because the crash never reproduced. The build guard was added beyond the plan. T02 added androidTest dependencies. T03 added colors.xml overrides.

## Known Limitations

Root cause of the Hilt/KSP crash is inferred, not proven. ksp.incremental=false slows incremental builds slightly.

## Follow-ups

If core-ui Paper/Ink colours change, update doorman_window_background in values and values-night to match.

## Files Created/Modified

None.
<!-- gsd:state-version=178:1 -->
