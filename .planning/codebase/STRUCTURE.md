# Codebase Structure

**Analysis Date:** 2026-02-28

## Directory Layout

```
AndroidUsageControl/
├── app/                          # Application module (entry point, navigation assembly)
│   ├── build.gradle.kts          # App-level Gradle config
│   └── src/main/
│       ├── AndroidManifest.xml   # Permissions, services, receivers, activities
│       ├── java/com/usagecontrol/
│       │   ├── MainActivity.kt    # Navigation root, service startup
│       │   ├── UsageControlApp.kt # Hilt @HiltAndroidApp, WorkManager config
│       │   ├── BootReceiver.kt    # Service restart after device boot
│       │   └── navigation/
│       │       └── AppScreen.kt   # Navigation enum (routes, icons)
│       └── res/                  # Resources (colors, strings, drawable, etc.)
├── core/                         # Core layers (reusable, cross-feature)
│   ├── core-common/              # Shared utilities
│   │   ├── build.gradle.kts
│   │   └── src/main/java/com/usagecontrol/core/common/
│   │       ├── TimeUtils.kt      # Date/time formatting, calculations
│   │       └── AppNameResolver.kt# Package to app name mapping
│   ├── core-model/               # Domain models (data classes)
│   │   ├── build.gradle.kts
│   │   └── src/main/java/com/usagecontrol/core/model/
│   │       ├── Rule.kt           # Rule, RulePillar enum, RuleCreator enum
│   │       ├── RuleCondition.kt  # Single condition (type, params)
│   │       ├── ConditionGroup.kt # Group of conditions (AND logic)
│   │       ├── RuleAction.kt     # Action to execute (type, params)
│   │       ├── UsageRecord.kt    # App usage data (package, time)
│   │       ├── NotificationRecord.kt
│   │       ├── ContextSnapshot.kt # Device state snapshot (WiFi, charging)
│   │       ├── SoundPreset.kt    # Sound profile configuration
│   │       ├── Suggestion.kt     # ML-generated rule suggestion
│   │       └── AppDismissStats.kt
│   ├── core-database/            # Persistence layer (Room)
│   │   ├── build.gradle.kts
│   │   └── src/main/java/com/usagecontrol/core/database/
│   │       ├── UsageControlDatabase.kt # Room database, migrations (v1→v2→v3)
│   │       ├── DatabaseModule.kt       # Hilt provider for database singleton
│   │       ├── Mappers.kt             # Extension functions: toModel(), toEntity()
│   │       ├── entity/                # Room entity classes (@Entity)
│   │       │   ├── RuleEntity.kt
│   │       │   ├── UsageEntity.kt
│   │       │   ├── NotificationEntity.kt
│   │       │   ├── ContextSnapshotEntity.kt
│   │       │   ├── SuggestionEntity.kt
│   │       │   └── SoundPresetEntity.kt
│   │       └── dao/                   # Data Access Objects
│   │           ├── RuleDao.kt         # CRUD + queries (by pillar, enabled)
│   │           ├── UsageDao.kt        # Insert usage records, query by date
│   │           ├── NotificationDao.kt
│   │           ├── ContextSnapshotDao.kt
│   │           ├── SuggestionDao.kt
│   │           └── SoundPresetDao.kt
│   ├── core-rules/               # Decision engine
│   │   ├── build.gradle.kts
│   │   └── src/main/java/com/usagecontrol/core/rules/
│   │       ├── RuleEngine.kt     # @Singleton, find matching rules
│   │       ├── ConditionEvaluator.kt # Evaluate conditions recursively
│   │       ├── ActionExecutor.kt # @Singleton, handler registry, execute actions
│   │       ├── EvaluationContext.kt # (inferred) Context snapshot at eval time
│   │       └── SuggestionEngine.kt # ML/heuristic rule generation
│   └── core-ui/                  # Shared Compose components
│       ├── build.gradle.kts
│       └── src/main/java/com/usagecontrol/core/ui/
│           ├── component/
│           │   ├── CommonComponents.kt # Cards, buttons, lists, dialogs
│           │   └── Charts.kt          # Chart composables for trends
│           └── theme/
│               └── Theme.kt           # Material3 theme, colors, typography
├── feature/                      # Feature modules (pillar-specific UIs)
│   ├── feature-notifications/    # Notification filtering UI
│   │   ├── build.gradle.kts
│   │   └── src/main/java/com/usagecontrol/feature/notifications/
│   │       ├── NotificationsScreen.kt # Composable UI
│   │       └── NotificationsViewModel.kt
│   ├── feature-screentime/       # Screen time tracking & blocking
│   │   ├── build.gradle.kts
│   │   ├── src/main/AndroidManifest.xml
│   │   └── src/main/java/com/usagecontrol/feature/screentime/
│   │       ├── ScreenTimeScreen.kt
│   │       ├── ScreenTimeViewModel.kt # Usage stats, rule list, trends
│   │       └── BlockOverlayActivity.kt # Full-screen block notification
│   ├── feature-lockcontrol/      # Device lock control UI
│   │   ├── build.gradle.kts
│   │   └── src/main/java/com/usagecontrol/feature/lockcontrol/
│   │       ├── LockControlScreen.kt
│   │       └── LockControlViewModel.kt
│   ├── feature-rules/            # Rule CRUD and editor
│   │   ├── build.gradle.kts
│   │   └── src/main/java/com/usagecontrol/feature/rules/
│   │       ├── RuleListScreen.kt      # Browse rules by pillar
│   │       ├── RuleListViewModel.kt
│   │       ├── RuleEditorScreen.kt    # Create/edit rules UI
│   │       ├── RuleEditorViewModel.kt # Condition/action state management
│   │       ├── AddConditionDialog.kt  # Condition picker UI
│   │       ├── AddActionDialog.kt     # Action picker UI
│   │       └── RuleDisplayHelpers.kt  # Format conditions/actions for display
│   ├── feature-learning/         # Insights and ML suggestions
│   │   ├── build.gradle.kts
│   │   └── src/main/java/com/usagecontrol/feature/learning/
│   │       ├── LearningScreen.kt
│   │       └── LearningViewModel.kt
│   └── feature-settings/         # App configuration
│       ├── build.gradle.kts
│       └── src/main/java/com/usagecontrol/feature/settings/
│           ├── SettingsScreen.kt
│           ├── SettingsViewModel.kt
│           └── CleanupWorker.kt  # WorkManager task for old data cleanup
├── service/                      # Service modules (background workers)
│   ├── service-usage/            # App usage polling
│   │   ├── build.gradle.kts
│   │   └── src/main/java/com/usagecontrol/service/usage/
│   │       └── UsageMonitorService.kt # Foreground service, polls UsageStatsManager
│   ├── service-context/          # Device context monitoring
│   │   ├── build.gradle.kts
│   │   └── src/main/java/com/usagecontrol/service/context/
│   │       ├── ContextMonitorService.kt # Foreground service, WiFi/charging/calendar polling
│   │       ├── BluetoothScanner.kt  # Get connected Bluetooth device name
│   │       ├── CalendarReader.kt    # Read current calendar event
│   │       ├── SoundPresetApplier.kt # Apply sound profiles based on location
│   │       ├── LockControlExecutor.kt # Lock/unlock device via DeviceAdmin
│   │       ├── SoundProfileExecutor.kt # Modify audio settings
│   │       ├── ActionHandlersModule.kt # Hilt bindings for action handlers
│   │       └── DeviceAdminReceiver.kt # Request device admin privileges
│   └── service-notification/     # Notification interception
│       ├── build.gradle.kts
│       └── src/main/java/com/usagecontrol/service/notification/
│           └── NotificationCaptureService.kt # Listen for notifications, evaluate rules
├── gradle/                       # Gradle wrapper files
├── settings.gradle.kts           # Module includes, dependency resolution
├── build.gradle.kts              # Root build config
├── gradle.properties             # Gradle properties
├── gradlew / gradlew.bat         # Gradle wrapper scripts
├── README.md                     # Project documentation
├── PROJECT.md                    # Project requirements and history
└── .planning/codebase/           # GSD planning documents
```

