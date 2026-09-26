# Security Policy

## Supported versions

Security fixes are provided for the latest production release. Users should update in place from version 1.1 or newer so the widget, settings and granted ADB permissions remain intact.

Public version 1.0 used a temporary debug signature and is no longer supported. Migrating from 1.0 requires a one-time uninstall, installation of the latest release and re-granting the required ADB permissions.

## Reporting a vulnerability

Do not disclose a suspected vulnerability, exploit details, signing material, device identifiers or private diagnostic output in a public issue.

Use GitHub's private vulnerability reporting for this repository:

[Report a vulnerability privately](https://github.com/EvgenyZucker/Samsung-Alarm-Clock-Widget-by-Zucker/security/advisories/new)

Include only the information needed to reproduce and assess the issue:

- affected application version;
- Samsung device model, Android version and One UI version, when relevant;
- a concise description of the impact;
- reproducible steps or a minimal proof of concept;
- whether the issue requires either of the application's ADB-granted permissions.

Remove device serial numbers, personal alarm labels, account data, passwords and unrelated system output. Never send the production keystore or its passwords.

The maintainer will acknowledge the report when it is reviewed, investigate it privately and coordinate disclosure after an appropriate fix or mitigation is available. Please do not publish details before that coordination is complete.

## Scope

Security reports may include vulnerabilities in the application, widget configuration, permission handling, update path or handling of Samsung Clock diagnostic data. General bugs, installation problems and compatibility results should use the repository's [issue forms](https://github.com/EvgenyZucker/Samsung-Alarm-Clock-Widget-by-Zucker/issues/new/choose).
