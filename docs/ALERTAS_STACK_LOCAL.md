# Alertas del stack local de VetSoftware

Runbook de las alertas que evalúa el **stack Docker local**:
`docker/prometheus-platform-alerts.yml`, `docker/prometheus-business-alerts.yml` y
`docker/prometheus-slo-alerts.yml`, con entrega por Alertmanager a Mailpit. Este plano es un
banco de pruebas de expresiones: **no pagina a nadie**.

> **Las alertas de `dev` y `prod` son otro plano y tienen otro runbook:**
> <https://github.com/kefaroTech/vetsoftware-infrastructure/blob/develop/docs/ALERTAMIENTO_OPERATIVO.md>
>
> Viven en `VetSoftwareIaC/observability/`, se evalúan en Grafana Cloud y sus métricas llegan por
> push OTLP, no por scrape: los timers van en **milisegundos** y el selector es
> `job="mainvet/vetsoftware"`. **Copiar una consulta de un documento al otro devuelve vacío**, y
> un resultado vacío se confunde con "no hay problema". Las reglas locales tienen gemelos allí
> (ver la cabecera `GEMELO CLOUD` de cada fichero `docker/prometheus-*.yml`); las alertas que
> solo existen en Cloud se documentan **solo** en ese runbook.

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

**Unidad única.** Todo este documento habla la lengua del scrape local de Prometheus: los timers
son de la familia `_seconds_`. Una consulta de aquí que mencione milisegundos es un error de este
documento y debe reportarse; esa familia pertenece al plano de Grafana Cloud. El job
`observability-rules` de CI falla si aparece.

Prometheus decide cuándo una condición es anómala. Alertmanager agrupa, deduplica,
inhibe, silencia y entrega notificaciones. Mailpit permite comprobar localmente los
correos en `http://localhost:8025` sin enviar mensajes reales.

## Configuración por ambiente

El entorno local monta `docker/alertmanager.yml`, que entrega los avisos a Mailpit.

`ALERTMANAGER_CONFIG_PATH` permite apuntar este mismo stack a otro fichero de configuración
(correo real, Amazon SNS, PagerDuty, Slack, Teams o Discord) cuando se quiere ensayar un canal de
entrega distinto:

```text
ALERTMANAGER_CONFIG_PATH=./ruta-segura/alertmanager-local-alterno.yml
```

No se deben guardar tokens, contraseñas ni webhooks en Git: los secretos se referencian mediante
archivos o el gestor de secretos del entorno.

**La notificación de `dev` y `prod` no pasa por este Alertmanager.** Se define en las políticas de
notificación de Grafana Cloud, en el otro plano, y su runbook es el enlazado en la cabecera.

## Validación

Las dos primeras comprobaciones ya no dependen de que alguien se acuerde: el job
`observability-rules` de `.github/workflows/ci.yml` las ejecuta en cada pull request, y además
verifica que **toda anotación `runbook:` de `docker/prometheus-*.yml` resuelva a un encabezado de
este documento** y que no aparezcan unidades del otro plano. Un enlace de runbook roto rompe el
build, no la guardia.

En local:

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

## VetSoftwareEntitlementResolutionEmpty

Una empresa se quedó **sin ninguna fila en `company_entitlements`**. Sus empleados reciben 403 en
todos los endpoints, sin ningún mensaje que lo explique: parece un problema de permisos del
usuario y no lo es.

**Qué empresa es no está en la alerta y es deliberado.** Etiquetar por empresa multiplicaría cada
serie por las 500 clínicas del plan, así que la empresa viaja por el registro y por el span, no por
la métrica. Se obtiene del `WARN` de `JpaEntitlementEffectivePermissionResolver`, que la nombra en
el mensaje y la lleva en `actor.companyId`. Ojo con la consulta: **desde agosto de 2026 los
registros van por CloudWatch/Firehose y ya no llevan `trace_id` ni `scope_name` como etiquetas de
Loki**; los atributos están dentro del cuerpo del mensaje, así que hay que filtrar con `| json`
sobre el contenido y no por etiqueta, o la consulta devuelve vacío y parece que la tubería murió.

