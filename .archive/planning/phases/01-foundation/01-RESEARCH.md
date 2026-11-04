# Phase 1: Foundation - Research

**Researched:** 2026-02-28
**Domain:** Android Jetpack Navigation Compose routing, Preferences DataStore, special-permission checking
**Confidence:** HIGH

---

<phase_requirements>
## Phase Requirements

| ID | Description | Research Support |
|----|-------------|-----------------|
| ONBD-01 | User sees onboarding flow only on first launch (DataStore flag persists completion) | DataStore Preferences with `booleanPreferencesKey("onboarding_complete")` — reads via Flow, writes on completion; absent key defaults to `false` (new user) |
| ONBD-02 | App routes to onboarding or main app based on completion flag before NavHost renders | MainViewModel reads DataStore flag in `init`, exposes `startDestination: StateFlow<String>`; NavHost uses `.collectAsState()` — graph defers rendering until state is ready |
| ONBD-03 | User can skip onboarding at any step and go directly to main app | Skip handler calls `markOnboardingComplete()` then navigates with `popUpTo("onboarding") { inclusive = true }` — identical code path to normal completion |
| ONBD-04 | Existing v0.1 users who already have all three permissions granted auto-skip onboarding on update | On first launch after upgrade: DataStore key absent → check all three permissions → if all granted, write flag immediately and route to main app without showing any onboarding screen |
</phase_requirements>

---

## Summary

Phase 1 installs the routing spine for the entire v0.2 milestone. The work has three separable concerns: (1) a persistent boolean flag in Preferences DataStore that records whether onboarding is complete; (2) a splash/loading pattern in Navigation Compose that reads that flag asynchronously before the NavHost decides its start destination; and (3) a `PermissionChecker` utility in `core-common` that knows how to interrogate all three special-permission APIs — none of which can be checked with the standard `checkSelfPermission`.

The critical architectural decision recorded in STATE.md — the **splash routing pattern** — means the NavHost's `startDestination` is *not* set to the onboarding graph by default. Instead, a neutral "deciding" destination (a transparent splash composable or a null-content stub) is the start destination while the ViewModel resolves state. Once the ViewModel emits the real destination, a `LaunchedEffect` fires a `navigate(target) { popUpTo(splash) { inclusive = true } }`, leaving a clean back stack with exactly one destination. This makes back-press-from-main-app work correctly and prevents the bottom navigation bar from appearing on onboarding screens.

The v0.1 codebase has **no DataStore at all** — only SharedPreferences at `usage_control_settings` (keys: `retention_days`, `auto_cleanup_enabled`). This means there is no pre-existing `onboarding_complete` key anywhere; the DataStore file is net-new and the upgrade concern from STATE.md is confirmed as safe.

**Primary recommendation:** Use `datastore-preferences:1.1.1` (stable, compatible with Compose BOM 2024.02.00/Kotlin 1.9.22), provide it as a `@Singleton` via a `DataStoreModule` in `core-common`, expose it through a `OnboardingRepository`, and read flag state in `MainViewModel` before NavHost renders.

---

## Standard Stack

### Core

| Library | Version | Purpose | Why Standard |
|---------|---------|---------|--------------|
| `androidx.datastore:datastore-preferences` | 1.1.1 | Persist `onboarding_complete` boolean | Official replacement for SharedPreferences; coroutine/Flow-native; atomic writes; used across all Android apps with Compose |
| `androidx.navigation:navigation-compose` | 2.7.7 (already in project) | NavHost, composable(), popUpTo, back-stack management | Already in project; provides all routing primitives needed |
| `dagger.hilt` | 2.50 (already in project) | DI for DataStore singleton, ViewModel injection | Already wired throughout project |

### Supporting

| Library | Version | Purpose | When to Use |
|---------|---------|---------|-------------|
| `kotlinx.coroutines` | 1.7.3 (already in project) | `viewModelScope.launch`, `Flow.first()`, `stateIn` | Reading DataStore in ViewModel — already in project |
| `androidx.lifecycle:lifecycle-runtime-compose` | 2.7.0 (already in project) | `collectAsStateWithLifecycle()` | Safer than `collectAsState()` — respects lifecycle, prevents emissions when UI is backgrounded |

### Alternatives Considered

