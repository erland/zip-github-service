# Step 9.18 report — deduplicated GitHub Actions/check presentation

Date: 2026-08-09
Revision: r0129
Version: 1.0.0-rc.81

## Goal

Remove the confusing duplicate presentation where a GitHub Actions job appeared once inside its workflow run and again in the generic Checks list, without changing the repository workflow configuration or losing non-duplicate checks.

## Implementation

- `ActionsPanel` keeps workflow runs and jobs as the primary hierarchy.
- For the response commit, displayed workflow job names are normalized by trimming, collapsing whitespace and case-folding.
- A secondary check is suppressed only when its `appName` is `GitHub Actions` and its normalized check name matches a displayed workflow job name for the same commit.
- Checks from other apps and unmatched GitHub Actions checks remain visible under `Övriga kontroller`.
- If every check is already represented by a displayed GitHub Actions workflow job, the secondary section is omitted entirely.
- Aggregate commit state, workflow/job states, diagnostics, artifacts, logs and Actions controls are unchanged.

## Regression coverage

`ActionsPanel.test.tsx` now verifies:

1. a matching GitHub Actions check is not shown twice, while CodeQL and an unmatched GitHub Actions check remain visible;
2. the `Övriga kontroller` section is absent when all checks are duplicates of displayed jobs.

## Quality gate

PASS when a GitHub Actions job is rendered at most once when its workflow job is available, no external/unmatched check disappears, and the secondary section exists only for genuinely additional controls.
