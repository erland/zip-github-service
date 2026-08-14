# rc.115 SimplifiedImportFlow test fixture correction

GitHub Actions job `94737697334` failed the end-to-end simplified import flow test.

Step 9.33 added `startProjectWork()` to `NewImportPage`. The focused `NewImportPage.test.tsx`
fixture was updated, but `SimplifiedImportFlow.test.tsx` maintains an independent mock of
`../api/projects` and did not expose `startProjectWork`.

Vitest therefore raised:

`No "startProjectWork" export is defined on the "../api/projects" mock.`

rc.115 updates only that E2E fixture, returns a representative ACTIVE Work from the automatic
start, and asserts that Work start happens before import creation. Production code is unchanged.
