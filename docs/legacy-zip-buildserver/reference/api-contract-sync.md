# API Contract Synchronization Strategy

## Purpose

The frontend keeps its API contract in TypeScript while the backend owns the HTTP API response and request DTOs. This document defines how those contracts stay synchronized so frontend types do not silently drift from backend behavior.

## Current strategy

The project currently uses **manual TypeScript API types with backend characterization tests**.

Generated TypeScript types may be evaluated later, but the active source of truth remains the backend Java API DTOs and mappers. When a backend API contract changes, the frontend API types must be updated in the same change unless the endpoint is not consumed by the frontend.

## Contract source of truth

Backend contracts are defined by the API request and response records/classes under:

- `backend/src/main/java/info/isaksson/erland/zipbuildserver/api/session/`
- `backend/src/main/java/info/isaksson/erland/zipbuildserver/api/packageupload/`
- `backend/src/main/java/info/isaksson/erland/zipbuildserver/api/run/`
- `backend/src/main/java/info/isaksson/erland/zipbuildserver/api/artifact/`
- `backend/src/main/java/info/isaksson/erland/zipbuildserver/api/verificationplan/`
- `backend/src/main/java/info/isaksson/erland/zipbuildserver/api/assistant/`

Backend response construction is centralized where possible in:

- `backend/src/main/java/info/isaksson/erland/zipbuildserver/application/mapper/RunResponseMapper.java`
- `backend/src/main/java/info/isaksson/erland/zipbuildserver/application/mapper/AssistantResponseMapper.java`

Frontend API types are defined in:

- `frontend/src/api/types.ts`

Frontend API clients that depend on those types are defined in:

- `frontend/src/api/client.ts`
- `frontend/src/api/sessions.ts`
- `frontend/src/api/packages.ts`
- `frontend/src/api/runs.ts`
- `frontend/src/api/artifacts.ts`

## When frontend types must be updated

Update `frontend/src/api/types.ts` in the same change whenever any backend DTO or mapper change:

- adds a response field,
- removes a response field,
- renames a request or response field,
- changes nullability,
- changes an enum/string literal value,
- changes a numeric field's meaning or unit,
- changes request body shape,
- changes list wrapper shape,
- changes error response shape consumed by the frontend.

If a backend field is added but the frontend does not display it yet, still add it to the TypeScript type so the frontend contract mirrors the backend response.

If a backend endpoint is assistant-only and is not consumed by the browser frontend, do not add it to `frontend/src/api/types.ts` unless the frontend starts consuming it.

## Required review checklist for API changes

For every backend API contract change, review these files:

1. Backend DTO in `backend/src/main/java/info/isaksson/erland/zipbuildserver/api/**`.
2. Backend mapper or resource that constructs the response.
3. Backend resource tests that characterize response shape.
4. Frontend type in `frontend/src/api/types.ts`.
5. Frontend API client in `frontend/src/api/*.ts`.
6. Frontend pages or components that consume the changed type.

## Testing expectations

Backend tests should pin the response shape for browser-facing and assistant-facing APIs. Relevant tests live under:

- `backend/src/test/java/info/isaksson/erland/zipbuildserver/api/session/`
- `backend/src/test/java/info/isaksson/erland/zipbuildserver/api/run/`
- `backend/src/test/java/info/isaksson/erland/zipbuildserver/api/assistant/`

Frontend tests should cover pages or components that depend on changed fields when behavior changes.

## Verification commands

Run backend verification after changing Java API DTOs, resources, or mappers:

```bash
cd backend
mvn test
```

Run frontend verification after changing TypeScript API types or frontend API clients:

```bash
cd frontend
npm test
npm run build
```

Run both sets when a change crosses the backend/frontend boundary.

## Generated-type evaluation

Generated TypeScript API types were evaluated in `docs/reference/generated-types-evaluation.md`.

The current decision is to defer generated types and continue using manual frontend API types with the synchronization checklist in this document. Do not partially introduce generated API types until a future step defines the generator, OpenAPI input source, generated output location, migration boundary, and verification process. Mixing generated and manual types without a clear boundary increases drift risk instead of reducing it.
