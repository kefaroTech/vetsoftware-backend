# Objetivos de nivel de servicio (SLO) de VetSoftware

**Fecha de definición:** 07/28/2026
**Estado de los objetivos:** provisionales — fijados sin línea base histórica (ver [Revisión de objetivos](#revisión-de-objetivos))
**Ventana de evaluación:** rodante de 30 días
**Implementación:**

- Reglas de grabación: `docker/prometheus-slo-rules.yml`
- Alertas: `docker/prometheus-slo-alerts.yml` — runbooks en `docs/ALERTAS_STACK_LOCAL.md`
- Pruebas: `docker/tests/prometheus-slo.test.yml`
- Dashboard: `docker/grafana/dashboards/slo-overview.json` (`uid: vetsoftware-slo`)

Este documento es la única fuente de verdad conceptual de los SLO. Los valores numéricos viven
además como series de Prometheus (`vetsoftware:slo_objective:ratio`), de modo que dashboards y
alertas nunca repiten un objetivo escrito a mano.

## 1. Alcance

Se definen SLO para **siete operaciones críticas**. Un SLO puede tener dos indicadores
(disponibilidad y latencia), lo que da nueve SLI en total.

No se añadió ningún histograma nuevo. Todos los SLI se construyen sobre instrumentación que ya
existía: los histogramas de `http.server.requests` y `vetsoftware.business.dian.transmission.duration`,
y los contadores de negocio de DIAN e inventario. Los SLI por operación se obtienen agrupando
`uri` y `method` en reglas de grabación, que **reducen** cardinalidad en lugar de crearla.

Quedan deliberadamente fuera:

- **Cierre de caja.** Una diferencia de arqueo es un hecho de negocio real, no una falla del
  sistema. Convertirla en error budget haría que un faltante de caja bloqueara despliegues.
- **Citas.** Las transiciones de estado las decide el usuario; no hay noción de "transición mala".
- **Backlog DIAN.** Es un estado acumulado, no un flujo de eventos; ya tiene alerta propia
  (`VetSoftwareDianBacklogOlderThanOneHour`).
- **Todo el bloque de dinero de suscripciones** (cargos, cuentas de cobro, pagos, imputaciones,
  cobranza, entitlements), y esta exclusion es la mejor argumentada de la lista.

  El bloque mueve del orden de **500 eventos al mes**. Un SLI basado en tasa necesita un numero
  minimo de muestras por ventana para que el estadistico exista: con el umbral del 5 % que usa el
  resto de este documento hacen falta 20 eventos por ventana solo para que un fallo suelto no
  supere el objetivo por si mismo, y aqui la ventana de 5 minutos contiene **cero** eventos casi
  siempre. Calcular un porcentaje sobre eso no produce un indicador impreciso: produce un
  indicador **vacio**, que alterna entre 0 % y 100 % segun caiga un evento dentro o fuera.

  Y el error budget seria peor todavia. Con 500 eventos mensuales, un objetivo del 99,5 % concede
  dos fallos y medio al mes: el tercer cargo mal emitido bloquearia los despliegues del resto del
  mes, y el primero no bloquearia nada. Ninguno de los dos comportamientos describe lo que hay que
  hacer cuando un cargo sale duplicado.

  **Se vigila por conteo absoluto**, que es lo correcto a este volumen: la afirmacion es «esto
  deberia ser cero» y el umbral es el cero. Las metricas estan en
  `docs/CONVENCION_NOMBRES_OBSERVABILIDAD.md` y sus alertas en
  `docker/prometheus-platform-alerts.yml` (grupos `vetsoftware-entitlements` y
  `vetsoftware-scheduled-jobs`) con su gemelo cloud. Si algun dia el volumen sube dos ordenes de
  magnitud, esta decision se revisa con el procedimiento de la seccion 10 — no antes.

## 2. Catálogo de SLO

| SLO | SLI | Objetivo 30d | Umbral | Presupuesto de error |
|---|---|---:|---|---|
| `api-availability` | disponibilidad | 99.50 % | — | 0.50 % de las solicitudes |
| `api-latency` | latencia | 99.00 % | 1 s | 1.00 % de las solicitudes |
| `auth-login` | disponibilidad | 99.90 % | — | 0.10 % de los intentos |
| `pos-checkout` | disponibilidad | 99.50 % | — | 0.50 % de las ventas |
| `pos-checkout` | latencia | 99.00 % | 2 s | 1.00 % de las ventas |
| `clinical-write` | disponibilidad | 99.50 % | — | 0.50 % de los registros |
| `clinical-write` | latencia | 99.00 % | 2 s | 1.00 % de los registros |
| `dian-transmission` | disponibilidad | 99.00 % | — | 1.00 % de las transmisiones |
| `dian-transmission` | latencia | 95.00 % | 15 s | 5.00 % de las transmisiones |
| `inventory-movement` | disponibilidad | 99.50 % | — | 0.50 % de los movimientos |

El objetivo de `dian-transmission` es el más laxo a propósito: depende de un tercero
(el proveedor MATIAS y la propia DIAN) sobre el que no tenemos control operativo.

## 3. Definición de cada SLI

Todos los SLI son **basados en eventos** (proporción de eventos buenos sobre eventos válidos),
no basados en tiempo. Un SLI basado en tiempo ("minutos en que el servicio estuvo arriba") oculta
el volumen: una hora con una sola solicitud fallida pesaría igual que una hora con mil.

### 3.1. `api-availability`

- **Fuente:** `http_server_requests_seconds_count`
- **Denominador:** todas las solicitudes HTTP del backend.
- **Eventos malos:** respuestas con `status=~"5.."`.

### 3.2. `api-latency`

- **Fuente:** `http_server_requests_seconds_bucket`, borde `le="1.0"`.
- **Denominador:** solicitudes con `status!~"5.."`.
- **Eventos malos:** solicitudes no-5xx que tardaron más de 1 s.

Los 5xx se excluyen del denominador de latencia porque un error devuelto rápido no es un evento
bueno: ya lo castiga `api-availability`. Contarlo dos veces duplicaría el gasto de budget.

### 3.3. `auth-login`

- **Fuente:** `http_server_requests_seconds_count` con `method="POST"` y
  `uri=~"(/api/v1)?/auth/login/(employee|system)"`.
- **Denominador:** todos los intentos de inicio de sesión.
- **Eventos malos:** respuestas 5xx.

Una credencial inválida responde 4xx y **no** gasta budget: es el sistema funcionando.

### 3.4. `pos-checkout`

- **Fuente:** `POST /electronic-documents/from-sale` (`RegisterPosSaleService`).
- **Disponibilidad:** malos = 5xx.
- **Latencia:** umbral 2 s sobre respuestas no-5xx.

Es la operación de dinero de mayor frecuencia: un fallo aquí deja al cliente en el mostrador
sin poder pagar.

### 3.5. `clinical-write`

- **Fuente:** solicitudes `POST` a las rutas de creación de registro clínico: consultas,
  vacunaciones, desparasitaciones, cirugías, hospitalizaciones, exámenes de laboratorio,
  imágenes diagnósticas, fórmulas, prescripciones de medicamento, spa, guardería y registros
  de peso.
- **Disponibilidad:** malos = 5xx.
- **Latencia:** umbral 2 s sobre respuestas no-5xx.

Se agrupan en un solo SLO porque comparten dueño, criticidad y patrón de falla; separarlos daría
diez SLO con tráfico individual demasiado bajo para ser estadísticamente útiles.

### 3.6. `dian-transmission`

- **Fuente:** `vetsoftware_business_dian_transmissions_total` y su temporizador asociado.
- **Denominador:** transmisiones resueltas — `validated`, `rejected`, `contingency`, `error`.
- **Eventos malos:** `rejected`, `contingency`, `error`.
- **Latencia:** umbral 15 s sobre las mismas transmisiones resueltas.

Dos decisiones de conteo que conviene tener explícitas:

- **`pending` se excluye del denominador.** La DIAN aún no decidió, así que el evento no es
  ni bueno ni malo. Su riesgo lo cubre la alerta de backlog.
- **`contingency` cuenta como malo.** La venta continuó, pero el documento no llegó a la DIAN
  y queda una obligación fiscal pendiente. Es la lectura conservadora y la que corresponde a un
  producto que promete cumplimiento.

### 3.7. `inventory-movement`

- **Fuente:** `vetsoftware_business_inventory_movements_total`.
- **Denominador:** `success`, `duplicate_ignored`, `error`.
- **Eventos malos:** `error`.

- `duplicate_ignored` **es un evento bueno**: es la idempotencia del kardex funcionando.
- `insufficient_stock` y `validation_error` **se excluyen del denominador**: son rechazos
  legítimos de reglas de negocio, el equivalente de un 4xx. Su tasa ya se vigila con
  `VetSoftwareInventoryInsufficientStockRateHigh`.

## 4. Regla transversal: qué gasta error budget

| Clase de evento | ¿Gasta budget? | Razón |
|---|---|---|
| HTTP 5xx | Sí | Falla del servicio |
| HTTP 4xx | No | Error del cliente; el servicio respondió correctamente |
| HTTP 429 | No (hoy) | El backend no aplica rate limiting propio; si se añade, reevaluar |
| Latencia sobre el umbral en respuesta exitosa | Sí | Lentitud percibida por el usuario |
| Latencia sobre el umbral en respuesta 5xx | No | Ya lo castiga el SLI de disponibilidad |
| Rechazo de regla de negocio (`insufficient_stock`, credencial inválida) | No | El sistema funcionó |
| Rechazo o contingencia de la DIAN | Sí | La obligación fiscal quedó incumplida |
| Operación pendiente de un tercero (`pending`) | No cuenta | Aún no hay resultado; se excluye del denominador |

## 5. Umbrales de latencia y bordes de histograma

Un SLI de latencia solo es auditable si el umbral coincide **exactamente** con un borde `le`
publicado. Cualquier otro valor obliga a interpolar dentro de un bucket, y el porcentaje
resultante deja de ser una cuenta de eventos.

Bordes declarados en `management.metrics.distribution.slo` de `application.yml` y verificados
contra Prometheus:

| Métrica | Bordes disponibles | Umbrales usados |
|---|---|---|
| `http.server.requests` | 250 ms, 500 ms, 1 s, 2 s, 5 s | 1 s, 2 s |
| `vetsoftware.business.dian.transmission.duration` | 2 s, 5 s, 15 s, 60 s | 15 s |
| `lettuce` (cliente Valkey) † | 1 ms, 5 ms, 25 ms, 50 ms, 100 ms | 50 ms |

† `lettuce` **no es un SLI**: no entra en ningún error budget ni en la cadena de burn rate. Está
en esta tabla porque sus bordes se declaran en el mismo sitio y por el mismo motivo — la alerta
técnica `VetSoftwareValkeyLatencyHigh` compara un p99 contra 50 ms y hasta agosto de 2026 esa
comparación era imposible: el medidor solo publicaba `le="+Inf"`, `histogram_quantile` devolvía
`NaN` y `NaN` nunca supera un umbral. El borde de 100 ms no es opcional: `histogram_quantile`
devuelve el borde finito más alto cuando el cuantil cae en `+Inf`, así que sin él el p99 quedaría
clavado en 50 exactos y la alerta seguiría sin poder dispararse. Unidad: el medidor sale por el
registro OTLP, cuya unidad base es el milisegundo, así que los bordes se publican como
`le="1"`…`le="100"`, no como fracciones de segundo.

**Consecuencia operativa:** cambiar el umbral de un SLO a un valor que no esté en esta tabla
exige añadir primero el borde en `application.yml`. Si no se añade, la serie del bucket no existe,
el numerador queda vacío y el SLI **desaparece en silencio** en lugar de fallar de forma visible.

La alerta `VetSoftwareSloSeriesAbsent` convierte esa desaparición silenciosa en una señal
visible: compara los objetivos declarados contra las series de eventos existentes y avisa cuando
un SLO deja de medirse.

## 6. Cadena de cálculo

```text
vetsoftware:sli_{bad,total}_events:rate5m      una regla por SLI (lo único específico por journey)
        │  avg_over_time
        ├─ :rate30m  :rate1h  :rate2h  :rate6h  (ventanas de burn rate)
        └─ :rate1d ──► :rate30d                 (ventana de cumplimiento)
                 │
                 ├─ vetsoftware:sli_bad:ratio_rateXX
                 ├─ vetsoftware:slo_burn_rate:ratio_rateXX
                 ├─ vetsoftware:slo_compliance:ratio30d
                 └─ vetsoftware:slo_error_budget_remaining:ratio30d
```

Dos propiedades del diseño que conviene no romper al editarlo:

- **Las ventanas largas promedian tasas de eventos, nunca razones ya calculadas.** Promediar
  razones daría el mismo peso a una hora con 5 solicitudes que a una con 5.000, y el cumplimiento
  mensual quedaría sesgado por las horas de bajo tráfico.
- **La ventana de 30 días se encadena desde `rate1d`, no desde `rate5m`.** El resultado ponderado
  por tráfico es el mismo con un orden de magnitud menos de puntos evaluados.

Añadir una operación crítica nueva son **dos reglas** (`sli_total_events:rate5m` y
`sli_bad_events:rate5m` con sus labels `slo`/`sli`/`service`) más una fila de objetivo. Todo lo
demás —ventanas, razones, burn rate, cumplimiento, budget, paneles y alertas— la sigue
automáticamente por labels.

### Interpretación del error budget

`vetsoftware:slo_error_budget_remaining:ratio30d`:

- `1.0` — presupuesto intacto, ningún evento malo en 30 días.
- `0.25` — queda una cuarta parte del presupuesto.
- `0.0` — presupuesto agotado; el SLO se cumple justo en el límite.
- `-0.5` — SLO incumplido, con un 50 % de gasto por encima de lo permitido.

`vetsoftware:slo_burn_rate:ratio_rateXX` indica cuántas veces más rápido de lo sostenible se
consume el presupuesto en esa ventana. `1` significa que se agotaría exactamente al cerrar los
30 días; `14.4` que se agotaría en poco más de dos días.

## 7. Alertamiento

Definido en `docker/prometheus-slo-alerts.yml`, con runbooks en
`docs/ALERTAS_STACK_LOCAL.md`. Las alertas consumen únicamente series grabadas, por lo que
cubren automáticamente cualquier SLO que se declare después.

| Alerta | Condición | Severidad | Respuesta |
|---|---|---|---|
| `VetSoftwareSloFastBurn` | burn rate > 14.4 en 1 h **y** 5 min | critical | Inmediata; presupuesto agotado en ~2 días |
| `VetSoftwareSloSlowBurn` | burn rate > 6 en 6 h **y** 30 min | warning | Mismo día; agotado en ~5 días |
| `VetSoftwareSloTicketBurn` | burn rate > 3 en 1 d **y** 2 h | warning | Trabajo planificado; agotado en ~10 días |
| `VetSoftwareSloErrorBudgetLow` | presupuesto restante < 25 % | warning | Gobierno de despliegue |
| `VetSoftwareSloErrorBudgetExhausted` | presupuesto restante < 0 | critical | Congelamiento del dominio |
| `VetSoftwareSloSeriesAbsent` | objetivo declarado sin serie de eventos en 24 h | warning | El SLO no se está midiendo |

Tres decisiones de diseño del alertamiento:

- **Cada nivel exige ventana larga y ventana corta a la vez.** La larga evita alertar por un pico
  irrelevante; la corta hace que la alerta se resuelva rápido cuando el problema termina, en lugar
  de quedar colgada horas después de la recuperación.
- **Guarda de volumen.** Con tráfico bajo, una sola solicitud fallida de tres da una razón de 0.33
  y un burn rate enorme. Cada nivel exige un mínimo de eventos en su ventana larga: 20 en 1 hora,
  60 en 6 horas, 200 en 1 día. Son mínimos modestos, pensados para el volumen actual, y **deben
  subirse cuando el tráfico de producción crezca**. Mientras el volumen sea bajo, un SLO puede
  degradarse sin que ninguna alerta se dispare; el panel "Volumen de eventos por SLI" del
  dashboard es la única forma de detectarlo.
- **Inhibición en cascada.** `alertmanager.yml` inhibe los niveles lentos cuando el rápido está
  activo, `ErrorBudgetLow` cuando está `ErrorBudgetExhausted`, y todas las anteriores cuando el
  SLI no produce datos (su burn rate no sería interpretable). El `group_by` incluye `slo` y `sli`
  para que dos SLO degradados a la vez no se agrupen en un solo correo.

Las reglas y alertas tienen pruebas en `docker/tests/prometheus-slo.test.yml`, incluida una que
falla si se declara un objetivo nuevo sin su regla de eventos.

## 8. Gobierno de despliegue

El presupuesto de error existe para tomar decisiones, no solo para medir. Sin una consecuencia
asociada, un SLO es un gráfico bonito.

| Presupuesto restante (30 d) | Consecuencia |
|---|---|
| ≥ 50 % | Sin restricciones. Ritmo normal de features. |
| 25 % – 50 % | Los cambios que toquen el dominio del SLO afectado requieren revisión explícita de riesgo antes de desplegar. |
| 0 % – 25 % | Se congelan los features no críticos de ese dominio. Correcciones de fiabilidad, seguridad y cumplimiento legal siguen habilitadas. |
| < 0 % | Solo correcciones de fiabilidad del dominio afectado hasta que la ventana rodante recupere presupuesto. |

Reglas de aplicación:

- **El alcance del congelamiento es el dominio del SLO, no el producto entero.** Un
  `dian-transmission` agotado no debe bloquear un cambio en la agenda de citas.
- **La ventana se recupera sola** al desplazarse los 30 días. No existe un mecanismo para
  "reiniciar" el presupuesto, y no debe crearse: sería equivalente a borrar la evidencia.
- **Las excepciones se documentan.** Desplegar un feature con presupuesto agotado es una decisión
  legítima si alguien la asume por escrito; lo que no es legítimo es desplegarlo sin registrarlo.
- **Nunca se ajusta un objetivo durante un incidente.** Cambiar el objetivo para que la alerta
  deje de sonar vacía el mecanismo completo. Los objetivos se revisan según la sección 10, en frío
  y por escrito.
- Los SLO de dinero y cumplimiento fiscal (`pos-checkout`, `dian-transmission`) tienen prioridad
  sobre el resto cuando compiten por capacidad de trabajo de fiabilidad.

## 9. Dependencia de retención

La ventana de 30 días exige que Prometheus conserve al menos 30 días de datos:

- **Local (Docker Compose):** `--storage.tsdb.retention.time=35d` y `retention.size=15GB`.
  Con 15 días la serie de cumplimiento se calcularía sobre una ventana truncada y **reportaría
  un cumplimiento mejor que el real**, sin ningún error visible.
- **Producción:** la retención larga la aporta Grafana Cloud
  (ver `docs/OBSERVABILIDAD_PROD_GRAFANA_S3.md`).

La retención por tamaño puede borrar bloques antiguos antes de los 35 días. Si el volumen crece,
subir `retention.size` antes que bajar `retention.time`.

## 10. Revisión de objetivos

Los diez objetivos de la sección 2 son **provisionales**: se fijaron con criterio de producto,
sin una línea base medida. El riesgo de un objetivo inventado es doble — si queda demasiado
laxo nunca alerta, y si queda demasiado estricto genera ruido y se acaba ignorando.

Procedimiento de revisión:

1. Dejar correr las reglas al menos **cuatro semanas completas** con tráfico representativo.
2. Comparar cada `vetsoftware:slo_compliance:ratio30d` con su objetivo.
3. Ajustar cuando se cumpla alguno de estos criterios:
   - cumplimiento sostenido **por encima** del objetivo con margen amplio → el objetivo es
     trivial y no protege nada;
   - cumplimiento sostenido **por debajo** del objetivo sin incidentes percibidos por usuarios
     → el objetivo no representa la experiencia real;
   - alertas de burn rate que se resuelven solas sin acción → ventana o umbral mal calibrados.
4. Registrar cada cambio de objetivo en este documento con fecha y motivo.

Un SLO se cambia **deliberadamente y por escrito**. Ajustarlo durante un incidente para que deje
de alertar vacía el mecanismo por completo.

## 11. Referencias

- [Google SRE Workbook: implementación de SLO](https://sre.google/workbook/implementing-slos/)
- [Google SRE Workbook: alertas sobre SLO](https://sre.google/workbook/alerting-on-slos/)
- [Prometheus: reglas de grabación](https://prometheus.io/docs/prometheus/latest/configuration/recording_rules/)
- [Micrometer: histogramas y percentiles](https://docs.micrometer.io/micrometer/reference/concepts/histogram-quantiles.html)
