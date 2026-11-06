# Project Knowledge

Append-only register of project-specific rules, patterns, and lessons learned.
Agents read this before every unit. Add entries when you discover something worth remembering.

## Rules

| # | Scope | Rule | Why | Added |
|---|-------|------|-----|-------|

## Patterns

| # | Pattern | Where | Notes |
|---|---------|-------|-------|
| P001 | Cross-module side effects (NotificationActions, SoundProfileApplier) are core-common interfaces implemented in service modules and bound via Hilt, keeping feature modules decoupled. | — | — |

## Lessons Learned

| # | What Happened | Root Cause | Fix | Scope |
|---|--------------|------------|-----|-------|
| L001 | Room 2.8.5 room-testing (room-migration) needs kotlinx-serialization >= 1.8.1 at runtime; AGP consistent resolution pins androidTest to the app's runtime version, so the app must apply kotlinx-serialization-bom 1.8.1 or MigrationTestHelper throws AbstractMethodError on device. | — | — | project |
| L002 | Hilt/KSP incremental builds can omit Hilt_ classes causing NoClassDefFoundError at launch; project uses ksp.incremental=false plus a root build.gradle.kts guard failing the build when an @AndroidEntryPoint lacks its Hilt_ class. | — | — | M007 |
