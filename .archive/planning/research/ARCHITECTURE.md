# Architecture Research

**Domain:** Android onboarding / permission walkthrough / feature tour — layered onto existing Jetpack Compose + Navigation + Hilt multi-module app
**Researched:** 2026-02-28
**Confidence:** HIGH (official Android docs + Hilt multi-module docs + verified patterns)

---

## Standard Architecture

### System Overview — Onboarding Integration with Existing App

```
┌─────────────────────────────────────────────────────────────────┐
│                        MainActivity                              │
│  (single-Activity — unchanged, keeps startMonitoringServices)   │
├─────────────────────────────────────────────────────────────────┤
│                    NavHost (top-level)                           │
│  startDestination driven by onboardingComplete: Boolean Flow     │
│                                                                  │
│  ┌──────────────────────────────────┐                           │
│  │  Onboarding Nested Nav Graph     │  (new)                    │
│  │  route: "onboarding"             │                           │
│  │  ┌────────┐ ┌──────────┐ ┌─────┐│                           │
│  │  │Welcome │→│Permission│→│Tour ││                           │
│  │  │Screen  │ │Walkthrough│ │Pager││                           │
│  │  └────────┘ └──────────┘ └──────┘│                          │
│  └──────────────────────────────────┘                           │
│                     │                                            │
│          popUpTo("onboarding") { inclusive = true }             │
│                     ▼                                            │
│  ┌──────────────────────────────────┐                           │
│  │  Main App (existing Scaffold)    │  (mostly unchanged)       │
│  │  startDestination: "notifications"│                          │
│  │  ┌────────┐ ┌──────┐ ┌────────┐ │                           │
│  │  │Notif   │ │Screen│ │Controls│ │                           │
│  │  │Screen  │ │Time  │ │Screen  │ │                           │
│  │  └────────┘ └──────┘ └────────┘ │                           │
│  │  ┌─────────┐ ┌────────┐         │                           │
│  │  │Insights │ │Settings│         │                           │
│  │  └─────────┘ └────────┘         │                           │
│  └──────────────────────────────────┘                           │
├─────────────────────────────────────────────────────────────────┤
│                  feature-onboarding module (new)                 │
│  OnboardingViewModel  OnboardingRepository  OnboardingDataStore  │
├─────────────────────────────────────────────────────────────────┤
│                  core-common (existing, extended)                │
│  PermissionChecker (new helper)  AppNameResolver  TimeUtils      │
├─────────────────────────────────────────────────────────────────┤
│              Jetpack DataStore (new, replaces SharedPrefs)       │
│  Keys: onboarding_complete (Boolean), permissions_prompted (Boolean)│
└─────────────────────────────────────────────────────────────────┘
```

### Component Responsibilities

| Component | Responsibility | Typical Implementation |
|-----------|----------------|------------------------|
| `MainActivity` | Reads `onboardingComplete` Flow before rendering NavHost | Existing, minimal change: collect `isOnboarded` from `OnboardingViewModel` before branching `startDestination` |
| Onboarding nested graph | Encapsulates 3–4 onboarding destinations; never reachable after completion | New `navigation(route = "onboarding") { ... }` block in `UsageControlAppContent` |
| `WelcomeScreen` | Value proposition, hero image, "Get Started" CTA | New composable in `feature-onboarding` |
| `PermissionWalkthroughScreen` | Step-by-step permission cards with status indicators; deep-links user to system settings | New composable + `PermissionChecker` helper |
| `FeatureTourScreen` | `HorizontalPager` over 5 tab-by-tab slides; dot page indicator | New composable, no ViewModel needed |
| `QuickSetupScreen` | One-tap preset choices (Night Mode, Meeting Mode) that create real rules | New composable, delegates to existing `RuleDao` via `OnboardingViewModel` |
| `OnboardingViewModel` | Checks permission state, writes completion flag, applies preset | `@HiltViewModel`, injected with `OnboardingRepository` |
| `OnboardingRepository` | DataStore read/write for completion flag; calls `PermissionChecker` | Standard repository pattern, injected via Hilt |
| `PermissionChecker` | Stateless helper that reads system state for each special permission | `object` in `core-common`; reads `Settings.Secure`, `AppOpsManager` |
| `OnboardingDataStore` | Wraps Jetpack DataStore `Preferences` for onboarding keys | New class in `feature-onboarding`, singleton via Hilt `@Provides` |

