---
id: T03
parent: S01
milestone: M007
key_files:
  - core/core-ui/src/main/java/com/usagecontrol/core/ui/theme/Theme.kt
  - core/core-ui/src/main/java/com/usagecontrol/core/ui/theme/Color.kt
  - core/core-ui/src/main/java/com/usagecontrol/core/ui/theme/LaneColors.kt
  - core/core-ui/src/main/java/com/usagecontrol/core/ui/theme/Type.kt
  - core/core-ui/src/main/java/com/usagecontrol/core/ui/theme/Shape.kt
  - core/core-ui/src/test/java/com/usagecontrol/core/ui/theme/LaneColorsTest.kt
  - core/core-ui/src/main/res/font/manrope.ttf
  - core/core-ui/src/main/res/font/dm_mono.ttf
  - core/core-ui/src/main/res/font/dm_mono_medium.ttf
  - core/core-ui/licenses/Manrope-OFL.txt
  - core/core-ui/licenses/DMMono-OFL.txt
  - core/core-ui/build.gradle.kts
  - app/src/main/java/com/usagecontrol/MainActivity.kt
  - feature/feature-screentime/src/main/java/com/usagecontrol/feature/screentime/BlockOverlayActivity.kt
key_decisions:
  - UsageControlTheme removed rather than aliased; all callers (MainActivity, BlockOverlayActivity) migrated to DoormanTheme
  - Dynamic colour only overrides primary-family roles; surfaces and lane colours stay Doorman's
  - DM Mono bundled as static Regular + Medium because no variable DM Mono exists; Manrope is the variable wght font
  - Lane API: Lane enum + LaneColors[lane] indexer + LocalLaneColors for T04 components; mono numerics via DoormanMonoType.hero/value/meta
duration:
verification_result: passed
completed_at:
blocker_discovered: false
---

# T03: Shipped DoormanTheme (MaterialExpressiveTheme + MotionScheme.expressive) with ink/paper/amber Color.kt, six-lane LaneColors + LocalLaneColors, Manrope/DM Mono (tnum) typography with bundled OFL fonts, 12dp/full-round shapes; replaced UsageControlTheme everywhere

**Shipped DoormanTheme (MaterialExpressiveTheme + MotionScheme.expressive) with ink/paper/amber Color.kt, six-lane LaneColors + LocalLaneColors, Manrope/DM Mono (tnum) typography with bundled OFL fonts, 12dp/full-round shapes; replaced UsageControlTheme everywhere**

## What Happened

Created core-ui theme package files: Color.kt (documented palette: ink #0E1116 dark ground with surfaceContainerLowest/Low/Container/High/Highest #0A0C10/#151922/#1B202A/#222833/#2A313D; warm paper #F7F3EC light ground with stepped containers; amber accent #FFB020 dark / #8A5A00 light, reusing the T02 icon values) building full DoormanDarkColors/DoormanLightColors schemes. LaneColors.kt: Lane enum (People, Money, Deliveries, Work, Live, Noise), @Immutable LaneColors with get(lane)/all, saturated DarkLaneColors (coral, amber, cyan, violet, lime, slate) and de-saturated LightLaneColors, laneColors(darkTheme), LocalLaneColors. Type.kt: Manrope variable font (wght 200-800, FontVariation.Settings per weight) applied to every M3 Typography style; DmMono family; DoormanMonoType.hero/value/meta with fontFeatureSettings "tnum". Shape.kt: RowShape/SheetShape 12dp, ChipShape = CircleShape, DoormanShapes (medium/large 12dp). Theme.kt: DoormanTheme(darkTheme = isSystemInDarkTheme(), dynamicColor = false) provides LocalLaneColors and wraps MaterialExpressiveTheme with MotionScheme.expressive(); when dynamicColor on API 31+ only primary/onPrimary/primaryContainer/onPrimaryContainer/inversePrimary are taken from the dynamic scheme. @OptIn(ExperimentalMaterial3ExpressiveApi) stays inside core-ui. Old UsageControlTheme removed; MainActivity.kt and feature-screentime BlockOverlayActivity.kt (also used it) now call DoormanTheme. Fonts: manrope.ttf downloaded from google/fonts (variable); DM Mono has no variable cut, so static Regular (dm_mono.ttf) and Medium (dm_mono_medium.ttf) were bundled from the system DM Mono package. OFL licence texts saved to core/core-ui/licenses/. Added testImplementation(libs.junit) to core-ui and LaneColorsTest (5 tests).

## Verification

:core:core-ui:testDebugUnitTest passed (5 tests, 0 failures); assembleDebug passed and the APK contains res/font/manrope.ttf, dm_mono.ttf, dm_mono_medium.ttf; no UsageControlTheme references remain in Kotlin sources.

## Verification Evidence

| # | Command | Exit Code | Verdict | Duration |
|---|---------|-----------|---------|----------|
| 1 | `./gradlew :core:core-ui:testDebugUnitTest` | 0 | pass | 7588ms |
| 2 | `./gradlew assembleDebug` | 0 | pass | 13556ms |

## Deviations

DM Mono is not available as a variable font, so dm_mono.ttf is the static Regular and an extra dm_mono_medium.ttf was added. BlockOverlayActivity.kt (not listed in the plan) also referenced UsageControlTheme and was migrated. testImplementation(libs.junit) added to core-ui build file. Font OFL licences stored in core/core-ui/licenses/ (res/font cannot hold .txt).

## Known Issues

None. Visual check of the theme is deferred to the T04 preview gallery.

## Files Created/Modified

- `core/core-ui/src/main/java/com/usagecontrol/core/ui/theme/Theme.kt`
- `core/core-ui/src/main/java/com/usagecontrol/core/ui/theme/Color.kt`
- `core/core-ui/src/main/java/com/usagecontrol/core/ui/theme/LaneColors.kt`
- `core/core-ui/src/main/java/com/usagecontrol/core/ui/theme/Type.kt`
- `core/core-ui/src/main/java/com/usagecontrol/core/ui/theme/Shape.kt`
- `core/core-ui/src/test/java/com/usagecontrol/core/ui/theme/LaneColorsTest.kt`
- `core/core-ui/src/main/res/font/manrope.ttf`
- `core/core-ui/src/main/res/font/dm_mono.ttf`
- `core/core-ui/src/main/res/font/dm_mono_medium.ttf`
- `core/core-ui/licenses/Manrope-OFL.txt`
- `core/core-ui/licenses/DMMono-OFL.txt`
- `core/core-ui/build.gradle.kts`
- `app/src/main/java/com/usagecontrol/MainActivity.kt`
- `feature/feature-screentime/src/main/java/com/usagecontrol/feature/screentime/BlockOverlayActivity.kt`
<!-- gsd:state-version=186:1 -->
