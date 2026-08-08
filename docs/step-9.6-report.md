# Step 9.6 report — retention, abuse protection and staging security regression

Date: 2026-08-08  
Repository revision: `r0098`  
Application version: `1.0.0-rc.50`

## Scope

Implemented only phase 9 step 9.6. Step 9.7 Shortcut distribution and step 9.8 final E2E/release regression are not implemented here.

## Implemented

- Added separate configurable claimed grace TTL (default four hours); first successful claim replaces the short AVAILABLE deadline and same-owner retry does not extend it indefinitely.
- Added scheduled, bounded staging cleanup with restart-safe physical-deletion marker (`artifact_deleted_at`).
- Persisted the original ordinary upload retention deadline independently from staging lifecycle expiry so promotion does not shorten ordinary Import retention.
- Coordinated promotion and cleanup with the same PostgreSQL row lock; cleanup uses `FOR UPDATE SKIP LOCKED`.
- Added crash-window reconciliation: a persisted ordinary Import with `source_reference=staging-import:<id>` repairs a stale CLAIMED row to PROMOTED before cleanup can delete its ZIP.
- Added durable deployment-level staging object/byte quotas with serialized PostgreSQL quota accounting.
- Preserved existing compressed/expanded ZIP safety limits and added retryable `429 STAGING_CAPACITY_EXCEEDED` for staging quota exhaustion.
- Added optional trusted-proxy network-source rate limiting in addition to per-capability and global staging limits. It is disabled by default.
- Documented immediate credential revoke/rotation: replace the deployment secret and redeploy/restart; no staging/database migration and no current/previous grace generation are required.
- Added regression coverage for old credential rejection after rotation, claimed grace, storage-capacity rejection cleanup, physical cleanup retry behavior and promotion-lock usage.

## Security invariants

- Upload credential grants staging-create only.
- Claim tokens remain hash-only at rest and are separate from upload credential rotation.
- Existing staging rows survive upload-credential rotation and keep their own TTL/claim rules.
- Cleanup never deletes a PROMOTED artifact.
- Promotion and cleanup cannot race on the same staging row across backend instances.
- No staging route reaches GitHub before authenticated, owner/project-checked promotion.
- Optional `X-Forwarded-For` use is opt-in only behind a trusted ingress.

## Verification performed in this environment

Passed:

- `bash scripts/verify-implementation-status.sh`
- `bash scripts/verify-structure.sh`
- `bash scripts/security-regression.sh`
- `bash scripts/verify-source-tracking.sh`
- `bash scripts/verify-release.sh`
- shell syntax checks for repository scripts
- dependency-free staging lifecycle/secret self-tests that can be compiled with local `javac`
- static migration/config checks for V12 retention columns, cleanup indexes, row-lock/`SKIP LOCKED` coordination and quota configuration
- final ZIP integrity/top-level-folder and Unix executable-mode checks

Environment limitations:

- Full Maven/JUnit/Quarkus verification could not start because the Maven wrapper could not DNS-resolve `repo.maven.apache.org` in this sandbox.
- Frontend dependencies were not changed in 9.6; no frontend runtime code was added. npm/Vitest was therefore not required for the code path, and the known sandbox npm proxy limitation remains documented from earlier revisions.

Normal CI should run the complete backend test suite, including the new JUnit regressions.

## Files added

- `backend/src/main/java/info/isaksson/erland/zipgithub/staging/StagingCapacityExceededException.java`
- `backend/src/main/java/info/isaksson/erland/zipgithub/staging/StagingRetentionService.java`
- `backend/src/main/resources/db/migration/V12__staging_retention_and_cleanup.sql`
- `backend/src/test/java/info/isaksson/erland/zipgithub/staging/StagingRetentionServiceTest.java`
- `docs/staging-retention-and-abuse.md`
- `docs/step-9.6-report.md`

## Files modified

- `.env.example`
- `CHANGELOG.md`
- `VERSION`
- `backend/src/main/java/info/isaksson/erland/zipgithub/api/StagingImportResource.java`
- `backend/src/main/java/info/isaksson/erland/zipgithub/domain/model/StagingImport.java`
- `backend/src/main/java/info/isaksson/erland/zipgithub/persistence/StagingImportPersistenceStore.java`
- `backend/src/main/java/info/isaksson/erland/zipgithub/persistence/entity/StagingImportEntity.java`
- `backend/src/main/java/info/isaksson/erland/zipgithub/security/RequestRateLimitFilter.java`
- `backend/src/main/java/info/isaksson/erland/zipgithub/staging/StagingClaimService.java`
- `backend/src/main/java/info/isaksson/erland/zipgithub/staging/StagingPromotionService.java`
- `backend/src/main/java/info/isaksson/erland/zipgithub/staging/StagingUploadService.java`
- `backend/src/main/resources/application.properties`
- `backend/src/test/java/info/isaksson/erland/zipgithub/api/StagingImportResourceTest.java`
- `backend/src/test/java/info/isaksson/erland/zipgithub/domain/StagingImportLifecycleSelfTest.java`
- `backend/src/test/java/info/isaksson/erland/zipgithub/domain/StagingImportLifecycleTest.java`
- `backend/src/test/java/info/isaksson/erland/zipgithub/staging/StagingClaimServiceTest.java`
- `backend/src/test/java/info/isaksson/erland/zipgithub/staging/StagingPromotionServiceTest.java`
- `backend/src/test/java/info/isaksson/erland/zipgithub/staging/StagingUploadCredentialTest.java`
- `backend/src/test/java/info/isaksson/erland/zipgithub/staging/StagingUploadServiceTest.java`
- `docker-compose.yml`
- `docs/api-contract.md`
- `docs/implementation-status.md`
- `docs/operations.md`
- `docs/phase8-plus-continuation-handoff.md`
- `docs/release-checklist.md`
- `docs/shortcut-stagingimport-design.md`
- `docs/staging-promotion.md`
- `docs/staging-claim.md`
- `docs/staging-upload.md`
- `docs/threat-model.md`
- `scripts/security-regression.sh`
- `scripts/verify-release.sh`

## Files moved

None.

## Files deleted

None.

## Next step

`9.7 — Distribuera en signerad referens-Shortcut för iOS`.