| Instead of | Could Use | Tradeoff |
|------------|-----------|----------|
| Preferences DataStore | SharedPreferences | SharedPreferences is synchronous, not coroutine-native. Project already uses it in SettingsViewModel; adding a second SP file would be fine but DataStore is the correct greenfield choice |
| Preferences DataStore | Proto DataStore | Proto requires protobuf schema; overkill for a single boolean flag |
| Dynamic `startDestination` ViewModel pattern | NavController with `navigate` in `LaunchedEffect` at the top level | Both work; dynamic `startDestination` is cleaner when the decision must be made before any destination is shown; `LaunchedEffect` navigation is more appropriate when a screen is shown first |

**Installation** (add to `core-common/build.gradle.kts`):
```kotlin
implementation("androidx.datastore:datastore-preferences:1.1.1")
```

Add to `gradle/libs.versions.toml`:
```toml
[versions]
datastore = "1.1.1"

[libraries]
datastore-preferences = { module = "androidx.datastore:datastore-preferences", version.ref = "datastore" }
```

---

## Architecture Patterns

### Recommended Project Structure

Changes are confined to three areas:

```
core/core-common/
└── src/main/java/com/usagecontrol/core/common/
    ├── AppNameResolver.kt          # existing
    ├── TimeUtils.kt                # existing
    ├── PermissionChecker.kt        # NEW — three special-permission check methods
    └── onboarding/
        ├── OnboardingDataStore.kt  # NEW — DataStore delegate + key definitions
        └── OnboardingRepository.kt # NEW — Flow<Boolean> + suspend write

core/core-database/ (no changes — DataStore is not Room)

app/
└── src/main/java/com/usagecontrol/
    ├── di/
    │   └── DataStoreModule.kt      # NEW — @Singleton DataStore<Preferences> provision
    ├── navigation/
    │   ├── AppScreen.kt            # existing — no changes
    │   └── AppNavigation.kt        # existing — no changes (bottom bar already route-aware)
    ├── MainActivity.kt             # MODIFY — remove startMonitoringServices call until permissions granted (or keep with guard)
    └── MainViewModel.kt            # NEW — reads OnboardingRepository, exposes startDestination StateFlow
    └── UsageControlAppContent.kt   # MODIFY — drive NavHost startDestination from ViewModel

feature/feature-onboarding/         # NEW module (or inline in app module for Phase 1)
└── src/main/java/.../onboarding/
    └── OnboardingScreen.kt         # NEW — placeholder onboarding scaffold (Phase 2/3 fill content)
```

**Note:** Phase 1 only needs the routing skeleton — onboarding content is a Phase 2/3 concern. The onboarding destination in Phase 1 can be a bare `OnboardingPlaceholderScreen` with a "Skip" button, enough to validate all four success criteria.

---

### Pattern 1: Splash Routing via Dynamic startDestination

**What:** A `MainViewModel` reads the DataStore flag in its `init` block. The NavHost's `startDestination` is bound to a `StateFlow<String?>` — `null` means "not yet decided". While null, the UI renders a transparent/blank composable (the splash stub). Once the ViewModel emits a real destination, a `LaunchedEffect` fires the navigation and clears the stub.

**When to use:** When the start destination depends on async I/O (DataStore read) that must complete before any content screen appears.

**Example:**
```kotlin
// Source: STATE.md architectural decision + verified pattern from monstar-lab.com
// MainViewModel.kt (app module)
@HiltViewModel
class MainViewModel @Inject constructor(
    private val onboardingRepository: OnboardingRepository,
    private val permissionChecker: PermissionChecker,
) : ViewModel() {

    private val _startDestination = MutableStateFlow<String?>(null)
    val startDestination: StateFlow<String?> = _startDestination

    init {
        viewModelScope.launch {
            val onboardingComplete = onboardingRepository.isOnboardingComplete.first()
            if (onboardingComplete) {
                _startDestination.value = "main"
            } else {
                // ONBD-04: v0.1 upgrader auto-skip
                if (permissionChecker.allPermissionsGranted()) {
                    onboardingRepository.setOnboardingComplete(true)
                    _startDestination.value = "main"
                } else {
                    _startDestination.value = "onboarding"
                }
            }
        }
    }
}
```

