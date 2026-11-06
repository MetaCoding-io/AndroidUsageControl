---
id: T01
parent: S03
milestone: M007
key_files:
  - app/src/main/java/com/usagecontrol/navigation/AppNavigation.kt
  - app/src/main/java/com/usagecontrol/navigation/PlaceholderScreens.kt
  - app/src/main/java/com/usagecontrol/MainActivity.kt
  - app/src/main/res/values/strings.xml
key_decisions:
  - Off-bar routes are centralized in an AppRoutes object instead of being AppScreen entries, so the bar iterates AppScreen.entries directly
  - Top bar and bottom bar both appear only on the three tab routes; pushed screens like Settings rely on system back
duration:
verification_result: passed
completed_at:
blocker_discovered: false
---

# T01: Replaced the five-tab shell with Now/Apps/Modes bottom tabs (string-resource labels), a TopAppBar settings gear (testTag settings_gear), and Coming-in-S04 placeholders for Apps/Modes

**Replaced the five-tab shell with Now/Apps/Modes bottom tabs (string-resource labels), a TopAppBar settings gear (testTag settings_gear), and Coming-in-S04 placeholders for Apps/Modes**

## What Happened

AppScreen enum is now NOW("now"), APPS("apps"), MODES("modes"), with @StringRes labels (tab_now/tab_apps/tab_modes) and icons Notifications/Apps/Tune. A new AppRoutes object holds the off-bar routes: settings, screentime, controls, insights, tour/standalone. In MainActivity, the "main" nested graph now starts at "now". That route still renders the existing NotificationsScreen until T03 swaps it. apps and modes render AppsPlaceholderScreen and ModesPlaceholderScreen, which are new in app/navigation/PlaceholderScreens.kt and show "Coming in S04" with testTags apps_placeholder and modes_placeholder. The Scaffold has a TopAppBar on tab routes only. Its title is the current tab label, and a gear IconButton (testTag settings_gear) navigates to "settings" with launchSingleTop. The settings, screentime, controls, insights, tour, rules/{pillar} and rule-editor routes stay registered in the main graph but are not in the bar. showBottomBar, like the top bar, is derived from currentRoute matching an AppScreen tab. The nav_tab_<route> testTags are kept. MainViewModel still emits "main" or "onboarding" as the root start destination, so nothing changed there. Onboarding still navigates to "main", which now lands on Now. A grep found no other references to the old AppScreen entries.

## Verification

./gradlew :app:assembleDebug passes. The settings_gear testTag is present in MainActivity.kt. A python check confirms the AppScreen entries are exactly [NOW, APPS, MODES].

## Verification Evidence

| # | Command | Exit Code | Verdict | Duration |
|---|---------|-----------|---------|----------|
| 1 | `./gradlew :app:assembleDebug -q` | 0 | pass | 9536ms |
| 2 | `grep -q "settings_gear" app/src/main/java/com/usagecontrol/MainActivity.kt` | 0 | pass | 8ms |
| 3 | `python3 -c "import re,sys; s=open('app/src/main/java/com/usagecontrol/navigation/AppNavigation.kt').read(); e=re.findall(r'^\s+([A-Z_]+)\(\"', s, re.M); print(e); sys.exit(0 if e==['NOW','APPS','MODES'] else 1)"` | 0 | pass | 29ms |

## Deviations

Added a new file, app/src/main/java/com/usagecontrol/navigation/PlaceholderScreens.kt, for the placeholder composables (the plan listed only three files). Enum labels use string resources instead of a title String.

## Known Issues

The Settings screen has no top-bar back arrow and relies on system back. No Compose UI test was added for the shell; T03/T04 or UAT should exercise the nav_tab_* and settings_gear tags on device.

## Files Created/Modified

- `app/src/main/java/com/usagecontrol/navigation/AppNavigation.kt`
- `app/src/main/java/com/usagecontrol/navigation/PlaceholderScreens.kt`
- `app/src/main/java/com/usagecontrol/MainActivity.kt`
- `app/src/main/res/values/strings.xml`
<!-- gsd:state-version=161:1 -->
