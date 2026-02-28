# AndroidUsageControl

## What This Is

A privacy-first Android app that helps users see and control how they use their phone. It captures notifications, tracks screen time, automates sound/lock profiles, and suggests intelligent rules — all on-device with no internet permission. Built with Kotlin, Jetpack Compose, and Material 3 across 14 Gradle modules.

## Core Value

Help users become aware of and take control of their phone usage patterns — screen time, notification noise, and device behavior — without sacrificing privacy.

## Requirements

### Validated

- ✓ Notification capture and history via NotificationListenerService — v0.1.0
- ✓ Notification feed with app filtering, expandable cards, and action tracking — v0.1.0
- ✓ Notification dashboard with daily trends, stat cards, and donut chart — v0.1.0
- ✓ Auto-dismiss notifications via rule engine evaluation — v0.1.0
- ✓ Screen time tracking via UsageStatsManager polling (per-app, per-day) — v0.1.0
- ✓ Screen time dashboard with trend charts, daily average, and peak day stats — v0.1.0
- ✓ Per-app time limits with one-tap dialog — v0.1.0
- ✓ Screen time blocking overlays (ADVISE, SOFT_BLOCK, HARD_BLOCK) — v0.1.0
- ✓ Context monitoring service (WiFi SSID, charging, Bluetooth, calendar) — v0.1.0
- ✓ Sound presets with time-based and WiFi-based triggers — v0.1.0
- ✓ Quick-setup Night Mode and Meeting Mode presets — v0.1.0
- ✓ Sound profile execution (ringer mode, per-stream volume, DND) — v0.1.0
- ✓ Compound rule engine with condition groups (OR between groups, AND within) — v0.1.0
- ✓ 10 condition types (APP_PACKAGE, TIME_RANGE, DAY_OF_WEEK, WIFI_SSID, KEYWORD_MATCH, etc.) — v0.1.0
- ✓ 10 action types (DISMISS, MUTE, SNOOZE, ADVISE, SOFT/HARD_BLOCK, SET_VOLUME, etc.) — v0.1.0
- ✓ Full rule editor UI with pillar selection, priority slider, and condition group builder — v0.1.0
- ✓ Heuristic suggestion engine (high dismiss rate, quick dismiss, time-concentrated, heavy usage) — v0.1.0
- ✓ Suggestion cards with confidence bar and accept/dismiss — v0.1.0
- ✓ Per-app insight stats with dismiss/open rate and hourly distribution chart — v0.1.0
- ✓ Settings screen with permission shortcuts, data retention picker, auto-cleanup — v0.1.0
- ✓ Export rules as JSON to clipboard — v0.1.0
- ✓ Boot receiver to restart foreground services — v0.1.0
- ✓ Room database v3 with proper SQL migrations — v0.1.0
- ✓ WorkManager daily cleanup with configurable retention — v0.1.0

### Active

(None yet — no active milestone)

### Out of Scope

- ML/TensorFlow Lite model — heuristics sufficient for now, revisit when data patterns warrant it
- Real-time chat or social features — this is a personal utility
- Cloud sync / backup — conflicts with privacy-first principle unless E2E encrypted
- iOS version — Android-only for the foreseeable future

## Context

- 14-module Gradle project using Kotlin DSL and version catalogs (libs.versions.toml)
- MVVM + clean architecture: core-model (pure Kotlin), core-database (Room), core-rules (engine), core-ui (theme/components)
- 5 feature modules mapping to bottom nav tabs: notifications, screentime, lockcontrol, learning, rules
- 3 service modules: notification listener, usage monitor, context monitor
- Custom Compose Canvas charts (line, bar, donut) — no third-party chart library
- Min SDK 29 (Android 10), Target/Compile SDK 34 (Android 14)
- Hilt 2.50 with KSP, Compose BOM 2024.02.00, Room 2.6.1
- Successfully builds to debug APK; release build has minification disabled
- Branded as Metacoding Solutions LLC, v0.1.0

## Constraints

- **Privacy**: No internet permission. All data on-device in SQLite. Non-negotiable.
- **Permissions**: Requires Notification Access, Usage Access, DND Access — each needs manual user grant
- **Background limits**: Android restricts background work; relies on foreground services and WorkManager
- **Target audience**: Broad consumer audience — UI must be intuitive, not power-user-only

## Key Decisions

| Decision | Rationale | Outcome |
|----------|-----------|---------|
| No internet permission | Privacy-first architecture, all data on-device | ✓ Good |
| Custom Canvas charts over chart library | Fewer dependencies, full control, small APK | ✓ Good |
| Heuristics over ML for suggestions | Simpler, no model training needed, good enough for v1 | — Pending |
| 14-module architecture | Clean separation, parallel builds, testability | ✓ Good |
| Room with SQL migrations over auto-migration | More control over schema evolution | ✓ Good |
| Foreground services over JobScheduler | More reliable polling, user-visible indicators | — Pending |

---
*Last updated: 2026-02-28 after GSD initialization*
