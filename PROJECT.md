# AndroidUsageControl - Project Definition

## Vision
A personal Android app that acts as an intelligent assistant for managing your phone's behavior — notifications, screen time, lock state, and sound — with user-defined rules and learned habits. Privacy-first, everything local.

## Target Audience
Personal use first, designed with enough code quality and UX to polish for public release later.

---

## Five Core Pillars

### Pillar 1: Notification History & Management
**Goal**: Capture, store, search, and analyze all device notifications.

- **Capture**: Use `NotificationListenerService` to intercept all notifications in real-time
- **Storage**: Persist to Room DB with full metadata (app, title, text, timestamp, category, priority, actions, icon)
- **Browse**: Scrollable timeline with filtering by app, date range, keyword search
- **Stats**: Dashboard showing notification volume by app, time-of-day heatmap, trends over time
- **Retention**: Configurable (default 30 days), auto-cleanup via WorkManager

### Pillar 2: Notification Dismissal Rules
**Goal**: Automatically dismiss unwanted notifications based on user-defined rules.

- **Rule conditions**: App name, keyword match (title/body), time of day, day of week, notification priority/category, sender (if available)
- **Rule actions**: Auto-dismiss, mute (remove sound/vibration but keep), snooze for duration
- **Management UI**: List of active rules, enable/disable toggle, create/edit/delete
- **Logging**: Record every rule firing for transparency and debugging

### Pillar 3: Habit Learning (Heuristics → ML)
**Goal**: Learn notification handling patterns and suggest new rules.

- **Phase 1 (Heuristics)**:
  - Track dismiss vs. open/interact ratio per app
  - Track time-to-dismiss per app (immediate dismiss = unwanted)
  - If dismiss rate >90% for an app over 7 days → suggest auto-dismiss rule
  - If notifications from app X are always dismissed between hours Y-Z → suggest time-based rule
- **Phase 2 (Future ML)**:
  - Data model designed from day one to support training features
  - Store interaction vectors: (app, category, time, day, location, action_taken, time_to_action)
  - Future: TensorFlow Lite model for multi-signal prediction
- **Suggestions UI**: Card-based suggestions the user can accept, modify, or dismiss

### Pillar 4: Screen Time Control
**Goal**: Track app usage and enforce time/schedule limits.

- **Tracking**: Use `UsageStatsManager` for per-app usage data
- **Dashboard**: Daily/weekly usage charts, top apps, comparison to previous periods
- **Rules**: Daily time limits per app/category, schedule blocks (e.g., "no social media 9-5 weekdays"), cooldown periods
- **Enforcement levels** (configurable per rule):
  - **Advisory**: Notification only ("You've used TikTok for 30 min today")
  - **Soft block**: Full-screen overlay with "Continue anyway" button (adds friction)
  - **Hard block**: Overlay with no bypass for configured duration
- **Implementation**: Overlay via `SYSTEM_ALERT_WINDOW`, periodic check via foreground service

### Pillar 5: Lock & Sound Behavior Rules
**Goal**: Automate device lock state, volume, and sound profiles based on context.

- **Conditions**: Location (geofence/WiFi SSID), time of day, calendar events, charging state, Bluetooth connection
- **Lock actions**: Keep unlocked (requires DeviceAdmin or Accessibility), auto-lock timeout adjustment
- **Sound actions**: Set ring/notification/alarm/media volume, toggle DND, set ringer mode (silent/vibrate/normal)
- **Presets**: Named profiles (Home, Work, Sleep, Meeting) that bundle multiple actions
- **Examples**:
  - "At home WiFi → stay unlocked, normal volume"
  - "10pm-7am → alarm volume max, DND on, auto-lock 30s"
  - "Calendar event with 'meeting' → silent mode"

---

## Unified Rules Engine

All five pillars share a common rules engine with:

### Conditions (shared, composable)
| Condition Type | Parameters |
|---|---|
| Time of day | start_time, end_time |
| Day of week | days[] |
| Location | WiFi SSID or lat/lng + radius |
| App | package_name or app_category |
| Calendar | event keyword match |
| Charging | is_charging, charger_type |
| Bluetooth | device_name or MAC |
| Notification content | title/body keyword or regex |
| Notification priority | min_priority level |

### Actions (per-pillar)
| Pillar | Actions |
|---|---|
| Notifications | dismiss, mute, snooze(duration) |
| Screen Time | advise, soft_block, hard_block |
| Lock/Sound | set_volume(stream, level), set_ringer_mode, set_dnd, set_lock_timeout |

