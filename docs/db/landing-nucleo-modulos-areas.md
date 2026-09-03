# Landing «núcleo + módulos sueltos» — modelo de datos

Especificación para `db-migrations`. **No contiene changesets**: contiene la tabla, la columna, el
tipo, la constraint con su nombre, el índice con su orden de columnas, la `preCondition` y el
`<rollback>` que ese agente tiene que escribir, y el motivo de cada uno.

- **Fecha:** 2026-09-02
- **Motor objetivo:** MySQL 8.4 / InnoDB (RDS dev y prod, `db.t4g.small`, 20 GiB gp3, Single-AZ)
- **Siguiente changeset libre:** **396**. Comprobado sobre el árbol: `migrations/` tiene 377
  ficheros y `db.changelog-master.xml` 377 `<include>`; el número más alto declarado es
  `395_reseed_local_e2e_catalog_after_structural_minimum_rename.xml`.
- **Nada de esto se ha ejecutado contra ninguna base de datos.** Todo se decide leyendo
  changesets, entidades, adaptadores y las reglas ArchUnit. Ver «Medido / no medido» al final.
- **Slice:** `catalogitem` para `catalog_areas` y las columnas nuevas de `catalog_items`.
  **Ninguna de sus tablas lleva `company_id`** y ninguna asociación alcanza `CompanyJpaEntity`:
  eso es lo que mantiene dormidas las cuatro reglas duras de BE-COV sobre esta feature
  (`CatalogItemJpaEntity.java:29-32`). **No lo rompas**: `catalog_areas` no lleva `company_id`.

---

## 0. Resumen de la decisión

| Necesidad del rediseño | Dónde vive hoy | Dónde debe vivir | Coste del `ALTER` |
|---|---|---|---|
| Área funcional del módulo | `catalogo.content.ts:69-83` (front) | **Tabla `catalog_areas` + `catalog_items.area_code`** | tabla nueva + `ADD COLUMN` **INSTANT** |
| Rótulo de cabecera del área | `catalogo.content.ts:50-55` (front) | `catalog_areas.name` | — |
| Orden de las áreas | `catalogo.content.ts:58` (front) | `catalog_areas.sort_order` | — |
| Rótulo corto del módulo | no existe | `catalog_items.short_label` | `ADD COLUMN` **INSTANT** |
| Combinación recomendada | `plans.content.ts:175` (front) | `catalog_items.recommended` | `ADD COLUMN` **INSTANT** |
| Una sola recomendada | nada lo garantiza | columna generada `STORED` + `UNIQUE` | **reconstruye la tabla** (26 filas) |
| Orden del módulo dentro del área | `catalog_items.sort_order` | **ya está** — no se toca | ninguno |
| Núcleo obligatorio | **`CORE` ya existe** | ya está — no se crea nada | ninguno |
| Días de prueba | `catalog_items.default_trial_days` | ya se publica | ninguno |

**Cuatro columnas nuevas y una tabla nueva. Ningún tipo cambia, ningún dato se reescribe, ningún
`code` se mueve.** Todo lo demás que la landing necesita ya está en la base y ya se publica.

---

## 1. Estructura real de las cuatro tablas — verbatim del árbol

### 1.1 `catalog_items` (`229_create_catalog_items.xml:38-120`, más `333` y `394`)

| Columna | Tipo declarado | Nulabilidad | Origen |
|---|---|---|---|
| `id` | `BIGINT AUTO_INCREMENT` | PK | `229:40-42` |
| `code` | `VARCHAR(50) CHARACTER SET ascii COLLATE ascii_bin` | `NOT NULL`, `uq_catalog_items_code` | `229:43-45`, colación fijada en `332_align_identifier_collation.xml:65-67` |
| `name` | `VARCHAR(120)` | `NOT NULL` | `229:46` |
| `short_description` | `VARCHAR(255)` | `NULL` | `229:47` |
| `long_description` | `TEXT` | `NULL` | `229:48` |
| `item_type` | `VARCHAR(20)` | `NOT NULL` | `229:49` |
| `capacity_unit` | `VARCHAR(50)` | `NULL`, FK → `limit_dimensions(code)` | `229:50` ampliada por `333:98,135-149` |
| `structural_minimum` | `TINYINT NOT NULL DEFAULT 0` | `NOT NULL` | nace `is_core` en `229:51-53`, renombrada en `394:39-42` |
| `min_quantity` | `INT DEFAULT 1` | `NOT NULL` | `229:54-56` |
| `max_quantity` | `INT` | `NULL` | `229:57` |
| `sort_order` | `INT DEFAULT 0` | `NOT NULL` | `229:58-60` |
| `status` | `VARCHAR(20) DEFAULT 'DRAFT'` | `NOT NULL` | `229:61-63` |
| `trial_eligibility` | `VARCHAR(20)` | `NOT NULL` | `229:64-66` |
| `default_trial_days` | `INT` | `NULL` | `229:67` |
| `trial_outcome` | `VARCHAR(20)` | `NULL` | `229:68` |
| `service_nature` | `VARCHAR(30)` | `NOT NULL` | `229:69-71` |
| `created_date` | `DATETIME DEFAULT CURRENT_TIMESTAMP` | `NOT NULL` | `229:72-74` |
| `enabled` | `BOOLEAN` (→ `TINYINT`) `DEFAULT TRUE` | `NOT NULL` | `229:75-77` |
| `version` | `BIGINT DEFAULT 0` | `NOT NULL` | `229:78-80` |

**No hay ninguna otra columna.** No existe `area`, ni `group`, ni `short_label`, ni `recommended`,
ni `featured`, ni `badge`. Ningún changeset posterior hace `addColumn` sobre `catalog_items`
(barrido completo: `grep 'addColumn tableName="catalog_items"' migrations/*.xml` → cero
resultados).

**Restricciones `CHECK`** (`229:83-110`, más la sustitución de `333:112-121`):

| Nombre | Qué garantiza |
|---|---|
| `chk_catalog_items_item_type` | `item_type IN ('MODULE','CAPACITY','ONE_TIME','BUNDLE')` |
| `chk_catalog_items_capacity_unit` | `CAPACITY` ⇒ unidad no nula; el resto ⇒ unidad nula. **Desde `333` la lista literal se sustituyó por la FK** a `limit_dimensions(code)` |
| `chk_catalog_items_status` | `status IN ('DRAFT','ACTIVE','DEPRECATED')` |
| `chk_catalog_items_quantity_range` | `min_quantity >= 0` y `max_quantity >= min_quantity` |
| `chk_catalog_items_sort_order` | `sort_order >= 0` |
| `chk_catalog_items_trial_eligibility` | `IN ('ELIGIBLE','NEVER_FREE')` |
| `chk_catalog_items_trial_policy` | arco exclusivo: `ELIGIBLE` ⇒ días > 0 y desenlace; `NEVER_FREE` ⇒ los dos `NULL` |
| `chk_catalog_items_bundle_not_trialable` | un `BUNDLE` no se prueba como paquete (D-05) |
| `chk_catalog_items_service_nature` | `IN ('SOFTWARE_LICENSING','TECHNICAL_SERVICE','CONSULTING')` |

**Índices:** `PRIMARY(id)`, `uq_catalog_items_code(code)`, `ix_catalog_items_status_sort(status,
sort_order)` (`229:112-115`), `ix_catalog_items_capacity_unit(capacity_unit)` (`333:135-137`).
Cuatro en total; ningún otro `createIndex` sobre la tabla en los 377 changesets.

