# MVP release candidate 1.0.0-rc.3

## Release decision

Revision `r0043` is the third corrected MVP release candidate. Revisions `r0041` and `r0042` exposed compile/test regressions during full local verification; the remaining policy, Git-tree fixture and asynchronous route-focus regressions are corrected in `r0043`. The implementation scope through phase 7 is complete in the repository. The candidate is suitable for deployment to a controlled test environment and live acceptance testing.

It is not yet declared production-ready because several acceptance tests require external infrastructure, real GitHub credentials, Docker/PostgreSQL and physical browser/device testing.

## User-visible capability

A signed-in user can:

1. select an authorized GitHub repository and branch;
2. upload a ZIP from desktop or mobile;
3. receive safe archive validation and normalization;
4. compare the ZIP with an exact GitHub commit;
5. review added, modified, unchanged, ignored and blocked files;
6. approve the exact immutable plan digest;
7. create an isolated workspace and deliver one branch and one commit;
8. create or reuse a draft pull request;
9. open permanent repository, branch, commit, PR, Checks and Actions links;
10. view basic check status and reopen previous imports.

## Definition of Done assessment

### Met in the repository

- Functional MVP flow is implemented end-to-end.
- Authorization checks are user scoped.
- ZIP processing is non-executing, bounded and traversal-safe.
- Review and delivery are cryptographically tied to the uploaded archive and frozen base commit.
- Delivery is non-force, atomic and idempotent.
- Web/API hardening and security regressions are present.
- CI, deployment, operations, backup/restore and incident documentation exist.
- Mobile and accessibility implementation baselines exist.

### Pending external evidence

- Green CI for the final release commit.
- Live GitHub E2E run.
- Docker Compose and PostgreSQL restore drill.
- Real iPhone/Safari/VoiceOver acceptance.

The authoritative list is `release-checklist.md`.

## Excluded from MVP

Phase 8 remains post-MVP:

- detailed workflow and job views;
- artifact links and condensed build errors;
- controlled workflow dispatch and reruns;
- AI-facing status/export integration.
