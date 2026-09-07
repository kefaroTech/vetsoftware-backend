# Auditoría de modelo de datos — integración Wompi (2026-09-05)

Contra `WOMPI-especificacion-integracion.md` §2-4.1. Verificado leyendo los changesets
252/253/319-322/326/339 y las entidades JPA de `subscriptionpayment`,
`subscriptionpaymentmethod`, `paymentattempt` en el worktree
`MainVetSoftware-wompi/VetSoftware` (rama `feature/pasarela-pago-wompi`). Sin consulta a
base de datos viva: todo lo de abajo sale de DDL declarado y código fuente.

## Veredicto general

El modelo de "Capa K" (252, 253, 319-322, 326) ya cubre casi todo lo que Wompi necesita: fue
diseñado para una pasarela de tarjeta tokenizada con webhook, no para transferencia/efectivo
manual, y eso se nota. Hay **un ajuste de esquema real que sí hace falta para esta entrega**
(`payment_attempts.gateway_decline_code`, hallazgo 1) y dos puntos de implementación que no
tocan el esquema pero sí determinan si el código nuevo escribe basura (mandate_evidence al
límite, hallazgo 2). El resto: SUFICIENTE.

## Tabla de veredictos

| Requisito | Tabla/columna | Veredicto | Evidencia |
|---|---|---|---|
| Guardar `payment_source_id` (entero de Wompi) como token | `subscription_payment_methods.token VARCHAR(255) ascii_bin` | SUFICIENTE | `319:76-77`; `String.valueOf(paymentSourceId)` cabe holgado (≤10 dígitos) |
| Unicidad del token por pasarela | `uq_subscription_payment_methods_token (gateway, token)` | SUFICIENTE | `319:90-91`; `JpaSubscriptionPaymentMethodRepository.findByGatewayAndToken` ya existe y el comentario del propio repo justifica por qué la búsqueda es global y no fuga (`SubscriptionPaymentMethodJpaRepository.java:19-28`) |
| Constancia del mandato (`wompi:ps=…;acc=…;pda=…;at=…`) | `mandate_evidence VARCHAR(255) NOT NULL` | AJUSTE MENOR (política de app, no de esquema) | `319:50-52`; ver hallazgo 2 — el margen medido es de 4 caracteres con permalinks reales del propio proveedor |
| `expires_on` derivado de `exp_month/exp_year`, obligatorio en CARD | `subscription_payment_methods.expires_on DATE` + `chk_subscription_payment_methods_card_shape` | SUFICIENTE | `319:97-101`; espejo exacto en dominio `SubscriptionPaymentMethod.java:244-246` (`expiresOn is required for a CARD payment method`) |
| `brand`/`last_four` obligatorios en CARD, prohibidos en PSE | mismo CHECK | SUFICIENTE | `319:97-101`; dominio `validateShape` (`SubscriptionPaymentMethod.java:235-249`) |
| Id de transacción Wompi (`"01-1532941443-49201"`) | `subscription_payments.gateway_reference VARCHAR(120) ascii_bin` | SUFICIENTE | `252:97`, `252:160-162`; ~20 chars reales contra 120 disponibles |
| Referencia propia `VS-<subscriptionNumber>-P1` | `subscription_payments.client_request_id VARCHAR(64) ascii_bin` | SUFICIENTE | `252:110-112`, `252:164-166`; `subscription_number` es `VARCHAR(30)` (`242:21`) y `Subscription.MAX_NUMBER_LENGTH = 30` (`Subscription.java:19`) ⇒ máximo real `"VS-" + 30 + "-P1"` = 36 de 64 |
| Barandilla del webhook (no duplicar el mismo aviso) | `uq_subscription_payments_gateway (gateway, gateway_reference)` | SUFICIENTE | `252:123-125`; global a propósito, motivo declarado en el propio changeset (`252:8-11`) |
| Idempotencia del pago manual/doble clic | `uq_subscription_payments_client_request (company_id, client_request_id)` | SUFICIENTE | `252:126-128` |
| Guardar referencia propia Y id de pasarela a la vez | mismas dos columnas | SUFICIENTE | Confirmado en el diseño (4.1.3.e): `clientRequestId` y `gateway_reference` (`transaction.id`) se escriben en la misma fila, cada uno en su columna y cada uno con su propia unicidad — no hay colisión de responsabilidad |
| Ciclo PENDING→CONFIRMED/FAILED del webhook | `chk_subscription_payments_status IN ('PENDING','CONFIRMED','FAILED','REFUNDED')` | SUFICIENTE | `252:135-136`; `ChangeSubscriptionPaymentStatusUseCase` ya transiciona ambos sentidos (`ChangeSubscriptionPaymentStatusService.java:64-89`) |
| `fee_amount`/`net_amount`/`settlement_reference` nulos hasta liquidar | `chk_subscription_payments_net`, `chk_subscription_payments_settlement` | SUFICIENTE, no bloquea nada | `252:147-150` (fee/net van juntos o ninguno) y `252:153-156` (settlement_reference+settled_on+gateway van juntos o ninguno); un pago CONFIRMED con las tres en NULL es una fila válida |
| Pago PENDING aplicado no reduce el saldo | `subscription_billing_documents.settled_amount` / `balance_amount` | SUFICIENTE | `recalculateSettledAmount` filtra `(a.source_kind='CREDIT_NOTE' OR p.status='CONFIRMED')` (`BillingDocumentSettlementJpaRepository.java:64-77`); `balance_amount` es columna `GENERATED ALWAYS AS (total_amount - settled_amount) VIRTUAL` (`249:96-97`), no editable desde código |
| El recálculo se dispara al confirmar | — | SUFICIENTE | `ChangeSubscriptionPaymentStatusService.recalculateAffectedDocuments` llama a `settlementPort.recalculateSettledAmount` para cada documento afectado tras el cambio de estado (`ChangeSubscriptionPaymentStatusService.java:96-105`) |
| `gateway_decline_code` frente al `status_message` de Wompi | `payment_attempts.gateway_decline_code VARCHAR(50) ascii_bin` | **FALTA (ajuste de esquema para esta entrega)** | Ver hallazgo 1 |
| `chk_payment_attempts_declined_by_gateway` exige código salvo CONFIGURATION | mismo CHECK | SUFICIENTE en su lógica, condicionado al hallazgo 1 | `321:88-89`; la exigencia de NOT NULL es correcta, el problema es lo que le cabe dentro |
| Idempotencia de webhook: ¿falta `gateway_webhook_events`? | — | NO BLOQUEANTE — nota | Ver hallazgo 3 |
| Webhook busca pago por `(gateway, gateway_reference)` sin empresa | `SubscriptionPaymentJpaRepository.findByGatewayAndGatewayReference` | SUFICIENTE, sin fuga | Búsqueda global intencional (mismo criterio documentado que `findByGatewayAndToken`); el resultado nunca se devuelve a un cliente, solo alimenta una mutación interna `SYSTEM` |
| Cobro busca medio por defecto acotado por empresa | método nuevo sobre `SubscriptionPaymentMethodJpaRepository` (por escribir) | SUFICIENTE en el esquema, pendiente de implementación disciplinada | Ver hallazgo 4 (nota de implementación, no de esquema) |
| Índice para medio por defecto de la empresa | `uq_subscription_payment_methods_default (default_marker)` + `ix_subscription_payment_methods_mandate (company_id, mandate_status)` | SUFICIENTE | `319:88-89`, `319:112-116`; ambos ya existen, ver hallazgo 4 para cuál usar |
| Índice para contrato vigente por empresa | `uq_subscriptions_active_company` sobre columna generada `active_marker` | SUFICIENTE | Mismo patrón K que `default_marker`; `FindCurrentSubscriptionService.findCurrent` → `SubscriptionRepository.findCurrentByCompanyId` ya lo usa (`FindCurrentSubscriptionService.java:20-24`) |

