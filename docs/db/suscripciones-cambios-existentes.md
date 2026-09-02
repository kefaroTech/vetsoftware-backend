# Suscripciones — el delta sobre lo que ya existe

**Estado:** especificación normativa para `db-migrations` (changesets), `backend-feature` (código) y
`backend-tests` (`SchemaSeed` y las reglas de ArchUnit).

**Premisa que gobierna todo este documento:** la base de datos se asume **vacía**. Esto **no es una
migración**: es cómo debe nacer el esquema desde el primer día. Lo que sobra **se elimina en vez de
arrastrarse**.

---

## 0. Resumen

| Objeto | Qué le pasa | Dónde |
|---|---|---|
| `memberships` | **Desaparece** | §2 |
| `membership_sub_modules` | **Desaparece** | §2 |
| `companies.membership_id` + `fk_companies_membership` | **Desaparecen** | §2 |
| `sub_modules` | `+ is_sellable`, `+ read_only_capable` | §3 |
| Los cinco seeds de permisos que escriben en `membership_sub_modules` | Se les quita **ese** bloque, se conserva el resto | §2.3 |
| `SchemaSeed.java` (soporte de ~93 tests de integración) | Se reescribe: fuera `MEMBERSHIP_ID`, dentro el contrato mínimo | §5 |
| `ENTIDADES_EXENTAS_DE_VERSION` (ArchUnit) | Se borra la entrada huérfana, se añaden 14 | §6 |
| El camino de alta de una empresa | Se reescribe: el contrato sustituye al plan por defecto | §4 |

---

## 1. ⚠️ AVISO OPERATIVO — la vía elegida rompe los checksums de Liquibase

**Léelo antes de tocar nada. No es una nota al pie.**

La forma correcta de eliminar un concepto de una base **vacía** no es añadir changesets que hagan
`DROP TABLE`. Es **borrar y editar los changesets existentes**, para que una base virgen nazca ya sin
el concepto. Un `DROP` añadido al final deja el esquema limpio pero la historia sucia: 227 changesets
para crear algo que se destruye en el 228, y una tabla `DATABASECHANGELOG` que documenta un concepto
que nunca existió de verdad.

**El precio, y es alto:**

> **Editar o borrar un changeset ya ejecutado rompe el checksum de Liquibase.** Liquibase guarda en
> `DATABASECHANGELOG.MD5SUM` el hash de cada changeset ejecutado; si el fichero cambia, el siguiente
> `liquibase update` **falla** con `ValidationFailedException: Validation Failed: N change sets check
> sum`. No es un aviso: es un arranque roto, y con `ddl-auto: validate` detrás, la aplicación entera
> no levanta.

**Por lo tanto:**

1. Esta vía es **válida SOLO bajo la premisa de base vacía**.
2. **Cualquier entorno que ya haya corrido estas migraciones —dev incluido— hay que recrearlo desde
   cero.** No hay `liquibase clearCheckSums` que valga: el esquema real de dev tiene las tablas y las
   columnas que estos changesets ya no declaran, así que aunque se recalcularan los checksums,
   `ddl-auto: validate` fallaría contra la entidad `CompanyJpaEntity` sin `membership`.
3. **Esa recreación es una tarea con dueño y con ventana**, no un efecto colateral. Dev tiene apagado
   programado (EventBridge, 20:00/20:15 hora de Bogotá, L-V) y se autoarranca a los 7 días: hay que
   coordinarla.
4. **Si la premisa de base vacía dejara de ser cierta antes del despliegue**, esta vía se descarta
   entera y se sustituye por changesets nuevos de `DROP`, que es la vía conservadora. La decisión es
   del usuario, no de `db-migrations`.

