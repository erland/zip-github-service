# Operations model

## Runtime topology

The supported MVP topology is three containers behind a TLS-terminating reverse proxy:

1. `frontend`: static React assets served by nginx and reverse proxying `/api` and `/q` to the backend.
2. `backend`: Quarkus API running as an unprivileged user.
3. `postgres`: PostgreSQL 16 with a named persistent volume.

The application does **not** mount or use the Docker socket. Project builds remain the responsibility of GitHub Actions in each target repository.

## Persistent and temporary data

| Data | Location | Persistence | Recovery expectation |
|---|---|---|---|
| PostgreSQL | `postgres-data` volume | durable | restored from database backup |
| Uploaded ZIP files | `upload-data` volume | temporary | not backed up; retention cleanup removes expired files |
| Delivery workspaces | `delivery-data` volume | temporary until delivery completes | not backed up; retry/reconciliation rebuilds from immutable plan |
| Snapshot workspaces | backend `/tmp` tmpfs | ephemeral | always recreated |
| Application logs | container stdout/stderr | external log platform | retained according to operator policy |

Uploaded ZIPs and Git workspaces are not system-of-record data. GitHub and PostgreSQL metadata are the authoritative sources.

## Runtime images

Normal server deployment uses prebuilt images from GitHub Container Registry:

- `ghcr.io/erland/zip-github-service-backend`
- `ghcr.io/erland/zip-github-service-frontend`
- `postgres:16-alpine`

The exact application version is selected with `ZIP_GITHUB_VERSION`. Release-candidate deployments should use an immutable version tag such as `1.0.0-rc.8`, not the mutable `rc` tag. If the GHCR packages are private, log the server into `ghcr.io` with a token that has `read:packages`; alternatively make the packages public in GitHub package settings.

## Startup and shutdown

1. Copy `.env.example` to `.env` and replace every placeholder.
2. Set `ZIP_GITHUB_VERSION` to the exact version to deploy.
3. Configure GitHub OAuth and GitHub App settings as described in `docs/github-app-setup.md`.
4. If GHCR packages are private, run `docker login ghcr.io` using a token with `read:packages`.
5. Pull the selected images with `docker compose pull`.
6. Start with `docker compose up -d`.
7. Verify `docker compose ps` reports all services healthy.
8. Verify frontend `/`, backend `/q/health/live`, and `/q/health/ready`.

Use `docker compose down` for a normal stop. Do not add `--volumes` unless permanent database deletion is intended and a verified backup exists.

For local development or an installation that intentionally builds images from source, use:

```bash
docker compose -f docker-compose.yml -f docker-compose.build.yml up -d --build
```

## Upgrade and rollback

To upgrade, set `ZIP_GITHUB_VERSION` to the new immutable version and run:

```bash
docker compose pull
docker compose up -d
```

To roll back, restore the previous `ZIP_GITHUB_VERSION` and run the same two commands. Database migrations are forward-only, so a rollback across a schema migration must follow the database recovery guidance rather than assuming an older application image can use a newer schema.

## Health checks

- PostgreSQL readiness: `pg_isready`.
- Backend readiness: `/q/health/ready`.
- Backend liveness: `/q/health/live`.
- Frontend: HTTP request to `/`.

A failed readiness check removes the service from normal traffic but does not itself delete or recreate data.

## Backups

Run:

```bash
./scripts/postgres-backup.sh
```

The script creates a PostgreSQL custom-format dump, writes a SHA-256 sidecar, uses restrictive file permissions and prunes backups older than `ZIP_GITHUB_BACKUP_RETENTION_DAYS`.

Recommended minimum schedule for an MVP installation:

- daily database backup,
- at least 14 daily restore points,
- one copy outside the Docker host,
- encrypted storage with restricted operator access,
- monthly restore drill.

The backup directory must not be located inside the PostgreSQL Docker volume.

## Restore

Restores are destructive. Stop user traffic first, preserve the current database and verify the backup checksum.

