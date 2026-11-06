---
sliceId: S02
uatType: runtime-executable
verdict: PARTIAL
attempt: 1
runId: uat:M007:S02:attempt-1
worktreeRoot: /home/james/Code/AndroidUsageControl
date: 2026-11-04T06:23:19.495Z
---

# UAT Result - S02

## Checks

| Check | Mode | Result | Evidence | Notes |
|-------|------|--------|----------|-------|
| Debug APK installed on device/emulator with listener access granted | runtime | FAIL | gsd_uat_exec:0c44b81d-42da-4a9f-b2bb-1e4111968626<br>gsd_uat_exec:2ac26033-ee67-4aeb-87ca-26a123c5cc93 | adb not on PATH; ~/Android/Sdk/platform-tools/adb devices lists no devices. Precondition unmet. |
| Post same tag twice -> one key, updateCount=1 in NotificationCapture log | human-follow-up | NEEDS-HUMAN | gsd_uat_exec:2ac26033-ee67-4aeb-87ca-26a123c5cc93 | Blocked: no device attached. Run adb cmd notification post twice and check logcat. |
| Cancel tag1 -> APP_CANCEL reason, one history row updateCount=1 with removalReason | human-follow-up | NEEDS-HUMAN | gsd_uat_exec:2ac26033-ee67-4aeb-87ca-26a123c5cc93 | Blocked: no device attached. |
| Media/FGS notification appears in live_notifications, not history | human-follow-up | NEEDS-HUMAN | gsd_uat_exec:2ac26033-ee67-4aeb-87ca-26a123c5cc93 | Blocked: no device attached. |
| Force-stop, re-enable listener, reopen -> icons from filesDir/icons, reconcile log matches active | human-follow-up | NEEDS-HUMAN | gsd_uat_exec:2ac26033-ee67-4aeb-87ca-26a123c5cc93 | Blocked: no device attached. |
| ./gradlew connectedDebugAndroidTest (com.usagecontrol.db) migration + DAO tests pass | runtime | NEEDS-HUMAN | gsd_uat_exec:ed9b7b4e-b204-4638-8661-4132ced12649 | Instrumented tests require a device; not runnable. Substitute: testDebugUnitTest exit=0. |
| Legacy v3 rows keep NULL key and stay in stats; ongoing rows excluded from dismiss stats | human-follow-up | NEEDS-HUMAN | gsd_uat_exec:ed9b7b4e-b204-4638-8661-4132ced12649 | Covered only by instrumented migration/DAO tests that need a device. |

## Overall Verdict

PARTIAL - No adb device/emulator attached (adb devices empty), so every device-dependent runtime step could not be executed; JVM unit tests pass as partial substitute.

## Tool Presentation

```json
{
  "surface": "mcp",
  "presentedTools": [
    "gsd_uat_exec",
    "gsd_uat_result_save",
    "gsd_resume",
    "gsd_milestone_status",
    "gsd_journal_query",
    "find",
    "glob",
    "grep",
    "ls",
    "read"
  ],
  "blockedTools": [
    {
      "name": "edit",
      "reason": "forbidden during run-uat"
    },
    {
      "name": "write",
      "reason": "forbidden during run-uat"
    },
    {
      "name": "gsd_exec",
      "reason": "forbidden during run-uat"
    },
    {
      "name": "gsd_summary_save",
      "reason": "forbidden during run-uat"
    },
    {
      "name": "gsd_save_gate_result",
      "reason": "forbidden during run-uat"
    },
    {
      "name": "search-the-web",
      "reason": "forbidden during run-uat"
    },
    {
      "name": "WebSearch",
      "reason": "forbidden during run-uat"
    },
    {
      "name": "Bash",
      "reason": "forbidden during run-uat"
    },
    {
      "name": "Write",
      "reason": "forbidden during run-uat"
    },
    {
      "name": "Edit",
      "reason": "forbidden during run-uat"
    }
  ],
  "toolPresentationPlanId": "run-uat/default-v1"
}
```

## Gate

Aggregate UAT gate saved as flag.

## Manual Validation

One or more checks are marked `NEEDS-HUMAN` and require a person to validate:

- Validate the work here: /home/james/Code/AndroidUsageControl
- Follow the UAT checklist at: .gsd/phases/07-v0-3-doorman/07-02-UAT.md
<!-- gsd:state-version=110:1 -->
