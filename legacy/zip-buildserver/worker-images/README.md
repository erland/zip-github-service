# Worker Images

Verification runs execute inside ephemeral worker containers rather than inside the API process.

## `node-maven`

The initial worker image lives in `worker-images/node-maven/` and includes:

- Java 21
- Maven
- Node.js LTS
- npm
- basic shell/archive tools
- a non-root `worker` user

Build it locally with:

```bash
./scripts/build-worker-image.sh
```

Or directly with Docker:

```bash
docker build -t zip-buildserver-worker-node-maven:local worker-images/node-maven
```

Smoke-check the image with:

```bash
docker run --rm zip-buildserver-worker-node-maven:local java -version
docker run --rm zip-buildserver-worker-node-maven:local mvn -version
docker run --rm zip-buildserver-worker-node-maven:local node --version
docker run --rm zip-buildserver-worker-node-maven:local npm --version
```

The image is intentionally generic. Server-side verification plans still control which commands may run.


## Docker executor

Step 14 adds backend support for running approved verification-plan commands in this worker image.
Build the image locally before enabling Docker execution:

```bash
./scripts/build-worker-image.sh
```

Enable real worker execution with:

```bash
ZIP_BUILDSERVER_WORKER_EXECUTOR=docker
ZIP_BUILDSERVER_WORKER_IMAGE=zip-buildserver-worker-node-maven:local
```

The Docker socket mode is intended for trusted self-hosted development environments.