**`@Version`: SÍ.** `CatalogItemJpaEntity.java:131-133`, con su `@SQLDelete … AND version = ?`
(`:36`). **No está** en `ENTIDADES_EXENTAS_DE_VERSION`. Las tres exentas de este bloque son
`CatalogItemSubModuleJpaEntity`, `CatalogItemDependencyJpaEntity` y `BundleComponentJpaEntity`,
las tres por `E2_TABLA_PUENTE` (`HexagonalArchitectureTest.java:687,689,692`).

### 1.2 `catalog_prices` (`234_create_catalog_prices.xml:12-78`)

| Columna | Tipo | Nulabilidad |
|---|---|---|
| `id` | `BIGINT AUTO_INCREMENT` | PK |
| `price_list_id` | `BIGINT` | `NOT NULL`, FK `fk_catalog_prices_price_list` → `price_lists(id)` |
| `catalog_item_id` | `BIGINT` | `NOT NULL`, FK `fk_catalog_prices_item` → `catalog_items(id)` |
| `billing_cycle` | `VARCHAR(20)` | `NOT NULL` |
| `tier_min` | `INT DEFAULT 1` | `NOT NULL` |
| `tier_max` | `INT` | `NULL` = tramo abierto |
| `included_quantity` | `INT DEFAULT 0` | `NOT NULL` |
| `unit_amount` | `DECIMAL(19,2)` | `NOT NULL` |
| `setup_amount` | `DECIMAL(19,2) DEFAULT 0.00` | `NOT NULL` |
| `tax_rate` | `DECIMAL(5,2) DEFAULT 0.00` | `NOT NULL` |
| `tax_treatment` | `VARCHAR(20)` | `NOT NULL` |
| `created_date`, `enabled`, `version` | `DATETIME` / `BOOLEAN` / `BIGINT` | `NOT NULL` |

**Única:** `uq_catalog_prices_tier (price_list_id, catalog_item_id, billing_cycle, tier_min)`
(`234:52-54`). **Es el único índice secundario y es deliberado** — el propio changeset lo escribe:
«`uq_catalog_prices_tier` ya sirve de índice de búsqueda de precio» (`234:10-11`). El orden de
columnas es exactamente el de los cuatro `LEFT JOIN` de la landing
(`JpaPublicCatalogQueryPort.java:130-141`), así que la lectura de precios usa prefijo por la
izquierda completo.

**`CHECK`:** ciclo `IN ('MONTHLY','ANNUAL')`, tratamiento `IN ('TAXED','EXEMPT','EXCLUDED')`,
`tier_min >= 1`, `included_quantity >= 0`, importes `>= 0`, `tax_rate` entre 0 y 100, y coherencia
`TAXED ⇔ tax_rate > 0` (`234:56-73`). **`@Version`: sí** (`CatalogPriceJpaEntity.java:72-74`).

### 1.3 `bundle_components` (`232_create_bundle_components.xml:11-48`)

`id` · `bundle_item_id BIGINT NOT NULL` FK → `catalog_items(id)` · `component_item_id BIGINT NOT
NULL` FK → `catalog_items(id)` · `quantity INT NOT NULL DEFAULT 1` · `created_date` · `enabled`.
**Sin `version`: exenta por `E2_TABLA_PUENTE`** (`232:10`, `HexagonalArchitectureTest.java:692`).

Única: `uq_bundle_components (bundle_item_id, component_item_id)` (`232:35-37`).
`CHECK`: `quantity > 0` y `bundle_item_id <> component_item_id` (`232:39-43`).
**No es declarable en un `CHECK`** que el padre sea `BUNDLE` ni que el hijo no lo sea — MySQL no
puede mirar el tipo de la fila referenciada; baja a reglas de código (`232:7-10`).

### 1.4 `price_lists` (`233_create_price_lists.xml:9-63`)

`id` · `code VARCHAR(50) ascii_bin NOT NULL` `uq_price_lists_code` · `name VARCHAR(120) NOT NULL` ·
`currency CHAR(3) NOT NULL DEFAULT 'COP'` · `valid_from DATE NOT NULL` · `valid_to DATE NULL` ·
`status VARCHAR(20) NOT NULL DEFAULT 'DRAFT'` · `published_at DATETIME NULL` ·
`published_by_system_user_id BIGINT NULL` FK → `system_users(id)` · `created_date` · `enabled` ·
`version`. Índice `ix_price_lists_status_valid (status, valid_from)` (`233:55-58`).

`currency` es `CHAR(3)` y la entidad lo declara con `columnDefinition = "char(3)"` explícito porque
sin él `ddl-auto: validate` tumba el arranque entero (`PriceListJpaEntity.java:44-48`). **Cualquier
columna nueva de tipo raro hereda esa lección.**

---

## 2. Inventario literal de la semilla

### 2.1 `catalog_items` sembrados — 26 artículos (`308:71-185`)

`status = 'ACTIVE'` y `service_nature` `SOFTWARE_LICENSING` en todos salvo los dos `ONE_TIME`
(`TECHNICAL_SERVICE`). `min_quantity`/`max_quantity` = 1/1 salvo los cuatro `EXTRA_*`.

