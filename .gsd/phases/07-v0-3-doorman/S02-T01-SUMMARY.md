---
id: T01
parent: S02
milestone: M007
key_files:
  - core/core-database/build.gradle.kts
  - gradle/libs.versions.toml
  - app/build.gradle.kts
  - core/core-database/schemas/com.usagecontrol.core.database.UsageControlDatabase/3.json
key_decisions:
  - Used the KSP arg room.schemaLocation instead of the Room Gradle plugin, so the plugin catalog did not change
  - Pinned androidx.test runner 1.5.2 / core 1.5.0 to match the versions already cached and the existing junitExt/espresso
duration:
verification_result: passed
completed_at:
blocker_discovered: false
---

# T01: Enabled Room schema export (KSP room.schemaLocation), generated the v3 baseline schema, and wired room-testing plus the schema assets into app androidTest

**Enabled Room schema export (KSP room.schemaLocation), generated the v3 baseline schema, and wired room-testing plus the schema assets into app androidTest**

## What Happened

core-database already had exportSchema = true but no schemaLocation, so no schema was ever written. Added `ksp { arg("room.schemaLocation", "$projectDir/schemas") }` to core/core-database/build.gradle.kts and kept the KSP arg route rather than adding the Room Gradle plugin. In gradle/libs.versions.toml, added room-testing (version.ref room 2.8.5), androidx-test-runner 1.5.2 and androidx-test-core 1.5.0. Those two versions were chosen because they are already in the local Gradle cache, next to junitExt 1.1.5 and espresso 3.5.1. In app/build.gradle.kts, added `sourceSets["androidTest"].assets.srcDir(project(":core:core-database").file("schemas"))` so MigrationTestHelper can find the schema JSONs, along with androidTestImplementation for the runner, core and room-testing. No entity was changed. kspDebugKotlin at HEAD generated core/core-database/schemas/com.usagecontrol.core.database.UsageControlDatabase/3.json with "version": 3 and 6 tables. The schemas directory is not gitignored, so the baseline will be committed.

## Verification

kspDebugKotlin exited 0. 3.json exists, with version 3 and 6 entity tables. :app:assembleDebug exited 0. The androidTest APK was not assembled in this task; T02 verifies it with :app:assembleDebugAndroidTest.

## Verification Evidence

| # | Command | Exit Code | Verdict | Duration |
|---|---------|-----------|---------|----------|
| 1 | `./gradlew :core:core-database:kspDebugKotlin -q` | 0 | pass | 1575ms |
| 2 | `test -f core/core-database/schemas/com.usagecontrol.core.database.UsageControlDatabase/3.json` | 0 | pass | 5ms |
| 3 | `./gradlew :app:assembleDebug -q` | 0 | pass | 1314529ms |

## Deviations

None.

## Known Issues

The androidTest APK build was not run here; T02 covers it.

## Files Created/Modified

- `core/core-database/build.gradle.kts`
- `gradle/libs.versions.toml`
- `app/build.gradle.kts`
- `core/core-database/schemas/com.usagecontrol.core.database.UsageControlDatabase/3.json`
<!-- gsd:state-version=161:1 -->
