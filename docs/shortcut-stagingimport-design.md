# Shortcut / StagingImport — design baseline

Date: 7 August 2026  
Planning revision: r0084  
Application version at planning point: 1.0.0-rc.39

## Purpose

This document fixes the intended architecture for phase 9 so implementation can continue in a later chat without reopening the core security and ownership decisions.

The target user flow is:

```text
ChatGPT / Files / Share Sheet
        ↓
iOS Shortcut
        ↓
capability-protected staging upload
        ↓
short-lived StagingImport + one-time claim token
        ↓
open zip-github claim URL in browser
        ↓
normal GitHub login if needed
        ↓
atomic claim by authenticated user
        ↓
choose zip-github Project / active Work
        ↓
promote already stored ZIP to ordinary Import
        ↓
existing prepare-review / selection / approval / commit / PR flow
```

Staging is a transport buffer only. It is not a second import model and must never be a route around normal authentication, project ownership, import policy, Work invariants or GitHub App authorization.

## Existing implementation that phase 9 must reuse

Phase 7 deliberately created the convergence point already needed by staging:

- `ZipIngestionService.store(...)` is the source-neutral byte-ingestion boundary. It owns streaming limits, safe filename handling, SHA-256 calculation and temporary storage.
- `StoredUploadArtifact` is the neutral result of successful ingestion.
- `ProjectApplicationService.createImportFromStoredUpload(...)` promotes an already stored artifact into an ordinary user-owned import without copying or re-streaming the ZIP.
- `ImportSource.STAGING_IMPORT` already exists as a reserved source classification.
- Import source metadata is diagnostic/audit-only and is deliberately excluded from plan digest, policy, selection and Git delivery semantics.
- The ordinary import pipeline already supports restart-safe state, cancellation, one active import per Work, immutable plan/selection/approval and exact Git delivery.

Phase 9 must extend around these seams rather than duplicate them.

## Security principals

### Upload capability is not user authentication

The Shortcut needs a credential that prevents the public Internet from freely filling staging storage. This is an **upload capability**, not a user identity.

The capability may only authorize creation of a new staging upload. It must not authorize:

- listing staging uploads,
- reading back ZIP bytes,
- claiming an upload,
- choosing a Project,
- reading repository metadata,
- creating an ordinary Import,
- calling GitHub,
- committing or creating a pull request.

The capability is supplied in an HTTP header, never in a URL. It must be independently rotatable from GitHub App credentials and web sessions.

A leaked upload capability is therefore primarily an abuse/storage risk, not a repository-access breach. Phase 9.5 must still limit that risk with rate, concurrency, size and total-storage controls.

### Claim token

Each accepted staging upload gets a new high-entropy random claim token. Requirements:

- token returned exactly at staging creation;
- only a one-way token hash stored server-side;
- token never written to normal audit/source-reference fields;
- token never logged;
- claim is atomic and single-owner;
- same authenticated owner may retry idempotently;
- a different user cannot take over a claimed object;
- expired/invalid/already-consumed responses should not reveal useful enumeration information.

Prefer carrying the raw token in the browser URL fragment (`#token=...`) so it is not sent in the initial HTTP request or normal access logs. The frontend can move it into short-lived `sessionStorage`, clear the fragment and submit the token to the authenticated claim API. If login is required, only the local claim intention/token state should survive the redirect; it must not be copied into OAuth state, sourceReference or server logs.

### No anonymous discovery

There must be no endpoint that lists unclaimed staging objects. Opaque staging IDs alone must not allow ZIP download, metadata lookup or claim.

### Same ZIP policy as ordinary upload

Staging upload uses `ZipIngestionService`; there is no staging-specific relaxed size/path/resource policy. Archive inspection/comparison still occurs only after promotion through the ordinary import pipeline.

## Proposed persistence model

A first implementation can use a table conceptually like:

```text
staging_import
- id UUID PK
- status AVAILABLE | CLAIMED | PROMOTED | EXPIRED | CANCELLED
- stored_upload_id UUID / storage correlation
- original_filename
- size_bytes
- sha256
- storage_path or storage key (same storage abstraction as StoredUploadArtifact)
- claim_token_hash
- created_at
- expires_at
- claimed_by_user_id nullable
- claimed_at nullable
- promoted_import_id nullable
- promoted_at nullable
- updated_at
```

Important constraints:

- `claim_token_hash` unique where applicable.
- Promotion stores/links the resulting ordinary import ID for retry idempotency.
- Claim uses a row lock/conditional update so two users cannot win concurrently.
- Cleanup and promotion must coordinate transactionally so a file cannot disappear while promotion is attaching it.
- Do not create a fake anonymous `ImportSession`; the staging entity is deliberately separate until claim/project authorization.

## Proposed API shape

Exact resource names may be refined in phase 9, but the security boundaries should remain.

### Anonymous/capability upload

```http
POST /api/staging-imports
Authorization: Bearer <staging-upload-capability>
Content-Type: multipart/form-data
```

