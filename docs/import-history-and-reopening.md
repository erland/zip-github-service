# Import history and reopening

Step 6.3 adds an owner-scoped import history to each project.

`GET /api/projects/{projectId}/imports` returns imports newest first together with available upload, plan and pull-request metadata. The response includes a server-derived `resumeStage`:

- `UPLOAD` for imports that have not reached a saved plan,
- `REVIEW` when an immutable plan exists or review/delivery is in progress,
- `RESULT` when pull-request metadata exists.

The project page uses this value to reopen the correct UI route. Upload-stage reopening reuses the existing import id, so retrying an unfinished upload does not create an unrelated import session. All history lookup is filtered by the authenticated owner and project id.

The current implementation uses the temporary in-memory application store. Import history becomes durable when the application services are moved to database repositories.
