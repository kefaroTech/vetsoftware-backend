# Cuenta gratuita — modelo de datos

**Estado:** especificación normativa para `db-migrations` (changesets 417+) y `backend-feature`
(BF-A/B/C/D). Audita las decisiones de negocio del 2026-09-07 (`cuenta-gratuita-PLAN.md`) contra
`MainVetSoftware/models/modelo-datos-suscripciones.html`, `docs/db/suscripciones-*.md` y el esquema
real hoy en el árbol (último changeset: `416`).

**Premisa que gobierna todo este documento (reforzada por el usuario mientras se escribía):** la
base de datos se asume **vacía** para toda definición nueva. Nada de retrocompatibilidad: sin
backfills de datos preexistentes, sin columnas duales, sin rutas de compatibilidad. Un `CHECK`
viejo se sustituye por el nuevo sin transición. La única restricción que sigue en pie es de
mecánica de Liquibase, no de datos: **un changeset ya desplegado en dev no se edita** (rompería su
checksum), así que todo cambio va en changesets **nuevos** (417+); pero el contenido de esos
changesets no contempla filas existentes que migrar — no las hay.

---

## 1. Papel de la concesión en el alta pública

### Decisión

**Se añade un tercer papel explícito, `origin = 'SIGNUP'`, en las dos tablas — no se relaja el
arco a "puede quedar vacío".** El papel del alta pública es la propia decisión de política del
catálogo (todo artículo `ELIGIBLE` se prueba automáticamente al registrarse), y esa decisión
necesita ser un valor declarado, no la ausencia de los otros dos. "Una prueba sin papel no se
puede defender" sigue siendo cierto: lo que cambia es que ahora hay **tres** papeles posibles, y
uno de ellos es "la política pública lo concedió", auditable exactamente igual que los otros dos.

Se rechaza la alternativa de solo permitir `source_quote_id`/`granting_amendment_id` en `NULL`
sin una columna que lo declare, por lo mismo que este esquema ya rechazó las columnas nulas
implícitas en `chk_catalog_items_trial_policy` y en `catalog_items.trial_eligibility` (§1.8 de
`suscripciones-modelo.md`): un `NULL` sin discriminador no dice "esto es SIGNUP", dice "no sé por
qué esta fila no tiene papel", y un olvido de escritura en el camino de cotización quedaría
indistinguible de un alta pública legítima.

### DDL/cambio

**Verificado en el esquema real:** `company_trial_windows.source_quote_id` es `NOT NULL` con FK
simple a `quotes(id)` (`301_create_company_trial_windows.xml`). `company_trial_grants` ya tiene
`source_quote_id` **nulable** (sin `<constraints nullable="false"/>` en `302`) y
`granting_amendment_id` nulable; el arco lo cierra `chk_company_trial_grants_paper`
(`((source_quote_id IS NOT NULL) + (granting_amendment_id IS NOT NULL)) = 1`).

**Changeset 417 — `company_trial_windows`:**

```sql
ALTER TABLE company_trial_windows
    MODIFY COLUMN source_quote_id BIGINT NULL,
    ADD COLUMN origin VARCHAR(10) NOT NULL;

ALTER TABLE company_trial_windows
    ADD CONSTRAINT chk_company_trial_windows_origin
        CHECK (origin IN ('SIGNUP', 'QUOTE')
           AND ((origin = 'SIGNUP' AND source_quote_id IS NULL)
             OR (origin = 'QUOTE'  AND source_quote_id IS NOT NULL)));
```

`fk_company_trial_windows_quote` (definida en `301_link_company_trial_windows_fk`) no cambia:
una FK simple con columna nulable ya admite `NULL` sin problema (§4.6 de `suscripciones-modelo.md`
documenta exactamente esta semántica para FK con columna nulable, aunque ahí sea el caso de FK
compuestas — con una FK simple la regla es más simple todavía: `NULL` nunca se comprueba).

**Changeset 418 — `company_trial_grants`:**

```sql
ALTER TABLE company_trial_grants
    ADD COLUMN origin VARCHAR(10) NOT NULL;

ALTER TABLE company_trial_grants
    DROP CONSTRAINT chk_company_trial_grants_paper;

ALTER TABLE company_trial_grants
    ADD CONSTRAINT chk_company_trial_grants_paper
        CHECK (origin IN ('SIGNUP', 'QUOTE', 'AMENDMENT')
           AND ((origin = 'SIGNUP'    AND source_quote_id IS NULL
                                       AND granting_amendment_id IS NULL)
             OR (origin = 'QUOTE'     AND source_quote_id IS NOT NULL
                                       AND granting_amendment_id IS NULL)
             OR (origin = 'AMENDMENT' AND source_quote_id IS NULL
                                       AND granting_amendment_id IS NOT NULL)));
```

Ninguna FK de `company_trial_grants` cambia: `fk_company_trial_grants_quote` y
`fk_company_trial_grants_amendment` ya tienen columnas nulables, y `fk_company_trial_grants_window`
(hacia `company_trial_windows`) no depende del papel — toda concesión, sea cual sea su origen,
sigue perteneciendo a una ventana.

### Por qué

- **`origin` se denormaliza (se copia) en vez de leerse de la tabla padre**, exactamente por la
  restricción que domina todo el diseño de `CHECK` en este esquema: *"Nongenerated and generated
  columns are permitted, except columns with the AUTO_INCREMENT attribute and **columns in other
  tables**"* (manual de MySQL 8.4,
  <https://dev.mysql.com/doc/refman/8.4/en/create-table-check-constraints.html>). Un `CHECK` de
  `company_trial_grants` no puede mirar `company_trial_windows.origin` para decidir si el arco de
  papel es válido; tiene que llevar su propia copia. Es el mismo patrón que `measure_kind` se
  copia en `catalog_item_limits`/`company_capacities`/`company_usage_events` para poder atar una
  FK compuesta contra él (§300, §314, §354): "una restricción no puede mirar otra tabla" no es una
  frase retórica de estos documentos, es literalmente lo que obliga a esta forma.
- **Nombre y vocabulario simétricos entre las dos tablas** (`origin`, valores en mayúscula sin
  guion bajo salvo el compuesto), consistente con la convención de tipos cerrados de
  `suscripciones-modelo.md §1.8`.
- **La ventana solo necesita dos papeles (`SIGNUP`, `QUOTE`)** porque una ventana es el reloj de
  la *empresa*, y una empresa nace de un registro público o de una cotización aceptada por
  consola — no hay una tercera vía de abrir ventana. **La concesión necesita tres** (`SIGNUP`,
  `QUOTE`, `AMENDMENT`) porque, además de las concesiones que nacen con la ventana, una concesión
  nueva puede llegar más tarde sobre una ventana **ya abierta**: un módulo `ELIGIBLE` que se añade
  a un contrato existente vía otrosí también merece su prueba, y ese camino ya existía
  (`granting_amendment_id`) antes de esta especificación.

### Riesgos

