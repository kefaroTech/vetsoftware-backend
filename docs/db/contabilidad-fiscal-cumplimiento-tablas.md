# Contabilidad, fiscal propio y cumplimiento — especificación de esquema

**Para `db-migrations`.** Cada tabla lleva sus columnas con tipo y nulabilidad, sus restricciones
con nombre, sus índices y el orden respecto a las claves foráneas. Donde hay una decisión no
obvia va el motivo. **No queda nada por decidir**: si algo no está aquí, es un defecto de esta
especificación, no una invitación a inventar.

Fuente de diseño: `models/modelo-datos-suscripciones.html` (capas M, N, O, P y «La ficha de
construcción»). Donde el documento se contradice consigo mismo o contradice a un changeset ya
aplicado, esta especificación decide y lo dice.

---

## 1. Alcance

**Diecisiete tablas nuevas y una que ya existe.**

| Bloque | Tablas |
|---|---|
| Contabilidad (M/N) | `accounting_accounts` · `account_mappings` · `revenue_recognition_lines` · `accounting_exports` — y `accounting_periods`, **que ya existe** (changeset `331`) |
| Fiscal propio | `tax_returns` · `supplier_withholdings` |
| Referencia con vigencia | `uvt_values` · `smmlv_values` · `vat_filing_periods` · `public_holidays` · `legal_document_versions` |
| Cumplir y medir (O) | `security_incidents` + `security_incident_companies` · `external_invoicing_outages` + `external_invoicing_outage_companies` · `company_usage_events` · `company_activity_months` |

**Nombres.** El encargo las llamó `revenue_recognition`, `vat_periodicity`, `annual_uvt_values`,
`colombian_holidays`, `usage_facts`, `company_monthly_activity`. Los nombres canónicos son los
del documento maestro y los de la lista de exenciones de ArchUnit —
`revenue_recognition_lines`, `vat_filing_periods`, `uvt_values`, `public_holidays`,
`company_usage_events`, `company_activity_months` — y son los que se usan aquí. Un segundo
vocabulario para la misma tabla es exactamente el defecto que el documento persigue.

**Fuera de alcance:** `payment_refunds`, `billing_document_status_history`, `gateway_settlements`,
`data_*`, `cancellation_requests`, `company_service_costs`, `acquisition_spend`,
`customer_credit_*`, `legal_document_acceptances`. Se citan solo donde una FK las toca.

---

## 2. El terreno, verificado contra el árbol

No son preferencias: son hechos comprobados en changesets aplicados y en el manual de MySQL 8.4.

### 2.1 Tipos y colaciones que ya están fijados y **no se pueden reabrir**

| Concepto | Tipo exacto | De dónde sale |
|---|---|---|
| Periodo contable | `CHAR(7) CHARACTER SET ascii COLLATE ascii_bin` | `331_create_accounting_periods.xml` (`period_key`) y `330_create_external_invoice_reconciliations.xml:66,88` (`posting_period`). **Toda columna `posting_period` nueva debe ser idéntica**: cruzar dos colaciones impide crear la clave foránea (errno 3780), no solo desactiva el índice |
| Periodo fiscal | `VARCHAR(10) CHARACTER SET ascii COLLATE ascii_bin`, columna llamada **`fiscal_period_key`** | `328_create_withholding_certificates.xml` y `329_create_document_withholdings.xml`. La ficha del modelo lo llama `tax_period_key`; **manda el changeset aplicado** — dos tablas ya lo llevan y un tercer nombre es la divergencia silenciosa que el propio documento caza |
| Tarifa | `DECIMAL(9,6)`, columna llamada **`rate_percent`** | `317_create_withholding_rate_rules.xml`, `328`, `329`. La ficha decía `rate DECIMAL(7,4)`; el repositorio ya subió la precisión y puso la unidad en el nombre. El ICA de Bogotá es 6,9 **por mil**: con `DECIMAL(7,4)` sobre porcentaje cabe (0,6900), pero el nombre sin unidad es lo que produce la lectura equivocada |
| Municipio | `VARCHAR(5) CHARACTER SET ascii COLLATE ascii_bin`, FK a `cities.dane_code` | `315_align_city_dane_code_for_withholdings.xml`, `329` |
| Centinela de municipio | `municipality_key` generada `STORED` = `COALESCE(municipality_code, '-')` | `329:` patrón literal de la casa |
| Marcador de unicidad condicional | generada `STORED` = `CASE WHEN <vive> THEN CONCAT(...) ELSE NULL END`, con `UNIQUE` propio | `317:87-100` (`current_rule_marker`) |
| Año gravable | `SMALLINT` + `CHECK (fiscal_year BETWEEN 2020 AND 2100)` | `328`, `329` |
| Dinero | `DECIMAL(19,2)` | todo el bloque de dinero |
| Instante | `DATETIME(6)` | regla de la ficha; `331` |
| Huella | `CHAR(64) CHARACTER SET ascii COLLATE ascii_bin` | convención de la ficha |
| Clave auxiliar de tenant | `UNIQUE (company_id, id)` con nombre `uq_<tabla>_company_id` | 17 tablas ya la llevan |
| Booleano | `type="BOOLEAN"` en Liquibase, **nunca `TINYINT(1)`** | `application.yml:85` (`preferred_boolean_jdbc_type: TINYINT`) |

### 2.2 Tres hechos del motor que gobiernan todo lo que sigue

**Uno · un `CHECK` que evalúa a nulo acepta la fila.** Manual de MySQL 8.4, *CHECK Constraints*:

> «`expr` specifies the constraint condition as a boolean expression that must evaluate to `TRUE`
> or `UNKNOWN` (for `NULL` values) for each row of the table. If the condition evaluates to
> `FALSE`, it fails and a constraint violation occurs.»

Consecuencia operativa: **en esta especificación, toda columna nulable con lista cerrada lleva su
`CHECK` escrito con las dos ramas** (`(A AND x IS NOT NULL) OR (NOT A AND x IS NULL)`), nunca un
`x IN (...)` suelto. Y el mismo manual: en un `CHECK` **no se permiten** funciones no
deterministas (`NOW()`, `CURRENT_USER()`), subconsultas ni columnas de otras tablas — por eso
ningún plazo se comprueba contra el reloj. Y **no se permite una columna `AUTO_INCREMENT`**: no
existe forma de escribir `CHECK (corrects_return_id <> id)`.

**Dos · añadir una columna generada `STORED` reconstruye la tabla.** Manual de MySQL 8.4,
*InnoDB Online DDL Operations*: para `ADD COLUMN` de una columna `STORED` → *Instant* **No**,
*In Place* **No**, *Rebuilds Table* **Yes** («ADD COLUMN is not an in-place operation for stored
columns … because the expression must be evaluated by the server»). **Todo marcador nace con su
tabla.** Añadirlo después no es un `ALTER` barato: es una copia completa con bloqueo de
escritura.

**Tres · un índice secundario se añade en sitio y una clave foránea no siempre.** Mismo manual:
crear un índice secundario es *In Place*, no reconstruye y **permite DML concurrente**; añadir una
clave foránea es *In Place* solo con `foreign_key_checks` desactivado — «Otherwise, only the
`COPY` algorithm is supported». Por eso los prerrequisitos del §3 son baratos y por eso la regla
de la casa («el índice antes que la clave, con nombre») se respeta sin excepción aquí.

### 2.3 Las reglas de la casa que se aplican sin repetirlas tabla por tabla

- **PK siempre `id BIGINT AUTO_INCREMENT`**, también en las tablas puente.
- **Todo campo nace obligatorio.** Solo admite vacío el que representa un hecho que aún no ha
  ocurrido, o la rama no elegida de un arco exclusivo.
- **Defectos: solo tres.** Estado inicial de una máquina de estados, marca de activo (`enabled`),
  y contador de concurrencia (`version = 0`). **Ningún importe, ninguna fecha, ninguna FK** lleva
  defecto.
- **`ON DELETE RESTRICT` y `ON UPDATE RESTRICT` en todas las claves foráneas**, sin excepción.
- **Nombres:** `chk_`, `uq_`, `fk_`, `ix_` + tabla + lo que restringen. Ninguna restricción anónima.
- **`created_date DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6)`** salvo en las dos puentes.
- **`enabled`** no lo llevan: los documentos de dinero, las bitácoras probatorias y las puentes.
- **`version BIGINT NOT NULL DEFAULT 0`** en el primer changeset de cada tabla que la lleve —
  nunca después: añadirla luego obliga a pasar por la lista de exenciones para algo que no es una
  excepción, y esa lista se poda sola.
- **Colación exacta** (`CHARACTER SET ascii COLLATE ascii_bin`) declarada **columna por columna**
  en un bloque `<sql>` posterior al `createTable`, como hacen `328`–`332`.

---

## 3. Prerrequisitos — un changeset antes que todo lo demás

Cinco unicidades auxiliares sobre tablas que ya existen. **Sin ellas, cinco claves foráneas
compuestas de este bloque no se pueden crear.**

| Tabla existente | Restricción a añadir | Para qué |
|---|---|---|
| `animals` (`031`) | `uq_animals_company_id UNIQUE (company_id, id)` | destino de `company_usage_events.usage_animal_id` |
| `owners` (`030`) | `uq_owners_company_id UNIQUE (company_id, id)` | destino de `usage_owner_id` |
| `appointments` (`174`) | `uq_appointments_company_id UNIQUE (company_id, id)` | destino de `usage_appointment_id` |
| `electronic_documents` (`121`) | `uq_electronic_documents_company_id UNIQUE (company_id, id)` | destino de `usage_electronic_document_id` |
| `limit_dimensions` (`300`) | `uq_limit_dimensions_id_code UNIQUE (id, code)` | destino de la FK compuesta que copia el código del eje a `company_usage_events` |

**Coste real, medido contra el manual:** `ADD UNIQUE INDEX` es *In Place*, **no reconstruye** la
tabla y **permite DML concurrente**. Y no hay riesgo de datos: `(company_id, id)` es únicamente
cierto por construcción, porque `id` ya es clave primaria. La unicidad no puede fallar.

`<rollback>`: `dropUniqueConstraint` de las cinco.

> **Esto es la segunda excepción a «no se toca lo existente».** La primera fue
> `315_align_city_dane_code_for_withholdings.xml`, por el mismo motivo exacto: sin alinear la
> columna del padre, la clave foránea del hijo no se puede crear. Va en el cuerpo del changeset
> como comentario.

---

## 4. Orden de creación

`db-migrations` asigna los números (el último aplicado es `339`). **El orden entre bloques no se
puede alterar**; dentro de cada bloque es libre.

| # | Changeset | Depende de |
|---|---|---|
| 0 | **Prerrequisitos** (§3) | `030`, `031`, `121`, `174`, `300` |
| 1 | `accounting_accounts` | — (autorreferencia por `parent_code`) |
| 2 | `account_mappings` | 1 (tres FK contra `accounting_accounts.code`), `catalog_items` |
| 3 | `revenue_recognition_lines` | `331` (`accounting_periods`), `subscription_charges`, `companies` |
| 4 | `accounting_exports` | `331`, `system_users` |
| 5 | **Disparadores de contabilidad** (§7) | 3, 4, `330`, `331` |
| 6 | `uvt_values` | — |
| 7 | `smmlv_values` | — |
| 8 | `vat_filing_periods` | — |
| 9 | `public_holidays` | — |
| 10 | `tax_returns` | 8 (FK compuesta a la periodicidad), `cities`, `system_users` |
| 11 | `supplier_withholdings` | `cities` |
| 12 | `legal_document_versions` + su disparador de inmutabilidad | `system_users` |
| 13 | `company_usage_events` | 0, `limit_dimensions`, `subscription_charges`, `companies` |
| 14 | `company_activity_months` | `companies` |
| 15 | `security_incidents` | — |
| 16 | `security_incident_companies` | 15, `companies` |
| 17 | `external_invoicing_outages` | — |
| 18 | `external_invoicing_outage_companies` | 17, `companies` |
| 19 | **Siembra mínima** (§9) | 6, 7, 8, 9, 1 |

> ### Aviso de empaquetado que rompe el build si se ignora
>
> **Las tablas sin `company_id` y las tablas con `company_id` no pueden compartir *slice*.** Una
> FK a `companies` dentro de una funcionalidad de catálogo dispara las cuatro reglas duras de
> aislamiento (`TENANT_DEFENSA_EN_PROFUNDIDAD`, `CARGA_POR_ID_ACOTADA_POR_EMPRESA`,
> `OPERACIONES_POR_ID_SIN_EMPRESA_SOLO_SYSTEM`, `MUTACIONES_SQL_ACOTADAS_POR_EMPRESA`) sobre
> **toda** la funcionalidad, incluidas las tablas globales que no tienen empresa que acotar.
>
> - **Sin `company_id`** (nacen en funcionalidad de plataforma, autorización de sistema):
>   `accounting_accounts`, `account_mappings`, `accounting_exports`, `tax_returns`,
>   `supplier_withholdings`, `uvt_values`, `smmlv_values`, `vat_filing_periods`,
>   `public_holidays`, `legal_document_versions`, `security_incidents`,
>   `external_invoicing_outages`.
> - **Con `company_id`** (funcionalidad aparte): `revenue_recognition_lines`,
>   `company_usage_events`, `company_activity_months`, `security_incident_companies`,
>   `external_invoicing_outage_companies`.
>
> Esto es de esquema solo a medias — la separación real la hace `backend-feature` al empaquetar —
> pero se decide aquí porque el orden de los changesets es lo que sugiere el empaquetado.

---

## 5. Las tablas, una por una

