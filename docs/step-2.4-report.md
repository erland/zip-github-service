# Step 2.4 report — connect project configuration to GitHub

Date: 2026-08-06  
Revision: r0013

## Outcome

Project creation and editing now require a verified GitHub App installation, repository and branch. Verification is performed with the authenticated user's server-side GitHub user access token. Project responses contain stable GitHub identifiers and safe repository metadata, never credentials.

## Implemented behavior

- Extended create-project contract with installation ID and repository ID.
- Added `PATCH /api/projects/{projectId}`.
- Verifies installation visibility before repository access.
- Verifies repository membership in the user-scoped installation.
- Uses the repository default branch when no branch is supplied.
- Verifies the selected branch exists before storing the project.
- Retains owner isolation for list, get, update and import creation.
- Prevents imports from inactive projects.
- Added an injectable GitHub project catalogue abstraction for deterministic tests.

## Verification

Passed:

- Project structure verification.
- XML validation of `backend/pom.xml`.
- JSON validation of package files.
- Shell syntax checks.
- Static check that project create/update requires installation and repository identifiers.
- Static check that no GitHub token appears in `ProjectResponse`.
- Static check that exactly one implementation step is `NEXT`.
- ZIP integrity verification.

Added but not executed in this environment:

- Quarkus API tests with a mocked GitHub catalogue.
- Pure configuration-service test for visible/invisible installation behavior.

Maven remains unavailable in the execution environment, so the JUnit/Quarkus suite could not be run here.

## Files changed

### Added

- `backend/src/main/java/info/isaksson/erland/zipgithub/github/GitHubProjectCatalog.java`
- `backend/src/main/java/info/isaksson/erland/zipgithub/application/GitHubProjectConfigurationService.java`
- `backend/src/main/java/info/isaksson/erland/zipgithub/api/dto/UpdateProjectRequest.java`
- `backend/src/test/java/info/isaksson/erland/zipgithub/application/GitHubProjectConfigurationServiceTest.java`
- `docs/github-project-configuration.md`
- `docs/step-2.4-report.md`

### Modified

- `backend/pom.xml`
- `backend/src/main/java/info/isaksson/erland/zipgithub/github/GitHubAppClient.java`
- `backend/src/main/java/info/isaksson/erland/zipgithub/application/ProjectApplicationService.java`
- `backend/src/main/java/info/isaksson/erland/zipgithub/api/ProjectResource.java`
- `backend/src/main/java/info/isaksson/erland/zipgithub/api/dto/CreateProjectRequest.java`
- `backend/src/main/java/info/isaksson/erland/zipgithub/api/dto/ProjectResponse.java`
- `backend/src/test/java/info/isaksson/erland/zipgithub/api/ProjectResourceTest.java`
- `docs/api-contract.md`
- `docs/implementation-status.md`

### Moved or deleted

None.
