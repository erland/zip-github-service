# Scripts

- `verify-structure.sh` confirms that the active source tree exists and contains no legacy worker/run implementation references.
- `verify-implementation-status.sh` verifies that exactly one implementation step is `NEXT` and matches the current-position section.
- `github-app-spike.sh` exercises the isolated GitHub App technical spike against an explicitly configured test repository.
- `postgres-backup.sh` creates a checksummed PostgreSQL custom-format backup and prunes expired backup files.
- `postgres-restore.sh` performs an explicitly confirmed destructive restore from a custom-format backup.

## security-regression.sh

Runs the release security baseline: no active Docker socket, no probable committed secrets, CSRF/origin/rate-limit/security-header invariants, ZIP controls and non-force Git delivery. It is executed by CI.

## Release verification

`./scripts/verify-release.sh` verifies the version marker, required MVP release artifacts, final implementation ledger and the structure/security regression baselines. It does not replace backend/frontend builds or external GitHub, Docker, PostgreSQL and device acceptance tests.

- `verify-source-tracking.sh` verifies required source files exist and are not accidentally ignored by Git.
