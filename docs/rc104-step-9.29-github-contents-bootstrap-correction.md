# rc.104 — Step 9.29 GitHub Contents bootstrap correction

Real repository verification against `erland/repo-fleet` showed `default_branch=main`, repository size 0 and no branches. GitHub documents that raw Git Database/reference operations are unavailable for empty repositories and recommends initializing them through the repository Contents API.

rc.104 therefore replaces the production empty-repository smart-HTTP push with a serial Contents API bootstrap: create `.zip-github-bootstrap`, delete it immediately on `main`, verify the branch, then continue ordinary Work provisioning. The current repository tree is empty before user ZIP content is reviewed or committed.

The repository-first `Starta arbete` endpoint also catches unexpected runtime failures and maps them to `REPOSITORY_WORK_START_FAILED`, so future failures are diagnosable rather than appearing only as generic `INTERNAL_ERROR`.
