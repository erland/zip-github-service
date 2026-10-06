# Step 10.2 report — ChatGPT plugin package and deployed acceptance

Date: 2026-10-06  
Revision: r0179  
Product version: 1.0.0-rc.131  
Status: BLOCKED — packaging implemented; live deployed acceptance remains required.

## Implemented

- Added a portable Agent Plugins 1.0 source package under `plugin/`.
- Added `plugin.json` and `mcp.json` templates.
- Added a minimal `zip-github` skill that instructs the host to call only `stage_zip`, return `review_url`, and leave repository selection/review/delivery to the web UI.
- Added `scripts/build-plugin.mjs` with semantic-version, HTTPS `/mcp`, identity, endpoint and short-description validation.
- Added CI package validation using a non-production example endpoint.
- Added a GitHub Release workflow that creates `zip-github-plugin-<version>.zip` with one top-level `zip-github/` directory and attaches it to the release.
- Generated plugin output is ignored from source control.

## Intended production package

```text
zip-github/
├── plugin.json
├── mcp.json
└── skills/
    └── zip-github/
        └── SKILL.md
```

The production MCP endpoint is `https://zip-github.apphome.one/mcp`.

## Verification performed

Step 10.1 PR #46 completed successfully and its final CI and Coolify validation were green before merge.

For step 10.2, the package is covered by repository CI. A live external acceptance was attempted from the available execution environment, but DNS resolution for `zip-github.apphome.one` failed before any HTTP request could reach the service. This is an environment/deployment verification limitation, not evidence that the endpoint itself is failing.

Because the planned step explicitly requires a deployed MCP upload/claim acceptance, step 10.2 is not marked DONE.

## Remaining acceptance gate

After rc.131 is built/deployed:

1. Connect/install the generated plugin using `https://zip-github.apphome.one/mcp`.
2. Confirm tool discovery exposes exactly `stage_zip`.
3. Supply a real ZIP file from ChatGPT.
4. Confirm `stage_zip` returns staging metadata and a `review_url`.
5. Open the URL and verify the ordinary authenticated staging claim page loads.
6. Verify repository selection/review remains in the web UI and no GitHub write occurred merely from staging.

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
