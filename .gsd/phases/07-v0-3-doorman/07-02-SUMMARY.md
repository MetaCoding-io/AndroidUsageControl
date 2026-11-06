---
id: S02
parent: M007
milestone: M007
provides:
  - NotificationDao.upsertByKey/markRemoved
  - LiveNotificationDao
  - IconStore
  - PendingIntentCache
  - ProvisionalLaneClassifier
requires:
  []
affects:
  []
key_files: []
key_decisions: []
patterns_established:
  - (none)
observability_surfaces:
  - logcat tag NotificationCapture: post/update/remove with key, updateCount, reason; reconcile added/removed counts
drill_down_paths:
  []
duration: ""
verification_result: passed
completed_at: 2026-11-04T06:22:42.347Z
blocker_discovered: false
---

# S02: Listener foundation

**Rewrote the notification listener on Room v4: key upsert with updateCount/removalReason, live table for ongoing, provisional lane classifier, disk IconStore + PendingIntentCache, reconcile on connect, lossless MIGRATION_3_4.**

## What Happened

T01 enabled Room schema export and generated 3.json. T02 added schema v4 (19 new NotificationEntity columns, unique key index, live_notifications + LiveNotificationDao, MIGRATION_3_4, upsertByKey/markRemoved, isOngoing=0 in stats queries) with 7 passing instrumented tests. T03 added ProvisionalLane plus a pure classifier, with 33 JVM tests. T04 added the IconStore (filesDir/icons/<pkg>/<hash>.png, prune wired into cleanup) and PendingIntentCache singletons. T05 rewrote NotificationCaptureService: a serial single-thread dispatcher, keyed upsert, live-table routing for ongoing/FGS/media, removal-reason mapping, reconcile in onListenerConnected, and NotificationCapture logging.

## Verification

Closeout gsd_exec: ./gradlew :app:assembleDebug :app:assembleDebugAndroidTest :core:core-rules:testDebugUnitTest exited 0. 3.json and 4.json are both present, and no fallbackToDestructiveMigration exists in core-database. The instrumented migration/DAO tests (7/7) passed on the emulator in T02. No adb device was attached at closeout, so the live adb post/update/remove check was deferred to UAT.

## Requirements Advanced

- R011 — LSTN-01..09 implemented; migration v3->v4 tested lossless

## Requirements Validated

None.

## New Requirements Surfaced

None.

## Requirements Invalidated or Re-scoped

None.

## Operational Readiness

None.

## Deviations

None.

## Known Limitations

The adb listener check (one row, updateCount=1, reason) and icon reload after restart were not re-run at closeout because no device was attached.

## Follow-ups

None.

## Files Created/Modified

None.
<!-- gsd:state-version=110:1 -->
