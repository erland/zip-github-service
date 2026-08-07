# MVP RC15 correction — persistent work branches

RC15 changes the delivery model from one branch/PR per import to one active work session per project.

## Behaviour

1. Starting the first ZIP import creates an active work session from the project default branch.
2. The first approved import creates the work branch and one commit.
3. Each later ZIP is compared to the latest HEAD of that same work branch and adds one commit.
4. No pull request is created automatically by an import.
5. The project page offers **Arbetet är klart – skapa pull request** once at least one commit exists.
6. Creating the PR closes the active work session; the next import starts a new work session.

The work-session metadata is durable in PostgreSQL. Import scratch state remains in-memory as before.
