# Resiliencia y límites del stack de observabilidad

**Fecha:** 07/28/2026
**Hallazgo que cierra:** OBS-027 del reporte de trazabilidad
**Implementación:** `docker-compose.yml`, `docker/otel-collector.yml`, `docker/tests/resilience.sh`

## 1. El problema real

El hallazgo señalaba tres cosas: no había límites de recursos, no había colas ni reintentos, y no
había ensayos. De las tres, la que causaba daño era la segunda.

Los exportadores del Collector no tenían **ni cola ni reintentos**. Con la configuración anterior,
si Loki o Tempo no respondían, el lote se descartaba en el primer fallo. La telemetría de esa ventana
se perdía y solo quedaba el rastro de un contador de errores. Un reinicio del Collector perdía además
todo lo que tuviera en memoria.

Los límites de recursos son la parte defensiva; las colas son la parte que evita perder datos.

## 2. Límites de recursos

Aplicados con `deploy.resources.limits`, que Compose V2 traduce a límites reales del kernel (`docker
inspect` los muestra en `HostConfig.Memory` y `HostConfig.NanoCpus`).

| Servicio | Memoria | CPUs | Nota |
|---|---:|---:|---|
| `redis` | 512M | 1.0 | `maxmemory 384mb` + `volatile-lru` |
| `redis-exporter` | 128M | 0.5 | |
| `mysql` | 4G | 4.0 | Buffer pool en su valor por omisión (128M) |
| `otel-queue-init` | 64M | 0.5 | Contenedor de un solo uso |
| `otel-collector` | 1G | 2.0 | Por encima de su `memory_limiter` a propósito |
| `tempo` | 2G | 2.0 | |
| `prometheus` | 4G | 4.0 | El tope de disco lo pone `retention.size=15GB` |
| `alertmanager` | 256M | 0.5 | |
| `mailpit` | 256M | 0.5 | `MP_MAX_MESSAGES=500` acota el buzón |
| `loki` | 2G | 2.0 | |
| `grafana` | 1G | 2.0 | |
| `sonarqube` | 4G | 4.0 | Herramienta de desarrollo; primer candidato a apagar |
| `localstack` | 1G | 2.0 | |

Suma de topes ≈ 20 GiB. Son límites, no reservas: no se preasigna nada.

### Dos reglas que no conviene romper

**El límite del Collector va por encima de su `memory_limiter`.** El `memory_limiter` está en 512 MiB
+ 128 MiB de pico; el contenedor en 1 GiB. Así el Collector rechaza telemetría entrante y aplica
backpressure al emisor **antes** de que el kernel lo mate por OOM. Si el límite del contenedor
estuviera por debajo del `memory_limiter`, el mecanismo de protección nunca llegaría a actuar y el
resultado sería un OOM kill con pérdida de la cola en memoria.

**Un límite sin política de descarte solo cambia quién muere.** Poner un tope de memoria a un
servicio que no sabe qué tirar cuando se llena convierte un problema de crecimiento en una caída.
Donde el servicio tiene su propio mecanismo, hay que configurarlo *además* del límite: `maxmemory` en
Redis, `retention.size` en Prometheus, `sending_queue` en el Collector.

### El caso de Redis: una alerta que no podía dispararse

`maxmemory` estaba sin fijar (`0` = sin límite). Eso tenía dos consecuencias, y la segunda no era
evidente:

1. Redis podía crecer hasta agotar la memoria del host.
2. **La alerta `VetSoftwareRedisMemoryHigh` era inoperante.** Su expresión exige
   `redis_memory_max_bytes > 0`, y con `maxmemory` sin fijar ese valor es `0`, así que la condición
   nunca se cumplía. Existía en el archivo pero no podía avisar de nada.

La política es `volatile-lru` y no `allkeys-lru` deliberadamente: solo desaloja claves con TTL. Todas
las entradas de caché de la aplicación llevan TTL de 5 minutos (`CacheConfig`), así que son
desalojables, y cualquier clave sin TTL que se añada en el futuro queda intacta. `allkeys-lru` habría
sido más agresivo y podría desalojar algo no reconstruible.

## 3. Colas persistentes y reintentos

Ambos exportadores llevan ahora `sending_queue` respaldada en disco por la extensión `file_storage`, y
`retry_on_failure` con backoff hasta 30 minutos.

```
                          ┌─ retry_on_failure (5s → 30s, hasta 30m)
receptor OTLP → memory_limiter → batch → sending_queue (disco) → exportador
                     ↓                        ↓
              rechaza entrante          cola llena → descarte CONTABILIZADO
              (backpressure)
```

**La cola es persistente para que sobreviva a un reinicio del Collector.** Es el escenario 3 del
hallazgo y no se puede cubrir con una cola en memoria.

**Cuando la cola se llena, el descarte es visible.** Se contabiliza en
`otelcol_exporter_enqueue_failed_*`, y el `memory_limiter` empieza a rechazar telemetría entrante, lo
que aparece en `otelcol_receiver_refused_*`. Ambas tienen alerta. Eso convierte la «pérdida
silenciosa» del hallazgo en pérdida medida — que no es lo mismo que pérdida cero, pero sí es operable.

### El volumen y el usuario del contenedor

La imagen del Collector es distroless y corre como `10001:10001`, pero un volumen nombrado se crea
como `root:root`, así que el Collector no podía crear su directorio de cola. Se resolvió con un
contenedor de inicialización de un solo uso (`otel-queue-init`) que ajusta la propiedad.