| `code` | `item_type` | `name` | `short_description` | `sort_order` | `capacity_unit` | `trial_eligibility` | `default_trial_days` |
|---|---|---|---|---|---|---|---|
| `CORE` | MODULE | Núcleo: clientes y mascotas | Clientes, mascotas y administración de la propia cuenta | 10 | — | ELIGIBLE | 30 |
| `SCHEDULING` | MODULE | Agenda de citas | Agenda, citas y recordatorios | 20 | — | ELIGIBLE | 30 |
| `CLINICAL_HISTORY` | MODULE | Historia clínica y consultas | Historia clínica del paciente y catálogo de medicamentos | 30 | — | ELIGIBLE | 30 |
| `VACCINATION_DEWORMING` | MODULE | Vacunación y desparasitación | Esquemas de vacunación y desparasitación con sus refuerzos | 35 | — | ELIGIBLE | 30 |
| `HOSPITALIZATION` | MODULE | Hospitalización | Ingreso, evolución y alta del paciente hospitalizado | 40 | — | ELIGIBLE | 30 |
| `SURGERY` | MODULE | Cirugía | Programación y registro de procedimientos quirúrgicos | 45 | — | ELIGIBLE | 30 |
| `LAB_IMAGING` | MODULE | Laboratorio e imagen diagnóstica | Órdenes, resultados y archivos de laboratorio e imagen | 50 | — | ELIGIBLE | 30 |
| `GROOMING` | MODULE | Spa, estética y guardería | Baños, estética y guardería por días | 55 | — | ELIGIBLE | 30 |
| `SERVICES` | MODULE | Servicios, tarifas y promociones | Catálogo de servicios con sus tarifas y promociones | 60 | — | ELIGIBLE | 30 |
| `CASH_REGISTER` | MODULE | Caja y punto de venta | Punto de venta, arqueo de caja e impuestos | 65 | — | ELIGIBLE | 14 |
| `INVENTORY` | MODULE | Inventario y kardex | Existencias, kardex y movimientos de mercancía | 70 | — | ELIGIBLE | 14 |
| `PURCHASES` | MODULE | Compras y proveedores | Órdenes de compra, proveedores y recepción de mercancía | 75 | — | ELIGIBLE | 14 |
| `OPEN_ACCOUNTS` | MODULE | Cuentas abiertas y cartera | Cuentas abiertas por cliente y cartera pendiente | 80 | — | ELIGIBLE | 14 |
| `ELECTRONIC_INVOICING` | MODULE | Facturación electrónica DIAN | Emisión y transmisión de la factura electrónica de venta | 85 | — | NEVER_FREE | `NULL` |
| `CAPACITY_USER` | CAPACITY | Usuario incluido | Los usuarios que trae el núcleo sin coste | 110 | `USER` | ELIGIBLE | 30 |
| `CAPACITY_BRANCH` | CAPACITY | Sede incluida | La sede que trae el núcleo sin coste | 111 | `BRANCH` | ELIGIBLE | 30 |
| `CAPACITY_TERMINAL` | CAPACITY | Terminal de caja incluida | La terminal de caja que trae el módulo de Caja sin coste | 112 | `TERMINAL` | ELIGIBLE | 14 |
| `EXTRA_USER` | CAPACITY | Usuario adicional | Cada usuario por encima de los incluidos | 120 | `USER` | NEVER_FREE | `NULL` |
| `EXTRA_BRANCH` | CAPACITY | Sede adicional | Cada sede por encima de la incluida | 125 | `BRANCH` | NEVER_FREE | `NULL` |
| `EXTRA_TERMINAL` | CAPACITY | Terminal de caja adicional | Cada punto de cobro simultáneo por encima del incluido | 130 | `TERMINAL` | NEVER_FREE | `NULL` |
| `EXTRA_STORAGE` | CAPACITY | Almacenamiento adicional (GB) | Cada gigabyte de archivos por encima del incluido | 135 | `STORAGE_GB` | NEVER_FREE | `NULL` |
| `ONBOARDING` | ONE_TIME | Implantación y capacitación | Puesta en marcha y capacitación del equipo | 210 | — | NEVER_FREE | `NULL` |
| `DATA_MIGRATION` | ONE_TIME | Migración de datos desde otro sistema | Traslado de clientes, mascotas e historia desde otro sistema | 220 | — | NEVER_FREE | `NULL` |
| `PACK_SPA` | BUNDLE | Pack Spa | Núcleo, agenda, servicios, spa y caja | 310 | — | NEVER_FREE | `NULL` |
| `PACK_CLINIC` | BUNDLE | Pack Clínica | Núcleo, agenda, historia clínica, vacunación y caja | 320 | — | NEVER_FREE | `NULL` |
| `PACK_FULL` | BUNDLE | Pack Clínica completa | Todo el producto: quince piezas enumeradas, sin anidar paquetes | 330 | — | NEVER_FREE | `NULL` |

`structural_minimum = TRUE` en tres y solo tres: `CORE`, `CAPACITY_USER`, `CAPACITY_BRANCH`
(`308:41-49`, `308:87`, `:131`, `:134`).

### 2.2 `catalog_prices` — 64 filas en `LISTA-2026-01` (`310:119-183`)

32 tramos × 2 ciclos. **Todas** con `tax_rate = 19.00` y `tax_treatment = 'TAXED'` (`310:128`),
supuesto S-01 declarado como supuesto, no como decisión fiscal (`310:10-20`).

| `code` | `tier_min` | `tier_max` | `included_quantity` | `unit_amount` MONTHLY | `unit_amount` ANNUAL | `setup_amount` |
|---|---|---|---|---|---|---|
| `CORE` | 1 | ∞ | 0 | 69 000,00 | 690 000,00 | 0 |
| `SCHEDULING` | 1 | ∞ | 0 | 35 000,00 | 350 000,00 | 0 |
| `CLINICAL_HISTORY` | 1 | ∞ | 0 | 49 000,00 | 490 000,00 | 0 |
| `VACCINATION_DEWORMING` | 1 | ∞ | 0 | 25 000,00 | 250 000,00 | 0 |
| `HOSPITALIZATION` | 1 | ∞ | 0 | 39 000,00 | 390 000,00 | 0 |
| `SURGERY` | 1 | ∞ | 0 | 29 000,00 | 290 000,00 | 0 |
| `LAB_IMAGING` | 1 | ∞ | 0 | 45 000,00 | 450 000,00 | 0 |
| `GROOMING` | 1 | ∞ | 0 | 29 000,00 | 290 000,00 | 0 |
| `SERVICES` | 1 | ∞ | 0 | 29 000,00 | 290 000,00 | 0 |
| `CASH_REGISTER` | 1 | ∞ | 0 | 46 000,00 | 460 000,00 | 0 |
| `INVENTORY` | 1 | ∞ | 0 | 39 000,00 | 390 000,00 | 0 |
| `PURCHASES` | 1 | ∞ | 0 | 29 000,00 | 290 000,00 | 0 |
| `OPEN_ACCOUNTS` | 1 | ∞ | 0 | 25 000,00 | 250 000,00 | 0 |
| `ELECTRONIC_INVOICING` | 1 | ∞ | 0 | 59 000,00 | 590 000,00 | 0 |
| `CAPACITY_USER` | 1 | ∞ | **1** | 0,00 | 0,00 | 0 |
| `CAPACITY_BRANCH` | 1 | ∞ | **0** | 0,00 | 0,00 | 0 |
| `CAPACITY_TERMINAL` | 1 | ∞ | **0** | 0,00 | 0,00 | 0 |
| `EXTRA_USER` | 1 | 8 | 0 | 12 000,00 | 120 000,00 | 0 |
| `EXTRA_USER` | 9 | ∞ | 0 | 9 000,00 | 90 000,00 | 0 |
| `EXTRA_BRANCH` | 1 | 2 | 0 | 35 000,00 | 350 000,00 | 0 |
| `EXTRA_BRANCH` | 3 | 9 | 0 | 28 000,00 | 280 000,00 | 0 |
| `EXTRA_BRANCH` | 10 | ∞ | 0 | 22 000,00 | 220 000,00 | 0 |
| `EXTRA_TERMINAL` | 1 | 3 | 0 | 18 000,00 | 180 000,00 | 0 |
| `EXTRA_TERMINAL` | 4 | ∞ | 0 | 14 000,00 | 140 000,00 | 0 |
| `EXTRA_STORAGE` | 1 | 50 | 0 | 1 200,00 | 12 000,00 | 0 |
| `EXTRA_STORAGE` | 51 | 200 | 0 | 900,00 | 9 000,00 | 0 |
| `EXTRA_STORAGE` | 201 | ∞ | 0 | 700,00 | 7 000,00 | 0 |
| `ONBOARDING` | 1 | ∞ | 0 | 0,00 | 0,00 | 0 |
| `DATA_MIGRATION` | 1 | ∞ | 0 | 0,00 | 0,00 | **450 000,00** |
| `PACK_SPA` | 1 | ∞ | 0 | 179 000,00 | 1 790 000,00 | 0 |
| `PACK_CLINIC` | 1 | ∞ | 0 | 189 000,00 | 1 890 000,00 | 0 |
| `PACK_FULL` | 1 | ∞ | 0 | 449 000,00 | 4 490 000,00 | 0 |

El anual es un **dato explícito** por tramo, no `mensual × 10` calculado, para que el descuento de
prepago quede auditable (`310:50-55`, `310:114-118`).