- **Coste del `ALTER`.** `MODIFY COLUMN ... NULL` (quitar `NOT NULL`) es *in place* pero
  **reconstruye la tabla** aunque permite DML concurrente (manual de MySQL 8.4, Tabla 17.17 —
  *Online DDL Support for Column Operations*, verificado por `WebFetch` el 2026-09-07). `ADD
  COLUMN` nulable es *in place* y **no** reconstruye. Con la base vacía en todo entorno real hoy,
  el coste es cero en la práctica; se declara para quien reutilice este patrón cuando ya existan
  filas.
- **Consecuencia de código, no de esquema, que hay que avisar a `backend-feature`:**
  `CompanyTrialWindow.java:63` exige `source_quote_id` no nulo en el dominio; con esta ALTER el
  dominio tiene que aceptar `origin`+`source_quote_id` nulable y validar el mismo arco que el
  `CHECK`. Sin ese cambio, `ddl-auto: validate` no se rompe (el dominio es más estricto que la
  base, no al revés), pero el alta pública nunca podría construir un `CompanyTrialWindow` válido.
- **No se toca `EntitlementCalculator` ni ninguna regla de ArchUnit por esta decisión**: el papel
  de la concesión es una cuestión de auditoría y trazabilidad de *por qué* existe la prueba, y no
  entra en el cálculo de permisos, que solo mira `charge_mode`/`trial_end_date`/`trial_outcome` de
  la línea del contrato (ya congelados, ver §2).

---

## 2. Líneas TRIAL en el alta y su sucesora

### Decisión

**El alta escribe una línea `charge_mode = 'TRIAL'` por cada `catalog_items` con
`trial_eligibility = 'ELIGIBLE'` del catálogo mínimo vendible**, no solo `CORE`. Con el catálogo
sembrado hoy (`308_seed_commercial_catalog_items.xml`) eso son **trece módulos** (`CORE`,
`SCHEDULING`, `CLINICAL_HISTORY`, `VACCINATION_DEWORMING`, `HOSPITALIZATION`, `SURGERY`,
`LAB_IMAGING`, `GROOMING`, `SERVICES`, `CASH_REGISTER`, `INVENTORY`, `PURCHASES`,
`OPEN_ACCOUNTS`) **más las tres capacidades del mínimo estructural** que también son `ELIGIBLE`
(`CAPACITY_USER`, `CAPACITY_BRANCH`, `CAPACITY_TERMINAL`) — dieciséis líneas en total.
`ELECTRONIC_INVOICING` es `NEVER_FREE` y **no** entra: nace `PAID` solo si el cliente la compra
(R-TRIAL-12).

**No hace falta ninguna columna nueva en `subscription_items`**: el esquema de `244` ya tiene
`charge_mode`, `trial_end_date`, `trial_eligibility`, `max_trial_days`, `succeeds_item_id`,
`activation_path` y `origin`, y cada uno recibe exactamente el valor que su `CHECK` exige.

### DDL/cambio

No hay DDL de esquema en este punto — es una especificación de **qué valores escribe** cada
caso de uso, para que `backend-feature` (BF-A) no invente ninguno:

| Columna | Valor en la línea TRIAL del alta | Por qué ese valor y no otro |
|---|---|---|
| `charge_mode` | `'TRIAL'` | Único valor legal para una línea con `trial_end_date` no nulo (`chk_subscription_items_trial_end`) |
| `trial_end_date` | `company_trial_windows.end_date` (la única ventana de la empresa) | Es el destino de `fk_subscription_items_trial_grant` — no se puede escribir otra fecha, el motor la rechazaría |
| `trial_eligibility` | copiado de `catalog_items.trial_eligibility` (`'ELIGIBLE'`) | Copia congelada, ya prevista por la ficha #15 de `suscripciones-tablas.md` y por `chk_subscription_items_never_free_is_paid` |
| `max_trial_days` | copiado de `catalog_items.default_trial_days` en el momento del alta (30, tras §4) | `chk_subscription_items_max_trial_days` exige `> 0` cuando `trial_eligibility = 'ELIGIBLE'` |
| `unit_amount`, `tax_rate`, `tax_treatment`, `included_quantity`, `tier_min` | el precio **vigente** de `catalog_prices` en el momento del alta, congelado igual que en `coreLine()`/`capacityLine()` hoy | R-TRIAL-14: una línea `TRIAL` conserva su tarifa real; es lo que hace que `BillableSubscriptionItem` no cobre nada mientras esté en `TRIAL` (el filtro es `charge_mode`, no el precio) |
| `origin` | `'INITIAL'` | Es parte del mismo contrato inicial que la línea de `CORE`; no es un `ADDON` ni una `QUANTITY_CHANGE` |
| `activation_path` | `'SELF_SERVICE'` | El registro público no pasa por `QUOTE` (no hay cotización) ni por `PLATFORM` (no interviene ningún operador); es el canal que ya existe para lo que el propio cliente desencadena sin intervención humana de VetSoftware. **Nota abierta:** hoy `activation_path` no restringe nada (D-58, comentario de `244`), así que el riesgo de elegir mal este valor es bajo, pero `backend-feature` debe confirmarlo contra el uso real que le dé la consola |
| `succeeds_item_id` | `NULL` | Es la primera línea de este artículo; no sucede a nada |

**Lo que el alta también tiene que escribir, y no es opcional:** una fila en `company_trial_grants`
por cada una de esas dieciséis líneas, con `origin = 'SIGNUP'` (§1), `days_granted =
max_trial_days`, `policy_trial_days` = el mismo valor, `policy_trial_outcome` = el
`catalog_items.trial_outcome` congelado en ese instante, y `trial_window_id`/`trial_window_end_date`
apuntando a la única ventana de la empresa. Sin esa fila, `JpaSubscriptionQueryPort.desenlace`
(`entitlement/infrastructure/persistence/JpaSubscriptionQueryPort.java:129-138`) lanza
`IllegalStateException` en el primer recálculo de permisos — es la barrera **ya construida** que
impide que una línea `TRIAL` sin concesión llegue a producción, y que confirma por qué la concesión
tiene que nacer en la misma transacción que la línea.

### Confirmación pedida: `EntitlementCalculator.grantFor` no necesita nada nuevo

**Confirmado leyendo `entitlement/domain/EntitlementCalculator.java:238-280`.** El método ya
escribe **dos filas** de una sola vez para una línea `TRIAL` vigente: la de la prueba (nivel
`FULL`, `EntitlementSource.TRIAL`, válida `[trial_start, trialCloses)`) y su sucesora (el nivel
del `TrialOutcomePolicy` congelado, válida desde `trialCloses` en adelante), siempre que la línea
siga viva después del vencimiento (`lineEnds == null || lineEnds.isAfter(trialCloses)`). La clave
única `uq_company_entitlements (company_id, sub_module_id, valid_from)` (`246`) ya admite las dos
filas por el mismo submódulo — es exactamente lo que existe para resolver R-ENT-01. **No hace
falta ninguna migración sobre `company_entitlements`.**

