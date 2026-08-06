# Step 1.3 report — API skeleton and error contract

Date: 2026-08-06  
Revision: `r0008`

## Outcome

Implemented the first project/import API boundary with an in-memory application service, owner-scoped access, a temporary request identity adapter and a consistent RFC 7807-style `application/problem+json` contract.

The temporary `X-Zip-Github-User` UUID header exists only to make ownership behavior executable before GitHub sessions are implemented in step 2.2. It is explicitly documented as non-production authentication.

## Delivered behavior

- List and create projects for the current user.
- Read a project only when owned by the current user.
- Create an empty import session for an owned project.
- Read an import only when owned by the current user.
- Return `404` for cross-user project/import access.
- Return machine-readable problem responses with correlation IDs.
- Generate an OpenAPI skeleton from JAX-RS resources and DTO records.

## Verification

Performed:

- `bash scripts/verify-structure.sh` — passed.
- Parsed `backend/pom.xml` as XML — passed.
- Static inspection confirmed all user-owned lookups compare `ownerUserId`.
- API tests were added for authentication, validation and cross-user isolation.
- ZIP integrity test — passed after packaging.

Not executable in this environment:

- Maven/Quarkus tests, because Maven remains unavailable.
- Full OpenAPI generation, because it requires the Quarkus Maven build.

## Changed files

### Added

- `backend/src/main/java/info/isaksson/erland/zipgithub/security/CurrentUserProvider.java`
- `backend/src/main/java/info/isaksson/erland/zipgithub/application/ProjectApplicationService.java`
- `backend/src/main/java/info/isaksson/erland/zipgithub/api/ProjectResource.java`
- `backend/src/main/java/info/isaksson/erland/zipgithub/api/ImportResource.java`
- `backend/src/main/java/info/isaksson/erland/zipgithub/api/dto/CreateProjectRequest.java`
- `backend/src/main/java/info/isaksson/erland/zipgithub/api/dto/ProjectResponse.java`
- `backend/src/main/java/info/isaksson/erland/zipgithub/api/dto/CreateImportRequest.java`
- `backend/src/main/java/info/isaksson/erland/zipgithub/api/dto/ImportResponse.java`
- `backend/src/main/java/info/isaksson/erland/zipgithub/api/error/ApiException.java`
- `backend/src/main/java/info/isaksson/erland/zipgithub/api/error/ApiExceptionMapper.java`
- `backend/src/main/java/info/isaksson/erland/zipgithub/api/error/UnexpectedExceptionMapper.java`
- `backend/src/main/java/info/isaksson/erland/zipgithub/api/error/ProblemDetails.java`
- `backend/src/test/java/info/isaksson/erland/zipgithub/api/ProjectResourceTest.java`
- `docs/api-contract.md`
- `docs/step-1.3-report.md`

### Modified

- `docs/implementation-status.md`

### Moved

- None.

### Deleted

- None.

## Follow-up

Step 1.4 will add frontend routes and screens against this API skeleton. Persistence adapters and GitHub-backed authentication remain intentionally outside this step.
