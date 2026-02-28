# Pitfalls Research

**Domain:** Android onboarding / permission flows for special permissions (NotificationListenerService, UsageStatsManager, DND)
**Researched:** 2026-02-28
**Confidence:** HIGH — Core pitfalls verified against official Android docs, AOSP issue tracker, and multiple developer reports. Codebase-specific pitfalls verified against existing source.

---

## Critical Pitfalls

### Pitfall 1: Service Not Starting After Permission Granted (NotificationListenerService)

**What goes wrong:**
The user grants Notification Access in system settings and returns to the app. The `NotificationCaptureService` (already declared in the manifest) does not begin receiving `onNotificationPosted` callbacks. The app shows a stale "permission needed" state even though the user just granted it.

**Why it happens:**
Android must bind the `NotificationListenerService` after the permission toggle. In some situations — especially during debug reinstalls or if the system's component binding gets stuck — the OS fails to rebind. There is no `onListenerConnected()` callback delivered, and the service silently does not work. This is a documented AOSP bug (issue tracker #226411020). The permission was granted in Settings.Secure, but the service binding never fires.

**How to avoid:**
1. Always verify using `NotificationManagerCompat.getEnabledListenerPackages(context).contains(packageName)` — this checks the actual enabled state, not just the manifest. Never use `checkSelfPermission()` for this; it always returns DENIED for special permissions regardless of actual state.
2. On the onboarding "confirm" screen after the user returns from settings, call `requestRebind(ComponentName(...))` to explicitly trigger system rebind if the service is not yet connected.
3. Offer a "retry / refresh" action on the permission confirmation step — "It may take a moment, tap here to verify" — rather than assuming success immediately.

**Warning signs:**
- `onListenerConnected()` is never called after user returns from settings.
- `NotificationManagerCompat.getEnabledListenerPackages()` contains the package name, but notification feed is empty.
- Debug reinstall triggers the issue — if it occurs during dev, it will occur for real users too.

**Phase to address:** Permission walkthrough implementation phase (the guided permission flow).

---

### Pitfall 2: Permission State Not Re-Checked When User Returns from System Settings

**What goes wrong:**
The onboarding screen sends the user to system settings via `startActivity(Intent(Settings.ACTION_NOTIFICATION_LISTENER_SETTINGS))`. The user grants the permission and presses back. The onboarding screen still shows "not granted" because it does not re-check on resume.

**Why it happens:**
In Jetpack Compose, there is no implicit lifecycle callback on `Activity.onResume()` unless you explicitly observe it. `rememberPermissionState()` from Accompanist only handles runtime permissions — it does not apply to special permissions. Developers assume a recomposition will trigger, but state only updates if you explicitly re-read the permission check.

**How to avoid:**
Use `LifecycleEventObserver` inside a `DisposableEffect` on every permission screen to re-check the grant status on `ON_RESUME`:

```kotlin
val lifecycleOwner = LocalLifecycleOwner.current
DisposableEffect(lifecycleOwner) {
    val observer = LifecycleEventObserver { _, event ->
        if (event == Lifecycle.Event.ON_RESUME) {
            viewModel.recheckPermissions()
        }
    }
    lifecycleOwner.lifecycle.addObserver(observer)
    onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
}
```

The ViewModel method must re-call the appropriate check function for each permission:
- Notification Access: `NotificationManagerCompat.getEnabledListenerPackages(context).contains(packageName)`
- Usage Access: `AppOpsManager.checkOpNoThrow(OPSTR_GET_USAGE_STATS, uid, packageName) == MODE_ALLOWED`
- DND Access: `NotificationManager.isNotificationPolicyAccessGranted()`

**Warning signs:**
- Permission screen does not advance after user returns from Settings.
- User must manually tap a "check" button to proceed.
- UI shows "not granted" immediately after granting, requiring app restart.

**Phase to address:** Permission walkthrough — every step that sends the user to system settings must have this lifecycle observer.

---

### Pitfall 3: Using Wrong Permission Check API for Special Permissions

**What goes wrong:**
Developer calls `ContextCompat.checkSelfPermission(context, Manifest.permission.PACKAGE_USAGE_STATS)` to check if Usage Access is granted. It always returns `PERMISSION_DENIED` even after the user has granted the permission. The onboarding flow never advances.