## Hallazgo 1 — `gateway_decline_code` no puede recibir el `status_message` de Wompi tal cual

**Qué está mal.** `payment_attempts.gateway_decline_code` es `VARCHAR(50) CHARACTER SET ascii
COLLATE ascii_bin` (`321:78-79`, `321:57`). El diseño acordado (§4.1.3.f) pasa **el
`statusMessage` crudo de Wompi** como valor de ese campo cuando `charge()`/`findTransaction()`
devuelve `DECLINED|ERROR|VOIDED`. Wompi no documenta un código corto y estable para el rechazo
(no hay campo tipo `decline_code`): solo expone `status_message`, que es **texto libre en
español** — el propio criterio del changeset 321 (`321:28-31`) asume una pasarela que devuelve
"un catálogo" de códigos comparables exactos, y Wompi no encaja en esa suposición.

Dos problemas independientes, no uno:

1. **Longitud.** No pude obtener de `docs.wompi.co` un `status_message` de ejemplo verbatim
   (los dos intentos de `WebFetch` no lo trajeron: uno no cubría el endpoint, el otro devolvió
   403). Es razonable esperar mensajes en prosa ("Transacción rechazada por el banco emisor:
   fondos insuficientes") que superen 50 caracteres con facilidad — **esto no está medido
   contra un ejemplo oficial**, queda como riesgo razonado, no como hecho verificado.
2. **Charset — esto sí es un hecho, no una proyección.** La columna es `CHARACTER SET ascii`.
   Un `status_message` en español con tildes o eñes (`"autorización"`, `"año"`) no es
   representable en `ascii` de 7 bits. MySQL 8.4 en modo estricto (el `sql_mode` por defecto de
   MySQL 8.x incluye `STRICT_TRANS_TABLES`, y no encontré ningún parameter group en
   `VetSoftwareIaC` que lo desactive — **no verificado contra el parameter group real de RDS**,
   solo contra lo que hay en Terraform) rechaza el `INSERT` con `Incorrect string value` en
   cuanto aparece un carácter fuera de ese rango. Si eso ocurre, `RecordPaymentAttemptUseCase`
   lanza una excepción no capturada por nada en el flujo descrito en 4.1.3.f, y el intento
   fallido **no queda registrado** — justo la bitácora que el changeset 321 dice que existe
   "para no degradar a nadie sin dejar rastro" (`321:39-41`) se rompe exactamente cuando más
   hace falta: en un rechazo real.

**Criterio.** Manual de MySQL, juegos de caracteres de columna (comportamiento de `sql_mode`
estricto y rechazo de cadenas fuera de rango: manual §5.1.11 "Server SQL Modes", no vuelto a
consultar con `WebFetch` en esta pasada). Principio 5 de este criterio de datos: los tipos son
la decisión más cara de revertir, y aquí el charset es demostrablemente incorrecto para el dato
que va a recibir, antes de que exista una sola fila con el problema.

**Impacto.** Grave, no bloqueante: no corrompe datos entre tenants ni pierde dinero, pero puede
tumbar silenciosamente el registro de auditoría de cobranza en el primer rechazo con texto
no-ASCII — en un cobro Wompi en español, el caso común, no el raro.

**Arreglo — changeset nuevo, para `db-migrations`.** `payment_attempts` está desplegada por 321
pero sin tráfico real de Wompi todavía; el `ALTER` es barato hoy y caro con datos reales dentro:

```sql
ALTER TABLE payment_attempts
    MODIFY COLUMN gateway_decline_code VARCHAR(160)
        CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL;
```

- **160, no 50**: margen para una frase corta en español. El cambio de charset (`ascii`→`utf8mb4`)
  hace que la operación sea como mínimo *INPLACE*, no *INSTANT* (verificar contra el manual de
  operaciones DDL online de InnoDB antes de aplicar; no medido). A la escala actual (tabla nueva,
  filas en el orden de cientos) el coste es irrelevante aunque reconstruya la tabla.
- `utf8mb4_0900_ai_ci` y no `ascii_bin`: no hay `UNIQUE` sobre esta columna, así que no aplica el
  criterio de colación exacta que sí rige `gateway_reference`/`token`. Es texto para humanos.
- **Verificación tras aplicar**: `SHOW CREATE TABLE payment_attempts` en local confirma el
  charset; un `INSERT` de prueba con "ó" debe pasar sin error contra `mysql-local` (no ejecutado
  en esta auditoría, dejar para `db-migrations`/`backend-tests`).

## Hallazgo 2 — `mandate_evidence` cabe por 4 caracteres con permalinks reales, no con margen

**Medido.** `docs.wompi.co` no publica un ejemplo colombiano completo del payload de
`GET /merchants/{public_key}`; sí hay un ejemplo oficial de Wompi Panamá (mismo proveedor, misma
convención de nombre de archivo) con el permalink
`https://wompi.pa/wp-content/uploads/2019/09/TERMINOS-Y-CONDICIONES-DE-USO-USUARIOS-WOMPI.pdf`
(**92 caracteres**). Construyendo la cadena completa con un `payment_source_id` de 6 dígitos, un
segundo permalink de autorización de datos personales con nombre de archivo de longitud
comparable (~103 caracteres, estimado por convención de nomenclatura, no verificado) y el
`at=` en ISO-8601 con microsegundos:

```
wompi:ps=123456;acc=<92>;pda=<103>;at=2026-09-05T15:30:00.123456Z
```

da **251 caracteres** contra un límite de **255** (`mandate_evidence VARCHAR(255)`, `319:50-52`,
`MAX_EVIDENCE_LENGTH = 255` en `SubscriptionPaymentMethod.java:45`). Margen: 4 caracteres. Un
`payment_source_id` de 7 dígitos, o un nombre de archivo de la autorización de datos personales
apenas 5 caracteres más largo que mi estimación, ya desborda.

**Criterio.** El propio diseño (§4.1.2) ya previó el problema: *"si no cabe, recorta los
permalinks a su último segmento"*. Lo que esta medición aporta es que **la rama de recorte no es
un caso de borde, es el camino esperado**: con datos reales del proveedor, la versión sin recortar
falla más veces de las que pasa.

**Impacto.** Grave si se ignora: `RegisterSubscriptionPaymentMethodCommand` fallaría con
`IllegalArgumentException` (validación de dominio, `SubscriptionPaymentMethod.java:221`) o, peor,
con una violación de columna si el dominio no se ejecuta antes de persistir — cualquiera de las
dos tumba el alta de medio de pago justo cuando el cliente ya tokenizó su tarjeta.

**Arreglo — no es de esquema, es de implementación; se lo dejo escrito para quien construya
`CreateWompiPaymentSourceUseCase`**: no condicionar el recorte a "si no cabe" — recortar **siempre**
los dos permalinks a su último segmento de ruta (`acc=TERMINOS-Y-CONDICIONES-DE-USO-USUARIOS-WOMPI.pdf`)
antes de componer `mandate_evidence`. Con esa política la cadena mide ~163 caracteres en el
ejemplo medido, con margen real. Ningún cambio de esquema: `VARCHAR(255)` sigue siendo correcto,
el ajuste vive en el use case.

## Hallazgo 3 — `gateway_webhook_events`: recomendación, no bloqueante

**Sin ella.** La idempotencia actual descansa en que el pago llega a un estado final
(`CONFIRMED`/`FAILED`) la primera vez que se procesa el evento, y en que Wompi reintenta el mismo
evento con el mismo `transaction.id` (mismo `checksum`, porque el checksum se calcula sobre datos
que no cambian entre reintentos). `ProcessWompiEventUseCase` (§4.1.4) ya contempla "si el pago ya
está en estado final ⇒ idempotente, 200 sin tocar nada". Para el patrón de reintento documentado
de Wompi (3 reintentos en 24 h, mismo evento) esto basta: no hay ventana donde el mismo aviso
produzca dos cargos ni dos cambios de estado.

**Lo que sí se pierde sin la tabla**: si el evento llega y `findByGatewayAndGatewayReference` no
encuentra el pago (fuera de orden, o un fallo transitorio entre el `charge()` y el
`RegisterSubscriptionPaymentUseCase` de la misma petición), el diseño actual solo deja "log WARN y
200" — Wompi no reintenta un 200, así que ese evento se pierde para siempre salvo que quede en los
logs de aplicación (no consultable con SQL, sujeto a retención de logs, no a retención de datos).
No hay corrupción ni fuga: hay ausencia de rastro auditable de un evento real recibido.

**Recomendación**: no bloqueante para esta entrega — la probabilidad de esa carrera es baja porque
el alta del `SubscriptionPayment` en estado `PENDING` ocurre de forma síncrona, en el mismo hilo,
inmediatamente después de que `charge()` devuelve el `id` de transacción (§4.1.3.e), antes de que
exista ninguna oportunidad razonable para que Wompi ya haya llamado al webhook. Se marca como
**opcional para esta entrega** y se deja la ficha lista por si se decide adelantarla o por si un
incidente real la pide después.

### Ficha propuesta (opcional) para `db-migrations`

Columnas: `id BIGINT AUTO_INCREMENT PK`; `gateway VARCHAR(40) ascii_bin NOT NULL`;
`event_type VARCHAR(60) ascii_bin NOT NULL` (`"transaction.updated"`);
`event_checksum VARCHAR(64) ascii_bin NOT NULL` (el SHA-256 hex del webhook — es el checksum lo que
hace de llave de idempotencia, Wompi no expone un id de evento propio); `gateway_reference
VARCHAR(120) ascii_bin NULL` (`transaction.id`, nulable por si el cuerpo llega malformado antes de
poder extraerlo); `received_at DATETIME(6) NOT NULL`; `raw_body TEXT NULL` (utf8mb4, cuerpo crudo
del POST — nulable a propósito: la retención del cuerpo crudo la resuelve un job que lo vacía
pasados N días, ver más abajo); `processed_at DATETIME(6) NULL`; `processing_outcome VARCHAR(30)
NULL` con CHECK `IN ('APPLIED','IGNORED_UNKNOWN_EVENT','IGNORED_ALREADY_FINAL','PAYMENT_NOT_FOUND',
'REJECTED_CHECKSUM','REJECTED_STALE','REJECTED_AMOUNT')`; `created_date`; `version BIGINT` (con
`@Version`: la fila se escribe dos veces — alta sin procesar, luego relleno de
`processed_at`/`processing_outcome` — es una segunda escritura declarada, no una exención).
`REJECTED_AMOUNT`: el evento trae un `amount_in_cents` distinto del pago registrado; el pago se
deja en `PENDING`. `REJECTED_STALE` (#789): el evento llega fuera de la ventana de frescura que
valida `ProcessWompiEventUseCase` antes de aplicarlo — evita reprocesar un webhook demorado contra
un pago que ya avanzó por otro camino.

**Retención de `raw_body` (#791).** La fila completa (`gateway`, `event_type`, `event_checksum`,
`processing_outcome`) es evidencia contable y se conserva indefinidamente; solo `raw_body` —el
cuerpo crudo, con datos potencialmente sensibles del payload de Wompi— se purga pasados N días.
Un job vacía la columna (`UPDATE gateway_webhook_events SET raw_body = NULL WHERE received_at <
:umbral AND raw_body IS NOT NULL`) sin tocar el resto de la fila. Ese barrido filtra por
antigüedad de llegada, no por si el evento ya se procesó, así que
`ix_gateway_webhook_events_pending (processed_at)` no le sirve:
`ix_gateway_webhook_events_purge (received_at)` es el índice nuevo que necesita.

**`DUPLICATE` no entra en el catálogo del CHECK**, aunque sea un desenlace real (mismo checksum ya
recibido; se responde 200 sin reprocesar): `uq_gateway_webhook_events_checksum` impide insertar una
segunda fila con el mismo `(gateway, event_checksum)`, así que no hay fila nueva donde grabar ese
valor, y sobrescribir el `processing_outcome` de la fila original borraría el desenlace real del
primer intento. El servicio trata el reintento como una lectura de la fila existente —comprueba el
checksum, ve que ya tiene un desenlace, y responde 200— y no como una escritura con outcome propio.

Constraints: `uq_gateway_webhook_events_checksum UNIQUE (gateway, event_checksum)` (la barandilla
de idempotencia); `chk_gwe_processed CHECK ((processed_at IS NULL AND processing_outcome IS NULL)
OR (processed_at IS NOT NULL AND processing_outcome IS NOT NULL))`. Índices:
`ix_gateway_webhook_events_reference (gateway, gateway_reference)` para correlacionar con
`subscription_payments`; `ix_gateway_webhook_events_pending (processed_at)` para el barrido de
reconciliación; `ix_gateway_webhook_events_purge (received_at)` para el barrido de retención que
vacía `raw_body` (#791). **Sin `company_id`** (el evento llega sin empresa, tabla de plataforma pura, mismo
criterio que `gateway_settlements`, `326`) y **sin FK** a `subscription_payments`: debe poder
persistirse incluso cuando NO se encuentra el pago correspondiente — para eso existe.

## Hallazgo 4 — nota de implementación: cómo consultar el medio por defecto

`default_marker` (`319:80-87`) es la vía más barata para "medio por defecto ACTIVE de la empresa":
un `WHERE default_marker = :companyId` golpea directamente `uq_subscription_payment_methods_default`,
que es una igualdad sobre clave única. Pero la entidad JPA **no mapea esa columna a propósito**
(`SubscriptionPaymentMethodJpaEntity.java:21-30`, comentario explícito: mapearla rompería todos los
`INSERT`). El nuevo método de repositorio que pida `ChargeContractFirstPeriodUseCase` (§4.1.3.c)
debe construirse como JPQL sobre `companyId + defaultMethod + mandateStatus` (cubierto por
`ix_subscription_payment_methods_mandate (company_id, mandate_status)`, `319:112-116`), filtrando
`gateway = 'WOMPI'` en memoria o en el propio `WHERE` — el volumen por empresa (unos pocos medios
como máximo) hace irrelevante cuál de las dos rutas se elija. No hace falta índice nuevo. Esto
es guía de implementación, no un hallazgo de esquema.

## Cambios de esquema necesarios para esta entrega

Uno solo, real y necesario hoy — el resto de la tabla anterior es SUFICIENTE tal cual está:

- **`payment_attempts.gateway_decline_code`**: ampliar a `VARCHAR(160)` y cambiar de
  `CHARACTER SET ascii COLLATE ascii_bin` a `CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci`
  (hallazgo 1). Changeset nuevo a partir del siguiente número libre en
  `db.changelog-master.xml` (`407` es el último al momento de este informe — confirmar el
  siguiente disponible al escribirlo, no asumir `408`). Rollback: `MODIFY COLUMN` de vuelta a
  `VARCHAR(50) CHARACTER SET ascii COLLATE ascii_bin NULL` (seguro solo si no hay filas con
  datos no-ASCII o de más de 50 caracteres; verificar antes del rollback si llega a usarse).

`gateway_webhook_events` (hallazgo 3) queda **explícitamente fuera** de "necesario para esta
entrega": es una mejora, con su ficha completa lista si se decide adelantarla.

## MEDIDO / NO MEDIDO

**Medido**: longitudes de columna contra los changesets 252/253/319-322/326/339 (lectura directa);
`subscription_number` = `VARCHAR(30)` y `MAX_NUMBER_LENGTH` en dominio (`242:21`,
`Subscription.java:19`); longitud de un permalink real de Wompi (Panamá, vía búsqueda web) = 92
caracteres; aritmética de `mandate_evidence` con esos datos.

**No medido**: un `status_message` real de Wompi para transacción `DECLINED` (dos intentos de
`WebFetch` contra `docs.wompi.co` no lo trajeron: uno cubría otro endpoint, el otro devolvió 403);
el `sql_mode` real del parameter group de RDS dev/prod (inferido del default de MySQL 8.4, no
confirmado contra la instancia — instrucción del encargo prohíbe consultar dev); el coste exacto
(`INSTANT`/`INPLACE`) del `ALTER` propuesto en el hallazgo 1 contra una instancia real; no se
ejecutó ningún `EXPLAIN` porque ninguna de las consultas nuevas necesita índice adicional según
lo que ya existe.

## Fuentes citadas

- Manual de MySQL 8.4, juegos de caracteres y colaciones de columna (base del hallazgo 1, sin URL
  específica consultada en esta pasada — criterio general del motor, no verificado con `WebFetch`
  en este informe).
- `docs.wompi.co` — hechos de §2 de la especificación vigente, ya verificados por quien la redactó;
  esta auditoría solo añadió la búsqueda de un ejemplo real de permalink (resultado: Wompi Panamá,
  vía búsqueda web, no `docs.wompi.co` directamente) y no logró un ejemplo de `status_message`.
- `Subscription.java`, `SubscriptionPaymentMethod.java`, `ChangeSubscriptionPaymentStatusService.java`,
  `BillingDocumentSettlementJpaRepository.java` — código fuente del worktree, citado línea a línea
  arriba.

## ISSUES ABIERTOS

Ninguno. El único hallazgo con acción pendiente (hallazgo 1) se resuelve con un changeset de esta
misma entrega, no con un issue para después; los hallazgos 2 y 4 son guía de implementación sin
cambio de esquema; el hallazgo 3 queda documentado en este mismo informe como mejora opcional, con
su ficha completa lista, y no requiere seguimiento en GitHub porque no hay decisión pendiente que
se pierda si nadie la retoma — la ficha queda aquí para cuando se necesite.
