# Doorman (AndroidUsageControl)

## Vision
A personal, privacy-first Android app that stands between the world and you: it lets the right notifications through immediately, holds the rest until you ask, and remembers everyone who came by. It also manages screen time and sound/DND via modes. Personal use first; quality bar high enough for later public release.

**Core value:** Help users become aware of and take control of their phone usage patterns without sacrificing privacy.

**Design source:** `.planning/research/DESIGN-REVIEW.md` (+ `.html` mockups) and `.planning/research/DOORMAN-MILESTONES.md` (2026-11-03).

## Product model
- **Lanes** — user-defined buckets (People, Money, Deliveries, Work, Live, Noise) with colour, delivery policy (Instant / Batched / Quiet / Digest / Blocked), decay and breakthrough keywords/people.
- **Screening** — each new sender is screened once and routed forever; heuristics become re-screening prompts.
- **Delivery** — non-Instant lanes are mirrored under Doorman's channels; batches at set times with bounded deferral, cooldown and breakthroughs.
- **Memory** — every removal archived with reason; undo, restore, search, keyword pages, digest.
- **Modes** — presets become modes that change lanes, block apps (friction, not walls) and set sound/DND.
- **Bridge (opt-in, pending privacy decision)** — context to the user's own SemPKM instance, never notification content.

## Shell
Three tabs: Now, Apps, Modes; Settings behind a gear. Dark-first LED-dot theme, Material 3 Expressive.

## Unified Rules Engine
Shared composable conditions; per-pillar actions. `Rule { id, name, enabled, priority, conditions (AND), conditionGroups (OR between, AND within), actions, pillar, createdBy }`.

## Stack
Kotlin, Jetpack Compose + Material 3 (moving to 1.5 Expressive), MVVM/clean architecture, Hilt, Room, Coroutines/Flow, WorkManager + foreground service, Gradle version catalogs. minSdk 29. applicationId stays `com.usagecontrol`.

## Modules
`app/`; `core/{core-common, core-database, core-rules, core-ui, core-model}`; `feature/{feature-notifications→feature-now, feature-learning (removed in M007), feature-screentime, feature-lockcontrol, feature-settings, feature-onboarding, feature-rules}`; `service/{service-notification, service-usage, service-context}`; planned `service-bridge`.

## Milestone Sequence
- [x] M001: v0.1 Scaffold — Module structure and basic stubs only; pillars not yet functional end-to-end
- [x] M002: v0.2 User Testing — Onboarding, permission walkthrough, welcome tour and quick setup (device-verified, Maestro E2E)
- [ ] M003: v0.3 Learning & Blocking (parked) — Superseded by Doorman M008/M009
- [ ] M004: v0.4 Context & Lock (parked) — Superseded by Doorman M009
- [ ] M005: v0.5 Polish (parked) — Superseded by Doorman M007/M008
- [ ] M007: v0.3 Doorman — Rename, theme, listener foundation, three-tab shell and Now stream
- [ ] M008: v0.4 Lanes — Lanes, Screener, mirroring, batched delivery, archive
- [ ] M009: v0.5 Modes and bridge — Modes, screen-time friction, SemPKM bridge
- [ ] M006: v1.0 Stable Release — Stable personal release, all pillars fully functional

Later (v1.x+): ML learning, widgets, backup/export, public release polish.

## Risks
- Toolchain bump (M007/S01) can stall everything; do it first and alone.
- Mirroring changes what per-app system notification settings point at; Screener copy must explain it once.
- The bridge contradicts the current no-internet decision (D002); README and PROJECT.md must change in the same commit if it is accepted.

## Sources ingested
PROJECT.md, README.md, .planning/{REQUIREMENTS,ROADMAP,STATE}.md, .planning/phases/*/SUMMARY.md, .planning/research/{DESIGN-REVIEW,DOORMAN-MILESTONES}.md. `.planning/codebase/*` left in place as architecture reference.
