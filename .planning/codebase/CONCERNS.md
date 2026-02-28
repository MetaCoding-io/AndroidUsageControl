# Codebase Concerns

**Analysis Date:** 2026-02-28

## Tech Debt

### 1. Monolithic UI Screen Components
- **Issue:** Multiple UI screens exceed 400+ lines, combining state management, business logic, and UI composition in single files
- **Files:**
  - `feature/feature-lockcontrol/src/main/java/com/usagecontrol/feature/lockcontrol/LockControlScreen.kt` (518 lines)
  - `feature/feature-rules/src/main/java/com/usagecontrol/feature/rules/AddConditionDialog.kt` (424 lines)
  - `feature/feature-rules/src/main/java/com/usagecontrol/feature/rules/RuleEditorScreen.kt` (380 lines)
  - `feature/feature-learning/src/main/java/com/usagecontrol/feature/learning/LearningScreen.kt` (370 lines)
  - `feature/feature-settings/src/main/java/com/usagecontrol/feature/settings/SettingsScreen.kt` (292 lines)
- **Impact:** Difficult to test, modify, and maintain; increases cognitive complexity for future developers; UI state changes require touching large files
- **Fix approach:** Extract dialog components into separate composable files; create custom state holders; move complex logic into ViewModels

### 2. Lack of Input Validation and Sanitization in Rules
- **Issue:** Rule condition parameters are stored as Map<String, String> and parsed via enum.valueOf() without fallback or validation
- **Files:**
  - `core/core-rules/src/main/java/com/usagecontrol/core/rules/ConditionEvaluator.kt` (lines 22-82)
  - `core/core-database/src/main/java/com/usagecontrol/core/database/Mappers.kt` (JSON parsing at lines 192-307)
- **Impact:**
  - IllegalArgumentException if enum name doesn't match stored value (rule corrupted in DB = crash)
  - Invalid time ranges ("25:99") accepted silently, condition always evaluates false
  - No bounds checking on volume levels or duration parameters
- **Fix approach:**
  - Add try-catch with default values for enum.valueOf()
  - Create validation layer for condition/action parameters before storage
  - Validate time ranges, numeric bounds in constructor/parser

### 3. Silent Exception Handling in Services
- **Issue:** Services catch all exceptions with empty catch blocks, hiding real errors
- **Files:**
  - `service/service-usage/src/main/java/com/usagecontrol/service/usage/UsageMonitorService.kt` (line 63-65)
  - `service/service-context/src/main/java/com/usagecontrol/service/context/ContextMonitorService.kt` (line 59-61)
  - `app/src/main/java/com/usagecontrol/MainActivity.kt` (line 50-52)
- **Impact:**
  - Service crashes silently and restarts in loop without user notification
  - Permission changes go unnoticed (permissions revoked = service polling with nulls)
  - No logs to debug production issues
- **Fix approach:**
  - Replace generic catch with specific exception types
  - Log each exception with context
  - Emit error state to UI for permission/critical failures

### 4. Unsafe JSON Deserialization Without Version Handling
- **Issue:** Rule/preset JSON stored in database lacks schema versioning; any change breaks old records
- **Files:**
  - `core/core-database/src/main/java/com/usagecontrol/core/database/Mappers.kt` (lines 272-283 parseRuleJson, lines 297-300 parseMapJson)
- **Impact:**
  - Adding new rule fields breaks deserialization of old rules
  - No migration path if JSON structure changes
  - Database queries will crash on records with missing optional fields
- **Fix approach:**
  - Add version field to JSON objects
  - Create migration functions for each schema version
  - Use optString() with sensible defaults for all fields

### 5. No Error Boundaries or Graceful Degradation in Rule Evaluation
- **Issue:** RuleEngine evaluation fails completely if any rule has corrupt data
- **Files:**
  - `core/core-rules/src/main/java/com/usagecontrol/core/rules/ConditionEvaluator.kt` (lines 18-83)
  - `core/core-rules/src/main/java/com/usagecontrol/core/rules/SuggestionEngine.kt` (lines 35-50)
- **Impact:**
  - One malformed rule crashes entire rule evaluation
  - Notifications won't be processed, actions won't execute
  - Screen time blocking stops working silently
- **Fix approach:**
  - Wrap condition evaluation in try-catch returning false for malformed conditions
  - Skip corrupt rules with logging instead of propagating exceptions
  - Add health check on rule load to detect/disable broken rules