### 2.3 `bundle_components` — 27 filas, `quantity = 1` en todas (`309` §`309_seed_bundle_components`)

| Paquete | Componentes |
|---|---|
| `PACK_SPA` (6) | `CORE`, `SCHEDULING`, `SERVICES`, `GROOMING`, `CASH_REGISTER`, `CAPACITY_TERMINAL` |
| `PACK_CLINIC` (6) | `CORE`, `SCHEDULING`, `CLINICAL_HISTORY`, `VACCINATION_DEWORMING`, `CASH_REGISTER`, `CAPACITY_TERMINAL` |
| `PACK_FULL` (15) | `CORE`, `SCHEDULING`, `CLINICAL_HISTORY`, `VACCINATION_DEWORMING`, `HOSPITALIZATION`, `SURGERY`, `LAB_IMAGING`, `GROOMING`, `SERVICES`, `CASH_REGISTER`, `CAPACITY_TERMINAL`, `INVENTORY`, `PURCHASES`, `OPEN_ACCOUNTS`, `ELECTRONIC_INVOICING` |

**Sin anidamiento**: ningún `component_item_id` apunta a un `BUNDLE`.
**`CAPACITY_USER` y `CAPACITY_BRANCH` no son componentes de ningún paquete** — dato clave para
§4.d.

Descuento implícito del paquete frente a la suma de sus piezas al precio mensual de la §2.2
(aritmética sobre la semilla, no medición):

| Paquete | Suma de piezas | Precio | Descuento |
|---|---|---|---|
| `PACK_SPA` | 208 000 | 179 000 | 13,9 % |
| `PACK_CLINIC` | 224 000 | 189 000 | 15,6 % |
| `PACK_FULL` | 547 000 | 449 000 | 17,9 % |

### 2.4 `catalog_item_dependencies` — 13 filas en `309` + 1 en `380`

Nueve `REQUIRES` + cuatro `RECOMMENDS` (`309`), más `GROOMING REQUIRES SERVICES`
(`380_seed_grooming_requires_services.xml:46-60`). **Ningún `EXCLUDES`, y está escrito para que
nadie invente uno.**

### 2.5 La lista publicada

| `code` | `name` | `currency` | `valid_from` | `valid_to` | `status` |
|---|---|---|---|---|---|
| `LISTA-2026-01` | Tarifa 2026 | `COP` | `2026-08-27` | `NULL` | `PUBLISHED` |
| `LISTA-LOCAL-LAB` | (laboratorio) | `COP` | `2026-01-01` | `2026-08-26` | `ARCHIVED` |

Nace `DRAFT` en `310:103-112` y la publica `311:42-59` firmando con la cuenta de sistema real
resuelta por precedencia `admin` > `local-admin` > la más antigua. **Si no hay ninguna cuenta de
sistema, el changeset no publica nada y lo hace en silencio deliberado** (`311:24-30`): la lista
queda en `DRAFT`, `/catalog` y `/plans` devuelven 200 con las listas vacías y `POST
/api/v1/register` sigue devolviendo `PLATFORM_CATALOG_NOT_CONFIGURED`.

---

## 3. Las preguntas, respondidas con evidencia

### a. ¿Existe un artículo NÚCLEO? — **Sí: `CORE`**

`308:83-89`. Es `item_type = 'MODULE'` (no un tipo propio), `structural_minimum = TRUE`,
`min_quantity = 1`, `max_quantity = 1`, `sort_order = 10`, y **tiene precio propio**: 69 000
mensual / 690 000 anual, `included_quantity = 0` (`310:133-135`).

Y ya se publica como no desmarcable: `SQL_ITEMS` proyecta `ci.structural_minimum`
(`JpaPublicCatalogQueryPort.java:107`), que llega a `PublicCatalogItemResponse.mandatory`
(`:262`, columna 4 → `asBoolean`). **El front no necesita nada nuevo para el núcleo.**

**Trampa que hay que conocer:** `mandatory = true` no significa «es el núcleo», significa «forma
parte del mínimo estructural», que es un **conjunto de tres** (`CORE`, `CAPACITY_USER`,
`CAPACITY_BRANCH`). Es literalmente el error que costó la incidencia #490 y el motivo del renombre
del changeset `394` (`394:8-14`). En el `/catalog` público solo `CORE` cae en `modules` y los otros
dos en `capacities`, así que la confusión no llega a la landing — pero no la reintroduzcas.

**Qué representa hoy el precio de entrada de un `BUNDLE`:** un precio **plano y propio del
paquete**, escrito a mano en `catalog_prices` para la fila `PACK_*` (`310:164-166`). No es la suma
de sus componentes ni «núcleo + módulos»: es un cuarto precio independiente, con el descuento
implícito de la tabla §2.3. Cambiar el precio de un módulo **no** mueve el del paquete.

### b. Los 13 módulos del diseño — **los 13 existen como `MODULE`**

| Código del diseño | ¿Existe? | Evidencia |
|---|---|---|
| `SCHEDULING` | Sí | `308:90` |
| `CLINICAL_HISTORY` | Sí | `308:93` |
| `VACCINATION_DEWORMING` | Sí | `308:96` |
| `GROOMING` | Sí | `308:108` |
| `CASH_REGISTER` | Sí | `308:114` |
| `SERVICES` | Sí | `308:111` |
| `OPEN_ACCOUNTS` | Sí | `308:123` |
| `ELECTRONIC_INVOICING` | Sí | `308:126` |
| `HOSPITALIZATION` | Sí | `308:99` |
| `SURGERY` | Sí | `308:102` |
| `LAB_IMAGING` | Sí | `308:105` |
| `INVENTORY` | Sí | `308:117` |
| `PURCHASES` | Sí | `308:120` |

**Faltantes: ninguno.** Hay un decimocuarto `MODULE` que el diseño no lista y que **no es
opcional**: `CORE`. Total `MODULE` en el catálogo = 14.

### c. Precio por módulo, `MONTHLY` y `ANNUAL`, `tier_min = 1` — **completo, sin huecos**

`310:130-131` hace `JOIN (SELECT 'MONTHLY' UNION ALL SELECT 'ANNUAL') cyc ON 1 = 1`: **cada tramo
se inserta en los dos ciclos por construcción**. Un hueco solo sería posible si alguien hubiera
borrado filas después.

| Módulo | MONTHLY | ANNUAL | Hueco |
|---|---|---|---|
| `CORE` | 69 000 | 690 000 | — |
| `SCHEDULING` | 35 000 | 350 000 | — |
| `CLINICAL_HISTORY` | 49 000 | 490 000 | — |
| `VACCINATION_DEWORMING` | 25 000 | 250 000 | — |
| `HOSPITALIZATION` | 39 000 | 390 000 | — |
| `SURGERY` | 29 000 | 290 000 | — |
| `LAB_IMAGING` | 45 000 | 450 000 | — |
| `GROOMING` | 29 000 | 290 000 | — |
| `SERVICES` | 29 000 | 290 000 | — |
| `CASH_REGISTER` | 46 000 | 460 000 | — |
| `INVENTORY` | 39 000 | 390 000 | — |
| `PURCHASES` | 29 000 | 290 000 | — |
| `OPEN_ACCOUNTS` | 25 000 | 250 000 | — |
| `ELECTRONIC_INVOICING` | 59 000 | 590 000 | — |

