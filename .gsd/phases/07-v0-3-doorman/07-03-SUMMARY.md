---
id: S03
parent: M007
milestone: M007
provides:
  - (none)
requires:
  []
affects:
  []
key_files: []
key_decisions: []
patterns_established:
  - (none)
observability_surfaces:
  - logcat tag NotificationCapture: archive/snooze/open with key and outcome (cancelled, snoozed, listener-unavailable, intent-missing->launch-fallback)
drill_down_paths:
  []
duration: ""
verification_result: passed
completed_at: 2026-11-04T06:40:49.256Z
blocker_discovered: false
---

# S03: Shell and Now

**Now/Apps/Modes shell with Settings gear; Now stream with real v4 rows, icons, lane dots, sections/bundles, hero count, search, recently dismissed, swipe archive+10s undo, swipe snooze-to-time, tap-to-open via core NotificationActions bridge**

## What Happened

T01 replaced the five-tab shell with Now/Apps/Modes tabs (nav_tab_*), a settings_gear in the TopAppBar, and S04 placeholder screens. T02 added the DAO queries (nowStreamFlow, recentlyDismissedFlow, todayInterruptionCount, markUserAction/clearUserAction), added NotificationAction.ARCHIVED, and wrote the pure NowStream functions (toLane, sectionize, bundle, filter) with JVM tests. T03 built NowViewModel and NowScreen: hero count, context strip, search (now_search), section headers, rows and bundles, a collapsible recently dismissed list, and safe icon loading backed by an LruCache. T04 added the NotificationActions interface in core-common. NotificationActionsImpl in service-notification implements it using the listener instance plus PendingIntentCache and falls back to the launch intent. It is bound with Hilt. T04 also wired swipe archive with a 10 s deferred cancel and undo, swipe-left snooze through a TimePicker (with a pure duration function and its test), and tap to open with snackbar fallbacks.

## Verification

gsd_exec e523de28: `./gradlew assembleDebug check :app:assembleDebugAndroidTest` exited 0. The feature-notifications unit test suites passed (28 + 5 tests, 0 failures). The settings_gear and now_search testTags are present. Instrumented DAO tests compile but were not run because no device was attached. On-device behaviour is deferred to UAT.

## Requirements Advanced

- R012 — Shell and Now surface implemented; on-device UAT pending for validation

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

Instrumented DAO tests have not been executed (no device). Swipe, snooze and open have not been verified on device yet; UAT covers them. Settings has no back arrow.

## Follow-ups

Run :app:connectedDebugAndroidTest on a device.

## Files Created/Modified

None.
<!-- gsd:state-version=134:1 -->