```kotlin
// UsageControlAppContent.kt (Composable in MainActivity)
@Composable
fun UsageControlAppContent(viewModel: MainViewModel = hiltViewModel()) {
    val startDestination by viewModel.startDestination.collectAsStateWithLifecycle()
    val navController = rememberNavController()
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry?.destination?.route

    // Only show bottom bar on main app routes, never on onboarding or splash
    val showBottomBar = currentRoute in AppScreen.entries.map { it.route }

    if (startDestination == null) {
        // Blank while deciding — prevents flicker of wrong destination
        Box(modifier = Modifier.fillMaxSize())
        return
    }

    Scaffold(
        bottomBar = { if (showBottomBar) { /* existing NavigationBar */ } }
    ) { innerPadding ->
        NavHost(
            navController = navController,
            startDestination = startDestination!!,
            modifier = Modifier.fillMaxSize().padding(innerPadding),
        ) {
            // Onboarding graph (Phase 1: placeholder, Phase 2/3: real content)
            navigation(startDestination = "onboarding/start", route = "onboarding") {
                composable("onboarding/start") {
                    OnboardingPlaceholderScreen(
                        onComplete = {
                            viewModel.markOnboardingComplete()  // writes DataStore
                            navController.navigate("main") {
                                popUpTo("onboarding") { inclusive = true }
                            }
                        }
                    )
                }
            }
            // Main app graph
            navigation(startDestination = AppScreen.Notifications.route, route = "main") {
                composable(AppScreen.Notifications.route) { /* existing */ }
                composable(AppScreen.ScreenTime.route)   { /* existing */ }
                composable(AppScreen.Controls.route)     { /* existing */ }
                composable(AppScreen.Insights.route)     { /* existing */ }
                composable(AppScreen.Settings.route)     { /* existing */ }
                // rule screens unchanged
            }
        }
    }
}
```

**Why this prevents back-navigation into onboarding:** When `startDestination = "main"` (returning user), onboarding is never in the back stack. When `startDestination = "onboarding"` and user completes/skips, `popUpTo("onboarding") { inclusive = true }` removes the entire onboarding graph. Either way, pressing back from the Notifications tab exits the app — never navigates backward into onboarding.

---

### Pattern 2: OnboardingRepository with DataStore

**What:** Thin repository wrapping a Preferences DataStore singleton. Exposes a `Flow<Boolean>` and a `suspend` write method. Provided via Hilt.

**Example:**
```kotlin
// Source: developer.android.com/topic/libraries/architecture/datastore (verified 2026-02-28)
// OnboardingDataStore.kt in core-common
val Context.onboardingDataStore: DataStore<Preferences> by preferencesDataStore(
    name = "onboarding_prefs"
)

object OnboardingPreferencesKeys {
    val ONBOARDING_COMPLETE = booleanPreferencesKey("onboarding_complete")
}
```

```kotlin
// DataStoreModule.kt in app/di/
@Module
@InstallIn(SingletonComponent::class)
object DataStoreModule {
    @Provides
    @Singleton
    fun provideOnboardingDataStore(
        @ApplicationContext context: Context,
    ): DataStore<Preferences> = context.onboardingDataStore
}
```

```kotlin
// OnboardingRepository.kt in core-common
class OnboardingRepository @Inject constructor(
    private val dataStore: DataStore<Preferences>,
) {
    val isOnboardingComplete: Flow<Boolean> = dataStore.data
        .catch { e ->
            if (e is IOException) emit(emptyPreferences())
            else throw e
        }
        .map { prefs ->
            prefs[OnboardingPreferencesKeys.ONBOARDING_COMPLETE] ?: false
        }

    suspend fun setOnboardingComplete(complete: Boolean) {
        dataStore.edit { prefs ->
            prefs[OnboardingPreferencesKeys.ONBOARDING_COMPLETE] = complete
        }
    }
}
```

---

### Pattern 3: PermissionChecker for Special Permissions

**What:** Three special permissions cannot be checked with `checkSelfPermission`. Each requires a different API. `PermissionChecker` centralizes all three checks and is shared by onboarding (Phase 2) and future settings/degraded-mode logic.

