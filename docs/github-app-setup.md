# GitHub OAuth and GitHub App setup

zip-github uses two separate GitHub integrations:

- a GitHub OAuth App identifies the browser user,
- a GitHub App grants repository access and creates branches, commits, pull requests and check-status reads.

Do not reuse a personal access token or embed a token in the frontend.

## Public URLs

Choose the final HTTPS origin before configuring GitHub. Example:

```text
Frontend origin: https://zip-github.example.com
OAuth callback:  https://zip-github.example.com/api/auth/github/callback
```

When frontend nginx proxies `/api` to the backend on the same public host, the callback should use that public host. The exact values must match `ZIP_GITHUB_FRONTEND_URL` and `GITHUB_OAUTH_CALLBACK_URL`.

## GitHub OAuth App

Create an OAuth App in the owning GitHub account or organization.

- Homepage URL: public frontend origin.
- Authorization callback URL: exact callback URL above.

Store the generated values as:

```text
GITHUB_OAUTH_CLIENT_ID
GITHUB_OAUTH_CLIENT_SECRET
```

The OAuth App is used for user identity. Repository write access comes from the GitHub App.

## GitHub App

Create a GitHub App with a clear production name and homepage URL.

Recommended repository permissions for the MVP:

- Contents: Read and write.
- Pull requests: Read and write.
- Checks: Read-only.
- Metadata: Read-only (implicit).

No organization administration permission is required. Do not request Actions write permission for the MVP.

The app does not require a webhook for the current polling-based implementation. Leave webhook delivery disabled unless a later step explicitly introduces it.

## Installation

Install the GitHub App only on repositories that users are allowed to import into. Prefer **Only select repositories** rather than all repositories.

The service intersects:

1. repositories available to the authenticated user,
2. repositories granted to the GitHub App installation.

Both checks must pass before a project can be configured.

## Private key

Generate a GitHub App private key and store it only in the server-side secret manager. The backend expects a PKCS#8 PEM value. Environment-file deployments can encode line breaks as literal `\n` sequences.

Configure:

```text
GITHUB_APP_ID
GITHUB_APP_PRIVATE_KEY
```

Never commit the PEM file, copy it into a container image, expose it to the frontend or place it in logs.

## Callback and proxy requirements

The reverse proxy must preserve:

- `Host`,
- `X-Forwarded-For`,
- `X-Forwarded-Proto`.

TLS must terminate at the proxy or load balancer. In production set:

```text
ZIP_GITHUB_SECURE_COOKIES=true
ZIP_GITHUB_CSRF_ENABLED=true
```

The configured frontend URL must be one exact origin, without a trailing path, wildcard or comma-separated alternatives.

## Verification checklist

1. Open the login endpoint and complete GitHub authorization.
2. Confirm the callback returns to the configured frontend origin.
3. List GitHub App installations.
4. Confirm only user-accessible, installation-approved repositories appear.
5. Create a project against a test repository.
6. Create a snapshot and confirm an installation token can read contents.
7. Complete a test delivery and draft pull request.
8. Confirm check status can be read for the delivered commit.

Use a dedicated private test repository before enabling production repositories.
