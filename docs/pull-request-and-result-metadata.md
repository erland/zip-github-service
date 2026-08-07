# Pull request and result metadata

Step 5.4 creates a draft pull request only after the approved branch and commit have been pushed. The GitHub App installation token is short-lived and never returned. The PR body records the immutable plan digest, base commit and delivered commit for auditability.

Recorded result metadata is immutable per import: repository, base branch, import branch, commit SHA, plan digest, PR number, URL, draft flag, state and creation time. Re-reading metadata does not call GitHub.
