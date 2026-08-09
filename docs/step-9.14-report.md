# Step 9.14 report — GitHub-triggered production deployment

## Result

Step 9.14 is complete in revision `r0121` / `1.0.0-rc.73`.

Production deployment can now be explicitly triggered from GitHub Actions by selecting an immutable application image version. The workflow does not deploy on push and refuses dispatches from a non-default branch.

## Delivered deployment path

- `.github/workflows/deploy-production.yml`
  - manual `workflow_dispatch` only;
  - required immutable `version` input;
  - `contents: read` workflow permission;
  - `production` environment;
  - production concurrency group with no cancellation of an in-flight deployment;
  - strict version validation;
  - pinned SSH `known_hosts` supplied from a verified GitHub variable;
  - normal OpenSSH client, no third-party SSH action.
- `ops/production/deploy.sh`
  - root-owned installation target `/opt/zip-github/bin/deploy.sh`;
  - validates one version argument;
  - refuses tracked local Git changes;
  - updates the checkout as `zip-github-deploy` with `fetch` + `pull --ff-only`;
  - pulls the requested Compose images before persisting the new version, so a missing image leaves `.env` untouched;
  - updates only `ZIP_GITHUB_VERSION` while preserving `.env` owner/mode and starts Compose;
  - waits for backend readiness and verifies frontend HTTP;
  - intentionally avoids automatic rollback because Flyway migrations are forward-only;
  - never runs `git clean`, preserving ignored deployment-only artifacts.
- `ops/production/deploy-ssh-command.sh`
  - validates `SSH_ORIGINAL_COMMAND` and only accepts `deploy <version>`;
  - invokes the root-owned deploy script through the allowlisted sudo path.
- `ops/production/zip-github-deploy.sudoers`
  - grants only the dedicated deploy account passwordless execution of the root-owned deploy script.
- `docs/production-deployment.md`
  - server ownership migration from the current operator-owned checkout;
  - deploy-account creation;
  - restricted SSH key setup;
  - host-key verification;
  - GitHub Environment variables/secret;
  - manual deployment and rollback procedure.

## Security decisions

The deployment user is deliberately not placed in the Docker group. Docker access is root-equivalent, so Docker operations are kept inside the root-owned deployment script reached through a constrained sudo rule.

The GitHub Actions SSH key does not grant a normal shell. `authorized_keys` uses OpenSSH `restrict` plus a forced command. The server host key is verified out of band and stored as `PRODUCTION_SSH_KNOWN_HOSTS`; the workflow does not trust a freshly scanned key at deployment time.

The server checkout is owned by `zip-github-deploy` only to permit unprivileged Git updates. `.env` remains `root:zip-github-deploy` mode `0640`, and `/opt/zip-github/bin` plus deployment scripts remain root-owned.

## Verification

The revision verifies:

- workflow YAML parses;
- deployment scripts pass `bash -n`;
- deployment script rejects malformed versions in a sandboxed dry invocation before filesystem mutation;
- workflow is manual-only and uses the production environment/concurrency guard;
- workflow permissions are `contents: read`;
- SSH uses `StrictHostKeyChecking=yes` and supplied known-hosts material;
- no third-party SSH Action is used;
- forced-command helper only accepts `deploy <version>`;
- deploy script contains no `git clean` or automatic rollback;
- status/release ledgers identify step 9.14 as complete.

Live SSH deployment is intentionally an operator acceptance check because the packaging environment has no access to the production host or its GitHub Environment secret.
