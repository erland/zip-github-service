# RC43 CI correction

Date: 2026-08-07  
Repository revision: r0088  
Application version: 1.0.0-rc.43

## Scope

Correct the three CI regressions reported after r0087 without implementing phase 9 or changing the intended step-8.3 behavior.

## Corrections

### Structure/security CI shell execution

The ZIP contains executable shell files, but ZIP-to-GitHub delivery cannot be relied on to preserve Git executable mode metadata. The CI workflow now invokes repository shell scripts with `bash ./scripts/...` and invokes the Maven wrapper with `bash ./mvnw ...`. This removes an unnecessary dependency on the tracked executable bit while keeping the same scripts and checks.

### Backend compilation

`GitHubAppClient.hasActionsWritePermission` referenced an undefined `apiBase` variable. The installation permission request now uses the same explicit `https://api.github.com` base URL used by the rest of this client. No permission semantics changed.

### Frontend regression test

The step-8.3 test used an unscoped `getByText` query for the Work branch. The branch is intentionally rendered both in delivery metadata and in the controlled-Actions confirmation text, so Testing Library correctly reported multiple matches. The assertion is now scoped with `within(...)` to the `Kontrollerade Actions` section. Production UI behavior is unchanged.

## Security/invariant impact

None. Owner checks, current-Work ref/commit validation, workflow allowlists, Actions-write permission checks, persistent audit/idempotency, immutable import approval and non-force delivery remain unchanged. No phase-9 or AI/integration functionality was added.

## Verification performed

Successful:

- `bash scripts/verify-implementation-status.sh`
- `bash scripts/verify-structure.sh`
- `bash scripts/security-regression.sh`
- `bash scripts/verify-source-tracking.sh`
- `bash scripts/verify-release.sh`
- `bash -n` over repository shell scripts and Maven wrapper
- static verification that every repository shell entrypoint in `.github/workflows/ci.yml` is invoked through `bash`
- parsed `.github/workflows/ci.yml` successfully with the available YAML parser
- compiled and ran `ActionsControlRulesSelfTest` directly with installed `javac`/`java`; it passed
- static source verification confirms the undefined `apiBase` reference is removed from `GitHubAppClient`

Environment-limited:

- `cd backend && bash ./mvnw --batch-mode --no-transfer-progress verify` could not bootstrap Maven because DNS access to `repo.maven.apache.org` is unavailable (`curl: (6) Could not resolve host`)
- `cd frontend && npm ci` failed because the sandbox npm proxy returned `404 Not Found` for `yallist-3.1.1.tgz`; therefore Vitest and the production frontend build could not be run here

The reported CI failures are therefore corrected at source, and the repository/static gates pass, but the full Maven/Vitest/build verification remains for the normal GitHub CI environment.

## Files added

- `docs/rc43-ci-correction.md`

## Files modified

- `.github/workflows/ci.yml`
- `VERSION`
- `CHANGELOG.md`
- `backend/src/main/java/info/isaksson/erland/zipgithub/github/GitHubAppClient.java`
- `docs/implementation-status.md`
- `docs/phase8-plus-continuation-handoff.md`
- `frontend/src/pages/ImportResultPage.test.tsx`
- `scripts/verify-release.sh`

## Files moved

None.

## Files deleted

None.

## Next step

`9.1 — Define and persist the StagingImport lifecycle` remains `NEXT`.
