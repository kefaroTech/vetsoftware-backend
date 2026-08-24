# Suscripciones — datos semilla

# ⚠️ PROPUESTA PARA UN PR POSTERIOR — NO SE IMPLEMENTA AHORA

**Decisión del usuario (2026-08-22): las semillas quedan aplazadas.** Este documento **no** es una
especificación ejecutable como los otros tres. Es la propuesta con la que se abrirá el PR de semillas
cuando llegue, y sirve mientras tanto para dos cosas concretas:

1. **Dejar por escrito qué catálogo existe realmente hoy**, verificado contra los changesets, para que
   nadie lo invente.
2. **Separar lo estructural de lo comercial**, para que la decisión de negocio (los precios en COP,
   sobre todo) llegue ya acotada y no se tome por defecto dentro de un changeset.

**Consecuencia inmediata del aplazamiento —y es entregable, no nota al margen—:** con el catálogo
vacío, el alta de una empresa no puede crear su contrato inicial. El mínimo estructural que lo
desbloquea y la recomendación de que el arranque en vacío falle de forma explícita y legible están en
**`suscripciones-modelo.md` §6** y en **`suscripciones-cambios-existentes.md` §4.3**. Léelos antes que
esto.

**Nada de este documento debe convertirse en un changeset sin aprobación comercial escrita.**

---

## 0. Cómo se marca cada cosa

| Marca | Significado |
|---|---|
| **[ESTRUCTURAL]** | El sistema no funciona sin esta fila. No hay decisión comercial: es fontanería. Un cambio aquí es un bug |
| **[COMERCIAL]** | Necesita decisión de negocio. Los valores escritos son **propuesta del autor de este documento**, puestos para que la conversación tenga algo concreto que corregir, no para que se implementen tal cual |
| **[BLOQUEADO]** | No se puede sembrar hoy porque depende de filas que **no existen en el árbol de migraciones**. Requiere trabajo previo |

---

## 1. El punto de partida real — qué hay hoy, verificado

**Esto es lo más importante del documento y es lo que hace falta leer antes de proponer nada.**

### 1.1 Módulos y submódulos que nacen en migraciones versionadas

Censo completo sobre `VetSoftware/src/main/resources/db/changelog/migrations/`:

| Objeto | Código | Nombre | Changeset | Línea |
|---|---|---|---|---|
| `modules` | `GENERAL` | General | `184_seed_branch_permissions.xml` | 23-25 |
| `sub_modules` | `BRANCH` | Sucursales | `184_seed_branch_permissions.xml` | 34-36 |
| `sub_modules` | `INVENTORY` | Inventario | `191_seed_inventory_permissions.xml` | 17-19 |
| `sub_modules` | `CASH` | Caja | `196_seed_cashregister_permissions.xml` | 17-19 |
| `sub_modules` | `PURCHASES` | Compras | `199_seed_purchases_permissions.xml` | 17-19 |

**Un módulo. Cuatro submódulos. Eso es todo.** El documento de diseño lo dice y el árbol lo confirma:
*"Hoy solo hay un módulo y cuatro submódulos sembrados y el resto se creó a mano."*

### 1.2 Lo que esto significa para el catálogo comercial

El caso que guía todo el diseño es **Spa Ana Pet**: Ana lleva **núcleo, agenda, servicios y caja**.
De esos cuatro, el único que existe hoy como `sub_modules` sembrado es **`CASH`**.

**No existen en migraciones** los submódulos de: clientes, mascotas, agenda/citas, historia clínica,
consultas, hospitalización, prescripciones, vacunaciones, laboratorio, spa/estética, servicios,
facturación electrónica, cuentas por cobrar, reportes, ni configuración. Están en los entornos
—creados a mano— pero **una base nueva arranca sin ellos**.

