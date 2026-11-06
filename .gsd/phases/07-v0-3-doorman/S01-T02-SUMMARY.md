---
id: T02
parent: S01
milestone: M007
key_files:
  - app/src/main/res/values/strings.xml
  - app/src/main/res/values/themes.xml
  - app/src/main/AndroidManifest.xml
  - app/src/main/res/mipmap-anydpi-v26/ic_launcher.xml
  - app/src/main/res/drawable/ic_launcher_foreground.xml
  - app/src/main/res/drawable/ic_launcher_background.xml
  - app/src/main/res/drawable/ic_launcher_monochrome.xml
  - feature/feature-onboarding/src/main/res/values/strings.xml
  - feature/feature-settings/src/main/res/values/strings.xml
  - service/service-usage/src/main/res/values/strings.xml
  - service/service-context/src/main/res/values/strings.xml
key_decisions:
  - Brand copy lives in per-module string resources with module-prefixed names (not core-ui) because services don't depend on core-ui and core-ui has no res; prefixes avoid resource-merge overrides
  - Icon palette: ink #0E1116 background, amber #FFB020 LED dot; T03 Color.kt should reuse these values
duration:
verification_result: passed
completed_at:
blocker_discovered: false
---

# T02: Renamed app to Doorman (label, Theme.Doorman, all Kotlin copy moved to per-module string resources) and added an LED-dot adaptive launcher icon with ink background and monochrome layer

**Renamed app to Doorman (label, Theme.Doorman, all Kotlin copy moved to per-module string resources) and added an LED-dot adaptive launcher icon with ink background and monochrome layer**

## What Happened

app_name is now "Doorman". Theme.UsageControl was renamed to Theme.Doorman in themes.xml and in all three manifest references. Added mipmap-anydpi-v26/ic_launcher.xml as an adaptive icon. Its background is a cool-ink #0E1116 vector, its foreground is an amber #FFB020 LED dot with two translucent glow rings and a highlight, all inside the 66dp safe zone, and its monochrome layer is a ring plus a dot. The legacy PNGs are kept as the pre-26 fallback. The manifest has no roundIcon attribute, so no ic_launcher_round was added. core-ui has no res dir and the services don't depend on core-ui, so each affected module got its own res/values/strings.xml with module-prefixed names to avoid manifest-merge collisions: onboarding_*, settings_app_name, usage_service_notification_title, context_service_notification_title. WelcomeScreen and SettingsScreen use stringResource. PermissionWalkthroughScreen uses %1$s format strings fed by onboarding_app_name. UsageMonitorService and ContextMonitorService use getString(R.string...). applicationId and packages are unchanged. The first assembleDebug failed in :service:service-context:kspDebugKotlin because Hilt couldn't resolve ContextSnapshotDao. The cause was a stale KSP incremental cache: a clean of that module fixed it, and the build has been green since.

## Verification

assembleDebug passes. The grep finds no "Usage Control" literal in Kotlin under app/core/feature/service. ./gradlew check (lint + unit tests) passes.

## Verification Evidence

| # | Command | Exit Code | Verdict | Duration |
|---|---------|-----------|---------|----------|
| 1 | `./gradlew assembleDebug` | 0 | pass | 9896ms |
| 2 | `! grep -rq --include=*.kt "Usage Control" app core feature service` | 0 | pass | 118ms |
| 3 | `./gradlew check` | 0 | pass | 19256ms |

## Deviations

Strings were added as module resources in onboarding, settings, service-usage and service-context instead of core-ui. The plan allowed either option.

## Known Issues

After resource changes, KSP incremental builds can report spurious "could not be resolved" Hilt errors. Cleaning the module, e.g. ./gradlew :service:service-context:clean, clears them. On-device rendering of the icon was not checked visually.

## Files Created/Modified

- `app/src/main/res/values/strings.xml`
- `app/src/main/res/values/themes.xml`
- `app/src/main/AndroidManifest.xml`
- `app/src/main/res/mipmap-anydpi-v26/ic_launcher.xml`
- `app/src/main/res/drawable/ic_launcher_foreground.xml`
- `app/src/main/res/drawable/ic_launcher_background.xml`
- `app/src/main/res/drawable/ic_launcher_monochrome.xml`
- `feature/feature-onboarding/src/main/res/values/strings.xml`
- `feature/feature-settings/src/main/res/values/strings.xml`
- `service/service-usage/src/main/res/values/strings.xml`
- `service/service-context/src/main/res/values/strings.xml`
<!-- gsd:state-version=161:1 -->
