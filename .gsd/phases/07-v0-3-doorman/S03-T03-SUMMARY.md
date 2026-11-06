---
id: T03
parent: S03
milestone: M007
key_files:
  - feature/feature-notifications/src/main/java/com/usagecontrol/feature/notifications/NowViewModel.kt
  - feature/feature-notifications/src/main/java/com/usagecontrol/feature/notifications/NowScreen.kt
  - feature/feature-notifications/src/main/java/com/usagecontrol/feature/notifications/NotificationIcon.kt
  - feature/feature-notifications/src/test/java/com/usagecontrol/feature/notifications/NotificationIconPathTest.kt
  - app/src/main/java/com/usagecontrol/MainActivity.kt
key_decisions:
  - A minute clock drives sections and the 1 h dismissed window in memory. Room queries restart only on a local-day change.
  - Stored icon paths already include the icons/ prefix (relative to filesDir), so the UI resolves File(filesDir, path) after the guard instead of File(filesDir, "icons/"+path).
  - The icon order is large icon, then small icon tinted as an alpha mask, then the app icon.
  - The context strip uses the latest ContextSnapshot plus the listener permission state, because modes arrive in S04.
duration:
verification_result: passed
completed_at:
blocker_discovered: false
---

# T03: Built NowScreen/NowViewModel: hero interruption count, context strip, search (now_search), time sections with bundles and lane dots, real stored icons via a safe-path LruCache loader, and a collapsible Recently dismissed list; the now route renders it and the old NotificationsScreen/ViewModel are deleted

**Built NowScreen/NowViewModel: hero interruption count, context strip, search (now_search), time sections with bundles and lane dots, real stored icons via a safe-path LruCache loader, and a collapsible Recently dismissed list; the now route renders it and the old NotificationsScreen/ViewModel are deleted**

## What Happened

NowViewModel (Hilt) uses a one-minute clock flow. The Room queries (nowStreamFlow since yesterday's start, recentlyDismissedFlow since today's start, todayInterruptionCount) restart only when the local day changes, through distinctUntilChanged on dayStart and flatMapLatest. Each tick reapplies sectioning and the 1 h recently-dismissed cutoff in memory. The ViewModel combines those flows with the latest ContextSnapshot (polled each tick), the search query and the notification-listener permission into NowUiState, using NowStream filter/sectionize/bundle. Each DAO flow has a catch that logs under the NowViewModel tag and emits empty or zero. NowScreen is one LazyColumn: HeroCount ("interruptions today"), ContextStrip (calendar event, location, "No mode active", or "Notification access is off", with an "N in shade" detail), an OutlinedTextField search (testTag now_search) with clear, SectionHeader per section with counts, NotificationRow per single item with LaneDot via record.lane, BundleRow per bundle (tap expands to child rows), an empty state, and a collapsible Recently dismissed section. NotificationIcon.kt checks persisted paths against the IconStore format ("icons/<pkg>/<64-hex>.png", no ".."). It decodes the large icon, otherwise the small icon tinted as a mask, using BitmapFactory in produceState on Dispatchers.IO with an 8 MB LruCache. It falls back to the app icon from PackageManager, or no icon. MainActivity's now route renders NowScreen(). The old NotificationsScreen and NotificationsViewModel were referenced only there, so I deleted them.

## Verification

:app:assembleDebug passed. The now_search grep passed. :feature:feature-notifications:testDebugUnitTest passed: 22 tests, 0 failures, including 5 new NotificationIconPathTest cases. I did not exercise the screen on a device in this task. The slice-level logcat verification for archive/snooze/open belongs to T04.

## Verification Evidence

| # | Command | Exit Code | Verdict | Duration |
|---|---------|-----------|---------|----------|
| 1 | `./gradlew :app:assembleDebug` | 0 | pass | 2363ms |
| 2 | `grep -q "now_search" feature/feature-notifications/src/main/java/com/usagecontrol/feature/notifications/NowScreen.kt` | 0 | pass | 8ms |
| 3 | `./gradlew :feature:feature-notifications:testDebugUnitTest` | 0 | pass | 2836ms |

## Deviations

I deleted NotificationsScreen.kt and NotificationsViewModel.kt, which the plan allowed. I used git rm, so the deletion is staged. Icon paths resolve against filesDir directly, as explained in the key decisions. I added NotificationIconPathTest. Single rows have no tap action yet, because T04 adds open.

## Known Issues

Not yet exercised on a device or emulator. Bundle expansion state is in-memory only and does not survive process death.

## Files Created/Modified

- `feature/feature-notifications/src/main/java/com/usagecontrol/feature/notifications/NowViewModel.kt`
- `feature/feature-notifications/src/main/java/com/usagecontrol/feature/notifications/NowScreen.kt`
- `feature/feature-notifications/src/main/java/com/usagecontrol/feature/notifications/NotificationIcon.kt`
- `feature/feature-notifications/src/test/java/com/usagecontrol/feature/notifications/NotificationIconPathTest.kt`
- `app/src/main/java/com/usagecontrol/MainActivity.kt`
<!-- gsd:state-version=186:1 -->
