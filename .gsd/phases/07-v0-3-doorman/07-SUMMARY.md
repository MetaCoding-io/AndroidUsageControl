---
id: M007
title: "v0.3 Doorman"
status: complete
completed_at: 2026-11-06T03:10:48.963Z
key_decisions:
  - AGP 9.5 built-in Kotlin + compileSdk 37 for material3 Expressive
  - KSP build guard and ksp.incremental=false against missing Hilt_ classes
  - core-common bridge interfaces for NotificationActions and SoundProfileApplier
key_files:
  - build.gradle.kts
  - gradle.properties
  - app/src/main/java/com/usagecontrol/MainActivity.kt
  - app/src/main/res/values-night/themes.xml
  - core/core-common/src/main/java/com/usagecontrol/core/common/SoundProfileApplier.kt
  - feature/feature-settings/src/main/java/com/usagecontrol/feature/settings/SettingsScreen.kt
lessons_learned:
  - Incremental KSP can drop Hilt_ classes; keep the guard
  - values-night window theme needed alongside Compose dark theme
  - Ensure adb is on PATH before device verification
---

# M007: v0.3 Doorman

**Rebranded to Doorman with a dark-first Material 3 Expressive theme, Room v4 listener foundation, Now/Apps/Modes shell, and build/navigation/dark-mode hardening**

## What Happened

S01 upgraded the toolchain (AGP 9.5, compileSdk 37, material3 1.5.0-beta02) and added DoormanTheme, lane colours, typography and shared core-ui components. S02 rewrote the notification listener on Room v4 with key upsert, removal reasons, icon files, a PendingIntent cache and a lossless migration. S03 built the three-tab shell and Now stream with swipe archive/snooze and tap-to-open. S04 delivered the Apps list and profile and Modes preset cards that apply sound profiles. S05 guarded against the Hilt/KSP missing-class crash, added a Settings back arrow and made the window follow dark mode via values-night.

## Success Criteria Results

- Doorman name, theme, tabs on device: PASS (emulator launch, dark screenshot, SettingsBackTest)
- Listener data model: PASS (on-device row with updateCount=1, SNOOZED, icon; MigrationTest, NotificationDaoTest)

## Definition of Done Results

All 5 slices complete with summaries; 24/24 instrumented tests and unit tests/assembleDebug pass; validation receipt pass.

## Requirement Outcomes

R010, R011, R012, R013: active → validated (evidence per validation receipt).

## Deviations

S05 could not reproduce the Hilt crash; root cause inferred.

## Follow-ups

Hand-drive swipe/snooze gestures on Now; keep doorman_window_background in sync with core-ui Paper/Ink; manual mode may be overridden by auto presets; profile 'today' midnight rollover.
<!-- gsd:state-version=186:1 -->
