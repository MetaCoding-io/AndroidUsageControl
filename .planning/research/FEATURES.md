# Feature Research

**Domain:** Android app onboarding / permission walkthrough / feature tour / setup wizard
**Researched:** 2026-02-28
**Confidence:** HIGH (official Android docs + multiple cross-verified sources)

---

## Context: This App's Permission Reality

All three required permissions (Notification Access, Usage Access, DND Access) are **Android special permissions**, not standard runtime permissions. They cannot be requested via `ActivityResultLauncher`. Each requires:
1. A custom check function (not `checkSelfPermission`)
2. Navigating user to a specific Settings screen via explicit Intent
3. Detecting grant on return (in `onResume` / `LaunchedEffect`)

This fundamentally shapes the onboarding UX: there is no system dialog to prime around — the app must motivate the user to navigate to Settings, flip a switch, and come back. This is a higher-friction flow than typical permission requests.

| Permission | Check Method | Settings Intent |
|------------|-------------|-----------------|
| Notification Access | `NotificationManagerCompat.getEnabledListenerPackages()` | `Settings.ACTION_NOTIFICATION_LISTENER_SETTINGS` |
| Usage Access | `AppOpsManager.checkOpNoThrow(OPSTR_GET_USAGE_STATS)` | `Settings.ACTION_USAGE_ACCESS_SETTINGS` |
| DND (Policy Access) | `NotificationManager.isNotificationPolicyAccessGranted()` | `Settings.ACTION_NOTIFICATION_POLICY_ACCESS_SETTINGS` |

---

## Feature Landscape

### Table Stakes (Users Expect These)

Features users assume exist. Missing these = product feels incomplete or broken.

| Feature | Why Expected | Complexity | Notes |
|---------|--------------|------------|-------|
| Welcome screen with value proposition | Users need to understand what the app does before granting invasive permissions; without this, permission requests feel hostile | LOW | Single Composable screen, shown once. Dismiss stores flag in DataStore. |
| One-time-only show logic | Returning users must not see onboarding again; this is a basic app contract | LOW | Boolean in DataStore Preferences (`onboarding_completed`). Check on startup, conditional NavGraph start destination. |
| Skip/exit from onboarding at any step | Forcing users through onboarding is the #1 onboarding anti-pattern; users who want to explore independently must be able to | LOW | "Skip" text button visible on all onboarding screens. Skipping marks onboarding as complete. |
| Permission status indicators on each permission step | Users need to know if a permission is already granted; the UI should update immediately on return from Settings | LOW-MEDIUM | `LaunchedEffect` + `DisposableEffect` / `onResume` lifecycle hook to re-check status. Checkmark/green state when granted. |
| Deep link to correct Settings screen per permission | Users are confused by generic Settings; the app must open the exact right screen directly | LOW | Use explicit Intents per permission (see table above). `startActivity(Intent(Settings.ACTION_NOTIFICATION_LISTENER_SETTINGS))`. |
| "Why does this app need X?" rationale per permission | Without explanation, users deny permissions that feel invasive (notification access feels especially scary) | LOW | One sentence + concrete benefit per permission on the priming screen before the Settings redirect button. |
| Graceful degradation if permissions skipped | App must not crash or show broken state if any permission is denied; features should degrade with clear messaging | MEDIUM | Already partially built (services catch exceptions). Onboarding must not block main app access. |
| Progress indicator during permission flow | Users need to know how many steps remain; "will this ever end?" causes abandonment | LOW | Step dots or "Step 2 of 3" label. Material 3 `LinearProgressIndicator` or custom dot indicators. |
| Return-to-app flow after Settings | After granting permission in Settings, user presses back — app must detect and confirm the grant, advance to next step | MEDIUM | `LaunchedEffect(lifecycleOwner)` observing ON_RESUME to re-check permission status. Show success state before auto-advancing. |
| Feature tour of 5 tabs | First-time users don't know what each tab does; a brief orientation prevents "I don't get this app" abandonment | LOW-MEDIUM | HorizontalPager with 5 slides (one per tab), each showing the tab name + one-sentence description + icon. Can be skipped. |

### Differentiators (Competitive Advantage)

Features that set the product apart. Not required, but valued.

