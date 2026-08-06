#!/usr/bin/env bash
set -euo pipefail
for required in backend/pom.xml frontend/package.json docker-compose.yml docs/implementation-status.md; do test -f "$required"; done
! grep -R -n --exclude-dir=target --exclude='*.md' -E 'zipbuildserver|DockerCommandExecutor|VerificationRun|/var/run/docker.sock' backend/src frontend/src docker-compose.yml
printf 'Clean baseline structure verified.\n'