> **Esto convierte la mitad del catálogo propuesto en [BLOQUEADO].** No se puede mapear
> «Historia clínica» a un `sub_modules` que no existe: `catalog_item_sub_modules.sub_module_id` es una
> FK real y el `INSERT` fallaría.
>
> **El paso cero del PR de semillas no es sembrar `catalog_items`. Es sembrar los `sub_modules` que
> faltan**, con sus `base_permissions` correspondientes, siguiendo el patrón exacto de `191`, `196` y
> `199` —submódulo + permisos base + atado al rol base `ADMIN` + *backfill* a empresas existentes—
> pero **sin** el bloque de `membership_sub_modules`, que ya no existirá.
>
> Ese trabajo es grande, toca autorización, y **no es alcance de esta especificación de datos**.
> Cuánto exactamente falta hay que censarlo contra la navegación real de los dos fronts y contra
> `base_permissions`, no contra este documento.

### 1.3 Qué se puede sembrar hoy sin bloqueo

Con los cuatro submódulos existentes, el catálogo mapeable **hoy** es:

| Artículo comercial | Submódulos que abriría | ¿Existe? |
|---|---|---|
| Caja y punto de venta | `CASH` | ✅ |
| Inventario | `INVENTORY` | ✅ |
| Compras y proveedores | `PURCHASES` | ✅ |
| Multi-sede | `BRANCH` | ✅ |
| **Núcleo** (clientes, mascotas) | — | ❌ **[BLOQUEADO]** |
| **Agenda** | — | ❌ **[BLOQUEADO]** |
| **Historia clínica** | — | ❌ **[BLOQUEADO]** |
| **Servicios / spa** | — | ❌ **[BLOQUEADO]** |
| **Facturación electrónica DIAN** | — | ❌ **[BLOQUEADO]** |

**Cuatro de nueve.** Y el que falta con más urgencia es el núcleo, que es precisamente el que el alta
de una empresa necesita para existir.

---

## 2. `catalog_items` — el estante propuesto

**Todas las filas de esta sección son [COMERCIAL] en su nombre, descripción y orden, y
[ESTRUCTURAL] en su `code`, `item_type`, `is_core` y `min/max_quantity`.**

Los `code` son la parte que **no puede cambiar después**: el código va congelado en
`subscription_items.item_code` de cada contrato firmado, y renombrarlo rompe la trazabilidad. Los
nombres comerciales sí pueden cambiar cuando se quiera; para eso existe la separación.

### 2.1 Módulos funcionales (`item_type = 'MODULE'`)

| `code` | `name` [COMERCIAL] | `is_core` | `min` | `max` | `sort_order` | `status` | Bloqueo |
|---|---|---|---|---|---|---|---|
| `CORE` | Núcleo: clientes y mascotas | **`TRUE`** | 1 | 1 | 10 | `ACTIVE` | **[BLOQUEADO]** — falta el `sub_modules` |
| `SCHEDULING` | Agenda de citas | `FALSE` | 1 | 1 | 20 | `ACTIVE` | **[BLOQUEADO]** |
| `CLINICAL_HISTORY` | Historia clínica y consultas | `FALSE` | 1 | 1 | 30 | `ACTIVE` | **[BLOQUEADO]** |
| `SERVICES` | Servicios y estética | `FALSE` | 1 | 1 | 40 | `ACTIVE` | **[BLOQUEADO]** |
| `CASH_REGISTER` | Caja y punto de venta | `FALSE` | 1 | 1 | 50 | `ACTIVE` | ✅ mapeable a `CASH` |
| `INVENTORY` | Inventario y kardex | `FALSE` | 1 | 1 | 60 | `ACTIVE` | ✅ mapeable a `INVENTORY` |
| `PURCHASES` | Compras y proveedores | `FALSE` | 1 | 1 | 70 | `ACTIVE` | ✅ mapeable a `PURCHASES` |
| `ELECTRONIC_INVOICING` | Facturación electrónica DIAN | `FALSE` | 1 | 1 | 80 | `ACTIVE` | **[BLOQUEADO]** |

**`is_core = TRUE` solo en `CORE`, y esto es [ESTRUCTURAL].** El configurador no deja quitarlo y la
baja lo rechaza. Evita que alguien se quede con una cuenta sin clientes ni mascotas. Marcar un
segundo artículo como núcleo cambia el significado de «lo mínimo que se puede comprar» y es una
decisión comercial con consecuencia técnica: se convierte en obligatorio para todos los contratos
futuros.

