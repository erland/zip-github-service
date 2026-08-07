FROM maven:3.9.11-eclipse-temurin-21 AS build
WORKDIR /workspace
COPY pom.xml ./
COPY src ./src
RUN mvn --batch-mode --no-transfer-progress package -DskipTests

FROM eclipse-temurin:21-jre
RUN apt-get update \
    && apt-get install -y --no-install-recommends curl git \
    && rm -rf /var/lib/apt/lists/* \
    && useradd --system --uid 10001 zipgithub
WORKDIR /app
COPY --from=build /workspace/target/quarkus-app/ ./
COPY docker/git-askpass.sh /usr/local/bin/zip-github-git-askpass
RUN chmod 0555 /usr/local/bin/zip-github-git-askpass
USER zipgithub
EXPOSE 8080
ENTRYPOINT ["java", "-jar", "quarkus-run.jar"]
