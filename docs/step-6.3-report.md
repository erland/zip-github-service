# Step 6.3 report — import history and reopening

## Delivered

- Owner-scoped, newest-first import history API per project.
- History metadata for source ZIP, immutable plan and pull request.
- Server-derived reopen stage (`UPLOAD`, `REVIEW`, `RESULT`).
- Project detail page connected to the real project and history APIs.
- Reopening links for upload, review and result stages.
- Existing unfinished import sessions are reused by the upload page.
- Component test for result and review reopening routes.

## Verification

- Project structure and implementation-status checks passed.
- JSON/XML and shell syntax checks passed.
- TypeScript/TSX source structure reviewed and balanced.
- ZIP integrity passed.

Full frontend and backend suites should be run locally and by GitHub Actions.

## Changed files

### Added

- `backend/src/main/java/info/isaksson/erland/zipgithub/api/dto/ImportHistoryResponse.java`
- `frontend/src/api/projects.ts`
- `frontend/src/pages/ProjectDetailPage.test.tsx`
- `docs/import-history-and-reopening.md`
- `docs/step-6.3-report.md`

### Modified

- `backend/src/main/java/info/isaksson/erland/zipgithub/application/ProjectApplicationService.java`
- `backend/src/main/java/info/isaksson/erland/zipgithub/api/ProjectResource.java`
- `frontend/src/api/imports.ts`
- `frontend/src/pages/ProjectDetailPage.tsx`
- `frontend/src/pages/NewImportPage.tsx`
- `frontend/src/styles/global.css`
- `docs/api-contract.md`
- `docs/implementation-status.md`

No files were moved or deleted.
