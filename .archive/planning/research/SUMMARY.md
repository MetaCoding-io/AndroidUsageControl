# Project Research Summary

**Project:** AndroidUsageControl — v0.2 Onboarding Milestone
**Domain:** Android onboarding / permission walkthrough / feature tour / setup wizard (Jetpack Compose, multi-module)
**Researched:** 2026-02-28
**Confidence:** HIGH

## Executive Summary

AndroidUsageControl v0.2 requires adding a first-run onboarding experience to a working Jetpack Compose app that already has all core features built. The onboarding flow must walk new users through three Android special permissions (Notification Access, Usage Access, DND Access) — permissions that cannot use the standard runtime permission API and instead require sending users to system settings pages. This is a fundamentally higher-friction flow than typical app onboarding, and its design must account for the round-trip to Settings, the absence of OS callbacks on grant, and the need to re-check permission status when the user returns. The recommended approach is a nested Compose Navigation graph (`feature-onboarding` module) layered on top of the existing NavHost, with a DataStore-backed completion flag that gates the start destination.

The minimal set of new dependencies is small: `datastore-preferences:1.1.7` for the onboarding completion flag, `accompanist-permissions:0.37.3` only for the `POST_NOTIFICATIONS` runtime permission (Android 13+), and a Compose BOM upgrade from `2024.02.00` to `2025.01.01` to maintain compatibility. Everything else — `HorizontalPager` for the feature tour, `AnimatedContent` for transitions, dot indicators, and navigation between onboarding steps — uses libraries already present in the project. Third-party onboarding libraries, a second Activity, and any attempt to use `checkSelfPermission` or `ActivityResultLauncher` for the three special permissions are all anti-patterns to avoid.

The primary risk is correctness of the permission layer. Every pitfall identified that would block a user in the onboarding flow or cause the flow to show incorrect state (stale "not granted" UI, wrong check API, no migration for existing users, back-navigation into onboarding from the main app) has a HIGH recovery cost if discovered late. These must be designed correctly before the first screen is built. The navigation architecture (splash routing pattern, `popUpTo` cleanup) and the `PermissionChecker` utility must be the first things implemented, as every subsequent screen depends on them.

---

## Key Findings

### Recommended Stack

The app already has everything it needs for onboarding except DataStore. The only new Gradle dependencies are `datastore-preferences:1.1.7` (onboarding flag persistence) and `accompanist-permissions:0.37.3` (POST_NOTIFICATIONS on Android 13+). The Compose BOM should be upgraded from `2024.02.00` to `2025.01.01` to maintain Accompanist compatibility — this is a low-risk upgrade with no API breakage. `HorizontalPager` (Compose Foundation) is already transitive via `compose-material3` and requires no new dependency; the Accompanist pager is deprecated and must not be used. All animation APIs needed for screen transitions are in `compose-animation`, already transitive.

**Core technologies (new additions only):**
- `datastore-preferences:1.1.7` — onboarding completion flag — coroutine-native Flow API; correct modern replacement for SharedPreferences for new code
- `accompanist-permissions:0.37.3` — POST_NOTIFICATIONS runtime permission — the only maintained Compose runtime-permission helper; not used for special permissions (which require Settings intents)
- Compose BOM `2025.01.01` — version alignment — enables Accompanist 0.37.x compatibility; low-risk upgrade
- `HorizontalPager` (Foundation, via BOM) — 5-slide feature tour — already transitive; graduated stable since Compose 1.4.x; Accompanist pager is removed

**Critical version constraint:** Do not use `accompanist-permissions` for Notification Access, Usage Access, or DND Access. These are not runtime permissions. Do not use Navigation 3 (alpha, breaking APIs). Pin DataStore to 1.1.7, not 1.2.0 (avoids unnecessary Guava dependency).

### Expected Features

The app's three special permissions set an unusual onboarding constraint: there are no system dialogs to prime around, so the app must motivate the user to navigate to Settings, flip a toggle, and return — a higher-friction flow requiring explicit rationale screens and lifecycle-aware re-checking. The UX pattern research is clear that permission priming screens (explaining why before the Settings redirect) increase grant rates significantly and are non-optional for permissions as invasive as Notification Access.