---

## Recommended Project Structure

```
feature/
└── feature-onboarding/               # NEW module
    └── src/main/java/com/usagecontrol/feature/onboarding/
        ├── OnboardingModule.kt        # Hilt @Module — provides OnboardingDataStore
        ├── OnboardingRepository.kt    # DataStore read/write + permission gate logic
        ├── OnboardingViewModel.kt     # @HiltViewModel
        ├── screens/
        │   ├── WelcomeScreen.kt       # Splash-style value prop
        │   ├── PermissionWalkthroughScreen.kt   # 3-step permission cards
        │   ├── FeatureTourScreen.kt   # HorizontalPager, 5 slides
        │   └── QuickSetupScreen.kt    # Night Mode / Meeting Mode preset picker
        └── components/
            ├── PermissionStatusCard.kt   # Reusable permission row card
            └── OnboardingProgressBar.kt  # Step progress indicator

core/
└── core-common/                       # EXISTING module, add one file
    └── src/main/java/com/usagecontrol/core/common/
        └── PermissionChecker.kt       # NEW: checks NLS, UsageStats, DND state

app/
└── src/main/java/com/usagecontrol/
    ├── MainActivity.kt                # MODIFY: read isOnboarded before NavHost
    └── navigation/
        └── AppNavigation.kt           # MODIFY: add onboarding nested graph
```

### Structure Rationale

- **`feature-onboarding/`:** New Gradle module matching the existing `feature:feature-*` convention. Depends on `core-common`, `core-model`, `core-database` (for QuickSetup preset writes). The `app` module adds it as `implementation(project(":feature:feature-onboarding"))`.
- **`PermissionChecker` in `core-common`:** The three special permissions (Notification Access, Usage Access, DND) are needed by both `feature-settings` (existing) and `feature-onboarding` (new). Centralising the check prevents duplication. It is a pure Kotlin helper with no framework dependency — just `Context`.
- **`feature-onboarding` does NOT depend on other feature modules:** Avoids circular dependencies. QuickSetup writes directly to `core-database` (RuleDao), same as other features. No feature-to-feature references.

---

## Architectural Patterns

### Pattern 1: Conditional startDestination — Read Before Compose

**What:** `OnboardingViewModel` exposes `isOnboarded: StateFlow<Boolean>` backed by DataStore. `MainActivity` (or `UsageControlAppContent`) collects it synchronously before the `NavHost` renders. The `startDestination` of the `NavHost` is either `"onboarding"` or `"notifications"`.

**When to use:** Single decision point at app cold-start. After completion, the onboarding graph is popped inclusive so the back-button never reveals it.

**Trade-offs:** Adds a tiny startup delay (~10 ms DataStore read). Use `collectAsStateWithLifecycle(initialValue = null)` and show a splash/loading state while `null` to avoid flicker.

**Example:**
```kotlin
// In UsageControlAppContent
@Composable
private fun UsageControlAppContent(
    onboardingViewModel: OnboardingViewModel = hiltViewModel()
) {
    val isOnboarded by onboardingViewModel.isOnboarded
        .collectAsStateWithLifecycle(initialValue = null)

    // Defer NavHost until we know state — avoid startDestination flip-flop
    if (isOnboarded == null) {
        SplashScreen()
        return
    }

    val navController = rememberNavController()
    NavHost(
        navController = navController,
        startDestination = if (isOnboarded == true) "notifications" else "onboarding"
    ) {
        // Onboarding nested graph
        navigation(route = "onboarding", startDestination = "welcome") {
            composable("welcome") {
                WelcomeScreen(onGetStarted = { navController.navigate("permissions") })
            }
            composable("permissions") {
                PermissionWalkthroughScreen(
                    onAllGranted = { navController.navigate("tour") },
                    onSkip = { navController.navigate("tour") }
                )
            }
            composable("tour") {
                FeatureTourScreen(
                    onFinish = { navController.navigate("quick-setup") }
                )
            }
            composable("quick-setup") {
                QuickSetupScreen(
                    onComplete = {
                        onboardingViewModel.markOnboardingComplete()
                        navController.navigate("notifications") {
                            popUpTo("onboarding") { inclusive = true }
                        }
                    }
                )
            }
        }

        // Existing main tabs scaffold — unchanged
        composable("notifications") { NotificationsScreen(...) }
        // ... rest of existing graph
    }
}
```

