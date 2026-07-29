# Perfiles de ejecución

VetSoftware usa exactamente tres perfiles Spring:

| Perfil | Dependencias | Observabilidad | Auditoría |
|---|---|---|---|
| `local` | Docker local | Collector, Loki, Tempo, Prometheus y Grafana locales | Outbox → Firehose de LocalStack → S3 de LocalStack |
| `dev` | Servicios remotos | OTLP directo a Grafana Cloud | Outbox → AWS Data Firehose remoto → S3 remoto |
| `prod` | Servicios administrados/remotos | OTLP directo a Grafana Cloud | Outbox → AWS Data Firehose → S3 Object Lock |

`application.yml` solo contiene configuración común. No contiene direcciones de DB, Redis,
S3, Firehose ni endpoints OTLP. Si no se selecciona un perfil se usa `local`.

## Local

Copiar el contrato local y levantar los contenedores:

```bash
cp deploy/env/local.env.example .env.local
# Definir GRAFANA_PASSWORD en .env.local: el compose ya no tiene fallback `admin` y aborta si falta.
docker compose --env-file .env.local up -d
```

Todos los puertos se publican en `127.0.0.1` (`LOCAL_BIND_HOST`), y Loki y Tempo no publican ninguno:
se consultan desde Grafana. Para alcanzarlos con `curl` hay un override de depuración, y para una
máquina compartida hay otro con TLS y autenticación en el receptor OTLP. Ver
`docs/SEGURIDAD_OBSERVABILIDAD.md`.

Iniciar el backend usando el mismo archivo de variables. Los valores predeterminados sirven
cuando Java corre directamente en el host. El compose levanta:

- MySQL 8;
- Redis 7;
- LocalStack con S3, IAM y Data Firehose;
- OpenTelemetry Collector, Loki, Tempo, Prometheus y Grafana;
- servicios auxiliares existentes como SonarQube.

El hook `docker/localstack/ready.d/01-create-bucket.sh` crea idempotentemente el bucket
`vetsoftware-local`, un rol IAM simulado y el delivery stream
`vetsoftware-audit-local`. Los eventos publicados por el worker aparecen bajo `audit/`.
LocalStack valida la integración y los reintentos, pero no sustituye una prueba de la
garantía WORM real de S3 Object Lock.

El compose fija LocalStack en la rama 3.x porque las imágenes 2026 requieren una cuenta y
`LOCALSTACK_AUTH_TOKEN`; para desarrollo individual se conserva así una emulación sin
credenciales ni suscripción externa.

Comprobaciones:

```bash
docker compose --env-file .env.local ps
docker compose --env-file .env.local exec localstack \
  awslocal firehose describe-delivery-stream \
  --delivery-stream-name vetsoftware-audit-local
docker compose --env-file .env.local exec localstack \
  awslocal s3 ls s3://vetsoftware-local/audit/ --recursive
```

## Dev

Copiar `deploy/env/dev.env.example` al gestor de secretos o configuración de la plataforma.
Este perfil no tiene fallbacks locales. Requiere:

- MySQL remoto mediante `DB_URL`, `DB_USERNAME` y `DB_PASSWORD`;
- Redis remoto mediante una URL completa `REDIS_URL` — usar `rediss://` cuando esté
  disponible;
- AWS real para S3 y Firehose, preferiblemente con IAM Role/workload identity;
- Grafana Cloud mediante los tres endpoints OTLP y `OTEL_EXPORTER_OTLP_HEADERS`;
- Resend, reCAPTCHA, CORS y URLs del frontend de desarrollo.

Dev debe usar bucket y delivery stream separados de producción. La plantilla
`deploy/aws/audit-object-lock.yml` puede desplegarse con otro nombre de stack, bucket y
`DeliveryStreamName`.

## Prod

Usar `deploy/env/prod.env.example` como contrato del deployment. El perfil:

- no admite overrides de endpoint para S3 o Firehose;
- usa `DefaultCredentialsProvider`, por lo que no necesita access keys cuando la plataforma
  entrega una identidad IAM;
- exige endpoints remotos para MySQL, Redis y Grafana Cloud;
- no escribe logs en archivos ni en consola;
- publica métricas, logs y trazas directamente por OTLP/HTTP;
- conserva el 100 % de trazas mientras `TRACING_SAMPLING=1.0`.

El validador de arranque rechaza dependencias que resuelvan a `localhost`, `127.0.0.1`,
`0.0.0.0`, `::1` o `host.docker.internal` en `dev` y `prod`.

## Credenciales OTLP

Copiar desde la tarjeta OpenTelemetry del stack Grafana Cloud:

```text
OTEL_EXPORTER_OTLP_METRICS_ENDPOINT=.../otlp/v1/metrics
OTEL_EXPORTER_OTLP_LOGS_ENDPOINT=.../otlp/v1/logs
OTEL_EXPORTER_OTLP_TRACES_ENDPOINT=.../otlp/v1/traces
OTEL_EXPORTER_OTLP_HEADERS=Authorization=Basic%20...
```

Los endpoints específicos deben incluir `/v1/metrics`, `/v1/logs` y `/v1/traces`.
No versionar el header de autorización.

## Consideración de disponibilidad

El envío directo a Grafana elimina Collector/Alloy de `dev` y `prod`, pero sus colas están en
memoria. Una interrupción prolongada de Grafana puede descartar telemetría operativa. La
auditoría no tiene ese riesgo: permanece en MySQL y se reintenta hasta que Firehose confirme.
