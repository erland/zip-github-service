# GitHub App setup

zip-github uses **one GitHub App** for both user authorization and repository automation. A separate GitHub OAuth App is not used.

The GitHub App serves two authentication roles:

1. **User authorization:** the browser OAuth-style web application flow exchanges the GitHub App Client ID and Client Secret for a GitHub App user access token. This token is kept server-side in the zip-github session and is used to discover only installations/repositories that the current user can access.
2. **Installation authentication:** the backend signs a GitHub App JWT with the App ID and private key, then creates short-lived installation access tokens for Git/repository operations.

Do not use a personal access token and do not expose any GitHub token to frontend JavaScript.

## Public URLs

Production example:

```text
Homepage URL: https://zip-github.isaksson.info
Callback URL: https://zip-github.isaksson.info/api/auth/github/callback
```

The callback must exactly match `GITHUB_APP_CALLBACK_URL`.

## GitHub App: identifying and authorizing users

On the GitHub App settings page configure:

- **Homepage URL:** `https://zip-github.isaksson.info`
- **Callback URL:** `https://zip-github.isaksson.info/api/auth/github/callback`
- Generate a **Client Secret** and store it with the GitHub App **Client ID**.
- **Request user authorization (OAuth) during installation:** leave this **off** for zip-github. The application's own **Logga in med GitHub** button starts the web application flow and explicitly supplies the callback URL.
- **Enable Device Flow:** off; the current web UI does not use device flow.

Configure the server with:

```text
GITHUB_APP_CLIENT_ID
GITHUB_APP_CLIENT_SECRET
GITHUB_APP_CALLBACK_URL
```

The Client ID is not the same as the numeric App ID.

## Repository permissions

Recommended MVP permissions:

- Contents: Read and write
- Pull requests: Read and write
- Checks: Read-only
- Actions: Read and write
- Metadata: Read-only (implicit/required)

Steps 8.1–8.2 use Actions read access. Step 8.3 additionally performs only explicitly allowlisted `workflow_dispatch` and failed-job rerun operations, which require Actions write. Before each such write the backend reads the owner-scoped installation metadata with the App JWT and requires `permissions.actions` to be `write`; GitHub then independently enforces the same permission on the Actions endpoint. Existing installations may require owner approval when the App permission is upgraded. No generic arbitrary Actions write surface is exposed, and webhooks are not required by the current implementation.

## Installation

Install the GitHub App on the repositories that zip-github may use. Prefer **Only select repositories** during controlled rollout.

The service uses the GitHub App user access token to call the user-scoped installation endpoints. GitHub therefore returns the intersection of repositories available to the user and repositories granted to the GitHub App installation.

## App ID and private key

The same GitHub App also provides:

```text
GITHUB_APP_ID
GITHUB_APP_PRIVATE_KEY
```

`GITHUB_APP_ID` is the numeric App ID and is distinct from `GITHUB_APP_CLIENT_ID`. Generate a private key in the GitHub App settings. The backend expects a PKCS#8 PEM value; environment-file deployments may encode line breaks as literal `\n`.

Never commit the private key, include it in a container image, expose it to the frontend or log it.

## Production environment summary

```dotenv
ZIP_GITHUB_FRONTEND_URL=https://zip-github.isaksson.info
GITHUB_APP_CALLBACK_URL=https://zip-github.isaksson.info/api/auth/github/callback
GITHUB_APP_CLIENT_ID=<GitHub App Client ID>
GITHUB_APP_CLIENT_SECRET=<GitHub App Client Secret>
GITHUB_APP_ID=<numeric GitHub App ID>
GITHUB_APP_PRIVATE_KEY=<PKCS#8 PEM with \n escapes>
```

The old `GITHUB_OAUTH_CLIENT_ID`, `GITHUB_OAUTH_CLIENT_SECRET` and `GITHUB_OAUTH_CALLBACK_URL` variables are no longer used.

## Verification checklist

1. Open the login endpoint and authorize the GitHub App.
2. Confirm callback returns to `https://zip-github.isaksson.info`.
3. `GET /api/github/installations` returns the installations accessible to that user.
4. Repository selection contains only repositories available to both the user and the installation.
5. Create a project in a dedicated test repository.
6. Complete a test ZIP delivery and draft pull request.
7. Confirm check status can be read.
8. Confirm workflow runs/jobs for the delivered commit can be read through the result page and that the full run opens on GitHub.
9. With dedicated test workflows in the operation-specific allowlists, confirm one manual dispatch and one failed-job rerun succeed for the current Work head, while an unlisted workflow is rejected.
