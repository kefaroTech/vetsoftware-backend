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

## Alertas en Grafana Cloud

El stack local descrito arriba evalúa los ficheros `docker/prometheus-*.yml`. Los ambientes
`dev` y `prod` evalúan además sus propias reglas en el **ruler de Mimir** del stack de
Grafana Cloud de cada ambiente, sincronizadas con `mimirtool` desde
`VetSoftwareIaC/observability/mimir-rules/`. Esos ficheros son **gemelos traducidos** de los
locales, no copias: las métricas llegan por OTLP push y no por scrape, así que los timers van
en milisegundos, el selector es `job="mainvet/vetsoftware"` y los bordes `le` son enteros. Un
cambio en un fichero local debe evaluarse también en su gemelo (y viceversa); el detalle de la
traducción, lo que no se portó y por qué, está en el README de ese directorio. Producción
añade un heartbeat de ingesta (`VetSoftwareBackendTelemetryAbsent`) que sustituye a
`VetSoftwareBackendDown` en un mundo sin scrape; no se sincroniza a dev porque el apagado
programado del ambiente lo haría sonar cada noche.

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

## VetSoftwareSecurityTokenTableGrowth

La tabla indicada por `token_type` superó durante treinta minutos el umbral publicado en
`vetsoftware_security_tokens_growth_threshold`. Revise primero los logs del trabajo
`security.tokens.cleanup` y la métrica `tasks_scheduled_execution_seconds_count` con
`job_name="security.tokens.cleanup"`. Si el trabajo funciona pero alcanza su límite por ejecución,
ajuste con prudencia `TOKEN_CLEANUP_BATCH_SIZE` o `TOKEN_CLEANUP_MAX_BATCHES_PER_RUN`; si no hay
filas elegibles, el crecimiento corresponde a sesiones o solicitudes todavía vigentes y debe
investigarse antes de reducir la retención.

## VetSoftwareScheduledJobFailing

Solo existe en Grafana Cloud (`VetSoftwareIaC/observability/mimir-rules/vetsoftware-cloud-additions.yml`).
Un job programado acumula ejecuciones con `job_outcome` en `failure` o `error`: la variante
warning exige fallos sostenidos durante treinta minutos; la critical, que **todas** las
ejecuciones de las últimas dos horas hayan fallado sin un solo éxito — fallo determinista que
ningún ciclo posterior va a recuperar.

1. Identificar el trabajo con la etiqueta `job_name` de la alerta.
2. Buscar en Loki los logs del job y seguir el `trace_id` de una ejecución fallida en Tempo.
3. Distinguir fallo transitorio (dependencia caída, timeout puntual) de determinista (dato
   atascado, configuración inválida). Si la variante critical está activa, asumir determinista.
4. Un job que falla no tiene usuario delante que reintente: el trabajo pendiente se acumula en
   silencio mientras la alerta siga activa.

Caso conocido: `dian.pending.reconciliation` falla de forma sostenida por un documento
atascado (el documento 44). Ese patrón — mismo `job_name`, mismo error, cada ciclo — es un
dato envenenado, no un problema de infraestructura: se corrige el dato, no el job.

## VetSoftwareEmailSendFailing

Solo existe en Grafana Cloud (`VetSoftwareIaC/observability/mimir-rules/vetsoftware-cloud-additions.yml`).
El envío de correo es fire-and-forget: cada `email_outcome="failure"` es un correo que el
destinatario no recibió, aunque la petición HTTP del usuario respondiera 200. La variante
warning es tasa parcial (más del 10 % de fallos con volumen mínimo); la critical es fallo
total: hay fallos y **cero** éxitos durante media hora — determinista y sistémico.

1. Buscar en Loki el error del cliente de correo y el código de respuesta del proveedor.
2. Un 4xx del proveedor (401/403/422) es determinista: fallará el 100 % de los envíos hasta
   que alguien cambie configuración. Un 429/5xx/timeout es transitorio y aislado.
3. Caso vivo que motivó la alerta: Resend responde `403` porque el dominio remitente no está
   verificado. El arreglo es **verificar el dominio `kefaro.tech` en Resend** (o cambiar el
   remitente a un dominio ya verificado), no reintentar.
4. Tras corregir, confirmar que aparecen envíos con `email_outcome="success"` y que la
   variante critical se resuelve sola.

## VetSoftwareBackendRestartLoop

Solo existe en Grafana Cloud (`VetSoftwareIaC/observability/mimir-rules/vetsoftware-cloud-additions.yml`).
El proceso se reinició tres o más veces en una hora. Sustituye por otra vía lo que el stack
local cubría con `VetSoftwareBackendDown`: con ingesta push por OTLP no existe `up`, así que
la señal no es «el scrape falla» sino «el arranque se repite».

