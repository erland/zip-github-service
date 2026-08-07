# Step 7.16 report — automatic upload to review preparation

Date: 2026-08-07  
Repository revision: `r0071`  
Application version: `1.0.0-rc.29`

## Outcome

The normal browser flow no longer stops after a successful ZIP upload and no longer asks the user to click **Skapa granskningsplan**. The frontend immediately starts review preparation and navigates to the existing review route when the immutable plan is ready.

A new orchestration endpoint, `POST /api/imports/{importId}/prepare-review`, reuses the existing pipeline rather than duplicating it. It returns an already stored immutable plan first, otherwise reuses an existing repository snapshot, and only resolves a new snapshot when the import has never been frozen before. It then delegates to the existing archive inventory/comparison/policy/plan path.

If automatic preparation fails after upload, the stored ZIP remains attached to the import and the UI exposes **Försök skapa granskningsplan igen**. The file input is locked in that state so recovery cannot accidentally attempt a second upload. Retry uses the same endpoint and therefore preserves any already locked snapshot/base SHA.

## Added files

- `backend/src/test/java/info/isaksson/erland/zipgithub/api/ImportReviewPreparationResourceTest.java`
- `frontend/src/pages/NewImportPage.test.tsx`
- `docs/step-7.16-report.md`

## Modified files

- `backend/src/main/java/info/isaksson/erland/zipgithub/api/ImportResource.java`
- `backend/src/main/java/info/isaksson/erland/zipgithub/application/ProjectApplicationService.java`
- `frontend/src/api/imports.ts`
- `frontend/src/pages/NewImportPage.tsx`
- `docs/api-contract.md`
- `docs/architecture.md`
- `docs/upload-streaming.md`
- `docs/implementation-status.md`
- `CHANGELOG.md`
- `VERSION`
- `scripts/verify-release.sh`

## Moved files

None.

## Deleted files

None.

## Verification intent

- backend resource regression proves repeated preparation returns the already immutable plan/base identity;
- frontend regression proves upload immediately prepares/navigates to review;
- frontend regression proves a preparation failure retries preparation without uploading the ZIP again;
- repository structure, implementation ledger, source tracking, security regression and release checks are run before packaging;
- full Maven/Vitest execution is attempted when dependency access is available.

## Follow-up

Step `7.17` should make **Godkänn valda förändringar** continue directly through workspace, exact diff verification, commit and push while preserving immutable selection/approval as the security boundary.

## Verification results

Passed locally in the packaging environment:

- `./scripts/verify-implementation-status.sh`
- `./scripts/verify-structure.sh`
- `./scripts/security-regression.sh`
- `./scripts/verify-source-tracking.sh`
- `./scripts/verify-release.sh`
- shell syntax validation for repository scripts
- JSON/YAML parsing
- TypeScript parser/transpile syntax check for `NewImportPage.tsx`, `NewImportPage.test.tsx` and `api/imports.ts`

Attempted but environment-blocked:

- `./mvnw -Dtest=ImportReviewPreparationResourceTest test` — Maven Wrapper could not resolve `repo.maven.apache.org`.

Full Maven and Vitest execution therefore remains a local/GitHub Actions verification item.
