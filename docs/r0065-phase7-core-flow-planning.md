# r0065 — Phase 7 core-flow planning refinement

## Decision

Before phase 8, complete phase 7 with steps 7.11–7.18. The work has two goals: make ZIP ingestion reusable for future staging/iOS Shortcut flows, and remove unnecessary user actions from the normal web import flow without weakening immutable-selection or approval guarantees.

## Added steps

- 7.11 reusable ZIP ingestion/storage
- 7.12 normal Import from an already stored ZIP
- 7.13 import source/audit metadata
- 7.14 alternative-ingestion regression
- 7.15 unchanged protected-path policy correction
- 7.16 automatic upload-to-review orchestration
- 7.17 approval-to-commit as one user action
- 7.18 streamlined-flow E2E regression

## UX decisions

Normal happy path becomes:

```text
Välj ZIP
→ automatisk bearbetning/plan
→ granska och justera selection/overrides
→ Godkänn valda förändringar
→ automatisk workspace/commit/push
→ resultat
```

Internal stages remain separate and auditable. Automation removes redundant clicks; it does not remove the requirement that exact selection and overrides are persisted and approved before any GitHub write.

## Protected unchanged files

A path-based change blocker such as `.github/**` applies only when the import would alter repository state (`ADDED`, `MODIFIED`, `WOULD_DELETE`). `UNCHANGED` entries do not require override because there is no write to approve. Archive-level safety checks remain independent of diff status.

## Product version

No runtime behavior is changed by r0065. `VERSION` therefore remains `1.0.0-rc.23`.
