# Shortcut / StagingImport — design baseline

Date: 7 August 2026  
Planning revision: r0092  
Application version at latest planning update: 1.0.0-rc.44

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

The capability is deployment-scoped in the first phase-9 version and supplied in a dedicated HTTP header, preferably `X-ZipGitHub-Upload-Credential`, never in a URL. It is not user-specific identity and must be independently revocable/rotatable from GitHub App credentials and web sessions. It must never appear in access logs, analytics or ordinary audit text.

A leaked upload capability is therefore primarily an abuse/storage risk, not a repository-access breach. Phase 9.6 must still limit that risk with rate, concurrency, size and total-storage controls. Rotation does not require a `current`/`previous` overlap in the first version: an old Shortcut may fail after immediate revoke and direct the user to install the latest signed Shortcut. Existing staging objects remain governed by their independent claim-token and TTL state.

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

Staging upload uses `ZipIngestionService`; there is no staging-specific relaxed size/path/resource policy. Secure central-directory inspection may run at ingestion to reject unsafe archives and capture trustworthy Unix mode metadata, but repository comparison/policy/review still occurs only after promotion through the ordinary import pipeline.

## Git file-mode preservation

Phase 9 also owns closing the executable-bit/file-mode gap observed in ZIP→GitHub imports. File mode is project metadata, not merely an operating-system convenience. The implementation must preserve the Git-relevant distinction between ordinary files (`100644`) and executable files (`100755`) without guessing from names or extensions.

Rules:

- If trustworthy Unix mode metadata is present in the ZIP entry, carry the executable-bit signal through the neutral stored-upload/staging representation into the ordinary import pipeline.
- If mode metadata is absent for an existing repository path, preserve the exact base-snapshot Git mode rather than silently degrading an executable file to `100644`.
- If mode metadata is absent for a new path, use safe default `100644`; do not auto-promote `.sh`, `mvnw`, hooks or any other filename to executable.
- Mode changes (`100644 <-> 100755`) are real proposed changes. They must be visible in review, covered by selection/approval digest semantics and verified in the staged Git diff before commit.
- A path excluded from the approved selection must not receive either a content change or a mode change. Hard-blocked paths remain hard blocked regardless of mode metadata.
- Staging remains transport-only: it preserves source metadata but does not independently decide comparison, policy or delivery semantics.

The detailed implementation is intentionally split across 9.1 (representation/persistence), 9.4 (promotion plus ordinary import/review/delivery integration) and 9.8 (regression/E2E).


## User-controlled commit message

Phase 9 also closes the existing UX gap where Git delivery chooses a generated commit message without giving the user an explicit editable commit-message decision. This is a property of the ordinary Import approval/delivery pipeline, not of staging itself, so browser uploads and StagingImport promotions must converge on exactly the same behavior.

Rules:

- The ordinary review/approval UI shows a commit-message field before delivery.
- The current generated message may be used as an editable suggestion, but the user can replace it completely.
- The final normalized message is persisted with restart-safe import/approval state and bound to the explicit delivery intention before Git commit creation.
- Refresh, logout/login, backend restart and retry must preserve the exact selected message; retry must never silently regenerate it.
- Changing the message after approval requires a new explicit approval/delivery intention.
- Interactive new flows reject an empty/whitespace-only message; a deterministic compatibility fallback may exist only for legacy resume/internal callers that predate the field.
- Server-side bounds and control-character/newline normalization apply before the message reaches Git/GitHub.
- Work history continues to display GitHub's actual committed message after delivery.

The dedicated implementation is step 9.5, with final cross-source/retry regression in step 9.8.

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

## Implemented transport boundary (step 9.2)

`POST /api/staging-imports` is implemented as the only capability-protected staging-create route. It uses `X-ZipGitHub-Upload-Credential`, reuses `ZipIngestionService`, creates an `AVAILABLE` row and returns one raw 256-bit claim token only inside the creation response/URL fragment. The credential is deny-all when unconfigured. No anonymous list/read/download route exists. Authenticated claim and owner binding remain step 9.3.

## Proposed API shape

Exact resource names may be refined in phase 9, but the security boundaries should remain.

### Anonymous/capability upload

```http
POST /api/staging-imports
X-ZipGitHub-Upload-Credential: <deployment-upload-credential>
Content-Type: multipart/form-data
```

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

Implemented in r0095: `/staging/claim#token=...` captures the raw token into same-tab `sessionStorage`, removes the fragment, and lets normal GitHub OAuth return only to `/staging/claim`. The authenticated POST remains protected by the standard same-origin CSRF policy. The backend hashes the token before lookup, claims under a row lock, returns the same neutral 410 for invalid/expired/taken states, and treats same-owner retry as idempotent. No token is copied into OAuth state, query parameters or persisted browser storage.

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

## Implemented promotion boundary (step 9.4)

Implemented in r0096. Owner-scoped `GET /api/staging-imports/{id}` supports safe browser resume after claim, and `POST /api/staging-imports/{id}/promote` verifies staging ownership plus Project ownership before invoking the ordinary stored-upload import path. Promotion uses `ImportSource.STAGING_IMPORT` and stable non-secret `staging-import:<id>` source correlation. That correlation is persisted on the ordinary Import and allows restart recovery if the process stops between Import creation and the final staging `PROMOTED` transition. No ZIP bytes are copied or re-streamed.

