# S04: Apps and Modes v1 — Research

**Date:** 2026-11-04

## Summary

This is targeted research. All the data S04 needs already exists: v4 `notifications` rows (packageName, channelId, timestamp, userAction, actionTimestamp, removalReason, removedAt), `usage_records` (UsageDao), `suggestions` (SuggestionDao), `sound_presets` (SoundPresetDao plus SoundPreset/RingerMode), and time-limit rules (`ScreenTimeViewModel.createTimeLimitRule` creates a rule with action param `dailyLimitMinutes`). The work is to add new aggregate DAO queries, pure stats functions with JVM tests, and two feature screens that replace `AppsPlaceholderScreen` and `ModesPlaceholderScreen`.

The one architectural constraint: feature modules depend only on core-* modules (see the feature-lockcontrol and feature-notifications build files), and `SoundProfileExecutor.applyPreset(SoundPreset)` lives in service-context. Follow the S03 NotificationActions pattern: put an interface in core-common (e.g. `SoundProfileApplier`), implement it in service-context by delegating to SoundProfileExecutor, and bind it with Hilt.

## Recommendation

Do the stats in SQL plus pure Kotlin, following the S03 NowStream pattern: the DAO returns raw rows or day buckets, and pure functions compute alerts/day, sparkline series, dismiss rate, median time to act and hourly bars, each with a JVM test. Put Apps in feature-notifications (it already owns notification data and NowStream) or in a new package inside it. Put Modes in feature-lockcontrol, which already deals with presets and ringer mode, or in a small new feature module. Reuse `SimpleLineChart`/`SimpleBarChart`/`DonutChart` (core-ui Charts.kt) for the sparkline, hourly bars and ring. Reuse `ContextStrip`, `SectionHeader`, `PolicySegment` and `LaneDot` from DoormanComponents.kt.

## Implementation Landscape

### Key Files

- `app/src/main/java/com/usagecontrol/navigation/PlaceholderScreens.kt`: delete it, or keep only for fallback. Replace in `MainActivity.kt:187-188` (`composable(AppScreen.APPS.route)` / `MODES`). Add an app profile route `apps/{packageName}`, alongside the existing argument routes at MainActivity ~216/233.
- `core/core-database/.../dao/NotificationDao.kt`: add these queries:
  - per-app counts since a cutoff (`GROUP BY packageName`, with COUNT and dismissed count where userAction/removalReason is CANCEL/ARCHIVED)
  - per-app day buckets for the sparkline (`timestamp/86400000` grouping or local-day computed in Kotlin)
  - per-package rows since a cutoff, for hourly bars and time-to-act (`actionTimestamp - timestamp`, or `removedAt - timestamp`)
  - per-package `channelId` counts
  - Expose these as Flow. Indices already exist on packageName/timestamp/userAction, so no migration is needed.
- `core/core-database/.../dao/UsageDao.kt`: `getByPackageAndDateRange` and `getTotalTimeForAppOnDate` already supply screen time for the profile and the Modes screen-time summary (`getDailyTotals`).
- `core/core-database/.../dao/SuggestionDao.kt`: `getPendingFlow()`, filtered by `appPackageName`, gives profile suggestions. `markAccepted`/`updateStatus` already exist.
- `core/core-database/.../dao/RuleDao.kt` and `feature-screentime/ScreenTimeViewModel.kt:76`: the existing limit-rule creation. Extract it or reuse it so the profile's "screen-time limit" can read and set `dailyLimitMinutes`.
- `core/core-database/.../dao/SoundPresetDao.kt` and `core/core-model/SoundPreset.kt`: Modes preset cards come from `getAllFlow()`. "Toggle applies the sound profile" means calling the new bridge with the preset. Seed default presets (e.g. Focus/Sleep/Normal as MANUAL) only if the DAO is empty. Decide whether the toggle also flips `enabled` or just applies once (recommendation: apply plus track an active id in the ViewModel or DataStore).
- `service/service-context/.../SoundProfileExecutor.kt` (`applyPreset`, `setDnd`) and `ActionHandlersModule.kt`: bind the new core-common interface here. DND needs notification policy access; the executor already handles that, but the UI should surface failure.
- `core/core-common/.../NotificationActions.kt`: the template for the bridge interface.
- `core/core-common/.../AppNameResolver.kt`: app labels. Reuse `NotificationIcon.kt` (feature-notifications) for app icons.
- `core/core-ui/.../component/Charts.kt` and `DoormanComponents.kt`: chart and component reuse. A compact sparkline may need a small `Sparkline` composable in core-ui. Add it to ComponentGallery.

### Build Order

1. DAO aggregate queries, pure `AppStats` functions and JVM tests. This is the biggest unblocker.
2. Apps list screen and ViewModel (sorted by alerts/day, sparkline, dismiss hint "you dismiss N%"), then the route wiring.
3. App profile screen (ring, median time to act, hourly bars, channels, limit, suggestions).
4. SoundProfileApplier bridge in core-common, its service-context impl and Hilt binding.
5. Modes screen (preset cards with toggle, ContextStrip, screen-time summary from UsageDao.getDailyTotals), then remove the placeholders.

Seams: steps 1→2→3 and steps 4→5 are independent tracks.

### Verification Approach

- `./gradlew assembleDebug check :app:assembleDebugAndroidTest` must exit 0.
- JVM tests for the stats functions: median, empty input, day bucketing across midnight, and hourly histogram with 24 entries.
- Compile instrumented DAO tests for the new queries (no device attached, so they may not run).
- testTags: `apps_list`, `app_profile`, `modes_list`, `mode_toggle_<id>`. grep for them.
- On-device UAT: toggling a mode changes the ringer mode.

## Constraints

- Feature modules must not depend on service modules; use the core-common interface bridge (D010-era pattern from S03 T04).
- applicationId and packages stay `com.usagecontrol` (D010).
- Legacy v3 rows have NULL channelId/key. Group them as "Unknown channel" and exclude them from time-to-act when actionTimestamp is null.

## Common Pitfalls

- **Ringer/DND permission:** setting SILENT or DND without notification policy access throws SecurityException. Catch it in the impl and return a result for a snackbar.
- **SoundPresetApplier auto-apply:** the context poller may override a manually applied preset (MANUAL presets are never auto-applied, but TIME_RANGE/WIFI ones are). Document this or accept it for v1.
- **Day bucketing:** use local-zone day start (TimeUtils), not UTC `/86400000`, or sparklines will shift.
<!-- gsd:state-version=156:1 -->
