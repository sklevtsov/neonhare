# Security policy

## Reporting a vulnerability

Report security issues privately through [GitHub Security Advisories](https://github.com/sklevtsov/neonhare/security/advisories/new) when private reporting is enabled. If the form is unavailable, open an issue requesting a private contact without including exploit details, secrets or personal logs. Maintainer: [@sklevtsov](https://github.com/sklevtsov).

Provide the affected version/commit, Android version, prerequisites, reproduction steps and demonstrated impact. Do not upload signing keys, device identifiers, private save files or personal data. Response times are not guaranteed.

## System and scope

Neon Hare is an offline Android platform game. Scope includes the Android app, level parser and packaged content, save migration, diagnostics, developer tools, dependencies, CI and release signing. The current app has no accounts, payments, online leaderboard or requested network permission. Reassess this model before adding network features or third-party SDKs.

## Trust boundaries

- Another Android app can send intents to exported components; extras are untrusted.
- Touch/controller input crosses into the simulation through copied events and a bounded queue.
- Packaged level JSON is maintained in this repository; there is no downloaded-level execution feature.
- Saves and logs may be malformed. Root access and an explicitly authorised ADB connection are privileged prerequisites, not ordinary app permissions.
- Pull requests and their workflows are untrusted. Signing credentials and release publication are privileged operations.

## Security invariants

- Keep requested permissions minimal. Do not add network, storage or sensitive permissions without a documented feature and review.
- Test profiles, debug rewards and debug-only entry points must stay unavailable in local/release builds.
- Validate level schemas, dimensions, identifiers and references. Never execute scripts or load arbitrary classes from content.
- Preserve user saves before migration. Reject an unsupported newer format without overwriting it.
- Do not put credentials, private keys, personal logs or device captures containing private information in Git, APK resources, issues or CI output.
- Public APKs must use a dedicated persistent release key with a verified certificate fingerprint. Never silently fall back to a debug key or generate a fresh signing identity per release.
- Untrusted PR jobs must not receive signing secrets or write access to releases. Keep workflow permissions minimal.
- Publish APKs and checksums together; document signing-identity changes and migration implications.

## Severity and evidence

Prioritise reachable unauthorised access, sensitive-data exposure, arbitrary code execution, cross-app privilege abuse, destructive save migration and release/supply-chain compromise. Explain attacker capabilities, preconditions and affected data. Tests and static configuration are evidence, not a guarantee of security.

Changing one's own offline score does not by itself demonstrate cross-user access or privilege escalation. A launcher Activity being exported is not by itself a vulnerability. Evaluate the actual sensitive operation and reachability; do not suppress a finding merely because the project is a game.

## Known limitations and support

The game is under active development. Fixes normally target the latest maintained version; older builds have no guaranteed backport support. Local development APKs intentionally use a development certificate and are not public production releases. Diagnostics are bounded and currently contain gameplay events; app-specific external storage has different protections on older Android versions. Keep personal diagnostics out of reports and exclude them from backup/public artifacts.

No additional exclusions or acceptance of undisclosed risks are established by this policy. A security check is not certification or a promise that all vulnerabilities have been found.
