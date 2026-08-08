# Step 8.1 report — workflow runs and jobs

Date: 2026-08-07  
Repository revision: r0085  
Application version: 1.0.0-rc.40

## Baseline verification

The supplied r0084 ZIP matched the documented continuation point:

- `docs/implementation-status.md` had exactly `8.1` as `NEXT`;
- step 7.24 and all prerequisites were `DONE`;
- former 8.4 remained `SKIPPED` and explicitly moved to the future backlog;
- phase 9 remained entirely pending;
- `VERSION` was `1.0.0-rc.39`;
- the phase-7 Work/import flow and existing basic check-status integration described by the handoff were present.

No code/status discrepancy requiring correction was found before implementing 8.1.

## Implemented

- Added owner-scoped `GET /api/imports/{importId}/actions` for the exact delivered commit SHA.
- Reused short-lived GitHub App installation tokens; browser clients receive no GitHub credentials.
- Added bounded workflow-run/job/check reads and a small stable status model.
- Added server-side observation caching: 8 seconds for active/non-terminal status and 5 minutes for terminal status.
- Added result-page polling with increasing delay, a hard observation cap and immediate stop on terminal aggregate state.
- Added a workflow/job/check overview with direct GitHub links and graceful `not_started`/`unavailable` handling.
- Kept the existing check-status endpoint intact for compatibility.
- Added GitHub App setup documentation for the required read-only Actions permission.

No artifact/log retrieval, workflow dispatch/rerun, phase-9 staging or AI/integration backlog work was implemented.

## Security and consistency invariants

- Every Actions read starts from the existing owner-scoped import/delivery lookup.
- Repository access uses the existing installation id and server-side short-lived installation-token provider.
- No new credential persistence or frontend token exposure was introduced.
- The endpoint is read-only and cannot trigger/rerun workflows.
- Immutable plan/selection/approval, exact base SHA, Work single-active-import and non-force delivery behavior are unchanged.

## Verification actually performed

Successful:

- extracted and inventoried the supplied complete ZIP;
- read `AGENTS.md`, handoff, status ledger and step 8.1 scope before changes;
- compiled and ran `GitHubActionsStatusMapperSelfTest` directly with the installed JDK using `javac`/`java`;
- `./scripts/verify-implementation-status.sh` — passed;
- `./scripts/verify-structure.sh` — passed;
- `./scripts/security-regression.sh` — passed;
- `./scripts/verify-source-tracking.sh` — passed;
- `./scripts/verify-release.sh` — passed.

Environment-limited:

- `backend/./mvnw -DskipTests compile` could not bootstrap Maven because DNS/network access to `repo.maven.apache.org` is unavailable in this environment;
- `frontend/npm ci` failed because the sandbox npm proxy returned `404 Not Found` for `yallist-3.1.1.tgz`; consequently `node_modules` remained unavailable and `npm test -- --run` could not start because `vitest` is not installed locally;
- no live GitHub App installation/repository credentials were available, so no live Actions API call was made.

Full Maven tests and frontend `npm ci && npm test && npm run build` remain required in normal local/CI verification with dependency access.

## Files added

- `backend/src/main/java/info/isaksson/erland/zipgithub/actions/ImportActionsStatus.java`
- `backend/src/main/java/info/isaksson/erland/zipgithub/actions/ImportActionsStatusService.java`
- `backend/src/main/java/info/isaksson/erland/zipgithub/api/dto/ImportActionsStatusResponse.java`
- `backend/src/main/java/info/isaksson/erland/zipgithub/github/GitHubActionsClient.java`
- `backend/src/main/java/info/isaksson/erland/zipgithub/github/GitHubActionsStatusMapper.java`
- `backend/src/test/java/info/isaksson/erland/zipgithub/github/GitHubActionsStatusMapperSelfTest.java`
- `docs/workflow-runs-and-jobs.md`
- `docs/step-8.1-report.md`

## Files modified

- `VERSION`
- `CHANGELOG.md`
- `backend/src/main/java/info/isaksson/erland/zipgithub/api/ImportResource.java`
- `backend/src/main/java/info/isaksson/erland/zipgithub/github/GitHubAppClient.java`
- `docs/api-contract.md`
- `docs/github-app-setup.md`
- `docs/implementation-status.md`
- `docs/phase8-plus-continuation-handoff.md`
- `docs/release-checklist.md`
- `frontend/src/api/imports.ts`
- `frontend/src/pages/ImportResultPage.tsx`
- `frontend/src/pages/ImportResultPage.test.tsx`
- `frontend/src/styles/global.css`
- `scripts/verify-release.sh`

## Files moved

None.

## Files deleted

None.

## Next step

`8.2 — Artifacts and condensed errors` is `NEXT`.