Lo que **sí** falta, y es código, no esquema (confirma lo que dice el brief): **no existe el
barrido de vencimiento por línea.** `SubscriptionLifecycleWorker` (`subscription/application/
usecase/SubscriptionLifecycleWorker.java:88-94`) solo mueve el **contrato** de `TRIALING` a
`ACTIVE` comparando `subscription.getTrialEndDate()` — una fecha por contrato, no por línea — y
`ConsumeTrialGrantService` (`companytrialgrant/application/usecase/ConsumeTrialGrantService.java`)
solo sella `consumed_at`+`outcome` en la concesión; **no** toca `subscription_items` ni escribe la
línea sucesora `FREE_LIMITED`/`EXPIRED_READ_ONLY`. Ese trabajo (BF-B) tiene que: (1) cerrar la
línea `TRIAL` (`effective_to = trial_end_date + 1 día`, `ended_amendment_id` del otrosí de
sistema que lo hace), (2) abrir la línea sucesora con `charge_mode` = `FREE_LIMITED` si
`policy_trial_outcome = 'LIMITED'`, `EXPIRED_READ_ONLY` si `'READ_ONLY'`, o dejarla en `TRIAL`→
conversión a `PAID` si `'CONVERT_TO_PAID'` (caso raro hoy, ningún artículo del catálogo lo usa),
con `succeeds_item_id` apuntando a la línea `TRIAL`, y (3) llamar a `ConsumeTrialGrantService`.
Ninguna tabla nueva: todas las columnas ya existen.

### `subscriptions.status`: TRIALING, y el worker no necesita cambio

**Decisión: `TRIALING`, con `subscriptions.trial_end_date = company_trial_windows.end_date`** (la
misma fecha, porque hay una sola ventana por cuenta).

`chk_subscriptions_trial` (`242`) exige `trial_end_date IS NOT NULL` cuando `status = 'TRIALING'`,
así que el valor encaja sin fricción. La objeción obvia —"¿no dijo R-TRIAL-13 que el estado del
contrato no decide el cobro?"— está resuelta y verificada en el propio código: `DunningEvaluationService.java:78-79`
trata `ACTIVE` y `TRIALING` **exactamente igual** a la hora de detectar mora
(`if (current == ACTIVE || current == TRIALING)`), y `BillableSubscriptionItem.devenga` (§del
brief, `subscriptionbilling/domain/BillableSubscriptionItem.java:56-58`) filtra por
**`charge_mode` de la línea**, nunca por `subscriptions.status`. Es decir: con el código que ya
existe, poner el contrato en `TRIALING` **no cambia nada** en cobro ni en mora — solo cambia lo
que ve la consola ("esta cuenta está en su ventana de prueba").

`SubscriptionLifecycleWorker.process` (línea 88-94) ya hace exactamente lo que hace falta sin
ningún cambio: `if (status == TRIALING && trialEndDate.isBefore(today)) → ACTIVE`. El día que la
ventana de la empresa vence, el barrido diario del contrato pasa el estado a `ACTIVE` solo,
**en paralelo** al barrido de líneas de BF-B (que es el que de verdad mueve permisos y cobro). Los
dos barridos leen la misma fecha (`trial_end_date` del contrato = `end_date` de la ventana) por
construcción, así que nunca divergen.

### Riesgos

- **El catálogo mínimo vendible pasa de 4 filas (`CORE` + 2 capacidades hoy, según
  `initialLines()`) a 16.** Es un cambio de comportamiento grande en
  `CreateInitialSubscriptionService.initialLines()` y en `PlatformCatalogTemplateJpaRepository`
  (que hoy filtra por `code = 'CORE'` explícitamente para el módulo, línea 58 de
  `PlatformCatalogTemplateJpaRepository.java`): la consulta tiene que generalizarse a "todos los
  `MODULE` y `CAPACITY` con `trial_eligibility = 'ELIGIBLE'`, `status = 'ACTIVE'`,
  `enabled = TRUE`, con tramo publicado para el ciclo pedido" en vez de "el único `code = 'CORE'`".
  Es trabajo de BF-A, no de esquema, pero se avisa aquí porque cambia el contrato del puerto
  (`findInitialContractTemplate` deja de tener sentido como "una sola fila"; hace falta una lista,
  como ya lo es `findInitialCapacityTemplates`).
- **`requireOperableMinimum`** (`CreateInitialSubscriptionService.java:133-145`) solo exige las
  capacidades **estructurales** (`structural_minimum = TRUE`): `CAPACITY_USER`, `CAPACITY_BRANCH`.
  Nada de esto cambia con la ampliación a 16 líneas — los 13 módulos nuevos no son estructurales y
  su ausencia (por ejemplo, si un artículo se retira del catálogo) no debe bloquear el alta. Hay
  que verificar en BF-A que la nueva consulta de módulos `ELIGIBLE` sea tolerante a lista vacía
  (algo que hoy no puede pasar porque `CORE` siempre está, pero con la generalización sí es
  representable).

---

## 3. Compra a mitad de prueba con cobro diferido

### Decisión

**Se modela como una sucesión física de líneas, exactamente con el mismo mecanismo que ya usa el
vencimiento natural — no como una reescritura del desenlace congelado**, que R-TRIAL-28 en efecto
prohíbe (`policy_trial_days`/`policy_trial_outcome` se congelan el día de la concesión).

Al comprar el artículo el día 10 de una ventana de 30 días:

1. Se cierra la línea `TRIAL` existente: `effective_to = trial_end_date + 1 día` (el mismo instante
   en que la prueba natural habría cerrado, **no** el día de la compra), `ended_amendment_id` = el
   otrosí que registra la compra.
2. Se abre una línea nueva `charge_mode = 'PAID'`, `effective_from = trial_end_date + 1 día`,
   `succeeds_item_id` = el id de la línea `TRIAL`, `origin = 'ADDON'`, `activation_path` según el
   canal de compra (`'SELF_SERVICE'` si la compra la hizo el dueño desde el tenant), con el precio
   y el `tax_rate`/`tax_treatment` **vigentes el día de la compra** (no el del día 31: es lo que
   se acuerda al aceptar, y por eso se congela ya).
3. La concesión de prueba (`company_trial_grants`) **no se toca**: sigue con `consumed_at NULL`
   hasta que el barrido de BF-B la selle el día 31 con `outcome = 'CONVERTED'` (nuevo valor de uso,
   ya presente en el dominio: `chk_company_trial_grants_outcome` admite `'CONVERTED'` entre los
   cuatro desenlaces posibles desde `302`).