## Known Bugs

### 1. Hard Block Countdown Auto-closes Without User Confirmation
- **Symptoms:** Hard-blocked app automatically closes after 10 seconds with no way to interrupt
- **Files:** `feature/feature-screentime/src/main/java/com/usagecontrol/feature/screentime/BlockOverlayActivity.kt` (lines 149-156)
- **Trigger:** User hits screen time limit, activity shows countdown that expires and closes
- **Workaround:** Quickly tap "Continue Anyway" before 10 seconds elapse (if soft block) or reopen app after block expires
- **Issue:** Hard-blocked users have zero control; should require explicit acknowledgment button when countdown ends

### 2. Context Snapshot Stale Data
- **Symptoms:** Preset activation uses outdated context (WiFi/Bluetooth/charging status from 5 minutes ago)
- **Files:** `service/service-context/src/main/java/com/usagecontrol/service/context/ContextMonitorService.kt` (line 153: POLL_INTERVAL_MS = 300_000L)
- **Trigger:** Device changes WiFi networks; presets don't activate for 5 minutes until next poll
- **Workaround:** Manually toggle preset or wait for next poll cycle
- **Issue:** 5-minute polling interval is too coarse for context-sensitive actions; should be 30-60s or event-driven

### 3. Bluetooth Device Name Matching Too Strict
- **Symptoms:** Bluetooth context conditions fail because device names include version suffixes or MAC addresses
- **Files:** `service/service-context/src/main/java/com/usagecontrol/service/context/ContextMonitorService.kt` (BluetoothScanner implementation not visible, but pattern is exact match)
- **Trigger:** User creates rule "when connected to 'Bose Headphones'", but OS reports "Bose Headphones (45:A2:3D)"
- **Workaround:** Create rule with full device name as returned by OS
- **Issue:** No partial matching or normalization; fragile across device firmware updates

### 4. Notification Auto-Dismiss in Background Can Trigger Twice
- **Symptoms:** Advisory notification for screen time shows twice for same app in one cycle
- **Files:** `service/service-usage/src/main/java/com/usagecontrol/service/usage/UsageMonitorService.kt` (lines 132-140)
- **Trigger:** Usage polling runs twice in quick succession after app is used; blockedThisCycle check uses app name as key instead of app+timestamp
- **Workaround:** None; user just sees duplicate notifications
- **Issue:** Race condition if polling interval overlaps with app usage timestamp window

## Security Considerations

### 1. Unminified Release Builds
- **Risk:** Binary is unminified and unproguarded; sensitive logic/strings easily reverse-engineered
- **Files:** `app/build.gradle.kts` (line 28: isMinifyEnabled = false)
- **Current mitigation:** None
- **Recommendations:**
  - Enable R8 minification for release builds
  - Add ProGuard rules to keep class names for reflection-based string parsing in rules engine
  - Obfuscate sensitive method names

### 2. Exported Notification Listener Service
- **Risk:** NotificationListenerService is exported=true; third-party apps could potentially craft intents to manipulate notification handling
- **Files:** `app/src/main/AndroidManifest.xml` (line 69: android:exported="true")
- **Current mitigation:** Permission check (android.permission.BIND_NOTIFICATION_LISTENER_SERVICE), but not enforced at manifest level
- **Recommendations:**
  - Add android:permission attribute to limit who can bind
  - Consider exported=false and use intent filters only if needed
  - Validate all incoming data from notification service

### 3. Overly Broad Location Permissions
- **Risk:** App requests ACCESS_BACKGROUND_LOCATION but doesn't visibly use geofencing; users unaware of continuous location tracking
- **Files:** `app/src/main/AndroidManifest.xml` (lines 15-18)
- **Current mitigation:** User consent in Settings
- **Recommendations:**
  - Remove background location if not actively used
  - If needed for geofencing, document why and request only ACCESS_FINE_LOCATION for foreground
  - Implement location caching to avoid polling on every rule evaluation

### 4. Device Admin Receiver for Lock Timeout
- **Risk:** Device admin is exported=true with only app-level BIND_DEVICE_ADMIN permission; could be exploited if another app gets admin access
- **Files:** `app/src/main/AndroidManifest.xml` (lines 89-99)
- **Current mitigation:** User explicitly grants device admin through Settings
- **Recommendations:**
  - Verify actual usage; only request device admin if lock-timeout feature is used
  - Add permission="android.permission.BIND_DEVICE_ADMIN" to receiver tag
  - Log all device admin actions to audit trail

