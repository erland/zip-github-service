# Step 9.13 report — Repository-first UX and lazy internal Project

Revision: r0118  
Application version: 1.0.0-rc.70  
Date: 2026-08-09

## Result

The user-facing entry model is now repository-first. `Project` remains an internal owner-bound persistence/audit concept, but normal users no longer create or name Projects manually.

## Implemented

- `GET /api/repositories` lists repositories visible through the authenticated user's GitHub App installations and annotates an existing internal `projectId` when one exists. Listing is read-only and never creates Project rows.
- `POST /api/repositories/{installationId}/{repositoryId}/work` verifies the GitHub binding, lazily creates/reuses the internal Project, provisions/reuses Work through the established Work service, and returns both resources.
- Internal Project names are generated from repository identity with collision fallbacks. They are no longer part of the primary UX.
- The start page now shows only repository names, disambiguating duplicate short names with `owner/repo`, and includes case-insensitive client-side filtering on both forms.
- `/projects/new` redirects to the repository list; the previous manual create form is no longer reachable from normal routing.
- A repository without Project gets a minimal landing page and creates the Project only when `Starta arbete` is invoked.
- Existing repositories with Project continue directly into the established Project/Work detail flow.
- Shortcut claim now selects a repository. Promotion reuses an existing Project or performs the same lazy `ensureProject` before entering the ordinary stored-upload import pipeline.
- Primary navigation/detail copy now uses Repository terminology while retaining internal Project APIs for compatibility and audit continuity.

## Security/consistency

- Repository visibility still comes from the authenticated GitHub user's installation catalogue.
- Lazy creation reuses `GitHubProjectConfigurationService.verify`, including installation visibility and default-branch validation.
- Existing owner checks on Project/Work/import resources remain unchanged.
- No Project is created by `GET /api/repositories`.
- Existing Project/Work/import and exact delivery pipelines are reused after bootstrap; no parallel authorization or delivery path was introduced.