Convenciones de lectura: **N** = `NOT NULL`, **n** = nulable. Toda FK es `RESTRICT`/`RESTRICT`.
El bloque `<sql>` posterior al `createTable` es donde van colación, columnas generadas, `UNIQUE`
y `CHECK`, igual que en `328`–`331`.

---

### 5.1 `accounting_accounts` — el catálogo de cuentas *(global, sin empresa)*

Nace **primero de todo el bloque**: es el destino de las tres claves foráneas de
`account_mappings` y sin él ese changeset no se puede aplicar.

| Columna | Tipo | N/n | Regla |
|---|---|---|---|
| `id` | `BIGINT` AI | N | PK |
| `code` | `VARCHAR(10)` ascii_bin | N | `uq_accounting_accounts_code UNIQUE (code)`. Comparación exacta por ser identificador ajeno: con la colación heredada, `1105` y `1105` con relleno serían la misma cuenta, y dos códigos que difieran en el caso también |
| `name` | `VARCHAR(120)` | N | |
| `account_class` | `VARCHAR(20)` | N | `chk_accounting_accounts_class CHECK (account_class IN ('ASSET','LIABILITY','EQUITY','REVENUE','EXPENSE','COST'))` — decide el signo del asiento y no se deduce del código sin conocer la norma |
| `parent_code` | `VARCHAR(10)` ascii_bin | n | FK a sí misma contra `code`. Vacío **solo** en la raíz |
| `account_level` | `TINYINT` | N | `chk_accounting_accounts_level CHECK (account_level IN (1,2,4,6))` — clase, grupo, cuenta, subcuenta. **`level` a secas no**: es palabra clave de MySQL 8 y obliga a comillas invertidas en cada consulta |
| `postable` | `BOOLEAN` | N | `chk_accounting_accounts_postable CHECK (postable = FALSE OR account_level = 6)` — solo el último nivel admite asiento. Sin esto se asienta contra un grupo y el balance de prueba deja de cuadrar por arrastre |
| `requires_third_party` | `BOOLEAN` | N | Sin defecto: la siembra lo dice. Cartera y proveedores exigen tercero identificado; se comprueba al asentar, no al crear la cuenta |
| `valid_from` | `DATE` | N | |
| `valid_to` | `DATE` | n | `chk_accounting_accounts_validity CHECK (valid_to IS NULL OR valid_to > valid_from)` |
| `created_date` · `enabled` · `version` | | N | Catálogo que muta: lleva las tres |

**El `CHECK` que sí muerde sobre una columna nulable:**

```sql
chk_accounting_accounts_parent
  CHECK ((account_level = 1 AND parent_code IS NULL)
      OR (account_level > 1 AND parent_code IS NOT NULL))
```

Sin la segunda rama escrita, una subcuenta huérfana entra en silencio: `NULL` no está «fuera de la
lista», está indefinido, y el motor acepta.

**Índices — antes de la clave foránea:**

| Nombre | Columnas | Sirve a |
|---|---|---|
| `ix_accounting_accounts_plan` | `(account_class, valid_to)` | resolver el plan vigente por clase |
| `ix_accounting_accounts_parent` | `(parent_code)` | armar el árbol; **y es el índice de la FK autorreferente** — se crea antes que ella o InnoDB genera uno anónimo que nadie puede nombrar en un rollback |

**Decisión mía · el código es único globalmente, no por vigencia.** La cuenta tiene `valid_from` /
`valid_to`, pero `code` es único a secas: un código no puede significar dos cosas distintas en dos
épocas. Si el plan cambia el significado de un código, se abre un código nuevo. *Coste de
cambiarlo después:* alto — habría que sustituir el `UNIQUE` por un marcador generado y revisar
todas las filas de `account_mappings` que apuntan por código.

---

### 5.2 `accounting_periods` — **ya existe, no se toca**

Construida en `331_create_accounting_periods.xml`. Lo que hay que saber para lo que sigue:

- `period_key CHAR(7)` ascii_bin, `uq_accounting_periods_period UNIQUE (period_key)`, con
  `chk_accounting_periods_key CHECK (period_key REGEXP '^[0-9]{4}-(0[1-9]|1[0-2])$')`.
- Estados: **`OPEN` · `SOFT_CLOSED` · `LOCKED`**.
- Ya lleva `version` y **no lleva `enabled`**.
- Ya existe `fk_eir_posting_period`, de `external_invoice_reconciliations.posting_period` hacia
  `accounting_periods.period_key`.

> **Contradicción del documento maestro, resuelta.** La sección «Los códigos de cada lista
> cerrada» dice `OPEN · CLOSING · CLOSED` para `accounting_periods`; la ficha de la capa M y el
> changeset aplicado dicen `OPEN · SOFT_CLOSED · LOCKED`. **Manda el changeset aplicado.**
> Ninguna tabla nueva puede escribir `CLOSING` ni `CLOSED`.

Lo que le falta y **no es de esta especificación**: la `@Entity` de Java. Sin ella la tabla existe
y nadie la lee. `ddl-auto: validate` no rompe por una tabla sin entidad — valida entidades contra
el esquema, no al revés —, así que esto no tumba el arranque; simplemente el cierre contable no
existe todavía como operación. Es trabajo de `backend-feature`.

---

### 5.3 `account_mappings` — qué cuenta mueve cada cosa *(global, sin empresa)*

| Columna | Tipo | N/n | Regla |
|---|---|---|---|
| `id` | `BIGINT` AI | N | PK |
| `mapping_kind` | `VARCHAR(25)` | N | doce valores, abajo |
| `mapping_key` | `VARCHAR(60)` ascii_bin | N | La subclave dentro de la clase: el código del artículo para `REVENUE`, la tarifa (`19`, `5`, `0`) para `VAT_PAYABLE`, el tipo y municipio para `WITHHOLDING`, el código del banco para `BANK`. **Nunca nula**: donde no hay subclave, la siembra escribe `-`. Una columna nulable dentro de una unicidad no restringe nada |
| `catalog_item_id` | `BIGINT` | n | FK a `catalog_items(id)` |
| `charge_type` | `VARCHAR(20)` | n | |
| `tax_treatment` | `VARCHAR(20)` | n | |
| `debit_account_code` | `VARCHAR(10)` ascii_bin | N | FK a `accounting_accounts(code)` |
| `credit_account_code` | `VARCHAR(10)` ascii_bin | N | FK a `accounting_accounts(code)` |
| `deferred_account_code` | `VARCHAR(10)` ascii_bin | n | FK a `accounting_accounts(code)`. **Clave foránea de verdad, no texto suelto** — cierra la asimetría que el documento señala: sus dos hermanas ya lo eran y esta no |
| `valid_from` | `DATE` | N | |
| `valid_to` | `DATE` | n | |
| `created_date` · `enabled` · `version` | | N | Se cierra su vigencia: lleva versión |

**Lista cerrada — la del documento, resuelta.** El documento da **dos listas incompatibles** para
`mapping_kind`: la prosa de la capa N (`CATALOG_ITEM · TAX_OUTPUT · WITHHOLDING · …`) y la sección
«Los códigos de cada lista cerrada». **Manda la segunda**, que es la posterior y la que corrigió
la falta de cartera:

```sql
chk_account_mappings_kind CHECK (mapping_kind IN (
  'RECEIVABLE','DEFERRED_REVENUE','REVENUE','VAT_PAYABLE','VAT_CREDITABLE',
  'CASH_IN_TRANSIT','BANK','GATEWAY_FEE','WITHHOLDING_ASSET','FINANCIAL_TAX',
  'PENALTY_REVENUE','CUSTOMER_CREDIT'))
```

**Los tres centinelas y los dos marcadores.** El documento propone la unicidad
`(mapping_kind, mapping_key, catalog_item_id, charge_type, tax_treatment, valid_from)` con **tres
columnas nulables dentro**. En un índice único dos vacíos no chocan: esa unicidad no restringe
nada para todos los mapeos que no son de artículo — que son nueve de las doce clases. Es el mismo
defecto que el propio documento caza en el contador de cupo y en las tarifas de retención, y aquí
no lo vio.


```sql
ADD COLUMN catalog_item_key BIGINT
    GENERATED ALWAYS AS (COALESCE(catalog_item_id, 0)) STORED,
ADD COLUMN charge_type_key VARCHAR(20) CHARACTER SET ascii COLLATE ascii_bin
    GENERATED ALWAYS AS (COALESCE(charge_type, '-')) STORED,
ADD COLUMN tax_treatment_key VARCHAR(20) CHARACTER SET ascii COLLATE ascii_bin
    GENERATED ALWAYS AS (COALESCE(tax_treatment, '-')) STORED,
ADD COLUMN current_mapping_marker VARCHAR(150) CHARACTER SET ascii COLLATE ascii_bin
    GENERATED ALWAYS AS (
        CASE WHEN valid_to IS NULL
             THEN CONCAT(mapping_kind, '|', mapping_key, '|',
                         COALESCE(catalog_item_id, 0), '|',
                         COALESCE(charge_type, '-'), '|',
                         COALESCE(tax_treatment, '-'))
             ELSE NULL END) STORED,
ADD CONSTRAINT uq_account_mappings_case
    UNIQUE (mapping_kind, mapping_key, catalog_item_key,
            charge_type_key, tax_treatment_key, valid_from),
ADD CONSTRAINT uq_account_mappings_current
    UNIQUE (current_mapping_marker),
```

`uq_account_mappings_current` **no estaba en el documento** y es lo que impide dos mapeos vigentes
para el mismo supuesto — el gemelo exacto de `uq_withholding_rate_rules_current` (`317:99-100`).
Sin él la consulta devuelve dos cuentas y el asiento toma la primera que llegue.

Los otros cuatro `CHECK`:

```sql
chk_account_mappings_key      CHECK (mapping_key <> ''),
chk_account_mappings_refine   CHECK (mapping_kind IN ('REVENUE','DEFERRED_REVENUE')
                                     OR (catalog_item_id IS NULL AND charge_type IS NULL
                                         AND tax_treatment IS NULL)),
chk_account_mappings_deferred CHECK (deferred_account_code IS NULL
                                     OR mapping_kind IN ('REVENUE','DEFERRED_REVENUE')),
chk_account_mappings_validity CHECK (valid_to IS NULL OR valid_to > valid_from)
```

El segundo es la respuesta a «el afinado, cuando el hecho sí viene de algo vendido»: el impuesto
generado, la comisión o el banco no tienen artículo, y ahora la base lo impide en vez de confiarlo.

**Índices — los cinco van antes de sus cuatro claves foráneas:**

| Nombre | Columnas | Sirve a |
|---|---|---|
| `ix_account_mappings_current` | `(mapping_kind, valid_to)` | resolver el vigente por clase |
| `ix_account_mappings_item` | `(catalog_item_id)` | FK a `catalog_items` |
| `ix_account_mappings_debit` | `(debit_account_code)` | FK |
| `ix_account_mappings_credit` | `(credit_account_code)` | FK |
| `ix_account_mappings_deferred` | `(deferred_account_code)` | FK |

---

### 5.4 `revenue_recognition_lines` — cuánto se ganó de verdad

| Columna | Tipo | N/n | Regla |
|---|---|---|---|
| `id` | `BIGINT` AI | N | PK |
| `company_id` | `BIGINT` | N | |
| `charge_id` | `BIGINT` | N | |
| `period_key` | `CHAR(7)` ascii_bin | N | El mes al que se **imputa**. `chk_rrl_period_key`, mismo `REGEXP` mensual que `chk_accounting_periods_key` |
| `posting_period` | `CHAR(7)` ascii_bin | N | El periodo contable en que se **registra**. FK a `accounting_periods(period_key)` |
| `recognized_amount` | `DECIMAL(19,2)` | N | Puede ser negativo: una corrección es otra fila que compensa. `chk_rrl_amount CHECK (recognized_amount <> 0)` |
| `method` | `VARCHAR(25)` | N | `chk_rrl_method CHECK (method IN ('STRAIGHT_LINE_DAYS','POINT_IN_TIME','OVER_CUSTOMER_LIFE'))`. El tercero queda declarado y no se implementa: no se cobra implantación |
| `created_date` | `DATETIME(6)` | N | |

**Sin `enabled`** (documento de dinero) y **sin `version`**: la ficha la lista entre las
veintitrés exentas. Entrada literal para `ENTIDADES_EXENTAS_DE_VERSION`:

```java
exenta("RevenueRecognitionLineJpaEntity", E1_APPEND_ONLY,
        "solo se agrega: un reconocimiento mal calculado se corrige con otra fila de signo"
                + " contrario, nunca encima; ningun caso de uso reescribe el importe")
```

**La invariante «nunca hacia atrás», puesta en la base:**

```sql
chk_rrl_not_backwards CHECK (posting_period >= period_key)
```

Con formato `AAAA-MM` y colación `ascii_bin`, la comparación lexicográfica **es** la cronológica.
Un hecho tardío se puede registrar en un periodo posterior o en el mismo, jamás en uno anterior —
que es lo que hace que el informe de marzo siga dando lo que se declaró. De las cuatro reglas de
periodo del encargo, **esta es la única que la base puede imponer sola**; las otras tres van con
disparador (§6).

**La unicidad, corregida.** El documento propone `(charge_id, period_key)`. Con ella **la
corrección es inescribible**: la fila que compensa lleva el mismo cargo y el mismo mes, y choca.
Es el mismo defecto que el propio documento caza en el libro del saldo a favor — «un libro que no
deja escribir su propia corrección».

```sql
ADD CONSTRAINT uq_rrl_recognition
    UNIQUE (company_id, charge_id, period_key, posting_period)
```

