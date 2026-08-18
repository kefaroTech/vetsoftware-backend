# Producción: Grafana Cloud

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

## Auditoría

Los eventos de auditoría salen **únicamente** por el logger `AUDIT`, que viaja en el mismo
pipeline de logs que el resto de la aplicación hasta Loki.

**No hay archivo inmutable.** El outbox que persistía cada evento en `audit_event_outbox`, lo
encadenaba por hash y lo publicaba a Firehose para archivarlo en un bucket con S3 Object Lock
`COMPLIANCE` se retiró (changeset `224_drop_audit_outbox.xml`). Con él se fueron la plantilla de
CloudFormation `deploy/aws/audit-object-lock.yml` y las reglas de alerta del outbox.

Las tres consecuencias que hay que tener presentes antes de prometer algo a un cliente o a un
auditor:

- **El rastro es mutable.** Quien pueda escribir en Loki o en su ruta de ingesta puede alterarlo,
  y nada lo detecta. Antes, la cadena de hash convertía una alteración en una alerta.
- **La retención es la de Loki**, no una elegida por cumplimiento. No hay retención `COMPLIANCE`
  ni bloqueo de borrado.
- **No hay no repudio.** ASVS V7, NIST SP 800-53 AU-9 e ISO/IEC 27001 A.8.15 piden protección de
  los registros frente a modificación; hoy no se cumple por diseño, no por descuido.

Si aparece una obligación de conservación o de evidencia —DIAN, Ley 1581, un contrato con un
cliente grande— hay que reponer un destino durable. El historial de git conserva la
implementación completa: el mecanismo funcionaba y está probado, se retiró por proporcionalidad,
no por defectuoso.

## Servicios remotos

- MySQL: URL JDBC con TLS y credenciales desde el gestor de secretos.
- Redis: servicio administrado, preferiblemente `rediss://`.
- S3: IAM Role o identidad de workload; no access keys permanentes.
- Resend y reCAPTCHA: claves inyectadas desde secretos.

## Alertas

Las reglas de plataforma viven en `docker/prometheus-platform-alerts.yml` y las de SLO en
`docker/prometheus-slo-alerts.yml`, con runbooks en `docs/ALERTAMIENTO_OPERATIVO.md`.

La entrega OTLP directa usa colas en memoria. Ante una caída prolongada de Grafana **se puede
perder telemetría, y ahora también auditoría**: al ser el log su único destino, un descarte del
pipeline se lleva por delante el evento de auditoría sin dejar rastro. Antes esa pérdida solo
afectaba a la telemetría operativa porque el outbox sostenía el evento en MySQL hasta confirmar
la entrega.
