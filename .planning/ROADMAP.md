# Roadmap: AndroidUsageControl — v0.2 User Testing

## Overview

This milestone makes the app self-sufficient for new users. A first-time installer can open the app, understand its value, grant the required permissions, take a feature tour, and activate a first preset — all without outside help. The three phases are sequenced by hard technical dependency: the navigation and permission infrastructure must be correct before any screen is built, the permission walkthrough depends on that infrastructure, and the welcome/tour/wizard screens come last because they depend on the permission flow being complete.

## Phases

**Phase Numbering:**
- Integer phases (1, 2, 3): Planned milestone work
- Decimal phases (2.1, 2.2): Urgent insertions (marked with INSERTED)

Decimal phases appear between their surrounding integers in numeric order.

- [ ] **Phase 1: Foundation** - Navigation architecture, DataStore flag, and permission-check utilities that gate the entire onboarding flow
- [ ] **Phase 2: Permission Walkthrough** - Three-step guided flow that walks users through Notification Access, Usage Access, and DND Access with live re-check on return
- [ ] **Phase 3: Welcome, Tour, and Quick Setup** - Welcome screen, 5-tab feature tour, quick setup wizard, and revisit entry point in Settings

## Phase Details

### Phase 1: Foundation
**Goal**: New users are routed to onboarding and returning users (including v0.1 upgraders) go directly to the main app — with correct permission-check logic in place as the shared utility for all downstream phases
**Depends on**: Nothing (first phase)
**Requirements**: ONBD-01, ONBD-02, ONBD-03, ONBD-04
**Success Criteria** (what must be TRUE):
  1. A fresh install opens the app and lands on the onboarding flow; a second open of the same install skips onboarding entirely and lands on the main Notifications tab
  2. A v0.1 user who already has all three permissions granted upgrades to v0.2 and is sent directly to the main app, never seeing the onboarding screens
  3. The app never shows the bottom navigation bar while any onboarding screen is active
  4. Pressing back from the first screen of the main app does not navigate back into any onboarding screen
**Plans**: 2 plans
- [ ] 01-01-PLAN.md -- Infrastructure: DataStore dependency, PermissionChecker, OnboardingRepository, feature-onboarding module
- [ ] 01-02-PLAN.md -- Routing: MainViewModel, NavHost restructure with nested graphs, OnboardingPlaceholderScreen

### Phase 2: Permission Walkthrough
**Goal**: Users can successfully grant all three required permissions (Notification Access, Usage Access, DND Access) through a guided flow, or skip permissions at any step and still reach the main app with clear messaging about what is degraded
**Depends on**: Phase 1
**Requirements**: PERM-01, PERM-02, PERM-03, PERM-04, PERM-05
**Success Criteria** (what must be TRUE):
  1. Each permission step shows a priming screen explaining why that permission is needed before the user is sent to system Settings
  2. When a user returns from the Settings screen after granting a permission, the app shows a green checkmark on that step without requiring any manual tap
  3. The user can see a step counter (e.g., 1/3, 2/3, 3/3) at all times during the permission walkthrough
  4. A user who skips all three permissions still reaches the main app and sees clear per-feature messaging about what is unavailable
  5. The app opens the correct system Settings screen for each permission (not a generic Settings page)
**Plans**: TBD

### Phase 3: Welcome, Tour, and Quick Setup
**Goal**: Users experience a complete end-to-end onboarding flow — welcome proposition, feature tour, and one-tap preset activation — and can re-access the tour from Settings after onboarding completes
**Depends on**: Phase 2
**Requirements**: TOUR-01, TOUR-02, TOUR-03, TOUR-04
**Success Criteria** (what must be TRUE):
  1. The first screen a new user sees is a welcome screen that communicates the app's core value proposition before any permission request appears
  2. A user can swipe through a 5-slide feature tour with dot page indicators and a visible skip option on every slide
  3. At the end of onboarding the user is offered a one-tap option to enable Night Mode preset, which activates immediately without navigating to the Rules tab
  4. A returning user can open Settings, tap a "Revisit tour" option, and view the feature tour again without triggering the full onboarding flow
**Plans**: TBD

## Progress

**Execution Order:**
Phases execute in numeric order: 1 → 2 → 3

| Phase | Plans Complete | Status | Completed |
|-------|----------------|--------|-----------|
| 1. Foundation | 1/2 | In Progress|  |
| 2. Permission Walkthrough | 0/TBD | Not started | - |
| 3. Welcome, Tour, and Quick Setup | 0/TBD | Not started | - |
