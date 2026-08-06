# syntax=docker/dockerfile:1

FROM eclipse-temurin:21-jdk-jammy

ARG NODE_MAJOR=20

LABEL org.opencontainers.image.title="zip-buildserver node-maven worker"
LABEL org.opencontainers.image.description="Ephemeral verification worker with Java 21, Maven, Node.js LTS, and npm"

ENV DEBIAN_FRONTEND=noninteractive
ENV MAVEN_CONFIG=/home/worker/.m2
ENV NPM_CONFIG_CACHE=/home/worker/.npm

RUN apt-get update \
    && apt-get install -y --no-install-recommends \
        ca-certificates \
        curl \
        gnupg \
        maven \
        unzip \
        zip \
        git \
        bash \
    && mkdir -p /etc/apt/keyrings \
    && curl -fsSL https://deb.nodesource.com/gpgkey/nodesource-repo.gpg.key \
        | gpg --dearmor -o /etc/apt/keyrings/nodesource.gpg \
    && echo "deb [signed-by=/etc/apt/keyrings/nodesource.gpg] https://deb.nodesource.com/node_${NODE_MAJOR}.x nodistro main" \
        > /etc/apt/sources.list.d/nodesource.list \
    && apt-get update \
    && apt-get install -y --no-install-recommends nodejs \
    && apt-get clean \
    && rm -rf /var/lib/apt/lists/*

RUN useradd --create-home --shell /bin/bash --uid 10001 worker \
    && mkdir -p /workspace /home/worker/.m2 /home/worker/.npm \
    && chown -R worker:worker /workspace /home/worker

WORKDIR /workspace

USER worker

CMD ["bash", "-lc", "java -version && mvn -version && node --version && npm --version"]
