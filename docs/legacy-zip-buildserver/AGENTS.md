# AGENTS.md

This repository uses a lightweight ChatGPT delivery workflow.

## Source of truth

The files in this repository override generic assistant behavior.

## Required workflow files

Before implementing a step, read:

1. `docs/delivery-plan.md`
2. `docs/agent-progress.md`
3. `docs/agent-review-checklist.md`

Also read relevant files in `docs/reference/` when they constrain the selected step.

## Step selection

Find the first unchecked step in `docs/agent-progress.md`.

If progress has not been initialized, derive the checklist from `docs/delivery-plan.md`, initialize progress, and implement the first step.

If there are no unchecked steps, stop and report that the delivery plan appears complete.

## Scope rule

Implement exactly one step.

Do not implement future steps.

Do not perform unrelated cleanup.

Do not rewrite large areas unless required by the selected step.

If the selected step requires a small prerequisite, make only that prerequisite change and document it.

## Per-step flow

For every step:

### 1. Architecture pass

Identify:

- affected files or modules
- contracts that must remain stable
- likely tests that should be added or updated
- risks, assumptions, or constraints

### 2. Implementation pass

Make only the scoped code changes required for the selected step.

Rules:

- preserve existing behavior unless the selected step changes it
- follow existing project conventions
- avoid broad rewrites
- avoid unrelated cleanup
- keep changes cohesive and reviewable

### 3. Test pass

Add or update tests where appropriate.

Run verification if possible.

If verification cannot be run, document exact commands to run locally.

### 4. Review pass

Review the resulting changes against:

- `docs/delivery-plan.md`
- `docs/agent-review-checklist.md`
- relevant files in `docs/reference/`
- the selected step

Look specifically for:

- scope creep
- missing tests
- broken existing behavior
- inconsistent naming
- incomplete progress updates

### 5. Documentation pass

Update `docs/agent-progress.md`:

- mark the completed step as done
- summarize changed files
- record verification status
- record known follow-up issues

## Packaging rule

Return an updated zip of the repository.

Do not add an extra top-level wrapper folder unless the uploaded zip already had one.

## Final response

Summarize:

1. completed step
2. changed files
3. tests added or updated
4. verification result
5. known limitations or follow-ups
6. link to updated zip
