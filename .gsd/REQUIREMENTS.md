# Requirements

This file is the explicit capability and coverage contract for the project.

## Active

### R004 — Habit learning surfaces as re-screening prompts ("You dismissed 40 of 42 from Zalando in under 3 s. Move to Noise?") in the app profile and Screener, replacing rule-phrased suggestion cards
- Class: differentiator
- Status: active
- Description: Habit learning surfaces as re-screening prompts ("You dismissed 40 of 42 from Zalando in under 3 s. Move to Noise?") in the app profile and Screener, replacing rule-phrased suggestion cards
- Why it matters: Pillar 3 — the "intelligent assistant" differentiator
- Source: PROJECT.md Pillar 3, roadmap v0.3
- Primary owning slice: M008/S01
- Notes: Re-scoped by Doorman design review 2026-11-03; heuristics (D004) unchanged

### R005 — Screen-time friction: pause-before-open for limited/blocked apps, session lengths, mode-based app blocking, screen-time summary on Modes
- Class: core-capability
- Status: active
- Description: Screen-time friction: pause-before-open for limited/blocked apps, session lengths, mode-based app blocking, screen-time summary on Modes
- Why it matters: Pillar 4 enforcement beyond advisory
- Source: PROJECT.md Pillar 4, roadmap v0.3
- Primary owning slice: M009/S02
- Notes: Replaces soft/hard overlay blocking per D012

### R006 — Context conditions and lock/sound profiles: location (geofence/WiFi), calendar, charging, Bluetooth conditions; volume/ringer/DND/lock-timeout actions; named presets (Home, Work, Sleep, Meeting)
- Class: core-capability
- Status: active
- Description: Context conditions and lock/sound profiles: location (geofence/WiFi), calendar, charging, Bluetooth conditions; volume/ringer/DND/lock-timeout actions; named presets (Home, Work, Sleep, Meeting)
- Why it matters: Pillar 5 automation
- Source: PROJECT.md Pillar 5, roadmap v0.4
- Primary owning slice: M009/S01
- Notes: Presets become Modes (AutomaticZenRule + ZenDeviceEffects); geofence/calendar/Bluetooth context partly via M009/S03 context push. Lock control remains unowned — revisit before M006

### R007 — Stats dashboards and retention: notification volume/heatmap/trends, usage charts vs previous periods, configurable retention (default 30d) with WorkManager cleanup
- Class: quality-attribute
- Status: active
- Description: Stats dashboards and retention: notification volume/heatmap/trends, usage charts vs previous periods, configurable retention (default 30d) with WorkManager cleanup
- Why it matters: Awareness half of the core value
- Source: PROJECT.md Pillars 1 & 4, roadmap v0.5
- Primary owning slice: M008/S04
- Supporting slices: M007/S04
- Notes: Retention extended to archive, icons and parcels in M008/S04; per-app stats in Apps profile (M007/S04). Old dashboards are removed, not polished

### R009 — No mandatory permission gate, account creation, long marketing carousel, mascot, personalization quiz, or push-notification opt-in
- Class: anti-feature
- Status: active
- Description: No mandatory permission gate, account creation, long marketing carousel, mascot, personalization quiz, or push-notification opt-in
- Why it matters: Each adds friction or breaks trust for an offline utility app
- Source: .planning/REQUIREMENTS.md Out of Scope

### R014 — Lanes and Screener (LANE, SCRN): lane entity (name, colour, policy Instant/Batched/Quiet/Digest/Blocked, decay, sound, breakthroughs); six seeded defaults; per-app/channel/person routes; new senders screened once with one-tap routing that re-files history; category defaults until screened
- Class: differentiator
- Status: active
- Description: Lanes and Screener (LANE, SCRN): lane entity (name, colour, policy Instant/Batched/Quiet/Digest/Blocked, decay, sound, breakthroughs); six seeded defaults; per-app/channel/person routes; new senders screened once with one-tap routing that re-files history; category defaults until screened
- Why it matters: Decide once, route forever — the core of the new notification model
- Source: DESIGN-REVIEW.md §3.1-3.2, DOORMAN-MILESTONES.md v0.4
- Primary owning slice: M008/S01

### R015 — Mirroring and delivery (MIRR, DLVR): mirrors for non-Instant lanes with lane channels/groups, decay, restore and RemoteInput reply; per-lane batch times on AlarmManager, bounded deferral, per-sender cooldown, breakthrough bypass, batch summary notification, snooze-to-time for mirrors
- Class: core-capability
- Status: active
- Description: Mirroring and delivery (MIRR, DLVR): mirrors for non-Instant lanes with lane channels/groups, decay, restore and RemoteInput reply; per-lane batch times on AlarmManager, bounded deferral, per-sender cooldown, breakthrough bypass, batch summary notification, snooze-to-time for mirrors
- Why it matters: Makes the app change how the phone behaves rather than report on it
- Source: DESIGN-REVIEW.md §3.3-4, DOORMAN-MILESTONES.md v0.4
- Primary owning slice: M008/S02
- Supporting slices: M008/S03