**Cómo se arregla**: ejecutar `RecalculateCompanyEntitlementsUseCase` para esa empresa
(`POST /company-entitlements/recalculate` con principal SYSTEM). Es idempotente: borra y reinserta
las filas derivándolas del contrato vigente.

**Antes de cerrar**, mirar por qué se quedó sin filas: lo normal es que un cambio de contrato no
llamara al recálculo, y ahí la métrica que lo dice es
`vetsoftware_business_subscription_entitlement_recalculations_total{result="failed"}`.

## VetSoftwareEntitlementContractRestricted

Un empleado resolvió permisos efectivos y **todos sus permisos base cayeron fuera de lo que el
contrato de su empresa concede**: se queda sin una sola authority y recibe el mismo 403 universal
que el caso anterior, pero por otro motivo.

**No es un caso legítimo de "bajó de plan".** Un downgrade correcto retira módulos y deja al
empleado con el resto; que se quede con cero significa que su rol entero dependía de algo que ya no
se paga. Las dos causas posibles:

1. **El contrato perdió una línea que sí paga.** Es la carrera que abre la capa de límites del
   modelo: el recálculo borra y reinserta la tabla entera sin tomar el candado del contrato, así
   que un otrosí confirmado en medio reinserta sin la línea nueva. Se comprueba cruzando
   `subscription_items` vigentes contra `company_entitlements`; el arreglo es volver a recalcular.
2. **Un downgrade dejó roles asignados que ya no cubre.** Aquí el arreglo es de negocio —reasignar
   el rol del empleado— y no técnico.

**El umbral es cero y no una línea base medida, a propósito.** Si esto resulta ser rutinario, la
respuesta correcta es corregir el flujo de downgrade para que reasigne el rol, **no** subir el
umbral hasta que la alerta deje de sonar: eso convertiría un cliente sin acceso en ruido tolerado.

## VetSoftwareScheduledJobOverdue

El barrido nombrado por `job_name` **no ha terminado bien dentro de su ventana**. Han pasado más de
1,25 intervalos esperados desde su último final correcto.

**No es lo mismo que un fallo.** `VetSoftwareScheduledJobFailing` vigila los barridos que fallan
—cuenta `job_outcome` de fallo—; esta vigila los que **no se ejecutan**, que es el caso que no deja
ninguna otra señal: ni contador, ni log, ni serie que cambie. Un `partial_failure` **sí** sella el
heartbeat a propósito, porque el barrido corrió; si esta alerta suena, no llegó a ejecutarse.

**Horas acordadas** (zona `America/Bogota`, declaradas en `ScheduledJobCatalog`; cambiarlas es
cambiar el contrato del que cuelga esta alerta):

| `job_name` | cron | hora | ¿una sola réplica? |
|---|---|---|---|
| `subscription.lifecycle` | `0 10 3 * * *` | 03:10 | sí |
| `quote.expiration` | `0 25 3 * * *` | 03:25 | sí |
| `subscription.dunning` | `0 40 3 * * *` | 03:40 | sí |
| `usage.reconciliation` | `0 10 4 * * *` | 04:10 | sí |
| `security.tokens.cleanup` | `0 20 * * * *` | cada hora, :20 | sí |
| `dian.contingency.retry` | `0 15 2,14 * * *` | 02:15 y 14:15 | no (lease por lote) |
| `dian.pending.reconciliation` | `0 30 2,14 * * *` | 02:30 y 14:30 | no (lease por lote) |
| `dian.delivery.retry` | `0 45 2,14 * * *` | 02:45 y 14:45 | no (lease por lote) |

Los tres de negocio corren de madrugada por una razón medida, no estética: el servidor son dos
núcleos y los créditos de CPU son el primer techo, así que un barrido de facturación a media mañana
compite con las clínicas que están atendiendo. A las 03:10 no compite con nadie.

