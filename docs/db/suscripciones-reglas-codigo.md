# Suscripciones — lo que la base de datos no puede garantizar sola

**Estado:** especificación normativa para `backend-feature` (las implementa) y `backend-tests` (las
convierte en pruebas automáticas).

Un esquema bien hecho impide muchos desastres por sí mismo, pero no todos. Estas son las reglas que
el código tiene que cumplir **porque MySQL 8.4 no sabe expresarlas**, y son exactamente los puntos
donde un modelo de facturación se corrompe en silencio.

**Van aquí para que se conviertan en pruebas automáticas, no en buenas intenciones.** Cada una lleva
su **consulta de vigilancia**: la que la detecta si se rompe. Toda consulta de vigilancia de este
documento devuelve **cero filas cuando el sistema está sano**; cualquier fila es un incidente.

---

## 0. Por qué MySQL no puede con estas

Tres límites del motor explican casi toda la lista, y conviene tenerlos claros para no perder tiempo
buscando una constraint que no existe:

| Límite | Consecuencia | Fuente |
|---|---|---|
| **Un `CHECK` no ve otras tablas ni agrega filas** | Todo lo que sea «la suma de X no supera Y» es de código | <https://dev.mysql.com/doc/refman/8.4/en/create-table-check-constraints.html> — *"except columns with the `AUTO_INCREMENT` attribute and **columns in other tables**"* · *"**Subqueries are not permitted**"* |
| **Un `CHECK` no ve el valor anterior de la fila** | Toda inmutabilidad es de código o de disparador | Ídem: el `CHECK` se evalúa sobre la fila resultante |
| **No existen restricciones de exclusión ni índices parciales** | El no solapamiento de rangos no es declarable | MySQL no tiene el `EXCLUDE` de PostgreSQL. Lo más cercano es el patrón de columna generada, que solo cubre igualdad exacta |
| **Un `CHECK` no puede referenciar una columna `AUTO_INCREMENT`** | «No apuntarse a sí mismo» por `id` es de código | Misma página: *"except columns with the `AUTO_INCREMENT` attribute"* |

Y uno que **no** es un límite y conviene decirlo para que nadie lo dé por perdido: **la base sí impide
que un pago de una clínica salde la factura de otra**. Lo hacen las 22 claves foráneas compuestas
(`suscripciones-modelo.md` §4). Esa regla **no está en esta lista** porque no hace falta.

---

## 1. Índice de reglas

| # | Regla | Quién la garantiza | Severidad si falla |
|---|---|---|---|
| **R1** | Un cargo o un pago nunca cambia de importe tras crearse | código | Se puede reescribir el pasado |
| **R2** | Un documento con factura externa registrada no cambia de importe | código | VetSoftware y la DIAN dejan de coincidir |
| **R3** | La suma de lo aplicado desde un origen nunca supera ese origen | código + vigilancia | La cartera cuadra con plata que no entró |
| **R4** | El saldo de una factura es su total menos lo aplicado confirmado | código + vigilancia | Se le cobra a quien ya pagó |
| **R5** | Los totales de una cotización cuadran con la suma de sus líneas | código + vigilancia | El cliente firma un número y se le factura otro |
| **R6** | Los cargos de un documento suman su subtotal (**conciliación cargos ↔ documento**) | código + vigilancia | El desglose no explica el total |
| **R7** | Dos tramos del mismo artículo con fechas futuras no se pisan | código + vigilancia | Se factura dos veces el mismo módulo |
| **R8** | El cargo de anulación es negativo; el documento siempre positivo | esquema + código | Una devolución no cabe o descuadra |
| **R9** | Una lista de precios publicada es inmutable, ella y sus precios | código o disparador | Cambia retroactivamente lo que se ofreció |
| **R10** | Toda empresa nace con un contrato, en la misma transacción | código | Empresa que entra y no puede hacer nada |
| **R11** | Permisos y contadores se recalculan ante cualquier cambio del contrato | código | Se paga por lo que no se usa, o al revés |
| **R12** | Dar de baja un módulo jamás borra datos: solo baja a solo lectura | código | Se destruye información del cliente |
| **R13** | Toda petición que mueva dinero lleva llave de idempotencia | código | Un doble clic cobra dos veces |
| **R14** | El empleado que firma un otrosí es de la misma empresa que el contrato | código + vigilancia | Fuga entre clínicas por FK simple |
| **R15** | El configurador resta lo incluido antes de fijar la cantidad | código | Se cobra una unidad que venía incluida |
| **R16** | Las dependencias entre artículos no forman ciclos indirectos | código + vigilancia | El configurador entra en bucle |
| **R17** | La conciliación con la facturación externa se hace periódicamente | proceso | El único hueco que ninguna FK puede tapar |
| **R18** | No existe, ni debe implementarse, un corte total de acceso | **política** | Riesgo legal real |

---

## 2. Las reglas, una a una

### R1 · Un cargo o un pago nunca cambia de importe tras crearse

**Quién:** código.
**Anular es crear una fila que compensa**, nunca editar.

**Qué se rompe si falla:** se puede reescribir el pasado y ninguna auditoría vale nada.

**Cómo se implementa:**
- `SubscriptionChargeJpaEntity`: los campos `quantity`, `unit_amount`, `subtotal_amount`, `tax_rate`,
  `tax_treatment`, `service_period_*`, `proration_days`, `period_days` se declaran `final` en el
  dominio y **sin mutador**. Precedente en el árbol: `LaboratoryTestFileJpaEntity`, exenta de
  `@Version` precisamente porque *"el dominio tiene los diez campos final y ningún mutador"*.
- Los únicos mutadores permitidos: `status` (`PENDING → INVOICED → VOIDED`) y `billing_document_id`.
- `SubscriptionPaymentJpaEntity`: `amount`, `currency`, `payment_method`, `received_at` sin mutador.
  Solo mutan `status` y `reconciled_at`.