**Why not `checkSelfPermission`:** `PACKAGE_USAGE_STATS` and `BIND_NOTIFICATION_LISTENER_SERVICE` are not runtime permissions — they are special-access settings that users toggle in system Settings screens. `checkSelfPermission` always returns `PERMISSION_DENIED` for these regardless of whether the user has granted them.

**Example:**
```kotlin
// Source: developer.android.com/reference + AppOpsManager docs (verified 2026-02-28)
// PermissionChecker.kt in core-common
class PermissionChecker @Inject constructor(
    @ApplicationContext private val context: Context,
) {
    /** Notification Access — uses Settings.Secure enabled_notification_listeners */
    fun isNotificationAccessGranted(): Boolean {
        return NotificationManagerCompat
            .getEnabledListenerPackages(context)
            .contains(context.packageName)
    }

    /** Usage Access — uses AppOpsManager, NOT checkSelfPermission */
    fun isUsageAccessGranted(): Boolean {
        val appOps = context.getSystemService(Context.APP_OPS_SERVICE) as AppOpsManager
        val mode = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            appOps.unsafeCheckOpNoThrow(
                AppOpsManager.OPSTR_GET_USAGE_STATS,
                Process.myUid(),
                context.packageName,
            )
        } else {
            @Suppress("DEPRECATION")
            appOps.checkOpNoThrow(
                AppOpsManager.OPSTR_GET_USAGE_STATS,
                Process.myUid(),
                context.packageName,
            )
        }
        return mode == AppOpsManager.MODE_ALLOWED
    }

    /** DND Access — uses NotificationManager.isNotificationPolicyAccessGranted */
    fun isDndAccessGranted(): Boolean {
        val nm = context.getSystemService(NotificationManager::class.java)
        return nm.isNotificationPolicyAccessGranted
    }

    /** Convenience: all three required permissions */
    fun allPermissionsGranted(): Boolean =
        isNotificationAccessGranted() && isUsageAccessGranted() && isDndAccessGranted()
}
```

**Live re-check on resume (Phase 2 concern, but architect now):** Phase 2 needs to detect permission grants when the user returns from system Settings. The right hook is `Activity.onResume` or `Lifecycle.Event.ON_RESUME` observed in the ViewModel. Phase 1 just provides the checker; Phase 2 wires the live polling.

---

### Anti-Patterns to Avoid

- **Anti-pattern — Onboarding as NavHost startDestination with navigate-away:** Setting `startDestination = "onboarding"` unconditionally then navigating away for returning users causes a visible flash of the onboarding screen. The splash-routing pattern avoids this by deciding before NavHost renders.
- **Anti-pattern — `checkSelfPermission` for special permissions:** Always returns DENIED for Notification Access and Usage Access. Silent failure; the check will always route users to onboarding even if they've already granted. Must use `NotificationManagerCompat.getEnabledListenerPackages`, `AppOpsManager`, and `NotificationManager.isNotificationPolicyAccessGranted`.
- **Anti-pattern — Multiple DataStore instances for same file:** Creating DataStore via `preferencesDataStore` delegate more than once (e.g., calling it in both ViewModel and Repository) corrupts the store. Must provide as `@Singleton` via Hilt.
- **Anti-pattern — `navigate()` directly inside recomposition:** Calling `navController.navigate()` directly in the composition (not inside a `LaunchedEffect`) can fire on every recomposition. Use `LaunchedEffect(startDestination)` or a one-shot StateFlow read.
- **Anti-pattern — Using `popUpTo` without `inclusive = true` to clear onboarding:** Without `inclusive = true`, the onboarding route itself remains on the back stack and back-press returns to it.
- **Anti-pattern — Bottom bar logic based on onboarding routes:** The existing `showBottomBar` logic in `MainActivity.kt` checks `currentRoute in AppScreen.entries.map { it.route }`. Onboarding routes are not in `AppScreen` so they are automatically excluded — this logic does not need to change.

---

## Don't Hand-Roll

