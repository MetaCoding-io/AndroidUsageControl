# M007/S02 Research: Listener foundation

## Summary

The listener is a single 110-line `NotificationCaptureService` that inserts a fresh row on every post, matches removals by `postTime ± 1 s`, and captures no icons, actions, channel, or ranking data. The Room layer is v3 with `exportSchema = true` but no schema directory and no `room.schemaLocation` KSP argument, so there is no exported v3 schema to test a migration against, and no test infrastructure exists in any core module. Every test path (unit or instrumented) has to be built from zero. Recommended first proof: export the v3 schema at HEAD before touching the entity, then write MIGRATION_3_4 with a migration test, then key-based upsert in the DAO.

## Recommendation

1. Before editing any entity, add `ksp { arg("room.schemaLocation", "$projectDir/schemas") }` to `core/core-database/build.gradle.kts` and run a build at HEAD so `schemas/.../3.json` is generated. Commit that baseline.
2. Extend `NotificationEntity` to v4 per LSTN-01 (key, id, tag, channelId, importance, isConversation, shortcutId, whenMs, groupKey, isGroupSummary, flags, updateCount, removalReason, icon paths, color, provisionalLane, messagesJson, actionsJson, parcel path). Add `LiveNotificationEntity` and DAO. Write `MIGRATION_3_4`. Keep `userAction` so SuggestionEngine and the stats queries keep working.
3. Key-based upsert: do NOT use Room `@Upsert` for this. It conflicts on the primary key, not on a unique `key` index. Use a `@Transaction` that looks up the id by `key`, then inserts or updates.
4. Make post and remove ordering deterministic: both currently `scope.launch` on `Dispatchers.IO` in parallel, so a remove can overtake its post. Use a single-threaded dispatcher (`Dispatchers.IO.limitedParallelism(1)`, available with coroutines 1.7.3) or a Channel.
5. Classifier, icon store, and intent cache are pure or isolated pieces; build them after the schema so the service rewrite has seams.

## Implementation Landscape

### Key files (absolute paths under /home/james/Code/AndroidUsageControl)

Listener (service-notification):
- `service/service-notification/src/main/java/com/usagecontrol/service/notification/NotificationCaptureService.kt`. Inserts on every post (`notificationDao.insert`). Removal matches via `notificationDao.search(packageName, startTime=postTime-1000, endTime=postTime+1000, limit=1)` and only writes CLICK→OPENED, else DISMISSED. Auto-dismiss rule path calls `markAutoDismissed` then `cancelNotification(sbn.key)`. `onListenerConnected` absent. `onDestroy` cancels scope.
- Manifest: `app/src/main/AndroidManifest.xml`. Service `com.usagecontrol.service.notification.NotificationCaptureService`, exported, BIND_NOTIFICATION_LISTENER_SERVICE permission.
- `service/service-notification/build.gradle.kts`: depends on core-model, core-database, core-rules, core-common. No test deps.

Database (core-database):
- `core/core-database/src/main/java/com/usagecontrol/core/database/UsageControlDatabase.kt`. `version = 3`, `exportSchema = true`, six entities. Migrations: `MIGRATION_1_2` (ALTER TABLE rules), `MIGRATION_2_3` (CREATE suggestions). Companion-object vals, not a separate file.
- `core/core-database/src/main/java/com/usagecontrol/core/database/entity/NotificationEntity.kt`. Table `notifications`, indices on packageName, timestamp, userAction. Auto-generated Long id. Columns: packageName, appName, title, text, subText, category, priority, timestamp, groupKey, isOngoing, userAction (String), actionTimestamp, ruleIdIfAutoDismissed. No `key`.
- `core/core-database/src/main/java/com/usagecontrol/core/database/dao/NotificationDao.kt`. insert, update, getAllFlow, getByPackageFlow, getById, getDistinctAppsFlow, search (±time window), updateAction, markAutoDismissed, deleteOlderThan, getCount, getDismissalsByHour, getDismissStats, getDailyStats, getActionBreakdown. Stats queries count every row with no isOngoing filter.
- `core/core-database/src/main/java/com/usagecontrol/core/database/Mappers.kt`: `NotificationEntity.toModel()` / `NotificationRecord.toEntity()`. Must be extended for new fields.
- `core/core-database/src/main/java/com/usagecontrol/core/database/DatabaseModule.kt`: `Room.databaseBuilder(..., "usage_control.db").addMigrations(MIGRATION_1_2, MIGRATION_2_3)`. Add MIGRATION_3_4 here. No fallbackToDestructiveMigration (good; keep it that way).
- `core/core-database/build.gradle.kts`: room runtime, ktx, ksp compiler, hilt, coroutines. No room-testing, no junit, no androidTest. minSdk 29.
- `core/core-database/build/` contains generated artefacts only, no schema JSON.

