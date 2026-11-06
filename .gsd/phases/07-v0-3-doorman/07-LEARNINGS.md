---
phase: 07
phase_name: v0.3 Doorman
project: AndroidUsageControl
generated: 2026-11-05T00:00:00Z
counts: {decisions: 2, lessons: 2, patterns: 1, surprises: 1}
missing_artifacts: []
---

### Decisions
- Adopted AGP 9.5 built-in Kotlin with compileSdk 37 because material3 1.5.0-beta02 (Expressive) requires it; material3 pinned via catalog over the BOM.
  Source: 07-01-SUMMARY.md/Key decisions
- Fail the build at KSP time when an @AndroidEntryPoint lacks its Hilt_ class, plus ksp.incremental=false.
  Source: 07-05-SUMMARY.md/Key decisions

### Lessons
- Hilt/KSP incremental builds can drop Hilt_ classes, causing NoClassDefFoundError at launch; non-incremental KSP plus a build guard prevents it (root cause inferred).
  Source: 07-05-SUMMARY.md/Known limitations
- Compose dark theme alone is not enough: a values-night Theme.Doorman is needed so the window background and system bars follow dark mode.
  Source: 07-05-SUMMARY.md/One-liner

### Patterns
- Cross-module side effects (notification actions, sound profiles) go through core-common bridge interfaces implemented in service modules and bound via Hilt.
  Source: 07-04-SUMMARY.md/Key files

### Surprises
- Earlier PARTIAL validations were caused by adb missing from the agent PATH, not product failures.
  Source: 07-VALIDATION.md/Slice Delivery Audit
