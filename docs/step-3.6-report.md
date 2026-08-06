# Step 3.6 report — Complete CI baseline

Date: 6 August 2026  
Revision: `r0019`

## Delivered

- root `.gitignore` for secrets, IDE files, backend/frontend outputs, temporary uploads/workspaces and generated release packages;
- pinned project-local Maven bootstrap for Maven 3.9.11 on Unix and Windows;
- GitHub Actions workflow with independent structure/security, backend and frontend jobs;
- implementation-ledger verification script;
- documentation and plan updates that place step 3.6 before phase 4.

## Verification performed in this environment

Passed:

- `bash -n backend/mvnw`;
- shell syntax for all active scripts;
- `scripts/verify-structure.sh`;
- `scripts/verify-implementation-status.sh`;
- XML parsing of `backend/pom.xml`;
- JSON parsing of frontend package files;
- YAML parsing of `.github/workflows/ci.yml`;
- `.gitignore` rule smoke checks;
- ZIP integrity verification.

Not executable here:

- Maven download and `./mvnw verify`: attempted and failed because `repo.maven.apache.org` could not be resolved;
- `npm ci`: attempted and failed because the internal npm mirror returned `404` for `yallist@3.1.1`; tests and build therefore could not start;
- an actual GitHub Actions run, because the project has not yet been committed to `erland/zip-github-service`.

The workflow is the intended authoritative full build/test environment. Step 3.6 establishes the CI path; the first repository push must confirm that all three jobs pass and any code-level failures found there must be corrected before phase 4 work is merged.

## Changed files

### Added

- `.gitignore`
- `.github/workflows/ci.yml`
- `backend/mvnw`
- `backend/mvnw.cmd`
- `backend/.mvn/wrapper/maven-wrapper.properties`
- `scripts/verify-implementation-status.sh`
- `docs/ci-baseline.md`
- `docs/step-3.6-report.md`

### Modified

- `docs/implementation-steps.md`
- `docs/zip-github-development-plan-v1.1.md`
- `docs/implementation-status.md`
- `README.md`

### Moved

None.

### Removed

None.
