# rc.52 CI correction — r0100

Date: 8 August 2026  
Application version: 1.0.0-rc.52  
Phase state: 9.7 remains BLOCKED on external Apple signing/iOS installation; 9.8 not started.

## Reported CI failures

1. Backend test compilation failed because static wildcard imports made `endsWith(String)` ambiguous between Hamcrest and Mockito.
2. The second `ShortcutInstallPage` test still saw the download link rendered by the first test because this test file did not explicitly clean up its previous Testing Library render.

## Corrections

- Qualified the assertion as `org.hamcrest.Matchers.endsWith(...)`.
- Imported Testing Library `cleanup` and invoked it from `afterEach`, before unstubbing globals.

No runtime security, staging, Shortcut distribution, or phase-order semantics were changed.

## Verification

Verified in this correction revision:

- implementation ledger: PASS;
- clean structure: PASS;
- security regression: PASS;
- source tracking: PASS;
- release verification: PASS;
- repository shell syntax: PASS;
- GitHub Actions YAML parse: PASS.

Targeted Maven execution was attempted but Maven Wrapper bootstrap could not resolve `repo.maven.apache.org`. Frontend dependency installation was attempted but the sandbox npm proxy returned 404 for `yallist-3.1.1.tgz`, so the corrected Vitest file could not be executed locally. The user's normal CI has already demonstrated that the rest of the frontend suite reaches this test file successfully.

## Files added

- `docs/rc52-ci-correction.md`

## Files modified

- `VERSION`
- `CHANGELOG.md`
- `backend/src/test/java/info/isaksson/erland/zipgithub/api/StagingImportResourceTest.java`
- `frontend/src/pages/ShortcutInstallPage.test.tsx`
- `docs/implementation-status.md`
- `docs/phase8-plus-continuation-handoff.md`
- `scripts/verify-release.sh`

## Files moved

None.

## Files deleted

None.