| Feature | Value Proposition | Complexity | Notes |
|---------|-------------------|------------|-------|
| Permission priming screens (custom rationale UI before Settings redirect) | "Permission priming increases acceptance by 81%" (Appcues research). Showing WHY before sending to Settings dramatically improves grant rate vs just showing a button | LOW | One screen per permission: icon, headline, 1-2 sentence benefit, "Grant in Settings" button, "Skip" link. This is different from Android's `shouldShowRequestPermissionRationale` — that API does not apply to special permissions. |
| Visual permission status throughout onboarding (live check) | Users who granted a permission before reaching that step see a green checkmark without having to tap anything; reduces confusion | LOW-MEDIUM | On each permission step, check status on compose and show "Already granted" state instantly. |
| Quick setup wizard (first rule or preset) | Gets users from "I have the app" to "the app is doing something for me" — the fastest path to first value. Night Mode preset and Meeting Mode preset already exist. | MEDIUM | After permission flow, offer: "Set up Night Mode automatically?" with one-tap enable. This replaces a blank-slate feeling with immediate value. |
| Contextual onboarding hints (first-use coach marks on tabs) | Highlights specific UI elements (e.g., "Tap here to see your notification history") on first visit to each tab rather than a pre-use slideshow — more effective because it's in context | MEDIUM-HIGH | Requires overlay/coach mark system. Material 3 has `RichTooltip` but not a full coach mark system. Libraries: `coachmark` (pseudoankit), `reveal` (svenjacobs). Adds dependency. |
| Permission status card in Settings (persistent) | After onboarding, users can return to Settings to see which permissions are granted and fix ones that have been revoked; addresses "things stopped working" support confusion | LOW | Already partially exists per PROJECT.md ("permission shortcuts" in Settings). Enhance to show grant status clearly. |
| Onboarding accessible from Settings ("Revisit tour") | Power users or users who skipped want to revisit the tour; also useful after major feature updates | LOW | "Take the tour" button in Settings resets the onboarding state or launches a separate tour flow. |

### Anti-Features (Commonly Requested, Often Problematic)

Features that seem good but create problems.

| Feature | Why Requested | Why Problematic | Alternative |
|---------|---------------|-----------------|-------------|
| Require all permissions before showing main app | Feels thorough and ensures app works from the start | Blocks users who want to explore; causes abandonment; violates Android best practices; app must work (degraded) without permissions | Show main app immediately on skip; degrade gracefully per missing permission |
| Carousel of feature marketing slides (5+ screens) | Explains everything upfront | Users don't retain information before using the app; static slides teach nothing; Day 1 retention is 22.6% for Android — every extra screen risks losing the user | Max 3 permission steps + 1 quick tab tour. No marketing slides. |
| Mandatory account creation or registration | Common for subscription apps | This app has no internet, no accounts — forcing any form of data entry is 100% friction with 0% benefit | Skip entirely; this app has no auth concept |
| Inline tutorial that blocks the UI | Seems like good guidance | Annoying; users who already know what to do are stuck; crashes the experience for returning/re-install users | Use contextual hints on first interaction with each feature, not blocking overlays |
| Animated mascot / product tour character | Feels friendly and polished | Adds engineering complexity, increases APK size, can feel patronizing to Android utility app users | Simple icons + clear copy achieves the same trust without the overhead |
| "What do you want to use this app for?" personalization quiz | Used by consumer apps (Duolingo, etc.) for personalization | This app has no personalization backend (no internet, privacy-first). A quiz that doesn't adapt the experience is false engagement | Skip; the app's features are fixed. Quick setup wizard (preset selection) achieves the "configure to your needs" feeling instead. |
| Push notification opt-in during onboarding | Increases engagement in commercial apps | This app doesn't send push notifications. Asking for a permission it doesn't need destroys trust for an app positioned as privacy-first | Never ask for this |
| Gamification / progress bars with streaks during onboarding | Drives engagement in habit apps | Engineering overhead; doesn't fit a utility/privacy app's brand | Clean, minimal onboarding reflects the app's brand values |

---

## Feature Dependencies

