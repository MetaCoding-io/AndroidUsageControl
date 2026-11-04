# External Integrations

**Analysis Date:** 2026-02-28

## APIs & External Services

**Android System Services:**
- NotificationListenerService - Captures all device notifications in real-time
  - Implementation: `com.usagecontrol.service.notification.NotificationCaptureService`
  - Requires: `BIND_NOTIFICATION_LISTENER_SERVICE` permission (user-granted in Settings)
  - Data flow: Notifications → Room DB (`NotificationEntity`)

- UsageStatsManager - Reads per-app screen time and foreground usage
  - Implementation: `com.usagecontrol.service.usage.UsageMonitorService`
  - Requires: `PACKAGE_USAGE_STATS` permission (user-granted in Settings)
  - Data flow: Usage stats polling → Room DB (`UsageEntity`)

- LocationManager & FusedLocationProviderClient - Geofencing and WiFi-based location detection
  - Integration: `com.usagecontrol.service.context.ContextMonitorService`
  - SDK: Google Play Services Location 21.1.0
  - Requires: `ACCESS_FINE_LOCATION`, `ACCESS_COARSE_LOCATION`, `ACCESS_BACKGROUND_LOCATION`
  - Used for: WiFi SSID detection, geofence-based rules

- CalendarProvider - Reads device calendar events for context-based rules
  - Requires: `READ_CALENDAR` permission
  - Used by: `com.usagecontrol.service.context.ContextMonitorService`
  - Purpose: Event-based rule triggering (e.g., "During meeting → silent mode")

- AudioManager - Volume control and Do Not Disturb mode management
  - Requires: `MODIFY_AUDIO_SETTINGS`, `ACCESS_NOTIFICATION_POLICY`
  - Used by: Sound preset and DND actions in rules engine
  - Implementation: `com.usagecontrol.core.rules.*` (rule actions)

- BluetoothAdapter - Detects connected Bluetooth devices
  - Requires: `BLUETOOTH`, `BLUETOOTH_CONNECT`
  - Used for: Device-based context (e.g., "Car connected → safe mode")
  - Implementation: `com.usagecontrol.service.context.ContextMonitorService`

- DeviceAdminReceiver - Lock screen control and device state management
  - Implementation: `com.usagecontrol.service.context.DeviceAdminReceiver`
  - Requires: `BIND_DEVICE_ADMIN` permission
  - Purpose: Auto-lock timeout adjustment, lock state control

## Data Storage

**Databases:**
- SQLite via Android Room ORM (2.6.1)
  - Provider: Room abstraction layer
  - Connection: In-process local file database
  - Database: `usage_control.db`
  - Client: Room DAOs with Kotlin Flow for reactive queries
  - Tables:
    - `notification_records` - Captured notifications with full metadata
    - `usage_records` - Per-app screen time snapshots
    - `rules` - User-defined and learned rules
    - `rule_conditions` - Rule condition mappings
    - `rule_actions` - Rule action definitions
    - `sound_presets` - Named sound/lock profiles
    - `suggestions` - ML-learned rule suggestions
    - `context_snapshots` - Device context history (location, WiFi, time, charging)

**File Storage:**
- Local filesystem only - No external storage or cloud sync
- Configuration: `.planning/` directory for development artifacts
- App data: `/data/data/com.usagecontrol/` (on-device private storage)

**Caching:**
- In-memory caching via Room DB queries with Kotlin Flow
- No external caching layer (Redis, Memcached)
- Datastore/Preferences: Not detected

## Authentication & Identity

**Auth Provider:**
- None - Local-only app, no user accounts or backend authentication
- Implementation: On-device permission grants only
- Session: Single-user device context, no multi-user support

**Permission Model:**
- User grants via Android Settings:
  - Notification Access: Settings → Apps → Special access → Notification access
  - Usage Access: Settings → Apps → Special access → Usage access
  - Location: Runtime permission prompt (optional)
  - Do Not Disturb: Settings → Apps → Special access → Do Not Disturb access
  - Display over apps: Settings → Apps → Special access → Display over other apps

## Monitoring & Observability

**Error Tracking:**
- None detected - No Sentry, Crashlytics, or similar integration

**Logs:**
- Android Logcat - Standard `Log.d()`, `Log.e()` calls in source code
- Approach: Development logging only, no persistent log aggregation
- Output: System logs via `android.util.Log`

**Debugging:**
- Android Studio debugger integration
- No performance monitoring or APM (Application Performance Monitoring)

## CI/CD & Deployment

**Hosting:**
- Local device installation - Physical Android device or emulator
- No backend/cloud deployment
- Installation method: ADB install, sideload APK, or system app

**CI Pipeline:**
- None detected - No GitHub Actions, GitLab CI, or Jenkins configuration
- Build process: Manual via `./gradlew assembleDebug` or Android Studio

**Build Outputs:**
- Debug APK: `app/build/outputs/apk/debug/app-debug.apk`
- Release APK: `app/build/outputs/apk/release/app-release.apk` (unsigned)

## Environment Configuration

**Required env vars:**
- `ANDROID_HOME` - Path to Android SDK (e.g., `~/android-sdk`)
- `JAVA_HOME` - Path to JDK 17 (e.g., `/usr/lib/jvm/java-17-openjdk-amd64`)
- Optional: `PATH` additions for `adb` and `gradle`

**Build Configuration:**
- `gradle.properties` - Gradle JVM args, AndroidX settings
- `local.properties` - Local development overrides (not committed)
- Gradle wrapper properties: `gradle/wrapper/gradle-wrapper.properties`

**Secrets location:**
- None detected - No API keys, auth tokens, or sensitive configuration
- App operates offline with no external service credentials
- All configuration is code/XML (no separate secrets management)

## Webhooks & Callbacks

**Incoming:**
- None - App is event-driven by Android system, not webhook-driven

**Outgoing:**
- None - No external API calls or webhook notifications to other services
- Internal callbacks: Android service broadcasts, content provider changes

**Event Listeners:**
- NotificationListenerService callbacks for new notifications
- UsageStatsManager change events
- WorkManager task completion callbacks
- BroadcastReceiver for BOOT_COMPLETED, system broadcasts

## Background Jobs & Scheduling

**WorkManager:**
- Used for scheduled data retention cleanup jobs
- Implementation: `com.usagecontrol.feature.settings.*`
- Schedule: Daily auto-cleanup (configurable retention: 7 days to 1 year)

**Foreground Services:**
- NotificationCaptureService - Always listening for notifications
- UsageMonitorService - Periodic polling of usage stats
- ContextMonitorService - Background monitoring of location, WiFi, Bluetooth, calendar

**Boot Receiver:**
- Implementation: `com.usagecontrol.BootReceiver`
- Purpose: Restart services after device reboot
- Intent: `android.intent.action.BOOT_COMPLETED`

## Data Privacy & Storage

**Encryption:**
- None detected - SQLite database is not encrypted (standard Android behavior)
- Future consideration: Could use SQLCipher for encrypted Room DB

**Data Retention:**
- Configurable via Settings tab: 7 days, 30 days, 90 days, 1 year
- Auto-cleanup: WorkManager daily job to delete older records
- Manual cleanup: User trigger in Settings
- No cloud backup or sync

**Network Access:**
- Internet permission: NOT DECLARED - App is entirely offline
- No network calls, no phone home, no analytics
- Privacy model: All processing on-device

---

*Integration audit: 2026-02-28*
