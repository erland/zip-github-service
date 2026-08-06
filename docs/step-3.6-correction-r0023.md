# Correction r0023 — Quarkus test state isolation

## Problem

`ProjectResourceTest` uses an `@ApplicationScoped` in-memory `ProjectApplicationService`. Quarkus keeps that bean alive for the test application, so projects created by one test remained visible to later tests. The `unknownRepositoryIsRejected` test therefore hit the duplicate project-name rule before GitHub repository validation.

## Correction

- Added an explicit test reset method to the temporary in-memory application store.
- `ProjectResourceTest` clears projects, imports, and upload metadata in `@BeforeEach`.
- Production request behavior is unchanged.

The reset hook is temporary and should be removed when the application service is backed by transactional database repositories.

## Expected result

Every test starts from an empty in-memory application state. `unknownRepositoryIsRejected` can now reach the intended GitHub repository validation and return `404 GITHUB_REPOSITORY_NOT_FOUND`.

## Mockito warning

The Mockito/Byte Buddy messages are warnings from inline mocking on a recent JDK and are not the cause of this failure. Agent configuration can be hardened separately if the warning becomes an error on a future JDK.
