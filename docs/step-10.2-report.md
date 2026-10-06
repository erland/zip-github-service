# Step 10.2 report — ChatGPT plugin package and deployed acceptance

Date: 2026-10-06  
Revision: r0179  
Product version: 1.0.0-rc.131  
Status: BLOCKED — app-bound packaging implemented; ChatGPT app registration and live deployed acceptance remain required.

## Implemented

- Added a portable Agent Plugins 1.0 source package under `plugin/`.
- Added `plugin.json` and `mcp.json` templates.
- Corrected the ChatGPT distribution after live testing showed that an imported package containing `mcp.json` is desktop-only.
- Added `plugin/app.template.json` and an app-bound ChatGPT build target that emits `.app.json` and deliberately omits `mcp.json`.
- Kept a separate desktop target with the direct remote MCP declaration.
- Added a minimal `zip-github` skill that instructs the host to call only `stage_zip`, return `review_url`, and leave repository selection/review/delivery to the web UI.
- Added `scripts/build-plugin.mjs` with semantic-version, HTTPS `/mcp`, identity, endpoint and short-description validation.
- Added CI package validation using a non-production example endpoint.
- Added a GitHub Release workflow that creates `zip-github-plugin-<version>.zip` with one top-level `zip-github/` directory and attaches it to the release.
- Generated plugin output is ignored from source control.

## Intended production packages

Primary ChatGPT web package:

```text
zip-github/
├── plugin.json
├── .app.json
└── skills/
    └── zip-github/
        └── SKILL.md
```

Desktop fallback package:

```text
zip-github/
├── plugin.json
├── mcp.json
└── skills/
    └── zip-github/
        └── SKILL.md
```

The registered ChatGPT app must ultimately connect to the production MCP endpoint at `https://zip-github.apphome.one/mcp`.

## Verification performed

Step 10.1 PR #46 completed successfully and its final CI and Coolify validation were green before merge.

For step 10.2, the package is covered by repository CI. A live external acceptance was attempted from the available execution environment, but DNS resolution for `zip-github.apphome.one` failed before any HTTP request could reach the service. This is an environment/deployment verification limitation, not evidence that the endpoint itself is failing.

Live testing also confirmed that the original direct-MCP plugin package was marked desktop-only by ChatGPT. The source package has therefore been corrected to support an app-bound web target. Because a real ChatGPT app id is still required before that target can be installed and tested, step 10.2 is not marked DONE.

## Remaining acceptance gate

After rc.131 is built/deployed:

1. Register the deployed MCP endpoint as a supported ChatGPT app and obtain its real app id.
2. Configure repository variable `ZIP_GITHUB_CHATGPT_APP_ID` and build the app-bound package.
3. Install the generated ChatGPT web plugin and confirm it is not marked desktop-only.
4. Confirm tool discovery exposes exactly `stage_zip`.
5. Supply a real ZIP file from ChatGPT.
6. Confirm `stage_zip` returns staging metadata and a `review_url`.
7. Open the URL and verify the ordinary authenticated staging claim page loads.
8. Verify repository selection/review remains in the web UI and no GitHub write occurred merely from staging.

Only after these checks pass may step 10.2 be marked DONE.

## Changed files

Added:
- `plugin/plugin.template.json`
- `plugin/mcp.template.json`
- `plugin/skills/zip-github/SKILL.md`
- `scripts/build-plugin.mjs`
- `docs/openai-plugin-distribution.md`
- `.github/workflows/release-plugin.yml`
- `docs/step-10.2-report.md`

Modified:
- `.github/workflows/ci.yml`
- `.gitignore`
- `README.md`
- `docs/implementation-status.md`
- `CHANGELOG.md`
- `scripts/verify-release.sh`
- `VERSION`
- `.env.example`
- `docker-compose.yml`
