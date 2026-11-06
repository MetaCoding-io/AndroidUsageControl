---
id: T02
parent: S02
milestone: M007
key_files:
  - (none)
key_decisions:
  - (none)
duration:
verification_result: passed
completed_at:
blocker_discovered: false
---

# T02: Shipped Room schema v4: keyed NotificationEntity (19 new columns, unique key index), live_notifications table + LiveNotificationDao, MIGRATION_3_4, transactional upsertByKey/markRemoved, isOngoing exclusion in stats, plus 7 passing instrumented migration/DAO tests

**Shipped Room schema v4: keyed NotificationEntity (19 new columns, unique key index), live_notifications table + LiveNotificationDao, MIGRATION_3_4, transactional upsertByKey/markRemoved, isOngoing exclusion in stats, plus 7 passing instrumented migration/DAO tests**

## What Happened

NotificationEntity gained nullable key (UNIQUE index; legacy rows keep NULL, which SQLite UNIQUE permits), notifId, tag, channelId, importance, isConversation, shortcutId, whenMs, isGroupSummary, flags, updateCount, removalReason, removedAt, smallIconPath, largeIconPath, color, provisionalLane, messagesJson, actionsJson. Every NOT NULL column carries @ColumnInfo(defaultValue="0"), which matches the SQL DEFAULT. NotificationRecord and Mappers got the same fields with defaults, so the existing NotificationCaptureService still compiles unchanged until T05.

Added LiveNotificationEntity (live_notifications, PK key) and LiveNotificationDao (@Upsert upsert, deleteByKey, getAllFlow, getAllKeys, deleteNotIn, clear). The database is now version 4 and registers the new entity and DAO. DatabaseModule adds MIGRATION_3_4 and provides LiveNotificationDao; there is no destructive fallback. MIGRATION_3_4 runs ALTER TABLE ADD COLUMN for each new column, then CREATE UNIQUE INDEX index_notifications_key, then CREATE TABLE live_notifications. All three statements use SQL copied from the generated 4.json.

NotificationDao changes:
- findIdByKey: matches only live rows (removedAt IS NULL).
- releaseRemovedKey and applyUpdate: helpers for the upsert.
- @Transaction upsertByKey: rows with a null key are always inserted. For a key it has not seen, it releases the key from any previously removed row and then inserts with updateCount=0. For an existing live row, it updates title, text, subText, timestamp, whenMs, flags and importance, keeps the old icon/lane values when the new ones are null (COALESCE), increments updateCount, and returns the same id.
- markRemoved: sets removalReason and removedAt. It maps CLICK to OPENED and anything else to DISMISSED, but only when userAction is still NONE, so AUTO_DISMISSED and its actionTimestamp are kept.
- getDismissStats, getDismissalsByHour, getDailyStats and getActionBreakdown now filter isOngoing = 0. getCount and the SuggestionEngine signatures are unchanged.

Tests in app/src/androidTest/java/com/usagecontrol/db:
- MigrationTest.migrate3To4_preservesRows: seeds 3 v3 rows (NONE, DISMISSED, OPENED), migrates with MigrationTestHelper and schema validation, then checks count, packageName, timestamp and userAction are kept, key and removalReason are NULL, updateCount is 0, and live_notifications is empty.
- NotificationDaoTest: 6 cases on an in-memory database.

A Medium_Phone_API_36.1 emulator was booted headless; all 7 tests passed and the emulator was then shut down.

Build fix needed along the way: on the device, MigrationTest at first failed with AbstractMethodError in kotlinx-serialization. AGP consistent resolution pinned the androidTest classpath to the app's kotlinx-serialization 1.7.3, but room-migration 2.8.5 needs 1.8.1. Fixed by adding kotlinx-serialization-bom 1.8.1 to the catalog and applying it as implementation(platform(...)) in the app. Also added androidTestImplementation room-runtime and coroutines-core so the tests compile against Room and runBlocking.

## Verification