**Why it happens:**
Special permissions (`PACKAGE_USAGE_STATS`, `BIND_NOTIFICATION_LISTENER_SERVICE`, `ACCESS_NOTIFICATION_POLICY`) are not runtime permissions. The standard `checkSelfPermission()` API does not apply to them. Each requires a different, custom check mechanism. Developers familiar with the runtime permission model mistakenly apply it to special permissions.

**How to avoid:**
Use the correct check for each permission:

| Permission | Correct Check Method |
|------------|----------------------|
| Notification Access | `NotificationManagerCompat.getEnabledListenerPackages(ctx).contains(packageName)` |
| Usage Access | `AppOpsManager.checkOpNoThrow(AppOpsManager.OPSTR_GET_USAGE_STATS, uid, packageName) == AppOpsManager.MODE_ALLOWED` |
| DND Access | `(getSystemService(NOTIFICATION_SERVICE) as NotificationManager).isNotificationPolicyAccessGranted` |

Also: do NOT use `ActivityCompat.requestPermissions()` for these — it will either fail silently or crash with an exception on some devices.

**Warning signs:**
- Permission check always returns false/denied even after granting.
- onboarding step never advances past the permission request screen.
- No exception thrown, but no progress — silent failure.

**Phase to address:** Permission walkthrough — gate the correct check method behind a utility function in `core-common` to prevent mis-use across the codebase.

---

### Pitfall 4: Existing Users Shown Onboarding on Every Launch

**What goes wrong:**
After adding the onboarding flow, users who already had the app installed (with permissions already granted) are shown the welcome screen and permission walkthrough on every launch. Or, worse, they are shown it once on the first update launch and can never skip it because the "onboarding complete" flag was never set for existing installs.

**Why it happens:**
The onboarding completion flag is stored in DataStore/SharedPreferences. For existing users, this key does not exist (it was never written), so the app interprets it as "not yet onboarded." The developer forgets to handle the migration case: if permissions are already granted, the onboarding state should be initialized to "complete."

**How to avoid:**
On first app launch after this version update, check permissions at startup. If all three special permissions are already granted, write the onboarding-complete flag immediately and skip the flow. This is a one-time migration:

```kotlin
// In OnboardingViewModel or splash logic
suspend fun resolveOnboardingState(): OnboardingState {
    val flaggedComplete = dataStore.isOnboardingComplete.first()
    if (flaggedComplete) return OnboardingState.Complete

    // Migration: existing user with permissions already granted
    if (allPermissionsGranted()) {
        dataStore.setOnboardingComplete()
        return OnboardingState.Complete
    }

    return OnboardingState.Pending
}
```

**Warning signs:**
- Beta testers (existing users) report being shown onboarding on first update launch.
- After dismissing/completing onboarding, it reappears on the next launch.
- Onboarding completion is stored in SharedPreferences (not DataStore) — race conditions on startup can cause inconsistency.

**Phase to address:** Onboarding entry point / state management phase. Use DataStore (not SharedPreferences) for the completion flag to avoid threading issues.

---

### Pitfall 5: Back Navigation into Onboarding from Main App

**What goes wrong:**
The user completes onboarding and enters the main app. Pressing the back button takes them back to the permission walkthrough step — or worse, the welcome screen — because the onboarding destinations were not popped from the back stack.

**Why it happens:**
The navigation graph does not pop the onboarding flow when transitioning to the main app. The onboarding screens remain on the back stack, so back navigation re-enters them. This is a structural issue with how Compose Navigation handles conditional start destinations.

**How to avoid:**
Do NOT make the onboarding screen the start destination of the main NavHost. Instead, use a splash/routing screen as the start destination that evaluates state and navigates immediately:

```kotlin
// AppNavigation.kt
NavHost(navController, startDestination = "splash") {
    composable("splash") {
        // Evaluate onboarding state, then navigate with popUpTo
        LaunchedEffect(Unit) {
            if (onboardingComplete) {
                navController.navigate("main") {
                    popUpTo("splash") { inclusive = true }
                }
            } else {
                navController.navigate("onboarding") {
                    popUpTo("splash") { inclusive = true }
                }
            }
        }
    }
    // ...onboarding and main graphs
}
```

When completing onboarding, navigate to main with `popUpTo("onboarding") { inclusive = true }` to clear the entire onboarding graph.

**Warning signs:**
- Back button from main app shows a blank or flicker screen before closing the app.
- Back button from main app navigates to a permission step.
- Deep links into the main app bypass state check and land on unexpected screens.