**`max_quantity = 1` en todos los módulos, y esto es [ESTRUCTURAL].** Un módulo se tiene o no se
tiene; comprarlo dos veces no significa nada, y `chk_catalog_items_quantity_range` no lo impediría
por sí solo. Es lo que evita la línea de contrato absurda «2 × Historia clínica».

### 2.2 Capacidades (`item_type = 'CAPACITY'`)

| `code` | `name` [COMERCIAL] | `capacity_unit` | `min` | `max` | `sort_order` | Bloqueo |
|---|---|---|---|---|---|---|
| `EXTRA_USER` | Usuario adicional | `USER` | 0 | 200 | 110 | ✅ no necesita `sub_modules` |
| `EXTRA_BRANCH` | Sede adicional | `BRANCH` | 0 | 50 | 120 | ✅ mapeable a `BRANCH` |
| `EXTRA_TERMINAL` | Terminal de caja adicional | `TERMINAL` | 0 | 100 | 130 | ✅ mapeable a `CASH` |
| `EXTRA_STORAGE` | Almacenamiento adicional (GB) | `STORAGE_GB` | 0 | 5000 | 140 | ✅ no necesita `sub_modules` |

**«Usuario adicional» no desbloquea ningún submódulo: solo sube un contador.** Es el ejemplo que el
documento de diseño usa para explicar por qué el catálogo comercial es una capa aparte. Su fila en
`catalog_item_sub_modules` **no existe**, y eso es correcto, no un olvido.

**`min_quantity = 0` en las capacidades y esto es [ESTRUCTURAL]:** contratar cero usuarios extra es
legítimo (te quedas con los incluidos). `chk_catalog_items_quantity_range` lo permite
(`min_quantity >= 0`).

Los `max_quantity` de arriba son **[COMERCIAL]** y su único propósito es *"impedir cotizar 9.999 sedes
por un error de tecleo"*. Los valores propuestos son deliberadamente holgados.

### 2.3 Cobros únicos (`item_type = 'ONE_TIME'`)

| `code` | `name` [COMERCIAL] | `min` | `max` | `sort_order` |
|---|---|---|---|---|
| `ONBOARDING` | Implantación y capacitación | 1 | 1 | 210 |
| `DATA_MIGRATION` | Migración de datos desde otro sistema | 1 | 1 | 220 |

### 2.4 Paquetes (`item_type = 'BUNDLE'`) — [COMERCIAL] en su totalidad

Aquí es donde aterrizan los planes actuales. Hoy solo existe uno sembrado, `BASIC`
(`020_seed_membership_basic.xml:9-10`), y se convierte en paquete sugerido, **no en jaula**.

| `code` | `name` | `sort_order` | Composición propuesta |
|---|---|---|---|
| `PACK_SPA` | Pack Spa | 310 | `CORE` + `SCHEDULING` + `SERVICES` + `CASH_REGISTER` |
| `PACK_CLINIC` | Pack Clínica | 320 | `CORE` + `SCHEDULING` + `CLINICAL_HISTORY` + `CASH_REGISTER` |
| `PACK_FULL` | Pack Clínica completa | 330 | `PACK_CLINIC` + `INVENTORY` + `PURCHASES` + `ELECTRONIC_INVOICING` |

