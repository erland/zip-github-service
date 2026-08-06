# GitHub technical spike — step 2.1

Date: 6 August 2026

## Goal

Verify the smallest GitHub delivery chain needed by zip-github before the production GitHub App integration is implemented:

1. identify the authenticated GitHub user;
2. read repository metadata and permissions;
3. resolve the target branch to an immutable commit SHA;
4. create an isolated branch from that SHA;
5. create one commit on the branch;
6. open a draft pull request against the target branch;
7. query commit/check status.

## Executed test

Repository: `erland/got-test-repo` (private)

| Item | Result |
|---|---|
| Authenticated user | `erland` |
| Repository permissions | admin, maintain, pull, push and triage |
| Target branch | `main` |
| Frozen base SHA | `bf0058cc0871daa556c6b65292096b0e03efbd94` |
| Spike branch | `zip-github/spike-20260806-1506` |
| Created file | `zip-github-spike/2026-08-06.md` |
| Commit SHA | `dd1fa5d2c06da887ce2e1e34ef6d8381a51c598c` |
| Pull request | `erland/got-test-repo#2` |
| Pull request mode | draft, open, not merged |
| Changed files | 1 |
| Commit statuses returned | 0 |

The default branch was not written to directly. The spike created exactly one commit on a separate branch and one draft pull request.

## Conclusions

- The required GitHub object flow is viable: base ref -> branch -> commit -> pull request -> status lookup.
- The delivery implementation should freeze the base SHA before generating the import plan.
- The branch and pull request operations should be idempotent and linked to one import-session identifier.
- An empty status result is a valid `unavailable/no checks configured` state and must not make a successful Git delivery appear failed.
- The production service must use short-lived GitHub App installation tokens server-side. The connector identity used for this spike proves the API flow but is not the service authentication design.

## Recommended GitHub App permissions for MVP

- Metadata: read
- Contents: read/write
- Pull requests: read/write
- Checks: read, or Actions: read when workflow-run presentation is implemented
- Workflows: no write permission

The app should be installed only for explicitly selected repositories. Login identity and repository automation must remain separate: the user session identifies the actor, while the installation token authorizes operations for one installation and repository scope.

## Production implementation boundary

This spike does not implement OAuth, GitHub App JWT creation, installation-token caching, webhook handling or repository selection in the application. Those belong to steps 2.2 and 2.3.

## Cleanup

The draft pull request and branch are intentionally left visible as auditable spike evidence. They must not be merged automatically. They may be closed/deleted manually after the project no longer needs the evidence.
