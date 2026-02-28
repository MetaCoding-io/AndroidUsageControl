---
phase: 02-permission-walkthrough
verified: 2026-02-28T21:00:00Z
status: passed
score: 11/11 must-haves verified
re_verification: false
gaps: []
human_verification:
  - test: "Full permission walkthrough on device — grant all three permissions in sequence"
    expected: "Steps show priming content, correct Settings screens open, green checkmark appears on return, step counter advances, main app reached after step 3"
    why_human: "Settings intents and ON_RESUME grant detection require a real device with system Settings interaction; cannot be verified programmatically"
  - test: "Skip all three permissions during onboarding"
    expected: "User reaches main app; Notifications tab shows notification banner, Screen Time tab shows usage banner, Controls tab shows DND banner"
    why_human: "Visual rendering and tab navigation require device or emulator"
  - test: "Banner disappears after granting permission and returning to app"
    expected: "ON_RESUME triggers recheckPermission(); banner is gone without app restart"
    why_human: "Requires returning from system Settings to verify live recheck behavior"
  - test: "NLS requestRebind behavior after Notification Access grant"
    expected: "NotificationCaptureService rebinds and begins receiving notification events"
    why_human: "AOSP bug #226411020 — NLS rebind only verifiable on physical device; emulator may not surface the issue"
---

# Phase 02: Permission Walkthrough Verification Report

**Phase Goal:** Users can successfully grant all three required permissions (Notification Access, Usage Access, DND Access) through a guided flow, or skip permissions at any step and still reach the main app with clear messaging about what is degraded

**Verified:** 2026-02-28T21:00:00Z
**Status:** passed
**Re-verification:** No — initial verification

---

## Goal Achievement

### Observable Truths

| # | Truth | Status | Evidence |
|---|-------|--------|----------|
| 1 | Each permission step shows a priming screen explaining why that permission is needed before the user taps a button to open Settings | VERIFIED | `PermissionStepContent` in `PermissionWalkthroughScreen.kt` renders `title`, `explanation`, and `hint` Text composables for each of the 3 `PermissionStep` enum values (lines 131–146) |
| 2 | Tapping 'Grant Access' opens the correct system Settings screen for each permission | VERIFIED | `openNotificationListenerSettings()` uses `ACTION_NOTIFICATION_LISTENER_DETAIL_SETTINGS` (API 30+) or `ACTION_NOTIFICATION_LISTENER_SETTINGS`; `openUsageAccessSettings()` uses `ACTION_USAGE_ACCESS_SETTINGS`; `openDndAccessSettings()` uses `ACTION_NOTIFICATION_POLICY_ACCESS_SETTINGS` — all in `PermissionWalkthroughScreen.kt` lines 224–266 |
| 3 | When the user returns from Settings after granting a permission, a green checkmark appears on that step without any manual tap | VERIFIED | `DisposableEffect(lifecycleOwner)` calls `viewModel.recheckAll()` on `Lifecycle.Event.ON_RESUME` (screen lines 35–43); `PermissionStepContent` renders `Icons.Default.CheckCircle` and "Access granted" text when `isGranted == true` (screen lines 180–199) |
| 4 | The user sees a step counter (1/3, 2/3, 3/3) and progress bar at all times during the walkthrough | VERIFIED | `Row` showing `"${state.currentStep + 1} / ${PermissionStep.TOTAL_STEPS}"` and `LinearProgressIndicator` rendered unconditionally inside `PermissionWalkthroughContent` `Column` (screen lines 78–98) |
| 5 | The user can skip any permission step and advance to the next step or complete onboarding | VERIFIED | `TextButton("Skip for now")` calls `onSkip` which calls `viewModel.advanceStep()` when `isGranted == false` (screen lines 209–214); `advanceStep()` increments step or calls `completeOnboarding()` (ViewModel lines 72–79) |
| 6 | After the last step (grant or skip), the user is navigated to the main app with onboarding marked complete | VERIFIED | `completeOnboarding()` writes `onboardingRepository.setOnboardingComplete(true)` and sets `isComplete = true`; `LaunchedEffect(state.isComplete)` triggers `onComplete()` nav lambda in screen (screen lines 46–48); `MainActivity.kt` nav lambda calls `navController.navigate("main") { popUpTo("onboarding") { inclusive = true } }` (MainActivity lines 115–121) |
| 7 | A user who skipped all permissions sees a warning banner on the Notifications tab explaining notification access is missing | VERIFIED | `NotificationsViewModel.showPermissionBanner` initialized from `!permissionChecker.isNotificationAccessGranted()` (line 26); `NotificationsScreen` renders `DegradedPermissionBanner` conditionally after `TopAppBar` (screen lines 80–84) |
| 8 | A user who skipped all permissions sees a warning banner on the Screen Time tab explaining usage access is missing | VERIFIED | `ScreenTimeViewModel.showPermissionBanner` initialized from `!permissionChecker.isUsageAccessGranted()` (line 25); `ScreenTimeScreen` renders `DegradedPermissionBanner` after `TopAppBar` (screen lines 69–73) |
| 9 | A user who skipped all permissions sees a warning banner on the Controls tab explaining DND access is missing | VERIFIED | `LockControlViewModel.showPermissionBanner` initialized from `!permissionChecker.isDndAccessGranted()` (line 30); `LockControlScreen` renders `DegradedPermissionBanner` after `TopAppBar` (screen lines 64–68) |
| 10 | A user who granted all permissions sees no warning banners on any tab | VERIFIED | All three `showPermissionBanner` StateFlows initialize to `false` when their respective `PermissionChecker` method returns `true`; conditional rendering only shows banner when `showPermissionBanner == true` |
| 11 | The warning banners disappear after granting the relevant permission and returning to the app (ON_RESUME re-check) | VERIFIED | All three feature screens have `DisposableEffect(lifecycleOwner)` calling `viewModel.recheckPermission()` on `ON_RESUME` (Notifications line 50, ScreenTime line 47, LockControl line 38) |

