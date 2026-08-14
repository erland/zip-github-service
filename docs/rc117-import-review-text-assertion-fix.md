# rc.117 ImportReviewPage text assertion correction

GitHub Actions job `94756486427` failed one frontend test after step 9.34.

The production UI rendered:

`2 vanliga filförändringar är valbara enligt ordinarie regler.`

but React split that sentence across a `<strong>` element and surrounding text nodes. Testing Library's
exact `getByText(string)` matcher therefore did not see the whole sentence as one matching text node.

rc.117 changes only the test: it scopes to the `Vanliga ändringar` section and verifies the paragraph's
combined `textContent`. Production behavior is unchanged.
