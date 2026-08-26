# Semilla de `laboratory_test_types` y `diagnostic_imaging_types`

**Especificación de datos semilla. NO es un changeset.** Este documento es el insumo con el que
`db-migrations` escribe los changesets `285` y `286`. Quien lo lea para implementarlo no tiene que
decidir nada: tipo, longitud, `general`, `company_id`, `preCondition` y `<rollback>` están fijados
abajo. Lo que sí tiene que hacer es leer primero **§2**, porque hay un defecto de modelado vivo
que condiciona cuántas filas es prudente sembrar.

- **Autor:** agente `db-modeling` · **Fecha:** 2026-08-25
- **Verificado contra:** árbol de `VetSoftware/` en esa fecha, leyendo changesets y entidades. **No
  se consultó la base de datos de dev ni se levantó ningún servicio.**
- **Alcance:** dos catálogos clínicos globales. No toca ninguna otra tabla.

---

## 0. Resumen ejecutable

| | `laboratory_test_types` | `diagnostic_imaging_types` |
|---|---|---|
| Filas semilla propuestas | **87** | **21** |
| `company_id` | `NULL` en todas | `NULL` en todas |
| `general` | `TRUE` en todas — **obligatorio explícito** (§3.2) | `TRUE` en todas |
| Granularidad de la fila | **unidad ordenable/facturable** (perfil o prueba suelta) | **modalidad + variante técnica**, nunca la región anatómica |
| Changeset propuesto | `285_seed_laboratory_test_types.xml` | `286_seed_diagnostic_imaging_types.xml` |
| Coste del cambio | `INSERT` de 87 filas en tabla vacía — no hay `ALTER`, no hay reconstrucción | ídem, 21 filas |

---

## 1. El punto de partida real — qué hay hoy, verificado

### 1.1 De dónde salen las dos tablas

`laboratory_test_types` **no tiene changeset de creación propio**, y por eso no aparece buscando su
nombre en las migraciones. Nació con otro nombre:

| Tabla | Nace en | Gana `company_id` + `general` | Gana `enabled` | Gana `version` |
|---|---|---|---|---|
| `laboratory_test_types` | `037_create_test_types.xml:8` **como `test_types`**, renombrada en `059_rename_test_types_to_laboratory_test_types.xml:8` | `056_alter_test_types_add_company_and_general.xml` | `068_add_enabled_to_all_tables.xml:302` | `225_add_version_optimistic_lock_wave2.xml:447` |
| `diagnostic_imaging_types` | `053_create_diagnostic_imaging_types.xml:8` | `058_alter_diagnostic_imaging_types_add_company_and_general.xml` | `068_add_enabled_to_all_tables.xml:118` | `225_add_version_optimistic_lock_wave2.xml:491` |

**Consecuencia práctica para quien busque:** `grep laboratory_test_types` sobre las migraciones
devuelve solo `068` y `225`. El DDL de la tabla está bajo el nombre `test_types`. Cualquier
auditoría que dé por supuesto que la tabla no existe porque no aparece su `createTable` se equivoca.

### 1.2 Esquema efectivo de las dos tablas (idéntico)

```
id           BIGINT       AUTO_INCREMENT, PK
name         VARCHAR(100) NOT NULL, UNIQUE  <-- índice único ANÓNIMO y GLOBAL (§2)
description  VARCHAR(500) NOT NULL          <-- no admite NULL desde la BD
company_id   BIGINT       NULL, FK -> companies(id)
general      BOOLEAN      NOT NULL, DEFAULT FALSE   <-- ojo con el default (§3.2)
created_date DATETIME     NOT NULL, DEFAULT CURRENT_TIMESTAMP
version      BIGINT       NOT NULL, DEFAULT 0
enabled      BOOLEAN      NOT NULL, DEFAULT TRUE
```

Índices existentes: la PK y el único de `name`. Más el índice de una sola columna sobre `company_id`
que InnoDB crea solo por la FK. **No hay ningún índice compuesto y no hace falta ninguno** (§6).

### 1.2.1 La collation efectiva: `utf8mb4_0900_ai_ci` — y por qué cambia el trabajo

**Verificado en tres niveles** (`@@collation_server`, el default del esquema `vetsoftware` y una
columna `name` real): es `utf8mb4_0900_ai_ci`. Ninguna migración del repo declara `COLLATE`, así que
las 105 tablas lo heredan del servidor.

`ai` es *accent insensitive* y `ci` es *case insensitive*. Para el índice único de `name` eso
significa que **estos cinco son el mismo valor**:

```
Uroanálisis  ==  Uroanalisis  ==  UROANÁLISIS  ==  uroanalisis  ==  UroAnalisis
```

Dos consecuencias, y las dos son operativas, no teóricas:

1. **Si dos filas de la semilla difieren solo en acento o en caja, el `INSERT` falla** y con él el
   changeset entero, porque los 87 `<insert>` van en un solo `changeSet` transaccional. Esto se
   comprueba **antes** de escribir el XML, y está comprobado en §3.5.
2. **Agrava el problema de namespace de §2.** Sembrar «Hemograma» no le quita a la clínica ese
   nombre: le quita además «hemograma», «HEMOGRAMA», «Hemógrama» y toda variante de caja y acento.
   Un tenant que intente distinguir su tipo propio poniéndolo en mayúsculas **no puede**: para el
   índice es el mismo nombre.

Lo que la collation **no** ignora es la puntuación ni los espacios: `ALT (TGP)` y `ALT(TGP)` sí son
valores distintos, y `Hemograma completo` y `Hemograma  completo` (dos espacios) también. Eso no es
un alivio, es una trampa distinta: dos filas casi idénticas que el índice deja pasar y que el
usuario no sabe distinguir en un desplegable.

### 1.3 Las dos tablas se despliegan vacías — confirmado

Los únicos changesets con `<insert>`, `<loadData>` o `<sqlFile>` de semilla en todo el árbol son
`022`, `148`, `173`, `212`, `215`, `255` y `284`. **Ninguno toca estas dos tablas.** El catálogo
clínico llega vacío a cualquier entorno recién levantado.

### 1.4 Cómo se consume el catálogo — esto acota la granularidad

`VetSoftware/src/main/java/com/vetsoftware/app/laboratorytesttype/infrastructure/persistence/JpaLaboratoryTestTypeRepository.java:55-58`

```java
public List<LaboratoryTestType> findAllAvailableForCompany(Long companyId) {
    return jpaRepository.findAllByGeneralTrueOrCompany_Id(companyId)...
}
```

Es decir: cada clínica ve **`general = TRUE` unión lo suyo**, más el `enabled = true` que impone el
`@SQLRestriction` de la entidad. El listado sin tenant (`findAll`) está reservado a `ROLE_SYSTEM`
por BE-29, documentado en `ListLaboratoryTestTypesUseCase.java:8-19`. `DiagnosticImagingType` es
simétrico, incluida la separación deliberada entre `findAvailableById` (lectura, incluye lo global)
y `findByIdAndCompany_Id` (escritura, excluye lo global) que está comentada en
`DiagnosticImagingTypeJpaRepository.java:36-44`.

**Aislamiento por tenant: correcto.** Estas filas semilla son globales a propósito, y el camino de
escritura ya impide que una clínica edite o borre una fila global.

### 1.5 Y sobre todo: cómo se registra una prueba hecha

Esto es lo que fija la granularidad, y no está en el catálogo sino en la tabla de la orden:

- `038_create_laboratory_tests.xml` — `laboratory_tests` tiene **un solo** `test_type_id NOT NULL`,
  un `quantity INT` y un `diagnosis VARCHAR(2000)` de texto libre. **No hay tabla de resultados por
  analito**: ni valor numérico, ni unidad, ni rango de referencia, ni composición del perfil.
- `054_create_diagnostic_imagings.xml` — `diagnostic_imagings` tiene `diagnostic_imaging_type_id` y,
  además, **`study_type VARCHAR(200) NOT NULL` de texto libre**.

Ese `study_type` ya es el segundo nivel del modelo de imagen. Sembrar «Radiografía de tórax» en el
catálogo duplicaría un eje que la tabla de la orden ya tiene.

---

## 2. El defecto de modelado que condiciona todo: `name` es UNIQUE **global**

