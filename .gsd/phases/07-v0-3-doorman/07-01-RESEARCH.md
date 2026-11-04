# S01 Research: Toolchain and theme (M007 v0.3 Doorman)

## Summary

The repo is still on the v0.1/v0.2 toolchain: AGP 8.2.2, Kotlin 1.9.22, KSP 1.9.22-1.0.17, Hilt 2.50, Compose BOM 2024.02.00, material3 1.2.0, Compose compiler extension 1.5.8 (set per module via `composeOptions`), Gradle 8.11.1, compileSdk 34. material3 1.5.0-beta02 is not in the local Gradle cache (only AGP 8.2.2 is cached), so its exact compatible toolchain cannot be confirmed offline. The planner must spike it.

Existing theme is `UsageControlTheme` in `core/core-ui/.../theme/Theme.kt`: dynamic colour on Android 12+, stock M3 green seed, default typography, no fonts, no previews. `core-ui` already exists as a module (`:core:core-ui`, namespace `com.usagecontrol.core.ui`).

Launcher icon is PNG-only mipmaps (`mipmap-{m,h,xh,xxh,xxxh}dpi/ic_launcher.png`). There is no `mipmap-anydpi-v26` adaptive icon and no monochrome layer. Label is `@string/app_name` = "Usage Control". There are no font resources and no `@Preview` in the repo.

The palette, lane hues, type and shape specs are documented in `.archive/planning/research/DESIGN-REVIEW.md` section 2.2 and in `DOORMAN-MILESTONES.md` Phase 4. Decisions D010 and D013 are in `.gsd/DECISIONS.md`.

## Recommendation

1. Spike the toolchain first on a branch. Find the set of Kotlin, KSP, Hilt, AGP, Compose BOM, material3 1.5.0-beta02 and Navigation Compose versions that resolves, and record the exact versions. Do not guess them in the plan.
2. Move the Compose compiler from `composeOptions { kotlinCompilerExtensionVersion }` to the Kotlin Compose compiler Gradle plugin if Kotlin goes to 2.x. This is the single most likely build break.
3. Keep `applicationId`, Kotlin packages and namespaces unchanged (D010). Change only `app_name`, the launcher icon, the theme name, and copy.
4. Build `DoormanTheme` in `core-ui` (static dark and light schemes, `LaneColors` plus `LocalLaneColors`, typography with bundled Manrope and DM Mono, dynamic colour off by default, `MaterialExpressiveTheme`). Keep `UsageControlTheme` as a thin alias or delete it at the call site in `MainActivity.kt`.
5. Add shared components and a `ComponentGallery` preview file in `core-ui`. Use `LedDot`, `LaneChip`, `SectionHeader`, `PolicySegment`, `NotificationRow`, `BundleRow`, `HeroCount`, `ContextStrip`.

## Implementation Landscape

### Key files

