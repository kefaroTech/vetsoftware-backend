# Matriz de versiones del stack local

**Fecha:** 07/28/2026
**Hallazgo que cierra:** OBS-024 del reporte de trazabilidad
**Implementación:** `docker-compose.yml`, `docker/image-versions.lock`, `docker/tests/image-pins.sh`

## 1. El problema real

El hallazgo señalaba tres imágenes con `latest` (Collector, Loki, Grafana). Al revisarlo, el Collector
ya estaba fijado en `0.153.0`, pero había cuatro tags móviles más que el hallazgo no nombraba y que
tienen el mismo defecto:

| Tag anterior | Por qué también era móvil |
|---|---|
| `grafana/loki:latest` | Cualquier release, incluido un cambio de major |
| `grafana/grafana:latest` | Idem |
| `mysql:8.0` | Recibe cada parche de la serie 8.0 |
| `redis:7-alpine` | Recibe cada parche de la serie 7 |
| `localstack/localstack:3` | Recibe cada parche de la serie 3 |
| `sonarqube:lts-community` | Alias de canal: salta de major cuando Sonar promueve una nueva LTA |
| `busybox:1.37` | Recibe cada parche de la serie 1.37 |

`latest` es el caso llamativo, pero no el más peligroso. Un `mysql:8.0` da la **impresión** de estar
fijado y no lo está: dos máquinas que hicieron `docker compose up` en semanas distintas corren
binarios distintos con el mismo repositorio. Eso es exactamente lo que impide reproducir un
incidente pasado, que es el riesgo que describe el hallazgo.

Por eso el criterio aplicado no es "quitar `latest`" sino **toda imagen con versión concreta x.y.z**.

## 2. Matriz vigente

Todas las versiones se leyeron del stack en ejecución antes de fijarlas, no se eligieron de un
changelog. Cada tag fijado resuelve al **mismo image ID** que ya estaba corriendo, así que este
cambio no altera ningún binario: solo lo vuelve reproducible.

| Servicio | Imagen | Versión | Rol |
|---|---|---|---|
| `redis` | `redis` | `7.4.8-alpine` | Caché de aplicación |
| `redis-exporter` | `oliver006/redis_exporter` | `v1.87.0` | Métricas de Redis para Prometheus |
| `mysql` | `mysql` | `8.0.45` | Base de datos |
| `otel-queue-init` | `busybox` | `1.37.0` | Init de permisos del volumen de cola |
| `otel-collector` | `otel/opentelemetry-collector-contrib` | `0.153.0` | Recepción OTLP, tail sampling, colas |
| `tempo` | `grafana/tempo` | `3.0.2` | Trazas |
| `prometheus` | `prom/prometheus` | `v3.13.1` | Métricas, reglas SLO, burn rate |
| `alertmanager` | `prom/alertmanager` | `v0.32.1` | Enrutado e inhibición de alertas |
| `mailpit` | `axllent/mailpit` | `v1.30.6` | SMTP de prueba para Alertmanager |
| `loki` | `grafana/loki` | `3.7.1` | Logs |
| `grafana` | `grafana/grafana` | `13.0.1` | Dashboards y exploración |
| `sonarqube` | `sonarqube` | `9.9.8-community` | Calidad de código (solo desarrollo) |
| `localstack` | `localstack/localstack` | `3.8.1` | S3 + Data Firehose emulados |

Los digests correspondientes están en `docker/image-versions.lock`. El tag dice qué se instala; el
digest prueba qué se instaló.

## 3. Restricciones de compatibilidad

Estas son las que realmente acotan el movimiento. Cada una se deriva de la configuración del repo,
no del changelog del proyecto upstream.

### Loki ≥ 3.0 — ingesta OTLP nativa

`docker/otel-collector.yml` exporta logs con `otlphttp/loki` a `http://loki:3100/otlp`. Ese endpoint
existe a partir de Loki 3.0; en 2.x no está y el Collector recibiría 404 en cada lote. No se usa el
exportador `loki` del Collector (deprecado y retirado), así que **no hay ruta de vuelta a Loki 2.x**
sin reescribir el pipeline de logs.