**14 de 14 tarifados en los dos ciclos.** El suelo de artículo recurrente es 25 000 y ningún
`MODULE` baja de ahí (`310:86-93`).

### d. `EXTRA_USER` / `EXTRA_BRANCH` y la cantidad incluida

Los dos existen (`308:139-144`) con escalera de tramos: `EXTRA_USER` 1-8 a 12 000 y 9-∞ a 9 000;
`EXTRA_BRANCH` 1-2 a 35 000, 3-9 a 28 000 y 10-∞ a 22 000. También `EXTRA_TERMINAL` y
`EXTRA_STORAGE`.

**La cantidad incluida NO sale de `bundle_components.quantity`.** Sale de
`catalog_prices.included_quantity` de los artículos `CAPACITY_*`, y el techo es
`included_quantity + quantity`, donde `quantity = max(min_quantity, 1)`
(`271_raise_local_e2e_core_user_capacity_minimum.xml:11-13`, `:62`; confirmado por `310:66-74`).
`CAPACITY_USER` y `CAPACITY_BRANCH` **no aparecen en ninguna fila de `bundle_components`**: la
comprobación es directa sobre la enumeración de §2.3.

| Eje | Artículo incluido | `included_quantity` en `LISTA-2026-01` | `min_quantity` | Techo de fábrica |
|---|---|---|---|---|
| Usuarios | `CAPACITY_USER` | 1 | 1 | **2** |
| Sedes | `CAPACITY_BRANCH` | 0 | 1 | **1** |
| Terminales | `CAPACITY_TERMINAL` | 0 | 1 | **1** (solo si se contrata Caja) |

Y ya se publica: `SQL_ITEMS` proyecta `pm.included_quantity` / `pa.included_quantity`
(`JpaPublicCatalogQueryPort.java:113-114`) → `PublicCatalogCapacityResponse.monthlyIncludedQuantity`
/ `annualIncludedQuantity` (`PublicCatalogCapacityResponse.java:33-34`). **La landing puede decir
«incluye 2 usuarios y 1 sede» sin tocar el backend.**

> **Divergencia abierta que un humano tiene que resolver** (`310:76-84`): `271` subió
> `included_quantity` de `CAPACITY_USER` a **2** en la lista de laboratorio (techo 3) por decisión
> explícita del dueño, y `310` siembra **1** (techo 2) porque es lo que exige la aritmética de
> D-66. Los dos números no pueden ser correctos a la vez. Si la landing va a **imprimir** el número
> de usuarios incluidos, esto deja de ser una nota interna y pasa a ser una promesa comercial
> publicada.

### e. ¿Alguna columna puede transportar área, rótulo corto o recomendado? — **Ninguna**

Barrido completo de la §1.1: las 19 columnas de `catalog_items` están todas ocupadas y ninguna es
reutilizable.

- **Área funcional**: no existe. Vive hoy en `catalogo.content.ts:69-83` como
  `GRUPO_POR_CODIGO`, un `Record<string, GrupoCatalogo>` con las 13 asignaciones a mano, y los
  rótulos en `GRUPOS` (`:50-55`) y el orden en `ORDEN_GRUPOS` (`:58`). **Cuatro grupos, no cuatro
  áreas del diseño nuevo** — los actuales son `CLINICA` / `AGENDA` / `DINERO` / `EXISTENCIAS`.
- **Rótulo corto**: no existe. Lo más cercano es `short_description VARCHAR(255)`, que es la frase
  de escaparate («Órdenes, resultados y archivos de laboratorio e imagen»), no un rótulo de chip.
  Reutilizarla obliga a truncar en el front y a que un cambio de copy rompa la cabecera.
- **Recomendado**: no existe. Vive en `plans.content.ts:175`,
  `OVERLAY_EDITORIAL = { PACK_CLINIC: { recommended: true }, … }`. El propio comentario del fichero
  (`:166-171`) declara que es un parche y que «el arreglo de verdad es la semilla».
- `sort_order` transporta **orden global**, no pertenencia. `structural_minimum` transporta el
  mínimo estructural, que es otra cosa (§3.a). `capacity_unit` está atada por FK a
  `limit_dimensions(code)` desde `333` y no admite un uso paralelo.

**Cambio mínimo:** §4.

---

## 4. Diseño propuesto

### 4.1 `catalog_areas` — tabla de catálogo, **no** columna enum

**Recomendación: tabla + FK.** Tres argumentos, y el tercero es el que decide.

1. **Normalización.** El área no es un discriminador desnudo: tiene **rótulo de cabecera** y
   **orden de presentación**. Una columna `VARCHAR` con `CHECK` solo puede guardar el código; el
   rótulo y el orden se quedarían sin sitio y volverían al front —que es exactamente el defecto que
   esta feature existe para cerrar—. Guardar el rótulo repetido en cada `catalog_items` sería una
   dependencia funcional de una no-clave sobre otra no-clave: 2NF rota, y *SQL Antipatterns* lo
   cataloga por su nombre («Metadata Tribbles» cuando además se cablea la lista en el DDL).
2. **Evolución.** Añadir un área quinta con la tabla es **un `INSERT`**: sin DDL, sin changeset de
   esquema, sin `ALTER` y sin ventana de despliegue. Con `CHECK` es `DROP CONSTRAINT` + `ADD
   CONSTRAINT` sobre `catalog_items` en cada cambio comercial. Con `ENUM` de MySQL es un `ALTER
   TABLE` sobre la tabla y está **prohibido por la regla de la casa** (los enums de dominio van
   como `VARCHAR` con `CHECK` o tabla de catálogo, nunca `ENUM`).