Dos filas del mismo cargo y del mismo mes solo caben si se registraron en **periodos contables
distintos** — que es la regla de negocio literal: no se corrige dentro de un periodo, se compensa
en el primero abierto. Y el reintento del proceso nocturno dentro del mismo periodo choca, que es
la llave antiduplicados que faltaba. La empresa va delante para que la unicidad **sea** el índice
de la clave foránea compuesta, en vez de pagar dos objetos por lo mismo.

**Índices y claves foráneas** (índice siempre antes de su clave):

| Nombre | Columnas | Sirve a |
|---|---|---|
| `uq_rrl_recognition` | `(company_id, charge_id, period_key, posting_period)` | la unicidad **y** el índice de `fk_rrl_charge` y de `fk_rrl_company` |
| `ix_rrl_posting` | `(company_id, posting_period)` | el ingreso del cierre, por clínica |
| `ix_rrl_posting_fk` | `(posting_period)` | índice de `fk_rrl_posting_period` |
| `ix_rrl_period` | `(posting_period, period_key)` | **barrido de plataforma**: el cierre mensual de todas las clínicas |

```
fk_rrl_charge          (company_id, charge_id) -> subscription_charges(company_id, id)
fk_rrl_company         (company_id)            -> companies(id)
fk_rrl_posting_period  (posting_period)        -> accounting_periods(period_key)
```

`uq_subscription_charges_company_id` ya existe, así que la compuesta se crea sin prerrequisitos.

> `ix_rrl_period` no lleva la empresa delante **a propósito**: ponérsela lo haría inútil para el
> cierre. Eso dispara `LISTADOS_SIN_EMPRESA_SOLO_SYSTEM`, y el caso de uso que lo consulta nace
> en funcionalidad de plataforma cerrado a `hasRole('SYSTEM')`, con su hermano acotado por
> empresa para lo que el cliente necesite ver. Declararlo aquí **no exime**: la regla recorre los
> casos de uso, no los documentos.

---

### 5.5 `accounting_exports` — el asiento resumen mensual *(global, sin empresa)*

Doce filas al año. Sustituye al diario y a sus renglones.

| Columna | Tipo | N/n | Regla |
|---|---|---|---|
| `id` | `BIGINT` AI | N | PK |
| `period_key` | `CHAR(7)` ascii_bin | N | FK a `accounting_periods(period_key)` |
| `export_kind` | `VARCHAR(25)` | N | `chk_accounting_exports_kind CHECK (export_kind IN ('JOURNAL_SUMMARY','THIRD_PARTY_REPORT','VAT_SUPPORT'))` — **decisión mía**, §8 |
| `attempt_number` | `INT` | N | `chk_accounting_exports_attempt CHECK (attempt_number >= 1)` |
| `status` | `VARCHAR(20)` | N | defecto `'GENERATED'`; `CHECK (status IN ('GENERATED','DELIVERED','REJECTED','SUPERSEDED'))` |
| `generated_at` | `DATETIME(6)` | N | |
| `generated_by_system_user_id` | `BIGINT` | N | FK a `system_users(id)` |
| `total_debit` · `total_credit` | `DECIMAL(19,2)` | N | |
| `totals_hash` | `CHAR(64)` ascii_bin | N | `CHECK (totals_hash REGEXP '^[0-9a-f]{64}$')` |
| `file_ref` | `VARCHAR(255)` | N | |
| `delivered_at` · `rejected_at` | `DATETIME(6)` | n | hechos que aún no han ocurrido |
| `rejection_reason` | `VARCHAR(255)` | n | |
| `created_date` · `version` | | N | «Se resuelve»: recibe su desenlace después. **Sin `enabled`** |

**La partida doble, comprobada por el motor sobre dos números:**

```sql
chk_accounting_exports_balanced CHECK (total_debit = total_credit AND total_debit >= 0)
```

Es la única invariante contable que el documento declaraba imposible de imponer. Al eliminar el
diario pasa a ser trivial: **que cuadren se comprueba antes de exportar, no después**.

**Coherencia de estado — con todas las ramas escritas** (el patrón de `331`):

```sql
chk_accounting_exports_lifecycle CHECK (
     (status = 'GENERATED' AND delivered_at IS NULL AND rejected_at IS NULL
                           AND rejection_reason IS NULL)
  OR (status = 'DELIVERED' AND delivered_at IS NOT NULL AND rejected_at IS NULL
                           AND rejection_reason IS NULL AND delivered_at >= generated_at)
  OR (status = 'REJECTED'  AND rejected_at IS NOT NULL AND rejection_reason IS NOT NULL
                           AND rejected_at >= generated_at)
  OR (status = 'SUPERSEDED'))
```

**Unicidad y marcador:**

```sql
ADD COLUMN current_export_marker VARCHAR(40) CHARACTER SET ascii COLLATE ascii_bin
    GENERATED ALWAYS AS (
        CASE WHEN status IN ('GENERATED','DELIVERED')
             THEN CONCAT(period_key, '|', export_kind)
             ELSE NULL END) STORED,
ADD CONSTRAINT uq_accounting_exports_attempt
    UNIQUE (period_key, export_kind, attempt_number),
ADD CONSTRAINT uq_accounting_exports_current
    UNIQUE (current_export_marker),
```

La primera es la del documento: el número de intento es lo que libera rehacer un fichero
rechazado sin abrir la puerta a exportar dos veces lo mismo. La segunda **no estaba** y es la que
hace que «rehacer» signifique algo — un solo fichero vivo por mes y clase; rechazado o reemplazado
libera el hueco.

**Índices** (los dos últimos, antes de sus claves):

| Nombre | Columnas | Sirve a |
|---|---|---|
| `ix_accounting_exports_status` | `(period_key, status)` | la bandeja del mes |
| `ix_accounting_exports_generated` | `(generated_at)` | **barrido de plataforma** |
| `ix_accounting_exports_period_fk` | `(period_key)` | `fk_accounting_exports_period` |
| `ix_accounting_exports_generated_by` | `(generated_by_system_user_id)` | `fk_accounting_exports_generated_by` |

---

### 5.6 `tax_returns` — lo que se declaró, y hasta cuándo pueden revisarlo

**Sin `company_id`: son declaraciones de VetSoftware, no de la clínica.** Cero superficie de
cliente.

| Columna | Tipo | N/n | Regla |
|---|---|---|---|
| `id` | `BIGINT` AI | N | PK |
| `tax_kind` | `VARCHAR(20)` | N | `CHECK (tax_kind IN ('INCOME_TAX','VAT','ICA','WITHHOLDING'))` — los tres primeros son los mismos literales del tipo de retención, a propósito |
| `fiscal_year` | `SMALLINT` | N | `CHECK (fiscal_year BETWEEN 2020 AND 2100)` |
| `fiscal_period_key` | `VARCHAR(10)` ascii_bin | N | La forma la impone el `CHECK` según el tipo, abajo |
| `sequence_number` | `INT` | N | 1 la inicial, 2+ cada corrección. `CHECK (sequence_number >= 1)` |
| `municipality_code` | `VARCHAR(5)` ascii_bin | n | FK a `cities(dane_code)` |
| `municipality_key` | `VARCHAR(5)` ascii_bin | N | generada `STORED` = `COALESCE(municipality_code, '-')` |
| `vat_frequency` | `VARCHAR(15)` | n | Copiada de `vat_filing_periods`, solo para IVA |
| `vat_frequency_year` | `SMALLINT` | n | generada `STORED` = `CASE WHEN tax_kind = 'VAT' THEN fiscal_year ELSE NULL END` |
| `status` | `VARCHAR(20)` | N | defecto `'DRAFT'`; `CHECK (status IN ('DRAFT','FILED','CORRECTED','ANNULLED'))` — **decisión mía**, §8 |
| `filed_at` | `DATETIME(6)` | n | |
| `filed_by_system_user_id` | `BIGINT` | n | FK a `system_users(id)` |
| `receipt_ref` | `VARCHAR(100)` | n | el radicado |
| `file_ref` | `VARCHAR(255)` | n | dónde está la copia. Conservar copia de lo presentado y de su recibo de pago es obligación expresa |
| `total_generated` · `total_deductible` · `balance_payable` · `balance_credit` | `DECIMAL(19,2)` | N | `CHECK` de no negatividad de las cuatro, más `CHECK (balance_payable = 0 OR balance_credit = 0)` |
| `firmeza_until` | `DATE` | n | |
| `corrects_return_id` | `BIGINT` | n | FK a sí misma |
| `created_date` · `version` | | N | «Se resuelve». **Sin `enabled`** |

**La forma de la clave de periodo, por tipo de impuesto** — el `CHECK` que impide que una
retención de diciembre acabe declarada en el bimestre de enero:

```sql
chk_tax_returns_period CHECK (
     (tax_kind = 'INCOME_TAX'  AND fiscal_period_key = CONCAT(fiscal_year, '-A'))
  OR (tax_kind = 'WITHHOLDING' AND fiscal_period_key REGEXP '^[0-9]{4}-M(0[1-9]|1[0-2])$'
                               AND LEFT(fiscal_period_key, 4) = CAST(fiscal_year AS CHAR))
  OR (tax_kind = 'ICA'         AND fiscal_period_key REGEXP '^[0-9]{4}-B0[1-6]$'
                               AND LEFT(fiscal_period_key, 4) = CAST(fiscal_year AS CHAR))
  OR (tax_kind = 'VAT'         AND LEFT(fiscal_period_key, 4) = CAST(fiscal_year AS CHAR)
                               AND ((vat_frequency = 'BIMONTHLY'
                                     AND fiscal_period_key REGEXP '^[0-9]{4}-B0[1-6]$')
                                 OR (vat_frequency = 'FOURMONTHLY'
                                     AND fiscal_period_key REGEXP '^[0-9]{4}-C0[1-3]$')
                                 OR (vat_frequency = 'ANNUAL'
                                     AND fiscal_period_key = CONCAT(fiscal_year, '-A')))))
```

Mezclar `fiscal_period_key` (ascii_bin) con `CONCAT(fiscal_year, …)` dentro de un `CHECK` **no da
error 1267**: el resultado de la función es más coercible que la columna, así que gana `ascii_bin`.
No hay que fiarse de mi palabra — `329:` ya escribe exactamente esa comparación y está aplicado.

**Los otros seis `CHECK`, todos con las dos ramas:**

```sql
chk_tax_returns_municipality CHECK ((tax_kind =  'ICA' AND municipality_code IS NOT NULL)
                                 OR (tax_kind <> 'ICA' AND municipality_code IS NULL)),
chk_tax_returns_vat_freq     CHECK ((tax_kind =  'VAT' AND vat_frequency IS NOT NULL
                                        AND vat_frequency IN ('BIMONTHLY','FOURMONTHLY','ANNUAL'))
                                 OR (tax_kind <> 'VAT' AND vat_frequency IS NULL)),
chk_tax_returns_filed        CHECK ((status IN ('DRAFT','ANNULLED')
                                        AND filed_at IS NULL AND filed_by_system_user_id IS NULL
                                        AND receipt_ref IS NULL AND firmeza_until IS NULL)
                                 OR (status IN ('FILED','CORRECTED')
                                        AND filed_at IS NOT NULL
                                        AND filed_by_system_user_id IS NOT NULL
                                        AND receipt_ref IS NOT NULL
                                        AND file_ref IS NOT NULL
                                        AND firmeza_until IS NOT NULL
                                        AND firmeza_until > DATE(filed_at))),
chk_tax_returns_amounts      CHECK (total_generated >= 0 AND total_deductible >= 0
                                    AND balance_payable >= 0 AND balance_credit >= 0),
chk_tax_returns_balance      CHECK (balance_payable = 0 OR balance_credit = 0),
chk_tax_returns_correction   CHECK ((sequence_number = 1 AND corrects_return_id IS NULL)
                                 OR (sequence_number > 1 AND corrects_return_id IS NOT NULL))
```

`chk_tax_returns_filed` es el que hace que `firmeza_until` **exista siempre que la declaración
esté presentada**. Es la columna de la que cuelga toda la política de conservación (§6.4): el
término de conservación de los soportes es el término de firmeza de la declaración de renta que
sostienen (art. 632 ET, modificado por el art. 46 de la Ley 962 de 2005), no un número escrito en
un changeset.

> **Lo que no se puede escribir como `CHECK`:** `corrects_return_id <> id`. El manual prohíbe
> referenciar una columna `AUTO_INCREMENT` dentro de un `CHECK`. Una declaración que se corrige a
> sí misma se impide en el caso de uso; queda declarada como **invariante no garantizada por la
> base** en §7.

**Unicidad y marcador de vigente:**

```sql
ADD COLUMN current_return_marker VARCHAR(45) CHARACTER SET ascii COLLATE ascii_bin
    GENERATED ALWAYS AS (
        CASE WHEN status IN ('DRAFT','FILED')
             THEN CONCAT(tax_kind, '|', fiscal_period_key, '|', municipality_key)
             ELSE NULL END) STORED,
ADD CONSTRAINT uq_tax_returns_case
    UNIQUE (tax_kind, fiscal_period_key, municipality_key, sequence_number),
ADD CONSTRAINT uq_tax_returns_current
    UNIQUE (current_return_marker),
```

El documento propone `(tax_kind, tax_period_key, municipality_code)` a secas. Dos problemas: el
municipio vacío no choca —mismo defecto que ya mordió en las tarifas de retención, por eso el
centinela— y **una corrección es una declaración nueva del mismo periodo**, así que con esa clave
«las declaraciones no se editan: se suceden» era inescribible. Con `sequence_number` dentro caben
las correcciones, y con el marcador solo hay **una vigente** por periodo: al corregir, la anterior
pasa a `CORRECTED` y libera el hueco.