### R016 — Archive and memory (ARCH): every removal archived with reason; search and lane/app filters; restore single and batch as working mirrors; keyword pages promotable to breakthroughs; on-device template digest; export
- Class: continuity
- Status: active
- Description: Archive and memory (ARCH): every removal archived with reason; search and lane/app filters; restore single and batch as working mirrors; keyword pages promotable to breakthroughs; on-device template digest; export
- Why it matters: Nothing the user or a rule removes is ever lost
- Source: DESIGN-REVIEW.md §3.4, DOORMAN-MILESTONES.md v0.4
- Primary owning slice: M008/S04

### R017 — SemPKM bridge: opt-in service-bridge module (sole INTERNET holder, off until URL + API key) pushing context (zone, activity, time period, calendar, focus_mode, notification_pressure) and receiving persona switches into modes; never sends notification content
- Class: integration
- Status: active
- Description: SemPKM bridge: opt-in service-bridge module (sole INTERNET holder, off until URL + API key) pushing context (zone, activity, time period, calendar, focus_mode, notification_pressure) and receiving persona switches into modes; never sends notification content
- Why it matters: Original goal of being a SemPKM context provider
- Source: DESIGN-REVIEW.md §3.5, DOORMAN-MILESTONES.md v0.5
- Primary owning slice: M009/S03
- Notes: Unblocked by the decision that supersedes D002 (bridge-only INTERNET, opt-in, context only).

## Validated

### R001 — Onboarding foundation (ONBD-01..04): first-launch only via DataStore flag, routing before NavHost renders, skippable at any step, v0.1 upgraders with permissions auto-skip
- Class: launchability
- Status: validated
- Description: Onboarding foundation (ONBD-01..04): first-launch only via DataStore flag, routing before NavHost renders, skippable at any step, v0.1 upgraders with permissions auto-skip
- Why it matters: First impression for user testing without trapping existing users
- Source: .planning/REQUIREMENTS.md v0.2
- Primary owning slice: M002

### R002 — Permission walkthrough (PERM-01..05): priming screen per permission, opens correct Settings screen (NLS, Usage Access, DND), detects grant on return with checkmark, step progress, degraded mode with messaging when skipped
- Class: launchability
- Status: validated
- Description: Permission walkthrough (PERM-01..05): priming screen per permission, opens correct Settings screen (NLS, Usage Access, DND), detects grant on return with checkmark, step progress, degraded mode with messaging when skipped
- Why it matters: App is useless without special permissions; users need context before Settings
- Source: .planning/REQUIREMENTS.md v0.2
- Primary owning slice: M002

### R003 — Welcome & tour (TOUR-01..04): welcome screen, 5-tab HorizontalPager tour with dots, quick setup offering Night Mode preset, tour revisitable from Settings
- Class: primary-user-loop
- Status: validated
- Description: Welcome & tour (TOUR-01..04): welcome screen, 5-tab HorizontalPager tour with dots, quick setup offering Night Mode preset, tour revisitable from Settings
- Why it matters: Communicates value and gets one rule active immediately
- Source: .planning/REQUIREMENTS.md v0.2
- Primary owning slice: M002

### R010 — Brand and theme (BRND-01..06): Doorman label + LED-dot adaptive icon, DoormanTheme static dark/light, LocalLaneColors tokens, Manrope/DM Mono with tabular numerals, MaterialExpressiveTheme on material3 1.5.0-beta02, shared core-ui components with previews
- Class: launchability
- Status: validated
- Description: Brand and theme (BRND-01..06): Doorman label + LED-dot adaptive icon, DoormanTheme static dark/light, LocalLaneColors tokens, Manrope/DM Mono with tabular numerals, MaterialExpressiveTheme on material3 1.5.0-beta02, shared core-ui components with previews
- Why it matters: App needs an identity where lane colour is semantic
- Source: .planning/research/DOORMAN-MILESTONES.md v0.3
- Primary owning slice: M007/S01
- Validation: M007: Doorman label and dark theme observed on emulator; build green

