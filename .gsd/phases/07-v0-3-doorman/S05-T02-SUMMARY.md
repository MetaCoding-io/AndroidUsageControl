---
id: T02
parent: S05
milestone: M007
key_files:
  - feature/feature-settings/src/main/java/com/usagecontrol/feature/settings/SettingsScreen.kt
  - app/src/main/java/com/usagecontrol/MainActivity.kt
  - app/src/androidTest/java/com/usagecontrol/SettingsBackTest.kt
  - app/build.gradle.kts
key_decisions:
  - onBack is a required parameter with no default, so any new caller must wire up back navigation explicitly
  - App-level UI tests skip onboarding by writing ONBOARDING_COMPLETE to the real onboardingDataStore before calling ActivityScenario.launch, instead of using Hilt test replacement
duration:
verification_result: passed
completed_at:
blocker_discovered: false
---

# T02: Added a Settings TopAppBar back arrow (testTag settings_back) wired to navController.popBackStack(), plus SettingsBackTest that verifies gear → Settings → back returns to the selected Now tab

**Added a Settings TopAppBar back arrow (testTag settings_back) wired to navController.popBackStack(), plus SettingsBackTest that verifies gear → Settings → back returns to the selected Now tab**

## What Happened

SettingsScreen now takes a required `onBack: () -> Unit` and renders a navigationIcon IconButton (Icons.AutoMirrored.Filled.ArrowBack, contentDescription "Back", Modifier.testTag("settings_back")). MainActivity's settings route passes `onBack = { navController.popBackStack() }`. MainActivity was the only caller and there were no previews to update. I added app/src/androidTest/java/com/usagecontrol/SettingsBackTest.kt. It sets onboarding_complete=true through the real `context.onboardingDataStore` so MainViewModel starts on "main", then launches MainActivity with ActivityScenario and uses createEmptyComposeRule. It waits for settings_gear and clicks it, then checks that settings_back is shown and the shell bar (gear) is gone. After clicking back it checks that settings_back is gone, the gear is back, and nav_tab_now is displayed and selected. For this to work, app androidTest needed new dependencies: datastore-preferences (core-common only has it as an implementation dependency), the compose BOM, and ui-test-junit4. ui-test-manifest was added as debugImplementation. NotificationActionsTest isn't a Compose test, so I used SwipeArchiveUndoTest's Compose test setup, and copied the KDoc style and AndroidJUnit4 runner from NotificationActionsTest.

## Verification

assembleDebug and :app:assembleDebugAndroidTest succeeded. The grep for settings_back passed. connectedDebugAndroidTest filtered to SettingsBackTest ran on Medium_Phone_API_36.1 (AVD) and passed: the result XML shows tests=1 failures=0 for backArrow_returnsFromSettingsToNowTab.

## Verification Evidence

| # | Command | Exit Code | Verdict | Duration |
|---|---------|-----------|---------|----------|
| 1 | `./gradlew assembleDebug :app:assembleDebugAndroidTest` | 0 | pass | 3658ms |
| 2 | `grep -q "settings_back" feature/feature-settings/src/main/java/com/usagecontrol/feature/settings/SettingsScreen.kt` | 0 | pass | 10ms |
| 3 | `./gradlew :app:connectedDebugAndroidTest -Pandroid.testInstrumentationRunnerArguments.class=com.usagecontrol.SettingsBackTest` | 0 | pass | 12803ms |
| 4 | `grep -ho 'tests="[0-9]*" failures="[0-9]*"\|testcase name="[^"]*"' app/build/outputs/androidTest-results/connected/debug/*.xml` | 0 | pass | 8ms |

## Deviations

Added androidTest/debug dependencies to app/build.gradle.kts (compose BOM, ui-test-junit4, ui-test-manifest, datastore-preferences). They weren't listed in the task files but the Compose-based test needs them. NotificationActionsTest isn't a Compose test, so the Compose test setup came from SwipeArchiveUndoTest.

## Known Issues

SettingsBackTest leaves onboarding_complete=true on the device after it runs. That's harmless for later test runs.

## Files Created/Modified

- `feature/feature-settings/src/main/java/com/usagecontrol/feature/settings/SettingsScreen.kt`
- `app/src/main/java/com/usagecontrol/MainActivity.kt`
- `app/src/androidTest/java/com/usagecontrol/SettingsBackTest.kt`
- `app/build.gradle.kts`
<!-- gsd:state-version=186:1 -->
