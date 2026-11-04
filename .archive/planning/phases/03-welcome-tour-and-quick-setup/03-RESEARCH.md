# Phase 3: Welcome, Tour, and Quick Setup - Research

**Researched:** 2026-02-28
**Domain:** Jetpack Compose HorizontalPager, multi-screen onboarding flow ordering, Night Mode preset activation, "Revisit Tour" deep-link from Settings
**Confidence:** HIGH

---

<phase_requirements>
## Phase Requirements

| ID | Description | Research Support |
|----|-------------|-----------------|
| TOUR-01 | User sees welcome screen with app value proposition as the first onboarding step | Insert a new `WelcomeScreen` composable as a new route (`"onboarding/welcome"`) in the existing `"onboarding"` nested nav graph; make it the `startDestination` of that graph, displacing `"onboarding/start"` (which was pointing directly to `PermissionWalkthroughScreen`). No new nav infrastructure needed. |
| TOUR-02 | User can swipe through a 5-tab feature tour with HorizontalPager and dot indicators | `HorizontalPager` + `rememberPagerState` are in `androidx.compose.foundation.pager` — **already available** via Compose BOM 2024.02.00 (Foundation 1.6.1). No new dependency. Dot indicators are a plain `Row` of `Box(CircleShape)` driven by `pagerState.currentPage`. |
| TOUR-03 | User is offered a quick setup wizard to enable Night Mode preset at end of onboarding | `LockControlViewModel.createNightPreset()` already exists and inserts a fully-configured `SoundPreset` via `SoundPresetDao`. The quick setup screen calls an equivalent method injected into a new `QuickSetupViewModel` (or directly into the screen's ViewModel); no new Room schema needed. |
| TOUR-04 | User can revisit the feature tour from the Settings tab | Add a `composable("tour/standalone")` route to the `"main"` nested graph. `SettingsScreen` receives an `onRevisitTour: () -> Unit` callback. The tour composable in this context calls `onDone: () -> Unit` which pops back to Settings. No full onboarding flow is triggered. |
</phase_requirements>

---

## Summary

Phase 3 adds three new screens to the `feature-onboarding` module — a Welcome screen, a Feature Tour screen, and a Quick Setup screen — and then exposes the tour as a standalone route reachable from Settings. All of this builds on the existing navigation and Hilt infrastructure from Phases 1 and 2 without requiring new Gradle dependencies.

The biggest architectural decision is onboarding flow ordering. The new full onboarding sequence is: Welcome → Feature Tour → Permission Walkthrough → Quick Setup → Main App. This means the existing nav graph structure inside `"onboarding"` must grow to support sequential routing between these four stages. The cleanest approach is to let each screen call `onNext()` and thread that through the graph in `MainActivity.kt`, keeping screens themselves navigation-agnostic.

The `HorizontalPager` API (TOUR-02) lives in `androidx.compose.foundation.pager`, which is part of the compose-foundation artifact already bundled via BOM 2024.02.00 (Foundation 1.6.1). No accompanist-pager or any other third-party library is needed. The dot indicator pattern is a plain Compose Row/Box idiom with no library dependency.

For TOUR-04, the "Revisit Tour" entry point in Settings navigates to a standalone `"tour/standalone"` route inside the existing `"main"` nav graph. This route renders `FeatureTourScreen` with an `onDone` callback that calls `navController.popBackStack()`. The onboarding DataStore flag is NOT re-checked or re-written — the user is already in the main app graph.

**Primary recommendation:** Keep all new composables in `feature/feature-onboarding`. Add `SoundPresetDao` as a dependency to the onboarding module (via Hilt injection) for the Night Mode quick setup; do not create a new DAO or duplicate the preset logic that already exists in `LockControlViewModel`.

---

## Standard Stack

### Core

| Library | Version | Purpose | Why Standard |
|---------|---------|---------|--------------|
| `androidx.compose.foundation.pager` | Foundation 1.6.1 (BOM 2024.02.00) | `HorizontalPager`, `rememberPagerState`, `PagerState` | Already in project via BOM; stable since Foundation 1.4.0 |
| `feature/feature-onboarding` module | existing | All new Phase 3 composables and ViewModels | Already wired into nav graph; has Hilt, Compose, lifecycle deps |
| `core/core-database` — `SoundPresetDao` | existing | Insert Night Mode preset for Quick Setup | `createNightPreset()` logic already proven in `LockControlViewModel` |
| `core/core-model` — `SoundPreset`, `PresetTrigger`, `RingerMode` | existing | Domain model for Night Mode preset | Fully defined; no schema change needed |
| `androidx.compose.material3` | BOM 2024.02.00 | All UI components on new screens | Already in feature-onboarding deps |

### Supporting

| Library | Version | Purpose | When to Use |
|---------|---------|---------|-------------|
| `androidx.compose.foundation` (shapes/layout) | BOM 2024.02.00 | `CircleShape`, `Box`, `Row` for dot indicators | Standard Compose layout — no extra dep |
| `androidx.hilt:hilt-navigation-compose` | 1.1.0 | `hiltViewModel()` for `QuickSetupViewModel` | Already in feature-onboarding deps |
| `core-database` module | existing | `SoundPresetDao` injection into onboarding module | Requires adding `implementation(project(":core:core-database"))` to feature-onboarding build.gradle.kts |

### Alternatives Considered

| Instead of | Could Use | Tradeoff |
|------------|-----------|----------|
| `androidx.compose.foundation.pager.HorizontalPager` | Accompanist Pager (`com.google.accompanist:accompanist-pager`) | Accompanist Pager is deprecated since Foundation 1.4.0 absorbed it. **Never use accompanist-pager.** |
| Custom dot indicator (hand-rolled `Row + Box`) | External indicator library (`platacard/PagerIndicator`, etc.) | Dot indicators are 10 lines of Compose code. External library adds dependency weight for zero functional gain. Build it inline. |
| New `QuickSetupViewModel` in onboarding | Call `LockControlViewModel.createNightPreset()` directly from onboarding | Cross-module ViewModel coupling violates module boundaries. QuickSetupViewModel in feature-onboarding injects `SoundPresetDao` directly and replicates the same one-liner insert. |
| Navigate Settings → onboarding graph for revisit | Standalone `"tour/standalone"` route in main graph | Navigating back into the onboarding graph would trigger MainViewModel's DataStore check and potentially re-run the full onboarding. Standalone route is clean and has no state side effects. |

**No new Gradle dependencies.** The only build.gradle.kts change is adding `core-database` to `feature-onboarding` dependencies.

---

## Architecture Patterns

### Recommended Project Structure

```
feature/feature-onboarding/
└── src/main/java/com/usagecontrol/feature/onboarding/
    ├── OnboardingPlaceholderScreen.kt         # DELETE — was Phase 1 placeholder, now unused
    ├── PermissionStep.kt                       # KEEP — unchanged
    ├── PermissionWalkthroughScreen.kt          # KEEP — unchanged
    ├── PermissionWalkthroughViewModel.kt       # KEEP — unchanged
    ├── WelcomeScreen.kt                        # NEW — TOUR-01: value proposition + "Get Started" CTA
    ├── FeatureTourScreen.kt                    # NEW — TOUR-02: HorizontalPager, 5 slides, dot indicators, Skip
    ├── QuickSetupScreen.kt                     # NEW — TOUR-03: Night Mode one-tap offer + "Skip" option
    └── QuickSetupViewModel.kt                  # NEW — injects SoundPresetDao, calls createNightPreset()

feature/feature-settings/
└── src/main/java/com/usagecontrol/feature/settings/
    └── SettingsScreen.kt                       # MODIFY — add "Revisit tour" item + onRevisitTour callback

app/src/main/java/com/usagecontrol/
└── MainActivity.kt                             # MODIFY — expand onboarding nav graph; add standalone tour route
```

### Pattern 1: Onboarding Sequence Routing in the Nav Graph

**What:** The `"onboarding"` nested graph in `MainActivity.kt` currently has one route: `"onboarding/start"` (PermissionWalkthroughScreen). Phase 3 adds three more routes and changes the `startDestination` to `"onboarding/welcome"`.

**New route chain:**
```
"onboarding/welcome"  →  "onboarding/tour"  →  "onboarding/permissions"  →  "onboarding/quicksetup"  →  "main"
```

**When to use:** Each screen calls `onNext()` which the nav graph wires to `navController.navigate("onboarding/[next-route]")`. Back navigation from within onboarding is blocked at the `"onboarding/welcome"` root (no back destination).

```kotlin
// Source: existing pattern from Phase 1/2, extended for Phase 3
navigation(startDestination = "onboarding/welcome", route = "onboarding") {
    composable("onboarding/welcome") {
        WelcomeScreen(
            onGetStarted = { navController.navigate("onboarding/tour") },
            onSkip = {
                navController.navigate("main") {
                    popUpTo("onboarding") { inclusive = true }
                }
            },
        )
    }
    composable("onboarding/tour") {
        FeatureTourScreen(
            onNext = { navController.navigate("onboarding/permissions") },
            onSkip = {
                navController.navigate("main") {
                    popUpTo("onboarding") { inclusive = true }
                }
            },
        )
    }
    composable("onboarding/permissions") {
        PermissionWalkthroughScreen(
            onComplete = { navController.navigate("onboarding/quicksetup") },
        )
    }
    composable("onboarding/quicksetup") {
        QuickSetupScreen(
            onActivate = { viewModel -> viewModel.createNightPreset(); navController.navigate("main") { popUpTo("onboarding") { inclusive = true } } },
            onSkip = { navController.navigate("main") { popUpTo("onboarding") { inclusive = true } } },
        )
    }
}
```

**Key change from Phase 2:** `PermissionWalkthroughViewModel.completeOnboarding()` currently writes `onboarding_complete = true` and sets `isComplete = true`. In Phase 3, the DataStore write must move to the **end of the onboarding graph** (QuickSetupScreen), not the end of PermissionWalkthroughScreen — because after permissions, there is still a QuickSetup step. Option A: Remove the DataStore write from `PermissionWalkthroughViewModel.completeOnboarding()` and let `QuickSetupViewModel` do it. Option B: Keep the write in PermissionWalkthrough (it is idempotent) and also let QuickSetup proceed regardless. **Option B is safer** — the `onboarding_complete` flag being written early has no user-visible side effect (user is still in the onboarding graph), and it prevents losing onboarding state if the process is killed between permissions and quick setup.

### Pattern 2: HorizontalPager with 5 Slides and Dot Indicators

**What:** `FeatureTourScreen` uses `rememberPagerState(pageCount = { 5 })` and `HorizontalPager` to create a swipeable 5-slide tour. Dot indicators are a `Row` of `Box` composables keyed to `pagerState.currentPage`. A "Skip" `TextButton` is visible on every slide.

**When to use:** Any horizontally swipeable page sequence where the page count is known at composition time.

```kotlin
// Source: https://developer.android.com/develop/ui/compose/layouts/pager
// Source: https://developer.android.com/develop/ui/compose/quick-guides/content/custom-page-indicator
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState

@Composable
fun FeatureTourScreen(
    onNext: () -> Unit,
    onSkip: () -> Unit,
) {
    val pagerState = rememberPagerState(pageCount = { 5 })
    val isLastPage = pagerState.currentPage == 4
    val coroutineScope = rememberCoroutineScope()

    Scaffold { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding),
        ) {
            // Swipeable pages
            HorizontalPager(
                state = pagerState,
                modifier = Modifier.fillMaxSize(),
            ) { page ->
                TourSlide(slideIndex = page)
            }

            // Dot indicators — bottom center
            Row(
                modifier = Modifier
                    .wrapContentHeight()
                    .fillMaxWidth()
                    .align(Alignment.BottomCenter)
                    .padding(bottom = 80.dp),
                horizontalArrangement = Arrangement.Center,
            ) {
                repeat(5) { iteration ->
                    val color = if (pagerState.currentPage == iteration)
                        MaterialTheme.colorScheme.primary
                    else
                        MaterialTheme.colorScheme.onSurface.copy(alpha = 0.3f)
                    Box(
                        modifier = Modifier
                            .padding(horizontal = 4.dp)
                            .size(8.dp)
                            .clip(CircleShape)
                            .background(color),
                    )
                }
            }

            // Skip button — always visible (top-end)
            TextButton(
                onClick = onSkip,
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(8.dp),
            ) {
                Text("Skip")
            }

            // Next / Done CTA — bottom
            Button(
                onClick = {
                    if (isLastPage) {
                        onNext()
                    } else {
                        coroutineScope.launch { pagerState.animateScrollToPage(pagerState.currentPage + 1) }
                    }
                },
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .fillMaxWidth()
                    .padding(horizontal = 24.dp, vertical = 16.dp),
            ) {
                Text(if (isLastPage) "Continue" else "Next")
            }
        }
    }
}
```

### Pattern 3: Night Mode Preset Activation (TOUR-03)

**What:** `QuickSetupViewModel` injects `SoundPresetDao` and calls the same insert logic as the existing `LockControlViewModel.createNightPreset()`. The screen shows a one-tap "Enable Night Mode" button and a "Skip" option.

**When to use:** When the onboarding module needs to write to Room but the LockControl ViewModel is out of scope.

```kotlin
// Source: mirrors existing LockControlViewModel.createNightPreset() exactly
@HiltViewModel
class QuickSetupViewModel @Inject constructor(
    private val soundPresetDao: SoundPresetDao,
    private val onboardingRepository: OnboardingRepository,
) : ViewModel() {

    fun createNightPreset(onDone: () -> Unit) {
        viewModelScope.launch {
            val preset = SoundPreset(
                name = "Night Mode",
                enabled = true,
                ringerMode = RingerMode.SILENT,
                alarmVolume = 100,
                ringVolume = 0,
                notificationVolume = 0,
                mediaVolume = 0,
                dndEnabled = true,
                triggerType = PresetTrigger.TIME_RANGE,
                triggerParams = mapOf("startTime" to "22:00", "endTime" to "07:00"),
            )
            soundPresetDao.insert(preset.toEntity())
            onDone()
        }
    }
}
```

`feature-onboarding/build.gradle.kts` needs one new line:
```kotlin
implementation(project(":core:core-database"))
```

### Pattern 4: Standalone Tour Route for "Revisit Tour" (TOUR-04)

**What:** A new `composable("tour/standalone")` added to the `"main"` nested graph in `MainActivity.kt`. `SettingsScreen` receives an `onRevisitTour: () -> Unit` callback that triggers `navController.navigate("tour/standalone")`. The standalone tour renders `FeatureTourScreen` with `onNext` and `onSkip` both calling `navController.popBackStack()`.

**When to use:** Any time a user wants to re-access a flow that is normally part of onboarding but does not affect onboarding state.

```kotlin
// In the "main" nav graph block in MainActivity.kt:
composable("tour/standalone") {
    FeatureTourScreen(
        onNext = { navController.popBackStack() },
        onSkip = { navController.popBackStack() },
    )
}

// SettingsScreen composable signature change:
@Composable
fun SettingsScreen(
    onRevisitTour: () -> Unit = {},           // NEW parameter
    viewModel: SettingsViewModel = hiltViewModel(),
)

// Settings item added in SettingsScreen:
SettingsItem(
    icon = Icons.Default.Tour,
    title = "Revisit tour",
    subtitle = "View the feature tour again",
    onClick = onRevisitTour,
)
```

**Note on bottom bar:** The existing `showBottomBar` logic in `MainActivity.kt` uses `currentRoute in AppScreen.entries.map { it.route }`. Route `"tour/standalone"` is NOT in `AppScreen.entries`, so the bottom bar automatically hides during the standalone tour. No code change needed.

### Anti-Patterns to Avoid

- **Navigating back into the onboarding graph for "Revisit Tour":** Setting `startDestination = "onboarding"` on navigate would re-trigger `MainViewModel`'s DataStore check and could write `onboarding_complete = false` in edge cases. Use a standalone route in `"main"` graph instead.
- **Calling `PermissionWalkthroughScreen` from the standalone tour:** The standalone tour shows only `FeatureTourScreen` — no permissions, no quick setup. Onboarding screens are not re-entered.
- **Using accompanist-pager:** Deprecated and removed from accompanist. The project's Compose Foundation version (1.6.1 via BOM 2024.02.00) has the stable `HorizontalPager` directly in `androidx.compose.foundation.pager`.
- **Storing tour slide content in the ViewModel:** Slide copy (titles, descriptions) is presentation data; it lives in the Composable or in string resources, not in ViewModel state. No ViewModel is needed for `FeatureTourScreen` unless you want to persist "which slide was last viewed."
- **Blocking onboarding completion on quick setup:** If the user skips Night Mode, `onboarding_complete` must still be written. Never gate the DataStore write on preset activation.

---

## Don't Hand-Roll

| Problem | Don't Build | Use Instead | Why |
|---------|-------------|-------------|-----|
| Horizontal swipeable pages | Custom drag gesture + Animatable offset | `HorizontalPager` from Foundation | Handles fling, accessibility (TalkBack swipe navigation), RTL, edge-case scroll physics |
| Page dot indicators | External indicator library | Inline `Row + Box(CircleShape)` | 10 lines of code; matches Material color scheme; no dependency |
| Night Mode preset creation | New DAO method or new entity | Existing `SoundPresetDao.insert()` + existing `SoundPreset` model | Logic already proven in `LockControlViewModel.createNightPreset()`; identical insert |

**Key insight:** Phase 3 is primarily a UI composition phase. The data layer (Room, DataStore) and navigation infrastructure are fully built. The work is assembling composables and wiring navigation callbacks.

---

## Common Pitfalls

### Pitfall 1: Onboarding DataStore Write Timing

**What goes wrong:** `PermissionWalkthroughViewModel.completeOnboarding()` writes `onboarding_complete = true` and sets `isComplete = true`. If onboarding flows into QuickSetup after permissions, the DataStore write now happens before the final step. If process is killed after the write but before QuickSetup is shown, the user never sees QuickSetup again.

**Why it happens:** The original Phase 2 design treated permissions as the last onboarding step.

**How to avoid:** Keep the DataStore write in `PermissionWalkthroughViewModel` (it is idempotent and harmless to write early) BUT modify `PermissionWalkthroughScreen`'s `onComplete` callback to navigate to `"onboarding/quicksetup"` rather than to `"main"`. The write occurring before QuickSetup is acceptable because if the app is killed and re-launched, the user goes directly to main app — missing Quick Setup once is an acceptable edge case (user can do it from Settings/Controls).

**Warning signs:** If the user never sees the Quick Setup screen, check whether `isComplete = true` is navigating directly to `"main"` before the graph reaches `"onboarding/quicksetup"`.

### Pitfall 2: Bottom Bar Visibility During Standalone Tour

**What goes wrong:** If `"tour/standalone"` accidentally ends up in the `AppScreen` enum (e.g., someone adds it to enable tab-level access), the bottom bar will show during the tour, which is visually broken.

**Why it happens:** `showBottomBar` uses an allowlist: `currentRoute in AppScreen.entries.map { it.route }`. As long as `"tour/standalone"` is NOT in `AppScreen`, the bottom bar is hidden automatically.

**How to avoid:** Never add `"tour/standalone"` to the `AppScreen` enum. It is a modal-style destination, not a tab.

### Pitfall 3: `PagerState.currentPage` vs `settledPage` for "Next" Button Label

**What goes wrong:** Using `pagerState.currentPage` to decide whether to show "Next" or "Continue" works correctly for most gestures, but during a fling animation `currentPage` may not yet reflect the final resting page. The label can briefly flicker.

**Why it happens:** `currentPage` is the nearest page to the snap position during animation; `settledPage` is the stable value after animation completes.

**How to avoid:** For the "last page" check (`isLastPage = pagerState.currentPage == 4`), `currentPage` is fine — a brief label flicker during fling is imperceptible. If perfect stability is required, use `pagerState.settledPage == 4`. For most onboarding UIs, `currentPage` is the standard approach.

### Pitfall 4: SoundPreset Duplicate on "Revisit Tour"

**What goes wrong:** A user finishes onboarding (Night Mode created), then revisits the tour from Settings. The tour itself does NOT include Quick Setup, so no duplicate issue there. But if the QuickSetupScreen were ever reached a second time and the user taps "Enable Night Mode" again, a second `SoundPreset` named "Night Mode" is inserted (Room auto-generates a new `id`).

**Why it happens:** `SoundPresetDao.insert()` always inserts — no upsert or uniqueness constraint on `name`.

**How to avoid:** TOUR-04 only revisits `FeatureTourScreen`, NOT `QuickSetupScreen`. The standalone route is `"tour/standalone"`, not the full quick setup. No duplicate risk as long as the standalone tour does not include the Quick Setup card. If Quick Setup is re-exposed in the future, add a `@Query("SELECT * FROM sound_presets WHERE name = :name")` guard first.

### Pitfall 5: Skip on Every Tour Slide Must Skip the Entire Tour

**What goes wrong:** Placing the "Skip" button only on some slides violates TOUR-02 ("a visible skip option on every slide"). If the Skip button is hidden on the last slide (because the Next button changes to "Continue"), users may feel trapped.

**Why it happens:** Common mistake to remove Skip when "Continue" appears.

**How to avoid:** Keep the `TextButton("Skip")` visible on all 5 slides regardless of `pagerState.currentPage`. On the last slide, both "Skip" and "Continue" are present. Tapping either goes to the next onboarding step. This is standard iOS/Android tour practice.

---

## Code Examples

Verified patterns from official sources:

### HorizontalPager with rememberPagerState

```kotlin
// Source: https://developer.android.com/develop/ui/compose/layouts/pager
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState

val pagerState = rememberPagerState(pageCount = { 5 })

HorizontalPager(state = pagerState) { page ->
    // page: Int 0..4
    TourSlide(slideIndex = page)
}
```

### Dot Indicators

```kotlin
// Source: https://developer.android.com/develop/ui/compose/quick-guides/content/custom-page-indicator
Row(
    modifier = Modifier
        .wrapContentHeight()
        .fillMaxWidth()
        .align(Alignment.BottomCenter)
        .padding(bottom = 80.dp),
    horizontalArrangement = Arrangement.Center,
) {
    repeat(pagerState.pageCount) { iteration ->
        val color = if (pagerState.currentPage == iteration)
            MaterialTheme.colorScheme.primary
        else
            MaterialTheme.colorScheme.onSurface.copy(alpha = 0.3f)
        Box(
            modifier = Modifier
                .padding(horizontal = 4.dp)
                .size(8.dp)
                .clip(CircleShape)
                .background(color),
        )
    }
}
```

### Programmatic Page Navigation

```kotlin
// Source: https://developer.android.com/develop/ui/compose/layouts/pager
val coroutineScope = rememberCoroutineScope()

// Animate to next page
coroutineScope.launch {
    pagerState.animateScrollToPage(pagerState.currentPage + 1)
}
```

### Night Mode Preset Insert (mirrors LockControlViewModel.createNightPreset())

```kotlin
// Source: /feature/feature-lockcontrol/.../LockControlViewModel.kt (existing project code)
// Used verbatim in QuickSetupViewModel
val preset = SoundPreset(
    name = "Night Mode",
    enabled = true,
    ringerMode = RingerMode.SILENT,
    alarmVolume = 100,
    ringVolume = 0,
    notificationVolume = 0,
    mediaVolume = 0,
    dndEnabled = true,
    triggerType = PresetTrigger.TIME_RANGE,
    triggerParams = mapOf("startTime" to "22:00", "endTime" to "07:00"),
)
soundPresetDao.insert(preset.toEntity())
```

### Standalone Tour Navigation Wiring

```kotlin
// In "main" nav graph in MainActivity.kt:
composable("tour/standalone") {
    FeatureTourScreen(
        onNext = { navController.popBackStack() },
        onSkip = { navController.popBackStack() },
    )
}

// In SettingsScreen calling site in MainActivity.kt:
composable(AppScreen.Settings.route) {
    SettingsScreen(
        onRevisitTour = { navController.navigate("tour/standalone") },
    )
}
```

---

## State of the Art

| Old Approach | Current Approach | When Changed | Impact |
|--------------|------------------|--------------|--------|
| Accompanist Pager (`accompanist-pager`) | `androidx.compose.foundation.pager.HorizontalPager` | Foundation 1.4.0 (Compose BOM ~2023.06) | Accompanist is deprecated; use Foundation directly |
| Accompanist Page Indicators | Inline `Row + Box(CircleShape)` | Same as above | No library needed; 10 lines inline |
| `pagerState.pageCount` (old API) | `rememberPagerState(pageCount = { N })` lambda form | Foundation 1.5.0 | Lambda form allows dynamic page counts; old `pageCount: Int` param removed |

**Deprecated/outdated:**
- `com.google.accompanist:accompanist-pager`: Deprecated; do not add to this project. Foundation has full replacement.
- `accompanist-pager-indicators`: Deprecated alongside accompanist-pager.
- `PagerState(pageCount: Int)` direct constructor: Replaced by `rememberPagerState(pageCount = { N })` in stable Foundation APIs.

---

## Open Questions

1. **Welcome screen content — what is the app's core value proposition copy?**
   - What we know: Requirements say "communicates the app's core value proposition" but provide no copy
   - What's unclear: The exact headline, subhead, and CTA text for WelcomeScreen
   - Recommendation: Planner should include a task to write copy inline in the composable (not in strings.xml for now); placeholder text is fine for Phase 3 since this is a dev milestone, not a marketing launch

2. **Feature Tour slide content — what are the 5 slides?**
   - What we know: TOUR-02 specifies 5 slides but does not define what each slide covers
   - What's unclear: Whether slides map to the 5 app tabs (Notifications, Screen Time, Controls, Insights, Settings) or to 5 features
   - Recommendation: Map one slide per main feature tab (Notifications, Screen Time, Controls, Insights, plus one for Presets/Rules). Content can be title + one-sentence description + an icon. Plan a task to define the 5-slide content structure.

3. **Should `PermissionWalkthroughScreen.onComplete` navigate to QuickSetup or call `completeOnboarding()`?**
   - What we know: Currently it calls `completeOnboarding()` which writes DataStore and sets `isComplete = true`
   - What's unclear: Whether moving to QuickSetup requires extracting the DataStore write to `QuickSetupViewModel`
   - Recommendation: Keep the DataStore write where it is (idempotent, harmless to write early). Change only the nav callback: `onComplete` in the permissions graph node navigates to `"onboarding/quicksetup"` instead of `"main"`. `QuickSetupViewModel` does NOT write DataStore again (already written).

---

## Sources

### Primary (HIGH confidence)

- [developer.android.com — Pager in Compose](https://developer.android.com/develop/ui/compose/layouts/pager) — HorizontalPager API, rememberPagerState signature, programmatic navigation
- [developer.android.com — Custom page indicator](https://developer.android.com/develop/ui/compose/quick-guides/content/custom-page-indicator) — Dot indicator pattern (Row + Box + CircleShape)
- [developer.android.com — Compose BOM mapping](https://developer.android.com/jetpack/compose/bom/bom-mapping) — Confirmed Foundation 1.6.1 in BOM 2024.02.00; HorizontalPager stable since Foundation 1.4.0
- Project source: `/app/src/main/java/com/usagecontrol/MainActivity.kt` — exact nav graph structure that Phase 3 extends
- Project source: `/feature/feature-lockcontrol/.../LockControlViewModel.kt` — `createNightPreset()` implementation used verbatim
- Project source: `/core/core-database/src/main/java/com/usagecontrol/core/database/dao/SoundPresetDao.kt` — DAO interface to inject
- Project source: `/gradle/libs.versions.toml` — confirms composeBom = "2024.02.00", no accompanist present

### Secondary (MEDIUM confidence)

- [developer.android.com — Navigation with Compose](https://developer.android.com/develop/ui/compose/navigation) — Nested graph patterns, popUpTo/popBackStack behavior (verified against existing nav graph in project)

### Tertiary (LOW confidence)

- None. All findings verified against official docs or project source.

---

## Metadata

**Confidence breakdown:**
- Standard stack: HIGH — verified against BOM mapping and existing project build files
- Architecture: HIGH — verified against existing nav graph and Phase 1/2 patterns
- HorizontalPager API: HIGH — verified against official Android Developers documentation
- Night Mode preset: HIGH — `createNightPreset()` exists verbatim in project; DAO is proven
- Pitfalls: HIGH — derived from existing project decisions and official API behavior

**Research date:** 2026-02-28
**Valid until:** 2026-03-28 (stable APIs; 30-day window)
