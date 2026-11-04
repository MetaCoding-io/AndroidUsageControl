# Stack Research

**Domain:** Android onboarding/UX — permission walkthrough, feature tours, setup wizards (Jetpack Compose)
**Researched:** 2026-02-28
**Confidence:** HIGH (verified via official Android Developers docs + Accompanist official docs + Maven release data)

---

## Context: What Already Exists

The app runs on Compose BOM `2024.02.00`, Compose Navigation `2.7.7`, Hilt `2.50`, Room `2.6.1`, Kotlin `1.9.22`.
Persistence today: SharedPreferences in `feature-settings` (SettingsViewModel, CleanupWorker). DataStore is **not** currently in the project.

All research below is scoped to **new additions needed for v0.2 onboarding milestone only**.

---

## Recommended Stack (New Additions Only)

### Core Technologies — New for Onboarding

| Technology | Version | Purpose | Why Recommended |
|------------|---------|---------|-----------------|
| `androidx.datastore:datastore-preferences` | `1.1.7` | First-run flag, onboarding-complete detection | Coroutine-native Flow API; proper replacement for SharedPreferences when new code is written; aligns with Compose's reactive model. Pin to 1.1.7 (latest stable as of 2026-02; 1.2.0 adds Guava/device-protected storage — unneeded complexity) |
| `androidx.compose.foundation:foundation` | via BOM | `HorizontalPager` + `rememberPagerState` for feature tour carousel | HorizontalPager is now in official Compose Foundation — Accompanist pager is deprecated and removed. Already transitive via `compose-material3`, but pager APIs need explicit awareness |
| `com.google.accompanist:accompanist-permissions` | `0.37.3` | `rememberPermissionState` / `rememberMultiplePermissionsState` for the POST_NOTIFICATIONS runtime permission | The only mainstream runtime-permission helper for Compose. Google itself maintains it. No official Compose replacement exists as of 2026-02. Experimental annotation required but widely production-deployed |

### Supporting Libraries — No New Dependencies Needed

| Capability | Mechanism | Notes |
|------------|-----------|-------|
| Special permissions (Notification Access, Usage Stats, DND Access) | `startActivity(Intent(Settings.ACTION_NOTIFICATION_LISTENER_SETTINGS))` etc. | These are **not** runtime permissions — they require sending users to system settings. No library handles this. Pure Kotlin/Activity code. Check grant status via `NotificationManagerCompat.getEnabledListenerPackages()`, `AppOpsManager.checkOpNoThrow()`, `NotificationManager.isNotificationPolicyAccessGranted()` |
| Onboarding screen transitions (slide, fade) | `AnimatedContent`, `AnimatedVisibility`, `slideInHorizontally`, `fadeIn/fadeOut` | All in `compose-animation`, which is **already transitive** via `compose-ui`. Zero new dependencies |
| Navigation to/from onboarding graph | `NavHost` + nested nav graph | Already using `compose-navigation 2.7.7`. Add a dedicated `onboarding` nav graph with its own `startDestination` |
| Onboarding ViewModel scoped to flow | `hiltViewModel(navBackStackEntry)` | Already in project via `hilt-navigation-compose`. Share VM across onboarding steps without re-injecting |
| Page indicators (dot indicators) | Custom Compose composable | 8 lines of `Row` + `Box` + `CircleShape` from official docs. No library needed |

---

## Installation (Gradle changes required)

### `gradle/libs.versions.toml` additions

```toml
[versions]
datastorePreferences = "1.1.7"
accompanistPermissions = "0.37.3"

[libraries]
datastore-preferences = { module = "androidx.datastore:datastore-preferences", version.ref = "datastorePreferences" }
accompanist-permissions = { module = "com.google.accompanist:accompanist-permissions", version.ref = "accompanistPermissions" }
```

### Where to add dependencies

- **`datastore-preferences`** — Add to `core:core-common` (or a new `core:core-onboarding` module). The onboarding flag is app-wide, so it belongs in a shared core layer, injected via Hilt as a singleton repository.
- **`accompanist-permissions`** — Add to the new `feature:feature-onboarding` module only (permission request UI is localized there).

### Module build.gradle.kts examples

