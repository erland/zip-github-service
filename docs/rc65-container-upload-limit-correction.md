# r0113 / rc.65 — container upload-limit correction

## Problem

Uploads just above 1 MB could receive HTTP 413 before the request reached Quarkus even when the external reverse proxy and backend upload limit were configured for larger ZIPs.

## Root cause

The frontend nginx container proxied `/api/` to the backend but did not set `client_max_body_size`. nginx therefore used its 1 MB default and rejected larger request bodies inside the container.

## Correction

- The frontend nginx config now sets `client_max_body_size` from `ZIP_GITHUB_NGINX_CLIENT_MAX_BODY_SIZE`.
- Both frontend runtime images default that variable to `200M` and render nginx config through the official nginx template mechanism at container startup.
- Docker Compose exposes the same variable with a `200M` default.
- The backend compressed ZIP default is aligned to 200 MiB (`209715200` bytes); deployments can still override it through `ZIP_GITHUB_UPLOAD_MAX_COMPRESSED_BYTES`.
- Documentation now explicitly requires the external proxy, frontend nginx and backend limits to be coordinated.

No import, authorization, GitHub, Work or phase-9 behavior changed.
