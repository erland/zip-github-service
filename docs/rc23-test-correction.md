# RC23 test correction

Revision: `r0064`  
Version: `1.0.0-rc.23`

RC23 is a test-only correction after the flexible-review regression package in RC22.

## Backend

`ImportSelectionResourceTest` used `contains(...)` for JSON-array assertions. In the local RestAssured/Hamcrest dependency combination this resolved incompatibly and was passed to RestAssured as a string argument. The assertion now compares the returned JSON list with `equalTo(List.of(...))`, which is explicit and type-safe for this response shape.

## Frontend

The hierarchical review tree intentionally renders only the basename in each file row because ancestor directories are rendered separately. The full repository path is retained as the file node `title` and in checkbox accessible names. Tests now assert complete paths through `title` instead of expecting the complete path as one visible text node.

No runtime behavior is changed. Next implementation step remains **8.1 – Workflow runs och jobs**.
