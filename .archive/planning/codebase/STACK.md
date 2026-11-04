# Technology Stack

**Analysis Date:** 2026-02-28

## Languages

**Primary:**
- Kotlin 1.9.22 - Application logic, services, and data models
- XML - Android manifests and UI layouts

**Build Configuration:**
- Gradle Kotlin DSL (.kts files) - Build automation and dependency management

## Runtime

**Environment:**
- Android 10 (API 29) minimum to Android 14 (API 34) target
- Java Virtual Machine via Android Runtime (ART)
- Java Development Kit 17 - Compile target

**Package Manager:**
- Gradle 8.11.1 - Build orchestration and dependency management
- Lockfile: `gradle-wrapper.properties` and `gradle/libs.versions.toml`

## Frameworks

**Core Android:**
- AndroidX (core-ktx 1.12.0) - Modern Android framework utilities
- Jetpack Compose 2024.02.00 - Declarative UI framework
- Material 3 (1.2.0) - Design system and components
- Jetpack Navigation 2.7.7 - Fragment and route navigation

**Dependency Injection:**
- Hilt 2.50 - Compile-time dependency injection framework
- KSP (Kotlin Symbol Processing) 1.9.22-1.0.17 - Code generation for Hilt

**Data & State Management:**
- Room 2.6.1 - SQLite abstraction and ORM
- Kotlin Coroutines 1.7.3 - Asynchronous programming
- Jetpack Lifecycle 2.7.0 - Lifecycle-aware components

**Background Processing:**
- WorkManager 2.9.0 - Scheduled and deferred task execution
- Jetpack Compose Runtime - State management in UI

**Testing:**
- JUnit 4.13.2 - Unit test framework
- AndroidX JUnit Extension 1.1.5 - Android-specific testing utilities
- Espresso 3.5.1 - UI testing framework

**Build/Dev Tools:**
- Android Gradle Plugin 8.2.2 - Android build system
- Kotlin Compiler Extension 1.5.8 - Compose compatibility

## Key Dependencies

**Critical Infrastructure:**
- Room runtime (2.6.1) - Local SQLite database persistence for all app data
- Kotlin Coroutines core (1.7.3) - Async/concurrent operations across services
- Jetpack Compose UI - Entire UI layer rendered through Compose

**Location & Context:**
- Google Play Services Location 21.1.0 - Geofencing and fine/coarse location access
- AndroidX Activity Compose (1.8.2) - Activity integration with Compose

**Backend Services:**
- Hilt Navigation Compose (1.1.0) - DI-integrated navigation in Compose
- Hilt Work (1.1.0) - DI integration for WorkManager background tasks

## Configuration

**Environment:**
- Java home: `/usr/lib/jvm/java-17-openjdk-amd64` (Linux development)
- Max JVM heap: 2048m
- Kotlin code style: official
- Non-transitive R classes enabled (Android optimization)

**Build Configuration:**
- SDK versions: `gradle/libs.versions.toml` - Centralized version catalog
- App namespace: `com.usagecontrol`
- Application ID: `com.usagecontrol`
- Version: 0.1.0 (versionName), code 1

**Database:**
- Database filename: `usage_control.db` (Room SQLite)
- Location: `/data/data/com.usagecontrol/databases/` (on-device storage)
- Migrations: 2 migrations tracked in `DatabaseModule.kt` (versions 1→2, 2→3)

**ProGuard:**
- Release builds include default Android ProGuard rules
- ProGuard file: `app/proguard-rules.pro`
- Minification: disabled in release build

## Platform Requirements

**Development:**
- JDK 17+ (minimum `JavaVersion.VERSION_17`)
- Android SDK compileSdk 34
- Android Build Tools 34.0.0
- Gradle Wrapper: 8.11.1 (bundled)
- IDE: Android Studio Panda or later (tested with Panda)

**Production:**
- Deployment target: Android 10 (API 29) minimum
- Target: Android 14 (API 34)
- Installation: APK sideload or system app installation
- Storage: On-device SQLite database (no cloud sync)

**Permissions & Services:**
- Android services: NotificationListenerService, UsageStatsManager, WorkManager
- System access: SYSTEM_ALERT_WINDOW, PACKAGE_USAGE_STATS, BIND_NOTIFICATION_LISTENER_SERVICE
- Hardware: GPS/Location, Bluetooth, WiFi scanning

---

*Stack analysis: 2026-02-28*
