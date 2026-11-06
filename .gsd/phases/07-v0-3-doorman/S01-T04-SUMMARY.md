---
id: T04
parent: S01
milestone: M007
key_files:
  - core/core-ui/src/main/java/com/usagecontrol/core/ui/component/DoormanComponents.kt
  - core/core-ui/src/main/java/com/usagecontrol/core/ui/component/ComponentGallery.kt
  - core/core-ui/src/test/java/com/usagecontrol/core/ui/component/BundleCountLabelTest.kt
key_decisions:
  - PolicySegment is generic <T> with a label lambda so no policy domain enum is defined in core-ui ahead of M008 lanes
  - Components take strings as parameters; gallery sample copy lives only in the preview file
  - Previews worked, so there is no debug gallery activity
duration:
verification_result: passed
completed_at:
blocker_discovered: false
---

# T04: Added shared Doorman components (LedDot/LaneDot, LaneChip, SectionHeader, generic PolicySegment, NotificationRow, BundleRow, HeroCount, ContextStrip) and a ComponentGallery with dark and light @Previews under DoormanTheme

**Added shared Doorman components (LedDot/LaneDot, LaneChip, SectionHeader, generic PolicySegment, NotificationRow, BundleRow, HeroCount, ContextStrip) and a ComponentGallery with dark and light @Previews under DoormanTheme**

## What Happened

Created core/core-ui/.../component/DoormanComponents.kt. Every lane-coloured element reads LocalLaneColors, and counts/times use DoormanMonoType (DM Mono with tnum). LedDot is drawn on a Canvas: a solid dot with a radial-gradient halo when lit, an outlined ring when off. LaneDot is the lane-colour shortcut. LaneChip is a full-round (ChipShape) pill tinted with the lane colour; it is clickable only when onClick is passed, and uses a plain Surface otherwise so static chips aren't reported as disabled buttons. SectionHeader takes an optional lane dot and a mono meta. PolicySegment is generic over the option type and wraps M3 SingleChoiceSegmentedButtonRow, so S03/S04 and M008 can bring their own policy enum. NotificationRow and BundleRow share a private 12dp RowShape container on surfaceContainerLow. BundleRow's count badge uses internal bundleCountLabel (clamps values of 0 or below to "0" and caps above 99 as "99+"). HeroCount is a big mono figure with a caption and optional lane tint. ContextStrip shows an amber accent LED, the mode/context text and a mono detail. Added ComponentGallery.kt: a public ComponentGallery composable that renders every component, all six lanes, and lit and unlit dots, plus private dark and light @Preview functions wrapped in DoormanTheme(darkTheme = true/false). Preview tooling compiled fine (ui-tooling-preview was already a dependency), so no debug gallery activity was needed. CommonComponents.kt is untouched. Added a JVM test BundleCountLabelTest covering the boundaries.

## Verification

core-ui compileDebugKotlin passed with no warnings. core-ui testDebugUnitTest passed: BundleCountLabelTest 3/3 and LaneColorsTest 5/5. assembleDebug and the full check (lint plus all unit tests) passed.

## Verification Evidence

| # | Command | Exit Code | Verdict | Duration |
|---|---------|-----------|---------|----------|
| 1 | `./gradlew :core:core-ui:compileDebugKotlin` | 0 | pass | 2431ms |
| 2 | `./gradlew :core:core-ui:testDebugUnitTest` | 0 | pass | 2674ms |
| 3 | `./gradlew assembleDebug` | 0 | pass | 5235ms |
| 4 | `./gradlew check` | 0 | pass | 37421ms |

## Deviations

Added a small BundleCountLabelTest and a LaneDot helper that the plan didn't list.

## Known Issues

The gallery previews compile, but I did not render them visually. No screenshot or preview rendering was run in this headless environment.

## Files Created/Modified

- `core/core-ui/src/main/java/com/usagecontrol/core/ui/component/DoormanComponents.kt`
- `core/core-ui/src/main/java/com/usagecontrol/core/ui/component/ComponentGallery.kt`
- `core/core-ui/src/test/java/com/usagecontrol/core/ui/component/BundleCountLabelTest.kt`
<!-- gsd:state-version=161:1 -->
