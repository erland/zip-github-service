# Step 4.2 report — hash-based comparison

## Delivered

- SHA-256 content hashes for repository blobs.
- Deterministic comparison model and service.
- Classification of `ADDED`, `MODIFIED`, `UNCHANGED` and `WOULD_DELETE`.
- Comparison API endpoint tied to user-owned import, upload and immutable snapshot.
- Unit and standalone determinism/classification tests.

## Verification

- Java 21 compilation of snapshot and comparison components with minimal framework annotation stubs: passed.
- Standalone comparison self-test: passed.
- Maven test attempt: blocked because this execution environment cannot resolve `repo.maven.apache.org`.
- Structure/status and ZIP integrity checks: passed.

## Scope boundary

No policy is applied yet. In particular, `WOULD_DELETE` is reported but not blocked until step 4.3.
