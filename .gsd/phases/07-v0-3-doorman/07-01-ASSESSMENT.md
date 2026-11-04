---
sliceId: S01
uatType: runtime-executable
verdict: PARTIAL
attempt: 1
runId: uat:M007:S01:attempt-1
worktreeRoot: /home/james/Code/AndroidUsageControl
date: 2026-11-04T05:30:27.766Z
---

# UAT Result - S01

## Checks

| Check | Mode | Result | Evidence | Notes |
|-------|------|--------|----------|-------|
| ./gradlew assembleDebug check exits 0 | runtime | PASS | gsd_uat_exec:f383391c-1cb4-4135-bfb8-05b6532e5746 | EXIT=0 |
| Installed app label reads Doorman | human-follow-up | NEEDS-HUMAN | gsd_uat_exec:2946fbb5-28cb-4f21-8745-baf89257073e | out-of-surface-tool: adb: command not found. app_name=Doorman in strings.xml. A human should install the APK and check the launcher label. |
| Amber LED-dot icon on ink; monochrome themed icon renders | human-follow-up | NEEDS-HUMAN | gsd_uat_exec:2946fbb5-28cb-4f21-8745-baf89257073e | out-of-surface-tool: no adb/device. The adaptive icon declares <monochrome>. |
| App launches without a crash using the Doorman dark theme | human-follow-up | NEEDS-HUMAN | gsd_uat_exec:2946fbb5-28cb-4f21-8745-baf89257073e | out-of-surface-tool: no adb/device. MainActivity uses DoormanTheme and the manifest uses Theme.Doorman. |
| ComponentGallery previews render dark/light with distinct lane colours | human-follow-up | NEEDS-HUMAN | gsd_uat_exec:f383391c-1cb4-4135-bfb8-05b6532e5746 | The previews compile but rendering them requires Android Studio. |

## Overall Verdict

PARTIAL - assembleDebug check passed and the label/icon/theme artifacts are present, but the device steps could not run because adb is not installed on the host (out-of-surface-tool).

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
- Follow the UAT checklist at: .gsd/phases/07-v0-3-doorman/07-01-UAT.md
<!-- gsd:state-version=80:1 -->