| Problem | Don't Build | Use Instead | Why |
|---------|-------------|-------------|-----|
| Persistent boolean flag | Custom file I/O, shared pref wrapper | `datastore-preferences` | DataStore handles concurrency, I/O dispatching, atomic writes, and IOException recovery |
| Async state before NavHost | Custom "splash" Activity or blocking I/O on main thread | `StateFlow<String?>` in ViewModel + `collectAsStateWithLifecycle` | Compose-idiomatic; ViewModel survives config changes; no threading bugs |
| Special permission check | `ContextCompat.checkSelfPermission` for PACKAGE_USAGE_STATS | `AppOpsManager.unsafeCheckOpNoThrow` | checkSelfPermission always returns DENIED for this; AppOpsManager is the documented approach |
| Notification listener check | Manual Settings.Secure string parse | `NotificationManagerCompat.getEnabledListenerPackages(context).contains(packageName)` | Higher-level API; handles multi-component cases |

**Key insight:** The three special permissions have three completely different check APIs — there is no unified approach. The `PermissionChecker` utility's value is exactly this consolidation so no downstream code needs to know which API to call.

---

## Common Pitfalls

### Pitfall 1: DataStore Multiple Instance Corruption
**What goes wrong:** Two places in code each call `context.onboardingDataStore` via the property delegate — which internally creates two separate DataStore instances pointing at the same file. Writes from one instance are not visible to the other; data corruption can occur.
**Why it happens:** The `preferencesDataStore` delegate is a top-level property, not a singleton. Developers assume it is safe to call anywhere.
**How to avoid:** Declare the delegate exactly once (in `OnboardingDataStore.kt`) and provide the resulting `DataStore<Preferences>` object via `@Singleton` Hilt injection. All code receives the same instance.
**Warning signs:** DataStore writes appear to succeed but reads return stale values; unit tests see different data than production.

### Pitfall 2: checkSelfPermission Returns DENIED for Special Permissions
**What goes wrong:** `PermissionChecker.isUsageAccessGranted()` is implemented with `ContextCompat.checkSelfPermission(context, Manifest.permission.PACKAGE_USAGE_STATS)` — always returns `PERMISSION_DENIED` even after the user grants Usage Access in Settings. App permanently routes to onboarding.
**Why it happens:** `PACKAGE_USAGE_STATS` is a `privileged|development|appop` permission, not a runtime permission. The runtime permission check API is the wrong tool.
**How to avoid:** Use `AppOpsManager.unsafeCheckOpNoThrow(OPSTR_GET_USAGE_STATS, ...)` as documented above. Confirmed by STATE.md decision: "never call `checkSelfPermission` for special permissions".
**Warning signs:** On a device where Usage Access is visibly granted in Settings > Apps > Special App Access, the permission check returns false.

### Pitfall 3: NotificationManagerCompat.getEnabledListenerPackages Timing Lag
**What goes wrong:** Immediately after `NotificationListenerService.onListenerConnected()` fires, calling `getEnabledListenerPackages()` may still return the old (pre-grant) state. This affects the live re-check in Phase 2, not Phase 1's startup check.
**Why it happens:** Documented in Google issue tracker #257072930 — the API lags behind the actual permission state by a short interval after `onListenerConnected`.
**How to avoid:** For startup check (Phase 1), this is not a problem — the check runs long after the app process starts, not immediately after grant. For Phase 2 live re-check, use `onResume` polling rather than relying on `onListenerConnected` as the check trigger.
**Warning signs:** Permission check returns false immediately after returning from Settings despite having just granted access.

### Pitfall 4: Back Stack Contains Onboarding After Navigate-To-Main
**What goes wrong:** After completing onboarding, user navigates to main app but pressing back still shows onboarding screen.
**Why it happens:** `navController.navigate("main")` was used without `popUpTo`. Navigation adds "main" on top of "onboarding" — the back stack is [onboarding, main].
**How to avoid:** Always pair onboarding completion with `popUpTo("onboarding") { inclusive = true }`:
```kotlin
navController.navigate("main") {
    popUpTo("onboarding") { inclusive = true }
}
```
**Warning signs:** Pressing back from the first main-app screen navigates to an onboarding screen instead of exiting the app.

### Pitfall 5: Bottom Nav Bar Appears During Onboarding
**What goes wrong:** Scaffold's bottom bar is visible on onboarding screens.
**Why it happens:** The `showBottomBar` logic evaluates routes correctly, but if the NavHost nested graph approach is not used (routes like `"onboarding/start"` exist alongside main routes in a flat graph), a mis-configuration could cause `currentRoute` to match an `AppScreen` route.
**How to avoid:** Use nested navigation graphs (`navigation(route = "onboarding") { ... }` and `navigation(route = "main") { ... }`). Ensure `AppScreen.entries` only lists main tabs — confirmed from existing code, this is already safe.
**Warning signs:** NavigationBar renders on the onboarding placeholder screen.