**Vigilancia:** no hay consulta que detecte un `UPDATE` que ya ocurrió — ese es el problema. Se
detecta **antes**, con un test de arquitectura o de dominio que compruebe que los setters no existen.
Es trabajo de `backend-tests`.

---

### R2 · Un documento con factura externa registrada no cambia de importe

**Quién:** código.

Solo cambian `settled_amount`, `balance_amount` (que es generada) y `issue_status`. **Corregirlo exige
una nota crédito emitida fuera y registrada aquí, encadenada al original** con `corrects_document_id`.

**Qué se rompe si falla:** lo que dice VetSoftware deja de coincidir con lo que tiene la DIAN, y no
hay forma de saber cuál de los dos miente.

**Cómo se implementa:** el caso de uso de actualización rechaza cualquier cambio de
`subtotal_amount`, `tax_amount`, `total_amount`, `period_start`, `period_end` o `document_kind`
cuando `issue_status = 'EXTERNAL_REGISTERED'`, con un error de dominio propio
(`DOCUMENT_ALREADY_ISSUED`) mapeado a **409** en `GlobalExceptionHandler`.

**Vigilancia** — detecta documentos registrados cuyo total ya no cuadra con su desglose fiscal, que
es la huella que deja una edición posterior:

```sql
SELECT d.id, d.document_number, d.company_id,
       d.subtotal_amount, d.tax_amount, d.total_amount,
       COALESCE(SUM(t.taxable_base), 0) AS base_desglosada,
       COALESCE(SUM(t.tax_amount),   0) AS iva_desglosado
  FROM subscription_billing_documents d
  LEFT JOIN subscription_billing_document_taxes t
         ON t.billing_document_id = d.id AND t.company_id = d.company_id
 WHERE d.issue_status = 'EXTERNAL_REGISTERED'
 GROUP BY d.id, d.document_number, d.company_id,
          d.subtotal_amount, d.tax_amount, d.total_amount
HAVING ABS(d.subtotal_amount - COALESCE(SUM(t.taxable_base), 0)) > 0.00
    OR ABS(d.tax_amount      - COALESCE(SUM(t.tax_amount),   0)) > 0.00;
```

**Cero filas = sano.**

> **Nota sobre el `> 0.00`.** Con `DECIMAL(19,2)` la comparación es exacta, sin épsilon: no hay error
> de coma flotante que perdonar. Si en algún momento alguien mete un `DOUBLE` en esta capa, esta
> consulta empezará a dar falsos positivos, y eso es una señal, no un fallo de la consulta.

---

### R3 · La suma de lo aplicado desde un origen nunca supera ese origen

**Quién:** código + vigilancia.
**Es la regla que evita que la cartera cuadre con plata que no existe** —o con un crédito que se
gastó dos veces.

**Por qué la base no puede:** hay que **agregar** filas de `billing_document_applications` y
compararlas con una fila de otra tabla. Un `CHECK` no admite subconsultas ni columnas de otras tablas.

**Cómo se implementa:** al aplicar, se toma un bloqueo pesimista sobre la fila de origen
(`SELECT … FROM subscription_payments WHERE id = ? AND company_id = ? FOR UPDATE`) dentro de la misma
transacción, se suman las aplicaciones existentes y se rechaza si el total supera el importe. El
bloqueo es lo que serializa el *read-then-write*; sin él, dos aplicaciones concurrentes leen la misma
suma y las dos pasan. Precedente del patrón en el árbol:
`JpaEmployeeQueryPort.lockForOverlapCheck` en el flujo de citas (`226`).

**Vigilancia — pagos sobreaplicados:**

```sql
SELECT p.id            AS payment_id,
       p.company_id,
       p.amount,
       SUM(a.applied_amount) AS aplicado_neto
  FROM subscription_payments p
  JOIN billing_document_applications a
       ON a.payment_id = p.id AND a.company_id = p.company_id
 WHERE a.source_kind = 'PAYMENT'
 GROUP BY p.id, p.company_id, p.amount
HAVING SUM(a.applied_amount) > p.amount;
```

**Vigilancia — notas crédito sobreaplicadas:**

```sql
SELECT n.id            AS credit_note_id,
       n.company_id,
       n.total_amount,
       SUM(a.applied_amount) AS aplicado_neto
  FROM subscription_billing_documents n
  JOIN billing_document_applications a
       ON a.source_document_id = n.id AND a.company_id = n.company_id
 WHERE n.document_kind = 'CREDIT_NOTE'
   AND a.source_kind   = 'CREDIT_NOTE'
 GROUP BY n.id, n.company_id, n.total_amount
HAVING SUM(a.applied_amount) > n.total_amount;
```

**Nota sobre `SUM(applied_amount)`:** la suma es **neta** porque las contra-aplicaciones son negativas
(`chk_bda_reversal_sign`). Eso es deliberado: revertir una aplicación libera el importe del origen
para volver a aplicarlo, que es justamente lo que tiene que pasar.

---

### R4 · El saldo de una factura es su total menos lo aplicado confirmado

**Quién:** código + vigilancia.

**Lo que la base SÍ garantiza:** `balance_amount = total_amount - settled_amount`, porque es una
**columna calculada** y no hay ningún camino de código que pueda desincronizarla.

**Lo que la base NO garantiza y es esta regla:** que `settled_amount` sea de verdad la suma de las
aplicaciones confirmadas. Esa columna sí la escribe el código, y es la que decide si una cuenta entra
en mora — **un camino capaz de desincronizarla es un camino capaz de suspender a quien ya pagó**.

**Cómo se implementa:** `settled_amount` se recalcula **dentro de la misma transacción** que inserta o
revierte la aplicación, con un `UPDATE … SET settled_amount = (SELECT COALESCE(SUM(...), 0) …)` sobre
el documento destino, tomado con `FOR UPDATE`. Nunca `settled_amount = settled_amount + x` desde
Java, porque eso pierde la reconciliación si un paso falla a medias.

