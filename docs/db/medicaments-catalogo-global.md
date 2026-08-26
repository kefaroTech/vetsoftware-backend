# `medicaments` como catálogo GLOBAL de plataforma — auditoría de esquema

**Fecha:** 2026-08-26 · **Alcance:** tabla `medicaments` y sus dependientes
(`medicament_prescriptions`, `medicament_species_warnings`).
**Método:** lectura de código exclusivamente — changesets de Liquibase, entidades JPA y
repositorios. **No se consultó ninguna base de datos**, ni local ni dev (regla dura de la tarea).
Todo lo que exige una base viva queda marcado como *no medido*.

---

## 1. Definición real de la tabla

La tabla no está declarada en un solo sitio: es la suma de cuatro changesets. Reconstrucción a
partir del árbol (`VetSoftware/src/main/resources/db/changelog/migrations/`), todos registrados en
`db.changelog-master.xml` (líneas 174, 223, 385, 386, 389).

| Columna | Tipo | Nulabilidad / default | Origen |
|---|---|---|---|
| `id` | `BIGINT AUTO_INCREMENT` | PK, `NOT NULL` | `173_create_medicaments.xml` · changeset `173a_create_medicaments` |
| `name` | `VARCHAR(200)` | `NOT NULL` | `173a` |
| `description` | `VARCHAR(500)` | `NULL` | `173a` |
| `company_id` | `BIGINT` | `NULL` (FK a `companies(id)`) | `173a` |
| `general` | `BOOLEAN` (→ `TINYINT`) | `NOT NULL DEFAULT FALSE` | `173a` |
| `created_date` | `DATETIME` | `NOT NULL DEFAULT CURRENT_TIMESTAMP` | `173a` |
| `enabled` | `BOOLEAN` | `NOT NULL DEFAULT TRUE` | `173a` |
| `version` | `BIGINT` | `NOT NULL DEFAULT 0` | `225_add_version_optimistic_lock_wave2.xml:557` · `225_add_version_to_medicaments` |
| `owner_scope` | `BIGINT GENERATED ALWAYS AS (COALESCE(company_id,0)) STORED` | derivada | `285_fix_unique_index_catalogs_with_company.xml:172` · `285_fix_unique_index_medicaments` |
| `active_name` | `VARCHAR(200) GENERATED ALWAYS AS (CASE WHEN enabled = TRUE THEN name ELSE NULL END) STORED` | derivada | `285` (mismo changeset) |
| `atcvet_code` | `VARCHAR(10)` | `NULL` | `289_add_medicament_clinical_fields.xml:33` · `289_add_medicament_clinical_fields` |
| `controlled_substance` | `BOOLEAN` | `NOT NULL DEFAULT FALSE` | `289` |
| `ica_registration` | `VARCHAR(20)` | `NULL` | `289` |

**Constraints**

| Nombre | Tipo | Definición | Origen |
|---|---|---|---|
| (PK) | PRIMARY KEY | `(id)` | `173a` |
| `fk_medicaments_company` | FOREIGN KEY | `(company_id) → companies(id)`, **sin `ON DELETE` ni `ON UPDATE`** → `RESTRICT`/`NO ACTION` | `173a` |
| `uq_medicaments_owner_active_name` | UNIQUE | `(owner_scope, active_name)` | `285_fix_unique_index_medicaments` |
| `ck_medicaments_owner_xor` | CHECK | `((general = TRUE AND company_id IS NULL) OR (general = FALSE AND company_id IS NOT NULL))` | `286_add_general_xor_company_check.xml:105` · `286_check_general_xor_company_medicaments` |
| ~~`uq_medicaments_name`~~ | UNIQUE `(name)` | **RETIRADO** por `285` (`DROP INDEX uq_medicaments_name`) | creado en `173a`, eliminado en `285` |

**Índices.** No hay ni un `createIndex` sobre `medicaments` en los 291 changesets (verificado con
`grep -rn 'medicaments' --include=*.xml | grep -i index`). Los tres que existen son:

1. `PRIMARY (id)` — clustered.
2. `fk_medicaments_company (company_id)` — creado automáticamente por InnoDB al declarar la FK
   inline. Una sola columna.