La alternativa —`user: "0:0"` en el Collector— se descartó: dejar el receptor OTLP corriendo como root
es exactamente el tipo de exposición que señala OBS-025.

## 4. Los ensayos

`docker/tests/resilience.sh` cubre los cinco escenarios del hallazgo.

```bash
bash docker/tests/resilience.sh              # todos
bash docker/tests/resilience.sh loki collector   # solo algunos
```

Requiere el stack levantado. **No requiere el backend**: la telemetría se inyecta directamente por
OTLP. Es deliberado — da marcadores únicos y conteos exactos, no necesita base de datos ni
credenciales, y lo que el hallazgo quiere comprobar es el comportamiento del stack, no de la
aplicación.

Tampoco requiere el override de depuración. Desde OBS-025 (docs/SEGURIDAD_OBSERVABILIDAD.md) Loki y
Tempo no publican puerto en el host: el script consulta sus APIs desde un contenedor `curl` conectado a
la red interna `vetsoftware_telemetry`, y el escenario `network` desconecta Loki de **todas** sus redes,
no de una sola —si quedara una, el Collector conservaría ruta y el ensayo no probaría nada.

| Escenario | Qué afirma |
|---|---|
| `loki` | Los logs emitidos con Loki caído llegan al reactivarlo |
| `tempo` | La traza emitida con Tempo caído se puede consultar después |
| `collector` | Con la cola llena, reiniciar el Collector no pierde nada |
| `burst` | El Collector sobrevive a un pico y toda pérdida queda contabilizada |
| `network` | Un corte de red se absorbe igual que una caída de servicio |

Dos decisiones sobre qué se afirma:

- **No basta con que el Collector siga en pie.** Cada escenario cuenta los registros que llegan al
  destino. El riesgo del hallazgo es la pérdida silenciosa, no la caída.
- **El escenario del pico no exige entrega total.** Exige que la diferencia entre lo emitido y lo
  entregado quede explicada por `otelcol_receiver_refused_*` y
  `otelcol_exporter_enqueue_failed_*`. Un descarte medido es operable; uno que no aparece en ninguna
  métrica es el defecto.

El script restaura el stack al terminar (`trap ... EXIT`), incluso si un ensayo falla a mitad.

### Una trampa que costó un falso negativo

La primera ejecución dio 4 de 5, con el ensayo del pico reportando «3200 registros desaparecieron sin
quedar en ninguna métrica». No era pérdida: **Loki descarta entradas con el mismo par (timestamp,
contenido) dentro de un stream**, y el ensayo emitía 20 lotes con cuerpos idénticos (`n=1..200`) y
timestamps de granularidad de segundo. Solo había unos 4 segundos distintos × 200 líneas únicas = los
800 que aparecían.

Dos cambios lo cerraron:

1. Cada registro lleva timestamp en nanosegundos único y el índice de lote en el cuerpo.
2. El veredicto se juzga sobre `otelcol_exporter_sent_log_records`, no sobre lo que Loki conserva. Si
   el Collector entregó todo lo que aceptó, el pipeline no perdió nada aunque el destino descarte;
   así el diagnóstico apunta al lugar correcto en vez de acusar al pipeline.

Vale la pena tenerlo presente al escribir cualquier ensayo contra Loki: **contenido repetido en el
mismo instante no se cuenta dos veces.**

### La segunda trampa: `docker network connect` pierde el alias de servicio

El ensayo de corte de red falló con «Loki no volvió a estar alcanzable» aunque el contenedor estaba
sano. El diagnóstico: respondía por IP (`ready=200`) pero el nombre `loki` **no resolvía**.

`docker network connect` sin `--alias` registra solo el nombre del contenedor
(`vetsoftware_loki`), no el alias que Compose le puso al crearlo (`loki`). Tras reconectar, el
servicio queda vivo y accesible por IP, pero `http://loki:3100` deja de resolver — ni el Collector ni
los ensayos lo encuentran.

Es un fallo especialmente confuso porque **el servicio parece caído estando perfectamente sano**, y
los logs de Loki no muestran nada anormal. Toda reconexión en el script pasa ahora por
`reconnect_with_alias`.

Aplica fuera de los ensayos: si alguien alguna vez desconecta y reconecta a mano un contenedor del
stack, debe hacerlo con `--alias <nombre-del-servicio>` o romperá el descubrimiento por nombre.

### Márgenes de tiempo

Los logs pasan por `batch` (timeout 5 s); las trazas además por `tail_sampling`
(`decision_wait` 10 s), de ahí que su margen sea mayor. Es la misma demora que describe OBS-015. Los
márgenes se pueden ajustar con `LOG_SETTLE_SECONDS` y `TRACE_SETTLE_SECONDS`.

## 5. Lo que queda fuera

- **Los límites son de desarrollo local.** En producción el dimensionamiento lo hace ECS
  (`modules/ecs_backend`) y la observabilidad la aporta Grafana Cloud; los valores de esta tabla no se
  trasladan.
- **No hay ensayo de caída de MySQL ni de Redis.** El hallazgo no los pedía, pero son dependencias del
  backend con más impacto en el usuario que Loki o Tempo.
- **No hay ensayo de disco lleno.** `retention.size` de Prometheus y la cola en disco del Collector
  compiten por el mismo volumen del host.
- **La suite no corre en CI.** Necesita Docker y varios minutos; hoy se ejecuta a mano.
