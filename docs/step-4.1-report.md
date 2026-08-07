# Step 4.1 report — repository snapshot

## Outcome

Implemented a shallow Git repository snapshot that locks the selected import branch to one exact commit SHA and inventories the full Git tree at that SHA.

## Main decisions

- Selected shallow Git workspace over Git tree API for the first implementation.
- Resolve the branch once, then fetch the exact resolved SHA.
- Store an immutable, path-sorted tree inventory.
- Use a short-lived GitHub App installation token only through `GIT_ASKPASS`.
- Delete all temporary Git files after success or failure.

## Verification

- Added JUnit tests using a real local Git repository.
- Verified exact SHA locking, deterministic path ordering and workspace cleanup.
- Added parser coverage for NUL-delimited `git ls-tree` output.
- Ran structure and implementation-status checks.
- Validated XML, JSON, shell syntax and ZIP integrity.

Full Quarkus/Maven tests should be run locally or in GitHub Actions after unpacking this revision. The user confirmed the previous revision built and tested successfully before step 4.1 began.

## Limitations and follow-ups

- Snapshot metadata is still stored in the temporary in-memory application store.
- Step 4.2 adds stable content SHA-256 values and ZIP-versus-repository classification.
- The snapshot currently inventories regular committed tree entries; submodule policy is handled explicitly in later import policy work.
- Git must be installed in the backend runtime image.

## Files changed

### Added

- `backend/src/main/java/info/isaksson/erland/zipgithub/github/GitHubInstallationTokenProvider.java`
- `backend/src/main/java/info/isaksson/erland/zipgithub/snapshot/RepositorySnapshot.java`
- `backend/src/main/java/info/isaksson/erland/zipgithub/snapshot/RepositorySnapshotEntry.java`
- `backend/src/main/java/info/isaksson/erland/zipgithub/snapshot/RepositorySnapshotException.java`
- `backend/src/main/java/info/isaksson/erland/zipgithub/snapshot/RepositorySnapshotService.java`
- `backend/src/main/java/info/isaksson/erland/zipgithub/api/dto/RepositorySnapshotResponse.java`
- `backend/src/test/java/info/isaksson/erland/zipgithub/snapshot/RepositorySnapshotServiceTest.java`
- `backend/src/test/java/info/isaksson/erland/zipgithub/snapshot/RepositorySnapshotParserSelfTest.java`
- `docs/repository-snapshot.md`
- `docs/step-4.1-report.md`

### Modified

- `.env.example`
- `backend/src/main/java/info/isaksson/erland/zipgithub/github/GitHubAppClient.java`
- `backend/src/main/java/info/isaksson/erland/zipgithub/application/ProjectApplicationService.java`
- `backend/src/main/java/info/isaksson/erland/zipgithub/api/ImportResource.java`
- `backend/src/main/java/info/isaksson/erland/zipgithub/api/error/ApiException.java`
- `backend/src/main/resources/application.properties`
- `docs/api-contract.md`
- `docs/implementation-status.md`

### Moved or deleted

None.
