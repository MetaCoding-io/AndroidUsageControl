---
id: S01
parent: M007
milestone: M007
provides:
  - DoormanTheme
  - LocalLaneColors/Lane enum
  - DoormanMonoType
  - core-ui Doorman components
requires:
  []
affects:
  []
key_files: []
key_decisions:
  - AGP 9.5 built-in Kotlin, because material3 1.5.0-beta02 requires compileSdk 37
  - material3 pinned via catalog over the BOM
  - Brand strings in per-module resources
  - Dynamic colour recolours only the primary family
patterns_established:
  - (none)
observability_surfaces:
  - none
drill_down_paths:
  []
duration: ""
verification_result: passed
completed_at: 2026-11-04T05:29:31.339Z
blocker_discovered: false
---

# S01: Toolchain and theme

**Toolchain on AGP 9.5.0/Gradle 9.8.2/Kotlin 2.4.21/compileSdk 37 with material3 1.5.0-beta02; app rebranded Doorman with LED-dot adaptive icon; DoormanTheme (MaterialExpressiveTheme), LocalLaneColors, Manrope/DM Mono type and shared core-ui components with dark/light gallery previews**

## What Happened

T01 upgraded the toolchain. material3 1.5.0-beta02 needs compileSdk 37 and AGP 9.1+, so the build moved to AGP 9 with built-in Kotlin and the Compose compiler plugin, and material3 is pinned explicitly over the BOM. T02 renamed the app to Doorman (app_name, Theme.Doorman, per-module string resources) and added an adaptive icon: ink #0E1116 background, amber #FFB020 LED dot, plus a monochrome layer. T03 shipped DoormanTheme with an ink/paper/amber palette, the six-lane LaneColors/LocalLaneColors, Manrope and DM Mono (tnum) typography and 12dp/full-round shapes; UsageControlTheme was removed. T04 added the shared components (LedDot, LaneDot, LaneChip, SectionHeader, generic PolicySegment, NotificationRow, BundleRow, HeroCount, ContextStrip) and ComponentGallery dark/light previews.

## Verification

Closer re-ran `./gradlew assembleDebug check`, which exited 0. The grep for a "Usage Control" literal in Kotlin found none. app_name=Doorman; applicationId=com.usagecontrol; ic_launcher.xml has a monochrome layer; MainActivity uses DoormanTheme; ComponentGallery has 2 @Preview functions. Unit tests: LaneColorsTest 5/5, BundleCountLabelTest 3/3 (per the task evidence).

## Requirements Advanced

- R010 — Brand, theme, lane tokens, typography and components built and compiled; on-device check pending UAT

## Requirements Validated

None.

## New Requirements Surfaced

None.

## Requirements Invalidated or Re-scoped

None.

## Operational Readiness

None.

## Deviations

None.

## Known Limitations

Previews are compiled but have not been rendered visually. The icon has not been checked on a device. After resource changes, KSP incremental builds may need a module clean.

## Follow-ups

None.

## Files Created/Modified

None.
<!-- gsd:state-version=80:1 -->
