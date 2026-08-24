# Suscripciones — DDL objetivo, tabla por tabla

**Estado:** especificación normativa. `db-migrations` la convierte en changesets sin tomar ninguna
decisión de modelado; `backend-feature` mapea las `@Entity` contra este documento **sin volver a leer
el HTML de diseño**.

**Lee primero `suscripciones-modelo.md`.** Aquí no se repiten: las convenciones de `created_date`,
`enabled`, `version`, colación, tipos, nombres, la convención de signos ni el patrón de FK
compuestas. Cada ficha asume todo eso.

**Cualquier ambigüedad de esta especificación se convierte en un fallo de `ddl-auto: validate` que
tumba el arranque de la aplicación entera.** Si algo no está escrito aquí, no se improvisa: se
pregunta.

---

## 0. Cómo leer una ficha

- **Slice** — el paquete `com.vetsoftware.app.<slice>` al que pertenece la tabla. Es el reparto de
  trabajo entre instancias de `backend-feature`: los slices son **disjuntos** y dos agentes en slices
  distintos no se pisan.
- **`version` / `enabled`** — se indican explícitamente en cada ficha. Las listas nominales completas
  están en `suscripciones-modelo.md` §2.
- **Tipos** — el tipo escrito es el que va en el atributo `type=` de Liquibase. Donde pone `BOOLEAN`
  va `type="BOOLEAN"` **nunca** `TINYINT(1)`.
- **Nulabilidad** — `NOT NULL` explícito o `NULL` explícito. No hay implícitos.
- **`[AÑADIDO]`** — columna que **no** está en el documento de diseño. Lleva su justificación al
  lado. Están todas inventariadas en `suscripciones-modelo.md` §5.

**Todas las FK, sin excepción:** `ON DELETE RESTRICT ON UPDATE RESTRICT`, declarado explícitamente.
Motivo en `suscripciones-modelo.md` §1.9 y §1.10.

---

## 1. Orden de creación

Respeta las dependencias de FK. Un cambio de orden rompe el `liquibase update` en una base vacía.

| Fase | Paso | Tabla / operación | Slice |
|---|---|---|---|
| **0** | 0.1 | `sub_modules` `+ is_sellable`, `+ read_only_capable` | `submodule` (existente) |
| **0** | 0.2 | Demolición de `memberships` / `membership_sub_modules` / `companies.membership_id` | ver `suscripciones-cambios-existentes.md` |
| **1** | 1 | `catalog_items` | `catalogitem` |
| **1** | 2 | `catalog_item_sub_modules` | `catalogitem` |
| **1** | 3 | `catalog_item_dependencies` | `catalogitem` |
| **1** | 4 | `bundle_components` | `catalogitem` |
| **1** | 5 | `price_lists` | `pricelist` |
| **1** | 6 | `catalog_prices` | `pricelist` |
| **2** | 7 | `configurator_questions` (**sin** `parent_option_id`) | `configurator` |
| **2** | 8 | `configurator_options` | `configurator` |
| **2** | 9 | `ALTER configurator_questions ADD parent_option_id` + FK — **rompe el ciclo** | `configurator` |
| **2** | 10 | `configurator_effects` | `configurator` |
| **3** | 11 | `quotes` | `quote` |
| **3** | 12 | `quote_lines` | `quote` |
| **3** | 13 | `quote_answers` | `quote` |
| **4** | 14 | `subscriptions` | `subscription` |
| **4** | 15 | `subscription_amendments` | `subscription` |
| **4** | 16 | `subscription_items` | `subscription` |
| **4** | 17 | `subscription_status_history` | `subscription` |
| **5** | 18 | `company_entitlements` | `entitlement` |
| **5** | 19 | `company_capacities` | `entitlement` |
| **6** | 20 | `billing_document_sequences` | `subscriptionbilling` |
| **6** | 21 | `subscription_billing_documents` | `subscriptionbilling` |
| **6** | 22 | `subscription_billing_document_taxes` | `subscriptionbilling` |
| **6** | 23 | `subscription_charges` | `subscriptionbilling` |
| **6** | 24 | `subscription_payments` | `subscriptionpayment` |
| **6** | 25 | `billing_document_applications` | `subscriptionpayment` |
| **6** | 26 | `dunning_events` | `dunning` |
| **7** | 27 | `platform_billing_config` | `platformbillingconfig` |

**El único ciclo del modelo** es `configurator_questions.parent_option_id → configurator_options.id`
contra `configurator_options.question_id → configurator_questions.id`. Se rompe creando
`configurator_questions` sin la columna y añadiéndola en el paso 9. No hay otra forma en MySQL: una
FK exige que la tabla referenciada exista.

### Reparto en slices — bloques disjuntos para `backend-feature`

| Slice (`com.vetsoftware.app.<slice>`) | Tablas | Frontera de tenant | Notas de autorización |
|---|---|---|---|
| `catalogitem` | `catalog_items`, `catalog_item_sub_modules`, `catalog_item_dependencies`, `bundle_components` | **ninguna** — catálogo global de plataforma | Todos sus endpoints son `SYSTEM`. `LISTADOS_SIN_EMPRESA_SOLO_SYSTEM` se satisface por ahí. **Ninguna de sus tablas lleva `company_id`**, así que las cuatro reglas duras de tenant no se disparan sobre este slice |
| `pricelist` | `price_lists`, `catalog_prices` | **ninguna** — tarifa global | Igual: endpoints `SYSTEM` |
| `configurator` | `configurator_questions`, `configurator_options`, `configurator_effects` | **ninguna** | Endpoints `SYSTEM` para editar; el cuestionario se **lee** desde el front público sin autenticar (es el asistente de venta), así que hará falta un endpoint de lectura anónimo explícito |
| `quote` | `quotes`, `quote_lines`, `quote_answers` | **`quotes.company_id`, nulable** | El caso raro del modelo. Ver `suscripciones-modelo.md` §4.7. Listado por empresa **o** `SYSTEM`; nunca sin acotar desde un endpoint de tenant |
| `subscription` | `subscriptions`, `subscription_items`, `subscription_amendments`, `subscription_status_history` | `company_id` **NOT NULL** en las cuatro | Todas las reglas duras de tenant aplican |
| `entitlement` | `company_entitlements`, `company_capacities` | `company_id` **NOT NULL** | Es la tabla que consulta **cada petición**: su lectura por `company_id` tiene que ser un *point lookup* por `uq_company_entitlements` |
| `subscriptionbilling` | `subscription_billing_documents`, `subscription_billing_document_taxes`, `subscription_charges`, `billing_document_sequences` | `company_id` **NOT NULL** salvo `billing_document_sequences`, que es global | `billing_document_sequences` es la excepción: es un contador de plataforma sin tenant, y sus endpoints son `SYSTEM`. Está en este slice porque su única razón de existir es numerar los documentos de aquí |
| `subscriptionpayment` | `subscription_payments`, `billing_document_applications` | `company_id` **NOT NULL** | Cruza a `subscriptionbilling` por FK compuesta. `REFERENCIAS_CROSS_FEATURE_ACOTADAS_POR_EMPRESA` obliga a que la resolución del documento destino vaya acotada por empresa |
| `dunning` | `dunning_events` | `company_id` **NOT NULL** | Cruza a `subscription` y `subscriptionbilling` |
| `platformbillingconfig` | `platform_billing_config` | **ninguna** — singleton global | Endpoints `SYSTEM` |

**Diez slices, 26 tablas, bloques disjuntos.** Las dependencias entre slices son solo por FK
compuesta (`subscriptionpayment` → `subscriptionbilling` → `subscription`), así que el orden de
implementación recomendado es: `catalogitem` y `pricelist` primero (no dependen de nadie),
`configurator` y `platformbillingconfig` en paralelo, luego `quote`, luego `subscription`, luego
`entitlement`, `subscriptionbilling`, `subscriptionpayment` y `dunning`.

---

# FASE 1 · CATÁLOGO Y PRECIOS

## 1 · `catalog_items`

**Slice:** `catalogitem` · **`version`:** SÍ · **`enabled`:** SÍ · **`company_id`:** no

El estante de la tienda. Cada fila es una cosa que se puede comprar. **No es multi-tenant**: es el
catálogo global de la plataforma.

### Columnas

| Columna | Tipo | Nulabilidad | Default | Notas |
|---|---|---|---|---|
| `id` | `BIGINT` | `NOT NULL` | `AUTO_INCREMENT` | PK |
| `code` | `VARCHAR(50)` | `NOT NULL` | — | Código estable e inmutable: `CLINICAL_HISTORY`. Nunca cambia aunque cambie el nombre comercial |
| `name` | `VARCHAR(120)` | `NOT NULL` | — | Cómo lo ve el cliente |
| `short_description` | `VARCHAR(255)` | `NULL` | — | La frase junto a la casilla del configurador |
| `long_description` | `TEXT` | `NULL` | — | Detalle para la página de precios y la cotización impresa |
| `item_type` | `VARCHAR(20)` | `NOT NULL` | — | `MODULE` · `CAPACITY` · `ONE_TIME` · `BUNDLE` |
| `capacity_unit` | `VARCHAR(30)` | `NULL` | — | Solo para `CAPACITY`: `USER` · `BRANCH` · `TERMINAL` · `STORAGE_GB` |
| `is_core` | `BOOLEAN` | `NOT NULL` | `FALSE` | Núcleo obligatorio: el configurador no deja quitarlo y la baja lo rechaza |
| `min_quantity` | `INT` | `NOT NULL` | `1` | Tope inferior de venta |
| `max_quantity` | `INT` | `NULL` | — | Tope superior. `NULL` = sin tope |
| `sort_order` | `INT` | `NOT NULL` | `0` | Orden de presentación, comercial |
| `status` | `VARCHAR(20)` | `NOT NULL` | `'DRAFT'` | `DRAFT` · `ACTIVE` · `DEPRECATED` |
| `created_date` | `DATETIME` | `NOT NULL` | `CURRENT_TIMESTAMP` | |
| `enabled` | `BOOLEAN` | `NOT NULL` | `TRUE` | |
| `version` | `BIGINT` | `NOT NULL` | `0` | |

### Constraints

| Nombre | Tipo | Definición |
|---|---|---|
| `PRIMARY` | PK | `(id)` |
| `uq_catalog_items_code` | UNIQUE | `(code)` |
| `chk_catalog_items_item_type` | CHECK | `item_type IN ('MODULE','CAPACITY','ONE_TIME','BUNDLE')` |
| `chk_catalog_items_capacity_unit` | CHECK | `(item_type = 'CAPACITY' AND capacity_unit IN ('USER','BRANCH','TERMINAL','STORAGE_GB')) OR (item_type <> 'CAPACITY' AND capacity_unit IS NULL)` |
| `chk_catalog_items_status` | CHECK | `status IN ('DRAFT','ACTIVE','DEPRECATED')` |
| `chk_catalog_items_quantity_range` | CHECK | `min_quantity >= 0 AND (max_quantity IS NULL OR max_quantity >= min_quantity)` |
| `chk_catalog_items_sort_order` | CHECK | `sort_order >= 0` |

`chk_catalog_items_capacity_unit` hace dos trabajos en una sola constraint: valida el dominio
cerrado **y** ata la unidad al tipo. Sin él se puede vender un `MODULE` con `capacity_unit = 'USER'`,
que el configurador interpretaría como un contador y sumaría usuarios que nadie compró.

### Índices

| Nombre | Columnas | Qué consulta sirve |
|---|---|---|
| `ix_catalog_items_status_sort` | `(status, sort_order)` | El listado del configurador: `WHERE status = 'ACTIVE' AND enabled = TRUE ORDER BY sort_order`. Evita el `filesort` |

`enabled` **no** va en el índice a propósito: la tabla tendrá del orden de decenas de filas y no hay
un solo `catalog_items` deshabilitado previsto; añadir la columna al índice paga escritura en cada
`UPDATE` para no filtrar nada. Si algún día el catálogo pasa de unos cientos de filas, se reevalúa
con un `EXPLAIN`, no antes.

---

## 2 · `catalog_item_sub_modules`

**Slice:** `catalogitem` · **`version`:** NO (`E2_TABLA_PUENTE`) · **`enabled`:** SÍ ·
**`company_id`:** no

El puente entre vender y funcionar. Muchos a muchos **a propósito**: «Historia clínica» puede abrir
consultas, hospitalización y prescripciones de un golpe.

| Columna | Tipo | Nulabilidad | Default |
|---|---|---|---|
| `id` | `BIGINT` | `NOT NULL` | `AUTO_INCREMENT` |
| `catalog_item_id` | `BIGINT` | `NOT NULL` | — |
| `sub_module_id` | `BIGINT` | `NOT NULL` | — |
| `created_date` | `DATETIME` | `NOT NULL` | `CURRENT_TIMESTAMP` |
| `enabled` | `BOOLEAN` | `NOT NULL` | `TRUE` |

| Nombre | Tipo | Definición |
|---|---|---|
| `PRIMARY` | PK | `(id)` |
| `fk_catalog_item_sub_modules_item` | FK | `(catalog_item_id) → catalog_items(id)` `ON DELETE RESTRICT ON UPDATE RESTRICT` |
| `fk_catalog_item_sub_modules_sub_module` | FK | `(sub_module_id) → sub_modules(id)` `ON DELETE RESTRICT ON UPDATE RESTRICT` |
| `uq_catalog_item_sub_modules` | UNIQUE | `(catalog_item_id, sub_module_id)` |

