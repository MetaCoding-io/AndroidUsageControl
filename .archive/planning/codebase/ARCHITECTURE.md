# Architecture

**Analysis Date:** 2026-02-28

## Pattern Overview

**Overall:** Modular Clean Architecture with Layered Service Model

**Key Characteristics:**
- Multi-module organization with clear separation of concerns (Core, Feature, Service)
- Dependency Injection via Hilt for loose coupling
- Reactive data flows using Kotlin Coroutines and StateFlow
- Declarative UI using Jetpack Compose
- Domain-driven design with Rule Engine as central abstraction
- Foreground services for continuous background monitoring

## Layers

**Presentation (Feature Modules):**
- Purpose: Render UI and handle user interactions for specific features
- Location: `feature/feature-*/src/main/java/com/usagecontrol/feature/*/`
- Contains: Composable screens, ViewModels (MVVM pattern), navigation logic
- Depends on: Core layers (model, database, rules, UI components)
- Used by: App module for navigation assembly

**Domain (Core Rules & Models):**
- Purpose: Business logic for rule evaluation and action execution
- Location: `core/core-rules/` and `core/core-model/`
- Contains: RuleEngine, ActionExecutor, ConditionEvaluator, domain data classes
- Depends on: Database layer for rule retrieval
- Used by: Service modules for decision-making

**Data Access (Core Database):**
- Purpose: Data persistence and retrieval via Room ORM
- Location: `core/core-database/src/main/java/com/usagecontrol/core/database/`
- Contains: Database entities, DAOs, migrations, mappers (entity ↔ model conversion)
- Depends on: Core model layer
- Used by: Feature ViewModels, RuleEngine, Service modules

**Service Layer (Background Workers):**
- Purpose: Continuous monitoring and action execution (background foreground services)
- Location: `service/service-*/src/main/java/com/usagecontrol/service/*/`
- Contains: Long-running services (UsageMonitorService, ContextMonitorService), action handlers
- Depends on: Core layers (database, rules, model)
- Used by: App startup, Rule evaluation flows

**UI Components (Core UI):**
- Purpose: Reusable Compose components and theming
- Location: `core/core-ui/src/main/java/com/usagecontrol/core/ui/`
- Contains: Common composables, theme, charts, styling utilities
- Depends on: Nothing (leaf module)
- Used by: All feature modules

**Utilities (Core Common):**
- Purpose: Cross-cutting utilities and helpers
- Location: `core/core-common/src/main/java/com/usagecontrol/core/common/`
- Contains: TimeUtils, AppNameResolver, date formatting
- Depends on: Nothing (leaf module)
- Used by: All layers

**Application (App Module):**
- Purpose: Bootstrap and navigation assembly
- Location: `app/src/main/java/com/usagecontrol/`
- Contains: UsageControlApp (Hilt bootstrap), MainActivity (navigation root), BootReceiver
- Depends on: All core, feature, and service modules
- Used by: Android system (entry point)

## Data Flow

**Rule Evaluation & Execution Flow:**

1. **Capture Event** - Service polls system state (UsageMonitorService, ContextMonitorService)
2. **Build Context** - Creates EvaluationContext with current state (app package, WiFi, charging, etc.)
3. **Query Rules** - RuleEngine retrieves enabled rules for pillar (NOTIFICATION, SCREEN_TIME, LOCK_SOUND) from database
4. **Evaluate Conditions** - ConditionEvaluator checks if all conditions in rule match the context
5. **Execute Actions** - ActionExecutor dispatches matched rules to registered ActionHandlers
6. **Handle Action** - Pillar-specific handler executes (block overlay, sound profile change, etc.)

**State Management:**

- **ViewModels** store UI state in StateFlow (mutable) and expose as StateFlow (immutable)
- **Database flows** (`getByDateFlow`, `getByPillarFlow`) emit updates when data changes
- **Services** poll at fixed intervals and publish via database inserts (eventual consistency)
- **Hilt** provides singleton instances (RuleEngine, ActionExecutor, DAOs)

## Key Abstractions

**RuleEngine:**
- Purpose: Core decision-making engine that finds matching rules for a pillar and context
- Location: `core/core-rules/src/main/java/com/usagecontrol/core/rules/RuleEngine.kt`
- Pattern: Dependency injection + query builder pattern
- Usage: Services call `findMatchingRules(pillar, context)` to get applicable rules

