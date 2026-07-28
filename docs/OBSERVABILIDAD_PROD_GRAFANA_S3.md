# Producción: Grafana Cloud + Firehose + S3 Object Lock

Producción no instala Collector, Loki, Tempo, Prometheus, Redis ni MySQL en
el host de la aplicación. Las conexiones se entregan mediante
`deploy/env/prod.env.example`.

## Telemetría operativa

Micrometer y OpenTelemetry envían métricas, logs y trazas directamente al endpoint OTLP de
Grafana Cloud Brasil:

- `OTEL_EXPORTER_OTLP_METRICS_ENDPOINT`;
- `OTEL_EXPORTER_OTLP_LOGS_ENDPOINT`;
- `OTEL_EXPORTER_OTLP_TRACES_ENDPOINT`;
- `OTEL_EXPORTER_OTLP_HEADERS`.

Logback solo usa el appender OTLP en `prod`; no existe appender de consola o archivo. El
muestreo permanece en 100 % y los probes de Actuator se excluyen de las trazas.

## Auditoría durable

Los eventos de auditoría se insertan primero en `audit_event_outbox`. El worker:

1. reclama hasta 100 filas usando `FOR UPDATE SKIP LOCKED`;
2. libera la transacción antes de llamar AWS;
3. publica con `PutRecordBatch`;
4. procesa cada respuesta de Firehose de forma individual;
5. marca aceptados como `PUBLISHED` y reintenta fallidos con backoff y jitter;
6. elimina únicamente publicados con más de siete días.

Firehose agrupa los JSON como NDJSON GZIP y los entrega en S3. El bucket aplica retención
predeterminada Object Lock `COMPLIANCE`.

## Infraestructura AWS

```bash
aws cloudformation deploy \
  --region sa-east-1 \
  --stack-name vetsoftware-audit-prod \
  --template-file deploy/aws/audit-object-lock.yml \
  --capabilities CAPABILITY_NAMED_IAM \
  --parameter-overrides \
    BucketName=MI-BUCKET-AUDITORIA \
    RetentionDays=365 \
    ApplicationRoleName=vetsoftware-prod \
    DeliveryStreamName=vetsoftware-audit-prod
```

Verificar antes de habilitar tráfico:

```bash
aws s3api get-object-lock-configuration \
  --region sa-east-1 --bucket MI-BUCKET-AUDITORIA
aws firehose describe-delivery-stream \
  --region sa-east-1 --delivery-stream-name vetsoftware-audit-prod
```

El bucket debe indicar `COMPLIANCE` y el delivery stream debe estar `ACTIVE`.

## Servicios remotos

- MySQL: URL JDBC con TLS y credenciales desde el gestor de secretos.
- Redis: servicio administrado, preferiblemente `rediss://`.
- S3/Firehose: IAM Role o identidad de workload; no access keys permanentes.
- Resend y reCAPTCHA: claves inyectadas desde secretos.

## Alertas

Importar `deploy/grafana/audit-outbox-alerts.yml`. Las reglas cubren:

- filas `FAILED`;
- backlog con más de 15 minutos;
- errores de publicación a Firehose;
- ausencia de métricas;
- errores del exportador OTLP cuando estén disponibles como métricas.

La entrega OTLP directa usa colas en memoria. Ante una caída prolongada de Grafana se puede
perder telemetría operativa; los eventos de auditoría siguen protegidos por el outbox.