**Índices** (los cuatro últimos, antes de sus claves foráneas):

| Nombre | Columnas | Sirve a |
|---|---|---|
| `ix_tax_returns_firmeza` | `(firmeza_until)` | **barrido de plataforma**: las que están a punto de quedar en firme, y con ellas la ventana de conservación |
| `ix_tax_returns_period` | `(fiscal_period_key, tax_kind)` | armar y cotejar un periodo |
| `ix_tax_returns_vat_frequency` | `(vat_frequency_year, vat_frequency)` | `fk_tax_returns_vat_frequency` |
| `ix_tax_returns_municipality` | `(municipality_code)` | `fk_tax_returns_municipality` |
| `ix_tax_returns_corrects` | `(corrects_return_id)` | `fk_tax_returns_corrects` |
| `ix_tax_returns_filed_by` | `(filed_by_system_user_id)` | `fk_tax_returns_filed_by` |

**La clave foránea que convierte la periodicidad en un dato y no en una fórmula:**

```
fk_tax_returns_vat_frequency (vat_frequency_year, vat_frequency)
    -> vat_filing_periods(fiscal_year, frequency)
```

Es el mismo mecanismo que `fk_company_capacities_dimension` (`314`) usa para copiar `measure_kind`
del eje: la fila **copia** el dato del padre y una clave compuesta impide que diverja. Cuando
`tax_kind` no es IVA, la columna generada vale `NULL` y **InnoDB no comprueba la clave** — es la
misma semántica de la que ya depende `fk_document_withholdings_certificate` (`329`), con
`certificate_id` nulable.

Y el manual permite esta clave: una columna generada `STORED` puede ser columna hija de una
foránea; lo único prohibido es `CASCADE`/`SET NULL`/`SET DEFAULT`, que aquí no se usan porque la
casa manda `RESTRICT`.

---

### 5.7 `supplier_withholdings` — lo que tú le retienes a otros *(sin empresa)*

Gemela de `document_withholdings` (`329`) en la dirección contraria. **Se escribe con el mismo
vocabulario**, columna por columna, o las dos divergen.

| Columna | Tipo | N/n | Regla |
|---|---|---|---|
| `id` | `BIGINT` AI | N | PK |
| `supplier_tax_id` | `VARCHAR(50)` ascii_bin | N | El NIT. Sin él no se puede armar el reporte anual de terceros, que se hace con el documento y no con el nombre |
| `supplier_name` | `VARCHAR(200)` | N | |
| `supplier_doc_type` | `VARCHAR(15)` | N | `CHECK (supplier_doc_type IN ('NIT','CC','CE','PASSPORT','FOREIGN_ID'))` — el mismo vocabulario de `company_billing_profiles.tax_id_kind` |
| `supplier_invoice_ref` | `VARCHAR(100)` ascii_bin | N | **decisión mía**, §8: sin la referencia del soporte no se cuadra contra el gasto ni se sostiene la deducción, y sin ella la unicidad es falsa |
| `withholding_type` | `VARCHAR(20)` | N | `CHECK (withholding_type IN ('INCOME_TAX','VAT','ICA'))` — **idéntico literal a `329` y `328`** |
| `concept` | `VARCHAR(60)` | N | |
| `taxable_base` | `DECIMAL(19,2)` | N | |
| `rate_percent` | `DECIMAL(9,6)` | N | Porcentaje, no fracción — el nombre lo dice |
| `amount` | `DECIMAL(19,2)` | N | |
| `municipality_code` | `VARCHAR(5)` ascii_bin | n | FK a `cities(dane_code)` |
| `municipality_key` | `VARCHAR(5)` ascii_bin | N | generada `STORED` = `COALESCE(municipality_code, '-')` |
| `fiscal_year` | `SMALLINT` | N | |
| `fiscal_period_key` | `VARCHAR(10)` ascii_bin | N | |
| `practiced_on` | `DATE` | N | |
| `certificate_issued_at` | `DATETIME(6)` | n | |
| `certificate_ref` | `VARCHAR(100)` | n | |
| `payment_receipt_ref` | `VARCHAR(255)` | n | la prueba de la consignación; obligación legal de conservación |
| `created_date` · `version` | | N | «Recibe un acuse o un documento que llega tarde». **Sin `enabled`** |

```sql
chk_sw_amounts      CHECK (taxable_base > 0 AND amount > 0 AND amount <= taxable_base),
chk_sw_rate         CHECK (rate_percent > 0 AND rate_percent <= 100),
chk_sw_year         CHECK (fiscal_year BETWEEN 2020 AND 2100),
chk_sw_municipality CHECK ((withholding_type =  'ICA' AND municipality_code IS NOT NULL)
                        OR (withholding_type <> 'ICA' AND municipality_code IS NULL)),
chk_sw_certificate  CHECK ((certificate_issued_at IS NULL AND certificate_ref IS NULL)
                        OR (certificate_issued_at IS NOT NULL AND certificate_ref IS NOT NULL)),
chk_sw_period       CHECK (LEFT(fiscal_period_key, 4) = CAST(fiscal_year AS CHAR)
                           AND ((withholding_type = 'INCOME_TAX'
                                 AND fiscal_period_key REGEXP '^[0-9]{4}-M(0[1-9]|1[0-2])$')
                             OR (withholding_type IN ('VAT','ICA')
                                 AND fiscal_period_key REGEXP '^[0-9]{4}-B0[1-6]$')))
```

> **Ojo con `INCOME_TAX`: aquí es mensual y en `document_withholdings` es anual.** No es un
> descuido, es la diferencia real: la retención que **te practican** se imputa al año gravable de
> tu renta (`'2026-A'`, `329:`); la que **tú practicas** se declara en la retención en la fuente,
> que es **mensual** (`'2026-M03'`). Misma columna, mismo nombre, dos granularidades legítimas.
> Va escrito en el comentario del changeset, o el primer lector lo «corrige».

**Unicidad, corregida.** El documento propone
`(supplier_tax_id, tax_period_key, withholding_type, municipality_key)`. Esa clave **prohíbe dos
facturas distintas del mismo proveedor en el mismo mes**, que es el caso normal:

```sql
ADD CONSTRAINT uq_supplier_withholdings_case
    UNIQUE (supplier_tax_id, fiscal_period_key, withholding_type,
            municipality_key, supplier_invoice_ref)
```

Con la factura dentro sigue impedido declarar dos veces la misma retención al mismo proveedor por
el mismo soporte —que es lo que duplicaría el reporte anual de terceros y descuadraría la
mensual— y dos facturas distintas caben.

**Índices:**

| Nombre | Columnas | Sirve a |
|---|---|---|
| `ix_sw_declaration` | `(fiscal_period_key, withholding_type)` | **barrido de plataforma**: armar la declaración del mes |
| `ix_sw_certificate` | `(supplier_tax_id, fiscal_year)` | el certificado anual que hay que entregarle al proveedor |
| `ix_sw_municipality` | `(municipality_code)` | `fk_sw_municipality` (antes de la clave) |

---

### 5.8 `uvt_values` — la unidad de valor tributario, por año *(sin empresa)*

| Columna | Tipo | N/n | Regla |
|---|---|---|---|
| `id` | `BIGINT` AI | N | PK |
| `fiscal_year` | `SMALLINT` | N | `uq_uvt_values_year UNIQUE (fiscal_year)` · `CHECK (fiscal_year BETWEEN 2020 AND 2100)` |
| `value_amount` | `DECIMAL(19,2)` | N | `CHECK (value_amount > 0)` |
| `legal_reference` | `VARCHAR(255)` | N | La resolución que lo fijó. **Obligatoria**: sin ella el número es una afirmación |
| `created_date` · `enabled` | | N | **Sin `version`** |

Sin índices adicionales: la unicidad es la única entrada.

```java
exenta("UvtValueJpaEntity", E1_APPEND_ONLY,
        "dato anual inerte: se siembra por ano y no se relee para escribir; corregir un ano"
                + " es publicar la fila del ano siguiente")
```

---

### 5.9 `smmlv_values` — el salario mínimo, por año *(sin empresa)*

Gemela de la anterior con una diferencia que hoy mismo importa: **su estado es un dato**.

| Columna | Tipo | N/n | Regla |
|---|---|---|---|
| `id` | `BIGINT` AI | N | PK |
| `fiscal_year` | `SMALLINT` | N | `uq_smmlv_values_year UNIQUE (fiscal_year)` · `CHECK (fiscal_year BETWEEN 2020 AND 2100)` |
| `value_amount` | `DECIMAL(19,2)` | N | `CHECK (value_amount > 0)` |
| `legal_reference` | `VARCHAR(255)` | N | El decreto que lo fijó |
| `status` | `VARCHAR(20)` | N | defecto `'IN_FORCE'`; `CHECK (status IN ('IN_FORCE','SUSPENDED','SUPERSEDED'))` |
| `status_reference` | `VARCHAR(255)` | n | El auto o el decreto que cambió el estado |
| `status_changed_on` | `DATE` | n | |
| `created_date` · `enabled` · `version` | | N | Catálogo que muta: **sí lleva versión**, porque la suspensión judicial se anota sobre la fila que ya existía |

```sql
chk_smmlv_values_status CHECK ((status =  'IN_FORCE' AND status_reference IS NULL
                                                     AND status_changed_on IS NULL)
                            OR (status <> 'IN_FORCE' AND status_reference IS NOT NULL
                                                     AND status_changed_on IS NOT NULL))
```

**Se mantiene `UNIQUE (fiscal_year)` a secas, sin marcador de vigencia** — lo que dice el
documento. Es defendible: no puede haber dos salarios mínimos aplicables el mismo año. El riesgo
conocido es que hoy conviven el decreto suspendido y un decreto transitorio, y con esta clave solo
cabe una fila. *Coste de cambiarlo si el fallo de fondo obliga a guardar los dos:* **bajo** — la
tabla tiene una decena de filas, sustituir el `UNIQUE` por un marcador generado es una operación
de segundos aunque reconstruya la tabla. Por eso no se sobre-modela hoy.

---

### 5.10 `vat_filing_periods` — cada cuánto se declara, año por año *(sin empresa)*

| Columna | Tipo | N/n | Regla |
|---|---|---|---|
| `id` | `BIGINT` AI | N | PK |
| `fiscal_year` | `SMALLINT` | N | `CHECK (fiscal_year BETWEEN 2020 AND 2100)` |
| `frequency` | `VARCHAR(15)` | N | `CHECK (frequency IN ('BIMONTHLY','FOURMONTHLY','ANNUAL'))` |
| `legal_reference` | `VARCHAR(255)` | N | |
| `created_date` · `enabled` | | N | **Sin `version`** (E1, dato anual inerte) |

```sql
ADD CONSTRAINT uq_vat_filing_periods_year      UNIQUE (fiscal_year),
ADD CONSTRAINT uq_vat_filing_periods_frequency UNIQUE (fiscal_year, frequency),
```

La segunda **no está en el documento y es obligatoria**: es la clave auxiliar contra la que apunta
`fk_tax_returns_vat_frequency`. Sin ella esa clave no se puede crear, y la periodicidad vuelve a
ser una fórmula en el código. Es redundante con la primera en el sentido lógico —si el año es
único, el par también lo es— pero **no en el sentido físico**: InnoDB necesita un índice cuyas
columnas de la izquierda sean exactamente las referenciadas.

---

### 5.11 `public_holidays` — los festivos colombianos *(sin empresa)*

El dato más pequeño del bloque y el que sostiene toda regla de plazo en días hábiles.

| Columna | Tipo | N/n | Regla |
|---|---|---|---|
| `id` | `BIGINT` AI | N | PK |
| `holiday_date` | `DATE` | N | **La fecha observada**: la que hay que consultar para saber si un día es hábil. `uq_public_holidays_date UNIQUE (holiday_date)` |
| `name` | `VARCHAR(120)` | N | Para que la pantalla no sea un calendario mudo |
| `nominal_date` | `DATE` | n | La fecha de la efeméride antes del traslado. **No única** |
| `moved` | `BOOLEAN` | N | Si se trasladó al lunes siguiente |
| `legal_reference` | `VARCHAR(255)` | N | `Ley 51 de 1983` para los trasladables, `Art. 177 CST` para los de fecha fija |
| `created_date` · `enabled` | | N | **Sin `version`** (E1) |

```sql
chk_public_holidays_move CHECK (
     (moved = FALSE AND (nominal_date IS NULL OR nominal_date = holiday_date))
  OR (moved = TRUE  AND nominal_date IS NOT NULL AND nominal_date < holiday_date)),
chk_public_holidays_range CHECK (holiday_date BETWEEN '2020-01-01' AND '2100-12-31')
```

> **`nominal_date` no puede ser única, y esto no es teoría.** La Ley 51 de 1983 traslada al lunes
> siguiente el 6 de enero, el 19 de marzo, el 29 de junio, el 15 de agosto, el 12 de octubre, el
> 1 y el 11 de noviembre, la Ascensión, el Corpus Christi y el Sagrado Corazón. **Dos efemérides
> distintas pueden caer en el mismo lunes**: el 1 de julio de 2019 lo fue a la vez el Sagrado
> Corazón (Pascua + 71 días) y San Pedro y San Pablo (29 de junio, sábado, trasladado). La clave
> es la **fecha observada**, que es lo que el cálculo de días hábiles pregunta; cuando dos
> coinciden, `name` lleva las dos y `nominal_date` la primera. Una unicidad sobre `nominal_date`
> habría hecho ese año inescribible.

**La cobertura por año no se puede imponer con una restricción** — es una comprobación de
conjunto. Va en §7 como invariante no garantizada, con su consulta de vigilancia.

