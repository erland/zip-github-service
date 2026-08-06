#!/usr/bin/env bash
set -euo pipefail

ROOT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"

require_command() {
  if ! command -v "$1" >/dev/null 2>&1; then
    echo "Missing required command: $1" >&2
    exit 1
  fi
}

require_command mvn
require_command npm
require_command docker

echo "==> Running backend tests"
(
  cd "$ROOT_DIR/backend"
  mvn test
)

echo "==> Installing frontend dependencies"
(
  cd "$ROOT_DIR/frontend"
  npm install
)

echo "==> Running frontend tests"
(
  cd "$ROOT_DIR/frontend"
  npm test
)

echo "==> Building frontend"
(
  cd "$ROOT_DIR/frontend"
  npm run build
)

echo "==> Building worker image"
"$ROOT_DIR/scripts/build-worker-image.sh"

echo "All local build checks completed."
