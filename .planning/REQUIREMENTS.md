# Requirements: AndroidUsageControl

**Defined:** 2026-02-28
**Core Value:** Help users become aware of and take control of their phone usage patterns without sacrificing privacy

## v0.2 Requirements

Requirements for milestone v0.2 — User Testing. Each maps to roadmap phases.

### Onboarding Foundation

- [x] **ONBD-01**: User sees onboarding flow only on first launch (DataStore flag persists completion)
- [x] **ONBD-02**: App routes to onboarding or main app based on completion flag before NavHost renders
- [x] **ONBD-03**: User can skip onboarding at any step and go directly to main app
- [x] **ONBD-04**: Existing v0.1 users who already have permissions granted auto-skip onboarding on update

### Permission Walkthrough

- [x] **PERM-01**: User sees a custom priming screen per permission explaining why it's needed before being sent to Settings
- [x] **PERM-02**: App opens the correct system Settings screen for each permission (Notification Access, Usage Access, DND)
- [x] **PERM-03**: App detects permission grant when user returns from Settings and shows green checkmark
- [x] **PERM-04**: User sees step progress (1/3, 2/3, 3/3) during permission walkthrough
- [ ] **PERM-05**: App works in degraded mode if user skips permissions, with clear messaging about what's missing

### Welcome & Tour

- [ ] **TOUR-01**: User sees welcome screen with app value proposition as the first onboarding step
- [ ] **TOUR-02**: User can swipe through a 5-tab feature tour with HorizontalPager and dot indicators
- [ ] **TOUR-03**: User is offered a quick setup wizard to enable Night Mode preset at end of onboarding
- [ ] **TOUR-04**: User can revisit the feature tour from the Settings tab

## Future Requirements

### Contextual Guidance

- **GUID-01**: User sees contextual coach marks on first visit to each tab
- **GUID-02**: User receives tooltips on key UI elements (e.g., rule editor condition groups)

### Enhanced Permissions UX

- **PERM-06**: App calls NLS requestRebind after NotificationListenerService permission granted
- **PERM-07**: User sees degraded-feature explanation per-tab when relevant permission is missing

## Out of Scope

| Feature | Reason |
|---------|--------|
| Mandatory permission gate (block app without permissions) | Anti-pattern — app must work degraded; forces abandonment |
| Account creation / registration | No internet, no auth — zero value, 100% friction |
| Marketing carousel (5+ slides) | Users don't retain pre-use info; extra screens lose users |
| Animated mascot / tutorial character | APK bloat, patronizing for utility app |
| Personalization quiz | No personalization backend (no internet) |
| Push notification opt-in | App doesn't send push notifications — asking destroys trust |

## Traceability

| Requirement | Phase | Status |
|-------------|-------|--------|
| ONBD-01 | Phase 1 | Complete |
| ONBD-02 | Phase 1 | Complete |
| ONBD-03 | Phase 1 | Complete |
| ONBD-04 | Phase 1 | Complete |
| PERM-01 | Phase 2 | Complete |
| PERM-02 | Phase 2 | Complete |
| PERM-03 | Phase 2 | Complete |
| PERM-04 | Phase 2 | Complete |
| PERM-05 | Phase 2 | Pending |
| TOUR-01 | Phase 3 | Pending |
| TOUR-02 | Phase 3 | Pending |
| TOUR-03 | Phase 3 | Pending |
| TOUR-04 | Phase 3 | Pending |

**Coverage:**
- v0.2 requirements: 13 total
- Mapped to phases: 13
- Unmapped: 0

---
*Requirements defined: 2026-02-28*
*Last updated: 2026-02-28 — ONBD-01 through ONBD-04 marked complete after Plan 01-02*