---

### Pattern 2: PermissionChecker — System Setting Readers in core-common

**What:** The three required permissions (Notification Access, Usage Access, DND Access) are NOT standard Android runtime permissions — they require the user to navigate to system settings. They cannot use `rememberPermissionState` or `ActivityResultLauncher`. Instead, check system state directly and deep-link to the appropriate `Settings.*` action.

**When to use:** Called from `OnboardingViewModel` and `SettingsViewModel`. Live-checked in `onResume` via `LaunchedEffect(lifecycle)` to refresh status when user returns from settings.

**Trade-offs:** No OS callback when permission is granted — must poll on resume. This is unavoidable for these permission types on Android.

**Example:**
```kotlin
// core/core-common/PermissionChecker.kt
object PermissionChecker {

    fun isNotificationListenerEnabled(context: Context): Boolean {
        val flat = Settings.Secure.getString(
            context.contentResolver,
            "enabled_notification_listeners"
        ) ?: return false
        val myComponent = ComponentName(context, "com.usagecontrol.service.notification.NotificationCaptureService")
        return flat.contains(myComponent.flattenToString())
    }

    fun isUsageAccessGranted(context: Context): Boolean {
        val appOps = context.getSystemService(Context.APP_OPS_SERVICE) as AppOpsManager
        val mode = appOps.checkOpNoThrow(
            AppOpsManager.OPSTR_GET_USAGE_STATS,
            android.os.Process.myUid(),
            context.packageName
        )
        return mode == AppOpsManager.MODE_ALLOWED
    }

    fun isDndAccessGranted(context: Context): Boolean {
        val notifManager = context.getSystemService(NotificationManager::class.java)
        return notifManager.isNotificationPolicyAccessGranted
    }
}
```

---

### Pattern 3: HorizontalPager Feature Tour — No ViewModel Needed

**What:** The feature tour is purely presentational. 5 static slide data objects (title, subtitle, icon, tab highlight color). `rememberPagerState` drives the dot indicator. "Skip" and "Next" buttons advance or complete.

**When to use:** Stateless tour content. Do not persist which slide the user reached — it is ephemeral UI state only.

**Trade-offs:** Simple and fast. If slides need to be data-driven in future, extract to a repository.

**Example:**
```kotlin
@Composable
fun FeatureTourScreen(onFinish: () -> Unit) {
    val slides = remember { buildTourSlides() } // list of TourSlide data class
    val pagerState = rememberPagerState(pageCount = { slides.size })
    val scope = rememberCoroutineScope()

    Column(modifier = Modifier.fillMaxSize()) {
        HorizontalPager(state = pagerState, modifier = Modifier.weight(1f)) { page ->
            TourSlideContent(slide = slides[page])
        }

        // Dot indicator
        Row(horizontalArrangement = Arrangement.Center, modifier = Modifier.fillMaxWidth()) {
            repeat(slides.size) { index ->
                Box(
                    modifier = Modifier
                        .size(8.dp)
                        .padding(2.dp)
                        .clip(CircleShape)
                        .background(
                            if (pagerState.currentPage == index)
                                MaterialTheme.colorScheme.primary
                            else
                                MaterialTheme.colorScheme.outline
                        )
                )
            }
        }

        Row(modifier = Modifier.padding(16.dp)) {
            TextButton(onClick = onFinish) { Text("Skip") }
            Spacer(Modifier.weight(1f))
            Button(
                onClick = {
                    if (pagerState.currentPage < slides.lastIndex) {
                        scope.launch { pagerState.animateScrollToPage(pagerState.currentPage + 1) }
                    } else {
                        onFinish()
                    }
                }
            ) {
                Text(if (pagerState.currentPage < slides.lastIndex) "Next" else "Done")
            }
        }
    }
}
```

