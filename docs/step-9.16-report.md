# Step 9.16 report — PR lifecycle, continued Work and external branch changes

## Result

Step 9.16 is complete in revision r0123 / 1.0.0-rc.75.

A pull request is now a phase of the same logical Work rather than its terminal event. Work persists as `PR_OPEN` while the PR is open and as `PR_CLOSED` when closed without merge. GitHub PR state is refreshed when the Work view is loaded; a merged PR transitions to `MERGED` and is no longer returned as active Work, so a subsequent Work starts from the repository's then-current default branch.

New ZIP imports remain allowed in `PR_OPEN`/`PR_CLOSED`. They snapshot the existing Work branch exactly as sequential imports already did, so pushing another commit to an open PR branch updates the same GitHub PR automatically. PR discussion, approvals, inline review and merge strategy remain GitHub responsibilities.

The Work API now also reports the current remote Work HEAD separately from zip-github's last delivered HEAD. Actions/check reads use the remote SHA. This makes CI failures from commits made through GitHub or another tool visible in zip-github without pretending those commits were produced by zip-github.

When review is prepared after the branch has moved beyond zip-github's last known delivery, the backend compares the two GitHub commits and exposes the changed paths. Review marks ZIP changes that overlap those paths, provides an `Externa ändringar` filter and requires an explicit UI acknowledgement if an overlapping path remains selected. This warning is intentionally not a policy blocker. The existing delivery SHA invariant remains authoritative: if the branch moves again after snapshot/review, delivery must still fail as stale instead of committing unreviewed state.

## Files added

- `backend/src/main/java/info/isaksson/erland/zipgithub/api/dto/ExternalBranchChangesResponse.java`
- `backend/src/main/resources/db/migration/V14__pull_request_work_lifecycle.sql`
- `docs/step-9.16-report.md`

## Files modified

- `CHANGELOG.md`
- `VERSION`
- `backend/src/main/java/info/isaksson/erland/zipgithub/api/ImportResource.java`
- `backend/src/main/java/info/isaksson/erland/zipgithub/api/ProjectResource.java`
- `backend/src/main/java/info/isaksson/erland/zipgithub/api/dto/WorkSessionResponse.java`
- `backend/src/main/java/info/isaksson/erland/zipgithub/application/ProjectApplicationService.java`
- `backend/src/main/java/info/isaksson/erland/zipgithub/github/GitHubAppClient.java`
- `backend/src/main/java/info/isaksson/erland/zipgithub/github/GitHubBranchClient.java`
- `backend/src/main/java/info/isaksson/erland/zipgithub/github/GitHubPullRequestClient.java`
- `backend/src/main/java/info/isaksson/erland/zipgithub/persistence/WorkPersistenceStore.java`
- `backend/src/test/java/info/isaksson/erland/zipgithub/application/WorkLifecycleServiceTest.java`
- `docs/api-contract.md`
- `docs/domain-model.md`
- `docs/implementation-status.md`
- `docs/implementation-steps.md`
- `docs/phase8-plus-continuation-handoff.md`
- `docs/pull-request-and-result-metadata.md`
- `frontend/src/api/imports.ts`
- `frontend/src/api/projects.ts`
- `frontend/src/components/ReviewFileTree.tsx`
- `frontend/src/pages/ImportReviewPage.test.tsx`
- `frontend/src/pages/ImportReviewPage.tsx`
- `frontend/src/pages/ProjectDetailPage.test.tsx`
- `frontend/src/pages/ProjectDetailPage.tsx`
- `scripts/verify-release.sh`

## Files moved/deleted

None.

## Verification

Repository structure/security/source/release/phase-9 verification scripts are run before packaging. All TypeScript/TSX source files are parser/transpiler checked with the installed TypeScript compiler. Full Maven compilation/tests cannot run in this sandbox because the Maven wrapper cannot resolve `repo.maven.apache.org`; GitHub CI remains the dependency-backed validation environment.

The existing stale Work-branch delivery regression remains part of the release gate and was not weakened.