**Vigilancia — la consulta de cartera más importante del sistema:**

```sql
SELECT d.id, d.document_number, d.company_id,
       d.total_amount,
       d.settled_amount               AS saldado_guardado,
       COALESCE(SUM(CASE WHEN a.source_kind = 'PAYMENT'
                          AND p.status = 'CONFIRMED'  THEN a.applied_amount
                         WHEN a.source_kind = 'CREDIT_NOTE' THEN a.applied_amount
                         ELSE 0 END), 0) AS saldado_real
  FROM subscription_billing_documents d
  LEFT JOIN billing_document_applications a
         ON a.target_document_id = d.id AND a.company_id = d.company_id
  LEFT JOIN subscription_payments p
         ON p.id = a.payment_id AND p.company_id = a.company_id
 WHERE d.document_kind = 'INVOICE'
 GROUP BY d.id, d.document_number, d.company_id, d.total_amount, d.settled_amount
HAVING d.settled_amount <> COALESCE(SUM(CASE WHEN a.source_kind = 'PAYMENT'
                                              AND p.status = 'CONFIRMED' THEN a.applied_amount
                                             WHEN a.source_kind = 'CREDIT_NOTE' THEN a.applied_amount
                                             ELSE 0 END), 0);
```

**Solo los pagos `CONFIRMED` cuentan como cobro.** Un pago `PENDING` aplicado no debe reducir el saldo,
y esta consulta lo comprueba. Es exactamente el caso que hace que una clínica que "ya pagó" aparezca
en mora: la pasarela avisó pero no confirmó.

**Recomendación de operación:** esta consulta va en el mismo trabajo programado que R3 y **su
resultado no vacío es una alerta, no un informe**. Va a la telemetría, no a un correo que nadie lee.

---

### R5 · Los totales de una cotización cuadran con la suma de sus líneas

**Quién:** código + vigilancia.
**Qué se rompe:** el cliente firma un número y se le factura otro.

```sql
SELECT q.id, q.quote_number, q.company_id,
       q.subtotal_amount, q.discount_amount, q.tax_amount, q.total_amount,
       COALESCE(SUM(l.unit_amount * l.quantity), 0) AS bruto_lineas,
       COALESCE(SUM(l.discount_amount), 0)          AS descuento_lineas,
       COALESCE(SUM(l.tax_amount), 0)               AS iva_lineas,
       COALESCE(SUM(l.line_total), 0)               AS total_lineas
  FROM quotes q
  LEFT JOIN quote_lines l ON l.quote_id = q.id AND l.enabled = TRUE
 WHERE q.enabled = TRUE
   AND q.status IN ('SENT', 'ACCEPTED')
 GROUP BY q.id, q.quote_number, q.company_id,
          q.subtotal_amount, q.discount_amount, q.tax_amount, q.total_amount
HAVING q.discount_amount <> COALESCE(SUM(l.discount_amount), 0)
    OR q.tax_amount      <> COALESCE(SUM(l.tax_amount), 0)
    OR q.total_amount    <> COALESCE(SUM(l.line_total), 0);
```

**Ojo con `l.enabled = TRUE`.** `quote_lines` lleva borrado lógico, así que una línea desactivada
después de enviar la cotización descuadra el total **sin borrar nada** — y esta consulta lo caza. Es
el motivo por el que una cotización en `SENT` o `ACCEPTED` no debería admitir cambios en sus líneas,
que es a su vez una regla de código derivada de R1.

---

### R6 · Conciliación cargos ↔ documento

**Quién:** código + vigilancia. **Es una de las dos que el encargo señala como prioritarias.**

**La invariante:** el `subtotal_amount` de un documento de cobro es exactamente la suma —**con
signo**— de los `subtotal_amount` de los cargos que agrupa. Y su `tax_amount` es exactamente la suma
del desglose de `subscription_billing_document_taxes`, calculado sobre la base agregada por
tratamiento y tarifa.

**Por qué la base no puede:** hay que agregar dos tablas hijas y comparar con la cabecera.

**Por qué importa:** es donde se cruzan las dos convenciones de signo. Un documento positivo cuyo
subtotal es la suma de cargos que pueden ser negativos parece contradictorio y no lo es: en una
factura todos los cargos suman positivo; en una nota crédito todos suman negativo y el documento
guarda el valor absoluto. **Si esta conciliación no se vigila, el descuadre aparece en la declaración
bimestral, no antes.**

**Vigilancia — nivel 1: el subtotal del documento contra sus cargos**

```sql
SELECT d.id, d.document_number, d.company_id, d.document_kind,
       d.subtotal_amount                    AS subtotal_documento,
       COALESCE(SUM(c.subtotal_amount), 0)  AS suma_cargos,
       COUNT(c.id)                          AS num_cargos
  FROM subscription_billing_documents d
  LEFT JOIN subscription_charges c
         ON c.billing_document_id = d.id
        AND c.company_id          = d.company_id
        AND c.status              = 'INVOICED'
 WHERE d.issue_status <> 'VOIDED'
 GROUP BY d.id, d.document_number, d.company_id, d.document_kind, d.subtotal_amount
HAVING d.subtotal_amount <> ABS(COALESCE(SUM(c.subtotal_amount), 0))
    OR COUNT(c.id) = 0;
```

`ABS(...)` es lo que traduce entre las dos convenciones: el documento siempre positivo, los cargos con
signo. `COUNT(c.id) = 0` caza además el documento **sin ningún cargo detrás**, que es un cobro que
nadie puede explicar.

**Vigilancia — nivel 2: el desglose fiscal contra las bases de los cargos**

