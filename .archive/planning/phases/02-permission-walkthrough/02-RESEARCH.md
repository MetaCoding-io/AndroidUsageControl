# Phase 2: Permission Walkthrough - Research

**Researched:** 2026-02-28
**Domain:** Android special-permission settings intents, Compose lifecycle-aware permission re-check, multi-step onboarding state, degraded-mode messaging
**Confidence:** HIGH

---

<phase_requirements>
## Phase Requirements

| ID | Description | Research Support |
|----|-------------|-----------------|
| PERM-01 | User sees a custom priming screen per permission explaining why it's needed before being sent to Settings | One composable per permission step (`PermissionStepScreen`) wraps a priming card and a CTA button; the step is determined by `currentStep: Int` from the ViewModel. No system dialog is shown until user taps the CTA. |
| PERM-02 | App opens the correct system Settings screen for each permission (Notification Access, Usage Access, DND) | Three distinct `Settings.*` action constants: `ACTION_NOTIFICATION_LISTENER_SETTINGS` (API 22), `ACTION_USAGE_ACCESS_SETTINGS` (no package URI — global list only), `ACTION_NOTIFICATION_POLICY_ACCESS_SETTINGS` (API 23). All must be wrapped in `startActivity(Intent(...))` inside a try/catch. |
| PERM-03 | App detects permission grant when user returns from Settings and shows green checkmark | `DisposableEffect(lifecycleOwner)` observing `Lifecycle.Event.ON_RESUME` triggers `viewModel.recheckCurrentPermission()`. PermissionChecker (already in core-common) provides the check. Green checkmark shown via `isGranted` boolean in step UI state. |
| PERM-04 | User sees step progress (1/3, 2/3, 3/3) at all times during the permission walkthrough | `LinearProgressIndicator(progress = { currentStep.toFloat() / totalSteps })` plus `Text("${currentStep + 1}/$totalSteps")` in the screen header. Both driven by the same step index from ViewModel state. |
| PERM-05 | App works in degraded mode if user skips permissions, with clear messaging about what is missing | Skip stores nothing — `markOnboardingComplete()` is called and user goes to main app. Feature screens read `PermissionChecker` state and show per-feature banners. The NOT granted state for each permission is already queryable via the existing `PermissionChecker`. |
</phase_requirements>

---

## Summary

Phase 2 replaces `OnboardingPlaceholderScreen` with a real three-step guided permission walkthrough inside the `feature-onboarding` module. The work has four separable concerns: (1) a ViewModel that manages step index (0–2) and per-step grant state; (2) a priming screen composable that explains each permission before sending the user to Settings; (3) an `ON_RESUME` lifecycle hook that re-checks permission state when the user returns from Settings; and (4) degraded-mode banners in the main-app feature screens for users who skipped.

The architecture is constrained by Phase 1's existing structure: `OnboardingPlaceholderScreen` is the sole composable under `"onboarding/start"` in the nested nav graph. Phase 2 replaces that composable (or modifies its route) — `MainViewModel`, `MainActivity`, and the nav graph structure are not touched. The `PermissionChecker` utility (already built and injected in `core-common`) provides all three re-check methods and requires no changes. No new Gradle dependencies are needed.

The key technical complexity in this phase is the `ON_RESUME` re-check. Android provides no reactive callback when a special permission is granted in system Settings — the app is simply resumed. The standard Compose pattern is a `DisposableEffect` on `LocalLifecycleOwner.current` with a `LifecycleEventObserver` that calls back to the ViewModel on `Lifecycle.Event.ON_RESUME`. This is verified as the canonical approach from official Android documentation (side-effects guide) and is already safe for this project's minSdk 29 / lifecycle 2.7.0 stack.

**Primary recommendation:** Keep all Phase 2 logic (ViewModel, step composable, priming content) inside `feature/feature-onboarding`. Replace the `OnboardingPlaceholderScreen` composable with a `PermissionWalkthroughScreen` that owns step navigation internally. Use a single `PermissionWalkthroughViewModel` (Hilt-injected) with a `data class` UI state exposing `currentStep`, `permissionStates: List<Boolean>`, and `isComplete`. Use `DisposableEffect + LifecycleEventObserver(ON_RESUME)` for live re-check. Add per-feature degraded banners to main-app feature screens after onboarding is complete.

---

## Standard Stack

### Core

| Library | Version | Purpose | Why Standard |
|---------|---------|---------|--------------|
| `feature/feature-onboarding` module | existing | All Phase 2 composables and ViewModel | Already wired in nav graph; has Hilt, Compose, lifecycle deps |
| `core/core-common` `PermissionChecker` | existing | All three permission re-checks | Already provides `isNotificationAccessGranted()`, `isUsageAccessGranted()`, `isDndAccessGranted()` |
| `androidx.lifecycle:lifecycle-runtime-compose` | 2.7.0 (in project) | `LocalLifecycleOwner.current` for ON_RESUME observation | In `feature-onboarding/build.gradle.kts` already |
| `androidx.compose.material3:material3` | BOM 2024.02.00 (in project) | `LinearProgressIndicator`, `Icon(Icons.Default.CheckCircle)`, `Card`, `Button` | Already in feature-onboarding deps |