3. `uq_medicaments_owner_active_name (owner_scope, active_name)`.

**No existe ningún índice cuya primera columna sea `name`.** Es consecuencia deliberada de `285`,
no un olvido.

**Semilla.** `173b` sembró 6 filas globales; `173d` hizo backfill de nombres ya recetados (cantidad
desconocida desde el árbol, varía por entorno); `299_seed_global_medicament_catalog.xml` añade 153
moléculas globales más de forma idempotente (`INSERT ... SELECT ... WHERE NOT EXISTS`). Verificado
por script sobre el XML: ningún `name` supera 200 caracteres y ninguna `description` supera 500
(máxima observada: 283, en «Permetrina»), así que la semilla no puede reventar por error 1406.

---

## 2. Unicidad del nombre: existe y es POR ÁMBITO, no global

Desde `285`, la unicidad es `UNIQUE (owner_scope, active_name)`, donde
`owner_scope = COALESCE(company_id, 0)` (el `0` es la plataforma) y `active_name` vale `NULL`
cuando `enabled = FALSE`. Es el patrón de la casa para índice único parcial, que MySQL no tiene
como tal: se emula con columna generada `STORED`
(<https://dev.mysql.com/doc/refman/8.4/en/create-table-generated-columns.html>).

### El escenario planteado

> El superusuario crea el global «Amoxicilina» y la empresa 7 ya tiene el suyo propio con ese
> nombre.

**Qué pasa hoy:** el `INSERT` global escribe la clave `(owner_scope = 0, active_name =
'Amoxicilina')`; la fila de la empresa 7 ocupa `(7, 'Amoxicilina')`. **Son claves distintas: no
colisionan y las dos filas conviven.** La constraint hace exactamente lo correcto.

**Qué debería pasar:** eso mismo. Un vademécum de plataforma no puede quedar bloqueado porque una
de N clínicas se adelantó con el nombre, ni al revés. Es la doctrina de multi-tenancy con
discriminador: la clave natural es única *por tenant*, nunca globalmente
(<https://docs.citusdata.com/en/stable/use_cases/multi_tenant.html>).

**El efecto secundario que sí queda abierto** y que nadie había registrado: a la empresa 7,
`GET /medicaments/available` (`findAllByGeneralTrueOrCompany_Id`,
`MedicamentJpaRepository.java:45-46`) le devuelve **dos filas llamadas «Amoxicilina»** —la global y
la suya— sin ninguna deduplicación. El esquema es correcto; la lista que ve el veterinario, no. El
`MedicamentResponse` sí lleva `general` y `company`, así que el front puede distinguirlas, pero hoy
no hay nada en el backend que lo obligue ni que ofrezca «adoptar la global y retirar la propia».
Con las 153 moléculas de `299` recién sembradas, el número de colisiones potenciales pasa de
teórico a real de golpe. → **issue #591**.

### ¿Hace falta un único parcial «solo para `general = true`»?

**No, y además sería un retroceso.** Un `UNIQUE` sobre `name` restringido a `general = TRUE` es
justo el caso particular que `uq_medicaments_owner_active_name` ya cubre: las filas globales tienen
todas `owner_scope = 0`, así que el par `(0, active_name)` **ya impone unicidad del nombre dentro
del vademécum de plataforma y solo dentro de él**. Añadir otra constraint sería redundante y
pagaría escritura en cada `INSERT`.

Para el registro, y porque la pregunta es legítima: MySQL 8.4 no tiene índices únicos parciales
(eso es PostgreSQL) ni índices `INCLUDE`. Las dos únicas formas de expresar una unicidad
condicional son (a) la columna generada `STORED` que vale `NULL` fuera de alcance —el patrón de
`285`, `226`, `210` y `206`— o (b) un índice funcional sobre una expresión, que MySQL implementa
internamente como una columna generada oculta. Es decir: **la opción (b) es la (a) con otra
sintaxis**, y el repositorio ya eligió la (a) de forma explícita. Cualquier necesidad futura de
unicidad condicional en esta tabla debe reutilizar ese patrón, no inventar uno segundo.

---

## 3. La invariante `general XOR company_id`: protegida en la base

**Sí, hay CHECK.** `ck_medicaments_owner_xor`, en
`286_add_general_xor_company_check.xml:105-116`:

```sql
CHECK ((general = TRUE AND company_id IS NULL) OR (general = FALSE AND company_id IS NOT NULL))
```

MySQL 8.4 los aplica de verdad (antes de 8.0.16 se parseaban y se ignoraban). El changeset lleva
además `preCondition` con `sqlCheck expectedResult="0"` y `onFail="HALT"`, así que si alguna fila ya
violara la regla el despliegue se para en limpio en vez de fallar a medio `ALTER`.

**Merecía la pena, y el razonamiento del propio changeset es el correcto:** sin el CHECK, la
invariante solo vivía en `Medicament.validate` (`Medicament.java:50-53`), que se evalúa **al
construir el objeto de dominio, es decir al LEER**. Una fila mal formada no rompe el `INSERT` que la
creó: rompe el primer `findAll` que la mapee, con `IllegalArgumentException`, y tumba el listado del
catálogo para todos los tenants a la vez —incluido el listado SYSTEM de la consola— no solo para
quien la creó. Ese es el modo de fallo que el CHECK elimina, y es exactamente el argumento de por
qué una invariante que solo vive en Java no es una invariante.

Con la administración desde consola el argumento se refuerza: la consola será el único sitio desde
el que se escriban filas con `company_id NULL`, así que es el único camino capaz de fabricar la
combinación prohibida `general = FALSE, company_id = NULL`.

---

## 4. Quién referencia `medicaments` y qué pasa al pausar un global

Solo dos tablas tienen FK a `medicaments(id)` (verificado con
`grep -rn 'references="medicaments\|referencedTableName="medicaments"'`):

| Tabla hija | Constraint | Columna | `ON DELETE` | Origen |
|---|---|---|---|---|
| `medicament_prescriptions` | `fk_medicament_prescriptions_medicament` | `medicament_id BIGINT NOT NULL` | ninguno → `RESTRICT` | `173_create_medicaments.xml:100-104` · `173e_medicament_id_not_null_and_fk` |
| `medicament_species_warnings` | `fk_medicament_species_warnings_medicament` | `medicament_id BIGINT NOT NULL` | ninguno → `RESTRICT` | `289_add_medicament_clinical_fields.xml:66-70` |

### El borrado es lógico, así que la FK nunca llega a disparar

`MedicamentJpaEntity` lleva
`@SQLDelete("UPDATE medicaments SET enabled = false WHERE id = ? AND version = ?")` y
`@SQLRestriction("enabled = true")` (`MedicamentJpaEntity.java:11-12`). `repository.delete(id)` no
emite `DELETE`: pone `enabled = false`. El `RESTRICT` de InnoDB queda como red de seguridad para el
único caso en que alguien lance un `DELETE` físico (una migración, una mano en la consola), y ahí
hace lo correcto: **impide borrar un medicamento con recetas**, que es lo que debe pasar con un
histórico clínico.

### Qué le pasa a una receta histórica cuando el global se pausa — **nada, y está verificado**

La cadena completa, leída en el código:

1. **Guarda previa.** `DeleteMedicamentService.java:36-38` consulta `existsActiveByMedicamentId(id)`
   → `MedicamentPrescriptionJpaRepository.existsByMedicament_Id`, sobre una entidad con
   `@SQLRestriction("enabled = true")`: **si hay una sola receta activa de cualquier empresa
   apuntando al global, la pausa se rechaza** con `MedicamentHasActiveChildrenException`. La
   comprobación es cross-tenant a propósito y para un global es lo correcto.
2. **Si se pausa igualmente** (todas sus recetas ya estaban dadas de baja), la fila desaparece de
   toda lectura que pase por el `@SQLRestriction`.
3. **La receta histórica sobrevive** porque `medicament_prescriptions` guarda un **snapshot del
   nombre**: `MedicamentPrescriptionJpaEntity.java:19-21` (`// Snapshot histórico del nombre del
   medicamento al momento de recetar`). Y el mapper **no desreferencia la asociación**:
   `MedicamentPrescriptionJpaMapper.java:32-35` lee `entity.getMedicament().getId()` —el id de un
   proxy LAZY se obtiene sin inicializarlo— y el nombre del snapshot de la propia fila. Por eso **no
   salta `EntityNotFoundException` al leer una receta cuyo medicamento está pausado**, que es el
   fallo clásico al combinar `@SQLRestriction` con un `@ManyToOne` obligatorio. El comentario del
   mapper deja constancia de que es deliberado.
4. **Lo que sí se pierde** es la capacidad de volver a recetarlo: `findAvailableById`
   (`MedicamentJpaRepository.java:22-33`) hereda el `@SQLRestriction`, así que una receta nueva
   contra un global pausado da 404. Correcto.
5. **La pausa libera el nombre** (`active_name` pasa a `NULL` y MySQL no deduplica `NULL`), y la
   tabla admite una fila activa y N pausadas con el mismo nombre. El riesgo de que un «Amoxicilina»
   global recreado nazca con **otro `id`** y parta en dos la trazabilidad de las recetas está
   **mitigado**: `CreateMedicamentService.java:57-61` busca primero con
   `findGlobalByNameIncludingDisabled` y, si el nombre lo ocupa una fila pausada, **la reactiva en
   vez de insertar otra** (`reactivateWithDetails`), preservando el `id` y por tanto las FK. La
   mitigación depende de que el nombre coincida exactamente bajo `utf8mb4_0900_ai_ci`; si la
   plataforma lo recrea con otra grafía, sí nacen dos ids para la misma molécula.

**Veredicto del punto 4: el riesgo de pausar un global referenciado por recetas de varias empresas
está cubierto, y lo está en tres capas independientes** —guarda de hijos activos, snapshot del
nombre en la línea recetada, y FK `RESTRICT` contra el borrado físico—. No hace falta tocar nada.

---

## 5. Índices frente a las consultas de la pantalla de administración

Volumen de referencia (contado en el árbol, **no medido en base**): 6 filas globales de `173b` + 153
de `299` + el backfill de `173d` (desconocido) + las propias de cada clínica. Orden de magnitud
actual: **unos cientos de filas**. Proyección con 50 clínicas, vademécum de 500 moléculas y 30
propias por clínica: ~2.000 filas. En una tabla de ese tamaño InnoDB resuelve un escaneo completo
desde el buffer pool sin que se note; **por eso ningún índice de esta sección es bloqueante y la
mayoría no debe crearse todavía**.

| Consulta de la consola | Método | Índice que la sirve hoy | Dictamen |
|---|---|---|---|
| Listado paginado de todos, `ORDER BY name, id` | `JpaMedicamentRepository.findAll:57-58` (SYSTEM, `ListMedicamentsUseCase:21`) | ninguno → escaneo completo + `filesort` | Aceptable a cientos de filas. **No crear índice todavía.** |
| Filtrar «solo globales» | (no existe aún) | `fk_medicaments_company (company_id)`: InnoDB indexa los `NULL` y resuelve `company_id IS NULL` por el índice | **Ya cubierta.** Filtrar por `company_id IS NULL`, no por `general = TRUE`. |
| Filtrar por `general` | (no existe aún) | ninguno | **No crear índice sobre `general`.** Cardinalidad 2: un índice de baja selectividad no lo usa el optimizador y solo paga escritura (Use The Index, Luke!, <https://use-the-index-luke.com/>). Equivale a la fila anterior. |
| Filtrar por empresa | `findAllDisabledForCompany:51-56`, `findByIdAndCompany_Id:43` | `fk_medicaments_company (company_id)` | **Ya cubierta.** |
| Guarda de nombre al crear/editar un GLOBAL: `WHERE name = ? AND company_id IS NULL` | `findGlobalByNameIncludingDisabled:153-161`; `reactivateWithDetails(id,name,desc):198-205` | solo `fk_medicaments_company`: acota a «todas las globales» y filtra `name` fila a fila | **El único hueco real.** Ver abajo. |
| Búsqueda por nombre | (no existe aún) | ninguno | Si es `LIKE '%texto%'`, **ningún índice B-tree puede servirla**, ni existente ni futuro. Si se acepta prefijo (`LIKE 'texto%'`), la sirve el índice propuesto. |

### El único índice que recomiendo, y es de coste neto cero

```sql
-- propuesto (NO bloqueante)
ALTER TABLE medicaments
    ADD INDEX ix_medicaments_company_name (company_id, name),
    DROP INDEX fk_medicaments_company;
```

Razones, en orden:

- **Sirve las dos guardas de nombre**, la de empresa (`company_id = ? AND name = ?`) y la de
  plataforma (`company_id IS NULL AND name = ?`), que son las consultas que la consola de
  administración ejecutará en **cada alta y cada edición** de un global. Hoy la segunda recorre todo
  el vademécum global —la partición más grande de la tabla y la única que la nueva feature hace
  crecer— para filtrar por nombre.
- **Sustituye al índice de la FK sin dejarla huérfana**: InnoDB acepta como soporte de una FK
  cualquier índice cuya columna izquierda sea la de la clave, y `company_id` lo es. Es la regla del
  prefijo por la izquierda del manual —«any leftmost prefix of the index can be used by the
  optimizer», <https://dev.mysql.com/doc/refman/8.4/en/multiple-column-indexes.html>— y el mismo
  movimiento que ya hizo `287_drop_redundant_fk_index_breeds_colors.xml`.
- **El número de índices de la tabla no cambia**: se cambia uno de una columna por uno de dos. El
  coste de escritura por `INSERT` es prácticamente el mismo y no se añade espacio significativo.
- No sirve para el `ORDER BY name` del listado general (su primera columna es `company_id`), y está
  bien: ese listado no lo necesita.

**Lo que NO recomiendo crear:** índice sobre `general`, sobre `enabled`, sobre `created_date`, ni
`(enabled, name, id)` para matar el `filesort` del listado. A este volumen son deuda de escritura
sin contrapartida medible. Si algún día el listado de la consola se nota lento, primero la medición
(`EXPLAIN ANALYZE`) y después el índice.

**Nota sobre `/medicaments/available`** (camino del tenant, no de la consola, y por tanto el más
caliente): `findAllByGeneralTrueOrCompany_Id` genera
`WHERE enabled = 1 AND (general = true OR company_id = ?)`. El `OR` sobre dos columnas distintas es
lo que impide usar índice. Reescrito como `owner_scope IN (0, :companyId)` lo serviría el índice
único `uq_medicaments_owner_active_name`, que ya tiene `owner_scope` como primera columna — **cero
índices nuevos**. Es un cambio en `src/` (territorio de `backend-feature`) y solo merece la pena si
la medición lo pide; queda anotado, no propuesto.

---

## 6. Veredicto

> **El esquema de `medicaments` soporta el catálogo GLOBAL administrado desde la consola TAL CUAL.
> No hace falta ninguna migración bloqueante.**

Lo soporta porque los cuatro elementos que la feature necesita ya están en la base, y no solo en
Java:

1. La dualidad global/tenant está modelada (`company_id` nullable + `general`) **y forzada** por
   `ck_medicaments_owner_xor` (`286`).
2. La unicidad del nombre es **por ámbito** (`uq_medicaments_owner_active_name`, `285`): el global y
   el propio de una clínica pueden llamarse igual, y la baja lógica libera el nombre.
3. El histórico clínico está a salvo de la pausa de un global: guarda de hijos activos + snapshot
   del nombre en `medicament_prescriptions` + FK `RESTRICT`.
4. El listado de la consola existe, está paginado y es **SYSTEM-only** por `@PreAuthorize`
   (`ListMedicamentsUseCase.java:21`), así que la regla dura `LISTADOS_SIN_EMPRESA_SOLO_SYSTEM`
   (BE-29) se cumple.

**Lo que falta no es esquema, es `src/`** —y por tanto no es mío ni de `db-migrations`, sino de
`backend-feature`—:

| Qué falta | Dónde | Issue |
|---|---|---|
| No hay camino de escritura para un global: `MedicamentController.java:63-64` fija `authz.currentCompanyId()` y `general = false`, y `currentCompanyId()` lanza `AccessDeniedException` para `SystemContext` (`Authz.java:52`) | `MedicamentController` | ya cubierto por **#590** (y **#568** para el contenido del catálogo) |
| `DeleteMedicamentService:32-35` y `UpdateMedicamentService:43-46` no llevan `.filter(Medicament::isGeneral)` en la rama SYSTEM: al abrir el camino, la consola podrá pausar el medicamento **privado** de una clínica | slice `medicament` | **#590**, abierto |
| `MedicamentJpaEntity.java:18` sigue declarando `unique = true` sobre `name`, el índice que `285` borró | entidad | **#585**, abierto |
| `atcvet_code`, `controlled_substance`, `ica_registration` existen como columnas (`289`) pero **no están mapeadas en la entidad ni las llena `299`**: la consola no podrá editarlas | entidad + feature | **#567**, abierto |

### Cambios de esquema para `db-migrations`, en orden

**Bloqueantes: ninguno.** La pantalla se puede construir sobre el esquema actual.

**Opcionales, por orden de valor:**

1. **`ix_medicaments_company_name (company_id, name)` + `DROP INDEX fk_medicaments_company`** — un
   solo changeset, DDL exacto en §5. `ALGORITHM=INPLACE`: permite DML concurrente y no reconstruye
   la tabla (<https://dev.mysql.com/doc/refman/8.4/en/innodb-online-ddl-operations.html>); sobre
   cientos de filas es instantáneo en la práctica. `preCondition onFail="HALT"`:
   `indexExists fk_medicaments_company` + `not indexExists ix_medicaments_company_name`.
   `<rollback>`: recrear `fk_medicaments_company (company_id)` **antes** de borrar el nuevo, para no
   dejar la FK sin índice de soporte ni un instante. → **issue #592**.
2. Nada más. Cualquier otra columna (`updated_date`, `updated_by`, catálogo de principios activos,
   `atcvet_code` como FK a una tabla ATCvet real) es una decisión de producto que hoy no tiene
   evidencia que la pida.

---

## Medido / no medido

**Medido (leyendo el árbol):** definición completa de la tabla y sus cuatro changesets, las dos FK
entrantes, la ausencia total de `createIndex` sobre `medicaments`, las 13 consultas de
`MedicamentJpaRepository`, la cadena completa del borrado lógico, y las longitudes de la semilla
`299` (153 pares nombre/descripción, ninguno fuera de rango).

**NO medido — requiere base de datos y la tarea lo prohíbe expresamente:**

- Filas reales de `medicaments` por entorno, y en particular cuántas metió el backfill `173d`.
- **Cuántos nombres propios de clínicas colisionan con las 153 moléculas de `299`** — el número que
  dimensiona el duplicado del §2. Es la comprobación más valiosa que quedó pendiente.
- La collation efectiva de `medicaments.name` y `medicament_prescriptions.name`: ningún
  `CREATE TABLE` del repositorio declara charset ni collation, así que heredan el default del
  servidor (`utf8mb4_0900_ai_ci` salvo que el parameter group diga otra cosa). Toda la lógica de «el
  nombre ya existe» depende de que esa collation sea la que se supone.
- Cualquier plan de ejecución. No hay ni un `EXPLAIN` detrás de este informe: las afirmaciones de §5
  se apoyan en la regla del prefijo por la izquierda del manual, no en un plan observado.

## Fuentes citadas

- <https://dev.mysql.com/doc/refman/8.4/en/multiple-column-indexes.html> — regla del prefijo por la
  izquierda; sostiene que `(company_id, name)` puede sustituir al índice de soporte de la FK.
- <https://dev.mysql.com/doc/refman/8.4/en/create-table-generated-columns.html> — columnas generadas
  `STORED`; base del patrón de unicidad condicional de `285`.
- <https://dev.mysql.com/doc/refman/8.4/en/innodb-online-ddl-operations.html> — coste del `ALTER`
  propuesto (`INPLACE`, sin reconstrucción, con DML concurrente).
- <https://use-the-index-luke.com/> — por qué no se indexa una columna de cardinalidad 2.
- <https://docs.citusdata.com/en/stable/use_cases/multi_tenant.html> — la clave natural en un
  esquema multi-tenant es única por tenant, no globalmente.

🤖 Generated with [Claude Code](https://claude.com/claude-code)