---

### 5.12 `legal_document_versions` — qué texto aceptó, exactamente *(sin empresa)*

| Columna | Tipo | N/n | Regla |
|---|---|---|---|
| `id` | `BIGINT` AI | N | PK |
| `code` | `VARCHAR(50)` ascii_bin | N | El identificador del documento |
| `document_version` | `INT` | N | **No `version`**, ver el aviso de abajo. `CHECK (document_version >= 1)` |
| `kind` | `VARCHAR(30)` | N | `CHECK (kind IN ('TERMS','PRIVACY_POLICY','DATA_PROCESSING_AGREEMENT','PRIVACY_NOTICE','COMMITMENT_ANNEX'))` |
| `title` | `VARCHAR(200)` | N | |
| `content` | `MEDIUMTEXT` | N | **No `TEXT`**: `TEXT` acaba a los 64 KiB y un contrato de adhesión con anexos lo pasa. Ampliarlo después reconstruye la tabla |
| `content_hash` | `CHAR(64)` ascii_bin | N | `CHECK (content_hash REGEXP '^[0-9a-f]{64}$')` |
| `published_at` | `DATETIME(6)` | N | |
| `published_by_system_user_id` | `BIGINT` | N | FK a `system_users(id)` |
| `effective_from` | `DATE` | N | |
| `superseded_at` | `DATETIME(6)` | n | `CHECK (superseded_at IS NULL OR superseded_at >= published_at)` |
| `created_date` · `version` | | N | «Se cierra su vigencia»: lleva versión. **Sin `enabled`** — un texto legal publicado no se desactiva, se sucede |

> **El choque de nombres que el documento no vio.** La ficha pide una columna `version` de
> negocio (la versión del texto) y a la vez pone la tabla entre las que llevan columna de
> concurrencia, que también se llama `version`. Dos columnas con el mismo nombre no caben.
> **Decisión: la de negocio es `document_version`; `version` queda para el bloqueo optimista**,
> igual que en las otras 83 entidades. *Coste de cambiarlo después:* alto — toca la `@Entity`, el
> mapper y toda consulta que la nombre. Hoy cuesta cero.

**Unicidad y marcador:**

```sql
ADD COLUMN current_version_marker VARCHAR(50) CHARACTER SET ascii COLLATE ascii_bin
    GENERATED ALWAYS AS (CASE WHEN superseded_at IS NULL THEN code ELSE NULL END) STORED,
ADD CONSTRAINT uq_ldv_code_version UNIQUE (code, document_version),
ADD CONSTRAINT uq_ldv_content      UNIQUE (code, content_hash),
ADD CONSTRAINT uq_ldv_current      UNIQUE (current_version_marker),
```

La primera es la del documento. Las otras dos **no estaban**: `uq_ldv_content` impide publicar dos
veces el mismo texto bajo el mismo documento —convierte la huella en una clave de verdad— y
`uq_ldv_current` impide dos versiones vigentes a la vez, que es lo que haría que la aceptación
apuntase a la que llegara primero.

**Índices:** `ix_ldv_kind (kind, published_at)`;
`ix_ldv_published_by (published_by_system_user_id)`, antes de su clave.

**Disparador de inmutabilidad — §6.3.** Es lo que convierte «se acepta por huella» en algo
demostrable: sin él, editar `content` deja intacta la aceptación que apuntaba a esa fila y la
prueba se evapora en silencio.

---

### 5.13 `security_incidents` — cuando algo se rompe y hay que avisar *(sin empresa)*

| Columna | Tipo | N/n | Regla |
|---|---|---|---|
| `id` | `BIGINT` AI | N | PK |
| `detected_at` | `DATETIME(6)` | N | |
| `occurred_at` | `DATETIME(6)` | n | `CHECK (occurred_at IS NULL OR occurred_at <= detected_at)`. La distancia entre las dos es la primera pregunta que hace cualquiera que revise |
| `kind` | `VARCHAR(30)` | N | `CHECK (kind IN ('UNAUTHORIZED_ACCESS','DATA_LOSS','DATA_LEAK','RANSOMWARE','SERVICE_ABUSE','OTHER'))` |
| `severity` | `VARCHAR(10)` | N | `CHECK (severity IN ('LOW','MEDIUM','HIGH','CRITICAL'))` |
| `summary` | `VARCHAR(255)` | N | **decisión mía**, §8: una lista de incidentes sin título es ilegible y la ficha no daba ninguna descripción corta |
| `affected_subject_count` | `INT` | N | `CHECK (affected_subject_count >= 0)`. Es un contador de conveniencia; **la verdad está en la puente** |
| `deadline_at` | `DATETIME(6)` | N | `CHECK (deadline_at > detected_at)`. El plazo de reporte, como dato — no como cálculo — para poder listar lo que está a punto de incumplirse |
| `reported_to_authority_at` | `DATETIME(6)` | n | |
| `report_reference` | `VARCHAR(100)` | n | el radicado |
| `notified_subjects_at` | `DATETIME(6)` | n | Se conserva la columna y **no se le construye plazo legal**: en Colombia la obligación es informar a la autoridad, no a los titulares |
| `containment` · `root_cause` | `TEXT` | n | |
| `closed_at` | `DATETIME(6)` | n | |
| `created_date` · `version` | | N | «Se cierra». **Sin `enabled`**: una prueba que se puede desactivar no prueba nada |

```sql
chk_security_incidents_report CHECK ((reported_to_authority_at IS NULL
                                        AND report_reference IS NULL)
                                  OR (reported_to_authority_at IS NOT NULL
                                        AND report_reference IS NOT NULL
                                        AND reported_to_authority_at >= detected_at)),
chk_security_incidents_close  CHECK (closed_at IS NULL
                                  OR (containment IS NOT NULL AND root_cause IS NOT NULL
                                        AND closed_at >= detected_at))
```

El segundo es la regla que impide cerrar un incidente sin causa raíz ni contención escritas — un
incidente que no se documentó en su momento es indistinguible de uno que se ocultó.

**Índices:**

| Nombre | Columnas | Sirve a |
|---|---|---|
| `ix_security_incidents_detected` | `(detected_at)` | **barrido de plataforma** |
| `ix_security_incidents_unreported` | `(reported_to_authority_at, deadline_at)` | **barrido**: los no reportados con el plazo encima. Sí funciona con `IS NULL` — MySQL indexa los nulos y los busca por índice; lo que no existe en este motor son índices parciales |

**Sin unicidad natural**, a propósito: un incidente es un hecho, no tiene clave. El riesgo de alta
doble por reintento queda declarado en §7.

---

### 5.14 `security_incident_companies` — la puente de afectados

Se escribe **una sola vez**, al cerrar el incidente. Sin `created_date`, sin `enabled`, sin
`version`.

| Columna | Tipo | N/n | Regla |
|---|---|---|---|
| `id` | `BIGINT` AI | N | PK (la regla Uno vale también aquí) |
| `security_incident_id` | `BIGINT` | N | FK a `security_incidents(id)` |
| `company_id` | `BIGINT` | N | FK a `companies(id)` |
| `affected_scope` | `VARCHAR(30)` | N | `CHECK (affected_scope IN ('PERSONAL_DATA','CLINICAL_DATA','BILLING_DATA','CREDENTIALS'))` |
| `affected_subject_count` | `INT` | N | `CHECK (>= 0)`. Los titulares **de esa clínica**, no del incidente entero |

```sql
ADD CONSTRAINT uq_sic_pair UNIQUE (security_incident_id, company_id, affected_scope)
```

> **Decisión mía · el ámbito entra en la unicidad.** El documento propone
> `(security_incident_id, company_id)`. Pero él mismo dice que «el alcance decide el plazo de
> notificación» y que «dos clínicas del mismo incidente pueden estar alcanzadas por cosas
> distintas»: un ataque que expone credenciales **y** datos clínicos de la misma clínica son dos
> ámbitos con dos plazos, y con la clave corta el segundo es inescribible. *Coste de revertirlo:*
> **bajo** — quitar `affected_scope` de la unicidad es `DROP INDEX` + `ADD UNIQUE`, en sitio y sin
> reconstruir.

**Índices:** la unicidad sirve a `fk_sic_incident`; `ix_sic_company (company_id,
security_incident_id)` **en orden inverso a propósito**, porque la consulta que importa es la del
cliente —«qué incidentes me afectaron»— y sin él recorre la tabla.

**Sin operación de borrado.** La `@Entity` no lleva `@SQLDelete` y el repositorio no expone
`delete`: quitar una clínica de la lista de afectados es destruir la prueba de que se le notificó.
Eso es de `backend-feature`; queda declarado aquí.

```java
exenta("SecurityIncidentCompanyJpaEntity", E2_TABLA_PUENTE,
        "puente escrita una sola vez al cerrar el incidente; no lleva fecha de creacion ni"
                + " marca de activo y ningun caso de uso la reescribe")
```

---

### 5.15 `external_invoicing_outages` — cuando la emisión fiscal se cae *(sin empresa)*

| Columna | Tipo | N/n | Regla |
|---|---|---|---|
| `id` | `BIGINT` AI | N | PK |
| `started_at` | `DATETIME(6)` | N | |
| `ended_at` | `DATETIME(6)` | n | `CHECK (ended_at IS NULL OR ended_at > started_at)`. Abierta mientras dura, que es cuando alguien va a preguntar |
| `cause_party` | `VARCHAR(20)` | N | `CHECK (cause_party IN ('EXTERNAL_ISSUER','AUTHORITY','NETWORK','OWN'))`. **Es la columna que separa un incidente de un incumplimiento**, y por eso no puede ser texto libre |
| `summary` | `VARCHAR(255)` | N | **decisión mía**, §8 |
| `affected_company_count` | `INT` | N | `CHECK (>= 0)` |
| `notified_companies_at` | `DATETIME(6)` | n | `CHECK (notified_companies_at IS NULL OR notified_companies_at >= started_at)` |
| `external_incident_ref` | `VARCHAR(100)` | n | El radicado del proveedor. Es lo que traslada la responsabilidad con nombre y número |
| `created_date` · `version` | | N | «Se cierra». **Sin `enabled`** |

```sql
ADD COLUMN open_outage_marker VARCHAR(20) CHARACTER SET ascii COLLATE ascii_bin
    GENERATED ALWAYS AS (CASE WHEN ended_at IS NULL THEN cause_party ELSE NULL END) STORED,
ADD CONSTRAINT uq_eio_open UNIQUE (open_outage_marker),
```

**Decisión mía**, §8: una sola caída abierta **por causante**. Dos caídas simultáneas del emisor
externo son la misma caída, y sin el marcador el proceso de detección abre una nueva en cada
sondeo y deja un rastro de caídas vivas que nunca se cierran. Dos causantes distintos —el emisor y
la red— sí pueden solaparse, y por eso el marcador lleva `cause_party` y no una constante.

**Índices:** `ix_eio_started (started_at)` — barrido; `ix_eio_open (ended_at, started_at)` —
barrido, las todavía abiertas.

---

### 5.16 `external_invoicing_outage_companies` — la segunda puente

| Columna | Tipo | N/n | Regla |
|---|---|---|---|
| `id` | `BIGINT` AI | N | PK |
| `outage_id` | `BIGINT` | N | FK a `external_invoicing_outages(id)` |
| `company_id` | `BIGINT` | N | FK a `companies(id)` |
| `failed_document_count` | `INT` | N | `CHECK (>= 0)`. Es el número que sostiene la reclamación |
| `resolved_by` | `VARCHAR(25)` | N | `CHECK (resolved_by IN ('RETRIED','MANUAL','CONTINGENCY_NUMBERING'))` — el tercero es el que hay que poder demostrar ante la autoridad |

`uq_eioc_pair UNIQUE (outage_id, company_id)` — aquí sí una fila por clínica y caída: no hay
ámbito que multiplique. `ix_eioc_company (company_id, outage_id)` para el expediente de una
clínica. Sin `created_date`, sin `enabled`, sin `version` (`E2_TABLA_PUENTE`, mismo motivo
literal). Sin operación de borrado.

> **Lo que el cliente ve de estas dos puentes: que hubo una caída o un incidente, nunca a cuántos
> alcanzó.** El contador de la tabla madre y las filas de las otras clínicas no salen por ningún
> puerto de cliente. Es una decisión de autorización, no de esquema, pero se decide aquí porque el
> esquema es lo que la hace posible.

---

### 5.17 `company_usage_events` — el hecho que sostiene el cobro

La tabla más grande del bloque y la única cuyo volumen hay que mirar.

| Columna | Tipo | N/n | Regla |
|---|---|---|---|
| `id` | `BIGINT` AI | N | PK |
| `company_id` | `BIGINT` | N | |
| `limit_dimension_id` | `BIGINT` | N | |
| `limit_dimension_code` | `VARCHAR(50)` ascii_bin | N | **Copiado del eje** y atado por clave compuesta, igual que `company_capacities` copia `measure_kind` (`314`) |
| `usage_owner_id` · `usage_animal_id` · `usage_appointment_id` · `usage_electronic_document_id` | `BIGINT` | n | Las cuatro ramas. **Una clave foránea por rama, nunca un identificador suelto con una columna de tipo** |
| `occurred_at` | `DATETIME(6)` | N | **El instante del registro consumido, no el del proceso.** De eso depende la unicidad, ver abajo |
| `period_key` | `VARCHAR(7)` ascii_bin | N | Tres granularidades más el centinela, igual que `company_capacities` |
| `billable` | `BOOLEAN` | N | |
| `charge_id` | `BIGINT` | n | El cargo que lo facturó. Es la segunda escritura declarada |
| `created_date` · `version` | | N | **Sin `enabled`**: la ficha excluye expresamente los hechos de uso de la marca de activo |

