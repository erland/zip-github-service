# Step 9.9 report — revisitable GitHub Actions status on Work

## Scope

Step 9.9 makes the existing phase-8 commit-scoped GitHub Actions integration available from the active Work section on the project page after navigation, refresh or a later login. It does not add a background monitor or new GitHub authorization model.

## Backend

- Added `GET /api/projects/{projectId}/work/actions`.
- Added `GET /api/projects/{projectId}/work/actions/details`.
- Both endpoints require the authenticated owner and an active Work with at least one delivered import.
- The endpoints derive `importId` from `WorkSession.lastImportId` and the target SHA from `WorkSession.headCommitSha`, then reuse `ImportActionsStatusService` and `ImportActionsDetailsService`.
- GitHub queries therefore remain exact-commit scoped. A workflow for an older commit on the same branch cannot become the displayed current Work status.
- Existing phase-8 failure extraction, log bounding and credential/token redaction remain the source of condensed diagnostics.

## Frontend

The active Work card now contains a GitHub Actions section when `headCommitSha` exists. It shows:

- exact Work commit SHA,
- aggregate state (`pending` while any exact-commit item is active),
- latest commit-matching workflow runs and jobs with distinct `queued` / `in_progress` states,
- links to GitHub Actions/run/job pages,
- explicit **Uppdatera status**,
- bounded 10-second polling only while `queued` or `in_progress`,
- condensed failures from the existing details endpoint,
- **Kopiera fel** with repository, branch, exact commit, workflow, job/step/tool and the already-sanitized log excerpt.

The copied diagnostic is capped at 24,000 characters and each failure contributes at most 80 already-filtered lines.

## Regression coverage

`ProjectDetailPage.test.tsx` now covers revisiting a failed Work run and copying commit-correct condensed failure information. Backend route identity is guarded by using `activeWorkSource()` and Work's persisted `lastImportId`/`headCommitSha` rather than a branch-only lookup.

## Security

No anonymous Actions access was introduced. No raw GitHub token, installation token or unfiltered full job log is returned or copied. Existing owner checks and GitHub installation token creation remain server-side.

## Result

Step 9.9 is DONE. Step 9.10 is NEXT.
