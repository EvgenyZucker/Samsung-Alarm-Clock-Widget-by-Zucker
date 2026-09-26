# Contributing

Thank you for helping improve Samsung Alarm Clock Widget by Zucker.

## Before opening a report

- Review the [installation and troubleshooting guide](README.md#troubleshooting).
- Search the [existing issues](https://github.com/EvgenyZucker/Samsung-Alarm-Clock-Widget-by-Zucker/issues) for the same problem.
- Use the bug report or compatibility report form so the device and One UI details are included.
- Remove device serial numbers, private alarm labels, passwords, signing material and unrelated diagnostic output.

Security vulnerabilities should be reported according to [SECURITY.md](SECURITY.md), not through a public issue.

## Development setup

The project requires a compatible JDK and Android SDK 36. On Windows, run:

```powershell
.\gradlew.bat lintDebug testDebugUnitTest assembleDebug
```

On macOS or Linux, run:

```bash
./gradlew lintDebug testDebugUnitTest assembleDebug
```

These are the same debug checks used by GitHub Actions. They do not require the production signing key.

## Pull requests

Keep each pull request focused and explain:

- what changed and why;
- how the change was tested;
- which Samsung device, Android version and One UI version were used for device-specific testing;
- whether widget instances, settings and ADB permissions survive an in-place update.

For user-interface changes, include screenshots in English and Russian when the change affects localized text. For widget changes, test resizing, reconfiguration and the relevant alignment options in One UI Home.

Do not change the alarm-selection algorithm or Samsung Clock parsing behavior without describing the reason, expected compatibility impact and focused device tests.

## Translations and user-facing text

All user-facing strings belong in Android string resources. Keep the default English resources in `app/src/main/res/values/strings.xml` and the Russian translation in `app/src/main/res/values-ru/strings.xml` synchronized.

## Secrets and release signing

Never commit or attach:

- `keystore.properties`;
- `.jks` or `.keystore` files;
- signing passwords or private keys;
- production-signed APKs outside an authorized GitHub Release;
- device serial numbers or private diagnostic data.

Production releases are signed only by the maintainer. Contributors should use debug builds for development and verification.

## License

By contributing, you agree that your contribution is licensed under the project's [Apache License 2.0](LICENSE). Only submit code and assets that you created or that can legally be distributed under compatible terms. Document third-party material and its license in [THIRD_PARTY_NOTICES.md](THIRD_PARTY_NOTICES.md).
