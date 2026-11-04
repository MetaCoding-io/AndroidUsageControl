# Doorman: milestone, phase and task breakdown

**Date:** 2026-11-03
**Source:** `.planning/research/DESIGN-REVIEW.md` (the redesign) and its HTML version with mockups.
**Purpose:** A ready-to-run plan for `/gsd-new-milestone`, `/gsd-discuss-phase` and
`/gsd-plan-phase`. Terminology follows GSD Core as used in this repo: milestone → phase →
plan → task (GSD Pi calls phases "slices"). Phase numbers continue from v0.2 (last was 3).

## Locked decisions (2026-11-03)

| # | Decision | Consequence |
|---|---|---|
| D1 | The app is **Doorman**. | Label, launcher icon and in-app copy change. `applicationId` stays `com.usagecontrol` so existing installs upgrade in place and notification-listener access survives; Kotlin packages are not renamed (cost, no user value). |
| D2 | **Privacy rule rewritten.** "No internet permission" becomes "nothing leaves the device except context, to your own SemPKM instance, and never notification content." | A single `service-bridge` module holds `INTERNET`; it is off until an instance URL and API key are entered. PROJECT.md constraint and README privacy section change in the bridge phase. |
| D3 | **Mirroring accepted.** For Batched, Quiet, Digest and Blocked lanes the listener cancels the original and re-posts a mirror under Doorman's channels; Instant and Live lanes are never touched. | The shade shows Doorman's icon with the original app's icon as large icon and its label as sub-text. Conversation shortcuts and bubbles are lost on mirrors. |

---

## Milestone sequence

Three milestones, each shippable and visible on the device at the end.

| Milestone | Phases | What the user sees when it ships |
|---|---|---|
| **v0.3 Doorman** | 4 to 7 | The app is called Doorman, looks like the review, has three tabs, and the Now stream shows icons, lane dots, time sections, undo, search and a working "open". The phone's behaviour is unchanged. |
| **v0.4 Lanes** | 8 to 11 | Lanes are editable, new senders are screened, non-Instant lanes are mirrored, batches deliver at set times, everything removed is restorable, and the archive is searchable with keyword pages. This is the product. |
| **v0.5 Modes and bridge** | 12 to 14 | Presets become modes with instant lanes and blocked apps, screen-time enforcement uses friction instead of overlays, and the SemPKM bridge sends context and receives persona switches. |

v0.3 is broken down to task level below. v0.4 is broken down to plan level with task bullets.
v0.5 is broken down to phase and plan names; its tasks depend on what v0.4 teaches us.

---

## Milestone v0.3: Doorman

### Definition of done

A fresh install and an upgrade from v0.2 both open an app named Doorman with the new theme and
three tabs. Every notification that arrives is stored with key, channel, importance, icons,
people, actions and intent; updates do not duplicate rows; removals carry a reason. The Now
tab is the daily screen: sections by time, rows with icon and lane dot, bundles, swipe to
archive with undo, snooze an active notification to a time, search, open the original. Apps
and Modes tabs exist with the re-homed v0.2 content. No notification is mirrored or held yet.

### Requirements (v0.3)

IDs are stable; the roadmapper maps them to phases below.

**Build and theme (BRND)**
- BRND-01: The app is labelled "Doorman" with an LED-dot launcher icon (adaptive, monochrome layer), `applicationId` unchanged.
- BRND-02: `UsageControlTheme` is replaced by `DoormanTheme` with static dark and light colour schemes; dynamic colour is off by default and available as a Settings toggle that recolours only the accent.
- BRND-03: Six lane colours (people, money, deliveries, work, live, noise) are theme tokens exposed through a `LocalLaneColors` composition local, with dark and light values.
- BRND-04: Manrope and DM Mono ship as bundled font resources; the typography scale uses Manrope for UI and DM Mono for times, counts and package names with tabular numerals.
- BRND-05: The project builds on a Compose BOM that carries `material3` 1.5.0-beta02, and the app is wrapped in `MaterialExpressiveTheme` with the expressive motion scheme.
- BRND-06: `core-ui` provides `LedDot`, `LaneChip`, `NotificationRow`, `BundleRow`, `SectionHeader`, `HeroCount`, `ContextStrip` and `PolicySegment` composables with `@Preview`s in both themes.

