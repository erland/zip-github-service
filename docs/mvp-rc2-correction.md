# MVP RC2 correction — r0042

Date: 7 August 2026  
Product version: `1.0.0-rc.2`

## Reason

Full local compilation and test execution of `r0041` exposed merge regressions that were not caught by the earlier environment-limited checks. This revision corrects those issues without advancing the implementation-step ledger; phase 8.1 remains the single `NEXT` step.

## Backend corrections

1. `ImportResource` now imports `AppliedImportWorkspaceResponse`.
2. The public ownership-check helper is named `assertOwnedImport`; the private lookup helper remains `requireOwnedImport` and returns `OwnedImport`. This removes the duplicate-signature error and restores typed callers.
3. `ImportHistoryResponse.pullRequestNumber` is `Long`, matching the `long` value returned by `PullRequestResult`. This removes the conditional-expression type error and the downstream stream/comparator inference error.

## Frontend correction

`App.test.tsx` now mocks the API requests made by `ProjectDetailPage` after phase 6.3 moved the page from demo data to live API data. The routing test waits asynchronously for the project heading and global fetch stubs are cleared after every test.

## Verification performed here

- Release verification script passed after updating it to RC2/r0042.
- Structure verification passed.
- Security regression passed.
- Implementation-status consistency passed.
- Shell syntax passed.
- XML/JSON/YAML parsing passed.
- ZIP integrity passed.

Full Maven and Vitest execution could not be run in this execution environment because Maven cannot be downloaded from `repo.maven.apache.org` and frontend dependencies are not installed. The fixes directly address the compiler/test failures reported from a full local environment and should be verified there and by GitHub Actions before deployment.

## Changed files

### Modified

- `backend/src/main/java/info/isaksson/erland/zipgithub/api/ImportResource.java`
- `backend/src/main/java/info/isaksson/erland/zipgithub/application/ProjectApplicationService.java`
- `backend/src/main/java/info/isaksson/erland/zipgithub/api/dto/ImportHistoryResponse.java`
- `frontend/src/App.test.tsx`
- `VERSION`
- `CHANGELOG.md`
- `README.md`
- `docs/mvp-release.md`
- `docs/release-checklist.md`
- `docs/implementation-status.md`
- `scripts/verify-release.sh`

### Added

- `docs/mvp-rc2-correction.md`

No files were moved or deleted.