**Phase to address:** Navigation architecture — must be designed correctly before any onboarding screens are built, as retrofitting is expensive.

---

### Pitfall 6: UsageStatsManager Returns Empty List Despite Permission Granted

**What goes wrong:**
Usage Access is granted. The onboarding wizard completes. But the Screen Time tab shows no data (empty list from `queryUsageStats()`). Users think the app is broken.

**Why it happens:**
Two distinct causes:
1. **Device locked on Android R+:** `queryUsageStats()` returns null/empty when the device is locked (not decrypted). The foreground service may call it before the user unlocks.
2. **Time range error:** The query uses a zero-length or incorrect time window (e.g., start and end are both `System.currentTimeMillis()`).
3. **Race condition on first grant:** The Usage Stats service may not have data yet for the current day immediately after permission is granted — there is a brief initialization period.

**How to avoid:**
- Check `UserManager.isUserUnlocked(context)` before calling `queryUsageStats()` in the monitoring service.
- After permission is granted during onboarding, inform the user: "Usage data will appear shortly — the system needs a few minutes to gather data."
- Use a non-zero time range: `start = Calendar.startOfDay.timeInMillis`, `end = System.currentTimeMillis()`.
- Move all `queryUsageStats()` calls off the main thread (they can cause frame drops).

**Warning signs:**
- Screen Time tab shows empty on first launch despite granted permission.
- Data appears after a few minutes or after the user locks and unlocks the device.
- `UsageMonitorService` logs show empty results on Android 11+.

**Phase to address:** Permission walkthrough — set expectations in the "Usage Access granted" confirmation screen.

---

## Moderate Pitfalls

### Pitfall 7: DND Permission Not Grantable in Work Profiles

**What goes wrong:**
Users with Android work profiles (company-managed devices) cannot grant DND access to apps installed in the work profile. The Settings screen opens but shows no toggle for the app, or the toggle is greyed out. The onboarding flow loops indefinitely.

**Why it happens:**
Android work profile policy restricts DND override access. An app installed in the work profile cannot receive DND access through the standard `ACTION_NOTIFICATION_POLICY_ACCESS_SETTINGS` intent.

**How to avoid:**
- Detect work profile context using `UserManager.isManagedProfile()`.
- If in a work profile, show a contextual message: "DND control is not available in work profiles — sound features may be limited."
- Do NOT block onboarding completion on DND permission when the device is in a work profile. Make DND optional (it powers sound presets but is not required for notification capture or screen time).
- The app already has a graceful degradation path — DND features simply do not activate if the permission is absent.

**Warning signs:**
- User reports onboarding spinner never completes.
- `isNotificationPolicyAccessGranted()` always returns false on managed devices.
- Users on company devices cannot proceed past the DND step.

**Phase to address:** Permission walkthrough — detect work profile early and skip or mark DND as optional.

---

### Pitfall 8: No Feedback During System Settings Navigation (User Abandonment)

**What goes wrong:**
The app sends the user to system settings with `startActivity(Settings.ACTION_NOTIFICATION_LISTENER_SETTINGS)`. The Settings screen opens, but the user does not know which toggle to find or why they are there. They tap back without granting, or they grant but tap back immediately without returning to the app naturally.

**Why it happens:**
System settings screens are generic and do not highlight the specific app. For Notification Access, the user lands on a list of all apps — they must scroll to find the app and understand what the toggle does.

**How to avoid:**
- Show a "pre-permission" rationale screen BEFORE launching the settings intent. Explain: what the permission does, what breaks without it, and exactly what they will see in settings (e.g., "You'll see a list of apps — find 'Usage Control' and toggle it on").
- On Android 11+, use `Settings.ACTION_NOTIFICATION_LISTENER_DETAIL_SETTINGS` with `EXTRA_NOTIFICATION_LISTENER_COMPONENT_NAME` to deep-link directly to the app's specific toggle row (where supported).
- After the user returns, show an explicit "Thanks — your data is being processed" confirmation rather than silently advancing.

**Warning signs:**
- User testing shows high drop-off at the Notification Access step.
- Users say "I couldn't find where to enable it."
- Users grant the permission but tap home instead of back, so the onboarding re-check never fires.

**Phase to address:** Permission walkthrough UX — pre-permission rationale screens are required for all three permissions.

---

### Pitfall 9: Services Starting Before Permissions Granted (Silent Failure + Battery Impact)

