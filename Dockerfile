# Imagen de UNA sola etapa: el jar entra ya construido por el contexto.
#
# Hasta agosto de 2026 este fichero tenia una etapa `build` sobre
# maven:3.9.16-eclipse-temurin-25-alpine que compilaba el proyecto entero dentro de la
# imagen: `dependency:go-offline` (39,7 s) + `package -DskipTests` (45,0 s) por cada
# publicacion, para producir exactamente el mismo binario que el job `verify` de
# `ci.yml` acababa de producir sobre ESE MISMO commit. Y el cache de buildx no lo
# amortiguaba nunca: el step "Bump development version" reescribe `pom.xml` antes del
# build, asi que el `COPY pom.xml` cambiaba en todos los runs e invalidaba las dos
# capas caras. Ahora el jar se copia y punto.
#
# De donde sale el jar en cada camino:
#   - dev  (.github/workflows/publish-dev-image.yml): se descarga el artefacto
#          `backend-boot-jar` que publica `ci.yml`, se comprueba que trae uno y solo
#          uno, y se renombra a build/docker/application.jar.
#   - release (.github/workflows/publish-release.yml): se copia el jar que dejo en
#          target/ el `mvn verify` del propio job, en el mismo workspace.
#   - local:
#          mvn --batch-mode -DskipTests package
#          mkdir -p build/docker && cp target/vetsoftware-*.jar build/docker/application.jar
#          docker build -t vetsoftware/backend:local .
#
# Saltarse esos dos primeros comandos da este error, y no otro mas explicito, porque
# BuildKit resuelve los origenes de `COPY` antes de ejecutar nada y no hay ningun paso
# donde imprimir una explicacion:
#
#   ERROR: failed to solve: failed to compute cache key: failed to calculate checksum
#   of ref ...: "/build/docker/application.jar": not found
#
# Al desaparecer la etapa `build` desaparece tambien el `-Dspotless.check.skip=true`
# que habia que justificar aqui: spotless y checkstyle vuelven a correr solo donde
# siempre debieron correr, en el `mvn verify` de `ci.yml` sobre el checkout de Linux y
# en el pre-commit. La imagen ya no tiene opinion sobre el formato del codigo.
#
# AVISO de seguridad, porque el reparto cambia: el jar es OPACO para `.dockerignore`.
# Las exclusiones de `src/main/resources/application-local.*` que cerraron INF-34 ya no
# pueden filtrar nada, porque ese fichero se empaqueta en BOOT-INF/classes durante el
# `mvn package`, no durante el `docker build`. En CI es inocuo -el checkout no lo trae,
# esta en .gitignore-, pero un `docker build` local sobre un `mvn package` local SI
# mete el perfil local, con su contrasena de MySQL, dentro de la imagen. Las imagenes
# construidas en una copia de trabajo no se publican.

FROM eclipse-temurin:25.0.3_9-jre-noble@sha256:fbcf915c585659b30eb766ada4d6d7cfc9ec1040bf521e95bf61b10a25af73db AS runtime

ARG APP_UID=10001
ARG APP_GID=10001

# La ruta del jar dentro del contexto. Es una ruta EXACTA y no un glob a proposito: un
# `COPY dir/*.jar /app/application.jar` con dos ficheros en el directorio los concatena
# en el destino sin emitir un solo aviso, y `target/` deja `*.jar` junto a `*.jar.original`
# y -si alguien activa los plugins- junto a `*-sources.jar`. Quien llena el directorio
# es responsable de que haya un unico fichero con este nombre; los dos workflows lo
# comprueban antes de construir. Por eso `.dockerignore` tampoco admite otra cosa.
ARG JAR_FILE=build/docker/application.jar

