# Step 9.7 report — signed iOS Shortcut reference client and installation guide

Date: 8 August 2026  
Revision: r0099  
Application version: 1.0.0-rc.51  
Status: BLOCKED on external Apple signing/install verification

## Scope implemented

Implemented the complete zip-github side of the static signed-Shortcut distribution model:

- authenticated `GET /api/shortcut-release` metadata and `/download` endpoints;
- backend release resolver that accepts only an existing readable `.shortcut` file and never creates an unsigned fallback;
- mobile `/shortcut` installation/update page for signed-in users;
- explicit `403 STAGING_SHORTCUT_OUTDATED` response for missing/old/revoked staging upload credentials;
- Compose read-only mount and deployment configuration for the secret-bearing signed release file;
- source-control ignore rule for signed `.shortcut` artifacts;
- trusted-Mac `shortcuts sign --mode anyone` helper and detailed release/rotation instructions;
- non-secret version/generation + SHA-256 metadata for identifying the current release without logging credentials.

## Why the step is BLOCKED rather than DONE

The step definition explicitly requires a **static, pre-signed, installer-ready `.shortcut` release artifact**. This environment cannot satisfy the Apple signing gate. The earlier practical GitHub-hosted macOS spike reached `/usr/bin/shortcuts sign` but Apple returned `In order to do this, you must be signed into iCloud.` The current sandbox is not a trusted iCloud-signed-in Apple environment.

No unsigned file has been substituted or misrepresented as a release artifact. To complete 9.7, an operator must create/export the documented reference Shortcut in Apple Shortcuts, sign it for `anyone` on a trusted iCloud-signed-in Mac, publish it as `shortcut/releases/zip-github.shortcut`, configure version/generation metadata, and verify iOS accepts the downloaded artifact.

9.8 remains PENDING and is not started because 9.7 is a prerequisite.

## Verification performed

- `ShortcutReleaseServiceSelfTest` compiled and executed with local `javac/java`: PASS.
- Repository structure/status/security/source/release scripts: see final verification in this revision; the ledger verifier was updated to allow the documented single-BLOCKED/no-NEXT state permitted by `AGENTS.md`.
- Shell syntax for the new signing helper: PASS.
- No `.shortcut` file is tracked in the repository: verified.
- Download path requires `CurrentUserProvider.requireUserId()`: statically verified.
- Missing artifact returns unavailable metadata / no download rather than an unsigned fallback: dependency-free self-test PASS.
- Full Maven/JUnit/Quarkus attempted: Maven Wrapper bootstrap failed because `repo.maven.apache.org` could not be DNS-resolved.
- Frontend `npm ci` attempted: sandbox npm proxy returned 404 for `yallist-3.1.1.tgz`, so Vitest/build could not run here.
- Apple signing and iOS installation: NOT POSSIBLE in this environment; this is the explicit blocker.

## Files added

- `backend/src/main/java/info/isaksson/erland/zipgithub/api/ShortcutReleaseResource.java`
- `backend/src/main/java/info/isaksson/erland/zipgithub/api/dto/ShortcutReleaseResponse.java`
- `backend/src/main/java/info/isaksson/erland/zipgithub/shortcut/ShortcutReleaseArtifact.java`
- `backend/src/main/java/info/isaksson/erland/zipgithub/shortcut/ShortcutReleaseService.java`
- `backend/src/test/java/info/isaksson/erland/zipgithub/shortcut/ShortcutReleaseServiceSelfTest.java`
- `docs/signed-shortcut-release.md`
- `docs/step-9.7-report.md`
- `frontend/src/api/shortcut.ts`
- `frontend/src/pages/ShortcutInstallPage.tsx`
- `frontend/src/pages/ShortcutInstallPage.test.tsx`
- `scripts/sign-shortcut-release.sh`
- `shortcut/releases/README.md`

## Files modified

- `.env.example`
- `.gitignore`
- `CHANGELOG.md`
- `VERSION`
- `backend/src/main/java/info/isaksson/erland/zipgithub/api/StagingImportResource.java`
- `backend/src/main/resources/application.properties`
- `docker-compose.yml`
- `docs/api-contract.md`
- `docs/implementation-status.md`
- `docs/operations.md`
- `docs/phase8-plus-continuation-handoff.md`
- `docs/release-checklist.md`
- `docs/shortcut-stagingimport-design.md`
- `docs/threat-model.md`
- `frontend/src/App.tsx`
- `frontend/src/components/AppLayout.tsx`
- `scripts/security-regression.sh`
- `scripts/verify-implementation-status.sh`
- `scripts/verify-release.sh`

## Files moved

None.

## Files deleted

None.

## r0103 signed-artifact follow-up — 8 August 2026

The external signing prerequisite is now resolved: the operator supplied and independently verified an Apple-signed reference Shortcut. The delivery bundle contains it at `shortcut/releases/zip-github.shortcut` (version `1`, generation `g1`, 23821 bytes, SHA-256 `21a9e220067681994ff42326a0b430261fe84583bfbc614297c634ae752af50a`).

To keep the deployment bundle safe when it is itself processed by zip-github, `ImportPolicyService` now hard-blocks that exact credential-bearing path with `SIGNED_SHORTCUT_SECRET_ARTIFACT`. The file remains ignored by Git as defense in depth.

Step 9.7 is still `BLOCKED` on one final acceptance item only: the deployed service must serve this exact artifact through authenticated `/shortcut`, and an iOS device must accept the copy downloaded through that service route. The user has verified the signed Shortcut itself, but the service-mediated download has not yet been exercised in this environment. 9.8 remains PENDING.

Repository/status/structure/security/source/release checks pass with the signed artifact present. A targeted Maven `ImportPolicyServiceTest` run could not bootstrap because this sandbox could not DNS-resolve Maven Central; CI should execute the test after this revision is imported.

### r0103 files added

- `shortcut/releases/zip-github.shortcut`
- `docs/rc55-signed-shortcut-integration.md`

### r0103 files modified

- `.env.example`
- `CHANGELOG.md`
- `VERSION`
- `backend/src/main/java/info/isaksson/erland/zipgithub/policy/ImportPolicyService.java`
- `backend/src/test/java/info/isaksson/erland/zipgithub/policy/ImportPolicyServiceTest.java`
- `docker-compose.yml`
- `docs/implementation-status.md`
- `docs/phase8-plus-continuation-handoff.md`
- `docs/release-checklist.md`
- `docs/signed-shortcut-release.md`
- `docs/step-9.7-report.md`
- `scripts/security-regression.sh`
- `scripts/verify-release.sh`
- `shortcut/releases/README.md`

### r0103 files moved

None.

### r0103 files deleted

None.
