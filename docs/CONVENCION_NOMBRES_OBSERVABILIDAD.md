# Convención de nombres de observabilidad

Esta convención aplica a las observaciones Micrometer, los spans de negocio derivados de
ellas y los nombres de trabajos programados de VetSoftware.

## Observaciones y métricas

Los nombres técnicos usan `lowercase.dot.notation`:

```text
dominio[.recurso].operacion
```

El segmento de recurso es opcional. Cada palabra ocupa un segmento separado por punto. No se permiten camelCase, guiones,
guiones bajos, espacios, identificadores ni otros valores de cardinalidad no acotada.

Ejemplos:

| Incorrecto | Correcto |
|---|---|
| `animal_alert.listByAnimal` | `animal.alert.list.by.animal` |
| `cashRegister.openSession` | `cash.register.open.session` |
| `clinicalhistory.list_by_company` | `clinical.history.list.by.company` |
| `supplierInvoice.registerPayment` | `supplier.invoice.register.payment` |

El nombre debe identificar una clase estable de operaciones. Los identificadores de empresa,
usuario, animal, factura o solicitud pertenecen a atributos y nunca al nombre.

## Nombres contextuales de spans

Cuando se define `contextualName`, se usa una frase corta y de baja cardinalidad con el patrón
`verbo objeto`, en minúsculas y separada por espacios:

```text
send email
render pdf
```

El nombre contextual facilita la lectura en Tempo. El nombre técnico de la observación se
conserva como identificador estable para métricas, paneles y alertas.

## Métricas de negocio del dinero de suscripciones

