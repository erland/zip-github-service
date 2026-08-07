# Import result page

Step 6.1 adds a result route at `/projects/{projectId}/imports/{importId}/result`.

The page reads the immutable pull-request result metadata already stored by the backend through `GET /api/imports/{importId}/pull-request`. It does not depend on a live GitHub status lookup to render its primary navigation.

The page exposes direct links to:

- the repository;
- the imported branch;
- the exact commit;
- the draft pull request;
- the commit checks page;
- GitHub Actions filtered for the import branch;
- the target branch.

The review page now continues the approved flow by preparing the workspace, delivering the branch and creating or reusing the pull request before navigating to the result page. These POST operations are idempotent according to step 5.5.

Actual combined check status is intentionally deferred to step 6.2. The direct checks and Actions links remain usable even when that integration is unavailable.
