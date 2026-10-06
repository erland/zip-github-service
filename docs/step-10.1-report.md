# Step 10.1 report — MCP staging-upload bridge

Date: 2026-10-06  
Revision: r0178  
Product version: 1.0.0-rc.130

## Goal

Add the smallest useful MCP integration: transfer one ChatGPT-provided ZIP into the existing
staging pipeline and return the ordinary browser claim/review URL. Do not expose repository
selection, review approval or Git delivery through MCP.

## Implemented

- Added a stateless MCP endpoint at `/mcp`.
- Added exactly one tool, `stage_zip`.
- Declared the tool's file input using ChatGPT `openai/fileParams` metadata and the required four-field file schema.
- Reused `StagingUploadService`, so MCP and Shortcut uploads converge on the same `StoredUploadArtifact`/StagingImport path.
- Returned the existing fragment-token claim URL as `review_url`.
- Added bounded in-process call throttling in addition to existing staging capacity limits.
- Added an HTTPS host allowlist and redirect revalidation for temporary file downloads.
- Proxied `/mcp` through the frontend nginx container so the public endpoint can live on the same origin as the web UI.
- Added deployment configuration for the allowed ChatGPT temporary-file host.
- Added backend tests for MCP initialization/tool discovery, successful staging handoff, non-ZIP rejection and URL allowlist behavior.

## Security properties

The MCP tool has no GitHub authority. It cannot choose a repository, approve a plan, commit,
push or create a pull request. A staged ZIP is unusable for Git delivery until a user opens
the returned claim URL, signs in through the existing web flow and completes the normal
review/approval path.

The downloader accepts HTTPS only, rejects userinfo and non-443 ports, and accepts only
configured hosts. Redirects are checked again before connection.

## Verification

Attempted verification:

- direct local clone/build was unavailable because the local execution environment could not resolve GitHub;
- Agent Workspace sandbox creation was attempted as the alternate Java 21 verification path but the service returned an internal tool failure.

Static verification performed in this step:

- existing Quarkus/Jackson dependencies are sufficient; no second runtime or MCP sidecar was introduced;
- `stage_zip` delegates to the existing `StagingUploadService` rather than duplicating ZIP persistence or claim-token logic;
- nginx and both Compose variants expose/pass the new endpoint/configuration;
- focused Quarkus/JUnit tests are included and are expected to run in repository CI.

The pull request CI is therefore the executable quality gate for this revision.

## Changed files

Added:

- `backend/src/main/java/info/isaksson/erland/zipgithub/mcp/McpResource.java`
- `backend/src/main/java/info/isaksson/erland/zipgithub/mcp/McpFileDownloadService.java`
- `backend/src/test/java/info/isaksson/erland/zipgithub/mcp/McpResourceTest.java`
- `backend/src/test/java/info/isaksson/erland/zipgithub/mcp/McpFileDownloadServiceTest.java`
- `docs/mcp-staging-upload.md`
- `docs/step-10.1-report.md`

Modified:

- `backend/src/main/resources/application.properties`
- `frontend/nginx.conf`
- `.env.example`
- `docker-compose.yml`
- `ops/coolify/compose.yaml`
- `docs/configuration-reference.md`
- `docs/implementation-steps.md`
- `docs/implementation-status.md`
- `README.md`
- `CHANGELOG.md`
- `VERSION`

## Next step

Step 10.2: package the remote MCP endpoint as a ChatGPT plugin, add a minimal skill and run a
deployed end-to-end MCP upload/claim acceptance test.