### Supporting

| Library | Version | Purpose | When to Use |
|---------|---------|---------|-------------|
| `android.provider.Settings` | platform (no dep) | Intent action constants for three Settings screens | Standard Android API; no library needed |
| `androidx.compose.material.icons` | BOM-tracked (in project) | `Icons.Default.CheckCircle` for granted state | Already in feature-onboarding deps |
| `androidx.lifecycle:lifecycle-runtime-ktx` | 2.7.0 (in project) | `LifecycleEventObserver` | Already in feature-onboarding deps |

### Alternatives Considered

| Instead of | Could Use | Tradeoff |
|------------|-----------|----------|
| `DisposableEffect + LifecycleEventObserver` | Accompanist Permissions library | Accompanist is for runtime permissions (uses `ActivityResultContracts`). The three special permissions are NOT runtime permissions — they cannot be requested via result contract. Must use manual Settings launch + resume re-check. |
| Custom animated checkmark vector | `Icons.Default.CheckCircle` | Animated vector requires XML asset and `AnimatedImageVector` setup. For Phase 2, a simple `Icon(Icons.Default.CheckCircle, tint = Color.Green)` revealed by a visibility transition is sufficient and zero-dependency. Add animation in a future polish pass. |
| Three separate composable routes | Single `PermissionWalkthroughScreen` with internal step state | Separate routes would require nav argument passing and back-stack management. Internal step state in one composable is simpler, keeps step counter always visible, and avoids back-press navigating backward through individual steps. |

**No new dependencies to install.** All required libraries are already in `feature-onboarding/build.gradle.kts`.

---

## Architecture Patterns

### Recommended Project Structure

```
feature/feature-onboarding/
└── src/main/java/com/usagecontrol/feature/onboarding/
    ├── OnboardingPlaceholderScreen.kt     # REPLACE (or delete entirely)
    ├── PermissionWalkthroughScreen.kt     # NEW — outer scaffold with step counter + pager
    ├── PermissionWalkthroughViewModel.kt  # NEW — step state, re-check, skip/complete
    └── PermissionStep.kt                  # NEW — sealed class/enum for the 3 steps

# Degraded-mode banners added to existing feature screens:
feature/feature-notifications/...NotificationsScreen.kt   # MODIFY — banner if notification access missing
feature/feature-screentime/...ScreenTimeScreen.kt         # MODIFY — banner if usage access missing
feature/feature-lockcontrol/...LockControlScreen.kt       # MODIFY — banner if DND access missing
```

The nav graph change in `MainActivity.kt` is minimal: the `composable("onboarding/start")` block is updated to call `PermissionWalkthroughScreen(...)` instead of `OnboardingPlaceholderScreen(...)`. All other nav graph code is untouched.

---

### Pattern 1: PermissionWalkthroughViewModel State

**What:** A single ViewModel manages the ordered three-step flow. Step index advances when user taps "Grant" or "Skip". Permission states are re-checked on every `ON_RESUME`. When all steps are complete (or skipped), the ViewModel emits an `isComplete` flag that the composable uses to call `onComplete()`.

**When to use:** When a multi-step flow has shared state (step counter, per-step grant status) that must survive config changes.

```kotlin
// Source: established Compose UDF pattern — verified against developer.android.com/develop/ui/compose/state
data class PermissionWalkthroughState(
    val currentStep: Int = 0,                        // 0 = Notification, 1 = Usage, 2 = DND
    val permissionStates: List<Boolean> = List(3) { false },
    val isComplete: Boolean = false,
)

enum class PermissionStep(val index: Int) {
    NotificationAccess(0),
    UsageAccess(1),
    DndAccess(2);
}

@HiltViewModel
class PermissionWalkthroughViewModel @Inject constructor(
    private val permissionChecker: PermissionChecker,
    private val onboardingRepository: OnboardingRepository,
) : ViewModel() {

    private val _state = MutableStateFlow(PermissionWalkthroughState())
    val state: StateFlow<PermissionWalkthroughState> = _state.asStateFlow()

    init {
        recheckAll()
    }

    /** Called from ON_RESUME observer — re-reads all three permissions */
    fun recheckAll() {
        _state.update { current ->
            current.copy(
                permissionStates = listOf(
                    permissionChecker.isNotificationAccessGranted(),
                    permissionChecker.isUsageAccessGranted(),
                    permissionChecker.isDndAccessGranted(),
                )
            )
        }
    }

    /** Advance to next step (after grant or skip) */
    fun advanceStep() {
        val nextStep = _state.value.currentStep + 1
        if (nextStep >= 3) {
            completeOnboarding()
        } else {
            _state.update { it.copy(currentStep = nextStep) }
        }
    }

    private fun completeOnboarding() {
        viewModelScope.launch {
            onboardingRepository.setOnboardingComplete(true)
            _state.update { it.copy(isComplete = true) }
        }
    }
}
```

