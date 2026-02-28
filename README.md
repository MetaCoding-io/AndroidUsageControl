# AndroidUsageControl

A personal Android app that manages notifications, screen time, lock state, and sound profiles with user-defined rules and learned habits. Privacy-first — all data stays on-device.

## Quick Start (Ubuntu + Docker)

### Prerequisites

You need either a **physical Android device** or an **Android emulator**. The emulator requires KVM (hardware virtualization), which **does not work inside Docker**. So your options are:

| Approach | What you need |
|---|---|
| **A) Physical device via USB** | Android phone + USB cable + ADB on host |
| **B) Emulator on host** | Android Studio or command-line SDK on host (not in Docker) |
| **C) Build-only in Docker** | Docker (build APK, then sideload to phone) |

**Recommended for first-timers**: Option A (physical device) or Option C (Docker build + sideload).

---

### Option A: Physical Android Device

#### 1. Install Android SDK command-line tools

```bash
# Install Java 17
sudo apt update
sudo apt install openjdk-17-jdk -y

# Download Android command-line tools
mkdir -p ~/android-sdk/cmdline-tools
cd ~/android-sdk/cmdline-tools
wget https://dl.google.com/android/repository/commandlinetools-linux-11076708_latest.zip
unzip commandlinetools-linux-11076708_latest.zip
mv cmdline-tools latest

# Set environment variables (add to ~/.bashrc)
echo 'export ANDROID_HOME=~/android-sdk' >> ~/.bashrc
echo 'export PATH=$PATH:$ANDROID_HOME/cmdline-tools/latest/bin:$ANDROID_HOME/platform-tools' >> ~/.bashrc
source ~/.bashrc

# Accept licenses and install required SDK components
sdkmanager --licenses
sdkmanager "platforms;android-34" "build-tools;34.0.0" "platform-tools"
```

#### 2. Enable Developer Mode on your phone

1. Go to **Settings > About Phone**
2. Tap **Build Number** 7 times
3. Go back to **Settings > Developer Options**
4. Enable **USB Debugging**
5. Plug in your phone via USB and accept the prompt

#### 3. Build and install

```bash
cd AndroidUsageControl

# Build debug APK
./gradlew assembleDebug

# Install to connected device
adb install app/build/outputs/apk/debug/app-debug.apk
```

---

### Option B: Android Studio (Easiest)

1. Download [Android Studio](https://developer.android.com/studio) for Linux
2. Extract and run: `./android-studio/bin/studio.sh`
3. Open this project folder
4. Let it sync Gradle (takes a few minutes first time)
5. Click the green **Run** button
6. Choose your device or create an emulator

---

### Option C: Docker Build (APK only, no emulator)

This builds the APK inside Docker. You then transfer it to a phone.

```bash
# Build APK in Docker
docker run --rm -v $(pwd):/project -w /project \
  thyrlian/android-sdk:latest \
  bash -c "sdkmanager 'platforms;android-34' 'build-tools;34.0.0' && ./gradlew assembleDebug"

# The APK will be at:
# app/build/outputs/apk/debug/app-debug.apk

# Transfer to phone via ADB (install ADB on host):
sudo apt install android-tools-adb
adb install app/build/outputs/apk/debug/app-debug.apk

# Or transfer the APK file to your phone and install manually
```

---

## First Launch Setup

After installing the app, you need to grant permissions. The app will guide you via **Settings tab**, but here's what each does:

### Required Permissions

| Permission | What it does | How to grant |
|---|---|---|
| **Notification Access** | Captures all notifications | Settings > Apps > Special access > Notification access > enable UsageControl |
| **Usage Access** | Reads app screen time | Settings > Apps > Special access > Usage access > enable UsageControl |

### Optional Permissions

| Permission | What it does | How to grant |
|---|---|---|
| **Do Not Disturb** | Controls DND mode | Settings > Apps > Special access > Do Not Disturb access |
| **Display over apps** | Screen time blocking overlays | Settings > Apps > Special access > Display over other apps |
| **Location** | WiFi-based rules | Grant when prompted |

**Tip**: Open the app's **Settings tab** — each permission has a shortcut button that takes you directly to the right system settings page.

---

## App Architecture

```
5 tabs: Notifications | Screen Time | Controls | Insights | Settings
```

### Notifications Tab
- Live feed of all captured notifications
- Filter by app
- Tap to expand, "Auto-dismiss this app" to create rules
- Dashboard toggle (chart icon) shows daily trend + action breakdown

### Screen Time Tab
- Today's total screen time and per-app breakdown
- Multi-day trend chart (7/14/30 days)
- Set daily time limits per app (advisory notifications)

### Controls Tab
- Sound & lock presets (Night Mode, Meeting Mode, WiFi-based)
- Current device context (WiFi, charging, Bluetooth, calendar)
- Create custom presets with triggers (time, WiFi, manual)

### Insights Tab
- AI-powered rule suggestions based on your behavior
- Dismiss rate analytics per app
- Hourly distribution chart (when you dismiss most)
- Notification trend over time

### Settings Tab
- Permission shortcuts
- Data retention (7 days to 1 year)
- Auto-cleanup toggle (daily WorkManager job)
- Export rules to clipboard (JSON)
- Manual cleanup

---

## Building from Source

```bash
# Debug build
./gradlew assembleDebug

# Release build (unsigned)
./gradlew assembleRelease

# Run all checks
./gradlew check

# Clean build
./gradlew clean assembleDebug
```

### Requirements
- JDK 17+
- Android SDK 34 (compileSdk)
- Min Android 10 (API 29)
- Gradle 8.2+ (bundled via wrapper)

---

## Project Structure

```
AndroidUsageControl/
├── app/                          # Main app, navigation, DI
├── core/
│   ├── core-common/              # TimeUtils, AppNameResolver
│   ├── core-database/            # Room DB, DAOs, entities
│   ├── core-model/               # Domain models (Rule, Notification, etc.)
│   ├── core-rules/               # Rule engine, condition evaluator
│   └── core-ui/                  # Theme, shared components, charts
├── feature/
│   ├── feature-notifications/    # Notification feed + dashboard
│   ├── feature-learning/         # Insights, suggestions
│   ├── feature-screentime/       # Usage tracking, limits, blocking
│   ├── feature-lockcontrol/      # Sound presets, context monitor
│   ├── feature-rules/            # Rule list + editor
│   └── feature-settings/         # Settings, cleanup, export
└── service/
    ├── service-notification/     # NotificationListenerService
    ├── service-usage/            # Foreground service for usage polling
    └── service-context/          # WiFi, BT, calendar monitoring
```

## Privacy

All data is stored locally on-device using Room (SQLite). No data is transmitted anywhere. The app has no internet permission.
