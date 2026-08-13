# rc.94 — Step 9.24 backend test CI correction

GitHub Actions run `31717342660`, backend job `94505281489`, passed production-source compilation but failed during test compilation because two `AlternativeZipIngestionRegressionTest` calls still used the pre-9.24 `ImportSelectionFactory.create(...)` signature.

This release candidate adds the required empty blocker-decision list to those two regression-test calls. Step 9.24 behavior and the `selection-2` blocker-decision contract are unchanged. Step 9.25 remains the next implementation step.