**Por qué esto no colisiona con `uq_subscription_items_current` (el marcador de "línea vigente
por artículo y tramo").** La columna generada `current_item_marker` vale `catalog_item_id` solo
cuando `effective_to IS NULL`. Al cerrar la línea `TRIAL` con una fecha (aunque sea futura), su
marcador pasa a `NULL` de inmediato, liberando el hueco para que la línea `PAID` nueva —con
`effective_to IS NULL`— sea la única con marcador activo. **No hace falta esperar al día 31 para
escribir las dos filas**: se escriben las dos el día de la compra, con fechas que hablan del
futuro. El patrón de "vigencia futura" ya está anticipado en el propio esquema:
`subscription_items.billing_effect` admite `'NEXT_CYCLE'` desde `244`
(`chk_subscription_items_billing_effect`), precisamente para cambios que surten efecto en un
momento posterior al de su registro.

**Por qué la entitlement no se adelanta ni se atrasa un solo día — verificado, no supuesto, en
`EntitlementCalculator.grantFor`.** Con la línea `TRIAL` cerrada en `lineEnds = trialCloses`
(exactamente igual, no antes ni después), la comprobación `lineEnds.isAfter(trialCloses)` en la
línea 272 de `EntitlementCalculator.java` da **`false`**, así que el propio `grantFor` de la línea
`TRIAL` **no** escribe la sucesora `FREE_LIMITED` que hubiera escrito en el caso natural — sin
tocar ese método. El acceso pleno sigue viniendo de la línea `TRIAL` hasta el instante exacto en
que cierra, y desde ese instante lo entrega la línea `PAID` nueva (que, al ser `isCurrentOn(day)`
verdadero desde `trial_end_date+1`, entra por el otro brazo de `grantFor` — el de una línea no
`TRIAL` — y concede `FULL` sin fecha de fin). **Cero código nuevo en `EntitlementCalculator` para
este camino**: el comportamiento correcto es consecuencia directa de una igualdad de fechas que
ya estaba contemplada.

### DDL/cambio

**Ninguno.** Todas las columnas necesarias (`effective_from`, `effective_to`, `succeeds_item_id`,
`ended_amendment_id`, `created_amendment_id`, `charge_mode`, `origin`) ya existen desde `244`. La
concesión de prueba tampoco necesita columnas nuevas: `outcome = 'CONVERTED'` ya es un valor legal
de `chk_company_trial_grants_outcome`.

### La aceptación con tarjeta guardada sin cargo inmediato

**Se apoya en `subscription_payment_methods` (`319_create_subscription_payment_methods.xml`,
ya en el árbol) sin ningún cambio de esquema.** `SubscriptionPaymentMethod.register(...)` ya
modela exactamente "el cliente autorizó el cobro con una constancia (`mandateEvidence`), sin que
eso implique un cargo capturado ese mismo día" — es el mismo mecanismo que sostiene el "cobro
anticipado" de Wompi documentado en la memoria del proyecto
(`vetsoftware-wompi-cobro-anticipado.md`): una tokenización/autorización de tarjeta que se guarda
como medio de pago por defecto (`makeDefault()`), y que `ChargeContractFirstPeriodService` /
`DefaultCardPaymentMethodQueryPort.findDefaultActiveCard` ya saben leer para cobrar **después**,
en el momento correcto.

### Qué necesita el job de facturación para emitir la cuenta el día 31 — y el hueco real que esto destapa

`IssueSubscriptionPeriodDocumentService.accrue()` (`subscriptionbilling/application/usecase/
IssueSubscriptionPeriodDocumentService.java:124-149`) ya selecciona, vía
`itemPort.findCurrentOn(companyId, subscriptionId, period.start())`, solo las líneas
`vigenteEn(period.start())` — y la consulta nativa que hay detrás
(`JpaBillableSubscriptionItemPort.SELECT_VIGENTES`) exige `effective_from <= :day`. **La línea
`PAID` con `effective_from = trial_end_date + 1` sencillamente no aparece en ningún devengo
anterior a esa fecha**, y aparece sola el primer día en que un barrido de facturación corra sobre
un `period.start()` que la alcance. **Cero código nuevo para que no se cobre antes de tiempo.**

**Lo que sí es un hueco real, y hay que decirlo con precisión.** `IssueSubscriptionPeriodDocumentService`
solo se dispara en dos caminos: el corte de ciclo del **contrato entero**
(`RunSubscriptionBillingCycleService`, en la fecha `next_billing_date` del contrato) y el cobro del
**primer periodo del contrato** (`ChargeContractFirstPeriodService`, una sola vez, al firmar). Si el
`next_billing_date` del contrato **no coincide** con `trial_end_date + 1` de este artículo en
concreto —que puede pasar: la ventana de prueba son 30 días naturales desde el registro
(`company_trial_windows.window_days`), y el ciclo de facturación mensual del contrato es
`start + 1 mes - 1 día` (`CreateInitialSubscriptionService.java:84-86`), y ambos arrancan el mismo
día pero un mes calendario no siempre son 30 días (28, 29 o 31)—, el primer devengo de esta línea
**no ocurre exactamente el día 31**: ocurre en el **siguiente** corte de ciclo del contrato que
alcance esa fecha, que puede ser días después. No es un cobro indebido (nunca se adelanta), pero
tampoco es "el día 31 exacto" si el negocio lo exige al día. **Esto queda como issue abierto** (ver
cierre): la solución de esquema no hace falta —todas las fechas están bien— pero la orquestación
(¿se dispara un `ChargeContractFirstPeriodService`-like específico por artículo el día
`trial_end_date+1`, en vez de esperar al corte del contrato?) es una decisión de `backend-feature`
que este documento no puede tomar por sí solo.

### La excepción `ELECTRONIC_INVOICING`

No necesita nada de lo anterior. Es `trial_eligibility = 'NEVER_FREE'`: nunca nace `TRIAL`, nace
`PAID` desde el primer día en que se compra (`effective_from` = hoy) y se cobra con el flujo
actual (`ChargeContractFirstPeriodService`, si es la primera línea de pago del contrato, o el
corte de ciclo normal en caso contrario). Es exactamente el camino que ya existe y no cambia.

### Riesgos

- **El riesgo grande es de orquestación (el "cuándo se dispara"), no de datos.** Documentado
  arriba y en el cierre.
- **`subscription_item_limits` de la línea `PAID` nueva.** Si el artículo comprado lleva un eje de
  límite (por ejemplo `GROOMING`), la línea `PAID` nueva necesita su propia fila congelada en
  `subscription_item_limits` (`304`, ya existente, sin cambios de esquema) con `mode = 'FULL'` —
  pagar quita el techo gratuito, por la misma doctrina que "el escalón gratuito es un suelo, no un
  techo que sobrevive al contrato" (§3.4 de este documento). Si el negocio decide otra cosa (un
  techo más alto pero no ilimitado), es una decisión comercial a tomar en BF-C, no un defecto de
  esquema: la tabla admite cualquiera de las dos.

---

## 4. Ejes de límite nuevos

### Decisión

**Dos ejes nuevos en `limit_dimensions`, con la naturaleza que la propia descripción de negocio
exige y no la que su nombre sugiere.** `GROOMING_SERVICE` es `FLOW`/`MONTH` como pide la Decisión
5 del plan. **`SERVICE_ITEM` se corrige de "acumulativo" a `STOCK`**, y es una corrección
necesaria, no una preferencia:

> El plan describe SERVICE_ITEM como *"acumulativo: tarifas activas, libera al desactivar"*. Pero
> **"libera al desactivar" es, letra por letra, la definición de `STOCK`** en este esquema, no la
> de `CUMULATIVE`. La diferencia está en el propio `CHECK` de `limit_dimensions`
> (`chk_limit_dimensions_release_delay`): un eje `CUMULATIVE` **exige** `release_delay_days` — es
> el eje que solo libera **después de un enfriamiento** (30 días para `ANIMAL`/`OWNER`, ver `313`),
> y un eje `STOCK`/`FLOW` lo **prohíbe** — es el eje que libera **al instante**, exactamente el
> comportamiento de `USER`/`BRANCH`/`TERMINAL` hoy. "Una tarifa que se desactiva libera su cupo
> ya" es `STOCK`, no `CUMULATIVE`. Se documenta como choque corregido, con el mismo criterio que
> `suscripciones-modelo.md §5` usa para C1-C8: se resuelve técnicamente y se deja escrito para que
> nadie lo repita.

### DDL/cambio

**Changeset 419 — corrige el `default_trial_days` de los artículos con 14 días, y suma
`CAPACITY_TERMINAL`, que el plan no nombró pero que la propia Decisión 2 del plan
("una sola fecha de vencimiento por cuenta") obliga a mover.**

> `CAPACITY_TERMINAL` tiene hoy `default_trial_days = 14` (`308`), igual que `CASH_REGISTER`, del
> que depende (una terminal de caja sin el módulo de Caja no sirve de nada). Si solo se cambian
> los "4 módulos" que el plan nombra explícitamente y se deja `CAPACITY_TERMINAL` en 14, su
> concesión de prueba (`days_granted = LEAST(14, policy_trial_days)`) vencería el día 14 mientras
> el resto de la cuenta sigue viva hasta el día 30 — **exactamente la fuga que "una sola fecha de
> vencimiento por cuenta" existe para cerrar**, y en el artículo que menos sentido tiene que se
> adelante (una terminal de caja sin caja no hace nada). Se corrige aquí sin esperar a que alguien
> lo descubra en producción.

```sql
UPDATE catalog_items
   SET default_trial_days = 30,
       version = version + 1
 WHERE code IN ('CASH_REGISTER', 'INVENTORY', 'PURCHASES', 'OPEN_ACCOUNTS', 'CAPACITY_TERMINAL')
   AND trial_eligibility = 'ELIGIBLE';
```

`chk_catalog_items_trial_policy` sigue satisfecho (`default_trial_days > 0` y `trial_outcome`
`NOT NULL`, ninguno de los dos toca). `version = version + 1` es la disciplina de la casa para
todo `UPDATE` masivo sobre una tabla versionada (`244`, `308` ya la siguen).

**Changeset 420 — revierte D-06 en las ocho filas sembradas por `313`: `trial_mode = 'FULL'` pasa
a `'LIMITED'` con `trial_limit_quantity = limit_quantity`.**

```sql
UPDATE catalog_item_limits cil
  JOIN catalog_items ci      ON ci.id = cil.catalog_item_id
  JOIN limit_dimensions ld   ON ld.id = cil.limit_dimension_id
   SET cil.trial_mode = 'LIMITED',
       cil.trial_limit_quantity = cil.limit_quantity,
       cil.version = cil.version + 1
 WHERE ci.code IN ('CORE', 'SCHEDULING', 'CASH_REGISTER', 'CAPACITY_USER',
                   'CAPACITY_BRANCH', 'CAPACITY_TERMINAL', 'LAB_IMAGING')
   AND ld.code IN ('ANIMAL', 'OWNER', 'APPOINTMENT', 'INVOICE',
                   'USER', 'BRANCH', 'TERMINAL', 'STORAGE_GB');
```

Ocho filas, ocho ejes — cuadra con "los 8 ejes" del plan. `chk_catalog_item_limits_trial_mode`
(`(trial_mode = 'LIMITED' AND trial_limit_quantity IS NOT NULL) OR (...)`) admite
`trial_limit_quantity = 0` para `CAPACITY_TERMINAL`/`TERMINAL` (columna `INT UNSIGNED`, `0` es un
valor válido, no nulo): el techo en prueba de terminales sigue siendo cero, igual que el techo
gratuito, coherente con que "los techos en prueba son los techos gratuitos" sin excepción.

**Changeset 421 — los dos ejes nuevos y sus techos de fábrica.**

```sql
-- Los dos ejes. GROOMING_SERVICE no lleva un submódulo único: GROOMING abre SPA y DAYCARE a la
-- vez (309_seed_commercial_catalog_relations.xml:69-70), y el eje mide el uso combinado de los
-- dos. sub_module_id es nulable desde 300 y este es exactamente el caso legítimo de dejarlo vacío
-- en vez de elegir uno arbitrario que confundiría a quien lo lea después.
INSERT INTO limit_dimensions
        (code, name, measure_kind, sub_module_id, release_delay_days, available_from)
SELECT seed.code, seed.name, seed.measure_kind, sm.id, NULL, '<FECHA_DE_DESPLIEGUE>'
  FROM (
    SELECT 'GROOMING_SERVICE' AS code,
           'Servicios de spa y guardería del periodo' AS name,
           'FLOW' AS measure_kind, CAST(NULL AS CHAR(30)) AS sub_module_code
    UNION ALL
    SELECT 'SERVICE_ITEM', 'Tarifas de servicio activas', 'STOCK', 'SERVICES'
  ) seed
  LEFT JOIN sub_modules sm ON sm.code = seed.sub_module_code
 WHERE NOT EXISTS (SELECT 1 FROM limit_dimensions x WHERE x.code = seed.code);

-- Los techos de fábrica. GROOMING/SERVICES ya existen como catalog_items (308).
INSERT INTO catalog_item_limits
        (catalog_item_id, limit_dimension_id, measure_kind, mode, limit_quantity,
         reset_period, enforcement, overage_unit_amount, warn_threshold,
         trial_mode, trial_limit_quantity)
SELECT ci.id, ld.id, ld.measure_kind, seed.mode, seed.limit_quantity,
       seed.reset_period, seed.enforcement, NULL, seed.warn_threshold,
       'LIMITED', seed.limit_quantity
  FROM (
    SELECT 'GROOMING' AS item_code, 'GROOMING_SERVICE' AS dimension_code,
           'LIMITED' AS mode, 30 AS limit_quantity, 'MONTH' AS reset_period,
           'BLOCK' AS enforcement, 60 AS warn_threshold
    UNION ALL
    SELECT 'SERVICES', 'SERVICE_ITEM', 'LIMITED', 20, NULL, 'BLOCK', 80
  ) seed
  JOIN catalog_items ci    ON ci.code = seed.item_code
  JOIN limit_dimensions ld ON ld.code = seed.dimension_code
 WHERE NOT EXISTS (SELECT 1 FROM catalog_item_limits x
                    WHERE x.catalog_item_id = ci.id AND x.limit_dimension_id = ld.id);
```

`<FECHA_DE_DESPLIEGUE>` queda deliberadamente sin fijar: D-74 (comentario de `313`) exige que sea
la fecha real en que la política entra en vigor, no la fecha en que se escribe el changeset —
`db-migrations` la rellena con la fecha de publicación real, igual que `310`/`313` lo hicieron con
`2026-09-01`.

