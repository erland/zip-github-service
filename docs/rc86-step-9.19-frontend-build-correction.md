# rc.86 — Step 9.19 frontend build correction

## Trigger

GitHub Actions run `31460628980`, job `93683258922`, passed all 58 frontend runtime tests but failed `tsc -b` in `ImportReviewPage.test.tsx`.

## Root cause

The rc.85 plan itself was typed as `ImportPlanResponse`, but object literals appended via `entries: [...plan.entries, {...}]` were inferred before assignment. TypeScript therefore widened fields such as `status`, `severity` and `blockerType` to `string`, which is not assignable to the literal unions in `ImportPlanEntry`.

## Correction

- Import `ImportPlanEntry` in the review test.
- Add a small typed `planEntry()` helper.
- Route the newly appended bulk/deletion/hard-blocker fixtures through that helper.

No production source file changes.

## Verification

- `tsc -b --pretty false` passes locally after the correction.
- GitHub Actions rc.85 already demonstrated 58/58 Vitest tests passing before the build step.
- Project release/security/source-tracking gates are run for rc.86 packaging.
