# rc.71 frontend repository-list CI correction

## Observed CI failure

GitHub Actions run `31297540060`, frontend job `93204995587`, executed 51 frontend tests. 50 passed and one failed in `src/App.test.tsx`.

The failing regression asserted that the `example-book-project` repository link existed immediately after the page heading rendered. The repository list is loaded asynchronously from `/api/repositories`, so the heading can render while the page still displays `Hämtar repositories…`.

## Correction

The test now awaits the repository link with `findByRole` before entering text in the repository search field. This preserves the intended 9.13 contract while removing the race between page-shell rendering and repository-data loading.

## Scope

No production frontend or backend source changed. Step 9.13 remains complete.