**Listener foundation (LSTN)**
- LSTN-01: Each stored notification keeps `key`, `id`, `tag`, channel id, importance, conversation flag, shortcut id, `when`, group key, group-summary flag, flags, category and update count. Room migrates from v3 to v4 without data loss.
- LSTN-02: Small icon, large icon and notification colour are captured at post time and stored as files under the app's data directory, referenced by path.
- LSTN-03: Actions (title, semantic action, whether it has a RemoteInput) are stored; `contentIntent`, `deleteIntent` and action PendingIntents are kept in an in-memory cache keyed by notification key while the listener is connected.
- LSTN-04: Removals are matched by key and record the removal reason (click, cancel, app cancel, timeout, snoozed, listener cancel, group summary cancelled, other).
- LSTN-05: A re-post of an existing key updates the row (text, time, update count) instead of inserting a new one.
- LSTN-06: Ongoing notifications are tracked in a separate live-state table (present or gone) and never counted in the event history or stats.
- LSTN-07: `onListenerConnected` reconciles the live-state table with `getActiveNotifications()`.
- LSTN-08: MessagingStyle notifications store their messages (sender, text, timestamp) and the conversation title.
- LSTN-09: Each stored notification gets a provisional lane from category, channel importance, conversation flag and, on Android 16 QPR1, reserved bundle channel ids.

**Shell (SHEL)**
- SHEL-01: The bottom bar has three tabs: Now, Apps, Modes. Settings opens from a gear in the Now top bar.
- SHEL-02: Rules list and editor remain reachable from the Apps profile and from Settings until v0.4 replaces them.
- SHEL-03: The feature tour describes the three tabs; the welcome screen shows the lane-stream illustration.
- SHEL-04: Maestro flows pass against the new shell.

**Now (NOW)**
- NOW-01: The stream is sectioned into Now (last hour), Earlier today, Yesterday, then dated sections.
- NOW-02: A row shows the lane LED, the app icon (or the sender avatar for conversations), the sender or title, one line of text and the time in mono.
- NOW-03: Notifications from the same app and lane within ten minutes collapse into a bundle row that expands in place.
- NOW-04: Swipe right archives a row with a ten-second undo; archiving an active notification also cancels it.
- NOW-05: Swipe left on an active notification snoozes it to a chosen clock time via `snoozeNotification`; swipe left is disabled on past rows.
- NOW-06: Search covers title, text, sender and app, and results render as the same rows.
- NOW-07: The hero number counts today's interruptions (notifications that audibly or visually alerted), with a held count that reads 0 until v0.4.
- NOW-08: A "Recently dismissed" row shows how many were removed in the last hour and opens a sheet listing them.
- NOW-09: Tapping a row sends the original `contentIntent` with background-activity-start options; when the intent is gone the row says so.
- NOW-10: The context strip shows the current context snapshot and the active preset, and opens Modes.

**Apps and Modes v1 (APPS, MODE)**
- APPS-01: The Apps tab lists apps by alerts per day with icon, name, provisional lane LED, a seven-day sparkline and a dismiss-rate hint when the heuristic fires.
- APPS-02: An app profile shows the dismiss/open ring, median time to action, hour-of-day bars, the app's channels with their provisional lanes, today's screen time with the limit control, and suggestions for that app.
- MODE-01: The Modes tab shows presets as mode cards with a toggle and trigger line, the full context strip, and the screen-time summary.

### Phases

Dependency note: Phase 4 (toolchain and theme) and Phase 5 (listener) are independent. With
`git.isolation: worktree` they can run in parallel; Phase 6 needs both.

