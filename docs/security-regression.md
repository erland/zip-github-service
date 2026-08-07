# Final security regression

## Automated baseline

`scripts/security-regression.sh` is a fail-fast release check executed by CI. It verifies:

- no Docker socket in active runtime configuration;
- no probable committed token/private-key material or private-key files;
- same-origin CSRF marker and Origin validation;
- request throttling and security headers;
- restricted credentialed CORS configuration;
- ZIP symlink and compression-ratio protections;
- non-interactive Git and absence of force-push code.

The script supplements, but does not replace, backend unit/integration tests.

## Security fixtures

The backend test suite contains fixtures/self-tests for:

- traversal, absolute paths, duplicate/case collisions, symlinks and special files;
- compressed/uncompressed/file-count/single-file and compression-ratio limits;
- deterministic normalization and hashing;
- import-policy blockers;
- immutable plan digest and exact approval;
- verified workspace application;
- atomic Git delivery, retry classification and PR reuse;
- CSRF origin matching, session expiry and rate limiting.

## Cleanup and recovery scenarios

Verified in code/tests or static inspection:

- failed uploads delete partial files;
- expired uploads and metadata are removed by retention cleanup;
- snapshot workspaces are removed after inventory;
- failed workspace preparation deletes the workspace;
- successful delivery deletes the retained workspace;
- temporary askpass files are removed and authenticated remotes are not retained;
- backup restore requires explicit destructive confirmation and checksum verification when present.

## Environment-dependent acceptance checks

The current execution environment has no GitHub App credentials, Docker daemon or disposable PostgreSQL service. Therefore these checks are mandatory in CI/release infrastructure before MVP release:

1. full `./mvnw verify` and frontend tests/build;
2. live import against a dedicated test repository;
3. PostgreSQL backup and restore drill;
4. container health/readiness and retention cleanup drill;
5. real mobile/browser accessibility checks.


## Flexible review invariants (step 7.10)

The automated security regression additionally asserts that:

- hard-blocked paths are rejected by selection creation;
- overridable paths require an explicit audit acknowledgement;
- approvals are bound to `selectionDigestSha256`;
- workspace preparation contains defense-in-depth hard-block and override checks;
- the workspace verifies the complete Git diff against the selected path set;
- delivery retains the stale-base/work-branch guard.

JUnit/self-tests add behavioral coverage for mixed ordinary/blocked/deletion trees, exact path application, excluded-file preservation and stale branch movement.
