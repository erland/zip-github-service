# Capability-protected staging upload — phase 9 step 9.2

## Purpose

Step 9.2 adds one deliberately narrow transport endpoint for the signed iOS Shortcut and other simple clients. The endpoint can create a short-lived `AVAILABLE` `StagingImport`, but it cannot identify a user, list/read staged data, select a Project, create an ordinary Import, or reach GitHub.

```text
X-ZipGitHub-Upload-Credential + ZIP bytes
→ ZipIngestionService
→ StoredUploadArtifact
→ AVAILABLE StagingImport
→ opaque staging id + one-time claim URL
```

Claim and promotion remain later phase-9 steps.

## Capability boundary

The deployment credential is configured with `ZIP_GITHUB_STAGING_UPLOAD_CREDENTIAL` and sent only in `X-ZipGitHub-Upload-Credential`.

The credential:

- grants only `POST /api/staging-imports`;
- is not a web session, OAuth token, GitHub token, user id or Project authorization;
- is compared through SHA-256 digests using `MessageDigest.isEqual` rather than ordinary String equality;
- defaults to deny-all when missing/blank;
- is never put in URLs, response bodies, audit records or log messages;
- can be rotated independently of GitHub App/session credentials.

The capability POST is the only API write deliberately exempted from the browser same-origin CSRF marker because a Shortcut is not a browser session. That exemption is exact-path/exact-method; authorization is instead provided by the staging credential. Normal authenticated writes remain CSRF protected.

## Request and response

```http
POST /api/staging-imports
X-ZipGitHub-Upload-Credential: <deployment credential>
X-Filename: project.zip
Content-Type: application/zip

<ZIP bytes>
```

`application/octet-stream` is also accepted, matching the existing upload transport behavior.

A successful `201` response contains only:

- opaque `stagingId`;
- normalized original filename;
- compressed byte size;
- SHA-256 of the stored ZIP;
- staging expiry;
- one-time `claimUrl` whose token is in the URL fragment.

There is no anonymous GET/list/download endpoint.

## Claim token handling

Each staging creation uses 32 random bytes (256 bits) encoded as URL-safe Base64 without padding. Only lowercase SHA-256 of the token is put in PostgreSQL. The raw token is returned once inside the creation response's `claimUrl`; later APIs must never re-expose it.

The claim URL has the form:

```text
<frontend>/staging/claim#token=<one-time-token>
```

The fragment is intentionally not sent to the server in the initial browser request. Step 9.3 owns client-side capture and authenticated claim.

## Resource and abuse bounds

Staging uses the same `ZipIngestionService` as browser upload, therefore it inherits the same compressed-size limit, streamed storage behavior, SHA-256 calculation, filename normalization, retention root and no-execution rule.

The generic API rate limiter applies a 30 requests/minute bucket keyed by a SHA-256 fingerprint of the presented upload capability plus a 120 requests/minute global staging ceiling. Invalid callers therefore do not consume the valid capability bucket unless they know that capability, while the global ceiling still bounds random-credential abuse. Finer network-source quotas, live-staging quotas and cleanup belong to step 9.6.

Invalid credentials produce a generic unauthorized response. Invalid upload metadata and oversize uploads use bounded generic messages that do not echo secrets, filesystem paths or ZIP content.

If database persistence fails after bytes were stored, the just-created storage file is deleted best-effort so an untracked staging artifact is not intentionally left behind.

## Configuration

```text
ZIP_GITHUB_STAGING_UPLOAD_CREDENTIAL=<long random deployment secret>
ZIP_GITHUB_STAGING_AVAILABLE_TTL_MINUTES=60
```

The first setting should be present in production before the reference Shortcut is distributed. Blank means staging upload is disabled/deny-all.

## Explicit non-goals in 9.2

- authenticated claim;
- owner read/cancel;
- Project selection;
- promotion to ordinary Import;
- Shortcut `.shortcut` artifact publication;
- staging cleanup/grace-period policy beyond the initial AVAILABLE expiry;
- GitHub access of any kind.

## Step 9.6 capacity/rotation additions

Staging-create is also bounded by deployment live-object/live-byte quotas (`ZIP_GITHUB_STAGING_MAX_LIVE_OBJECTS`, `ZIP_GITHUB_STAGING_MAX_LIVE_BYTES`) under serialized database accounting. Quota exhaustion returns `429 STAGING_CAPACITY_EXCEEDED`. The deployment credential remains single-generation in the first implementation: replacing the configured secret and redeploying immediately rejects the old Shortcut without changing existing staging rows or claim tokens.