```kotlin
// core:core-common or new core:core-onboarding
implementation(libs.datastore.preferences)

// feature:feature-onboarding
implementation(libs.accompanist.permissions)
implementation(platform(libs.compose.bom))          // already in version catalog
implementation(libs.compose.foundation)              // verify this alias exists; foundation is BOM-managed
```

---

## Alternatives Considered

| Recommended | Alternative | Why Not |
|-------------|-------------|---------|
| `datastore-preferences 1.1.7` for onboarding flag | Continue using SharedPreferences | SharedPreferences is synchronous and non-coroutine-native. New code should not extend its use. DataStore integrates cleanly with Flow + ViewModel |
| `datastore-preferences 1.1.7` | `datastore-preferences 1.2.0` | 1.2.0 adds Guava + device-protected storage — neither feature is needed here. 1.1.7 is the last 1.1.x stable and avoids over-upgrading for this milestone |
| `accompanist-permissions 0.37.3` | Write permission logic manually with `rememberLauncherForActivityResult` | Manual approach works for simple cases but doesn't handle rationale state, should-show-rationale tracking, or denied state transitions cleanly. Accompanist encapsulates all of this with `PermissionStatus` sealed class |
| `HorizontalPager` (Compose Foundation, via BOM) | Accompanist pager | Accompanist pager is deprecated and removed from the library. The official Foundation pager is the correct successor — graduated stable in Compose March 2023 release |
| Custom dot indicator composable | Third-party indicator library | 8 lines of Compose. Any library adds dead weight and version coupling |
| `IntentNav` to system settings | Accompanist Permissions for special permissions | Accompanist handles standard runtime permissions only. Notification Listener, Usage Stats, DND Access are `AppOps`/`BindService` style — they live in system Settings pages. No library covers them |

---

## What NOT to Use

| Avoid | Why | Use Instead |
|-------|-----|-------------|
| Accompanist pager (`com.google.accompanist:accompanist-pager`) | Deprecated and removed from Accompanist as of v0.35+. Will conflict with Foundation pager | `androidx.compose.foundation.pager.HorizontalPager` (in compose-foundation, via BOM) |
| Accompanist Navigation Animation | Deprecated and removed. Navigation Compose 2.7+ has built-in transitions | `NavHost` with `enterTransition`/`exitTransition` parameters on `composable()` |
| Any third-party onboarding library (AppIntro, Material Intro Screen, etc.) | View-based or requires XML; conflicts with pure-Compose architecture; adds permissions/dependencies with no privacy audit | HorizontalPager + custom Compose screens is 50-100 lines and fully controlled |
| SharedPreferences for the onboarding flag | Synchronous; not coroutine-native; the project should stop extending SharedPreferences usage for new features | `datastore-preferences` with a coroutine-based repository |
| `Navigation 3` (`androidx.navigation:navigation3-*`) | Still in alpha (2026-02). Breaking API changes expected. Not compatible with existing nav graph setup | Stay on `navigation-compose 2.7.7` already in the project |

---

## Stack Patterns for This Milestone

**For the onboarding-complete flag:**
- Inject a `OnboardingRepository` singleton (Hilt `@Singleton`) backed by DataStore
- Expose `isOnboardingComplete: Flow<Boolean>`
- In `MainActivity` or a root-level `SplashViewModel`, collect this flow and navigate to `onboarding` or `main` graph accordingly

**For the permission walkthrough (3 special permissions — Notification, Usage, DND):**
- Use `startActivity(Intent(Settings.ACTION_NOTIFICATION_LISTENER_SETTINGS))` — no `ActivityResultLauncher` needed since result comes via `onResume` check
- Check status in `LifecycleObserver` `onResume` via `NotificationManagerCompat.getEnabledListenerPackages()`, `AppOpsManager`, `NotificationManager.isNotificationPolicyAccessGranted()`
- Only use `accompanist-permissions` for `POST_NOTIFICATIONS` (Android 13+ runtime permission) which IS a standard runtime permission

**For the feature tour (5 tabs carousel):**
- `rememberPagerState(pageCount = { 5 })` + `HorizontalPager`
- Animate between pages programmatically with `pagerState.animateScrollToPage()`
- Dot indicator: custom `Row` of `Box` composables, reading `pagerState.currentPage`

