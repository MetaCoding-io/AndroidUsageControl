---
id: T03
parent: S02
milestone: M007
key_files:
  - core/core-model/src/main/java/com/usagecontrol/core/model/ProvisionalLane.kt
  - core/core-rules/src/main/java/com/usagecontrol/core/rules/ProvisionalLaneClassifier.kt
  - core/core-rules/src/test/java/com/usagecontrol/core/rules/ProvisionalLaneClassifierTest.kt
  - core/core-rules/build.gradle.kts
key_decisions:
  - Lane persisted by enum name in the existing String? column; ProvisionalLane.fromName returns null for unknown values
  - Precedence PEOPLE > bundle channel > PRIORITY > QUIET > UPDATES; bundle channels override importance since the system already classified them
  - Ongoing notifications are not promoted to PRIORITY by importance alone, and remote-input on ongoing notifications does not imply PEOPLE
  - IMPORTANCE_UNSPECIFIED (-1000) treated as default, not as low
duration:
verification_result: passed
completed_at:
blocker_discovered: false
---

# T03: Added ProvisionalLane enum (PRIORITY/PEOPLE/UPDATES/QUIET) in core-model and a pure ProvisionalLaneClassifier + LaneInput in core-rules, with 33 passing JVM unit tests

**Added ProvisionalLane enum (PRIORITY/PEOPLE/UPDATES/QUIET) in core-model and a pure ProvisionalLaneClassifier + LaneInput in core-rules, with 33 passing JVM unit tests**

## What Happened

Created core-model ProvisionalLane enum with fromName() for parsing the persisted String? column added in T02 (NotificationEntity.provisionalLane). Created core-rules ProvisionalLaneClassifier object with LaneInput(category, channelId, importance, isConversation, isOngoing, hasRemoteInputAction). Android importance/category constants and Android 16 reserved bundle channel ids (android.app.promotions/social/recs -> QUIET, android.app.news -> UPDATES) are mirrored as string/int literals so no SDK symbols are needed. Precedence: PEOPLE (conversation, msg/call category, or remote-input action on non-ongoing) > bundle channel mapping > PRIORITY (alarm/reminder, or importance >= HIGH when not ongoing) > QUIET (promo/social/recommendation, or importance <= LOW excluding UNSPECIFIED) > UPDATES. Category matching is case-insensitive. Added testImplementation(libs.junit) to core-rules and ProvisionalLaneClassifierTest covering every branch, precedence conflicts and boundaries.

## Verification

./gradlew :core:core-rules:testDebugUnitTest passed; test report shows tests=33 failures=0 errors=0 skipped=0.

## Verification Evidence

| # | Command | Exit Code | Verdict | Duration |
|---|---------|-----------|---------|----------|
| 1 | `./gradlew :core:core-rules:testDebugUnitTest` | 0 | pass | 13994ms |

## Deviations

hasRemoteInputAction and isOngoing had no explicit rule in the plan; used remote input as a PEOPLE signal (non-ongoing only) and ongoing as a guard against importance-based PRIORITY. Added ProvisionalLane.fromName helper.

## Known Issues

none

## Files Created/Modified

- `core/core-model/src/main/java/com/usagecontrol/core/model/ProvisionalLane.kt`
- `core/core-rules/src/main/java/com/usagecontrol/core/rules/ProvisionalLaneClassifier.kt`
- `core/core-rules/src/test/java/com/usagecontrol/core/rules/ProvisionalLaneClassifierTest.kt`
- `core/core-rules/build.gradle.kts`
<!-- gsd:state-version=161:1 -->
