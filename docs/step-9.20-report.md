# Step 9.20 report — simplified commit-derived PR metadata

Date: 11 August 2026
Repository revision: r0135
Application version: 1.0.0-rc.87

## Result

`Fyll från commitmeddelanden` continues to use only trustworthy current-Work GitHub commit history and chronological ordering. The generated PR description is now the Markdown bullet list itself; the previous `## Ingående commits` heading is no longer emitted.

When the PR title is blank, the helper takes the first line of the first commit in that same chronological list and uses it as the editable title, bounded by the existing 256-character title limit. A title already entered by the user is preserved exactly.

No backend, GitHub attribution, lifecycle, validation or PR-create contract changed.

## Regression coverage

- focused composer regression: blank title gets the first chronological commit subject and description contains only the list;
- focused composer regression: an existing title is not overwritten;
- existing post-commit flow regression: manually entered title remains unchanged and the submitted description contains no generated heading.
