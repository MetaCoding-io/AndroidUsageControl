---
id: T03
parent: S05
milestone: M007
key_files:
  - app/src/main/res/values/themes.xml
  - app/src/main/res/values-night/themes.xml
  - app/src/main/res/values/colors.xml
  - app/src/main/res/values-night/colors.xml
key_decisions:
  - The window background is a single color resource (doorman_window_background) with values/values-night overrides that mirror core-ui Paper/Ink, so the theme XML refers to one name; if core-ui background colors change, update these resources too
  - Kept enableEdgeToEdge() default SystemBarStyle.auto, which already follows the uiMode configuration, rather than hard-coding bar styles
duration:
verification_result: passed
completed_at:
blocker_discovered: false
---

# T03: Added values-night Theme.Doorman (Material dark parent, Ink #0E1116 window background, light bar icons) alongside an explicit light theme (Paper #F7F3EC, dark bar icons), so the window and system bars follow system dark mode

**Added values-night Theme.Doorman (Material dark parent, Ink #0E1116 window background, light bar icons) alongside an explicit light theme (Paper #F7F3EC, dark bar icons), so the window and system bars follow system dark mode**

## What Happened

Theme.Doorman was a bare android:Theme.Material.Light.NoActionBar with no night variant, so the window background and system bar icons stayed light in dark mode. values/themes.xml now sets the window background to @color/doorman_window_background (Paper #FFF7F3EC, which mirrors core-ui Paper) with windowLightStatusBar and windowLightNavigationBar both true. The new values-night/themes.xml uses parent android:Theme.Material.NoActionBar, the same color name overridden in values-night/colors.xml to Ink #FF0E1116 (the core-ui dark background/surface), and both light-bar flags false. Putting one color name in values and values-night means the theme refers to a single resource. minSdk is 29, so windowLightNavigationBar (API 27) needs no qualifier. MainActivity calls enableEdgeToEdge() with the default SystemBarStyle.auto, which picks bar icon contrast from the current uiMode configuration, so it agrees with the night-qualified theme. No code change was needed there. On emulator-5554 I installed the debug APK, ran `cmd uimode night yes`, launched the app and took a screencap. It showed the dark Ink background with light status and nav icons. After `cmd uimode night no` and a relaunch, it showed the Paper background with dark icons. The crash log buffer was empty in both runs, and the emulator was left in light mode.

## Verification

./gradlew assembleDebug check :app:assembleDebugAndroidTest passed (exit 0). values-night/themes.xml exists, and the doorman_window_background resource is in the APK's resources.arsc. Emulator screenshots: in night mode the window background is dark (Ink) with light status and nav bar icons; in light mode the background is Paper with dark icons. Logcat showed no crash or NoClassDefFoundError.

## Verification Evidence

| # | Command | Exit Code | Verdict | Duration |
|---|---------|-----------|---------|----------|
| 1 | `./gradlew assembleDebug check :app:assembleDebugAndroidTest --console=plain -q` | 0 | pass | 4914ms |
| 2 | `test -f app/src/main/res/values-night/themes.xml` | 0 | pass | 6ms |
| 3 | `unzip -p app/build/outputs/apk/debug/app-debug.apk resources.arsc | strings | grep -c doorman_window_background` | 0 | pass | 18ms |

## Deviations

Added values/colors.xml and values-night/colors.xml (not listed in the plan) so the window background is defined as one qualified color resource instead of hex literals in each theme.

## Known Issues

None.

## Files Created/Modified

- `app/src/main/res/values/themes.xml`
- `app/src/main/res/values-night/themes.xml`
- `app/src/main/res/values/colors.xml`
- `app/src/main/res/values-night/colors.xml`
<!-- gsd:state-version=178:1 -->
