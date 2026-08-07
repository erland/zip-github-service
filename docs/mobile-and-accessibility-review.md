# Mobile and accessibility review

Step 7.1 establishes the MVP interaction baseline for small screens and keyboard/screen-reader use.

## Implemented baseline

- A keyboard-visible skip link targets the main content landmark.
- Route changes move focus to the destination page's `h1`, without scrolling the page unexpectedly.
- The active import step uses `aria-current="step"`.
- Form help is connected to branch and ZIP controls with `aria-describedby`.
- Upload progress has an accessible name and status/error messages use live-region semantics.
- Interactive controls have a minimum 44 px touch target.
- `:focus-visible` provides a high-contrast focus indicator.
- Layouts collapse at 760 px and receive an additional compact treatment below 480 px.
- Long repository names, paths and hashes wrap rather than causing horizontal page scrolling.
- Reduced-motion and Windows forced-colors preferences are respected.

## Reviewed flows

1. Project list and project detail.
2. New or reopened ZIP upload.
3. Immutable-plan review and filters.
4. Approval and delivery controls.
5. Result page, checks and external GitHub links.
6. Import history and stage-aware reopening.

## Manual acceptance checklist

The following remains a device/browser acceptance activity and cannot be fully proven by jsdom or static CSS inspection:

- iPhone Safari portrait at 320–430 CSS px.
- iPhone Safari landscape.
- 200% browser zoom without horizontal page scrolling.
- Keyboard-only traversal in Safari/Chrome/Firefox.
- VoiceOver announcements for route changes, upload status, errors and check status.
- External GitHub links open without losing the service state.

No attempt is made to claim WCAG conformance from automated checks alone. Step 7.1 supplies the implementation baseline and explicit manual regression checklist.