> **[Grave]** El índice único de `name` no incluye `company_id` — `037_create_test_types.xml:12-14`
> y `053_create_diagnostic_imaging_types.xml:12-14`
>
> **Criterio:** en un esquema multi-tenant con discriminador, una clave natural es única **por
> tenant**, no globalmente; el tenant va primero en toda clave e índice
> (<https://docs.citusdata.com/en/stable/use_cases/multi_tenant.html>). Es el antipatrón de clave
> natural que no modela la invariante real (*SQL Antipatterns*, Karwin). Precedente ya cerrado en
> este repo: **#427**, el único global de `client_request_id` en `quotes`, que dejaba bloqueado al
> segundo tenant que reutilizara una llave.
>
> **Impacto:** `laboratory_test_types` y `diagnostic_imaging_types` **sí tienen `company_id`** y
> permiten filas propias de empresa (`general = FALSE`). Con el único global, el nombre es un recurso
> compartido: la primera clínica que cree «Hemograma» se lo quita a todas las demás, y cada fila que
> sembremos como global se lo quita a todas de entrada. El error que recibe la segunda clínica es un
> `DataIntegrityViolationException` sin traducir que además **no le puede decir cuál es la fila en
> conflicto**, porque puede pertenecer a otro tenant y ella no la ve en ningún listado.
>
> **Y el alcance del daño es mayor de lo que parece, por la collation (§1.2.1).** Con
> `utf8mb4_0900_ai_ci`, cada nombre quemado se lleva por delante **todas sus variantes de caja y de
> acento**: sembrar «Hemograma» bloquea también «hemograma», «HEMOGRAMA» y «Hemógrama». La salida
> intuitiva del usuario —«lo escribo en mayúsculas y ya»— **no funciona**, y eso convierte un error
> confuso en un error del que no se sale sin cambiar de verdad el nombre.
>
> **Blast radius:** el mismo patrón (`company_id` NULL + `general` + `name` UNIQUE global) está en
> **cuatro** catálogos: `laboratory_test_types`, `diagnostic_imaging_types`, `surgery_types`
> (`051` + `057`) y `vaccination_types` (`034` + `055`). `consultation_types` (`032`) y `spa_types`
> (`043`) tienen el único global pero **sin** `company_id`, así que ahí el defecto es solo el de
> #482 (borrado lógico que quema el nombre), no este.
>
> **Arreglo (no es de esta tanda):** ver §7.

**Cuántas filas es prudente sembrar, a la luz de esto.** La respuesta no es «las mínimas». La
mitigación correcta no pasa por sembrar poco, sino por cómo se nombra lo que se siembra, y es
gratis:

> **Usar el nombre canónico completo y desambiguado en la semilla, y dejar libre el nombre corto y
> coloquial para los tenants.**

Se siembra `Hemograma completo (cuadro hemático)`, no `Hemograma`. Se siembra
`Ecografía abdominal completa`, no `Ecografía`. Esto ya es lo correcto en un catálogo clínico por sí
solo —el nombre de una prueba es su nombre técnico—, y como efecto colateral devuelve a cada clínica
el namespace que de verdad va a querer usar.

Con ese criterio, **87 + 21 = 108 filas son prudentes**, y este es el desglose que sostiene el
número: 7 de hematología, 11 perfiles bioquímicos, 17 analitos individuales, 5 de uroanálisis, 7 de
coprología, 9 de endocrinología, 13 de infecciosas, 11 de citología, histopatología y microbiología,
7 de coagulación, gases y transfusión, y las 21 modalidades de imagen. Es el portafolio que
publican los laboratorios veterinarios colombianos citados en §8.2, ni uno más. Y **105 de los 108
nombres llevan dos palabras o más**: los tres de una sola —`Fluoroscopia`, `Mielografía` y
`Colonoscopia`— son términos técnicos inequívocos, sin sinónimo coloquial ni variante propia que una
clínica quisiera nombrar de otro modo. Ninguno es un nombre genérico del tipo `Hemograma`,
`Ecografía` o `Radiografía`, que son exactamente los que se dejan libres.

**Por qué no menos.** Un catálogo de 20 filas obligaría a cada clínica a teclear las otras 88, y
entonces son las clínicas las que colisionan entre sí sin verse, que es el escenario peor: la
colisión con una fila global al menos es determinista, ocurre una sola vez y el nombre en conflicto
sí es visible para quien la sufre.

**Por qué no más.** El techo lo pone la usabilidad del selector, no la base de datos: 108 filas en
un desplegable sin buscador ya son incómodas. Todo lo que quedó fuera está en §4.11 y §5.3 con su
motivo, y el criterio siempre es el mismo — si lo ofrece un centro de referencia y no una clínica
de tamaño medio, lo crea el tenant que lo ofrezca.

**Lo que la semilla NO debe hacer, por el mismo motivo:**

- No sembrar sinónimos como dos filas («Cuadro hemático» **y** «Hemograma»).
- No sembrar la misma prueba con y sin especie («Perfil renal canino» + «Perfil renal felino»)
  cuando la especie ya está en el animal de la orden.
- No sembrar abreviaturas sueltas («UPC», «TP/TTPa», «T4»): son exactamente los nombres cortos que
  una clínica teclearía. Van dentro del nombre largo, entre paréntesis.

---

## 3. Especificación de escritura para `db-migrations`

### 3.1 Ficheros y numeración

El último changeset del árbol es `284_seed_platform_access_switch.xml`. Los dos nuevos son:

| Nº | Fichero | Contenido |
|---|---|---|
| 285 | `285_seed_laboratory_test_types.xml` | 87 `<insert>` en `laboratory_test_types` |
| 286 | `286_seed_diagnostic_imaging_types.xml` | 21 `<insert>` en `diagnostic_imaging_types` |

**Dos ficheros, no uno.** Son dos tablas independientes y dos decisiones de negocio distintas: una
clínica puede querer el catálogo de laboratorio y no el de imagen. Separarlos permite que uno quede
`MARK_RAN` sin el otro y hacer rollback de uno sin el otro.

### 3.2 Las columnas que hay que escribir, y por qué

| Columna | Valor | ¿Se puede omitir? |
|---|---|---|
| `name` | el de las tablas de §4 y §5 | no |
| `description` | la de las tablas de §4 y §5 | **no** — `NOT NULL` sin default. Y una cadena vacía tampoco vale: esa columna es el único sitio donde el sistema explica para qué sirve la prueba |
| `general` | `valueBoolean="true"` | **NO SE PUEDE OMITIR.** El default de la columna es **`FALSE`** (`056` y `058`, `defaultValueBoolean="false"`). Una fila sembrada sin `general` nace `general = FALSE` **y** `company_id = NULL`, que es justo la combinación que `LaboratoryTestType.validate` declara inválida (`domain/LaboratoryTestType.java:52-53`: `"non-general type requires company"`). Esa fila **no rompe la migración**: rompe el primer `GET` que la mapee, con `IllegalArgumentException` desde el constructor del dominio. Es el error más fácil de cometer en este changeset |
| `company_id` | **no se escribe** | sí, se omite: su ausencia deja `NULL`, que es lo que se quiere. Escribir `<column name="company_id" value=""/>` insertaría cadena vacía y reventaría la FK |
| `created_date` | no se escribe | sí — `defaultValueComputed="CURRENT_TIMESTAMP"` |
| `version` | no se escribe | sí — `defaultValueNumeric="0"`, que es el valor inicial correcto para `@Version` |
| `enabled` | `valueBoolean="true"` | podría omitirse (default `TRUE`), pero **se escribe explícito** por coherencia con `284_seed_platform_access_switch.xml:31`, el precedente de semilla más reciente del repo |

### 3.3 Forma del changeset

Usar `<insert>` nativo, **no** `<sql>` ni `sqlFile`. El precedente `022_seed_americas_geography.xml`
usa `sqlFile` porque son miles de filas de DIVIPOLA; 87 no lo justifican, y los change types nativos
son lo que recomienda Liquibase precisamente porque el rollback y la portabilidad los deriva la
herramienta (<https://docs.liquibase.com/concepts/bestpractices.html>).

Esqueleto exacto (el de laboratorio; el de imagen es idéntico cambiando tabla y nombres):

```xml
<changeSet id="285_seed_laboratory_test_types" author="orlando">
    <preConditions onFail="MARK_RAN">
        <sqlCheck expectedResult="0">
            SELECT COUNT(*) FROM laboratory_test_types WHERE general = TRUE
        </sqlCheck>
    </preConditions>

    <insert tableName="laboratory_test_types">
        <column name="name" value="Hemograma completo (cuadro hemático)"/>
        <column name="description" value="..."/>
        <column name="general" valueBoolean="true"/>
        <column name="enabled" valueBoolean="true"/>
    </insert>
    <!-- ... x87 ... -->

    <rollback>
        <delete tableName="laboratory_test_types">
            <where>general = TRUE AND company_id IS NULL AND name IN ('...', '...')</where>
        </delete>
    </rollback>
</changeSet>
```

**Sobre la `preCondition`: `MARK_RAN`, no `HALT`.** Aquí no se está protegiendo una invariante —que
es el caso del changeset `226`, donde `HALT` es lo correcto— sino haciendo idempotente una siembra
que puede haberse hecho a mano en dev o en una empresa piloto. Con `HALT`, una base donde alguien ya
creó un tipo global a mano dejaría el arranque de la aplicación muerto. El
`SELECT COUNT(*) ... WHERE general = TRUE` es la condición correcta, y no un `COUNT(*)` a secas: una
empresa que ya haya creado tipos propios (`general = FALSE`) no debe impedir la siembra global.

**Sobre el `<rollback>`: enumerar los nombres.** Un `<delete>` con `WHERE general = TRUE AND
company_id IS NULL` a secas borra «lo que cumpla la condición», y algún día eso incluirá una fila
global que creó otra persona. El `name IN (...)` con los 87 nombres es verboso y es el correcto, por
la misma razón por la que a las constraints se les pone nombre.

**Un solo `changeSet` por fichero, con los 87 `<insert>` dentro**, no 87 changesets. Es una unidad
transaccional: o está el catálogo entero o no está. Liquibase envuelve cada `changeSet` en su propia
transacción, así que un fallo en el `<insert>` 40 revierte los 39 anteriores.

### 3.4 Coste operativo

`INSERT` de 87 y 21 filas sobre tablas vacías. **No hay `ALTER`, no hay reconstrucción de tabla, no
hay backfill, no hay bloqueo de metadatos apreciable.** La tabla de operaciones DDL online de InnoDB
(<https://dev.mysql.com/doc/refman/8.4/en/innodb-online-ddl-operations.html>) no aplica: aquí no hay
DDL. En una `db.t4g.small` esto es del orden de milisegundos.

### 3.5 Comprobaciones que debe pasar el changeset antes de mergear

1. Los 87 + 21 `name` **caben en `VARCHAR(100)`** y las 108 `description` en `VARCHAR(500)`. Medido
   en caracteres, no en bytes: las columnas heredan `utf8mb4` del servidor, así que «ñ» y las tildes
   cuentan como un carácter para el límite de la columna. **Comprobado al escribir esta
   especificación: el `name` más largo mide 64 caracteres y la `description` más larga 252.** Hay
   margen de sobra; ningún valor está cerca del límite.
2. **Unicidad bajo la collation real, no bajo comparación exacta — ya comprobado, `db-migrations` no
   tiene que repetirlo.** Contrastar los 108 nombres con un `==` de Python o de Java **no sirve**:
   el índice compara con `utf8mb4_0900_ai_ci` (§1.2.1), que ignora acentos y caja. El criterio que
   se aplicó fue normalizar cada nombre y contar claves distintas, con **dos** normalizaciones de
   dureza creciente:

   | Normalización | Qué simula | Resultado |
   |---|---|---|
   | minúsculas + eliminación de diacríticos (NFD, descartando la categoría Unicode `Mn`) | exactamente lo que ve `utf8mb4_0900_ai_ci` | **108 claves para 108 nombres — cero colisiones** |
   | la anterior **más** eliminar puntuación y espacios (solo `isalnum`) | más dura que el índice, para cazar pares como `ALT (TGP)` / `ALT(TGP)`, que el índice sí distinguiría pero un humano no | **108 claves para 108 nombres — cero colisiones** |

   La segunda es deliberadamente más estricta que la base de datos: su objetivo no es predecir el
   `INSERT`, sino que no queden en el catálogo dos filas que el usuario no pueda distinguir en un
   desplegable. Pasar las dos significa que **no hay ningún par de nombres que difiera solo en
   acento, caja, espaciado o puntuación**, ni dentro de cada tabla ni entre las dos —el chequeo se
   corrió sobre las 108 juntas, no tabla por tabla—.

   Dos casos concretos que el chequeo confirma limpios, porque son los que más fácil se cuelan:
   las abreviaturas de bioquímica van **siempre** dentro del nombre canónico y una sola vez
   (`Alanina aminotransferasa (ALT)`, nunca además una fila `ALT` suelta), y ninguna modalidad de
   imagen reaparece con otra grafía en el bloque de laboratorio.
3. Los 108 `name` no colisionan con nada existente, porque las dos tablas están vacías (§1.3). **Si
   entre la redacción de este documento y la ejecución del changeset alguien creara tipos a mano**,
   la `preCondition` de §3.3 lo marca `MARK_RAN` y no rompe nada — pero entonces el catálogo queda a
   medias y hay que resolverlo a mano.
4. Ninguna `description` va vacía (§3.2).
5. Todos los `<insert>` llevan `general` explícito (§3.2).
6. `mvn -q -Dtest=…PersistenceIT test` del slice, si `backend-tests` lo añade, o como mínimo un
   arranque con Liquibase contra Testcontainers (`mysql:8.4`) que valide que ninguna fila sembrada
   revienta el mapeo al dominio.

---

## 4. `laboratory_test_types` — 87 filas

### 4.1 Decisión de granularidad, razonada

**La fila es la unidad que un veterinario ordena y un laboratorio cobra.** Ni más fina ni más
gruesa. En concreto:

- **Los perfiles van como fila** («Perfil renal», «Perfil hepático»), porque así se piden, así se
  cobran y así los publican los laboratorios veterinarios colombianos: Faunalab lista 16 perfiles
  como productos independientes de los analitos sueltos.
- **Los analitos sueltos también van como fila**, pero solo los que de verdad se piden aislados
  (ALT, creatinina, glucosa, T4 total, SDMA…). Faunalab publica precisamente esa lista de química
  individual: albúmina, ALT, AST, bilirrubinas, calcio, colesterol, creatinina, fosfatasa alcalina,
  fósforo, GGT, glucosa, proteínas, triglicéridos, urea/BUN.
- **Las dos cosas conviven, y eso no es redundancia.** Son dos productos distintos con precio
  distinto: pedir un perfil renal no es pedir creatinina.
- **La composición del perfil NO va en este catálogo.** «Qué analitos incluye el perfil renal» es
  otro nivel del modelo, y **hoy ese nivel no existe**: no hay tabla de analitos, ni de valores, ni
  de rangos de referencia (§1.5). Modelarlo aquí, metiendo cada analito del perfil como fila
  suelta, obligaría a crear 12 filas de `laboratory_tests` para una sola química —una orden por
  analito, cada una con su `diagnosis` de 2.000 caracteres—, que es inviable en consulta. Queda
  como mejora futura en §7.
- **Ninguna fila lleva especie en el nombre.** La especie está en el animal de la orden. Se hace
  una única excepción donde la prueba es materialmente distinta por especie y así la venden los
  laboratorios: FeLV/FIV (felinos) y las tipificaciones sanguíneas canina y felina.

### 4.2 Hematología (7)

| `name` | `description` |
|---|---|
| Hemograma completo (cuadro hemático) | Recuento de eritrocitos, leucocitos y plaquetas con hematocrito, hemoglobina, índices eritrocitarios, recuento diferencial leucocitario y evaluación morfológica. Es la prueba base de tamizaje en anemia, infección, inflamación y trastornos plaquetarios. |
| Extendido de sangre periférica (frotis) | Revisión microscópica manual del frotis teñido para evaluar morfología de eritrocitos, leucocitos y plaquetas, confirmar recuentos del analizador y detectar células anómalas, agregados plaquetarios e inclusiones. |
| Recuento de reticulocitos | Cuantifica los eritrocitos inmaduros circulantes para clasificar una anemia como regenerativa o arregenerativa. Es la prueba que decide el enfoque diagnóstico de toda anemia. |
| Microhematocrito con proteínas totales por refractometría | Determinación rápida del hematocrito por centrifugación en capilar junto con proteínas totales por refractómetro. Prueba de consultorio para valorar hidratación, anemia y pérdida proteica en minutos. |
| Frotis para hemoparásitos | Búsqueda microscópica dirigida de hemoparásitos en extendido sanguíneo teñido: Ehrlichia, Anaplasma, Babesia, Hepatozoon y Mycoplasma haemofelis. Su resultado negativo no descarta infección. |
| Hemoparásitos en capa leucocitaria (buffy coat) | Concentración de la capa leucocitaria por centrifugación para aumentar la probabilidad de observar mórulas de Ehrlichia y Anaplasma frente al frotis convencional. |
| Recuento manual de plaquetas en cámara | Recuento plaquetario por microscopía en cámara de Neubauer para confirmar trombocitopenias reportadas por el analizador y descartar seudotrombocitopenia por agregados. |

### 4.3 Bioquímica clínica — perfiles (11)

| `name` | `description` |
|---|---|
| Perfil bioquímico general (química sanguínea completa) | Panel amplio de química sérica con función renal, hepática, glucosa, proteínas, electrolitos y enzimas. Es el tamizaje bioquímico de referencia en el paciente enfermo sin diagnóstico orientado. |
| Perfil renal | Creatinina, nitrógeno ureico (BUN), fósforo, calcio y proteínas totales, con densidad urinaria cuando se acompaña de orina. Permite estadificar la enfermedad renal crónica según el sistema IRIS. |
| Perfil hepático | ALT, AST, fosfatasa alcalina, GGT, bilirrubina total y directa, proteínas totales, albúmina y glucosa. Distingue el daño hepatocelular del patrón colestásico y valora la función de síntesis. |
| Perfil pancreático | Amilasa, lipasa, glucosa, calcio, triglicéridos y proteínas totales, orientado al paciente con sospecha de pancreatitis aguda. Se confirma con lipasa pancreática específica. |
| Perfil lipídico | Colesterol total y triglicéridos en ayuno, para evaluar hiperlipidemias primarias y secundarias a hipotiroidismo, diabetes, hiperadrenocorticismo y síndrome nefrótico. |
| Perfil prequirúrgico | Hemograma, ALT, fosfatasa alcalina, creatinina, nitrógeno ureico, glucosa y proteínas totales. Valora el riesgo anestésico y detecta la enfermedad subclínica que obliga a aplazar o modificar el protocolo. |
| Perfil geriátrico | Hemograma, química sanguínea amplia, uroanálisis y T4 total, para el tamizaje anual del paciente mayor donde la enfermedad renal, hepática y endocrina cursa sin signos. |
| Perfil pediátrico | Hemograma y química sanguínea básica adaptados a cachorros y gatitos, con valores de referencia propios de la edad, orientados a anemia, hipoglucemia, parasitismo y déficit de crecimiento. |
| Perfil de paciente convulsivo | Glucosa, calcio, electrolitos, función hepática con ácidos biliares o amoníaco, y función renal, para descartar las causas metabólicas y extracraneales de convulsión antes de asumir causa intracraneal. |
| Perfil dermatológico | Panel de apoyo al paciente con enfermedad cutánea crónica: hemograma, química básica, T4 total y pruebas de tamizaje endocrino, junto con raspado y citología cuando se solicitan aparte. |
| Perfil cardíaco (troponina I y NT-proBNP) | Biomarcadores cardíacos en suero: troponina I como marcador de daño miocárdico y NT-proBNP como marcador de estiramiento de pared. Apoyan la decisión de estudiar con ecocardiografía. |

### 4.4 Bioquímica clínica — analitos individuales (17)

| `name` | `description` |
|---|---|
| Alanina aminotransferasa (ALT) | Enzima de localización hepatocelular. Su elevación indica lesión de la célula hepática y es el marcador más específico de daño hepatocelular en perro y gato. |
| Aspartato aminotransferasa (AST) | Enzima presente en hígado y músculo. Se interpreta junto con ALT y creatina quinasa para separar el origen hepático del muscular. |
| Fosfatasa alcalina (FA) | Enzima que se eleva en colestasis, inducción por corticoides o fenobarbital, y en crecimiento óseo activo. En el gato cualquier elevación es significativa por su vida media corta. |
| Gamma glutamil transferasa (GGT) | Marcador de colestasis y de inducción enzimática. Junto con la fosfatasa alcalina ayuda a confirmar el patrón colestásico y a interpretar elevaciones de origen óseo. |
| Bilirrubina total y directa | Cuantifica y fracciona la bilirrubina sérica para clasificar la ictericia en prehepática, hepática o poshepática. |
| Proteínas totales y fraccionadas (albúmina y globulinas) | Proteínas séricas totales con separación de albúmina y globulinas y relación albúmina/globulina. Orienta a pérdida proteica, inflamación crónica, gammapatías y fallo de síntesis hepática. |
| Albúmina sérica | Principal proteína de síntesis hepática y determinante de la presión oncótica. Su descenso orienta a enteropatía o nefropatía perdedora de proteínas, hepatopatía o inflamación crónica. |
| Nitrógeno ureico en sangre (BUN) | Producto nitrogenado de eliminación renal. Se interpreta junto con creatinina y densidad urinaria para separar la azotemia prerrenal, renal y posrenal. |
| Creatinina sérica | Marcador de filtración glomerular y eje de la estadificación IRIS de enfermedad renal crónica. Se eleva cuando ya se ha perdido buena parte de la función renal, por lo que se combina con SDMA. |
| Dimetilarginina simétrica (SDMA) | Marcador de filtración glomerular más precoz que la creatinina y menos dependiente de la masa muscular. Permite detectar enfermedad renal en estadios tempranos y afinar la estadificación IRIS. |
| Glucosa sérica | Concentración de glucosa en sangre. Base del diagnóstico y seguimiento de la diabetes mellitus y de la evaluación urgente de hipoglucemia en cachorros, sepsis e insulinoma. |
| Calcio total y fósforo | Cuantifica ambos minerales, cuyo desequilibrio acompaña a la enfermedad renal crónica, el hiperparatiroidismo, las neoplasias con hipercalcemia y la eclampsia puerperal. |
| Calcio ionizado | Fracción biológicamente activa del calcio. Es la medida válida cuando hay hipoalbuminemia y la que se exige para confirmar hipercalcemia o hipocalcemia verdaderas. |
| Amilasa y lipasa séricas | Enzimas pancreáticas de tamizaje en el abdomen agudo. Su baja especificidad obliga a confirmar la pancreatitis con lipasa pancreática específica y ecografía. |
| Lipasa pancreática específica (cPL/fPL) | Inmunoensayo específico de lipasa de origen pancreático en perro y gato. Es la prueba sérica de elección para el diagnóstico de pancreatitis. |
| Ácidos biliares séricos pre y posprandiales | Par de muestras en ayuno y dos horas tras el alimento. Evalúa la función hepática y es la prueba de tamizaje de derivación portosistémica. |
| Creatina quinasa (CK) | Enzima de músculo esquelético y cardíaco. Confirma el origen muscular de una elevación de AST y apoya el diagnóstico de miopatía, trauma y decúbito prolongado. |

### 4.5 Uroanálisis (5)

| `name` | `description` |
|---|---|
| Uroanálisis completo (parcial de orina con sedimento) | Examen físico, químico y microscópico del sedimento urinario: color, densidad, pH, proteína, glucosa, cetonas, sangre, cilindros, cristales, células y bacterias. Se debe indicar el método de recolección. |
| Densidad urinaria por refractometría | Mide la capacidad de concentración renal. Es el dato que convierte una azotemia en interpretable y no puede sustituirse por la tira reactiva. |
| Relación proteína/creatinina en orina (UPC) | Cuantifica la proteinuria de forma independiente de la concentración de la orina. Es el criterio con que el sistema IRIS subestadifica la enfermedad renal crónica y decide tratar la proteinuria. |
| Urocultivo con recuento de colonias y antibiograma | Cultivo cuantitativo de orina obtenida por cistocentesis, con identificación del germen y sensibilidad antibiótica. Es obligatorio antes de tratar infección urinaria recurrente o complicada. |
| Análisis de composición de urolitos | Determinación de la composición del cálculo extraído (estruvita, oxalato de calcio, urato, cistina). Define la dieta y la prevención de recidiva, que difieren por completo según el tipo. |

### 4.6 Coprología y parasitología (7)

| `name` | `description` |
|---|---|
| Coprológico directo y por flotación | Examen microscópico de materia fecal, en fresco y tras flotación en solución de alta densidad, para detectar huevos de helmintos, ooquistes de coccidias y protozoos. |
| Coprológico seriado de tres muestras | Tres exámenes coprológicos de días alternos. La eliminación de huevos es intermitente, de modo que una sola muestra negativa no descarta parasitismo. |
| Flotación fecal por centrifugación | Flotación asistida por centrifugación, que recupera significativamente más huevos y ooquistes que la flotación pasiva. Es el método recomendado como estándar de tamizaje. |
| Técnica de Baermann para larvas | Migración activa de larvas desde la materia fecal en columna de agua. Es la prueba de elección para larvas de nematodos respiratorios como Angiostrongylus y Aelurostrongylus. |
| Antígeno de Giardia en materia fecal | Inmunoensayo de antígeno específico de Giardia, complementario del examen microscópico, que detecta infecciones con eliminación intermitente de quistes. |
| Tinción de Ziehl-Neelsen modificada en materia fecal | Coloración ácido-alcohol resistente para identificar ooquistes de Cryptosporidium, que pasan desapercibidos en el coprológico convencional por su tamaño. |
| Sangre oculta en materia fecal | Detección de sangre no visible en heces, como apoyo al estudio de anemia y de enfermedad gastrointestinal ulcerativa. Requiere control dietario previo para evitar falsos positivos. |

### 4.7 Endocrinología (9)

| `name` | `description` |
|---|---|
| Tiroxina total (T4 total) | Concentración sérica de T4 total. Es la prueba de tamizaje tiroideo: valor alto confirma hipertiroidismo felino y valor bajo obliga a completar con T4 libre y TSH en el perro. |
| Tiroxina libre por diálisis de equilibrio (T4 libre) | Mide la fracción libre de T4 sin interferencia de proteínas transportadoras ni de enfermedad no tiroidea. Es la prueba que confirma el hipotiroidismo canino cuando la T4 total no es concluyente. |
| Hormona estimulante de tiroides canina (cTSH) | Se interpreta junto con la T4 libre. Una TSH alta con T4 libre baja es el patrón que confirma el hipotiroidismo primario canino. |
| Cortisol sérico basal | Concentración puntual de cortisol. Por sí sola no diagnostica hiperadrenocorticismo, pero un valor basal alto es útil para descartar hipoadrenocorticismo. |
| Prueba de supresión con dexametasona a dosis baja | Cortisol basal y a las 4 y 8 horas de administrar dexametasona. Es la prueba de tamizaje preferida para el hiperadrenocorticismo canino y en parte de los casos diferencia el origen hipofisario del adrenal. |
| Prueba de supresión con dexametasona a dosis alta | Prueba de diferenciación que se realiza tras confirmar el hiperadrenocorticismo, para separar el origen hipofisario del tumor adrenal según el grado de supresión del cortisol. |
| Prueba de estimulación con ACTH | Cortisol antes y después de administrar ACTH sintética. Es la prueba de elección para diagnosticar hipoadrenocorticismo y para monitorizar el tratamiento del hiperadrenocorticismo. |
| Relación cortisol/creatinina en orina | Prueba de tamizaje de hiperadrenocorticismo en muestra de orina tomada en casa. Su alta sensibilidad y baja especificidad la hacen útil para descartar, no para confirmar. |
| Fructosamina sérica | Refleja la glucemia media de las dos a tres semanas previas. Diferencia la hiperglucemia por estrés de la diabetes real, sobre todo en el gato, y monitoriza el control glucémico. |

### 4.8 Serología y enfermedades infecciosas (13)

| `name` | `description` |
|---|---|
| Prueba rápida de Ehrlichia, Anaplasma y antígeno de Dirofilaria | Inmunoensayo de consultorio que detecta en una sola muestra anticuerpos frente a Ehrlichia y Anaplasma y antígeno de Dirofilaria immitis. Es el tamizaje habitual del paciente con exposición a garrapatas. |
| Ehrlichia canis y Anaplasma spp. por PCR | Detección molecular del ADN del agente en sangre. Confirma infección activa cuando la serología es dudosa y permite diferenciar especies que el frotis no distingue. |
| Antígeno de Dirofilaria immitis | Detecta antígeno de hembra adulta de dirofilaria en suero o sangre. Se recomienda usarlo siempre junto con una prueba de microfilarias, porque los resultados discordantes son frecuentes. |
| Test de Knott modificado para microfilarias | Concentración y observación microscópica de microfilarias, con medición para diferenciar Dirofilaria immitis de otras filarias. Es la prueba de microfilarias de referencia. |
| Anticuerpos anti-Leishmania por inmunofluorescencia indirecta | Serología cuantitativa frente a Leishmania infantum. Es la técnica con la que el Instituto Nacional de Salud confirma los casos caninos dentro de la vigilancia epidemiológica en Colombia. |
| Prueba rápida rK39 para leishmaniasis canina | Inmunocromatografía frente al antígeno recombinante rK39, usada como tamizaje de leishmaniasis visceral canina en campo y aceptada en estudios de prevalencia en Colombia. |
| Leishmania spp. por PCR | Detección de ADN de Leishmania en médula ósea, ganglio, piel o sangre. Confirma infección activa y es la prueba de elección cuando la serología es dudosa. |
| Antígeno de parvovirus canino en materia fecal | Prueba rápida de antígeno en heces o vómito, con resultado en pocos minutos. Es la primera prueba en todo cachorro con vómito y diarrea, y condiciona el aislamiento inmediato del paciente. |
| Antígeno del virus del moquillo canino | Prueba rápida de antígeno en secreción conjuntival o nasal, orientada al paciente con signos respiratorios, digestivos o neurológicos compatibles con distemper. |
| Parvovirus y virus del moquillo canino por PCR | Detección molecular de ambos virus, con mayor sensibilidad que la prueba rápida. Confirma casos con carga viral baja y resuelve los resultados dudosos por interferencia vacunal reciente. |
| Prueba rápida combinada de antígeno de FeLV y anticuerpos de FIV | Inmunoensayo de consultorio para leucemia e inmunodeficiencia felinas. Todo gato debe conocer su estado retroviral; un resultado positivo requiere confirmación en un segundo nivel de prueba. |
| Anticuerpos de Brucella canis por aglutinación rápida en placa | Prueba serológica de tamizaje de brucelosis canina, con o sin 2-mercaptoetanol. Es una zoonosis reproductiva y todo positivo debe confirmarse con hemocultivo o PCR. |
| Anticuerpos anti-Leptospira por microaglutinación (MAT) | Serología de referencia frente a los serovares de Leptospira, con titulación en muestras pareadas. Zoonosis relevante en Colombia por el clima y la exposición a aguas contaminadas. |

### 4.9 Citología, histopatología y microbiología (11)

| `name` | `description` |
|---|---|
| Citología por aspiración con aguja fina | Estudio microscópico de células obtenidas por punción de masa, ganglio, órgano o lesión cutánea. Es el primer paso ante cualquier masa: separa lo inflamatorio de lo neoplásico sin cirugía. |
| Citología de hisopado ótico | Evaluación microscópica del contenido del conducto auditivo para identificar levaduras, cocos, bacilos y ácaros. Define el tratamiento tópico de la otitis y su respuesta. |
| Citología vaginal | Evaluación de la descamación del epitelio vaginal para determinar la fase del ciclo estral, apoyar el momento óptimo de la monta y estudiar secreciones anómalas. |
| Citología de líquidos corporales con análisis fisicoquímico | Recuento celular, proteínas y estudio citológico de líquido abdominal, torácico, pericárdico o sinovial, para clasificarlo como trasudado, trasudado modificado o exudado. |
| Citología de líquido cefalorraquídeo | Recuento celular, proteínas y citología del líquido cefalorraquídeo, para diferenciar la enfermedad inflamatoria e infecciosa del sistema nervioso central de la degenerativa y la neoplásica. |
| Raspado cutáneo y tricograma | Raspado superficial y profundo de piel y examen del pelo arrancado, para detectar Demodex, Sarcoptes y dermatofitos, y evaluar el ciclo del folículo piloso. |
| Histopatología de biopsia | Estudio anatomopatológico de tejido fijado en formol, con diagnóstico definitivo, grado tumoral y evaluación de márgenes quirúrgicos cuando se remite la pieza completa. |
| Necropsia con estudio histopatológico | Examen post mortem sistemático con toma de muestras para histopatología. Determina la causa de muerte y detecta enfermedad transmisible que afecte a los demás animales del hogar o del criadero. |
| Cultivo bacteriano aerobio con antibiograma | Aislamiento e identificación del germen y determinación de su sensibilidad antibiótica. Es el requisito de un uso responsable de antimicrobianos en infecciones recurrentes o graves. |
| Cultivo bacteriano anaerobio | Cultivo en condiciones de anaerobiosis para abscesos profundos, piotórax, peritonitis y heridas por mordedura, donde el cultivo aerobio convencional resulta falsamente negativo. |
| Cultivo micológico e identificación de dermatofitos | Siembra de pelo y escamas en medio selectivo con identificación de la especie de dermatofito. Es la prueba de confirmación de la tiña, una dermatopatía zoonótica. |

### 4.10 Coagulación, gases y medicina transfusional (7)

| `name` | `description` |
|---|---|
| Tiempos de coagulación (TP y TTPa) | Tiempo de protrombina y tiempo de tromboplastina parcial activada. Evalúan las vías extrínseca e intrínseca y son la prueba clave ante intoxicación por rodenticidas, hepatopatía y sangrado quirúrgico. |
| Fibrinógeno plasmático | Cuantifica el fibrinógeno como factor de coagulación y reactante de fase aguda. Su descenso acompaña a la coagulación intravascular diseminada y al fallo hepático. |
| Dímero D | Producto de degradación de la fibrina. Apoya el diagnóstico de coagulación intravascular diseminada y de tromboembolismo, siempre interpretado junto con el cuadro clínico. |
| Tiempo de sangría de la mucosa bucal | Prueba de consultorio que evalúa la hemostasia primaria y la función plaquetaria. Se realiza antes de una cirugía en pacientes con historia de sangrado y recuento plaquetario normal. |
| Gases sanguíneos y equilibrio ácido-base | Determinación de pH, presiones de oxígeno y dióxido de carbono, bicarbonato y exceso de base en sangre arterial o venosa. Guía la reanimación del paciente crítico y la ventilación. |
| Electrolitos séricos (sodio, potasio y cloro) | Cuantifica los electrolitos mayores. La hiperpotasemia del paciente obstruido y del hipoadrenocorticismo es una urgencia con riesgo vital inmediato. |
| Tipificación sanguínea y prueba cruzada de compatibilidad | Determinación del grupo sanguíneo (DEA 1 en el perro; A, B y AB en el gato) y prueba cruzada mayor y menor con el donante. Obligatoria antes de transfundir a un gato y en la segunda transfusión de un perro. |

### 4.11 Lo que deliberadamente NO se siembra

Cada una de estas se deja fuera con motivo, y el motivo importa tanto como la lista:

| No se siembra | Por qué |
|---|---|
| Analitos de perfil que nunca se piden sueltos (sodio aislado, VCM, HCM…) | Son componentes de un resultado, no unidades ordenables. Sembrarlos convierte el catálogo en un glosario y multiplica las filas de `laboratory_tests` (§1.5) |
| Inmunohistoquímica, citometría de flujo, clonalidad PARR | Solo se hacen en laboratorio de referencia y llegan por remisión externa. Una clínica que las use las crea como tipo propio |
| Titulación de anticuerpos antirrábicos (FAVN/RFFIT) | Existe y es real —se exige para exportar mascotas—, pero en Colombia se tramita en laboratorio autorizado en el exterior. Va como fila propia de la clínica que lo ofrezca, no como global |
| Perfiles por especie exótica (aves, reptiles, conejos) | Faunalab publica perfiles de conejo, pero el sistema no modela especie en el catálogo. Que los cree la clínica exótica que los ofrezca |
| Nombres cortos («Hemograma», «Orina», «T4») | §2: cada uno quemaría un nombre que las clínicas van a querer |

---

## 5. `diagnostic_imaging_types` — 21 filas

### 5.1 Decisión de granularidad, razonada

**La fila es la modalidad, más la variante técnica cuando cambia el equipo, el operador, la
preparación del paciente o el consentimiento. La fila NO es la región anatómica.**

El motivo es del modelo, no de gusto: `diagnostic_imagings` ya tiene
`study_type VARCHAR(200) NOT NULL` (`054_create_diagnostic_imagings.xml`), texto libre, que es
exactamente donde va «tórax en proyección latero-lateral derecha y ventrodorsal». Si el catálogo
tuviera «Radiografía de tórax», «Radiografía de abdomen», «Radiografía de columna»…, el mismo eje
quedaría representado dos veces y de dos formas incompatibles: una cerrada en el catálogo y otra
abierta en la orden. Ese es el momento en que un catálogo empieza a crecer sin fin y nadie sabe cuál
de los dos campos es el bueno.

**Las proyecciones son otro nivel del modelo y hoy no existen.** Una radiografía de tórax son al
menos dos proyecciones, cada una con su archivo; el sistema guarda archivos en
`laboratory_test_files` para laboratorio, y para imagen no hay tabla de proyecciones. Modelarlas
exigiría una tabla `diagnostic_imaging_projections` hija de `diagnostic_imagings`. Queda en §7 como
mejora futura, **no se resuelve metiendo proyecciones en el catálogo de tipos**.

**Sí se separan por fila** los casos en que la variante cambia algo material:

- **Con y sin medio de contraste**: distinto consentimiento, distinto riesgo, distinto tiempo, y en
  el caso de la tomografía distinto costo.
- **Los protocolos oficiales de displasia de cadera**: no son la misma radiografía. OFA, FCI y
  BVA/KC evalúan **una** proyección ventrodorsal con extremidades extendidas, mientras que PennHIP
  exige **tres** radiografías —distracción, compresión y ventrodorsal extendida—, requiere
  certificación del veterinario y anestesia general, y difiere en la edad mínima de evaluación
  (24 meses en OFA, 12 a 18 meses en FCI según raza, 12 meses en BVA/KC, y desde 16 semanas en
  PennHIP). Son productos distintos y se cobran distinto.
- **Las endoscopias, por vía de acceso**: gastroscopia, colonoscopia, rinoscopia, broncoscopia y
  cistoscopia usan equipos, preparación y tiempos distintos.

### 5.2 Las 21 filas

| `name` | `description` |
|---|---|
| Radiografía digital simple | Estudio radiográfico sin medio de contraste, en las proyecciones que indique el caso. Es la imagen de primera línea en trauma, tórax, abdomen agudo y patología osteoarticular. |
| Radiografía con medio de contraste | Estudio radiográfico con contraste positivo o negativo para valorar tránsito digestivo, vías urinarias u otras estructuras huecas que no se distinguen en la radiografía simple. |
| Mielografía | Radiografía de la columna tras inyectar medio de contraste en el espacio subaracnoideo, para localizar una compresión medular cuando no se dispone de resonancia magnética. |
| Radiografía dental intraoral | Radiografía con sensor o placa dentro de la cavidad oral, indispensable para valorar raíces, hueso alveolar y reabsorción dentaria, que la radiografía extraoral no muestra. |
| Radiografía de displasia de cadera, protocolo OFA | Proyección ventrodorsal con extremidades extendidas según el protocolo de la Orthopedic Foundation for Animals, con evaluación por panel y edad mínima de 24 meses. |
| Radiografía de displasia de cadera, protocolo PennHIP | Serie de tres radiografías —distracción, compresión y ventrodorsal extendida— que cuantifica la laxitud articular mediante el índice de distracción. Exige veterinario certificado y anestesia general. |
| Radiografía de displasia de cadera, protocolo FCI | Proyección ventrodorsal con extremidades extendidas según el protocolo de la Fédération Cynologique Internationale, con calificación de A a E y edad mínima de 12 a 18 meses según la raza. |
| Radiografía de displasia de codo | Serie radiográfica del codo, con proyección medio-lateral flexionada, para detectar los procesos que integran la displasia de codo y la artrosis secundaria. |
| Ecografía abdominal completa | Exploración ultrasonográfica sistemática de hígado, vías biliares, bazo, riñones, vejiga, tracto digestivo, ganglios y aparato reproductor. Complementa a la radiografía y no la sustituye. |
| Ecocardiografía con Doppler | Estudio ultrasonográfico del corazón con medición de cámaras, función sistólica y evaluación de flujos por Doppler. Es el estándar de referencia para estadificar la enfermedad valvular mitral y la cardiomiopatía felina. |
| Ecografía de gestación y seguimiento fetal | Confirma la gestación desde etapas tempranas, evalúa la viabilidad y la frecuencia cardíaca fetal y detecta sufrimiento fetal. No es el método fiable para contar fetos. |
| Ecografía ocular | Exploración ultrasonográfica del globo ocular y la órbita, indicada cuando la opacidad de medios impide la exploración oftalmoscópica directa, ante desprendimiento de retina o masa retrobulbar. |
| Ecografía musculoesquelética y de tejidos blandos | Evaluación de tendones, músculos, articulaciones y masas superficiales, y valoración dinámica en tiempo real que la radiografía no permite. |
| Ecografía FAST abdominal y torácica | Protocolo ecográfico abreviado en urgencias para detectar líquido libre abdominal, pleural o pericárdico y neumotórax en el paciente politraumatizado o inestable. |
| Punción o biopsia guiada por ecografía | Toma de muestra citológica o histológica de un órgano o masa con guía ecográfica en tiempo real, lo que reduce el riesgo de lesionar estructuras vecinas. |
| Tomografía computarizada simple | Estudio tomográfico sin contraste, con reconstrucción multiplanar. Es la modalidad de elección en trauma craneal, patología nasal, oído medio, tórax y planificación quirúrgica ortopédica. |
| Tomografía computarizada con medio de contraste | Estudio tomográfico con contraste intravenoso para caracterizar masas, valorar vascularización, estadificar tumores y estudiar anomalías vasculares como la derivación portosistémica. |
| Resonancia magnética | Estudio de alta resolución del tejido blando, de elección en enfermedad del sistema nervioso central, médula espinal, discopatías y patología de tejidos blandos que la tomografía no resuelve. |
| Fluoroscopia | Radiografía dinámica en tiempo real, indicada para colapso traqueal, trastornos de la deglución, estudios de tránsito y procedimientos intervencionistas guiados. |
| Endoscopia digestiva alta | Exploración del esófago, estómago y duodeno con endoscopio flexible, con toma de biopsias y extracción de cuerpos extraños sin cirugía. |
| Colonoscopia | Exploración del colon y el íleon distal con endoscopio flexible y toma de biopsias, indicada en diarrea crónica de intestino grueso y hematoquecia. |

### 5.3 Lo que deliberadamente NO se siembra

| No se siembra | Por qué |
|---|---|
| «Radiografía de tórax», «Ecografía renal» y demás combinaciones modalidad + región | Duplicarían `diagnostic_imagings.study_type`, que ya es texto libre para eso (§5.1) |
| Proyecciones radiográficas (latero-lateral, ventrodorsal, oblicua…) | Son otro nivel del modelo, hijo de la orden, no del catálogo. Ver §7 |
| Rinoscopia, broncoscopia, cistoscopia, videootoscopia | Son reales y se practican, pero son de centro de referencia y de uso mucho menor. Se dejan al tenant que las ofrezca, para no gastar cuatro nombres globales (§2) |
| Artroscopia y laparoscopia | Son procedimientos quirúrgicos mínimamente invasivos; su catálogo natural es `surgery_types`, no este |
| Gammagrafía y medicina nuclear | Requieren licencia de material radiactivo y no hay oferta veterinaria establecida en Colombia |
| Densitometría ósea y termografía | La primera no tiene uso clínico establecido en pequeños animales; la segunda no tiene respaldo diagnóstico suficiente |

---

## 6. Índices: qué hace falta y qué no

**No hace falta ningún índice nuevo.** Y esto es un dictamen, no una omisión:

- La consulta real de cada tenant es `WHERE enabled = 1 AND (general = 1 OR company_id = ?)`, de
  `findAllByGeneralTrueOrCompany_Id`. **Un `OR` entre dos columnas distintas no puede usar un índice
  compuesto**: el manual lo dice explícitamente con el ejemplo
  `WHERE last_name='Jones' OR first_name='John'`, que declara como caso en el que MySQL **no** usa
  el índice (<https://dev.mysql.com/doc/refman/8.4/en/multiple-column-indexes.html>). Lo mejor que
  cabría esperar es un *index merge*, y para 108 filas el optimizador escogerá recorrer la tabla, que
  es más barato.
- **El volumen es el argumento decisivo.** 87 + 21 filas globales, más lo que cada clínica añada.
  Con 200 clínicas y 20 tipos propios cada una, `laboratory_test_types` tendría unas 4.100 filas:
  sigue cabiendo entera en el *buffer pool* con holgura. Proponer un índice aquí sería añadir coste
  de escritura y espacio para resolver un problema que no existe.
- **El índice de `company_id` ya existe** y no hay que declararlo: InnoDB lo crea automáticamente al
  declarar la FK inline de `056`/`058`.
- Si algún día el catálogo creciera de orden de magnitud, el índice a evaluar sería
  `(company_id, enabled, name)` **y** reescribir el `OR` como `UNION ALL` de las dos ramas. **Esto
  es una proyección, no una medición**: no se ha ejecutado ningún `EXPLAIN`, porque las tablas están
  vacías y un plan sobre una tabla vacía no dice nada.

---

## 7. Lo que queda abierto — con su issue

| Issue | Qué | Relación con esta especificación |
|---|---|---|
| [#561](https://github.com/kefaroTech/vetsoftware-backend/issues/561) | Los dos catálogos se despliegan vacíos: ninguna clínica puede registrar una prueba el primer día | **Es el issue de este trabajo.** Se cierra cuando `db-migrations` escriba `285` y `286` con lo que dice este documento |
| [#556](https://github.com/kefaroTech/vetsoftware-backend/issues/556) | `name` es UNIQUE global en cuatro catálogos que sí tienen `company_id`: la primera clínica que cree «Hemograma» se lo quita a las demás | §2. **Conviene arreglarlo ANTES de sembrar**: el `ALTER` reconstruye la tabla y es más barato con la tabla vacía; además evita tener que censar colisiones |
| [#562](https://github.com/kefaroTech/vetsoftware-backend/issues/562) | El resultado de una prueba es un párrafo de texto libre: no hay analitos, valores, unidades ni rangos de referencia | §1.5 y §4.1. **Es lo que fuerza la granularidad** de «unidad ordenable»: con un solo `test_type_id` por orden, sembrar analitos sueltos haría inviable pedir un perfil |
| [#563](https://github.com/kefaroTech/vetsoftware-backend/issues/563) | Un estudio de imagen no registra sus proyecciones y su región anatómica es texto libre sin catálogo | §5.1. **Es lo que fuerza que la fila sea la modalidad** y no el estudio: `study_type` ya ocupa el eje de la región |
| [#482](https://github.com/kefaroTech/vetsoftware-backend/issues/482) (comentado) | El borrado lógico quema el nombre y el `id` no es recuperable por la API | Dejaba estas dos tablas explícitamente como «no comprobado». **Se comentó el resultado del barrido**: las seis tablas de catálogo lo tienen, y estas dos tampoco tienen rama de reactivación en su `Create…Service` |
| [#483](https://github.com/kefaroTech/vetsoftware-backend/issues/483) (comentado) | `description` es `NOT NULL` en la columna pero opcional en el request y en el dominio | **Mismo defecto, verificado en estos dos slices**: los cuatro DTO de entrada declaran solo `@Size(max = 500)`, sin `@NotBlank`, y `validate` solo comprueba la longitud si la descripción viene |

**Arreglo propuesto para #556, para cuando se decida abordarlo.** No sirve un simple
`UNIQUE (company_id, name)`: en MySQL dos filas con `company_id NULL` **no colisionan** en un índice
único, así que las filas globales dejarían de estar protegidas entre sí. El patrón correcto es el
que ya usa la casa en el changeset `226` —columna generada `STORED` más único sobre ella—:

```sql
ALTER TABLE laboratory_test_types
  ADD COLUMN tenant_scope BIGINT
      GENERATED ALWAYS AS (COALESCE(company_id, 0)) STORED,
  ADD UNIQUE KEY uq_laboratory_test_types_tenant_name (tenant_scope, name),
  DROP INDEX name;
```

Con el orden correcto —**el tenant primero**— y con el nombre explícito, que el actual no tiene.
Antes de ejecutarlo hay que verificar en la tabla de DDL online de InnoDB qué algoritmo admite cada
paso: añadir una columna generada `STORED` **reconstruye la tabla**
(<https://dev.mysql.com/doc/refman/8.4/en/innodb-online-ddl-operations.html>), y por eso el
changeset `283` la declaró dentro de su propio `createTable`. Con 108 filas eso es irrelevante hoy;
con datos de producción dentro, no.

---

## 8. Fuentes

Consultadas y verificadas el **2026-08-25**.

### 8.1 Motor y migraciones

| URL | Qué sostiene |
|---|---|
| <https://dev.mysql.com/doc/refman/8.4/en/multiple-column-indexes.html> | La regla del prefijo por la izquierda y, textualmente, que un `WHERE a=… OR b=…` **no** usa el índice. Sostiene §6 |
| <https://dev.mysql.com/doc/refman/8.4/en/innodb-online-ddl-operations.html> | Qué `ALTER` es instantáneo, cuál se hace en sitio y cuál reconstruye la tabla. Sostiene §3.4 y §7 |
| <https://dev.mysql.com/doc/refman/8.4/en/create-table-generated-columns.html> | Columnas generadas `STORED`, base del patrón de unicidad condicional del repo (changesets `206`, `210`, `226`). Sostiene §7 |
| <https://docs.liquibase.com/concepts/bestpractices.html> | Preferir change types nativos frente a SQL bruto, y un changeset por unidad lógica. Sostiene §3.3 |
| <https://docs.citusdata.com/en/stable/use_cases/multi_tenant.html> | El tenant va primero en clave e índice en un esquema multi-tenant con discriminador. Sostiene §2 |
| <https://martinfowler.com/bliki/ParallelChange.html> | Expand/contract, patrón con el que se ejecutaría el cambio de índice de §7 sin downtime |

### 8.2 Laboratorio clínico veterinario

| URL | Qué bloque del catálogo sostiene |
|---|---|
| <https://faunalab.com.co/servicios/> | §4.2 a §4.6. Laboratorio clínico veterinario colombiano: publica hemograma con extendido, reticulocitos, hemoparásitos en frotis y en *buffy coat*, la lista de química individual (albúmina, ALT, AST, bilirrubinas, calcio, colesterol, creatinina, fosfatasa alcalina, fósforo, GGT, glucosa, proteínas, triglicéridos, urea/BUN), 16 perfiles, citoquímico de orina, UPC, urocultivo, coprológico directo y por flotación, raspado de piel y citologías tumoral, ótica y vaginal |
| <https://retterlab.com/> | Confirma el portafolio de un laboratorio veterinario de referencia en Bogotá (hematología, química, uroanálisis) y la existencia de banco de sangre veterinario, que sostiene la fila de tipificación y prueba cruzada de §4.10. **El menú detallado está en un documento externo que no se pudo abrir**; la lista de esta especificación no depende de él |
| <https://veterinlab.apoyarte.com/servicios/> | Portafolio colombiano con perfiles renales, hepáticos, geriátricos, de convulsión y dermatológicos, que sostiene los nombres de perfil de §4.3 |
| <https://icmt.org.co/laboratorio-veterinario/> | Laboratorio de diagnóstico veterinario de la Universidad CES: hematología, química, microbiología, parasitología, inmunología, análisis de líquidos y orina, coagulación, hormonas, serología (ELISA), biología molecular, pruebas rápidas y hemoparásitos. Sostiene la lista de **áreas** de §4 |
| <https://www.iris-kidney.com/iris-staging-system> | Estadificación IRIS por creatinina y SDMA, con subestadificación por UPC y presión arterial. Sostiene creatinina, SDMA, UPC y densidad urinaria (§4.4 y §4.5) |
| <https://www.idexx.com/en/veterinary/reference-laboratories/sdma/> | SDMA como prueba ofertada por laboratorio de referencia, sola o dentro del panel. Sostiene §4.4 |
| <https://pubmed.ncbi.nlm.nih.gov/24112317/> · <https://onlinelibrary.wiley.com/doi/abs/10.1111/jvim.12192> | Consenso ACVIM 2012 de hiperadrenocorticismo canino: las pruebas de tamizaje son la supresión con dexametasona a dosis baja, la estimulación con ACTH y la relación cortisol/creatinina en orina. Sostiene §4.7 |
| <https://todaysveterinarypractice.com/parasitology/ahs-heartworm-diagnostics-canine-guidelines/> | Guías caninas de la American Heartworm Society (actualización 2024): uso **conjunto** de antígeno y prueba de microfilarias, con el Knott modificado como método de elección. Sostiene §4.8 |
| <https://capcvet.org/guidelines/general-guidelines/> · <https://capcvet.org/guidelines/giardia/> | CAPC: la flotación fecal debe hacerse **con centrifugación** (la pasiva no se recomienda), el Baermann es la prueba para larvas y Giardia se estudia combinando microscopía con ELISA o PCR. Sostiene §4.6 |
| <https://catvets.com/resource/feline-retrovirus-management-guidelines/> | Guías AAFP 2020 de retrovirus felinos: prueba combinada de consultorio de antígeno FeLV y anticuerpo FIV, con confirmación en segundo nivel. Sostiene la fila de FeLV/FIV de §4.8 |
| <https://www.idexx.com/en/veterinary/snap-tests/snap-4dx-plus-test/> | Prueba de consultorio que detecta anticuerpos de *Anaplasma* y *Ehrlichia* y antígeno de *Dirofilaria* en una sola muestra. Sostiene la primera fila de §4.8 |
| <https://pmc.ncbi.nlm.nih.gov/articles/PMC6712844/> | Evaluación de pruebas rápidas de leishmaniasis visceral canina y humana **en Colombia**; el rK39 se acepta en estudios de prevalencia y el Instituto Nacional de Salud confirma por inmunofluorescencia indirecta. Sostiene las tres filas de leishmaniasis de §4.8 |
| <https://www.msdvetmanual.com/es/sistema-reproductivo/brucelosis-en-perros/brucelosis-en-perros> | Diagnóstico de *Brucella canis*: aglutinación rápida en placa con y sin 2-mercaptoetanol como tamizaje, y confirmación por hemocultivo o PCR. Sostiene §4.8 |
| <https://www.vanguardiaveterinaria.com.mx/en/parvovirus-y-distemper-canino-una-actualizacion-de-los-metodos-diagnosticos-para-la-practica-ve> | Métodos diagnósticos de parvovirus y moquillo: inmunocromatografía de antígeno en heces y en secreción conjuntival, y RT-PCR de confirmación. Sostiene §4.8 |

### 8.3 Imagen diagnóstica veterinaria

| URL | Qué sostiene |
|---|---|
| <https://www.vin.com/apputil/content/defaultadv1.aspx?pId=20539&id=8506283> | Métodos radiográficos de displasia de cadera (WSAVA 2017): OFA, FCI, BVA/KC y PennHIP; la ventrodorsal con extremidades extendidas es la proyección de los tres primeros, mientras PennHIP usa **tres** radiografías. Sostiene las cuatro filas de displasia de §5.2 y la decisión de separarlas |
| <https://avmajournals.avma.org/view/journals/javma/237/5/javma.237.5.532.xml> | Comparación entre la calificación OFA y el índice de distracción PennHIP: son métricas distintas sobre protocolos distintos. Refuerza §5.1 |
| <https://onlinelibrary.wiley.com/doi/10.1111/jvim.15488> | Consenso ACVIM 2019 de enfermedad valvular mitral mixomatosa: la ecocardiografía es el estándar de estadificación y la radiografía torácica la alternativa cuantificada cuando no se dispone de ella. Sostiene las filas de radiografía simple y ecocardiografía de §5.2 |
| <https://www.abanimalimagenesdiagnosticas.com/que-hacemos/tomograf%C3%8DA-1> | Oferta real de tomografía computarizada veterinaria en Colombia. Sostiene §5.2 y el criterio de qué es realista en el país |
| <https://www.movet.co/servicios/imagenes-diagnosticas> | Servicio de imágenes diagnósticas veterinarias en Bogotá con radiografía digital, ecografía y tomografía. Sostiene §5.2 |

### 8.4 Fuentes que no se pudieron consultar

| URL | Qué pasó |
|---|---|
| <https://www.idexx.com/media/filer_public/4c/c2/4cc22b15-02f9-4a71-8d92-fae0f4d61e8b/small_animal_requistion_form.pdf> | El formulario de solicitud de IDEXX, que habría dado el menú completo agrupado por sección, devolvió `ECONNRESET`. **Ninguna fila de §4 depende de él en exclusiva**: todas están respaldadas por al menos una de las otras fuentes |
| <https://www.iris-kidney.com/guidelines/staging.html> | Devuelve 404; la URL viva es `https://www.iris-kidney.com/iris-staging-system`, que sí se consultó |
| Catálogo de servicios de RetterLab | Está en un documento externo enlazado desde la página, no en la página. No se abrió |

---

## 9. Qué NO se verificó

- **No se consultó ninguna base de datos.** Ni la local ni la de dev. Todo lo afirmado sobre el
  esquema sale de leer los changesets y las entidades, que es la fuente de verdad del proyecto.
- **No se ejecutó ningún `EXPLAIN`.** Las tablas están vacías y un plan sobre una tabla vacía no
  sostiene ninguna conclusión (§6).
- **La collation SÍ está verificada, y no por mí.** `utf8mb4_0900_ai_ci`, confirmada en tres
  niveles (`@@collation_server`, default del esquema y una columna `name` real) por la coordinación
  de esta tanda. Es el dato del que depende la comprobación de unicidad de §3.5 punto 2, y por eso
  se incorporó a §1.2.1 en vez de dejarse como supuesto. Lo que **yo** no hice fue la consulta:
  este documento se escribió sin abrir ninguna conexión.
- **No se comprobó qué muestra el front** (`vetsoftware-public-web`) al listar estos catálogos, ni
  si hay un buscador que se degrade con 87 filas frente a 0.
- **No se validó la lista con un médico veterinario.** Cada fila está respaldada por una fuente
  citada en §8, pero la revisión clínica final la debe hacer una persona con esa formación,
  especialmente en la composición implícita de los perfiles de §4.3.