**What goes wrong:**
`MainActivity.startMonitoringServices()` is called on every `onCreate`. If the user has just installed the app and has not yet granted any permissions, both `UsageMonitorService` and `ContextMonitorService` start, poll repeatedly, get empty/null results, and waste battery. The existing code swallows the exception (`catch (_: Exception) {}`), masking the failure.

**Why it happens:**
The existing `startMonitoringServices()` call does not check permissions before starting services. This was acceptable for a single-developer install, but with new users going through onboarding, services will be started before permissions exist.

**How to avoid:**
- Gate `startMonitoringServices()` behind a permission check in `MainActivity`.
- After the onboarding permission walkthrough completes, trigger service start from the onboarding ViewModel instead of always starting on `onCreate`.
- Alternatively, have each service check its required permission at the top of its `onStartCommand()` / `onCreate()` and stop itself gracefully if the permission is absent.

**Warning signs:**
- Foreground service notification appears during onboarding before permissions are granted.
- Battery drain reported on first launch.
- Services show in notification tray with no useful data.

**Phase to address:** Permission walkthrough and service lifecycle integration.

---

### Pitfall 10: Onboarding Flow Skippable Without Re-Entry Path

**What goes wrong:**
The user skips the permission walkthrough during onboarding. Later, after understanding the app, they want to grant permissions. There is no obvious path back to the permission grant flow — just the buried Settings tab with raw links to system settings (current state). Users do not know what to do.

**Why it happens:**
Skip functionality is added for good UX reasons (do not block users), but re-entry into the guided flow is not designed. The Settings screen has the permission shortcuts, but without the explanatory context of the onboarding flow, users who skipped still do not understand what to do.

**How to avoid:**
- On the tab screens that require permissions (Notifications tab without notification access, Screen Time tab without usage access), show a prominent inline "Grant permission" card that re-enters the guided permission screen — not just a raw settings link.
- The "Grant permission" card should use the same rationale language as the onboarding walkthrough.
- Mark permissions granted/skipped individually. A user who granted Notification Access but skipped Usage Access should only see the Usage Access card, not all three.

**Warning signs:**
- User testing: users who skip cannot figure out how to enable features later.
- Settings screen is found via trial and error, not obvious navigation.
- All three permission banners appear even after some are already granted.

**Phase to address:** Feature tour and post-onboarding re-entry design.

---

## Minor Pitfalls

### Pitfall 11: Feature Tour Shown to Users Who Regranted Permissions

**What goes wrong:**
User installs app, completes onboarding including the feature tour. Six months later, they revoke and re-grant Notification Access (e.g., after a factory reset). The app detects incomplete onboarding state and shows the full feature tour again, which the user has already seen.

**Prevention:**
Track feature tour completion separately from permission state. `onboarding_complete` flag should not be reset when permissions change. Permission state is dynamic; the feature tour is a one-time educational experience. Use separate DataStore keys: `onboarding_flow_complete` and `permissions_all_granted`.

---

### Pitfall 12: Onboarding Navigation Breaks the Bottom Nav Bar

**What goes wrong:**
The onboarding graph is added inside the same `NavHost` as the main app tabs. The bottom navigation bar appears during onboarding steps, allowing users to skip onboarding by tapping a tab.

**Prevention:**
The existing `showBottomBar` logic in `MainActivity.kt` checks if `currentRoute` is in `AppScreen.entries`. Extend this check to explicitly exclude all onboarding routes. Or, better: use a completely separate `NavHost` for onboarding, rendered conditionally before the main scaffold is shown.

---

### Pitfall 13: Compose Pager / HorizontalPager State Not Saved on Configuration Change

**What goes wrong:**
User is on step 2 of the 3-permission walkthrough. They rotate the device (or the system kills the process for memory). They return to step 1.

**Prevention:**
Use `rememberPagerState()` from Compose Foundation — it automatically persists across recompositions but NOT across process death. For the permission walkthrough specifically, persist the current step index in the ViewModel's `SavedStateHandle` so it survives process death and configuration changes.

---

## Technical Debt Patterns

