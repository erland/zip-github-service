# rc.88 — Step 9.20 frontend CI correction

## Problem

GitHub Actions showed 59 of 60 frontend tests passing. The only failure was the new `PullRequestComposer` regression test using `findByDisplayValue(/First commit title/)`. After Step 9.20, that text correctly appears in both the title input and the description textarea, so Testing Library reported multiple matches.

## Correction

The test now selects the description with `findByLabelText('Beskrivning')` and the title with `getByLabelText('Titel')`, then verifies each exact value independently.

## Scope

No production code or behavior changed. Step 9.20 remains complete.
