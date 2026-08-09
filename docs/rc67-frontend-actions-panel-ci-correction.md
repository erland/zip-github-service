# rc.67 frontend ActionsPanel CI correction

Revision: `r0115`  
Version: `1.0.0-rc.67`

## Observed failure

GitHub Actions run `31270676551`, frontend job `93135974997`, executed 49 Vitest tests. 46 passed and three ImportResultPage tests failed because the shared `ActionsPanel` threw `TypeError: Cannot read properties of undefined (reading 'slice')` at the workflow SHA rendering path. The subsequent missing UI assertions were consequences of the component crash.

## Root cause

Step 9.11 made Work and result/commit views share `ActionsPanel`. The production API includes workflow `headSha`, but older result-view test fixtures and a possible rolling-upgrade/partial response may omit it. The component called `workflow.headSha.slice(...)` unconditionally.

## Correction

- Render workflow SHA from `workflow.headSha`, falling back to the Actions response commit SHA and then the panel commit SHA.
- Treat missing workflow/check/job arrays as empty collections so a partial response cannot crash the whole result page.
- Add a component regression that deliberately omits workflow `headSha` and verifies the workflow still renders against the current commit.

No backend behavior or phase/step state changed.
