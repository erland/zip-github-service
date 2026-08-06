# Step 1.4 report — frontend shell and routing

Date: 6 August 2026  
Revision: r0009

## Result

Implemented a mobile-friendly React routing shell for the first user-facing workflow:

- project list at `/projects`
- project details at `/projects/:projectId`
- new import shell at `/projects/:projectId/imports/new`
- product information at `/about`
- fallback not-found route
- root redirect to `/projects`

The views use explicit demo data until authenticated API integration is introduced. No uploaded file is sent or processed by the import shell in this step.

## Verification

Passed:

- route and component source inspection
- TypeScript/JSX structure inspection
- responsive CSS inspection
- existing project structure verification
- exactly one `NEXT` step in the implementation ledger

Not executable in this environment:

- `npm ci`, tests and production build, because the internal npm registry returns HTTP 404 for `yallist@3.1.1`

## Changed files

### Added

- `frontend/src/components/AppLayout.tsx`
- `frontend/src/data/demoProjects.ts`
- `frontend/src/pages/ProjectListPage.tsx`
- `frontend/src/pages/ProjectDetailPage.tsx`
- `frontend/src/pages/NewImportPage.tsx`
- `frontend/src/pages/NotFoundPage.tsx`
- `docs/step-1.4-report.md`

### Modified

- `frontend/src/App.tsx`
- `frontend/src/App.test.tsx`
- `frontend/src/pages/AboutPage.tsx`
- `frontend/src/styles/global.css`
- `frontend/README.md`
- `docs/implementation-status.md`

### Moved

None.

### Deleted

None.
