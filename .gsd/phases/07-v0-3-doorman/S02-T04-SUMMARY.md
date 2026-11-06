---
id: T04
parent: S02
milestone: M007
key_files:
  - service/service-notification/src/main/java/com/usagecontrol/service/notification/IconStore.kt
  - service/service-notification/src/main/java/com/usagecontrol/service/notification/PendingIntentCache.kt
  - service/service-notification/src/test/java/com/usagecontrol/service/notification/IconStorePathTest.kt
  - service/service-notification/build.gradle.kts
  - core/core-database/src/main/java/com/usagecontrol/core/database/dao/NotificationDao.kt
  - feature/feature-settings/build.gradle.kts
  - feature/feature-settings/src/main/java/com/usagecontrol/feature/settings/CleanupWorker.kt
  - feature/feature-settings/src/main/java/com/usagecontrol/feature/settings/SettingsViewModel.kt
key_decisions:
  - Icon paths are stored relative to filesDir (icons/<pkg>/<sha256>.png); delete/prune only touch paths that match that strict pattern
  - Pruning uses a 10-minute mtime grace so it can't race a save whose row isn't inserted yet
  - getAllIconPaths unions history and live_notifications so live icons are never pruned
  - feature-settings depends on service-notification to reach IconStore (no cycle)
duration:
verification_result: passed
completed_at:
blocker_discovered: false
---

# T04: Added the IconStore singleton (on-disk PNG icons keyed by hash, prune safe against races) and the PendingIntentCache singleton, plus NotificationDao.getAllIconPaths. CleanupWorker and Settings cleanup now prune icons that no row references.

**Added the IconStore singleton (on-disk PNG icons keyed by hash, prune safe against races) and the PendingIntentCache singleton, plus NotificationDao.getAllIconPaths. CleanupWorker and Settings cleanup now prune icons that no row references.**

## What Happened

IconStore (service-notification, @Singleton, @Inject with @ApplicationContext):
- save(pkg, icon) loads the drawable through createPackageContext(pkg, 0), falling back to the app context. It scales the drawable to fit 192px and keeps the aspect ratio; when intrinsic size is ≤0 it uses 192x192. It encodes PNG, names the file by SHA-256, and writes filesDir/icons/<pkg>/<hash>.png by writing a .tmp file and renaming it. It returns the path relative to filesDir, or null on any failure. Package names are checked against a regex, so a crafted package name can't escape the directory.
- delete(paths) only deletes paths that match the strict icons/<pkg>/<64hex>.png pattern.
- pruneUnreferenced(referenced, graceMs = 10 min) walks the icon folders and deletes files that nothing references and that are older than the grace window. The window keeps an icon the listener just saved, before its row is inserted. It also removes empty package folders and logs counts under the IconStore tag.

PendingIntentCache (@Singleton, @Inject constructor()): a ConcurrentHashMap from key to Entry(contentIntent, actionIntents) with put/get/contentIntent/actionIntent/remove/clear/size. A put with no intents at all removes the entry.

NotificationDao.getAllIconPaths() returns the UNION of smallIconPath and largeIconPath from notifications plus smallIconPath from live_notifications.

CleanupWorker and SettingsViewModel.runCleanup now call iconStore.pruneUnreferenced(getAllIconPaths().toSet()) after the retention deletes; the ViewModel runs it on Dispatchers.IO. feature-settings now depends on :service:service-notification, and service-notification gained testImplementation(libs.junit) plus a JVM test for the pure path and size helpers.

## Verification

./gradlew :app:assembleDebug passes. ./gradlew :service:service-notification:testDebugUnitTest passes: IconStorePathTest, 9/9 tests, 0 failures, confirmed from the JUnit XML. An earlier piped run was replaced with these unpiped runs so the exit codes are valid evidence. IconStore.save (bitmap rendering and file writes) was not exercised on a device; that happens through the T05 listener flow.

## Verification Evidence

| # | Command | Exit Code | Verdict | Duration |
|---|---------|-----------|---------|----------|
| 1 | `./gradlew :service:service-notification:testDebugUnitTest` | 0 | pass | 1861ms |
| 2 | `./gradlew :app:assembleDebug` | 0 | pass | 13622ms |

## Deviations

The plan's file list didn't name feature-settings, but the plan said to extend CleanupWorker and SettingsViewModel 'plus module deps', so feature-settings/build.gradle.kts gained the service-notification dependency. Added a JVM test file and junit to service-notification for the Q7 gate.

## Known Issues

IconStore.save and pruneUnreferenced haven't run on a device yet; they first run in T05's listener flow and the daily cleanup. Nothing calls PendingIntentCache until T05.

## Files Created/Modified

- `service/service-notification/src/main/java/com/usagecontrol/service/notification/IconStore.kt`
- `service/service-notification/src/main/java/com/usagecontrol/service/notification/PendingIntentCache.kt`
- `service/service-notification/src/test/java/com/usagecontrol/service/notification/IconStorePathTest.kt`
- `service/service-notification/build.gradle.kts`
- `core/core-database/src/main/java/com/usagecontrol/core/database/dao/NotificationDao.kt`
- `feature/feature-settings/build.gradle.kts`
- `feature/feature-settings/src/main/java/com/usagecontrol/feature/settings/CleanupWorker.kt`
- `feature/feature-settings/src/main/java/com/usagecontrol/feature/settings/SettingsViewModel.kt`
<!-- gsd:state-version=156:1 -->
