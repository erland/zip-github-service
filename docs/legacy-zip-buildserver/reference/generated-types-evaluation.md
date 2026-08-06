# Generated TypeScript API Types Evaluation

## Decision

Generated TypeScript API types are **deferred** for now.

The project should continue using the existing manual frontend API types in `frontend/src/api/types.ts`, backed by the API contract synchronization checklist in `docs/reference/api-contract-sync.md`.

## Context

The backend already exposes an OpenAPI document through Quarkus SmallRye OpenAPI at:

```text
/q/openapi
```

The frontend currently has a small, hand-written API surface:

- `frontend/src/api/types.ts`
- `frontend/src/api/client.ts`
- `frontend/src/api/sessions.ts`
- `frontend/src/api/packages.ts`
- `frontend/src/api/runs.ts`
- `frontend/src/api/artifacts.ts`

The frontend build tooling does not currently include an OpenAPI TypeScript generation package or script in `frontend/package.json`.

## Options considered

### Option 1 — Adopt generated types now

This would add an OpenAPI-to-TypeScript generator, generate frontend types from the backend OpenAPI document, and replace or wrap the current manual types.

Benefits:

- Reduces manual drift between Java DTOs and TypeScript types.
- Makes backend contract changes visible as generated frontend diffs.
- Can become the long-term source for request and response types.

Costs and risks:

- Requires choosing and maintaining a generator.
- Requires deciding how generated files are produced in CI.
- Requires running or packaging the backend OpenAPI output during frontend generation.
- May generate broad type shapes that are noisier than the current small frontend contract.
- Could introduce churn before the API contract has stabilized further.

### Option 2 — Keep manual types with a strict sync checklist

This keeps `frontend/src/api/types.ts` as the browser frontend contract and uses backend response tests plus documentation to prevent drift.

Benefits:

- Minimal tooling churn.
- Fits the current small API surface.
- Keeps frontend types readable and intentionally scoped to consumed browser APIs.
- Avoids adding a build-time dependency on backend OpenAPI generation.

Costs and risks:

- Requires discipline when backend DTOs change.
- Drift is still possible if API changes bypass the checklist.
- Manual enum and nullability updates can be missed.

### Option 3 — Add runtime validation for critical API responses

This would add a schema validation library at the frontend API boundary for selected responses.

Benefits:

- Catches unexpected backend responses at runtime.
- Can be introduced endpoint-by-endpoint.
- Complements either manual or generated types.

Costs and risks:

- Adds another source of schema definitions unless generated from OpenAPI.
- Increases frontend bundle and maintenance surface.
- Does not by itself eliminate compile-time drift.

## Chosen approach

Use **Option 2** now: keep manual frontend API types with a strict synchronization process.

Generated types should be reconsidered when at least one of these triggers occurs:

- The browser frontend starts consuming many more endpoints.
- API DTOs begin changing frequently.
- A CI job is added that can reliably produce or fetch the backend OpenAPI document.
- The project needs typed API coverage for assistant-only endpoints or additional clients.
- Manual frontend/backend drift causes repeated defects.

Runtime validation should be considered separately for user-visible critical flows if malformed API responses become a practical issue.

## Current source of truth

The backend Java DTOs and mapper/resource tests remain the source of truth for HTTP behavior.

Frontend manual types remain the source of truth for what the browser frontend consumes.

Do not introduce generated types until the repository also defines:

- the generator package,
- the generation command,
- where the OpenAPI input comes from,
- where generated output is committed or produced,
- whether generated files replace or supplement `frontend/src/api/types.ts`,
- CI verification for stale generated output.

## Future adoption proposal

If generated types are adopted later, use a separate implementation step with this scope:

1. Pick a generator and document why it was chosen.
2. Add a deterministic generation command, for example `npm run generate:api-types`.
3. Define the OpenAPI input source, such as a committed `docs/reference/openapi.yaml` or a backend-generated artifact.
4. Generate into an isolated path such as `frontend/src/api/generated/`.
5. Keep handwritten API client helpers separate from generated types.
6. Migrate one endpoint group first, such as runs or sessions.
7. Add CI or local verification that fails when generated output is stale.

Avoid mixing generated and manual DTO names in the same module without a clear boundary.

## Required verification for this decision

This step is documentation-only. No automated test is required.

When future work changes API contracts or generated-type tooling, run:

```bash
cd backend
mvn test

cd ../frontend
npm test
npm run build
```
