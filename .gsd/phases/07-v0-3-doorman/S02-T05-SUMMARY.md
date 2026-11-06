---
id: T05
parent: S02
milestone: M007
key_files:
  - service/service-notification/src/main/java/com/usagecontrol/service/notification/NotificationCaptureService.kt
  - service/service-notification/src/main/java/com/usagecontrol/service/notification/CaptureMapping.kt
  - service/service-notification/src/test/java/com/usagecontrol/service/notification/CaptureMappingTest.kt
key_decisions:
  - Live/history split and REASON_* naming live in a pure CaptureMapping object so they are JVM-testable
  - Reconcile inserts only actives without a live history row and skips rules, so reconnects neither inflate updateCount nor auto-cancel visible notifications
  - Stored text prefers EXTRA_BIG_TEXT over EXTRA_TEXT (no separate bigText column)
  - Removal reasons outside the plan list are named explicitly (APP_CANCEL_ALL, LISTENER_CANCEL_ALL, CHANNEL_BANNED, PACKAGE_BANNED, PACKAGE_SUSPENDED, USER_STOPPED, ERROR); the rest become OTHER_<code>
duration:
verification_result: passed
completed_at:
blocker_discovered: false
---

# T05: Rewrote NotificationCaptureService: serial single-thread dispatcher, key-based upsert with updateCount, removal reasons via markRemoved, live table for ongoing/FGS/media, rich fields (ranking, messages/actions JSON, icons, provisional lane), PendingIntent caching, and reconcile on connect; pure CaptureMapping helper with 7 JVM tests

**Rewrote NotificationCaptureService: serial single-thread dispatcher, key-based upsert with updateCount, removal reasons via markRemoved, live table for ongoing/FGS/media, rich fields (ranking, messages/actions JSON, icons, provisional lane), PendingIntent caching, and reconcile on connect; pure CaptureMapping helper with 7 JVM tests**

## What Happened

NotificationCaptureService now runs every callback on CoroutineScope(SupervisorJob + Dispatchers.IO.limitedParallelism(1) + CoroutineExceptionHandler), so a post, its updates and its removal reach the DB in order, and a failure gets logged instead of crashing the process. onNotificationPosted(sbn, rankingMap) caches the content/action PendingIntents. Live notifications (FLAG_ONGOING_EVENT, FLAG_FOREGROUND_SERVICE, MediaStyle/DecoratedMediaCustomViewStyle template, or EXTRA_MEDIA_SESSION) are written to LiveNotificationDao.upsert and skip history. Everything else is built into a v4 NotificationEntity with: key, id, tag; channelId and importance from Ranking; isConversation and conversationShortcutInfo behind an API 30 guard, falling back to notification.shortcutId; when, groupKey, isGroupSummary, flags, color; title (TITLE, else TITLE_BIG), text (BIG_TEXT, else TEXT), subText; messagesJson via NotificationCompat.MessagingStyle; actionsJson with title/hasRemoteInput/semanticAction; small and large icon paths via IconStore; and provisionalLane via ProvisionalLaneClassifier. The entity goes to NotificationDao.upsertByKey, and the returned id feeds the existing DISMISS_NOTIFICATION rule path (markAutoDismissed + cancelNotification). A non-live post also deletes any live row with the same key, which covers a notification that stops being ongoing. onNotificationRemoved maps REASON_* to strings through CaptureMapping.removalReasonName (unknown codes become OTHER_n), removes the cache entry, deletes the live row if there is one, and otherwise calls findIdByKey -> markRemoved. onListenerConnected reconciles: it upserts active live notifications and deletes stale ones with deleteNotIn (calling clear() when the active set is empty, which avoids an empty IN list), and inserts only active non-live notifications that have no live history row, so a reconnect does not bump updateCount. Rules are not applied during reconcile, so reconnecting never cancels notifications the user can see. onListenerDisconnected clears PendingIntentCache. Logging uses tag NotificationCapture, one line each for posted (key, id, updateCount, lane), removed (key, id, updateCount, reason), live upsert/remove, auto-dismiss, and reconcile counts (active/live/liveRemoved/historyAdded). service-notification already depended on core-rules, so build.gradle.kts did not change.

## Verification

./gradlew :app:assembleDebug :core:core-rules:testDebugUnitTest passed. ./gradlew :service:service-notification:testDebugUnitTest passed, including CaptureMappingTest (7 tests, 0 failures). The on-device adb flow (post/update/remove test notification, check one row with updateCount=1 and removalReason) was NOT run: no device is attached (adb devices returned an empty list).

## Verification Evidence

| # | Command | Exit Code | Verdict | Duration |
|---|---------|-----------|---------|----------|
| 1 | `./gradlew :app:assembleDebug :core:core-rules:testDebugUnitTest` | 0 | pass | 3324ms |
| 2 | `./gradlew :service:service-notification:testDebugUnitTest` | 0 | pass | 1967ms |

## Deviations

Added CaptureMapping.kt and CaptureMappingTest.kt, which are not in the plan's file list. build.gradle.kts needed no change. The text column now prefers bigText.

## Known Issues

The on-device adb verification is pending because no device was connected. History rows whose notifications were removed while the listener was unbound stay with removedAt NULL; reconcile does not close them, since that would need a new DAO query. deleteNotIn binds one variable per live key; this is fine for realistic counts but has no chunking.

## Files Created/Modified

- `service/service-notification/src/main/java/com/usagecontrol/service/notification/NotificationCaptureService.kt`
- `service/service-notification/src/main/java/com/usagecontrol/service/notification/CaptureMapping.kt`
- `service/service-notification/src/test/java/com/usagecontrol/service/notification/CaptureMappingTest.kt`
<!-- gsd:state-version=186:1 -->