```sql
SELECT d.id, d.document_number, d.company_id,
       t.tax_treatment, t.tax_rate,
       t.taxable_base                       AS base_declarada,
       ABS(COALESCE(SUM(c.subtotal_amount), 0)) AS base_real_de_cargos,
       t.tax_amount                         AS iva_declarado,
       ROUND(ABS(COALESCE(SUM(c.subtotal_amount), 0)) * t.tax_rate / 100, 2) AS iva_recalculado
  FROM subscription_billing_documents d
  JOIN subscription_billing_document_taxes t
       ON t.billing_document_id = d.id AND t.company_id = d.company_id
  LEFT JOIN subscription_charges c
         ON c.billing_document_id = d.id
        AND c.company_id          = d.company_id
        AND c.status              = 'INVOICED'
        AND c.tax_treatment       = t.tax_treatment
        AND c.tax_rate            = t.tax_rate
 WHERE d.issue_status <> 'VOIDED'
 GROUP BY d.id, d.document_number, d.company_id,
          t.tax_treatment, t.tax_rate, t.taxable_base, t.tax_amount
HAVING t.taxable_base <> ABS(COALESCE(SUM(c.subtotal_amount), 0))
    OR t.tax_amount   <> ROUND(ABS(COALESCE(SUM(c.subtotal_amount), 0)) * t.tax_rate / 100, 2);
```

**Vigilancia — nivel 3: cargos huérfanos y cargos perdidos**

```sql
-- (a) Cargos marcados INVOICED sin documento: el CHECK lo impide, pero si alguien
--     lo desactiva esta es la red.
SELECT id, company_id, subscription_id, description, subtotal_amount
  FROM subscription_charges
 WHERE status = 'INVOICED' AND billing_document_id IS NULL;

-- (b) Cargos PENDING de periodos ya facturados: el servicio se devengó y NADIE lo cobró.
--     Es el modo de fallo silencioso más caro de esta capa.
SELECT c.id, c.company_id, c.subscription_id, c.description,
       c.service_period_start, c.service_period_end, c.subtotal_amount
  FROM subscription_charges c
 WHERE c.status = 'PENDING'
   AND EXISTS (
       SELECT 1
         FROM subscription_billing_documents d
        WHERE d.subscription_id = c.subscription_id
          AND d.company_id      = c.company_id
          AND d.document_kind   = 'INVOICE'
          AND d.issue_status    <> 'VOIDED'
          AND d.period_end      >= c.service_period_end
   );

-- (c) Cargos en un documento cuyo contrato no es el del cargo. La FK compuesta
--     garantiza la EMPRESA, no el CONTRATO: dos contratos de la misma clínica
--     (uno cancelado y otro vivo) podrían cruzarse. Esta es la red para eso.
SELECT c.id, c.company_id,
       c.subscription_id AS contrato_del_cargo,
       d.subscription_id AS contrato_del_documento
  FROM subscription_charges c
  JOIN subscription_billing_documents d
       ON d.id = c.billing_document_id AND d.company_id = c.company_id
 WHERE c.subscription_id <> d.subscription_id;
```

**La consulta (c) merece una nota, porque es un hueco real que el modelo deja abierto y que conviene
tener escrito:** las FK compuestas arrastran `company_id`, no `subscription_id`. Una clínica con dos
contratos —el original cancelado y el nuevo— podría, por un error de código, meter cargos del contrato
viejo en la factura del nuevo, y **la base lo aceptaría**, porque la empresa coincide.

Arreglarlo estructuralmente exigiría FK de **tres** columnas
`(company_id, subscription_id, billing_document_id)` con su clave auxiliar correspondiente. **No se
propone**, por dos razones: `active_marker` ya garantiza un solo contrato vigente por empresa, así que
el caso requiere que alguien facture contra un contrato cancelado; y una tercera columna en seis
claves foráneas engorda todos los índices para cubrir un escenario que la vigilancia (c) detecta en
una consulta. **Si algún día se decide permitir dos contratos vivos por empresa, esta decisión hay que
revisarla el mismo día.**

---

### R7 · Dos tramos del mismo artículo con fechas de fin futuras no se pisan

**Quién:** código + vigilancia. **La otra que el encargo señala como prioritaria.**

**Lo que la base SÍ garantiza:** que no haya **dos líneas abiertas** —sin `effective_to`— del mismo
artículo en el mismo contrato. Lo hace `current_item_marker` con su índice único. **Ese es el caso
común**, y está cerrado.

**Lo que la base NO garantiza, y el modelo dejó de prometerlo:** dos tramos con **fechas de fin
futuras** que se pisen. Ejemplo concreto: la línea A del 1-ene al 30-jun y la línea B del 1-may al
31-dic, del mismo artículo, en el mismo contrato. Las dos tienen `effective_to`, las dos dan
`current_item_marker = NULL`, y **MySQL las acepta**. En mayo y junio, ese módulo se factura dos veces.

**Esto no es expresable en MySQL:** no existen restricciones de exclusión. La primera versión del
modelo lo daba por garantizado y **no lo estaba** — es una de las dos correcciones de la primera
pasada que la segunda auditoría encontró incompletas.

**Cómo se implementa:** el caso de uso que abre o modifica una línea de contrato hace, **dentro de una
transacción y tras tomar un bloqueo pesimista sobre la fila de `subscriptions`**, una lectura de
solape con el criterio de intervalo semiabierto `[from, to)`:

```sql
SELECT 1
  FROM subscription_items
 WHERE company_id      = :companyId
   AND subscription_id = :subscriptionId
   AND catalog_item_id = :catalogItemId
   AND enabled         = TRUE
   AND id             <> :idQueSeEstaEditando
   AND effective_from  < COALESCE(:nuevoTo, '9999-12-31')
   AND COALESCE(effective_to, '9999-12-31') > :nuevoFrom
 FOR UPDATE;
```

