# Coding Conventions

**Analysis Date:** 2026-02-28

## Naming Patterns

**Files:**
- PascalCase for Kotlin classes and composables: `RuleListScreen.kt`, `RuleEditorViewModel.kt`, `ConditionEvaluator.kt`
- camelCase for utility/helper files: `Mappers.kt`, `RuleDisplayHelpers.kt`
- Package-based organization following Android conventions: `com.usagecontrol.{module}.{feature}`

**Functions:**
- camelCase for all function names: `toggleRule()`, `deleteRule()`, `evaluateRule()`, `findMatchingRules()`
- Private helper functions prefixed with underscore pattern in state: `_selectedDate`, `_state`, `_dailyTotals`
- Composable functions follow same camelCase naming: `RuleListItem()`, `EmptyState()`, `StatCard()`
- Extension functions use camelCase with clear purpose: `toModel()`, `toEntity()`, `displayName()`

**Variables:**
- camelCase for all variables: `pillar`, `selectedDate`, `blockedThisCycle`, `viewModel`
- Private mutable state uses underscore prefix: `_selectedApp: MutableStateFlow`, `_state: MutableStateFlow`
- Public immutable state flow without underscore: `selectedApp: StateFlow`, `state: StateFlow`
- Constants in companion objects use UPPER_SNAKE_CASE: `NOTIFICATION_ID`, `CHANNEL_ID`, `POLL_INTERVAL_MS`

**Types:**
- PascalCase for data classes: `RuleEditorState`, `EvaluationContext`, `DailyUsageTotalRow`
- Enum values in UPPER_SNAKE_CASE: `RulePillar.NOTIFICATION`, `ActionType.DISMISS_NOTIFICATION`, `ConditionType.APP_PACKAGE`
- Type parameters use single capital letters: `T`, `E`

## Code Style

**Formatting:**
- No explicit formatter configured; rely on Android Studio defaults
- 4-space indentation (standard Kotlin/Java convention)
- Braces on same line for control flow: `if (rules.isEmpty()) { ... }`
- Max line length observed is typically 100-120 characters

**Linting:**
- No dedicated linter (ESLint, ktlint, detekt) configured
- Gradle 8.x enforces standard build practices
- Kotlin compiler version 1.9.22 provides compile-time checks

## Import Organization

**Order:**
1. Package declaration
2. Android framework imports (androidx.*)
3. Third-party library imports (com.google.*, kotlinx.*, org.*)
4. Project/module imports (com.usagecontrol.*)
5. Kotlin stdlib imports (kotlin.*, java.*)

**Example from `RuleListViewModel.kt`:**
```kotlin
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.usagecontrol.core.database.dao.RuleDao
import com.usagecontrol.core.database.toEntity
import com.usagecontrol.core.database.toModel
import com.usagecontrol.core.model.Rule
import com.usagecontrol.core.model.RulePillar
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject
```

**Path Aliases:**
- Not explicitly configured
- Relative package imports used throughout

## Error Handling

**Patterns:**
- Try-catch with silent failure and log pattern: `catch (e: Exception) { // Log but don't crash }`
- Null-safe operators with defaults: `?.toModel()`, `?:`, `?.let`, `?.firstNotNullOfOrNull`
- Early returns for validation: `if (name.isBlank()) return`, `if (actions.isEmpty()) return`
- Optional return with `?:` fallback: `val entity = ruleDao.getById(id) ?: return@launch`
- No custom exception types observed; relies on stdlib exceptions

**Example from `UsageMonitorService.kt`:**
```kotlin
try {
    pollUsageStats()
    evaluateScreenTimeRules()
} catch (e: Exception) {
    // Log but don't crash
}
```

## Logging

**Framework:** No explicit logging framework (Timber, Logback); not implemented

**Patterns:**
- Logging not actively used in current codebase
- When needed, would use Android's native `Log` class or Timber if added

## Comments

