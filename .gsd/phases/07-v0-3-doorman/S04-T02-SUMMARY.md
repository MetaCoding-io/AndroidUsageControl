---
id: T02
parent: S04
milestone: M007
key_files:
  - core/core-ui/src/main/java/com/usagecontrol/core/ui/component/Charts.kt
  - core/core-ui/src/main/java/com/usagecontrol/core/ui/component/ComponentGallery.kt
  - feature/feature-notifications/src/main/java/com/usagecontrol/feature/notifications/NotificationIcon.kt
  - feature/feature-notifications/src/main/java/com/usagecontrol/feature/notifications/apps/AppsLogic.kt
  - feature/feature-notifications/src/main/java/com/usagecontrol/feature/notifications/apps/AppsViewModel.kt
  - feature/feature-notifications/src/main/java/com/usagecontrol/feature/notifications/apps/AppsScreen.kt
  - feature/feature-notifications/src/main/java/com/usagecontrol/feature/notifications/apps/AppProfileViewModel.kt
  - feature/feature-notifications/src/main/java/com/usagecontrol/feature/notifications/apps/AppProfileScreen.kt
  - feature/feature-notifications/src/test/java/com/usagecontrol/feature/notifications/apps/AppsLogicTest.kt
  - app/src/main/java/com/usagecontrol/MainActivity.kt
  - app/src/main/java/com/usagecontrol/navigation/AppNavigation.kt
key_decisions:
  - 'Acted' in the profile ring = total minus user-dismissed (opened, snoozed or left), because AppEventRow carries no dismissal flag
  - Apps list window is 7 local days; profile window is 30 local days so the median time to act has enough samples
  - The app's limit rule is the newest SCREEN_TIME rule with an APP_PACKAGE condition for the package and a positive dailyLimitMinutes; setLimit updates it in place rather than adding duplicates
  - Added AppIcon(packageName) next to NotificationIcon, since list rows have no NotificationRecord
duration:
verification_result: passed
completed_at:
blocker_discovered: false
---

# T02: Built the Apps tab (rows sorted by alerts/day with icon, label, 7-day Sparkline and "you dismiss N%" hint) and an apps/{packageName} profile (dismissed-vs-acted DonutChart, median time to act, hourly bars, channels, today's screen time, editable daily-limit rule, pending suggestions with Accept), wired into MainActivity