Model and consumers:
- `core/core-model/src/main/java/com/usagecontrol/core/model/NotificationRecord.kt`. Mirror of entity plus `NotificationAction` enum (NONE, DISMISSED, OPENED, SNOOZED, AUTO_DISMISSED). Add new fields with defaults.
- `core/core-rules/src/main/java/com/usagecontrol/core/rules/SuggestionEngine.kt`: calls `getDismissStats` and `getDismissalsByHour`. Any change to the DAO/stats SQL affects it. core-rules has no test deps.
- `feature/feature-notifications/src/main/java/com/usagecontrol/feature/notifications/NotificationsViewModel.kt` and `NotificationsScreen.kt` (uses NotificationRecord).
- `feature/feature-settings/.../SettingsViewModel.kt` (getCount, deleteOlderThan) and `CleanupWorker.kt` (deleteOlderThan). Deleting rows here must also delete icon/parcel files (new), or they orphan.
- `feature/feature-learning/.../LearningViewModel.kt` uses stats queries (module removed in M007 per PROJECT.md).

Rule engine and common:
- `core/core-rules/src/main/java/com/usagecontrol/core/rules/RuleEngine.kt`: `findMatchingRules(RulePillar.NOTIFICATION, EvaluationContext)`. Used by listener for DISMISS_NOTIFICATION.
- `core/core-common/src/main/java/com/usagecontrol/core/common/AppNameResolver.kt`.

DI: Hilt everywhere. `DatabaseModule` (SingletonComponent) provides db and DAOs. `NotificationCaptureService` is `@AndroidEntryPoint` with field injection of `NotificationDao` and `RuleEngine`. New stores (IconStore, PendingIntentCache, ParcelStore) should be `@Singleton` providers in a module so the service and future UI share them. Note the service's `RuleEngine` injection needs core-rules' Hilt module to be visible (it already works).

Tests: none. `find` for `test/`, `androidTest/`, and `schemas/` directories across the repo returned nothing. `app/build.gradle.kts` has `testImplementation(libs.junit)`, `androidTestImplementation(libs.junit.ext)`, `espresso`, and `testInstrumentationRunner = androidx.test.runner.AndroidJUnitRunner`. Only the app module has any test wiring.
- `gradle/libs.versions.toml`: room 2.6.1, ksp 1.9.22-1.0.17, kotlin 1.9.22, hilt 2.50, coroutines 1.7.3, agp 8.2.2, junit 4.13.2. No `room-testing` entry. Add `androidx.room:room-testing:2.6.1`.

SDK: minSdk 29, targetSdk 34, compileSdk 34 in all modules (app targetSdk 34). Ranking and Notification APIs must be checked against 29 (see Pitfalls).

Specs and docs:
- `.archive/planning/research/DOORMAN-MILESTONES.md` lines 57-66 (LSTN-01..09), Phase 5 plans at roughly lines 118-140: 05-01 schema v4, 05-02 capture service rewrite, 05-03 icon/intent/lane stores. Suggested decisions: icons as PNG at `filesDir/icons/<package>/<hash>.png`, PendingIntents process-scoped only, classifier as pure function in core-rules with unit tests, keep `userAction` one milestone and add `removalReason` beside it.
- `.archive/planning/research/DESIGN-REVIEW.md` section 1.3 (~line 88): listed listener defects 1-7. Matches this audit.
- `.gsd/REQUIREMENTS.md` line 59-62 and line 168: R011 mapped to M007/S02, status active, owner unmapped.
- `.gsd/DECISIONS.md`: D010 (applicationId stays com.usagecontrol, packages not renamed) and D014 (local-first, no egress of notification content).
- `.gsd/phases/07-v0-3-doorman/07-ROADMAP.md`: S02 line and boundary map "Not provided".
- `.gsd/KNOWLEDGE.md`: empty. `.gsd/PREFERENCES.md`: default.

