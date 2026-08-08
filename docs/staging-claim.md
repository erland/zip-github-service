# Authenticated StagingImport claim — step 9.3

## Boundary

The Shortcut upload capability remains transport-only. Ownership is established only by the normal authenticated browser session through `POST /api/staging-imports/claim`.

The raw claim token is a short-lived bearer secret. The Shortcut opens `/staging/claim#token=...`; browser code moves the token to same-tab `sessionStorage` and removes the fragment from the address bar before login continues. The token is never copied into OAuth `state`, `returnTo`, query parameters, analytics or server logs.

## Login continuation

When no web session exists, the ordinary GitHub OAuth login uses only `returnTo=/staging/claim`. The token stays in the same browser tab's `sessionStorage`. After the OAuth callback, the authenticated claim page reads the token and submits it in a JSON body with the existing same-origin CSRF marker.

This intentionally does not support transferring a pending claim between devices or browsers.

## Atomic ownership

The backend SHA-256 hashes the presented token and selects the matching staging row under a database row lock. It then applies these rules atomically:

- `AVAILABLE` + unexpired -> bind `owner_user_id`, record `claimed_at`, transition to `CLAIMED`;
- `CLAIMED` by the same owner -> idempotent success, for lost-response retry;
- wrong token, expired row, terminal row, promoted row or a row claimed by another owner -> the same neutral `410 STAGING_CLAIM_UNAVAILABLE` response;
- an expired `AVAILABLE` row is marked `EXPIRED` while holding the lock.

No caller-supplied staging ID participates in claim, so opaque IDs do not become a second bearer credential.

## Response and authority

Successful claim returns owner-safe ZIP metadata: staging ID, original filename, size, SHA-256, expiry and claim time. It does not return storage paths, raw token, upload credential, Project data or GitHub data.

Claim does not create an ordinary `Import`, select a Project or authorize GitHub. Those remain step 9.4 and the existing Project/Import/GitHub App pipeline.

## Retry behavior

The browser removes the local token after confirmed success. On request failure it keeps the token in same-tab `sessionStorage`, allowing a reload to retry. This is safe because the backend is idempotent for the same authenticated owner.

## Step 9.6 claimed grace

A successful first claim replaces the short AVAILABLE deadline with the configurable claimed grace deadline (`ZIP_GITHUB_STAGING_CLAIMED_TTL_MINUTES`, default 240 minutes). Same-owner retry remains idempotent and does not extend the deadline again, preventing indefinite retention through repeated claim requests.