#### Phase 4: Toolchain and theme
**Goal:** The project builds on a current Compose toolchain with Material 3 Expressive, the app is
named Doorman, and the theme and shared components from the design review exist with previews.
**Depends on:** nothing
**Requirements:** BRND-01 to BRND-06
**Success criteria:**
1. `./gradlew assembleDebug` succeeds on the bumped toolchain and the existing app runs unchanged.
2. The launcher shows "Doorman" with the LED-dot icon; upgrading from v0.2 keeps data and listener access.
3. A `@Preview` gallery renders every shared component in dark and light.
4. Dynamic colour is off by default; the Settings toggle recolours only the accent.

**Suggested CONTEXT.md decisions for `/gsd-discuss-phase 4`:**
- Keep `applicationId` and Kotlin packages; change label, icon, theme name and copy only.
- Pin exact versions in the toolchain plan after a spike; do not guess them in the plan.
- Near-black ground, not pure black; add an "Extra dark" toggle later only if asked.
- No new third-party UI libraries.

| Plan | Tasks | Notes |
|---|---|---|
| **04-01 Toolchain bump** (wave 1) | T1 Spike: on a branch, raise Kotlin, KSP, Hilt, AGP, Compose BOM and `material3` to the set that carries `material3` 1.5.0-beta02; record the exact versions and every compile error in the plan summary. T2 Apply the pinned versions in `gradle/libs.versions.toml`, fix API breaks (`HorizontalPager` opt-in, `LocalLifecycleOwner` import, deprecated `Divider`), confirm Room schema export still works. T3 Run `./gradlew assembleDebug check` and the Maestro onboarding flows; commit. | Highest-risk plan of the milestone. Set `depth: standard` for this phase. |
| **04-02 Brand and theme** (wave 2) | T1 Rename: `strings.xml` app name, adaptive launcher icon with monochrome layer, `Theme.Doorman` in `themes.xml`, welcome and settings copy. T2 `DoormanTheme`: static `darkColorScheme` and `lightColorScheme` with surface containers, `LaneColors` data class and `LocalLaneColors`, dynamic-colour opt-in read from DataStore, `MaterialExpressiveTheme(motionScheme = MotionScheme.expressive())`. T3 Typography: bundle Manrope and DM Mono variable fonts in `core-ui/res/font`, define the scale, add `tabular-nums` feature setting for the mono styles. | Add the dynamic-colour toggle to Settings as part of T2. |
| **04-03 Shared components** (wave 2) | T1 `LedDot`, `LaneChip`, `SectionHeader`, `PolicySegment` with previews. T2 `NotificationRow` and `BundleRow` taking a UI model (lane, icon, title, text, time, state) with swipe slots. T3 `HeroCount` and `ContextStrip`; a `ComponentGallery` preview file covering all components in both themes. | Pure `core-ui`, no DAO access. |

#### Phase 5: Listener foundation
**Goal:** The listener captures everything the Now screen and the v0.4 mirroring need, matches
removals by key, de-duplicates updates and treats ongoing notifications as live state.
**Depends on:** nothing (parallel to Phase 4)
**Requirements:** LSTN-01 to LSTN-09
**Success criteria:**
1. Posting, updating and removing a test notification produces one row with the right reason and update count.
2. Media and foreground-service notifications appear in the live table and not in history or stats.
3. Icons for a captured notification load from disk after the listener process restarts.
4. After the listener reconnects, the live table matches `getActiveNotifications()`.

**Suggested CONTEXT.md decisions:**
- Store icons as PNG files in `filesDir/icons/<package>/<hash>.png`, de-duplicated by hash; never in Room blobs.
- PendingIntents live only in a process-scoped cache; the archive stores the parcelled `Notification` for v0.4 mirroring (encrypted at rest is out of scope).
- Provisional lane classification is a pure function in `core-rules` with unit tests.
- Keep the old `userAction` column for one milestone so v0.2 stats keep working; add `removalReason` beside it.