> **⚠️ `PACK_FULL` tal como está escrito viola una regla del modelo.** `bundle_components` no admite
> paquetes anidados (`suscripciones-tablas.md`, ficha 4: *"que `component_item_id` no apunte a otro
> `BUNDLE`"* es una regla de código). `PACK_FULL` tiene que enumerar sus componentes uno a uno, no
> referenciar `PACK_CLINIC`. Se deja escrito así **a propósito**, marcado, para que el PR de semillas
> no repita el error.

`PACK_SPA` es literalmente el caso de Ana: trabaja sola, un local, una caja, hace baños y estética,
no vende productos y no atiende consultas médicas. En su menú **no existen** «Inventario» ni «Historia
clínica» — no están bajo un candado, simplemente no están.

---

## 3. `catalog_item_sub_modules` — el mapeo contra lo que REALMENTE existe

**Solo estas cuatro filas son sembrables hoy.** Las demás esperan a que existan sus `sub_modules`.

| `catalog_items.code` | `sub_modules.code` | ¿Existe el submódulo? | Changeset que lo crea |
|---|---|---|---|
| `CASH_REGISTER` | `CASH` | ✅ | `196_seed_cashregister_permissions.xml:17-19` |
| `INVENTORY` | `INVENTORY` | ✅ | `191_seed_inventory_permissions.xml:17-19` |
| `PURCHASES` | `PURCHASES` | ✅ | `199_seed_purchases_permissions.xml:17-19` |
| `EXTRA_BRANCH` | `BRANCH` | ✅ | `184_seed_branch_permissions.xml:34-36` |

**Fila que el documento de diseño hace esperar y que hoy no cabe:** el ejemplo canónico
*«Historia clínica puede abrir consultas, hospitalización y prescripciones de un golpe»* necesita tres
`sub_modules` que no existen. Es el mejor argumento de que sembrar el catálogo entero es *"el paso
cero de todo el proyecto"*, y de que ese paso cero no se ha dado.

**Nota sobre `EXTRA_BRANCH` → `BRANCH`.** Es discutible que una **capacidad** abra un submódulo: lo
que compra el cliente es el derecho a más sedes, no la pantalla. Alternativa más limpia: que
`CORE` abra `BRANCH` con una sede incluida y que `EXTRA_BRANCH` solo suba el contador. **Es una
decisión [COMERCIAL] pendiente**, y la propuesta del autor es la alternativa: capacidades que solo
cuentan, módulos que solo abren.

### Patrón SQL de la siembra (idempotente, como los seeds existentes)

Los seeds del árbol resuelven las FK por código, nunca por id, porque los ids no son estables entre
entornos. Mismo patrón:

```sql
INSERT INTO catalog_item_sub_modules (catalog_item_id, sub_module_id, created_date, enabled)
SELECT ci.id, sm.id, NOW(), 1
  FROM catalog_items ci
  CROSS JOIN sub_modules sm
 WHERE ci.code = 'CASH_REGISTER' AND sm.code = 'CASH'
   AND NOT EXISTS (SELECT 1 FROM catalog_item_sub_modules x
                    WHERE x.catalog_item_id = ci.id AND x.sub_module_id = sm.id);
```

Precedente exacto: `196_seed_cashregister_permissions.xml:71-79`.

---

## 4. `catalog_item_dependencies` — las reglas del configurador

**Estructurales en su existencia, [COMERCIAL] en el texto de `note`.**

| `catalog_item_id` | `related_item_id` | `relation_type` | `note` [COMERCIAL] |
|---|---|---|---|
| `ELECTRONIC_INVOICING` | `CASH_REGISTER` | `REQUIRES` | «Facturar electrónicamente necesita el módulo de Caja» |
| `INVENTORY` | `CASH_REGISTER` | `RECOMMENDS` | «Con Caja, el inventario descuenta solo al vender» |
| `PURCHASES` | `INVENTORY` | `REQUIRES` | «Registrar compras necesita Inventario para recibir la mercancía» |
| `CLINICAL_HISTORY` | `SCHEDULING` | `RECOMMENDS` | «La historia clínica se llena sola desde las citas» |
| `EXTRA_TERMINAL` | `CASH_REGISTER` | `REQUIRES` | «Las terminales de caja necesitan el módulo de Caja» |

**`REQUIRES` no es solo una regla de venta.** Del documento de diseño: *"no se puede vender sin el
otro, **y dar de baja el otro arrastra a este**"*. Es decir, dar de baja `CASH_REGISTER` en una
clínica que tiene `ELECTRONIC_INVOICING` tiene que arrastrar también la facturación electrónica, o el
cliente se queda con un módulo que no puede usar y que sigue pagando.

**Ningún `EXCLUDES` propuesto.** No hay hoy dos artículos que no puedan coexistir. La relación existe
en el modelo porque el día que se lance un «Pack Spa» y un «Pack Clínica» excluyentes hará falta, no
porque haga falta ahora.

**Verificación obligatoria antes de sembrar:** que estas filas no formen ciclos indirectos. Con las
cinco de arriba no los hay, pero la comprobación tiene que estar en el changeset o en su test, no en
la confianza. La consulta de vigilancia está en `suscripciones-reglas-codigo.md` (R16).

---

## 5. `price_lists` y `catalog_prices` — **la parte que necesita decisión comercial**

# 💰 TODO ESTE APARTADO ES [COMERCIAL]. NINGÚN NÚMERO ES UNA RECOMENDACIÓN.

**Con las semillas aplazadas, la lista de precios inicial NO nace.** Este apartado existe para que la
conversación comercial tenga una estructura concreta que rellenar, no un formulario en blanco.

### 5.1 La lista

| Campo | Valor propuesto | Marca |
|---|---|---|
| `code` | `LISTA-2026-01` | [ESTRUCTURAL] — el formato, no el año |
| `name` | Tarifa 2026 | [COMERCIAL] |
| `currency` | `COP` | [ESTRUCTURAL] — solo Colombia |
| `valid_from` | la fecha de publicación real | [COMERCIAL] |
| `valid_to` | `NULL` | [ESTRUCTURAL] — vacío = es la vigente |
| `status` | `PUBLISHED` | [ESTRUCTURAL] — un contrato no se puede firmar contra una lista en `DRAFT` |
| `published_by_system_user_id` | **hay que decidir cuál** | ⚠️ Ver abajo |

⚠️ **`published_by_system_user_id` es un problema real del PR de semillas.** `chk_price_lists_published`
exige que una lista `PUBLISHED` tenga firma. Un changeset de Liquibase no tiene usuario, así que hay
que resolver una de dos formas, **y es una decisión pendiente**:

- (a) Sembrar la lista en `DRAFT` y que un humano la publique desde la consola. **Consecuencia:** el
  sistema no puede dar de alta empresas hasta que alguien entre y pulse el botón. Es honesto y es
  auditable.
- (b) Sembrar un `system_users` de plataforma (`code = 'PLATFORM-SEED'`) y firmar con él.
  **Consecuencia:** la firma de la primera tarifa es un usuario técnico, lo cual es exactamente lo que
  la columna existe para evitar, pero desbloquea el arranque.

**Propuesta del autor: (a).** Publicar la tarifa es una decisión comercial y debe tener una persona
detrás desde la primera vez. Y encaja con la recomendación de
`suscripciones-modelo.md` §6.3 de que el arranque en vacío falle **legible**.

### 5.2 Los precios — plantilla vacía a rellenar por negocio

Un `catalog_prices` por `(artículo, ciclo, tramo)`. **Los importes de abajo son marcadores de
posición para que la tabla no esté vacía; ninguno sale de un análisis de mercado ni de un coste.**

| Artículo | Ciclo | `tier_min` | `tier_max` | `included_qty` | `unit_amount` COP | `setup_amount` | `tax_rate` | `tax_treatment` |
|---|---|---|---|---|---|---|---|---|
| `CORE` | `MONTHLY` | 1 | 1 | 2 usuarios | **_____** | 0 | **_____** | **_____** |
| `CORE` | `ANNUAL` | 1 | 1 | 2 usuarios | **_____** | 0 | **_____** | **_____** |
| `SCHEDULING` | `MONTHLY` | 1 | 1 | 0 | **_____** | 0 | **_____** | **_____** |
| `CLINICAL_HISTORY` | `MONTHLY` | 1 | 1 | 0 | **_____** | 0 | **_____** | **_____** |
| `SERVICES` | `MONTHLY` | 1 | 1 | 0 | **_____** | 0 | **_____** | **_____** |
| `CASH_REGISTER` | `MONTHLY` | 1 | 1 | 1 terminal | **_____** | 0 | **_____** | **_____** |
| `INVENTORY` | `MONTHLY` | 1 | 1 | 0 | **_____** | 0 | **_____** | **_____** |
| `PURCHASES` | `MONTHLY` | 1 | 1 | 0 | **_____** | 0 | **_____** | **_____** |
| `ELECTRONIC_INVOICING` | `MONTHLY` | 1 | 1 | 0 | **_____** | **_____** | **_____** | **_____** |
| `EXTRA_USER` | `MONTHLY` | 1 | 10 | 0 | **_____** | 0 | **_____** | **_____** |
| `EXTRA_USER` | `MONTHLY` | 11 | `NULL` | 0 | **_____** | 0 | **_____** | **_____** |
| `EXTRA_BRANCH` | `MONTHLY` | 1 | `NULL` | 0 | **_____** | 0 | **_____** | **_____** |
| `EXTRA_TERMINAL` | `MONTHLY` | 1 | `NULL` | 0 | **_____** | 0 | **_____** | **_____** |
| `EXTRA_STORAGE` | `MONTHLY` | 1 | `NULL` | 5 GB | **_____** | 0 | **_____** | **_____** |
| `ONBOARDING` | `MONTHLY` | 1 | 1 | 0 | 0 | **_____** | **_____** | **_____** |
| `DATA_MIGRATION` | `MONTHLY` | 1 | 1 | 0 | 0 | **_____** | **_____** | **_____** |

### 5.3 Las cuatro decisiones comerciales que hay que tomar, y por qué cada una es cara de revertir

1. **`tax_treatment` de cada artículo.** ¿El software en la nube va gravado al 19 %, exento o
   excluido? **No es una decisión técnica y tiene consecuencias fiscales reales.** El modelo permite
   que un mismo módulo esté gravado en una tarifa y excluido en otra, precisamente para el día que se
   aclare — pero la tarifa inicial hay que decidirla. Todos los contratos firmados congelan
   `tax_rate` y `tax_treatment` en `subscription_items`, así que un cambio posterior **no toca a
   nadie que ya firmó**: hace falta una `PRICE_LIST_MIGRATION` explícita por contrato.

2. **`included_quantity` de `CORE`.** *"El núcleo incluye 2 usuarios; el tercero es el primero que se
   factura."* Ese número queda **congelado en cada contrato** al firmar
   (`subscription_items.included_quantity`). Elegirlo mal no se arregla subiendo la tarifa: hay que
   migrar contrato por contrato. Es literalmente la corrección que la primera auditoría marcó como
   *"la causa número uno de sobrefacturación en modelos de suscripción"*.

3. **El importe anual.** *"El anual lleva su propio importe, no un descuento calculado."* Hay que
   escribir los 12 importes anuales a mano, no poner «×10» en el código. Es lo que hace que el
   descuento anual sea un dato auditable.

4. **Los tramos de `EXTRA_USER`.** *"los usuarios 3 a 10 a 12.000 y del 11 en adelante a 9.000"*. Los
   tramos propuestos (1-10 y 11-∞) son estructura; los importes son negocio.

### 5.4 `billing_document_sequences` — **[ESTRUCTURAL], y sí va en la migración**

Es la única semilla de esta página que **no** es comercial y que **no** puede esperar: sin fila, el
generador de números de cuenta de cobro falla en el primer documento.

```sql
INSERT INTO billing_document_sequences (prefix, next_value, created_date)
SELECT 'DC', 1, NOW()
 WHERE NOT EXISTS (SELECT 1 FROM billing_document_sequences WHERE prefix = 'DC');

INSERT INTO billing_document_sequences (prefix, next_value, created_date)
SELECT 'NC', 1, NOW()
 WHERE NOT EXISTS (SELECT 1 FROM billing_document_sequences WHERE prefix = 'NC');

INSERT INTO billing_document_sequences (prefix, next_value, created_date)
SELECT 'ND', 1, NOW()
 WHERE NOT EXISTS (SELECT 1 FROM billing_document_sequences WHERE prefix = 'ND');
```

**Recomendación:** estas tres filas van en el **mismo changeset que crea la tabla**, no en el PR de
semillas. Son parte de la definición de la tabla, no del catálogo comercial.

### 5.5 `platform_billing_config` — **[ESTRUCTURAL] la fila, [COMERCIAL] sus valores**

La tabla garantiza por esquema que hay **exactamente una** fila. Si no se siembra, no hay ninguna, y
todo el código que lea la configuración tiene que manejar el caso `Optional.empty()`, que es
exactamente el estado que la tabla existe para evitar.

**Recomendación:** sembrarla en el mismo changeset que crea la tabla, con
`default_price_list_id = NULL` (es nulable), y que el PR de semillas solo la **actualice** para
apuntar a la tarifa.

| Campo | Valor propuesto | Marca |
|---|---|---|
| `singleton` | `1` | [ESTRUCTURAL] |
| `default_price_list_id` | `NULL` al crear, se rellena en el PR de semillas | [ESTRUCTURAL] |
| `default_grace_days` | `5` | [COMERCIAL] |
| `default_trial_days` | `14` | [COMERCIAL] |
| `invoice_day_of_month` | `1` | [COMERCIAL] — recuerda: entre 1 y 28 |
| `default_payment_term_days` | `5` | [COMERCIAL] |
| `external_billing_provider` | **hay que decidir cuál** | [COMERCIAL] — *"documenta dónde vive la otra mitad del circuito"* |

---

## 6. El cuestionario del configurador — seis preguntas

**Estructura [ESTRUCTURAL], textos [COMERCIAL], efectos [ESTRUCTURAL] en su forma y [COMERCIAL] en
qué artículo añaden.**

El configurador **no tiene ninguna pregunta escrita en el programa**. Cambiar el cuestionario,
añadir una pregunta para un nuevo tipo de negocio o lanzar una campaña se hace desde la consola, sin
desplegar nada. Estas seis son la propuesta inicial y están escritas para el caso de Ana.

### P1 · `BUSINESS_TYPE`

| Campo | Valor |
|---|---|
| `answer_type` | `SINGLE` |
| `question_text` | «¿Qué tipo de negocio tienes?» |
| `help_text` | «Nos ayuda a proponerte un punto de partida. Después puedes quitar o añadir lo que quieras.» |
| `required` | `TRUE` |
| `parent_option_id` | `NULL` |
| `sort_order` | 10 |

| Opción `code` | `label` | Efecto propuesto |
|---|---|---|
| `SPA` | «Spa, baños y estética» | `ADD CORE`, `ADD SCHEDULING`, `ADD SERVICES` |
| `CLINIC` | «Clínica veterinaria» | `ADD CORE`, `ADD SCHEDULING`, `ADD CLINICAL_HISTORY` |
| `PETSHOP` | «Tienda de mascotas» | `ADD CORE`, `ADD INVENTORY`, `ADD CASH_REGISTER` |
| `MIXED` | «Un poco de todo» | `ADD CORE`, `ADD SCHEDULING` |

**Las cuatro opciones añaden `CORE`.** Es redundante —`is_core = TRUE` ya lo hace obligatorio— pero
explícito, y el configurador debe poder mostrarlo en el carrito desde la primera respuesta.

### P2 · `MEDICAL_CONSULTATIONS`

| Campo | Valor |
|---|---|
| `answer_type` | `BOOLEAN` |
| `question_text` | «¿Atiendes consultas médicas?» |
| `help_text` | «Si un veterinario ve pacientes y registra diagnósticos, sí.» |
| `required` | `TRUE` |
| `sort_order` | 20 |

| Opción | Efecto |
|---|---|
| `YES` | `ADD CLINICAL_HISTORY` |
| `NO` | `REMOVE CLINICAL_HISTORY` |

**Esta es la pregunta de Ana**, y su respuesta `NO` es lo que hace que en su menú **no exista**
«Historia clínica». No está bajo un candado: simplemente no está.

### P3 · `SELLS_PRODUCTS`

| Campo | Valor |
|---|---|
| `answer_type` | `BOOLEAN` |
| `question_text` | «¿Vendes productos, alimento o accesorios?» |
| `help_text` | «Si tienes mercancía que entra y sale, necesitas inventario.» |
| `required` | `TRUE` |
| `sort_order` | 30 |

| Opción | Efecto |
|---|---|
| `YES` | `ADD INVENTORY`, `ADD PURCHASES` |
| `NO` | `REMOVE INVENTORY`, `REMOVE PURCHASES` |

### P4 · `CHARGES_AT_COUNTER`

| Campo | Valor |
|---|---|
| `answer_type` | `BOOLEAN` |
| `question_text` | «¿Cobras en mostrador?» |
| `help_text` | «Efectivo, tarjeta o transferencia con arqueo de caja al cerrar el día.» |
| `required` | `TRUE` |
| `sort_order` | 40 |

| Opción | Efecto |
|---|---|
| `YES` | `ADD CASH_REGISTER` |
| `NO` | `REMOVE CASH_REGISTER` |

### P5 · `TERMINAL_COUNT` — **la pregunta condicional**

| Campo | Valor |
|---|---|
| `answer_type` | `NUMBER` |
| `question_text` | «¿Cuántas cajas tienes?» |
| `help_text` | «Cada punto donde se cobra a la vez.» |
| `required` | `TRUE` |
| **`parent_option_id`** | **la opción `YES` de P4** |
| `sort_order` | 50 |

Efecto (disparado por `question_id`, no por `option_id`):
`QUANTITY_FROM_ANSWER` sobre `EXTRA_TERMINAL`.

**Es la que hace que el asistente se sienta corto e inteligente en vez de un formulario largo**:
«¿Cuántas cajas?» solo aparece si antes dijo que cobra en mostrador. Y es la única de las seis que
usa `configurator_effects.question_id` en vez de `option_id` — por eso existe
`chk_configurator_effects_trigger`.

⚠️ **Detalle de implementación que el modelo deja abierto:** `QUANTITY_FROM_ANSWER` sobre
`EXTRA_TERMINAL` pondría la cantidad **igual al número de cajas**, pero `CASH_REGISTER` ya incluye
una terminal (§5.2). El configurador tiene que restar lo incluido, y esa resta **no está en los
datos**: sale de `catalog_prices.included_quantity`. Es una regla de código, y hay que escribirla o
Ana con una caja acabará pagando una terminal extra que ya tenía incluida.

### P6 · `USER_COUNT`

| Campo | Valor |
|---|---|
| `answer_type` | `NUMBER` |
| `question_text` | «¿Cuántas personas van a usar el sistema?» |
| `help_text` | «Contando a los veterinarios, auxiliares y a quien esté en recepción.» |
| `required` | `TRUE` |
| `parent_option_id` | `NULL` |
| `sort_order` | 60 |

Efecto: `QUANTITY_FROM_ANSWER` sobre `EXTRA_USER`.

**Mismo aviso que en P5:** hay que restar los `included_quantity` de `CORE`. Ana, que trabaja sola,
responde «1», el núcleo incluye 2, y **el resultado tiene que ser cero usuarios extra**, no uno.

### Lo que este cuestionario NO pregunta, y es deliberado

No pregunta por sedes (`EXTRA_BRANCH`), ni por facturación electrónica, ni por almacenamiento.
Motivo: el asistente tiene que ser corto. Multi-sede y facturación electrónica son ventas
consultivas que no caben en una casilla, y el almacenamiento es una ampliación que se ofrece cuando
el contador se acerca al techo (`company_capacities`), que es exactamente para lo que existe esa
tabla: *"poder avisar antes de bloquear y ofrecer la ampliación en el momento exacto en que el
cliente la necesita"*.

---

## 7. Cierre — lo que hay que decidir antes de abrir el PR de semillas

| # | Decisión | Quién | Bloquea |
|---|---|---|---|
| D1 | **Los `sub_modules` que faltan**: cuáles, con qué código y con qué `base_permissions` | Producto + backend | **Todo.** Sin ellos no hay catálogo mapeable ni `CORE` |
| D2 | `tax_treatment` y `tax_rate` de cada artículo | Contabilidad / fiscal | Los precios y todos los contratos futuros |
| D3 | `included_quantity` de `CORE` (usuarios incluidos) | Comercial | **Irreversible por contrato firmado** |
| D4 | Los importes mensuales y **anuales** en COP | Comercial | La lista de precios entera |
| D5 | Cómo se firma la primera `price_lists` (§5.1: opción a o b) | Producto | El arranque en vacío |
| D6 | `external_billing_provider`: con qué sistema se emiten las facturas de suscripción | Administración | La otra mitad del circuito de cobro |
| D7 | Si `EXTRA_BRANCH` abre el submódulo `BRANCH` o solo cuenta (§3) | Producto | El mapeo de capacidades |
| D8 | Los textos de las seis preguntas y sus `help_text` | Comercial / UX | La conversión del asistente |

**Ninguna de estas ocho la puede tomar un agente.** Están enumeradas para que se tomen juntas y una
sola vez, no de una en una dentro de ocho changesets distintos.