### Pitfall 6: v0.1 Upgrader DataStore Flag
**What goes wrong:** The STATE.md blocker concern — what if v0.1 code wrote `onboarding_complete = false` to DataStore?
**How to avoid:** Confirmed by codebase audit: **v0.1 has no DataStore**. Only SharedPreferences is used, at file `usage_control_settings`. The DataStore file `onboarding_prefs` will not exist on any v0.1 device. When the file is absent, `dataStore.data` emits `emptyPreferences()`, and `prefs[ONBOARDING_COMPLETE] ?: false` returns `false`. This triggers the permission check path, which for a v0.1 user with all permissions granted returns `true`, setting the flag and routing to main. This is the correct behavior per ONBD-04.
**Warning signs:** This pitfall is confirmed NOT to exist in this codebase. No mitigation needed beyond normal implementation.

---

## Code Examples

### Read DataStore flag (one-shot in ViewModel init)
```kotlin
// Source: developer.android.com/topic/libraries/architecture/datastore (verified 2026-02-28)
val onboardingComplete: Boolean = onboardingRepository.isOnboardingComplete.first()
```

### Write DataStore flag (suspend, in ViewModel)
```kotlin
// Source: developer.android.com/topic/libraries/architecture/datastore (verified 2026-02-28)
fun markOnboardingComplete() {
    viewModelScope.launch {
        onboardingRepository.setOnboardingComplete(true)
    }
}
```

### Navigate to main and clear onboarding from back stack
```kotlin
// Source: developer.android.com (Navigation docs, verified 2026-02-28)
navController.navigate("main") {
    popUpTo("onboarding") { inclusive = true }
}
```

### Check Usage Access (AppOpsManager, minSdk 29 = Android 10 = Q+)
```kotlin
// Source: developer.android.com AppOpsManager reference + search-verified AppOpsManager.OPSTR_GET_USAGE_STATS
// Project minSdk is 29 (Android Q), so the >= Q branch is always taken
val appOps = context.getSystemService(Context.APP_OPS_SERVICE) as AppOpsManager
val mode = appOps.unsafeCheckOpNoThrow(
    AppOpsManager.OPSTR_GET_USAGE_STATS,
    Process.myUid(),
    context.packageName,
)
val granted = mode == AppOpsManager.MODE_ALLOWED
```

### Check DND Access
```kotlin
// Source: developer.android.com NotificationManager reference (verified 2026-02-28)
val nm = context.getSystemService(NotificationManager::class.java)
val granted = nm.isNotificationPolicyAccessGranted
```

### Check Notification Listener Access
```kotlin
// Source: NotificationManagerCompat (AndroidX Core) — verified via developer.android.com/reference (2026-02-28)
val granted = NotificationManagerCompat
    .getEnabledListenerPackages(context)
    .contains(context.packageName)
```

---

## State of the Art

| Old Approach | Current Approach | When Changed | Impact |
|--------------|------------------|--------------|--------|
| SharedPreferences for boolean flags | Preferences DataStore | Stable since ~2021, universally recommended as of 2024 | DataStore is coroutine-native; eliminates blocking disk I/O; atomic writes |
| Setting startDestination based on synchronous check | Dynamic startDestination via ViewModel StateFlow | Established pattern ~2022, now canonical | No UI flicker; config-change safe; testable |
| Flat NavGraph with all composables | Nested navigation graphs (`navigation { }`) | Stable since Navigation 2.4 (2022) | Clean route namespacing; `popUpTo(outerRoute)` works correctly across nested graphs |

**Deprecated/outdated:**
- SharedPreferences for new flags in Compose-first apps: works but not idiomatic; new code should use DataStore
- `checkSelfPermission` for PACKAGE_USAGE_STATS: never worked, but still the first instinct; document explicitly in PermissionChecker KDoc

---

## Open Questions