**Score:** 11/11 truths verified

---

### Required Artifacts

| Artifact | Expected | Exists | Substantive | Wired | Status |
|----------|----------|--------|-------------|-------|--------|
| `feature/feature-onboarding/src/main/java/com/usagecontrol/feature/onboarding/PermissionStep.kt` | Enum of three permission steps with index mapping | Yes | Yes — 3 entries, `TOTAL_STEPS = 3`, `fromIndex()` companion | Yes — imported in `PermissionWalkthroughScreen.kt` and `PermissionWalkthroughViewModel.kt` | VERIFIED |
| `feature/feature-onboarding/src/main/java/com/usagecontrol/feature/onboarding/PermissionWalkthroughViewModel.kt` | Step state management, permission re-check on resume, NLS requestRebind on grant transition, onboarding completion | Yes | Yes — `PermissionWalkthroughState`, `recheckAll()`, `advanceStep()`, `autoAdvancePastGranted()`, `completeOnboarding()`, `requestRebind` call | Yes — consumed via `hiltViewModel()` in `PermissionWalkthroughScreen` | VERIFIED |
| `feature/feature-onboarding/src/main/java/com/usagecontrol/feature/onboarding/PermissionWalkthroughScreen.kt` | Full walkthrough composable with ON_RESUME lifecycle observer, step counter, priming content, checkmark, and Settings launch | Yes | Yes — 267 lines; `DisposableEffect`, `LaunchedEffect`, `LinearProgressIndicator`, step counter, `PermissionStepContent`, Settings helpers | Yes — wired in `MainActivity.kt` nav graph | VERIFIED |
| `app/src/main/java/com/usagecontrol/MainActivity.kt` | Onboarding nav graph uses `PermissionWalkthroughScreen`, `markOnboardingComplete()` removed | Yes | Yes — import and composable call present; `markOnboardingComplete()` absent from onComplete lambda | Yes — `PermissionWalkthroughScreen` called in `composable("onboarding/start")` | VERIFIED |
| `core/core-ui/src/main/java/com/usagecontrol/core/ui/component/CommonComponents.kt` | `DegradedPermissionBanner` shared composable with Material3 errorContainer styling | Yes | Yes — lines 151–171; `errorContainer` / `onErrorContainer` colors; accepts `message: String` | Yes — imported in all three feature screens | VERIFIED |
| `feature/feature-notifications/src/main/java/com/usagecontrol/feature/notifications/NotificationsViewModel.kt` | `showPermissionBanner` StateFlow derived from PermissionChecker | Yes | Yes — `_showPermissionBanner`, `showPermissionBanner`, `recheckPermission()` present | Yes — collected in `NotificationsScreen` | VERIFIED |
| `feature/feature-screentime/src/main/java/com/usagecontrol/feature/screentime/ScreenTimeViewModel.kt` | `showPermissionBanner` StateFlow derived from PermissionChecker | Yes | Yes — `_showPermissionBanner`, `showPermissionBanner`, `recheckPermission()` present | Yes — collected in `ScreenTimeScreen` | VERIFIED |
| `feature/feature-lockcontrol/src/main/java/com/usagecontrol/feature/lockcontrol/LockControlViewModel.kt` | `showPermissionBanner` StateFlow derived from PermissionChecker | Yes | Yes — `_showPermissionBanner`, `showPermissionBanner`, `recheckPermission()` present | Yes — collected in `LockControlScreen` | VERIFIED |

