FROM --platform=$BUILDPLATFORM maven:3-eclipse-temurin-24-alpine@sha256:1e5a24dab38f3160d404439891ad4fd9b7e14b9e3c5bf65e3a953ba7d6ab4e8e AS build

WORKDIR /workspace

COPY pom.xml ./
RUN mvn --batch-mode --no-transfer-progress dependency:go-offline

COPY src ./src
RUN mvn --batch-mode --no-transfer-progress -DskipTests package

FROM eclipse-temurin:26_35-jre-noble@sha256:280784b83edd2f8c3e70d90a9dcd0b1f08137f0f64db3b5bde5ee1ec1807d384 AS runtime

ARG APP_UID=10001
ARG APP_GID=10001

RUN apt-get update \
    && apt-get install --no-install-recommends --yes ca-certificates curl fontconfig fonts-noto-core \
    && rm -rf /var/lib/apt/lists/* \
    && groupadd --gid "${APP_GID}" vetsoftware \
    && useradd --uid "${APP_UID}" --gid "${APP_GID}" --create-home --shell /usr/sbin/nologin vetsoftware

WORKDIR /app
COPY --from=build --chown=${APP_UID}:${APP_GID} /workspace/target/*.jar /app/application.jar

ENV JAVA_TOOL_OPTIONS="-XX:MaxRAMPercentage=75.0 -XX:+ExitOnOutOfMemoryError" \
    SPRING_PROFILES_ACTIVE=prod

USER ${APP_UID}:${APP_GID}

EXPOSE 8080

HEALTHCHECK --interval=30s --timeout=5s --start-period=60s --retries=3 \
  CMD curl --fail --silent --show-error http://localhost:8080/api/v1/actuator/health/readiness >/dev/null || exit 1

ENTRYPOINT ["java", "-jar", "/app/application.jar"]
