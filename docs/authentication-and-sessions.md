# GitHub authentication and web sessions

Step 2.2 replaces the temporary `X-Zip-Github-User` header with GitHub OAuth login and an opaque server-side web session.

## Endpoints

- `GET /api/auth/github/login?returnTo=/projects`
- `GET /api/auth/github/callback`
- `GET /api/auth/me`
- `POST /api/auth/logout`

## Security model

- OAuth `state` is random, short-lived, server-side, single-use and also bound to an HttpOnly cookie.
- The application exchanges the authorization code server-side; OAuth credentials and access tokens never reach browser JavaScript.
- The browser receives only an opaque `zip_github_session` cookie.
- Session cookies are `HttpOnly`, `SameSite=Lax`, path `/`, and `Secure` outside local development/test.
- Logout invalidates the server record and expires the cookie.
- User IDs are deterministic UUIDs derived from the immutable GitHub numeric user ID.
- Repository authorization is intentionally separate and will use GitHub App installation tokens in step 2.3.

The current in-memory session store is suitable for the API skeleton and tests. Before multi-instance production deployment it must be replaced by a shared persistent/cache-backed implementation while retaining the same opaque-cookie contract.

## Step 2.3 extension

The GitHub authorization is configured as GitHub App user authorization. Its user access token is retained only inside the server-side session so the backend can discover the installations and repositories available to both the user and this GitHub App. Repository write automation still uses separately generated, short-lived installation tokens; the user access token is never returned to frontend JavaScript.
