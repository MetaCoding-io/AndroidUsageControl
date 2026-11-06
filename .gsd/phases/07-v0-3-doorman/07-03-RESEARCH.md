# S03: Shell and Now — Research

**Date:** 2026-11-04

## Summary

Targeted research. This slice uses Compose and Room patterns that already exist in the codebase, and the components it needs were built in S01 (`NotificationRow`, `BundleRow`, `HeroCount`, `ContextStrip`, `SectionHeader`, `LaneDot`). Its data comes from the S02 v4 schema (`provisionalLane`, `smallIconPath`/`largeIconPath`, `key`, `removedAt`/`removalReason`, `updateCount`, `isGroupSummary`). The shell today has five tabs and lives in `app/.../MainActivity.kt` (`UsageControlAppContent`, a NavHost plus `NavigationBar` built from the `AppScreen` enum in `app/.../navigation/AppNavigation.kt`). It needs to become Now/Apps/Modes with Settings opened from a gear. The old screens stay reachable as routes (Apps and Modes get placeholder screens until S04).

The real risk is phone-side actions that only the listener can perform: archive (`cancelNotification(key)`), snooze (`snoozeNotification(key, durationMs)`), and opening the original (`PendingIntentCache.contentIntent(key)?.send()`). Feature modules don't depend on `service-notification`; only app and feature-settings do. A small action bridge is needed.

## Recommendation

- Add a `NotificationActions` interface in core (core-common or core-model) with `archive(key)`, `snooze(key, untilMs)`, `open(key): Boolean` and `restore`/undo semantics. Implement it in service-notification on top of a static `@Volatile var instance` set in `onListenerConnected` and cleared in `onListenerDisconnected`/`onDestroy` of NotificationCaptureService, together with PendingIntentCache. Bind it with Hilt. feature-notifications (or a new `feature-now`; reusing feature-notifications is simpler) depends only on the interface.
- Undo archive: mark the row archived in the DB right away, delay the `cancelNotification` call by 10 s (a coroutine in the ViewModel, or a pending-action job), and cancel the job on undo. Keep in mind that a cancelled system notification can't be re-posted.
- Snooze: a time picker (M3 `TimePicker` dialog) computes `untilMs - now`, then calls `snoozeNotification`. Record `userAction=SNOOZED` with `actionTimestamp`.
- Open: if the cache misses (listener restarted), show the NOW-09 message "no longer available" and fall back to `packageManager.getLaunchIntentForPackage`.
- Now stream: a new DAO query for active rows (`removedAt IS NULL` or within the last N hours, `isGroupSummary=0`) ordered by timestamp. Grouping into time sections (Now/Earlier today/Yesterday) and bundles (same package+groupKey with ≥2 rows, or the UPDATES/QUIET lanes) should be pure Kotlin functions so they can have JVM unit tests. Hero count is today's non-ongoing interruptions. Recently dismissed is rows with `removedAt` in the last 1 h, ordered by removedAt. Search filters title/text/appName.
- Icons: load `filesDir/icons/<relPath>` with `BitmapFactory` plus a `remember`/LRU cache, or add Coil (`AsyncImage(File)`). Coil isn't in the catalog yet. Hand-decoding the small PNGs is fine and avoids a new dependency.
- Swipe: M3 `SwipeToDismissBox` (StartToEnd → archive, EndToStart → snooze picker; return false from confirmValueChange for snooze so the row resets).

## Implementation Landscape

### Key Files

- `app/src/main/java/com/usagecontrol/navigation/AppNavigation.kt` — `AppScreen` enum. Replace its entries with Now/Apps/Modes. Settings becomes a route that isn't a tab.
- `app/src/main/java/com/usagecontrol/MainActivity.kt` (221 lines) — NavHost, bottom bar (`showBottomBar` = route in AppScreen), and existing routes (notifications, screentime, controls, insights, settings, rules, tour). Add a TopAppBar gear → settings. Keep the testTags `nav_tab_<route>`.
- `feature/feature-notifications/.../NotificationsScreen.kt` (329), `NotificationsViewModel.kt` (113) — the old list. Rewrite it as NowScreen/NowViewModel, or add new files next to it.
- `core/core-database/.../dao/NotificationDao.kt` — add `nowStreamFlow(since)`, `recentlyDismissedFlow(since)`, `todayInterruptionCount(since)`, `markArchived(id, ts)`/`clearArchived` for undo. If a query needs a new column, Room v4 is already shipped, so a migration would be required. Prefer the existing `userAction`/`actionTimestamp` (no schema change).
- `core/core-ui/.../component/DoormanComponents.kt` — NotificationRow(lane, appName, title, time, text, unread, icon slot, onClick), BundleRow, HeroCount, ContextStrip, SectionHeader are ready. `ProvisionalLane` (core-rules) must map to the UI `Lane` enum (LaneColors.kt). Add the mapper to the feature module.
- `service/service-notification/.../NotificationCaptureService.kt` — `companion object` at line 322. Add the instance ref. `cancelNotification` is already used at line 186.
- `service/service-notification/.../PendingIntentCache.kt` — `contentIntent(key)`, `actionIntent`.
- `service/service-notification/.../IconStore.kt` — `relativePath`/`isSafeRelativePath`. The UI resolves paths against `filesDir/icons`.
- `service/service-context` (SoundPresetApplier) — the Modes tab belongs to S04. Here it's just a placeholder.

### Build Order

1. Shell: AppScreen → Now/Apps/Modes + gear → Settings, with placeholder Apps/Modes screens. This is low risk and unblocks everything else.
2. DAO queries + pure stream grouping (sections, bundles, lane mapping, hero count), with JVM tests.
3. NowScreen rendering real rows with icons, lane dots, hero count, context strip, search, and recently dismissed.
4. Action bridge (interface + service impl + Hilt binding), then swipe archive with 10 s undo snackbar, swipe-left snooze to a time, and tap-to-open with the not-available fallback.

### Verification Approach

- `./gradlew assembleDebug check` and `:core:core-rules:testDebugUnitTest`, plus feature unit tests for the grouping/section/bundle logic.
- Instrumented DAO tests (pattern from S02 T02) for the new queries.
- On device: `adb shell cmd notification post -S bigtext -t T tag1 "hi"`, then check that the row appears in Now with an icon and lane dot. Swipe right should leave the system notification in place for 10 s and then remove it; undo should keep it. Swipe left plus a time should take it off the shade until that time. Tap should open the source app. logcat tag `NotificationCapture`.

## Constraints

- Feature modules must not depend on service modules for the bridge. Put the interface in core and bind it in service/app.
- `snoozeNotification`/`cancelNotification` only work while the listener is connected. A null instance means show an error.
- PendingIntents live only in memory. After a listener restart, open must degrade gracefully (NOW-09).
- Schema v4 is shipped. Any new column means v5 plus a migration, so use the existing columns.
- Phone behaviour must not change except for the user's explicit swipe and tap actions (milestone vision).

## Common Pitfalls

- **SwipeToDismissBox snooze** — return false from confirmValueChange, otherwise the row stays dismissed. Reset the state after the picker is cancelled.
- **Undo after cancel** — never cancel the system notification before the undo window closes.
- **Group summaries/ongoing** — exclude `isGroupSummary` and ongoing rows (ongoing ones live in `live_notifications`) from the stream and the counts.
- **KSP incremental** — after DAO or resource changes, a module clean may be needed (S01 note).
<!-- gsd:state-version=134:1 -->