1. **Should `feature-onboarding` be a new Gradle module or live in `app`?**
   - What we know: Project uses a strict multi-module pattern (feature/, core/, service/). Onboarding is feature-level UI.
   - What's unclear: Phase 1 only needs a one-screen placeholder. Creating a full module for one composable adds 5-10 minutes of Gradle wiring. But it avoids a future refactor.
   - Recommendation: Create `feature/feature-onboarding` module now. Matches project conventions; Phase 2 and Phase 3 add real screens to it. One-time cost is worth structural consistency.

2. **Where does `DataStoreModule.kt` live?**
   - What we know: The `DatabaseModule.kt` is in `core-database`. `core-common` is where `PermissionChecker` and `OnboardingRepository` should live.
   - What's unclear: DataStore provisioning could go in `core-common/di/` or in `app/di/`. DataStore depends on Context (Application), so it belongs in a module that can use `SingletonComponent`.
   - Recommendation: Put `DataStoreModule.kt` in `app/src/main/java/com/usagecontrol/di/` (app module). This mirrors the pattern for app-level singletons and keeps `core-common` free of Hilt DI annotations (it currently has no Hilt plugin).

3. **Does `core-common` need the Hilt plugin to inject `PermissionChecker`?**
   - What we know: `core-common/build.gradle.kts` currently has no Hilt plugin. `PermissionChecker` uses `@Inject constructor` and `@ApplicationContext`.
   - What's unclear: Hilt constructor injection in a library module requires the `hilt-android` dependency and `kotlin-kapt` or `ksp` plugin on that module.
   - Recommendation: Add `hilt-android` and `ksp(hilt-compiler)` to `core-common`'s build file. This is the standard pattern — see `core-database` (already uses Room annotations via KSP).

---

## Sources

### Primary (HIGH confidence)
- `developer.android.com/topic/libraries/architecture/datastore` — DataStore API, versions, Flow-based reads, `edit()` API, Hilt injection pattern (fetched 2026-02-28)
- `developer.android.com/reference/android/service/notification/NotificationListenerService` — Notification listener access check — confirms no static `isEnabled` method; Settings.Secure is the documented approach (fetched 2026-02-28)
- `developer.android.com/develop/ui/compose/navigation` — NavHost, `startDestination`, `popUpTo`/`inclusive` (fetched 2026-02-28)
- `issuetracker.google.com/issues/257072930` — `NotificationManagerCompat.getEnabledListenerPackages` timing lag after `onListenerConnected` (fetched 2026-02-28)
- Codebase audit — confirmed no DataStore in v0.1; SharedPreferences only in `feature-settings`

### Secondary (MEDIUM confidence)
- `engineering.monstar-lab.com/en/post/2022/07/13/Navigation-parameters-with-Jetpack-Compose/` — Dynamic `startDestination` ViewModel pattern with `StateFlow` (fetched 2026-02-28, code verified against official Navigation docs)
- `developer.android.com/codelabs/android-preferences-datastore` — Complete Preferences DataStore codelab including error handling with `catch { emit(emptyPreferences()) }` (fetched 2026-02-28)
- WebSearch verification of `AppOpsManager.unsafeCheckOpNoThrow` for `PACKAGE_USAGE_STATS` — multiple sources agree; minSdk 29 = always use `unsafeCheckOpNoThrow` (deprecated `checkOpNoThrow` pre-Q)

### Tertiary (LOW confidence)
- WebSearch: `NotificationManagerCompat.getEnabledListenerPackages` as the recommended check — multiple sources agree but the Google issue tracker shows a known timing bug; LOW for the live-re-check use case in Phase 2, HIGH for startup-time check in Phase 1

---

## Metadata

**Confidence breakdown:**
- Standard stack: HIGH — versions verified against official docs; all libraries already in project except DataStore which has verified 1.1.1 release
- Architecture patterns: HIGH — splash routing pattern verified against official Navigation docs and working code examples; confirmed safe by codebase audit (no v0.1 DataStore)
- Permission checking: HIGH for startup check; MEDIUM for live re-check (timing bug caveat applies to Phase 2 only)
- Pitfalls: HIGH — each pitfall grounded in official docs, codebase audit, or tracked bugs

**Research date:** 2026-02-28
**Valid until:** 2026-04-28 (stable libraries — 60 days reasonable; re-verify if Compose BOM is upgraded)