Success criteria (from Phase 5 roadmap, which maps to S02):
1. Post, update, and remove a test notification gives one row with correct reason and update count.
2. Media/foreground-service notifications land in the live table, not history or stats.
3. Icons load from disk after listener process restart.
4. After reconnect, live table matches `getActiveNotifications()`.

## Build Order

1. Schema export baseline: add `room.schemaLocation` to core-database; add `room-testing` to the version catalog and to core-database's `androidTestImplementation`; build at HEAD; commit `schemas/.../3.json`.
2. Entity and migration: v4 `NotificationEntity` (add `key`, `removalReason`, `updateCount`, etc., all with NOT NULL defaults in Kotlin and matching SQL defaults), `LiveNotificationEntity`, `MIGRATION_3_4` (ALTER TABLE ADD COLUMN for each, CREATE INDEX on key, CREATE live table, backfill `key` as NULL for legacy rows). Bump version to 4, add to DatabaseModule, export `4.json`. Write the migration test (see First Proof).
3. DAO: `findIdByKey(key)`, `insertNew`, `updateOnRepost(id, text, time, count)`, `markRemoved(id, reason, ts)`, live-table DAO (upsert by key, delete by key, getAll, reconcile delete-not-in-list). Update stats SQL to exclude `isOngoing = 1` and legacy-ongoing rows (LSTN-06).
4. Mappers and `NotificationRecord`: new fields with defaults so NotificationsScreen compiles unchanged.
5. Pure `ProvisionalLaneClassifier` in core-rules (add `testImplementation(libs.junit)` to core-rules build). Inputs: category, channel importance, isConversation, reserved bundle channel ids (SDK-guarded). Unit tests are pure JVM.
6. `IconStore` (`Icon.loadDrawable` → PNG → `filesDir/icons/<pkg>/<hash>.png`, dedup by hash), `PendingIntentCache` (keyed by sbn.key, cleared on remove and on listener disconnect), `NotificationParcelStore` (optional for v0.4, can defer).
7. Service rewrite: single-thread dispatcher; `onNotificationPosted` (branch on isOngoing → live table; otherwise key-upsert with update count; extract extras, MessagingStyle messages, actions with RemoteInput flag, channel and ranking via `rankingMap` lookup by key, icons via IconStore, cache intents); `onNotificationRemoved(sbn, rankingMap, reason)` (key match, map reason, route ongoing to live delete); `onListenerConnected` (reconcile live table with `activeNotifications`). Keep DISMISS rule path, now keyed by row id from upsert.
8. Cleanup: CleanupWorker and SettingsViewModel delete paths also remove icon files for deleted rows.

## First Proof

Smallest slice to prove first, before the service:
- Migration test `MigrationTest.migrate3To4_preservesRows` using `MigrationTestHelper` (needs `room-testing` and the `schemas` directory as test assets). Seed three v3 rows including one with `userAction = 'DISMISSED'`, run migration, assert count, `packageName`, `timestamp`, `userAction` unchanged, and that `key` is NULL for legacy rows.
- DAO upsert test: insert, then upsert with same key increments `updateCount` and keeps the same id; remove sets `removalReason` and does not change row count.

Where to put it: the app module already has `androidTest` wiring and a runner (`app/build.gradle.kts`), so the least-friction home is `app/src/androidTest/...`, with `sourceSets` adding `core/core-database/schemas` as assets. Alternative: add the runner to core-database. Planner should choose; app-level is faster.

## Verification Approach

Build and unit:
```
cd /home/james/Code/AndroidUsageControl
./gradlew :core:core-database:kspDebugKotlin           # regenerates schema JSON
./gradlew :core:core-rules:testDebugUnitTest           # classifier tests
./gradlew :app:assembleDebug
./gradlew :app:connectedDebugAndroidTest --tests '*MigrationTest*'   # needs device/emulator
```

Device (real behaviour; requires listener access granted):
```
adb devices
adb shell cmd notification allow_listener com.usagecontrol/com.usagecontrol.service.notification.NotificationCaptureService
adb shell cmd notification post -S bigtext -t "Test" doorman-test-1 "first body"
adb shell cmd notification post -S bigtext -t "Test" doorman-test-1 "updated body"   # same tag => same key => should upsert
adb shell cmd notification cancel doorman-test-1                                    # verify subcommand with: adb shell cmd notification help
```
Inspect DB on device (debug build only):
```
adb shell run-as com.usagecontrol sqlite3 databases/usage_control.db "select id,key,updateCount,removalReason,userAction from notifications where packageName='com.android.shell';"
```
Caveat: shell-posted notifications are attributed to `com.android.shell`, so the key includes that package. Shell posts have no custom icon; to test icons after restart, use a notification from an app that sets a large icon, or a tiny test app in a separate directory (not in this repo).

