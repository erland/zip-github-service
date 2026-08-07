# MVP RC5 correction

Revision: `r0045`  
Version: `1.0.0-rc.5`

## Problem

GitHub Actions could not resolve `../data/demoProjects` from `ProjectListPage.tsx` even though `frontend/src/data/demoProjects.ts` existed in local ZIP packages.

## Root cause

The root `.gitignore` contained `data/`. Git ignore patterns without a leading slash match directories with that name at any depth, so `frontend/src/data/` was ignored and the demo source file was never committed to GitHub. Local builds succeeded because the file remained on disk.

## Correction

Runtime storage rules are now repository-root anchored: `/data/`, `/uploads/`, `/workspaces/`, `/tmp/`, and `/temp/`. This preserves runtime ignore behavior while allowing source directories such as `frontend/src/data/` to be tracked.

A new `scripts/verify-source-tracking.sh` CI guard verifies that required source files exist and, when executed in a Git checkout, are not ignored.

No implementation step status changed; `8.1` remains the only `NEXT` step.
