# Idempotency, retry and failure recovery

Step 5.5 makes the externally visible delivery operations safe to repeat.

## Delivery replay

`POST /api/imports/{id}/delivery` first returns already recorded immutable delivery metadata. It does not require the temporary workspace after a successful push and it never creates a second commit for a completed import.

Transient Git transport failures are reported as `502 GIT_DELIVERY_RETRYABLE`. Permanent identity, base-branch and non-fast-forward conflicts remain `409 GIT_DELIVERY_FAILED` and require a new plan or operator action.

## Pull request replay and ambiguous creation

`POST /api/imports/{id}/pull-request` first returns recorded metadata. If metadata is missing, the service searches GitHub for an existing open pull request with the exact import head branch and base branch before creating one. If the create request fails ambiguously, the same search is repeated before the error is returned. This prevents duplicate pull requests after timeouts.

## Retry boundary

Automatic retry is deliberately not performed in a tight server loop. The client may repeat a retryable request with backoff. Every retry revalidates ownership and immutable import identity.
