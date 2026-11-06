---
id: T01
parent: S04
milestone: M007
key_files:
  - core/core-database/src/main/java/com/usagecontrol/core/database/dao/NotificationDao.kt
  - feature/feature-notifications/src/main/java/com/usagecontrol/feature/notifications/apps/AppStats.kt
  - feature/feature-notifications/src/test/java/com/usagecontrol/feature/notifications/apps/AppStatsTest.kt
  - app/src/androidTest/java/com/usagecontrol/db/NotificationDaoTest.kt
key_decisions:
  - User-dismissed = removalReason CANCEL/CANCEL_ALL, userAction ARCHIVED, or legacy DISMISSED with NULL removalReason; APP_CANCEL etc. not counted
  - Added appDailyCountsFlow (SQL local-day buckets) plus AppStats.dailySeriesFromDayCounts so the Apps list sparklines avoid loading every raw row
duration:
verification_result: passed
completed_at:
blocker_discovered: false
---

# T01: Added four per-app Flow aggregate queries to NotificationDao and a pure AppStats object (alerts/day, local-day series, dismiss rate, median time to act, 24-hour histogram, channel breakdown), covered by 10 JVM tests and 4 compile-only DAO instrumented tests

**Added four per-app Flow aggregate queries to NotificationDao and a pure AppStats object (alerts/day, local-day series, dismiss rate, median time to act, 24-hour histogram, channel breakdown), covered by 10 JVM tests and 4 compile-only DAO instrumented tests**

## What Happened

NotificationDao gained appAlertCountsFlow(since) (per-package total + user-dismissed, busiest first), appDailyCountsFlow(since) (per-package local-day 'yyyy-MM-dd' buckets via SQLite localtime, for list sparklines without loading raw rows), appEventsFlow(packageName, since) (timestamp, actionTimestamp, removedAt, channelId oldest first) and appChannelCountsFlow(packageName, since) (channelId nullable). Row classes AppAlertCountRow, AppDayCountRow, AppEventRow, ChannelCountRow. All exclude ongoing and group-summary rows; existing packageName/timestamp indices cover them, no migration. 'Dismissed' = removalReason CANCEL/CANCEL_ALL OR userAction ARCHIVED OR (legacy v3) userAction DISMISSED with NULL removalReason — app-side cancels (which markRemoved also maps to DISMISSED) are deliberately excluded. AppStats (feature-notifications/apps) is pure: alertsPerDay, dailySeries(timestamps, days, zone, now) using java.time local dates (DST-safe), dailySeriesFromDayCounts for SQL buckets, dismissRate (clamped, 0 on empty), medianTimeToAct (skips null and negative gaps, null on empty), hourlyHistogram (always 24), channelBreakdown (null/blank -> 'Unknown channel', merged, sorted desc). One test fixture was initially wrong (a 'future' timestamp was still today) and was corrected to tomorrow.

## Verification

./gradlew :feature:feature-notifications:testDebugUnitTest :app:assembleDebugAndroidTest succeeded; AppStatsTest 10/10 passing; :app:compileDebugAndroidTestKotlin executed with the new NotificationDaoTest methods (compile only, not run on device).

## Verification Evidence

| # | Command | Exit Code | Verdict | Duration |
|---|---------|-----------|---------|----------|
| 1 | `./gradlew :feature:feature-notifications:testDebugUnitTest :app:assembleDebugAndroidTest` | 0 | pass | 6221ms |

## Deviations

Added an extra query appDailyCountsFlow and helper dailySeriesFromDayCounts beyond the plan, for the Apps-list sparklines. dailySeries takes an explicit `now` parameter for testability.

## Known Issues

The DAO instrumented tests are compiled but have not run on a device. appDailyCountsFlow buckets using SQLite 'localtime', which is the device zone, and can't be exercised in JVM tests.

## Files Created/Modified

- `core/core-database/src/main/java/com/usagecontrol/core/database/dao/NotificationDao.kt`
- `feature/feature-notifications/src/main/java/com/usagecontrol/feature/notifications/apps/AppStats.kt`
- `feature/feature-notifications/src/test/java/com/usagecontrol/feature/notifications/apps/AppStatsTest.kt`
- `app/src/androidTest/java/com/usagecontrol/db/NotificationDaoTest.kt`
<!-- gsd:state-version=161:1 -->
