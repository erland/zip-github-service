# Coolify deployment

This profile deploys Zip GitHub Service on Coolify using the published GHCR images, the shared PostgreSQL resource, and Coolify-managed proxy/TLS.

It is intentionally separate from the existing root `docker-compose.yml` and `ops/production/` SSH deployment.

## Target topology

```text
Internet
  |
  v
https://zip-github.apps.isaksson.info
  |
  v
Coolify / Traefik
  |
  v
frontend:80 (nginx)
  |
  +--> /api/* --> backend:8080
                    |
                    +--> shared PostgreSQL:5432
                    +--> upload-data
                    +--> delivery-data
```

Only `frontend` is public. `backend`, `storage-init`, PostgreSQL and the application volumes remain internal.

## 1. DNS

With the current Loopia setup, create:

```text
zip-github.apps.isaksson.info  CNAME  apps.isaksson.info
```

The resulting hostname must resolve to the public IP of the Coolify server.

## 2. Shared PostgreSQL

Use the existing shared PostgreSQL resource in Coolify. Do not create a PostgreSQL service from this Compose profile.

Create a dedicated database and role:

```text
database: zip_github
owner:    zip_github
```

`bootstrap-db.sql` contains the required statements. Replace `CHANGE_ME` before running it and never commit a production password.

Flyway runs automatically when the backend starts and owns the application schema.

Do not expose PostgreSQL port 5432 publicly.

## 3. Create the application from Git

In Coolify create a project such as:

```text
Zip GitHub
```

with environment:

```text
production
```

Create an Application from the Git repository:

```text
https://github.com/erland/zip-github-service
```

Use branch:

```text
main
```

Select the Docker Compose build pack and set the Compose file to:

```text
/ops/coolify/compose.yaml
```

(or `ops/coolify/compose.yaml` if the UI expects a relative path without a leading slash).

Do not use the "Docker Compose without Git repository" resource type.

## 4. Connect to the predefined network

Enable Coolify's **Connect To Predefined Network** setting for the application.

This is required because PostgreSQL is a different Coolify resource. It allows the backend to reach the shared database by its internal Coolify hostname/alias while preserving the private `app` network used between frontend and backend.

Set `DB_HOST` to the exact internal hostname/alias shown by the PostgreSQL resource. Do not use the server's public IP and do not expose port 5432.

## 5. Environment variables

Use `env.example` as the checklist and configure production values in Coolify.

At minimum set real values for:

```text
ZIP_GITHUB_VERSION
DB_HOST
POSTGRES_DB
POSTGRES_USER
POSTGRES_PASSWORD
GITHUB_APP_CLIENT_ID
GITHUB_APP_CLIENT_SECRET
GITHUB_APP_ID
GITHUB_APP_PRIVATE_KEY
```

Production URLs are:

```text
ZIP_GITHUB_FRONTEND_URL=https://zip-github.apps.isaksson.info
GITHUB_APP_CALLBACK_URL=https://zip-github.apps.isaksson.info/api/auth/github/callback
ZIP_GITHUB_SECURE_COOKIES=true
ZIP_GITHUB_CSRF_ENABLED=true
```

The callback URL must also be configured in the GitHub App.

Keep `GITHUB_APP_PRIVATE_KEY` and all credentials as secrets in Coolify. The private key can be supplied on one line with literal `\n` sequences, matching the existing application configuration.

## 6. Release version

Pin production to an explicit published image version, for example:

```text
ZIP_GITHUB_VERSION=1.0.0-rc.129
```

The images are:

```text
ghcr.io/erland/zip-github-service-backend:<version>
ghcr.io/erland/zip-github-service-frontend:<version>
```

The existing CI publishes these GHCR images from `main`. Avoid `latest` for production when a concrete version is available so rollback is deterministic.

## 7. Persistent application storage

The Coolify profile intentionally keeps two named Docker volumes:

```text
upload-data
 delivery-data
```

They survive ordinary container replacement/redeployments within the same Coolify resource.

`upload-data` contains temporary/staging ZIP uploads and is subject to the configured retention/cleanup policy. `delivery-data` holds isolated delivery workspaces that are also operational/temporary rather than the durable source of truth.

The application database remains the durable configuration store. Do not delete/recreate the Coolify resource or its volumes casually while work is in progress.

The root Compose profile also bind-mounts `shortcut/releases`. The Coolify profile deliberately does not depend on a source-tree bind mount. Signed Shortcut distribution is optional and `ZIP_GITHUB_SHORTCUT_RELEASE_PATH` is empty by default. If signed Shortcut distribution is enabled later, provision the signed artifact as explicit persistent storage rather than relying on the Git checkout path.

## 8. Domain and proxy

Assign this domain to the `frontend` service only:

```text
https://zip-github.apps.isaksson.info
```

Do not assign a public domain to `backend`.

The Compose profile contains no host `ports:` mappings. Coolify/Traefik terminates HTTPS and routes to port 80 in `frontend`. The frontend nginx then proxies `/api/` and `/q/` internally to `backend:8080`.

After changing the service domain in Coolify, use **Deploy again** so the generated proxy configuration is applied.

## 9. First deployment checklist

Before deploying verify:

- `zip_github` database exists
- `zip_github` database role exists and owns the database
- `DB_HOST` is the shared PostgreSQL resource's internal hostname/alias
- Connect To Predefined Network is enabled
- the pinned GHCR version exists for both frontend and backend
- all GitHub App secrets are configured
- GitHub App callback is `https://zip-github.apps.isaksson.info/api/auth/github/callback`
- the CNAME resolves to the Coolify server
- the public domain is assigned to `frontend` only

Deploy the application.

Expected state:

```text
storage-init  completed successfully
backend       healthy
frontend      healthy
```

Then open:

```text
https://zip-github.apps.isaksson.info
```

and verify GitHub sign-in, project listing, upload and delivery flows.

## 10. Backups and recovery

Configure scheduled backups of the `zip_github` database to the external S3/R2 backup destination.

The upload/delivery volumes contain temporary operational files, not the primary durable configuration. Database backup is therefore the essential backup for application state. If preserving in-flight uploads across total server loss is important, add those two volumes to the external storage backup policy as well.

GHCR contains the deployable application images, so local Docker images do not need backup.

## 11. Upgrades and rollback

For the initial setup, upgrade manually:

1. verify the new image version exists in GHCR
2. update `ZIP_GITHUB_VERSION`
3. deploy again
4. verify health and core flows

Rollback by restoring the previous `ZIP_GITHUB_VERSION` and redeploying. Database migrations may not be reversible; take a PostgreSQL backup before schema-changing upgrades.

Automatic release-to-Coolify deployment can be added after the first manual production deployment has been verified. Because production is intentionally version-pinned, automation should update `ZIP_GITHUB_VERSION` through the Coolify API before triggering redeploy rather than calling a deploy webhook that would simply redeploy the current version.