**For the setup wizard (first rule / preset):**
- Nested nav graph within onboarding, reuses existing `feature-rules` or `feature-settings` composables
- Shared ViewModel via `hiltViewModel(navBackStackEntry = parentEntry)` so wizard steps share state

---

## Version Compatibility

| Package | Version | Compatible With | Notes |
|---------|---------|-----------------|-------|
| `datastore-preferences` | `1.1.7` | Kotlin 1.9.x, coroutines 1.7.x | No compatibility issues expected. Pure Kotlin library |
| `accompanist-permissions` | `0.37.3` | Compose UI 1.7.x (Compose BOM ~2024.09+) | Current project BOM is 2024.02.00 (Compose 1.6.x). **See warning below** |
| `compose-foundation` (HorizontalPager) | via BOM `2024.02.00` → foundation `1.6.x` | Kotlin 1.9.22, AGP 8.2.2 | HorizontalPager stable since foundation 1.4.x. Current BOM includes it |

### Compatibility Warning: accompanist-permissions 0.37.3 vs current BOM

Accompanist 0.37.0+ was built against Compose 1.7.x (BOM ~2024.09). The project currently uses BOM `2024.02.00` (Compose 1.6.x). Two options:

**Option A — Upgrade Compose BOM (Recommended):** Upgrade BOM to `2025.01.01` or later (maps to Compose 1.8.x / foundation 1.8.x). This is a non-breaking upgrade since the project does not use experimental Compose APIs. Kotlin 1.9.22 is compatible.

**Option B — Pin older accompanist:** Use `accompanist-permissions 0.36.0` which targets Compose 1.7.x, or `0.34.0` which targets Compose 1.6.x. Avoids BOM upgrade but uses an older permissions library.

**Recommendation: Option A** — upgrade BOM to `2025.01.01` as part of this milestone. The upgrade is low-risk (bug-fix and stability focused between 2024.02 and 2025.01) and brings the project's Compose dependency current. All existing APIs remain compatible.

**Updated BOM recommendation:**
```toml
[versions]
composeBom = "2025.01.01"   # was 2024.02.00
```

---

## Sources

- [Compose BOM to library version mapping](https://developer.android.com/develop/ui/compose/bom/bom-mapping) — BOM 2026.02.01 → foundation 1.10.4; BOM 2025.01.01 → foundation 1.8.x range (HIGH confidence)
- [androidx.datastore releases](https://developer.android.com/jetpack/androidx/releases/datastore) — 1.1.7 stable (May 2025), 1.2.0 stable (Nov 2025) (HIGH confidence)
- [compose-foundation releases](https://developer.android.com/jetpack/androidx/releases/compose-foundation) — 1.10.4 latest stable Feb 2026; HorizontalPager stable since 1.4.x (HIGH confidence)
- [Accompanist GitHub releases](https://github.com/google/accompanist/releases) — v0.37.3 latest (Apr 2025); permissions library actively maintained; pager deprecated (HIGH confidence)
- [Accompanist permissions docs](https://google.github.io/accompanist/permissions/) — API still `@ExperimentalPermissionsApi`; `rememberPermissionState`, `rememberMultiplePermissionsState` (HIGH confidence)
- [Pager in Compose — Android Developers](https://developer.android.com/develop/ui/compose/layouts/pager) — `rememberPagerState`, `HorizontalPager` canonical usage (HIGH confidence)
- [Request special permissions — Android Developers](https://developer.android.com/training/permissions/requesting-special) — Settings intent pattern for special permissions (HIGH confidence)
- [Compose animation quick guide](https://developer.android.com/develop/ui/compose/animation/quick-guide) — `AnimatedContent`, `AnimatedVisibility`, `slideIntoContainer` APIs verified (HIGH confidence)
- "Accompanist is Dead" article on Medium — confirmed pager deprecated, permissions still maintained (MEDIUM confidence, corroborated by official GitHub)

---

*Stack research for: AndroidUsageControl v0.2 onboarding/UX milestone*
*Researched: 2026-02-28*