---

### Key Link Verification

| From | To | Via | Status | Detail |
|------|----|-----|--------|--------|
| `PermissionWalkthroughViewModel.kt` | `core-common/PermissionChecker.kt` | `@Inject constructor` receives `PermissionChecker` | WIRED | Line 27: `private val permissionChecker: PermissionChecker` — confirmed in source |
| `PermissionWalkthroughViewModel.kt` | `core-common/onboarding/OnboardingRepository.kt` | `@Inject constructor` receives `OnboardingRepository` | WIRED | Line 28: `private val onboardingRepository: OnboardingRepository` — confirmed in source |
| `PermissionWalkthroughScreen.kt` | `PermissionWalkthroughViewModel.kt` | `hiltViewModel()` in composable | WIRED | Line 28 of screen: `viewModel: PermissionWalkthroughViewModel = hiltViewModel()` |
| `MainActivity.kt` | `PermissionWalkthroughScreen.kt` | composable route in onboarding nested graph | WIRED | Import line 28 + call at `composable("onboarding/start")` line 113–121; `OnboardingPlaceholderScreen` reference absent from `app/src/` |
| `NotificationsViewModel.kt` | `PermissionChecker.kt` | Added to `@Inject constructor` | WIRED | Line 23: `private val permissionChecker: PermissionChecker`; KSP generated factory confirmed in build artifacts |
| `NotificationsScreen.kt` | `CommonComponents.kt` | `DegradedPermissionBanner` composable call | WIRED | Import line 26 + conditional call at lines 81–83 |
| `ScreenTimeViewModel.kt` | `PermissionChecker.kt` | Added to `@Inject constructor` | WIRED | Line 22: `private val permissionChecker: PermissionChecker` |
| `ScreenTimeScreen.kt` | `CommonComponents.kt` | `DegradedPermissionBanner` composable call | WIRED | Import line 23 + conditional call at lines 70–72 |
| `LockControlViewModel.kt` | `PermissionChecker.kt` | Added to `@Inject constructor` | WIRED | Line 27: `private val permissionChecker: PermissionChecker` |
| `LockControlScreen.kt` | `CommonComponents.kt` | `DegradedPermissionBanner` composable call | WIRED | Import line 23 + conditional call at lines 65–67 |

---

### Requirements Coverage

