# Scripts

- `verify-structure.sh` confirms that the active source tree exists and contains no legacy worker/run implementation references.
- `verify-implementation-status.sh` verifies that exactly one implementation step is `NEXT` and matches the current-position section.
- `github-app-spike.sh` exercises the isolated GitHub App technical spike against an explicitly configured test repository.
- `postgres-backup.sh` creates a checksummed PostgreSQL custom-format backup and prunes expired backup files.
- `postgres-restore.sh` performs an explicitly confirmed destructive restore from a custom-format backup.

## PostgreSQL backup and restore configuration

`postgres-backup.sh` and `postgres-restore.sh` resolve the project root from their own location and automatically read the relevant settings from the project `.env` file. The full `.env` file is deliberately not sourced as shell code; only the explicitly supported keys are read.

Backup reads:

- `ZIP_GITHUB_BACKUP_DIR`
- `ZIP_GITHUB_BACKUP_RETENTION_DAYS`
- `POSTGRES_DB`
- `POSTGRES_USER`

Restore reads:

- `POSTGRES_DB`
- `POSTGRES_USER`

Already exported environment variables take precedence over `.env`. An alternate env file can be selected with `ZIP_GITHUB_ENV_FILE=/path/to/file`.

The restore confirmation remains an explicit runtime variable and is not loaded from `.env`:

```bash
ZIP_GITHUB_CONFIRM_RESTORE='RESTORE zip_github' \
  ./scripts/postgres-restore.sh /var/backups/zip-github/postgres/<backup>.dump
```

This keeps destructive restore authorization separate from persistent configuration.

## security-regression.sh

Runs the release security baseline: no active Docker socket, no probable committed secrets, CSRF/origin/rate-limit/security-header invariants, ZIP controls and non-force Git delivery. It is executed by CI.

## Release verification

`./scripts/verify-release.sh` verifies the version marker, required MVP release artifacts, final implementation ledger and the structure/security regression baselines. It does not replace backend/frontend builds or external GitHub, Docker, PostgreSQL and device acceptance tests.

- `verify-source-tracking.sh` verifies required source files exist and are not accidentally ignored by Git.

Container image publication is handled by `.github/workflows/ci.yml`; normal servers pull GHCR images and do not run build scripts.
