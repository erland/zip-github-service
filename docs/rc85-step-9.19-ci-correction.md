# rc.85 — Step 9.19 CI correction

## Scope

This release corrects CI fixtures/type inference discovered by GitHub Actions after rc.84. It does not change production behavior.

## Backend

The complete-ZIP model introduced in step 9.19 means a repository `.gitignore` that is absent from the uploaded ZIP is deleted and must not continue affecting new-file classification. Two older tests expected repository ignore rules to remain active while their archive fixtures omitted the relevant `.gitignore` files. The fixtures now include both the ignore files and their prospective contents, matching the behavior they intend to test.

## Frontend

The bulk-override test adds `WOULD_DELETE` entries. Deletion entries correctly have `archiveSizeBytes` and `archiveSha256` set to `null`. The shared test plan is now explicitly typed as `ImportPlanResponse`, matching the API contract instead of an overly narrow object-literal inference.