**`period_key` es `VARCHAR(7)` y no `CHAR(7)`** — al contrario que `posting_period`. No es una
inconsistencia: es la misma desviación deliberada que `314` documenta para `company_capacities`
(«MySQL recorta los espacios finales de un `CHAR` al leerlo»), y aquí la clave admite tres
longitudes de contenido distintas (`2026-03`, `2026-Q3`, `ALLTIME`), así que el relleno sería
real. `posting_period` puede ser `CHAR(7)` porque su `REGEXP` prohíbe cualquier cosa que no sean
siete caracteres exactos.

```sql
chk_cue_period_key CHECK (period_key REGEXP
    '^([0-9]{4}-(0[1-9]|1[0-2])|[0-9]{4}-Q[1-4]|[0-9]{4}-S[12]|ALLTIME)$'),
chk_cue_billable   CHECK (charge_id IS NULL OR billable = TRUE),
chk_cue_branch CHECK (
     (limit_dimension_code = 'OWNER'       AND usage_owner_id IS NOT NULL
        AND usage_animal_id IS NULL AND usage_appointment_id IS NULL
        AND usage_electronic_document_id IS NULL)
  OR (limit_dimension_code = 'ANIMAL'      AND usage_animal_id IS NOT NULL
        AND usage_owner_id IS NULL AND usage_appointment_id IS NULL
        AND usage_electronic_document_id IS NULL)
  OR (limit_dimension_code = 'APPOINTMENT' AND usage_appointment_id IS NOT NULL
        AND usage_owner_id IS NULL AND usage_animal_id IS NULL
        AND usage_electronic_document_id IS NULL)
  OR (limit_dimension_code = 'INVOICE'     AND usage_electronic_document_id IS NOT NULL
        AND usage_owner_id IS NULL AND usage_animal_id IS NULL
        AND usage_appointment_id IS NULL))
```

`chk_cue_branch` hace dos cosas a la vez, y las dos importan. Impide que un hecho del eje de
mascotas apunte a una cita, y **impide que exista un hecho de uso para un eje de existencias**
(`USER`, `BRANCH`, `TERMINAL`, `STORAGE_GB`): esos no se acumulan hecho a hecho, se cuentan. Que
sea el motor quien lo diga y no un comentario es la diferencia entre una regla y un recuerdo.

> **Consecuencia honesta, que hay que decir en voz alta:** vender un eje **contable** nuevo sí es
> un despliegue —columna nueva, clave nueva, `CHECK` reescrito—, porque una rama nueva necesita su
> propia clave foránea. Vender un eje **de existencias** nuevo sigue siendo insertar una fila. El
> documento promete lo segundo para todos los ejes; para los contables no es cierto, y la
> alternativa (un identificador polimórfico) es justo lo que el propio documento rechaza — es la
> *Polymorphic Association* de *SQL Antipatterns*, y aquí sostiene un cobro.

**La unicidad, que el documento no declaraba:**

```sql
ADD COLUMN usage_ref_key VARCHAR(80) CHARACTER SET ascii COLLATE ascii_bin
    GENERATED ALWAYS AS (CONCAT(limit_dimension_code, '|',
        COALESCE(usage_owner_id, usage_animal_id, usage_appointment_id,
                 usage_electronic_document_id))) STORED,
ADD CONSTRAINT uq_cue_fact
    UNIQUE (company_id, limit_dimension_id, usage_ref_key, occurred_at),
```

Sin ella el reintento del proceso de medición duplica el hecho, y con él el excedente facturado —
sobre la única tabla que existe para **ganar** esa reclamación. La condición que la hace correcta
está escrita arriba: **`occurred_at` se toma del registro consumido, no del reloj del proceso.** Si
alguien la rellena con `now()`, el reintento deja de chocar y la protección desaparece sin ruido.
Los ajustes por reconteo entran como hechos que compensan, con su propio instante, y sí caben.

**Índices y claves foráneas:**

| Nombre | Columnas | Sirve a |
|---|---|---|
| `uq_cue_fact` | `(company_id, limit_dimension_id, usage_ref_key, occurred_at)` | la unicidad **y** el índice de `fk_cue_company` |
| `ix_cue_period` | `(company_id, limit_dimension_id, period_key, occurred_at)` | la consulta del cupo: **tres igualdades y un rango, en ese orden** |
| `ix_cue_charge` | `(company_id, charge_id)` | desglosar un cargo por excedente, y `fk_cue_charge` |
| `ix_cue_dimension` | `(limit_dimension_id, limit_dimension_code)` | `fk_cue_dimension` |
| `ix_cue_owner` | `(company_id, usage_owner_id)` | `fk_cue_owner` |
| `ix_cue_animal` | `(company_id, usage_animal_id)` | `fk_cue_animal` |
| `ix_cue_appointment` | `(company_id, usage_appointment_id)` | `fk_cue_appointment` |
| `ix_cue_document` | `(company_id, usage_electronic_document_id)` | `fk_cue_document` |

```
fk_cue_company     (company_id)                              -> companies(id)
fk_cue_dimension   (limit_dimension_id, limit_dimension_code) -> limit_dimensions(id, code)
fk_cue_charge      (company_id, charge_id)                   -> subscription_charges(company_id, id)
fk_cue_owner       (company_id, usage_owner_id)              -> owners(company_id, id)
fk_cue_animal      (company_id, usage_animal_id)             -> animals(company_id, id)
fk_cue_appointment (company_id, usage_appointment_id)        -> appointments(company_id, id)
fk_cue_document    (company_id, usage_electronic_document_id)-> electronic_documents(company_id, id)
```

Las cinco últimas dependen del changeset de prerrequisitos (§3).

#### El único número de escala de todo el bloque

El documento se contradice: la ficha de la capa L dice «menos de doscientos registros al mes por
clínica → ~1 millón de filas al año a quinientas clínicas»; la de la capa O dice «a diez años y
quinientas clínicas rondan los doce millones de filas». Las dos son la misma cifra vista a
distinto plazo, y **doce millones de filas con ocho índices sobre una `db.t4g.small` de 2 GiB es
lo único de este diseño que puede incomodar al servidor**.

**Proyección, no medición** —hoy la tabla no existe y no hay nada que medir—: a ~40 bytes por
entrada de índice secundario (columnas + la PK que InnoDB mete dentro), ocho índices sobre doce
millones de filas rondan los **3 GB solo de índices**, sobre un *buffer pool* de ~1,4 GiB
utilizables y 20 GiB de gp3. El supuesto de crecimiento es el del propio documento: 500 clínicas,
200 hechos/mes, diez años.

Qué se hace con eso, en orden:

1. **Nada todavía.** Con tráfico de desarrollo y cero clínicas, cualquier partición, réplica o
   consolidación es cambiar un problema que no existe por tres que sí.
2. **La política de purga se escribe en el mismo changeset que crea la tabla**, como pide el
   documento — pero **no a 24 meses**: ver §7.
3. **Se mide al año del primer cliente**, con `information_schema.tables` y
   `sys.schema_unused_indexes`, y ahí se decide si sobra alguno de los cuatro índices de rama.
   Antes de eso, cualquier recorte es opinión.

---

### 5.18 `company_activity_months` — la actividad, mes a mes

La tabla más barata del documento: 500 clínicas × 12 = **6.000 filas al año**.

| Columna | Tipo | N/n | Regla |
|---|---|---|---|
| `id` | `BIGINT` AI | N | PK |
| `company_id` | `BIGINT` | N | FK a `companies(id)` |
| `period_key` | `CHAR(7)` ascii_bin | N | Siempre mensual; mismo `REGEXP` que `accounting_periods` |
| `commercial_state` | `VARCHAR(15)` | N | `CHECK (commercial_state IN ('PAID','FREE','TRIAL','CHURNED'))` — **decisión mía**, §8: la ficha decía «pagando, gratuito, en prueba o ido» y nunca dio los códigos |
| `active_days` | `INT` | N | `CHECK (active_days BETWEEN 0 AND 31)` |
| `active_users` | `INT` | N | `CHECK (active_users >= 0)` |
| `records_created` | `INT` | N | `CHECK (records_created >= 0)` |
| `mrr_snapshot` | `DECIMAL(19,2)` | N | `CHECK (mrr_snapshot >= 0)`. Ya normalizado a mensual: guardarlo evita recalcular el pasado y evita que dos personas lo calculen distinto |
| `created_date` · `version` | | N | «Proyección que se recalcula sobre sí misma»: el mes en curso se actualiza cada día hasta que termina. **Sin `enabled`** |

`uq_cam_month UNIQUE (company_id, period_key)` — sirve además de índice de `fk_cam_company`.
`ix_cam_dormant (period_key, active_days)` — **barrido de plataforma**: hallar los dormidos.

Esta tabla es la que hace que «una clínica que entra veinte días al mes» y «otra que no entra
ninguno» dejen de ser idénticas en los informes. Una columna de último acceso repetiría el defecto
que el documento acaba de corregir en el ciclo de facturación: se sobrescribe, y con ella se
pierde la serie.

---

## 6. Los disparadores — lo que ninguna restricción puede decir

Un `CHECK` mira una fila. Tres de las cuatro reglas de periodo del encargo miran **otra tabla** o
**el conjunto**, y ahí el motor solo ofrece disparadores. El propio changeset `331` ya lo anticipa:
«el disparador que la ficha menciona sería para impedir cerrar un periodo con hechos pendientes,
que es una comprobación de conjunto y no de fila».

Van todos en **un changeset propio**, después de las tablas que vigilan, con `<rollback>` que hace
`DROP TRIGGER`. Liquibase los declara con `<createProcedure>` o `<sql endDelimiter="//">`.

### 6.1 Un periodo cerrado no admite escrituras

Uno por cada tabla que escribe contra un periodo contable: `revenue_recognition_lines`,
`accounting_exports` y —cerrando la carencia declarada en `330`—
`external_invoice_reconciliations`.

```sql
CREATE TRIGGER trg_rrl_bi_period_open BEFORE INSERT ON revenue_recognition_lines
FOR EACH ROW
BEGIN
    DECLARE v_status VARCHAR(15);
    SELECT status INTO v_status
      FROM accounting_periods WHERE period_key = NEW.posting_period;
    IF v_status <> 'OPEN' THEN
        SIGNAL SQLSTATE '45000'
            SET MESSAGE_TEXT = 'periodo contable no abierto: use el primer periodo abierto';
    END IF;
END
```

Y su gemelo `BEFORE UPDATE` con la misma condición, porque mover el `posting_period` de una fila a
un mes ya cerrado es exactamente el mismo daño.

**Por qué esto no puede ser una clave foránea ni un `CHECK`.** La clave foránea garantiza que el
mes **existe**, no que esté abierto — y no puede mirar el estado. El `CHECK` no puede leer otra
tabla: el manual lo prohíbe expresamente. Sin el disparador, resolver una conciliación imputándola
a `2026-03` seis meses después de que marzo esté `LOCKED` sigue siendo posible, y **ese fallo es
silencioso por definición**: no hay error, hay un número mal imputado en una declaración.

### 6.2 Siempre al menos un periodo abierto, y un `LOCKED` no se reabre

```sql
CREATE TRIGGER trg_accounting_periods_bu_guard BEFORE UPDATE ON accounting_periods
FOR EACH ROW
BEGIN
    IF OLD.status = 'LOCKED' AND NEW.status <> 'LOCKED' THEN
        SIGNAL SQLSTATE '45000'
            SET MESSAGE_TEXT = 'un periodo declarado no se reabre';
    END IF;
    IF OLD.status = 'OPEN' AND NEW.status <> 'OPEN'
       AND (SELECT COUNT(*) FROM accounting_periods
             WHERE status = 'OPEN' AND id <> OLD.id) = 0 THEN
        SIGNAL SQLSTATE '45000'
            SET MESSAGE_TEXT = 'no queda ningun periodo abierto: abra el siguiente antes de cerrar';
    END IF;
END
```

Sin la segunda condición, un hecho tardío **no tiene dónde escribirse** y el sistema se bloquea
entero sin un solo error que lo explique.

> **PENDIENTE DE VERIFICAR, y no lo puedo cerrar yo.** El `SELECT` sobre la propia tabla dentro de
> su disparador es legal en MySQL —la prohibición (`ER_CANT_UPDATE_USED_TABLE`, 1442) es para
> **escribir** en la tabla sujeto, no para leerla—, pero conviene comprobarlo antes de dar el
> changeset por bueno. **No lo he medido**: verificarlo exige crear el disparador, y yo no escribo
> en ninguna base. La prueba es un `@DataJpaTest` con Testcontainers que cierre el único periodo
> abierto y espere el `SIGNAL`. Si fallara, la salida es mover la comprobación al procedimiento de
> cierre, que abre el siguiente mes en la misma transacción — y entonces la invariante baja a §7.

### 6.3 Una versión publicada es inmutable

```sql
CREATE TRIGGER trg_ldv_bu_immutable BEFORE UPDATE ON legal_document_versions
FOR EACH ROW
BEGIN
    IF NEW.code <> OLD.code OR NEW.document_version <> OLD.document_version
       OR NEW.kind <> OLD.kind OR NEW.content_hash <> OLD.content_hash
       OR NEW.published_at <> OLD.published_at
       OR NEW.published_by_system_user_id <> OLD.published_by_system_user_id
       OR NOT (NEW.content <=> OLD.content) THEN
        SIGNAL SQLSTATE '45000'
            SET MESSAGE_TEXT = 'una version publicada no se edita: se sucede';
    END IF;
END
```