**Combinaciones válidas de `catalog_item_limits`, verificadas contra los siete `CHECK` de `303`
para las dos filas nuevas:**

| Regla | GROOMING → GROOMING_SERVICE | SERVICES → SERVICE_ITEM |
|---|---|---|
| `chk_..._mode` | `LIMITED` + `limit_quantity=30` ✔ | `LIMITED` + `limit_quantity=20` ✔ |
| `chk_..._reset_period` | `FLOW` exige `reset_period IN (...)` → `'MONTH'` ✔ | `STOCK` exige `reset_period IS NULL` → `NULL` ✔ |
| `chk_..._overage` | `BLOCK` ≠ `OVERAGE` → `overage_unit_amount IS NULL` ✔ | igual ✔ |
| `chk_..._warn_threshold` | `60` ∈ [1,100] ✔ | `80` ∈ [1,100] ✔ |
| `chk_..._trial_mode` | `LIMITED` + `trial_limit_quantity=30` ✔ | `LIMITED` + `trial_limit_quantity=20` ✔ |

### Por qué

- **`GROOMING_SERVICE` sin `release_delay_days`** porque no es `CUMULATIVE`: un servicio de spa
  prestado este mes no "libera" el mes que viene por enfriamiento, simplemente el contador de
  `MONTH` siguiente empieza en cero — es la semántica normal de `FLOW`.
- **`warn_threshold` 60/80** replica literalmente la convención que `313` deja escrita: "60 en los
  ejes de crecimiento y 80 en los de existencias". `GROOMING_SERVICE` es un eje de flujo/consumo
  mensual (crecimiento dentro del periodo); `SERVICE_ITEM` es un inventario de tarifas activas
  (existencias).

### Riesgos

- **Coste del `ALTER`**: los tres changesets de esta sección son `INSERT`/`UPDATE` puros, sin
  `ALTER TABLE`. Con la base vacía el `UPDATE` no toca ninguna fila hasta que `308`/`313` hayan
  corrido antes en la misma cadena (ya lo hacen, por numeración). Coste nulo en cualquier entorno
  real hoy.
