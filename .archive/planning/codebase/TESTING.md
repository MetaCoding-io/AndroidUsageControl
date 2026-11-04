# Testing Patterns

**Analysis Date:** 2026-02-28

## Test Framework

**Runner:**
- JUnit 4 (version 4.13.2)
- AndroidJUnit4 (version 1.1.5) for instrumented tests
- No test runner currently configured in build.gradle

**Assertion Library:**
- JUnit assertions (assertTrue, assertEquals, etc.)
- Espresso (version 3.5.1) for UI testing
- No Mockito, MockK, or Truth library in dependencies

**Run Commands:**
```bash
./gradlew test                    # Run unit tests
./gradlew connectedAndroidTest    # Run instrumented tests
./gradlew testDebug               # Run debug variant tests
```

## Test File Organization

**Location:**
- No test files currently present in codebase
- Standard Android location: `src/test/` for unit tests
- Standard Android location: `src/androidTest/` for instrumented tests

**Naming:**
- Recommended convention (not yet applied): `{ClassName}Test.kt`
- Example: `RuleEngineTest.kt`, `ConditionEvaluatorTest.kt`

**Structure:**
```
app/
├── src/
│   ├── main/
│   ├── test/              (unit tests - NOT PRESENT)
│   │   └── java/...
│   └── androidTest/       (instrumented tests - NOT PRESENT)
│       └── java/...
```

## Test Structure

**Suite Organization (Recommended for this project):**

```kotlin
// Example: ConditionEvaluatorTest.kt
class ConditionEvaluatorTest {
    private lateinit var evaluator: ConditionEvaluator

    @Before
    fun setUp() {
        evaluator = ConditionEvaluator()
    }

    @Test
    fun testEvaluateAppPackageCondition() {
        // Given
        val condition = RuleCondition(
            type = ConditionType.APP_PACKAGE,
            params = mapOf("packageName" to "com.example.app")
        )
        val context = EvaluationContext(packageName = "com.example.app")

        // When
        val result = evaluator.evaluate(condition, context)

        // Then
        assertTrue(result)
    }
}
```

**Patterns:**
- Use @Before for setup, @After for teardown (JUnit 4)
- Arrange-Act-Assert (Given-When-Then) structure
- Single responsibility per test method
- Descriptive test names following `test{FunctionName}{Scenario}`

**Teardown pattern:**
```kotlin
@After
fun tearDown() {
    // Clean up resources, close database, etc.
}
```

## Mocking

**Framework:** Not yet configured
- Mockito would be recommended for Java/Kotlin mocking
- MockK for Kotlin-specific mocking (coroutines, extension functions)

**Recommended addition to gradle/libs.versions.toml:**
```toml
mockito = "5.2.0"
mockk = "1.13.8"
```

**Patterns to implement:**

For Room DAOs:
```kotlin
@get:Rule
val instantTaskExecutorRule = InstantTaskExecutorRule()

@Test
fun testGetAllFlow() {
    val testRule = Rule(id = 1, name = "Test", pillar = RulePillar.NOTIFICATION)

    // Would need mock RuleDao or test database
    val result = ruleDao.getAllFlow().firstOrNull()
    assertEquals(testRule, result)
}
```

For ViewModels:
```kotlin
@get:Rule
val instantTaskExecutorRule = InstantTaskExecutorRule()

private val testDispatcher = StandardTestDispatcher()

@Before
fun setUp() {
    Dispatchers.setMain(testDispatcher)
}

@Test
fun testToggleRule() {
    val viewModel = RuleListViewModel(mockRuleDao, mockSavedStateHandle)

    viewModel.toggleRule(1L, true)
    advanceUntilIdle()

    verify(mockRuleDao).setEnabled(1L, true)
}
```

**What to Mock:**
- Database DAOs (RuleDao, NotificationDao, UsageDao)
- External services (LocationManager, UsageStatsManager)
- Navigation callbacks and lambdas
- SavedStateHandle for arguments

**What NOT to Mock:**
- Data classes and value objects (Rule, RuleCondition, UsageRecord)
- Utility functions (TimeUtils, AppNameResolver)
- Enum types (RulePillar, ActionType, ConditionType)
- Pure business logic functions (ConditionEvaluator.evaluate)

## Fixtures and Factories

**Test Data (Recommended pattern):**

```kotlin
// TestDataFactory.kt
object TestDataFactory {
    fun createRule(
        id: Long = 1,
        name: String = "Test Rule",
        pillar: RulePillar = RulePillar.NOTIFICATION,
        enabled: Boolean = true,
        conditions: List<RuleCondition> = emptyList(),
        actions: List<RuleAction> = emptyList(),
    ) = Rule(
        id = id,
        name = name,
        pillar = pillar,
        enabled = enabled,
        conditions = conditions,
        actions = actions,
    )

    fun createRuleCondition(
        type: ConditionType = ConditionType.APP_PACKAGE,
        params: Map<String, String> = mapOf("packageName" to "com.example.app"),
    ) = RuleCondition(type = type, params = params)

    fun createEvaluationContext(
        packageName: String = "com.example.app",
        notificationTitle: String? = null,
        snapshot: ContextSnapshot? = null,
    ) = EvaluationContext(
        packageName = packageName,
        notificationTitle = notificationTitle,
        snapshot = snapshot,
    )
}
```