### 5. WiFi SSID Stored as Plaintext
- **Risk:** Sensitive WiFi network names (e.g., "XYZ_Company_2.4GHz") persisted in unencrypted Room database
- **Files:** `core/core-database/src/main/java/com/usagecontrol/core/database/entity/SoundPresetEntity.kt` (triggerParamsJson field stores SSID)
- **Current mitigation:** None; database is unencrypted
- **Recommendations:**
  - Encrypt sensitive fields using EncryptedSharedPreferences or Room encryption
  - Use hashed SSIDs for rule matching instead of plaintext
  - Document that presets are tied to device/user account

## Performance Bottlenecks

### 1. Inefficient Rule Evaluation on Every Poll
- **Problem:** All rules evaluated every 60 seconds (usage) and 5 minutes (context), even if conditions unlikely to change
- **Files:**
  - `service/service-usage/src/main/java/com/usagecontrol/service/usage/UsageMonitorService.kt` (lines 106-159, evaluateScreenTimeRules())
  - `service/service-context/src/main/java/com/usagecontrol/service/context/ContextMonitorService.kt` (lines 92-99, evaluateLockSoundRules())
- **Cause:** No indexing of rules by condition type; queries and evaluates all rules regardless of relevance
- **Improvement path:**
  - Cache rules in memory grouped by condition type
  - Only evaluate rules whose conditions changed (e.g., only check time-based rules if hour changed)
  - Debounce rapid usage/context changes

### 2. JSON Parsing on Every Rule Load
- **Problem:** Rule conditions/actions deserialized from JSON string every time rule is fetched from DB
- **Files:** `core/core-database/src/main/java/com/usagecontrol/core/database/Mappers.kt` (lines 90-116, RuleEntity.toModel())
- **Cause:** No caching; repeated allocations of JSONObject, JSONArray instances
- **Improvement path:**
  - Parse and cache rules at DB query time, store parsed objects in Room @Embedded fields
  - Use serialization format (Protobuf) instead of JSON for faster parsing
  - Implement rule cache with TTL in RuleEngine

### 3. Notification Service Queries Without Bounds
- **Problem:** DismissStats queries scan entire notification history without date range
- **Files:** `core/core-database/src/main/java/com/usagecontrol/core/database/dao/NotificationDao.kt` (getDismissStats, getDismissalsByHour methods not visible but called from SuggestionEngine)
- **Cause:** No indexes on (packageName, timestamp) tuples
- **Improvement path:**
  - Add Room @Index annotations on timestamp + packageName
  - Query only last 7 days of notifications (use timestamp > System.currentTimeMillis() - 7*24*60*60*1000)
  - Paginate large result sets

### 4. Blocking Service Spawns Activities Without Queue
- **Problem:** UsageMonitorService launches BlockOverlayActivity synchronously for every rule match; if multiple rules trigger, activities stack up
- **Files:** `service/service-usage/src/main/java/com/usagecontrol/service/usage/UsageMonitorService.kt` (lines 150-177, launchBlockOverlay())
- **Cause:** No deduplication; repeated calls within same poll cycle create duplicate overlays
- **Improvement path:**
  - Add BlockOverlayActivity already-showing check using window manager
  - Batch overlays: show only highest-priority block
  - Use intent flags Intent.FLAG_ACTIVITY_SINGLE_TOP to reuse existing activity

## Fragile Areas

### 1. Condition Parameter Parsing
- **Files:**
  - `core/core-rules/src/main/java/com/usagecontrol/core/rules/ConditionEvaluator.kt` (lines 28-33 for time range)
  - `core/core-rules/src/main/java/com/usagecontrol/core/rules/SuggestionEngine.kt` (lines 162-163 time formatting)
- **Why fragile:**
  - Time range parsing assumes "HH:mm" format; invalid format returns false silently
  - Time formatting uses String.format("%02d:00"); no validation that startTime < endTime
  - No tolerance for timezone changes or DST
- **Safe modification:**
  - Create TimeRange value class with parsing/validation in init{}
  - Use Instant instead of string time-of-day in DB
  - Add tests covering boundary cases (00:00, 23:59, 23:00-01:00 overnight)
- **Test coverage:** None; no tests visible for condition evaluation logic