| Shortcut | Immediate Benefit | Long-term Cost | When Acceptable |
|----------|-------------------|----------------|-----------------|
| Swallowing permission check exceptions silently | Avoids crash on first launch | Masks real failures; battery drain from polling without permissions | Never — add explicit logging and graceful stop |
| Using SharedPreferences for onboarding flag | Simpler API | Not thread-safe; can lead to race conditions on startup | Never for new code — use DataStore |
| Single "all permissions" screen instead of individual guided steps | Less code | High drop-off; users do not understand 3 different settings screens | Never — users need rationale for each permission |
| Checking permissions only on app start (not on resume) | Simpler flow | Permission state changes at any time; stale UI | Never for special permissions — must check on resume |
| Blocking onboarding completely if any permission denied | Simplest logic | Users who deny DND (e.g., work profile) cannot use the app at all | Never — DND must be optional |

---

## Integration Gotchas

| Integration | Common Mistake | Correct Approach |
|-------------|----------------|------------------|
| NotificationListenerService | Checking `checkSelfPermission()` to verify Notification Access | Use `NotificationManagerCompat.getEnabledListenerPackages(ctx).contains(packageName)` |
| UsageStatsManager | Calling `queryUsageStats()` while device is locked (Android R+) | Check `UserManager.isUserUnlocked()` first |
| DND / NotificationPolicy | Assuming DND access works in work profiles | Detect managed profile, mark DND permission as optional |
| System Settings Intent | Opening generic `ACTION_NOTIFICATION_LISTENER_SETTINGS` without context | On API 31+, use `ACTION_NOTIFICATION_LISTENER_DETAIL_SETTINGS` with component name extra |
| Compose Navigation + Onboarding | Making onboarding the NavHost start destination | Use a routing/splash destination that evaluates state and navigates with `popUpTo` |

---

## Performance Traps

| Trap | Symptoms | Prevention | When It Breaks |
|------|----------|------------|----------------|
| Checking permission status in a tight loop (polling) | Battery drain on first launch | Check once on resume via lifecycle observer, not on a timer | Immediately on install |
| `queryUsageStats()` on main thread | UI jank on Screen Time tab first load | Use `Dispatchers.IO` in a coroutine; already done in service but must verify onboarding check | On any device at query time |
| Checking permissions in `onCreate` without `onResume` | Stale "not granted" UI state | Always re-check on `ON_RESUME` lifecycle event | Every time user returns from system settings |

---

## UX Pitfalls

| Pitfall | User Impact | Better Approach |
|---------|-------------|-----------------|
| Sending user to settings without explanation | User does not know what to enable; abandons | Pre-permission rationale screen with visual instructions and exact step description |
| No feedback after user returns from settings | User thinks nothing happened | Animate the permission check; show checkmark + confirmation message |
| Blocking app entirely when permissions denied | User cannot explore; likely uninstalls | Show empty states with inline "grant permission" CTA; allow partial use |
| Feature tour before user grants permissions | User sees screenshots of features they cannot use yet | Show feature tour AFTER permissions are granted OR use it as rationale during the grant flow |
| All-or-nothing onboarding (must complete all steps to proceed) | Users on restricted devices (work profiles, kiosks) cannot use the app | Mark each permission as required/optional; allow proceeding with partial grant |

---

## "Looks Done But Isn't" Checklist

- [ ] **Permission granted check:** Verify with correct API for each permission (NOT `checkSelfPermission`) — write a test that grants each and asserts the check returns true.
- [ ] **Lifecycle re-check:** Verify permission status updates immediately after returning from system settings — do NOT require a tap or app restart.
- [ ] **Existing user migration:** On first launch after adding onboarding, users with permissions already granted should NOT see the onboarding flow.
- [ ] **Back stack clean:** After completing or skipping onboarding, pressing back should close the app, not navigate into an onboarding screen.
- [ ] **Service start timing:** Services should not start (or should self-stop) before permissions are granted — verify the foreground notification does not appear during onboarding.
- [ ] **Work profile DND:** Verify app does not loop indefinitely on managed/work profile devices when DND access is unavailable.
- [ ] **Skip re-entry:** After skipping any permission during onboarding, verify users can find and grant that permission from the relevant tab's empty state.
- [ ] **NotificationListenerService connected:** After granting Notification Access, verify `onListenerConnected()` fires AND the notification feed receives events — do not trust the settings toggle alone.

---

## Recovery Strategies

| Pitfall | Recovery Cost | Recovery Steps |
|---------|---------------|----------------|
| Service not rebinding after permission grant | LOW | Add `requestRebind()` call in the onboarding confirmation step; surface "Retry" button |
| Existing users shown onboarding | MEDIUM | Release patch with migration logic checking existing permissions and writing the completion flag |
| Back stack shows onboarding after main app | HIGH | Requires navigation graph refactor — must be caught in initial implementation |
| Work profile DND loop | LOW | Add `isManagedProfile()` check and mark DND optional; release point fix |
| Wrong permission check API | MEDIUM | Requires audit of all permission-check call sites; regression test each |

