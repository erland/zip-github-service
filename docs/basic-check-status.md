# Basic commit check status

Step 6.2 adds a bounded, read-only GitHub check-status integration for the delivered commit.

## API

`GET /api/imports/{importId}/checks` verifies ownership, requires a completed Git delivery, creates a short-lived installation token and reads GitHub check runs for the immutable delivered commit SHA.

The response exposes one normalized state: `pending`, `success`, `failure`, `cancelled` or `unavailable`, plus counters, terminal status, a permanent GitHub checks URL and the observation timestamp.

## Aggregation

- queued or in-progress check runs produce `pending`;
- completed failure-like conclusions produce `failure`;
- cancelled conclusions produce `cancelled` when no failure is present;
- success, neutral and skipped conclusions produce `success` when all runs are terminal;
- API failures produce `unavailable` without hiding the permanent GitHub link.

An empty check-run list is treated as `pending`, because workflows may not have been registered yet.

## Polling

The result page polls immediately and then at most every ten seconds, for no more than twelve observations. Polling stops as soon as a terminal state is returned. The browser never receives an installation token.
