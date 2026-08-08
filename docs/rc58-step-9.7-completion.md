# r0106 / 1.0.0-rc.58 — step 9.7 completion

## Scope

Complete the final step-9.7 release requirements without starting step 9.8.

## Implemented

- User-facing Shortcut download filename: `Skicka till zip-github.shortcut`.
- Technical deployment filename remains `shortcut/releases/zip-github.shortcut`.
- Signing helper publishes mode `0644` for backend runtime readability.
- Release gate verifies download identity, signed artifact hash/size, and readable deployment permissions when the binary is present.
- Operator evidence confirms the deployed authenticated `/shortcut` copy downloads and imports on iPhone.

## Phase state

- 9.7: DONE
- 9.8: NEXT
- 9.9: PENDING
- 9.10: PENDING

## Files

### Added
- `backend/src/main/java/info/isaksson/erland/zipgithub/shortcut/ShortcutDownloadHeaders.java`
- `backend/src/test/java/info/isaksson/erland/zipgithub/shortcut/ShortcutDownloadHeadersSelfTest.java`
- `docs/rc58-step-9.7-completion.md`

### Modified
- `CHANGELOG.md`
- `VERSION`
- `backend/src/main/java/info/isaksson/erland/zipgithub/api/ShortcutReleaseResource.java`
- `docs/implementation-status.md`
- `docs/phase8-plus-continuation-handoff.md`
- `docs/release-checklist.md`
- `docs/signed-shortcut-release.md`
- `docs/step-9.7-report.md`
- `scripts/sign-shortcut-release.sh`
- `scripts/verify-release.sh`
- `shortcut/releases/README.md`
- `shortcut/releases/zip-github.shortcut` (permissions only: `0600 -> 0644`; signed bytes unchanged)

### Moved
None.

### Deleted
None.
