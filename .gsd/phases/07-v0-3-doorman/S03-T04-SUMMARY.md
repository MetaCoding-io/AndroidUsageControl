---
id: T04
parent: S03
milestone: M007
key_files:
  - core/core-common/src/main/java/com/usagecontrol/core/common/NotificationActions.kt
  - service/service-notification/src/main/java/com/usagecontrol/service/notification/NotificationActionsImpl.kt
  - service/service-notification/src/main/java/com/usagecontrol/service/notification/NotificationCaptureService.kt
  - feature/feature-notifications/src/main/java/com/usagecontrol/feature/notifications/NowViewModel.kt
  - feature/feature-notifications/src/main/java/com/usagecontrol/feature/notifications/NowScreen.kt
  - feature/feature-notifications/src/main/java/com/usagecontrol/feature/notifications/NowStream.kt
  - feature/feature-notifications/src/test/java/com/usagecontrol/feature/notifications/NowStreamTest.kt
  - core/core-database/src/main/java/com/usagecontrol/core/database/dao/NotificationDao.kt
key_decisions:
  - Used the non-deprecated SwipeToDismissBox onDismiss overload: EndToStart calls state.reset() and opens the picker, which is equivalent to confirmValueChange=false
  - PendingIntent.send passes an ActivityOptions BAL opt-in, since targetSdk 37 otherwise blocks the creator's activity start
  - nowStreamFlow excludes SNOOZED as well as ARCHIVED; the repost creates a fresh row
  - Archive commit to the shade happens only for rows still in the shade (key != null && removedAt == null); pending archives commit in onCleared
duration:
verification_result: passed
completed_at:
blocker_discovered: false
---

# T04: Added the NotificationActions bridge (core-common interface, service-side impl over a @Volatile bound-listener instance and PendingIntentCache) plus Now swipe-right archive with 10 s undo, swipe-left snooze to a picked clock time, and tap-to-open with launch fallback and snackbars

**Added the NotificationActions bridge (core-common interface, service-side impl over a @Volatile bound-listener instance and PendingIntentCache) plus Now swipe-right archive with 10 s undo, swipe-left snooze to a picked clock time, and tap-to-open with launch fallback and snackbars**

## What Happened

1. core-common NotificationActions: archive(key)/snooze(key,durationMs) return Result {DONE, LISTENER_UNAVAILABLE, FAILED}; open(key?, packageName) returns OpenResult {OPENED, FALLBACK_LAUNCHED, UNAVAILABLE}; isListenerConnected.
2. service-notification NotificationActionsImpl (@Singleton, bound via @Binds NotificationActionsModule in SingletonComponent). NotificationCaptureService gets a companion @Volatile `instance`, set in onListenerConnected and cleared (identity-checked) in onListenerDisconnected/onDestroy. archive → cancelNotification, snooze → snoozeNotification (rejects non-positive durations), open → PendingIntentCache.contentIntent(key).send(...) with ActivityOptions BAL opt-in (ALLOW_IF_VISIBLE on 36+, ALLOWED on 34/35), since targetSdk 37 otherwise blocks the creator's activity start. If the intent is missing or cancelled, it falls back to getLaunchIntentForPackage + NEW_TASK, else UNAVAILABLE. Every action logs under the NotificationCapture tag with the key and outcome (cancelled / snoozed / listener-unavailable / intent-missing→launch-fallback / failed).
3. NowViewModel: archive() marks ARCHIVED immediately (the row leaves nowStreamFlow), emits NowEvent.Archived, and starts a 10 s job that calls actions.archive(key), but only for rows still in the shade. undoArchive() cancels the job and clears userAction. onCleared commits any archives still pending, so leaving the screen doesn't silently drop them. snooze() refuses rows not in the shade, computes snoozeDurationUntil, calls snooze on IO, and marks SNOOZED only on DONE. open() calls actions.open and on anything but OPENED emits "Original no longer available". A null listener or failure emits an error snackbar.
4. NowScreen: SwipeToDismissBox on stream rows and bundle children, with an Archive/Snooze background. StartToEnd archives; EndToStart resets the swipe state and opens an AlertDialog with an M3 TimePicker (testTags now_snooze_dialog / now_snooze_confirm). Tap opens. A local SnackbarHost (now_snackbar) shows Undo with SnackbarDuration.Long (10 s). Recently dismissed rows are tap-to-open only.
5. NowStream.snoozeDurationUntil(hour, minute, now, zone): pure, always in (0, 24h], DST-aware, validates ranges. 11 new JVM tests.
6. nowStreamFlow also excludes SNOOZED rows. When a snoozed notification is reposted, upsertByKey releases the removed key and inserts a fresh row, so it comes back.
7. Lint (`check`) failed on a pre-existing NewApi error: Ranking.isConversation/conversationShortcutInfo were guarded by SDK_INT >= R, but lint says API 31. I raised the guards to S, which also avoids a crash on Android 11.

## Verification

./gradlew assembleDebug check --continue passed (lint, all unit tests). The NowStreamTest XML shows 28 tests, 0 failures, including 11 snoozeDurationUntil cases. :app:assembleDebugAndroidTest still compiles after the DAO query change. Not exercised on a device: swipe gestures, the time picker, and PendingIntent BAL behavior. Those go to slice UAT.

## Verification Evidence

| # | Command | Exit Code | Verdict | Duration |
|---|---------|-----------|---------|----------|
| 1 | `./gradlew assembleDebug check --console=plain --continue` | 0 | pass | 20316ms |
| 2 | `./gradlew :app:assembleDebugAndroidTest --console=plain -q` | 0 | pass | 3924ms |

## Deviations

Used the onDismiss overload instead of the deprecated confirmValueChange (same behavior). AlertDialog wraps TimePicker instead of the experimental TimePickerDialog. Added SNOOZED to the nowStreamFlow exclusion. Fixed a pre-existing lint NewApi error in NotificationCaptureService (R→S guards) so `check` passes.

## Known Issues

Swipe, time picker, and open/BAL behavior have not been checked on a device yet. If the user presses Undo after the 10 s commit already cancelled the notification, the row is restored in-app but stays gone from the shade.

## Files Created/Modified

- `core/core-common/src/main/java/com/usagecontrol/core/common/NotificationActions.kt`
- `service/service-notification/src/main/java/com/usagecontrol/service/notification/NotificationActionsImpl.kt`
- `service/service-notification/src/main/java/com/usagecontrol/service/notification/NotificationCaptureService.kt`
- `feature/feature-notifications/src/main/java/com/usagecontrol/feature/notifications/NowViewModel.kt`
- `feature/feature-notifications/src/main/java/com/usagecontrol/feature/notifications/NowScreen.kt`
- `feature/feature-notifications/src/main/java/com/usagecontrol/feature/notifications/NowStream.kt`
- `feature/feature-notifications/src/test/java/com/usagecontrol/feature/notifications/NowStreamTest.kt`
- `core/core-database/src/main/java/com/usagecontrol/core/database/dao/NotificationDao.kt`
<!-- gsd:state-version=161:1 -->