Sin índice adicional: la FK sobre `sub_module_id` crea el suyo automáticamente
(<https://dev.mysql.com/doc/refman/8.4/en/create-table-foreign-keys.html>: *"Such an index is created
on the referencing table automatically if it does not exist"*), y sirve la consulta inversa
"¿qué artículos abren este submódulo?".

---

## 3 · `catalog_item_dependencies`

**Slice:** `catalogitem` · **`version`:** NO (`E2_TABLA_PUENTE`) · **`enabled`:** SÍ

Las reglas del configurador. Sin esta tabla, un cliente compra «Facturación electrónica» sin «Caja»
y descubre después de pagar que no le sirve.

| Columna | Tipo | Nulabilidad | Default | Notas |
|---|---|---|---|---|
| `id` | `BIGINT` | `NOT NULL` | `AUTO_INCREMENT` | |
| `catalog_item_id` | `BIGINT` | `NOT NULL` | — | El artículo sujeto de la regla |
| `related_item_id` | `BIGINT` | `NOT NULL` | — | El otro artículo |
| `relation_type` | `VARCHAR(20)` | `NOT NULL` | — | `REQUIRES` · `RECOMMENDS` · `EXCLUDES` |
| `note` | `VARCHAR(255)` | `NULL` | — | El mensaje que se le muestra al cliente. Un mensaje, no un error críptico |
| `created_date` | `DATETIME` | `NOT NULL` | `CURRENT_TIMESTAMP` | |
| `enabled` | `BOOLEAN` | `NOT NULL` | `TRUE` | |

> **Choque C2 resuelto aquí.** El documento de diseño escribe estos tres códigos en español
> (`REQUIERE` / `RECOMIENDA` / `EXCLUYE`). Se especifican **en inglés**, porque todos los demás tipos
> cerrados del mismo documento (`MODULE`, `CAPACITY`, `ADD`, `SET_QUANTITY`, `EXTERNAL_REGISTERED`) y
> los 105 del árbol existente están en inglés. Detalle en `suscripciones-modelo.md` §5.

| Nombre | Tipo | Definición |
|---|---|---|
| `PRIMARY` | PK | `(id)` |
| `fk_catalog_item_dependencies_item` | FK | `(catalog_item_id) → catalog_items(id)` RESTRICT/RESTRICT |
| `fk_catalog_item_dependencies_related` | FK | `(related_item_id) → catalog_items(id)` RESTRICT/RESTRICT |
| `uq_catalog_item_dependencies` | UNIQUE | `(catalog_item_id, related_item_id, relation_type)` |
| `chk_catalog_item_dependencies_type` | CHECK | `relation_type IN ('REQUIRES','RECOMMENDS','EXCLUDES')` |
| `chk_catalog_item_dependencies_not_self` | CHECK | `catalog_item_id <> related_item_id` |

`chk_catalog_item_dependencies_not_self` **sí** es declarable —no referencia `id`, referencia dos
columnas normales— y cierra el ciclo trivial de longitud 1. **Los ciclos indirectos (A requiere B,
B requiere C, C requiere A) no son expresables en MySQL** y bajan a `suscripciones-reglas-codigo.md`
con su consulta de vigilancia recursiva.

---

## 4 · `bundle_components`

**Slice:** `catalogitem` · **`version`:** NO (`E2_TABLA_PUENTE`) · **`enabled`:** SÍ

Qué trae un paquete. Aquí es donde aterrizan los planes actuales: `BASIC` se convierte en un
`catalog_items` de tipo `BUNDLE` con sus componentes, **no en una jaula**.

| Columna | Tipo | Nulabilidad | Default | Notas |
|---|---|---|---|---|
| `id` | `BIGINT` | `NOT NULL` | `AUTO_INCREMENT` | |
| `bundle_item_id` | `BIGINT` | `NOT NULL` | — | El paquete. Su `catalog_items.item_type` debe ser `BUNDLE` |
| `component_item_id` | `BIGINT` | `NOT NULL` | — | Una pieza que incluye |
| `quantity` | `INT` | `NOT NULL` | `1` | Permite que un pack traiga «3 usuarios» sin vender tres líneas |
| `created_date` | `DATETIME` | `NOT NULL` | `CURRENT_TIMESTAMP` | |
| `enabled` | `BOOLEAN` | `NOT NULL` | `TRUE` | |

| Nombre | Tipo | Definición |
|---|---|---|
| `PRIMARY` | PK | `(id)` |
| `fk_bundle_components_bundle` | FK | `(bundle_item_id) → catalog_items(id)` RESTRICT/RESTRICT |
| `fk_bundle_components_component` | FK | `(component_item_id) → catalog_items(id)` RESTRICT/RESTRICT |
| `uq_bundle_components` | UNIQUE | `(bundle_item_id, component_item_id)` |
| `chk_bundle_components_quantity` | CHECK | `quantity > 0` |
| `chk_bundle_components_not_self` | CHECK | `bundle_item_id <> component_item_id` |

**No declarable en la base y baja a las reglas de código:** que `bundle_item_id` apunte a un artículo
de tipo `BUNDLE` y que `component_item_id` no apunte a otro `BUNDLE` (paquetes anidados). Un `CHECK`
no puede leer columnas de otra tabla —
<https://dev.mysql.com/doc/refman/8.4/en/create-table-check-constraints.html>: *"except columns with
the `AUTO_INCREMENT` attribute and **columns in other tables**"*.

---

## 5 · `price_lists`

**Slice:** `pricelist` · **`version`:** SÍ · **`enabled`:** SÍ

La tarifa oficial, con fecha. Subir precios no es editar: es **publicar una lista nueva**.

| Columna | Tipo | Nulabilidad | Default | Notas |
|---|---|---|---|---|
| `id` | `BIGINT` | `NOT NULL` | `AUTO_INCREMENT` | |
| `code` | `VARCHAR(50)` | `NOT NULL` | — | `LISTA-2026-01` |
| `name` | `VARCHAR(120)` | `NOT NULL` | — | **[AÑADIDO]** Sin un nombre legible, la consola solo puede mostrar el código. Coste cero, ambigüedad cero |
| `currency` | `CHAR(3)` | `NOT NULL` | `'COP'` | Existe para el día que se venda fuera de Colombia sin rehacer el modelo |
| `valid_from` | `DATE` | `NOT NULL` | — | |
| `valid_to` | `DATE` | `NULL` | — | Vacío = es la vigente |
| `status` | `VARCHAR(20)` | `NOT NULL` | `'DRAFT'` | `DRAFT` editable · `PUBLISHED` congelada · `ARCHIVED` consultable |
| `published_at` | `DATETIME` | `NULL` | — | |
| `published_by_system_user_id` | `BIGINT` | `NULL` | — | FK a `system_users`. Es la firma de la decisión comercial |
| `created_date` | `DATETIME` | `NOT NULL` | `CURRENT_TIMESTAMP` | |
| `enabled` | `BOOLEAN` | `NOT NULL` | `TRUE` | |
| `version` | `BIGINT` | `NOT NULL` | `0` | |

| Nombre | Tipo | Definición |
|---|---|---|
| `PRIMARY` | PK | `(id)` |
| `uq_price_lists_code` | UNIQUE | `(code)` |
| `fk_price_lists_published_by` | FK | `(published_by_system_user_id) → system_users(id)` RESTRICT/RESTRICT |
| `chk_price_lists_status` | CHECK | `status IN ('DRAFT','PUBLISHED','ARCHIVED')` |
| `chk_price_lists_validity` | CHECK | `valid_to IS NULL OR valid_to >= valid_from` |
| `chk_price_lists_currency` | CHECK | `CHAR_LENGTH(currency) = 3 AND currency = UPPER(currency)` |
| `chk_price_lists_published` | CHECK | `(status = 'DRAFT' AND published_at IS NULL AND published_by_system_user_id IS NULL) OR (status <> 'DRAFT' AND published_at IS NOT NULL AND published_by_system_user_id IS NOT NULL)` |

`chk_price_lists_published` es la que convierte «quién publicó y cuándo» en un dato **obligatorio**
en vez de aspiracional: una lista no puede pasar a `PUBLISHED` sin firma. `UPPER()` y
`CHAR_LENGTH()` son deterministas y por tanto legales en un `CHECK`.

**Lo que la base NO puede imponer:** que una lista `PUBLISHED` sea inmutable, ella y sus precios. Un
`CHECK` no ve el valor anterior de la fila. Baja a `suscripciones-reglas-codigo.md` (regla R11).

### Índices

| Nombre | Columnas | Qué consulta sirve |
|---|---|---|
| `ix_price_lists_status_valid` | `(status, valid_from)` | "¿Cuál es la lista vigente?": `WHERE status = 'PUBLISHED' AND valid_from <= CURRENT_DATE ORDER BY valid_from DESC` |

---

## 6 · `catalog_prices`

**Slice:** `pricelist` · **`version`:** SÍ · **`enabled`:** SÍ

El precio de un artículo dentro de una lista. Una misma cosa tiene varios precios a la vez según cómo
se pague y cuánto se lleve.

| Columna | Tipo | Nulabilidad | Default | Notas |
|---|---|---|---|---|
| `id` | `BIGINT` | `NOT NULL` | `AUTO_INCREMENT` | |
| `price_list_id` | `BIGINT` | `NOT NULL` | — | |
| `catalog_item_id` | `BIGINT` | `NOT NULL` | — | |
| `billing_cycle` | `VARCHAR(20)` | `NOT NULL` | — | `MONTHLY` · `ANNUAL`. El anual lleva **su propio importe**, no un descuento calculado: así el descuento anual es un dato auditable y no una fórmula escondida en el código |
| `tier_min` | `INT` | `NOT NULL` | `1` | Precio por tramos: los usuarios 3 a 10 a 12.000 y del 11 en adelante a 9.000 |
| `tier_max` | `INT` | `NULL` | — | `NULL` = «del `tier_min` en adelante» |
| `included_quantity` | `INT` | `NOT NULL` | `0` | Cuántas unidades vienen ya incluidas sin cobrar |
| `unit_amount` | `DECIMAL(19,2)` | `NOT NULL` | — | Precio unitario **sin IVA** |
| `setup_amount` | `DECIMAL(19,2)` | `NOT NULL` | `0.00` | Cobro único de puesta en marcha. Se factura una vez y no se repite en la renovación |
| `tax_rate` | `DECIMAL(5,2)` | `NOT NULL` | `0.00` | Porcentaje, `19.00` para el 19 % |
| `tax_treatment` | `VARCHAR(20)` | `NOT NULL` | — | `TAXED` · `EXEMPT` · `EXCLUDED` |
| `created_date` | `DATETIME` | `NOT NULL` | `CURRENT_TIMESTAMP` | |
| `enabled` | `BOOLEAN` | `NOT NULL` | `TRUE` | |
| `version` | `BIGINT` | `NOT NULL` | `0` | |

> **`EXEMPT` y `EXCLUDED` no se pueden colapsar en «tarifa cero».** Excluido y exento se declaran
> distinto y dan derechos distintos ante la DIAN. Confundirlos es un error que solo aparece en una
> revisión fiscal.

| Nombre | Tipo | Definición |
|---|---|---|
| `PRIMARY` | PK | `(id)` |
| `fk_catalog_prices_price_list` | FK | `(price_list_id) → price_lists(id)` RESTRICT/RESTRICT |
| `fk_catalog_prices_item` | FK | `(catalog_item_id) → catalog_items(id)` RESTRICT/RESTRICT |
| `uq_catalog_prices_tier` | UNIQUE | `(price_list_id, catalog_item_id, billing_cycle, tier_min)` |
| `chk_catalog_prices_cycle` | CHECK | `billing_cycle IN ('MONTHLY','ANNUAL')` |
| `chk_catalog_prices_tax_treatment` | CHECK | `tax_treatment IN ('TAXED','EXEMPT','EXCLUDED')` |
| `chk_catalog_prices_tier` | CHECK | `tier_min >= 1 AND (tier_max IS NULL OR tier_max >= tier_min)` |
| `chk_catalog_prices_included` | CHECK | `included_quantity >= 0` |
| `chk_catalog_prices_amounts` | CHECK | `unit_amount >= 0 AND setup_amount >= 0` |
| `chk_catalog_prices_tax_rate` | CHECK | `tax_rate >= 0 AND tax_rate <= 100` |
| `chk_catalog_prices_tax_coherence` | CHECK | `(tax_treatment = 'TAXED' AND tax_rate > 0) OR (tax_treatment <> 'TAXED' AND tax_rate = 0)` |

`uq_catalog_prices_tier` es también el índice de búsqueda de precio: `WHERE price_list_id = ? AND
catalog_item_id = ? AND billing_cycle = ? AND tier_min <= ?` usa las tres igualdades más el rango,
en ese orden — igualdad primero, rango después. **No hace falta ningún índice adicional.**
Criterio: <https://dev.mysql.com/doc/refman/8.4/en/multiple-column-indexes.html>.

`chk_catalog_prices_tax_coherence` impide el error que la auditoría señaló como caro: un
`tax_treatment = 'EXCLUDED'` con `tax_rate = 19.00` produce un IVA sobre una base que no debía
llevarlo, y sale en la declaración bimestral.

**No declarable, baja a las reglas de código:** que los tramos de un mismo
`(price_list_id, catalog_item_id, billing_cycle)` no se pisen ni dejen huecos. Es el mismo tipo de
problema que el solape de vigencias (R7): MySQL no tiene restricciones de exclusión.

---

# FASE 2 · CONFIGURADOR

## 7 · `configurator_questions`

**Slice:** `configurator` · **`version`:** SÍ · **`enabled`:** SÍ

Las preguntas del asistente. **Ninguna pregunta está escrita en el programa**: cambiar el
cuestionario es cambiar filas.

| Columna | Tipo | Nulabilidad | Default | Notas |
|---|---|---|---|---|
| `id` | `BIGINT` | `NOT NULL` | `AUTO_INCREMENT` | |
| `code` | `VARCHAR(50)` | `NOT NULL` | — | Referencia estable: `SELLS_PRODUCTS` |
| `question_text` | `VARCHAR(255)` | `NOT NULL` | — | La pregunta tal cual la lee el cliente |
| `help_text` | `VARCHAR(500)` | `NULL` | — | La aclaración pequeña debajo. Reduce el abandono más que ninguna otra cosa |
| `answer_type` | `VARCHAR(20)` | `NOT NULL` | — | `SINGLE` · `MULTI` · `NUMBER` · `BOOLEAN` |
| `parent_option_id` | `BIGINT` | `NULL` | — | **Se añade en el paso 9**, no en el `CREATE TABLE`. Preguntas condicionales |
| `required` | `BOOLEAN` | `NOT NULL` | `TRUE` | |
| `sort_order` | `INT` | `NOT NULL` | `0` | **[AÑADIDO]** Sin él, el orden del cuestionario depende del orden de inserción, que no es un contrato. El documento sí lo declara para las opciones, y omitirlo en las preguntas es un descuido del documento |
| `created_date` | `DATETIME` | `NOT NULL` | `CURRENT_TIMESTAMP` | |
| `enabled` | `BOOLEAN` | `NOT NULL` | `TRUE` | |
| `version` | `BIGINT` | `NOT NULL` | `0` | |

| Nombre | Tipo | Definición |
|---|---|---|
| `PRIMARY` | PK | `(id)` |
| `uq_configurator_questions_code` | UNIQUE | `(code)` |
| `fk_configurator_questions_parent_option` | FK | `(parent_option_id) → configurator_options(id)` RESTRICT/RESTRICT · **paso 9** |
| `chk_configurator_questions_answer_type` | CHECK | `answer_type IN ('SINGLE','MULTI','NUMBER','BOOLEAN')` |
| `chk_configurator_questions_sort_order` | CHECK | `sort_order >= 0` |

### Índices

| Nombre | Columnas | Qué sirve |
|---|---|---|
| *(automático de la FK)* | `(parent_option_id)` | "¿Qué preguntas cuelgan de esta opción?" |
| `ix_configurator_questions_order` | `(sort_order)` | El render del cuestionario en orden, sin `filesort` |

### El paso 9, literal

```sql
ALTER TABLE configurator_questions
    ADD COLUMN parent_option_id BIGINT NULL,
    ADD CONSTRAINT fk_configurator_questions_parent_option
        FOREIGN KEY (parent_option_id) REFERENCES configurator_options (id)
        ON DELETE RESTRICT ON UPDATE RESTRICT;
```

`<rollback>`:

```sql
ALTER TABLE configurator_questions
    DROP FOREIGN KEY fk_configurator_questions_parent_option,
    DROP COLUMN parent_option_id;
```

Coste en una tabla con datos: `ADD COLUMN` nulable es *instant* y `ADD FOREIGN KEY` es *in place*
(<https://dev.mysql.com/doc/refman/8.4/en/innodb-online-ddl-operations.html>). Sin bloqueo.

---

## 8 · `configurator_options`

**Slice:** `configurator` · **`version`:** SÍ · **`enabled`:** SÍ

| Columna | Tipo | Nulabilidad | Default | Notas |
|---|---|---|---|---|
| `id` | `BIGINT` | `NOT NULL` | `AUTO_INCREMENT` | |
| `question_id` | `BIGINT` | `NOT NULL` | — | |
| `code` | `VARCHAR(50)` | `NOT NULL` | — | Referencia interna |
| `label` | `VARCHAR(255)` | `NOT NULL` | — | El texto que se lee: «Sí, tengo punto de venta» |
| `help_text` | `VARCHAR(500)` | `NULL` | — | |
| `sort_order` | `INT` | `NOT NULL` | `0` | |
| `created_date` | `DATETIME` | `NOT NULL` | `CURRENT_TIMESTAMP` | |
| `enabled` | `BOOLEAN` | `NOT NULL` | `TRUE` | |
| `version` | `BIGINT` | `NOT NULL` | `0` | |

| Nombre | Tipo | Definición |
|---|---|---|
| `PRIMARY` | PK | `(id)` |
| `fk_configurator_options_question` | FK | `(question_id) → configurator_questions(id)` RESTRICT/RESTRICT |
| `uq_configurator_options_code` | UNIQUE | `(question_id, code)` |
| `chk_configurator_options_sort_order` | CHECK | `sort_order >= 0` |

`uq_configurator_options_code` es **por pregunta**, no global: dos preguntas distintas pueden tener
una opción `YES`. Y sirve además como índice de listado `WHERE question_id = ?`.

---

## 9 · `configurator_effects`

**Slice:** `configurator` · **`version`:** SÍ · **`enabled`:** SÍ

El corazón del configurador: traduce respuestas en artículos.

| Columna | Tipo | Nulabilidad | Default | Notas |
|---|---|---|---|---|
| `id` | `BIGINT` | `NOT NULL` | `AUTO_INCREMENT` | |
| `option_id` | `BIGINT` | `NULL` | — | La respuesta que dispara el efecto (preguntas de opción) |
| `question_id` | `BIGINT` | `NULL` | — | La pregunta que lo dispara (preguntas numéricas, donde el número **es** la respuesta) |
| `catalog_item_id` | `BIGINT` | `NOT NULL` | — | El artículo afectado |
| `effect` | `VARCHAR(25)` | `NOT NULL` | — | `ADD` · `REMOVE` · `SET_QUANTITY` · `QUANTITY_FROM_ANSWER` |
| `quantity` | `INT` | `NULL` | — | La cantidad fija, cuando aplica |
| `created_date` | `DATETIME` | `NOT NULL` | `CURRENT_TIMESTAMP` | |
| `enabled` | `BOOLEAN` | `NOT NULL` | `TRUE` | |
| `version` | `BIGINT` | `NOT NULL` | `0` | |

| Nombre | Tipo | Definición |
|---|---|---|
| `PRIMARY` | PK | `(id)` |
| `fk_configurator_effects_option` | FK | `(option_id) → configurator_options(id)` RESTRICT/RESTRICT |
| `fk_configurator_effects_question` | FK | `(question_id) → configurator_questions(id)` RESTRICT/RESTRICT |
| `fk_configurator_effects_item` | FK | `(catalog_item_id) → catalog_items(id)` RESTRICT/RESTRICT |
| `uq_configurator_effects_option` | UNIQUE | `(option_id, catalog_item_id, effect)` |
| `uq_configurator_effects_question` | UNIQUE | `(question_id, catalog_item_id, effect)` |
| `chk_configurator_effects_effect` | CHECK | `effect IN ('ADD','REMOVE','SET_QUANTITY','QUANTITY_FROM_ANSWER')` |
| `chk_configurator_effects_trigger` | CHECK | `(option_id IS NOT NULL AND question_id IS NULL) OR (option_id IS NULL AND question_id IS NOT NULL)` |
| `chk_configurator_effects_quantity` | CHECK | `(effect = 'SET_QUANTITY' AND quantity IS NOT NULL AND quantity > 0) OR (effect <> 'SET_QUANTITY' AND quantity IS NULL)` |

`chk_configurator_effects_trigger` es exclusividad mutua: exactamente uno de los dos disparadores.
Sin él, un efecto con los dos rellenos se dispararía dos veces y metería el artículo dos veces en el
carrito.

**Ojo con las dos UNIQUE.** MySQL permite múltiples `NULL` en una constraint única, así que
`uq_configurator_effects_option` **no** deduplica las filas disparadas por pregunta (donde
`option_id` es `NULL`) y viceversa. Por eso hacen falta las dos, una para cada tipo de disparador.
No es redundancia: cada una cubre el conjunto que la otra deja pasar.

---

# FASE 3 · COTIZACIÓN

## 10 · `quotes`

**Slice:** `quote` · **`version`:** SÍ · **`enabled`:** SÍ · **`company_id`: NULABLE**

La oferta. Sirve para dos cosas distintas: cotizar a un prospecto que todavía no es cliente, y
cotizar una ampliación a uno que ya tiene contrato. **A partir de aquí nada se recalcula.**

> **Corrección post-implementación (issue #427, la otra mitad de #408): la idempotencia estaba
> acotada en la lectura pero no en el índice.** `CreateQuoteService` busca por `client_request_id`
> **acotado por empresa** en cuanto hay `companyId` (corregido en #408 para que reutilizar la llave
> de otra clínica no devolviera su cotización). Pero `uq_quotes_client_request` seguía siendo
> `UNIQUE (client_request_id)` **global**: la lectura y el índice dejaron de medir lo mismo. La
> consecuencia era peor que una fuga — el segundo tenant que reutilizaba una llave que otra clínica
> ya había usado quedaba **bloqueado permanentemente**: su lectura acotada no veía la fila que
> estorbaba, decidía insertar, chocaba contra el índice global, y reintentar con la misma llave
> —el contrato mismo de la idempotencia— nunca salía del 409.
>
> **Por qué un compuesto simple `UNIQUE (company_id, client_request_id)` no sirve.** `company_id`
> es nulable a propósito (prospecto sin empresa todavía), y MySQL da por satisfecha una `UNIQUE`
> multicolumna en cuanto **una** de sus columnas es `NULL` — múltiples filas con `company_id NULL`
> conviven sin comprobarse jamás entre sí, sea cual sea su `client_request_id`. Con el compuesto a
> secas, la deduplicación de prospectos —el caso para el que existe esta columna— quedaría
> completamente rota, no solo debilitada.
>
> **La corrección:** una columna generada que colapsa el `NULL` a un centinela, mismo patrón que
> `active_marker`, `current_item_marker` y `recurring_cycle_marker` — una `GENERATED ALWAYS ...
> STORED` que expresa una unicidad condicional que MySQL no sabe declarar de otra forma. Ver
> `client_request_scope` más abajo. Changeset `261_scope_quotes_client_request_by_company.xml`,
> **sin editar el `239` que crea esta tabla** — el propio issue avisa de que hacerlo rompe el
> checksum.

| Columna | Tipo | Nulabilidad | Default | Notas |
|---|---|---|---|---|
| `id` | `BIGINT` | `NOT NULL` | `AUTO_INCREMENT` | |
| `quote_number` | `VARCHAR(30)` | `NOT NULL` | — | `COT-2026-00184`. El número que se le dice al cliente por teléfono |
| `company_id` | `BIGINT` | **`NULL`** | — | Vacío si aún es un prospecto. Se rellena cuando la cotización se acepta y nace la empresa |
| `prospect_name` | `VARCHAR(150)` | `NULL` | — | |
| `prospect_email` | `VARCHAR(120)` | `NULL` | — | |
| `prospect_document` | `VARCHAR(50)` | `NULL` | — | Mismo ancho que `companies.identifier` (`014_create_companies.xml:15`) |
| `prospect_phone` | `VARCHAR(30)` | `NULL` | — | |
| `price_list_id` | `BIGINT` | `NOT NULL` | — | La tarifa con la que se cotizó, **congelada** |
| `billing_cycle` | `VARCHAR(20)` | `NOT NULL` | — | `MONTHLY` · `ANNUAL` |
| `subtotal_amount` | `DECIMAL(19,2)` | `NOT NULL` | `0.00` | Guardado, no calculado al vuelo |
| `discount_amount` | `DECIMAL(19,2)` | `NOT NULL` | `0.00` | |
| `tax_amount` | `DECIMAL(19,2)` | `NOT NULL` | `0.00` | |
| `total_amount` | `DECIMAL(19,2)` | `NOT NULL` | `0.00` | |
| `status` | `VARCHAR(20)` | `NOT NULL` | `'DRAFT'` | `DRAFT` · `SENT` · `ACCEPTED` · `REJECTED` · `EXPIRED` |
| `valid_until` | `DATE` | `NOT NULL` | — | Hasta cuándo se respeta el precio. Sin esto, alguien aparece en 2029 con una cotización de 2026 y tiene razón |
| `trial_days` | `INT` | `NOT NULL` | `0` | Días de prueba de **esta oferta**. Permite campañas sin tocar configuración global |
| `accepted_at` | `DATETIME(6)` | `NULL` | — | |
| `accepted_by_email` | `VARCHAR(120)` | `NULL` | — | |
| `accepted_ip` | `VARCHAR(45)` | `NULL` | — | 45 caracteres: cabe una IPv6 completa en notación textual (`0000:…:0000` = 39) más `%` y zona |
| `client_request_id` | `VARCHAR(64)` | `NOT NULL` | — | Llave antiduplicados: si el navegador reenvía la petición, no nacen dos cotizaciones. Deduplicación acotada por empresa desde el issue #427, ver `client_request_scope` |
| `client_request_scope` | `BIGINT` | *generada* `STORED` | — | **[AÑADIDO, issue #427, migración 261]** `GENERATED ALWAYS AS (COALESCE(company_id, -1)) STORED`. El centinela que separa el espacio de llaves de cada empresa real del espacio compartido de los prospectos. Ver la nota de corrección arriba y el detalle debajo de la tabla de constraints |
| `created_date` | `DATETIME` | `NOT NULL` | `CURRENT_TIMESTAMP` | |
| `enabled` | `BOOLEAN` | `NOT NULL` | `TRUE` | |
| `version` | `BIGINT` | `NOT NULL` | `0` | |

| Nombre | Tipo | Definición |
|---|---|---|
| `PRIMARY` | PK | `(id)` |
| `uq_quotes_number` | UNIQUE | `(quote_number)` |
| `uq_quotes_client_request` | UNIQUE | `(client_request_scope, client_request_id)` — desde el issue #427; era `(client_request_id)` a secas |
| `fk_quotes_company` | FK | `(company_id) → companies(id)` RESTRICT/RESTRICT |
| `fk_quotes_price_list` | FK | `(price_list_id) → price_lists(id)` RESTRICT/RESTRICT |
| `chk_quotes_status` | CHECK | `status IN ('DRAFT','SENT','ACCEPTED','REJECTED','EXPIRED')` |
| `chk_quotes_cycle` | CHECK | `billing_cycle IN ('MONTHLY','ANNUAL')` |
| `chk_quotes_amounts` | CHECK | `subtotal_amount >= 0 AND discount_amount >= 0 AND tax_amount >= 0 AND total_amount >= 0` |
| `chk_quotes_trial_days` | CHECK | `trial_days >= 0` |
| `chk_quotes_party` | CHECK | `company_id IS NOT NULL OR prospect_name IS NOT NULL` |
| `chk_quotes_accepted` | CHECK | `(status = 'ACCEPTED' AND accepted_at IS NOT NULL) OR (status <> 'ACCEPTED')` |

`chk_quotes_party` cierra el estado absurdo de una cotización sin destinatario: o es de una empresa,
o al menos tiene el nombre del prospecto. Es lo mínimo para que el embudo comercial signifique algo.

**`client_request_scope` — por qué `-1` como centinela y no `0`.** `company_id` es `BIGINT
AUTO_INCREMENT` vía `companies(id)`, que arranca en 1 y solo crece. Insertar explícitamente `0` en
una columna `AUTO_INCREMENT` sin `NO_AUTO_VALUE_ON_ZERO` en el `sql_mode` hace que MySQL regenere
el siguiente valor en vez de guardar el `0` literal, así que `0` ya era un centinela razonablemente
seguro — pero ese comportamiento depende de una configuración de `sql_mode` que esta migración no
controla ni puede verificar contra dev/prod (regla del proyecto: no se consulta la base viva).
`-1` no depende de ninguna configuración: `AUTO_INCREMENT` jamás genera un valor negativo bajo
ningún `sql_mode`, y ninguna de las migraciones existentes ni `SchemaSeed.java` inserta jamás un id
explícito negativo. En sentido estricto sigue siendo un valor **insertable a mano** si alguien lo
fuerza —`companies.id` no lleva un `CHECK` que lo prohíba—, pero es la vía más alejada de cualquier
operación normal de la aplicación.

**Por qué `STORED` y no `VIRTUAL`.** `balance_amount` (ficha #20) es la única columna generada
`VIRTUAL` del modelo, y lo es porque no lleva índice encima. Esta sí lleva un índice `UNIQUE`, que
es el mismo criterio que decidió `STORED` para `active_marker`, `current_item_marker` y
`recurring_cycle_marker`. `company_id` solo cambia una vez por fila —cuando un prospecto acepta y
nace la empresa—, así que el coste de recalcular en cada `UPDATE` es irrelevante.

**Por qué sin `<preConditions>` de guardia, a diferencia de 206/210/226.** Esos tres changesets
**añaden** una restricción que datos existentes podrían violar, y por eso llevan un `HALT` previo.
Este changeset **afloja** una restricción (de global a acotada): si la tabla ya satisfacía
`UNIQUE (client_request_id)` global, satisface trivialmente
`UNIQUE (client_request_scope, client_request_id)` — un subconjunto de un conjunto ya único es
único. No hay combinación de datos existentes que pueda romper la creación del nuevo índice.

**Consecuencia de código que no se pudo resolver aquí** (fuera de mandato de `db-migrations`):
`QuotePersistenceIT.Idempotencia` fija el defecto de hoy —el test
`defecto_el_indice_unico_es_global_y_la_misma_llave_en_dos_empresas_revienta` espera
`DataIntegrityViolationException` al reutilizar la misma llave desde dos empresas distintas—, y con
este changeset esa inserción pasa a tener éxito, que es el comportamiento correcto. Ese test y el
javadoc de la clase necesitan actualizarse en el mismo PR. No hace falta tocar `QuoteJpaEntity` ni
ningún mapeo JPA: la columna nueva no está mapeada por ninguna entidad y `ddl-auto: validate` no
comprueba columnas ni índices que el `@Entity` no declare.

### Índices

| Nombre | Columnas | Qué consulta sirve |
|---|---|---|
| `ix_quotes_company_status` | `(company_id, status, created_date)` | El listado del cliente: sus cotizaciones, por estado, más recientes primero. **`company_id` delante** |
| `ix_quotes_expiring` | `(status, valid_until)` | El proceso que marca `EXPIRED` las que vencieron. Consulta de plataforma, sin tenant: `WHERE status IN ('SENT','DRAFT') AND valid_until < CURRENT_DATE` |

`ix_quotes_expiring` **no empieza por `company_id` a propósito y esto es una decisión, no un olvido**:
es un barrido de plataforma sobre todas las clínicas. Poner `company_id` delante lo obligaría a
recorrer el índice empresa por empresa. Está declarado aquí para que la próxima auditoría no lo
marque como defecto.

---

## 11 · `quote_lines`

**Slice:** `quote` · **`version`:** NO (`E1_APPEND_ONLY`) · **`enabled`:** SÍ · **sin `company_id`**
(ver `suscripciones-modelo.md` §4.7)

El renglón de la oferta. Los campos `item_*` son **copias, no referencias**: aunque el artículo se
renombre o se retire del catálogo, la cotización sigue diciendo lo que el cliente leyó.

> **Corrección post-implementación (issue #388): una sola `quantity` no bastaba.** La primera
> redacción de esta ficha daba por buena una única columna de cantidad. El configurador devuelve la
> cantidad **bruta** que pidió el cliente, pero lo que se factura es esa cantidad **menos las
> unidades que ya vienen incluidas en el tramo de la tarifa** — si el núcleo trae 2 usuarios
> incluidos y el cliente pide 3, se cobra 1. Con un solo campo no se puede guardar a la vez lo que
> el cliente contrató y lo que se le cobra, y una cotización que solo enseña el resultado obliga a
> hacer arqueología cuando el cliente reclama, que es justo lo que este modelo existe para evitar.
> Se añaden `contracted_quantity` e `included_quantity`; `quantity` se mantiene sin cambio de
> significado. Las tres cifras van **congeladas**, por el mismo motivo que `unit_amount` e
> `item_name`: editar un tramo de la tarifa no puede cambiar retroactivamente cuántas unidades le
> sobran a quien firmó hace un año — la causa número uno de sobrefacturación en modelos de
> suscripción, que esta misma especificación ya señalaba para `subscription_items` (ficha #15) y
> resulta que aplica igual aquí.

| Columna | Tipo | Nulabilidad | Default | Notas |
|---|---|---|---|---|
| `id` | `BIGINT` | `NOT NULL` | `AUTO_INCREMENT` | |
| `quote_id` | `BIGINT` | `NOT NULL` | — | |
| `catalog_item_id` | `BIGINT` | `NOT NULL` | — | Referencia al artículo, para poder navegar |
| `line_number` | `INT` | `NOT NULL` | — | **[AÑADIDO]** Orden estable de impresión. Sin él, el orden de la cotización impresa depende del orden de recuperación, que no es determinista |
| `item_code` | `VARCHAR(50)` | `NOT NULL` | — | Copia congelada |
| `item_name` | `VARCHAR(120)` | `NOT NULL` | — | Copia congelada |
| `item_type` | `VARCHAR(20)` | `NOT NULL` | — | Copia congelada |
| `quantity` | `INT` | `NOT NULL` | — | |
| `contracted_quantity` | `INT` | `NOT NULL` | — | **[AÑADIDO, issue #388]** Lo que el cliente pidió en bruto, antes de descontar lo incluido. Congelada |
| `included_quantity` | `INT` | `NOT NULL` | `0` | **[AÑADIDO, issue #388]** Cuántas unidades traía ya cubiertas el tramo de la tarifa en el momento de cotizar. Congelada — mismo motivo que `subscription_items.included_quantity` (ficha #15) |
| `unit_amount` | `DECIMAL(19,2)` | `NOT NULL` | — | Precio unitario congelado |
| `discount_percent` | `DECIMAL(5,2)` | `NOT NULL` | `0.00` | |
| `discount_amount` | `DECIMAL(19,2)` | `NOT NULL` | `0.00` | Guardado en porcentaje **y** en pesos: así se sabe qué se negoció y cuánto costó |
| `tax_rate` | `DECIMAL(5,2)` | `NOT NULL` | `0.00` | Congelada. Si el IVA cambia del 19 % al 18 %, los documentos viejos no mienten |
| `tax_treatment` | `VARCHAR(20)` | `NOT NULL` | — | **[AÑADIDO]** `TAXED` · `EXEMPT` · `EXCLUDED`. Sin él, `tax_rate = 0` es ambiguo entre exento, excluido y gravado al 0 % |
| `tax_amount` | `DECIMAL(19,2)` | `NOT NULL` | `0.00` | |
| `line_total` | `DECIMAL(19,2)` | `NOT NULL` | — | El total de la línea, guardado |
| `created_date` | `DATETIME` | `NOT NULL` | `CURRENT_TIMESTAMP` | |
| `enabled` | `BOOLEAN` | `NOT NULL` | `TRUE` | |

| Nombre | Tipo | Definición |
|---|---|---|
| `PRIMARY` | PK | `(id)` |
| `fk_quote_lines_quote` | FK | `(quote_id) → quotes(id)` RESTRICT/RESTRICT |
| `fk_quote_lines_item` | FK | `(catalog_item_id) → catalog_items(id)` RESTRICT/RESTRICT |
| `uq_quote_lines_number` | UNIQUE | `(quote_id, line_number)` |
| `uq_quote_lines_item` | UNIQUE | `(quote_id, catalog_item_id)` |
| `chk_quote_lines_quantity` | CHECK | `quantity > 0` |
| `chk_quote_lines_contracted_quantity` | CHECK | `contracted_quantity > 0` |
| `chk_quote_lines_included_quantity` | CHECK | `included_quantity >= 0` |
| `chk_quote_lines_amounts` | CHECK | `unit_amount >= 0 AND discount_amount >= 0 AND tax_amount >= 0 AND line_total >= 0` |
| `chk_quote_lines_discount_percent` | CHECK | `discount_percent >= 0 AND discount_percent <= 100` |
| `chk_quote_lines_tax_rate` | CHECK | `tax_rate >= 0 AND tax_rate <= 100` |
| `chk_quote_lines_item_type` | CHECK | `item_type IN ('MODULE','CAPACITY','ONE_TIME','BUNDLE')` |
| `chk_quote_lines_tax_treatment` | CHECK | `tax_treatment IN ('TAXED','EXEMPT','EXCLUDED')` |

`uq_quote_lines_item` impide cotizar el mismo artículo dos veces en la misma oferta, que es la vía
rápida a un total que no cuadra con lo que el cliente cree que compró.

**No declarable:** que `subtotal + tax = total` en la cabecera coincida con la suma de las líneas. Un
`CHECK` no puede agregar filas de otra tabla. Regla R5 en `suscripciones-reglas-codigo.md`.

---

## 12 · `quote_answers`

**Slice:** `quote` · **`version`:** NO (`E1_APPEND_ONLY`) · **`enabled`:** SÍ · **sin `company_id`**

Por qué se cotizó eso. Parece accesorio y no lo es: es la única forma de responder «¿por qué le
vendimos esto?» seis meses después.

| Columna | Tipo | Nulabilidad | Default | Notas |
|---|---|---|---|---|
| `id` | `BIGINT` | `NOT NULL` | `AUTO_INCREMENT` | |
| `quote_id` | `BIGINT` | `NOT NULL` | — | |
| `question_id` | `BIGINT` | `NOT NULL` | — | |
| `option_id` | `BIGINT` | `NULL` | — | Vacío en preguntas numéricas o de texto |
| `question_code` | `VARCHAR(50)` | `NOT NULL` | — | Copia del código, por si se reescribe el cuestionario |
| `answer_value` | `VARCHAR(255)` | `NULL` | — | La respuesta literal |
| `created_date` | `DATETIME` | `NOT NULL` | `CURRENT_TIMESTAMP` | |
| `enabled` | `BOOLEAN` | `NOT NULL` | `TRUE` | |

| Nombre | Tipo | Definición |
|---|---|---|
| `PRIMARY` | PK | `(id)` |
| `fk_quote_answers_quote` | FK | `(quote_id) → quotes(id)` RESTRICT/RESTRICT |
| `fk_quote_answers_question` | FK | `(question_id) → configurator_questions(id)` RESTRICT/RESTRICT |
| `fk_quote_answers_option` | FK | `(option_id) → configurator_options(id)` RESTRICT/RESTRICT |
| `uq_quote_answers` | UNIQUE | `(quote_id, question_id, option_id)` |
| `chk_quote_answers_payload` | CHECK | `option_id IS NOT NULL OR answer_value IS NOT NULL` |

**Límite conocido de `uq_quote_answers`:** MySQL permite múltiples `NULL` en una constraint única, así
que **no** impide dos respuestas a la misma pregunta numérica en la misma cotización (ambas con
`option_id` nulo). No se emula con columna generada porque el daño es cosmético —una respuesta
duplicada en un informe— y no justifica una columna `STORED` más en cada fila. Queda registrado como
límite, no como olvido.

---

# FASE 4 · EL CONTRATO

## 13 · `subscriptions`

**Slice:** `subscription` · **`version`:** SÍ · **`enabled`:** SÍ · **`company_id` NOT NULL**

La carpeta del cliente. Reemplaza a `companies.membership_id`: en vez de «esta empresa es del plan
3», ahora es «esta empresa tiene el contrato SUS-2026-00184, vigente, ciclo mensual, periodo actual
del 1 al 30 de septiembre».

| Columna | Tipo | Nulabilidad | Default | Notas |
|---|---|---|---|---|
| `id` | `BIGINT` | `NOT NULL` | `AUTO_INCREMENT` | |
| `subscription_number` | `VARCHAR(30)` | `NOT NULL` | — | `SUS-2026-00184`. El que se cita en soporte y cobranza |
| `company_id` | `BIGINT` | `NOT NULL` | — | |
| `quote_id` | `BIGINT` | `NULL` | — | Cierra la trazabilidad cotización → contrato |
| `price_list_id` | `BIGINT` | `NOT NULL` | — | La tarifa con la que se firmó, congelada mientras dure el contrato |
| `billing_cycle` | `VARCHAR(20)` | `NOT NULL` | — | `MONTHLY` · `ANNUAL` |
| `status` | `VARCHAR(20)` | `NOT NULL` | — | `TRIALING` · `ACTIVE` · `PAST_DUE` · `READ_ONLY` · `CANCELLED` · `EXPIRED` |
| `start_date` | `DATE` | `NOT NULL` | — | |
| `trial_end_date` | `DATE` | `NULL` | — | Vacío si no hubo prueba |
| `current_period_start` | `DATE` | `NOT NULL` | — | |
| `current_period_end` | `DATE` | `NOT NULL` | — | **La referencia de todos los prorrateos**: los días que faltan se cuentan contra aquí |
| `next_billing_date` | `DATE` | `NULL` | — | Por donde busca el proceso automático de facturación |
| `commitment_end_date` | `DATE` | `NULL` | — | Permanencia del plan anual. Sin este campo, el descuento anual se toma y se cancela al mes siguiente |
| `grace_days` | `INT` | `NOT NULL` | `0` | Por contrato, no por código: a un cliente grande se le pueden dar 15 |
| `past_due_since` | `DATE` | `NULL` | — | Desde cuándo debe. Cuenta la gracia y alimenta el informe de cartera |
| `auto_renew` | `BOOLEAN` | `NOT NULL` | `TRUE` | |
| `cancel_requested_at` | `DATETIME(6)` | `NULL` | — | **Cuándo lo pidió** |
| `cancel_effective_date` | `DATE` | `NULL` | — | **Cuándo surte efecto.** El cliente cancela el 10 y se va el 30, que es lo que ya pagó |
| `cancel_reason` | `VARCHAR(255)` | `NULL` | — | Información de negocio, no burocracia |
| `active_marker` | `BIGINT` | *generada* | — | Ver abajo |
| `created_date` | `DATETIME` | `NOT NULL` | `CURRENT_TIMESTAMP` | |
| `enabled` | `BOOLEAN` | `NOT NULL` | `TRUE` | |
| `version` | `BIGINT` | `NOT NULL` | `0` | |

### La columna generada `active_marker`

**Invariante:** *una empresa tiene como máximo un contrato vigente*. Es lo único de la lista de
invariantes que la base impide por sí sola, y su ausencia es la vía más rápida a facturar doble.

MySQL **no tiene índices únicos parciales** (eso es PostgreSQL). Se emula con el patrón de la casa —
`195_create_cash_register.xml:52-59`, `206`, `210_scope_open_accounts_by_branch.xml:13-18`,
`226_add_unique_active_appointment_slot.xml:61-73` — una columna `GENERATED ALWAYS AS (…) STORED` que
vale `NULL` fuera de alcance, porque **MySQL permite múltiples `NULL` en un índice único**.

```sql
active_marker BIGINT
GENERATED ALWAYS AS (
    CASE
        WHEN enabled = TRUE
         AND status IN ('TRIALING', 'ACTIVE', 'PAST_DUE', 'READ_ONLY')
        THEN company_id
        ELSE NULL
    END
) STORED
```

**Criterio de «vigente», y esto es lo que más se equivoca:** *no* es «sin fecha de fin» ni «status =
ACTIVE». Es **«ya empezó y todavía no ha terminado»**. Un contrato en `PAST_DUE` sigue siendo el
contrato vigente de esa empresa —debe, pero sigue trabajando— y uno en `READ_ONLY` también. Los que
salen del marcador son `CANCELLED` y `EXPIRED`. Si el recálculo de permisos usa el criterio
equivocado, el error es invisible hasta que un cliente reclama.

**Por qué `STORED` y no `VIRTUAL`:** porque lleva un índice único encima. InnoDB admite índices
secundarios sobre columnas `VIRTUAL`, así que las dos serían legales; se elige `STORED` porque es el
patrón que el repositorio ya tiene en cuatro sitios y romperlo obligaría a justificar en cada
revisión por qué esta es distinta, y porque la columna se lee directamente en la consulta de
vigilancia sin reevaluar la expresión. **Se declara dentro del `CREATE TABLE`**, nunca en un `ALTER`
posterior: añadir una columna generada `STORED` reconstruye la tabla y no permite DML concurrente
(<https://dev.mysql.com/doc/refman/8.4/en/innodb-online-ddl-operations.html>).

### Constraints

| Nombre | Tipo | Definición |
|---|---|---|
| `PRIMARY` | PK | `(id)` |
| `uq_subscriptions_number` | UNIQUE | `(subscription_number)` |
| `uq_subscriptions_active_company` | UNIQUE | `(active_marker)` |
| `uq_subscriptions_company_id` | UNIQUE | `(company_id, id)` — **clave auxiliar de las FK compuestas** |
| `fk_subscriptions_company` | FK | `(company_id) → companies(id)` RESTRICT/RESTRICT |
| `fk_subscriptions_quote` | FK | `(quote_id) → quotes(id)` RESTRICT/RESTRICT |
| `fk_subscriptions_price_list` | FK | `(price_list_id) → price_lists(id)` RESTRICT/RESTRICT |
| `chk_subscriptions_status` | CHECK | `status IN ('TRIALING','ACTIVE','PAST_DUE','READ_ONLY','CANCELLED','EXPIRED')` |
| `chk_subscriptions_cycle` | CHECK | `billing_cycle IN ('MONTHLY','ANNUAL')` |
| `chk_subscriptions_period` | CHECK | `current_period_end >= current_period_start` |
| `chk_subscriptions_grace_days` | CHECK | `grace_days >= 0` |
| `chk_subscriptions_trial` | CHECK | `status <> 'TRIALING' OR trial_end_date IS NOT NULL` |
| `chk_subscriptions_cancel` | CHECK | `(cancel_requested_at IS NULL AND cancel_effective_date IS NULL) OR (cancel_requested_at IS NOT NULL AND cancel_effective_date IS NOT NULL)` |
| `chk_subscriptions_commitment` | CHECK | `commitment_end_date IS NULL OR commitment_end_date >= start_date` |
| `chk_subscriptions_past_due` | CHECK | `past_due_since IS NULL OR past_due_since >= start_date` |

### Índices

| Nombre | Columnas | Qué consulta sirve |
|---|---|---|
| `uq_subscriptions_company_id` | `(company_id, id)` | Doble uso: clave auxiliar de FK **y** listado de contratos de una empresa. **`company_id` delante** |
| `ix_subscriptions_next_billing` | `(next_billing_date, status)` | El proceso automático de facturación: `WHERE next_billing_date <= CURRENT_DATE AND status IN ('ACTIVE','TRIALING')` |
| `ix_subscriptions_past_due` | `(status, past_due_since)` | El informe de cartera y el proceso de mora |

`ix_subscriptions_next_billing` e `ix_subscriptions_past_due` **no empiezan por `company_id`, y es
deliberado**: son barridos de plataforma sobre todas las clínicas, no consultas de un tenant. Poner
`company_id` delante los obligaría a recorrer clínica por clínica. Escrito aquí para que no se
"corrija" en la próxima auditoría.

---

## 14 · `subscription_amendments`

**Slice:** `subscription` · **`version`:** NO (`E1_APPEND_ONLY`) · **`enabled`:** NO ·
**`company_id` NOT NULL**

El papel de cada cambio. Un documento inmutable por modificación del contrato, el equivalente al
otrosí. **Sin esta tabla no hay auditoría posible:** se vería el estado final pero no la película de
cómo se llegó ahí.

Se crea **antes** que `subscription_items` porque esta apunta a ella con `created_amendment_id` /
`ended_amendment_id`.

> **Corrección post-implementación: la llave de idempotencia se acota por empresa.** La primera
> redacción declaraba `uq_subscription_amendments_client_request` como `UNIQUE (client_request_id)`
> a secas —global—, pero `JpaSubscriptionAmendmentRepository.findByClientRequestIdAndCompanyId`
> deduplica **acotado por empresa**. Con un índice global, dos clínicas que generasen la misma
> cadena (el cliente la elige, no hay que imaginar una colisión de UUID) chocarían: la segunda
> busca acotada por su empresa, no encuentra nada, decide insertar y se estrella contra una fila de
> otro tenant que no puede ver. Se corrige a `UNIQUE (company_id, client_request_id)` —mismo ajuste
> que en `billing_document_applications` (ficha #24, issue #396)—, con la empresa primero, mismo
> criterio que `uq_subscription_amendments_company_id`.

| Columna | Tipo | Nulabilidad | Default | Notas |
|---|---|---|---|---|
| `id` | `BIGINT` | `NOT NULL` | `AUTO_INCREMENT` | |
| `company_id` | `BIGINT` | `NOT NULL` | — | Discriminador, arrastrado por las FK compuestas |
| `subscription_id` | `BIGINT` | `NOT NULL` | — | |
| `amendment_number` | `VARCHAR(30)` | `NOT NULL` | — | Su número propio, citable |
| `amendment_type` | `VARCHAR(25)` | `NOT NULL` | — | `ADD_ITEM` · `REMOVE_ITEM` · `CHANGE_QUANTITY` · `CHANGE_CYCLE` · `SUSPEND` · `REACTIVATE` · `CANCEL` · `PRICE_LIST_MIGRATION` |
| `effective_date` | `DATE` | `NOT NULL` | — | Desde cuándo aplica el cambio |
| `reason` | `VARCHAR(255)` | `NULL` | — | «Contrató veterinaria», «se pasó a la competencia» |
| `requested_by_employee_id` | `BIGINT` | `NULL` | — | El propio cliente desde su cuenta |
| `requested_by_system_user_id` | `BIGINT` | `NULL` | — | Alguien de la plataforma. **Dos campos distintos porque la responsabilidad es distinta** |
| `proration_amount` | `DECIMAL(19,2)` | `NOT NULL` | `0.00` | **Con signo**: negativo si se acredita |
| `monthly_delta_amount` | `DECIMAL(19,2)` | `NOT NULL` | `0.00` | **Con signo**: cuánto sube o baja la factura recurrente |
| `quote_id` | `BIGINT` | `NULL` | — | Si la ampliación vino de una cotización |
| `client_request_id` | `VARCHAR(64)` | `NOT NULL` | — | Antiduplicados, acotado por empresa: dos clics en «Añadir» no generan dos cobros |
| `created_date` | `DATETIME` | `NOT NULL` | `CURRENT_TIMESTAMP` | |

| Nombre | Tipo | Definición |
|---|---|---|
| `PRIMARY` | PK | `(id)` |
| `uq_subscription_amendments_number` | UNIQUE | `(amendment_number)` |
| `uq_subscription_amendments_client_request` | UNIQUE | `(company_id, client_request_id)` |
| `uq_subscription_amendments_company_id` | UNIQUE | `(company_id, id)` — clave auxiliar |
| `fk_subscription_amendments_company` | FK | `(company_id) → companies(id)` RESTRICT/RESTRICT |
| `fk_subscription_amendments_subscription` | FK | **compuesta** `(company_id, subscription_id) → subscriptions(company_id, id)` RESTRICT/RESTRICT |
| `fk_subscription_amendments_employee` | FK | `(requested_by_employee_id) → employees(id)` RESTRICT/RESTRICT |
| `fk_subscription_amendments_system_user` | FK | `(requested_by_system_user_id) → system_users(id)` RESTRICT/RESTRICT |
| `fk_subscription_amendments_quote` | FK | `(quote_id) → quotes(id)` RESTRICT/RESTRICT |
| `chk_subscription_amendments_type` | CHECK | `amendment_type IN ('ADD_ITEM','REMOVE_ITEM','CHANGE_QUANTITY','CHANGE_CYCLE','SUSPEND','REACTIVATE','CANCEL','PRICE_LIST_MIGRATION')` |
| `chk_subscription_amendments_actor` | CHECK | `(requested_by_employee_id IS NOT NULL AND requested_by_system_user_id IS NULL) OR (requested_by_employee_id IS NULL AND requested_by_system_user_id IS NOT NULL)` |

`chk_subscription_amendments_actor` es exclusividad mutua y **obligatoriedad**: todo otrosí tiene
exactamente un responsable. Un cambio de contrato sin responsable es un cambio que nadie firmó.

**Nota para `backend-feature`:** `fk_subscription_amendments_employee` cruza a la feature de
empleados. `REFERENCIAS_CROSS_FEATURE_ACOTADAS_POR_EMPRESA` obliga a que la resolución del empleado
vaya acotada por empresa — y aquí además la base no lo puede imponer, porque el empleado se referencia
por FK simple. Es una regla de código: **el empleado que firma el otrosí tiene que ser de la misma
empresa que el contrato**. Va como R14 en `suscripciones-reglas-codigo.md`.

### Índices

| Nombre | Columnas | Qué sirve |
|---|---|---|
| `uq_subscription_amendments_company_id` | `(company_id, id)` | Clave auxiliar + listado por empresa |
| *(automático de la FK compuesta)* | `(company_id, subscription_id)` | El historial de otrosíes de un contrato |
| `ix_subscription_amendments_effective` | `(company_id, subscription_id, effective_date)` | La película en orden: `WHERE company_id = ? AND subscription_id = ? ORDER BY effective_date` sin `filesort` |

`ix_subscription_amendments_effective` **contiene** al índice automático de la FK como prefijo por la
izquierda. Es redundancia deliberada y resoluble: se declara solo `ix_subscription_amendments_effective`
y se deja que MySQL use ese mismo índice para la comprobación de la FK, porque
`(company_id, subscription_id)` es su prefijo por la izquierda
(<https://dev.mysql.com/doc/refman/8.4/en/multiple-column-indexes.html>: *"any leftmost prefix of the
index can be used by the optimizer"*). **`db-migrations`: crea el índice de tres columnas ANTES de
declarar la FK compuesta, y MySQL no creará el suyo.** Si se declara la FK primero, aparecen los dos
y `sys.schema_redundant_indexes` marcará uno.

Esa advertencia aplica igual a **todas** las tablas de este documento con FK compuesta e índice de
listado más largo.

---

## 15 · `subscription_items`

**Slice:** `subscription` · **`version`:** SÍ · **`enabled`:** SÍ · **`company_id` NOT NULL**

**La tabla más importante del modelo.** Cada fila es «este cliente tiene contratado esto, a este
precio, desde esta fecha». Dar de baja un módulo **no borra la fila**: le pone fecha de fin.

| Columna | Tipo | Nulabilidad | Default | Notas |
|---|---|---|---|---|
| `id` | `BIGINT` | `NOT NULL` | `AUTO_INCREMENT` | |
| `company_id` | `BIGINT` | `NOT NULL` | — | |
| `subscription_id` | `BIGINT` | `NOT NULL` | — | |
| `catalog_item_id` | `BIGINT` | `NOT NULL` | — | |
| `item_code` | `VARCHAR(50)` | `NOT NULL` | — | Copia congelada |
| `item_name` | `VARCHAR(120)` | `NOT NULL` | — | Copia congelada |
| `item_type` | `VARCHAR(20)` | `NOT NULL` | — | Copia congelada |
| `capacity_unit` | `VARCHAR(30)` | `NULL` | — | Copia congelada; solo para `CAPACITY` |
| `included_quantity` | `INT` | `NOT NULL` | `0` | **Congelada al firmar.** Sin este campo, editar un tramo de la tarifa cambiaría retroactivamente cuántos usuarios le sobran a un cliente que firmó hace un año — **la causa número uno de sobrefacturación en modelos de suscripción** |
| `tax_treatment` | `VARCHAR(20)` | `NOT NULL` | — | Congelado |
| `quantity` | `INT` | `NOT NULL` | `1` | Para módulos siempre 1; para capacidades, el número contratado |
| `unit_amount` | `DECIMAL(19,2)` | `NOT NULL` | — | **El precio congelado del cliente. No se edita jamás:** cambiar de precio es cerrar esta línea y abrir otra |
| `tax_rate` | `DECIMAL(5,2)` | `NOT NULL` | `0.00` | Congelada |
| `effective_from` | `DATE` | `NOT NULL` | — | Desde cuándo cuenta |
| `effective_to` | `DATE` | `NULL` | — | Vacío = vigente ahora. Dar de baja = poner esta fecha |
| `origin` | `VARCHAR(25)` | `NOT NULL` | — | `INITIAL` · `ADDON` · `QUANTITY_CHANGE` · `REMOVAL` · `MIGRATION` |
| `created_amendment_id` | `BIGINT` | `NULL` | — | Qué documento la abrió |
| `ended_amendment_id` | `BIGINT` | `NULL` | — | Cuál la cerró |
| `current_item_marker` | `BIGINT` | *generada* | — | Ver abajo |
| `created_date` | `DATETIME` | `NOT NULL` | `CURRENT_TIMESTAMP` | |
| `enabled` | `BOOLEAN` | `NOT NULL` | `TRUE` | |
| `version` | `BIGINT` | `NOT NULL` | `0` | |

### La columna generada `current_item_marker`

**Invariante:** *un mismo artículo no puede tener dos líneas abiertas a la vez en el mismo
contrato*. Sería facturarlo dos veces.

```sql
current_item_marker BIGINT
GENERATED ALWAYS AS (
    CASE
        WHEN enabled = TRUE AND effective_to IS NULL
        THEN catalog_item_id
        ELSE NULL
    END
) STORED
```

Con `UNIQUE (subscription_id, current_item_marker)`.

**Lo que NO garantiza, y el modelo ya no lo promete:** dos tramos del mismo artículo con **fechas de
fin futuras** que se pisen **sí caben**. Eso no es expresable en MySQL —no existen restricciones de
exclusión— y está reclasificado como **regla que el código debe garantizar**, con su consulta de
vigilancia (R7 en `suscripciones-reglas-codigo.md`). La primera versión del modelo lo daba por seguro
y no lo estaba.

`STORED` por el mismo motivo que en `subscriptions`, y declarada dentro del `CREATE TABLE`.

### Constraints

| Nombre | Tipo | Definición |
|---|---|---|
| `PRIMARY` | PK | `(id)` |
| `uq_subscription_items_current` | UNIQUE | `(subscription_id, current_item_marker)` |
| `uq_subscription_items_company_id` | UNIQUE | `(company_id, id)` — clave auxiliar (**par P6**) |
| `fk_subscription_items_company` | FK | `(company_id) → companies(id)` RESTRICT/RESTRICT |
| `fk_subscription_items_subscription` | FK | **compuesta** `(company_id, subscription_id) → subscriptions(company_id, id)` RESTRICT/RESTRICT (**par P1**) |
| `fk_subscription_items_item` | FK | `(catalog_item_id) → catalog_items(id)` RESTRICT/RESTRICT |
| `fk_subscription_items_created_amendment` | FK | **compuesta** `(company_id, created_amendment_id) → subscription_amendments(company_id, id)` RESTRICT/RESTRICT |
| `fk_subscription_items_ended_amendment` | FK | **compuesta** `(company_id, ended_amendment_id) → subscription_amendments(company_id, id)` RESTRICT/RESTRICT |
| `chk_subscription_items_dates` | CHECK | `effective_to IS NULL OR effective_to >= effective_from` |
| `chk_subscription_items_quantity` | CHECK | `quantity > 0` |
| `chk_subscription_items_included` | CHECK | `included_quantity >= 0` |
| `chk_subscription_items_unit_amount` | CHECK | `unit_amount >= 0` |
| `chk_subscription_items_tax_rate` | CHECK | `tax_rate >= 0 AND tax_rate <= 100` |
| `chk_subscription_items_origin` | CHECK | `origin IN ('INITIAL','ADDON','QUANTITY_CHANGE','REMOVAL','MIGRATION')` |
| `chk_subscription_items_item_type` | CHECK | `item_type IN ('MODULE','CAPACITY','ONE_TIME','BUNDLE')` |
| `chk_subscription_items_tax_treatment` | CHECK | `tax_treatment IN ('TAXED','EXEMPT','EXCLUDED')` |
| `chk_subscription_items_capacity_unit` | CHECK | `(item_type = 'CAPACITY' AND capacity_unit IN ('USER','BRANCH','TERMINAL','STORAGE_GB')) OR (item_type <> 'CAPACITY' AND capacity_unit IS NULL)` |
| `chk_subscription_items_ended` | CHECK | `ended_amendment_id IS NULL OR effective_to IS NOT NULL` |

`chk_subscription_items_ended` cierra la incoherencia de una línea "cerrada por el otrosí 12" que
sigue sin fecha de fin: o está cerrada de verdad, o no tiene documento de cierre.

### Índices

| Nombre | Columnas | Qué consulta sirve |
|---|---|---|
| `uq_subscription_items_company_id` | `(company_id, id)` | Clave auxiliar |
| `ix_subscription_items_vigencia` | `(company_id, subscription_id, effective_from, effective_to)` | **La consulta que da sentido a la tabla**: «¿qué tenía Ana el 3 de marzo?» → `WHERE company_id = ? AND subscription_id = ? AND effective_from <= ? AND (effective_to IS NULL OR effective_to > ?)`. Igualdades primero, rangos después |
| `ix_subscription_items_catalog_item` | `(catalog_item_id)` | «¿Qué clínicas tienen contratado este módulo?» — informe de plataforma. Lo crearía la FK de todos modos |

`ix_subscription_items_vigencia` **contiene como prefijo** a `(company_id, subscription_id)`, así que
sirve además para la FK compuesta `fk_subscription_items_subscription`. **Créalo antes de declarar la
FK.**

---

## 16 · `subscription_status_history`

**Slice:** `subscription` · **`version`:** NO (`E1_APPEND_ONLY`) · **`enabled`:** NO ·
**`company_id` NOT NULL**

La película del contrato. Responde «¿por qué esta cuenta está en solo lectura?» en un segundo, en
lugar de deducirlo de los pagos.

| Columna | Tipo | Nulabilidad | Default | Notas |
|---|---|---|---|---|
| `id` | `BIGINT` | `NOT NULL` | `AUTO_INCREMENT` | |
| `company_id` | `BIGINT` | `NOT NULL` | — | |
| `subscription_id` | `BIGINT` | `NOT NULL` | — | |
| `from_status` | `VARCHAR(20)` | `NULL` | — | `NULL` en la primera fila: el contrato no venía de ningún estado |
| `to_status` | `VARCHAR(20)` | `NOT NULL` | — | |
| `reason` | `VARCHAR(255)` | `NULL` | — | «Factura FE-1043 vencida hace 6 días» |
| `occurred_at` | `DATETIME(6)` | `NOT NULL` | — | Microsegundos: dos transiciones dentro del mismo segundo tienen que ordenarse |
| `actor` | `VARCHAR(120)` | `NOT NULL` | — | El sistema automático o una persona con nombre |
| `created_date` | `DATETIME` | `NOT NULL` | `CURRENT_TIMESTAMP` | |

| Nombre | Tipo | Definición |
|---|---|---|
| `PRIMARY` | PK | `(id)` |
| `fk_ssh_company` | FK | `(company_id) → companies(id)` RESTRICT/RESTRICT |
| `fk_ssh_subscription` | FK | **compuesta** `(company_id, subscription_id) → subscriptions(company_id, id)` RESTRICT/RESTRICT |
| `chk_ssh_to_status` | CHECK | `to_status IN ('TRIALING','ACTIVE','PAST_DUE','READ_ONLY','CANCELLED','EXPIRED')` |
| `chk_ssh_from_status` | CHECK | `from_status IS NULL OR from_status IN ('TRIALING','ACTIVE','PAST_DUE','READ_ONLY','CANCELLED','EXPIRED')` |
| `chk_ssh_change` | CHECK | `from_status IS NULL OR from_status <> to_status` |

`chk_ssh_change` impide la fila de ruido «de ACTIVE a ACTIVE», que ensucia la película sin aportar.

### Índices

| Nombre | Columnas | Qué sirve |
|---|---|---|
| `ix_ssh_subscription_occurred` | `(company_id, subscription_id, occurred_at)` | La película en orden. Prefijo de la FK compuesta: **crear antes que la FK** |

---

# FASE 5 · PERMISOS DERIVADOS

> Las dos tablas de esta fase son **derivadas**: no contienen ninguna decisión, solo el resultado de
> aplicar el contrato vigente. Si se corrompen, se recalculan desde cero y no se pierde nada. Son lo
> único que la aplicación consulta **en cada petición**.

## 17 · `company_entitlements`

**Slice:** `entitlement` · **`version`:** NO (`E6_YA_PROTEGIDO`) · **`enabled`:** NO ·
**`company_id` NOT NULL**

Qué puede usar cada empresa **ahora mismo**. Sustituye funcionalmente a `membership_sub_modules` como
fuente de verdad del acceso, y cierra un agujero real del sistema actual: **bajar de plan hoy no le
quita el acceso a nadie**.

| Columna | Tipo | Nulabilidad | Default | Notas |
|---|---|---|---|---|
| `id` | `BIGINT` | `NOT NULL` | `AUTO_INCREMENT` | |
| `company_id` | `BIGINT` | `NOT NULL` | — | |
| `sub_module_id` | `BIGINT` | `NOT NULL` | — | |
| `access_level` | `VARCHAR(15)` | `NOT NULL` | — | `FULL` uso normal · `READ_ONLY` consulta e impresión, no crear ni modificar · `NONE` no existe para él |
| `source` | `VARCHAR(20)` | `NOT NULL` | — | `SUBSCRIPTION` lo paga · `TRIAL` está de prueba · `CORE` viene con el núcleo · `MANUAL_GRANT` se lo diste tú a mano, **y queda constancia de que fue a mano** |
| `subscription_id` | `BIGINT` | `NULL` | — | Qué contrato lo justifica |
| `subscription_item_id` | `BIGINT` | `NULL` | — | Qué **línea** lo justifica. El puente de vuelta al dinero |
| `valid_from` | `DATETIME(6)` | `NOT NULL` | — | |
| `valid_until` | `DATETIME(6)` | `NULL` | — | **Lo que hace que la prueba caduque sola a la fecha**, sin ningún proceso que se pueda olvidar de correr |
| `recalculated_at` | `DATETIME(6)` | `NOT NULL` | — | Si esta fecha se queda vieja, hay un proceso caído. **Es un indicador de salud, no un adorno** |
| `created_date` | `DATETIME` | `NOT NULL` | `CURRENT_TIMESTAMP` | |

| Nombre | Tipo | Definición |
|---|---|---|
| `PRIMARY` | PK | `(id)` |
| `uq_company_entitlements` | UNIQUE | `(company_id, sub_module_id)` |
| `fk_company_entitlements_company` | FK | `(company_id) → companies(id)` RESTRICT/RESTRICT |
| `fk_company_entitlements_sub_module` | FK | `(sub_module_id) → sub_modules(id)` RESTRICT/RESTRICT |
| `fk_company_entitlements_subscription` | FK | **compuesta** `(company_id, subscription_id) → subscriptions(company_id, id)` RESTRICT/RESTRICT |
| `fk_company_entitlements_item` | FK | **compuesta** `(company_id, subscription_item_id) → subscription_items(company_id, id)` RESTRICT/RESTRICT (**par P6**) |
| `chk_company_entitlements_access_level` | CHECK | `access_level IN ('FULL','READ_ONLY','NONE')` |
| `chk_company_entitlements_source` | CHECK | `source IN ('SUBSCRIPTION','TRIAL','CORE','MANUAL_GRANT')` |
| `chk_company_entitlements_validity` | CHECK | `valid_until IS NULL OR valid_until > valid_from` |
| `chk_company_entitlements_origin` | CHECK | `source NOT IN ('SUBSCRIPTION','TRIAL') OR subscription_id IS NOT NULL` |

`chk_company_entitlements_origin` obliga a que un permiso que dice venir del contrato **tenga**
contrato. Sin él, `source = 'SUBSCRIPTION'` con `subscription_id` nulo es un permiso huérfano que
nadie sabe de dónde salió y que el recálculo no puede revocar.

### Índices

| Nombre | Columnas | Qué consulta sirve |
|---|---|---|
| `uq_company_entitlements` | `(company_id, sub_module_id)` | **El acceso caliente.** «¿Qué puede usar esta empresa?» → `WHERE company_id = ?`, y «¿puede usar este submódulo?» → *point lookup* por las dos columnas |
| `ix_company_entitlements_stale` | `(recalculated_at)` | El indicador de salud: `WHERE recalculated_at < NOW() - INTERVAL 1 DAY`. Barrido de plataforma |

**Deliberadamente NO se crea** un índice `(company_id, access_level, valid_until)`. Por empresa habrá
del orden de 15-40 filas: `uq_company_entitlements` las trae todas con un rango sobre `company_id` y
el filtro por validez se resuelve en memoria. Un índice más pagaría escritura en cada recálculo para
no ahorrar nada. **Si algún día una empresa pasa de unos cientos de submódulos, se reevalúa con un
`EXPLAIN`, no antes.**

**Cómo se recalcula, y esto condiciona el mapeo JPA:** borrado **físico** de las filas de la empresa
más reinserción, dentro de una transacción. No hay `enabled`, no hay `@SQLDelete`, no hay
`@SQLRestriction`. El repositorio expone un `deleteByCompanyId` real, y es **la única excepción de
borrado físico de todo este modelo**.

---

## 18 · `company_capacities`

**Slice:** `entitlement` · **`version`:** NO (`E6_YA_PROTEGIDO`) · **`enabled`:** NO ·
**`company_id` NOT NULL**

Lo que no es una pantalla sino una cantidad. Guarda el techo contratado **y** el consumo actual, para
poder avisar antes de bloquear y ofrecer la ampliación en el momento exacto en que hace falta.

| Columna | Tipo | Nulabilidad | Default | Notas |
|---|---|---|---|---|
| `id` | `BIGINT` | `NOT NULL` | `AUTO_INCREMENT` | |
| `company_id` | `BIGINT` | `NOT NULL` | — | |
| `capacity_unit` | `VARCHAR(30)` | `NOT NULL` | — | `USER` · `BRANCH` · `TERMINAL` · `STORAGE_GB` |
| `limit_quantity` | `INT` | `NOT NULL` | `0` | El techo que pagó, sumando lo incluido y lo comprado aparte |
| `used_quantity` | `INT` | `NOT NULL` | `0` | Lo que lleva usado |
| `subscription_id` | `BIGINT` | `NULL` | — | **[AÑADIDO]** Qué contrato justifica el techo. Sin él, `limit_quantity` es un número sin papel detrás y el recálculo no puede verificarse |
| `recalculated_at` | `DATETIME(6)` | `NOT NULL` | — | |
| `created_date` | `DATETIME` | `NOT NULL` | `CURRENT_TIMESTAMP` | |

| Nombre | Tipo | Definición |
|---|---|---|
| `PRIMARY` | PK | `(id)` |
| `uq_company_capacities` | UNIQUE | `(company_id, capacity_unit)` |
| `fk_company_capacities_company` | FK | `(company_id) → companies(id)` RESTRICT/RESTRICT |
| `fk_company_capacities_subscription` | FK | **compuesta** `(company_id, subscription_id) → subscriptions(company_id, id)` RESTRICT/RESTRICT |
| `chk_company_capacities_unit` | CHECK | `capacity_unit IN ('USER','BRANCH','TERMINAL','STORAGE_GB')` |
| `chk_company_capacities_quantities` | CHECK | `limit_quantity >= 0 AND used_quantity >= 0` |

**No hay `CHECK (used_quantity <= limit_quantity)` a propósito.** Bajar de plan deja legítimamente a
un cliente con 5 usuarios y un techo de 3: los datos no se destruyen, se le impide **crear más**. Una
constraint que lo prohibiera haría imposible registrar la bajada, que es exactamente el tipo de
operación normal que la auditoría encontró irregistrable en la primera versión del modelo.

**Cómo se mueve `used_quantity`:** con `UPDATE company_capacities SET used_quantity = used_quantity +
? WHERE company_id = ? AND capacity_unit = ?`, atómico en el motor. **Nunca** leer-modificar-guardar
desde Java. Y ese `UPDATE` está sujeto a `MUTACIONES_SQL_ACOTADAS_POR_EMPRESA`: el `WHERE` lleva
`company_id`, que ya lo lleva.

`ix_company_capacities_stale (recalculated_at)`: mismo indicador de salud que en
`company_entitlements`.

---

# FASE 6 · DINERO

> **Regla de la capa, y no admite excepción:** ninguna fila de estas tablas se edita ni se borra
> después de creada. Un error se arregla con un documento nuevo que lo compensa, y los dos quedan
> visibles. Se separan tres cosas que suelen confundirse: **devengar** (el servicio se prestó),
> **facturar** (se emitió el documento) y **cobrar** (entró la plata).
>
> La convención de signos está declarada **una sola vez**, en `suscripciones-modelo.md` §3, y no se
> repite aquí.

## 19 · `billing_document_sequences`

**Slice:** `subscriptionbilling` · **`version`:** NO (`E6_YA_PROTEGIDO`) · **`enabled`:** NO ·
**sin `company_id`** (contador global de plataforma)

**No es solo el consecutivo de las cuentas de cobro: es el contador de la numeración interna de
TODOS los documentos de plataforma**, y `prefix` es la serie que distingue uno de otro. Existe como
tabla propia porque **un número consecutivo no se puede sacar de un «máximo más uno»**: dos procesos
simultáneos leerían el mismo máximo y darían el mismo número a dos documentos distintos.

> **`quote` también la consume (issue #390), y esto no es un defecto, es la decisión correcta.** El
> consecutivo de cotizaciones (`prefix = 'COT-<año>'`, p. ej. `COT-2026`) reutiliza esta tabla en vez
> de duplicar el mecanismo en una tabla propia: duplicarlo habría significado tener dos
> implementaciones de la única cosa que esta ficha declara que no se puede duplicar —un contador
> serializado sin carrera y sin huecos— y que un día divergieran. `quote` la escribe por **SQL
> nativo**, sin importar ni una clase de `subscriptionbilling`; el acoplamiento entre slices es cero,
> el acoplamiento con la tabla es deliberado. Cabe sin cambiar el esquema: `prefix` es `VARCHAR(10)`
> y `COT-2026` son 8 caracteres, y `uq_billing_document_sequences_prefix` es exactamente la clave que
> el `INSERT … ON DUPLICATE KEY UPDATE` de `quote` necesita.
>
> **Consecuencia que conviene tener presente:** el listado de secuencias del propio slice
> `subscriptionbilling` (`ListBillingDocumentSequencesUseCase`) va a mostrar filas `COT-` junto a las
> `DC-`. No es basura ni un error de datos — bórralas y `quote` vuelve a empezar la numeración desde
> el 1 la próxima vez que cotice —, es sencillamente que la tabla ya no es privada de un solo slice.

| Columna | Tipo | Nulabilidad | Default | Notas |
|---|---|---|---|---|
| `id` | `BIGINT` | `NOT NULL` | `AUTO_INCREMENT` | |
| `prefix` | `VARCHAR(10)` | `NOT NULL` | — | La serie: `DC` cuentas de cobro, `NC` notas crédito, `ND` notas débito, `COT-<año>` cotizaciones (p. ej. `COT-2026`, consumida por `quote`) — y lo que venga. 10 caracteres sobran y no se paga nada por ellos |
| `next_value` | `BIGINT` | `NOT NULL` | `1` | El siguiente número libre. **Se lee y se incrementa en la misma operación** |
| `created_date` | `DATETIME` | `NOT NULL` | `CURRENT_TIMESTAMP` | |

| Nombre | Tipo | Definición |
|---|---|---|
| `PRIMARY` | PK | `(id)` |
| `uq_billing_document_sequences_prefix` | UNIQUE | `(prefix)` |
| `chk_billing_document_sequences_next_value` | CHECK | `next_value >= 1` |

**Cómo se usa — dos formas legales, las dos atómicas, y esto es normativo para `backend-feature`:**

```sql
SELECT next_value FROM billing_document_sequences WHERE prefix = ? FOR UPDATE;
UPDATE billing_document_sequences SET next_value = next_value + 1 WHERE prefix = ?;
```

Las dos sentencias **dentro de la misma transacción de negocio**, no en un `REQUIRES_NEW`. Es la
diferencia con `NumberingResolutionJpaEntity`, y es deliberada: el consecutivo fiscal de la DIAN no
puede tener huecos aunque la emisión falle, pero el consecutivo **interno** sí debe deshacerse si el
documento no llega a existir. La auditoría lo validó como correcto: *"sin carrera y sin huecos,
porque el incremento va dentro de la misma transacción y un fallo lo deshace"*.

Precedente del `SELECT … FOR UPDATE` en el árbol: `NumberingResolutionJpaEntity.lockActiveForUpdate`.

**La alternativa que usa `quote` (issue #390), igual de válida y sin `FOR UPDATE`:**

```sql
INSERT INTO billing_document_sequences (prefix, next_value)
VALUES (?, 2)
ON DUPLICATE KEY UPDATE next_value = next_value + 1;
```

Una sola sentencia que crea la fila si no existía (reservando el `1`) o la incrementa si ya existía,
sin lectura previa: InnoDB deja la fila bloqueada en exclusiva hasta el commit en los dos casos, así
que no hay ventana entre leer y escribir porque no hay lectura que preceda a la escritura. Pedir
`FOR UPDATE` aquí sería redundante -el candado ya se tiene- y sugeriría que la lectura es un paso
independiente, que es justo lo que no es. **Misma transacción de negocio, mismo motivo, mismo
comportamiento** que el patrón `SELECT … FOR UPDATE` de arriba: los dos son legales y la elección
entre uno y otro es del consumidor, no de esta ficha.

---

## 20 · `subscription_billing_documents`

**Slice:** `subscriptionbilling` · **`version`:** SÍ · **`enabled`:** NO ·
**`company_id` NOT NULL**

La cuenta de cobro y su factura. **La factura electrónica de la suscripción no se emite con este
software**: se emite fuera y aquí se guarda su referencia.

> **La distinción que no se puede confundir.** El motor de facturación electrónica DIAN sigue siendo
> parte del producto: es lo que usan las clínicas para facturarle a los dueños de mascotas, con su
> propia resolución y su propia numeración (`electronic_documents`, `numbering_resolutions`). Lo
> único que queda fuera es la factura que **tú** le emites a la clínica. **Dos emisores, dos
> numeraciones, dos tablas.** El día que alguien las mezcle, la contabilidad de tus clientes y la
> tuya quedan enredadas.
>
> Nomenclatura fijada: `DC-` documento de cobro que genera VetSoftware · `FE-` factura fiscal del
> sistema externo · `NC-` nota crédito externa. **El número `DC` debe viajar impreso en la factura
> externa**: es lo que permite emparejarlas después sin adivinar.

| Columna | Tipo | Nulabilidad | Default | Notas |
|---|---|---|---|---|
| `id` | `BIGINT` | `NOT NULL` | `AUTO_INCREMENT` | |
| `document_number` | `VARCHAR(30)` | `NOT NULL` | — | El número interno, generado aquí. Existe **desde que se calcula**, antes de que haya factura |
| `company_id` | `BIGINT` | `NOT NULL` | — | |
| `subscription_id` | `BIGINT` | `NOT NULL` | — | |
| `document_kind` | `VARCHAR(20)` | `NOT NULL` | — | `INVOICE` · `CREDIT_NOTE` · `DEBIT_NOTE` |
| `billing_reason` | `VARCHAR(20)` | `NOT NULL` | — | **[AÑADIDO]** `RECURRING_CYCLE` · `PRORATION` · `ONE_TIME` · `ADJUSTMENT`. Ver justificación abajo |
| `period_start` | `DATE` | `NOT NULL` | — | |
| `period_end` | `DATE` | `NOT NULL` | — | |
| `issue_status` | `VARCHAR(20)` | `NOT NULL` | `'DRAFT'` | `DRAFT` calculado · `AWAITING_EXTERNAL` pendiente de emitirse fuera · `EXTERNAL_REGISTERED` emitido y con la referencia capturada · `VOIDED`. **Los atascados en `AWAITING_EXTERNAL` son tu lista de trabajo pendiente cada mes** |
| `external_invoice_number` | `VARCHAR(60)` | `NULL` | — | Lo que el cliente ve en su factura y por lo que va a preguntar |
| `external_cufe` | `VARCHAR(100)` | `NULL` | — | El CUFE/CUDE que devolvió la DIAN |
| `external_issued_at` | `DATE` | `NULL` | — | **La fecha fiscal real**, que puede ser distinta de la del documento interno |
| `external_provider` | `VARCHAR(40)` | `NULL` | — | Qué sistema la emitió |
| `external_registered_at` | `DATETIME(6)` | `NULL` | — | Cuándo se capturó aquí la referencia |
| `external_registered_by_system_user_id` | `BIGINT` | `NULL` | — | Quién. El rastro del paso manual |
| `corrects_document_id` | `BIGINT` | `NULL` | — | Encadena una nota crédito con el documento que corrige |
| `due_date` | `DATE` | `NULL` | — | **Se cuenta desde `external_issued_at`**, la fecha fiscal, no desde que se calculó el cobro. Contarlo desde el cálculo interno suspendería cuentas por un retraso administrativo tuyo, no del cliente |
| `subtotal_amount` | `DECIMAL(19,2)` | `NOT NULL` | `0.00` | |
| `tax_amount` | `DECIMAL(19,2)` | `NOT NULL` | `0.00` | |
| `total_amount` | `DECIMAL(19,2)` | `NOT NULL` | `0.00` | |
| `settled_amount` | `DECIMAL(19,2)` | `NOT NULL` | `0.00` | **Lo saldado: pagado o acreditado.** No se llama «pagado» a propósito, porque una nota crédito también reduce lo que se debe sin que entre un peso |
| `balance_amount` | `DECIMAL(19,2)` | *generada* `VIRTUAL` | — | Ver abajo |
| `recurring_cycle_marker` | `BIGINT` | *generada* `STORED` | — | Ver abajo |
| `overdue_marker` | `BIGINT` | *generada* `STORED` | — | Ver abajo |
| `created_date` | `DATETIME` | `NOT NULL` | `CURRENT_TIMESTAMP` | |
| `version` | `BIGINT` | `NOT NULL` | `0` | |

### `balance_amount` — la columna calculada que ningún camino de código puede desincronizar

```sql
balance_amount DECIMAL(19,2)
GENERATED ALWAYS AS (total_amount - settled_amount) VIRTUAL
```

**No se escribe nunca.** La calcula la base. Es la columna que decide si una cuenta entra en mora,
así que **un camino de código capaz de desincronizarla es un camino capaz de suspender a quien ya
pagó**. Sumarla da tu cartera, al peso.

**Por qué `VIRTUAL` y no `STORED`** —y es la única generada del modelo que lo es—:

1. No lleva índice encima. La consulta de mora usa `overdue_marker`, no `balance_amount`.
2. `VIRTUAL` no ocupa espacio y no se recalcula en disco en cada `UPDATE` de `settled_amount`, que es
   la columna que más se mueve de la tabla.
3. Añadir una columna generada `VIRTUAL` es *in place*, no reconstruye la tabla y **permite DML
   concurrente**; la `STORED` reconstruye la tabla y **no** lo permite
   (<https://dev.mysql.com/doc/refman/8.4/en/innodb-online-ddl-operations.html>). Si mañana hay que
   cambiar la fórmula, `VIRTUAL` se cambia sin ventana de bloqueo.

**Mapeo JPA obligatorio** (si no, `ddl-auto: validate` o el `INSERT` fallan):

```java
@Column(name = "balance_amount", insertable = false, updatable = false)
@org.hibernate.annotations.Generated(event = { EventType.INSERT, EventType.UPDATE })
private BigDecimal balanceAmount;
```

### `recurring_cycle_marker` — la barandilla contra la doble facturación, por **periodo exacto**

**Este es un bloqueante corregido en la auditoría. No se relaja.**

La primera versión agrupaba **por mes**, y con eso la factura anual emitida a mitad de agosto chocaba
con la mensual del día 1: **un cliente que quisiera pasarse al plan anual —el que más caja te trae—
no podía**. La corrección agrupa por **periodo exacto**: sigue impidiendo regenerar dos veces la
factura del mismo periodo, que es la doble facturación real, y deja de bloquear dos periodos
distintos que caen en el mismo mes.

```sql
recurring_cycle_marker BIGINT
GENERATED ALWAYS AS (
    CASE
        WHEN document_kind = 'INVOICE'
         AND billing_reason = 'RECURRING_CYCLE'
         AND issue_status <> 'VOIDED'
        THEN subscription_id
        ELSE NULL
    END
) STORED
```

Con:

```sql
CONSTRAINT uq_sbd_recurring_cycle UNIQUE (recurring_cycle_marker, period_start, period_end)
```

**Léelo así:** el marcador es el patrón exacto de la casa —vale el `subscription_id` cuando la fila
está "en alcance" y `NULL` cuando no, igual que `active_open_branch_id` en
`210_scope_open_accounts_by_branch.xml:13-16` y `active_slot_employee_id` en
`226_add_unique_active_appointment_slot.xml:63-70`—, y **el periodo va en el índice como columnas
reales**, no dentro del marcador. Eso es lo que consigue "por periodo exacto": la unicidad es sobre
la terna `(contrato vigente, inicio exacto, fin exacto)`.

**Tres decisiones de esta expresión, explicadas porque cada una se puede equivocar:**

- **`billing_reason = 'RECURRING_CYCLE'`** es la columna `[AÑADIDA]`. Sin ella, la barandilla o cubre
  todos los `INVOICE` —y entonces una factura de prorrateo emitida con el mismo periodo exacto que la
  de ciclo se rechaza, bloqueando un cobro legítimo—, o no cubre nada. El documento de diseño exige
  la barandilla pero no da la columna que distingue la factura de ciclo de una puntual. **Es una
  adición estructural, no un adorno.**
- **`issue_status <> 'VOIDED'`** deja fuera las anuladas, para poder reemitir el mismo periodo tras
  anular. Sin esto, un error en la factura de septiembre haría el periodo irrecuperable para siempre.
- **`BIGINT`, no `VARCHAR`.** Una alternativa era concatenar las dos fechas en una cadena
  (`CONCAT(period_start,'_',period_end)`). Se descarta: introduce una columna de texto con colación
  en una tabla que no tiene ninguna, es más ancha (21 bytes frente a 8) y no aporta nada que el
  índice de tres columnas no dé.

### `overdue_marker` — el índice sobre las facturas realmente vencidas

```sql
overdue_marker BIGINT
GENERATED ALWAYS AS (
    CASE
        WHEN issue_status = 'EXTERNAL_REGISTERED'
         AND document_kind = 'INVOICE'
         AND total_amount > settled_amount
         AND due_date IS NOT NULL
        THEN company_id
        ELSE NULL
    END
) STORED
```

Con `ix_sbd_overdue (overdue_marker, due_date)`.

**Lo que NO puede llevar dentro, y es una limitación del motor, no una omisión:** la comparación
`due_date < CURRENT_DATE`. Las expresiones de columna generada **tienen que ser deterministas**
(<https://dev.mysql.com/doc/refman/8.4/en/create-table-generated-columns.html>), y `CURRENT_DATE` no
lo es. El marcador codifica «registrada y no saldada»; **el «vencida» lo pone la consulta**:

```sql
SELECT ... FROM subscription_billing_documents
 WHERE overdue_marker = :companyId AND due_date < CURRENT_DATE;   -- cartera de una clínica
SELECT ... FROM subscription_billing_documents
 WHERE overdue_marker IS NOT NULL AND due_date < CURRENT_DATE;    -- barrido de mora de plataforma
```

**Corrección honesta sobre «índice disperso».** El documento de diseño lo llama así y la auditoría lo
señaló como una de las mejores decisiones del modelo. Es cierto que el **escaneo** solo recorre las
filas relevantes. Pero conviene decirlo con precisión para que nadie se lleve una sorpresa midiendo:
**InnoDB indexa también los `NULL`**, así que el índice tiene una entrada por fila igual que
cualquier otro y **no es más pequeño en disco** que `(company_id, issue_status, due_date)`. Lo que
gana no es tamaño: es que el rango que el optimizador recorre queda confinado a las filas realmente
impagadas, y que una factura saldada **sale sola** del rango en cuanto sube `settled_amount`. Eso
sigue siendo la decisión correcta. Lo que no es, es un índice parcial de PostgreSQL.

### Constraints

| Nombre | Tipo | Definición |
|---|---|---|
| `PRIMARY` | PK | `(id)` |
| `uq_sbd_number` | UNIQUE | `(document_number)` |
| `uq_sbd_company_id` | UNIQUE | `(company_id, id)` — clave auxiliar (**pares P2, P3, P5**) |
| `uq_sbd_recurring_cycle` | UNIQUE | `(recurring_cycle_marker, period_start, period_end)` |
| `uq_sbd_external` | UNIQUE | `(external_provider, external_invoice_number)` — la misma factura externa no se registra dos veces |
| `fk_sbd_company` | FK | `(company_id) → companies(id)` RESTRICT/RESTRICT |
| `fk_sbd_subscription` | FK | **compuesta** `(company_id, subscription_id) → subscriptions(company_id, id)` RESTRICT/RESTRICT |
| `fk_sbd_corrects` | FK | **compuesta autorreferencial** `(company_id, corrects_document_id) → subscription_billing_documents(company_id, id)` RESTRICT/RESTRICT (**par P5**) |
| `fk_sbd_registered_by` | FK | `(external_registered_by_system_user_id) → system_users(id)` RESTRICT/RESTRICT |
| `chk_sbd_kind` | CHECK | `document_kind IN ('INVOICE','CREDIT_NOTE','DEBIT_NOTE')` |
| `chk_sbd_billing_reason` | CHECK | `billing_reason IN ('RECURRING_CYCLE','PRORATION','ONE_TIME','ADJUSTMENT')` |
| `chk_sbd_issue_status` | CHECK | `issue_status IN ('DRAFT','AWAITING_EXTERNAL','EXTERNAL_REGISTERED','VOIDED')` |
| `chk_sbd_period` | CHECK | `period_end >= period_start` |
| `chk_sbd_amounts_positive` | CHECK | `subtotal_amount >= 0 AND tax_amount >= 0 AND total_amount >= 0 AND settled_amount >= 0` |
| `chk_sbd_total` | CHECK | `total_amount = subtotal_amount + tax_amount` |
| `chk_sbd_settled_cap` | CHECK | `settled_amount <= total_amount` |
| `chk_sbd_external_registered` | CHECK | `issue_status <> 'EXTERNAL_REGISTERED' OR (external_invoice_number IS NOT NULL AND external_issued_at IS NOT NULL AND external_provider IS NOT NULL)` |
| `chk_sbd_due_date` | CHECK | `due_date IS NULL OR external_issued_at IS NULL OR due_date >= external_issued_at` |
| `chk_sbd_corrects_kind` | CHECK | `corrects_document_id IS NULL OR document_kind IN ('CREDIT_NOTE','DEBIT_NOTE')` |

Tres de esas constraints merecen que se las lea despacio, porque cada una cierra un modo de fallo
concreto:

- **`chk_sbd_amounts_positive`** es la convención de signos hecha esquema: *el documento siempre lleva
  importes positivos y el signo lo da su tipo*. Es la mitad de la corrección que hizo que una
  devolución cupiera en el modelo.
- **`chk_sbd_settled_cap`** impide saldar más de lo que se debe, que es como la cartera acaba cuadrando
  con plata que no existe.
- **`chk_sbd_external_registered`** es la invariante del anexo técnico *"un documento de cobro marcado
  como facturado tiene sí o sí número de factura externo y fecha de emisión"*, y es una de las pocas
  de esa lista que la base **sí** puede imponer. Sin ella se da por facturado algo que no lo está, y
  ese cobro desaparece del radar para siempre.

### Índices

| Nombre | Columnas | Qué consulta sirve |
|---|---|---|
| `uq_sbd_company_id` | `(company_id, id)` | Clave auxiliar + listado por empresa |
| `ix_sbd_subscription_period` | `(company_id, subscription_id, period_start)` | El historial de cobros de un contrato. **Prefijo de `fk_sbd_subscription`: crear antes que la FK** |
| `ix_sbd_overdue` | `(overdue_marker, due_date)` | Cartera y mora |
| `ix_sbd_awaiting` | `(issue_status, created_date)` | **La lista de trabajo mensual**: `WHERE issue_status = 'AWAITING_EXTERNAL' ORDER BY created_date`. Barrido de plataforma, sin tenant delante — declarado |

---

## 21 · `subscription_billing_document_taxes`

**Slice:** `subscriptionbilling` · **`version`:** NO (`E1_APPEND_ONLY`) · **`enabled`:** NO ·
**`company_id` NOT NULL**

**El único sitio donde el IVA está calculado una sola vez y de la forma correcta**, sobre la base
agregada de todas las líneas que comparten tarifa.

> **Bloqueante corregido, y cómo lo respeta este DDL.** En la primera versión el cargo guardaba su
> impuesto por línea y el documento otro agregado, sin nada que dijera cuál manda; difieren en un
> peso por documento, que es justo lo que descuadra la declaración bimestral. **La corrección:
> `subscription_charges` guarda su base y su tarifa pero NO su importe de impuesto** (ver ficha 22 —
> no existe la columna `tax_amount` en `subscription_charges`), y el importe del IVA vive **solo
> aquí**, calculado sobre la base agregada. Una sola verdad, un solo sitio.

Y es lo que permite emitir una factura con **tarifas mixtas** —unos módulos gravados y otros
excluidos— que con un solo importe de impuesto por documento sería literalmente inexpresable.

| Columna | Tipo | Nulabilidad | Default | Notas |
|---|---|---|---|---|
| `id` | `BIGINT` | `NOT NULL` | `AUTO_INCREMENT` | |
| `company_id` | `BIGINT` | `NOT NULL` | — | |
| `billing_document_id` | `BIGINT` | `NOT NULL` | — | |
| `tax_treatment` | `VARCHAR(20)` | `NOT NULL` | — | `TAXED` · `EXEMPT` · `EXCLUDED`. **Excluido y exento no son lo mismo y no se pueden colapsar en «tarifa cero»** |
| `tax_rate` | `DECIMAL(5,2)` | `NOT NULL` | — | |
| `taxable_base` | `DECIMAL(19,2)` | `NOT NULL` | — | La base sumada de las líneas con esa tarifa. **Esta suma es la que se declara** |
| `tax_amount` | `DECIMAL(19,2)` | `NOT NULL` | — | El impuesto calculado sobre ella |
| `created_date` | `DATETIME` | `NOT NULL` | `CURRENT_TIMESTAMP` | |

| Nombre | Tipo | Definición |
|---|---|---|
| `PRIMARY` | PK | `(id)` |
| `uq_sbdt_document_rate` | UNIQUE | `(billing_document_id, tax_treatment, tax_rate)` |
| `fk_sbdt_company` | FK | `(company_id) → companies(id)` RESTRICT/RESTRICT |
| `fk_sbdt_document` | FK | **compuesta** `(company_id, billing_document_id) → subscription_billing_documents(company_id, id)` RESTRICT/RESTRICT |
| `chk_sbdt_tax_treatment` | CHECK | `tax_treatment IN ('TAXED','EXEMPT','EXCLUDED')` |
| `chk_sbdt_rate` | CHECK | `tax_rate >= 0 AND tax_rate <= 100` |
| `chk_sbdt_amounts_positive` | CHECK | `taxable_base >= 0 AND tax_amount >= 0` |
| `chk_sbdt_coherence` | CHECK | `(tax_treatment = 'TAXED' AND tax_rate > 0) OR (tax_treatment <> 'TAXED' AND tax_rate = 0 AND tax_amount = 0)` |

**`uq_sbdt_document_rate` es la constraint que hace cumplir «una sola vez».** Un documento no puede
tener dos bloques con el mismo tratamiento y la misma tarifa: si aparecieran, la suma declarada
sería el doble. Es la traducción exacta del bloqueante.

**`chk_sbdt_amounts_positive` es la otra mitad de la corrección de signos:** la fila de impuesto exige
base positiva, y eso es coherente **porque el documento también es positivo** y el sentido lo da
`document_kind`. Las tres reglas ya no se contradicen.

`ix_sbdt_document (company_id, billing_document_id)` lo crea la FK compuesta; no hace falta declarar
ninguno más.

---

## 22 · `subscription_charges`

**Slice:** `subscriptionbilling` · **`version`:** NO (`E6_YA_PROTEGIDO`) · **`enabled`:** NO ·
**`company_id` NOT NULL**

Lo que se devengó. Cada fila es **un servicio prestado en un periodo**, con o sin factura todavía.
Existe separada de la factura porque una factura agrupa varios cargos y porque **el servicio se
devenga aunque la emisión falle**.

| Columna | Tipo | Nulabilidad | Default | Notas |
|---|---|---|---|---|
| `id` | `BIGINT` | `NOT NULL` | `AUTO_INCREMENT` | |
| `company_id` | `BIGINT` | `NOT NULL` | — | |
| `subscription_id` | `BIGINT` | `NOT NULL` | — | |
| `subscription_item_id` | `BIGINT` | `NULL` | — | Qué línea del contrato lo devengó. Nulo en cobros únicos sin línea |
| `charge_type` | `VARCHAR(20)` | `NOT NULL` | — | `RECURRING` la cuota · `PRORATION` el proporcional de un cambio a mitad de ciclo · `ONE_TIME` implantación o migración · `CREDIT` lo que se le devuelve · `DISCOUNT` |
| `description` | `VARCHAR(255)` | `NOT NULL` | — | **El texto que sale impreso en la factura. Se guarda, no se genera al vuelo**: la factura de hace dos años debe leerse igual hoy |
| `service_period_start` | `DATE` | `NOT NULL` | — | |
| `service_period_end` | `DATE` | `NOT NULL` | — | **Permite cerrar un mes contable correctamente y separar lo devengado de lo cobrado** |
| `quantity` | `DECIMAL(12,3)` | `NOT NULL` | `1.000` | |
| `unit_amount` | `DECIMAL(19,2)` | `NOT NULL` | — | |
| `subtotal_amount` | `DECIMAL(19,2)` | `NOT NULL` | — | **CON SIGNO.** Ver `suscripciones-modelo.md` §3 |
| `tax_rate` | `DECIMAL(5,2)` | `NOT NULL` | `0.00` | La tarifa que le aplica |
| `tax_treatment` | `VARCHAR(20)` | `NOT NULL` | — | **[AÑADIDO]** Necesario para agrupar en `subscription_billing_document_taxes`. Sin él, `tax_rate = 0` no distingue exento de excluido y el desglose fiscal es inconstruible |
| `proration_days` | `INT` | `NULL` | — | Cuántos días se cobraron |
| `period_days` | `INT` | `NULL` | — | Sobre cuántos días del periodo. **Sin estos dos números un prorrateo no se puede reconstruir**: se ve el importe pero no de dónde salió |
| `status` | `VARCHAR(20)` | `NOT NULL` | `'PENDING'` | `PENDING` aún sin facturar · `INVOICED` ya salió en una factura · `VOIDED` anulado por una nota crédito |
| `amendment_id` | `BIGINT` | `NULL` | — | Si el cargo nació de un cambio de contrato, cuál |
| `billing_document_id` | `BIGINT` | `NULL` | — | En qué factura acabó |
| `voids_charge_id` | `BIGINT` | `NULL` | — | **[AÑADIDO]** A qué cargo compensa este cargo de anulación. Sin esta columna, «los dos quedan y suman cero» es una afirmación que ninguna consulta puede verificar |
| `created_date` | `DATETIME` | `NOT NULL` | `CURRENT_TIMESTAMP` | |

> **NO existe la columna `tax_amount` en esta tabla, y es deliberado.** El cargo guarda su **base** y
> su **tarifa**; el importe del IVA se calcula **una sola vez**, sobre la base agregada del documento,
> y vive en `subscription_billing_document_taxes`. Guardarlo también aquí creaba dos verdades que
> difieren en un peso, que es justo el descuadre que la regla de cálculo existe para evitar. **Si
> alguien añade `tax_amount` aquí, ha reabierto un bloqueante cerrado.**

### Constraints

| Nombre | Tipo | Definición |
|---|---|---|
| `PRIMARY` | PK | `(id)` |
| `uq_subscription_charges_company_id` | UNIQUE | `(company_id, id)` — clave auxiliar (autorreferencia `voids_charge_id`) |
| `fk_subscription_charges_company` | FK | `(company_id) → companies(id)` RESTRICT/RESTRICT |
| `fk_subscription_charges_subscription` | FK | **compuesta** `(company_id, subscription_id) → subscriptions(company_id, id)` RESTRICT/RESTRICT |
| `fk_subscription_charges_item` | FK | **compuesta** `(company_id, subscription_item_id) → subscription_items(company_id, id)` RESTRICT/RESTRICT |
| `fk_subscription_charges_amendment` | FK | **compuesta** `(company_id, amendment_id) → subscription_amendments(company_id, id)` RESTRICT/RESTRICT |
| `fk_subscription_charges_document` | FK | **compuesta** `(company_id, billing_document_id) → subscription_billing_documents(company_id, id)` RESTRICT/RESTRICT (**par P2**) |
| `fk_subscription_charges_voids` | FK | **compuesta autorreferencial** `(company_id, voids_charge_id) → subscription_charges(company_id, id)` RESTRICT/RESTRICT |
| `chk_subscription_charges_type` | CHECK | `charge_type IN ('RECURRING','PRORATION','ONE_TIME','CREDIT','DISCOUNT')` |
| `chk_subscription_charges_status` | CHECK | `status IN ('PENDING','INVOICED','VOIDED')` |
| `chk_subscription_charges_tax_treatment` | CHECK | `tax_treatment IN ('TAXED','EXEMPT','EXCLUDED')` |
| `chk_subscription_charges_period` | CHECK | `service_period_end >= service_period_start` |
| `chk_subscription_charges_quantity` | CHECK | `quantity > 0` |
| `chk_subscription_charges_unit_amount` | CHECK | `unit_amount >= 0` |
| `chk_subscription_charges_tax_rate` | CHECK | `tax_rate >= 0 AND tax_rate <= 100` |
| `chk_subscription_charges_sign` | CHECK | `(charge_type IN ('CREDIT','DISCOUNT') AND voids_charge_id IS NULL AND subtotal_amount <= 0) OR (charge_type IN ('RECURRING','ONE_TIME') AND subtotal_amount >= 0) OR charge_type = 'PRORATION' OR (charge_type = 'CREDIT' AND voids_charge_id IS NOT NULL)` |
| `chk_subscription_charges_proration` | CHECK | `(proration_days IS NULL AND period_days IS NULL) OR (proration_days IS NOT NULL AND period_days IS NOT NULL AND period_days > 0 AND proration_days >= 0 AND proration_days <= period_days)` |
| `chk_subscription_charges_invoiced` | CHECK | `status <> 'INVOICED' OR billing_document_id IS NOT NULL` |
| `chk_subscription_charges_voids` | CHECK | `voids_charge_id IS NULL OR charge_type = 'CREDIT'` |

> **Corrección post-implementación (issue #402): anular un `CREDIT` ya negativo era
> inexpresable.** La primera redacción de `chk_subscription_charges_sign` exigía `<= 0` para
> **todo** `CREDIT`, sin excepción. Pero `chk_subscription_charges_voids` exige que el cargo que
> anula a otro sea siempre `CREDIT`, y anular un `CREDIT` que ya era negativo —una nota de crédito
> mal emitida por −50.000, que es justo el momento en que el cliente está mirando— exige un
> compensador **positivo** (+50.000, para que los dos sumen cero). Las dos restricciones no podían
> cumplirse a la vez: mismo género de defecto que "una devolución no cabía en el esquema" de
> `suscripciones-modelo.md` §3. Se corrige eximiendo del signo fijo únicamente al `CREDIT` que
> anula a otro (`voids_charge_id IS NOT NULL`); un `CREDIT` directo —el caso normal, que no anula
> nada— sigue obligado a `<= 0` exactamente como antes. Detalle completo y por qué no se resolvió
> haciendo que el `CHECK` mirara el signo del cargo anulado (un `CHECK` no puede leer otra fila) en
> `suscripciones-modelo.md` §3.4.

**`chk_subscription_charges_sign` es la convención de signos hecha esquema en el otro lado.**
`PRORATION` queda libre de signo a propósito: una ampliación a mitad de ciclo cobra (positivo) y una
reducción a mitad de ciclo acredita (negativo), y las dos son operaciones normales. `CREDIT` y
`DISCOUNT` **tienen** que ser negativos o cero cuando **no** anulan nada; `RECURRING` y `ONE_TIME`,
positivos o cero. Un `CREDIT` que sí anula a otro cargo (`voids_charge_id IS NOT NULL`) queda libre
de signo, porque su signo lo decide el cargo que compensa, no su tipo.

**`chk_subscription_charges_proration` es la que impide el prorrateo irreconstruible:** o están los
dos números o no está ninguno, y los días cobrados nunca superan los días del periodo. Sin ella,
explicarle un prorrateo a un cliente que reclama pasa a ser un ejercicio de arqueología.

### Índices

| Nombre | Columnas | Qué consulta sirve |
|---|---|---|
| `uq_subscription_charges_company_id` | `(company_id, id)` | Clave auxiliar |
| `ix_subscription_charges_pending` | `(company_id, subscription_id, status, service_period_start)` | **La consulta del proceso de facturación**: los cargos `PENDING` de un contrato para un periodo. **Prefijo de `fk_subscription_charges_subscription`: crear antes que la FK** |
| *(automático de `fk_subscription_charges_document`)* | `(company_id, billing_document_id)` | «¿Qué cargos agrupa esta factura?» — la conciliación cargos ↔ documento |

---

## 23 · `subscription_payments`

**Slice:** `subscriptionpayment` · **`version`:** SÍ · **`enabled`:** NO ·
**`company_id` NOT NULL**

La plata que entró, **independiente de a qué factura se aplique**. Se separan a propósito: un cliente
puede pagar tres facturas de un giro, o abonar la mitad de una.

| Columna | Tipo | Nulabilidad | Default | Notas |
|---|---|---|---|---|
| `id` | `BIGINT` | `NOT NULL` | `AUTO_INCREMENT` | |
| `company_id` | `BIGINT` | `NOT NULL` | — | |
| `amount` | `DECIMAL(19,2)` | `NOT NULL` | — | |
| `currency` | `CHAR(3)` | `NOT NULL` | `'COP'` | |
| `payment_method` | `VARCHAR(30)` | `NOT NULL` | — | `TRANSFER` · `CARD` · `PSE` · `CASH` · `OTHER` |
| `gateway` | `VARCHAR(40)` | `NULL` | — | La pasarela |
| `gateway_reference` | `VARCHAR(120)` | `NULL` | — | Su número de operación |
| `received_at` | `DATETIME(6)` | `NOT NULL` | — | **Cuándo entró de verdad**, que no siempre es cuándo se registró |
| `status` | `VARCHAR(20)` | `NOT NULL` | `'PENDING'` | `PENDING` · `CONFIRMED` · `FAILED` · `REFUNDED`. **Solo los confirmados cuentan como cobro** |
| `reconciled_at` | `DATETIME(6)` | `NULL` | — | Cuándo se cuadró contra el extracto bancario. **Lo no conciliado es lo que hay que revisar cada mes** |
| `client_request_id` | `VARCHAR(64)` | `NULL` | — | **[AÑADIDO]** El anexo técnico exige llave de idempotencia en *toda* petición que mueva dinero. `gateway_reference` cubre el reintento de la pasarela; esto cubre el doble clic del operador que registra un pago manual. Deduplicación acotada por empresa, ver la nota bajo la tabla de constraints |
| `created_date` | `DATETIME` | `NOT NULL` | `CURRENT_TIMESTAMP` | |
| `version` | `BIGINT` | `NOT NULL` | `0` | |

| Nombre | Tipo | Definición |
|---|---|---|
| `PRIMARY` | PK | `(id)` |
| `uq_subscription_payments_company_id` | UNIQUE | `(company_id, id)` — clave auxiliar (**par P4**) |
| `uq_subscription_payments_gateway` | UNIQUE | `(gateway, gateway_reference)` |
| `uq_subscription_payments_client_request` | UNIQUE | `(company_id, client_request_id)` |
| `fk_subscription_payments_company` | FK | `(company_id) → companies(id)` RESTRICT/RESTRICT |
| `chk_subscription_payments_method` | CHECK | `payment_method IN ('TRANSFER','CARD','PSE','CASH','OTHER')` |
| `chk_subscription_payments_status` | CHECK | `status IN ('PENDING','CONFIRMED','FAILED','REFUNDED')` |
| `chk_subscription_payments_amount` | CHECK | `amount > 0` |
| `chk_subscription_payments_currency` | CHECK | `CHAR_LENGTH(currency) = 3 AND currency = UPPER(currency)` |
| `chk_subscription_payments_gateway_pair` | CHECK | `(gateway IS NULL AND gateway_reference IS NULL) OR (gateway IS NOT NULL AND gateway_reference IS NOT NULL)` |
| `chk_subscription_payments_reconciled` | CHECK | `reconciled_at IS NULL OR status = 'CONFIRMED'` |

**`uq_subscription_payments_gateway` es la barandilla del webhook**: el mismo aviso de la pasarela
recibido dos veces **no crea dos pagos**. Los `NULL` múltiples de MySQL hacen que los pagos manuales
—sin pasarela— no colisionen entre sí, que es justo lo que se quiere.

> **Corrección post-implementación: `uq_subscription_payments_client_request` se acota por
> empresa.** Hallazgo encontrado al verificar el patrón que corrigió el issue #396 en
> `billing_document_applications`: `RegisterSubscriptionPaymentService` deduplica con
> `SubscriptionPaymentRepository.findByCompanyIdAndClientRequestId`, ACOTADO POR EMPRESA, pero la
> primera redacción de esta ficha declaraba el índice como `UNIQUE (client_request_id)` a secas
> —global—. Mismo defecto y misma corrección que en `billing_document_applications` (ficha #24) y
> `subscription_amendments` (ficha #14): dos clínicas que generen la misma cadena chocarían contra
> una fila que la segunda no puede ver. Se corrige a `UNIQUE (company_id, client_request_id)`;
> `client_request_id` sigue nulable, así que las filas sin llave (origen pasarela, no cliente) no
> colisionan entre sí.

**`chk_subscription_payments_reconciled`** impide marcar como conciliado un pago que la pasarela nunca
confirmó, que es como aparece plata en la cartera sin haber entrado en el banco.

### Índices

| Nombre | Columnas | Qué consulta sirve |
|---|---|---|
| `uq_subscription_payments_company_id` | `(company_id, id)` | Clave auxiliar |
| `ix_subscription_payments_company_status` | `(company_id, status, received_at)` | Los pagos de una clínica, por estado, más recientes primero. **`company_id` delante** |

---

## 24 · `billing_document_applications`

**Slice:** `subscriptionpayment` · **`version`:** NO (`E1_APPEND_ONLY`) · **`enabled`:** NO ·
**`company_id` NOT NULL**

Qué salda qué.

> **Bloqueante corregido, y cómo lo respeta este DDL.** En la primera versión esta tabla **solo
> aceptaba pagos**. Como una nota crédito no es un pago, no había forma de descontarla: el saldo de la
> factura no bajaba nunca, el reloj de la mora seguía corriendo y **una clínica a la que le habías
> devuelto dinero acababa en solo lectura por una deuda que ya no existía** — y el sistema tenía
> razón según sus propios números, que es lo que hace este tipo de fallo tan difícil de discutir con
> un cliente enfadado.
>
> **La corrección, y está toda en el DDL de abajo:** `source_kind` acepta `PAYMENT` **y**
> `CREDIT_NOTE`; `payment_id` y `source_document_id` son mutuamente excluyentes y **la base lo
> comprueba** con `chk_bda_source_exclusive`; y una aplicación equivocada se revierte con otra que la
> contra-aplica, sin borrar nada.

> **Corrección post-implementación (issue #396): faltaba la llave de idempotencia.** El modelo ya
> exigía que toda petición que mueva dinero la lleve, y esta ficha no la tenía. Sin ella, una
> aplicación parcial repetida —doble clic, reintento del cliente HTTP— crea **dos** filas que R3 no
> detecta: R3 acota el total aplicado desde un origen, no cuántas veces se aplicó, así que dos
> aplicaciones de 50 desde un pago de 100 le parecen legítimas a la regla aunque el operador solo
> quiso aplicar 50 una vez. Se añade `client_request_id`, mismo patrón que
> `uq_quotes_client_request` y `uq_subscription_amendments_client_request`.

| Columna | Tipo | Nulabilidad | Default | Notas |
|---|---|---|---|---|
| `id` | `BIGINT` | `NOT NULL` | `AUTO_INCREMENT` | |
| `company_id` | `BIGINT` | `NOT NULL` | — | La empresa, que la clave arrastra para que no se pueda cruzar con otra clínica |
| `target_document_id` | `BIGINT` | `NOT NULL` | — | La factura cuyo saldo se reduce |
| `source_kind` | `VARCHAR(20)` | `NOT NULL` | — | `PAYMENT` un pago recibido · `CREDIT_NOTE` un saldo a favor |
| `payment_id` | `BIGINT` | `NULL` | — | El origen, si es un pago |
| `source_document_id` | `BIGINT` | `NULL` | — | El origen, si es una nota crédito |
| `applied_amount` | `DECIMAL(19,2)` | `NOT NULL` | — | **CON SIGNO**: positivo si aplica, negativo si contra-aplica |
| `reversal_of_id` | `BIGINT` | `NULL` | — | Cómo se deshace una aplicación equivocada **sin borrar nada** |
| `applied_at` | `DATETIME(6)` | `NOT NULL` | — | **[AÑADIDO]** Cuándo se aplicó. Sin ella, el orden de aplicación depende de `created_date` con precisión de segundo, y dos aplicaciones del mismo segundo son inordenables — que es exactamente el caso de un pago que salda tres facturas de un giro |
| `client_request_id` | `VARCHAR(64)` | `NULL` | — | **[AÑADIDO, issue #396]** Llave de idempotencia. Nulable a propósito: las aplicaciones que nacen de un proceso interno sin petición de cliente no tienen llave que guardar, y MySQL permite varios `NULL` en un índice único |
| `created_date` | `DATETIME` | `NOT NULL` | `CURRENT_TIMESTAMP` | |

### Constraints

| Nombre | Tipo | Definición |
|---|---|---|
| `PRIMARY` | PK | `(id)` |
| `uq_bda_company_id` | UNIQUE | `(company_id, id)` — clave auxiliar (autorreferencia `reversal_of_id`) |
| `uq_bda_reversal` | UNIQUE | `(reversal_of_id)` |
| `uq_bda_client_request` | UNIQUE | `(client_request_id)` |
| `fk_bda_company` | FK | `(company_id) → companies(id)` RESTRICT/RESTRICT |
| `fk_bda_target_document` | FK | **compuesta** `(company_id, target_document_id) → subscription_billing_documents(company_id, id)` RESTRICT/RESTRICT (**par P3**) |
| `fk_bda_source_document` | FK | **compuesta** `(company_id, source_document_id) → subscription_billing_documents(company_id, id)` RESTRICT/RESTRICT |
| `fk_bda_payment` | FK | **compuesta** `(company_id, payment_id) → subscription_payments(company_id, id)` RESTRICT/RESTRICT (**par P4**) |
| `fk_bda_reversal` | FK | **compuesta autorreferencial** `(company_id, reversal_of_id) → billing_document_applications(company_id, id)` RESTRICT/RESTRICT |
| `chk_bda_source_kind` | CHECK | `source_kind IN ('PAYMENT','CREDIT_NOTE')` |
| **`chk_bda_source_exclusive`** | CHECK | `(source_kind = 'PAYMENT' AND payment_id IS NOT NULL AND source_document_id IS NULL) OR (source_kind = 'CREDIT_NOTE' AND source_document_id IS NOT NULL AND payment_id IS NULL)` |
| `chk_bda_amount_not_zero` | CHECK | `applied_amount <> 0` |
| `chk_bda_reversal_sign` | CHECK | `(reversal_of_id IS NULL AND applied_amount > 0) OR (reversal_of_id IS NOT NULL AND applied_amount < 0)` |
| `chk_bda_not_self_target` | CHECK | `source_document_id IS NULL OR source_document_id <> target_document_id` |

**`chk_bda_source_exclusive` es el bloqueante que el encargo pide verificar explícitamente, y así lo
cierra:** hace las dos cosas a la vez —exige el origen correcto **y** prohíbe el otro— en una sola
constraint. `source_kind = 'PAYMENT'` con `source_document_id` relleno no entra en la base; `PAYMENT`
sin `payment_id` tampoco. No hay estado intermedio en el que el saldo se reduzca sin saber de dónde
salió el dinero.

**Que sea declarable no es gratuito:** `payment_id` y `source_document_id` son columnas de clave
foránea, y el manual dice que *"`CHECK` constraints are prohibited on columns used in foreign key
referential actions"*
(<https://dev.mysql.com/doc/refman/8.4/en/create-table-check-constraints.html>). Es legal **porque
todas las FK de este modelo usan `RESTRICT`, sin acciones referenciales**. Si alguien pone un
`ON DELETE SET NULL` en `fk_bda_payment`, esta constraint deja de poder existir y el bloqueante se
reabre.

**`chk_bda_reversal_sign`** convierte «se revierte con otra fila que la contra-aplica» en una regla
del motor: una fila de reversa **tiene** que ser negativa y una aplicación normal **tiene** que ser
positiva. Junto con `uq_bda_reversal` —una aplicación se revierte **una sola vez**—, la suma de
`applied_amount` de un origen es el neto real y no hay forma de inflarlo.

**Lo que sigue sin poder imponer la base y baja a las reglas de código:** que la suma de lo aplicado
desde un origen no supere ese origen (R3), y que `settled_amount` del documento destino sea siempre
la suma de sus aplicaciones confirmadas (R4). Las dos requieren agregar filas, y un `CHECK` no puede.

### Índices

| Nombre | Columnas | Qué consulta sirve |
|---|---|---|
| `uq_bda_company_id` | `(company_id, id)` | Clave auxiliar |
| `uq_bda_client_request` | `(client_request_id)` | La barandilla anti-doble-clic: el mismo `client_request_id` no puede insertarse dos veces |
| *(automático de `fk_bda_target_document`)* | `(company_id, target_document_id)` | «¿Qué salda esta factura?» — el cálculo de `settled_amount` |
| *(automático de `fk_bda_payment`)* | `(company_id, payment_id)` | «¿Cuánto se ha aplicado de este pago?» — la regla R3 |
| `ix_bda_company_applied` | `(company_id, applied_at)` | El listado cronológico de aplicaciones de una clínica |

---

## 25 · `dunning_events`

**Slice:** `dunning` · **`version`:** NO (`E1_APPEND_ONLY`) · **`enabled`:** NO ·
**`company_id` NOT NULL**

El expediente de cobranza. Sirve para dos cosas muy prácticas: **demostrar que se avisó antes de
restringir la cuenta**, y medir qué recordatorio funciona.

| Columna | Tipo | Nulabilidad | Default | Notas |
|---|---|---|---|---|
| `id` | `BIGINT` | `NOT NULL` | `AUTO_INCREMENT` | |
| `company_id` | `BIGINT` | `NOT NULL` | — | |
| `subscription_id` | `BIGINT` | `NOT NULL` | — | **[AÑADIDO]** El documento de diseño no lo lista, pero sin él un evento de cobranza no se puede atar al contrato que se va a restringir |
| `billing_document_id` | `BIGINT` | `NULL` | — | **[AÑADIDO]** Qué documento concreto disparó el aviso. Nulo en eventos de contrato (`READ_ONLY_APPLIED`, `REACTIVATED`) |
| `event_type` | `VARCHAR(30)` | `NOT NULL` | — | `REMINDER_SENT` · `GRACE_STARTED` · `READ_ONLY_APPLIED` · `REACTIVATED` · `WRITTEN_OFF` |
| `days_overdue` | `INT` | `NULL` | — | Cuántos días llevaba |
| `channel` | `VARCHAR(20)` | `NULL` | — | `EMAIL` · `SMS` · `WHATSAPP` · `PHONE` · `IN_APP` |
| `detail` | `VARCHAR(255)` | `NULL` | — | |
| `occurred_at` | `DATETIME(6)` | `NOT NULL` | — | |
| `created_date` | `DATETIME` | `NOT NULL` | `CURRENT_TIMESTAMP` | |

| Nombre | Tipo | Definición |
|---|---|---|
| `PRIMARY` | PK | `(id)` |
| `fk_dunning_events_company` | FK | `(company_id) → companies(id)` RESTRICT/RESTRICT |
| `fk_dunning_events_subscription` | FK | **compuesta** `(company_id, subscription_id) → subscriptions(company_id, id)` RESTRICT/RESTRICT |
| `fk_dunning_events_document` | FK | **compuesta** `(company_id, billing_document_id) → subscription_billing_documents(company_id, id)` RESTRICT/RESTRICT |
| `chk_dunning_events_type` | CHECK | `event_type IN ('REMINDER_SENT','GRACE_STARTED','READ_ONLY_APPLIED','REACTIVATED','WRITTEN_OFF')` |
| `chk_dunning_events_channel` | CHECK | `channel IS NULL OR channel IN ('EMAIL','SMS','WHATSAPP','PHONE','IN_APP')` |
| `chk_dunning_events_days` | CHECK | `days_overdue IS NULL OR days_overdue >= 0` |
| `chk_dunning_events_reminder_channel` | CHECK | `event_type <> 'REMINDER_SENT' OR channel IS NOT NULL` |

`chk_dunning_events_reminder_channel` es lo que hace que «se avisó» sea demostrable: un recordatorio
sin canal no prueba nada ante una reclamación.

| Índice | Columnas | Qué sirve |
|---|---|---|
| `ix_dunning_events_subscription` | `(company_id, subscription_id, occurred_at)` | El expediente en orden. **Prefijo de `fk_dunning_events_subscription`: crear antes que la FK** |

---

# FASE 7 · CONFIGURACIÓN

## 26 · `platform_billing_config`

**Slice:** `platformbillingconfig` · **`version`:** SÍ · **`enabled`:** **NO** (ver
`suscripciones-modelo.md` §2.2 fila 12) · **sin `company_id`**

Las políticas del negocio, en un sitio. **Una sola fila, garantizada por el esquema.** Cambiarlos es
editar un formulario, no desplegar una versión.

| Columna | Tipo | Nulabilidad | Default | Notas |
|---|---|---|---|---|
| `id` | `BIGINT` | `NOT NULL` | `AUTO_INCREMENT` | |
| `singleton` | `TINYINT` | `NOT NULL` | `1` | El truco para que la tabla no pueda tener más de una fila |
| `default_price_list_id` | `BIGINT` | `NULL` | — | Con qué tarifa se cotiza por defecto |
| `default_grace_days` | `INT` | `NOT NULL` | `5` | |
| `default_trial_days` | `INT` | `NOT NULL` | `0` | |
| `invoice_day_of_month` | `INT` | `NOT NULL` | `1` | Qué día del mes se emiten los cobros |
| `default_payment_term_days` | `INT` | `NOT NULL` | `0` | A cuántos días vence la factura desde su emisión. **Cero = pago inmediato** |
| `external_billing_provider` | `VARCHAR(40)` | `NULL` | — | Con qué sistema se emiten las facturas de suscripción. **Documenta dónde vive la otra mitad del circuito** |
| `created_date` | `DATETIME` | `NOT NULL` | `CURRENT_TIMESTAMP` | |
| `version` | `BIGINT` | `NOT NULL` | `0` | |

### La columna `singleton` — cómo se declara y por qué NO es generada

| Nombre | Tipo | Definición |
|---|---|---|
| `PRIMARY` | PK | `(id)` |
| `uq_platform_billing_config_singleton` | UNIQUE | `(singleton)` |
| `chk_platform_billing_config_singleton` | CHECK | `singleton = 1` |
| `fk_platform_billing_config_price_list` | FK | `(default_price_list_id) → price_lists(id)` RESTRICT/RESTRICT |
| `chk_platform_billing_config_grace` | CHECK | `default_grace_days >= 0` |
| `chk_platform_billing_config_trial` | CHECK | `default_trial_days >= 0` |
| `chk_platform_billing_config_invoice_day` | CHECK | `invoice_day_of_month BETWEEN 1 AND 28` |
| `chk_platform_billing_config_term` | CHECK | `default_payment_term_days >= 0` |

`UNIQUE (singleton)` + `CHECK (singleton = 1)` juntos garantizan **exactamente una fila**: el `CHECK`
impide cualquier otro valor y el `UNIQUE` impide un segundo `1`. Evita el clásico «hay dos
configuraciones y nadie sabe cuál manda».

**Precedente exacto del árbol:** `215_add_audit_chain_integrity.xml:125`,
`chk_audit_chain_head_singleton CHECK (id = 1)`. Ahí el `CHECK` puede ir sobre `id` porque esa tabla
declara `id TINYINT` **sin** `autoIncrement` (`215_add_audit_chain_integrity.xml:104-106`). **Aquí no
se puede repetir ese atajo**, porque `platform_billing_config.id` sí es `AUTO_INCREMENT` y el manual
lo prohíbe expresamente. De ahí la columna `singleton` aparte.

**Alternativa verificada y descartada.** `singleton TINYINT GENERATED ALWAYS AS (1) STORED` es legal
—el manual permite expresiones formadas solo por literales
(<https://dev.mysql.com/doc/refman/8.4/en/create-table-generated-columns.html>)— y sería
estrictamente más fuerte, porque ningún camino de código podría escribir otro valor. Se descarta
porque obliga a mapearla en JPA con `insertable = false, updatable = false` más `@Generated`, añade
una sutileza de mapeo en la única tabla trivial del modelo, y **no aporta ninguna garantía extra una
vez que el `CHECK` está puesto**. Si `db-migrations` la prefiere, es un cambio compatible; lo que
**no** debe hacerse es cambiarla sin probarla contra un Testcontainer `mysql:8.4`.

`invoice_day_of_month BETWEEN 1 AND 28`: los días 29, 30 y 31 no existen en todos los meses. Aceptar
un 31 significa que en febrero el proceso de facturación no corre, o corre un día que nadie decidió.

**`invoice_day_of_month` no ordena nada por sí solo:** el proceso automático busca por
`subscriptions.next_billing_date` (`ix_subscriptions_next_billing`), y este campo es solo el valor
por defecto con el que se calcula esa fecha al crear un contrato.

---

## Apéndice A · Resumen de columnas generadas

| Tabla | Columna | Modo | Índice encima | Por qué ese modo |
|---|---|---|---|---|
| `subscriptions` | `active_marker` | **STORED** | `uq_subscriptions_active_company` | Lleva índice único; patrón de la casa (195/206/210/226) |
| `subscription_items` | `current_item_marker` | **STORED** | `uq_subscription_items_current` | Ídem |
| `subscription_billing_documents` | `recurring_cycle_marker` | **STORED** | `uq_sbd_recurring_cycle` | Ídem |
| `subscription_billing_documents` | `overdue_marker` | **STORED** | `ix_sbd_overdue` | Ídem |
| `subscription_billing_documents` | `balance_amount` | **VIRTUAL** | ninguno | Sin índice, sin coste de escritura, y su `ALTER` futuro es *in place* con DML concurrente |

**Las cinco se declaran dentro del `CREATE TABLE` inicial.** Ninguna en un `ALTER` posterior.

## Apéndice B · Resumen de claves únicas auxiliares para FK compuestas

`uq_subscriptions_company_id` · `uq_subscription_amendments_company_id` ·
`uq_subscription_items_company_id` · `uq_sbd_company_id` · `uq_subscription_payments_company_id` ·
`uq_bda_company_id` · `uq_subscription_charges_company_id`

Son **siete**, no seis: a las seis de tabla padre de los pares P1–P6 se suma
`uq_subscription_charges_company_id`, que existe solo para la autorreferencia `voids_charge_id`.

## Apéndice C · Advertencia repetida para `db-migrations`

**El orden dentro de un changeset importa.** En toda tabla donde el índice de listado sea más largo
que las columnas de la FK compuesta y las contenga como prefijo por la izquierda:

1. `createTable`
2. bloque `<sql>` con las columnas generadas y las constraints `CHECK`
3. **`createIndex` de los índices de listado**
4. **después**, `addForeignKeyConstraint` de las FK compuestas

Si se invierten 3 y 4, MySQL crea su propio índice para la FK
(<https://dev.mysql.com/doc/refman/8.4/en/create-table-foreign-keys.html>: *"Such an index is created
on the referencing table automatically if it does not exist"*), quedan dos índices donde bastaba uno,
y `sys.schema_redundant_indexes` lo marcará en la próxima auditoría.

Tablas afectadas: `subscription_amendments`, `subscription_items`, `subscription_status_history`,
`subscription_billing_documents`, `subscription_charges`, `dunning_events`.
