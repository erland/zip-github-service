# Step 9.3 report — authenticated browser claim

**Revision:** r0095  
**Application version:** 1.0.0-rc.47  
**Date:** 2026-08-08  
**Status:** DONE  
**Next:** 9.4 — project selection and promotion to ordinary Import

## Implemented

- Added authenticated `POST /api/staging-imports/claim` using the normal zip-github web session and existing same-origin CSRF protection.
- Added hash-only claim lookup and atomic owner binding under a database row lock.
- Added neutral `410 STAGING_CLAIM_UNAVAILABLE` behavior for wrong, expired, terminal, promoted or other-owner claims.
- Preserved idempotent same-owner retry after a lost response.
- Added `/staging/claim` mobile browser route. The URL fragment token is copied to same-tab `sessionStorage` and removed from the address bar; OAuth `returnTo` carries only the route, never the token.
- Added owner-safe claim response metadata and explicitly no Project/GitHub authority or promotion in this step.
- Added tests for claim-service hashing/neutral failures and frontend fragment-token capture.

## Verification performed

- `bash ./scripts/verify-implementation-status.sh` — PASS.
- `bash ./scripts/verify-structure.sh` — PASS.
- `bash ./scripts/security-regression.sh` — PASS.
- `bash ./scripts/verify-source-tracking.sh` — PASS.
- `bash ./scripts/verify-release.sh` — PASS.
- Shell syntax checks — PASS.
- Static inspection confirmed the raw claim token is not persisted and is not put in OAuth state/query parameters.
- Full Maven/JUnit/Quarkus verification was attempted but could not bootstrap because this sandbox could not resolve Maven Central.
- Frontend `npm ci`/Vitest/build was attempted but dependency installation remained blocked by the sandbox npm proxy; the added Vitest regression is therefore committed for CI execution.

## Security notes

- Claim token remains a bearer secret until ownership is bound; it is carried in an URL fragment and same-tab ephemeral storage only.
- Claim API is *not* CSRF-exempt. It requires normal session authentication plus the existing same-origin request marker/origin checks.
- A stolen/guessed token cannot reveal whether a row exists, expired, was already used or belongs to another user; these cases share one neutral response.
- Claim does not create an Import and does not touch GitHub.

## Files added

- `backend/src/main/java/info/isaksson/erland/zipgithub/api/dto/StagingClaimRequest.java`
- `backend/src/main/java/info/isaksson/erland/zipgithub/api/dto/StagingClaimResponse.java`
- `backend/src/main/java/info/isaksson/erland/zipgithub/staging/StagingClaimService.java`
- `backend/src/test/java/info/isaksson/erland/zipgithub/staging/StagingClaimServiceTest.java`
- `frontend/src/api/staging.ts`
- `frontend/src/pages/StagingClaimPage.tsx`
- `frontend/src/staging/claimToken.ts`
- `frontend/src/staging/claimToken.test.ts`
- `docs/staging-claim.md`
- `docs/step-9.3-report.md`

## Files modified

- `VERSION`
- `CHANGELOG.md`
- `backend/src/main/java/info/isaksson/erland/zipgithub/api/StagingImportResource.java`
- `backend/src/main/java/info/isaksson/erland/zipgithub/api/error/ApiException.java`
- `backend/src/main/java/info/isaksson/erland/zipgithub/persistence/StagingImportPersistenceStore.java`
- `docs/api-contract.md`
- `docs/implementation-status.md`
- `docs/phase8-plus-continuation-handoff.md`
- `docs/release-checklist.md`
- `docs/shortcut-stagingimport-design.md`
- `docs/threat-model.md`
- `frontend/src/App.tsx`
- `frontend/src/components/AppLayout.tsx`
- `frontend/src/styles/global.css`
- `scripts/security-regression.sh`
- `scripts/verify-release.sh`

## Files moved

None.

## Files deleted

None.