| Plan | Tasks | Notes |
|---|---|---|
| **05-01 Schema v4** (wave 1) | T1 Extend `NotificationEntity` (key, id, tag, channelId, importance, isConversation, shortcutId, whenMs, groupKey, isGroupSummary, flags, updateCount, removalReason, smallIconPath, largeIconPath, color, provisionalLane, messagesJson, actionsJson, notificationParcelPath). T2 Add `LiveNotificationEntity` and `LiveNotificationDao`; add indices on `key`. T3 Write `MIGRATION_3_4`, update `Mappers.kt` and `NotificationRecord`, add a migration test. | |
| **05-02 Capture service rewrite** (wave 2) | T1 `onNotificationPosted`: branch on `isOngoing` to live table; upsert by key with update count; extract extras, MessagingStyle, people, actions; call the classifier. T2 `onNotificationRemoved`: match by key, map reason codes, update live table; `onListenerConnected`: reconcile with `getActiveNotifications()`. T3 Keep the existing DISMISS rule path working on the new entity; unit-test the reason mapping and the upsert. | Wave 2 because it needs 05-01. |
| **05-03 Icon, intent and lane stores** (wave 2) | T1 `IconStore` (save small/large icon via `Icon.loadDrawable`, hash, path). T2 `PendingIntentCache` (key → content, delete, action intents; cleared on removal) and `NotificationParcelStore` (write the `Notification` parcel for later `recoverBuilder`). T3 `ProvisionalLaneClassifier` in `core-rules` with tests for conversation, promo category, transport/progress, low importance and reserved bundle channel ids. | |

#### Phase 6: Shell and Now
**Goal:** Three tabs, Settings behind a gear, and a Now screen that is the daily surface: sections,
rows, bundles, archive with undo, snooze to a time, search, open the original.
**Depends on:** Phase 4, Phase 5
**Requirements:** SHEL-01 to SHEL-04, NOW-01 to NOW-10
**Success criteria:**
1. Bottom bar shows Now / Apps / Modes; Settings opens from the gear; back from any tab exits the app.
2. The Now stream shows real captured notifications with icons and lane dots in time sections, and bundles collapse and expand.
3. Swipe right archives with undo; swipe left on an active notification opens the time picker and the notification disappears until that time.
4. Tapping a row opens the originating app; a stale row shows "no longer available".
5. Maestro flows for onboarding and bottom navigation pass.

**Suggested CONTEXT.md decisions:**
- `feature-notifications` becomes `feature-now`; Apps and Modes get their own feature modules; `feature-learning` content moves into `feature-apps` in Phase 7.
- Archive is a state on the record (`archivedAt`), not a delete; undo clears it.
- Section boundaries are computed in the ViewModel from local time, not in SQL.
- The time picker for snooze offers four presets (1 h, lunch, evening, tomorrow 8 am) and a custom time.

| Plan | Tasks | Notes |
|---|---|---|
| **06-01 Three-tab shell** (wave 1) | T1 Replace `AppScreen` with Now / Apps / Modes, add `settings` route behind the gear, keep `rules/*` and `rule-editor/*` routes. T2 Update tour slides and welcome screen copy and illustration; update `testTag`s. T3 Update Maestro flows `07-bottom-nav` and helpers; run them. | Apps and Modes can be placeholder screens in this plan. |
| **06-02 Now stream** (wave 2) | T1 `NowViewModel`: paged flow of records joined with live state, section computation, bundle grouping, search query flow, hero count query (alerted = importance at or above default and not from a silent channel). T2 `NowScreen`: context strip, hero, section headers, `NotificationRow` and `BundleRow`, search field, empty and degraded states. T3 `ContextStrip` wired to `ContextSnapshotDao` and the active preset; tap navigates to Modes. | |
| **06-03 Now actions** (wave 3) | T1 Swipe right: archive with undo snackbar; cancel the live notification when present. T2 Swipe left: snooze-to-time sheet calling `snoozeNotification`; persist the chosen time for display. T3 Row tap: send `contentIntent` from the Activity with `ActivityOptions` background-start mode; "Recently dismissed" row and sheet. | |

#### Phase 7: Apps and Modes v1
**Goal:** Every piece of v0.2 content has a home in the new shell, the old tabs are deleted, and the
milestone audit passes.
**Depends on:** Phase 6
**Requirements:** APPS-01, APPS-02, MODE-01
**Success criteria:**
1. Apps lists every app that has notified, sorted by alerts per day, with sparklines drawn from real data.
2. An app profile shows stats, channels, screen-time limit and suggestions, and the limit still enforces.
3. Modes shows presets as mode cards and toggling one still applies the sound profile.
4. `feature-learning` and the old Notifications dashboard are gone; `/gsd-audit-milestone` passes.

