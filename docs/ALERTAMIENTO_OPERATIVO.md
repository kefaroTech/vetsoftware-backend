# Alertamiento operativo de VetSoftware

## Objetivo

Este runbook describe las señales, rutas y acciones del stack local de alertamiento:

```text
Backend y componentes
        ↓ métricas
    Prometheus
        ↓ alertas
   Alertmanager
        ↓ correo local
      Mailpit
```

Prometheus decide cuándo una condición es anómala. Alertmanager agrupa, deduplica,
inhibe, silencia y entrega notificaciones. Mailpit permite comprobar localmente los
correos en `http://localhost:8025` sin enviar mensajes reales.

## Configuración por ambiente

El entorno local monta `docker/alertmanager.yml`, que entrega los avisos a Mailpit.

Producción debe suministrar un archivo externo mediante:

```text
ALERTMANAGER_CONFIG_PATH=./ruta-segura/alertmanager-prod.yml
```

Ese archivo puede configurar correo, Amazon SNS, PagerDuty, Slack, Teams, Discord u
otro receptor soportado. No se deben guardar tokens, contraseñas ni webhooks en Git.
Los secretos deben referenciarse mediante archivos o el gestor de secretos del
entorno de despliegue.

## Validación

```powershell
docker run --rm `
  -v "${PWD}/docker:/workspace" `
  --entrypoint /bin/promtool `
  prom/prometheus:v3.13.1 `
  check rules /workspace/prometheus-slo-rules.yml /workspace/prometheus-slo-alerts.yml /workspace/prometheus-business-alerts.yml /workspace/prometheus-platform-alerts.yml

docker run --rm `
  -v "${PWD}/docker:/workspace" `
  --entrypoint /bin/promtool `
  prom/prometheus:v3.13.1 `
  test rules /workspace/tests/prometheus-alerts.test.yml /workspace/tests/prometheus-slo.test.yml

docker run --rm `
  -v "${PWD}/docker/alertmanager.yml:/etc/alertmanager/alertmanager.yml:ro" `
  --entrypoint /bin/amtool `
  prom/alertmanager:v0.32.1 `
  check-config /etc/alertmanager/alertmanager.yml
```

## Acciones comunes

1. Confirmar si la alerta sigue activa en Alertmanager.
2. Consultar el panel y la serie que originaron la alerta.
3. Correlacionar el intervalo con logs en Loki y trazas en Tempo.
4. Corregir la causa, no la señal.
5. Verificar que Alertmanager entregue la notificación de resolución.
6. Crear un silencio con vencimiento solamente durante una intervención controlada.

## VetSoftwareBackendDown

Compruebe el proceso Java, el puerto `8080`, el perfil activo, las credenciales de
Actuator y los logs de arranque. Si la aplicación está disponible pero el target no,
revise conectividad desde Prometheus y el `metrics_path`.

## VetSoftwareObservabilityTargetDown

Use la etiqueta `job` para identificar Alertmanager, Grafana, Loki, Collector, Redis
o Tempo. Compruebe el estado del contenedor, su healthcheck, red interna y endpoint
`/metrics`.

## VetSoftwareHttp5xxRateHigh

Agrupe `http_server_requests_seconds_count` por `uri`, `method`, `status` y
`exception`. Busque la traza asociada y el error correlacionado en Loki. La alerta
exige al menos veinte solicitudes para evitar ruido con muestras pequeñas.

## VetSoftwareHttpP95LatencyHigh

Identifique las rutas con mayor contribución a los buckets HTTP. Revise spans JDBC,
llamadas externas, saturación de HikariCP, pausas de GC y CPU.

## VetSoftwareHttpP99LatencyCritical

Trate el p99 sostenido por encima de dos segundos como degradación crítica. Compare
las rutas lentas con trazas, consultas JDBC y dependencias externas.

## VetSoftwareJvmHeapHigh

Revise crecimiento sostenido del heap, frecuencia de GC y dumps controlados. No
aumente memoria sin descartar primero una fuga o una caché sin límite.

