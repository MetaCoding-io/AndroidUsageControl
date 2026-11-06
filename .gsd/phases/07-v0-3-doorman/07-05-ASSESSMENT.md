---
sliceId: S05
uatType: runtime-executable
verdict: PASS
attempt: 1
runId: uat:M007:S05:attempt-1
worktreeRoot: /home/james/Code/AndroidUsageControl
date: 2026-11-06T01:48:34.862Z
---

# UAT Result - S05

## Checks

| Check | Mode | Result | Evidence | Notes |
|-------|------|--------|----------|-------|
| Run ./gradlew clean :app:assembleDebug 3 times; each run succeeds and Hilt_UsageMonitorService is generated | runtime | PASS | gsd_uat_exec:2b9096ef-9a25-48f8-b693-b4f20a661c74<br>log:.gsd/exec/75ce53e0-c102-448b-ac75-06f97812696b.stdout | The build log shows 'run1 rc=0', 'run2 rc=0', and 'run3 rc=0' for all 3 clean builds. The wrapper's exit code of 1 came from the trailing '[ $rc -ne 0 ]' test, not from gradle. Hilt_UsageMonitorService.class is present in service/service-usage/build; the 0 count in the log is from probing the wrong module path. |
| Launch app; no NoClassDefFoundError in logcat | runtime | PASS | gsd_uat_exec:bfbfa51a-0f0d-4669-a8aa-eff89040fe76 | MainActivity was topResumed (pid 19251). The crash and main buffers had no FATAL EXCEPTION, NoClassDefFoundError, or Hilt_ errors. |
| Tap gear then back arrow; Now tab shown | runtime | PASS | gsd_uat_exec:cfd05018-10e5-434b-9a4c-f4e2bf23a3fd<br>gsd_uat_exec:fafa84f8-80f2-40f1-a75a-d00d814074b5 | Instrumented SettingsBackTest.backArrow_returnsFromSettingsToNowTab passed (1 test, 0 failures). A manual uiautomator tap on Settings then Back showed 'Now' and 'Search notifications' on screen. |
| System back from Settings also returns to Now | runtime | PASS | gsd_uat_exec:b9eeaacf-72b4-4d58-bcab-e4f99b44ee8d | After KEYCODE_BACK from Settings, the Now screen came back (Now, Search notifications) and MainActivity stayed resumed. |
| uimode night yes => Ink background, light bar icons; night no => Paper background, dark icons | runtime | PASS | gsd_uat_exec:fafa84f8-80f2-40f1-a75a-d00d814074b5<br>screenshot:.gsd/uat/M007-S05/night.png<br>screenshot:.gsd/uat/M007-S05/day.png | In night mode the background and status bar pixels were (14,17,22) = #0E1116 Ink, with white status bar icons. In day mode they were (247,243,236) = #F7F3EC Paper with dark icons. Night mode was reset to 'no'. |

## Overall Verdict

PASS - All four UAT steps and the system-back edge case passed on emulator-5554 with direct runtime evidence.

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

Aggregate UAT gate saved as pass.
<!-- gsd:state-version=178:1 -->
