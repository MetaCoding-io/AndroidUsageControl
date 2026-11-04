# Project Knowledge

Append-only register of project-specific rules, patterns, and lessons learned.
Agents read this before every unit. Add entries when you discover something worth remembering.

## Rules

| # | Scope | Rule | Why | Added |
|---|-------|------|-----|-------|

## Patterns

| # | Pattern | Where | Notes |
|---|---------|-------|-------|

## Lessons Learned

| # | What Happened | Root Cause | Fix | Scope |
|---|--------------|------------|-----|-------|
| L001 | Room 2.8.5 room-testing (room-migration) needs kotlinx-serialization >= 1.8.1 at runtime; AGP consistent resolution pins androidTest to the app's runtime version, so the app must apply kotlinx-serialization-bom 1.8.1 or MigrationTestHelper throws AbstractMethodError on device. | — | — | project |
