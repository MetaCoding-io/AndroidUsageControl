# Research: app groups and on-demand app activation

- Requirement: [R018](../REQUIREMENTS.md#r018--research-app-groups-and-on-demand-app-activation)
- Date: 2026-11-18
- Status: research proposal; no backend selected or implementation scheduled
- Origin: user request to keep occasional shopping and credit-card apps dormant until needed
- Code baseline reviewed: `8de08aa7ab52a99417ce5e141e9840e2c957f180`

## Problem and desired experience

Apps needed for a membership card, an occasional purchase, or account access should be available without demanding daily attention. The user initially described an Android VM that could be started when needed, then proposed a lighter alternative: app groups with shared policies.

Example: Costco and IKEA belong to Shopping. They remain dormant by default. Tapping a Doorman shortcut activates Costco for a session, with a possible 15-minute or screen-lock endpoint. Ending that session reapplies the selected restriction while preserving app data and sign-in where the backend supports it. Opening one app should not automatically activate every other group member.

Research must establish what “dormant” means for each backend. Notification silence, blocked interaction, disabled packages, and a stopped Android profile provide different guarantees. This proposal does not promise a VM boundary or a universal background-execution kill switch.

## Relationship to Doorman

The current [lane model](../../core/core-model/src/main/java/com/usagecontrol/core/model/Lane.kt) groups notifications by delivery destination. [LaneResolver](../../core/core-rules/src/main/java/com/usagecontrol/core/rules/LaneResolver.kt) resolves person, channel, app, then category defaults. The [capture service](../../service/service-notification/src/main/java/com/usagecontrol/service/notification/NotificationCaptureService.kt) stores the result and applies explicit dismissal rules; assigning a lane does not itself freeze the originating app.

Proposed app groups govern application availability. Keep this concept separate from notification lanes:

| Concept | Example | Responsibility |
| --- | --- | --- |
| App group | Shopping: Costco, IKEA | Membership and default lifecycle policy |
| Notification lane | Noise, Deliveries, Money | Delivery of notifications while an app can run |
| Activation session | Costco until 15:30 | Temporary exception to an app restriction |
| Mode | Deep Work | Possible future overrides with defined precedence |

One app could belong to Shopping while its marketing channel routes to Noise and order updates route to Deliveries. Freezing an app can also prevent useful alerts; group membership must be explicit rather than inferred solely from notification annoyance.

Related requirements: R014/R015 (lanes and delivery), R005 (screen-time friction), and R006 (modes/context). A breathing pause or overlay is not package freezing. R018 does not expand those milestones.

## Candidate mechanisms and evidence

The following is a comparison of documented capabilities, not a device-tested compatibility matrix.

| Candidate | Relevant capability | Limits and research question |
| --- | --- | --- |
| Existing notification controls | Doorman can manage captured notifications and dismissal rules | No app lifecycle isolation; cancellation after posting cannot be treated as preventing the initial alert |
| Android Private Space | Google documents that locking it stops its apps and hides them | Android 15+ with device support; separate installations/data; determine whether any supported third-party control or handoff fits Doorman |
| Work profile via Shelter or a future profile controller | Shelter documents isolated installs and per-app freezing inside a work profile | Profile scope, existing employer-profile conflicts, cross-profile access and app compatibility need checking |
| Hail integration | Documented package launch and tag-based freeze/unfreeze actions | External dependency; investigate result reporting, identity checks, backend state and recovery before relying on it |
| Direct Shizuku-backed control | Candidate for managing existing main-profile apps through elevated operations | Privilege grant, supported operations, OEM differences and restart handling need a prototype |
| Full Android VM | Closest to the user's initial metaphor | Investigate only if native controls cannot meet the goal; boot cost, Google services and app compatibility remain unverified |

### Important distinctions

**Private Space:** Google's documentation requires Android 15 or later and manufacturer support. Locking stops apps in the space. Installing there creates a separate app copy; data and accounts do not automatically migrate. This is a user-managed native profile option, not evidence that an ordinary Doorman installation can programmatically lock it. [1][2]

**Shelter:** The project uses Android's work profile APIs and documents per-app freezing. Its source also describes launch shortcuts with refreezing after screen lock. These are precedents for UX and profile-scoped control; a stable external integration API has not been verified. [3][4]

**Hail:** Its README distinguishes disable, hide and suspend. In particular, suspend suppresses interaction and notifications but does not prevent background execution. It documents package-level launch and tag-level actions. Its privilege matrix supports different operations for different backends, and reversal must use the same working mode. An adapter must identify the actual operation and cannot label every result “stopped.” [5]

**Android suspension:** Official policy APIs describe restrictions on activities, notifications and other user-facing behavior, with caller restrictions and packages that cannot be suspended. Doorman's existing optional legacy device-admin receiver must not be treated as device-owner/profile-owner authority. [6]

**Shizuku:** Non-root wireless-debugging startup is available from Android 11; its documented startup must be repeated after reboot. Android 10 requires the computer-based startup route. Investigate privilege loss while apps are restricted and show recovery instructions rather than claiming a successful thaw/refreeze. [7]

## Proposed scope to investigate

- A named app-group model with explicit membership and one primary lifecycle group per app initially. Use an identity including Android user/profile as well as package name.
- Per-app activation and optional group activation, with manual “Freeze now.”
- Session endpoints: manual, duration, or screen lock. “Until app closes” needs a precise lifecycle definition and should not be assumed reliable.
- Home-screen shortcuts that activate before launching; a launch failure must not silently leave an indefinite exception.
- An optional backend interface that reports capabilities, permission state, current restrictions, per-package outcomes and recovery options.
- UI distinctions between silencing notifications, suspension, disabling and profile-level control. Preserve core Doorman functionality when no privileged backend is available.

Potential state records: AppGroup, AppMembership, LifecyclePolicy, ActivationSession and BackendCapability. These are design candidates, not a schema commitment.

## Decisions and failure cases to resolve

1. **Backend choice:** Compare external Hail integration, direct Shizuku control, and a work-profile approach. Prefer a small experiment before choosing. Verify integration contracts and licensing before reusing external code.
2. **Lifecycle enforcement:** Measure activities, services, jobs, alarms, push delivery and launcher behavior separately for each operation. Retain sessions across Doorman process death; specify reboot and overdue-session reconciliation.
3. **Policy ownership:** Avoid undoing another controller's suspension or a user's pre-existing disabled state. Record what Doorman changed, and define restoration behavior.
4. **Automation reliability:** Determine scheduling precision under Doze and background restrictions; document any exact-alarm or foreground-service requirements. No timer guarantee until measured.
5. **Recovery:** Cover revoked privilege, missing helper, controller uninstall, device restart, partial group failure and manually changed settings. Provide an independent route to restore apps.
6. **App suitability:** Explicitly consider payment approvals, fraud alerts, order tracking, authentication callbacks and membership-card access. Keep essential apps and Doorman/backend dependencies out of default freeze suggestions.
7. **Visibility:** Disabling may remove normal launcher icons; determine shortcut discoverability and behavior when users try to open an app outside Doorman.
8. **Data and privacy:** Verify sign-in retention, updates and package replacement. Keep management local; no cloud service or SemPKM bridge dependency is needed.

## Research completion criteria

Before scheduling implementation:

- Produce a capability matrix across Android 10 (the project's minimum), Android 11–14, and Android 15+ where hardware/emulators are available; record unsupported and untested cases explicitly.
- Prototype one noncritical test app through restrict → activate → launch → refreeze, checking data retention and observed background behavior.
- Exercise screen lock, expiry under Doze, process death, restart and privilege loss; verify both recovery and reported state.
- Compare setup and everyday use against manually using Private Space or Shelter.
- Record a backend recommendation, remaining limitations and a small proposed implementation slice; update R018 only after the scope is decided.

No runtime implementation, device validation or release commitment is included in this note.

## Primary sources

Reviewed 2026-11-18:

1. [Google: Private Space requirements and behavior](https://support.google.com/android/answer/15341885?hl=en)
2. [AOSP: Private Space profile model and separate app data](https://source.android.com/docs/security/features/private-space)
3. [Shelter README](https://github.com/PeterCxy/Shelter/blob/master/README.md)
4. [Shelter source strings: launch shortcuts and auto-freeze](https://github.com/PeterCxy/Shelter/blob/master/app/src/main/res/values/strings.xml)
5. [Hail README: operation semantics, privilege matrix and integration actions](https://github.com/aistra0528/Hail/blob/main/README.md)
6. [Android DevicePolicyManager.setPackagesSuspended](https://developer.android.com/reference/android/app/admin/DevicePolicyManager#setPackagesSuspended(android.content.ComponentName,%20java.lang.String%5B%5D,%20boolean))
7. [Shizuku startup and reboot requirements](https://shizuku.rikka.app/guide/setup/)