## VetSoftwareProcessCpuHigh

Correlacione CPU con volumen HTTP, GC, tareas programadas, consultas y generación de
PDF. Capture perfiles solamente con herramientas y permisos aprobados.

## VetSoftwareJvmGcOverheadHigh

Compruebe presión de heap, tasa de asignación y pausas. Un overhead sostenido puede
preceder degradación severa aunque todavía exista memoria disponible.

## VetSoftwareJvmThreadCountHigh

Compare el número actual con la línea base y agrupe los estados de hilo. Busque
ejecutores sin límites, bloqueos, tareas que no terminan o creación continua de hilos.

## VetSoftwareDatabasePoolSaturated

Revise consultas lentas, transacciones largas, fugas de conexiones, tamaño del pool
y capacidad real de MySQL. Aumentar el pool sin medir la base puede empeorarla.

## VetSoftwareDatabaseConnectionPending

Busque transacciones o consultas que retengan conexiones. Compare conexiones
activas, máximas y tiempo de adquisición.

## VetSoftwareDatabaseConnectionTimeouts

Trate cualquier incremento como pérdida real de capacidad. Revise disponibilidad de
MySQL, conectividad, credenciales, saturación y timeouts.

## VetSoftwareRedisDown

Compruebe el contenedor Redis, persistencia, red interna y respuesta a `PING`. Evalúe
el impacto en rate limiting, caché y demás consumidores antes de reiniciar.

## VetSoftwareRedisMemoryHigh

Revise `maxmemory`, política de expulsión, TTL y claves de mayor tamaño. La regla solo
se activa cuando Redis tiene un límite de memoria explícito.

## VetSoftwareRedisRejectedConnections

Revise `maxclients`, conexiones activas y clientes que no liberan recursos.

## VetSoftwareOtelLogExportFailing

Revise la disponibilidad de Loki, respuestas HTTP del exporter, límites de memoria,
batch y reintentos del Collector.

Con los reintentos activos (OBS-027) esta alerta **no significa pérdida**: significa que los envíos
llevan minutos fallando y la cola en disco está creciendo. Hay margen para intervenir antes de que se
llene. Si llega a llenarse, salta `VetSoftwareOtelQueueDroppingTelemetry`, que sí es pérdida
consumada.

## VetSoftwareOtelQueueDroppingTelemetry

**Pérdida de datos en curso.** La cola de envío se llenó y el Collector está descartando. Ya no basta
con esperar el reintento.

1. Averiguar por qué el destino no drena: `docker logs vetsoftware_loki` / `vetsoftware_tempo`,
   y si están vivos, si están aplicando rate limiting.
2. Si el destino tarda en recuperarse, subir `queue_size` en `docker/otel-collector.yml` compra
   tiempo a cambio de disco.
3. La cantidad perdida está en `otelcol_exporter_enqueue_failed_log_records` y
   `otelcol_exporter_enqueue_failed_spans`.

## VetSoftwareOtelQueueNearCapacity

Indicador adelantado: la cola pasa del 80 %. Nadie ha perdido nada todavía, pero el destino no drena
al ritmo de entrada. Es el momento de intervenir; cuando salte
`VetSoftwareOtelQueueDroppingTelemetry` ya habrá pérdida.

## VetSoftwareOtelTraceExportFailing

Revise conectividad OTLP entre Collector y Tempo, límites de Tempo y cola del
exporter.

## VetSoftwareOtelReceiverRefusedTelemetry

Compruebe memoria del Collector, volumen entrante, tamaño de lotes y formato OTLP.

## VetSoftwareLokiDiscardingLogs

Agrupe `loki_discarded_samples_total` por `reason`. Corrija límites, timestamps o
cardinalidad según la causa; no suprima la alerta sin explicar la pérdida.

## VetSoftwareLokiPanics

Conserve logs y versión exacta de la imagen, revise recursos y busque el defecto
correspondiente antes de reiniciar.

## VetSoftwareTempoDiscardingSpans

