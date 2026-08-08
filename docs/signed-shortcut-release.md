# Signed iOS Shortcut release

## Boundary

Phase 9 uses one static, pre-signed reference Shortcut. The Java backend never generates or Apple-signs a Shortcut and does not require an iCloud identity. The Shortcut contains only the deployment-scoped staging-create credential and the deployment URL; it contains no GitHub token, GitHub App key, user ID, Project ID or import-policy logic.

## Reference Shortcut actions

Create the reference Shortcut in a trusted Apple environment. Configure it to be available from the Share Sheet for files and keep the flow deliberately small:

1. Require exactly one input file and reject/stop when the supplied file is not a ZIP where Shortcuts can determine that reliably.
2. Perform an HTTP `POST` to `<ZIP_GITHUB_FRONTEND_URL>/api/staging-imports` with the ZIP bytes as the request body.
3. Set `Content-Type: application/zip`, `X-Filename` to the source file name and `X-ZipGitHub-Upload-Credential` to the current deployment staging credential.
4. Read the JSON success response and open `claimUrl` in the default browser.
5. For `403` / `STAGING_SHORTCUT_OUTDATED`, tell the user that the Shortcut is outdated and that the current version can be downloaded after signing in to zip-github.
6. For `413`, report that the ZIP exceeds server limits. For `429`, report temporary rate/capacity limiting. For network/server failures, leave the local ZIP untouched and allow a later retry.
7. Do not display, persist or copy the upload credential or claim token into diagnostics/notifications.

The Shortcut itself should have a human-visible version/generation in its name/comment (for example `zip-github 1 / g1`), but the secret must never be used as the version identifier.

## Sign and publish

1. Export the configured reference Shortcut from Shortcuts on a trusted Mac.
2. Make sure the Mac is signed into iCloud. The practical GitHub-hosted macOS spike showed that `/usr/bin/shortcuts` exists on hosted runners but `shortcuts sign` fails with `In order to do this, you must be signed into iCloud.`
3. Sign for general sharing:

   `scripts/sign-shortcut-release.sh exported.shortcut shortcut/releases/zip-github.shortcut`

4. Set non-secret deployment metadata `ZIP_GITHUB_SHORTCUT_VERSION` and `ZIP_GITHUB_SHORTCUT_GENERATION`.
5. Deploy/restart zip-github. The Compose deployment mounts `./shortcut/releases` read-only into the backend.
6. Sign in and open `/shortcut`. Verify the release metadata and download the exact signed artifact.
7. On an iOS device, open the downloaded file and confirm Shortcuts accepts it for installation. This final Apple/iOS check is intentionally part of step 9.8 as well.

The signed `.shortcut` is ignored by Git because it embeds the low-privilege deployment credential. It belongs in deployment secret/release handling, not source control.

## Credential rotation/revoke

There is no current/previous grace generation in the first version.

1. Replace `ZIP_GITHUB_STAGING_UPLOAD_CREDENTIAL` with a newly generated strong credential.
2. Update the reference Shortcut with the same new credential in a trusted Apple environment.
3. Export/sign it and replace `shortcut/releases/zip-github.shortcut`.
4. Advance the non-secret Shortcut version/generation and redeploy.
5. Old installations immediately receive `403 STAGING_SHORTCUT_OUTDATED`; already-created staging rows keep their independent claim token and TTL semantics.
6. Users sign in to zip-github and download/install the current release from `/shortcut`.

Rotation of this credential never requires GitHub OAuth, GitHub App or repository credential rotation.

## Authenticated distribution

`GET /api/shortcut-release` and `GET /api/shortcut-release/download` require the normal zip-github web session. Metadata exposes only version, generation, filename, size and SHA-256. The download endpoint is `private, no-store` and never exposes the staging credential separately from the signed binary.

If no readable `.shortcut` is configured, metadata returns `available=false` and the UI explains that an administrator must publish the signed artifact. The backend does not substitute an unsigned or dynamically generated file.

## Published reference artifact — r0103

The operator supplied an Apple-signed reference Shortcut and confirmed the Shortcut itself on iPhone. The r0103 deployment bundle publishes it at `shortcut/releases/zip-github.shortcut` with default metadata version `1`, generation `g1`, size 23821 bytes and SHA-256 `21a9e220067681994ff42326a0b430261fe84583bfbc614297c634ae752af50a`.

Because the signed binary embeds the staging upload credential, the source-control ignore remains in place and the ordinary zip-github import policy now hard-blocks this exact release path with policy code `SIGNED_SHORTCUT_SECRET_ARTIFACT`. This permits the deployment ZIP to carry the release while preventing an accidental Git delivery when the same ZIP is reviewed through zip-github.

The final 9.7 gate is operational: deploy r0103, sign in to `/shortcut`, download the served artifact, and confirm that iOS accepts that downloaded copy. Until that exact served-copy check is reported successful, 9.7 remains `BLOCKED` rather than `DONE`.
