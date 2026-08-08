# r0105 — Shortcut release verification refinement

Date: 2026-08-08
Application version: 1.0.0-rc.57

## Scope

This is a planning-only refinement while phase 9.7 remains BLOCKED. No phase 9.8 implementation is included.

Real deployment testing established two additional acceptance requirements for the signed Shortcut release:

1. iOS derives the imported Shortcut display name from the downloaded `.shortcut` filename in the exercised flow. The backend may continue storing the deployment artifact as `shortcut/releases/zip-github.shortcut`, but the authenticated download must expose `Skicka till zip-github.shortcut` through `Content-Disposition` while serving the exact same signed bytes/hash.
2. A correctly mounted signed release can still be reported unavailable when the file is `0600` and owned by the deployment user rather than the backend runtime user. The release/deployment verification must therefore prove runtime readability. `0644` is the recommended simple mode for the read-only bind-mounted artifact; confidentiality relies on keeping the credential-bearing artifact out of source control and behind the authenticated download endpoint.

These requirements are recorded in step 9.7 and repeated in the final 9.10 E2E/release gate.

## Files

Added:
- `docs/r0105-shortcut-release-verification-refinement.md`

Modified:
- `CHANGELOG.md`
- `VERSION`
- `docs/implementation-status.md`
- `docs/implementation-steps.md`
- `docs/phase8-plus-continuation-handoff.md`
- `scripts/verify-release.sh`

Moved: none.
Deleted: none.
