# RC83 — Quarkus upload body-limit correction

## Observed production failure

A signed-Shortcut staging upload of approximately 18.7 MB returned HTTP 413 even though the external nginx proxy, frontend-container nginx and `ZIP_GITHUB_UPLOAD_MAX_COMPRESSED_BYTES` were already configured for 200 MB/MiB-class uploads. Frontend logs showed the proxied `POST /api/staging-imports` receiving 413 while application logs contained no upload handling, locating the rejection before application-level ingestion.

## Correction

The backend Compose service now exports:

```yaml
QUARKUS_HTTP_LIMITS_MAX_BODY_SIZE: ${QUARKUS_HTTP_LIMITS_MAX_BODY_SIZE:-200M}
```

This aligns Quarkus' HTTP request-body ceiling with the existing frontend nginx default (`200M`) and zip-github's compressed-upload default (`209715200` bytes). `.env.example` and configuration/upload documentation now describe all ingress ceilings explicitly.

## Security and behavior

The correction does not relax zip-github's authoritative archive/resource policies: compressed bytes, uncompressed expansion, entry count, single-file size, path length and compression ratio continue to be validated independently. Operators changing the HTTP ceilings must keep them coordinated with storage/tmpfs capacity and the application-level resource limits.

## Verification

- `docker-compose.yml` contains the Quarkus body-size environment mapping with a `200M` default.
- `.env.example` contains `QUARKUS_HTTP_LIMITS_MAX_BODY_SIZE=200M`.
- configuration and upload-streaming documentation describe the four-layer ingress chain.
- project structure/security/release verification is run for the packaged revision.

## Files added

- `docs/rc83-quarkus-upload-body-limit-correction.md`

## Files modified

- `.env.example`
- `CHANGELOG.md`
- `VERSION`
- `docker-compose.yml`
- `docs/configuration-reference.md`
- `docs/implementation-status.md`
- `docs/phase8-plus-continuation-handoff.md`
- `docs/upload-streaming.md`
- `scripts/verify-release.sh`

## Files moved or deleted

None.
