# Configuration reference

## Required production values

| Variable | Purpose |
|---|---|
| `POSTGRES_PASSWORD` | PostgreSQL application-role password |
| `GITHUB_APP_CLIENT_ID` | GitHub App Client ID used for user authorization |
| `GITHUB_APP_CLIENT_SECRET` | GitHub App Client Secret used for user authorization |
| `GITHUB_APP_CALLBACK_URL` | Exact public GitHub App user-authorization callback URL |
| `GITHUB_APP_ID` | GitHub App numeric ID |
| `GITHUB_APP_PRIVATE_KEY` | GitHub App PKCS#8 private key |
| `ZIP_GITHUB_FRONTEND_URL` | Exact public frontend origin used by redirects, CORS and CSRF |

## Security values

| Variable | Default | Production guidance |
|---|---:|---|
| `ZIP_GITHUB_SECURE_COOKIES` | `true` in application | keep `true` behind HTTPS |
| `ZIP_GITHUB_CSRF_ENABLED` | `true` | keep enabled |
| `ZIP_GITHUB_UPLOAD_RETENTION_HOURS` | `24` | minimize while allowing review |
| `ZIP_GITHUB_UPLOAD_CLEANUP_INTERVAL` | `1h` | alert if cleanup repeatedly fails |

## Resource limits

| Variable | Default |
|---|---:|
| `ZIP_GITHUB_UPLOAD_MAX_COMPRESSED_BYTES` | 200 MiB |
| `QUARKUS_HTTP_LIMITS_MAX_BODY_SIZE` | `200M` |
| `ZIP_GITHUB_NGINX_CLIENT_MAX_BODY_SIZE` | `200M` |
| `ZIP_GITHUB_ARCHIVE_MAX_UNCOMPRESSED_BYTES` | 500 MiB |
| `ZIP_GITHUB_ARCHIVE_MAX_ENTRIES` | 20,000 |
| `ZIP_GITHUB_ARCHIVE_MAX_SINGLE_FILE_BYTES` | 50 MiB |
| `ZIP_GITHUB_ARCHIVE_MAX_PATH_LENGTH` | 1,024 characters |
| `ZIP_GITHUB_ARCHIVE_MAX_COMPRESSION_RATIO` | 100:1 |

The external reverse proxy, frontend-container nginx, Quarkus HTTP request-body ceiling and backend upload policy must all admit the intended request size. `ZIP_GITHUB_NGINX_CLIENT_MAX_BODY_SIZE` and `QUARKUS_HTTP_LIMITS_MAX_BODY_SIZE` should be at least as large as `ZIP_GITHUB_UPLOAD_MAX_COMPRESSED_BYTES`; the zip-github compressed-byte limit remains authoritative. Changing limits requires matching disk/tmpfs capacity and a security review.

## Backup values

| Variable | Default |
|---|---|
| `ZIP_GITHUB_BACKUP_DIR` | `./backups/postgres` |
| `ZIP_GITHUB_BACKUP_RETENTION_DAYS` | `14` |
| `ZIP_GITHUB_CONFIRM_RESTORE` | unset; destructive restore remains disabled |

### ZIP_GITHUB_RATE_LIMIT_ENABLED

Enables the in-process request throttle for unsafe API operations. Keep `true` in production. The MVP limits ordinary state-changing requests to 120 per session/minute and ZIP uploads to 12 per session/minute. A reverse proxy should add IP- and network-level throttling.

## Container deployment

| Variable | Default | Purpose |
|---|---|---|
| `ZIP_GITHUB_VERSION` | `1.0.0-rc.15` | Exact backend/frontend image tag used by server Compose. |
| `ZIP_GITHUB_BACKEND_IMAGE` | `ghcr.io/erland/zip-github-service-backend` | Backend image repository. |
| `ZIP_GITHUB_FRONTEND_IMAGE` | `ghcr.io/erland/zip-github-service-frontend` | Frontend image repository. |

Prefer an immutable exact version in server deployments. The mutable `rc` or `latest` tags are conveniences and should not be used as the rollback anchor.