**When to Comment:**
- KDoc comments on public functions explaining behavior and parameters
- Inline comments explain non-obvious logic (example: migration of flat conditions to groups in `RuleEditorViewModel.kt`)
- Business logic comments document complex evaluation rules

**JSDoc/TSDoc:**
- KDoc style used for public APIs
- `@param` and `@return` tags not consistently used
- Example from `ConditionEvaluator.kt`:
```kotlin
/**
 * Evaluate a single condition against the current context and optional notification data.
 */
fun evaluate(condition: RuleCondition, context: EvaluationContext): Boolean
```

## Function Design

**Size:**
- Typical functions 10-50 lines
- UI composables 30-80 lines
- ViewModel methods 5-20 lines

**Parameters:**
- Use named parameters for clarity in function calls
- Hilt `@Inject` for constructor dependency injection
- SavedStateHandle for navigation arguments: `savedStateHandle.get<String>("pillar")`
- Optional parameters default to null: `subtitle: String? = null`, `modifier: Modifier = Modifier`

**Return Values:**
- Suspend functions for database operations: `suspend fun getById(id: Long): RuleEntity?`
- Flow for reactive streams: `fun getAllFlow(): Flow<List<RuleEntity>>`
- StateFlow for UI state: `val rules: StateFlow<List<Rule>>`
- Direct values for synchronous operations

## Module Design

**Exports:**
- Data classes and enums exported from `core-model`: `Rule`, `RuleCondition`, `ActionType`, `RulePillar`
- Mappers exported from `core-database`: `toModel()`, `toEntity()` extensions
- ViewModels exported from feature modules with `@HiltViewModel`
- DAOs exported from `core-database` for repository pattern

**Barrel Files:**
- Mapper extensions defined in single file: `Mappers.kt` in `core-database`
- Display helpers in single file: `RuleDisplayHelpers.kt` in `feature-rules`
- UI components barrel file: `CommonComponents.kt` in `core-ui`

**Example barrel pattern from `RuleDisplayHelpers.kt`:**
```kotlin
fun RulePillar.displayName(): String = when (this) { ... }
fun ConditionType.displayName(): String = when (this) { ... }
fun ConditionType.icon(): ImageVector = when (this) { ... }
fun RuleCondition.displayParams(): String = when (this) { ... }
fun ActionType.displayName(): String = when (this) { ... }
fun Rule.conditionSummary(): String { ... }
```

## Architecture Patterns

**MVVM with Compose:**
- ViewModels manage UI state as StateFlow/MutableStateFlow
- Composables consume state via `collectAsStateWithLifecycle()`
- Hilt provides dependency injection for ViewModels and repositories

**Example from `RuleListScreen.kt`:**
```kotlin
@Composable
fun RuleListScreen(
    viewModel: RuleListViewModel = hiltViewModel(),
) {
    val rules by viewModel.rules.collectAsStateWithLifecycle()
    // UI renders rules
}
```

**Data Flow:**
- Room DAOs provide Flow<List<Entity>> for reactive updates
- Mappers convert Entity -> Model at DAO level
- ViewModels apply business logic and expose StateFlow<Model>
- Composables subscribe to StateFlow and render

**State Management:**
- Immutable data classes for models
- MutableStateFlow for internal ViewModel state
- `update { }` blocks for state mutations: `_state.update { it.copy(name = name) }`
- ViewModelScope for coroutine lifetime

## Dagger Hilt Patterns

**Injection:**
- Constructor injection with `@Inject` on ViewModel: `class RuleListViewModel @Inject constructor(ruleDao: RuleDao)`
- Field injection for Android components: `@Inject lateinit var usageDao: UsageDao`
- `@HiltViewModel` annotation marks ViewModels for injection
- `@Singleton` for services: `@Singleton class RuleEngine @Inject constructor(...)`

**Modules:**
- `DatabaseModule.kt` provides database and DAO instances
- No custom scopes beyond default (Activity, ViewModel scope)

---

*Convention analysis: 2026-02-28*