---

## Pitfall-to-Phase Mapping

| Pitfall | Prevention Phase | Verification |
|---------|------------------|--------------|
| Service not rebinding (Pitfall 1) | Permission walkthrough implementation | Manual test: grant notification access, verify `onListenerConnected()` fires within 5s |
| Permission state not re-checked on resume (Pitfall 2) | Permission walkthrough implementation | Manual test: grant permission in settings, return, verify UI updates without tap |
| Wrong permission check API (Pitfall 3) | Permission walkthrough implementation | Code review: all three permissions use correct check method; add unit test |
| Existing users shown onboarding (Pitfall 4) | Onboarding entry point / state management | Test: install app with permissions granted from v0.1, update to v0.2, verify no onboarding |
| Back navigation into onboarding (Pitfall 5) | Navigation architecture | Test: complete onboarding, press back multiple times, verify app closes |
| UsageStats returns empty (Pitfall 6) | Permission walkthrough UX + service hardening | Test: grant usage access, verify data appears within 5 minutes; check for race conditions |
| Work profile DND (Pitfall 7) | Permission walkthrough implementation | Test on work profile device or emulator; verify onboarding completes without DND |
| No feedback during settings navigation (Pitfall 8) | Permission walkthrough UX | User testing: observe users completing each permission step without confusion |
| Services start before permissions (Pitfall 9) | Service lifecycle integration | Test: fresh install, observe that no foreground notification appears until at least one permission is granted |
| No re-entry path after skip (Pitfall 10) | Feature tour and post-onboarding design | Test: skip all permissions, navigate to each tab, verify inline grant-permission CTA is visible |

---

## Sources

- [NotificationListenerService API Reference — Android Developers](https://developer.android.com/reference/android/service/notification/NotificationListenerService) — HIGH confidence (official)
- [Request Special Permissions — Android Developers](https://developer.android.com/training/permissions/requesting-special) — HIGH confidence (official)
- [Conditional Navigation — Android Developers](https://developer.android.com/guide/navigation/use-graph/conditional) — HIGH confidence (official)
- [DND (Do Not Disturb) — Android Open Source Project](https://source.android.com/docs/core/display/dnd) — HIGH confidence (official)
- [Notification Access Policy — AOSP](https://source.android.com/docs/automotive/hmi/notifications/notification-access) — HIGH confidence (official)
- [NotificationListenerService bug: fails to rebind — AOSP Issue Tracker #226411020](https://issuetracker.google.com/issues/226411020) — HIGH confidence (official bug tracker)
- [NotificationListenerService toggle/rebind workaround — AOSP Issue Tracker #158918654](https://issuetracker.google.com/issues/158918654) — HIGH confidence (official bug tracker)
- [Plan for onboarding users — Android Health APIs](https://developer.android.com/health-and-fitness/guides/health-connect/plan/onboard-users) — HIGH confidence (official pattern reference)
- [Avoiding Conditional Navigation Pitfalls — DoorDash Engineering](https://doordash.engineering/2020/08/04/implementing-the-android-navigation-library/) — MEDIUM confidence (verified against official nav docs)
- [AppOpsManager OPSTR_GET_USAGE_STATS — Microsoft Learn](https://learn.microsoft.com/en-us/dotnet/api/android.app.appopsmanager.opstrgetusagestats) — MEDIUM confidence (platform binding docs)
- [UsageStatsManager — Android Developers](https://developer.android.com/reference/android/app/usage/UsageStatsManager) — HIGH confidence (official)
- [Work profiles — Android Enterprise](https://developer.android.com/work/managed-profiles) — HIGH confidence (official)
- [Work profile DND limitation — Community report](https://community.atlassian.com/forums/Opsgenie-questions/OpsGenie-and-work-profiles-on-Android-9/qaq-p/1106115) — MEDIUM confidence (multiple reports agree)
- [GSoC 2025: Onboarding Revamp in ListenBrainz Android](https://blog.metabrainz.org/2025/08/31/gsoc-2025-onboarding-revamp-in-listenbrainz-android/) — MEDIUM confidence (real-world implementation report)

---

*Pitfalls research for: Android onboarding/permission flows — v0.2 milestone*
*Researched: 2026-02-28*