| Plan | Tasks | Notes |
|---|---|---|
| **07-01 Apps list** (wave 1) | T1 `AppsViewModel`: per-app aggregates (alerts per day, dismiss rate, seven-day counts) via new DAO queries. T2 `AppsScreen` list with icon, lane LED, sparkline (reuse `SimpleLineChart` sized small), sort toggle (alerts, screen time). | |
| **07-02 App profile** (wave 2) | T1 Profile header and "How you treat it" (ring, median, hourly bars) from `getDismissStats` and `getDismissalsByHour`. T2 Channels section (distinct channel ids for the package with provisional lane), screen-time section (today, limit dialog moved from ScreenTime), suggestions filtered to the package, link to rules. T3 Delete `feature-learning` and the Notifications dashboard composables; move the `SuggestionEngine` call into Apps. | |
| **07-03 Modes v1 and cleanup** (wave 2) | T1 `ModesScreen`: preset cards with toggle and trigger line, full context strip, screen-time summary; custom and WiFi preset dialogs moved over. T2 Remove `feature-screentime` tab screen (keep `BlockOverlayActivity` and the service); README and PROJECT.md updated for the three tabs and the name. | |

### Research to request in `/gsd-plan-phase`
- Phase 4: exact compatible versions for Kotlin, KSP, Hilt, AGP, Compose BOM, `material3` 1.5.0-beta02, Navigation Compose; whether `material-icons-extended` still resolves.
- Phase 5: `Icon.loadDrawable` behaviour for icons from uninstalled packages; parcel size of a `Notification` with a large picture; Room 2.6 migration testing setup.
- Phase 6: swipe-to-dismiss API in the bumped Compose (`SwipeToDismissBox`), `ActivityOptions` background-start modes by target SDK.

---

## Milestone v0.4: Lanes

### Definition of done
Lanes are user-defined. A new sender is screened once and routed forever. Non-Instant lanes are
mirrored under Doorman's channels with lane colour and grouping. Batched lanes deliver at set
times or at a breakpoint, with cooldown and breakthroughs. Everything removed is restorable from
the archive, which has search, keyword pages and a daily digest.

### Requirements (v0.4), by category
- **LANE**: lane entity (name, colour, policy, decay, sound, breakthrough keywords and people); six defaults seeded on upgrade; lane editor; per-app and per-channel and per-person routes.
- **SCRN**: Screener notification and sheet for an unscreened sender; one tap routes and re-files history; re-screening prompts replace the suggestion cards; category defaults until screened.
- **MIRR**: `MirrorPoster` using `recoverBuilder`; per-lane channels and groups; Quiet and Blocked policies; decay rules (after N minutes, when the app opens, at next batch); restore posts a working mirror; reply through RemoteInput where mutable.
- **DLVR**: batch times per lane; `BatchScheduler` on `AlarmManager`; bounded deferral (screen off, app switch, or max delay); per-sender cooldown; breakthrough bypass; batch summary notification with Open, Deliver all, Keep holding; snooze-to-time for mirrors.
- **ARCH**: archive screen with search, keyword pages (saved searches), restore batch, per-day digest text, retention settings covering the archive and the parcel store.

