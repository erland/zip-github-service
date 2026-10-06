# MCP staging upload

## Purpose

zip-github exposes a deliberately narrow Model Context Protocol (MCP) surface at `/mcp`.
The first supported goal is to transfer a user-provided ZIP from ChatGPT into the existing
staging pipeline and then hand the user over to the ordinary zip-github browser UI.

MCP does **not** select a GitHub repository, approve an import plan, write Git, create a pull
request, or receive GitHub credentials.

## Tool

### `stage_zip`

Input:

- one top-level `file` parameter declared through `_meta["openai/fileParams"]`;
- ChatGPT supplies `download_url` and `file_id`, with optional `mime_type` and `file_name`;
- the filename must identify a `.zip` archive.

Output:

- `staging_id`
- `filename`
- `size_bytes`
- `sha256`
- `expires_at`
- `review_url`

The tool streams the temporary file URL directly into the existing `StagingUploadService`.
The returned `review_url` is the same fragment-token claim URL used by the Shortcut/staging
flow. The user must open that URL and use the normal authenticated web application to claim
the upload, choose the repository, review the immutable plan and perform any Git delivery.

## Transport

The endpoint uses MCP JSON-RPC over Streamable HTTP semantics at:

```text
https://zip-github.apphome.one/mcp
```

The implementation is stateless and currently supports the protocol operations needed for
tool discovery and invocation: `initialize`, `ping`, `tools/list`, `tools/call`, and
client notifications.

## Security boundary

The MCP surface deliberately has less authority than the browser application:

- no GitHub user or installation token is accepted or returned;
- no repository/project identifiers are accepted;
- the tool only creates a temporary staging object;
- ordinary staging TTL, capacity, ZIP byte limits and archive inspection still apply;
- each call creates a new staging object and is rate limited in-process;
- the tool-provided `download_url` is not treated as an arbitrary fetch URL.

To avoid turning zip-github into an SSRF proxy, MCP file downloads are restricted to HTTPS,
port 443 and the explicit `ZIP_GITHUB_MCP_FILE_DOWNLOAD_HOSTS` allowlist. Redirect targets
are revalidated before following them. The default is the current ChatGPT temporary-file host
`files.oaiusercontent.com`.

A deployment should keep the allowlist as narrow as possible. If ChatGPT changes its file
download host, update the deployment variable only after verifying the new host.

## Authentication model

Step 10.1 intentionally exposes no account-level MCP authorization because the tool cannot
read private zip-github data or perform GitHub actions. The opaque claim token remains the
capability needed to attach the staged bytes to a user, and claiming still requires the
normal zip-github browser login.

If the MCP surface later gains project reads, approvals or Git actions, those tools require a
separate authenticated MCP design rather than extending this anonymous staging capability.

## Next step

Package the remote MCP endpoint as a zip-github ChatGPT plugin with a small skill that tells
the model to call `stage_zip`, return `review_url`, and leave all review/delivery actions to
the ordinary web UI. Then run an end-to-end test against the deployed HTTPS endpoint.
