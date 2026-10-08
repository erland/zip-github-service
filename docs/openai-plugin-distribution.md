# OpenAI plugin distribution

zip-GitHub publishes a complete portable Agent Plugins package that is also the author-supplied ZIP for Marketplace / public submission.

## Package layout

The release artifact `zip-github-plugin-<version>.zip` contains one self-contained plugin directory:

```text
zip-github/
├── plugin.json
├── mcp.json
├── assets/
│   ├── logo.png
│   └── composer-icon.png
└── skills/
    └── zip-github/
        └── SKILL.md
```

The public upload deliberately contains **no** `.app.json`, no non-null `apps` declaration and no `.codex-plugin/` update overlay. The Developer Portal can create the app binding during MCP conversion after the author-supplied package is imported.

## MCP integration

`mcp.json` points at the deployed Streamable HTTP endpoint:

```text
https://zip-github.apphome.one/mcp
```

Override it for non-production builds with `ZIP_GITHUB_MCP_URL`.

## Marketplace listing metadata

`plugin.json` contains the public listing metadata under `extensions.com.openai.interface`, including:

- display name, descriptions, category and default prompts;
- website: `https://zip-github.apphome.one/about`;
- support: `https://zip-github.apphome.one/support`;
- privacy policy: `https://zip-github.apphome.one/privacy`;
- terms of service: `https://zip-github.apphome.one/terms`;
- `assets/logo.png` and `assets/composer-icon.png`.

The four public pages are available without GitHub login. They identify the service and publisher, explain support, and describe the service's data handling and terms.

Publication metadata declares no commerce and limits availability to Sweden (`SE`).

## Build

Build a public package directory with:

```bash
ZIP_GITHUB_MCP_URL=https://zip-github.apphome.one/mcp \
  node scripts/build-plugin.mjs --version 1.0.0 --target marketplace
```

The generated directory is:

```text
build/plugin-marketplace/zip-github/
```

Create the upload ZIP from the directory above, keeping `zip-github/` as the single top-level plugin directory.

A `desktop` build target is retained for local/portable workflows and uses the same complete direct-MCP package structure.

## Behavior

The plugin has one intentionally narrow handoff:

1. transfer one user-provided ZIP to temporary staging;
2. return `review_url`;
3. continue repository selection, review, approval and Git delivery in the ordinary zip-GitHub web UI.

Staging itself does not change GitHub. Repository selection, approval and Git writes remain outside the MCP tool surface.

## Release artifact

A published GitHub Release tag is the version source. The release workflow publishes:

- backend and frontend container images;
- `zip-github-plugin-<version>.zip` — complete Marketplace/portable plugin package.

No GitHub token, session cookie, staging capability, signed temporary file URL, client secret or private key may be embedded in the ZIP.

## Marketplace handoff

The ZIP is the source package for the public submission flow. Portal-only work remains separate from package creation, including verified developer/publisher setup, country targeting, reviewer access where applicable, demo/review materials, attestations and the final submit-for-review action.