**Must have (table stakes — v0.2 launch blockers):**
- Welcome screen with value proposition — users must understand the app before facing invasive permission requests
- Permission flow — 3 sequential steps (Notification, Usage, DND), each with priming text, Settings button, and live re-check on return
- One-time-only show logic — DataStore flag; conditional NavGraph start destination; never show to returning users
- Skip at any step — mandatory; forces completion or permission-blocked users can never access the app
- Progress indicator — step count or dot indicators so users know how many steps remain
- Return-from-Settings detection — lifecycle observer re-checks permission status on `ON_RESUME`; no manual tap required
- Feature tour — `HorizontalPager`, 5 slides (one per tab), skip available
- Quick setup wizard — one-tap preset offer (Night Mode or Meeting Mode) for immediate first value

**Should have (differentiators — add after v0.2 validates):**
- "Revisit tour" in Settings — add only after confirming users want it via Settings tap data
- Permission status card polish in Settings — enhance existing permission shortcuts to show grant/revoke state
- Inline "grant permission" CTA on each tab's empty state — re-entry path for users who skipped

**Defer (v1.x+):**
- First-use coach marks on individual UI elements — adds library dependency; defer until static tour proves insufficient
- Interactive guided walkthrough — only if user testing shows rule creation is still confusing post-onboarding

**Anti-features to avoid entirely:** mandatory permission grant before main app access, marketing slide carousels (5+ screens), personalization quiz, push notification opt-in, blocking overlays during onboarding.

### Architecture Approach

The architecture is a new `feature-onboarding` Gradle module containing a nested Compose Navigation graph, a `@HiltViewModel`, a DataStore-backed repository, and 4 screen composables. A `PermissionChecker` object in `core-common` centralises the three special-permission status checks, shared between `feature-onboarding` and `feature-settings`. The existing `MainActivity` is minimally modified to inject `OnboardingViewModel` and set the NavHost `startDestination` based on a `StateFlow<Boolean?>` — holding at a splash screen while the DataStore read resolves (~10ms), then routing to either `"onboarding"` or `"notifications"`. `feature-onboarding` depends on `core-common`, `core-model`, `core-database`, and `core-ui`; it must NOT depend on any other feature module.

**Major components:**
1. `PermissionChecker` (core-common) — stateless object; correct check API per permission; shared by onboarding and settings
2. `OnboardingRepository` (feature-onboarding) — DataStore read/write for completion flag; singleton via Hilt
3. `OnboardingViewModel` (feature-onboarding) — permission re-check on resume; marks completion; applies presets via `RuleDao`
4. Onboarding nested nav graph (app module) — `splash → onboarding → main` routing; `popUpTo(inclusive=true)` clears back stack
5. 4 screen composables — `WelcomeScreen`, `PermissionWalkthroughScreen`, `FeatureTourScreen`, `QuickSetupScreen`

**Implementation order is mandatory:** `PermissionChecker` and DataStore dependency must exist before any screen is written; the navigation architecture (splash routing, `popUpTo` cleanup) must be correct before onboarding is first wired into the app.

### Critical Pitfalls

1. **Wrong permission check API** — `checkSelfPermission()` always returns DENIED for special permissions. Use `NotificationManagerCompat.getEnabledListenerPackages()`, `AppOpsManager.checkOpNoThrow()`, and `NotificationManager.isNotificationPolicyAccessGranted()`. Gate behind `PermissionChecker` in `core-common` to prevent misuse. Recovery cost: MEDIUM (audit of all call sites).

2. **Permission state not re-checked on resume** — Compose does not automatically update permission status when the user returns from Settings. Every permission screen must use `DisposableEffect(lifecycleOwner)` with a `LifecycleEventObserver` that calls `viewModel.recheckPermissions()` on `ON_RESUME`. Recovery cost: LOW, but causes a broken UX if missed.

3. **Back navigation into onboarding from main app** — If `"onboarding"` is the NavHost `startDestination`, back navigation from the main app re-enters the onboarding screens. Fix: use a `"splash"` routing destination as the true `startDestination`; navigate to onboarding or main with `popUpTo("splash") { inclusive = true }`. Recovery cost: HIGH — requires navigation graph refactor.

4. **Existing users shown onboarding after update** — Users with all permissions already granted will have no `onboarding_complete` DataStore key and be routed to onboarding. Fix: on first launch after update, check if all permissions are already granted; if so, write the completion flag and skip the flow. Recovery cost: MEDIUM (patch release).

5. **Services starting before permissions granted** — `startMonitoringServices()` is called on every `MainActivity.onCreate` without a permission check. On fresh install during onboarding, this causes battery-draining polling and a foreground notification before the user has granted anything. Fix: gate service start on permission check; move service start to the onboarding completion event.