```bash
export ZIP_GITHUB_CONFIRM_RESTORE='RESTORE zip_github'
./scripts/postgres-restore.sh backups/postgres/zip_github-YYYYMMDDTHHMMSSZ.dump
```

After restore:

1. restart backend,
2. confirm Flyway reports no migration error,
3. check `/q/health/ready`,
4. verify project/import history,
5. reconcile any import whose GitHub operation may have completed after the backup timestamp.

## Retention and cleanup

The backend scheduler removes expired uploaded ZIPs using:

- `ZIP_GITHUB_UPLOAD_RETENTION_HOURS`,
- `ZIP_GITHUB_UPLOAD_CLEANUP_INTERVAL`.

Snapshot workspaces use tmpfs and disappear on restart. Delivery workspaces are deleted after successful push and on handled failure. Operators should alert on unexpectedly growing `upload-data` or `delivery-data` volumes.

## Logging and observability

Container logs are written to stdout/stderr. Central logging should capture:

- timestamp,
- severity,
- service/container,
- correlation ID,
- import/project identifiers where safe,
- error category and retryability.

Never log OAuth secrets, GitHub App private keys, installation tokens, session cookies, ZIP contents or private key file contents. API clients receive a correlation ID rather than stack traces.

Initial alerts should cover:

- backend or database not ready for more than five minutes,
- repeated `5xx` or `GIT_DELIVERY_RETRYABLE` responses,
- backup failure or stale backup age,
- disk/volume usage above 80%,
- repeated OAuth callback failures,
- retention cleanup failures.

## Secret rotation

Rotate one credential at a time and keep a rollback value until verification succeeds.

### GitHub OAuth client secret

1. Generate a new secret in GitHub.
2. update the deployment secret source,
3. restart backend,
4. verify a new login and callback,
5. revoke the old secret.

Existing server sessions may remain valid, but a deliberate session invalidation is recommended after suspected exposure.

### GitHub App private key

1. Generate an additional private key in the GitHub App settings.
2. convert/store it in the expected PKCS#8 PEM representation,
3. deploy and restart backend,
4. verify installation listing, snapshot and a test delivery,
5. delete the old GitHub App key.

GitHub permits overlapping keys, enabling rotation without downtime.

### PostgreSQL password

Coordinate database and backend changes in a maintenance window. Change the database role password, update the secret source and restart backend. Verify readiness and a read/write operation before ending the window.

## Incident handling

For suspected credential exposure:

1. stop or isolate backend traffic,
2. rotate the affected credential,
3. invalidate sessions if OAuth/session material may be involved,
4. review GitHub App audit events and created branches/PRs,
5. review logs by correlation ID without copying sensitive values,
6. document affected imports and recovery actions.

For an interrupted delivery, use the idempotent delivery and PR endpoints. Do not manually force-push an import branch unless incident procedures explicitly approve it.

## Capacity and scaling

The current release persists project configuration, resumable import state, Work lifecycle, staging lifecycle and approval/delivery recovery data in PostgreSQL. Web login sessions remain deployment-scoped, so horizontal scaling still requires a deliberately shared/sticky session strategy before multiple backend replicas are introduced. Temporary workspace capacity must accommodate configured ZIP expansion limits plus concurrent Git workspaces.


## Container storage initialization

The production Compose stack includes a one-shot `storage-init` service. It runs as root only long enough to prepare the named upload/delivery volumes for backend UID/GID 10001, then exits. The backend remains non-root and depends on successful storage initialization.

The backend runtime image must include the Git CLI because repository snapshot, approved-workspace verification and delivery execute Git as an external process. Verify after deployment with:

```bash
docker compose exec backend git --version
docker compose exec backend sh -c 'id; ls -ld /var/lib/zip-github/uploads /var/lib/zip-github/delivery'
```

## Durable project configuration (RC13)

From `1.0.0-rc.13`, authenticated user identity, per-user GitHub App installation visibility and project configuration are stored in PostgreSQL. Project lists therefore survive backend restarts and image deployments.

