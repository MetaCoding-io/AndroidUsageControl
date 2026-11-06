# S05: Hardening — Research

**Date:** 2026-11-05

## Summary

Three independent fixes. (1) Intermittent clean-build crash `NoClassDefFoundError Hilt_UsageMonitorService`: KSP sometimes emits no Hilt output for `:service:service-usage`. Toolchain is AGP 9.5.0 (built-in Kotlin), Kotlin 2.4.21, KSP 2.3.13, Hilt 2.60.1. service-usage applies `android.library`, `hilt`, `ksp` and uses `ksp(libs.hilt.compiler)`; `UsageMonitorService` is `@AndroidEntryPoint` (service/service-usage/.../UsageMonitorService.kt:32). The root cause is not proven yet. Likely suspects: stale KSP incremental caches or task ordering under the built-in Kotlin of AGP 9, or the Hilt aggregating task in `:app` running before the library's KSP output is ready. (2) Settings has `TopAppBar(title = { Text("Settings") })` with no navigationIcon (feature-settings/.../SettingsScreen.kt:42). MainActivity's shell TopAppBar only shows on tabs, so Settings has no back affordance. (3) The dark theme uses `DoormanTheme(darkTheme = isSystemInDarkTheme())` in core-ui Theme.kt:23. But `res/values/themes.xml` sets `Theme.Doorman` parent `android:Theme.Material.Light.NoActionBar` and there is no `values-night`, so the window background and system bars may flash or render light in dark mode.

## Recommendation

- **T01 (highest risk, first):** reproduce the crash with repeated `./gradlew clean :app:assembleDebug`. After each build, check that `service/service-usage/build/generated/ksp/debug/java/**/Hilt_UsageMonitorService.java` exists and that the class is in the APK dex. Bisect: build with `ksp.incremental=false` and check whether `:app:hiltJavaCompileDebug` or the KSP task of `:app` depends on `:service:service-usage:kspDebugKotlin`. Fix candidates, in order: add `ksp.incremental=false` (or the KSP2 setting) to gradle.properties, bump KSP or Hilt to compatible patch versions, and make the Hilt Gradle plugin `enableAggregatingTask` explicit. Record the root cause as a decision.
- **T02:** add an `onBack: () -> Unit` param to `SettingsScreen`, with a `navigationIcon` IconButton using `Icons.AutoMirrored.Filled.ArrowBack` and testTag `settings_back`. Wire it in MainActivity.kt:207 as `navController.popBackStack()`. Add an androidTest that checks gear → settings → back returns to Now.
- **T03:** change the `Theme.Doorman` parent to the DayNight equivalent (`android:Theme.Material.NoActionBar` in `values-night/themes.xml`, or `Theme.Material3.DayNight.NoActionBar` if the material dependency is present). Set a dark `windowBackground`. Verify on the emulator with `adb shell cmd uimode night yes` and a screenshot.

## Implementation Landscape

### Key Files
- `service/service-usage/build.gradle.kts`: the KSP/Hilt config for the failing module
- `gradle/libs.versions.toml`, `gradle.properties`: tool versions and KSP flags
- `app/build.gradle.kts`: Hilt aggregation in the app module
- `feature/feature-settings/src/main/java/com/usagecontrol/feature/settings/SettingsScreen.kt:26,42`: add onBack and the navigationIcon
- `app/src/main/java/com/usagecontrol/MainActivity.kt:207`: wire popBackStack
- `app/src/main/res/values/themes.xml`, new `values-night/themes.xml`: DayNight window theme
- `core/core-ui/src/main/java/com/usagecontrol/core/ui/theme/Theme.kt`: already follows the system setting

### Build Order
T01 crash investigation first, because it needs repeated builds and has the most unknowns. T02 and T03 are independent and small.

### Verification Approach
- Loop 3–5× `./gradlew clean :app:assembleDebug && unzip -l` / `apkanalyzer dex packages`, grepping for `Hilt_UsageMonitorService`, then install and launch without a crash in logcat.
- `./gradlew assembleDebug check :app:assembleDebugAndroidTest` plus the new settings-back instrumented test on Medium_Phone_API_36.1.
- `adb shell cmd uimode night yes`, launch, then `adb exec-out screencap` to check for a dark background and light status-bar icons.

## Open Risks
- The crash is intermittent, so a "fix" may be coincidental. Require N consecutive clean builds to pass, and confirm the root cause from the KSP logs and task graph.
<!-- gsd:state-version=178:1 -->