---

### Pattern 4: DataStore for Onboarding State — Replace SharedPreferences

**What:** Use `androidx.datastore:datastore-preferences` to store `onboarding_complete` as a `Boolean` preference. DataStore is coroutine-native and exposes a `Flow<Preferences>` — integrates cleanly with `collectAsStateWithLifecycle` in Compose.

**When to use:** Replacing all new preference writes. The existing `SettingsViewModel` already uses `SharedPreferences` directly — do not migrate it in this milestone to limit scope.

**Trade-offs:** DataStore has a cold-start cost (~10 ms) compared to the synchronous SharedPreferences read. This is acceptable; mitigated by the `null` initial state guard in Pattern 1.

**Example:**
```kotlin
// feature-onboarding/OnboardingRepository.kt
class OnboardingRepository @Inject constructor(
    @ApplicationContext private val context: Context
) {
    private val Context.dataStore by preferencesDataStore("onboarding_prefs")
    private val ONBOARDING_COMPLETE = booleanPreferencesKey("onboarding_complete")

    val isOnboardingComplete: Flow<Boolean> = context.dataStore.data
        .map { prefs -> prefs[ONBOARDING_COMPLETE] ?: false }

    suspend fun markComplete() {
        context.dataStore.edit { prefs ->
            prefs[ONBOARDING_COMPLETE] = true
        }
    }
}
```

---

## Data Flow

### First-Launch Flow (Onboarding)

```
App cold-start
    ↓
UsageControlApp.onCreate (HiltApplication init)
    ↓
MainActivity.onCreate
    ↓
UsageControlAppContent composable
    ↓
OnboardingViewModel.isOnboarded (StateFlow<Boolean?>) ← DataStore.data.map
    │
    ├── null → SplashScreen (loading guard, ~10 ms)
    │
    ├── false → NavHost(startDestination = "onboarding")
    │       ↓
    │   WelcomeScreen → PermissionWalkthroughScreen
    │       ↓                   ↓
    │   (user taps)    PermissionChecker.check*() on resume
    │                  startActivity(Settings.ACTION_NOTIFICATION_LISTENER_SETTINGS)
    │                           ↓ (user returns)
    │                  LaunchedEffect(lifecycleState) → recheck status
    │                           ↓ (all granted OR skipped)
    │               FeatureTourScreen (HorizontalPager, 5 slides)
    │                           ↓ (Done/Skip)
    │               QuickSetupScreen (preset picker)
    │                           ↓ (confirm)
    │               OnboardingViewModel.markOnboardingComplete()
    │                   → DataStore.edit { ONBOARDING_COMPLETE = true }
    │               navController.navigate("notifications") {
    │                   popUpTo("onboarding") { inclusive = true }
    │               }
    │
    └── true → NavHost(startDestination = "notifications")
               (existing bottom-nav Scaffold, no change)
```

### Permission Re-check on Resume

```
User returns from Android system Settings screen
    ↓
Activity.onResume → Compose lifecycle event ON_RESUME
    ↓
LaunchedEffect(lifecycleOwner.lifecycle) {
    snapshotFlow { lifecycleState }
        .filter { it == Lifecycle.State.RESUMED }
        .collect { viewModel.recheckPermissions() }
}
    ↓
PermissionChecker.isNotificationListenerEnabled(context)
PermissionChecker.isUsageAccessGranted(context)
PermissionChecker.isDndAccessGranted(context)
    ↓
UI updates permission card status indicators reactively
```

---

## Integration Points

### Existing Modules — What Changes