Solo pueden moverse `superseded_at` y `version`. Es lo que hace verdadera la frase «una aceptación
tiene que poder probar qué texto se aceptó **aunque alguien edite la fila después**»: sin el
disparador, editar `content` deja la huella vieja apuntando a un texto que ya no existe, y la
prueba se evapora en silencio.

`<=>` (comparación segura con nulos) para `content` porque es la única columna larga; las demás son
`NOT NULL` y `<>` basta.

### 6.4 Lo que **no** lleva disparador, y por qué

- **«Un hecho tardío se reconoce en el primer periodo abierto.»** El disparador de §6.1 impide
  escribir en uno cerrado, y `chk_rrl_not_backwards` impide ir hacia atrás. Cuál es exactamente el
  primer abierto lo tiene que resolver el código: la base no lo sabe en el momento del `INSERT` sin
  una consulta de conjunto que además cambiaría el resultado según el orden de las filas.
- **La ventana de conservación.** Sale de `tax_returns.firmeza_until` y se aplica sobre
  `company_usage_events` en el proceso de purga, no en el esquema. **Los 24 meses del documento
  son incorrectos y hay que escribir la regla de otra manera** — ver §7.

---

## 7. Invariantes → qué las garantiza

### 7.1 Garantizadas por el motor

| Regla de negocio | Qué la garantiza |
|---|---|
| Un cargo no se reconoce dos veces en el mismo mes y periodo | `uq_rrl_recognition` |
| Un reconocimiento tardío nunca se imputa hacia atrás | `chk_rrl_not_backwards` |
| Nada se escribe en un periodo cerrado | `trg_*_bi_period_open` (§6.1) |
| Siempre queda un periodo abierto | `trg_accounting_periods_bu_guard` (§6.2) *(pendiente de verificar)* |
| Un periodo declarado no se reabre | mismo disparador |
| Un mapeo de cuentas no se edita: se cierra y se abre otro | `uq_account_mappings_current` + `valid_from`/`valid_to` |
| Un solo mapeo vigente por supuesto | `uq_account_mappings_current` |
| No se asienta contra un grupo, solo contra subcuenta | `chk_accounting_accounts_postable` |
| El asiento resumen cuadra | `chk_accounting_exports_balanced` |
| Un solo fichero de exportación vivo por mes y clase | `uq_accounting_exports_current` |
| Una sola declaración vigente por impuesto, periodo y municipio | `uq_tax_returns_current` |
| Una declaración presentada tiene siempre fecha de firmeza | `chk_tax_returns_filed` |
| La periodicidad del IVA sale de un dato con vigencia, no de una fórmula | `fk_tax_returns_vat_frequency` + `chk_tax_returns_period` |
| La retención se guarda con año gravable y no se pierde | `fiscal_year` `NOT NULL` + `chk_sw_period` |
| No se declara dos veces la misma retención al mismo proveedor por el mismo soporte | `uq_supplier_withholdings_case` |
| Un valor de UVT / salario mínimo por año, con su norma | `uq_uvt_values_year`, `uq_smmlv_values_year`, `legal_reference` `NOT NULL` |
| El estado judicial del salario mínimo es un dato | `smmlv_values.status` + `chk_smmlv_values_status` |
| Una versión de texto legal es inmutable | `trg_ldv_bu_immutable` (§6.3) |
| Una sola versión vigente por documento legal | `uq_ldv_current` |
| Un hecho de uso no se contabiliza dos veces | `uq_cue_fact` *(depende de que `occurred_at` sea el del registro)* |
| Un hecho de uso apunta a un registro **de su propia clínica** | las cuatro FK compuestas con `company_id` |
| Un hecho de uso apunta a la clase de registro que le corresponde | `chk_cue_branch` |
| No se factura un hecho no facturable | `chk_cue_billable` |
| Una clínica no se apunta dos veces al mismo incidente y ámbito | `uq_sic_pair` |
| Un incidente no se cierra sin causa raíz ni contención | `chk_security_incidents_close` |
| Una sola caída abierta por causante | `uq_eio_open` |
| Un mes de actividad por clínica | `uq_cam_month` |

### 7.2 **No** garantizadas por la base — y qué se hace con cada una

| Invariante | Por qué no cabe | Qué la sostiene |
|---|---|---|
| «El hecho tardío va al **primer** periodo abierto» | requiere consulta de conjunto en el `INSERT` | código, con §6.1 y `chk_rrl_not_backwards` de red |
| Una declaración no se corrige a sí misma | el manual prohíbe `id` (`AUTO_INCREMENT`) dentro de un `CHECK` | caso de uso + prueba de rodaja |
| Cobertura completa de festivos por año | comprobación de conjunto | `preCondition` `sqlCheck` `onFail="HALT"` en el changeset de siembra, y consulta de vigilancia mensual |
| Los contadores `affected_subject_count` / `affected_company_count` cuadran con su puente | agregado sobre otra tabla | consulta de vigilancia; el número de la madre es de conveniencia y **la verdad es la puente** |
| Un incidente no se da de alta dos veces por reintento | no tiene clave natural | se crea a mano, no por proceso |
| `occurred_at` es el del registro consumido y no el del reloj | el motor no distingue dos instantes | **regla de código, escrita en el comentario del changeset** — de ella depende `uq_cue_fact` |

### 7.3 La ventana de conservación — el documento se equivoca en doce meses, y hay que corregirlo

El documento manda: «se conserva el detalle veinticuatro meses y luego se consolida». **Esa regla
purga la prueba antes de que prescriba la facultad de revisión.**

La cuenta, con la norma delante:

- El término de conservación de los documentos y pruebas del art. 632 ET **es el término de
  firmeza de la declaración de renta que soportan**, desde la modificación del art. 46 de la Ley
  962 de 2005. No es un número propio.
- La firmeza general son **tres años** desde el vencimiento del plazo para declarar (art. 714 ET),
  y **cinco** si hay compensación o determinación de pérdidas fiscales o régimen de precios de
  transferencia.
- Un hecho de uso de enero de 2026 se factura en 2026, entra en la renta del año gravable 2026,
  que se presenta hacia abril de 2027 y queda en firme hacia abril de **2030**: **cincuenta y un
  meses** después del hecho. Con la regla de 24 meses, el detalle se habría consolidado en enero
  de 2028 — **veintisiete meses antes**, y con él se va la lista de facturas que sostiene el cargo
  por excedente.

**La regla que hay que escribir en el mismo changeset que crea `company_usage_events`:**

> El detalle de `company_usage_events` **no se consolida ni se purga** mientras exista una
> `tax_returns` de tipo `INCOME_TAX` que cubra el año gravable del `subscription_charges` al que
> apunta `charge_id` y cuya `firmeza_until` no haya pasado. Ancla de retención:
> `TAX_RETURN_FIRMEZA`. Suelo mínimo si esa declaración aún no existe: **cinco años** desde
> `occurred_at`, que es el término del art. 632 ET para agentes de retención y responsables de
> IVA.

`ix_tax_returns_firmeza` es el índice que hace barata esa consulta, y por eso está.

> **Esto hay que confirmarlo con un contador.** Yo verifiqué el criterio general (art. 632 ET +
> art. 46 Ley 962/2005 + art. 714 ET); **no** verifiqué si a VetSoftware le aplica la firmeza de
> tres o de cinco años, que depende de si compensa pérdidas — y esa diferencia son dos años de
> almacenamiento sobre la tabla más grande del diseño.

---

## 8. Decisiones tomadas sin ti

El dueño está ausente. Estas quedan tomadas para que `db-migrations` no tenga que decidir nada, y
cada una lleva **lo que cuesta cambiarla después**, que es el único dato que importa para saber si
merece esperar.

### 8.1 Correcciones al documento maestro — donde su clave estaba mal

| # | Decisión | Qué corrige | Coste de revertirla |
|---|---|---|---|
| 1 | `uq_rrl_recognition` lleva `posting_period` y `company_id`, no solo `(charge_id, period_key)` | con la clave del documento, **la fila que compensa un reconocimiento es inescribible** | **Bajo**: `DROP INDEX` + `ADD UNIQUE`, en sitio, sin reconstruir |
| 2 | `uq_tax_returns_case` lleva `sequence_number` y `municipality_key` | sin lo primero **una corrección no cabe**; sin lo segundo dos declaraciones nacionales del mismo periodo no chocan | **Medio**: quitar `sequence_number` es barato; quitar el centinela obliga a borrar la columna generada, que **reconstruye la tabla** |
| 3 | `uq_supplier_withholdings_case` lleva `supplier_invoice_ref` | la clave del documento **prohíbe dos facturas del mismo proveedor en el mismo mes**, que es el caso normal | **Bajo** si se quita de la clave; **alto** si se quiere quitar la columna (`NOT NULL` con datos dentro) |
| 4 | `account_mappings` lleva tres centinelas generados en su unicidad | con tres columnas nulables dentro, la unicidad **no restringe nada** en nueve de las doce clases | **Alto**: borrar columnas generadas `STORED` reconstruye la tabla |
| 5 | `uq_sic_pair` incluye `affected_scope` | un incidente que expone credenciales **y** datos clínicos de la misma clínica son dos ámbitos con dos plazos | **Bajo**: `DROP` + `ADD UNIQUE` |
| 6 | `mapping_kind` usa la lista de doce de «Los códigos de cada lista cerrada», no la de nueve de la prosa de la capa N | el documento da **dos listas incompatibles** para la misma columna | **Medio**: cambiar literales con datos sembrados obliga a un `UPDATE` de migración |
| 7 | `accounting_periods.status` se queda en `OPEN`/`SOFT_CLOSED`/`LOCKED` | el documento también escribe `OPEN`/`CLOSING`/`CLOSED`, y el changeset `331` **ya está aplicado** con los primeros | **Alto**: reescribir un changeset aplicado |
| 8 | La columna de periodo fiscal se llama `fiscal_period_key`, no `tax_period_key`; la tarifa `rate_percent DECIMAL(9,6)`, no `rate DECIMAL(7,4)` | `328` y `329` ya los llevan así; un tercer vocabulario es la divergencia silenciosa que el documento persigue | **Alto**: renombrar toca dos tablas construidas y sus entidades |
| 9 | La versión de negocio de un texto legal se llama `document_version` | `version` está ocupada por el bloqueo optimista y **dos columnas del mismo nombre no caben** | **Alto**: renombrar toca `@Entity`, mapper y consultas |

### 8.2 Marcadores y restricciones que el documento no pedía

| # | Decisión | Por qué | Coste de quitarla |
|---|---|---|---|
| 10 | `uq_account_mappings_current` | gemelo de `uq_withholding_rate_rules_current`: sin él, dos mapeos vigentes y el asiento toma el primero que llegue | **Alto** (columna generada) |
| 11 | `uq_accounting_exports_current` | «rehacer un fichero rechazado» sin él permite dos ficheros vivos del mismo mes | **Alto** (columna generada) |
| 12 | `uq_tax_returns_current` | «las declaraciones no se editan, se suceden» necesita saber cuál es la vigente | **Alto** (columna generada) |
| 13 | `uq_ldv_current` y `uq_ldv_content` | dos textos vigentes a la vez, y publicar dos veces el mismo texto | **Alto** / **Bajo** respectivamente |
| 14 | `uq_eio_open` (una caída abierta por causante) | sin él, cada sondeo abre una caída nueva y ninguna se cierra | **Alto** (columna generada) |
| 15 | `uq_cue_fact` | sin unicidad, el reintento **duplica el excedente facturado** | **Alto** (columna generada) |
| 16 | `chk_accounting_exports_balanced` | la partida doble, que el documento daba por imposible de imponer | **Bajo** |
| 17 | `chk_rrl_not_backwards` | la única de las cuatro reglas de periodo que cabe en un `CHECK` | **Bajo** |
| 18 | `chk_cue_branch` con los códigos de eje dentro | impide apuntar a la clase de registro equivocada **y** impide hechos de uso para ejes de existencias | **Bajo** el `CHECK`; alta la consecuencia (ver §5.17) |

### 8.3 Columnas y listas que el documento no cerró

| # | Decisión | Por qué | Coste |
|---|---|---|---|
| 19 | `export_kind IN ('JOURNAL_SUMMARY','THIRD_PARTY_REPORT','VAT_SUPPORT')` | la ficha exige `export_kind` en la unicidad y **nunca dio sus valores** | **Medio** (datos sembrados) |
| 20 | `tax_returns.status IN ('DRAFT','FILED','CORRECTED','ANNULLED')` | sin estado, «presentada» y «en borrador» son la misma fila y `firmeza_until` no se puede exigir | **Medio** |
| 21 | `commercial_state IN ('PAID','FREE','TRIAL','CHURNED')` | la ficha decía «pagando, gratuito, en prueba o ido» sin códigos | **Medio** |
| 22 | `smmlv_values.status IN ('IN_FORCE','SUSPENDED','SUPERSEDED')` | la ficha pide «vigente o suspendida» y falta el tercero para cuando llegue el decreto definitivo | **Medio** |
| 23 | `supplier_withholdings.supplier_invoice_ref` obligatoria | mismo argumento con el que la ficha exige `provider_invoice_ref` en `gateway_settlements`: sin soporte no hay deducción | **Alto** (`NOT NULL` nueva con datos) |
| 24 | `summary VARCHAR(255)` obligatorio en incidentes y caídas | una lista sin título es ilegible, y las dos son listas de trabajo | **Alto** (`NOT NULL`) |
| 25 | `content MEDIUMTEXT` en vez de `TEXT` | `TEXT` acaba a los 64 KiB | **Alto**: ampliar el tipo reconstruye la tabla |
| 26 | `account_level` en vez de `level` | `LEVEL` es palabra clave de MySQL 8 y obliga a comillas invertidas en cada consulta | **Alto** (renombrar) |
| 27 | `accounting_accounts.code` único **global**, no por vigencia | un código no puede significar dos cosas en dos épocas | **Alto** |
| 28 | `smmlv_values` conserva `UNIQUE (fiscal_year)` a secas | es lo que dice el documento y la invariante es real; el riesgo del decreto transitorio se acepta | **Bajo**: diez filas, sustituir el `UNIQUE` es cuestión de segundos |

