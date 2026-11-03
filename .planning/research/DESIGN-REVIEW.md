# Design Review: Theme, UI and the Notification Rethink

**Date:** 2026-11-03
**Scope:** Product and UI review of AndroidUsageControl v0.2 against its three stated goals
(context provider for SemPKM, screen-time control, a genuinely different way to handle
Android notifications). Part 1 is the audit, Part 2 the theme and UI proposal, Part 3 the
notification model, Part 4 feasibility, Part 5 research sources, Part 6 a roadmap, Part 7 open decisions.

---

## 0. TL;DR

1. **The app has no identity.** Dynamic colour is on by default, the fallback palette is
   stock Material green, and every tab is the same `TopAppBar + LazyColumn of Cards`. An app
   whose whole point is colour-coding notifications cannot let the wallpaper pick its colours.
   Proposal: dark-first, static palette, a single visual motif (the notification LED), one
   type family with a mono companion for times and counts, and Material 3 Expressive components.
2. **Five tabs is three too many.** Notifications, Screen Time, Controls, Insights and Settings
   share one mental model the UI never shows: *everything is per app, and everything is a
   policy*. Proposal: three tabs (`Now`, `Apps`, `Modes`), Settings behind a gear, Rules
   folded into the per-app profile.
3. **The notification feature is a log, not a product.** It captures and auto-dismisses, and
   that is all the listener executes (mute and snooze exist as enums only). There is no
   grouping, colouring, undo, history search, people, batching, or decay.