| Area | Path | Notes |
|---|---|---|
| Version catalog | `/home/james/Code/AndroidUsageControl/gradle/libs.versions.toml` | `[versions]` agp 8.2.2, kotlin 1.9.22, ksp 1.9.22-1.0.17, hilt 2.50, composeBom 2024.02.00, material3 1.2.0, lifecycle 2.7.0, navigation 2.7.7, activityCompose 1.8.2, room 2.6.1 |
| Root build | `/home/james/Code/AndroidUsageControl/build.gradle.kts` | Plugins apply false only |
| Settings | `/home/james/Code/AndroidUsageControl/settings.gradle.kts` | Includes app, 5 core, 7 feature, 3 service modules; foojay toolchain resolver |
| Wrapper | `/home/james/Code/AndroidUsageControl/gradle/wrapper/gradle-wrapper.properties` | Gradle 8.11.1 |
| gradle.properties | `/home/james/Code/AndroidUsageControl/gradle.properties` | JDK 17 at `/usr/lib/jvm/java-17-openjdk-amd64`, `nonTransitiveRClass=true` |
| App build | `/home/james/Code/AndroidUsageControl/app/build.gradle.kts` | compileSdk 34, targetSdk 34, minSdk 29, versionName 0.1.0, compose composeOptions 1.5.8, `resValue` app_developer and app_copyright |
| core-ui build | `/home/james/Code/AndroidUsageControl/core/core-ui/build.gradle.kts` | Library, compileSdk 34, depends on core-model, Compose BOM, material3, icons-extended, ui-tooling-preview, debug ui-tooling. No font or resource config |
| Theme | `/home/james/Code/AndroidUsageControl/core/core-ui/src/main/java/com/usagecontrol/core/ui/theme/Theme.kt` | `UsageControlTheme(darkTheme, dynamicColor = true)`. Hand-written light and dark `lightColorScheme`/`darkColorScheme`. Green seed. Only theme file |
| Components | `/home/james/Code/AndroidUsageControl/core/core-ui/src/main/java/com/usagecontrol/core/ui/component/CommonComponents.kt` | LoadingScreen, EmptyState, StatCard, SettingsRow, UsageBar, DegradedPermissionBanner (keep; D009) |
| Charts | `/home/james/Code/AndroidUsageControl/core/core-ui/src/main/java/com/usagecontrol/core/ui/component/Charts.kt` | `SimpleLineChart` and others, Canvas-based. Reused for Apps sparklines |
| Activity | `/home/james/Code/AndroidUsageControl/app/src/main/java/com/usagecontrol/MainActivity.kt` | `setContent { UsageControlTheme { UsageControlAppContent() } }`. Bottom bar uses `AppScreen.entries` |
| Tabs enum | `/home/james/Code/AndroidUsageControl/app/src/main/java/com/usagecontrol/navigation/AppNavigation.kt` | `AppScreen` enum with title and icon. Five entries today; S03 reduces to three |
| Manifest | `/home/james/Code/AndroidUsageControl/app/src/main/AndroidManifest.xml` | `android:icon="@mipmap/ic_launcher"`, `android:label="@string/app_name"`, theme `@style/Theme.UsageControl` |
| Strings | `/home/james/Code/AndroidUsageControl/app/src/main/res/values/strings.xml` | Only `app_name` = "Usage Control" |
| App theme | `/home/john/Code/AndroidUsageControl/app/src/main/res/values/themes.xml` | Note: path is `/home/james/...`. `Theme.UsageControl` parent `android:Theme.Material.Light.NoActionBar`. Rename to `Theme.Doorman` per milestone plan 04-02 T1 |
| Launcher icons | `/home/james/Code/AndroidUsageControl/app/src/main/res/mipmap-*/ic_launcher.png` | PNG only. Need `mipmap-anydpi-v26/ic_launcher.xml` with background, foreground and monochrome layers |
| Copy with app name | `feature/feature-settings/.../SettingsScreen.kt:162`, `feature/feature-onboarding/.../WelcomeScreen.kt:27`, `feature/feature-onboarding/.../PermissionWalkthroughScreen.kt:135-146`, `service/service-usage/.../UsageMonitorService.kt:205`, `service/service-context/.../ContextMonitorService.kt:138` | Hard-coded "Usage Control" strings. Move to `strings.xml` as part of the rename. Notification titles are user-visible |
| Room | `core/core-database/build.gradle.kts` | KSP room compiler. No `exportSchema` or schema dir set. Check before bumping Room |

### Design source (read these, not the code)

- `.archive/planning/research/DESIGN-REVIEW.md` section 2.2 has the palette, lane hues, type candidates, shape and M3 Expressive components.
- `.archive/planning/research/DOORMAN-MILESTONES.md` Phase 4 (lines about 97-120) has the plan split (04-01 toolchain, 04-02 brand and theme, 04-03 components) and BRND-01..06.
- `.gsd/DECISIONS.md` D010 and D013. `.gsd/REQUIREMENTS.md` R010.