### 8.4 Lo que **no** hice, aunque cabía

- **No añadí `country_code` a `public_holidays`.** Solo hay Colombia; `ADD COLUMN` es *instant*
  cuando haga falta.
- **No añadí el auxilio de transporte a `smmlv_values`.** No lo pide el documento y no lo usa
  ningún cálculo de este modelo.
- **No añadí `client_request_id` a `security_incidents`.** Se crean a mano, no por proceso.
- **No propuse partición, réplica ni consolidación anticipada** para `company_usage_events`: hoy
  la tabla no existe y no hay ningún número medido que las justifique.

---

## 9. Semilla mínima

Va en el **último changeset** del bloque. Sin ella, tres procesos arrancan calculando con
constantes inventadas.

| Tabla | Fila | Origen verificado |
|---|---|---|
| `uvt_values` | `2026`, `52374.00`, `'Resolucion DIAN 000238 del 15-12-2025'` | valor y resolución confirmados en fuente especializada (INCP, Actualícese) |
| `uvt_values` | `2025`, `49799.00` | valor confirmado; **el número de la resolución que lo fijó NO lo verifiqué** — que lo complete el contador antes de sembrar |
| `smmlv_values` | `2026`, `1750905.00`, `'Decreto 1469 de 2025'`, `status='SUSPENDED'`, `status_reference='Consejo de Estado, Seccion Segunda, auto del 12-02-2026 (suspension provisional); el Gobierno expidio decreto transitorio manteniendo el incremento'`, `status_changed_on='2026-02-12'` | decreto, cifra, fecha del auto y existencia del decreto transitorio confirmados (Holland & Knight, INCP, Ámbito Jurídico). **El número del decreto transitorio NO lo encontré**: lo completa el contador |
| `vat_filing_periods` | `2026`, `'BIMONTHLY'`, `'Art. 600 num. 1 ET - responsable nuevo: el primer ano es bimestral'` | art. 600 ET: bimestral para responsables nuevos y para ingresos brutos ≥ 92.000 UVT; cuatrimestral por debajo |
| `public_holidays` | los festivos de **cada año** que el sistema vaya a calcular, con `moved` y `nominal_date` | Ley 51 de 1983 para los diez trasladables; fecha fija para Año Nuevo, 1 de mayo, 20 de julio, 7 de agosto, 8 y 25 de diciembre, Jueves y Viernes Santo |
| `accounting_periods` | **el primer periodo abierto** | la regla dice que un hecho tardío va al primer periodo abierto; si no hay ninguno, no hay dónde escribirlo. **Quién lo abre y con qué mes es una decisión de negocio que sigue sin tomarse** |
| `accounting_accounts` | el plan de cuentas propio | **no lo puedo escribir yo**: el PUC dejó de ser obligatorio con la adopción de NIIF y cada empresa define el suyo. Lo decide el contador externo |
| `account_mappings` | las doce clases, con sus cuentas | depende del anterior |

**La siembra de festivos es la única con `preCondition`.** `sqlCheck` con `onFail="HALT"` que
compruebe que el año que se siembra no tiene ya filas, y una consulta de vigilancia mensual que
grite si al año siguiente no hay festivos cargados: **sin festivos sembrados, todo plazo en días
hábiles se acorta en silencio** y el fallo aparece el 1 de enero.

---

## 10. Migración y coste

**No hay una sola instrucción de modificación sobre tabla con datos en todo el plan.** Son
creaciones, más cinco `ADD UNIQUE INDEX` sobre tablas existentes (§3), que el manual clasifica
como *In Place*, sin reconstrucción y con **DML concurrente permitido**.

- **Expand/contract no aplica** a este bloque: nada existe todavía y todos los entornos se pueden
  recrear. La única disciplina que sí aplica es la que ya está escrita: **cada tabla nace
  completa**, porque añadir después una columna generada `STORED` —que llevan siete de estas
  tablas— **reconstruye la tabla** y bloquea escrituras.
- **Rollback real, no decorativo.** Cada changeset lleva su `<rollback>`: `dropTable` para las
  creaciones, `dropForeignKeyConstraint` **antes** del `dropTable` cuando la clave apunta hacia
  fuera (el patrón de `331`), `DROP TRIGGER` para los disparadores y `dropUniqueConstraint` para
  los prerrequisitos.
- **Cómo se añadirá una columna dentro de un año, sin parar la aplicación.** Una columna simple es
  `ALGORITHM=INSTANT` en MySQL 8.4 y no cuesta nada. Un backfill se hace **por lotes acotados por
  `id`**, nunca en una sola transacción sobre la tabla entera: sobre `company_usage_events` un
  `UPDATE` completo bloquearía millones de filas contra un pool de diez conexiones. Y si algún día
  hiciera falta un `ALTER` que reconstruya sobre esa tabla, la herramienta es `gh-ost` o
  `pt-online-schema-change`, no una ventana de mantenimiento.

---

## 11. Aislamiento por tenant

| Tabla | `company_id` | Cómo se acota |
|---|---|---|
| `revenue_recognition_lines` | sí | FK compuesta a `subscription_charges(company_id, id)`; la unicidad empieza por `company_id` |
| `company_usage_events` | sí | **cinco** FK compuestas: eje, cargo y las cuatro ramas clínicas, todas con `company_id` dentro. Es lo que impide que un hecho de la clínica A apunte a la mascota de B |
| `company_activity_months` | sí | `uq_cam_month (company_id, period_key)` |
| `security_incident_companies` · `external_invoicing_outage_companies` | sí | puentes; el cliente ve **que** hubo incidente, nunca a cuántos alcanzó |
| las otras trece | **no** | son de plataforma. Sus listados van cerrados a `hasRole('SYSTEM')` |

**Los cinco barridos de plataforma de este bloque** —`ix_rrl_period`,
`ix_accounting_exports_generated`, `ix_tax_returns_firmeza`, `ix_sw_declaration`,
`ix_cam_dormant`, más `ix_security_incidents_unreported` y `ix_eio_open`— **no llevan la empresa
delante a propósito**. Declararlo aquí no exime de nada: `LISTADOS_SIN_EMPRESA_SOLO_SYSTEM` recorre
los casos de uso, no los documentos. Cada uno nace en funcionalidad de plataforma con su
restricción de acceso puesta, y con su hermano acotado por empresa para lo que el cliente necesite
ver.

**Ninguna fuga detectada**, porque ninguna de estas tablas existe todavía. Lo que sí hay es una
trampa de empaquetado: ver el aviso al final del §4.

---

## 12. Fuentes

**Manual de MySQL 8.4** (consultado el 2026-08-27; `dev.mysql.com` devuelve 403 a `curl`, hay que
usar `WebFetch`):

- [CHECK Constraints](https://dev.mysql.com/doc/refman/8.4/en/create-table-check-constraints.html)
  — un `CHECK` que evalúa a `UNKNOWN` acepta la fila; prohibidas las funciones no deterministas,
  las subconsultas y las columnas `AUTO_INCREMENT`. Sostiene §2.2, y con ella los diez `CHECK` de
  dos ramas de esta especificación.
- [InnoDB Online DDL Operations](https://dev.mysql.com/doc/refman/8.4/en/innodb-online-ddl-operations.html)
  — `ADD COLUMN` de una generada `STORED` **reconstruye la tabla**; el índice secundario es *In
  Place* con DML concurrente; la clave foránea es *In Place* solo con `foreign_key_checks`
  desactivado. Sostiene §2.2, §3 y §10.
- [Generated Columns](https://dev.mysql.com/doc/refman/8.4/en/create-table-generated-columns.html)
  — una generada `STORED` puede indexarse y puede ser columna hija de una clave foránea; lo único
  prohibido es `CASCADE`/`SET NULL`/`SET DEFAULT`. Sostiene `fk_tax_returns_vat_frequency` (§5.6).
- [Multiple-Column Indexes](https://dev.mysql.com/doc/refman/8.4/en/multiple-column-indexes.html)
  — la regla del prefijo por la izquierda, que es por lo que `uq_rrl_recognition` sirve además de
  índice de dos claves foráneas.

**Normativa colombiana** (búsqueda del 2026-08-27; ninguna consultada en el sitio de la DIAN,
que no respondió a `curl` — todas en fuente especializada y **todas marcadas como pendientes de
confirmar con un contador**):

- **UVT 2026 = $52.374**, Resolución DIAN 000238 del 15-12-2025 —
  [INCP](https://incp.org.co/publicaciones/infoincp-publicaciones/impuestos/2025/12/dian-fijo-en-52-374-en-valor-de-la-uvt-para-el-ano-gravable-2026/)
  · [Actualícese](https://actualicese.com/uvt-2026/)
- **Art. 714 ET — firmeza**: tres años desde el vencimiento del plazo para declarar; cinco con
  pérdidas fiscales o precios de transferencia —
  [Estatuto.co art. 714](https://estatuto.co/714) ·
  [Gerencie](https://www.gerencie.com/firmeza-de-las-declaraciones-tributarias.html)
- **Art. 632 ET, modificado por el art. 46 de la Ley 962 de 2005 — conservación**: el término es
  el de firmeza de la declaración de renta que los documentos soportan; cinco años para agentes de
  retención y responsables de IVA —
  [Estatuto.co art. 632](https://estatuto.co/632) ·
  [Consejo de Estado, exp. 18971](https://www.consejodeestado.gov.co/documentos/boletines/146/S4/76001-23-31-000-2006-00242-01(18971).pdf)
  · **Es la fuente que corrige los 24 meses del documento maestro (§7.3).**
- **Art. 600 ET — periodicidad del IVA**: bimestral para ≥ 92.000 UVT y para responsables de los
  arts. 477 y 481; cuatrimestral por debajo; **bimestral siempre el primer año** por no haber
  ingresos del año anterior —
  [Actualícese](https://actualicese.com/declaraciones-de-iva-en-2026-periodicidad-bimestral-cuatrimestral-y-anual/)
  · [Gerencie](https://www.gerencie.com/periodo-gravable-en-el-impuesto-a-las-ventas.html)
- **Salario mínimo 2026**: Decreto 1469 de 2025 fijó $1.750.905 (+23 %); el Consejo de Estado,
  Sección Segunda, lo **suspendió provisionalmente** por auto del 12-02-2026 y ordenó un decreto
  transitorio; el fondo no llegaría antes de 2027 —
  [Holland & Knight](https://www.hklaw.com/en/insights/publications/2026/02/suspension-provisional-del-decreto-que-fijo-el-salario-minimo)
  · [INCP](https://incp.org.co/publicaciones/infoincp-publicaciones/impuestos/2026/02/consejo-de-estado-suspendio-provisionalmente-el-decreto-que-fijo-el-salario-minimo-para-2026/)
- **Ley 51 de 1983 (Emiliani)** — los diez festivos que se trasladan al lunes siguiente y los que
  no —
  [Función Pública](https://www.funcionpublica.gov.co/eva/gestornormativo/norma.php?i=4954)
- **El caso del 1 de julio de 2019**, dos efemérides en el mismo lunes observado, que es por lo
  que `nominal_date` no puede ser única —
  [Calendario de Colombia](https://www.calendariodecolombia.com/festivo/2019/sagrado-corazon)

**Modelado:**

- *SQL Antipatterns* (Bill Karwin), cap. 8 — *Polymorphic Associations*: la razón por la que
  `company_usage_events` lleva cuatro claves foráneas y no un par tipo+identificador —
  <https://pragprog.com/titles/bksap1/sql-antipatterns-volume-1/>
- *Parallel Change* (Fowler) — <https://martinfowler.com/bliki/ParallelChange.html>
- *Evolutionary Database Design* (Fowler/Sadalage) — <https://martinfowler.com/articles/evodb.html>

---

## 13. Lo que sigue pendiente de un humano con conocimiento contable

Ninguna de estas la puedo cerrar leyendo código, y ninguna debe adivinarse.

1. **El plan de cuentas propio.** El PUC dejó de ser obligatorio con la adopción de NIIF; sin las
   filas de `accounting_accounts` y sus doce mapeos, ningún asiento se puede generar. Lo decide el
   contador externo.
2. **Firmeza de tres o de cinco años.** Depende de si VetSoftware compensa pérdidas fiscales. Son
   dos años de almacenamiento de más o de menos sobre la tabla más grande del diseño (§7.3).
3. **Quién abre el primer periodo contable y con qué mes.** La regla del hecho tardío no funciona
   sin al menos uno abierto, y nadie lo ha decidido.
4. **El número del decreto transitorio del salario mínimo de 2026** y **el de la resolución de la
   UVT de 2025.** Los dos valores están confirmados; las dos referencias normativas, no.
5. **Si el servicio está gravado o excluido de IVA.** Decide `tax_treatment`, y por tanto qué
   mapeos de `VAT_PAYABLE` y `VAT_CREDITABLE` hay que sembrar. Está declarado bloqueante desde el
   principio del documento maestro y sigue abierto.
6. **Confirmar que el disparador de §6.2 puede leer su propia tabla.** Es de código, no contable,
   pero **no lo he medido**: exige crear el disparador y yo no escribo en ninguna base.
