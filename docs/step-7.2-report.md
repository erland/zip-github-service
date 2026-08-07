# Step 7.2 report — web and API security hardening

## Completed

- Added same-origin CSRF enforcement for unsafe API calls.
- Added required frontend request marker for fetch and streaming XHR upload.
- Restricted credentialed CORS to the configured frontend origin and explicit methods/headers.
- Added defensive response headers and no-store API caching.
- Added correlation-ID server logging for unexpected failures while keeping client details generic.
- Added exact-origin unit tests and documented production security requirements.

## Changed files

### Added
- `backend/src/main/java/info/isaksson/erland/zipgithub/security/SameOriginPolicy.java`
- `backend/src/main/java/info/isaksson/erland/zipgithub/security/CsrfProtectionFilter.java`
- `backend/src/main/java/info/isaksson/erland/zipgithub/security/SecurityHeadersFilter.java`
- `backend/src/test/java/info/isaksson/erland/zipgithub/security/SameOriginPolicyTest.java`
- `docs/web-and-api-security-hardening.md`
- `docs/step-7.2-report.md`

### Modified
- `backend/src/main/java/info/isaksson/erland/zipgithub/api/error/ApiException.java`
- `backend/src/main/java/info/isaksson/erland/zipgithub/api/error/ApiExceptionMapper.java`
- `backend/src/main/java/info/isaksson/erland/zipgithub/api/error/UnexpectedExceptionMapper.java`
- `backend/src/main/resources/application.properties`
- `frontend/src/api/imports.ts`
- `docs/implementation-status.md`

No files were moved or deleted.

## Verification

- Pure Java origin-policy compilation and self-check.
- XML/JSON, shell, structure and implementation-ledger checks.
- Static inspection that every unsafe frontend import request carries the CSRF marker.
- ZIP integrity test.

Full Maven/npm execution is delegated to the user's local environment and GitHub Actions.
