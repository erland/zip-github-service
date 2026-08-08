# Signed Shortcut release drop

Place the current Apple-signed reference Shortcut here as:

`zip-github.shortcut`

The binary is intentionally not committed because it embeds the deployment-scoped staging upload credential. The backend serves the file only to an authenticated zip-github user through `/api/shortcut-release/download`.

Use `scripts/sign-shortcut-release.sh` on a trusted, iCloud-signed-in Mac to sign an exported reference Shortcut for `anyone`, then copy the resulting file into this directory on the deployment host.
