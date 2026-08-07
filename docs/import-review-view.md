# Import review view

Version 1.0  
Implemented in step 4.5

## Purpose

The review view presents the already stored immutable import plan. It never recomputes the comparison in the browser and does not allow the user to modify plan entries.

## Route and API

The frontend route is:

```text
/projects/{projectId}/imports/{importId}/review
```

It reads the plan using:

```http
GET /api/imports/{importId}/plan
```

After a ZIP upload, the upload page can prepare the review by calling the snapshot and plan endpoints in sequence and then navigating to the route above.

## Information architecture

The page shows:

- whether the plan is approvable or blocked;
- counts for added, modified, blocked, warning, unchanged and ignored entries;
- the frozen base commit, plan digest, source ZIP hash and policy version;
- filterable file cards with path, status, policy message, sizes and text/binary indication;
- a disabled approval action that clearly belongs to step 5.1.

The default filter is **Changes**, containing added and modified entries. Blocked, warning, unchanged, ignored and all entries can be selected separately.

## Mobile and accessibility

The layout collapses to two summary columns on narrow screens. Filters become a two-column touch-friendly grid, file metadata wraps, and the approval area becomes vertical. Loading and error states use live status or alert semantics, filters expose `aria-pressed`, and the page retains a single descriptive heading.

## Security boundary

The browser only displays the immutable server-side plan. It does not accept client-side edits, does not infer approvability and does not send approval yet. Approval in step 5.1 must submit and verify the exact `planDigestSha256` server-side.
