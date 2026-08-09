# Production deployment from GitHub Actions

This guide replaces the manual production sequence (`ssh`, edit `.env`, `git pull`, run `deploy.sh`) with an explicit manual GitHub Actions deployment. GitHub remains the trigger and audit surface; the production host performs the actual checkout update, image pull, restart and readiness checks.

## Security model

- Deployment is **manual** through `workflow_dispatch`; pushing code does not automatically deploy production.
- The workflow uses the GitHub Environment `production` and a concurrency group so only one production deployment runs at a time.
- GitHub connects over SSH as a dedicated OS user, `zip-github-deploy`.
- The deployment SSH key is restricted in `authorized_keys` with `restrict` and a forced command. It cannot open a shell, create tunnels, forward an agent or run arbitrary commands.
- `zip-github-deploy` is **not** added to the `docker` group. Docker group membership is effectively root-level access.
- The forced SSH command may only invoke the root-owned `/opt/zip-github/bin/deploy.sh` through one narrowly scoped passwordless sudo rule.
- `/opt/zip-github/app` is owned by `zip-github-deploy` so Git fetch/pull does not need root. `.env` remains secret-bearing and should be `root:zip-github-deploy` mode `0640`.
- The workflow pins the server host key through `known_hosts`; it does not run `ssh-keyscan` on every deployment and blindly trust the result.

## 1. Install the deployment scripts on the server

Start from an authenticated operator shell (for example your existing `erland` account with sudo):

```bash
cd /opt/zip-github/app
git pull --ff-only

sudo install -o root -g root -m 0755 \
  ops/production/deploy.sh \
  /opt/zip-github/bin/deploy.sh

sudo install -o root -g root -m 0755 \
  ops/production/deploy-ssh-command.sh \
  /opt/zip-github/bin/deploy-ssh-command.sh

sudo install -o root -g root -m 0440 \
  ops/production/zip-github-deploy.sudoers \
  /etc/sudoers.d/zip-github-deploy

sudo visudo -cf /etc/sudoers.d/zip-github-deploy
```

Do not make `/opt/zip-github/bin/deploy.sh` writable by `zip-github-deploy`; otherwise the sudo rule would allow the deploy account to replace the script with arbitrary root commands.

## 2. Create the dedicated deployment account

```bash
sudo useradd \
  --create-home \
  --shell /bin/bash \
  zip-github-deploy
```

If the user already exists, do not recreate it.

The application checkout should now be controlled by this deployment account:

```bash
sudo chown -R zip-github-deploy:zip-github-deploy /opt/zip-github/app
sudo chown root:zip-github-deploy /opt/zip-github/app/.env
sudo chmod 0640 /opt/zip-github/app/.env
```

Keep `/opt/zip-github/bin` and its scripts root-owned:

```bash
sudo chown root:root /opt/zip-github/bin
sudo chmod 0755 /opt/zip-github/bin
sudo chown root:root \
  /opt/zip-github/bin/deploy.sh \
  /opt/zip-github/bin/deploy-ssh-command.sh
```

Verify that the checkout can be read and updated by the deployment user:

```bash
sudo -u zip-github-deploy git -C /opt/zip-github/app status --short
sudo -u zip-github-deploy git -C /opt/zip-github/app remote -v
```

If the checkout currently uses an SSH Git remote that only `erland` can authenticate to, change the checkout to HTTPS for a public repository, or deliberately configure a separate read-only repository credential for `zip-github-deploy`. For a public repository:

```bash
sudo -u zip-github-deploy git -C /opt/zip-github/app \
  remote set-url origin https://github.com/erland/zip-github-service.git
```

## 3. Create the deployment SSH key

Generate a dedicated key on your Mac. Do not reuse your personal SSH key:

```bash
ssh-keygen \
  -t ed25519 \
  -f ~/.ssh/zip-github-production-deploy \
  -C 'zip-github GitHub Actions production deploy'
```

Use no passphrase for this automation-only key because GitHub Actions must use it non-interactively. Protect the private key as a GitHub Actions secret and do not copy it into the repository.

Show the public key:

```bash
cat ~/.ssh/zip-github-production-deploy.pub
```

On the production server:

```bash
sudo install -d -o zip-github-deploy -g zip-github-deploy -m 0700 \
  /home/zip-github-deploy/.ssh
sudo touch /home/zip-github-deploy/.ssh/authorized_keys
sudo chown zip-github-deploy:zip-github-deploy \
  /home/zip-github-deploy/.ssh/authorized_keys
sudo chmod 0600 /home/zip-github-deploy/.ssh/authorized_keys
```

Append **one line** using the public key from the Mac:

```text
restrict,command="/opt/zip-github/bin/deploy-ssh-command.sh" ssh-ed25519 AAAA... zip-github GitHub Actions production deploy
```

`restrict` plus the forced command means possession of this key does not grant a general-purpose shell.

## 4. Verify the server SSH host key

On the production server, display the ED25519 host-key fingerprint:

```bash
sudo ssh-keygen -lf /etc/ssh/ssh_host_ed25519_key.pub
```

On your Mac, fetch the public host key:

```bash
ssh-keyscan -t ed25519 zip-github.isaksson.info
```

Verify the fetched key fingerprint against the fingerprint shown locally on the server before trusting it:

```bash
ssh-keyscan -t ed25519 zip-github.isaksson.info 2>/dev/null \
  | ssh-keygen -lf -
```

After they match, keep the complete `ssh-keyscan` output line. This becomes `PRODUCTION_SSH_KNOWN_HOSTS` in GitHub.

## 5. Test the restricted deployment key from the Mac

First verify that arbitrary commands are rejected:

```bash
ssh -i ~/.ssh/zip-github-production-deploy \
  zip-github-deploy@zip-github.isaksson.info \
  'whoami'
```

It should return `This SSH key may only run: deploy <version>` and must not provide shell access.

Then test a real deployment only when you intend to redeploy that version:

```bash
ssh -i ~/.ssh/zip-github-production-deploy \
  zip-github-deploy@zip-github.isaksson.info \
  'deploy 1.0.0-rc.72'
```

## 6. Configure GitHub Environment, variables and secret

In repository `erland/zip-github-service`:

1. Open **Settings → Environments → New environment**.
2. Name it `production`.
3. If your GitHub plan/repository visibility supports it, configure required reviewers and restrict deployment branches to the default branch.
4. Add these environment **variables**:
   - `PRODUCTION_HOST` = `zip-github.isaksson.info`
   - `PRODUCTION_PORT` = `22`
   - `PRODUCTION_USER` = `zip-github-deploy`
   - `PRODUCTION_SSH_KNOWN_HOSTS` = the verified complete ED25519 known-hosts line from step 4.
5. Add this environment **secret**:
   - `PRODUCTION_SSH_PRIVATE_KEY` = complete contents of `~/.ssh/zip-github-production-deploy` including the BEGIN/END lines.

The workflow uses the same `vars.*` / `secrets.*` names whether they are defined at environment or repository scope. If environment-scoped variables/secrets are unavailable for your repository/plan, define the same names under **Settings → Secrets and variables → Actions** instead; keep `environment: production` in the workflow for deployment history.

## 7. Deploy from GitHub

The workflow is `.github/workflows/deploy-production.yml` and is intentionally manual.

After it has been merged to the repository default branch:

1. Open **Actions**.
2. Select **Deploy production**.
3. Choose **Run workflow** from the default branch.
4. Enter an immutable image version, for example `1.0.0-rc.72`.
5. Start the workflow and approve the `production` environment if you configured reviewers.

The workflow rejects dispatches from non-default branches and rejects malformed versions before opening SSH.

## What the server deployment does

`/opt/zip-github/bin/deploy.sh <version>`:

1. validates the version argument;
2. refuses to overwrite tracked local Git changes;
3. fetches/pulls `origin/main` with `--ff-only` as `zip-github-deploy`;
4. pulls the requested images with a temporary `ZIP_GITHUB_VERSION` override so a missing image does not modify `.env`;
5. changes only `ZIP_GITHUB_VERSION` in `.env` while preserving its owner/mode;
6. runs `docker compose up -d`;
7. prints `docker compose ps`;
8. waits up to about one minute for backend readiness;
9. checks the frontend locally;
10. reports the deployed version and Git commit in the Actions log.

The script deliberately does **not** run `git clean`, because the production bundle may contain ignored deployment-only artifacts such as the signed Shortcut.

## Failure and rollback

A failed readiness check makes the GitHub Actions deployment red and prints diagnostic commands. It does not automatically roll back. Flyway/database migrations are forward-only, so automatic image rollback could make an incident worse after a schema migration.

Inspect on the server:

```bash
cd /opt/zip-github/app
docker compose ps
docker compose logs --tail=200 backend
```

When a rollback is safe, run the same **Deploy production** workflow again and enter the previous immutable image version. The old version does not need a special rollback workflow.

## Removing the old manual deployment path

After the GitHub deployment has been exercised successfully, your normal production procedure no longer requires:

- logging in as `erland` for each release;
- editing `.env` manually;
- manually running `git pull`;
- manually invoking `deploy.sh`.

Keep the `erland` operator account and sudo access for break-glass incident handling, server maintenance, database restore and deployment-key rotation.