**Built the Apps tab (rows sorted by alerts/day with icon, label, 7-day Sparkline and "you dismiss N%" hint) and an apps/{packageName} profile (dismissed-vs-acted DonutChart, median time to act, hourly bars, channels, today's screen time, editable daily-limit rule, pending suggestions with Accept), wired into MainActivity**

## What Happened

core-ui: added a Sparkline composable to Charts.kt (axis-free 64x20dp line; renders nothing for <2 points) and a Sparkline entry in ComponentGallery. feature-notifications: added AppIcon(packageName) to NotificationIcon.kt, reusing the existing app-icon loader and LRU cache. New apps package files: AppsLogic.kt holds pure code: buildRows (sorted by alerts/day descending, ties broken by label, series from AppStats.dailySeriesFromDayCounts), formatPerDay, and the limit-rule helpers findLimitRule, limitMinutes, newLimitRule and withLimit. newLimitRule mirrors ScreenTimeViewModel.createTimeLimitRule: a SCREEN_TIME rule with an APP_PACKAGE condition and an ADVISE action carrying dailyLimitMinutes. AppsViewModel combines appAlertCountsFlow and appDailyCountsFlow over a 7-local-day window that rolls at midnight (checked each minute). It resolves labels through AppNameResolver with a per-package cache, falling back to the stored appName when the app is uninstalled. AppsScreen uses testTags apps_list, apps_row, apps_dismiss_hint, apps_empty and apps_loading; tapping a row navigates to apps/{pkg}. AppProfileViewModel reads packageName from SavedStateHandle and combines nine flows over a 30-day window: counts, events, channels, the SCREEN_TIME rules, pending suggestions filtered by appPackageName, and today's UsageDao rows. It exposes setLimit (updates the existing limit rule or inserts a new one; rejects values <= 0), clearLimit (deletes the rule), acceptSuggestion (same as LearningViewModel: inserts the template as a LEARNED rule, then markAccepted) and dismissSuggestion. Results come back to the screen as snackbar messages. AppProfileScreen has its own TopAppBar with a back button, because the shell hides the top bar on non-tab routes. Its LazyColumn carries testTag app_profile and contains: a ring section, hourly SimpleBarChart with the peak hour highlighted, a channel list, screen time today plus the limit and set/change/remove buttons, a limit dialog that accepts 1..1440 digits only, and suggestion cards. MainActivity: the APPS route now renders AppsScreen, and the apps/{packageName} route was added using AppRoutes.APP_PROFILE and appProfile(pkg) (Uri-encoded). The AppsPlaceholderScreen import was removed; T03 deletes the placeholder file. Added AppsLogicTest with 9 JVM tests.

## Verification

./gradlew assembleDebug passed. ./gradlew :feature:feature-notifications:testDebugUnitTest passed, with AppsLogicTest 9/9 and the AppStatsTest suite still green. The grep checks for the apps_list and app_profile testTags both exit 0. The screens were not exercised on a device or emulator in this unit.

## Verification Evidence

| # | Command | Exit Code | Verdict | Duration |
|---|---------|-----------|---------|----------|
| 1 | `./gradlew assembleDebug` | 0 | pass | 2273ms |
| 2 | `./gradlew :feature:feature-notifications:testDebugUnitTest` | 0 | pass | 1502ms |
| 3 | `grep -rq "apps_list" feature/feature-notifications/src/main` | 0 | pass | 5ms |
| 4 | `grep -rq "app_profile" feature/feature-notifications/src/main` | 0 | pass | 6ms |

## Deviations

Added AppsLogic.kt and AppsLogicTest.kt beyond the planned file list, to keep sorting and limit-rule logic pure and tested. Added AppIcon to NotificationIcon.kt and APP_PROFILE/appProfile() to navigation/AppNavigation.kt. The profile also has a Remove-limit action and a Not-now (dismiss) action for suggestions.

## Known Issues

The screens have not been exercised on a device; there are no Compose UI tests yet. Profile 'today' screen time uses TimeUtils.todayDate() captured when the screen opens, so it won't roll at midnight while the screen stays open. PlaceholderScreens.kt still exists (T03 deletes it).

## Files Created/Modified

- `core/core-ui/src/main/java/com/usagecontrol/core/ui/component/Charts.kt`
- `core/core-ui/src/main/java/com/usagecontrol/core/ui/component/ComponentGallery.kt`
- `feature/feature-notifications/src/main/java/com/usagecontrol/feature/notifications/NotificationIcon.kt`
- `feature/feature-notifications/src/main/java/com/usagecontrol/feature/notifications/apps/AppsLogic.kt`
- `feature/feature-notifications/src/main/java/com/usagecontrol/feature/notifications/apps/AppsViewModel.kt`
- `feature/feature-notifications/src/main/java/com/usagecontrol/feature/notifications/apps/AppsScreen.kt`
- `feature/feature-notifications/src/main/java/com/usagecontrol/feature/notifications/apps/AppProfileViewModel.kt`
- `feature/feature-notifications/src/main/java/com/usagecontrol/feature/notifications/apps/AppProfileScreen.kt`
- `feature/feature-notifications/src/test/java/com/usagecontrol/feature/notifications/apps/AppsLogicTest.kt`
- `app/src/main/java/com/usagecontrol/MainActivity.kt`
- `app/src/main/java/com/usagecontrol/navigation/AppNavigation.kt`
<!-- gsd:state-version=156:1 -->
