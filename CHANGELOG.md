# Changelog

All notable changes to Samsung Alarm Clock Widget by Zucker are documented in this file.

The project follows a simple incremental versioning scheme. Version 1.1 is the first release signed with the permanent production key.

## [Unreleased]

- Fixed text alignment in the installed widget using the visible glyph bounds so all nine
  positions match the settings preview; centered content now has equal visible margins.
- Reduced background work by using system alarm-change events with a one-minute fallback check,
  skipping alarm reads while the screen is off, avoiding unchanged cache writes and reusing
  rendered widget frames. The non-wakeup fallback resumes the real check after the device wakes.
- Reduced the cost of parsing Samsung alarm diagnostics without changing alarm-selection rules.
- Limited midnight refresh scheduling to widgets that display a date.
- Added a delayed confirmation read after Samsung Clock change events because One UI can
  announce a distant recurring alarm several seconds before registering its final alarm entry.
- Documented and rejected Samsung-specific alarm events: Clock's widget event is package-scoped,
  while One UI's global event is blocked for background manifest receivers. The widget therefore
  keeps Android's standard event plus its non-wakeup verification fallback.
- Refresh the cached alarm after an in-place app update to prevent a pre-update value from
  remaining visible until the next scheduled verification.

### Added

- Added a complete adaptive launcher icon with round and monochrome themed variants.
- Added a native widget preview for supported launchers.
- Added English and Russian resources with per-app language selection.
- Added an About and legal screen with version, repository and licensing links.
- Added in-app status checks, warnings and copyable ADB instructions for the required permissions.
- Added the Apache License 2.0 and third-party notices for Google Material Icons and Material Symbols.
- Added GitHub Issue Forms for bug reports and compatibility reports.
- Added a read-only GitHub Actions workflow for linting, unit tests and debug builds without production-signing secrets.

### Changed

- Replaced the alarm font with a subset generated from the current official Google Material Symbols source.
- Updated Material icon resources from the official source.
- Improved accessibility descriptions, keyboard and TalkBack behavior, text contrast and large-font layouts.
- Added a shorter launcher label while retaining the full project name in About and widget metadata.
- Made the no-alarm result independent of localized diagnostic text without changing the alarm-selection algorithm.

### Verified

- Verified Russian and English interfaces on a Samsung Galaxy S25 FE running One UI 8.5.
- Verified the adaptive and monochrome launcher icons, native widget preview and maximum system font size.
- Verified missing-permission warnings, permission restoration and preservation after an in-place update.
- Verified repository, license and third-party notice links, Samsung Clock launching and widget refresh after changing an alarm.

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