Criterio: [Liquibase — Best practices](https://docs.liquibase.com/concepts/bestpractices.html), que
dice explícitamente que un changeset ejecutado no se modifica. **Aquí se modifica a sabiendas, con la
premisa escrita, y con el precio declarado.**

---

## 2. Demolición de `memberships` y `membership_sub_modules`

Desaparecen. El catálogo comercial las reemplaza por completo: lo que era un plan pasa a ser un
`catalog_items` de tipo `BUNDLE` con sus componentes, y lo que era «este plan incluye este submódulo»
pasa a ser `catalog_item_sub_modules`. **La diferencia de fondo no es de tablas: es que un paquete ya
no es una jaula.**

### 2.1 Ficheros que se ELIMINAN, con su `<include>` del maestro

| Fichero a borrar | `<include>` a borrar en `db.changelog-master.xml` | Qué contenía |
|---|---|---|
| `migrations/001_create_memberships.xml` | **línea 7** | `createTable memberships` |
| `migrations/007_create_membership_sub_modules.xml` | **línea 13** | `createTable membership_sub_modules` + `uq_membership_sub_modules` |
| `migrations/020_seed_membership_basic.xml` | **línea 26** | `INSERT memberships (name='BASIC', status='ACTIVE')` |
| `migrations/026_add_mandatory_to_memberships.xml` | **línea 31** | `addColumn memberships.mandatory` + el `UPDATE` que marca `BASIC` |

**No se renumeran los ficheros restantes.** La numeración se queda con huecos en 001, 007, 020 y 026,
igual que ya los tiene en 021 (que no existe) y 186 (que tampoco). Renumerar sería reescribir 220
`<include>` y 220 nombres de changeset para no ganar nada.

**Constraints que desaparecen con estos ficheros** (nombres reales, verificados):

- `fk_membership_sub_modules_membership` — `007_create_membership_sub_modules.xml:13`
- `fk_membership_sub_modules_sub_module` — `007_create_membership_sub_modules.xml:17`
- `uq_membership_sub_modules` — `007_create_membership_sub_modules.xml:26`

### 2.2 Ficheros que se EDITAN in situ

#### `014_create_companies.xml` — fuera la columna y su FK

Se borra el bloque de las **líneas 24-27**:

```xml
            <column name="membership_id" type="BIGINT">
                <constraints nullable="false" foreignKeyName="fk_companies_membership"
                             references="memberships(id)"/>
            </column>
```

**El nombre real de la clave foránea es `fk_companies_membership`**
(`014_create_companies.xml:25`). Es el que hay que citar en cualquier `DROP FOREIGN KEY` si algún día
se elige la vía conservadora.

**No se deja opcional «por si acaso».** Lo reemplaza `subscriptions`, que además guarda historia. Dos
fuentes de verdad sobre lo mismo es cómo se corrompe un modelo.

#### `068_add_enabled_to_all_tables.xml` — fuera los dos `addColumn`

Se borran dos `changeSet` completos:

| Líneas | `changeSet id` | Tabla |
|---|---|---|
| **174-180** | `068-memberships-enabled` | `memberships` |
| **182-188** | `068-membership_sub_modules-enabled` | `membership_sub_modules` |

El comentario de cabecera del fichero (`068_add_enabled_to_all_tables.xml:9-12`) dice *"añade columna
`enabled BOOLEAN NOT NULL DEFAULT TRUE` a las 41 tablas del schema"*. Tras el borrado son **39**.
Actualízalo: un comentario con un número que ya no cuadra es peor que ninguno.

#### `225_add_version_optimistic_lock_wave2.xml` — fuera el `addColumn` de `version`

Se borra el `changeSet` completo `225_add_version_to_memberships`, **líneas 334-343**, con su
`<rollback>`:

```xml
    <changeSet id="225_add_version_to_memberships" author="orlando">
        <addColumn tableName="memberships">
            <column name="version" type="BIGINT" defaultValueNumeric="0">
                <constraints nullable="false"/>
            </column>
        </addColumn>
        <rollback>
            <dropColumn tableName="memberships" columnName="version"/>
        </rollback>
    </changeSet>
```

**`membership_sub_modules` no aparece en `225`** —está exenta por `E2_TABLA_PUENTE`—, así que no hay
un segundo bloque que borrar aquí.

El comentario de cabecera del fichero (`225:16-20`) dice *"55 changeSets en total"* y *"deja 71 de 104
entidades con bloqueo optimista"*. Los dos números cambian. **Recuéntalos antes de escribirlos**, no
los deduzcas: ver §6.

### 2.3 Los cinco seeds de permisos — se les quita ESE bloque, se conserva el resto

Cada uno de estos ficheros hace varias cosas: crea el submódulo, siembra `base_permissions`, los ata
al rol base `ADMIN`, **habilita el submódulo para todas las membresías**, y hace *backfill* de
permisos a las empresas existentes. **Solo se elimina el cuarto bloque.** Todo lo demás se conserva
intacto, porque los submódulos, los permisos base y el *backfill* siguen siendo necesarios.

| Fichero | `changeSet` a borrar | Líneas exactas | Submódulo que habilitaba |
|---|---|---|---|
| `184_seed_branch_permissions.xml` | `184_branch_membership_sub_modules` | **76-93** | `BRANCH` |
| `191_seed_inventory_permissions.xml` | `191_inventory_membership_sub_modules` | **65-82** | `INVENTORY` |
| `196_seed_cashregister_permissions.xml` | `196_cash_membership_sub_modules` | **67-84** | `CASH` |
| `199_seed_purchases_permissions.xml` | `199_purchases_membership_sub_modules` | **69-86** | `PURCHASES` |
| `204_seed_accounts_payable_permissions.xml` | `204_ap_membership_sub_modules` | **56-69** | `PURCHASES` (guarda idempotente) |

Los cinco bloques tienen la misma forma —`INSERT INTO membership_sub_modules … CROSS JOIN
memberships m … WHERE sm.code = 'X'`— y el mismo comentario: *"Habilita el sub-módulo X para TODAS
las membresías … Sin esto, ni el registro ni el publish propagarían los permisos (gating por
membership)."* **Ese comentario describe exactamente el mecanismo que desaparece**, y por eso el
bloque entero se va con él.

Y hay un **comentario suelto** que también hay que corregir:

- `202_seed_purchase_order_receipt_permissions.xml:10` —
  *"El sub_module PURCHASES y su membership_sub_modules ya existen (199) — no se recrean aquí."*
  Se reescribe quitando la mención: *"El sub_module PURCHASES ya existe (199) — no se recrea aquí."*
  Es un comentario, no ejecuta nada, pero deja una referencia a una tabla inexistente en el árbol.

### 2.4 Verificación de que la demolición está completa

Tras las ediciones, este censo tiene que devolver **cero**:

```bash
cd VetSoftware/src/main/resources/db/changelog
grep -rn "memberships\|membership_sub_modules\|membership_id" . | grep -v "^Binary"
```

Y este otro, para el árbol Java (**103 ficheros hoy**, ver §7):

```bash
cd VetSoftware/src
grep -rln "membership\|Membership" --include=*.java .
```

---

## 3. `sub_modules` — dos columnas nuevas

**Fichero:** un changeset nuevo al final de la numeración (`db-migrations` asigna el número).
**Tabla existente:** creada en `003_create_sub_modules.xml`, con `enabled` desde `068` y `version`
desde `225`.

| Columna | Tipo | Nulabilidad | Default | Semántica |
|---|---|---|---|---|
| `is_sellable` | `BOOLEAN` | `NOT NULL` | **`FALSE`** | **Distingue lo vendible de la infraestructura interna.** Un submódulo con `is_sellable = FALSE` nunca puede aparecer como artículo en el configurador ni colgar de un `catalog_item_sub_modules` que se venda suelto. Evita que «Configuración del sistema» se ofrezca como módulo comprable |
| `read_only_capable` | `BOOLEAN` | `NOT NULL` | **`FALSE`** | **Dice si ese submódulo admite modo solo lectura.** Los que no lo admitan quedan **ocultos** al darse de baja, en lugar de mostrar pantallas rotas. Es la columna que consulta el recálculo de `company_entitlements` para decidir entre `READ_ONLY` y `NONE` |

### Por qué los dos defaults son `FALSE` y no `TRUE`

Es la decisión más importante de este changeset y va en contra del instinto.

- **`is_sellable = FALSE`**: el default seguro es «no se vende». Si un submódulo nuevo entra en el
  árbol y nadie decide si es comercial, el fallo debe ser *no aparece en el catálogo*, no *aparece un
  artículo que nadie ha puesto precio*. Un submódulo interno vendido por error es una promesa
  comercial que el producto no cumple.
- **`read_only_capable = FALSE`**: el default seguro es «no sabe funcionar en solo lectura». Si nadie
  lo ha comprobado, la pantalla se **oculta** al dar de baja. La alternativa —`TRUE` por defecto— es
  mostrar una pantalla que el equipo nunca probó en ese modo, con botones que fallan al pulsarlos. Es
  peor experiencia que no verla, y además contradice la política del anexo técnico: *dar de baja un
  módulo jamás borra ni desactiva datos; solo baja el nivel de acceso*. Ocultar una pantalla no borra
  nada; mostrar una rota sí destruye la confianza.

Con la base vacía y el catálogo aplazado (§4.3), los dos defaults `FALSE` significan que **el día 1
ningún submódulo es vendible**. Eso es correcto y es exactamente el aviso de
`suscripciones-modelo.md` §6: hasta que no haya semillas, no hay catálogo.

### DDL

```sql
ALTER TABLE sub_modules
    ADD COLUMN is_sellable       BOOLEAN NOT NULL DEFAULT FALSE,
    ADD COLUMN read_only_capable BOOLEAN NOT NULL DEFAULT FALSE;
```

En Liquibase, dos `<addColumn>` con `type="BOOLEAN"` y `defaultValueBoolean="false"`, **nunca**
`TINYINT(1)` (ver `suscripciones-modelo.md` §1.5). `<rollback>` con dos `<dropColumn>`.

**Coste del `ALTER`:** *instant*, solo metadatos, con DML concurrente
(<https://dev.mysql.com/doc/refman/8.4/en/innodb-online-ddl-operations.html>: añadir columna con
default es *instant*). No hay ventana de bloqueo ni en una tabla con datos.

**Blast radius:** `SubModuleJpaEntity` gana dos campos. La entidad la consumen
`membershipsubmodule/*` (que desaparece), `submodule/*`, `catalog_item_sub_modules` (nuevo) y
`company_entitlements` (nuevo).

---

## 4. De dónde sale ahora el reparto de permisos, y qué pasa el día 1

Esta sección responde a lo que los cinco seeds borrados hacían y que **nadie más hace hoy**.

### 4.1 Qué hacían esos seeds, en una frase

`INSERT INTO membership_sub_modules … CROSS JOIN memberships` significaba literalmente:
**«habilita este submódulo para todos los planes que existan»**. Es decir, el reparto no era por plan
en la práctica: era *todo para todos*, con la tabla de membresías haciendo de intermediario ceremonial.

Ese reparto lo consumían dos caminos, los dos verificados en el árbol:

1. **El alta de una empresa.** `RegisterUserService` →
   `registration/infrastructure/persistence/JpaDefaultMembershipProvider.java` busca la membresía
   marcada `mandatory` (`026_add_mandatory_to_memberships.xml:15-20`) y con ella
   `JpaRolePermissionInitializationPort` decide qué permisos copiarle a la empresa nueva.
2. **La republicación masiva de permisos.** `publishadminpermissions/…/PublishAdminPermissionsService`
   →`JpaMembershipSubModuleIdsQueryPort`, que hoy **filtra por membresía**.

### 4.2 De dónde sale en el modelo nuevo

El reparto sale del **contrato**, y la cadena es esta y solo esta:

```
subscriptions (contrato vigente de la empresa)
   └─ subscription_items (líneas vigentes: effective_from <= hoy AND (effective_to IS NULL OR effective_to > hoy))
        └─ catalog_items (el artículo comprado)
             └─ catalog_item_sub_modules (qué submódulos abre ese artículo)
                  └─ company_entitlements (la tabla derivada: empresa × submódulo × access_level)
                       └─ permissions / role_permissions de la empresa
```

**Cambios concretos que hay que hacer en el código** (son de `backend-feature`, aquí solo se
especifican):

| Camino de hoy | Camino nuevo |
|---|---|
| `JpaDefaultMembershipProvider` (busca la membresía obligatoria) | **Desaparece.** El alta crea la empresa **y su contrato en la misma transacción**, y deriva los permisos del contrato |
| `JpaRolePermissionInitializationPort` filtra por `membership_sub_modules` | Filtra por `company_entitlements` de la empresa recién creada |
| `JpaMembershipSubModuleIdsQueryPort` (republicación masiva) | Pasa a leer `company_entitlements` con `access_level IN ('FULL','READ_ONLY')` |
| `JpaBillingEntitlementQueryPort.hasEnabledSubModuleCode` (`electronicdocument`) — comprueba si el plan de la empresa incluye el submódulo de facturación | Pasa a `company_entitlements` con `access_level = 'FULL'` para ese `sub_module_id` |

Ese último es fácil de olvidar y es el que decide si una clínica puede emitir factura electrónica:
está en
`electronicdocument/infrastructure/persistence/JpaBillingEntitlementQueryPort.java`, y hoy consulta
`MembershipSubModuleJpaRepository.existsByMembership_IdAndSubModule_CodeAndEnabledTrueAndSubModule_EnabledTrue`.

### 4.3 Qué pasa el día 1, con el catálogo vacío

**Nada funciona, y hay que decidir cómo falla.** Las semillas están aplazadas a un PR posterior
(`suscripciones-datos-semilla.md`), así que el día del despliegue:

- No hay ningún `catalog_items` con `structural_minimum = TRUE`.
- No hay ninguna `price_lists` en `PUBLISHED`.
- Ningún `sub_modules` tiene `is_sellable = TRUE` (default `FALSE`, §3).
- Por lo tanto **`company_entitlements` nace vacía para toda empresa nueva**, y una empresa recién
  creada no puede hacer nada.

**El mínimo estructural que desbloquea el alta —cinco filas en cinco tablas— y la recomendación de
que el arranque en vacío falle de forma explícita y legible están en
`suscripciones-modelo.md` §6.** No se repiten aquí; esa sección es entregable y hay que leerla.

**La consecuencia de secuencia, y es la parte accionable:**

> **La demolición de `memberships` NO puede desplegarse en el mismo PR que la creación de las 26
> tablas, si las semillas siguen aplazadas.**
>
> Crear las 26 tablas es puramente aditivo y no rompe nada. Eliminar `memberships` **rompe el alta de
> empresas** y deja sin sustituto en pie hasta que exista catálogo. El orden seguro es:
>
> 1. **PR-1:** las 26 tablas + `sub_modules.is_sellable` / `.read_only_capable`. Aditivo, sin riesgo.
> 2. **PR-2:** las semillas del catálogo + el nuevo camino de alta + `company_entitlements` poblada.
> 3. **PR-3:** la demolición de `memberships`, `membership_sub_modules` y `companies.membership_id`,
>    junto con la reescritura de `SchemaSeed` (§5) y de la lista de ArchUnit (§6).
>
> Meter PR-3 antes que PR-2 deja el sistema sin poder dar de alta una empresa, sin mensaje que lo
> explique y sin `memberships` a la que volver.

---

## 5. `SchemaSeed` — el conjunto mínimo de filas para los ~93 tests de integración

**Entregable de primer nivel.** `VetSoftware/src/test/java/com/vetsoftware/app/testsupport/SchemaSeed.java`
es el soporte de aproximadamente 93 tests de integración. Al desaparecer `memberships`, **el árbol de
tests no compila ni corre** hasta que este seed se reescriba.

### 5.1 Lo que hay hoy y que se rompe

| Línea | Qué hace | Qué le pasa |
|---|---|---|
| `SchemaSeed.java:35` | `public static final Long MEMBERSHIP_ID = 900L;` | **Se elimina la constante** |
| `SchemaSeed.java:65-69` | `INSERT IGNORE INTO memberships (id, name, status) VALUES (:id, 'Plan test', 'ACTIVE')` | **Se elimina el bloque** |
| `SchemaSeed.java:108-113` | `INSERT IGNORE INTO companies (id, name, identifier, city_id, membership_id) VALUES (…, %d, %d)` | **Se le quita `membership_id`** |
| `SchemaSeed.java:14-15` (javadoc) | *"la cadena completa es pais → departamento → ciudad → **membresia** → empresa → sede → categoria → producto"* | **Se reescribe la cadena** |

### 5.2 El conjunto mínimo nuevo — copiable a SQL literal

Estos son los ids fijos que hay que añadir, siguiendo la convención del fichero (900+ para la cadena
raíz, y bloques de decena por concepto):

```java
public static final Long CATALOG_ITEM_CORE_ID   = 960L;
public static final Long CATALOG_PRICE_CORE_ID  = 961L;
public static final Long PRICE_LIST_ID          = 962L;
public static final Long SUBSCRIPTION_ID        = 970L;   // contrato de COMPANY_ID
public static final Long OTRA_SUBSCRIPTION_ID   = 971L;   // contrato de OTRA_COMPANY_ID
public static final Long SUBSCRIPTION_ITEM_ID   = 972L;
public static final Long OTRO_SUBSCRIPTION_ITEM_ID = 973L;
public static final Long SUB_MODULE_ID          = 980L;   // submódulo de prueba
public static final Long MODULE_ID              = 981L;   // módulo padre del anterior
```

**Orden de inserción, obligatorio** (cada paso depende del anterior por FK):

```sql
-- ── 1. Geografía (YA EXISTE en SchemaSeed, no se toca) ────────────────────
--    countries(900) → states(900) → cities(900)

-- ── 2. Módulo y submódulo de prueba ───────────────────────────────────────
--    Hacen falta porque catalog_item_sub_modules y company_entitlements
--    referencian sub_modules, y el árbol de migraciones solo siembra cuatro
--    (BRANCH, INVENTORY, CASH, PURCHASES) cuyos ids no son estables.
INSERT IGNORE INTO modules (id, name, code, created_date, enabled, version)
VALUES (981, 'Modulo de prueba', 'TEST_MODULE', NOW(), true, 0);

INSERT IGNORE INTO sub_modules (id, name, code, module_id, created_date, enabled, version,
                                is_sellable, read_only_capable)
VALUES (980, 'Submodulo de prueba', 'TEST_SUB_MODULE', 981, NOW(), true, 0, true, true);

-- ── 3. Catálogo mínimo: un artículo núcleo ────────────────────────────────
INSERT IGNORE INTO catalog_items (id, code, name, item_type, structural_minimum, min_quantity,
                                  max_quantity, sort_order, status, created_date, enabled, version)
VALUES (960, 'CORE', 'Nucleo de prueba', 'MODULE', true, 1, 1, 0, 'ACTIVE', NOW(), true, 0);

INSERT IGNORE INTO catalog_item_sub_modules (catalog_item_id, sub_module_id, created_date, enabled)
VALUES (960, 980, NOW(), true);

-- ── 4. Lista de precios publicada ─────────────────────────────────────────
--    published_by_system_user_id queda NULL: chk_price_lists_published exige
--    published_at Y published_by cuando status <> 'DRAFT', así que el seed
--    necesita también un system_user. Ver nota 5.4.
INSERT IGNORE INTO price_lists (id, code, name, currency, valid_from, status,
                                published_at, published_by_system_user_id,
                                created_date, enabled, version)
VALUES (962, 'LISTA-TEST', 'Lista de prueba', 'COP', '2026-01-01', 'PUBLISHED',
        '2026-01-01 00:00:00', 990, NOW(), true, 0);

INSERT IGNORE INTO catalog_prices (id, price_list_id, catalog_item_id, billing_cycle,
                                   tier_min, tier_max, included_quantity, unit_amount,
                                   setup_amount, tax_rate, tax_treatment,
                                   created_date, enabled, version)
VALUES (961, 962, 960, 'MONTHLY', 1, NULL, 2, 100000.00, 0.00, 19.00, 'TAXED', NOW(), true, 0);

-- ── 5. Empresas (YA EXISTEN, pero SIN membership_id) ──────────────────────
INSERT IGNORE INTO companies (id, name, identifier, city_id)
VALUES (900, 'Veterinaria de prueba', '900123456', 900);
INSERT IGNORE INTO companies (id, name, identifier, city_id)
VALUES (901, 'Veterinaria ajena', '900654321', 900);

-- ── 6. Un contrato vigente por empresa ────────────────────────────────────
--    OBLIGATORIO: la invariante del anexo dice que no existe empresa sin
--    contrato vigente, y active_marker impone uno solo por empresa.
--    OTRA_COMPANY_ID también lo lleva, porque los tests de aislamiento entre
--    tenants necesitan que la empresa ajena sea utilizable, no un cascarón.
INSERT IGNORE INTO subscriptions (id, subscription_number, company_id, quote_id, price_list_id,
                                  billing_cycle, status, start_date, trial_end_date,
                                  current_period_start, current_period_end, next_billing_date,
                                  commitment_end_date, grace_days, past_due_since, auto_renew,
                                  created_date, enabled, version)
VALUES (970, 'SUS-TEST-000900', 900, NULL, 962, 'MONTHLY', 'ACTIVE', '2026-01-01', NULL,
        '2026-01-01', '2026-01-31', '2026-02-01', NULL, 5, NULL, true, NOW(), true, 0);

INSERT IGNORE INTO subscriptions (id, subscription_number, company_id, quote_id, price_list_id,
                                  billing_cycle, status, start_date, trial_end_date,
                                  current_period_start, current_period_end, next_billing_date,
                                  commitment_end_date, grace_days, past_due_since, auto_renew,
                                  created_date, enabled, version)
VALUES (971, 'SUS-TEST-000901', 901, NULL, 962, 'MONTHLY', 'ACTIVE', '2026-01-01', NULL,
        '2026-01-01', '2026-01-31', '2026-02-01', NULL, 5, NULL, true, NOW(), true, 0);

-- ── 7. Una línea de contrato vigente por empresa ──────────────────────────
--    effective_to NULL = vigente. current_item_marker se calcula solo.
INSERT IGNORE INTO subscription_items (id, company_id, subscription_id, catalog_item_id,
                                       item_code, item_name, item_type, capacity_unit,
                                       included_quantity, tax_treatment, quantity, unit_amount,
                                       tax_rate, effective_from, effective_to, origin,
                                       created_amendment_id, ended_amendment_id,
                                       created_date, enabled, version)
VALUES (972, 900, 970, 960, 'CORE', 'Nucleo de prueba', 'MODULE', NULL,
        2, 'TAXED', 1, 100000.00, 19.00, '2026-01-01', NULL, 'INITIAL', NULL, NULL,
        NOW(), true, 0);

INSERT IGNORE INTO subscription_items (id, company_id, subscription_id, catalog_item_id,
                                       item_code, item_name, item_type, capacity_unit,
                                       included_quantity, tax_treatment, quantity, unit_amount,
                                       tax_rate, effective_from, effective_to, origin,
                                       created_amendment_id, ended_amendment_id,
                                       created_date, enabled, version)
VALUES (973, 901, 971, 960, 'CORE', 'Nucleo de prueba', 'MODULE', NULL,
        2, 'TAXED', 1, 100000.00, 19.00, '2026-01-01', NULL, 'INITIAL', NULL, NULL,
        NOW(), true, 0);

-- ── 8. Los permisos derivados de esas dos empresas ────────────────────────
--    Sin esta tabla, cualquier gate de entitlement deja fuera al test.
--    OJO: company_entitlements NO tiene columna `enabled` ni `version`.
INSERT IGNORE INTO company_entitlements (company_id, sub_module_id, access_level, source,
                                         subscription_id, subscription_item_id,
                                         valid_from, valid_until, recalculated_at, created_date)
VALUES (900, 980, 'FULL', 'SUBSCRIPTION', 970, 972,
        '2026-01-01 00:00:00.000000', NULL, '2026-01-01 00:00:00.000000', NOW());

INSERT IGNORE INTO company_entitlements (company_id, sub_module_id, access_level, source,
                                         subscription_id, subscription_item_id,
                                         valid_from, valid_until, recalculated_at, created_date)
VALUES (901, 980, 'FULL', 'SUBSCRIPTION', 971, 973,
        '2026-01-01 00:00:00.000000', NULL, '2026-01-01 00:00:00.000000', NOW());

-- ── 9. Capacidades contratadas (solo si un test toca límites) ─────────────
INSERT IGNORE INTO company_capacities (company_id, capacity_unit, limit_quantity, used_quantity,
                                       subscription_id, recalculated_at, created_date)
VALUES (900, 'USER', 2, 0, 970, '2026-01-01 00:00:00.000000', NOW());

-- ── 10. Resto de la cadena (YA EXISTE en SchemaSeed, no se toca) ──────────
--     branches(910, 911) → product_categories(920) → products(930, 931)
--     employees(940, 941) → cash_terminals(950, 951)
```

### 5.3 La cadena de dependencias actualizada, para el javadoc

El javadoc de `SchemaSeed` (`SchemaSeed.java:14-15`) describe hoy:

> `pais → departamento → ciudad → membresia → empresa → sede → categoria → producto`

Pasa a ser:

> `pais → departamento → ciudad → empresa → contrato → linea de contrato → permisos derivados`
> y en paralelo `modulo → submodulo → articulo de catalogo → lista de precios → precio`,
> y a partir de la empresa: `sede → categoria → producto`, `empleado`, `terminal`.

### 5.4 Tres cosas que hay que resolver al escribirlo, y no son opcionales

1. **`price_lists.published_by_system_user_id` es obligatorio cuando `status <> 'DRAFT'**
   (`chk_price_lists_published`). El seed necesita una fila en `system_users`, que **hoy no siembra**.
   Añádela con id fijo `990`:

   ```sql
   INSERT IGNORE INTO system_users (id, code, hash_password, created_date, enabled, version)
   VALUES (990, 'SEED-SYSTEM', 'x', NOW(), true, 0);
   ```

   La alternativa —dejar la lista en `DRAFT`— **no sirve**: un contrato apunta a la lista con la que
   se firmó, y firmar contra una lista en borrador es exactamente lo que el modelo prohíbe.

2. **`INSERT IGNORE` y las columnas generadas.** `subscriptions.active_marker`,
   `subscription_items.current_item_marker` y las tres de
   `subscription_billing_documents` son `GENERATED ALWAYS`: **no se pueden nombrar en el `INSERT`**.
   MySQL devuelve `ERROR 3105: The value specified for generated column ... is not allowed` si se
   listan, aunque el valor sea `NULL`. Están deliberadamente ausentes de todos los `INSERT` de
   arriba; no las añadas.

3. **`INSERT IGNORE` silencia también los errores de FK y de `CHECK`.** Es idempotente, que es lo que
   el fichero busca, pero convierte un seed mal escrito en un seed que "funciona" e inserta nada, y el
   test falla 40 líneas después con un `EntityNotFound` incomprensible. **Al escribir la versión
   nueva, prueba primero con `INSERT` a secas contra un Testcontainer `mysql:8.4`**, comprueba que las
   filas entran, y solo entonces pon el `IGNORE`.

### 5.5 Qué NO va en el seed, y por qué

**Ninguna tabla de dinero.** Ni `subscription_billing_documents`, ni `subscription_charges`, ni
`subscription_payments`, ni `billing_document_applications`, ni `billing_document_sequences`.

`SchemaSeed` existe para *"sembrar las filas raíz que exigen las claves foráneas del schema real"*
(`SchemaSeed.java:6`), no para montar un escenario de negocio. Un test de facturación construye sus
propios documentos, y sembrarlos aquí para todos los ~93 tests significaría que cualquier aserción de
"la cartera de esta clínica es cero" empieza en falso.

`billing_document_sequences` es el caso límite: sin fila, el generador de números falla. **Pero eso lo
siembra la migración**, no el seed de tests: es dato estructural de plataforma, como los prefijos de
resolución de la DIAN. Está anotado en `suscripciones-datos-semilla.md`.

---

## 6. ArchUnit — la exención huérfana y las 14 nuevas

**Esto va en el mismo despliegue que la demolición, o el build se pone en rojo.**

### 6.1 La entrada huérfana

`HexagonalArchitectureTest.java:551-552` contiene:

```java
            exenta("MembershipSubModuleJpaEntity", E2_TABLA_PUENTE,
                    "solo dos FK y ningún campo propio mutable; par único en BD"),
```

Al desaparecer la entidad, esa línea queda apuntando a una clase que ya no existe, y
`EXENCIONES_DE_VERSION_AL_DIA` (`HexagonalArchitectureTest.java:677`) la caza:
*"una exencion que nadie limpia deja de ser una decision y pasa a ser una mentira firmada"*.

**Acción:** borrar esa entrada. Es la única huérfana: `MembershipJpaEntity` **sí** lleva `@Version`
(`MembershipJpaEntity.java:29-31`), así que no está en la lista de exenciones y su desaparición no
deja rastro ahí.

### 6.2 Las 14 nuevas

La lista completa, con el código de exención y el motivo redactado para pegarlo tal cual, está en
**`suscripciones-modelo.md` §2.1**. No se duplica aquí para que no haya dos versiones que diverjan.

### 6.3 La aritmética del javadoc

El javadoc de `ENTIDADES_EXENTAS_DE_VERSION` (`HexagonalArchitectureTest.java:487-489`) presume de que
la cuenta cierra al dígito:

> *"104 clases `@Entity` = 71 versionadas (las 16 que ya lo estaban + 55 de la campaña de BE-26) +
> estas 33 exentas."*

Tras este trabajo, esos tres números cambian: desaparecen `MembershipJpaEntity` (versionada) y
`MembershipSubModuleJpaEntity` (exenta), y entran 12 versionadas y 14 exentas.

**No escribas el número deducido de este documento: recuéntalo sobre el árbol.** El censo de 104 y de
71 sale del javadoc, no de un conteo propio, y llevar una cuenta equivocada a una regla que existe
precisamente para que la cuenta cuadre sería un error con ironía.

### 6.4 Las dos reglas congeladas

`SIN_FINDALL_SIN_TENANT` y `REPOS_CON_ENTITYGRAPH` van con `FreezingArchRule` y lo registrado en
`config/archunit/violation-store` se tolera. Al eliminarse los repositorios de `membership` y
`membershipsubmodule`, **es probable que algunas violaciones registradas queden huérfanas**. Una
`FreezingArchRule` con entradas obsoletas no rompe el build, pero sí lo hace si el fichero de
violaciones deja de corresponder. `backend-tests` tiene que regenerar el *violation store* en el mismo
PR y revisar el diff: si desaparecen entradas, es deuda que se cerró sola y hay que decirlo.

---

## 7. Blast radius del borrado en el árbol Java

Censo medido: **103 ficheros `.java` en `src/main/java` mencionan `membership` o `Membership`.**

| Bloque | Ficheros | Qué pasa |
|---|---|---|
| `membership/**` | 30 | **Desaparece la feature entera** (dominio, casos de uso, puertos, JPA, controller, requests, responses) |
| `membershipsubmodule/**` | 34 | **Desaparece la feature entera** |
| `company/**` | 19 | Se les quita la membresía: `CompanyJpaEntity.java:35-37` (el `@ManyToOne`), `Company`, `MembershipRef`, `MembershipSummaryDto`, `MembershipQueryPort`, `CreateCompanyCommand`, `CreateCompanyService.java:33-37`, `CompanyResponse`, `CompanyMembershipSummary`, y los dos requests |
| `registration/**` | 6 | `DefaultMembershipProvider` y `JpaDefaultMembershipProvider` **desaparecen**; `RegisterUserService` y `CreateCompanyAdapter` se reescriben para crear el contrato |
| `publishadminpermissions/**` | 5 | `MembershipSubModuleIdsQueryPort` y su adaptador pasan a leer `company_entitlements` |
| `submodule/**` | 3 | `MembershipSubModuleChildrenQueryPort` y su adaptador **desaparecen** (`DeleteSubModuleService` ya no tiene que comprobar hijos de membresía; tendrá que comprobar hijos de `catalog_item_sub_modules`) |
| `electronicdocument/**` | 1 | `JpaBillingEntitlementQueryPort` pasa a `company_entitlements`. **El que decide si una clínica puede facturar electrónicamente** |
| `role/**` | 1 | `RoleJpaRepository` — mención en una consulta |
| `infrastructure/web/` | 1 | `GlobalExceptionHandler` — mapeo de `MembershipHasActiveChildrenException` y de la constraint `uq_membership_sub_modules` |
| Tests | ~40 más | Incluidos `JpaMembershipQueryPortTest` (×2), `CompanyJpaMapperTest`, `JpaBillingEntitlementQueryPortTest`, `JpaMembershipSubModuleChildrenQueryPortTest` |

**Y el contrato OpenAPI.** `api/openapi.json` describe hoy la membresía dentro de `CompanyResponse`
(hay ya dos issues abiertos sobre ese fichero, #328 y #334). Eliminar la membresía **cambia el
contrato público**: el front de plataforma (`VetSoftwareFront`) muestra el plan de cada empresa y va a
romperse. No es alcance de esta especificación, pero **es alcance del PR-3** y hay que decirlo antes
de abrirlo, no después.

---

## 8. Checklist de verificación tras la demolición

```bash
# 1. Ningún changeset menciona el concepto
cd VetSoftware/src/main/resources/db/changelog
grep -rn "memberships\|membership_sub_modules\|membership_id" .          # → 0

# 2. Ningún fuente Java lo menciona
cd VetSoftware/src
grep -rln "membership\|Membership" --include=*.java .                     # → 0

# 3. El maestro no tiene includes rotos: cada include apunta a un fichero existente
cd VetSoftware/src/main/resources
grep -o 'file="[^"]*"' db/changelog/db.changelog-master.xml \
  | sed 's/file="//;s/"//' | while read f; do [ -f "$f" ] || echo "FALTA: $f"; done

# 4. La cuenta de includes coincide con la de ficheros de migración
grep -c "<include" db/changelog/db.changelog-master.xml
ls db/changelog/migrations/*.xml | wc -l
```

**Y la comprobación cara, que va al final y en segundo plano:** un `mvn verify` completo con
Testcontainers `mysql:8.4`. Es lo único que demuestra a la vez que el changelog corre en una base
virgen, que `ddl-auto: validate` acepta las entidades, y que las tres reglas de BE-26 están al día.
