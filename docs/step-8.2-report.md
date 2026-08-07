# Step 8.2 report — artifacts and condensed errors

Date: 2026-08-07  
Repository revision: r0086  
Application version: 1.0.0-rc.41

## Baseline verification

The supplied r0085 ZIP matched the documented continuation point:

- `docs/implementation-status.md` had exactly `8.2` as `NEXT`;
- step 8.1 and all prerequisites were `DONE`;
- `VERSION` was `1.0.0-rc.40`;
- the owner-scoped read-only workflow/job/check endpoint and result-page polling from 8.1 were present;
- former 8.4 remained `SKIPPED` in the future backlog and all phase-9 steps remained pending.

No code/status discrepancy requiring correction was found before implementing 8.2.

## Implemented

- Added owner-scoped `GET /api/imports/{importId}/actions/details` for the exact delivered commit.
- Added bounded artifact metadata (maximum 20 artifacts across maximum 10 matching workflow runs) with safe links to the owning GitHub run; artifact bytes are never downloaded or stored by zip-github.
- Added bounded failed-job log reads (24 KiB/job, maximum three excerpts) only for failed workflows/jobs.
- Added deterministic condensation for Maven/Gradle, npm/Vite, Pandoc and xcodebuild patterns; unrecognized logs produce no guessed summary.
- Added ANSI/control-character sanitization and common credential/token redaction before excerpts are returned.
- Added workflow/job/failed-step/tool/source attribution and direct GitHub job links.
- Added five-minute in-memory detail caching so result reloads do not repeatedly fetch logs/artifacts.
- Added mobile-friendly artifact and condensed-error sections to the existing result page.

No workflow dispatch/rerun, phase-9 staging or future AI/integration backlog work was implemented.

## Security and consistency invariants

- Every detail read begins with the existing authenticated owner-scoped import/delivery lookup.
- Repository reads use only server-side short-lived GitHub App installation tokens.
- Installation credentials are never forwarded to GitHub's signed log redirect target.
- Redirects are HTTPS-only and host-restricted.
- No artifact archive URL, raw log or GitHub credential is returned to the browser or persisted.
- Raw logs are bounded before parsing and discarded after condensation.
- Immutable plan/selection/approval, exact base SHA, Work single-active-import and non-force delivery behavior are unchanged.

## Verification actually performed

Successful:

- extracted and inventoried the supplied complete r0085 ZIP;
- read `AGENTS.md`, implementation status, handoff and the exact 8.2 scope;
- compiled and ran `ActionLogCondensorSelfTest` directly with the installed JDK using `javac`/`java`;
- static inspection confirmed the detail endpoint reuses owner-scoped delivery sources and server-side installation tokens;
- repository verification scripts listed below were run after the documentation/status update.

Successful repository checks after all code/documentation changes:

- `./scripts/verify-implementation-status.sh` — passed;
- `./scripts/verify-structure.sh` — passed;
- `./scripts/security-regression.sh` — passed;
- `./scripts/verify-source-tracking.sh` — passed;
- `./scripts/verify-release.sh` — passed.

Environment-limited:

- `backend/./mvnw test` could not bootstrap Maven because DNS/network access to `repo.maven.apache.org` is unavailable in this environment (`curl: (6) Could not resolve host`);
- `frontend/npm ci` failed because the sandbox npm proxy returned `404 Not Found` for `yallist-3.1.1.tgz`; therefore Vitest and the production frontend build could not be run from a clean dependency installation here;
- no live GitHub App installation/repository credentials are available in the packaging environment, so no live artifact-list or job-log redirect call was made.

Full Maven tests and frontend `npm ci && npm test -- --run && npm run build`, plus one live private-repository Actions detail verification, remain required in normal local/CI acceptance with dependency and credential access.

## Files added

- `backend/src/main/java/info/isaksson/erland/zipgithub/actions/ImportActionsDetails.java`
- `backend/src/main/java/info/isaksson/erland/zipgithub/actions/ImportActionsDetailsService.java`
- `backend/src/main/java/info/isaksson/erland/zipgithub/api/dto/ImportActionsDetailsResponse.java`
- `backend/src/main/java/info/isaksson/erland/zipgithub/github/ActionLogCondensor.java`
- `backend/src/main/java/info/isaksson/erland/zipgithub/github/GitHubActionsDetailsClient.java`
- `backend/src/test/java/info/isaksson/erland/zipgithub/github/ActionLogCondensorSelfTest.java`
- `docs/actions-artifacts-and-condensed-errors.md`
- `docs/step-8.2-report.md`

## Files modified

- `VERSION`
- `CHANGELOG.md`
- `backend/src/main/java/info/isaksson/erland/zipgithub/api/ImportResource.java`
- `backend/src/main/java/info/isaksson/erland/zipgithub/github/GitHubAppClient.java`
- `docs/api-contract.md`
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

`8.3 — Controlled workflow dispatch and rerun` is `NEXT`.