**Qué mirar, en este orden**: (1) ¿el proceso está vivo? — si no, la causa es otra alerta;
(2) ¿arrancó el scheduler? — el arranque registra la programación, y un `@Scheduled` que no se
registró no deja más rastro; (3) el log del job por `job.name`, que **desde agosto de 2026 viaja
dentro del cuerpo del mensaje y no como etiqueta de Loki**; (4) `tasks_scheduled_execution_*` con
ese `job_name`, para ver si hubo ejecución con desenlace de fallo — si la hay, el problema es de la
otra alerta y esta es consecuencia.

**Ojo con el reinicio**: el heartbeat se inicializa al arranque del proceso, así que un despliegue
reinicia el reloj de esta alerta. Eso es correcto —tras un despliegue el barrido vuelve a tener su
ventana completa— pero significa que **un contenedor que se reinicia continuamente puede tapar un
barrido que nunca corre**. Si esta alerta va y viene, mirar primero la estabilidad del proceso.

## VetSoftwareScheduledJobMultipleReplicas

Más de un proceso está publicando el heartbeat del mismo barrido, y ese barrido **exige una sola
réplica**.

**Por qué importa hoy y no importaba ayer.** Los cinco barridos con `single_writer="true"`
recorren su tabla con un cursor y no arbitran nada entre réplicas. Mientras usaban `fixedDelay`, dos
tareas se escalonaban por accidente y el solape era intermitente; con `cron` arrancan **a la vez**,
así que el solape pasa a ser seguro. Lo único que lo impide hoy es que el servicio corre con una
sola tarea de ECS — una propiedad del despliegue, no del código.

**Qué se rompe si se ignora**: el barrido de facturación emite los cargos del cierre de mes dos
veces (`subscription_charges` es la única tabla del bloque sin llave antiduplicados), y el de
cobranza degrada dos veces al mismo moroso. El cliente lo descubre en su factura del mes siguiente.

**Las dos salidas, y solo hay dos**: volver a una sola tarea, o implementar el candado distribuido
antes del siguiente cierre de mes. Los tres barridos DIAN **no** aparecen aquí porque reclaman su
lote en exclusiva con `DianJobLeasePort` y toleran N réplicas; ese es el patrón a copiar.

## Alertas de negocio

Las cuatro alertas siguientes viven en `docker/prometheus-business-alerts.yml` y no vigilan la
plataforma sino el **resultado de una operación de negocio**. Todas llevan la etiqueta `domain`.

Sus etiquetas están acotadas por `BusinessMetricCardinalityFilter`: un valor fuera de la allowlist
se descarta antes de llegar al registro. Eso significa que **la serie nunca identifica el
documento, el producto ni la empresa concretos**; esa identidad se busca en Loki y en Tempo, no en
la métrica.

## VetSoftwareDianContingencyRateHigh

Más del 5 % de las transmisiones a la DIAN de los últimos 15 minutos terminó en contingencia, con
al menos 10 transmisiones en la ventana. La guarda de volumen evita que 1 de cada 3 dispare la
alerta.

Contingencia no es pérdida: el documento queda registrado para reintento posterior, y por eso esto
es `warning` y no `critical`. Lo que se pierde si se ignora es el **plazo fiscal**.

1. Desglosar `vetsoftware_business_dian_transmissions_total` por `result` y `origin`. Con
   `origin="retry"` u `origin="reconciliation"` dominando, el reintento ya está actuando.
2. Distinguir las dos poblaciones: una contingencia masiva y simultánea suele ser indisponibilidad
   del proveedor (transitoria, se recupera sola); una tasa sostenida con volumen bajo apunta a
   documentos concretos que fallan siempre, que es un problema de dato.
3. Correlacionar la ventana en Loki y seguir el `trace_id` de una transmisión fallida en Tempo.
4. Comprobar que el backlog drena después: si no lo hace, saltará
   `VetSoftwareDianBacklogOlderThanOneHour`, que ya no es transitorio.

## VetSoftwareDianBacklogOlderThanOneHour