---

### Pattern 2: ON_RESUME Permission Re-check

**What:** A `DisposableEffect` on `LocalLifecycleOwner.current` registers a `LifecycleEventObserver` that calls `viewModel.recheckAll()` on `Lifecycle.Event.ON_RESUME`. This fires every time the user returns from system Settings without any polling or delay.

**When to use:** Any time the app needs to detect side-effects that happen outside the app (permission grants in Settings, system preference changes).

```kotlin
// Source: developer.android.com/develop/ui/compose/side-effects#disposableeffect (verified 2026-02-28)
@Composable
fun PermissionWalkthroughScreen(
    onComplete: () -> Unit,
    viewModel: PermissionWalkthroughViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val lifecycleOwner = LocalLifecycleOwner.current

    // Trigger permission re-check every time the Activity resumes
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                viewModel.recheckAll()
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
        }
    }

    // Navigate to main app when complete
    LaunchedEffect(state.isComplete) {
        if (state.isComplete) onComplete()
    }

    PermissionWalkthroughContent(
        state = state,
        onOpenSettings = { step -> openPermissionSettings(step) },
        onSkip = { viewModel.advanceStep() },
    )
}
```

**IMPORTANT:** `recheckAll()` is called in the ViewModel `init` block AND on every `ON_RESUME`. This means if the user already granted permissions (v0.1 upgrader path that somehow slipped through), the walkthrough will immediately show green checkmarks and they can advance instantly.

---

### Pattern 3: Opening the Correct Settings Screen (PERM-02)

**What:** Three distinct Intent actions open the correct Settings screen for each permission. None of them accept a package URI — they open the global list where the user finds the app.

**Key verified facts:**
- `Settings.ACTION_NOTIFICATION_LISTENER_SETTINGS` — API 22. Opens "Notification access" list. **No package URI support.** A detail variant (`ACTION_NOTIFICATION_LISTENER_DETAIL_SETTINGS` + `EXTRA_NOTIFICATION_LISTENER_COMPONENT_NAME`) is available from API 30, which opens the toggle for a specific component — this is preferred on API 30+ because it takes the user directly to the app's toggle.
- `Settings.ACTION_USAGE_ACCESS_SETTINGS` — No API level annotation found but widely available. Input: Nothing. Opens global "Apps with usage access" list. **No package URI support documented.**
- `Settings.ACTION_NOTIFICATION_POLICY_ACCESS_SETTINGS` — API 23. Opens DND access list. Input: Nothing. **No package URI support.**

```kotlin
// Source: developer.android.com/reference/android/provider/Settings (verified 2026-02-28 via Microsoft Learn Xamarin mirror)
// All three are wrapped in try/catch as documentation warns "matching Activity may not exist"
fun Context.openNotificationListenerSettings(componentName: ComponentName) {
    try {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            // API 30+: go directly to this app's toggle
            startActivity(Intent(Settings.ACTION_NOTIFICATION_LISTENER_DETAIL_SETTINGS).apply {
                putExtra(
                    Settings.EXTRA_NOTIFICATION_LISTENER_COMPONENT_NAME,
                    componentName.flattenToString()
                )
            })
        } else {
            startActivity(Intent(Settings.ACTION_NOTIFICATION_LISTENER_SETTINGS))
        }
    } catch (_: Exception) {
        // Fallback: open generic app settings
        startActivity(Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
            data = Uri.fromParts("package", packageName, null)
        })
    }
}

fun Context.openUsageAccessSettings() {
    try {
        startActivity(Intent(Settings.ACTION_USAGE_ACCESS_SETTINGS))
    } catch (_: Exception) {
        startActivity(Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
            data = Uri.fromParts("package", packageName, null)
        })
    }
}

fun Context.openDndAccessSettings() {
    try {
        startActivity(Intent(Settings.ACTION_NOTIFICATION_POLICY_ACCESS_SETTINGS))
    } catch (_: Exception) {
        startActivity(Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
            data = Uri.fromParts("package", packageName, null)
        })
    }
}
```

These extension functions should live in the `feature-onboarding` module or `core-common` (as package-level functions, not in `PermissionChecker` which is context-bound to ApplicationContext, not Activity).

