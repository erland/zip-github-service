# Configuration reference

## Required production values

| Variable | Purpose |
|---|---|
| `POSTGRES_PASSWORD` | PostgreSQL application-role password |
| `GITHUB_OAUTH_CLIENT_ID` | OAuth browser-login client ID |
| `GITHUB_OAUTH_CLIENT_SECRET` | OAuth browser-login secret |
| `GITHUB_OAUTH_CALLBACK_URL` | Exact public OAuth callback URL |
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
| `ZIP_GITHUB_UPLOAD_MAX_COMPRESSED_BYTES` | 100 MiB |
| `ZIP_GITHUB_ARCHIVE_MAX_UNCOMPRESSED_BYTES` | 500 MiB |
| `ZIP_GITHUB_ARCHIVE_MAX_ENTRIES` | 20,000 |
| `ZIP_GITHUB_ARCHIVE_MAX_SINGLE_FILE_BYTES` | 50 MiB |
| `ZIP_GITHUB_ARCHIVE_MAX_PATH_LENGTH` | 1,024 characters |
| `ZIP_GITHUB_ARCHIVE_MAX_COMPRESSION_RATIO` | 100:1 |

Changing limits requires matching disk/tmpfs capacity and a security review.

## Backup values

| Variable | Default |
|---|---|
| `ZIP_GITHUB_BACKUP_DIR` | `./backups/postgres` |
| `ZIP_GITHUB_BACKUP_RETENTION_DAYS` | `14` |
| `ZIP_GITHUB_CONFIRM_RESTORE` | unset; destructive restore remains disabled |

### ZIP_GITHUB_RATE_LIMIT_ENABLED

Enables the in-process request throttle for unsafe API operations. Keep `true` in production. The MVP limits ordinary state-changing requests to 120 per session/minute and ZIP uploads to 12 per session/minute. A reverse proxy should add IP- and network-level throttling.
