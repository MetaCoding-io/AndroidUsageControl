---
sliceId: S04
uatType: runtime-executable
verdict: PARTIAL
attempt: 1
runId: uat:M007:S04:attempt-1
worktreeRoot: /home/james/Code/AndroidUsageControl
date: 2026-11-04T06:58:07.187Z
---

# UAT Result - S04

## Checks

| Check | Mode | Result | Evidence | Notes |
|-------|------|--------|----------|-------|
| Precondition: debug APK installed on a device with access granted | runtime | NEEDS-HUMAN | gsd_uat_exec:db8bbeea-ba49-46f4-adbf-3e6bd33f272c | app-debug.apk is built, but adb is not on PATH and no device is attached. |
| The apps_list, app_profile, modes_list, Unknown channel and SoundProfileApplier wiring is present, and the placeholders are removed | artifact | PASS | gsd_uat_exec:e9f7687a-2199-4918-8f9e-67716e7b8f47 | All identifiers were found and PlaceholderScreens.kt is removed. |
| Apps tab rows are sorted by alerts/day and show an icon, label, sparkline and dismiss hint | human-follow-up | NEEDS-HUMAN | - | No device is available. Verify on a device. |
| The app profile shows the ring, median, hourly bars, channels, screen time, limit and suggestions | human-follow-up | NEEDS-HUMAN | - | No device is available. |
| The daily limit persists with no duplicate rule | human-follow-up | NEEDS-HUMAN | - | No device is available. |
| Modes tab shows Focus, Sleep and Normal cards, the context strip and the screen-time summary | human-follow-up | NEEDS-HUMAN | - | No device is available. |
| Toggling a mode changes the ringer, and SoundProfileApplier logcat shows applied | human-follow-up | NEEDS-HUMAN | - | Requires adb and a device. |
| With notification-policy access revoked, a snackbar appears and logcat shows permission-denied | human-follow-up | NEEDS-HUMAN | - | Requires a device. |
| An app with no actions shows an em dash for the median, not a crash | human-follow-up | NEEDS-HUMAN | - | Requires a device. |

## Overall Verdict

PARTIAL - The on-device runtime checks could not run because adb is not installed and no device is attached; the static wiring checks passed.

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
- Follow the UAT checklist at: .gsd/phases/07-v0-3-doorman/07-04-UAT.md
<!-- gsd:state-version=156:1 -->
