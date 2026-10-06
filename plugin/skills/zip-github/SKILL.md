---
name: zip-github
description: Use zip-GitHub when the user wants to transfer a project ZIP into the existing zip-GitHub review and GitHub delivery workflow.
---

# zip-GitHub workflow

Use zip-GitHub only as a transport handoff into the existing browser review flow.

When the user wants to send a ZIP project to zip-GitHub:

1. Use `stage_zip` with the user-provided ZIP file.
2. On success, give the user the returned `review_url`.
3. Explain briefly that repository selection, review, approval, commit/push and pull-request handling continue in the normal zip-GitHub web interface.

Do not claim that staging changed GitHub. The MCP tool only creates a temporary staging upload.

Do not look for repository-selection, approval, Git-write or pull-request tools in this plugin. Those actions are intentionally outside the MCP surface.

If `stage_zip` fails because the file is not a ZIP, ask for a ZIP archive. If staging capacity or temporary-file retrieval fails, report the returned error without attempting an alternate GitHub write path.