| Module | Change Type | What Changes |
|--------|------------|--------------|
| `app` | **MODIFY** | Add `feature-onboarding` dependency; modify `UsageControlAppContent` to add onboarding nested graph; inject `OnboardingViewModel` |
| `app/navigation/AppNavigation.kt` | **MODIFY** | Add `AppScreen.entries` check to also hide bottom bar for onboarding routes |
| `core/core-common` | **EXTEND** | Add `PermissionChecker.kt` — stateless object, no DI needed |
| `feature/feature-settings` | **NO CHANGE** | Settings screen already shows permission shortcuts; `SettingsViewModel` SharedPrefs stays as-is |
| `service/service-notification` | **NO CHANGE** | Onboarding checks if NLS is enabled but does not touch the service itself |

### New Module — feature-onboarding

| Component | Depends On | Notes |
|-----------|-----------|-------|
| `OnboardingViewModel` | `core-common` (PermissionChecker), `core-database` (RuleDao for QuickSetup) | `@HiltViewModel` — Hilt handles injection; `core-database` access follows exact same pattern as `feature-settings` |
| `OnboardingRepository` | `core-common` (Context), DataStore | Singleton scoped via `@Provides @Singleton` in `OnboardingModule` |
| Screens | `core-ui` (theme + components), `core-model` (Rule model for preset) | Standard composables |

### Gradle Dependencies for New Module

```kotlin
// feature/feature-onboarding/build.gradle.kts
dependencies {
    implementation(project(":core:core-common"))
    implementation(project(":core:core-model"))
    implementation(project(":core:core-database"))
    implementation(project(":core:core-ui"))

    implementation(platform(libs.compose.bom))
    implementation(libs.compose.ui)
    implementation(libs.compose.material3)
    implementation(libs.compose.material.icons)
    implementation(libs.compose.foundation)         // HorizontalPager

    implementation(libs.hilt.android)
    ksp(libs.hilt.compiler)
    implementation(libs.hilt.navigation.compose)

    implementation(libs.lifecycle.runtime.compose)  // collectAsStateWithLifecycle
    implementation(libs.datastore.preferences)       // NEW: add to libs.versions.toml
}
```

Add to `libs.versions.toml`:
```toml
[versions]
datastore = "1.1.1"

[libraries]
datastore-preferences = { module = "androidx.datastore:datastore-preferences", version.ref = "datastore" }
```

---

## Anti-Patterns

### Anti-Pattern 1: Separate Activity for Onboarding

**What people do:** Create a dedicated `OnboardingActivity` that finishes and launches `MainActivity` when complete.

**Why it's wrong:** In a Compose-first, single-Activity app, adding a second Activity creates split state, breaks the `NavController` (each Activity has its own), and duplicates Hilt component setup. The modern Android recommendation (2023+) is "one Activity, nested navigation graphs."

**Do this instead:** Use a nested navigation graph within the existing `NavHost` in `MainActivity`. The `popUpTo(inclusive = true)` call removes the entire onboarding sub-graph from the back stack cleanly.

---

### Anti-Pattern 2: Using rememberPermissionState for Special Permissions

**What people do:** Use `accompanist-permissions` `rememberPermissionState(Manifest.permission.BIND_NOTIFICATION_LISTENER_SERVICE)` or similar.

**Why it's wrong:** `BIND_NOTIFICATION_LISTENER_SERVICE`, `PACKAGE_USAGE_STATS`, and `ACCESS_NOTIFICATION_POLICY` are **not runtime permissions** — they require manual user navigation to system settings. `rememberPermissionState` cannot trigger the system settings intent and will always report the wrong state.

**Do this instead:** Use `PermissionChecker` to read actual system state, and deep-link to the relevant `Settings.*` action. Re-check status in `LaunchedEffect` on `Lifecycle.State.RESUMED`.

---

### Anti-Pattern 3: Storing Onboarding State in SharedPreferences in a ViewModel

**What people do:** Inject `SharedPreferences` directly into `OnboardingViewModel` and read synchronously.