**Un crash loop no se parece a una caída, se parece a lentitud.** Entre reinicio y reinicio el
backend sí responde y sí emite telemetría, de modo que el heartbeat no lo detecta y las
peticiones que caen en la ventana de arranque fallan sin patrón claro.

1. Revisar los eventos de la task de ECS: motivo de parada (`OutOfMemoryError`, exit code,
   health check fallido) antes que cualquier hipótesis de código.
2. Contrastar `jvm_memory_used_bytes{area="heap"}` contra su máximo en la hora previa: un OOM
   kill deja el heap subiendo hasta el corte.
3. Si el reinicio coincide con un despliegue, comparar `service_version`: puede ser una imagen
   que no arranca, no una fuga de memoria.
4. El umbral (3 en una hora) descarta por construcción el arranque legítimo de dev tras el
   apagado programado de las 20:00, que produce un único cambio diario.

## VetSoftwareAuthFailureSpike

Solo existe en Grafana Cloud (`VetSoftwareIaC/observability/mimir-rules/vetsoftware-cloud-additions.yml`).
Más de 0,5 rechazos por segundo sostenidos diez minutos en `/auth/login/*`. El selector es
preciso a propósito: en este backend los 401/403 solo aparecen en esas dos rutas, así que la
alerta no arrastra el ruido de los tokens de acceso caducados del ciclo de refresco del front.

1. Agrupar en Loki los eventos `event="login_failure"` del canal AUDIT por `client.ip` y
   `user_agent.original`.
2. **Una sola IP con muchos identificadores distintos** es enumeración de códigos de empleado.
   **Muchas IP contra un solo identificador** es fuerza bruta dirigida. **Una IP con un solo
   identificador repetido** suele ser un cliente mal configurado en bucle, no un ataque.
3. Comprobar si el rate limiting está actuando (respuestas 429). Si no aparece ninguna,
   verificar la salud de Valkey: bucket4j se apoya en él y una caché degradada puede dejar el
   control abierto — ver `VetSoftwareValkeyCommandsFailing`.
4. No bloquear una IP sin el paso 2: un NAT corporativo concentra usuarios legítimos.

## VetSoftwareValkeyCommandsFailing

Solo existe en Grafana Cloud (`VetSoftwareIaC/observability/mimir-rules/vetsoftware-cloud-additions.yml`).
El cliente Lettuce registra comandos con `error` distinto de `none` durante cinco minutos.

**Valkey no es solo caché.** Sostiene la caché de permisos y sedes *y* el rate limiting de
bucket4j. Degradado, el control de fuerza bruta deja de ser fiable justo cuando más falta hace.

1. Estado de la instancia ElastiCache: CPU, memoria, evictions, conexiones.
2. Conectividad desde la task de ECS (grupos de seguridad, cambio de endpoint).
3. Revisar el valor concreto de la etiqueta `error` en la serie
   `lettuce_milliseconds_count{error!="none"}`: distingue timeout de conexión rechazada.
4. Mientras dure, asumir que la autorización responde más lento y que el rate limiting puede
   no estar aplicándose.

## VetSoftwareValkeyLatencyHigh

Solo existe en Grafana Cloud (`VetSoftwareIaC/observability/mimir-rules/vetsoftware-cloud-additions.yml`).
El p99 de los comandos Lettuce supera 50 ms durante diez minutos. Valkey está en el camino
caliente de la autorización: su latencia se suma a la de **cada** petición autenticada.

Revisar CPU y memoria de ElastiCache, la tasa de eviction y el tamaño de las entradas
cacheadas. Correlacionar con `VetSoftwareHttpP95LatencyHigh`: si ambas están activas, la causa
raíz probable es la caché, no la aplicación.

## VetSoftwareIngestionNearLimit

**No es una regla del ruler**: es una alerta Grafana-managed
(`VetSoftwareIaC/observability/grafana-managed/vetsoftware-cost-guard.yml`), porque consulta
métricas de uso que viven en otro tenant. Ver el README de ese directorio.

Las series activas del stack superan el 80 % del límite del plan. **Al llegar al 100 %,
Grafana Cloud rechaza la ingesta**: se pierde telemetría en silencio y con ella la capacidad de
ver cualquier otro incidente — incluidas todas las demás alertas, que se quedan sin datos que
evaluar.

1. Identificar la familia culpable:
   `topk(10, count by (__name__) ({__name__=~".+"}))`.
2. Dentro de esa familia, buscar la etiqueta que explotó:
   `count by (<etiqueta>) (<metrica>)`.
3. Causa más probable: una etiqueta sin acotar. `BusinessMetricCardinalityFilter` solo cubre el
   prefijo `vetsoftware.business.`; las familias `email.*` y `lettuce.*` quedan fuera de esa
   allowlist y llevan una etiqueta `error` derivada del nombre de la excepción.
4. El arreglo es acotar la etiqueta en el código (allowlist o `error.type` normalizado), no
   subir el plan.

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