### Phases and plans
| Phase | Goal | Plans |
|---|---|---|
| **8 Lanes and Screener** | Lane model, routes, editor, Screener. | 08-01 Lane and route schema v5 with seeded defaults and migration. 08-02 Lane editor and lane picker in the app profile (LaneChip, PolicySegment). 08-03 Screener: unscreened-sender detection in the listener, quiet notification with lane actions, in-app Screener sheet, re-file history on route; re-screening prompts generated from the existing heuristics. |
| **9 Mirroring and policies** | Doorman owns the shade for non-Instant lanes. | 09-01 `MirrorPoster` and lane channels (colour, group, importance per policy), cancel-then-post, large icon and sub-text, mapping table mirror key → original key. 09-02 Policies Quiet and Blocked, decay rules and the `DecayWorker`, restore from archive as mirror, "no longer available" handling. 09-03 Reply and actions on mirrors (RemoteInput forwarding, semantic actions), device test matrix for WhatsApp, Signal, Telegram, Slack, Gmail. |
| **10 Batching and delivery** | Held lanes deliver at the right moment. | 10-01 `BatchScheduler` (per-lane times, AlarmManager exact alarms with the permission flow, mode hooks stubbed), held queue, "Deliver now". 10-02 Bounded deferral and cooldown (screen state receiver, UsageEvents foreground change, max delay), breakthrough evaluation in the listener. 10-03 Batch summary notification (lane counts, top lines, three actions), "Next batch" card on Now, snooze-to-time for mirrors, hero held count. |
| **11 Archive and memory** | Nothing is lost. | 11-01 Archive screen with search, filters by lane and app, restore single and restore batch. 11-02 Keyword pages (saved searches as chips, promote to breakthrough). 11-03 Daily digest text generator and the digest row on Now; retention and cleanup extended to icons and parcels; export. |

Open device tests carried from the review: snoozing foreground-service notifications; reserved
bundle channel ids on Pixel; reply mutability per messaging app.

---

## Milestone v0.5: Modes and bridge

| Phase | Goal | Plans |
|---|---|---|
| **12 Modes** | Presets become modes that change lanes. | 12-01 `Mode` model (preset fields plus `instantLanes`, `demotedLanes`, `blockedApps`, `deliverHeldAtExit`), migration from `SoundPreset`. 12-02 Android 15 `AutomaticZenRule` with `ZenDeviceEffects` for each mode, migration of the `SET_DND` action, mode state in the context strip. 12-03 Delivery hooks: lane overrides while a mode is active, held batches released at exit, Modes screen v2. |
| **13 Screen-time friction** | Blocking becomes a pause, not a wall. | 13-01 Pause-before-open flow replacing `SOFT_BLOCK` (breathing timer, "why?" note), session lengths. 13-02 Mode-based blocking (apps blocked while a mode is on) and the screen-time summary on Modes; retire enforcement levels. |
| **14 SemPKM bridge** | The phone is a context provider again, and SemPKM talks back. | 14-01 `service-bridge` module with `INTERNET`, instance URL and API key in encrypted preferences, connection test against `/.well-known/sempkm`, privacy rewrite in PROJECT.md and README. 14-02 Context push: zone from WiFi-SSID map and geofences, activity recognition, time period, calendar, `focus_mode`, `notification_pressure`; push on change plus heartbeat. 14-03 Inbound: device registration on `/api/context/notifications/register`, SemPKM lane receiver, persona-to-mode mapping. **SemPKM side (separate SemPKM phase):** add `focus_mode` and `notification_pressure` to `UserContext`, the update endpoint and the context indicator chip. |

---

## How to run this

1. `/gsd-new-milestone "v0.3 Doorman"` and paste the v0.3 definition of done, requirements and phase list from this file when it asks what is next. Keep phase numbering continuing from 3.
2. `/gsd-discuss-phase 4` with the suggested decisions above; repeat for 5 before planning.
3. Set `depth` to `standard` for Phases 4 and 9 in `config.json`; `quick` is fine for the rest.
4. `/gsd-plan-phase 4` and `/gsd-plan-phase 5`, then execute them in parallel worktrees if `git.isolation` is on, otherwise 4 first.
5. After Phase 7, `/gsd-audit-milestone v0.3`, then `/gsd-complete-milestone`, then `/gsd-new-milestone "v0.4 Lanes"` from the v0.4 section.

## Risks worth a line in PROJECT.md
- The toolchain bump (Phase 4) is the one step that can stall everything; do it first and alone.
- Mirroring changes what the system's per-app notification settings point at; the Screener copy must explain it once.
- The bridge contradicts the current README privacy section; both must change in the same commit.
