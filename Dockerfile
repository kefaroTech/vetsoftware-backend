FROM --platform=$BUILDPLATFORM maven:3.9.16-eclipse-temurin-25-alpine@sha256:72e2d64836e659d053a573ac9ebab05b78ae78fa7bb69b7452a7cb877b465fc7 AS build

WORKDIR /workspace

COPY pom.xml ./
RUN mvn --batch-mode --no-transfer-progress dependency:go-offline

# spotless:check y checkstyle leen su configuracion de config/, fuera de src/: sin este COPY el
# `package` de abajo aborta con "Could not find resource /workspace/config/spotless/
# eclipse-formatter.properties". Va antes que src/ a proposito: cambia mucho menos, asi que la
# capa se reaprovecha entre builds.
COPY config ./config
COPY src ./src
# spotless:check se salta AQUI, y solo aqui. Su politica de finales de linea es GIT_ATTRIBUTES,
# pero .dockerignore excluye .git del contexto: dentro de la imagen no hay repositorio que
# consultar, asi que en un contexto construido desde una copia de trabajo Windows (CRLF, porque
# .gitattributes no normaliza *.java) marca los 5.174 ficheros como violaciones. El gate real no
# se pierde: ci.yml:134 corre `mvn verify` sobre el checkout de Linux -donde spotless-check sigue
# atado a process-sources- y el pre-commit lo vuelve a pasar en cada commit.
RUN mvn --batch-mode --no-transfer-progress -DskipTests -Dspotless.check.skip=true package

FROM eclipse-temurin:25.0.3_9-jre-noble@sha256:fbcf915c585659b30eb766ada4d6d7cfc9ec1040bf521e95bf61b10a25af73db AS runtime

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