Palette from the design review (planner should encode these as tokens; exact hex values are not in the repo and must be chosen in S01):
- Ground: near-black with a cool ink bias (not #000). Four stepped surface containers. Optional "Extra dark" deferred.
- Light: warm paper ground. Same lane hues at lower chroma.
- One accent: amber ("lamp is on"). Used for primary actions, active mode, batch countdown.
- Lane hues, dark (saturated) and light (de-saturated): People coral, Money amber, Deliveries cyan, Work violet, Live lime, Noise slate.
- Type: Manrope for UI, DM Mono for times, counts and package names, with tabular numerals.
- Shape: rows and sheets 12 dp; chips and LED pill fully rounded.

### Build order

1. Toolchain spike on a branch. Bump versions in `libs.versions.toml`. Fix compile errors. Record versions and errors in the slice summary.
2. Compose compiler migration if Kotlin is 2.x. Remove `composeOptions` from `app` and `core-ui` (and every feature and service module that sets it) and apply the Compose compiler plugin.
3. `assembleDebug` green with the existing UI unchanged. Confirm Room, KSP and Hilt still generate.
4. Rename: `app_name` = "Doorman", the copy strings (move hard-coded strings to resources), `Theme.Doorman`, adaptive icon XML with a monochrome layer.
5. `DoormanTheme` plus `LaneColors` and `LocalLaneColors`. Static dark and light schemes. Dynamic-colour opt-in read from DataStore (toggle in Settings recolours accent only). `MaterialExpressiveTheme(motionScheme = MotionScheme.expressive())`. Replace `UsageControlTheme` call in `MainActivity.kt`.
6. Typography: bundle Manrope and DM Mono variable fonts in `core/core-ui/src/main/res/font/`. Define the type scale. Add a tabular-numerals setting for mono styles (`fontFeatureSettings = "tnum"` on the mono styles).
7. Shared components and `ComponentGallery` preview file covering all components in dark and light.

### Verification approach

Run from `/home/james/Code/AndroidUsageControl`:

```
./gradlew --version
./gradlew :app:dependencies --configuration debugRuntimeClasspath | grep -i "material3\|compose.bom\|ui:ui:"
./gradlew assembleDebug
./gradlew check
./gradlew :core:core-ui:assembleDebug
./gradlew :core:core-ui:compileDebugKotlin
./gradlew :app:lintDebug
./gradlew :app:installDebug   # device or emulator needed
```

Preview gallery check: Android Studio Compose preview for `ComponentGallery` in both themes, or add a debug-only screenshot test. There is no preview today, so a first preview must compile.

Launcher check: `adb shell pm dump com.usagecontrol | grep -i label` or inspect the launcher. Confirm `applicationId` is still `com.usagecontrol` and an upgrade install keeps data.

Maestro flows in `/home/james/Code/AndroidUsageControl/.maestro/` should still pass (the design doc requires it for 04-01).

## Constraints

- applicationId stays `com.usagecontrol` (D010). Upgrading keeps data and listener access.
- Kotlin packages are not renamed.
- Dynamic colour must be off by default. Lane colours are semantic.
- No new third-party UI libraries (milestone plan).
- Degraded-mode banner stays (D009).
- minSdk 29 (D003). Keep it.
- Local-first, INTERNET is not added in this slice (D014 concerns a later module).

## Common Pitfalls

- Kotlin 2.x requires the Compose compiler Gradle plugin. `composeOptions.kotlinCompilerExtensionVersion` 1.5.8 will not work.
- material3 1.5.0-beta02 may need compileSdk 35 or 36 and AGP 8.9+ (not verified). Local SDK has android-34, 35 and 36.1 platforms installed, and build-tools 34.0.0, 35.0.0, 36.1.0. The platform for compileSdk must match what the androidx artifacts require.
- Expressive APIs in beta may carry `@ExperimentalMaterial3ExpressiveApi` opt-in. Add `@OptIn` where needed, and keep it in `core-ui` rather than in every screen.
- `material-icons-extended` may not resolve on the new BOM. Check it early (milestone plan calls this out).
- `HorizontalPager` may need opt-in; `LocalLifecycleOwner` import moved; `Divider` deprecated (from the milestone plan 04-01 T2).
- Adaptive icon XML needs `mipmap-anydpi-v26`. Keep PNG fallbacks for older APIs (minSdk 29 means the adaptive icon is always used on API 26+, so the PNGs can be removed once the XML exists).
- `tonalElevation` is a no-op with a hand-built scheme. Use surface container roles.
- Variable font `tnum` support depends on the font file and Compose text rendering. Verify on device.
- Room schema export is not configured. Bumping Room or KSP may change generated code; check that `exportSchema` is not required by a migration.

## Open Risks

- Exact compatible toolchain for material3 1.5.0-beta02 is unknown offline. The spike may find that the required AGP or Kotlin is above what the team wants. This is the highest risk of the milestone (rated `risk:high`).
- Font licensing and bundling: Manrope and DM Mono are OFL. Confirm the variable TTF files are sourced and checked in under `core-ui/src/main/res/font/`.
- Hex values for the palette are not in the repo; S01 has to pick them and the design review does not give exact values. Expect a review pass with the user.
- The `ComponentGallery` preview may not render in Android Studio if preview tooling and the new Compose version disagree. Fall back to a debug-only gallery activity if previews do not compile.

## First proof

Start with the toolchain spike: a branch where `./gradlew assembleDebug` passes with material3 1.5.0-beta02 and the current UI unchanged. Only after that, do the rename, theme and components. Record the exact versions in the slice summary.
<!-- gsd:state-version=80:1 -->