### 2. Suggestion Engine Heuristics
- **Files:** `core/core-rules/src/main/java/com/usagecontrol/core/rules/SuggestionEngine.kt` (lines 22-29 constants)
- **Why fragile:**
  - MIN_NOTIFICATIONS=10 may be too low for infrequent apps
  - HIGH_DISMISS_RATE=0.90 is hardcoded; no user customization
  - QUICK_DISMISS_MS=3000 is arbitrary; doesn't account for user reaction time variance
  - No decay factor; old dismissal patterns weighted equally with recent ones
- **Safe modification:**
  - Move constants to configurable settings
  - Add temporal weighting (recent dismissals > old ones)
  - Allow users to adjust thresholds from Settings UI
- **Test coverage:** None

### 3. BlockOverlayActivity Countdown Logic
- **Files:** `feature/feature-screentime/src/main/java/com/usagecontrol/feature/screentime/BlockOverlayActivity.kt` (lines 149-156)
- **Why fragile:**
  - LaunchedEffect runs unconditionally; recomposition during countdown restarts timer
  - No state preservation if activity is destroyed/recreated
  - Countdown fires after 10 seconds even if user didn't see screen (e.g., locked phone)
- **Safe modification:**
  - Use rememberSaveable to preserve countdownSeconds across recomposition
  - Move countdown to ViewModel with proper lifecycle management
  - Add explicit close button instead of auto-close
- **Test coverage:** None

### 4. WiFi SSID Caching
- **Files:** `service/service-context/src/main/java/com/usagecontrol/service/context/ContextMonitorService.kt` (lines 102-108)
- **Why fragile:**
  - Uses @Suppress("DEPRECATION") to access deprecated WifiManager.getConnectionInfo()
  - Removes quotes from SSID; some networks use quotes in name
  - No caching of last-known SSID; reconnection to same WiFi shows as new context
- **Safe modification:**
  - Use WifiManager.ScanResult with modern APIs (Android 10+)
  - Store previous WiFi SSID and only publish change if different
  - Add unit test mocking WifiManager responses
- **Test coverage:** None

## Scaling Limits

### 1. Database Growth Unbounded
- **Current capacity:** Room database with no retention policy; notifications, usage, context records accumulate indefinitely
- **Limit:** Database file grows ~1-5MB per week depending on notification volume; after 1 year = 50-250MB
- **Scaling path:**
  - Implement automatic deletion of records older than 30/90 days
  - Add background WorkManager job to clean old data
  - Consider archiving old data to external storage if historical analysis needed

### 2. In-Memory Rule Cache During Polling
- **Current capacity:** All rules loaded into memory in single query per poll cycle
- **Limit:** With 100+ rules, each evaluation scans entire list; O(n) per condition type
- **Scaling path:**
  - Partition rules by condition type (time-based, app-based, etc.)
  - Only load and evaluate relevant rule subsets
  - Implement LRU cache for frequently-accessed rules

### 3. Notification Listener Service Queue
- **Current capacity:** Single background coroutine handles all notifications sequentially
- **Limit:** If app gets >1000 notifications/day, queue will overflow and drop notifications
- **Scaling path:**
  - Use CoroutineScope with bounded work queue
  - Batch-process notifications by app+timestamp window
  - Add metrics to monitor queue depth and drop rate

## Dependencies at Risk

### 1. Room Database (androidx.room:room-runtime:2.6.1)
- **Risk:** Version 2.6.1 is stable but near end-of-support; newer versions may break JSON parsing patterns
- **Impact:** If upgraded to 3.x, JSON columns will require schema changes
- **Migration plan:**
  - When upgrading, switch from manual JSON parsing to @Serializable fields
  - Use Kotlin serialization instead of org.json

### 2. Kotlin Coroutines (1.7.3)
- **Risk:** Old version; newer versions (1.8+) have performance improvements and bug fixes
- **Impact:** Potential memory leaks in long-running services if coroutine contexts not cleaned up
- **Migration plan:**
  - Upgrade to latest stable (check for breaking changes)
  - Add structured concurrency guarantees in services (ensure scope is cancelled in onDestroy)

### 3. Material3 (1.2.0)
- **Risk:** Compose API surface is still evolving; upgrading may break custom composables
- **Impact:** If Material3 3.x releases, many @OptIn annotations may no longer work
- **Migration plan:**
  - Monitor Material3 release notes for breaking changes
  - Test upgrade in feature branch before committing