- **`SERVICE_ITEM` como `STOCK` es una corrección de la especificación de negocio, no una
  ambigüedad de esquema**: queda explícito arriba para que si el negocio de verdad quería un
  enfriamiento (por ejemplo, "una tarifa desactivada libera su cupo a los N días para evitar que
  alguien la reactive y desactive para inflar el cupo"), sea una decisión consciente que cambie
  `measure_kind` a `CUMULATIVE` y añada `release_delay_days`, no un descubrimiento tardío.

---

## 5. Qué tabla mide cada techo gratuito en runtime

**Esto es lo más importante del documento para BF-D, y el hallazgo central es que hoy no hay una
sola tabla: hay dos mecanismos distintos ya construidos y sin usar, y una lectura en vivo que no
necesita tabla nueva.** Verificado leyendo el código, no supuesto.

| Eje | `measure_kind` | Tabla contadora | Estado hoy |
|---|---|---|---|
| `ANIMAL` | `CUMULATIVE` | `company_usage_events` (`354`) — `COUNT(*)` filtrado por `limit_dimension_code='ANIMAL'`, `period_key='ALLTIME'` | Tabla completa, con FK `fk_cue_animal` a `animals(company_id,id)` ya en el esquema. **Cero escritores**: ninguna clase fuera de `companyusageevent` llama a `RecordCompanyUsageEventUseCase` |
| `OWNER` | `CUMULATIVE` | `company_usage_events`, `limit_dimension_code='OWNER'` | Igual — cero escritores |
| `APPOINTMENT` | `FLOW` (MONTH) | `company_usage_events`, `limit_dimension_code='APPOINTMENT'`, `period_key` = mes en curso | Igual — cero escritores |
| `GROOMING_SERVICE` | `FLOW` (MONTH) | `company_usage_events`, **extendida** con dos columnas nuevas | No existe todavía: requiere changeset (ver abajo) |
| `SERVICE_ITEM` | `STOCK` | **Ninguna tabla contadora nueva**: `SELECT COUNT(*) FROM services WHERE company_id = ? AND enabled = TRUE` | La tabla `services` (`093_create_services.xml`) ya tiene `company_id` y borrado lógico por `enabled`; es la fuente de verdad y no necesita copiarse a ningún sitio |

### Por qué `company_usage_events` y no `company_capacities` para los ejes contables

**Verificado leyendo la consulta exacta que puebla `company_capacities`.**
`ContractItemJpaRepository.findCapacityLines` (`entitlement/infrastructure/persistence/
ContractItemJpaRepository.java:118-144`) tiene, en su propio `WHERE`, `i.item_type = 'CAPACITY'
AND i.capacity_unit IS NOT NULL`. `ANIMAL`, `OWNER`, `APPOINTMENT`, `GROOMING_SERVICE` y
`SERVICE_ITEM` cuelgan de `catalog_items` con `item_type = 'MODULE'` (`CORE`, `SCHEDULING`,
`GROOMING`, `SERVICES`) — **nunca** pasan ese filtro, así que **nunca** llegan a
`company_capacities` por mucho que `subscription_item_limits`/`catalog_item_limits` tengan una
fila para ellos. Esto confirma, con la fuente exacta, lo que el comentario de `313` ya admitía en
voz alta: *"de los ocho ejes, SOLO TRES consultan el cupo hoy"* — y sigue siendo cierto tras
`354`: `company_usage_events` existe, está completa en esquema, pero **nada la escribe**. Es una
tubería que apunta a la nada, del mismo género que otras cinco que ya se documentaron en la
memoria del proyecto.

`company_capacities` es la tabla correcta para `STOCK` **cuando el artículo es `CAPACITY`**
(`USER`, `BRANCH`, `TERMINAL`, `STORAGE_GB` hoy): un contador mutable, actualizado con `UPDATE ...
SET used_quantity = used_quantity + ?` atómico, sin bitácora de hechos individuales, porque "hay 3
sedes activas" es una pregunta de estado, no una suma de eventos. `company_usage_events` es la
tabla correcta para `CUMULATIVE`/`FLOW`: una bitácora de hechos, uno por fila, porque "cuántas
mascotas se dieron de alta este año" y "cuántas citas se agendaron este mes" son sumas de hechos
que además tienen que poder **defenderse uno a uno** ante una reclamación de excedente — es
literalmente la razón de ser de la tabla, escrita en su propio Javadoc: *"la única que existe para
GANAR una reclamación"*. Mezclar los dos modelos en una sola tabla sería repetir el error que
`UsageBranch`'s Javadoc ya explica que `chk_cue_branch` existe para impedir: contar por eventos lo
que se cuenta por estado inflaría un contador que se dispara solo.

`SERVICE_ITEM` es `STOCK` pero **no** cuelga de un `catalog_items` de tipo `CAPACITY`: es un
atributo de un `MODULE` (`SERVICES`). Igual que `ANIMAL`/`OWNER`/`APPOINTMENT`, tampoco llega a
`company_capacities` por el mismo filtro. La diferencia es que, al ser `STOCK`, **no** necesita
una bitácora de eventos como `company_usage_events` tampoco la admite para ejes de existencias
(`chk_cue_branch` los prohíbe explícitamente) — y no hace falta inventar una tercera tabla: la
tabla de origen (`services`) ya es exactamente el conteo que hace falta, con borrado lógico
(`enabled`) que **libera el cupo al instante** con solo desactivar la fila, que es la semántica
`STOCK` completa sin escribir una sola línea de infraestructura nueva.

### DDL/cambio para `GROOMING_SERVICE`

**Changeset 422 — prerequisito, mismo patrón que `341` (que dejó escrito literalmente que era el
prerrequisito de `354`):** `spas` y `daycares` no tienen hoy una clave única auxiliar
`(company_id, id)`, y sin ella la FK compuesta que hace falta en el siguiente changeset no se
puede crear (el motor exige un índice con esas columnas como prefijo izquierdo en la tabla
referenciada — manual de MySQL 8.4, *Foreign Keys*).

```sql
ALTER TABLE spas
    ADD CONSTRAINT uq_spas_company_id UNIQUE (company_id, id);
ALTER TABLE daycares
    ADD CONSTRAINT uq_daycares_company_id UNIQUE (company_id, id);
```

**Changeset 423 — extiende `company_usage_events` con las dos columnas de rama que faltan.**

```sql
ALTER TABLE company_usage_events
    ADD COLUMN usage_spa_id BIGINT,
    ADD COLUMN usage_daycare_id BIGINT;

ALTER TABLE company_usage_events
    DROP CONSTRAINT chk_cue_branch;

ALTER TABLE company_usage_events
    ADD CONSTRAINT chk_cue_branch
        CHECK (
             (limit_dimension_code = 'OWNER' AND usage_owner_id IS NOT NULL
                  AND usage_animal_id IS NULL AND usage_appointment_id IS NULL
                  AND usage_electronic_document_id IS NULL
                  AND usage_spa_id IS NULL AND usage_daycare_id IS NULL)
          OR (limit_dimension_code = 'ANIMAL' AND usage_animal_id IS NOT NULL
                  AND usage_owner_id IS NULL AND usage_appointment_id IS NULL
                  AND usage_electronic_document_id IS NULL
                  AND usage_spa_id IS NULL AND usage_daycare_id IS NULL)
          OR (limit_dimension_code = 'APPOINTMENT' AND usage_appointment_id IS NOT NULL
                  AND usage_owner_id IS NULL AND usage_animal_id IS NULL
                  AND usage_electronic_document_id IS NULL
                  AND usage_spa_id IS NULL AND usage_daycare_id IS NULL)
          OR (limit_dimension_code = 'INVOICE' AND usage_electronic_document_id IS NOT NULL
                  AND usage_owner_id IS NULL AND usage_animal_id IS NULL
                  AND usage_appointment_id IS NULL
                  AND usage_spa_id IS NULL AND usage_daycare_id IS NULL)
          OR (limit_dimension_code = 'GROOMING_SERVICE'
                  AND ((usage_spa_id IS NOT NULL AND usage_daycare_id IS NULL)
                    OR (usage_spa_id IS NULL AND usage_daycare_id IS NOT NULL))
                  AND usage_owner_id IS NULL AND usage_animal_id IS NULL
                  AND usage_appointment_id IS NULL
                  AND usage_electronic_document_id IS NULL)
        );

CREATE INDEX ix_cue_spa     ON company_usage_events (company_id, usage_spa_id);
CREATE INDEX ix_cue_daycare ON company_usage_events (company_id, usage_daycare_id);

ALTER TABLE company_usage_events
    ADD CONSTRAINT fk_cue_spa FOREIGN KEY (company_id, usage_spa_id)
        REFERENCES spas (company_id, id) ON DELETE RESTRICT ON UPDATE RESTRICT,
    ADD CONSTRAINT fk_cue_daycare FOREIGN KEY (company_id, usage_daycare_id)
        REFERENCES daycares (company_id, id) ON DELETE RESTRICT ON UPDATE RESTRICT;
```

`UsageBranch.GROOMING_SERVICE` en el dominio (`companyusageevent/domain/UsageBranch.java`) tendría
que repartirse a **una de dos** columnas según de dónde viene el hecho (spa o guardería) — es el
único eje del modelo con dos tablas de origen para una sola rama, y el enum de Java necesitaría
ese reparto explícito (dos constructores o un campo adicional), documentado aquí para que
`backend-feature` no lo descubra a medio implementar.

### Riesgos

- **`GROOMING_SERVICE`/`ANIMAL`/`OWNER`/`APPOINTMENT` no tienen ni un solo escritor hoy.** El
  esquema está listo (o lo estará tras `423`); el trabajo real de BF-D es escribir el caso de uso
  que llama a `RecordCompanyUsageEventUseCase` desde `animal`, `owner`, `appointment`, `spa` y
  `daycare` en el momento correcto (¿al crear? ¿al confirmar/completar, para `spa`/`daycare`?), y
  ese "momento correcto" para spa/daycare no está decidido en este documento porque es una
  decisión de negocio sobre cuándo se considera "prestado" un servicio, no de datos.
- **Coste del `ALTER` de `423`**: `ADD COLUMN` nulable es *in place* sin reconstrucción;
  `ADD CONSTRAINT CHECK`/`DROP CONSTRAINT CHECK` no está tabulado explícitamente en el manual de
  DDL online consultado, pero es una operación de validación de motor sin copia completa de datos
  en la práctica; `ADD INDEX` es *in place* sin reconstrucción; `ADD FOREIGN KEY` es *in place*
  **solo** con `foreign_key_checks` deshabilitado, si no cae a `COPY` (Tabla 17.19 del manual). Con
  la tabla vacía en todo entorno real, el algoritmo elegido es irrelevante en coste.
- **`SERVICE_ITEM` no necesita ningún `ALTER`**, pero si mañana se decide que además hace falta
  una bitácora de "qué tarifa se activó/desactivó y cuándo" para auditoría (no para el cupo, que
  ya resuelve el `COUNT` en vivo), esa sería una tabla nueva de bitácora, no una extensión de
  `company_usage_events` (que `chk_cue_branch` cierra a los ejes contables por diseño).

---

## Changesets propuestos

| # | Nombre | Contenido |
|---|---|---|
| 417 | `417_relax_company_trial_windows_paper.xml` | `company_trial_windows`: `source_quote_id` pasa a nulable, se añade `origin VARCHAR(10) NOT NULL` y `chk_company_trial_windows_origin` (arco `SIGNUP`/`QUOTE`) |
| 418 | `418_relax_company_trial_grants_paper.xml` | `company_trial_grants`: se añade `origin VARCHAR(10) NOT NULL`, se sustituye `chk_company_trial_grants_paper` por el arco de tres (`SIGNUP`/`QUOTE`/`AMENDMENT`) |
| 419 | `419_widen_eligible_modules_trial_to_30_days.xml` | `UPDATE catalog_items`: `default_trial_days` 14→30 en `CASH_REGISTER`, `INVENTORY`, `PURCHASES`, `OPEN_ACCOUNTS`, `CAPACITY_TERMINAL` |
| 420 | `420_revert_catalog_item_limits_trial_mode_to_limited.xml` | `UPDATE catalog_item_limits`: las 8 filas de `313` pasan `trial_mode` de `FULL` a `LIMITED` con `trial_limit_quantity = limit_quantity` (revierte D-06) |
| 421 | `421_create_grooming_service_and_service_item_dimensions.xml` | `INSERT limit_dimensions` (`GROOMING_SERVICE` FLOW, `SERVICE_ITEM` STOCK) + `INSERT catalog_item_limits` (GROOMING→30/mes/BLOCK, SERVICES→20/BLOCK, ambos `trial_mode=LIMITED`) |
| 422 | `422_add_tenant_uniques_for_grooming_service_usage.xml` | Prerrequisito: `uq_spas_company_id`, `uq_daycares_company_id` (`(company_id, id)`), mismo patrón que `341` |
| 423 | `423_extend_company_usage_events_grooming_service.xml` | `company_usage_events`: añade `usage_spa_id`, `usage_daycare_id`; reescribe `chk_cue_branch` con la rama `GROOMING_SERVICE`; añade `fk_cue_spa`, `fk_cue_daycare`, `ix_cue_spa`, `ix_cue_daycare` |

---

## MEDIDO / NO MEDIDO

**MEDIDO** (lectura de código y changesets, sin tocar ninguna base viva):
`suscripciones-modelo.md`/`suscripciones-tablas.md`/`suscripciones-cambios-existentes.md`/
`suscripciones-reglas-codigo.md`/`suscripciones-politica-mora.md`; changesets `206`, `210`, `226`,
`229`, `239`, `242`, `244`, `246`, `247`, `300`–`303`, `304`, `308`–`309`, `310`–`311`, `313`–`314`,
`319`, `333`, `341`, `354`, `394`, `410`–`416`, y el inventario completo de
`db/changelog/migrations` (416 changesets); `EntitlementCalculator`, `ModuleGrantLine`,
`CompanyEntitlement`, `CapacityGrantLine`, `JpaSubscriptionQueryPort`, `ContractItemJpaRepository`,
`CreateInitialSubscriptionService`, `PlatformCatalogTemplateJpaRepository`,
`JpaPlatformCatalogPort`, `SubscriptionLifecycleWorker`, `DunningEvaluationService`,
`IssueSubscriptionPeriodDocumentService`, `BillableSubscriptionItem`,
`JpaBillableSubscriptionItemPort`, `ChargeContractFirstPeriodService`, `SubscriptionPaymentMethod`,
`ConsumeTrialGrantService`, `CatalogItemLimit`, `CompanyUsageEvent`, `UsageBranch`,
`CompanyLimitEvent`, `AcceptedQuoteContractLines`; secciones R-TRIAL-09/12/13/14/15/28,
R-ENT-01/02/03 del HTML de diseño.

**NO MEDIDO**: ninguna base de datos viva (ni local ni dev) — regla permanente del proyecto,
verificación solo por código. No se ejecutó `EXPLAIN` ni se contaron filas reales: todas las
afirmaciones de coste de `ALTER` vienen del manual de MySQL 8.4 (Online DDL Operations), no de una
medición contra `mysql-local`, porque las tablas afectadas están vacías en todo entorno real hoy y
un `EXPLAIN` sobre cero filas no aporta nada que el manual no diga ya.

## Fuentes citadas

- Manual de MySQL 8.4, restricciones `CHECK`:
  <https://dev.mysql.com/doc/refman/8.4/en/create-table-check-constraints.html> — sostiene por qué
  `origin` se copia en `company_trial_grants` y `company_trial_windows` en vez de leerse de la
  tabla padre (§1).
- Manual de MySQL 8.4, claves foráneas:
  <https://dev.mysql.com/doc/refman/8.4/en/create-table-foreign-keys.html> — exige el índice
  `(company_id, id)` en `spas`/`daycares` antes de poder declarar `fk_cue_spa`/`fk_cue_daycare`
  (§5).
- Manual de MySQL 8.4, DDL online de InnoDB:
  <https://dev.mysql.com/doc/refman/8.4/en/innodb-online-ddl-operations.html> — clasificación
  `MODIFY COLUMN NULL/NOT NULL`, `ADD COLUMN`, `ADD INDEX`, `ADD FOREIGN KEY` citada en los
  apartados de riesgos de §1 y §5 (consultado por `WebFetch` el 2026-09-07).
- `MainVetSoftware/models/modelo-datos-suscripciones.html`, filas R-TRIAL-09/12/13/14/15/28 y
  R-ENT-01/02/03 — criterio normativo de las reglas de prueba y de la clave única de
  `company_entitlements`.
- `docs/db/suscripciones-modelo.md` §1.8, §1.9, §4 — convención de tipos cerrados, restricción de
  `CHECK` entre tablas, patrón de FK compuesta con `company_id` delante.
- `vetsoftware-wompi-cobro-anticipado.md` (memoria del proyecto) — el mecanismo de tokenización
  sin cargo inmediato que sostiene §3.
- `vetsoftware-tuberias-apuntando-a-la-nada.md` (memoria del proyecto) — el patrón que confirma
  que `company_usage_events` es una sexta tubería sin escritores, no la quinta ya conocida.

## Issues abiertos

1. **Orquestación del primer cobro de una línea `PAID` con `effective_from` futuro (§3).** El
   esquema no lo bloquea ni lo adelanta, pero el corte de ciclo del contrato puede no coincidir
   con `trial_end_date + 1` del artículo comprado, así que el primer devengo puede llegar unos
   días después del vencimiento exacto en vez de exactamente ese día. Decisión de
   `backend-feature` (BF-B/BF-C): o se acepta el desfase (nunca hay cobro de más, solo de menos
   tiempo de espera), o se añade un disparo específico por artículo el día de su propio
   vencimiento, análogo a `ChargeContractFirstPeriodService` pero por línea en vez de por
   contrato.
2. **`activation_path = 'SELF_SERVICE'` para las líneas del alta pública (§2)** es la propuesta de
   este documento, no una confirmación: `backend-feature` debe verificar que ningún consumidor
   actual de esa columna (consola, reportes) distinga hoy entre "autoservicio post-registro" y
   "autoservicio de compra en el tenant" de una forma que este valor compartido rompería.
3. **El reparto de `GROOMING_SERVICE` entre `usage_spa_id` y `usage_daycare_id` en
   `UsageBranch` (§5)** es el único eje del modelo con dos tablas de origen para una sola rama; el
   enum de Java necesita una forma de decidir a cuál de las dos columnas escribir que hoy no tiene
   precedente en el propio enum (los otros cuatro valores son 1:1 con su tabla).
4. **Cuándo se considera "prestado" un servicio de `GROOMING_SERVICE` (§5)** — ¿al crear la cita
   de spa/guardería, o al marcarla como completada? Es una decisión de negocio, no de datos, y
   cambia cuál caso de uso llama a `RecordCompanyUsageEventUseCase`.
