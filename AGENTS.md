# AGENTS.md — zip-github delivery workflow

## Source of truth

Before implementing any step, read in this order:

1. `docs/implementation-status.md`
2. `docs/implementation-steps.md`
3. `docs/zip-github-development-plan-v1.1.md`
4. `docs/zip-github-functional-specification-v1.0.md`
5. relevant completed step reports and legacy references

## Selecting the next step

When the user asks to **run the next step**:

1. Find the single row marked `NEXT` in `docs/implementation-status.md`.
2. Verify that all prerequisite steps are `DONE`.
3. Implement exactly that step; do not implement later steps.
4. Run the verification required by the step when the environment permits it.
5. Update the status register and add or update the step report.
6. Mark the completed row `DONE` and the next pending row `NEXT`.
7. Package the whole repository as a new revision-numbered ZIP.

If there is no `NEXT` row, select the first `PENDING` row whose prerequisites are complete and mark it `NEXT` before starting. If no pending rows remain, report that the implementation plan is complete.

## Allowed statuses

- `PENDING` — not started.
- `NEXT` — the only step selected for the next prompt.
- `IN_PROGRESS` — currently being implemented; should not remain in a delivered ZIP.
- `DONE` — implemented and documented.
- `BLOCKED` — cannot proceed; reason recorded.
- `SKIPPED` — intentionally omitted; reason recorded.

A delivered ZIP must contain exactly one `NEXT` step unless all steps are complete or the next step is blocked.

## Scope and verification

- Implement one step per prompt.
- Avoid unrelated cleanup and future-step implementation.
- Record changed files, tests, commands, outcomes, limitations and next step.
- In every delivered ZIP, include an explicit list of all files added, modified, moved, or deleted by the completed step. Put the list in the step report and summarize it in the final response.
- Never mark a step `DONE` merely because documentation was written when the step requires working code or successful verification.
- Preserve multi-user isolation: every user-owned resource must be authorization-checked server-side.

## Packaging

Return a ZIP with one top-level folder named `zip-github/`.