6. **NotificationListenerService fails to rebind after permission grant** — A documented AOSP bug can cause the NLS to not receive callbacks even after the toggle is enabled in Settings. Fix: call `requestRebind(ComponentName(...))` after the user returns from Settings; surface a "Verify / Retry" action on the confirmation screen.

---

## Implications for Roadmap

Based on the combined research, the v0.2 milestone should be implemented in four sequential phases. The ordering is driven by hard technical dependencies: the navigation architecture and permission utilities must be correct before any screen exists, because they are foundational to everything else.

### Phase 1: Foundation — Navigation Architecture and Permission Utilities

**Rationale:** Every pitfall with HIGH recovery cost lives here. The navigation routing pattern (splash destination, `popUpTo` cleanup), the DataStore dependency, and the `PermissionChecker` utility must all be correct before any onboarding screen is written. Retrofitting these after screens are built is expensive and error-prone.

**Delivers:** A working conditional navigation system that routes returning users to the main app and new users to the onboarding graph. No visible UI change for returning users.

**Addresses:** DataStore onboarding flag (FEATURES.md P1), conditional NavGraph start destination (FEATURES.md P1)

**Avoids:**
- Back navigation into onboarding (Pitfall 5 — HIGH recovery cost)
- Existing users shown onboarding (Pitfall 4 — MEDIUM recovery cost)
- Wrong permission check API (Pitfall 3 — built correctly from the start via `PermissionChecker`)

**Concrete deliverables:**
- `PermissionChecker` object in `core-common` with correct check methods for all three permissions
- `datastore-preferences:1.1.7` added to `libs.versions.toml`; Compose BOM upgraded to `2025.01.01`
- `feature-onboarding` Gradle module created (empty, with correct build file)
- `OnboardingRepository` + `OnboardingModule` (DataStore wrapper + Hilt binding)
- `OnboardingViewModel` exposing `isOnboarded: StateFlow<Boolean?>`
- Navigation architecture wired into `app` module: splash routing, nested onboarding graph skeleton, `popUpTo` cleanup on completion
- Existing user migration logic: if all permissions granted and flag not set, write flag immediately

**Research flag:** Well-documented patterns. No additional research needed.

---

### Phase 2: Permission Walkthrough Screens

**Rationale:** The permission flow is the highest-value, highest-friction part of the onboarding. Users who cannot grant permissions get no value from the app. The pitfall density is highest here (Pitfalls 1, 2, 3, 6, 7, 9). These screens depend on Phase 1's `PermissionChecker` and `OnboardingViewModel` being in place.

**Delivers:** A 3-step permission walkthrough that correctly guides users through Notification Access, Usage Access, and DND Access — with rationale screens before each Settings redirect, live status re-check on return, progress indicator, skip at every step, and graceful handling of work profile DND unavailability.

**Uses:** `PermissionChecker` (Phase 1), `OnboardingViewModel.recheckPermissions()` (Phase 1), `DisposableEffect` lifecycle observer pattern, `Settings.ACTION_NOTIFICATION_LISTENER_DETAIL_SETTINGS` (API 31+ deep link), `accompanist-permissions` (POST_NOTIFICATIONS on API 33+)

**Implements:** `PermissionWalkthroughScreen` composable, `PermissionStatusCard` component, `OnboardingProgressBar` component

**Avoids:**
- Permission state not re-checked on resume (Pitfall 2 — lifecycle observer on every step)
- No feedback during Settings navigation (Pitfall 8 — pre-permission rationale screens required)
- Services starting before permissions granted (Pitfall 9 — gate `startMonitoringServices()` in this phase)
- UsageStats empty on first grant (Pitfall 6 — set user expectations on confirmation screen)
- Work profile DND loop (Pitfall 7 — detect `isManagedProfile()`, mark DND optional)

**Research flag:** Standard patterns for special permission flows. No additional research needed. However, device-testing on a managed/work profile device or emulator should be part of verification.

---

### Phase 3: Welcome Screen, Feature Tour, and Quick Setup Wizard

**Rationale:** These screens depend on the permission walkthrough (Phase 2) being complete. The feature tour is deliberately after permissions so users see descriptions of features they can actually use. The Quick Setup wizard requires at least Notification Access to be granted. Welcome screen is simple enough to be built in this phase rather than first, since it has no logic dependencies.

**Delivers:** A complete end-to-end onboarding flow: Welcome → Permissions → Feature Tour → Quick Setup → Main App.