Restart and reconnect:
```
adb shell am force-stop com.usagecontrol      # kills listener; system rebinds on next post
adb shell cmd notification disallow_listener com.usagecontrol/...NotificationCaptureService
adb shell cmd notification allow_listener com.usagecontrol/...NotificationCaptureService   # triggers onListenerConnected
```
Check icon file exists: `adb shell run-as com.usagecontrol ls files/icons`.

Migration on real data: install the current v0.2 build (versionCode 1, applicationId com.usagecontrol), create rows, then install the new build over it (`adb install -r`) and confirm rows survive. This is the "without data loss" acceptance check.

## Constraints

- applicationId and package names stay (D010). Do not rename packages.
- Room schema export must be configured before the entity changes, or the v3 baseline is lost.
- Room `@Upsert` conflicts on primary key, so it cannot upsert by `key`.
- Legacy rows have no key. A UNIQUE index on `key` is fine in SQLite because NULLs do not collide, so keep legacy rows NULL rather than synthesising keys.
- Keep `userAction` and existing stats semantics for v0.2 consumers until v0.4 (per phase-5 decision).
- No notification content leaves the device (D014). Icons and parcels stay in `filesDir`.
- No destructive migration fallback.

## Common Pitfalls

- Post/remove race: parallel coroutines on IO can apply a remove before its post, leaving a row that never closes. Serialise.
- `Ranking` and `Notification` APIs vary by level. minSdk is 29, so guard APIs that are later: `Ranking.isConversation()` and `getConversationShortcutInfo()` are API 30+; `Notification.Action.getSemanticAction()` is API 31+; `Notification.getShortcutId()` is API 30+. Verify each against the SDK before use. `recoverBuilder` is API 29, `Icon.loadDrawable` API 23, `MessagingStyle` API 24, `getActiveNotifications()` API 21.
- `REASON_*` constants: `REASON_SNOOZED`, `REASON_TIMEOUT`, `REASON_GROUP_SUMMARY_CANCELED`, `REASON_LISTENER_CANCEL`, `REASON_APP_CANCEL` are all in the onNotificationRemoved(reason) set. Confirm levels for 29.
- `Icon.loadDrawable` can throw or return null for resources from another package. Load with the source package context and catch.
- Listener disconnect loses in-memory PendingIntents. The row must render "no longer available" (NOW-09), and the cache must not be the only source of truth for ids.
- NOT NULL columns added via ALTER TABLE need a SQL DEFAULT. Room's post-migration validation compares the schema, so Kotlin defaults alone do not satisfy SQLite.
- Legacy ongoing rows exist in v3 history. Decide whether migration moves them out, or stats exclude `isOngoing = 1` going forward. The stats SQL currently counts them.
- Deleting old rows (CleanupWorker, SettingsViewModel delete-all) orphans icon files unless deletion is extended.
- SuggestionEngine and LearningViewModel depend on the stats queries. Any SQL change must keep their output.
- No test scaffolding exists. Every module that gets a test needs its own `testImplementation`/`androidTestImplementation` lines.

## Open Risks

- Android 16 QPR1 reserved bundle channel ids (LSTN-09) need an API 36 device and the exact channel id strings. compileSdk and targetSdk are 34, so those symbols are not available at compile time; use string constants behind an SDK check. Confirm device OS before relying on it.
- Room migration test needs `room-testing` 2.6.1 and the schema JSON as test assets; if the baseline is not committed at HEAD first, the test cannot be written for v3.
- `adb shell cmd notification` subcommand names (post/cancel) vary by Android version; run `adb shell cmd notification help` on the target device first.
- The Phase 5 plan doubles as the design doc. LSTN-09 provisional lane categories may need product input; the classifier is a pure function so this is isolated.
- Parcelled Notification storage (05-03 T2) is needed only for v0.4 recoverBuilder; can be deferred without failing S02 criteria.
<!-- gsd:state-version=110:1 -->
