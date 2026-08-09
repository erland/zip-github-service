# rc.72 frontend staging-promotion build correction

## Observed CI failure

GitHub Actions run `31297765981`, frontend job `93205529655`, passed all 51 Vitest tests and then failed during `npm run build` / `tsc -b`.

`StagingClaimPage` correctly passed the repository-first promotion target as either `{ projectId }` or `{ githubInstallationId, githubRepositoryId }`, while `frontend/src/api/staging.ts` still exposed the pre-9.13 signature `promoteStagingImport(stagingId, projectId: string)`. TypeScript therefore rejected the union object before Vite could build.

## Correction

`promoteStagingImport` now accepts a `StagingPromotionTarget` union matching the backend `StagingPromotionRequest` contract and serializes the selected target directly. Focused frontend API tests cover both an existing Project and lazy repository bootstrap.

## Scope

No backend behavior changed. Step 9.13 remains complete.
