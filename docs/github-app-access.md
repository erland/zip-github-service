# GitHub App access model

## Purpose

GitHub OAuth in zip-github is the GitHub App user authorization flow. It establishes the user's identity and returns a GitHub App user access token. Repository automation remains separate and uses short-lived installation access tokens.

## User-visible discovery

The backend exposes:

- `GET /api/github/installations`
- `GET /api/github/installations/{installationId}/repositories`

Both endpoints require the opaque zip-github web session. The browser never receives either the GitHub user access token or an installation token.

Installation discovery uses the authenticated user's GitHub App user access token. Repository discovery uses the user-scoped installation repositories endpoint. This produces the intersection of:

1. repositories available to the GitHub App installation; and
2. repositories available to the authenticated GitHub user.

Before repository lookup, the backend verifies that the requested installation occurs in the current user's installation list. An installation ID belonging only to another user is returned as `404 GITHUB_INSTALLATION_NOT_FOUND`.

## Installation tokens

`GitHubAppClient#createInstallationToken`:

1. signs a short-lived RS256 GitHub App JWT using the server-side private key;
2. calls GitHub's installation-token endpoint;
3. returns the token only to server-side application code.

Installation tokens are created on demand, are not included in API DTOs, are not sent to frontend JavaScript and are not persisted by this implementation. Later Git and pull-request operations must request a fresh token near the operation that uses it.

## Configuration

Required server-side environment variables (all from the same GitHub App):

- `GITHUB_APP_CLIENT_ID` — the GitHub App client ID used for user authorization;
- `GITHUB_APP_CLIENT_SECRET` — the GitHub App client secret used for user authorization;
- `GITHUB_APP_ID` — numeric GitHub App ID, distinct from the client ID;
- `GITHUB_APP_PRIVATE_KEY` — PKCS#8 RSA private key in PEM form. Literal `\\n` sequences are accepted.

The private key and all GitHub tokens must be provided only to the backend process.

## Current storage limitation

The GitHub App user access token is held only inside the in-memory server-side session created in step 2.2. It is not persistent and is lost on backend restart. Before horizontally scaled or durable production deployment, the session store must be replaced by shared encrypted storage or another suitable server-side session mechanism.

## Security invariants

- Installation and repository endpoints require an authenticated web session.
- A requested installation must be visible to the current GitHub user.
- Repository discovery uses the user-and-app intersection, not the broader installation token view.
- Installation tokens are short-lived and server-side only.
- Token strings are absent from response records and must not be logged.
- Unknown or cross-user installation IDs are hidden as `404`.