4. **The new model is Lanes.** A lane is a user-defined bucket (People, Money, Deliveries,
   Work, Noise) with a colour, a delivery policy (Instant, Batched at times, Quiet, Digest,
   Blocked) and a decay rule. Notifications are routed into lanes by sender-level screening
   (decide once, route forever), with keyword exceptions that punch through. Everything a user
   swipes away is recoverable for a window, and the history becomes a searchable archive. This
   combines four things no Android app currently combines: content bucketing with silent
   delivery (Pixel's Notification Organizer), keyword break-through (BuzzKill), sender screening
   (HEY) and bounded deferral to interaction breakpoints (Pielot 2018).
5. **The SemPKM goal is not built, and the current privacy rule forbids it.** The manifest has
   no INTERNET permission; SemPKM already has an Expo app doing the context-provider job
   (Chapter 46). Proposal: an opt-in `service-bridge` module speaking SemPKM's existing
   `/api/context/update` contract, adding two fields SemPKM does not have yet
   (`focus_mode`, `notification_pressure`), and receiving SemPKM's own notifications into a
   lane. This needs a deliberate change to the "no internet, non-negotiable" rule in PROJECT.md.

---

## 1. Audit: what exists today

### 1.1 Goals versus code

| Goal | State | Evidence |
|---|---|---|
| Context provider for SemPKM | Not started | No INTERNET permission in `app/src/main/AndroidManifest.xml`; `ContextSnapshot` has WiFi SSID, charging, Bluetooth, calendar but not zone, activity or time period, which are the fields SemPKM's `UserContext` stores. SemPKM docs cite this repo as the "open-source example" context provider, which is currently untrue. |
| Screen-time control ("Stay Focused") | Basic | Per-app daily limits, three enforcement levels, overlay activity. No schedules, no session lengths, no friction mechanics, no mode link. |
| Notifications | Capture + auto-dismiss | `NotificationCaptureService` stores title/text/subText/category/priority/groupKey and runs DISMISS rules. `MUTE_NOTIFICATION` and `SNOOZE_NOTIFICATION` are defined in `ActionType` but never executed. |

### 1.2 UI audit, screen by screen

**Theme (`core-ui/theme/Theme.kt`).** `dynamicColor = true` on Android 12+, so on most
phones the app wears the wallpaper's colour. The fallback is a stock M3 green seed. Typography
is the Material default. No shape overrides, no custom components beyond `StatCard`,
`UsageBar`, three Canvas charts and a red `DegradedPermissionBanner`. Material 3 is pinned at
1.2.0 (Compose BOM 2024.02), which predates M3 Expressive.

**Navigation.** Five bottom tabs. `Rules` is a sixth, hidden screen reached from three tabs
with a `pillar` argument; the user has no stable place to find "my rules". `Insights` and the
dashboard inside `Notifications` show the same Total/Dismissed/Opened triplet and the same
daily trend line twice.

**Notifications tab.** The dashboard (chips, three stat cards, line chart, donut) sits
*above* the feed, so the actual notifications start below the fold. Each item is a uniform
card: app name in primary colour, time, title, text, a tiny grey "Pending/Dismissed/Opened"
label. There is no app icon, no colour, no grouping by time or app, no search (the DAO has a
`search()` query the UI never calls), no swipe actions. Expanding a card reveals exactly one
action: "Auto-dismiss this app". Filter chips list every app that ever notified, in
alphabetical order, in one horizontal row.

**Screen Time.** Reasonable. Stat cards, trend chart, per-app bars, limit dialog. It is the
screen that most looks like a product.

**Controls.** "Current Context" card, two quick-setup buttons, WiFi preset button, preset
list. The name "Controls" says nothing; these are *modes*.

**Insights.** Suggestions as secondary-container cards with a confidence bar and "Create
Rule" buttons, then stats. Suggestions are phrased as rules ("Auto-dismiss X") which asks the
user to think in the engine's terms.

**Settings.** Fine, but it does not deserve a tab.

**Onboarding.** The permission walkthrough is good work (priming screen, deep links, live
re-check, step counter, degraded mode). The welcome screen is a headline and a paragraph;
there is nothing to look at.

### 1.3 Listener defects that block any notification product

These are in `NotificationCaptureService` and `NotificationEntity`, and must be fixed before
any of Part 3 is possible.

1. **Dismissals are matched by `postTime ± 1 s`**, not by `sbn.key`. Store `key` (and
   `id`, `tag`) on the entity and match on it. Also use `onNotificationRemoved(sbn, rankingMap,
   reason)` reasons beyond CLICK/CANCEL: `REASON_SNOOZED`, `REASON_TIMEOUT`,
   `REASON_APP_CANCEL`, `REASON_GROUP_SUMMARY_CANCELED`, `REASON_LISTENER_CANCEL`.
2. **Nothing visual is captured.** No small icon, large icon, colour, picture, or
   `MessagingStyle` messages, so the feed cannot show app icons or avatars.
3. **No channel or ranking data.** `Ranking.getChannel()`, `getImportance()`,
   `isConversation()`, `getConversationShortcutInfo()`, `isAmbient()` are all available and
   unused. Channel id is the single best grouping key Android gives you.
4. **Actions and `RemoteInput`s are not stored**, so a mirrored or restored notification
   cannot offer Reply, Mark as read, Archive.
5. **`contentIntent` is not kept**, so a history item cannot open what the original would
   have opened.
6. **Ongoing notifications are logged as events.** Media players, foreground services and
   progress notifications pollute the history and the stats. Treat `isOngoing` as a *live
   state* table, not an event.
7. **No `getActiveNotifications()` sync** on `onListenerConnected()`, so the app does not know
   what is currently in the shade after a restart.
8. **Updates are counted as new notifications.** The same `key` re-posted (chat apps do this
   constantly) inserts a new row each time, inflating counts and dismiss stats.

---

## 2. Theme and UI proposal

### 2.1 Positioning

The app is the doorman of your phone: it stands between the world and you, lets the right
things through immediately, holds the rest until you ask, and remembers everyone who came by.
"Usage Control" reads as parental control software. Working names in that register:
**Doorman**, **Sieve**, **Quiet**, **Tally**. The review uses "the app" below; the name is a
separate decision.

### 2.2 Visual direction: the LED is back

The original Android notification UI was a coloured LED that blinked in the dark. It was
glanceable, colour-coded, and silent. That is the motif: **one glowing dot per lane** on a
near-black ground. It gives the app a reason to be dark-first (LEDs look best on black), a
reason for saturated lane colours, an icon (a single dot), and a consistent indicator that
works in the shade, the status bar chip, the widget and the in-app feed.

**Decisions**

- **Dynamic colour off by default.** Lane colours are semantic; the wallpaper must not
  collide with them. Offer "Match wallpaper" as an opt-in that only recolours the accent.
- **Dark-first, light-complete.** OLED near-black ground with a slight cool ink bias, four
  stepped surface containers, one accent. The light theme is a real design, not an inversion:
  warm paper ground, the same lane hues at lower chroma.
- **One accent, not a brand green.** The accent is amber (the "lamp is on" colour); it is used
  for primary actions, the active mode and the batch countdown. Semantic good/warn/critical
  stay separate.
- **Lane hues (six, LED-saturated on dark, de-saturated on light):** coral (People), amber
  (Money), cyan (Deliveries), violet (Work), lime (Live/Ongoing), slate (Noise). Users may
  pick any; these are the defaults so screenshots and onboarding are consistent.
- **Type.** One variable grotesque for UI with a mono companion for times, counts and package
  names (tabular numerals everywhere digits align). Candidates that ship as a single TTF and
  read well at 12 sp on OLED: Manrope + DM Mono, or Figtree + JetBrains Mono. Avoid Inter and
  Roboto defaults; they are what every other utility app uses.
- **Shape.** Rows and sheets at 12 dp, chips and the LED pill fully rounded, one large
  expressive shape per screen (the hero count on `Now`, the mode toggle on `Modes`). Do not
  put every block in a card: cards are for the *current* batch and for suggestions, rows are
  for everything else.
- **Material 3 Expressive.** Upgrade `material3` to **1.5.0-beta02** (Expressive is *not* in
  1.4.0 stable; 1.4.0-beta01 removed every `ExperimentalMaterial3ExpressiveApi` and pointed to
  the 1.5 line). Wrap the app in `MaterialExpressiveTheme(motionScheme = MotionScheme.expressive())`,
  use `MaterialTheme.motionScheme.defaultSpatialSpec()` for row and card transitions,
  `ButtonGroup` for the delivery-policy picker, `SplitButton` on the batch card ("Deliver now"
  / "Snooze to..."), `FloatingToolbar` for multi-select in the archive, `LargeFlexibleTopAppBar`
  on profiles, and `LoadingIndicator` (still experimental, opt in) while the listener syncs.
  These components are what make a 2026 Android app feel current without inventing a widget set.
- **Surfaces, not elevation.** Build hierarchy with `surfaceContainerLowest` to `Highest`;
  `tonalElevation` is a no-op with a hand-built scheme. Material advises against pure #000
  behind content, so the ground is near-black (a cool ink), with an optional "Extra dark"
  true-black setting for OLED purists.

### 2.3 Information architecture: three tabs

| Tab | Job | Replaces |
|---|---|---|
| **Now** | What has arrived, by lane and time; the next batch; recently dismissed; search over the archive. | Notifications tab, dashboard, history |
| **Apps** | One profile per app (and per person for conversations): lane, policy, stats sparkline, screen-time limit, suggestions for *this* app. | Rules, Insights, screen-time limits |
| **Modes** | Night, Meeting, Deep Work, Home: each mode bundles sound, DND, lock timeout, which lanes are instant, and which apps are blocked. Shows the live context strip (zone, activity, calendar, SemPKM persona). | Controls, Screen Time blocking, context card |

Settings and permissions move behind a gear in the `Now` top bar. Screen-time *reporting*
stays reachable from `Apps` (sort by time) and from a `Modes` summary ("2 h 10 m today, 40 m
in blocked apps").

### 2.4 The `Now` screen

Top to bottom:

1. **Context strip.** A single line: `Office · Walking · Standup in 12 m · Deep Work`. Tapping
   opens Modes. When the SemPKM bridge is on, this is the same data the server sees, with a
   staleness dot.
2. **Hero count.** One large number: *interruptions today* (notifications that actually
   alerted), with the day's batch-held count underneath in muted text. This is the attention
   budget made visible. Tap to flip to the by-lane breakdown.
3. **Next batch card** (only when something is held). "14 held · Delivers at 12:30" with a
   split button: Deliver now / Snooze to.... Per-lane LED dots with counts across the top of
   the card.
4. **The stream.** Grouped by time section (Now, Earlier today, Yesterday, then dates), each
   row: LED dot in lane colour, app icon, sender or title, one line of text, time in mono.
   Conversation rows show the avatar instead of the app icon and the person's name first.
   Rows from the same app and lane within 10 minutes collapse into a bundle row ("DHL · 3
   updates") that expands in place.
5. **Swipe left:** Snooze (to a time, not a duration). **Swipe right:** Archive, with a
   10-second undo chip. **Long press:** the Screener sheet for this sender (see 3.2).
6. **Recently dismissed** is a pinned row at the top of "Earlier today" when non-empty:
   "You dismissed 6 in the last hour · Restore" so an accidental swipe-all is never fatal.
7. **Search** in the top bar across title, text, sender, app and lane; saved searches become
   keyword pages ("Order", "Invoice") pinned as chips under the search field.

### 2.5 The `Apps` screen

A list sorted by notification pressure (alerts per day), each row: icon, name, lane LED,
policy word (Instant / 12:30 & 18:00 / Quiet / Blocked), a 7-day sparkline of alerts and a
muted "92% dismissed in < 3 s" when the heuristic fires. Tapping opens the profile:

- Header: icon, name, lane picker (LED chips), policy segmented button group.
- "How you treat it": the dismiss/open ratio ring, median time-to-action, hour-of-day bars.
- "Channels": the app's notification channels, each with its own lane override.
- "People" for conversation apps: per-person lane override (VIP = Instant).
- "Screen time": today's bar, the limit, the enforcement level, which modes block it.
- "Suggestions for this app": the existing heuristics rewritten as actions ("Move to Noise",
  "Batch at 12:30 and 18:00", "Expire after 2 h") with a one-tap accept and a "Why?" line.

### 2.6 The `Modes` screen

A vertical list of mode cards with a large toggle, the trigger line ("22:00 to 07:00",
"Office WiFi", "Calendar: meeting", "Manual", "SemPKM persona: Work"), and a summary of what
the mode does. Below: the live context strip in full, and the screen-time summary. Modes are
the existing `SoundPreset` plus three new fields: `instantLanes`, `blockedApps`,
`heldLanesDeliverAtExit`.

### 2.7 Onboarding changes

Keep the permission walkthrough as is. Replace the welcome screen's paragraph with a live
mock of the `Now` stream animating three notifications into three lanes, and one sentence:
"Decide once where each sender goes. We hold the rest until you want it." Replace the Night
Mode quick setup with lane defaults: "Which apps are People?" (preselected from installed
messaging apps), then "When do you want everything else?" (default 12:30 and 18:00, per the
Fitz study).

---

## 3. The notification model: Lanes, Screening, Delivery, Memory

### 3.1 Lanes

A lane is `{ name, colour, policy, decay, sound, breakthrough }`.

| Field | Values | Notes |
|---|---|---|
| policy | **Instant** (untouched, pass through), **Batched** (held, delivered at times), **Quiet** (delivered immediately, silently, into the lane's group), **Digest** (never delivered individually; summarised in the next batch), **Blocked** (cancelled, archived, never shown) | Instant lanes are never mirrored; the original notification is left alone. |
| decay | none, expire after N minutes, expire when the app is opened, expire at the next batch | Replaces "auto-dismiss" with something that reads as intent: promotions are not deleted, they expire. |
| breakthrough | keywords, people, or channels that bypass Batched/Quiet/Digest and alert instantly | The BuzzKill "exception" idea. "urgent", "verification code", "delivered", your partner. |
| sound | per lane | Batched and Quiet lanes have none by definition. |

Default lanes on first run: People (Instant), Money (Instant, breakthrough on "code"),
Deliveries (Quiet, expire when opened), Work (Batched 12:30 and 18:00, Instant in Work mode),
Live (ongoing notifications, shown in a separate strip), Noise (Digest, expire at next batch).

### 3.2 Screening: decide once, route forever

The first time a new *sender* appears (app + channel, or app + person for conversations),
nothing is auto-dismissed. The app posts one quiet notification, "New: Zalando (Promotions
channel). Where should this go?", and the `Now` stream shows the Screener row with six LED
chips. One tap routes this sender forever and re-routes everything already in the archive
from it. Until screened, a sender defaults to the lane suggested by its Android category and
channel importance (conversations to People, `CATEGORY_PROMO` to Noise, `CATEGORY_TRANSPORT`
and `CATEGORY_PROGRESS` to Live, `IMPORTANCE_LOW` to Quiet). This is HEY's Screener applied to
notifications, and it replaces most of the current suggestion engine: the suggestions become
*re-screening* prompts ("You dismissed 40 of 42 from Zalando in under 3 s. Move to Noise?").

### 3.3 Delivery

- **Batch times** are per lane, default two per day. Research supports two or three
  predictable moments and argues against hourly batching and against total silence (Fitz et
  al. 2019; Pielot and Rello 2017).
- **Bounded deferral.** When the phone is unlocked and in use, a Batched delivery waits for a
  breakpoint (screen off, app switch, or at most N minutes) rather than landing mid-task
  (Pielot et al. 2018).
- **Cooldown** per sender: at most one alert per N minutes, the rest fold into the bundle row.
- **Mode override.** Modes can promote lanes to Instant (Work mode makes Work instant) or
  demote everything but People (Night mode). Batches held during a mode deliver when it ends.
- **Snooze to a time**, not a duration, using `snoozeNotification` for originals and a
  scheduled re-post for mirrors.
- **The batch notification** is one summary per delivery: lane LED dots with counts, the
  three most important lines, and actions "Open", "Deliver all", "Keep holding". Expanding it
  posts the held mirrors as a group.

### 3.4 Memory: undo, archive, digest

- Every removal, by the user, a rule, a decay or a mode, is a row in the archive with the
  reason. The archive keeps icons, intents and actions, so restoring a notification re-posts a
  working mirror (open, reply, mark read).
- **Undo** everywhere: the 10-second chip after a swipe, the "Recently dismissed" row, and a
  "Restore batch" action on auto-expired bundles.
- **Digest** per batch and per day, generated from templates on-device with no model:
  "3 deliveries (DHL, Amazon), 12 promotions expired, 2 messages from Anna held in Work."
  An optional on-device summariser (ML Kit GenAI Summarization on supported devices) can
  shorten long threads later; it is not required and must be labelled as a summary.
- **Keyword pages**: saved searches over the archive ("Invoice", "Order", "OTP") that act as
  folders, and can be promoted to breakthrough rules in one tap.

### 3.5 Context and SemPKM

The bridge is bidirectional and opt-in.

**Phone to SemPKM.** `POST /api/context/update` with `location_zone` (from a WiFi-SSID-to-zone
map or geofences), `activity` (Activity Recognition), `time_period`, `calendar_event`,
`calendar_busy`, `device_id`, plus two new fields to add on the SemPKM side: `focus_mode`
(active mode name) and `notification_pressure` (alerts in the last hour, bucketed low /
medium / high). SemPKM personas and rules can then react to "in Deep Work on the phone".

**SemPKM to phone.** The app registers as a device on `POST /api/context/notifications/register`
(the existing Expo contract) and receives persona switches and rule-triggered notifications
into a `SemPKM` lane. A persona switch can activate a mode ("Work" persona activates Work
mode), which is the context-provider goal running in reverse.

**Privacy.** The bridge is a separate Gradle module (`service-bridge`) that is the only thing
holding INTERNET; it talks only to the configured instance; it sends context, never
notification content; and it is off until the user enters an instance URL and API key.
PROJECT.md's constraint should be rewritten from "no internet permission" to "no data leaves
the device except to your own SemPKM instance, and only context, never content".

---

## 4. Feasibility: what a listener may do

Verified against the Android reference docs and AOSP `NotificationManagerService` on
2026-11-03. The whole model stands on one commitment: for non-Instant lanes the listener
cancels (or snoozes) the original and re-posts a mirror on the app's own channels.

| Need | Verdict | How, and the catch |
|---|---|---|
| Read everything | Possible | `Ranking.getChannel()`, `getImportance()`, `isConversation()`, `getConversationShortcutInfo()`, `getSmartReplies()`; extras `EXTRA_MESSAGES`, `EXTRA_PEOPLE_LIST`, `EXTRA_PICTURE`, `EXTRA_TEXT_LINES`; `n.getSmallIcon()` / `getLargeIcon()` load cross-package via `Icon.loadDrawable`; `n.actions[]` with `RemoteInput`s; `contentIntent`. |
| Cancel, snooze, sync | Possible | `cancelNotification(key)`, `snoozeNotification(key, ms)` (API 26, system re-posts it; no public un-snooze), `getActiveNotifications()` on connect, `setNotificationsShown()`. A listener cancel is silently ignored for ongoing and foreground-service notifications, which is why the Live lane is always pass-through. |
| Removal reasons | Possible | `onNotificationRemoved(sbn, ranking, reason)`: CLICK, CANCEL, APP_CANCEL, TIMEOUT, SNOOZED, GROUP_SUMMARY_CANCELED, LISTENER_CANCEL, and `REASON_BUNDLE_DISMISSED` on 36.1. The sbn passed here is "light" (no large icon), so icons must be captured at post time. |
| Mirror a notification | Possible | `Notification.Builder.recoverBuilder(ctx, sbn.notification)` (API 24) keeps `contentIntent`, actions with `RemoteInput`s, MessagingStyle content and large icon; set our channel, `setColor(laneColour)`, `setGroup(lane)`, `setLargeIcon(originalAppIcon)`, `setSubText(appLabel)`; convert the small icon to a bitmap. Cancel the original first. Needs `POST_NOTIFICATIONS`. |
| Open from a mirror or from the archive | Possible with care | A tap on the mirror is sent by SystemUI, which is exempt from background-launch rules. Sending `contentIntent` from our own Activity works but, targeting 34+, must pass `ActivityOptions.setPendingIntentBackgroundActivityStartMode(MODE_BACKGROUND_ACTIVITY_START_ALLOWED)`. Sending from a service is blocked. If the original is `FLAG_AUTO_CANCEL`, cancel it ourselves. One-shot or cancelled PendingIntents die; show "no longer available" in that case. |
| Reply from a mirror | Possible if the app allows | `RemoteInput.addResultsToIntent` then `actionIntent.send(ctx, 0, fillIn)`. Works only when the app created the reply intent `FLAG_MUTABLE` (Google's guidance says direct-reply intents should be); immutable ones deliver an empty reply. Test per messaging app and fall back to opening the conversation. |
| Undo a dismissal | Workaround | Nothing restores the original. Snooze instead of cancel when the user is likely to want it back; otherwise restore a mirror from the stored parcel. |
| Colour, group, order in the shade | Workaround | Only on our mirrors. The header shows our app name; mitigate with the large icon and sub-text. Per-app system notification settings then point at our app; conversation features (bubbles, shortcuts) are lost on mirrors unless we publish shortcuts ourselves. |
| Make an alert silent after the fact | Workaround | Not on the original. Cancel or snooze and re-post on an `IMPORTANCE_LOW` channel. `updateNotificationChannel` on other apps needs a CompanionDeviceManager association, which is not practical here. |
| Modes with device effects | Possible | Android 15 `AutomaticZenRule.Builder` with `setType`, `setDeviceEffects(ZenDeviceEffects)` (grayscale, dim wallpaper, suppress always-on display), `setTriggerDescription`, `setIconResId`; `addAutomaticZenRule` plus `setAutomaticZenRuleState`. Needs DND access, which the app already requests. Modes appear in system Settings with our icon. Apps targeting 35+ get an implicit rule from `requestInterruptionFilter`, so migrate the existing `SET_DND` action to a rule. |
| Live strip, status-bar chip | Possible for our own | Android 16 `ProgressStyle` + `setRequestPromotedOngoing(true)` + `POST_PROMOTED_NOTIFICATIONS` for the batch countdown; the system decides promotion. A listener can see `FLAG_PROMOTED_ONGOING` on others but not promote them. |
| Re-rank or re-classify the shade | System only | `NotificationAssistantService` and `Adjustment` are `@SystemApi`; one assistant per device, signature permission, on Pixel inside Android System Intelligence. Pixel's Notification Organizer and Gemini summaries cannot be hooked. On 36.1 a listener probably sees bundled notifications under the reserved channel ids `PROMOTIONS_ID`, `NEWS_ID`, `SOCIAL_MEDIA_ID`, `RECS_ID` (inferred from the API, unverified on device); use them as provisional lane hints. |
| Notification cooldown | System only | No developer API. Approximate per sender with our own cooldown; `getLastAudiblyAlertedMillis()` tells us when the system last alerted. |
| Background services | Constraint | The listener is bound by the system and needs no foreground service. Starting a foreground service from inside a listener callback under Android 12+ background-start rules is untested; schedule delivery with `AlarmManager` / WorkManager and post from a receiver instead. |

Open points to test on a device before Phase E: snoozing foreground-service notifications;
channel ids of bundled notifications on Pixel 36.1; reply mutability across WhatsApp, Signal,
Telegram, Slack and Gmail.

---

## 5. What we borrow, and from whom

Research scan, November 2026. Items marked (unverified) came from secondary coverage only.

| Source | Mechanic | What we take |
|---|---|---|
| **BuzzKill** | Rule = app + content condition + action; cooldown; batch and "Summarize" with an Expand button; keyword *exception* that breaks through a batch; snooze to a clock time; sticky and remind-me; 10 s undo; auto-reply if unseen. | Breakthrough keywords, snooze-to-time, cooldown, the undo window. |
| **Daywise** (maintenance status unverified) | Everything held in its own inbox and released at "notification breaks"; apps auto-sorted Instant vs Batched; VIP contacts bypass; built on the Fitz et al. study. | The Instant / Batched / VIP three-tier default and the two-per-day schedule. |
| **FilterBox** | Offline spam classifier; escalation into a call-style full-screen alert; webhook egress; searchable history with restore. | The escalation ladder (quiet, normal, breakthrough) and the webhook idea for the SemPKM bridge. |
| **Pixel Notification Organizer** (Android 16 QPR2, Pixel 9+) | On-device classification by *content* into Promotions, News, Social, Suggested, delivered silently into the Silent section. | Content-based default lanes with silent delivery. The system does it only on Pixels; we do it everywhere, with user-editable lanes. |
| **Android 16 Live Updates, Samsung Now Bar, dynamicSpot** | A separate ambient surface for *ongoing* state so the shade holds only discrete events. | The Live lane and the strip at the top of `Now`; post our own ProgressStyle summaries where the OS supports it. |
| **HEY** | Screener: decide once per sender, route forever to Imbox / Feed / Paper Trail; notifications opt-in per thread. | Screening as the primary onboarding of every new sender; per-person routing. |
| **Superhuman and Shortwave** | User-authored splits as tabs; bundles collapsing one sender's items into a row with bulk actions. | Lanes are user-authored, and bundle rows in the stream. |
| **Gmail categories** | Only Primary alerts; the category *is* the notification policy. | A lane's policy is the only place that decides whether anything alerts. |
| **Beeper Low Priority** | Hidden and silent except @mentions and direct replies. | Breakthrough-by-mention inside the People lane. |
| **iOS Scheduled Summary and Priority Notifications** | Digest at user-chosen times; the model lifts urgent items. | The batch summary layout and the digest template. |
| **Unpluq, one sec, ClearSpace** | Friction at the moment of acting (NFC tag, breathing pause, a walk). | Modes can require a pause before opening a blocked app; this replaces the hard overlay for most users. |
| **Fitz et al. 2019** | RCT: three batches a day beat hourly batching and beat no alerts; silence raised anxiety. | Defaults of two to three batches; never ship "all silent" as a preset. |
| **Pielot et al. 2018** | 795 k notifications: messaging converts to app opens at 64 percent, promos at 16; a third arrive while the phone is in use. | Messaging is Instant by default; defer batches to breakpoints when the phone is in use. |
| **Gluck et al. 2007, Horvitz bounded deferral** | Match attentional draw to utility; hold until a breakpoint or a maximum delay. | The delivery rules in 3.3. |

---

## 6. Roadmap

Each phase is independently shippable and keeps the app usable throughout.

**Phase A: listener foundation (prerequisite).** Store `key`, channel id, importance,
conversation flag, person, icons, actions, `RemoteInput`s and `contentIntent`; match removals
by key and record the reason; treat ongoing notifications as live state; sync on connect;
de-duplicate updates by key. Migrate Room to v4.

**Phase B: theme and shell.** material3 1.5.0-beta02 upgrade, `MaterialExpressiveTheme`, static
palette, type, LED motif, three-tab shell with the existing screens re-homed. No feature
changes, so the regression surface is layout only.

**Phase C: `Now` v1.** Stream with icons, lanes as a read-only classification from category
and channel, time sections, swipe archive with undo, search, recently dismissed. Lanes are
not yet editable.

**Phase D: Screener and Apps profile.** Editable lanes, the Screener sheet, per-sender
routing, the per-app profile replacing Rules and Insights, suggestions rewritten as
re-screening prompts.

**Phase E: Delivery.** Mirroring for non-Instant lanes, batch times, bounded deferral,
cooldown, snooze-to-time, the batch summary notification, decay. This is the phase that makes
the app change how the phone behaves rather than report on it.

**Phase F: Modes.** Presets become modes with instant lanes and blocked apps; screen-time
enforcement moves into modes; friction options.

**Phase G: SemPKM bridge.** `service-bridge` module, context push, device registration, the
SemPKM lane, persona-to-mode mapping. Needs the SemPKM-side fields `focus_mode` and
`notification_pressure` and a PROJECT.md privacy rewrite.

---

## 7. Decisions needed from the owner

1. Rename or keep "Usage Control".
2. Accept the privacy rewrite for the SemPKM bridge, or keep the app offline and let the
   Expo app remain the Android context provider.
3. Mirroring trade-off: for Batched, Quiet and Digest lanes the shade shows *our* app's icon
   with the original app's icon as the large icon. Instant lanes stay untouched. Confirm this
   is acceptable before Phase E.
4. Default lanes and batch times for first run.