### Loki 3.7 — esquema TSDB v13

El `local-config.yaml` de la imagen 3.7 escribe índices con el esquema v13. Bajar a una 3.x anterior
con `vetsoftware_loki_data` ya escrito deja índices que la versión antigua no interpreta. La ruta de
downgrade es borrar el volumen, no cambiar el tag.

### Collector 0.153.0 — el exportador se llama `otlp_grpc`

El pipeline de trazas usa el tipo de exportador `otlp_grpc/tempo`. En versiones anteriores del
Collector ese tipo se llamaba `otlp`. Bajar la versión del Collector sin renombrar el exportador en
`docker/otel-collector.yml` produce un fallo de arranque por tipo de componente desconocido, no una
degradación silenciosa.

El Collector además depende de la extensión `file_storage` (cola persistente) y de los procesadores
`memory_limiter` y `tail_sampling`, todos de la distribución **contrib**. La imagen `core` no sirve.

### Grafana 13 — la base interna no se revierte

Grafana migra su base de datos interna al arrancar y no la revierte. Una vez que 13.0.1 tocó
`vetsoftware_grafana_data`, volver a un major anterior exige recrear el volumen. El coste es bajo
porque dashboards y datasources se aprovisionan desde `./docker/grafana`: lo único que se pierde son
preferencias de usuario y anotaciones manuales.

### Prometheus 3.x — flags de arranque

`docker-compose.yml` arranca Prometheus con `--web.enable-remote-write-receiver` y
`--enable-feature=exemplar-storage`. Ambos son 2.x+, pero la retención por tamaño
(`--storage.tsdb.retention.size`) y las reglas SLO asumen PromQL 3.x. Alertmanager `v0.32.1` corre
con `--enable-feature=utf8-strict-mode`; Prometheus 3.x notifica por la API v2 de Alertmanager, así
que la pareja es compatible en este rango.

### LocalStack 3.x — límite de licencia

Las versiones 2026 de LocalStack exigen cuenta y token. El pin en `3.8.1` no es solo
reproducibilidad: subir de major rompe el arranque sin credenciales.

### Compatibilidad validada en ejecución

Comprobado tras recrear el stack con los tags fijados (07/28/2026):

| Comprobación | Resultado |
|---|---|
| Loki responde su versión | `3.7.1` |
| Grafana responde su versión | `13.0.1` |
| Datasource Prometheus desde Grafana 13.0.1 | `OK — Successfully queried the Prometheus API` |
| Datasource Loki desde Grafana 13.0.1 | `OK — Data source successfully connected` |
| Datasource Tempo desde Grafana 13.0.1 | `OK — Streaming test succeeded` |
| Log por OTLP → Collector 0.153.0 → Loki 3.7.1 | Marcador recuperado por `query_range` |
| Traza por OTLP → Collector 0.153.0 → Tempo 3.0.2 | Span recuperado por `/api/traces/{id}` |

## 4. Incompatibilidad conocida y no resuelta: SonarQube

`pom.xml` declara `<java.version>25</java.version>` y `sonar.java.source=25`, pero el stack fija
`sonarqube:9.9.8-community`, cuya línea 9.9 LTA es de 2023 y no analiza fuentes de Java 25.

El pin no causa este problema: lo hace visible. Con `lts-community` el mismo desfase existía y además
se movía solo. Se deja fijado y documentado en lugar de subir de major por dos razones:

- SonarQube es herramienta de desarrollo, no parte del runtime ni de la ruta de observabilidad; no
  bloquea nada de lo que audita OBS-024.
- Un salto de major de SonarQube migra irreversiblemente `vetsoftware_sonarqube_data` y cambia el
  quality gate. Es una decisión aparte, con su propia validación.

