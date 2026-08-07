# MVP RC12 correction — container runtime and storage

Revision: `r0052`  
Version: `1.0.0-rc.12`

## Symptoms observed in production

1. ZIP upload initially failed with `AccessDeniedException` below `/var/lib/zip-github/uploads`.
2. After manual volume ownership repair, creation of a review plan failed with `Could not start Git.`

## Root causes

The backend image intentionally runs as the non-root user `zipgithub` (UID 10001), but newly created named volumes are root-owned. The backend therefore could not create upload or delivery directories until ownership was fixed manually.

Repository snapshot, import workspace and delivery code start the external `git` executable with `ProcessBuilder`, but the production runtime image installed only `curl`. Java therefore raised `IOException` while starting Git.

## Corrections

- Runtime image installs both `curl` and `git`.
- Compose includes a one-shot root `storage-init` service that mounts only the two application data volumes, creates the expected directories, sets ownership to `10001:10001`, and restricts them to mode `0700`.
- Backend starts only after PostgreSQL is healthy and `storage-init` has completed successfully.
- Backend continues to run as UID 10001.

## Verification

Static release checks verify that Git is installed in the runtime image and that Compose contains the storage initialization/dependency invariants. Full image construction and end-to-end GitHub execution remain CI/production acceptance checks.

## Production acceptance

After deploying RC12:

```bash
docker compose pull
docker compose up -d
docker compose ps -a
docker compose exec backend git --version
docker compose exec backend sh -c 'id; ls -ld /var/lib/zip-github/uploads /var/lib/zip-github/delivery'
```

`storage-init` should be exited successfully, Git should print a version, and both persistent directories should be owned by UID/GID 10001. Then repeat ZIP upload and review-plan creation.
