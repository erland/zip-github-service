# OpenAI plugin distribution

zip-github ships a portable Agent Plugins 1.0 package that connects ChatGPT/Codex to the existing remote MCP server.

The package contains:

```text
zip-github/
├── plugin.json
├── mcp.json
└── skills/
    └── zip-github/
        └── SKILL.md
```

The plugin does not deploy the service and does not contain credentials. It declares the existing Streamable HTTP endpoint at `https://zip-github.apphome.one/mcp`.

## Behavior

The plugin intentionally exposes only the existing `stage_zip` workflow:

1. transfer one user-provided ZIP to temporary staging;
2. return `review_url`;
3. continue repository selection, review, approval and Git delivery in the ordinary zip-github web UI.

The plugin must not imply that staging itself changed GitHub.

## Build

```bash
node scripts/build-plugin.mjs --version 1.0.0-rc.131
```

Override the endpoint only when testing another deployment:

```bash
ZIP_GITHUB_MCP_URL=https://zip-github.example.test/mcp \
  node scripts/build-plugin.mjs --version 0.0.0-ci
```

Generated files are written under `build/plugin/zip-github/` and are not committed.

## Release artifact

A published GitHub Release tag is the sole version source for all release artifacts. The workflow normalizes an optional leading `v` and uses that version for backend image, frontend image and plugin package. `VERSION` is intentionally ignored for release publication.

The plugin artifact is built as `zip-github-plugin-<version>.zip` with one top-level `zip-github/` directory and attached to the corresponding GitHub Release. If a release run fails, the workflow can be started manually with the existing `release_tag` to rebuild and re-upload the artifacts without moving or recreating the tag.

No session cookie, GitHub token, staging capability, client secret, private key or other secret may be embedded in the package.
