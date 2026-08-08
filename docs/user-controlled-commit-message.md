# User-controlled commit message

Implemented in r0097 / `1.0.0-rc.49` as phase 9 step 9.5.

## Behavior

The ordinary import review page, used by both browser uploads and promoted StagingImports, shows the previous generated message `Apply approved ZIP import <importId>` as an editable suggestion. The user may replace it completely before approval. The final confirmation shows the commit message, locked base ref and selected-file count.

Interactive approval validates the message server-side: line endings become LF, surrounding whitespace is stripped, empty messages are rejected, unsupported ASCII control characters are rejected and the normalized value is limited to 500 characters.

## Approval and restart safety

The normalized message is part of `ImportPlanApproval` and is persisted in `approval_json`. It is therefore restored together with the immutable plan and selection after refresh, logout/login or backend restart. Approval idempotency compares plan digest, selection digest, owner **and commit message**. A different message cannot silently reuse an existing approval.

Delivery receives the message only from the approval-bound state and passes it as one Git argument to `git commit -m`; no shell interpolation is used. Retry uses the same persisted approval and existing delivery idempotency, so the message is not regenerated and the same recorded delivery cannot create a second commit.

## Compatibility

Old persisted approvals and internal callers that predate this field may lack `commitMessage`. Hydration then uses the exact former deterministic value `Apply approved ZIP import <importId>`. This fallback exists only for compatibility; every new interactive approval must submit a message explicitly.
