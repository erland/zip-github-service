# OpenAI plugin distribution

zip-github has two plugin package targets because imported plugins that declare MCP servers directly are desktop-only in ChatGPT.

## ChatGPT web package

The primary ChatGPT package is app-bound and contains no `mcp.json`:

```text
zip-github/
├── plugin.json
├── .app.json
└── skills/
    └── zip-github/
        └── SKILL.md
```

The package references an already registered ChatGPT app through `.app.json`. The app must expose the deployed zip-github MCP endpoint and be available to the user. The package itself does not create or register that app.

Build it with:

```bash
ZIP_GITHUB_CHATGPT_APP_ID=asdk_app_example   node scripts/build-plugin.mjs --version 1.1.0 --target chatgpt
```

`ZIP_GITHUB_CHATGPT_APP_ID` must be a real ChatGPT app id beginning with `asdk_app_`, `connector_`, or `templated_apps_`. A plugin URL id such as `plugin_asdk_app_...` is not valid here.

The generated package is written under `build/plugin-chatgpt/zip-github/`.

## Desktop MCP package

A separate desktop package keeps the direct remote MCP declaration:

```text
zip-github/
├── plugin.json
├── mcp.json
└── skills/
    └── zip-github/
        └── SKILL.md
```

Build it with:

```bash
ZIP_GITHUB_MCP_URL=https://zip-github.apphome.one/mcp   node scripts/build-plugin.mjs --version 1.1.0 --target desktop
```

The generated package is written under `build/plugin-desktop/zip-github/`.

This package is intentionally treated as desktop-only by ChatGPT because it declares an MCP server directly.

## Behavior

Both targets describe the same narrow workflow:

1. transfer one user-provided ZIP to temporary staging;
2. return `review_url`;
3. continue repository selection, review, approval and Git delivery in the ordinary zip-github web UI.

The plugin must not imply that staging itself changed GitHub.

## App registration prerequisite

The ChatGPT web package cannot invent an app id. Before release, the deployed MCP endpoint must be registered as a ChatGPT app through a supported ChatGPT app flow, for example an app provisioned by ChatGPT Sites or another supported app registration path.

After registration:

1. obtain the app id, not the plugin id shown in a plugin URL;
2. store it as the repository variable `ZIP_GITHUB_CHATGPT_APP_ID`;
3. run repository CI;
4. publish or rerun the GitHub Release workflow;
5. install the generated ChatGPT package and complete the live step 10.2 acceptance.

## Release artifacts

A published GitHub Release tag is the sole version source for release artifacts.

The release workflow publishes:

- `zip-github-plugin-<version>.zip` — app-bound ChatGPT web package;
- `zip-github-plugin-desktop-<version>.zip` — direct-MCP desktop package;
- backend and frontend container images using the same release version.

The release workflow fails before plugin publication when `ZIP_GITHUB_CHATGPT_APP_ID` is missing. This is intentional: it prevents accidentally shipping another desktop-only package under the ChatGPT web artifact name.

If a release run fails, the workflow can be started manually with the existing `release_tag` to rebuild and re-upload the artifacts without moving or recreating the tag.

No session cookie, GitHub token, staging capability, client secret, private key or other secret may be embedded in either package.
