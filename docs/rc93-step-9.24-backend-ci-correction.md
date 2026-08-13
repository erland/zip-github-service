# rc.93 — Step 9.24 backend CI correction

GitHub Actions run `31713345069`, backend job `94491707645`, failed during Java compilation because `ProjectApplicationService` referenced `ImmutableImportPlanEntry` without importing the type.

This release candidate adds the missing import only. Step 9.24 behavior and the `selection-2` blocker-decision contract are unchanged. Step 9.25 remains the next implementation step.
