# Scripts

Local development helpers:

```bash
./scripts/build-all.sh
./scripts/clean-temp.sh
./scripts/dev-up.sh
./scripts/dev-down.sh
./scripts/build-worker-image.sh
```

`dev-up.sh` starts the Docker Compose development stack.

`dev-down.sh` stops it and removes named volumes for a clean local reset.

`build-worker-image.sh` builds the local worker image used by future Docker-based verification execution:

```bash
ZIP_BUILDSERVER_WORKER_IMAGE=zip-buildserver-worker-node-maven:local ./scripts/build-worker-image.sh
```


## Real Docker worker execution

Build the worker image and enable the Docker executor:

```bash
./scripts/build-worker-image.sh
ZIP_BUILDSERVER_WORKER_EXECUTOR=docker docker compose up --build
```

## End-to-end Docker verification

Run the complete local verification flow with fixture packages:

```bash
./scripts/verify-local.sh
```

The script:

1. Builds the local worker image.
2. Starts Docker Compose with the Docker worker executor enabled.
3. Creates zip archives from `test-fixtures/`.
4. Creates sessions, uploads packages, starts runs, and validates expected passed/failed statuses.

The script uses a host bind mount for `/data/zip-buildserver` so worker containers launched by the backend can mount extracted workspaces. It also runs the backend container as `root` during the local E2E flow to avoid Docker socket permission issues on typical developer machines.

Useful overrides:

```bash
ZIP_BUILDSERVER_API_TOKEN=change-me ./scripts/verify-local.sh
ZIP_BUILDSERVER_E2E_KEEP_STACK=true ./scripts/verify-local.sh
```

## Temporary cleanup

Remove generated local build, test, and E2E artifacts:

```bash
./scripts/clean-temp.sh
```

Preview removals without deleting files:

```bash
./scripts/clean-temp.sh --dry-run
```

Also remove frontend dependencies and the E2E Docker Compose stack:

```bash
./scripts/clean-temp.sh --all --docker
```

## Full local build

Run backend tests, frontend tests/build, and worker image build:

```bash
./scripts/build-all.sh
```