# Dos cosas que parecen redundantes y no lo son. Quitar cualquiera de las dos devuelve la
# imagen al estado con el que el gate de escaneo de ECR bloqueo el despliegue de dev el
# 26-08-2026.
#
# 1. `apt-get upgrade`. El `install` de aqui abajo solo trae los cuatro paquetes nombrados;
#    a `libssl3t64` -preinstalado en la base, y con una version que ya satisface la
#    dependencia de `curl`- apt no lo toca. Asi es como la imagen se quedo en openssl
#    3.0.13-0ubuntu3.12 y el escaneo la rechazo con seis HIGH: CVE-2026-63072,
#    CVE-2026-63076 y CVE-2026-54874, cada uno contado en `openssl` y en `libssl3t64`,
#    cuando Ubuntu ya habia publicado la 3.0.13-0ubuntu3.15 en USN-8678-1. Fijar otro
#    digest de `eclipse-temurin` no lo arregla: la base se reconstruye con su propia
#    cadencia y aquel dia la mas reciente era anterior a la USN. Generico y no
#    `--only-upgrade libssl3t64 openssl` porque el gate bloquea con CUALQUIER HIGH en
#    cualquier paquete: lo puntual solo aplaza la misma rotura a la siguiente USN.
#
# 2. `APT_SECURITY_REFRESH`. Sin el, ese `upgrade` se ejecuta una sola vez y su capa se
#    sirve del cache `type=gha` en todas las publicaciones siguientes, congelando los
#    paquetes en la fecha del primer build. No es teorico: la imagen del 26-08-2026 se
#    publico con curl 8.5.0-2ubuntu10.12 aunque la 10.13 -que cierra CVE-2026-8932- ya
#    llevaba seis dias en el archivo, porque esta misma capa venia del cache. Los dos
#    workflows de publicacion le pasan el `run_id`, distinto en cada ejecucion, y asi la
#    capa se reconstruye siempre. Lo que se paga son los segundos de apt; el `COPY` del
#    jar que viene detras cambia en cada publicacion de todos modos.
ARG APT_SECURITY_REFRESH=cached

RUN echo "Refresco de seguridad: ${APT_SECURITY_REFRESH}" \
    && apt-get update \
    && apt-get upgrade --yes \
    && apt-get install --no-install-recommends --yes ca-certificates curl fontconfig fonts-noto-core \
    && rm -rf /var/lib/apt/lists/* \
    && groupadd --gid "${APP_GID}" vetsoftware \
    && useradd --uid "${APP_UID}" --gid "${APP_GID}" --create-home --shell /usr/sbin/nologin vetsoftware

WORKDIR /app
COPY --chown=${APP_UID}:${APP_GID} ${JAR_FILE} /app/application.jar

# La version que se publica, inyectada por quien construye. Se declara AQUI, lo mas tarde
# posible, para no meterse en la clave de cache del `apt-get` de arriba: solo afecta a las
# dos lineas de metadatos que siguen.
#
# Existe porque `application.yml` resuelve `service.version: ${VETSOFTWARE_VERSION:@project.version@}`
# y ese valor por defecto se HORNEA en el jar durante el `mvn package`. En desarrollo el jar
# lo empaqueta el job `verify`, que corre ANTES del step que bumpea la version, asi que el
# numero horneado es siempre el anterior al del tag con el que la imagen se despliega: toda
# traza, metrica y log de dev llegaba a Grafana marcada con una version que no era la suya.
# La variable de entorno gana al valor horneado, de modo que esto se corrige sin recompilar
# nada. En release el valor coincide con el horneado -`release-version.mjs verify` exige que
# `pom.xml` ya declare la version final antes del `mvn verify`- y se inyecta igualmente por
# simetria: los dos caminos dicen la version en voz alta en vez de confiar en el empaquetado.
#
# El defecto `local` es deliberado y NO es un valor vacio. Un `ENV VETSOFTWARE_VERSION=`
# vacio seria peor que no ponerlo: Spring aplica el valor por defecto de un placeholder solo
# cuando la propiedad no existe, no cuando existe vacia, asi que `service.version` se iria a
# la cadena vacia. Con `local`, una imagen construida en una copia de trabajo se delata sola
# en Grafana, que es exactamente lo que interesa de una imagen que no debe publicarse.
ARG APP_VERSION
ENV VETSOFTWARE_VERSION=${APP_VERSION:-local}

ENV JAVA_TOOL_OPTIONS="-XX:MaxRAMPercentage=75.0 -XX:+ExitOnOutOfMemoryError" \
    SPRING_PROFILES_ACTIVE=prod

USER ${APP_UID}:${APP_GID}

EXPOSE 8080

HEALTHCHECK --interval=30s --timeout=5s --start-period=60s --retries=3 \
  CMD curl --fail --silent --show-error http://localhost:8080/api/v1/actuator/health/readiness >/dev/null || exit 1

ENTRYPOINT ["java", "-jar", "/app/application.jar"]