**Uses:** `HorizontalPager` + `rememberPagerState` (Compose Foundation, already transitive), custom dot indicator composable (8 lines, no library), existing `RuleDao` (for Quick Setup preset writes), `hiltViewModel(navBackStackEntry)` for shared ViewModel scope across wizard steps

**Implements:** `WelcomeScreen`, `FeatureTourScreen` (5 slides, stateless — no ViewModel needed), `QuickSetupScreen` (Night Mode / Meeting Mode preset), `OnboardingViewModel.applyPreset()` calling `RuleDao.insertRule()` directly

**Avoids:**
- Feature tour shown to users who regranted permissions (Pitfall 11 — track tour completion separately from permission state; use distinct DataStore keys)
- Bottom nav bar appearing during onboarding (Pitfall 12 — extend `showBottomBar` logic in `MainActivity` to exclude all onboarding routes)
- Pager state lost on configuration change (Pitfall 13 — persist current permission step index in `SavedStateHandle`)

**Research flag:** All patterns well-documented. `HorizontalPager` canonical usage is in official docs. No additional research needed.

---

### Phase 4: Post-Onboarding Integration and Polish

**Rationale:** After the core flow is working and tested, address the re-entry paths for users who skipped permissions, polish the Settings permission status cards, and add the NotificationListenerService rebind safety net. These items do not block the core onboarding flow but are required for a production-quality experience.

**Delivers:** Inline "grant permission" CTAs on Notifications and Screen Time tabs (for users who skipped), enhanced permission status in Settings, `requestRebind()` call on NLS permission confirmation, and the "Revisit tour" entry point in Settings.

**Addresses:**
- No re-entry path after skip (Pitfall 10 — inline CTAs on relevant tabs)
- NLS not rebinding after grant (Pitfall 1 — `requestRebind()` safety net)
- Permission status card in Settings (FEATURES.md P2)
- "Revisit tour" in Settings (FEATURES.md P2)

**Research flag:** Standard patterns. `requestRebind()` usage is verified against AOSP issue tracker. No additional research needed.

---

### Phase Ordering Rationale

- **Phase 1 before everything:** The navigation architecture and permission utilities are load-bearing. The most expensive pitfalls (back-nav into onboarding, existing user regression) must be solved before any screen is written.
- **Phase 2 before Phase 3:** Permission walkthrough is the highest-friction part of onboarding and has the most pitfall exposure. Feature tour is deliberately placed after permissions so users are oriented to features they can use, not a promise of features they cannot.
- **Phase 4 last:** Post-onboarding polish and re-entry paths are real requirements but do not block the primary user flow. They can be shipped in a 0.2.1 patch if needed.
- **Feature-to-feature module dependency avoided throughout:** `feature-onboarding` writes presets by calling `core-database` directly, never by importing `feature-rules` or `feature-settings`.

### Research Flags

Phases needing deeper research during planning:
- None identified. All four phases operate on well-documented Android and Jetpack patterns. Official docs for all APIs are authoritative and current.

Phases with standard patterns (skip research-phase):
- **Phase 1:** DataStore, Hilt multi-module, Compose Navigation nested graphs — all have official guides and verified examples in research.
- **Phase 2:** Special permission check APIs, `DisposableEffect` lifecycle observer, Settings intents — all verified against official docs and AOSP issue tracker.
- **Phase 3:** `HorizontalPager`, `rememberPagerState`, Hilt ViewModel scoping — canonical patterns in official docs.
- **Phase 4:** `requestRebind()`, inline empty state CTAs — straightforward; no novel integration required.

---

## Confidence Assessment

| Area | Confidence | Notes |
|------|------------|-------|
| Stack | HIGH | All library versions verified against official release pages. BOM upgrade path and compatibility warning documented. Version catalog additions are specific. |
| Features | HIGH | Permission flow design grounded in official Android docs and authoritative UX research (NN/G). Anti-feature list is opinionated and well-supported. MVP scope is conservative and achievable. |
| Architecture | HIGH | All patterns verified against official Android Navigation, Hilt, and DataStore docs. Anti-patterns are specific and confirmed against official guidance. Build order is unambiguous. |
| Pitfalls | HIGH | Critical pitfalls verified against AOSP issue tracker (NLS rebind bug), official docs, and multiple developer reports. Recovery cost estimates are grounded in codebase analysis. |

**Overall confidence:** HIGH

### Gaps to Address

