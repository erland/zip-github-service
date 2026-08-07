# MVP RC16 correction

RC16 fixes the import-history link labels regressed by the RC15 work-session UI refactor.

- `RESULT` → **Öppna resultat**
- `REVIEW` → **Fortsätt granska**
- other resumable stages → **Fortsätt import**

The backend `DatabaseMigrationTest` remains guarded by Testcontainers and may be skipped when Docker is unavailable locally. GitHub Actions provides Docker and should execute that test normally.