Agrupe `tempo_discarded_spans_total` por `reason`. Revise rate limiting, tamaño de
trazas e identificadores inválidos.

## VetSoftwareAlertmanagerNotificationFailures

Compruebe el receptor, DNS, TLS, autenticación, límites y conectividad. En local,
verifique Mailpit; en producción, pruebe el canal secundario.

## VetSoftwarePrometheusRuleEvaluationFailures

Inspeccione el error de evaluación y la regla afectada. Valide cambios con
`promtool check rules` y `promtool test rules` antes de desplegarlos.

## VetSoftwarePrometheusNotificationFailures

Compruebe que Alertmanager esté listo, que Prometheus use el destino interno correcto
y que su cola de notificaciones no esté creciendo.

## VetSoftwareAuditChainBroken

Un evento de auditoría fue **suprimido o alterado en la base de datos**. Es un incidente de
seguridad, no un problema de capacidad: implica que alguien tiene acceso de escritura a la base de
producción.

**No escribir sobre `audit_event_outbox`.** Cualquier escritura destruye evidencia. El procedimiento
completo está en la sección 8 de `docs/AUDITORIA_INTEGRIDAD.md`; en resumen: acotar el tramo con
`audit_chain_failure_sequence`, comparar contra el último checkpoint del bucket WORM y reconstruir la
secuencia real desde el archivo inmutable.

## VetSoftwareAuditChainNotVerified

La métrica sigue en `-1`: el verificador no ha completado ninguna pasada desde el arranque. Revisar
los logs del trabajo `audit.chain.verify` y el estado del pool de conexiones. Mientras esta alerta
esté activa, **no hay evidencia de que la cadena esté sana** — la ausencia de
`VetSoftwareAuditChainBroken` no significa nada.

## VetSoftwareAuditChainUnsequencedBacklog

Hay eventos insertados sin posición en la cadena. Como solo se publica lo secuenciado, **esos eventos
no están llegando al archivo inmutable**. Causas probables: el publicador no corre
(`publisher-enabled`), o `sequence-batch-size` no alcanza para el ritmo de inserción.

## VetSoftwareAuditChainCheckpointStale

La cabeza de la cadena lleva horas sin anclarse en almacenamiento inmutable. Dos consecuencias: el
tramo nuevo no tiene ancla que impida recalcularlo, y la depuración de la outbox queda bloqueada
(por diseño), así que la tabla crecerá. Revisar el trabajo `audit.chain.checkpoint`.

## VetSoftwareSecurityTokenTableGrowth

La tabla indicada por `token_type` superó durante treinta minutos el umbral publicado en
`vetsoftware_security_tokens_growth_threshold`. Revise primero los logs del trabajo
`security.tokens.cleanup` y la métrica `tasks_scheduled_execution_seconds_count` con
`job_name="security.tokens.cleanup"`. Si el trabajo funciona pero alcanza su límite por ejecución,
ajuste con prudencia `TOKEN_CLEANUP_BATCH_SIZE` o `TOKEN_CLEANUP_MAX_BATCHES_PER_RUN`; si no hay
filas elegibles, el crecimiento corresponde a sesiones o solicitudes todavía vigentes y debe
investigarse antes de reducir la retención.

## Alertas de SLO

Las seis alertas siguientes no vigilan un umbral técnico sino el **consumo del presupuesto de
error** definido en `docs/SLO_VETSOFTWARE.md`. Todas llevan las etiquetas `slo` y `sli`, que
identifican la operación crítica y el indicador afectado.

Los tres niveles de burn rate describen el mismo incidente a distinta velocidad, así que
Alertmanager inhibe los niveles más lentos cuando el rápido está activo. Recibir `SloSlowBurn`
sin `SloFastBurn` significa degradación sostenida y moderada, no un pico.

El dashboard de referencia es **VetSoftware — SLO y presupuesto de error**
(`uid: vetsoftware-slo`).

## VetSoftwareSloFastBurn

A este ritmo el presupuesto de 30 días se agota en poco más de dos días. Es respuesta inmediata.

