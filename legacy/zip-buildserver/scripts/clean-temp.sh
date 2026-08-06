#!/usr/bin/env bash
set -euo pipefail

ROOT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"

usage() {
  cat <<'EOF'
Usage: ./scripts/clean-temp.sh [--all] [--docker] [--dry-run]

Remove generated local temporary files from the repository checkout.

Default cleanup removes local build/test artifacts that are safe to recreate:
  - root target/
  - backend/target/
  - frontend/dist/
  - frontend/coverage/
  - .local/zip-buildserver-data/

Options:
  --all      Also remove frontend/node_modules/.
  --docker   Also stop/remove the local e2e Docker Compose stack and named volumes.
  --dry-run  Print what would be removed without deleting files.
  -h, --help Show this help.
EOF
}

REMOVE_NODE_MODULES=false
REMOVE_DOCKER=false
DRY_RUN=false

while [[ $# -gt 0 ]]; do
  case "$1" in
    --all)
      REMOVE_NODE_MODULES=true
      ;;
    --docker)
      REMOVE_DOCKER=true
      ;;
    --dry-run)
      DRY_RUN=true
      ;;
    -h|--help)
      usage
      exit 0
      ;;
    *)
      echo "Unknown option: $1" >&2
      usage >&2
      exit 1
      ;;
  esac
  shift
done

remove_path() {
  local path="$1"

  if [[ "$path" != "$ROOT_DIR"/* ]]; then
    echo "Refusing to remove path outside repository: $path" >&2
    exit 1
  fi

  if [[ ! -e "$path" ]]; then
    return 0
  fi

  if [[ "$DRY_RUN" == "true" ]]; then
    echo "Would remove: ${path#$ROOT_DIR/}"
  else
    echo "Removing: ${path#$ROOT_DIR/}"
    rm -rf "$path"
  fi
}

remove_path "$ROOT_DIR/target"
remove_path "$ROOT_DIR/backend/target"
remove_path "$ROOT_DIR/frontend/dist"
remove_path "$ROOT_DIR/frontend/coverage"
remove_path "$ROOT_DIR/.local/zip-buildserver-data"

if [[ "$REMOVE_NODE_MODULES" == "true" ]]; then
  remove_path "$ROOT_DIR/frontend/node_modules"
fi

if [[ "$REMOVE_DOCKER" == "true" ]]; then
  if command -v docker >/dev/null 2>&1; then
    if [[ "$DRY_RUN" == "true" ]]; then
      echo "Would run: COMPOSE_PROJECT_NAME=zip-buildserver-e2e docker compose down -v --remove-orphans"
    else
      echo "Stopping and removing local e2e Docker Compose stack."
      COMPOSE_PROJECT_NAME=zip-buildserver-e2e docker compose down -v --remove-orphans || true
    fi
  else
    echo "Docker is not installed; skipping Docker cleanup." >&2
  fi
fi

echo "Temporary cleanup completed."
