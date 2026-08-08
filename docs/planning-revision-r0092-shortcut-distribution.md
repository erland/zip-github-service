# Planning revision r0092 — Shortcut distribution and credential lifecycle

Date: 8 August 2026  
Application version: `1.0.0-rc.44` (unchanged; planning-only revision)

## Decision

Phase 9 will use a static, pre-signed iOS Shortcut release artifact with a deployment-scoped, low-privilege staging upload credential.

The upload credential:

- authorizes only creation of a new staging upload;
- is not user authentication and grants no list/read/claim/Project/GitHub permission;
- is sent in `X-ZipGitHub-Upload-Credential`, never in a URL;
- can be revoked/rotated independently of GitHub App credentials and web sessions;
- must not be logged in access logs, analytics or ordinary audit records.

The first version deliberately does **not** require `current`/`previous` overlapping credentials. On compromise or planned replacement, the old credential may be revoked immediately. A new signed Shortcut containing the new credential is then published, and old installations receive a clear update-required error directing the user to the authenticated Shortcut download page.

## Shortcut signing/distribution

The Java backend will not dynamically generate/sign a per-user `.shortcut` file in phase 9. The reference Shortcut is created in a trusted Apple environment, signed for sharing and published as a static release artifact that an authenticated zip-github user can download/install.

A practical spike in `erland/got-test-repo` on 8 August 2026 established that a GitHub-hosted macOS runner contains `/usr/bin/shortcuts` and the `shortcuts sign` command, but signing fails with `In order to do this, you must be signed into iCloud.` Therefore ordinary GitHub-hosted Actions is not treated as an automated signing solution for phase 9. A self-hosted iCloud-signed-in Mac could be explored later but is unnecessary for the initial architecture.

## Security consequence

Embedding the deployment credential in the signed standard Shortcut is acceptable because the credential is intentionally low privilege. Even if extracted, it can at worst be used to consume bounded staging upload capacity. Ownership is still established only by the one-time claim token plus normal authenticated browser login, and all repository changes still pass through the existing Project/Import/approval/GitHub App security model.

## Files affected

This revision updates planning/documentation only. Runtime code and application version are unchanged.
