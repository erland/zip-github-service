FROM maven:3.9.9-eclipse-temurin-21 AS build
WORKDIR /workspace

COPY pom.xml .
RUN mvn -B -ntp dependency:go-offline

COPY src ./src
RUN mvn -B -ntp package -DskipTests

FROM eclipse-temurin:21-jre-alpine
WORKDIR /app

RUN apk add --no-cache curl docker-cli \
    && addgroup -S zipbuildserver \
    && adduser -S zipbuildserver -G zipbuildserver \
    && mkdir -p /data/zip-buildserver \
    && chown -R zipbuildserver:zipbuildserver /app /data/zip-buildserver

COPY --from=build /workspace/target/quarkus-app/ /app/

USER zipbuildserver
EXPOSE 8080

ENTRYPOINT ["java", "-jar", "/app/quarkus-run.jar"]