Ran `./gradlew :app:assembleDebug :app:assembleDebugAndroidTest`: exit 0. Checked that `core/core-database/schemas/com.usagecontrol.core.database.UsageControlDatabase/4.json` exists. Ran `connectedDebugAndroidTest` filtered to the com.usagecontrol.db package on the emulator: exit 0, 7 tests, 0 failures (JUnit XML report). Slice-level adb listener verification (NotificationCapture logs) is left for T05, which rewrites the listener.

## Failure Modes
- SQLite migration failure (e.g. a column already exists from a partial run): Room runs a migration inside a transaction, so a failed ALTER rolls back and Room throws IllegalStateException when the database opens. There is no destructive fallback, so v3 data is never silently dropped.
- Schema drift between the migration SQL and the entity: caught by MigrationTestHelper.runMigrationsAndValidate(validateDroppedTables=true) in MigrationTest, and by Room's runtime validation when the database opens.
- UNIQUE violation on key: upsertByKey runs lookup, release and insert in one @Transaction, and T05 serialises callers on a single-parallelism dispatcher. A re-post of a removed key releases the old row's key before inserting, so the constraint is not hit on legitimate reuse. If a concurrent writer did collide, insert throws SQLiteConstraintException up to the caller (it is not swallowed).
- Legacy rows with a NULL key: allowed by SQLite UNIQUE semantics, verified by MigrationTest (3 NULL-key rows) and by differentKeysAndNullKeys_insertSeparateRows.

## Load Profile
- Expected load is tens to hundreds of notification events per hour. At 10x (bursty chat apps sending thousands of updates per hour), the first resource to saturate is the SQLite write lock.
- Protection: updates go through applyUpdate on an existing row instead of inserting a new row per update, so a notification updated N times keeps one row and table growth no longer scales with update rate. findIdByKey is backed by the unique index on key, so each upsert is an O(log n) lookup plus a single-row write.
- live_notifications holds only the currently posted ongoing notifications (bounded, small). deleteNotIn takes the active-key list, which is far below SQLite's bind-variable limit in practice.
- Stats queries keep using the existing timestamp index; the added isOngoing = 0 predicate is a cheap filter.

## Negative Tests
- Rows with a null key must never merge, and different keys must stay separate: NotificationDaoTest.differentKeysAndNullKeys_insertSeparateRows.
- A re-post after removal must not resurrect or merge into the removed row: NotificationDaoTest.repostAfterRemoval_startsNewRow checks a new id, updateCount=0, the old row's key released, and the old row's reason kept.
- markRemoved must not overwrite an existing action: markRemoved_clickMapsToOpened_andPreservesExistingAction checks AUTO_DISMISSED and actionTimestamp are kept after LISTENER_CANCEL, and CLICK maps to OPENED.
- markRemoved must not touch updateCount or row count, and the key must no longer resolve: markRemoved_setsReasonAndKeepsCount.
- Ongoing rows are excluded from stats: ongoingRowsExcludedFromStats covers getDismissStats and getActionBreakdown, while getCount still counts all rows.
- Migration on legacy data: MigrationTest.migrate3To4_preservesRows checks NULL key, default updateCount and NULL removalReason on pre-existing rows.

## Verification Evidence

| # | Command | Exit Code | Verdict | Duration |
|---|---------|-----------|---------|----------|
| 1 | `cd /home/james/Code/AndroidUsageControl && ./gradlew :app:assembleDebug :app:assembleDebugAndroidTest -q` | 0 | pass | 4976ms |
| 2 | `cd /home/james/Code/AndroidUsageControl && test -f core/core-database/schemas/com.usagecontrol.core.database.UsageControlDatabase/4.json` | 0 | pass | 9ms |
| 3 | `cd /home/james/Code/AndroidUsageControl && ANDROID_SERIAL=emulator-5554 ./gradlew :app:connectedDebugAndroidTest -q -Pandroid.testInstrumentationRunnerArguments.package=com.usagecontrol.db` | 0 | pass | 9387ms |

## Deviations

None.

## Known Issues

None.

## Files Created/Modified

None.
<!-- gsd:state-version=161:1 -->
