---
sliceId: S03
uatType: runtime-executable
verdict: PARTIAL
attempt: 1
runId: uat:M007:S03:attempt-1
worktreeRoot: /home/james/Code/AndroidUsageControl
date: 2026-11-04T06:41:27.542Z
---

# UAT Result - S03

## Checks

| Check | Mode | Result | Evidence | Notes |
|-------|------|--------|----------|-------|
| Debug APK installed on a device with notification listener access granted | runtime | FAIL | gsd_uat_exec:5e0ce46f-0eb9-4f2b-9c5b-6016a2d5043e | adb: command not found. No device. app-debug.apk exists in app/build/outputs/apk/debug. |
| Static substitute: log/snackbar strings exist in source | artifact | PASS | gsd_uat_exec:8ed5c601-be1a-48ab-8d53-8c9eeb498749 | Found the snoozed log in NotificationActionsImpl.kt and the open-intent-cancelled path. This does not stand in for the runtime checks. |
| Bottom bar shows exactly Now, Apps, Modes; gear opens Settings | human-follow-up | NEEDS-HUMAN | gsd_uat_exec:5e0ce46f-0eb9-4f2b-9c5b-6016a2d5043e | Needs a device (blocked: no adb/device). |
| Posted notification appears in Now with icon and lane dot, hero count increments | human-follow-up | NEEDS-HUMAN | gsd_uat_exec:5e0ce46f-0eb9-4f2b-9c5b-6016a2d5043e | Needs a device. |
| Search 'hello' filters to matching row | human-follow-up | NEEDS-HUMAN | gsd_uat_exec:5e0ce46f-0eb9-4f2b-9c5b-6016a2d5043e | Needs a device. |
| Swipe right shows Undo snackbar; Undo within 10s restores row and system notification | human-follow-up | NEEDS-HUMAN | gsd_uat_exec:5e0ce46f-0eb9-4f2b-9c5b-6016a2d5043e | Needs a device. |
| Swipe right, wait 10s: system notification cancelled, logcat logs 'cancelled' | human-follow-up | NEEDS-HUMAN | gsd_uat_exec:5e0ce46f-0eb9-4f2b-9c5b-6016a2d5043e | Needs a device. |
| Swipe left opens TimePicker; confirm logs 'snoozed' and removes from shade | human-follow-up | NEEDS-HUMAN | gsd_uat_exec:5e0ce46f-0eb9-4f2b-9c5b-6016a2d5043e | Needs a device. |
| Tap row opens original app; fallback launch activity + 'Original no longer available' snackbar | human-follow-up | NEEDS-HUMAN | gsd_uat_exec:5e0ce46f-0eb9-4f2b-9c5b-6016a2d5043e | Needs a device. |
| With listener access revoked, actions show error snackbar without crash | human-follow-up | NEEDS-HUMAN | gsd_uat_exec:5e0ce46f-0eb9-4f2b-9c5b-6016a2d5043e | Needs a device. |
| Dismissed notifications appear under Recently dismissed for 1 hour | human-follow-up | NEEDS-HUMAN | gsd_uat_exec:5e0ce46f-0eb9-4f2b-9c5b-6016a2d5043e | Needs a device. |

## Overall Verdict

PARTIAL - This host has no adb and no connected device, so none of the device steps could run. The debug APK exists, and source checks found the snooze and open log paths. Every runtime step needs a human with a device.

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
- Follow the UAT checklist at: .gsd/phases/07-v0-3-doorman/07-03-UAT.md
<!-- gsd:state-version=134:1 -->
