# Signed Shortcut release drop

Place the current Apple-signed reference Shortcut here as:

`zip-github.shortcut`

The binary is intentionally not committed because it embeds the deployment-scoped staging upload credential. The backend serves the file only to an authenticated zip-github user through `/api/shortcut-release/download`.

Use `scripts/sign-shortcut-release.sh` on a trusted, iCloud-signed-in Mac to sign an exported reference Shortcut for `anyone`, then copy the resulting file into this directory on the deployment host.

## Current delivery artifact

The r0103 delivery bundle contains the operator-provided Apple-signed `zip-github.shortcut` (version `1`, generation `g1`, 23821 bytes, SHA-256 `21a9e220067681994ff42326a0b430261fe84583bfbc614297c634ae752af50a`). It is intentionally still matched by `.gitignore` and by the server import policy's `SIGNED_SHORTCUT_SECRET_ARTIFACT` hard block because the binary embeds the staging upload credential.

The artifact is present in the downloadable deployment bundle so `docker-compose.yml` can mount and serve it, but it must never be intentionally committed to a public or private Git repository.

## CI verification

`release-manifest.txt` is source-tracked and records the exact expected signed artifact identity. Clean GitHub Actions checkouts intentionally do not contain `zip-github.shortcut`; CI therefore verifies the manifest, the git-ignore/import-policy protections, and (when the binary is present in a deployment bundle) verifies its exact size and SHA-256.


## Runtime/read-download contract

The technical deployment filename remains `zip-github.shortcut`. It must be readable by the backend runtime user; the signing helper uses mode `0644` for the standard bind-mount deployment. The authenticated HTTP endpoint exposes the user-facing download name `Skicka till zip-github.shortcut` through `Content-Disposition` without modifying the signed bytes.
