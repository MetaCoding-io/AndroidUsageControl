---
id: T01
parent: S05
milestone: M007
key_files:
  - build.gradle.kts
  - gradle.properties
  - app/build.gradle.kts
  - service/service-usage/build.gradle.kts
key_decisions:
  - Fail the build after KSP if any @AndroidEntryPoint lacks a generated Hilt_ class, instead of relying on runtime detection; @HiltAndroidApp is excluded because its Hilt_ class comes from hiltJavaCompile under the aggregating task
  - ksp.incremental=false project-wide to remove the incremental-snapshot path suspected of leaving empty KSP output
duration:
verification_result: passed
completed_at:
blocker_discovered: false
---

# T01: Disabled KSP incremental processing, made Hilt aggregation explicit, and added a root Gradle guard that fails the build when KSP emits no Hilt_ class for an @AndroidEntryPoint

**Disabled KSP incremental processing, made Hilt aggregation explicit, and added a root Gradle guard that fails the build when KSP emits no Hilt_ class for an @AndroidEntryPoint**

## What Happened

Reproduction: before any change, 3 consecutive `./gradlew clean :app:assembleDebug` runs all produced Hilt_UsageMonitorService, both in service/service-usage/build/generated/ksp/debug/java and in the APK dex (apkanalyzer dex packages, 11 matching entries). 4 incremental scenarios also all produced it: touching UsageMonitorService.kt, an ABI change in core-common, reverting, and cleaning only the module. The `--dry-run` graph shows the correct order: :service:service-usage:kspDebugKotlin → compileDebugKotlin → transformDebugClassesWithAsm → bundleLib* → :app:hiltAggregateDepsDebug → :app:hiltJavaCompileDebug. A missing task dependency is ruled out.

Root cause (best evidence; not reproduced): the crash is silent at build time by design. The Hilt Gradle plugin's ASM transform (transformDebugClassesWithAsm) rewrites the superclass of the @AndroidEntryPoint `UsageMonitorService : Service()` to Hilt_UsageMonitorService in bytecode. If KSP emits nothing for the module, Kotlin compile still succeeds and the APK ships a reference to a class that does not exist, which gives NoClassDefFoundError at launch. Task ordering is correct, so the likely trigger is KSP's incremental snapshot path leaving the module with empty output (stale kspCaches or interleaved IDE and CLI daemons).

Since bisection was impossible, I applied the candidates and added a guard:
(1) `ksp.incremental=false` in gradle.properties, which removes the incremental-snapshot path.
(2) Explicit `hilt { enableAggregatingTask = true }` in app and service-usage. This is already the default in 2.60.1; it is now explicit.
(3) A root build.gradle.kts `subprojects` hook for every module that applies both the Hilt and KSP plugins. After each non-test `ksp<Variant>Kotlin` task it scans src/main .kt files for `@AndroidEntryPoint class X` and fails with a GradleException naming the module and class if `Hilt_X.java` is missing from build/generated/ksp/<variant>/java. The first version also checked @HiltAndroidApp. That gave a false positive on UsageControlApp, because with the aggregating task its Hilt_ class comes from hiltJavaCompile, not KSP. The guard is now limited to @AndroidEntryPoint.

After the fix, from a cold daemon: 5/5 consecutive clean builds each contained Hilt_UsageMonitorService in the generated source and the APK dex. The APK installed on emulator-5554 (Medium_Phone_API_36.1) and cold-launched MainActivity (Status ok, process alive); logcat had 0 NoClassDefFoundError/FATAL EXCEPTION lines and an empty crash buffer.

## Verification

Ran ./gradlew clean :app:assembleDebug (exit 0) and ./gradlew check (exit 0) via gsd_exec. Ad-hoc loop: 5/5 clean builds had the generated Hilt_UsageMonitorService.java and the class in the APK dex. Negative guard test: a probe GuardProbe.kt with a commented @AndroidEntryPoint made :service:service-usage:kspDebugKotlin fail with "produced no Hilt_ class for GuardProbe"; the probe was then removed. Emulator: installed and cold-launched with no crash in logcat.

## Verification Evidence

| # | Command | Exit Code | Verdict | Duration |
|---|---------|-----------|---------|----------|
| 1 | `./gradlew clean :app:assembleDebug` | 0 | pass | 29200ms |
| 2 | `./gradlew check` | 0 | pass | 94423ms |

## Deviations

The crash could not be reproduced (0 failures in 7 clean and 4 incremental builds), so a true bisect was impossible. I applied the planned candidates together and added a build-time guard that was not in the plan. KSP/Hilt version bumps were skipped: nothing was failing to bisect against, and the current versions build correctly.

## Known Issues

Root cause is inferred, not proven, because the crash never reproduced. If it comes back, the guard now fails the build at :<module>:kspDebugKotlin with a clear message instead of shipping a crashing APK. ksp.incremental=false makes incremental builds of KSP modules slightly slower.

## Files Created/Modified

- `build.gradle.kts`
- `gradle.properties`
- `app/build.gradle.kts`
- `service/service-usage/build.gradle.kts`
<!-- gsd:state-version=178:1 -->