**Why it's wrong:** `SharedPreferences.getBoolean()` blocks the calling thread. In a ViewModel called from Compose's main thread, this risks ANR. The project already has a pattern of `SharedPreferences` in `SettingsViewModel` — it survives for simple string values, but `OnboardingViewModel` needs to gate initial nav decisions, making the async DataStore `Flow` critical here.

**Do this instead:** Use DataStore in `OnboardingRepository`. The Flow emits the value asynchronously and `collectAsStateWithLifecycle(initialValue = null)` provides a safe loading guard.

---

### Anti-Pattern 4: Circular Feature Module Dependencies

**What people do:** Import `feature-rules` or `feature-settings` from inside `feature-onboarding` to reuse their rule-creation logic.

**Why it's wrong:** Feature modules must not depend on each other — this creates circular Gradle dependency chains and defeats the purpose of modularization.

**Do this instead:** `feature-onboarding` accesses `core-database` directly (same as other features do). The `OnboardingViewModel.applyPreset()` method calls `RuleDao.insertRule()` directly — it does not call into `feature-rules`.

---

## Scaling Considerations

This is a single-user local app with no server — scaling is not a concern. The relevant stability considerations are:

| Concern | Approach |
|---------|----------|
| Onboarding shown again after app update | DataStore key survives app updates (stored in app's data dir). Only cleared on full uninstall or `adb shell pm clear`. This is correct behavior. |
| Permission state stale after user revokes | `PermissionChecker` called on every `ON_RESUME` — always fresh. |
| FeatureTourScreen slow to compose | 5 static slides with no network. HorizontalPager is lazy — only adjacent pages compose. No perf risk. |
| QuickSetup preset conflicts with existing rules | Preset inserts new rules; does not overwrite existing ones. Users can delete in the Rules screen. |

---

## Build Order

The recommended implementation order given existing module dependencies:

1. **`core-common` — add `PermissionChecker`** (no new deps, unblocks everything else)
2. **Add `datastore-preferences` to `libs.versions.toml`** (unblocks `OnboardingRepository`)
3. **Create `feature-onboarding` Gradle module** with build file mirroring other feature modules
4. **`OnboardingRepository` + `OnboardingModule`** (DataStore wrapper + Hilt binding)
5. **`OnboardingViewModel`** (depends on repository and PermissionChecker)
6. **Screens** in order: `WelcomeScreen` → `PermissionWalkthroughScreen` → `FeatureTourScreen` → `QuickSetupScreen`
7. **Wire into `app` module** — add dependency, modify `UsageControlAppContent` to add nested graph and `OnboardingViewModel` injection

---

## Sources

- [Android Navigation with Compose — Official Docs](https://developer.android.com/develop/ui/compose/navigation) — HIGH confidence
- [Nested Graphs — Official Docs](https://developer.android.com/guide/navigation/design/nested-graphs) — HIGH confidence
- [Navigation Best Practices for Multi-Module Projects — Official Docs](https://developer.android.com/guide/navigation/integrations/multi-module) — HIGH confidence
- [Hilt in Multi-Module Apps — Official Docs](https://developer.android.com/training/dependency-injection/hilt-multi-module) — HIGH confidence
- [Jetpack DataStore — Official Docs](https://developer.android.com/topic/libraries/architecture/datastore) — HIGH confidence
- [Pager in Compose — Official Docs](https://developer.android.com/develop/ui/compose/layouts/pager) — HIGH confidence
- [Accompanist Permissions — still maintained, experimental API](https://google.github.io/accompanist/permissions/) — MEDIUM confidence (not used; special permissions can't use it anyway)
- [Mastering Jetpack Compose Navigation 2025 — nested graphs + safe args](https://medium.com/design-bootcamp/mastering-jetpack-compose-navigation-in-2025-with-nested-graphs-safe-args-2e1ccc2cc374) — MEDIUM confidence (verified against official docs)
- [DataStore vs SharedPreferences 2025](https://www.atipik.ch/en/blog/android-jetpack-datastore-vs-sharedpreferences) — MEDIUM confidence (verified against official docs)

---

*Architecture research for: AndroidUsageControl v0.2 — Onboarding Milestone*
*Researched: 2026-02-28*
