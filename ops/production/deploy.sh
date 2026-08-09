#!/usr/bin/env bash
set -euo pipefail

PATH=/usr/local/sbin:/usr/local/bin:/usr/sbin:/usr/bin:/sbin:/bin
export PATH
umask 077

APP_DIR=/opt/zip-github/app
ENV_FILE="$APP_DIR/.env"
DEPLOY_BRANCH=main

usage() {
  echo "Usage: $0 <version>" >&2
  exit 2
}

[[ $# -eq 1 ]] || usage
VERSION="$1"

if [[ ! "$VERSION" =~ ^[0-9]+\.[0-9]+\.[0-9]+(-rc\.[0-9]+)?$ ]]; then
  echo "Invalid version: $VERSION" >&2
  exit 2
fi

if [[ ! -d "$APP_DIR/.git" ]]; then
  echo "Missing Git checkout: $APP_DIR" >&2
  exit 1
fi

if [[ ! -f "$ENV_FILE" ]]; then
  echo "Missing deployment environment file: $ENV_FILE" >&2
  exit 1
fi

if ! grep -q '^ZIP_GITHUB_VERSION=' "$ENV_FILE"; then
  echo "ZIP_GITHUB_VERSION is missing from $ENV_FILE" >&2
  exit 1
fi

# The checkout is owned by the unprivileged deploy account. Refuse to overwrite
# local tracked changes. Ignored deployment artifacts such as .env and the
# signed Shortcut are intentionally not considered dirty Git state.
if ! sudo -u zip-github-deploy git -C "$APP_DIR" diff --quiet -- || \
   ! sudo -u zip-github-deploy git -C "$APP_DIR" diff --cached --quiet --; then
  echo "Tracked local changes exist in $APP_DIR; deployment aborted." >&2
  exit 1
fi

CURRENT_VERSION=$(sed -n 's/^ZIP_GITHUB_VERSION=//p' "$ENV_FILE" | head -n 1)
CURRENT_COMMIT=$(sudo -u zip-github-deploy git -C "$APP_DIR" rev-parse --verify HEAD)

echo "Current application version: ${CURRENT_VERSION:-unknown}"
echo "Current deployment commit: $CURRENT_COMMIT"
echo "Requested application version: $VERSION"

echo "Updating deployment checkout from origin/$DEPLOY_BRANCH ..."
sudo -u zip-github-deploy git -C "$APP_DIR" fetch --prune origin "$DEPLOY_BRANCH"
sudo -u zip-github-deploy git -C "$APP_DIR" checkout "$DEPLOY_BRANCH"
sudo -u zip-github-deploy git -C "$APP_DIR" pull --ff-only origin "$DEPLOY_BRANCH"

NEW_COMMIT=$(sudo -u zip-github-deploy git -C "$APP_DIR" rev-parse --verify HEAD)
echo "Deployment checkout commit: $NEW_COMMIT"

cd "$APP_DIR"

# Pull the requested images before changing the persisted version. An explicit
# process environment variable overrides .env for this pull, so a missing image
# leaves the currently deployed version untouched.
echo "Pulling immutable container images for $VERSION ..."
ZIP_GITHUB_VERSION="$VERSION" docker compose pull

# Update only ZIP_GITHUB_VERSION while preserving the permissions/ownership of
# the secret-bearing .env file.
tmp_env=$(mktemp "$APP_DIR/.env.deploy.XXXXXX")
trap 'rm -f "$tmp_env"' EXIT
awk -v version="$VERSION" '
  BEGIN { replaced = 0 }
  /^ZIP_GITHUB_VERSION=/ {
    print "ZIP_GITHUB_VERSION=" version
    replaced = 1
    next
  }
  { print }
  END {
    if (!replaced) exit 42
  }
' "$ENV_FILE" > "$tmp_env"
chown --reference="$ENV_FILE" "$tmp_env"
chmod --reference="$ENV_FILE" "$tmp_env"
mv -f "$tmp_env" "$ENV_FILE"
trap - EXIT

echo "Starting deployment ..."
docker compose up -d

docker compose ps

echo "Waiting for backend readiness ..."
ready=false
for attempt in $(seq 1 30); do
  if curl --fail --silent --show-error \
      http://127.0.0.1:8080/q/health/ready >/dev/null 2>&1; then
    ready=true
    break
  fi
  sleep 2
done

if [[ "$ready" != true ]]; then
  echo "Deployment failed readiness check for version $VERSION." >&2
  echo "No automatic rollback was attempted because database migrations are forward-only." >&2
  echo "Inspect: cd $APP_DIR && docker compose ps && docker compose logs --tail=200 backend" >&2
  exit 1
fi

if ! curl --fail --silent --show-error http://127.0.0.1:5173/ >/dev/null; then
  echo "Frontend health check failed for version $VERSION." >&2
  exit 1
fi

echo "Deployment successful."
echo "Version: $VERSION"
echo "Commit:  $NEW_COMMIT"