3. **Es la doctrina ya escrita en este mismo esquema, y en esta misma columna.** El changeset
   `333_open_capacity_unit_to_limit_dimensions.xml` hizo **esta misma migración en esta misma
   tabla**: sustituyó la lista literal de cuatro valores de `chk_catalog_items_capacity_unit` por
   una FK a `limit_dimensions(code)`, y el Javadoc de la entidad lo deja escrito —«vender un eje
   nuevo es sembrar una fila allí, no tocar este archivo (#655)»
   (`CatalogItemJpaEntity.java:60-66`, changeset `333:112-121,146-149`)—. Proponer un `CHECK`
   cerrado para el área sería repetir el error que el repo ya pagó y ya corrigió.

**Contraargumento honesto:** un `JOIN` más por consulta. Es irrelevante: la tabla tendrá **4
filas** y `catalog_items` **26**; InnoDB resuelve eso en el buffer pool sin tocar disco. El coste
real de la tabla es un `<createTable>` de más en un changeset.

```sql
CREATE TABLE catalog_areas (
    id          BIGINT       NOT NULL AUTO_INCREMENT,
    code        VARCHAR(30)  CHARACTER SET ascii COLLATE ascii_bin NOT NULL,
    name        VARCHAR(60)  NOT NULL,
    sort_order  INT          NOT NULL DEFAULT 0,
    created_date DATETIME    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    enabled     BOOLEAN      NOT NULL DEFAULT TRUE,
    version     BIGINT       NOT NULL DEFAULT 0,
    PRIMARY KEY (id),
    CONSTRAINT uq_catalog_areas_code       UNIQUE (code),
    CONSTRAINT uq_catalog_areas_sort_order UNIQUE (sort_order),
    CONSTRAINT chk_catalog_areas_sort_order CHECK (sort_order >= 0)
);
```

Notas vinculantes para `db-migrations`:

- **`code` va `CHARACTER SET ascii COLLATE ascii_bin` y no es opcional.** `catalog_items.code` y
  `price_lists.code` ya lo son desde `332:65-77`, y `catalog_items.area_code` se va a unir con esta
  columna por igualdad. **Una divergencia de colación entre las dos columnas del `JOIN` inutiliza
  el índice y además hace que MySQL rechace la FK.** Es el mismo motivo que `332` escribió:
  `utf8mb4_0900_ai_ci` es insensible a mayúsculas y a acentos, con lo que `'core'` y `'CORE'`
  serían el mismo artículo.
- **`uq_catalog_areas_sort_order`** convierte «dos áreas empatadas en la cabecera» en un error del
  motor. Sin ella, el orden de presentación depende del desempate por `id`, que nadie ha decidido.
  Es la invariante barata que evita un informe de bug irreproducible.
- **`version` sí**, y por tanto `@SQLDelete … AND version = ?` en la entidad
  (`ENTIDADES_CON_BLOQUEO_OPTIMISTA` + `BORRADO_LOGICO_RESPETA_LA_VERSION`, reglas duras de BE-26).
  No es tabla puente: tiene atributos propios editables desde la consola, así que **no** cabe
  `E2_TABLA_PUENTE` y no hay que tocar `ENTIDADES_EXENTAS_DE_VERSION`.
- `<rollback>`: `<dropTable tableName="catalog_areas"/>`. Real, porque nada apunta a ella todavía
  si el changeset del `ALTER` va después.

**Semilla (changeset aparte, sin `context`, como `308`):**

| `code` | `name` | `sort_order` |
|---|---|---|
| `PATIENT_CARE` | Atención a pacientes | 10 |
| `FRONT_DESK` | Mostrador y dinero | 20 |
| `HOSPITAL` | Hospital y quirófano | 30 |
| `WAREHOUSE` | Bodega y compras | 40 |

`sort_order` en decenas por lo mismo que `catalog_items` lo hace: intercalar un área sin reescribir
las demás.

### 4.2 `catalog_items.area_code` — el módulo apunta a su área

```sql
ALTER TABLE catalog_items
    ADD COLUMN area_code VARCHAR(30) CHARACTER SET ascii COLLATE ascii_bin NULL
        AFTER item_type,
    ALGORITHM=INSTANT;

ALTER TABLE catalog_items
    ADD CONSTRAINT fk_catalog_items_area FOREIGN KEY (area_code)
        REFERENCES catalog_areas (code) ON DELETE RESTRICT ON UPDATE RESTRICT;

ALTER TABLE catalog_items
    ADD CONSTRAINT chk_catalog_items_area
        CHECK ((item_type = 'MODULE' AND area_code IS NOT NULL)
               OR (item_type <> 'MODULE' AND area_code IS NULL));
```

- **`NULL` y no `NOT NULL`, a propósito.** Solo los `MODULE` tienen área; una `CAPACITY`, un
  `ONE_TIME` y un `BUNDLE` no van en ninguna cabecera. El `CHECK` escribe el arco exclusivo en la
  base en vez de dejarlo en Java, y sigue el patrón exacto de `chk_catalog_items_capacity_unit`
  (`229:87-89`): «este tipo lo exige, el resto lo prohíbe».
- **⚠️ Ordena los changesets: el `CHECK` va DESPUÉS del `UPDATE` que rellena las 14 filas.** Con la
  tabla ya sembrada, añadir el `CHECK` antes del backfill lo hace fallar en la validación de datos
  existentes. Secuencia: `ADD COLUMN` → `INSERT` de áreas → `UPDATE` de asignación → `ADD
  CONSTRAINT` FK → `ADD CONSTRAINT` CHECK.
- **`CORE` también lleva área.** Es `MODULE` y el `CHECK` lo exige. Va a `PATIENT_CARE`; el front
  lo pinta fuera de las casillas por `mandatory = true`, no por ausencia de área. Si el negocio
  prefiere que `CORE` no tenga área, el `CHECK` cambia a `item_type = 'MODULE' AND
  structural_minimum = 0` — **decidir antes de escribir el changeset, no después**.

**Asignación de los 14 `MODULE`** (traducción de `catalogo.content.ts:69-83` a las cuatro áreas
nuevas; **es una propuesta de negocio, no un dato del árbol** — confírmala con el dueño):

| `code` | `area_code` |
|---|---|
| `CORE` | `PATIENT_CARE` |
| `SCHEDULING` | `PATIENT_CARE` |
| `CLINICAL_HISTORY` | `PATIENT_CARE` |
| `VACCINATION_DEWORMING` | `PATIENT_CARE` |
| `LAB_IMAGING` | `PATIENT_CARE` |
| `GROOMING` | `PATIENT_CARE` |
| `SERVICES` | `FRONT_DESK` |
| `CASH_REGISTER` | `FRONT_DESK` |
| `OPEN_ACCOUNTS` | `FRONT_DESK` |
| `ELECTRONIC_INVOICING` | `FRONT_DESK` |
| `HOSPITALIZATION` | `HOSPITAL` |
| `SURGERY` | `HOSPITAL` |
| `INVENTORY` | `WAREHOUSE` |
| `PURCHASES` | `WAREHOUSE` |

**Índices: ninguno propuesto, y es deliberado.** La FK obliga a InnoDB a crear por su cuenta un
índice de una columna sobre `area_code` si no existe —así que existirá—, y **no hay que añadir
`ix_catalog_items_area_sort (area_code, sort_order)`**: la tabla tiene 26 filas y cabe en una
página; el optimizador va a escanear y va a acertar. Un índice compuesto aquí paga escritura en
cada `INSERT` y no compra nada medible. Se revisa si la tabla pasa de ~10 000 filas, cosa que no
va a ocurrir en un catálogo de plataforma.

### 4.3 `catalog_items.short_label` — el rótulo corto

```sql
ALTER TABLE catalog_items
    ADD COLUMN short_label VARCHAR(40) NULL AFTER name,
    ALGORITHM=INSTANT;

ALTER TABLE catalog_items
    ADD CONSTRAINT chk_catalog_items_short_label
        CHECK (short_label IS NULL OR CHAR_LENGTH(TRIM(short_label)) > 0);
```

- **`VARCHAR(40)` dimensionado por el dominio, no 255 por inercia.** El rótulo más largo que hoy
  necesitaría la casilla es «Laboratorio e imagen» (20). 40 deja el doble de holgura y sigue siendo
  un contrato: si alguien mete una frase de escaparate, el motor la rechaza y el defecto se ve en
  el changeset, no en la pantalla.
- **`NULL` permitido con caída a `name`.** El front pinta `short_label ?? name`. Así la columna se
  puede rellenar módulo a módulo sin dejar ninguna casilla en blanco y sin un backfill obligatorio.
  El `CHECK` prohíbe la cadena vacía, que es el único valor que rompería esa caída (`'' ?? name`
  devuelve `''`, no `name`).
- **No se toca `short_description`.** Sigue siendo la frase de escaparate y sigue publicándose como
  `description` / `tagline`.

**Rótulos propuestos** (propuesta de negocio, no dato del árbol): Núcleo · Agenda · Historia
clínica · Vacunación · Hospitalización · Cirugía · Laboratorio e imagen · Spa y estética ·
Servicios · Caja · Inventario · Compras · Cuentas abiertas · Facturación DIAN.

### 4.4 `catalog_items.recommended` — la combinación recomendada

```sql
ALTER TABLE catalog_items
    ADD COLUMN recommended BOOLEAN NOT NULL DEFAULT FALSE,
    ALGORITHM=INSTANT;

ALTER TABLE catalog_items
    ADD CONSTRAINT chk_catalog_items_recommended
        CHECK (recommended = FALSE OR item_type = 'BUNDLE');
```

`type="BOOLEAN"` en Liquibase, **nunca `TINYINT(1)`**: con el display width Connector/J
(`tinyInt1isBit=true`) reporta la columna como `Types.BIT` y `ddl-auto: validate` tumba el arranque
de la aplicación entera. Es la misma trampa que `394:26-28` documenta para `structural_minimum`.

**Invariante «a lo sumo una recomendada» — patrón de la casa, changesets 206 / 210 / 226.** MySQL
no tiene índice único parcial; la emulación del repo es una columna generada `STORED` que vale
`NULL` fuera de alcance:

```sql
ALTER TABLE catalog_items
    ADD COLUMN recommended_bundle_singleton TINYINT
        GENERATED ALWAYS AS (
            CASE WHEN recommended = 1 AND item_type = 'BUNDLE' AND enabled = 1
                 THEN 1 END) STORED,
    ADD CONSTRAINT uq_catalog_items_recommended_bundle
        UNIQUE (recommended_bundle_singleton);
```

Como `UNIQUE` no restringe los `NULL`, solo una fila del catálogo puede valer 1: exactamente «a lo
sumo un paquete recomendado, entre los vivos». Y el borrado lógico entra en la expresión, así que
recomendar un paquete nuevo tras apagar el anterior no choca.

- **La `preCondition` es obligatoria y `onFail="HALT"`**, igual que en `226`: un `sqlCheck` que
  verifique que hoy hay 0 filas con `recommended = 1` antes de crear el índice. Sin ella, el
  `ALTER` falla a mitad y deja el changeset a medias.
- **⚠️ Coste real, y aquí sí hay que decirlo:** el manual de MySQL 8.4 pone «Adding a `STORED`
  column» como *Instant: No · In Place: No · **Rebuilds Table: Yes** · Permits Concurrent DML: No*,
  solo `ALGORITHM=COPY`. Sobre 26 filas es instantáneo en la práctica, pero **no se puede combinar
  en el mismo `ALTER` con los `ADD COLUMN` instantáneos**: van en sentencias separadas. Es la única
  operación de este documento que reconstruye la tabla.
- **Si el negocio quiere varias recomendadas**, se quita solo el bloque de la columna generada y el
  `UNIQUE`; el `CHECK` de «solo `BUNDLE`» se queda. Hoy el front tiene exactamente una
  (`plans.content.ts:175`) y `plans.store.ts` cae al primero de la lista si ninguna casa, lo que
  supone que hay una.

### 4.5 Orden — **no hace falta ninguna columna nueva**

- Orden de las **áreas**: `catalog_areas.sort_order` (§4.1).
- Orden de los **módulos dentro del área**: `catalog_items.sort_order`, que **ya existe, ya está
  sembrado** (10, 20, 30, 35, 40, 45, 50, 55, 60, 65, 70, 75, 80, 85) y **ya ordena las cuatro
  consultas públicas**: `SQL_ITEMS` (`JpaPublicCatalogQueryPort.java:146`), `SQL_PACKS` (`:180`),
  `SQL_PACK_COMPONENTS` (`:205`), `SQL_REQUIREMENTS` (`:242`) y `SQL_COMPONENTS`
  (`JpaPublicPlanQueryPort.java:186`).

Un `sort_order_in_area` sería una segunda fuente de verdad para el mismo hecho, y la primera vez
que las dos discrepen nadie sabrá cuál manda. **No lo añadas.** Si un módulo tiene que subir dentro
de su área, se cambia su `sort_order` global: como el orden global ya agrupa por afinidad, el
efecto colateral es nulo.

### 4.6 Núcleo obligatorio — **no se crea nada**

`CORE` ya existe, ya es obligatorio en el modelo (`structural_minimum = TRUE`, `min_quantity = 1`)
y ya se publica como `mandatory = true`. Lo único pendiente es que el front deje de tratarlo como
una casilla — y eso ya lo hace (`catalogo.content.ts:63-67`: «`CORE` es `is_core` y entra siempre
—no es una casilla—»). **Cero cambios de esquema.**

---

## 5. Compatibilidad con `ddl-auto: validate`

`validate` compara **la entidad contra el esquema**, no al revés: **una columna que existe en la
base y no en la `@Entity` no rompe nada**. Eso da la ventana de expand/contract limpia.

| Cambio | ¿`@Version` propia? | ¿Rompe `validate`? | Nota |
|---|---|---|---|
| `catalog_areas` (tabla nueva) | **Sí**, `version BIGINT NOT NULL` | No mientras no exista `@Entity` | Al mapearla, `@SQLDelete … AND version = ?` obligatorio |
| `catalog_items.area_code` | usa la `version` de la fila | No | Añadir el campo Java después |
| `catalog_items.short_label` | idem | No | idem |
| `catalog_items.recommended` | idem | No | **`type="BOOLEAN"`**, nunca `TINYINT(1)` |
| `recommended_bundle_singleton` | idem | **Riesgo** | Si se mapea, tiene que ir `@Generated` + `insertable=false, updatable=false`. **Recomendación: no mapearla nunca.** Es infraestructura de la constraint, no un dato del agregado |

**Orden de despliegue (expand/contract, Parallel Change):**

1. **Expand** — changesets 396…: crear `catalog_areas`, sembrarla, `ADD COLUMN` × 3, backfill,
   FK, `CHECK`, columna generada + `UNIQUE`. La aplicación en marcha **no ve nada** y sigue
   funcionando: las columnas nuevas no están en ninguna `@Entity` ni en ninguno de los cinco `SELECT`
   nativos.
2. `backend-feature` añade los campos a `CatalogItemJpaEntity`, la nueva `CatalogAreaJpaEntity`, y
   las columnas a `SQL_ITEMS` / `SQL_PACKS` y a los DTO/Response.
3. Los fronts declaran los campos nuevos en su contrato (§6).
4. **Contract** — nada que retirar: no se sustituye ninguna columna.

**Backfill:** 14 filas. Un solo `UPDATE` es correcto aquí y **no** hay que trocearlo por lotes; la
regla de los lotes existe para tablas grandes y esta tiene 26 filas. Ese `UPDATE` **debe mover
`version`** (`ci.version = ci.version + 1`): es `UPDATE_MASIVO_MUEVE_LA_VERSION`, regla dura, y es
lo que ya hacen `308:281`, `311:55` y `271:81`.

---

## 6. Riesgos

### 6.1 Añadir una columna a `catalog_items` no rompe ninguna proyección — **verificado**

Barrido: **no hay un solo `SELECT *`** en el código Java que toque `catalog_items`,
`catalog_prices`, `bundle_components` ni `price_lists`
(`grep -rn "SELECT \*" src/main/java | grep -i "catalog\|price\|bundle"` → cero resultados). Los
cinco `SELECT` nativos enumeran columnas una a una y el Javadoc dice **por qué**: «aqui se enumeran
una a una las columnas que el mundo puede ver, de modo que una columna nueva del agregado no se
cuele sola en la respuesta publica» (`JpaPublicCatalogQueryPort.java:16-22`).

**Consecuencia práctica:** las tres columnas nuevas son invisibles hasta que alguien las añade
explícitamente al `SELECT` y al `Object[]`. Eso es exactamente lo que hace seguro el paso 1 del
expand/contract, y es lo que convierte «añadir columna» en un cambio de riesgo cero para lo que ya
está desplegado.

**Cuidado con los índices posicionales.** `findContractableItems` lee `columns[0..14]` por posición
(`JpaPublicCatalogQueryPort.java:261-266`). Cuando se amplíe el `SELECT`, **las columnas nuevas van
al final**; insertarlas en medio desplaza los quince índices y el fallo aparece en tiempo de
ejecución, no de compilación —`asString(columns[7])` sobre un `BigDecimal` compila igual—.

### 6.2 Ampliar una Response pública rompe la compilación de los dos fronts

`PublicPlanCatalogResponse` y `PublicCatalogResponse` los consume la landing del tenant con
`MatchesContract`, y su comprobación **`UndeclaredFields` falla en cuanto la respuesta trae un campo
que el front no declara**. Está escrito en tres sitios del backend:
`PublicRoutes.java:94-96`, `PublicCatalogController.java` (bloque «Ampliar la otra respuesta rompe
los dos fronts»), `GetPublicCatalogUseCase.java:14`.

**Esto no es un aviso teórico: es la razón de que `/catalog` sea un recurso aparte y no un campo más
en `/plans`.** Añadir `area` a `PublicCatalogItemResponse` y `recommended` a
`PublicCatalogPackResponse` **rompe el build de los dos fronts hasta que declaren los campos**.

**Secuencia obligatoria, y el orden importa:**

1. Backend expone los campos nuevos (backend verde, fronts **rojos**).
2. Los fronts declaran los campos en `api.contract.ts` (fronts verdes).

Con dos repos independientes eso es una ventana de build roto. La alternativa **aditiva pura**, y
es la que recomiendo si la ventana no es aceptable: **un recurso nuevo `GET /catalog-areas`** que
publique `[{code, name, sortOrder, moduleCodes[]}]`, siguiendo literalmente el precedente que dejó
`/catalog` frente a `/plans` («un recurso nuevo es estrictamente aditivo: no toca una sola linea de
lo que ya funciona»). Cuesta un controlador y un `PublicRoutes` literal más; ahorra un build rojo
coordinado entre tres repositorios. **Es decisión de `backend-feature`, no mía; queda documentada
con su precedente.**

### 6.3 Riesgos menores, con su mitigación

| Riesgo | Mitigación |
|---|---|
| Colación distinta entre `catalog_areas.code` y `catalog_items.area_code` | Declarar `ascii ascii_bin` en las **dos**, como `332:65-77`. Sin ello MySQL rechaza la FK |
| El `CHECK` de `area_code` se aplica antes del backfill | Ordenar los changesets: `ADD COLUMN` → semilla → `UPDATE` → FK → `CHECK` |
| `409 PRICE_LIST_NOT_EFFECTIVE` por fechas | No aplica: este trabajo no toca `price_lists`. Pero si alguien siembra una lista nueva, `valid_from` **literal fijo**, nunca `CURRENT_DATE` (`310:22-38` documenta los cinco días que ese error costó) |
| `docker-compose.yml:79` corre `mysql:8.0.45` y RDS/Testcontainers `8.4` | Lo que se valide del `ALTER` en el compose local puede no reproducir lo de RDS. Las columnas generadas `STORED` y el `INSTANT ADD COLUMN` existen en las dos, pero el veredicto vale contra Testcontainers `mysql:8.4`, no contra el compose |

---

## 7. Medido / no medido

**Medido:** nada. **No se consultó ninguna base de datos**, ni local ni dev, por la regla del
proyecto.

**Verificado leyendo el árbol** (`VetSoftware/`, rama de trabajo actual): los cuatro `CREATE TABLE`
y sus constraints, las 26 filas de `308`, las 64 de `310`, las 27 de `309`, los cuatro índices de
`catalog_items`, la ausencia de `addColumn` sobre `catalog_items` en los 377 changesets, la ausencia
de `SELECT *` en el slice, las tres exenciones de `@Version`, y las asignaciones de área y
recomendación cableadas en los dos ficheros de contenido del front.

**No verificado, y hay que decirlo:**

- **`table_rows` reales de `catalog_items` en dev y prod.** Se asume 26 (lo que siembra `308`) más
  lo que un operador haya tecleado desde la consola de plataforma. Si en dev hay artículos
  manuales, el backfill de `area_code` los dejará en `NULL` y **el `CHECK` los rechazará**. `410`
  debe llevar una `preCondition` `sqlCheck onFail="HALT"` que verifique que no queda ningún
  `MODULE` con `area_code IS NULL` antes de crear el `CHECK`.
- **Si `LISTA-2026-01` está realmente `PUBLISHED` en dev y en prod.** `311` publica en silencio
  o no publica en silencio según haya o no una cuenta en `system_users`. Si no publicó, `/catalog`
  devuelve listas vacías y todo el rediseño de la landing muestra una pantalla vacía **sin ningún
  error**.
- **La colación efectiva del servidor.** Se asume `utf8mb4_0900_ai_ci` por defecto de 8.0/8.4;
  el parameter group podría decir otra cosa.
- **Planes de ejecución.** Ninguno. Con 26 filas no habría nada que interpretar.

---

## 8. Fuentes

| Fuente | Qué sostiene |
|---|---|
| https://dev.mysql.com/doc/refman/8.4/en/innodb-online-ddl-operations.html | «Adding a column»: *Instant Yes · Rebuilds Table No · Permits Concurrent DML Yes*. «Adding a `STORED` column»: *Instant No · In Place No · **Rebuilds Table Yes** · Concurrent DML No*, solo `ALGORITHM=COPY`. «Adding a secondary index»: in-place, no reconstruye, permite DML |
| https://dev.mysql.com/doc/refman/8.4/en/create-table-generated-columns.html | Base del patrón de unicidad condicional de §4.4 |
| https://dev.mysql.com/doc/refman/8.4/en/multiple-column-indexes.html | Regla del prefijo por la izquierda: por qué `uq_catalog_prices_tier` ya sirve a la landing y por qué no hace falta otro índice |
| https://pragprog.com/titles/bksap1/sql-antipatterns-volume-1/ | Karwin, catálogo de errores de modelado: la lista cerrada cableada en el DDL frente a la tabla de catálogo |
| https://martinfowler.com/bliki/ParallelChange.html | Expand/contract de §5 |
| https://docs.gitlab.com/development/database/avoiding_downtime_in_migrations/ | Migrar sin downtime: por qué el backfill va antes que el `CHECK` |
| `333_open_capacity_unit_to_limit_dimensions.xml` + `CatalogItemJpaEntity.java:60-66` | **El precedente del propio repo**: lista literal → FK a tabla de catálogo, en esta misma tabla |
| `226`, `210`, `206` | Patrón de la casa para unicidad condicional: columna generada `STORED` que vale `NULL` fuera de alcance |
