# Security Model

`zip-buildserver` verifies untrusted source-code zip packages. The security model assumes uploaded packages may be malicious and that build/test scripts may attempt to access secrets, exfiltrate data, consume resources, or escape their workspace.

## Trust boundaries

### Trusted components

- Service operator configuration.
- Server-side verification plan files.
- Backend application code.
- Database and configured storage locations.
- Static API token configuration.

### Untrusted inputs

- Uploaded zip packages.
- Package file names and paths.
- Build files such as `pom.xml` and `package.json`.
- Package documentation such as `README.md` and `AGENTS.md`.
- Assistant-provided labels, metadata, requested plan IDs, and package references.

Uploaded package content is descriptive input only. It must not define commands for the service to execute.

## Archive validation

Package upload validation is required before storage or extraction.

The service validates:

- Zip format.
- Maximum compressed size.
- Maximum extracted size.
- Maximum file count.
- Relative and safe paths.
- Rejection of absolute paths and path traversal.
- Rejection of malformed or unsupported entries according to policy.

Unsafe archives are rejected with controlled error responses that do not expose host filesystem details.

## Command safety

Verification commands are selected only from server-side verification plans under:

```text
backend/src/main/resources/verification-plans/
```

The service does not execute arbitrary user-supplied shell commands and does not treat uploaded `README.md`, `AGENTS.md`, or other documentation as command policy.

## Worker isolation

Real verification uses an ephemeral Docker worker container per command/run flow. The backend prepares a workspace, launches the configured worker image, captures command output, then cleans up according to retention policy.

Worker containers should:

- Run without service secrets.
- Use a non-root worker user where practical.
- Receive only the run workspace mount.
- Enforce configured CPU, memory, timeout, and output limits.
- Avoid host filesystem access outside assigned workspaces.
- Be removed after execution.

## Docker socket risk

The MVP can orchestrate worker containers through Docker. Access to Docker control is powerful and can be equivalent to broad host control.

Recommended deployment posture:

1. Run the service on a dedicated host or VM.
2. Do not co-host unrelated sensitive workloads.
3. Use a strong API token.
4. Restrict network access to the service.
5. Avoid mounting secrets into the backend or worker containers.
6. Consider rootless Docker, Podman, Kubernetes Jobs, a separate worker host, or microVMs for stronger isolation in future deployments.

## Authentication

The MVP uses static bearer-token authentication.

Protected API requests must include:

```text
Authorization: Bearer <ZIP_BUILDSERVER_API_TOKEN>
```

Public endpoints are limited to operational metadata such as health and OpenAPI inspection. Do not disable authentication on a network-exposed deployment.

## Network policy

Verification plans include a declared network mode. Supported modes are:

- `none`
- `dependency`
- `full`

For the MVP, `dependency` may still mean ordinary outbound access. Operators should document this clearly and tighten it later with registry allowlists, proxies, or worker-network controls.

## Secrets

Worker containers must not receive:

- Service API tokens.
- Database credentials.
- Host credentials.
- Cloud credentials.
- Administrative tokens.

If private dependency registries are added later, credentials must be scoped, isolated, and never exposed in logs or summaries.

## Logs and artifacts

Build and test logs can contain sensitive data accidentally printed by package code.

The service therefore:

- Returns concise excerpts by default.
- Stores full logs as artifacts.
- Uses opaque artifact identifiers.
- Applies retention cleanup.
- Requires authentication for artifact access.

Operators should treat logs and uploaded packages as confidential user content.

## Retention

Default retention is configurable:

```text
uploaded packages: 7 days
logs/artifacts: 14 days
session metadata: 90 days
workspaces: removed after run or cleanup grace period
```

Retention cleanup removes expired data according to `docs/operations.md`.

## Out-of-scope hardening for the MVP

The MVP does not include:

- Multi-user RBAC.
- Per-user quotas.
- Private dependency registry isolation.
- Kubernetes or microVM execution.
- Advanced malware analysis.
- Formal sandbox escape guarantees.

Use the MVP as a trusted self-hosted development tool, not as an internet-scale untrusted code execution platform.