**ActionExecutor:**
- Purpose: Plugin-style action handler registry and executor
- Location: `core/core-rules/src/main/java/com/usagecontrol/core/rules/ActionExecutor.kt`
- Pattern: Handler registry with func interface ActionHandler
- Usage: Services register handlers per ActionType, then call `executeAll(rules)` to run actions

**ConditionEvaluator:**
- Purpose: Evaluates rule conditions against current context
- Location: `core/core-rules/src/main/java/com/usagecontrol/core/rules/ConditionEvaluator.kt`
- Pattern: Recursive group evaluation (AND within groups, OR between groups)
- Usage: Called by RuleEngine to filter rules

**EvaluationContext:**
- Purpose: Immutable snapshot of system state at decision time
- Location: `core/core-rules/src/main/java/com/usagecontrol/core/rules/EvaluationContext.kt` (inferred)
- Data: package name, WiFi SSID, charging status, time of day, calendar event, etc.
- Usage: Passed to condition evaluator for matching

**Rule Model Hierarchy:**
- `Rule` (domain): id, name, enabled, priority, pillar, conditions, conditionGroups, actions
- `RuleEntity` (database): serialized JSON for complex fields (conditionGroupsJson)
- Pattern: Mappers convert entity ↔ domain with `toModel()` and `toEntity()` extensions

## Entry Points

**MainActivity:**
- Location: `app/src/main/java/com/usagecontrol/MainActivity.kt`
- Triggers: User launches app from home screen
- Responsibilities: Initialize Compose theme, assemble navigation graph, start monitoring services

**UsageMonitorService:**
- Location: `service/service-usage/src/main/java/com/usagecontrol/service/usage/UsageMonitorService.kt`
- Triggers: Started by MainActivity.startMonitoringServices() or BootReceiver on device boot
- Responsibilities: Poll UsageStatsManager every 60s, store usage records, evaluate SCREEN_TIME rules, launch block overlays

**ContextMonitorService:**
- Location: `service/service-context/src/main/java/com/usagecontrol/service/context/ContextMonitorService.kt`
- Triggers: Started by MainActivity or BootReceiver
- Responsibilities: Capture WiFi/charging/calendar state every 5min, evaluate LOCK_SOUND rules, execute sound profiles/device lock

**NotificationCaptureService:**
- Location: `service/service-notification/src/main/java/com/usagecontrol/service/notification/NotificationCaptureService.kt`
- Triggers: System binds when user grants Notification Listener permission
- Responsibilities: Intercept posted notifications, evaluate NOTIFICATION rules, filter or dismiss notifications

**BootReceiver:**
- Location: `app/src/main/java/com/usagecontrol/BootReceiver.kt`
- Triggers: BOOT_COMPLETED broadcast after device restart
- Responsibilities: Re-start monitoring services to resume background operations

## Error Handling

**Strategy:** Graceful degradation with silent failure

**Patterns:**
- Services catch all exceptions in polling loops: `try { ... } catch (_: Exception) { }`
- Individual action execution failures don't stop other actions
- Missing handlers for action types fail individually without crashing
- Permission errors in services logged but not escalated (permissions may not be granted yet)
- Database operations fail-safe with null returns or empty lists

## Cross-Cutting Concerns

**Logging:** Not explicitly configured; uses Android default logcat via Try/catch blocks that suppress errors

**Validation:**
- UI: ViewModel validates rule name and actions before save (`if (name.isBlank() || actions.isEmpty())`)
- Service: EvaluationContext implicit validation through type safety

**Authentication:**
- No user accounts; app runs locally on device
- Permission-based access control via Android manifest (BIND_NOTIFICATION_LISTENER_SERVICE, PACKAGE_USAGE_STATS, etc.)
- Device Admin receiver for lock timeout functionality

**Concurrency:**
- Services use `CoroutineScope(SupervisorJob() + Dispatchers.IO)` for background work
- ViewModels use `viewModelScope` for lifecycle-aware async operations
- Database access is async via coroutines with `suspend` functions
- StateFlow provides thread-safe reactive updates

---

*Architecture analysis: 2026-02-28*