1. Abrir el dashboard de SLO y filtrar por la etiqueta `slo` de la alerta.
2. Si `sli=availability`, agrupar `http_server_requests_seconds_count` por `uri`, `status` y
   `exception` para localizar el origen; si `sli=latency`, revisar los buckets de las rutas del
   journey, spans JDBC y saturación de HikariCP.
3. Para `slo=dian-transmission`, distinguir entre rechazo del proveedor y contingencia: revisar
   `vetsoftware_business_dian_transmissions_total` por `result` y `origin`. Una contingencia
   masiva suele ser indisponibilidad del proveedor, no un defecto propio.
4. Correlacionar la ventana con Loki y Tempo.
5. Corregir la causa. **No** subir el objetivo del SLO para silenciar la alerta.

## VetSoftwareSloSlowBurn

Degradación sostenida: el presupuesto se agotaría en unos cinco días. Se atiende el mismo día,
sin interrumpir la guardia. Misma investigación que `SloFastBurn`, con más margen para buscar
correlación con un despliegue reciente o un cambio de volumen.

## VetSoftwareSloTicketBurn

Consumo elevado pero lento: el presupuesto duraría unos diez días. Corresponde trabajo
planificado dentro del ciclo. Si se repite ciclo tras ciclo sin degradación percibida por
usuarios, el candidato a revisión es el **objetivo**, no el código — seguir el procedimiento de
la sección "Revisión de objetivos" de `docs/SLO_VETSOFTWARE.md`.

## VetSoftwareSloErrorBudgetLow

Queda menos del 25 % del presupuesto de la ventana de 30 días. No hay incidente activo
necesariamente: es una señal de gobierno. Aplicar la política de la sección
"Gobierno de despliegue" de `docs/SLO_VETSOFTWARE.md` y revisar si hay trabajo de fiabilidad
pendiente en ese dominio.

## VetSoftwareSloErrorBudgetExhausted

El SLO está incumplido en la ventana rodante. La ventana se recupera por sí sola al desplazarse,
así que la alerta puede permanecer activa varios días después de resolver la causa: eso es
correcto y por eso su reenvío es cada 24 horas y no cada 30 minutos.

1. Confirmar en el dashboard cuánto excede el gasto (valores negativos del presupuesto).
2. Aplicar el congelamiento correspondiente del dominio afectado.
3. Registrar la causa raíz. Un presupuesto agotado sin causa raíz documentada suele indicar un
   objetivo mal calibrado.

## VetSoftwareSloSeriesAbsent

Un objetivo está declarado pero no produce serie de eventos: **ese SLO no se está midiendo**.
Es la alerta más importante del conjunto, porque su ausencia haría que un SLO roto pareciera
sano en lugar de fallar de forma visible.

Causas por probabilidad:

1. El umbral de latencia no coincide con ningún borde `le` publicado. Comprobar los bordes reales
   con `count by (le) (http_server_requests_seconds_bucket)` y contrastarlos con
   `management.metrics.distribution.slo` en `application.yml`.
2. Cambió una ruta incluida en el selector `uri` de la regla de grabación.
3. Se renombró una métrica de negocio o un valor de la etiqueta `result`.
4. La operación simplemente no se ejecutó. En local o en desarrollo esto es legítimo y la alerta
   se silencia con vencimiento; en producción, una operación crítica sin tráfico en 24 horas es
   en sí misma una señal que merece explicación.

Revisar el selector de la regla **antes** que el objetivo.

## Referencias oficiales

- https://sre.google/workbook/alerting-on-slos/
- https://prometheus.io/docs/alerting/latest/alertmanager/
- https://prometheus.io/docs/alerting/latest/configuration/
- https://prometheus.io/docs/prometheus/latest/configuration/unit_testing_rules/
- https://opentelemetry.io/docs/collector/internal-telemetry/
- https://grafana.com/docs/loki/latest/operations/meta-monitoring/
- https://grafana.com/docs/tempo/latest/operations/monitoring/
