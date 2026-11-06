# M007: v0.3 Doorman

**Vision:** The app becomes Doorman: new name, LED-motif dark-first theme on Material 3 Expressive, three tabs (Now, Apps, Modes), and a listener that captures everything later lanes and mirroring need. The Now stream is the daily surface. Phone behaviour is unchanged; nothing is mirrored or held yet.

## Success Criteria

- Doorman name, theme and three tabs live on device
- Listener data model complete enough for v0.4 mirroring

## Slices

- [x] **S01: Toolchain and theme** `risk:high` `depends:[]`
  > After this: Launcher shows Doorman with LED-dot icon; preview gallery renders every shared component in dark and light

- [x] **S02: Listener foundation** `risk:high` `depends:[]`
  > After this: Post/update/remove a test notification yields one row with correct reason and update count; icons load after listener restart

- [x] **S03: Shell and Now** `risk:medium` `depends:[S01,S02]`
  > After this: Bottom bar Now/Apps/Modes; Now shows real notifications with icons and lane dots, swipe archive with undo, swipe-left snooze to time, tap opens original

- [x] **S04: Apps and Modes v1** `risk:medium` `depends:[S03]`
  > After this: Apps lists apps by alerts/day with sparklines; app profile shows stats, channels, limit, suggestions; Modes shows preset cards whose toggle applies the sound profile

- [x] **S05: Hardening: Hilt/KSP clean-build crash, Settings back arrow, dark mode check** `risk:medium` `depends:[S04]`
  > After this: Repeated clean builds launch without NoClassDefFoundError; Settings has a working back arrow; the app shows the dark theme correctly on the emulator in dark mode

## Boundary Map

Not provided.
<!-- gsd:state-version=178:1 -->