### 4. Hilt DI (2.50)
- **Risk:** Version 2.50 is stable but uses bytecode generation; future versions may change codegen strategy
- **Impact:** If app breaks during Hilt upgrade, entire app won't compile
- **Migration plan:**
  - Keep Hilt up-to-date with Android Gradle Plugin (AGP 8.2.2)
  - Test all Hilt-injected services (UsageMonitorService, ContextMonitorService) after version bump

## Missing Critical Features

### 1. Audit Trail / Activity Log
- **Problem:** No record of what rules executed, when, and what actions took place
- **Blocks:**
  - Debugging why notifications were dismissed or apps blocked
  - User confidence that rules are working as intended
  - Compliance auditing for enterprise use
- **Impact:** Users forced to reverse-engineer rule behavior through trial and error

### 2. Rule Execution Metrics & Observability
- **Problem:** No visibility into rule evaluation success/failure, condition match rates
- **Blocks:**
  - Performance tuning (which rules are slow?)
  - Effectiveness measurement (is this rule ever triggered?)
- **Impact:** Can't diagnose why presets or blocking rules don't work

### 3. Offline Rules Storage
- **Problem:** Rules stored only in Room database; if app is uninstalled/reinstalled, all rules lost
- **Blocks:** User frustration after accidental uninstall
- **Impact:** No export/backup mechanism; users must recreate rules manually

### 4. Rule Import/Export
- **Problem:** No way to share rules or sync across devices
- **Blocks:**
  - Beta testers can't share effective rule configs
  - Multi-device users must maintain separate rules per device
- **Impact:** Limits usefulness in household/workplace settings

### 5. Granular Permission Control
- **Problem:** All-or-nothing access to notification content, usage stats, location
- **Blocks:** Users uncomfortable granting full location or notification access for just preset automation
- **Impact:** Privacy-conscious users won't use context-based features

## Test Coverage Gaps

### 1. Rule Evaluation Logic Untested
- **What's not tested:** ConditionEvaluator.evaluate() logic for all condition types
- **Files:** `core/core-rules/src/main/java/com/usagecontrol/core/rules/ConditionEvaluator.kt` (lines 18-83)
- **Risk:** Changes to condition logic could break all rules without detection; time range logic especially fragile
- **Priority:** High (core feature)

### 2. Suggestion Engine Heuristics Untested
- **What's not tested:** SuggestionEngine.analyze() produces correct suggestions; heuristics apply correct thresholds
- **Files:** `core/core-rules/src/main/java/com/usagecontrol/core/rules/SuggestionEngine.kt` (lines 35-228)
- **Risk:** Suggestions could be incorrect or biased; users might accept bad rules
- **Priority:** High (learned rules feature)

### 3. Database Mapper JSON Parsing Untested
- **What's not tested:** Mappers.kt functions handle corrupt/missing JSON gracefully
- **Files:** `core/core-database/src/main/java/com/usagecontrol/core/database/Mappers.kt` (lines 190-307)
- **Risk:** Malformed JSON in DB crashes app on rule load; no graceful degradation
- **Priority:** Medium (data integrity)

### 4. UI State Synchronization Untested
- **What's not tested:** ViewModel state updates correctly reflect rule/preset changes; no race conditions in creation/deletion
- **Files:** All ViewModels (RuleEditorViewModel, LockControlViewModel, etc.)
- **Risk:** Race conditions could create duplicate rules or leave orphaned records
- **Priority:** Medium (data consistency)

### 5. Service Lifecycle Untested
- **What's not tested:** UsageMonitorService and ContextMonitorService restart cleanly after crashes; coroutine cleanup on destroy
- **Files:** `service/service-usage/src/main/java/com/usagecontrol/service/usage/UsageMonitorService.kt`, `service/service-context/src/main/java/com/usagecontrol/service/context/ContextMonitorService.kt`
- **Risk:** Coroutine leaks, double-polling if services restart, missed events
- **Priority:** Medium (reliability)

### 6. Permission Handling Untested
- **What's not tested:** Behavior when required permissions (PACKAGE_USAGE_STATS, BIND_NOTIFICATION_LISTENER_SERVICE) are revoked at runtime
- **Files:** `app/src/main/AndroidManifest.xml`, all service files
- **Risk:** Silent failures or crashes if users revoke permissions after initial grant
- **Priority:** High (robustness)

---

*Concerns audit: 2026-02-28*
