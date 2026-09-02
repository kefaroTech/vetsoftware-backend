# Modelo de datos de suscripciones — decisiones transversales

**Estado:** especificación normativa para `db-migrations` y `backend-feature`.
**Fecha:** 2026-08-22.
**Documento de diseño de origen:** `MainVetSoftware/models/modelo-datos-suscripciones.html`.
**Premisa dada por el usuario:** la base de datos se asume **vacía**. No hay migración de datos, no
hay retrocompatibilidad, y lo que estorba **se elimina, no se depreca**.

Cuando el documento de diseño y el esquema real chocan, **gana el documento** (la BD está vacía).
Todo choque detectado queda escrito en la sección [Choques detectados](#choques-detectados-contra-el-esquema-real).

## Índice de entregables

| Archivo | Qué contiene | Quién lo consume |
|---|---|---|
| `suscripciones-modelo.md` (este) | Convenciones, listas nominales de exención, convención de signos, patrón de FK compuestas | `db-migrations`, `backend-feature`, `backend-tests` |
| `suscripciones-tablas.md` | DDL objetivo de las 26 tablas nuevas, en orden de creación, con slice vertical asignado | `db-migrations` (changesets), `backend-feature` (entidades JPA) |
| `suscripciones-cambios-existentes.md` | El delta sobre lo que ya existe: 2 tablas eliminadas, 2 modificadas | `db-migrations`, `backend-feature` |
| `suscripciones-datos-semilla.md` | **PROPUESTA PARA UN PR POSTERIOR.** Catálogo, precios y cuestionario. No se implementa ahora | pendiente de decisión comercial |
| `suscripciones-reglas-codigo.md` | Las reglas que la base no puede imponer, con su consulta SQL de vigilancia | `backend-feature`, `backend-tests`, `observability-telemetry` |

---

## 1. Convenciones heredadas del repositorio — verificadas contra el esquema real

Nada de lo que sigue es preferencia. Cada línea está comprobada leyendo los changesets del árbol.

### 1.1 Colación y charset: **ninguna tabla nueva declara colación, y esto está verificado**

**Hecho medido.** Un censo sobre los 227 changesets de
`VetSoftware/src/main/resources/db/changelog/migrations/` devuelve **cero apariciones** de
`COLLATE`, `CHARACTER SET`, `charset` o `collation`:

```bash
grep -rn "COLLATE\|CHARACTER SET\|charset\|collation" *.xml *.sql   # → 0 resultados
```

Las 105 tablas existentes heredan el default del servidor (`utf8mb4_0900_ai_ci` en MySQL 8.0/8.4
salvo que el *parameter group* de RDS lo cambie).

**Regla normativa:** ningún `CREATE TABLE` ni ninguna columna `VARCHAR`/`CHAR`/`TEXT` de las 26
tablas nuevas declara `CHARACTER SET` ni `COLLATE`. Ni siquiera "para dejarlo explícito".

**Criterio:** manual de MySQL 8.4, claves foráneas —
<https://dev.mysql.com/doc/refman/8.4/en/create-table-foreign-keys.html>:

> "For nonbinary (character) string columns, the character set and collation must be the same."

Y para los `JOIN` por texto contra el catálogo, una colación divergente entre las dos columnas
impide usar el índice. Como el arranque corre con `ddl-auto: validate`
(`application.yml:81`), una divergencia no rompe una consulta: **impide levantar la aplicación
entera**.

**Lo NO medido:** no se consultó ninguna base viva para confirmar el `collation_database` real de
dev/prod (regla permanente del proyecto: se verifica leyendo código). Queda como pendiente en el
issue de verificación, con la consulta exacta que lo cierra.

### 1.2 Motor y charset de tabla

No se declara `ENGINE` ni `DEFAULT CHARSET` en ningún `createTable` del árbol. InnoDB es el default
del servidor. **Las tablas nuevas tampoco los declaran.**

### 1.3 Clave primaria

`id BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY`, sin excepción, en las 26 tablas. Nunca UUID, nunca
`String`. Es la decisión correcta para InnoDB: la PK es la clave *clustered* y una PK aleatoria
provoca división de página y engorda **todos** los índices secundarios, que la llevan dentro.

Consecuencia que hay que tener presente y que se cita más abajo: **una columna `AUTO_INCREMENT` no
puede aparecer en un `CHECK`** (§1.9).

### 1.4 `created_date`

```xml
<column name="created_date" type="DATETIME" defaultValueComputed="CURRENT_TIMESTAMP">
    <constraints nullable="false"/>
</column>
```

Idéntico a `001_create_memberships.xml:18`, `014_create_companies.xml:28` y a las 105 tablas del
árbol. **Las 26 tablas nuevas lo llevan todas, sin excepción.**

Censo de tipos temporales en el árbol: 138 columnas `DATETIME` y 5 `DATETIME(6)`. **Cero
`TIMESTAMP`.** La convención se mantiene: `DATETIME` sin zona, sin límite de 2038.

**Cuándo `DATETIME(6)` y no `DATETIME`:** cuando el orden de dos filas dentro del mismo segundo es
información de negocio. En este modelo eso pasa en `subscription_status_history.occurred_at`,
`company_entitlements.valid_from`/`valid_until`/`recalculated_at`, `subscription_payments.received_at`,
`billing_document_applications.applied_at`, `dunning_events.occurred_at` y
`subscription_billing_documents.external_registered_at`. Precedente en el árbol:
`215_add_audit_chain_integrity.xml:119` (`updated_at DATETIME(6)`).

### 1.5 `enabled` — borrado lógico

```xml
<column name="enabled" type="BOOLEAN" defaultValueBoolean="true">
    <constraints nullable="false"/>
</column>
```

Idéntico a `068_add_enabled_to_all_tables.xml`. **`type="BOOLEAN"`, nunca `TINYINT(1)`**: el
*display width* hace que Connector/J (`tinyInt1isBit=true`) reporte la columna como `Types.BIT` y
`ddl-auto: validate` falle. El mapeo lo fija
`application.yml:85` → `preferred_boolean_jdbc_type: TINYINT`.

Qué tablas **no** lo llevan: §2.2.

### 1.6 `version` — bloqueo optimista

```xml
<column name="version" type="BIGINT" defaultValueNumeric="0">
    <constraints nullable="false"/>
</column>
```

Idéntico a `225_add_version_optimistic_lock_wave2.xml:24-27`.

Regla dura de ArchUnit asociada (**BE-26**, `HexagonalArchitectureTest.java:621`):

```java
static final ArchRule ENTIDADES_CON_BLOQUEO_OPTIMISTA = classes().that()
        .areAnnotatedWith(Entity.class)
        .should(VetSoftwareConditions
                .declararBloqueoOptimistaOEstarExenta(ENTIDADES_EXENTAS_DE_VERSION))
```

Y la que impide que la lista se pudra (`HexagonalArchitectureTest.java:677`):

```java
static final ArchRule EXENCIONES_DE_VERSION_AL_DIA = ...
        .because("una exencion que nadie limpia deja de ser una decision y pasa a ser"
                + " una mentira firmada");
```

**Consecuencia operativa que hay que hacer en el mismo despliegue o el build se pone en rojo:** las
14 entidades nuevas sin `@Version` necesitan su entrada en `ENTIDADES_EXENTAS_DE_VERSION`
(§2.1), y eliminar `membership_sub_modules` deja **huérfana** la entrada
`exenta("MembershipSubModuleJpaEntity", E2_TABLA_PUENTE, …)`
(`HexagonalArchitectureTest.java:551-552`), que `EXENCIONES_DE_VERSION_AL_DIA` va a echar en falta.

**Y la que casi nadie recuerda** (`BORRADO_LOGICO_RESPETA_LA_VERSION`): en cuanto una entidad lleva
`@Version`, Hibernate liga **dos** parámetros al SQL de su `@SQLDelete` —primero el `id`, después la
`version`—. El `@SQLDelete` de toda entidad versionada tiene que escribirse:

```java
@SQLDelete(sql = "UPDATE <tabla> SET enabled = false WHERE id = ? AND version = ?")
```

Precedente correcto: `CompanyJpaEntity.java:12`. Precedente que **no** hay que copiar:
`MembershipSubModuleJpaEntity.java:14` (sin `version`, porque esa entidad está exenta).

### 1.7 Importes: `DECIMAL(19,2)`

Todos los importes de estas 26 tablas son `DECIMAL(19,2)`. Los porcentajes (tarifas de IVA,
descuentos) son `DECIMAL(5,2)`. Jamás `FLOAT` ni `DOUBLE`.

**Choque declarado con el árbol.** El censo del repositorio da:

| Tipo | Usos hoy |
|---|---|
| `DECIMAL(12,2)` | 38 |
| `DECIMAL(14,2)` | 5 |
| `DECIMAL(19,4)` | 2 |
| `DECIMAL(15,2)` | 1 |
| `DECIMAL(5,2)` | 5 |
| **`DECIMAL(19,2)`** | **0** |

Es decir: `DECIMAL(19,2)` **no se usa hoy en ninguna columna del esquema**. El documento de diseño
lo fija para toda la capa de suscripciones y **gana el documento**. Queda registrado como choque
(§5) porque implica que este bloque no comparte tipo de importe con la facturación DIAN de los
clientes (`DECIMAL(12,2)` / `DECIMAL(19,4)`).

Efecto práctico y por qué no importa aquí: no hay ninguna FK entre importes, y `DECIMAL(19,2)` no
se une nunca con `DECIMAL(12,2)` en un `JOIN`. La única regla dura del manual sobre precisión y FK
—"The size and sign of fixed precision types such as INTEGER and DECIMAL must be the same"
(<https://dev.mysql.com/doc/refman/8.4/en/create-table-foreign-keys.html>)— aplica a claves, no a
importes, y ninguna clave de este modelo es `DECIMAL`.

### 1.8 Tipos cerrados: `VARCHAR` + `CHECK` con nombre, **nunca `ENUM` nativo**

Censo del árbol: **cero** columnas `type="ENUM"` en los 227 changesets. La convención es
`VARCHAR(n)` con los códigos en mayúsculas y guion bajo (`ACTIVE`, `EXTERNAL_REGISTERED`).

**Lo nuevo que aporta esta especificación:** además del `VARCHAR`, cada tipo cerrado lleva su
`CHECK` **con nombre explícito**, `chk_<tabla>_<concepto>`. Precedente en el árbol:
`213_create_audit_event_outbox.xml:53` —
`CHECK (status IN ('PENDING','PROCESSING','PUBLISHED','FAILED'))` — y los ocho `chk_…` de
`212_create_petshop_catalog.xml`.

Motivo: añadir un valor a un `ENUM` de MySQL es un `ALTER` sobre la tabla; añadir un valor a un
`CHECK` es sustituir la constraint, que también es un `ALTER` pero no toca la definición de la
columna ni el formato de la fila. Y sobre todo: un `CHECK` con nombre es referenciable en un
`<rollback>`; un `ENUM` anónimo no.

**Cómo se declara en Liquibase.** Liquibase no tiene un tag nativo de `CHECK`. El patrón de la casa
es un bloque `<sql>` con `ALTER TABLE … ADD CONSTRAINT chk_… CHECK (…)` inmediatamente después del
`<createTable>`, dentro del mismo `changeSet`, con su `<rollback>` que hace `DROP CHECK`
(`212_create_petshop_catalog.xml:95-100`).

### 1.9 Restricción del manual que condiciona todo el diseño de `CHECK`

Manual de MySQL 8.4 —
<https://dev.mysql.com/doc/refman/8.4/en/create-table-check-constraints.html>, verbatim:

> "Nongenerated and generated columns are permitted, **except columns with the `AUTO_INCREMENT`
> attribute and columns in other tables**."

> "Foreign key referential actions (`ON UPDATE`, `ON DELETE`) are prohibited on columns used in
> `CHECK` constraints. Likewise, `CHECK` constraints are prohibited on columns used in foreign key
> referential actions."

Dos consecuencias **normativas**, y no son teóricas:

1. **Ningún `CHECK` de este modelo referencia la columna `id`.** Reglas como "una nota crédito no
   puede corregirse a sí misma" (`corrects_document_id <> id`) o "una aplicación no puede revertirse
   a sí misma" **no son declarables** y bajan a `suscripciones-reglas-codigo.md` con su consulta de
   vigilancia. El precedente del árbol que parece contradecirlo —
   `215_add_audit_chain_integrity.xml:125`, `CHECK (id = 1)`— es legal precisamente porque ahí
   `id` es `TINYINT` **sin** `autoIncrement` (`215_add_audit_chain_integrity.xml:104-106`).
2. **Todas las FK de este modelo usan `ON DELETE RESTRICT ON UPDATE RESTRICT`** y ninguna usa
   `CASCADE`, `SET NULL` ni `SET DEFAULT`. No es solo doctrina de integridad: es lo que permite que
   columnas como `billing_document_applications.payment_id` y `.source_document_id` —que son FK—
   participen en `chk_bda_source_exclusive`, el `CHECK` de exclusividad mutua que la auditoría
   marcó como bloqueante. Con un `ON DELETE SET NULL` ahí, MySQL rechazaría el `CHECK`.

### 1.10 `ON DELETE` / `ON UPDATE`: se declaran explícitamente aunque hoy nadie lo haga

**Hecho medido.** Censo del árbol: **cero** apariciones de `ON DELETE`, `ON UPDATE` o
`deleteCascade` en los 227 changesets. Las 194 FK existentes están declaradas *inline*
(`foreignKeyName` + `references`, p. ej. `014_create_companies.xml:21-22`) y por tanto corren con el
default del motor.

Manual de MySQL 8.4 —
<https://dev.mysql.com/doc/refman/8.4/en/create-table-foreign-keys.html>, verbatim:

> "For an `ON DELETE` or `ON UPDATE` that is not specified, the default action is always
> `NO ACTION`."
> "`RESTRICT`: Rejects the delete or update operation for the parent table. Specifying `RESTRICT`
> (or `NO ACTION`) is the same as omitting the `ON DELETE` or `ON UPDATE` clause."

**Regla normativa:** las FK nuevas declaran `ON DELETE RESTRICT ON UPDATE RESTRICT` de forma
explícita. El comportamiento es idéntico al de las 194 existentes —no se introduce ninguna
divergencia— pero queda escrito, que es lo que exige §1.9 punto 2 y lo que evita que el próximo
cambio lo convierta en `CASCADE` sin darse cuenta de que rompe cuatro `CHECK`.

**Ninguna FK de este modelo borra en cascada.** En una capa de dinero, un `ON DELETE CASCADE` es un
camino para que un `DELETE` mal dirigido se lleve por delante la contabilidad de una clínica.

### 1.11 Nombres

Censo del árbol de prefijos usados: `idx_` (44 índices), `ix_` (8), `uq_` (48 constraints únicas),
`fk_` (21 nombrados en bloques `<sql>`; el resto vía `foreignKeyName`), `chk_` (los de 212/213/215).

**Convención normativa para las 26 tablas nuevas** (se elige `ix_` porque es el prefijo de los
changesets más recientes, `214_add_business_metrics_query_indexes.xml:8`, y porque `idx_`/`ix_`
mezclados ya son una inconsistencia que no hay que ampliar):

| Objeto | Prefijo | Ejemplo |
|---|---|---|
| Índice no único | `ix_` | `ix_subscriptions_next_billing` |
| Constraint única | `uq_` | `uq_subscriptions_active_company` |
| Clave foránea | `fk_` | `fk_subscription_items_subscription` |
| Constraint de comprobación | `chk_` | `chk_subscription_charges_sign` |

**Cero constraints anónimas.** Una constraint sin nombre no se puede referenciar en un
`<rollback>`.

Límite duro de MySQL: 64 caracteres por identificador. Cuando `subscription_billing_documents` haga
que un nombre se pase, se abrevia a `sbd` y queda escrito en la ficha de la tabla (p. ej.
`uq_sbd_recurring_cycle`). No se abrevia por gusto, solo cuando el nombre largo no cabe.

### 1.12 Paginación (BE-21) y aislamiento por tenant

No es una convención de esquema pero condiciona los índices, así que se declara aquí para que
`backend-feature` no lo descubra tarde:

- Todo listado usa el kernel `shared/pagination` (`Pages`, `PageResult`): `DEFAULT_SIZE` 20,
  `MAX_SIZE` 200. Reglas duras: `PAGINACION_CON_UN_SOLO_CONTRATO`,
  `PAGINA_ACOTADA_EN_UN_SOLO_SITIO`, `PUENTE_DE_PAGINACION_SOLO_EN_PERSISTENCIA`.
- `LISTADOS_SIN_EMPRESA_SOLO_SYSTEM` (BE-29): un listado que no filtra por empresa solo es legal en
  un endpoint `SYSTEM`. **Afecta directamente a este modelo**: los listados de `catalog_items`,
  `price_lists`, `catalog_prices`, `configurator_*`, `billing_document_sequences` y
  `platform_billing_config` son globales de plataforma y **tienen que colgar de endpoints
  `SYSTEM`**. Los de `quotes`, `subscriptions`, `subscription_*`, `company_*`, `dunning_events` y
  `billing_document_applications` **tienen que filtrar por `company_id`**, salvo la vista de
  consola de plataforma, que es `SYSTEM`.
- `EMPRESA_NO_VIAJA_EN_EL_CUERPO`: ningún `XxxRequest` de estos slices declara un componente
  `companyId`. La empresa se toma del principal con `authz.currentCompanyId()`. Esta regla **nace
  dura y en cero** (`HexagonalArchitectureTest.java:413`): el primer request de suscripciones que
  lleve `companyId` en el cuerpo rompe el build.

**Aviso operativo heredado (memoria del proyecto):** una FK a `companies` dentro de un slice que
parecía "de catálogo" activa las cuatro reglas duras de tenant sobre **toda** la feature. Por eso
las 26 tablas se reparten en slices donde el criterio de tenant es homogéneo (§4).

---

## 2. Las dos listas nominales que `db-migrations` y ArchUnit necesitan

### 2.1 Las **14 tablas nuevas sin `version`**

Coincide al dígito con lo que anuncia el documento de diseño ("las catorce tablas nuevas que no
llevan control de concurrencia necesitan su exención declarada y motivada").

Cada línea trae ya el **código de exención** que exige `ENTIDADES_EXENTAS_DE_VERSION`
(`HexagonalArchitectureTest.java:505`) y el motivo redactado para pegarlo tal cual. Los seis códigos
posibles son `E1_APPEND_ONLY`, `E2_TABLA_PUENTE`, `E3_TOKEN`, `E4_VISTA`, `E5_SEMILLA`,
`E6_YA_PROTEGIDO`.

| # | Tabla | Entidad JPA propuesta | Código | Motivo para la lista |
|---|---|---|---|---|
| 1 | `catalog_item_sub_modules` | `CatalogItemSubModuleJpaEntity` | `E2_TABLA_PUENTE` | solo dos FK y ningún campo propio mutable; par único en BD |
| 2 | `catalog_item_dependencies` | `CatalogItemDependencyJpaEntity` | `E2_TABLA_PUENTE` | dos FK y un tipo de relación que se reemplaza borrando e insertando; terna única en BD |
| 3 | `bundle_components` | `BundleComponentJpaEntity` | `E2_TABLA_PUENTE` | composición de un paquete: se reescribe en bloque desde el editor del bundle; par único en BD |
| 4 | `quote_lines` | `QuoteLineJpaEntity` | `E1_APPEND_ONLY` | renglón congelado de la oferta: se escribe con la cotización y ningún caso de uso lo reescribe; el bloqueo vive en `quotes`, ya versionada |
| 5 | `quote_answers` | `QuoteAnswerJpaEntity` | `E1_APPEND_ONLY` | respuesta del configurador tal como se dio: se inserta una vez y ahí acaba |
| 6 | `subscription_amendments` | `SubscriptionAmendmentJpaEntity` | `E1_APPEND_ONLY` | documento inmutable del contrato: corregir un otrosí es emitir otro, nunca editarlo |
| 7 | `subscription_status_history` | `SubscriptionStatusHistoryJpaEntity` | `E1_APPEND_ONLY` | bitácora de transiciones: solo se inserta; reescribirla sería falsificar por qué una cuenta está en solo lectura |
| 8 | `subscription_billing_document_taxes` | `SubscriptionBillingDocumentTaxJpaEntity` | `E1_APPEND_ONLY` | desglose fiscal calculado una vez al cerrar el documento; el bloqueo vive en la cabecera `subscription_billing_documents`, ya versionada |
| 9 | `billing_document_applications` | `BillingDocumentApplicationJpaEntity` | `E1_APPEND_ONLY` | una aplicación no se edita: si está mal se contra-aplica con otra fila negativa que apunta a ella |
| 10 | `dunning_events` | `DunningEventJpaEntity` | `E1_APPEND_ONLY` | expediente de cobranza: su valor es probar que se avisó, y eso exige que no se pueda reescribir |
| 11 | `subscription_charges` | `SubscriptionChargeJpaEntity` | `E6_YA_PROTEGIDO` | el importe nunca muta (anular es emitir un cargo negativo); el único `UPDATE` es el sellado `PENDING → INVOICED`, que corre dentro de la misma transacción que crea `subscription_billing_documents`, ya versionada, igual que `PurchaseOrderLineJpaEntity` con su cabecera |
| 12 | `company_entitlements` | `CompanyEntitlementJpaEntity` | `E6_YA_PROTEGIDO` | tabla derivada: la recalcula un único proceso que reescribe en bloque los permisos de una empresa dentro de una transacción; un 409 aquí bloquearía el recálculo en vez de proteger nada |
| 13 | `company_capacities` | `CompanyCapacityJpaEntity` | `E6_YA_PROTEGIDO` | contador: `used_quantity` se mueve con `UPDATE … SET used_quantity = used_quantity + ?`, atómico en el motor; `@Version` lo convertiría en un 409 cada vez que dos usuarios se dan de alta a la vez |
| 14 | `billing_document_sequences` | `BillingDocumentSequenceJpaEntity` | `E6_YA_PROTEGIDO` | el consecutivo se serializa con `SELECT … FOR UPDATE` sobre la fila del prefijo; mismo razonamiento que `NumberingResolutionJpaEntity`, y `@Version` arriesgaría un 409 en mitad de una emisión |

**Las 12 tablas nuevas que SÍ llevan `version`** (por descarte, y aquí completas para que la cuenta
cierre): `catalog_items`, `price_lists`, `catalog_prices`, `configurator_questions`,
`configurator_options`, `configurator_effects`, `quotes`, `subscriptions`, `subscription_items`,
`subscription_billing_documents`, `subscription_payments`, `platform_billing_config`.

**La cuenta que ArchUnit va a comprobar:** 12 + 14 = 26. Hoy el árbol tiene 104 `@Entity` = 71
versionadas + 33 exentas. Tras este trabajo: **130 `@Entity` = 83 versionadas + 47 exentas**, menos
la entrada huérfana de `MembershipSubModuleJpaEntity` que hay que **borrar** de la lista
(`HexagonalArchitectureTest.java:551-552`) → **129 `@Entity` = 83 versionadas + 46 exentas**.

> **Nota para `backend-tests`.** Esa aritmética está calculada sobre el censo declarado en el javadoc
> de la regla ("104 clases `@Entity` = 71 versionadas + 33 exentas",
> `HexagonalArchitectureTest.java:487-489`), no sobre un conteo propio del árbol. Antes de escribir
> el número en el javadoc hay que recontarlo, porque también desaparecen `MembershipJpaEntity` y
> `MembershipSubModuleJpaEntity` como entidades.

### 2.2 Las **12 tablas nuevas sin `enabled`**

| # | Tabla | Categoría | Por qué no lleva borrado lógico |
|---|---|---|---|
| 1 | `subscription_charges` | documento de dinero | Un cargo no se desactiva. Se anula con otro cargo negativo, y los dos quedan. Con `enabled`, el `@SQLRestriction` escondería la mitad de la conciliación |
| 2 | `subscription_billing_documents` | documento de dinero | Una cuenta de cobro no se borra: se anula (`issue_status = 'VOIDED'`) o se corrige con una nota crédito encadenada |
| 3 | `subscription_billing_document_taxes` | documento de dinero | Es la base declarable ante la DIAN. Ocultar una fila cambia lo declarado sin dejar rastro |
| 4 | `subscription_payments` | documento de dinero | Un pago que entró, entró. Si se devolvió, es `status = 'REFUNDED'`, no una fila invisible |
| 5 | `billing_document_applications` | documento de dinero | Se revierte con una contra-aplicación negativa que apunta a la original. Desactivarla haría el saldo irreconstruible |
| 6 | `billing_document_sequences` | contador técnico | Un consecutivo desactivado deja de ser visible para `@SQLRestriction("enabled = true")` y el siguiente documento arranca la serie desde cero. Es un modo de fallo sin vuelta atrás |
| 7 | `dunning_events` | bitácora | Su función es probar que se avisó antes de restringir la cuenta. Una bitácora que se puede ocultar no prueba nada |
| 8 | `subscription_amendments` | documento inmutable | Es el otrosí del contrato y `subscription_items` apunta a él con `created_amendment_id`/`ended_amendment_id`. Desactivarlo dejaría líneas del contrato apuntando a un papel que la aplicación no ve |
| 9 | `subscription_status_history` | bitácora | Igual que `dunning_events`: responde "¿por qué esta cuenta está en solo lectura?" y solo sirve si es completa |
| 10 | `company_entitlements` | derivada | Es caché reconstruible. Darla de baja lógicamente crea un tercer estado —ni `FULL`, ni `READ_ONLY`, ni `NONE`, sino invisible— que nadie sabe interpretar. El recálculo borra físicamente y reinserta |
| 11 | `company_capacities` | derivada | Mismo motivo |
| 12 | `platform_billing_config` | configuración singleton | **Añadido respecto del documento de diseño, y es deliberado.** Es una única fila; con `@SQLRestriction("enabled = true")`, desactivarla deja la plataforma sin políticas y sin ninguna forma de volver atrás desde la interfaz, porque el propio formulario de edición dejaría de encontrar la fila |

**Las 14 tablas nuevas que SÍ llevan `enabled`:** `catalog_items`, `catalog_item_sub_modules`,
`catalog_item_dependencies`, `bundle_components`, `price_lists`, `catalog_prices`,
`configurator_questions`, `configurator_options`, `configurator_effects`, `quotes`, `quote_lines`,
`quote_answers`, `subscriptions`, `subscription_items`.

12 + 14 = 26. ✔

**Consecuencia directa para `backend-feature`:** las 12 tablas de arriba **no llevan `@SQLDelete` ni
`@SQLRestriction`**, y su repositorio no puede exponer un `delete`. La regla
`BORRADO_LOGICO_RESPETA_LA_VERSION` no las toca porque no tienen `@SQLDelete`; la que sí las toca es
la revisión humana, y por eso está escrito aquí.

**Y para los índices:** en las 14 tablas *con* `enabled`, todo índice de listado tiene que
contemplar `enabled`, porque `@SQLRestriction("enabled = true")` cuelga esa condición de **todas**
las consultas. En las 12 tablas *sin* `enabled`, ese problema no existe, y es una ventaja de
rendimiento que conviene no perder por inercia.

---

## 3. La convención de signos — declarada una sola vez, aquí

La primera auditoría del modelo encontró tres reglas de signo contradictorias, con la consecuencia
concreta de que **una devolución no cabía en el esquema**: el cargo de anulación tenía que ser
negativo, la fila de impuesto exigía base positiva y el documento se guardaba en positivo. Las tres
no pueden ser ciertas a la vez si no se dice cuál manda dónde.

**La convención, y no se repite en ninguna ficha de tabla:**

### 3.1 El cargo lleva signo. El documento no.

| Tabla | Columna | Signo permitido | Constraint que lo impone |
|---|---|---|---|
| `subscription_charges` | `subtotal_amount` | **con signo** — ver la excepción de §3.4 para el cargo que anula a otro | `chk_subscription_charges_sign` |
| `subscription_charges` | `quantity`, `unit_amount` | siempre `> 0` / `>= 0` | `chk_subscription_charges_quantity`, `chk_subscription_charges_unit_amount` |
| `subscription_billing_documents` | `subtotal_amount`, `tax_amount`, `total_amount`, `settled_amount` | **siempre `>= 0`** | `chk_sbd_amounts_positive` |
| `subscription_billing_document_taxes` | `taxable_base`, `tax_amount` | **siempre `>= 0`** | `chk_sbdt_amounts_positive` |
| `subscription_payments` | `amount` | siempre `> 0` | `chk_subscription_payments_amount` |
| `billing_document_applications` | `applied_amount` | **con signo**: `> 0` si es aplicación, `< 0` si es contra-aplicación | `chk_bda_reversal_sign` |
| `subscription_amendments` | `proration_amount`, `monthly_delta_amount` | **con signo** (una baja resta) | sin `CHECK`: los dos signos son legítimos |

### 3.2 En el documento, **el signo lo da el tipo**

`document_kind` decide qué significan los importes positivos del documento:

- `INVOICE` → suma a la deuda del cliente.
- `CREDIT_NOTE` → resta. Sus importes se guardan **positivos** y el efecto negativo lo produce
  `billing_document_applications` con `source_kind = 'CREDIT_NOTE'`, que reduce
  `settled_amount` del documento destino.
- `DEBIT_NOTE` → suma.

**Regla derivada que la base no puede imponer y baja a `suscripciones-reglas-codigo.md`:** en un
documento de tipo `CREDIT_NOTE` **no se pueden mezclar cargos de los dos signos**. Todos los cargos
agrupados en una nota crédito tienen que tener el mismo sentido, o el total del documento no
coincide con la suma de sus cargos en valor absoluto.

### 3.3 Por qué el cargo de anulación tiene el signo que zanja la cuenta a cero, y el documento no

Porque son dos preguntas distintas:

- **El cargo** responde "¿cuánto se devengó?". Un cargo de anulación sumado al original tiene que
  dar **cero**, y **los dos quedan**. Es la única forma de que el devengado de un periodo cierre
  sumando filas. Eso significa que **el signo del cargo de anulación es el que zanja la cuenta a
  cero, no un signo fijo**: si anula un cargo de +179.000 (una cuota `RECURRING`), la anulación va
  en −179.000; si anula un cargo que ya era negativo —un `CREDIT` mal emitido por −50.000—, la
  anulación tiene que ir en **+50.000** para que sumen cero. La primera redacción de esta sección
  decía sin más "el cargo de anulación es negativo", que es cierto en el caso más común pero **no**
  en el segundo, y esa fue exactamente la omisión que abrió el choque de §3.4.
- **El documento** responde "¿cuánto se cobra en este papel?". Un papel con un total negativo no
  existe: existe una nota crédito por 179.000. Guardarlo negativo obligaría a que
  `chk_sbd_total: total_amount = subtotal_amount + tax_amount` y `settled_amount <= total_amount`
  cambiaran de sentido según el tipo, que es exactamente el laberinto que la auditoría encontró.

### 3.4 El choque corregido (issue #402): anular un `CREDIT` ya negativo era inexpresable

**El defecto, con el mismo patrón que el de la devolución que abre esta sección.** Dos `CHECK` de
`subscription_charges` convivían sin poderse cumplir a la vez para un caso real:

- `chk_subscription_charges_voids` exige que el cargo que anula a otro (`voids_charge_id IS NOT
  NULL`) sea de tipo `CREDIT`.
- `chk_subscription_charges_sign`, en su primera redacción, exigía que **todo** `CREDIT` fuera
  `<= 0`.

Anular un `CREDIT` que ya era negativo —una nota de crédito mal emitida, que es justo el momento en
que el cliente está mirando— exige un cargo compensador **positivo**. La primera regla obliga a que
ese compensador sea `CREDIT`; la segunda obliga a que un `CREDIT` sea `<= 0`. Las dos no pueden ser
cumplirse a la vez, y la operación queda **irrepresentable**: exactamente el género de defecto que
la nota crédito de §3.3 ya obligó a corregir una vez.

**La corrección, sin tocar `chk_subscription_charges_voids` ni la convención que protege:**
`chk_subscription_charges_sign` deja de exigir signo a un `CREDIT` que está anulando a otro cargo
(`voids_charge_id IS NOT NULL`); un `CREDIT` que **no** anula nada —un crédito directo al cliente,
el caso normal— sigue obligado a `<= 0`, sin cambio:

```sql
CHECK ((charge_type IN ('CREDIT','DISCOUNT') AND voids_charge_id IS NULL AND subtotal_amount <= 0)
       OR (charge_type IN ('RECURRING','ONE_TIME') AND subtotal_amount >= 0)
       OR charge_type = 'PRORATION'
       OR (charge_type = 'CREDIT' AND voids_charge_id IS NOT NULL))
```

**Por qué no es la alternativa "que el `CHECK` mire el signo del cargo anulado".** Esa forma sería
más ajustada —obligaría al compensador a llevar el signo exacto que zanja la cuenta— pero un
`CHECK` de MySQL **no puede leer otra fila**, ni de la misma tabla ni de otra
(<https://dev.mysql.com/doc/refman/8.4/en/create-table-check-constraints.html>): `voids_charge_id`
apunta a otra fila de `subscription_charges` y su `subtotal_amount` no es visible desde el `CHECK`
de esta fila. Que la anulación efectivamente sume cero con el cargo que anula **no lo puede imponer
la base** y baja a `suscripciones-reglas-codigo.md` como regla de código, igual que ya pasa con "la
suma de lo aplicado desde un origen no supere ese origen" (R3) en `billing_document_applications`.
Lo que el `CHECK` corregido sí seguía imponiendo: que `DISCOUNT` —que nunca anula nada, porque
`chk_subscription_charges_voids` no lo permite— siga siempre `<= 0` sin excepción, y que un
`CREDIT` directo (no anulación) tampoco pueda colarse en positivo.

---

## 4. FK compuestas que arrastran `company_id` — el patrón, y por qué se invierte el orden

### 4.1 El problema que resuelve

En la primera versión del modelo, un pago apuntaba a un pago y una factura a una factura, cada uno
con su empresa por su lado, y **nada impedía que un pago de la clínica A saldara la factura de la
clínica B**. No lo detecta ninguna revisión de código, porque no es un error de programación: es un
hueco del esquema. Seis pares de tablas tenían el mismo defecto.

La corrección: la clave foránea **arrastra la empresa**. En vez de apuntar "al pago 4711", apunta "al
pago 4711 de la empresa 42". Si la factura es de otra empresa, **la base rechaza la fila**. Deja de
ser una regla que hay que recordar y pasa a ser estructuralmente imposible.

**Criterio citado:**
[Azure Architecture Center — Tenancy models](https://learn.microsoft.com/en-us/azure/architecture/guide/multitenant/considerations/tenancy-models)
y [Citus — Multi-tenant applications](https://docs.citusdata.com/en/stable/use_cases/multi_tenant.html):
en el modelo de esquema compartido con discriminador, el identificador de tenant va **en la clave**,
no solo en un `WHERE`. MySQL no tiene RLS (es una función de PostgreSQL), así que la FK compuesta es
el único mecanismo del motor que puede imponerlo.

### 4.2 La decisión de orden: `(company_id, id)`, no `(id, company_id)`

**El encargo pedía la clave auxiliar como `(id, company_id)`. Esta especificación la invierte a
`(company_id, id)`, y esto es lo que hay que leer antes de aceptarlo o rechazarlo.**

Las dos formas son igual de válidas para el motor. Manual de MySQL 8.4 —
<https://dev.mysql.com/doc/refman/8.4/en/create-table-foreign-keys.html>, verbatim:

> "In the referencing table, there must be an index where the foreign key columns are listed as the
> *first* columns in the same order. Such an index is created on the referencing table automatically
> if it does not exist."
> "there must still be an index in the referenced table where the referenced columns are the *first*
> columns in the same order."

Es decir: el motor exige un índice en padre y otro en hijo con las columnas **en el orden en que se
declara la FK**. Eso convierte la elección de orden en una decisión de índices, no de estilo:

| Opción | Índice que queda en el padre | Índice que queda en el hijo |
|---|---|---|
| (a) `UNIQUE (id, company_id)`, FK `(parent_id, company_id)` | `(id, company_id)` — casi inútil: `PRIMARY(id)` ya resuelve todo lo que empieza por `id` | `(parent_id, company_id)` — sirve para "quién apunta al padre P", **no** para "todo lo de la empresa C" |
| (b) `UNIQUE (company_id, id)`, FK `(company_id, parent_id)` | `(company_id, id)` — **es el índice de listado por tenant del padre**, gratis | `(company_id, parent_id)` — **empieza por `company_id`**, sirve para el listado por tenant y para la FK |

La opción (b) da, sin pagar un índice extra, exactamente lo que el proyecto necesita: **el tenant
delante en todas las claves e índices**, que es la regla número uno de un esquema multi-tenant que
tiene que seguir siendo barato cuando pase de 3 clínicas a 300.

Con la opción (a), **ninguna** de las tablas de dinero tendría un índice que empiece por
`company_id`, porque `company_id` iría siempre en segunda posición de cada índice de FK. Y "tablas
con `company_id` que no lo tienen como primera columna de ningún índice" es el patrón exacto de
degradación al crecer el número de clínicas.

**Coste honesto de (b):** la clave `uq_<tabla>_company_id (company_id, id)` es un índice único
adicional de 16 bytes por fila más la PK. En una tabla de dinero con decenas de miles de filas es
irrelevante; en las 20 GiB de gp3 actuales, invisible. Y no es redundante con la PK:
`sys.schema_redundant_indexes` no la marca, porque `PRIMARY(id)` no es prefijo de `(company_id, id)`.

**Si el usuario prefiere `(id, company_id)`, se cambia el orden en las dos columnas de cada FK y en
cada clave auxiliar, y todo lo demás de esta especificación sigue siendo válido.** Queda escrito
para que la decisión sea suya y no un descubrimiento dentro de seis meses.

### 4.3 Los SEIS pares de tablas afectados

Estos son los seis que la auditoría identificó, en el orden en que aparecen en el circuito del
dinero. La columna "necesitaba clave nueva" recoge el hallazgo de la segunda pasada ("cuatro no
necesitaban nada nuevo; las otras dos necesitaban primero una clave única en su tabla padre").

| # | Hijo | Padre | Columnas de la FK (hijo) | Clave referenciada (padre) | Nombre de la FK | Qué fuga cierra |
|---|---|---|---|---|---|---|
| **P1** | `subscription_items` | `subscriptions` | `(company_id, subscription_id)` | `uq_subscriptions_company_id` | `fk_subscription_items_subscription` | Una línea de contrato colgando del contrato de otra clínica |
| **P2** | `subscription_charges` | `subscription_billing_documents` | `(company_id, billing_document_id)` | `uq_sbd_company_id` | `fk_subscription_charges_document` | Un cargo devengado de una clínica facturado en el documento de otra |
| **P3** | `billing_document_applications` | `subscription_billing_documents` | `(company_id, target_document_id)` | `uq_sbd_company_id` | `fk_bda_target_document` | **El hallazgo original:** una aplicación que reduce el saldo de la factura de otra clínica |
| **P4** | `billing_document_applications` | `subscription_payments` | `(company_id, payment_id)` | `uq_subscription_payments_company_id` | `fk_bda_payment` | **El hallazgo original:** un pago de la clínica A saldando la factura de la clínica B |
| **P5** | `subscription_billing_documents` | `subscription_billing_documents` (autorreferencia) | `(company_id, corrects_document_id)` | `uq_sbd_company_id` | `fk_sbd_corrects` | **El sexto, el que la segunda pasada encontró abierto:** la nota crédito de una clínica corrigiendo la factura de otra |
| **P6** | `company_entitlements` | `subscription_items` | `(company_id, subscription_item_id)` | `uq_subscription_items_company_id` | `fk_company_entitlements_item` | Un permiso concedido a una clínica justificado por la línea de contrato de un tercero |

De los seis, **P5 y P6 son los que necesitaron clave única nueva en el padre** (`uq_sbd_company_id`
sobre la propia `subscription_billing_documents` y `uq_subscription_items_company_id` sobre
`subscription_items`); los otros cuatro reutilizan claves que ya hacían falta por P1–P4.

### 4.4 Las claves únicas auxiliares — inventario completo por tabla padre

Toda tabla padre de una FK compuesta declara una clave única auxiliar. Son **seis**:

| Tabla padre | Clave auxiliar | Columnas | La necesitan |
|---|---|---|---|
| `subscriptions` | `uq_subscriptions_company_id` | `(company_id, id)` | `subscription_items`, `subscription_amendments`, `subscription_status_history`, `subscription_charges`, `subscription_billing_documents`, `company_entitlements`, `company_capacities`, `dunning_events` |
| `subscription_amendments` | `uq_subscription_amendments_company_id` | `(company_id, id)` | `subscription_items` (`created_amendment_id`, `ended_amendment_id`), `subscription_charges` (`amendment_id`) |
| `subscription_items` | `uq_subscription_items_company_id` | `(company_id, id)` | `company_entitlements`, `subscription_charges` |
| `subscription_billing_documents` | `uq_sbd_company_id` | `(company_id, id)` | `subscription_charges`, `subscription_billing_document_taxes`, `billing_document_applications` (×2), `dunning_events`, y ella misma (`corrects_document_id`) |
| `subscription_payments` | `uq_subscription_payments_company_id` | `(company_id, id)` | `billing_document_applications` |
| `billing_document_applications` | `uq_bda_company_id` | `(company_id, id)` | ella misma (`reversal_of_id`) |

### 4.5 Inventario completo de FK compuestas (13, no 6)

Los "seis pares" son los que la auditoría nombró. El patrón se aplica a **todas** las referencias
entre tablas con `company_id`, sean o no de dinero. Esta es la lista que `db-migrations` tiene que
escribir entera:

| Hijo | Columnas | Padre | Referenciada | Nombre |
|---|---|---|---|---|
| `subscription_items` | `(company_id, subscription_id)` | `subscriptions` | `(company_id, id)` | `fk_subscription_items_subscription` |
| `subscription_items` | `(company_id, created_amendment_id)` | `subscription_amendments` | `(company_id, id)` | `fk_subscription_items_created_amendment` |
| `subscription_items` | `(company_id, ended_amendment_id)` | `subscription_amendments` | `(company_id, id)` | `fk_subscription_items_ended_amendment` |
| `subscription_amendments` | `(company_id, subscription_id)` | `subscriptions` | `(company_id, id)` | `fk_subscription_amendments_subscription` |
| `subscription_status_history` | `(company_id, subscription_id)` | `subscriptions` | `(company_id, id)` | `fk_ssh_subscription` |
| `company_entitlements` | `(company_id, subscription_id)` | `subscriptions` | `(company_id, id)` | `fk_company_entitlements_subscription` |
| `company_entitlements` | `(company_id, subscription_item_id)` | `subscription_items` | `(company_id, id)` | `fk_company_entitlements_item` |
| `company_capacities` | `(company_id, subscription_id)` | `subscriptions` | `(company_id, id)` | `fk_company_capacities_subscription` |
| `subscription_billing_documents` | `(company_id, subscription_id)` | `subscriptions` | `(company_id, id)` | `fk_sbd_subscription` |
| `subscription_billing_documents` | `(company_id, corrects_document_id)` | `subscription_billing_documents` | `(company_id, id)` | `fk_sbd_corrects` |
| `subscription_billing_document_taxes` | `(company_id, billing_document_id)` | `subscription_billing_documents` | `(company_id, id)` | `fk_sbdt_document` |
| `subscription_charges` | `(company_id, subscription_id)` | `subscriptions` | `(company_id, id)` | `fk_subscription_charges_subscription` |
| `subscription_charges` | `(company_id, subscription_item_id)` | `subscription_items` | `(company_id, id)` | `fk_subscription_charges_item` |
| `subscription_charges` | `(company_id, amendment_id)` | `subscription_amendments` | `(company_id, id)` | `fk_subscription_charges_amendment` |
| `subscription_charges` | `(company_id, billing_document_id)` | `subscription_billing_documents` | `(company_id, id)` | `fk_subscription_charges_document` |
| `subscription_charges` | `(company_id, voids_charge_id)` | `subscription_charges` | `(company_id, id)` | `fk_subscription_charges_voids` |
| `billing_document_applications` | `(company_id, target_document_id)` | `subscription_billing_documents` | `(company_id, id)` | `fk_bda_target_document` |
| `billing_document_applications` | `(company_id, source_document_id)` | `subscription_billing_documents` | `(company_id, id)` | `fk_bda_source_document` |
| `billing_document_applications` | `(company_id, payment_id)` | `subscription_payments` | `(company_id, id)` | `fk_bda_payment` |
| `billing_document_applications` | `(company_id, reversal_of_id)` | `billing_document_applications` | `(company_id, id)` | `fk_bda_reversal` |
| `dunning_events` | `(company_id, subscription_id)` | `subscriptions` | `(company_id, id)` | `fk_dunning_events_subscription` |
| `dunning_events` | `(company_id, billing_document_id)` | `subscription_billing_documents` | `(company_id, id)` | `fk_dunning_events_document` |

Son 22 claves foráneas compuestas. Los "seis pares" de la auditoría son los seis **pares de tablas**
donde la fuga era de dinero o de permisos; el patrón aplica a las 22.

### 4.6 El agujero conocido de las FK compuestas con columna nulable

Varias de estas FK tienen la columna del padre **nulable** (`created_amendment_id`,
`corrects_document_id`, `payment_id`, `source_document_id`, `reversal_of_id`,
`subscription_item_id`, `billing_document_id`, `voids_charge_id`). En una FK multicolumna, si
**alguna** de las columnas es `NULL` la restricción se considera satisfecha (semántica `MATCH
SIMPLE` del estándar SQL, que es la que implementa InnoDB).

Aquí eso **no abre ninguna fuga**, y conviene entender por qué antes de preocuparse: `company_id` es
`NOT NULL` en las 22 filas hijas. La única forma de que la FK compuesta no se compruebe es que la
columna del padre sea `NULL`, y en ese caso **no hay ninguna fila padre a la que apuntar**. No existe
la combinación "apunta a un padre de otra empresa y la FK no lo ve".

**Lo NO verificado:** el manual de MySQL 8.4 que se consultó (`create-table-foreign-keys.html`) **no
documenta explícitamente** la semántica `MATCH SIMPLE` para FK multicolumna con `NULL` parcial. La
afirmación de arriba se apoya en el estándar SQL y en el comportamiento conocido de InnoDB, no en una
cita del manual. Se registra como pendiente de verificar con una prueba de integración
(`AbstractDataJpaTest`, Testcontainers `mysql:8.4`) que intente insertar la combinación y compruebe
que la base la rechaza. Va en el issue de cierre.

### 4.7 La excepción declarada: `quote_lines` y `quote_answers` no arrastran empresa

`quotes.company_id` es **nulable** a propósito: una cotización a un prospecto que todavía no es
empresa no tiene `company_id`. Eso hace que una FK compuesta `(company_id, quote_id)` desde
`quote_lines` sea inútil —con `company_id` nulo en el padre la restricción nunca se comprueba— y
además obligaría a duplicar una columna que la mitad de las veces está vacía.

**Decisión:** `quote_lines` y `quote_answers` **no llevan `company_id`**. Cuelgan de `quotes` con una
FK simple, y **`quotes` es la frontera de tenant** de todo ese bloque. Toda consulta sobre líneas o
respuestas de cotización pasa por `quotes` y se acota ahí.

Consecuencia para `backend-feature`: `JpaQuoteLineRepository` **no puede** exponer un `findByX` que
devuelva varias filas sin pasar por `quote_id`, y `REFERENCIAS_CROSS_FEATURE_ACOTADAS_POR_EMPRESA`
hay que satisfacerla resolviendo primero la cotización acotada por empresa. Está escrito aquí porque
es la única tabla del modelo donde el aislamiento **no** es estructural sino de consulta.

---

## 5. Choques detectados contra el esquema real

Gana el documento de diseño en todos (la BD está vacía), pero quedan escritos.

| # | Choque | Qué dice el esquema real | Qué dice el documento | Resolución |
|---|---|---|---|---|
| C1 | **Tipo de importe** | `DECIMAL(12,2)` en 38 columnas, `DECIMAL(19,4)` en 2, `DECIMAL(19,2)` en **0** | `DECIMAL(19,2)` para toda la capa de suscripciones | Gana el documento. Queda un esquema con dos convenciones de importe: la de facturación DIAN del cliente y la de suscripciones. No se cruzan en ningún `JOIN` |
| C2 | **Códigos en español** | Todos los tipos cerrados del árbol usan códigos en inglés (`ACTIVE`, `OPEN`, `PENDING`, `CANCELLED`) | `catalog_item_dependencies.relation_type` con `REQUIERE` / `RECOMIENDA` / `EXCLUYE` | **Gana el árbol**, con motivo: el propio documento usa inglés en todos los demás tipos cerrados (`MODULE`, `CAPACITY`, `ADD`, `REMOVE`, `SET_QUANTITY`), así que la terna en español es una inconsistencia interna del documento, no una decisión. Se especifica `REQUIRES` / `RECOMMENDS` / `EXCLUDES` |
| C3 | **Cero FK compuestas en el árbol** | Las 194 FK existentes son de una sola columna, declaradas *inline* | El modelo exige 22 FK compuestas | Gana el documento. Implica que `db-migrations` **no puede** usar el atributo *inline* `foreignKeyName`+`references` para estas: hace falta `<addForeignKeyConstraint baseColumnNames="company_id,x">` o un bloque `<sql>` |
| C4 | **Cero `ON DELETE`/`ON UPDATE` en el árbol** | Todas las FK corren con el default `NO ACTION` | El documento no se pronuncia | Se declara `ON DELETE RESTRICT ON UPDATE RESTRICT` explícito. Comportamiento idéntico, pero exigido por la restricción de `CHECK` del manual (§1.9) |
| C5 | **`platform_billing_config` y `enabled`** | La convención del árbol es que toda tabla lleva `enabled` | El documento dice que solo los documentos de dinero y las derivadas se libran | Se le quita `enabled` con motivo escrito (§2.2, fila 12). Divergencia consciente respecto del documento |
| C6 | **`billing_reason` no existe en el documento** | — | El documento pide una barandilla que agrupe "por periodo exacto" pero no da la columna que distingue la factura de ciclo de una puntual | Se **añade** `subscription_billing_documents.billing_reason VARCHAR(20)`. Sin ella la barandilla o no cierra la doble facturación o bloquea cobros legítimos. Detalle y justificación en `suscripciones-tablas.md` |
| C7 | **`sub_modules` no tiene `company_id`** | `003_create_sub_modules.xml` — tabla global de plataforma | `company_entitlements` mapea empresa × submódulo | Sin choque real, pero conviene decirlo: `catalog_item_sub_modules` es global y **no lleva `company_id`**; el tenant aparece solo en `company_entitlements` |
| C8 | **Cuatro submódulos sembrados, no un catálogo** | Migraciones siembran `GENERAL` + `BRANCH`, `INVENTORY`, `CASH`, `PURCHASES` (`184`, `191`, `196`, `199`) | El documento asume un catálogo completo | Confirmado y sin resolver en este trabajo: ver §6 |

Columnas añadidas por esta especificación que **no** están en el documento de diseño, todas marcadas
como tales en su ficha: `price_lists.name`, `configurator_questions.sort_order`,
`quote_lines.line_number`, `quote_lines.tax_treatment`, `subscription_charges.tax_treatment`,
`subscription_charges.voids_charge_id`, `subscription_billing_documents.billing_reason`,
`billing_document_applications.applied_at`, `subscription_payments.client_request_id`,
`dunning_events.subscription_id` y `dunning_events.billing_document_id`,
`company_capacities.subscription_id`.

---

## 6. Consecuencia de aplazar las semillas

**Decisión del usuario: en este trabajo no se siembra catálogo.** `suscripciones-datos-semilla.md`
queda como propuesta para un PR posterior. Esta sección es **entregable**, no una nota al margen,
porque el aplazamiento tiene una consecuencia funcional inmediata.

### 6.1 Qué se rompe exactamente

Con las 26 tablas creadas y **vacías**:

- No existe **ningún** `catalog_items`, y por tanto ninguno con `structural_minimum = TRUE`.
- No existe **ninguna** `price_lists` en estado `PUBLISHED`, y por tanto ningún `catalog_prices`.
- `platform_billing_config` no tiene fila, o la tiene con `default_price_list_id` nulo.

En ese estado, **el alta de una empresa no puede crear su contrato inicial**. Y eso choca de frente
con la regla del anexo técnico que dice: *"Toda empresa nace con un contrato. El alta de la empresa y
la creación de su suscripción ocurren en la misma transacción; no existe una empresa sin contrato
vigente."*

El camino que hoy hace el alta —`RegisterUserService` → `JpaDefaultMembershipProvider`, que busca la
membresía marcada como `mandatory` (`026_add_mandatory_to_memberships.xml:15-20`) y con ella decide
qué permisos copiar— **deja de existir** al eliminarse `memberships`. Su sustituto necesita un
catálogo del que partir. Sin catálogo no hay sustituto.

### 6.2 El mínimo estructural que desbloquea el alta

Estas son las filas exactas, en estas tablas exactas, sin las cuales el alta de una empresa no puede
completarse. Es el suelo, no el catálogo comercial:

| Orden | Tabla | Filas mínimas | Contenido obligatorio |
|---|---|---|---|
| 1 | `catalog_items` | **1** | `code = 'CORE'`, `item_type = 'MODULE'`, `structural_minimum = TRUE`, `status = 'ACTIVE'`, `min_quantity = 1`, `max_quantity = 1` |
| 2 | `catalog_item_sub_modules` | **≥ 1** | Al menos una fila que ate `CORE` a un `sub_modules` real. Con el árbol de hoy, los cuatro códigos disponibles son `BRANCH`, `INVENTORY`, `CASH`, `PURCHASES` — y ninguno es "núcleo" en sentido comercial. Esto es exactamente el problema, ver §6.4 |
| 3 | `price_lists` | **1** | `code`, `currency = 'COP'`, `valid_from`, `status = 'PUBLISHED'`, `published_at`, `published_by_system_user_id` |
| 4 | `catalog_prices` | **1 por ciclo** | `(price_list_id, catalog_item_id = CORE, billing_cycle = 'MONTHLY', tier_min = 1)` con `unit_amount`, `tax_rate`, `tax_treatment`. Idealmente también la fila `ANNUAL` |
| 5 | `platform_billing_config` | **1** | `singleton = 1`, `default_price_list_id` apuntando a la de (3), `default_grace_days`, `default_trial_days`, `invoice_day_of_month`, `default_payment_term_days` |

Con esas cinco cosas, el alta puede: crear la empresa, crear su `subscriptions` (`status`
`TRIALING` o `ACTIVE`, `price_list_id` = la de (3)), crear su `subscription_items` con `origin =
'INITIAL'` copiando `unit_amount`/`tax_rate`/`included_quantity` de `catalog_prices`, y derivar
`company_entitlements` desde `catalog_item_sub_modules`. **Cinco filas mínimas en cinco tablas.**

Nada de esto exige el cuestionario del configurador, ni los bundles, ni las dependencias: un alta
puede nacer sin pasar por el asistente.

### 6.3 Recomendación firme: que el arranque en vacío falle, y que falle legible

**Sí, conviene.** Una empresa sin contrato vigente no es un estado degradado: es una cuenta que entra
al sistema, no tiene `company_entitlements`, y **no puede hacer absolutamente nada sin ningún mensaje
que lo explique**. Es el peor modo de fallo posible, porque parece un problema de permisos del
usuario y se investiga en el sitio equivocado.

Las tres capas, de más barata a más cara, y las tres se recomiendan:

1. **Precondición de Liquibase, en el changeset que crea la suscripción inicial** (cuando llegue el
   PR de semillas). Patrón de la casa: `206_unique_open_cash_session_per_employee.xml:8-20` y
   `226_add_unique_active_appointment_slot.xml:47-59` — `<preConditions onFail="HALT" onError="HALT">`
   con un `<sqlCheck>`. La precondición sería:

   ```xml
   <preConditions onFail="HALT" onError="HALT">
       <sqlCheck expectedResult="1">
           SELECT COUNT(*) FROM catalog_items
            WHERE structural_minimum = TRUE AND status = 'ACTIVE' AND enabled = TRUE
       </sqlCheck>
   </preConditions>
   ```

2. **Fallo explícito en el arranque de la aplicación.** Un `ApplicationRunner` o un
   `@EventListener(ApplicationReadyEvent.class)` que consulte el mínimo de §6.2 y, si falta, escriba
   un `ERROR` con el texto exacto de qué falta y en qué tabla. **No** debe impedir el arranque: un
   entorno recién creado tiene que poder levantar para que alguien entre a la consola de plataforma
   y siembre el catálogo. Si impide el arranque, el sistema no se puede arrancar nunca por primera
   vez.

3. **Error de dominio legible en el alta**, y este sí es bloqueante. `RegisterUserService` /
   `CreateCompanyService`, al no encontrar un `catalog_items` núcleo activo, tiene que fallar con un
   código propio del tipo `PLATFORM_CATALOG_NOT_CONFIGURED` mapeado en `GlobalExceptionHandler` a un
   **503** o **409** con mensaje explícito —"la plataforma no tiene catálogo comercial configurado;
   no se puede dar de alta una empresa"— y **nunca** crear la empresa. Es la diferencia entre un
   mensaje que dice qué hacer y una cuenta corrupta que hay que borrar a mano de la base.

**Lo que no se debe hacer bajo ninguna circunstancia:** dejar `subscriptions` opcional "por si
acaso", o permitir que la empresa nazca sin contrato con la idea de asignárselo después. El propio
documento de diseño ya rechaza esa salida para `companies.membership_id` con el argumento correcto:
*"No se deja opcional «por si acaso»: dos fuentes de verdad sobre lo mismo es cómo se corrompe un
modelo."*

### 6.4 El problema de fondo que el aplazamiento deja abierto

Los cuatro submódulos que hoy nacen en migraciones son `BRANCH`, `INVENTORY`, `CASH` y `PURCHASES`
(`184_seed_branch_permissions.xml:34-36`, `191:17-19`, `196:17-19`, `199:17-19`). **Ninguno de ellos
es el núcleo**: no hay submódulo sembrado para clientes, mascotas, agenda, historia clínica o
servicios, que es exactamente lo que Ana necesita. Todo eso "se creó a mano" en los entornos, y por
eso el documento de diseño llama a sembrar el catálogo entero "el paso cero de todo el proyecto".

Aplazar las semillas significa que **ese paso cero sigue sin darse**, y que hasta que se dé:

- El PR de las 26 tablas es puramente estructural y no cambia ningún comportamiento.
- El PR que elimina `memberships` **no puede desplegarse solo**, porque rompe el alta sin dejar
  sustituto en pie.

**Recomendación de secuencia, y es la parte accionable de esta sección:** eliminar `memberships` y
`companies.membership_id` **no** va en el mismo PR que crear las 26 tablas. Va en el PR que trae las
semillas y el nuevo camino de alta, o el sistema queda un tiempo sin poder dar de alta una empresa.
Está detallado en `suscripciones-cambios-existentes.md` §4.

---

## 7. Migración: expand/contract y coste de cada `ALTER`

Con la base vacía, todo esto es gratis. Se escribe igualmente porque el coste de un `ALTER` sobre una
tabla con datos es lo único de esta especificación que no tiene arreglo barato después, y porque el
mismo DDL se va a volver a ejecutar en dev y en prod cuando ya tengan filas.

**Fuente:** manual de MySQL 8.4, *InnoDB online DDL operations* —
<https://dev.mysql.com/doc/refman/8.4/en/innodb-online-ddl-operations.html>. Verificado hoy:

| Operación | In place | Reconstruye la tabla | Permite DML concurrente | Solo metadatos |
|---|---|---|---|---|
| Añadir columna generada **STORED** | **No** | **Sí** | **No** | No |
| Añadir columna generada **VIRTUAL** | Sí | No | Sí | Sí |
| Añadir índice secundario | Sí | No | Sí | No |
| Añadir clave foránea | Sí* | No | Sí | Sí |
| Añadir columna (instant) | Sí | No | Sí** | Sí |

\* `INPLACE` solo con `foreign_key_checks` desactivado; con las comprobaciones activas, solo `COPY`.
\*\* Salvo si la columna es `AUTO_INCREMENT`.

**Tres reglas normativas que salen de esa tabla:**

1. **Toda columna generada `STORED` de este modelo se declara dentro del `CREATE TABLE` inicial, no
   en un `ALTER` posterior.** Añadirla después reconstruye la tabla y **bloquea el DML**: en
   `subscription_billing_documents` con producción viva eso es una ventana de escritura cerrada a
   media facturación. Los changesets `195`, `206`, `210` y `226` la añaden por `ALTER` porque las
   tablas ya existían; aquí no existen, así que no hay excusa para repetirlo.
2. **`balance_amount` es `VIRTUAL`**, no `STORED`. Es la única columna generada del modelo que no
   necesita índice, y `VIRTUAL` la hace *instant* y sin coste de almacenamiento ni de escritura.
3. Cuando dentro de un año haya que añadir una columna a alguna de estas 26 tablas: **columna
   nulable o con default, sin `NOT NULL` de golpe**, backfill **por lotes** con `LIMIT` y pausa entre
   lotes (nunca un `UPDATE` de una sola transacción sobre toda la tabla, que retiene el undo log y
   una de las 10 conexiones de Hikari), y solo después el `NOT NULL`. Criterio:
   [Fowler, *Parallel Change*](https://martinfowler.com/bliki/ParallelChange.html) ·
   [GitLab, *Avoiding downtime in migrations*](https://docs.gitlab.com/development/database/avoiding_downtime_in_migrations/).

**Rollback.** Cada changeset lleva `<rollback>` explícito. Para un `createTable` Liquibase lo
infiere, pero para los bloques `<sql>` (columnas generadas, `CHECK`, FK compuestas) **no**, y hay que
escribirlo a mano. Patrón: `226_add_unique_active_appointment_slot.xml:74-80`.

---

## 8. Escala: por qué aquí no se propone nada "grande"

Escrito para que nadie lea "22 FK compuestas" y concluya que esto pide particionado.

**El terreno real:** una sola instancia MySQL 8.4 `db.t4g.small` (2 GiB), Single-AZ, 20 GiB de gp3,
sin réplicas, sin particiones, `performance_insights_enabled = false` en dev. Hikari con
`maximum-pool-size` 10.

**Volumen proyectado de este bloque** — proyección, no medición, y sale de este supuesto explícito:
**100 clínicas activas, contrato mensual, 8 líneas de contrato de media**.

| Tabla | Filas/año con 100 clínicas | De dónde sale |
|---|---|---|
| `subscriptions` | ~100 | una por clínica |
| `subscription_items` | ~800 + ~400 de altas y bajas | 8 líneas × 100, más el crecimiento |
| `subscription_billing_documents` | ~1.200 | 12 ciclos × 100 |
| `subscription_charges` | ~10.000 | 8 cargos × 12 ciclos × 100 |
| `billing_document_applications` | ~1.500 | una por documento más reversas |
| `company_entitlements` | ~1.500 | 15 submódulos × 100, reescritas, no acumuladas |
| `dunning_events` | ~2.000 | tres avisos por documento vencido |

Total del bloque: **del orden de 20.000 filas al año**. Con `DECIMAL(19,2)` y una docena de columnas
por fila, es del orden de **decenas de MiB al año**, índices incluidos. Sobre 20 GiB.

**Conclusión, y va con número:** este bloque no justifica particionado, ni réplica de lectura, ni
sharding, ni cambio de motor, ni desnormalización, ni durante los próximos cinco años al ritmo de
crecimiento supuesto. Lo que sí justifica —y es lo que esta especificación exige sin negociación— es
que **el tenant vaya delante en cada clave e índice desde el primer changeset**, porque eso cuesta lo
mismo hoy y es carísimo de arreglar después.

Los casos de estudio que suelen citarse aquí describen otra escala: GitHub particionó cuando su
esquema relacional ya no cabía en una instancia
(<https://github.blog/2021-09-27-partitioning-githubs-relational-databases-scale/>), Figma llegó al
sharding **después** de agotar índices y réplicas
(<https://www.figma.com/blog/how-figmas-databases-team-lived-to-tell-the-scale/>), y Notion eligió su
clave de shard por el patrón de acceso —el *workspace*, que es el análogo exacto de nuestro
`company_id`— (<https://www.notion.so/blog/sharding-postgres-at-notion>). De los tres, lo único
aplicable hoy es la lección de Notion, y se aplica **eligiendo bien la clave**, que es precisamente
lo que hace §4.

---

## 9. Aviso sobre mediciones locales

`docker-compose.yml:79` levanta **`mysql:8.0.45`**, mientras que RDS (dev y prod) y Testcontainers
(`AbstractDataJpaTest:52`, `AbstractFullApplicationIT:65`) corren **8.4**. Son versiones mayores
distintas, con diferencias en defaults del optimizador y en comportamiento de DDL.

**Cualquier `EXPLAIN` o medición hecha contra el compose local sobre estas tablas hay que declararla
como "medido en 8.0.45, no reproduce necesariamente RDS".** Para validar un plan de ejecución de
verdad, el banco correcto es un Testcontainer `mysql:8.4`.

---

## 10. Fuentes citadas

| URL | Qué sostiene aquí |
|---|---|
| <https://dev.mysql.com/doc/refman/8.4/en/create-table-check-constraints.html> | Que un `CHECK` no puede referenciar una columna `AUTO_INCREMENT` ni convivir con acciones referenciales de FK (§1.9) |
| <https://dev.mysql.com/doc/refman/8.4/en/create-table-generated-columns.html> | `VIRTUAL` vs `STORED`, determinismo obligatorio de la expresión, referencias entre columnas generadas, y que una FK no puede referenciar una columna generada `VIRTUAL` |
| <https://dev.mysql.com/doc/refman/8.4/en/create-table-foreign-keys.html> | El requisito de índice en padre e hijo con las columnas como prefijo por la izquierda, la igualdad de charset/collation, y el default `NO ACTION` (§1.10, §4.2) |
| <https://dev.mysql.com/doc/refman/8.4/en/innodb-online-ddl-operations.html> | Que añadir una columna generada `STORED` reconstruye la tabla y no permite DML concurrente (§7) |
| <https://dev.mysql.com/doc/refman/8.4/en/multiple-column-indexes.html> | La regla del prefijo por la izquierda, que es lo que hace que el orden de columnas de §4.2 importe |
| <https://use-the-index-luke.com/no-offset> | Paginación por keyset frente a `OFFSET` creciente en los listados de documentos |
| <https://docs.citusdata.com/en/stable/use_cases/multi_tenant.html> | La doctrina "el tenant va primero en la clave" (criterio de modelado; el motor es otro) |
| <https://learn.microsoft.com/en-us/azure/architecture/guide/multitenant/considerations/tenancy-models> | El cuadro de modelos de tenencia que sitúa el esquema compartido con discriminador |
| <https://martinfowler.com/bliki/ParallelChange.html> | Expand/contract para cambios futuros de esquema |
| <https://docs.gitlab.com/development/database/avoiding_downtime_in_migrations/> | Backfill por lotes en vez de un `UPDATE` masivo |
| <https://pragprog.com/titles/bksap1/sql-antipatterns-volume-1/> | Bibliografía: los antipatrones nombrados en `suscripciones-tablas.md` (*Entity-Attribute-Value*, *Polymorphic Associations*) |
| <https://vladmihalcea.com/optimistic-vs-pessimistic-locking/> | Criterio de cuándo `@Version` y cuándo un bloqueo más fuerte (§2.1, códigos `E6_YA_PROTEGIDO`) |

**Fuentes NO consultadas y por qué:** ninguna base de datos viva. Regla permanente del proyecto: las
auditorías y especificaciones se verifican leyendo código y changesets, no consultando dev. Todo lo
que en este documento se afirma sobre el esquema actual sale de un `grep` sobre
`src/main/resources/db/changelog/migrations/` o de un fichero Java citado con línea.