In-progress imports are restart-resumable: source associations, immutable plan/selection/approval state and delivery recovery metadata are persisted, while temporary Git workspaces are intentionally disposable and rebuilt after restart. Preserve PostgreSQL and the upload volume across backend replacement; do not manually remove files for non-terminal imports.

## Git askpass runtime helper (RC13)

The backend image contains `/usr/local/bin/zip-github-git-askpass` as a fixed executable. Git operations no longer create executable askpass scripts inside temporary workspaces, which avoids `noexec` tmpfs mount failures while keeping installation tokens out of command-line arguments.


## Resumable imports across backend restarts

From RC34 onward, active import resume state is expected to survive ordinary backend container replacement. Operators should preserve both PostgreSQL and the upload volume. A restart may discard temporary Git workspaces; this is intentional because they are rebuilt and reverified from durable state. Do not delete active upload files manually while an import is non-terminal.

## Phase 9 staging retention and credential incident operations

Defaults: AVAILABLE TTL 60 minutes, CLAIMED grace 240 minutes, cleanup every 5 minutes in batches of 100, maximum 100 non-promoted/not-yet-deleted staging artifacts and 1 GiB staging bytes. Configure with the `ZIP_GITHUB_STAGING_*` variables documented in `.env.example`. Monitor repeated cleanup failures and sustained `STAGING_CAPACITY_EXCEEDED` responses; both can indicate storage pressure.

To revoke/rotate a leaked Shortcut upload credential, replace `ZIP_GITHUB_STAGING_UPLOAD_CREDENTIAL` with a new strong random value and redeploy/restart every backend instance. The old Shortcut then receives `STAGING_UPLOAD_UNAUTHORIZED` immediately. No database migration, GitHub credential rotation or invalidation of already-created staging claim tokens is required. Publish a newly signed Shortcut with the new credential in step 9.7. Do not configure parallel old/new credentials in the first implementation.

Enable `ZIP_GITHUB_TRUST_FORWARDED_FOR=true` only when a trusted ingress strips client-supplied forwarding headers and writes the real network source itself. Otherwise leave it false.

## Signed iOS Shortcut release operations

The reference Shortcut is a separately signed deployment artifact, not a backend build product. Keep `shortcut/releases/zip-github.shortcut` out of Git because it embeds the low-privilege deployment staging credential. Compose mounts `./shortcut/releases` read-only and the backend serves only the configured file to authenticated users.

Publication/rotation procedure is documented in `docs/signed-shortcut-release.md`. In short: update the Shortcut and deployment upload credential in a trusted Apple environment, export and sign with an iCloud-signed-in Mac for `anyone`, place the signed file in `shortcut/releases/`, advance `ZIP_GITHUB_SHORTCUT_VERSION`/`ZIP_GITHUB_SHORTCUT_GENERATION`, rotate `ZIP_GITHUB_STAGING_UPLOAD_CREDENTIAL`, then redeploy. There is deliberately no current/previous grace credential in the first version.

If no signed artifact is mounted, `/shortcut` stays usable but reports that no installer is published. Never work around this by serving an unsigned file.


## Phase 9 final operational gate

Before promoting a deployment, run `bash scripts/verify-phase9-release.sh` and `bash scripts/verify-release.sh`. The signed Shortcut binary is deployment material rather than Git source: when present at `shortcut/releases/zip-github.shortcut`, its bytes/size/SHA must match `shortcut/releases/release-manifest.txt` and it must be readable by the backend runtime user through the read-only mount. The authenticated download intentionally exposes the friendly filename `Skicka till zip-github.shortcut`.

For Work recovery, a Work is not active until its remote GitHub branch has been created/read back at the expected SHA. If a remote Work branch disappears or moves unexpectedly, delivery must fail closed; do not recreate or force-push it as an operational workaround. Use the explicit abandon/archive/restart-work controls instead. Actions diagnostics on the Work page are commit-bound and may be refreshed after reconnect/login.