## Directory Purposes

**app/**
- Purpose: Bootstrap application, define permissions, assemble navigation
- Contains: Hilt @HiltAndroidApp entry point, MainActivity with Compose NavHost, BootReceiver
- Key files: `AndroidManifest.xml` (permissions, services), `MainActivity.kt` (navigation root)

**core/core-common/**
- Purpose: Shared utilities across all layers
- Contains: Date/time helpers, app name resolution
- Key files: `TimeUtils.kt`, `AppNameResolver.kt`

**core/core-model/**
- Purpose: Domain models representing business concepts
- Contains: Rule, RuleCondition, ConditionGroup, RuleAction, UsageRecord, etc.
- Key files: `Rule.kt`, `RuleAction.kt`, `RuleCondition.kt`

**core/core-database/**
- Purpose: Data persistence layer with Room ORM
- Contains: Database schema, entities, DAOs, migrations, mappers
- Key files: `UsageControlDatabase.kt` (schema definition), `dao/*.kt` (CRUD), `Mappers.kt` (entity↔model)

**core/core-rules/**
- Purpose: Business logic for rule evaluation and action execution
- Contains: RuleEngine, ConditionEvaluator, ActionExecutor, SuggestionEngine
- Key files: `RuleEngine.kt`, `ActionExecutor.kt`, `ConditionEvaluator.kt`

**core/core-ui/**
- Purpose: Reusable Compose components and theming
- Contains: Common composables (cards, buttons, dialogs), chart components, Material3 theme
- Key files: `component/CommonComponents.kt`, `component/Charts.kt`, `theme/Theme.kt`

**feature/feature-notifications/**
- Purpose: Notification filtering and display UI
- Contains: Screen to view and configure notification rules
- Key files: `NotificationsScreen.kt`, `NotificationsViewModel.kt`

**feature/feature-screentime/**
- Purpose: Screen time tracking, analytics, and blocking controls
- Contains: Usage statistics display, rule management, block overlay activity
- Key files: `ScreenTimeScreen.kt`, `ScreenTimeViewModel.kt`, `BlockOverlayActivity.kt`

**feature/feature-lockcontrol/**
- Purpose: Device lock control UI
- Contains: Screen to configure lock timeout and access restrictions
- Key files: `LockControlScreen.kt`, `LockControlViewModel.kt`

**feature/feature-rules/**
- Purpose: Rule CRUD (Create, Read, Update, Delete) and editor
- Contains: Rule list view, rule editor with condition/action builders
- Key files: `RuleListScreen.kt`, `RuleEditorViewModel.kt`, `AddConditionDialog.kt`

**feature/feature-learning/**
- Purpose: Insights and ML-generated rule suggestions
- Contains: Trend analysis, suggested rules based on usage patterns
- Key files: `LearningScreen.kt`, `LearningViewModel.kt`

**feature/feature-settings/**
- Purpose: App settings and configuration
- Contains: Preferences, cleanup tasks
- Key files: `SettingsScreen.kt`, `CleanupWorker.kt`

**service/service-usage/**
- Purpose: Background app usage monitoring
- Contains: Long-running foreground service that polls system usage stats
- Key files: `UsageMonitorService.kt`

**service/service-context/**
- Purpose: Device context monitoring (WiFi, charging, calendar)
- Contains: Foreground service, context readers, action executors
- Key files: `ContextMonitorService.kt`, `BluetoothScanner.kt`, `CalendarReader.kt`

**service/service-notification/**
- Purpose: Notification interception and evaluation
- Contains: Notification listener service that evaluates rules
- Key files: `NotificationCaptureService.kt`

## Key File Locations

**Entry Points:**
- `app/src/main/java/com/usagecontrol/MainActivity.kt`: Navigation root, service startup
- `app/src/main/java/com/usagecontrol/UsageControlApp.kt`: Hilt bootstrap, WorkManager config
- `app/src/main/java/com/usagecontrol/BootReceiver.kt`: Restart services after boot

**Configuration:**
- `app/src/main/AndroidManifest.xml`: Permissions, service/receiver declarations
- `settings.gradle.kts`: Module includes
- `gradle.properties`: Gradle configuration

**Core Logic:**
- `core/core-rules/src/main/java/com/usagecontrol/core/rules/RuleEngine.kt`: Rule matching
- `core/core-rules/src/main/java/com/usagecontrol/core/rules/ActionExecutor.kt`: Action dispatch
- `core/core-database/src/main/java/com/usagecontrol/core/database/UsageControlDatabase.kt`: Schema

**Monitoring Services:**
- `service/service-usage/src/main/java/com/usagecontrol/service/usage/UsageMonitorService.kt`: Usage polling
- `service/service-context/src/main/java/com/usagecontrol/service/context/ContextMonitorService.kt`: Context polling
- `service/service-notification/src/main/java/com/usagecontrol/service/notification/NotificationCaptureService.kt`: Notification listening

**UI Features:**
- `feature/feature-rules/src/main/java/com/usagecontrol/feature/rules/RuleEditorScreen.kt`: Rule editor UI
- `feature/feature-screentime/src/main/java/com/usagecontrol/feature/screentime/ScreenTimeScreen.kt`: Usage stats UI

## Naming Conventions

**Files:**
- Kotlin files: PascalCase (`MainActivity.kt`, `RuleEngine.kt`, `TimeUtils.kt`)
- XML resources: snake_case (`activity_main.xml`, `string_common.xml`)
- Layout files: snake_case with descriptive suffix (`screen_rules.xml`)

**Directories:**
- Module directories: kebab-case (`feature-rules`, `service-usage`, `core-database`)
- Java packages: reverse domain notation with pillar/layer suffix (`com.usagecontrol.feature.rules`)
- Resource directories: lowercase standard Android (drawable, values, layout, etc.)

**Classes:**
- Activities/ViewModels: [Feature]ViewModel, [Feature]Screen, [Feature]Activity (PascalCase)
- DAO interfaces: [Entity]Dao (PascalCase)
- Entity classes: [Entity]Entity (PascalCase)
- Service classes: [Name]Service (PascalCase)
- Receiver classes: [Name]Receiver (PascalCase)
- Utility objects: [Name]Utils (PascalCase singleton objects)

**Functions:**
- Public functions: camelCase (standard Kotlin)
- Composable functions: PascalCase for top-level screens, camelCase for components
- Private/suspend functions: camelCase

**Variables:**
- Properties: camelCase
- Constants: UPPER_SNAKE_CASE (in companion objects)
- State holders: `_privateMutable` (MutableStateFlow), `publicImmutable` (StateFlow)

## Where to Add New Code

**New Feature (e.g., Screen Time Analysis):**
- Primary code: `feature/feature-screentime/src/main/java/com/usagecontrol/feature/screentime/`
- ViewModel: `ScreenTimeViewModel.kt`
- Composable UI: `ScreenTimeScreen.kt`
- Models: Add to `core/core-model/src/main/java/com/usagecontrol/core/model/`
- Database schema: Add entity and DAO to `core/core-database/`

**New Rule Pillar (e.g., BEHAVIOR_TRACKING):**
- Add to `RulePillar` enum in `core/core-model/src/main/java/com/usagecontrol/core/model/Rule.kt`
- Create feature module: `feature/feature-behavior/`
- Create service module: `service/service-behavior/`
- Register action handlers in `service/service-[name]/src/main/java/.../ActionHandlersModule.kt`

**New Utility Function:**
- General utils: `core/core-common/src/main/java/com/usagecontrol/core/common/`
- UI utils: `core/core-ui/src/main/java/com/usagecontrol/core/ui/util/` (create if needed)
- Domain logic: `core/core-rules/src/main/java/com/usagecontrol/core/rules/`

**New Action Type (e.g., BLOCK_INTERNET):**
- Add to `ActionType` enum in `core/core-model/src/main/java/com/usagecontrol/core/model/RuleAction.kt`
- Create handler: `service/service-context/src/main/java/.../[ActionName]Executor.kt`
- Register in `service/service-context/src/main/java/.../ActionHandlersModule.kt`
- Add UI picker in `feature/feature-rules/src/main/java/.../AddActionDialog.kt`

**New Condition Type (e.g., GEOFENCE):**
- Add to `ConditionType` enum (location TBD)
- Implement evaluation in `core/core-rules/src/main/java/.../ConditionEvaluator.kt`
- Add UI builder in `feature/feature-rules/src/main/java/.../AddConditionDialog.kt`

## Special Directories

**gradle/**
- Purpose: Gradle wrapper and plugin configuration
- Generated: Yes (by Gradle)
- Committed: Yes (to ensure reproducible builds)

**build/**
- Purpose: Compiled output for each module
- Generated: Yes (by Gradle build)
- Committed: No (in .gitignore)

**.gradle/**
- Purpose: Gradle daemon and cache
- Generated: Yes (by Gradle)
- Committed: No (in .gitignore)

**.idea/**
- Purpose: IntelliJ IDEA project settings
- Generated: Yes (by IDE)
- Committed: Partial (some configs tracked, cache ignored)

**res/** (in app and feature modules)
- Purpose: Android resources (layouts, drawables, strings, colors)
- Generated: No (authored)
- Committed: Yes

**AndroidManifest.xml**
- Purpose: Android app manifest (permissions, components)
- Generated: Partially (auto-merged from modules)
- Committed: Yes (source version)

---

*Structure analysis: 2026-02-28*