### Rule Structure
```
Rule {
  id, name, enabled, priority
  conditions: List<Condition>  // ALL must match (AND logic)
  conditionGroups: List<ConditionGroup>  // OR between groups, AND within
  actions: List<Action>
  pillar: enum(NOTIFICATION, SCREEN_TIME, LOCK_SOUND)
  createdBy: enum(USER, SUGGESTION, LEARNED)
}
```

---

## Technical Architecture

### Stack
- **Language**: Kotlin
- **UI**: Jetpack Compose + Material 3
- **Architecture**: MVVM with clean architecture layers
- **DI**: Hilt
- **DB**: Room
- **Async**: Kotlin Coroutines + Flow
- **Background**: WorkManager + Foreground Service
- **Build**: Gradle with version catalogs

### Module Structure
```
AndroidUsageControl/
├── app/                          # Main app module, navigation, DI setup
├── core/
│   ├── core-common/              # Shared utilities, extensions
│   ├── core-database/            # Room DB, DAOs, entities
│   ├── core-rules/               # Unified rules engine
│   ├── core-ui/                  # Shared Compose components, theme
│   └── core-model/               # Domain models shared across features
├── feature/
│   ├── feature-notifications/    # Pillar 1 + 2: History, browsing, dismissal
│   ├── feature-learning/         # Pillar 3: Habit analysis, suggestions
│   ├── feature-screentime/       # Pillar 4: Usage tracking, limits
│   ├── feature-lockcontrol/      # Pillar 5: Lock, sound, profiles
│   └── feature-settings/         # Global settings, retention config
└── service/
    ├── service-notification/     # NotificationListenerService
    ├── service-usage/            # Usage stats polling service
    └── service-context/          # Location, WiFi, calendar, BT monitoring
```

### Key Android Permissions & Services
```
NotificationListenerService      → notification access (user grants in Settings)
UsageStatsManager               → usage stats (user grants in Settings)
SYSTEM_ALERT_WINDOW             → overlay for app blocking
ACCESS_FINE_LOCATION            → geofencing
ACCESS_WIFI_STATE               → WiFi SSID detection
READ_CALENDAR                   → calendar-based rules
RECEIVE_BOOT_COMPLETED          → restart services after reboot
FOREGROUND_SERVICE              → persistent monitoring
DO_NOT_DISTURB_ACCESS           → DND control
MODIFY_AUDIO_SETTINGS           → volume control
BIND_DEVICE_ADMIN (optional)    → lock screen control
```

### Data Model Highlights

**NotificationRecord**
```
id, packageName, appName, title, text, subText, category, priority,
timestamp, groupKey, isOngoing, extras (JSON),
userAction (DISMISSED, OPENED, SNOOZED, AUTO_DISMISSED),
actionTimestamp, ruleIdIfAutoDismissed
```

**UsageRecord**
```
id, packageName, date, foregroundTimeMs, launchCount,
firstUsedTimestamp, lastUsedTimestamp
```

**Rule** (as described in rules engine section above)

**ContextSnapshot** (for learning)
```
id, timestamp, locationType (HOME, WORK, OTHER),
wifiSsid, timeOfDay, dayOfWeek, isCharging, calendarEvent
```

---

## MVP Scope (v0.1)

All five pillars scaffolded with basic functionality:

1. **Notifications**: Capture + store + browse (basic list with app filter)
2. **Dismissal Rules**: Create simple app-based dismiss rules
3. **Learning**: Track dismiss/open ratios, show basic stats (no suggestions yet)
4. **Screen Time**: Show daily usage per app, one rule type (daily time limit, advisory only)
5. **Lock/Sound**: One preset (e.g., "Night mode" with time-based volume), WiFi-based unlock

### MVP Non-Goals
- No ML model
- No complex compound rules (simple single-condition rules only)
- No calendar integration
- No onboarding flow
- No widgets
- No export/backup

---

## Iteration Plan

| Version | Focus |
|---|---|
| v0.1 | Scaffold all pillars, basic capture + display + simple rules |
| v0.2 | Full rules engine with compound conditions, rule management UI |
| v0.3 | Habit learning suggestions, screen time soft/hard blocking |
| v0.4 | Location/calendar context, lock control, sound profiles |
| v0.5 | Polish, stats dashboards, configurable retention, settings |
| v1.0 | Stable personal release, all pillars fully functional |
| v1.x+ | ML learning, widgets, backup/export, public release polish |