**Note on ACTION_NOTIFICATION_LISTENER_DETAIL_SETTINGS:** This API 30+ variant is the best UX since it goes directly to the app's own toggle, not the full list. The project's minSdk is 29, so this requires a `Build.VERSION.SDK_INT >= Build.VERSION_CODES.R` check. This is worth implementing — 99%+ of active Android devices in 2025 run API 30+.

---

### Pattern 4: Step Counter and Progress (PERM-04)

**What:** `LinearProgressIndicator` with `progress = { (currentStep + 1).toFloat() / 3f }` and a text label `"${currentStep + 1} / 3"`. Both derived from the same ViewModel state field.

```kotlin
// Source: developer.android.com/develop/ui/compose/components/progress (verified 2026-02-28)
// LinearProgressIndicator with lambda-based progress parameter (Compose Material3)
Column {
    Row(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        Text(
            text = "Set up permissions",
            style = MaterialTheme.typography.titleSmall,
        )
        Text(
            text = "${state.currentStep + 1} / 3",
            style = MaterialTheme.typography.titleSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
    LinearProgressIndicator(
        progress = { (state.currentStep + 1).toFloat() / 3f },
        modifier = Modifier.fillMaxWidth(),
    )
}
```

The `progress` parameter takes a `() -> Float` lambda in Material3 Compose (BOM 2024.02.00 / material3 1.2.x). Do not pass `Float` directly — it is a lambda in the current API.

---

### Pattern 5: Granted State Checkmark (PERM-03)

**What:** Each step shows either an "Open Settings" button (not yet granted) or a green `CheckCircle` icon (granted). The button for a granted step can still be tapped to return to Settings, but the primary CTA becomes "Continue to next step."

```kotlin
// Simple, no animation required for Phase 2
if (state.permissionStates[state.currentStep]) {
    // Permission granted -- show checkmark
    Icon(
        imageVector = Icons.Default.CheckCircle,
        contentDescription = "Permission granted",
        tint = MaterialTheme.colorScheme.primary,  // or a green custom color
        modifier = Modifier.size(48.dp),
    )
    Button(onClick = { viewModel.advanceStep() }) {
        Text("Continue")
    }
} else {
    // Not yet granted -- show priming content and CTA to open Settings
    Button(onClick = { openSettingsForCurrentStep() }) {
        Text("Grant Access")
    }
    TextButton(onClick = { viewModel.advanceStep() }) {
        Text("Skip for now")
    }
}
```

---

### Pattern 6: Degraded Mode Messaging (PERM-05)

**What:** After onboarding, the `PermissionChecker` is already injected into each feature's ViewModel. Each feature screen checks its relevant permission and shows an in-screen warning banner if missing.

**Architectural approach:** Each feature ViewModel reads its required permission via `PermissionChecker` on every `onResume` (same `DisposableEffect` pattern). A `showPermissionBanner: Boolean` in the UI state drives a dismissible `Card` at the top of the screen.

```kotlin
// In NotificationsViewModel (feature-notifications)
// PermissionChecker is already @Singleton — add to constructor
private val _showNotificationAccessBanner = MutableStateFlow(
    !permissionChecker.isNotificationAccessGranted()
)
val showNotificationAccessBanner: StateFlow<Boolean> = _showNotificationAccessBanner

fun recheckNotificationAccess() {
    _showNotificationAccessBanner.value = !permissionChecker.isNotificationAccessGranted()
}
```

```kotlin
// In NotificationsScreen composable
if (showBanner) {
    DegradedPermissionBanner(
        message = "Notification access is not granted. Notification rules won't fire.",
        onDismiss = { /* optional local dismiss */ },
    )
}
```

The `DegradedPermissionBanner` should be a shared composable in `core-ui` if it will appear in multiple feature screens (Notifications, Screen Time, Controls). Otherwise inline it per-screen.

---

### Anti-Patterns to Avoid