### R011 — Listener foundation (LSTN-01..09): store key/channel/importance/conversation/flags; icons as files; actions and in-memory PendingIntent cache; key-matched removals with reason; upsert on re-post; ongoing as live state; reconcile on connect; MessagingStyle messages; provisional lane classification. Room v3→v4 without data loss
- Class: core-capability
- Status: validated
- Description: Listener foundation (LSTN-01..09): store key/channel/importance/conversation/flags; icons as files; actions and in-memory PendingIntent cache; key-matched removals with reason; upsert on re-post; ongoing as live state; reconcile on connect; MessagingStyle messages; provisional lane classification. Room v3→v4 without data loss
- Why it matters: Current listener matches by timestamp, duplicates updates and captures nothing visual; blocks every notification feature
- Source: DESIGN-REVIEW.md §1.3, DOORMAN-MILESTONES.md v0.3
- Primary owning slice: M007/S02
- Validation: M007: on-device post/update/snooze → 1 row, updateCount=1, SNOOZED, icon file; MigrationTest v3→v4 + NotificationDaoTest pass

### R012 — Shell and Now (SHEL-01..04, NOW-01..10): Now/Apps/Modes tabs with Settings gear; time-sectioned stream with lane LED rows and bundles; swipe archive with 10 s undo; swipe snooze to clock time; search; hero interruption count; recently dismissed; tap opens original contentIntent; context strip
- Class: primary-user-loop
- Status: validated
- Description: Shell and Now (SHEL-01..04, NOW-01..10): Now/Apps/Modes tabs with Settings gear; time-sectioned stream with lane LED rows and bundles; swipe archive with 10 s undo; swipe snooze to clock time; search; hero interruption count; recently dismissed; tap opens original contentIntent; context strip
- Why it matters: Now is the daily surface of the product
- Source: DOORMAN-MILESTONES.md v0.3
- Primary owning slice: M007/S03
- Validation: M007: shell on device; NotificationActionsTest + SettingsBackTest pass (24/24 instrumented)

### R013 — Apps and Modes v1 (APPS-01/02, MODE-01): apps list by alerts/day with sparkline and dismiss hint; app profile with ring, median time to act, hourly bars, channels, screen-time limit, suggestions; Modes tab with preset cards, context strip and screen-time summary
- Class: primary-user-loop
- Status: validated
- Description: Apps and Modes v1 (APPS-01/02, MODE-01): apps list by alerts/day with sparkline and dismiss hint; app profile with ring, median time to act, hourly bars, channels, screen-time limit, suggestions; Modes tab with preset cards, context strip and screen-time summary
- Why it matters: Every v0.2 feature needs a home in the three-tab shell
- Source: DOORMAN-MILESTONES.md v0.3
- Primary owning slice: M007/S04
- Validation: M007: unit tests + assembleDebug pass; Apps/Modes in installed shell

## Deferred

### R008 — Contextual guidance and enhanced permission UX (GUID-01/02, PERM-06/07): coach marks per tab, tooltips on key UI, NLS requestRebind after grant, per-tab degraded-feature explanations
- Class: primary-user-loop
- Status: deferred
- Description: Contextual guidance and enhanced permission UX (GUID-01/02, PERM-06/07): coach marks per tab, tooltips on key UI, NLS requestRebind after grant, per-tab degraded-feature explanations
- Why it matters: Follow-ups from v0.2 user testing
- Source: .planning/REQUIREMENTS.md Future Requirements

## Out of Scope

## Traceability

| ID | Class | Status | Primary owner | Supporting | Proof |
| --- | --- | --- | --- | --- | --- |
| R001 | launchability | validated | M002 | none | unmapped |
| R002 | launchability | validated | M002 | none | unmapped |
| R003 | primary-user-loop | validated | M002 | none | unmapped |
| R004 | differentiator | active | M008/S01 | none | unmapped |
| R005 | core-capability | active | M009/S02 | none | unmapped |
| R006 | core-capability | active | M009/S01 | none | unmapped |
| R007 | quality-attribute | active | M008/S04 | M007/S04 | unmapped |
| R008 | primary-user-loop | deferred | none | none | unmapped |
| R009 | anti-feature | active | none | none | unmapped |
| R010 | launchability | validated | M007/S01 | none | M007: Doorman label and dark theme observed on emulator; build green |
| R011 | core-capability | validated | M007/S02 | none | M007: on-device post/update/snooze → 1 row, updateCount=1, SNOOZED, icon file; MigrationTest v3→v4 + NotificationDaoTest pass |
| R012 | primary-user-loop | validated | M007/S03 | none | M007: shell on device; NotificationActionsTest + SettingsBackTest pass (24/24 instrumented) |
| R013 | primary-user-loop | validated | M007/S04 | none | M007: unit tests + assembleDebug pass; Apps/Modes in installed shell |
| R014 | differentiator | active | M008/S01 | none | unmapped |
| R015 | core-capability | active | M008/S02 | M008/S03 | unmapped |
| R016 | continuity | active | M008/S04 | none | unmapped |
| R017 | integration | active | M009/S03 | none | unmapped |

## Coverage Summary

- Active requirements: 9
- Mapped to slices: 8
- Validated: 7 (R001, R002, R003, R010, R011, R012, R013)
- Unmapped active requirements: 1