Siete métricas nuevas (backend #606), todas bajo `vetsoftware.business.subscription.*` y todas
publicadas desde `AfterCommitMetricRecorder` para no contar cobros que después hacen rollback:

| Métrica | Etiquetas de **baja** cardinalidad | Qué responde |
|---|---|---|
| `vetsoftware.business.subscription.charges` | `charge.type`, `result` | ¿Cuántos cargos se devengaron y cuántos se anularon? |
| `vetsoftware.business.subscription.charged.amount` | `charge.type`, `charge.sign` | ¿Por cuánto? |
| `vetsoftware.business.subscription.documents` | `issue.status`, `result` | ¿Se emitieron las cuentas de cobro, o se rechazaron? |
| `vetsoftware.business.subscription.payments` | `payment.method`, `result` | ¿Entró la plata y en qué estado? |
| `vetsoftware.business.subscription.applications` | `source.kind`, `result` | ¿Se imputó contra la factura, y con qué fuente? |
| `vetsoftware.business.subscription.status.transitions` | `to.status` | ¿A cuántos clientes se les cortó la escritura? |
| `vetsoftware.business.subscription.entitlement.recalculations` | `trigger.reason`, `result` | ¿Se cerró el lazo entre lo que se paga y lo que se puede usar? |

**Ninguna lleva la empresa, y no es negociable.** Con 500 clínicas, una etiqueta por empresa
multiplica cada serie por 500 y convierte estas siete en varios miles, sobre un plan que ya roza su
techo de series activas y cuyo rebase hace que Grafana Cloud **rechace la ingesta y se pierda toda
la telemetría en silencio**. La empresa viaja en el MDC (`actor.companyId`) y como atributo de
span: ahí es donde se responde «¿a quién le pasó?», que es otra pregunta.

**Ninguna se vigila con un SLO.** El bloque mueve del orden de 500 eventos **al mes**; un indicador
basado en tasa sobre ese volumen es estadísticamente vacío. Se vigilan por conteo absoluto —«esto
debería ser cero»— y eso está desarrollado en `docs/SLO_VETSOFTWARE.md §1`.

**Ninguna aporta un valor nuevo al tag `result`.** Los seis que usan —`completed`, `cancelled`,
`rejected`, `pending`, `failed`, `duplicate_ignored`— ya estaban declarados. Es deliberado: abrir un
vocabulario paralelo para el mismo concepto impide preguntar «cuántas operaciones se rechazaron
hoy» a través de todo el sistema.

**`charge.sign` no es decoración.** `DistributionSummary` de Micrometer **descarta en silencio los
valores negativos**, y en este dominio los negativos son operaciones normales: un crédito, un
descuento y una proración de reducción restan. Sin ese tag habría que registrar el valor absoluto y
el histograma diría que se devengaron 500.000 pesos cuando en realidad se devolvieron. Con él, el
neto es una resta de dos series y cada lado significa algo por sí solo.

### La trampa de la lista blanca, otra vez

Añadir un valor a una etiqueta sin declararlo en `ALLOWED_VALUES` de
`BusinessMetricCardinalityFilter` **deniega el medidor entero**, no esa serie suelta, y el hueco
resultante es indistinguible de la ausencia de actividad. `BusinessMetricEnumAllowlistParityTest`
convierte ese fallo silencioso en un CI rojo para los seis vocabularios nuevos: añadir una constante
a `ChargeType`, `IssueStatus`, `PaymentMethod`, `ApplicationSourceKind`, `SubscriptionStatus` o
`Trigger` sin tocar la lista blanca rompe el build.

## Latencias de lectura que NO se publican

`ReadObservationMeterFilter` deniega el medidor de **diecinueve observaciones de lectura** del bloque de
suscripciones. El span sigue emitiéndose: lo que se retira es la serie temporal, no la traza.

**Por qué.** Cada `@Observed` produce ocho series —el `Timer` publica `_count`, `_sum`, `_bucket` y
`_max`, y el `LongTaskTimer` que crea `DefaultMeterObservationHandler` publica otras cuatro con
infijo `_active_`; medido en Grafana Cloud sobre
`subscription_billing_document_list_awaiting`—. Diecinueve lecturas × 8 = **152 series** que no
responden ninguna pregunta que no responda ya `http_server_requests_seconds`, que trae la misma
latencia por `uri`, `method` y `status` y además con el histograma completo activado. Cada una es un
`GET` servido por un único endpoint, así que la correspondencia es uno a uno. Ningún panel las
consulta y ninguna alerta las nombra: comprobado ruta por ruta en `docker/prometheus-*.yml` y en
`VetSoftwareIaC/observability/`.

**El riesgo asumido, dicho en voz alta**: un filtro que deniega es invisible, y quien busque
`subscription_billing_document_list_milliseconds_count` no lo encontrará. Por eso el proceso emite
en el arranque una línea de `INFO` que nombra lo que no publica y dónde está la latencia
equivalente, y por eso está aquí escrito. Un hueco explicado es operable; uno silencioso es el
defecto contra el que existe el resto de la observabilidad de este repositorio.

**Ninguna mutación entra en esa lista.** `ReadObservationMeterFilterTest` lo comprueba en las dos
direcciones: que cada nombre denegado corresponde a un `@Observed` que existe de verdad —una entrada
podrida enseña a no leer la lista— y que ninguna operación que mueve dinero pierde su latencia.

## Trabajos programados

`job.name` también usa `lowercase.dot.notation`, por ejemplo:

```text
security.tokens.cleanup
dian.contingency.retry
```

**El nombre ya no se escribe como literal en cada job** (backend #609): sale de
`ScheduledJobCatalog`, que declara además la expresión `cron`, su clave de propiedad y si el
barrido tolera más de una réplica. `ScheduledJobTelemetry.observe` exige una constante de ese enum
para los barridos de calendario; solo las dos sondas de muestreo continuo
(`database.availability`, `business.metrics.snapshot`) siguen pasando una cadena.

El motivo es que el nombre es la etiqueta de la que cuelgan las alertas: escrito como literal, un
typo creaba una serie nueva **sin romper nada** y dejaba la alerta vigilando un nombre que ya no
emitía nadie, verde para siempre.

De la cadencia declarada allí cuelga también el umbral de `VetSoftwareScheduledJobOverdue`, que se
publica como serie (`vetsoftware.scheduled.job.expected.interval`) en vez de escribirse en el
fichero de reglas: así, cambiar la cadencia mueve la alerta con ella.

`ScheduledJobTelemetry` rechaza nombres que no cumplan la convención. La prueba
`ObservationNamingConventionTest` examina todas las anotaciones `@Observed`, exige nombres
únicos y evita regresiones de formato; `ScheduledJobCatalogParityTest` comprueba que la expresión
`cron` de cada `@Scheduled` coincide **literalmente** con la del catálogo, en las dos
direcciones, y que ningún barrido de calendario vuelve a `fixedDelay`.

## Campos de log estructurado

Los campos de MDC y de `addKeyValue(...)` usan la misma `lowercase.dot.notation` que las
observaciones (`actor.employeeId`, `http.status`, `company.identifier`), alineada con las semantic
conventions de OpenTelemetry. Las claves propias se declaran en `MdcKeys`.

A diferencia de los nombres de observación, aquí el nombre no basta: **cada campo debe declararse
además en `LogFieldPolicy`**, que es la allowlist de salida. Un campo no declarado se emite como
`***`. Ver `docs/POLITICA_REDACCION_LOGS.md`.

## Métricas del asistente comercial con IA (`aiproposal`)

Cinco métricas bajo `vetsoftware.business.ai.proposal.*`, más los dos medidores del barrido de
retención, que se movieron a ese mismo prefijo en esta fase.

| Métrica | Etiquetas | Series | Qué responde a las 3 de la mañana |
|---|---|---:|---|
| `vetsoftware.business.ai.proposal.generated` | `ai.operation`(2) · `ai.outcome`(6) · `ai.presentation`(4) | ≤48, ~14 | ¿Convierte el embudo, y por qué camino salió cada propuesta? |
| `vetsoftware.business.ai.proposal.spend` | — | 1 | ¿Cuánto llevamos gastado? Contador acumulativo, en USD |
| `vetsoftware.business.ai.proposal.spend.today` | — | 1 | ¿Cuánto queda del cupo de hoy? Gauge que se reinicia al rotar el día |
| `vetsoftware.business.ai.proposal.reason.rejected` | `reason.rule`(9) | 9 | ¿Está derivando el prompt? Cada regla del saneador por separado |
| `vetsoftware.business.ai.proposal.invalid.lines` | `line.verdict`(5) | ≤5 | ¿Está alucinando el modelo códigos de catálogo? |
| `vetsoftware.business.ai.proposal.retention.rows` | `retention.step`(6) | 6 | ¿Movió filas cada paso del barrido, o hay uno plano? |
| `vetsoftware.business.ai.proposal.retention.batches.exhausted` | — | 1 | ¿El barrido pierde terreno contra el ritmo de entrada? |

**Ni un identificador, y aquí la regla es más dura que en el resto del catálogo**: los valores de
este bloque los origina un tercero anónimo de internet. El texto libre del prospecto, la prosa del
modelo, el código de catálogo que el modelo inventó y el `public_token` de 43 caracteres **no son
etiqueta de nada**. Lo alto de cardinalidad vive donde corresponde: como atributo del span
(`proposal.id` —el `BIGINT`, nunca el token—, `ai.input.chars`, `proposal.invalid.lines`,
`proposal.rejected.reasons`), emitido con `highCardinalityKeyValue` para que llegue a la traza y
**no** a la métrica.

**El desenlace no es el enum de dominio, y no es un descuido.** `AiProposalMetrics.Outcome` tiene
un valor que `GenerationOutcome` no puede tener —`no_catalog`, cuando no hay tarifa publicada y el
caso de uso responde 200 con cero líneas sin llegar a invocar nada—. Ese camino no emitía **ninguna
señal**: el producto respondía vacío a todos los prospectos a la vez y la única evidencia era que
nadie compraba.

**Dos métricas de gasto y no una**, porque son dos preguntas: el contador sobrevive a un envío OTLP
perdido (la agregación de Micrometer es acumulativa y el scrape siguiente trae el total) y el gauge
se reinicia solo al cambiar el día, que es justo lo que un contador no sabe hacer. Las dos las
publica `InProcessDailySpendGuard`, que es el único punto por el que pasan **todos** los cargos —
incluido el del intento que falló después de pagar, donde no hay `ModelUsage` que leer.

### Lo que se decidió NO medir, con su motivo

- **Un histograma de latencia del modelo** (`gen_ai.client.operation.duration` del anexo B).
  `BedrockDisabledInvoker` es el único invocador desplegado y devuelve `isAvailable() =
  false`, así que hoy publicaría ~80 series permanentemente a cero (histograma + `LongTaskTimer`
  `_active_`) y una alerta de p95 que no podría dispararse jamás. La latencia por intento **sí**
  está, como span `aiproposal.model.invoke` con su `error.type`; convertirla en histograma es un
  cambio de código, no una migración, y toca cuando entre el cliente de Bedrock.
- **`ai.input.size` como histograma.** `ProspectText` está acotado a 1.000 caracteres por
  construcción, así que «alguien está pegando 50 KB» —el escenario que esa métrica existía para
  separar— lo rechaza el DTO con un 400. La longitud sigue estando, como atributo de span.
- **`refinement.round` y la conversión (`quoted`, `registered`).** La primera responde una pregunta
  de producto, no de operación. La segunda **no se puede emitir todavía**: exige persistir el
  `proposal.id` en la cotización y en el alta de la empresa, y eso es una migración de esquema, no
  telemetría.
- **Ninguna alerta nueva en `docker/prometheus-business-alerts.yml`.** Ese fichero no tiene gemelo
  en la nube por decisión documentada en su cabecera, así que una alerta de coste puesta ahí
  **parecería configurada y no avisaría a nadie, nunca**. Las tres `critical` de esta rodaja
  —presupuesto diario superado, ráfaga de gasto anómala y proveedor mal configurado— van a
  `VetSoftwareIaC/observability/grafana-managed/` con `noDataState: OK`, y las escribe el dueño del
  repositorio de infraestructura.

### Spans

| Span | Tipo | Estado |
|---|---|---|
| `aiproposal.generate` / `aiproposal.refine` | `@Observed` en el caso de uso | **`Unset` siempre.** Una degradación sirvió una propuesta utilizable con HTTP 200; marcarla `Error` haría que la traza contradiga al log, al código de estado y al SLI |
| `aiproposal.model.invoke` | observación manual, un span **por intento** | `Error` cuando el proveedor falla, con `error.type` del vocabulario cerrado |
| `aiproposal.suppression` | `@Observed`, ruta `SYSTEM` | — |

**La excepción del proveedor no se registra en el span.** Es la única desviación deliberada de
«registra la excepción además de fijar el estado», y el motivo es R1: el mensaje de una excepción
de SDK puede arrastrar el cuerpo de la petición, y el cuerpo lleva el texto del prospecto. Un
`observation.error(e)` lo escribiría como `exception.message` y `exception.stacktrace` del span, y
**los spans no pasan por `RedactingAppender`** —es un appender de Logback y las trazas salen por
OTLP directo—. Lo que sale es el `error.type` cerrado y, en el log, el `failureCode` saneado a
`[A-Z][A-Z0-9_]` con 40 caracteres de tope, que además neutraliza la inyección de log de ASVS
V7.3.1.

### Niveles

Lo que decide el nivel es `AiErrorType.esSistemico()`, no la gravedad aparente:

| Caso | Nivel | Por qué |
|---|---|---|
| Modelo no disponible / sin hints / catálogo sin publicar | `INFO` **una vez por proceso** | Configuración aplicada, no avería. Un endpoint público recibe tráfico continuo: un evento por petición es la tormenta que enseña a ignorar el canal. El recuento vive en `ai_proposal_generated_total` |
| Tope de gasto agotado | `WARN` **una vez al día** | Hay camino de recuperación y es una decisión deliberada del sistema. Quien despierta a alguien es la alerta de coste |
| Timeout / 429 / 5xx / salida ilegible del modelo | `WARN` | Fallo aislado con degradación ya implementada |
| `unauthorized` · `forbidden` · `invalid_request` · `model_access_not_enabled` · `_other` | **`ERROR`** | Determinista: fallará el 100 % hasta que una persona cambie configuración |
| Estimación de coste no utilizable | **`ERROR`** | Apaga la IA para todos y nadie lo reintenta |
| 4xx del endpoint público (texto vacío, JSON roto, token inexistente) | nada / `DEBUG` | El sistema funcionó. Su vigilancia es una métrica de tasa, no un log |
