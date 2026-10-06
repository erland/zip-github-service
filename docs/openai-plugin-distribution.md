# OpenAI plugin distribution

zip-github has two release package targets.

## ChatGPT update package

The primary ChatGPT artifact is an update package for the already registered ChatGPT app created from the deployed MCP endpoint. It intentionally matches the structure exported by ChatGPT and has no extra top-level directory:

```text
.app.json
.codex-plugin/
└── plugin.json
```

The package preserves two stable identities:

- the registered ChatGPT App ID from `ZIP_GITHUB_CHATGPT_APP_ID`, for example `asdk_app_...`;
- the plugin name used by the ChatGPT export, normally `dev-<app-id-suffix>`.

By default the build derives the plugin name from the App ID. If an exported plugin uses a different stable name, set `ZIP_GITHUB_CHATGPT_PLUGIN_NAME` to that exact `dev-...` value.

Build it with:

```bash
ZIP_GITHUB_CHATGPT_APP_ID=asdk_app_example \
  node scripts/build-plugin.mjs --version 1.1.0 --target chatgpt
```

The generated files are written directly under `build/plugin-chatgpt/`.

For the registered zip-GitHub app, the known identities are:

```text
App ID: asdk_app_6ac482935f0881919b14e5d75c0fe8a6
Plugin name: dev-6ac482935f0881919b14e5d75c0fe8a6
```

The ChatGPT UI assigns its own `asdk_app_v_...` Version ID when a new app version is created. That Version ID must not be embedded in the ZIP.

The GitHub release ZIP is created with `.app.json` and `.codex-plugin/` at the archive root, so it can be selected as the new version upload for the existing ChatGPT app.

## Desktop MCP package

A separate portable package keeps the direct remote MCP declaration and Agent Plugins structure:

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
ZIP_GITHUB_MCP_URL=https://zip-github.apphome.one/mcp \
  node scripts/build-plugin.mjs --version 1.1.0 --target desktop
```

The generated package is written under `build/plugin-desktop/zip-github/`.

## Behavior

Both distributions refer to the same narrow workflow:

1. transfer one user-provided ZIP to temporary staging;
2. return `review_url`;
3. continue repository selection, review, approval and Git delivery in the ordinary zip-github web UI.

The plugin must not imply that staging itself changed GitHub.

## App registration prerequisite

Register the deployed MCP endpoint in ChatGPT first. The registered app supplies the stable App ID used by subsequent update packages.

After registration:

1. obtain the App ID (`asdk_app_...`), not the `plugin_asdk_app_...` URL wrapper and not the `asdk_app_v_...` Version ID;
2. store it as repository variable `ZIP_GITHUB_CHATGPT_APP_ID`;
3. optionally store the exact exported `dev-...` plugin name as `ZIP_GITHUB_CHATGPT_PLUGIN_NAME` if it differs from the derived default;
4. publish a GitHub release;
5. download `zip-github-plugin-<version>.zip`;
6. upload that ZIP as the new version of the existing ChatGPT app.

## Release artifacts

A published GitHub Release tag is the sole version source for release artifacts.

The release workflow publishes:

- `zip-github-plugin-<version>.zip` — ChatGPT update ZIP with no extra top-level directory;
- `zip-github-plugin-desktop-<version>.zip` — direct-MCP desktop package;
- backend and frontend container images using the same release version.

The release workflow fails before plugin publication when `ZIP_GITHUB_CHATGPT_APP_ID` is missing. This prevents producing an update package without a stable app binding.

If a release run fails, the workflow can be started manually with the existing `release_tag` to rebuild and re-upload the artifacts without moving or recreating the tag.

No session cookie, GitHub token, staging capability, client secret, private key, signed file URL or other secret may be embedded in either package.