- **Accompanist compatibility with BOM 2024.02.00:** The current BOM maps to Compose 1.6.x; `accompanist-permissions:0.37.3` targets 1.7.x. The STACK.md recommends upgrading BOM to `2025.01.01` as part of Phase 1. If the upgrade reveals unexpected breakage in existing screens, fall back to `accompanist-permissions:0.34.0` (targets 1.6.x) for the POST_NOTIFICATIONS flow. Validate BOM upgrade early in Phase 1.

- **`NotificationListenerService` rebind timing on target devices:** The AOSP bug (issue #226411020) affects some devices and debug reinstalls but not all. The `requestRebind()` mitigation is the documented workaround. Verify on at least one physical device during Phase 2 — emulator behavior may differ from real hardware.

- **Work profile DND availability:** No in-house test device with a managed work profile is confirmed. Phase 2 must include an emulator with a work profile configured to verify that `isManagedProfile()` detection and the "DND unavailable" code path work correctly.

- **DataStore file location on existing installs:** DataStore uses the app's data directory and survives app updates. The existing user migration logic (Pitfall 4) assumes the DataStore file does not exist for users who installed v0.1. Verify that no code in the v0.1 release accidentally created a DataStore file with the key `onboarding_complete` set to `false`.

---

## Sources

### Primary (HIGH confidence)
- [Android Navigation with Compose](https://developer.android.com/develop/ui/compose/navigation) — nested graphs, conditional navigation, `popUpTo`
- [Request Special Permissions](https://developer.android.com/training/permissions/requesting-special) — Notification Access, Usage Access, DND Access flows
- [Jetpack DataStore](https://developer.android.com/topic/libraries/architecture/datastore) — Preferences DataStore API, coroutine integration
- [Pager in Compose](https://developer.android.com/develop/ui/compose/layouts/pager) — `HorizontalPager`, `rememberPagerState`, dot indicators
- [Hilt in Multi-Module Apps](https://developer.android.com/training/dependency-injection/hilt-multi-module) — `@HiltViewModel`, `@Provides @Singleton` across modules
- [Compose BOM to library version mapping](https://developer.android.com/develop/ui/compose/bom/bom-mapping) — version compatibility matrix
- [androidx.datastore releases](https://developer.android.com/jetpack/androidx/releases/datastore) — version 1.1.7 confirmed stable
- [AOSP Issue Tracker #226411020](https://issuetracker.google.com/issues/226411020) — NLS rebind bug confirmation and `requestRebind()` workaround
- [Work profiles — Android Enterprise](https://developer.android.com/work/managed-profiles) — DND restrictions in managed profiles
- [UsageStatsManager](https://developer.android.com/reference/android/app/usage/UsageStatsManager) — `queryUsageStats()` behavior on locked devices
- [Mobile App Onboarding Analysis — NN/G](https://www.nngroup.com/articles/mobile-app-onboarding/) — skip requirements, anti-patterns
- [Permission Requests — NN/G](https://www.nngroup.com/articles/permission-requests/) — priming screen rationale

### Secondary (MEDIUM confidence)
- [Accompanist GitHub releases](https://github.com/google/accompanist/releases) — v0.37.3 latest; pager deprecated confirmed
- [Accompanist Permissions docs](https://google.github.io/accompanist/permissions/) — `rememberPermissionState` still experimental
- [Mastering Jetpack Compose Navigation 2025](https://medium.com/design-bootcamp/mastering-jetpack-compose-navigation-in-2025-with-nested-graphs-safe-args-2e1ccc2cc374) — nested graph patterns
- [Mobile permission priming — Appcues](https://www.appcues.com/blog/mobile-permission-priming) — 81% grant rate increase claim (directionally consistent with NN/G)
- [GSoC 2025: Onboarding Revamp in ListenBrainz Android](https://blog.metabrainz.org/2025/08/31/gsoc-2025-onboarding-revamp-in-listenbrainz-android/) — real-world NLS onboarding implementation

### Tertiary (LOW confidence)
- [Android permissions best practices 2025 — Medium](https://medium.com/@vivek.beladia/mastering-android-permissions-in-2025-best-practices-and-new-trends-0c1058c12673) — directionally consistent with official docs; single source
- [Work profile DND limitation — community report](https://community.atlassian.com/forums/Opsgenie-questions/OpsGenie-and-work-profiles-on-Android-9/qaq-p/1106115) — multiple community reports corroborate; no official doc

---
*Research completed: 2026-02-28*
*Ready for roadmap: yes*
