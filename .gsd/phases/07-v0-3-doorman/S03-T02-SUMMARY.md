---
id: T02
parent: S03
milestone: M007
key_files:
  - core/core-database/src/main/java/com/usagecontrol/core/database/dao/NotificationDao.kt
  - core/core-database/src/main/java/com/usagecontrol/core/database/Mappers.kt
  - core/core-model/src/main/java/com/usagecontrol/core/model/NotificationRecord.kt
  - feature/feature-notifications/src/main/java/com/usagecontrol/feature/notifications/NowStream.kt
  - feature/feature-notifications/src/test/java/com/usagecontrol/feature/notifications/NowStreamTest.kt
  - feature/feature-notifications/build.gradle.kts
  - feature/feature-notifications/src/main/java/com/usagecontrol/feature/notifications/NotificationsScreen.kt
  - app/src/androidTest/java/com/usagecontrol/db/NotificationDaoTest.kt
key_decisions:
  - Archived state is the string 'ARCHIVED' in the existing userAction column, plus a new NotificationAction.ARCHIVED enum value. The entity→model mapper now falls back to NONE for unknown action names instead of throwing.
  - nowStreamFlow only treats removedAt IS NULL as live when key IS NOT NULL, because legacy v3 rows never get removedAt and would otherwise flood Now with all history.
  - Lane mapping: PRIORITY→Live, PEOPLE→People, UPDATES/null→Work, QUIET→Noise.
  - Lane bundles (UPDATES/QUIET per app) take precedence over groupKey bundles, and bundles need ≥2 rows.
duration:
verification_result: passed
completed_at:
blocker_discovered: false
---

# T02: Added Now-stream DAO queries (nowStreamFlow, recentlyDismissedFlow, todayInterruptionCount, markUserAction/clearUserAction), an ARCHIVED NotificationAction, and pure NowStream functions (toLane, sectionize, bundle, filter) with 17 JVM tests and 7 new instrumented DAO tests

**Added Now-stream DAO queries (nowStreamFlow, recentlyDismissedFlow, todayInterruptionCount, markUserAction/clearUserAction), an ARCHIVED NotificationAction, and pure NowStream functions (toLane, sectionize, bundle, filter) with 17 JVM tests and 7 new instrumented DAO tests**

## What Happened

NotificationDao gained five members. nowStreamFlow(since, limit=500) returns live keyed rows (removedAt IS NULL AND key IS NOT NULL) plus rows posted since `since`, excluding group summaries, ongoing rows and userAction 'ARCHIVED', newest first. recentlyDismissedFlow(since, limit=100) returns rows with removedAt >= since, excluding summaries and ongoing rows, ordered by removedAt DESC. todayInterruptionCount(startOfDay) is a Flow<Int> over non-ongoing, non-summary rows. markUserAction(id, action, ts) and clearUserAction(id) reuse the existing userAction/actionTimestamp columns and return the affected row count, so there is no schema change. markRemoved already preserves non-NONE actions, so an archived row stays ARCHIVED after the delayed cancel.

Rows are represented as NotificationRecord. Because the entity→model mapper used NotificationAction.valueOf, writing 'ARCHIVED' would have crashed it. I added ARCHIVED to the NotificationAction enum and made the mapper fall back to NONE on unknown names. The exhaustive `when` in the legacy NotificationsScreen got an ARCHIVED branch.

NowStream.kt (feature-notifications) is pure Kotlin:
- toLane maps PRIORITY→Live, PEOPLE→People, UPDATES/null→Work and QUIET→Noise. There are also `provisional` and `lane` extension properties on NotificationRecord.
- sectionize(rows, now, zone) returns NOW (<1h, including future-dated rows), EARLIER_TODAY (local start of day in `zone`) and OLDER, omitting empty sections.
- bundle(rows) returns a sealed StreamItem: SingleItem or BundleItem(packageName, appName, groupKey, lane, rows, with count/timestamp/id). UPDATES and QUIET rows bundle per app and lane; other rows bundle by package+groupKey. Any bucket with fewer than 2 rows stays single, and first-occurrence order is preserved.
- filter(rows, query) is a trimmed, case-insensitive, literal substring match over title, text and appName.

junit was added as testImplementation in feature-notifications.

## Verification

./gradlew :feature:feature-notifications:testDebugUnitTest :app:assembleDebugAndroidTest exited 0. The NowStreamTest XML shows tests=17, failures=0, errors=0. The seven new NotificationDaoTest instrumented cases compile into the androidTest APK, but no device or emulator was attached (adb devices was empty), so they were not executed in this unit. The slice-level logcat check (archive/snooze/open logging) belongs to T04 and is not applicable yet.

## Verification Evidence

| # | Command | Exit Code | Verdict | Duration |
|---|---------|-----------|---------|----------|
| 1 | `./gradlew :feature:feature-notifications:testDebugUnitTest :app:assembleDebugAndroidTest` | 0 | pass | 21569ms |

## Deviations

The existing S02 DAO instrumented tests live in app/src/androidTest/java/com/usagecontrol/db/NotificationDaoTest.kt, not core/core-database/src/androidTest, so the new tests were added there (this matches the :app:assembleDebugAndroidTest verify command). Other changes beyond the plan: nowStreamFlow requires a non-null key for the live branch; both flows take a default LIMIT; recentlyDismissedFlow also excludes summaries and ongoing rows; ARCHIVED was added to NotificationAction and the mapper made tolerant; NowStream functions take NotificationRecord.

## Known Issues

The new instrumented DAO tests were compiled but not run because no device was attached. Run ./gradlew :app:connectedDebugAndroidTest on a device to execute them.

## Files Created/Modified

- `core/core-database/src/main/java/com/usagecontrol/core/database/dao/NotificationDao.kt`
- `core/core-database/src/main/java/com/usagecontrol/core/database/Mappers.kt`
- `core/core-model/src/main/java/com/usagecontrol/core/model/NotificationRecord.kt`
- `feature/feature-notifications/src/main/java/com/usagecontrol/feature/notifications/NowStream.kt`
- `feature/feature-notifications/src/test/java/com/usagecontrol/feature/notifications/NowStreamTest.kt`
- `feature/feature-notifications/build.gradle.kts`
- `feature/feature-notifications/src/main/java/com/usagecontrol/feature/notifications/NotificationsScreen.kt`
- `app/src/androidTest/java/com/usagecontrol/db/NotificationDaoTest.kt`
<!-- gsd:state-version=186:1 -->