```
[Welcome Screen (value prop)]
    └──leads to──> [Permission Flow]
                       ├──requires──> [DataStore: onboarding_completed flag]
                       ├──step 1──> [Notification Access Priming + Settings redirect]
                       ├──step 2──> [Usage Access Priming + Settings redirect]
                       └──step 3──> [DND Access Priming + Settings redirect]
                                        └──leads to──> [Feature Tour (tab overview)]
                                                            └──leads to──> [Quick Setup Wizard]
                                                                                └──marks complete──> [Main App NavGraph]

[Settings: Permission Shortcuts] ──re-uses logic from──> [Permission Status Check]
[Settings: "Revisit Tour"] ──resets──> [DataStore: onboarding_completed flag]

[Main App NavGraph] ──checks on start──> [DataStore: onboarding_completed flag]
    └──if false──> [Onboarding NavGraph]
    └──if true──> [Normal start: Notifications tab]
```

### Dependency Notes

- **Permission flow requires DataStore**: The `onboarding_completed` boolean must be persisted to DataStore Preferences (not SharedPreferences — DataStore is the current Android recommendation and supports async/Flow). Read before NavHost renders to avoid flash.
- **NavGraph requires startup check**: `MainActivity` or a `SplashViewModel` must read the DataStore value before deciding the start destination. A `LaunchedEffect` at the NavHost level reading a Flow prevents screen flash.
- **Permission status check requires lifecycle awareness**: `onResume` equivalent in Compose is `LaunchedEffect(lifecycleOwner.lifecycle)` observing `Lifecycle.Event.ON_RESUME`. This is needed to detect when user returns from Settings.
- **Feature tour does not require permissions**: The tab overview can be shown regardless of permission grant status — it describes what the tabs do, not what data is available.
- **Quick setup wizard requires at least Notification Access**: The first useful rule (e.g., auto-dismiss) requires NotificationListenerService to be active. Wizard should only offer relevant presets based on which permissions are granted.
- **Coach marks conflict with early tour**: If a pre-use feature tour is shown (slideshow), first-use coach marks on the same elements feel redundant. Pick one: either a pre-use tour or contextual first-use hints, not both.

---

## MVP Definition

### Launch With (v0.2 milestone — self-sufficient for new users)

These are the minimum features needed so a new user can install, grant permissions, and start using the app without external help.

- [x] Welcome screen — one screen, value prop, "Get Started" button, "Skip" link
- [x] Permission flow — 3 steps (Notification, Usage, DND), each with priming text + Settings button + live status check on return
- [x] One-time-only logic — DataStore flag, conditional NavGraph start destination
- [x] Skip at any step — always visible, marks onboarding complete
- [x] Feature tour — HorizontalPager, 5 slides (one per tab), skip available
- [x] Quick setup wizard — offer Night Mode or Meeting Mode preset with one tap

### Add After Validation (v0.2.x)

Features to add once core onboarding is working.

- [ ] "Revisit tour" in Settings — add after confirming users want it (check if Settings screen gets taps on placeholder)
- [ ] First-use coach marks on tabs — add only if user testing shows tab confusion persists after the tour
- [ ] Permission status card polish in Settings — enhance existing permission shortcuts with grant/revoke status

### Future Consideration (v1.x+)

Features to defer until the onboarding flow is validated.

- [ ] Interactive walkthrough (guided try-it) — only if testing shows users don't understand rule creation after static tour
- [ ] Contextual hints system (coach mark library) — adds dependency, deferred until simpler options prove insufficient

---

## Feature Prioritization Matrix

| Feature | User Value | Implementation Cost | Priority |
|---------|------------|---------------------|----------|
| Welcome screen | HIGH | LOW | P1 |
| Permission flow (3 steps) | HIGH | MEDIUM | P1 |
| One-time-only show logic (DataStore) | HIGH | LOW | P1 |
| Skip at any step | HIGH | LOW | P1 |
| Progress indicator in permission flow | HIGH | LOW | P1 |
| Return-from-Settings detection | HIGH | MEDIUM | P1 |
| Feature tour (5-tab HorizontalPager) | MEDIUM | LOW | P1 |
| Quick setup wizard (preset offer) | MEDIUM | LOW-MEDIUM | P1 |
| Permission priming screens | HIGH | LOW | P1 |
| Live permission status on each step | MEDIUM | LOW | P1 |
| "Revisit tour" in Settings | LOW | LOW | P2 |
| First-use coach marks on tabs | MEDIUM | HIGH | P3 |
| Permission status card in Settings | MEDIUM | LOW | P2 |

**Priority key:**
- P1: Must have for v0.2 milestone launch
- P2: Should have, add when possible
- P3: Nice to have, future consideration

