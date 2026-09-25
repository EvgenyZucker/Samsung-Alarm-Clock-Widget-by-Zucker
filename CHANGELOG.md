# Changelog

All notable changes to Samsung Alarm Clock Widget by Zucker are documented in this file.

The project follows a simple incremental versioning scheme. Version 1.1 is the first release signed with the permanent production key.

## [1.1] - 2026-09-26

### Changed

- Replaced the temporary debug signature with a permanent production signing key.
- Increased `versionCode` to `101` and `versionName` to `1.1`.
- Added secure local release-signing configuration through an ignored `keystore.properties` file.
- Updated the README with a prominent APK download button and detailed installation instructions.
- Documented the tested Samsung Galaxy S25 FE and One UI 8.5 environment.
- Added compatibility, privacy, troubleshooting and update guidance.

### Verified

- Verified the release APK signature and permanent certificate fingerprint.
- Verified installation of version 1.1 over an existing production-signed installation.
- Verified that the widget, its settings, `DUMP`, `PACKAGE_USAGE_STATS`, and `GET_USAGE_STATS` access remain intact after an in-place update.

### Upgrade notice

- Public version 1.0 used a temporary debug signature and cannot be updated directly to 1.1.
- Users of version 1.0 must uninstall it once, install 1.1, and grant the required ADB permissions again.
- Releases starting with 1.1 can be updated in place without repeating that migration.

## [1.0] - 2026-09-26

### Added

- First public release.
- Customizable home-screen clock widget with time, date and next-alarm display.
- Samsung Clock alarm detection that filters unrelated Android system events and Samsung Modes and Routines schedules.
- Automatic background refresh after alarm changes and phone restarts.
- Configurable colors, fonts, text sizes, alignment, shadow, time format, date format and time zone.
- Configurable widget tap action, including opening Samsung Clock or another selected application.
- Widget reconfiguration through the One UI long-press menu.
- Optimized release build with resource shrinking and code minification.

### Known limitation

- This release was signed with a temporary debug key.

## Pre-1.0 development builds

### Alarm detection

- Implemented direct inspection of Samsung Clock alarm records through `dumpsys alarm`.
- Added parsing for Samsung alarm records split across multiple output lines.
- Added focused diagnostics for `com.sec.android.app.clockpackage` and `EXPLICIT_ALARM_ALERT` records.
- Added filtering to prevent Modes and Routines and unrelated scheduled events from replacing the real next alarm.
- Added support for repeating alarms scheduled several days ahead.
- Added cached alarm state and periodic verification.

### Permissions and reliability

- Added the `DUMP` permission required to inspect system alarm records.
- Added `PACKAGE_USAGE_STATS` and the associated `GET_USAGE_STATS` AppOp required on the tested Android/One UI version.
- Confirmed that permissions and widget operation survive normal phone restarts.
- Confirmed operation without root, Shizuku or a permanently connected computer.

### Widget and interface

- Replaced the initial text layout with bitmap-based rendering for consistent fonts, sizing, shadows and alignment.
- Added nine alignment positions and multiple clock font choices.
- Added configurable time, date, alarm and background appearance.
- Added application and shortcut selection for widget taps.
- Added Samsung Clock package visibility and reliable launch behavior.
- Added widget configuration support to the One UI long-press menu.
- Recreated the main behavior and configuration structure of Digital Clock Widget by Maize / EZI Studio Inc. without using its source code.
