# AndroidUsageControl

## Vision
A personal, privacy-first Android app (everything local, no internet/auth) that acts as an intelligent assistant for managing notifications, screen time, lock state, and sound via user-defined rules and learned habits. Personal use first; quality bar high enough for later public release.

**Core value:** Help users become aware of and take control of their phone usage patterns without sacrificing privacy.

## Five Pillars
1. **Notification History** — NotificationListenerService capture → Room; browse/filter/search; stats; configurable retention (default 30d) via WorkManager.
2. **Dismissal Rules** — conditions (app, keyword, time, day, priority/category, sender) → dismiss / mute / snooze; every firing logged.
3. **Habit Learning** — heuristics first (dismiss-vs-open ratio, time-to-dismiss, >90% dismiss over 7d → suggest rule); ML-ready data model (TFLite later); card-based suggestions.
4. **Screen Time** — UsageStatsManager; dashboards; daily limits, schedule blocks, cooldowns; enforcement advisory / soft block / hard block via SYSTEM_ALERT_WINDOW overlay + foreground service.
5. **Lock & Sound** — conditions (WiFi/geofence, time, calendar, charging, Bluetooth) → volume, ringer, DND, lock timeout; named presets (Home, Work, Sleep, Meeting).

## Unified Rules Engine
Shared composable conditions; per-pillar actions. `Rule { id, name, enabled, priority, conditions (AND), conditionGroups (OR between, AND within), actions, pillar: NOTIFICATION|SCREEN_TIME|LOCK_SOUND, createdBy: USER|SUGGESTION|LEARNED }`.

## Stack
Kotlin, Jetpack Compose + Material 3, MVVM/clean architecture, Hilt, Room, Coroutines/Flow, WorkManager + foreground service, Gradle version catalogs. minSdk 29.

## Modules
`app/`; `core/{core-common, core-database, core-rules, core-ui, core-model}`; `feature/{feature-notifications, feature-learning, feature-screentime, feature-lockcontrol, feature-settings, feature-onboarding, feature-rules}`; `service/{service-notification, service-usage, service-context}`.

## Milestone Sequence
- [x] M001: v0.1 Scaffold — Module structure and basic stubs only; pillars not yet functional end-to-end
- [x] M002: v0.2 User Testing — Onboarding, permission walkthrough, welcome tour and quick setup (device-verified, Maestro E2E)
- [ ] M003: v0.3 Learning & Blocking — Habit learning suggestions, screen time soft/hard blocking
- [ ] M004: v0.4 Context & Lock — Location/calendar context, lock control, sound profiles
- [ ] M005: v0.5 Polish — Stats dashboards, configurable retention, settings polish
- [ ] M006: v1.0 Stable Release — Stable personal release, all pillars fully functional

Later (v1.x+): ML learning, widgets, backup/export, public release polish.

## Sources ingested
PROJECT.md, README.md, .planning/{REQUIREMENTS,ROADMAP,STATE}.md, .planning/phases/*/SUMMARY.md. `.planning/codebase/*` left in place as architecture reference.