---

## Competitor Feature Analysis

Apps that share this app's UX challenge: privacy/utility apps requiring special Android permissions.

| Feature | Digital Wellbeing (Google) | StayFree / AppBlock | Our Approach |
|---------|---------------------------|---------------------|--------------|
| Permission onboarding | Built into OS setup; no extra flow needed | Permission wizard with step-by-step settings redirects | Permission priming + step-by-step with live status |
| Feature tour | None (assumes OS knowledge) | Minimal — dashboard walkthrough | 5-slide tab overview, skippable |
| First run experience | N/A | Immediate rule setup after permissions | Quick setup wizard (preset offer) |
| Skip option | N/A | Varies; some force permission grant | Always visible skip at every step |
| Permission rationale | OS-level generic | Brief in-app text before redirect | App-specific benefit text per permission |
| Post-onboarding | Lands on dashboard | Lands on main screen | Lands on Notifications tab (first tab) |

---

## Implementation Notes (Specific to This App's Codebase)

### Current State
- `MainActivity.kt` has `startDestination = AppScreen.Notifications.route` — hardcoded, no onboarding check
- No DataStore dependency currently in the app module (check `libs.versions.toml`)
- Services are started in `onCreate` before permissions are checked — could crash on first launch (already has try/catch wrapper)
- Permission shortcuts exist in Settings screen but do not show grant status

### Key Implementation Points

1. **NavGraph change**: Add `"onboarding"` route as conditional start destination. Read DataStore in a `ViewModel` that `MainActivity` uses to decide. Use `startDestination` determined from the ViewModel's state (read synchronously with `runBlocking` or use a splash screen).

2. **DataStore dependency**: Add `androidx.datastore:datastore-preferences` to `app/build.gradle.kts`. Inject via Hilt as a singleton.

3. **HorizontalPager**: Already in Compose foundation (`androidx.compose.foundation.pager`). No new dependency needed for the feature tour.

4. **Permission check on resume**: Use `LifecycleEventObserver` inside `DisposableEffect` or `LaunchedEffect` checking `Lifecycle.Event.ON_RESUME` to re-evaluate each special permission's status when user returns from Settings.

5. **Avoid**: Do not use `shouldShowRequestPermissionRationale` — it does not apply to special permissions. Do not use `ActivityResultLauncher` for these permissions — they use Settings Intents, not the runtime permission API.

---

## Sources

- [Request runtime permissions | Android Developers](https://developer.android.com/training/permissions/requesting) — HIGH confidence, official
- [Request special permissions | Android Developers](https://developer.android.com/training/permissions/requesting-special) — HIGH confidence, official
- [App permissions best practices | Android Developers](https://developer.android.com/training/permissions/usage-notes) — HIGH confidence, official
- [Tooltip | Jetpack Compose | Android Developers](https://developer.android.com/develop/ui/compose/components/tooltip) — HIGH confidence, official
- [Mobile App Onboarding Analysis | Nielsen Norman Group](https://www.nngroup.com/articles/mobile-app-onboarding/) — HIGH confidence, authoritative UX research
- [3 Design Considerations for Permission Requests | NN/G](https://www.nngroup.com/articles/permission-requests/) — HIGH confidence, authoritative UX research
- [Asking nicely: Mobile permission priming | Appcues](https://www.appcues.com/blog/mobile-permission-priming) — MEDIUM confidence, industry practitioner source
- [Mobile Onboarding UX: 11 Best Practices | Design Studio](https://www.designstudiouiux.com/blog/mobile-app-onboarding-best-practices/) — MEDIUM confidence, verified against NN/G findings
- [Android permissions best practices 2025 | Medium](https://medium.com/@vivek.beladia/mastering-android-permissions-in-2025-best-practices-and-new-trends-0c1058c12673) — LOW confidence, single source; directionally consistent with official docs
- [7 Onboarding Mistakes | Glance](https://thisisglance.com/blog/7-onboarding-mistakes-that-are-killing-your-apps-success) — MEDIUM confidence, anti-patterns consistent with official docs guidance
- [Android runtime permissions Jetpack Compose guide | Composables](https://composables.com/blog/permissions) — MEDIUM confidence, practitioner verified against official docs

---

*Feature research for: Android onboarding flow / permission walkthrough / feature tour / setup wizard*
*Researched: 2026-02-28*