- **Anti-pattern — Accompanist Permissions for special permissions:** `rememberPermissionState` only works with runtime permissions (manifest-declared, requested via `ActivityResultContracts.RequestPermission`). The three special permissions (Notification Access, Usage Access, DND) are NOT runtime permissions. Accompanist will not work for these.
- **Anti-pattern — Polling for permission in a coroutine loop:** Using `while(true) { delay(500); recheckPermission() }` instead of `ON_RESUME` observer. Polling drains battery and fires while the Settings screen is still open (misleading timing). Use `DisposableEffect + LifecycleEventObserver`.
- **Anti-pattern — LaunchedEffect for ON_RESUME:** `LaunchedEffect` is keyed to a value changing. Using `LaunchedEffect(Unit)` runs once on composition, not on every resume. Must use `DisposableEffect` with a lifecycle observer for ongoing event subscription.
- **Anti-pattern — Hardcoded step strings in ViewModel:** User-facing strings ("Notification Access", "This lets the app read your notifications") belong in composable resources or the composable itself, not in the ViewModel. ViewModel holds `currentStep: Int`; the composable maps the step index to display content.
- **Anti-pattern — Requiring all permissions before advancing:** Success criteria explicitly require skip to be possible at every step (PERM-05). Never block the user from advancing. The "Skip" path and the "Grant" path must be equally easy to execute.
- **Anti-pattern — startActivity without try/catch for Settings intents:** Official docs explicitly state "a matching Activity may not exist." OEM-customized Android builds (Samsung, Xiaomi) sometimes cannot resolve `ACTION_USAGE_ACCESS_SETTINGS`. Always wrap in try/catch with a fallback to `ACTION_APPLICATION_DETAILS_SETTINGS`.
- **Anti-pattern — Using Context from ViewModel to launch Settings:** `startActivity` requires an `Activity` context. Pass a lambda `() -> Unit` from the composable to the ViewModel (or call `startActivity` from the composable's event handler), never from inside the ViewModel.

---

## Don't Hand-Roll

| Problem | Don't Build | Use Instead | Why |
|---------|-------------|-------------|-----|
| Detecting permission grant after Settings return | Polling loop or `onListenerConnected` callback | `DisposableEffect + LifecycleEventObserver(ON_RESUME)` | Polling wastes CPU; `onListenerConnected` has a documented timing lag (Google issue #257072930); ON_RESUME is the correct hook |
| Step progress bar | Custom drawn canvas arc | `LinearProgressIndicator` from Material3 | Already in project; correct theming; accessibility support built in |
| Green checkmark after grant | Custom animated view or Lottie | `Icon(Icons.Default.CheckCircle)` + conditional visibility | Zero additional dependencies; M3 consistent; animation can be added later if needed |
| Settings intent routing | Reflection or hardcoded action strings | `Settings.ACTION_NOTIFICATION_LISTENER_SETTINGS` etc. from `android.provider.Settings` | Platform constants are stable; reflection is fragile; string typos are silent failures |
| Degraded-mode state | Separate "permission denied" DataStore flag | Query `PermissionChecker` directly on each resume | PermissionChecker already exists; a separate flag would diverge from actual system state |

**Key insight:** The three special permissions have no unified grant API. `startActivity(Intent(Settings.ACTION_*))` is the only way to initiate the grant, and `ON_RESUME` + `PermissionChecker` is the only way to detect the result. There is no callback from the system.

---

## Common Pitfalls

### Pitfall 1: ACTION_NOTIFICATION_LISTENER_DETAIL_SETTINGS Missing Extra
**What goes wrong:** Intent fired with `ACTION_NOTIFICATION_LISTENER_DETAIL_SETTINGS` but missing `EXTRA_NOTIFICATION_LISTENER_COMPONENT_NAME` extra — opens an empty or crash Settings page.
**Why it happens:** The detail-settings action requires the component name as a string extra; without it the Intent resolves but shows nothing useful.
**How to avoid:** Always provide the extra: `putExtra(Settings.EXTRA_NOTIFICATION_LISTENER_COMPONENT_NAME, ComponentName(packageName, NotificationCaptureService::class.java).flattenToString())`.
**Warning signs:** Settings opens a blank screen or immediately crashes on API 30+ devices.

### Pitfall 2: ON_RESUME Fires Before Permission State Is Updated
**What goes wrong:** User grants Notification Access in Settings, presses back — app resumes and immediately calls `getEnabledListenerPackages()`, which still returns the old (non-granted) state for a brief interval.
**Why it happens:** Documented in Google issue tracker (noted in Phase 1 research) — `getEnabledListenerPackages` has a timing lag after the NLS binding changes.
**How to avoid:** In practice, the lag is short (milliseconds) and `ON_RESUME` fires after the system finishes the Settings transition, so in most cases the check will be correct. If the checkmark does not appear after grant, the next `ON_RESUME` (e.g., from a subsequent app switch) will catch it. For Phase 2 this is acceptable — do not add a `delay()` workaround as it creates a worse UX than the rare edge case.
**Warning signs:** Checkmark doesn't appear on first return from Settings but does appear after going to home and back to the app.

### Pitfall 3: ACTION_USAGE_ACCESS_SETTINGS with Package URI
**What goes wrong:** Code adds `intent.data = Uri.fromParts("package", packageName, null)` to `ACTION_USAGE_ACCESS_SETTINGS` expecting to deep-link to the specific app, but the Settings Activity ignores or rejects the URI and shows the generic list (or crashes on some OEMs).
**Why it happens:** Official documentation states Input: Nothing for `ACTION_USAGE_ACCESS_SETTINGS`. Unlike `ACTION_APPLICATION_DETAILS_SETTINGS`, it does not define a package URI extra.
**How to avoid:** Do not add a URI to this intent. Accept that the user must find the app in the list. Consider adding instructional text to the priming screen: "Find 'Usage Control' in the list and enable it."
**Warning signs:** On some devices the settings screen closes immediately or shows an error when a URI is provided.

### Pitfall 4: startActivity Called from ViewModel
**What goes wrong:** `viewModel.openSettings()` directly calls `context.startActivity(...)` using an `@ApplicationContext` context injected into the ViewModel — this fails with a `FLAG_ACTIVITY_NEW_TASK` requirement when called from a non-Activity context, and even with that flag it may not work correctly in all cases.
**Why it happens:** ViewModel is lifecycle-aware but should not hold UI references or perform UI actions. `startActivity` is a UI action.
**How to avoid:** Expose a `settingsLaunchIntent` or trigger a `StateFlow<LaunchTarget>` one-shot event. The composable observes the event and calls `LocalContext.current.startActivity(...)` in a `LaunchedEffect`.
**Warning signs:** `android.util.AndroidRuntimeException: Calling startActivity() from outside of an Activity context requires the FLAG_ACTIVITY_NEW_TASK flag`.

### Pitfall 5: requestRebind Not Called After Notification Access Grant
**What goes wrong:** After the user grants Notification Access, `NotificationCaptureService` does not receive notifications until the device is rebooted or the app is relaunched.
**Why it happens:** On some Android versions/OEMs, the system does not automatically bind the NLS after the user grants access in Settings. The AOSP bug tracker (STATE.md Blockers/Concerns: `AOSP #226411020`) documents this.
**How to avoid:** After `ON_RESUME` detects `isNotificationAccessGranted() == true` (newly granted), call `NotificationListenerService.requestRebind(ComponentName(context, NotificationCaptureService::class.java))`. This is safe to call from a non-service context as it is a static method. It should only be called once (after the state transitions from false to true).
**Warning signs:** App shows green checkmark for Notification Access but notifications are not captured on the same session; only captured after relaunch.

```kotlin
// Rebind pattern — call from ViewModel after detecting grant transition
fun recheckAll() {
    val wasNotificationGranted = _state.value.permissionStates[0]
    val isNowGranted = permissionChecker.isNotificationAccessGranted()
    if (!wasNotificationGranted && isNowGranted) {
        // Newly granted — trigger NLS rebind to avoid requiring relaunch
        NotificationListenerService.requestRebind(
            ComponentName(context, NotificationCaptureService::class.java)
        )
    }
    _state.update { current ->
        current.copy(
            permissionStates = listOf(
                isNowGranted,
                permissionChecker.isUsageAccessGranted(),
                permissionChecker.isDndAccessGranted(),
            )
        )
    }
}
```

**Note:** `requestRebind` requires `@ApplicationContext` which is already available to the ViewModel via Hilt injection. This is not "UI action from ViewModel" — it's a system API call, acceptable in ViewModel.

### Pitfall 6: Back Press During Walkthrough Returns to Step
**What goes wrong:** User taps Android back button on Step 2, expects to go back to Step 1 (or exit), but instead gets unexpected behavior.
**Why it happens:** Navigation Compose back press handling conflicts with internal step state if not configured explicitly.
**How to avoid:** Use a single composable route (`"onboarding/start"`) with internal step state — back press exits the composable (goes to the nav back stack entry below it, which is handled by `popUpTo` behavior from Phase 1). Do NOT use separate composable routes per step, as this requires complex back-stack management. If per-step back navigation is desired, use `BackHandler` in the composable with `if (currentStep > 0) viewModel.goBack() else { /* let default back handle it */ }`.

---

## Code Examples

### Full PermissionWalkthroughScreen skeleton

```kotlin
// feature/feature-onboarding/.../PermissionWalkthroughScreen.kt
@Composable
fun PermissionWalkthroughScreen(
    onComplete: () -> Unit,
    viewModel: PermissionWalkthroughViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val lifecycleOwner = LocalLifecycleOwner.current
    val context = LocalContext.current

    // Source: developer.android.com/develop/ui/compose/side-effects#disposableeffect
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) viewModel.recheckAll()
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    LaunchedEffect(state.isComplete) {
        if (state.isComplete) onComplete()
    }

    Scaffold { innerPadding ->
        Column(
            modifier = Modifier.fillMaxSize().padding(innerPadding),
        ) {
            // Step counter header (PERM-04)
            Row(
                modifier = Modifier.fillMaxWidth().padding(16.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                Text("Set up permissions", style = MaterialTheme.typography.titleSmall)
                Text("${state.currentStep + 1} / 3", style = MaterialTheme.typography.titleSmall)
            }
            LinearProgressIndicator(
                progress = { (state.currentStep + 1).toFloat() / 3f },
                modifier = Modifier.fillMaxWidth(),
            )

            // Step content (PERM-01, PERM-03)
            PermissionStepContent(
                step = state.currentStep,
                isGranted = state.permissionStates[state.currentStep],
                onOpenSettings = {
                    when (state.currentStep) {
                        0 -> context.openNotificationListenerSettings(
                            ComponentName(context, NotificationCaptureService::class.java)
                        )
                        1 -> context.openUsageAccessSettings()
                        2 -> context.openDndAccessSettings()
                    }
                },
                onContinue = { viewModel.advanceStep() },
                onSkip = { viewModel.advanceStep() },
            )
        }
    }
}
```

### Open Notification Listener Settings (API 30+ preferred)

```kotlin
// Source: developer.android.com/reference/android/provider/Settings#ACTION_NOTIFICATION_LISTENER_DETAIL_SETTINGS
// and Settings.ActionNotificationListenerSettings (ApiSince=22) from Microsoft Learn (2026-02-28)
fun Context.openNotificationListenerSettings(componentName: ComponentName) {
    try {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            startActivity(Intent(Settings.ACTION_NOTIFICATION_LISTENER_DETAIL_SETTINGS).apply {
                putExtra(
                    Settings.EXTRA_NOTIFICATION_LISTENER_COMPONENT_NAME,
                    componentName.flattenToString(),
                )
            })
        } else {
            startActivity(Intent(Settings.ACTION_NOTIFICATION_LISTENER_SETTINGS))
        }
    } catch (_: Exception) {
        startActivity(Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
            data = Uri.fromParts("package", packageName, null)
        })
    }
}
```

### NavGraph change in MainActivity (minimal)

```kotlin
// In MainActivity.kt — only this composable invocation changes
navigation(startDestination = "onboarding/start", route = "onboarding") {
    composable("onboarding/start") {
        // Phase 2: replace OnboardingPlaceholderScreen with real walkthrough
        PermissionWalkthroughScreen(
            onComplete = {
                viewModel.markOnboardingComplete()
                navController.navigate("main") {
                    popUpTo("onboarding") { inclusive = true }
                }
            }
        )
    }
}
```

Note: `viewModel.markOnboardingComplete()` in `MainViewModel` is still needed here because `PermissionWalkthroughViewModel.completeOnboarding()` already calls `onboardingRepository.setOnboardingComplete(true)` internally. To avoid double-write, either: (a) remove the `viewModel.markOnboardingComplete()` call from the `onComplete` lambda in `MainActivity` and rely on `PermissionWalkthroughViewModel` to do it, or (b) keep the double-write (idempotent DataStore operation — no harm). Option (a) is cleaner.

---

## State of the Art

| Old Approach | Current Approach | When Changed | Impact |
|--------------|------------------|--------------|--------|
| Polling for permission grant in onResume | `DisposableEffect + LifecycleEventObserver(ON_RESUME)` | Compose stable ~2022 | No polling; correct lifecycle-scoped observer; automatic cleanup |
| Accompanist Permissions for ALL permissions | Accompanist only for runtime permissions; manual Settings launch for special permissions | Special permissions were always excluded from Accompanist scope | Requires explicit understanding of which Android permission type you're dealing with |
| ActivityResultContracts for special permissions | `startActivity(Intent(Settings.ACTION_*))` | Never changed — special permissions never supported result contracts | Cannot use `rememberLauncherForActivityResult` for these three permissions |
| `LinearProgressIndicator(progress: Float)` | `LinearProgressIndicator(progress: () -> Float)` | Material3 BOM 2023.10.01+ / material3 1.2.x | Lambda avoids recomposition on every progress update; must use lambda form |

**Deprecated/outdated:**
- `ACTION_NOTIFICATION_LISTENER_SETTINGS` (on API 30+ devices): Still works but opens the full list. The `ACTION_NOTIFICATION_LISTENER_DETAIL_SETTINGS` variant added in API 30 is preferred as it targets the app directly — significantly better UX.
- `@Suppress("DEPRECATION") appOps.checkOpNoThrow(...)`: Already handled in existing `PermissionChecker` with the `Build.VERSION.SDK_INT` branch. Phase 2 does not touch `PermissionChecker`.

---

## Open Questions

1. **Should the walkthrough remember which step the user left off on across app kills?**
   - What we know: If the app is killed while the user is on Step 2 (Usage Access Settings), relaunching will restart the walkthrough at Step 0 since ViewModel state is in-memory only.
   - What's unclear: Whether this creates user confusion worth solving in Phase 2.
   - Recommendation: Do not persist step index in Phase 2. If the user has already granted permission 0 and 1, the ViewModel `init` calls `recheckAll()` which sets `permissionStates`, and the UI should auto-advance past already-granted steps on first composition. Implement auto-advance in the ViewModel's `init` to skip steps where `permissionStates[step] == true`.

2. **Should "Skip for now" on a step that is already granted still work?**
   - What we know: If permission is granted, the primary CTA changes to "Continue". Skip is less relevant but could still be shown.
   - Recommendation: Show only "Continue" (no "Skip") when a step is already granted. The user still moves forward at their own pace.

3. **Degraded banners: in each feature ViewModel, or in a shared UI component?**
   - What we know: Three different features need banners for three different permissions. The banner content (which permission, what feature is degraded) differs per screen.
   - Recommendation: Create a shared `DegradedPermissionBanner` composable in `core-ui` (already in project as `CommonComponents.kt`). Each feature ViewModel exposes a `showPermissionBanner: Boolean`. The composable in `core-ui` accepts `message: String` and `onOpenSettings: () -> Unit`. This avoids UI code duplication while keeping feature-specific messaging in the feature module.

4. **Should completing the walkthrough with all permissions granted trigger `requestRebind`?**
   - What we know: STATE.md Blockers/Concerns calls out the NLS rebind bug. `requestRebind()` should be called after Notification Access is granted.
   - Recommendation: Call `requestRebind()` inside `recheckAll()` when a state transition from not-granted to granted is detected for the Notification Access step (see Pitfall 5 code example above). This is the correct place — it's a system call, not a UI action, so it belongs in the ViewModel.

---

## Validation Architecture

> Skipped — `workflow.nyquist_validation` is not present in `.planning/config.json` (only `research`, `plan_check`, `verifier` are set). The standard `verifier` workflow covers manual device verification. No automated test framework has been established for this project.

---

## Sources

### Primary (HIGH confidence)
- `developer.android.com/develop/ui/compose/side-effects#disposableeffect` — `DisposableEffect` with `LifecycleEventObserver` for ON_RESUME, complete code example including `rememberUpdatedState` pattern (fetched 2026-02-28)
- `learn.microsoft.com/.../settings.actionnotificationlistenersettings` — `ACTION_NOTIFICATION_LISTENER_SETTINGS` ApiSince=22, Input: Nothing (fetched 2026-02-28; mirrors Java doc from AOSP)
- `learn.microsoft.com/.../settings.actionusageaccesssettings` — `ACTION_USAGE_ACCESS_SETTINGS` Input: Nothing, no package URI (fetched 2026-02-28)
- `learn.microsoft.com/.../settings.actionnotificationpolicyaccesssettings` — `ACTION_NOTIFICATION_POLICY_ACCESS_SETTINGS` ApiSince=23, Input: Nothing (fetched 2026-02-28)
- `developer.android.com/develop/ui/compose/components/progress` — `LinearProgressIndicator(progress: () -> Float)` lambda form, Material3 code example (fetched 2026-02-28)
- Codebase audit — `PermissionChecker.kt` already has all three check methods; `feature-onboarding/build.gradle.kts` already has all needed dependencies; `MainActivity.kt` nav graph structure confirmed

### Secondary (MEDIUM confidence)
- WebSearch: `ACTION_NOTIFICATION_LISTENER_DETAIL_SETTINGS` with `EXTRA_NOTIFICATION_LISTENER_COMPONENT_NAME` — API 30+ variant for direct app targeting; confirmed by Microsoft Learn search result and multiple sources (2026-02-28)
- WebSearch: `requestRebind()` is static, safe to call from non-service context, "fails for listeners that have not been granted permission" — multiple sources agree; complements STATE.md blocker note about AOSP #226411020 (2026-02-28)
- `developer.android.com/develop/ui/compose/animation/vectors` — `AnimatedImageVector.animatedVectorResource` + `rememberAnimatedVectorPainter` API (fetched 2026-02-28) — noted as an option but not required for Phase 2

### Tertiary (LOW confidence)
- WebSearch: Specific OEM behavior with `ACTION_USAGE_ACCESS_SETTINGS` + package URI crashing — reported in community forums, not in official docs. Flagged as LOW but directionally consistent with the official "Input: Nothing" documentation.

---

## Metadata

**Confidence breakdown:**
- Standard stack: HIGH — no new dependencies; all libraries already in project and verified against build.gradle.kts
- Architecture (ViewModel state, ON_RESUME pattern): HIGH — pattern directly from official Compose side-effects documentation
- Settings Intent actions: HIGH for action constants (verified via official references); MEDIUM for package URI behavior (official docs say Input: Nothing; OEM variance behavior is LOW)
- Pitfalls: HIGH for documented pitfalls (try/catch, startActivity from ViewModel, Accompanist limitation); MEDIUM for NLS rebind timing (flagged in STATE.md, confirmed by multiple sources but exact behavior is OEM-dependent)

**Research date:** 2026-02-28
**Valid until:** 2026-04-28 (stable libraries; re-verify if Compose BOM or navigation version is upgraded)