**Pendiente:** decidir a qué versión de SonarQube subir para que `mvn sonar:sonar` analice Java 25, y
alinear la mención de "SonarQube 9.9 LTS" en `CLAUDE.md` con lo que se elija.

## 5. Guarda automática

`docker/tests/image-pins.sh` comprueba tres cosas en orden de coste creciente:

1. **Ningún tag móvil en `docker-compose.yml`.** Rechaza `latest`, un tag ausente, un major/minor
   suelto (`8.0`, `3`) y los alias de canal. Un tag pasa solo si contiene `x.y.z`.
2. **Compose y el lock coinciden**, en los dos sentidos: un servicio nuevo sin línea en el lock falla,
   y una línea del lock cuyo servicio ya no existe también.
3. **Con `--digests`: el digest que el registro sirve hoy es el del lock.** Es la única comprobación
   que detecta un tag re-empujado. Requiere red.

```bash
bash docker/tests/image-pins.sh              # puntos 1 y 2, sin red — apto para CI rápido
bash docker/tests/image-pins.sh --digests    # los tres
```

Sale con 0 si todo cuadra y 1 si algo falla. El punto 1 sin el 3 sería falsa tranquilidad: un tag de
versión concreta es inmutable por convención del publicador, no por diseño del registro.

## 6. Procedimiento de actualización controlada

Subir una versión es un cambio deliberado con su propio commit, nunca un efecto secundario de
`docker compose pull`.

1. **Leer la restricción.** Buscar el servicio en la sección 3. Si tiene una restricción, resolverla
   primero (renombrar un exportador, borrar un volumen) o el arranque fallará.
2. **Cambiar un solo servicio por vez** en `docker-compose.yml`. Un salto simultáneo de Collector y
   Loki deja sin saber cuál rompió el pipeline.
3. **Recrear solo ese servicio y sus dependientes:**
   ```bash
   docker compose up -d <servicio>
   ```
4. **Regenerar el lock:**
   ```bash
   bash docker/tests/image-pins.sh --update
   ```
   Revisar el diff: debe cambiar exactamente una línea.
5. **Validar la ruta que toca esa imagen**, no solo que el contenedor arranque:
   - Collector, Loki o Tempo → `bash docker/tests/resilience.sh` (los cinco escenarios de OBS-027).
   - Grafana → salud de los tres datasources (`/api/datasources/uid/{prometheus,loki,tempo}/health`).
   - Prometheus o Alertmanager → `promtool` sobre `docker/prometheus-*.yml` y las pruebas de reglas
     en `docker/tests/`.
   - MySQL → arranque del backend con `ddl-auto: validate`. Ojo con el `tinyInt1isBit` del
     Connector/J descrito en `CLAUDE.md`.
6. **Actualizar la sección 2** de este documento y, si el salto añadió o quitó una restricción, la
   sección 3.

### Si `--digests` falla sin que nadie haya cambiado el compose

Un digest que no coincide bajo un tag de versión concreta significa que el publicador re-empujó ese
tag. **No regenerar el lock para silenciarlo.** Comparar primero qué cambió:

```bash
docker pull <imagen>:<tag>
docker image inspect <imagen>:<tag> --format '{{index .RepoDigests 0}}'
```

Si el cambio es legítimo (rebuild por CVE de la imagen base, habitual en las oficiales de Docker
Hub), regenerar el lock con `--update` y dejar constancia en el commit. Si no hay explicación, es un
incidente de cadena de suministro y el lock es la evidencia de qué se corría antes.

## 7. Alcance

Esta matriz cubre el stack **local** de `docker-compose.yml`. Producción no usa estas imágenes: el
backend corre en ECS (`VetSoftwareIaC/modules/ecs_backend`) y la observabilidad la aporta Grafana
Cloud, según `docs/OBSERVABILIDAD_PROD_GRAFANA_S3.md`. El pin de versiones de ECS y de los
exportadores de producción se gobierna desde el repositorio de IaC, no desde aquí.