or a dedicated capability header if that fits existing filters better.

Success response should contain only what the Shortcut needs, for example:

```json
{
  "stagingId": "...",
  "originalFilename": "project.zip",
  "sizeBytes": 123456,
  "sha256": "...",
  "expiresAt": "...",
  "claimUrl": "https://zip-github.example/staging/claim#token=..."
}
```

The raw token appears only in `claimUrl`/the creation response and is never returned by later GETs.

### Authenticated claim

```http
POST /api/staging-imports/claim
Content-Type: application/json
Cookie: normal zip-github web session

{"token":"<raw one-time claim token>"}
```

Success binds the staging object to the current GitHub user and returns owner-safe metadata plus staging ID. A retry by the same owner is idempotent.

### Owner read/cancel after claim

Possible endpoints:

```text
GET    /api/staging-imports/{id}
POST   /api/staging-imports/{id}/cancel
```

These require the normal authenticated owner. No equivalent anonymous read endpoint exists.

### Promotion

```http
POST /api/staging-imports/{id}/promote
Cookie: normal zip-github web session
Content-Type: application/json

{"projectId":"..."}
```

Server responsibilities:

1. Verify staging owner and non-expired claim.
2. Verify Project ownership and GitHub configuration using existing Project path.
3. Respect `ACTIVE_IMPORT_EXISTS` exactly as browser-created imports do.
4. Reconstruct/use the existing `StoredUploadArtifact` without byte copy.
5. Call `createImportFromStoredUpload(...)` with `ImportSource.STAGING_IMPORT` and an idempotency key derived from non-secret stable IDs.
6. Store `promoted_import_id` transactionally/idempotently.
7. Return the ordinary Import ID/route.
8. Frontend continues through the existing automatic `prepare-review` flow.

## Retention baseline

Recommended initial defaults:

- unclaimed `AVAILABLE`: about 1 hour;
- claimed but not promoted: short configurable grace period long enough for login/project choice, for example a few hours;
- promoted: storage ownership passes to the ordinary Import retention/resume model;
- cancelled/expired: become cleanup candidates immediately, with physical deletion performed by the normal cleanup mechanism.

The exact claimed grace period should be configured rather than hard-coded and finalized in phase 9.5.

## Abuse controls

At minimum phase 9 should enforce:

- existing compressed-byte and ingestion limits;
- request rate limit by upload capability and network source where practical;
- maximum concurrent incomplete staging uploads;
- maximum number/bytes of live staging objects per capability/deployment;
- no content execution;
- no anonymous list/read;
- generic auth/token errors;
- upload capability rotation and incident instructions;
- metrics/logging based on staging ID/status only, never raw claim/capability tokens or ZIP contents.

## iOS Shortcut reference flow

The client side can remain intentionally small. A reference Shortcut should:

1. Accept Files from Share Sheet and restrict/validate that a single ZIP was supplied where practical.
2. Perform `Get Contents of URL` / HTTP POST to the staging endpoint.
3. Send the configured staging upload capability in a header.
4. Send the ZIP as the request body/multipart file according to the final API contract.
5. Parse the JSON response.
6. Open `claimUrl` in Safari/default browser.
7. On HTTP 413 explain that the ZIP exceeds server limits.
8. On HTTP 429 explain that staging upload is rate-limited and can be retried later.
9. On connection/server error keep the original local ZIP untouched and allow retry.

The Shortcut must **not** contain:

- GitHub token or GitHub App private key,
- zip-github user ID,
- Project ID as an authorization shortcut,
- claim token in persistent notes/logs,
- import policy logic.

The upload capability can be entered as a Shortcut text/config value. Anyone who obtains it can potentially consume staging upload capacity, so it should be treated as a rotatable capability and not published. Its possession still grants no ability to claim another user's staging object or access GitHub.

## UX after opening claim URL

Suggested mobile flow:

```text
ZIP mottagen
project.zip · 2.4 MB

[Logga in med GitHub]      (only when needed)

↓ after authenticated claim

Välj projekt
- Project A
- Project B

[Använd valt projekt]

↓ promotion

ordinary import review page
```

If the chosen Project already has an active import, show that conflict before promotion and offer navigation to that import/project. Do not silently cancel or replace it.

## Explicit non-goals for phase 9

- No GitHub access from the Shortcut.
- No anonymous project selection.
- No permanent generic file drop.
- No listing of unclaimed uploads.
- No staging-specific comparison, policy, selection, Git workspace or delivery implementation.
- No requirement that arbitrary ZIP files contain zip-github metadata.
- No AI/Custom GPT/MCP API; that has been moved to future backlog.

## Acceptance summary

Phase 9 is complete only when the same ZIP against the same locked base SHA converges to the same ordinary import plan whether the bytes arrived through the browser or through Shortcut/StagingImport, while claim ownership, expiry, cleanup, retry and abuse controls remain safe.