Hay al menos un documento con más de una hora sin resolver
(`vetsoftware_business_dian_backlog_documents{age="gt_1h"}`). La contingencia dejó de ser
transitoria: **nadie está reintentando con éxito**.

1. Confirmar que el trabajo de reconciliación corre y con qué resultado: la métrica
   `tasks_scheduled_execution_seconds_count` filtrada por su `job_name`, y sus logs en Loki.
2. Un trabajo que corre y falla en cada ciclo con el mismo error es un **dato envenenado**, no un
   problema de infraestructura: se corrige el dato, no el trabajo. Caso observado en `dev`: un
   único documento atascado bastó para que `dian.pending.reconciliation` fallara de forma sostenida
   ciclo tras ciclo.
3. Un trabajo que **no corre** deja el backlog creciendo en silencio; comprobar primero que la
   aplicación esté viva y el planificador activo.
4. Esta métrica es un gauge de snapshot: si `VetSoftwareBusinessMetricsSnapshotStale` está activa a
   la vez, el valor que se está leyendo puede ser antiguo. Resolver esa primero.

## VetSoftwareInventoryInsufficientStockRateHigh

Más del 5 % de los movimientos de inventario de los últimos 15 minutos se rechazó por existencias
insuficientes, con al menos 10 movimientos en la ventana.

**Esto no es un fallo del sistema**: el sistema hizo exactamente lo que debía. Es una señal de
negocio con dos lecturas posibles, y distinguirlas es el trabajo:

1. Desglosar `vetsoftware_business_inventory_movements_total` por `movement_type` y `result`. Si
   los rechazos se concentran en `sale` o `clinical_use`, hay desabastecimiento real de cara al
   cliente.
2. Existencias que la aplicación cree agotadas pero que físicamente están en sede apuntan a
   descuadre del inventario (lotes vencidos, movimientos no registrados, un ajuste pendiente), no a
   falta de compra.
3. Contrastar con `vetsoftware_business_inventory_low_stock` y con los lotes por vencer: un pico de
   rechazos precedido de bajo stock sostenido es previsible y evitable.
4. La serie no dice **qué** producto. Ese dato se obtiene de los logs de la ventana y de las trazas
   de las operaciones rechazadas.

## VetSoftwareBusinessMetricsSnapshotStale

`vetsoftware_business_metrics_snapshot_age_seconds` supera 180 segundos: el snapshot periódico que
alimenta los gauges de backlog DIAN e inventario lleva más de tres refrescos sin completarse (el
intervalo por defecto es de 60 s,
`vetsoftware.observability.business-metrics.snapshot-refresh-ms`).

**Es la alerta que protege a las otras tres.** Cuando el refresco falla, `BusinessGaugeMetrics`
conserva deliberadamente el último valor conocido en lugar de dejar la serie ausente: los gauges se
**congelan**, no desaparecen. Un backlog que crece se ve plano y sano. Sin esta alerta, la ceguera
sería indistinguible de la normalidad.

1. Buscar en Loki el `WARN` de `BusinessGaugeMetrics` ("No se pudo actualizar el snapshot de
   métricas de negocio"): trae la causa del fallo **con su stacktrace y su cadena de causas**, no
   solo el mensaje.
2. Cuantificar cuántos ciclos fallan con `tasks_scheduled_execution_*` filtrada por
   `job_name="business.metrics.snapshot"` y desglosada por `job_outcome`. El trabajo abre su
   transacción **dentro** de la observación a propósito: si el pool no da conexión, el
   `CannotCreateTransactionException` sale con `job_outcome="error"` y no como una ejecución
   anónima sin `job_name`.
3. La causa habitual es la base de datos: indisponibilidad, saturación del pool o una consulta de
   conteo que se ha vuelto lenta. Correlacionar con `VetSoftwareDatabasePoolSaturated` y
   `VetSoftwareDatabaseConnectionTimeouts`.
4. **Mientras esta alerta esté activa, tratar como no fiables** el estado de
   `VetSoftwareDianBacklogOlderThanOneHour` y las series de inventario. Resolverla antes de sacar
   conclusiones de ellas.

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