**Location:**
- Place in `src/test/java/com/usagecontrol/testing/` directory
- Create separate TestDataFactory for each major domain (Rules, Notifications, Usage)

## Coverage

**Requirements:** None enforced currently

**Recommended targets:**
- Core business logic (RuleEngine, ConditionEvaluator, ActionExecutor): 80%+
- ViewModels: 60%+
- UI Composables: 20%+ (integration tests better suited)
- Mappers: 100% (simple but critical)

**View Coverage:**
```bash
./gradlew testDebugUnitTest --info

# Coverage report generation (after adding jacoco plugin):
./gradlew jacocoTestReport
```

## Test Types

**Unit Tests:**
- Scope: Single class/function in isolation
- Location: `src/test/`
- Examples:
  - `ConditionEvaluatorTest`: Test condition evaluation logic
  - `RuleEngineTest`: Test rule matching and sorting
  - `MapperTest`: Test entity<->model conversions
  - `TimeUtilsTest`: Test date/time formatting and calculations

**Integration Tests:**
- Scope: Multiple components working together
- Location: `src/androidTest/`
- Examples:
  - `RuleEditorViewModelIntegrationTest`: ViewModel + mock DAO
  - `RuleEngineIntegrationTest`: Engine + ConditionEvaluator
  - `DatabaseIntegrationTest`: Room operations with real database

**E2E Tests:**
- Framework: Not configured
- Would use Espresso for UI testing if implemented
- Example: Complete user flow of creating a rule

## Common Patterns

**Async Testing:**

```kotlin
@get:Rule
val instantTaskExecutorRule = InstantTaskExecutorRule()

@Test
fun testAsyncRuleLoading() {
    val viewModel = ScreenTimeViewModel(mockUsageDao, mockRuleDao)
    val testCollector = viewModel.rules.test()

    advanceUntilIdle()

    testCollector.assertValue { it.isNotEmpty() }
}
```

**Flow Testing (with turbine library - NOT YET ADDED):**

```kotlin
@Test
fun testRulesFlow() = runTest {
    val viewModel = RuleListViewModel(mockRuleDao, mockSavedStateHandle)

    viewModel.rules.test {
        awaitItem() // Get initial value
        val secondValue = awaitItem() // Get first emission
        assertEquals(expectedRules, secondValue)
    }
}
```

**Error Testing:**

```kotlin
@Test
fun testEvaluateWithMissingParam() {
    val condition = RuleCondition(
        type = ConditionType.APP_PACKAGE,
        params = emptyMap() // Missing packageName
    )
    val context = EvaluationContext(packageName = "com.example.app")

    val result = evaluator.evaluate(condition, context)

    assertFalse(result) // Should return false, not crash
}

@Test
fun testNullSafeSnapshot() {
    val context = EvaluationContext(snapshot = null)
    val condition = RuleCondition(
        type = ConditionType.CHARGING,
        params = mapOf("isCharging" to "true")
    )

    val result = evaluator.evaluate(condition, context)

    assertFalse(result) // Null-safe, returns false
}
```

## Critical Areas Needing Tests

**High Priority (no tests):**
1. `ConditionEvaluator.kt` - Core business logic with many branches
2. `RuleEngine.kt` - Rule matching and filtering
3. `Mappers.kt` - JSON parsing and serialization
4. ViewModels (RuleListViewModel, RuleEditorViewModel, ScreenTimeViewModel)
5. ActionExecutor - Rule action execution

**Medium Priority:**
1. TimeUtils - Date/time calculations
2. UsageMonitorService - Service lifecycle and polling logic
3. Database DAOs - CRUD operations

**Lower Priority:**
1. UI Composables - Better suited for Espresso tests
2. Theme and styling code

## Database Testing Strategy

**In-memory Room database (recommended):**

```kotlin
class RuleDaoTest {
    @get:Rule
    val instantTaskExecutorRule = InstantTaskExecutorRule()

    private lateinit var db: UsageControlDatabase
    private lateinit var ruleDao: RuleDao

    @Before
    fun setUp() {
        db = Room.inMemoryDatabaseBuilder(
            ApplicationProvider.getApplicationContext(),
            UsageControlDatabase::class.java
        ).allowMainThreadQueries().build()
        ruleDao = db.ruleDao()
    }

    @After
    fun tearDown() {
        db.close()
    }

    @Test
    suspend fun testInsertAndRetrieveRule() {
        val rule = RuleEntity(name = "Test", pillar = "NOTIFICATION")
        val id = ruleDao.insert(rule)

        val retrieved = ruleDao.getById(id)

        assertEquals(rule.name, retrieved?.name)
    }
}
```

---

*Testing analysis: 2026-02-28*