The common upload/review/delivery path now carries Git ordinary-file modes. Trustworthy ZIP mode metadata is captured for both browser and staging sources; otherwise existing paths preserve base-tree mode and new paths default to `100644`. Mode-only changes are ordinary `MODIFIED` changes, included in immutable plan identity and selection/approval, applied only to selected paths and checked in the staged Git index before commit.

## Shared commit-message behavior (step 9.5)

Implemented in r0097. Once staging promotion has produced an ordinary Import, commit-message handling is identical to browser upload: the review shows an editable generated suggestion, interactive approval validates and persists the final message, and delivery uses only that approval-bound value. Staging carries no separate commit-message field or delivery rule.

## Implemented retention and abuse boundary (step 9.6)

Implemented in r0098. `AVAILABLE` defaults to 60 minutes and the first successful claim moves the row to a separately configured 240-minute claimed grace period; same-owner retries do not extend the deadline. Cleanup is scheduled/bounded, leaves restart-safe tombstones until physical deletion succeeds and never staging-deletes a `PROMOTED` artifact. Promotion and cleanup coordinate through PostgreSQL row locks, and cleanup reconciles an already-created ordinary `STAGING_IMPORT` Import before expiring a stale CLAIMED row. Deployment-wide live-object/live-byte quotas are serialized at insertion time. Per-capability/global request limits remain, with optional per-network-source limiting only behind an explicitly trusted forwarded-header ingress. Credential rotation is secret replacement + backend redeploy, with no database migration or current/previous grace generation; existing staging rows retain their independent claim/TTL semantics. See `docs/staging-retention-and-abuse.md`.

## Retention baseline

Recommended initial defaults:

- unclaimed `AVAILABLE`: about 1 hour;
- claimed but not promoted: short configurable grace period long enough for login/project choice, for example a few hours;
- promoted: storage ownership passes to the ordinary Import retention/resume model;
- cancelled/expired: become cleanup candidates immediately, with physical deletion performed by the normal cleanup mechanism.

The exact claimed grace period should be configured rather than hard-coded and finalized in phase 9.6.

## Abuse controls

At minimum phase 9 should enforce:

- existing compressed-byte and ingestion limits;
- request rate limit by upload capability and network source where practical;
- maximum concurrent incomplete staging uploads;
- maximum number/bytes of live staging objects per capability/deployment;
- no content execution;
- no anonymous list/read;
- generic auth/token errors;
- immediate deployment upload-credential revoke/rotation and incident instructions;
- metrics/logging based on staging ID/status only, never raw claim/capability tokens or ZIP contents.

## iOS Shortcut reference flow

The client side can remain intentionally small. A reference Shortcut should:

1. Accept Files from Share Sheet and restrict/validate that a single ZIP was supplied where practical.
2. Perform `Get Contents of URL` / HTTP POST to the staging endpoint.
3. Send the configured deployment upload credential in `X-ZipGitHub-Upload-Credential`.
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

The first phase-9 release uses a **static, pre-signed reference Shortcut** containing the current deployment-scoped upload credential. The signed `.shortcut` is published by zip-github for authenticated users to download/install; the backend does not dynamically generate or sign a per-user Shortcut. Anyone who extracts the credential can potentially consume staging upload capacity, so it remains a revocable secret even though its blast radius is deliberately limited. Possession still grants no ability to claim an upload, identify a user, select a Project or access GitHub.

The Shortcut release process is deliberately separate from runtime deployment. Create/update the reference Shortcut in a trusted Apple environment, sign it for sharing (`anyone`), then publish the exact signed artifact plus a non-secret Shortcut version/generation identifier. A practical GitHub Actions spike on 8 August 2026 confirmed that GitHub-hosted macOS exposes `shortcuts sign` but fails with `In order to do this, you must be signed into iCloud.` Consequently, dynamic signing on ordinary GitHub-hosted runners is not a phase-9 dependency.

On credential compromise, revoke the old deployment credential immediately, issue a new one, rebuild/sign/publish a new reference Shortcut and let old installations receive a clear outdated/revoked response that points to the authenticated Shortcut download page. The first version does not need parallel `current`/`previous` credentials.

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
- No per-user dynamically generated Shortcut and no requirement for automated iCloud/Apple signing infrastructure in GitHub Actions or the Java backend.

## Acceptance summary

Phase 9 is complete only when the same ZIP against the same locked base SHA converges to the same ordinary import plan whether the bytes arrived through the browser or through Shortcut/StagingImport, while claim ownership, expiry, cleanup, retry and abuse controls remain safe.

## Step 9.7 implementation state (r0099)

Authenticated static release distribution is implemented at `/shortcut`, `GET /api/shortcut-release` and `GET /api/shortcut-release/download`. Compose mounts `shortcut/releases` read-only; `.shortcut` binaries are ignored by Git because they embed the deployment-scoped staging-create credential. Missing/old/revoked credentials receive `403 STAGING_SHORTCUT_OUTDATED`, directing users back to the authenticated install page.

The step remains BLOCKED until the actual reference Shortcut is created/exported in Apple Shortcuts, signed for `anyone` from a trusted iCloud-signed-in Mac, published under `shortcut/releases/zip-github.shortcut`, and accepted by iOS. The backend intentionally provides no unsigned fallback or dynamic signing path. See `docs/signed-shortcut-release.md` and `docs/step-9.7-report.md`.
