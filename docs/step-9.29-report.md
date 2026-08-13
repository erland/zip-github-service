# Step 9.29 report — support for completely empty repositories

## Problem

A newly created GitHub repository can have a configured default-branch name while still having no branch ref and no commit. rc.100 required that the configured branch already exist during project verification and later required a base commit SHA when provisioning Work. The first ZIP therefore could not be uploaded to a completely empty repository.

## Solution

The repository configuration gate now distinguishes a truly empty repository from a non-empty repository with a missing/wrong branch. The missing configured default branch is accepted only if GitHub reports zero branches.

When first Work provisioning observes that the configured default branch cannot resolve and branch enumeration is still empty, zip-GitHub initializes the repository through GitHub's Contents API: it creates a temporary `.zip-github-bootstrap` marker using the repository default branch and deletes that marker immediately on the configured default branch. The resulting current default-branch tree is empty before the ordinary `zip-github/work-*` branch is created. No bootstrap file remains in the current repository contents.

This deliberately keeps the rest of the model unchanged: repository snapshot has a real locked base SHA, the complete ZIP is compared against an empty tree, selection/approval remains exact, delivery creates one child commit from the locked base, and PR creation has a real base branch.

## Safety

- Merely listing/selecting the repository does not mutate GitHub.
- Bootstrap occurs only on explicit Work/import start.
- Installation-scoped branch enumeration must still be empty immediately before bootstrap.
- A non-empty repository with a missing configured branch remains an error.
- Concurrent initialization is accepted only when the configured default branch becomes resolvable; other bootstrap failures remain failures.
- Existing non-force Work push and exact parent checks remain in force.

## Regression coverage

- project configuration accepts missing default branch only for zero-branch repositories;
- Work lifecycle bootstraps before creating the Work branch;
- a focused HTTP regression verifies the required serial Contents API `PUT` then `DELETE` bootstrap contract, including use of the configured default branch for cleanup.


## Runtime correction r0150 / 1.0.0-rc.102

Production use exposed an additional empty-repository state: GitHub may provide no usable `default_branch` value before the first commit. rc.101 could therefore attempt to persist a blank default branch, violating the PostgreSQL `ck_project_default_branch_not_blank` constraint and surfacing as the generic unexpected-error response.

The correction normalizes missing/blank/JSON-null default-branch metadata. When and only when GitHub confirms that the repository has no branches, zip-GitHub resolves the bootstrap branch to `main` before project persistence. An initialized repository with missing default-branch metadata continues to fail closed rather than being repaired automatically.


## Runtime correction r0151 / rc.103

A production test showed that `Starta arbete` could still fail before Work provisioning for a completely empty repository. The repository-start path now initializes the remote first, re-verifies the resulting branch, and only then persists the internal project. This removes the temporary uninitialized-repository state from PostgreSQL-facing project creation. Bootstrap/state failures are converted to explicit API problem codes (`GITHUB_BRANCH_STATE_UNAVAILABLE`, `EMPTY_REPOSITORY_BOOTSTRAP_*`) rather than the generic unexpected-error mapper.


## rc.104 production correction

Real-world verification against the still-empty `erland/repo-fleet` repository showed that the prior local smart-HTTP push bootstrap did not initialize the repository. rc.104 follows GitHub's documented empty-repository path: create `.zip-github-bootstrap` through the Contents API, delete it serially on the configured default branch, verify the resulting branch, then continue with ordinary Work provisioning. The repository-start endpoint also maps unexpected runtime failures to `REPOSITORY_WORK_START_FAILED` rather than the generic internal-error mapper.