El bloqueo sobre `subscriptions` es lo que serializa el *read-then-write*; sin él, dos transacciones
concurrentes pasan las dos la comprobación. **Es exactamente el patrón que el árbol ya usa para el
solape de citas** (`226_add_unique_active_appointment_slot.xml:16-24`: *"Este índice es la última
línea de defensa del caso exacto; el lock es la primera línea para todo lo demás"*).

**Vigilancia — la consulta que detecta el solape ya ocurrido:**

```sql
SELECT a.company_id,
       a.subscription_id,
       a.catalog_item_id,
       a.item_code,
       a.id            AS linea_a,
       a.effective_from AS desde_a,
       a.effective_to   AS hasta_a,
       b.id            AS linea_b,
       b.effective_from AS desde_b,
       b.effective_to   AS hasta_b
  FROM subscription_items a
  JOIN subscription_items b
       ON  b.company_id      = a.company_id
       AND b.subscription_id = a.subscription_id
       AND b.catalog_item_id = a.catalog_item_id
       AND b.id              > a.id
 WHERE a.enabled = TRUE
   AND b.enabled = TRUE
   AND a.effective_from < COALESCE(b.effective_to, '9999-12-31')
   AND b.effective_from < COALESCE(a.effective_to, '9999-12-31')
 ORDER BY a.company_id, a.subscription_id, a.catalog_item_id;
```

**Cero filas = sano.** `b.id > a.id` evita que cada par salga dos veces y que una fila se compare
consigo misma. `COALESCE(..., '9999-12-31')` traduce «vigente» a una fecha comparable — y es
`'9999-12-31'` porque `effective_to` es `DATE`, no `DATETIME`.

**Coste de esta consulta:** es un auto-`JOIN` sobre `subscription_items`. Usa
`ix_subscription_items_vigencia (company_id, subscription_id, effective_from, effective_to)` para el
lado `b`. **Sobre el volumen proyectado (~1.200 filas al año con 100 clínicas,
`suscripciones-modelo.md` §8) es trivial.** Si algún día la tabla creciera al orden de cientos de
miles, la consulta se acota por empresa y se recorren las clínicas en lotes. **No medido: no se
ejecutó `EXPLAIN` contra ninguna base, porque las tablas todavía no existen.**

**Frecuencia recomendada:** diaria, y también **inmediatamente después de cualquier despliegue que
toque el caso de uso de altas y bajas de línea**. Es el momento en que se rompe.

---

### R8 · La convención de signos

**Quién:** esquema (en parte) + código (el resto).

Lo que el esquema **sí** impone (`suscripciones-tablas.md`, fichas 20, 21 y 22):
`chk_subscription_charges_sign`, `chk_sbd_amounts_positive`, `chk_sbdt_amounts_positive`,
`chk_bda_reversal_sign`.

**Lo que queda para el código:** *en un documento de nota crédito no se pueden mezclar cargos de los
dos signos.* Si se mezclan, `ABS(SUM(...))` de R6 deja de ser el subtotal del documento y la
conciliación miente sin dar ninguna fila.

**Vigilancia:**

```sql
SELECT d.id, d.document_number, d.company_id, d.document_kind,
       SUM(CASE WHEN c.subtotal_amount > 0 THEN 1 ELSE 0 END) AS cargos_positivos,
       SUM(CASE WHEN c.subtotal_amount < 0 THEN 1 ELSE 0 END) AS cargos_negativos
  FROM subscription_billing_documents d
  JOIN subscription_charges c
       ON c.billing_document_id = d.id AND c.company_id = d.company_id
 WHERE d.issue_status <> 'VOIDED'
 GROUP BY d.id, d.document_number, d.company_id, d.document_kind
HAVING SUM(CASE WHEN c.subtotal_amount > 0 THEN 1 ELSE 0 END) > 0
   AND SUM(CASE WHEN c.subtotal_amount < 0 THEN 1 ELSE 0 END) > 0;
```

---

### R9 · Una lista de precios publicada es inmutable, ella y sus precios

**Quién:** código **o disparador**.

**Por qué la base no puede con un `CHECK`:** una constraint no ve el valor anterior de la fila. Puede
comprobar «el estado actual es válido», no «este cambio estaba permitido».

**Dos vías, y hay que elegir una:**

- **(a) Control exclusivo en el servicio.** Todo `UPDATE` sobre `price_lists` y `catalog_prices` pasa
  por un único caso de uso que rechaza si `status <> 'DRAFT'`. Es lo coherente con el resto del árbol
  y con la arquitectura hexagonal. **Su punto débil:** un `UPDATE` por SQL desde una migración o desde
  la consola de la base lo esquiva entero.
- **(b) Disparador `BEFORE UPDATE` en MySQL.** Cierra también el camino de SQL directo. **Su punto
  débil:** el árbol **no tiene ni un solo disparador hoy** (censo: cero `CREATE TRIGGER` en los 227
  changesets), así que sería un mecanismo nuevo, invisible para `ddl-auto: validate`, invisible en el
  código Java, y que sorprende a quien depure un `UPDATE` que "no hace nada".

**Propuesta del autor: (a), más la vigilancia de abajo.** Introducir el primer disparador del
repositorio para esta regla es un precio alto por un camino de ataque que ya está cerrado por otras
vías (nadie escribe en producción por SQL, y `db-migrations` no toca datos de negocio). **Es una
decisión pendiente**, no una conclusión.

**Vigilancia** — no detecta la edición directamente, pero sí su huella más común: una lista publicada
cuyos precios se modificaron después de publicarse. Requiere que `catalog_prices` lleve `version`,
que lo lleva:

```sql
SELECT pl.id, pl.code, pl.status, pl.published_at,
       cp.id AS precio_id, cp.catalog_item_id, cp.version
  FROM price_lists pl
  JOIN catalog_prices cp ON cp.price_list_id = pl.id
 WHERE pl.status IN ('PUBLISHED', 'ARCHIVED')
   AND cp.version > 0;
```

**Cero filas = sano.** Un precio dentro de una lista publicada tiene que haber nacido y quedarse
quieto: `version = 0`. Cualquier `version > 0` es un `UPDATE` que ocurrió, y el `@Version` lo delata
sin que haga falta ninguna bitácora extra. **Es la mejor propiedad accidental de BE-26 en toda esta
capa.**

Limitación honesta: si el precio se editó **antes** de publicar la lista, también dará `version > 0` y
será un falso positivo. Se resuelve poniendo a cero la versión al publicar, o aceptando que la
consulta requiere revisión humana. La primera opción es sucia; la segunda, realista.

---

### R10 · Toda empresa nace con un contrato, en la misma transacción

**Quién:** código.
**Qué se rompe:** una empresa sin contrato no tiene permisos calculados: **entra al sistema y no puede
hacer nada, sin ningún mensaje que lo explique**.

**Cómo se implementa:** `RegisterUserService` / `CreateCompanyService` crean `companies`,
`subscriptions`, `subscription_items` y `company_entitlements` **en una sola transacción**. Si algo
falla, no nace la empresa.

**Regla dura asociada que hay que respetar al escribirlo:** `SIN_IO_EXTERNO_EN_TRANSACCION`. El
correo de bienvenida, la llamada a la pasarela y cualquier otro efecto externo van **fuera** de esa
transacción.

**El estado del día 1 con el catálogo vacío**, y qué hacer, está en `suscripciones-modelo.md` §6.3.

**Vigilancia:**

```sql
SELECT c.id, c.name, c.identifier, c.created_date
  FROM companies c
  LEFT JOIN subscriptions s
         ON s.company_id = c.id
        AND s.enabled    = TRUE
        AND s.status IN ('TRIALING', 'ACTIVE', 'PAST_DUE', 'READ_ONLY')
 WHERE c.enabled = TRUE
   AND s.id IS NULL;
```

**Cero filas = sano.** Y ojo con el criterio de «vigente»: **no** es `status = 'ACTIVE'`. Una empresa
en `PAST_DUE` o `READ_ONLY` tiene contrato vigente; una en `CANCELLED` no.

---

### R11 · Los permisos y los contadores se recalculan ante cualquier cambio del contrato

**Quién:** código.
**Qué se rompe:** el cliente paga por algo que no puede usar, o usa algo que dejó de pagar.

**Cuándo hay que recalcular**, y la lista es exhaustiva a propósito: alta de contrato · alta o baja de
línea (`subscription_items`) · cambio de cantidad · cambio de estado del contrato (incluido el paso a
`PAST_DUE` y a `READ_ONLY`) · fin del periodo de prueba · reactivación tras pago · cancelación
efectiva.

**Cómo se implementa:** un único servicio de recálculo que borra físicamente las
`company_entitlements` de la empresa y las reinserta desde el contrato vigente, dentro de una
transacción. Es la única excepción de borrado físico del modelo, y está justificada en
`suscripciones-tablas.md` ficha 17.

**Aviso operativo heredado (memoria del proyecto y issue #348):** cambiar los permisos en la base
**no invalida la caché Redis `employee-permissions`**. Un recálculo que no invalide la caché tarda
hasta 5 minutos por empleado en verse. El recálculo tiene que disparar la invalidación, y hoy **no
existe forma de invalidar por empresa** — es el issue #348, abierto, y es una dependencia real de esta
regla.

**Vigilancia — el indicador de salud que `recalculated_at` existe para dar:**

```sql
-- (a) Empresas cuyo recálculo se quedó viejo: hay un proceso caído.
SELECT company_id, MIN(recalculated_at) AS recalculo_mas_antiguo, COUNT(*) AS filas
  FROM company_entitlements
 GROUP BY company_id
HAVING MIN(recalculated_at) < NOW() - INTERVAL 1 DAY;

-- (b) Permisos que sobreviven a la línea de contrato que los justificaba.
--     La FK garantiza que la línea EXISTE, no que siga vigente.
SELECT e.company_id, e.sub_module_id, e.access_level, e.source,
       i.id AS linea, i.effective_to
  FROM company_entitlements e
  JOIN subscription_items i
       ON i.id = e.subscription_item_id AND i.company_id = e.company_id
 WHERE e.source      = 'SUBSCRIPTION'
   AND e.access_level = 'FULL'
   AND i.effective_to IS NOT NULL
   AND i.effective_to <= CURRENT_DATE;

-- (c) Contadores por debajo de lo usado sin que nadie lo sepa: no es un error
--     (bajar de plan es legítimo), pero SÍ es la lista de clientes a los que
--     hay que ofrecerles la ampliación antes de que choquen contra el techo.
SELECT company_id, capacity_unit, limit_quantity, used_quantity
  FROM company_capacities
 WHERE used_quantity >= limit_quantity;
```

La consulta (b) es la que caza el agujero exacto que este modelo existe para cerrar:
**hoy, bajar de plan no le quita el acceso a nadie** (issue #347).

---

### R12 · Dar de baja un módulo jamás borra ni desactiva datos

**Quién:** código.
**Qué se rompe:** se destruye información que legalmente es del cliente. **No tiene vuelta atrás.**

**Cómo se implementa:** el caso de uso de baja de línea **solo** escribe `effective_to` en
`subscription_items`, crea el `subscription_amendments` correspondiente y dispara el recálculo, que
baja el `access_level` a `READ_ONLY` —o lo oculta, si `sub_modules.read_only_capable = FALSE`—.
**Nunca** toca `enabled` de ninguna tabla clínica ni comercial.

**Vigilancia:** no hay consulta que detecte un borrado que ya ocurrió sin una bitácora. Se cubre con
un test de integración que dé de baja un módulo y compruebe que el recuento de filas de las tablas
clínicas de esa empresa no cambia. Es trabajo de `backend-tests`.

---

### R13 · Toda petición que mueva dinero lleva llave de idempotencia

**Quién:** código.
**Qué se rompe:** un doble clic o un reintento de la pasarela **cobra dos veces**.

**Lo que el esquema ya da:** `uq_quotes_client_request`, `uq_subscription_amendments_client_request`,
`uq_subscription_payments_client_request`, `uq_subscription_payments_gateway`.

**Lo que falta y es de código:** *"se busca antes de insertar"*. La constraint única convierte el
duplicado en un error, pero un error 500 en la cara del cliente no es una respuesta idempotente. El
patrón correcto: buscar por `client_request_id` **antes** de insertar y, si existe, devolver el
recurso que ya se creó con el mismo código de estado que la primera vez.

**Vigilancia:** la propia constraint. Lo que sí conviene vigilar es la **tasa de colisiones** —cuántas
veces al día se rechaza un duplicado— como métrica: si sube, hay un reintento mal configurado.

---

### R14 · El empleado que firma un otrosí es de la misma empresa que el contrato

**Quién:** código + vigilancia.

**Por qué la base no puede:** `subscription_amendments.requested_by_employee_id` es una FK **simple** a
`employees(id)`. Hacerla compuesta con `company_id` exigiría una clave auxiliar
`(company_id, id)` sobre `employees`, que es una tabla de otra feature con
514 consumidores de `CompanyJpaEntity` alrededor. **Se descarta por alcance**, no porque no sea
posible: es un cambio de esquema en una tabla caliente para cubrir un caso que la autorización ya
debería cerrar (`@authz.currentCompanyId()`).

**Queda escrito como límite del modelo**, y con su vigilancia:

```sql
SELECT a.id, a.amendment_number, a.company_id AS empresa_del_contrato,
       e.id AS empleado, e.company_id AS empresa_del_empleado
  FROM subscription_amendments a
  JOIN employees e ON e.id = a.requested_by_employee_id
 WHERE e.company_id <> a.company_id;
```

**Cero filas = sano.** Regla dura relacionada:
`REFERENCIAS_CROSS_FEATURE_ACOTADAS_POR_EMPRESA`.

---

### R15 · El configurador resta lo incluido antes de fijar la cantidad

**Quién:** código.
**Qué se rompe:** se le cobra al cliente una unidad que ya venía incluida.

El caso exacto: Ana trabaja sola, responde «1 persona», el núcleo incluye 2 usuarios
(`catalog_prices.included_quantity = 2`), y el efecto `QUANTITY_FROM_ANSWER` sobre `EXTRA_USER` tiene
que dar **cero**, no uno. Lo mismo con las cajas: `CASH_REGISTER` incluye una terminal, Ana tiene una,
y `EXTRA_TERMINAL` tiene que quedar en cero.

**La resta no está en los datos.** `configurator_effects` dice «usa el número que escribió el
cliente»; de dónde sale lo incluido es `catalog_prices.included_quantity` de la lista con la que se
cotiza. **Es cálculo, y va en el servicio del configurador.**

**Vigilancia** — cotizaciones que cobran menos unidades de las incluidas, o sea, líneas de capacidad
que no deberían existir:

```sql
SELECT q.id, q.quote_number, l.item_code, l.quantity, cp.included_quantity
  FROM quotes q
  JOIN quote_lines    l  ON l.quote_id = q.id AND l.enabled = TRUE
  JOIN catalog_prices cp ON cp.price_list_id  = q.price_list_id
                        AND cp.catalog_item_id = l.catalog_item_id
                        AND cp.billing_cycle   = q.billing_cycle
 WHERE q.enabled = TRUE
   AND l.item_type = 'CAPACITY'
   AND l.quantity <= cp.included_quantity;
```

Una línea de capacidad con `quantity <= included_quantity` es una línea que no debería haberse
cotizado.

---

### R16 · Las dependencias entre artículos no forman ciclos indirectos

**Quién:** código + vigilancia.
**Qué se rompe:** el configurador entra en bucle y **no se puede cotizar**.

El ciclo directo (A → A) lo cierra `chk_catalog_item_dependencies_not_self`. El indirecto
(A → B → C → A) no es expresable en un `CHECK`.

**Cómo se implementa:** el caso de uso que crea o edita una dependencia recorre el grafo antes de
guardar y rechaza si el nuevo arco cierra un ciclo.

**Vigilancia — con CTE recursiva, que MySQL 8.4 sí soporta:**

```sql
WITH RECURSIVE camino (origen, actual, profundidad, ruta) AS (
    SELECT d.catalog_item_id, d.related_item_id, 1,
           CAST(CONCAT(d.catalog_item_id, '>', d.related_item_id) AS CHAR(1000))
      FROM catalog_item_dependencies d
     WHERE d.relation_type = 'REQUIRES' AND d.enabled = TRUE
    UNION ALL
    SELECT c.origen, d.related_item_id, c.profundidad + 1,
           CONCAT(c.ruta, '>', d.related_item_id)
      FROM camino c
      JOIN catalog_item_dependencies d
           ON d.catalog_item_id = c.actual
          AND d.relation_type   = 'REQUIRES'
          AND d.enabled         = TRUE
     WHERE c.profundidad < 20
       AND c.actual <> c.origen
)
SELECT origen, actual, profundidad, ruta
  FROM camino
 WHERE actual = origen;
```

**Cero filas = sano.** El tope `profundidad < 20` es la red contra el bucle infinito **de la propia
consulta de vigilancia**, que es un detalle que se olvida con facilidad: sin él, si hay un ciclo, la
CTE recursiva agota `cte_max_recursion_depth` y falla con un error en vez de dar el diagnóstico.

Solo se recorren los arcos `REQUIRES`: `RECOMMENDS` no arrastra nada y un "ciclo" de recomendaciones
es inofensivo.

---

### R17 · La conciliación con la facturación externa

**Quién:** proceso periódico. **Es el hueco que ninguna clave foránea puede tapar, porque la otra
mitad vive en otro sistema.**

Y por eso es el más fácil de olvidar de los tres.

**Vigilancia — la lista de trabajo pendiente de cada mes:**

```sql
-- (a) Documentos atascados esperando emisión externa.
--     Cada fila es dinero devengado que nadie facturó.
SELECT id, document_number, company_id, subscription_id,
       period_start, period_end, total_amount,
       DATEDIFF(CURRENT_DATE, DATE(created_date)) AS dias_esperando
  FROM subscription_billing_documents
 WHERE issue_status = 'AWAITING_EXTERNAL'
 ORDER BY created_date;

-- (b) Documentos marcados como emitidos sin CUFE: el CHECK exige número y fecha,
--     no CUFE. Sin CUFE no se puede cuadrar contra la DIAN.
SELECT id, document_number, company_id, external_invoice_number,
       external_issued_at, external_provider
  FROM subscription_billing_documents
 WHERE issue_status = 'EXTERNAL_REGISTERED'
   AND (external_cufe IS NULL OR external_cufe = '');

-- (c) Pagos sin conciliar contra el extracto bancario.
SELECT id, company_id, amount, payment_method, gateway, received_at,
       DATEDIFF(CURRENT_DATE, DATE(received_at)) AS dias_sin_conciliar
  FROM subscription_payments
 WHERE status = 'CONFIRMED'
   AND reconciled_at IS NULL
   AND received_at < NOW() - INTERVAL 15 DAY
 ORDER BY received_at;
```

**Por qué (b) no es un `CHECK`:** `chk_sbd_external_registered` exige número, fecha y proveedor
—que son lo que hace falta para **cobrar**— pero no el CUFE, que es lo que hace falta para **cuadrar
con la DIAN** y que a veces llega en un segundo paso. Exigirlo en el esquema bloquearía el registro
legítimo de una factura recién emitida. Es una decisión, no un olvido, y por eso está aquí.

---

### R18 · No existe, ni debe implementarse, un estado de corte total de acceso

**Quién:** **política**, y por eso va la última y en su propia categoría.

**Qué pasa si alguien la incumple:** el cliente moroso queda sin poder consultar su propia historia
clínica. **Reclamación garantizada y riesgo legal real.**

El estado máximo de restricción es `READ_ONLY`: puede consultar e imprimir, no crear ni modificar.
`company_entitlements.access_level` **no tiene ningún valor que signifique «bloqueado»**, y `NONE`
significa *«ese módulo no existe para él»*, no *«se le cortó»*.

**Vigilancia:** ninguna consulta. Se vigila en revisión de código y de producto. Está escrito aquí
porque una política sin registro escrito la deroga el primer PR que "arregle" la morosidad.

---

## 3. Cómo se opera esta lista

**No sirve de nada si vive solo en un documento.** Tres cosas concretas:

1. **Las que tienen consulta se convierten en un trabajo programado.** Un `@Scheduled` diario que
   ejecute R3, R4, R6, R7, R10, R11 y R16, y que emita una **métrica por regla** con el número de
   filas encontradas. Cero es lo normal; cualquier otra cosa dispara alerta. Va a Grafana Cloud, no a
   un correo.
2. **Las que no tienen consulta se convierten en tests.** R1, R2, R12 y R13 se cubren con pruebas de
   integración que intentan la operación prohibida y comprueban que falla. Trabajo de
   `backend-tests`.
3. **R7 y R6 se ejecutan además tras cada despliegue** que toque sus casos de uso. Son las dos que se
   rompen por un cambio de código, no por deriva de datos.

**Coste de las consultas de vigilancia.** Ninguna se midió: las tablas todavía no existen. Sobre el
volumen proyectado (`suscripciones-modelo.md` §8: ~20.000 filas al año en todo el bloque con 100
clínicas), todas son agregaciones sobre miles de filas y ninguna debería pasar de decenas de
milisegundos. **Antes de programarlas a diario, pásalas por `EXPLAIN FORMAT=JSON` contra un
Testcontainer `mysql:8.4` con datos sintéticos** y comprueba que ninguna hace `type: ALL` sobre
`subscription_charges`, que es la tabla más grande del bloque. No lo hagas contra el compose local:
corre `mysql:8.0.45` (`docker-compose.yml:79`) y el optimizador no es el mismo que el de RDS.

---

## 4. Lo que la base SÍ garantiza, para no rediscutirlo dentro de seis meses

Tan útil como la lista de arriba:

| Invariante | Mecanismo |
|---|---|
| Una empresa tiene **como máximo un contrato vigente** | `uq_subscriptions_active_company` sobre `active_marker` |
| Un artículo no tiene **dos líneas abiertas** en un contrato | `uq_subscription_items_current` sobre `current_item_marker` |
| No se cobra **dos veces el mismo periodo exacto** de un contrato | `uq_sbd_recurring_cycle (recurring_cycle_marker, period_start, period_end)` |
| El **saldo** de un documento es su total menos lo saldado | `balance_amount` es columna calculada `VIRTUAL` |
| Un pago, una factura y una aplicación son **de la misma empresa** | 22 claves foráneas compuestas |
| Una aplicación tiene **un solo origen**, pago o nota crédito, nunca los dos | `chk_bda_source_exclusive` |
| Una aplicación se **revierte una sola vez**, y la reversa es negativa | `uq_bda_reversal` + `chk_bda_reversal_sign` |
| Un documento **facturado** tiene número externo y fecha de emisión | `chk_sbd_external_registered` |
| El **IVA** de un documento está calculado **una sola vez** por tratamiento y tarifa | `uq_sbdt_document_rate`, y la ausencia deliberada de `tax_amount` en `subscription_charges` |
| El **consecutivo interno** no tiene carreras ni huecos | `billing_document_sequences` + `SELECT … FOR UPDATE` en la misma transacción |
| Un cargo de **anulación** es negativo y el documento siempre positivo | `chk_subscription_charges_sign` + `chk_sbd_amounts_positive` |
| Las **cantidades incluidas** están congeladas al firmar | `subscription_items.included_quantity`, sin mutador |

**Si alguien propone "reforzar" alguna de estas con código, está duplicando una garantía del motor.**
Dos verdades sobre lo mismo es cómo se corrompe un modelo — que es el argumento con el que este
diseño elimina `companies.membership_id` en vez de dejarlo opcional.
