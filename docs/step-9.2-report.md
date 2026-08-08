# Step 9.2 report — capability-protected staging upload

Date: 2026-08-08  
Repository revision: `r0094`  
Application version: `1.0.0-rc.46`

## Scope completed

Implemented only phase 9 step 9.2. The service now has a narrow capability-protected endpoint that streams a ZIP through the existing `ZipIngestionService`, persists a new `AVAILABLE` `StagingImport`, and returns opaque staging metadata plus a one-time claim URL/token. No authenticated claim, Project selection or promotion was implemented.

Security properties implemented:

- deployment-scoped low-privilege `X-ZipGitHub-Upload-Credential`;
- blank/missing configured credential is deny-all;
- SHA-256 + constant-time digest comparison for the presented credential;
- exact POST-only CSRF exemption for the capability endpoint, with all normal browser writes unchanged;
- dedicated hard staging-create rate bucket;
- 256-bit URL-safe one-time claim token;
- only claim-token SHA-256 is persisted;
- raw claim token is returned only at creation and placed in the claim URL fragment;
- no anonymous list/read/download endpoint;
- same streaming/compressed-size/filename/SHA/storage behavior as browser upload through `ZipIngestionService`;
- generic bounded error messages;
- best-effort stored-file deletion if staging persistence fails.

## Verification actually performed

Passed:

- dependency-free `StagingSecretCodecSelfTest` compiled/run with local `javac/java`;
- repository implementation-ledger verification;
- repository structure verification;
- repository security regression;
- source-tracking verification;
- release verification;
- shell syntax checks for repository scripts;
- static inspection that `StagingImportResource` exposes only `POST /api/staging-imports` and no anonymous GET/list/read route;
- ZIP packaging/integrity verification.

Attempted but environment-blocked:

```text
cd backend
bash ./mvnw -q -Dtest='StagingUploadCredentialTest,StagingUploadServiceTest,StagingImportResourceTest' test
```

The Maven wrapper could not bootstrap because this sandbox could not DNS-resolve `repo.maven.apache.org`. The new JUnit/Quarkus tests are included for normal CI execution but were therefore not executed in this environment.

Frontend runtime code was not changed by step 9.2, so no new frontend test was required for this transport-only backend step.

## Files added

- `backend/src/main/java/info/isaksson/erland/zipgithub/api/StagingImportResource.java`
- `backend/src/main/java/info/isaksson/erland/zipgithub/api/dto/StagingUploadResponse.java`
- `backend/src/main/java/info/isaksson/erland/zipgithub/staging/StagingClaimTokenFactory.java`
- `backend/src/main/java/info/isaksson/erland/zipgithub/staging/StagingSecretCodec.java`
- `backend/src/main/java/info/isaksson/erland/zipgithub/staging/StagingUploadCredential.java`
- `backend/src/main/java/info/isaksson/erland/zipgithub/staging/StagingUploadService.java`
- `backend/src/test/java/info/isaksson/erland/zipgithub/api/StagingImportResourceTest.java`
- `backend/src/test/java/info/isaksson/erland/zipgithub/staging/StagingSecretCodecSelfTest.java`
- `backend/src/test/java/info/isaksson/erland/zipgithub/staging/StagingUploadCredentialTest.java`
- `backend/src/test/java/info/isaksson/erland/zipgithub/staging/StagingUploadServiceTest.java`
- `docs/staging-upload.md`
- `docs/step-9.2-report.md`

## Files modified

- `.env.example`
- `CHANGELOG.md`
- `VERSION`
- `backend/src/main/java/info/isaksson/erland/zipgithub/security/CsrfProtectionFilter.java`
- `backend/src/main/java/info/isaksson/erland/zipgithub/security/RequestRateLimitFilter.java`
- `backend/src/main/resources/application.properties`
- `docker-compose.yml`
- `docs/api-contract.md`
- `docs/implementation-status.md`
- `docs/phase8-plus-continuation-handoff.md`
- `docs/release-checklist.md`
- `docs/shortcut-stagingimport-design.md`
- `docs/threat-model.md`
- `scripts/security-regression.sh`
- `scripts/verify-release.sh`

## Files moved

None.

## Files deleted

None.

## Next step

`9.3 — Autentiserad claim från webbläsaren`.
