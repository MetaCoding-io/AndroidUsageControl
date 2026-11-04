# GSD State

**Active Milestone:** M007: v0.3 Doorman
**Active Slice:** S01: Toolchain and theme
**Phase:** planning
**Requirements Status:** 13 active · 3 validated · 1 deferred · 0 out of scope

## Milestone Registry
- 🔄 **M007:** v0.3 Doorman
- ⬜ **M008:** v0.4 Lanes
- ⬜ **M009:** v0.5 Modes and bridge
- ⬜ **M006:** v1.0 Stable Release
- ⏸️ **M003:** v0.3 Learning & Blocking
- ⏸️ **M004:** v0.4 Context & Lock
- ⏸️ **M005:** v0.5 Polish

## Recent Decisions
- D010 (Design review 2026-11-03 (DOORMAN-MILESTONES D1), M007): App name and identity -> App is Doorman; applicationId stays com.usagecontrol and Kotlin packages are not renamed; only label, icon, theme name and copy change
- D011 (Design review 2026-11-03 (DOORMAN-MILESTONES D3), M008): Mirroring non-Instant lanes -> For Batched, Quiet, Digest and Blocked lanes the listener cancels the original and re-posts a mirror (recoverBuilder) under Doorman's channels with the original icon as large icon and app label as sub-text; Instant and Live lanes are never touched
- D012 (Design review 2026-11-03, M009 S02): Screen time enforcement mechanism -> Friction instead of walls: pause-before-open (breathing timer, why? note) and mode-based app blocking replace the advisory/soft/hard overlay levels
- D013 (Design review 2026-11-03, M007): Information architecture and visual identity -> Three tabs (Now, Apps, Modes) with Settings behind a gear; dark-first static palette with LED-dot lane motif, dynamic colour opt-in (accent only), Manrope + DM Mono, Material 3 Expressive (material3 1.5.0-beta02)
- D014 (Doorman design review 2026-11-03, confirmed by user 2026-11-04): Data residency and network use -> Local-first: Room storage, no accounts, no push. Nothing leaves the device except context, sent only to the user's own SemPKM instance and never notification content. Only the service-bridge module holds INTERNET, and it stays off until an instance URL and API key are configured.

## Blockers
- None

## Next Action
Slice S01 has no DB tasks. Plan slice tasks before execution.