| Requirement | Source Plan | Description | Status | Evidence |
|-------------|-------------|-------------|--------|----------|
| PERM-01 | 02-01 | User sees a custom priming screen per permission explaining why it's needed before being sent to Settings | SATISFIED | `PermissionStepContent` renders title, explanation, and hint text per `PermissionStep`; all three steps have distinct content strings |
| PERM-02 | 02-01 | App opens the correct system Settings screen for each permission | SATISFIED | `openNotificationListenerSettings()`, `openUsageAccessSettings()`, `openDndAccessSettings()` each use the correct system Settings action with API-level fallback |
| PERM-03 | 02-01 | App detects permission grant when user returns from Settings and shows green checkmark | SATISFIED | `ON_RESUME` `DisposableEffect` calls `recheckAll()`; `isGranted == true` renders `CheckCircle` icon and "Access granted" text |
| PERM-04 | 02-01 | User sees step progress (1/3, 2/3, 3/3) during permission walkthrough | SATISFIED | Step counter text `"${state.currentStep + 1} / ${PermissionStep.TOTAL_STEPS}"` and `LinearProgressIndicator` rendered unconditionally at top of walkthrough |
| PERM-05 | 02-02 | App works in degraded mode if user skips permissions, with clear messaging about what's missing | SATISFIED | `DegradedPermissionBanner` with feature-specific message shown conditionally in all three feature screens; banners reactive to ON_RESUME permission recheck |

No orphaned requirements: all five PERM requirements mapped to Phase 2 plans are accounted for.

---

### Anti-Patterns Found

No anti-patterns detected across all key files:

- No TODO/FIXME/PLACEHOLDER comments
- No stub return values (`return null`, `return {}`, `return []`)
- No console.log-only handlers
- No empty `onSubmit`/`onClick` handlers
- `markOnboardingComplete()` correctly absent from `MainActivity.kt` onComplete lambda (no double-write anti-pattern)
- `OnboardingPlaceholderScreen` correctly absent from `app/src/` imports and usages

---

### Human Verification Required

The following items require device or emulator testing and cannot be verified programmatically:

#### 1. Full Permission Walkthrough on Device

**Test:** Clear app data (`adb shell pm clear com.usagecontrol`), install, and walk through all three permission steps granting each one.

**Expected:** Each step shows priming content; "Grant Access" opens the correct Settings screen; returning from Settings triggers a green checkmark without any user tap; step counter increments from 1/3 to 3/3; after step 3 the main app is reached.

**Why human:** Settings intents require a real device and system Settings interaction. ON_RESUME grant detection and Hilt ViewModel DI cannot be exercised in a static code scan.

#### 2. Skip All Permissions and Verify Banners

**Test:** Clear app data, open app, tap "Skip for now" on all three steps, then check each main tab.

**Expected:** Notifications tab shows "Notification access is not granted..." banner; Screen Time tab shows "Usage access is not granted..." banner; Controls tab shows "Do Not Disturb access is not granted..." banner.

**Why human:** Requires visual inspection of composable rendering and tab navigation.

#### 3. Banner Disappears After Granting Permission

**Test:** With banners showing, go to device Settings, grant Notification Access, return to app.

**Expected:** Notification banner is gone without restarting the app.

**Why human:** ON_RESUME reactive behavior requires live lifecycle events that cannot be simulated statically.

#### 4. NLS requestRebind on Physical Device

**Test:** Grant Notification Access during walkthrough on a physical device. Verify notification capture begins working immediately (no app restart required).

**Expected:** `NotificationCaptureService` starts receiving notification events after `requestRebind()` is called.

**Why human:** AOSP bug #226411020 means this workaround only verifies on physical hardware. Emulators may not surface the failure.

---

### Gaps Summary

No gaps. All eleven observable truths are verified by code evidence. All eight required artifacts exist, are substantive, and are wired. All ten key links are confirmed. All five PERM requirements are satisfied. No stub, placeholder, or anti-patterns were found.

Four items require human verification on device, but these are runtime behaviors (Settings navigation, ON_RESUME lifecycle, NLS rebind) that the static code correctly supports. The automated code checks pass completely.

---

_Verified: 2026-02-28T21:00:00Z_
_Verifier: Claude (gsd-verifier)_
