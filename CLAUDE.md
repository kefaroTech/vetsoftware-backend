# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Reglas activas (override temporal — vigentes hasta que el usuario las revoque)

> Pausa indicada por el usuario el 2026-06-06. Mantener hasta que el usuario diga explícitamente lo contrario.

- ❌ **No actualizar ni crear diagramas `.puml`** (ni `uml/Veterinaria.puml` ni los `uml/sequenceDiagram/**`). Aunque se toque un endpoint, NO sincronizar su diagrama.
- ~~No crear ni modificar tests unitarios~~ — **levantada por el usuario el 2026-08-08**. Los tests se rigen ahora por la sección **Testing conventions**, reescrita ese mismo día: JUnit 6 + Mockito + AssertJ + JaCoCo. La regla vieja de "sin Mockito, stubs manuales" queda derogada.

La pausa de diagramas suspende la convención de "diagramas sincronizados". El resto del documento sigue vigente.

## Las reglas de este documento se verifican solas

`HexagonalArchitectureTest` y `PiramideDeTestsTest` (ArchUnit) ejecutan **veintitrés** de las reglas de aquí y **rompen el build** si se incumplen. Antes de discutir si algo "va contra el CLAUDE.md", córrelas:

```bash
mvn test -Dtest='HexagonalArchitectureTest,PiramideDeTestsTest'
```

**Diecisiete** reglas son duras: dominio sin framework, sin cruce de dominios, **todo puerto de entrada con `@PreAuthorize`**, validar el tenant cuando el puerto recibe `companyId`, sin HTTP externo dentro de una transacción, **cerrar a `ROLE_SYSTEM` los listados que no filtran por empresa**, las **cuatro de la familia «por id»** de BE-COV —ver «Autorización»—, las tres de paginación (BE-21) —**un solo contrato**, **un solo sitio donde se acota el tamaño de página** y **el puente con Spring Data confinado a `infrastructure/persistence`**—, las **tres de bloqueo optimista** de BE-26 —**toda `@Entity` con `@Version` o exenta por escrito**, **el `@SQLDelete` de una entidad versionada acotado por `version`** y **la lista de exenciones sin entradas podridas**— y **ningún doble de test escaneable**. Las otras **seis** encontraron deuda anterior y van **congeladas** (`FreezingArchRule`): lo registrado en `config/archunit/violation-store` se tolera, cualquier violación nueva falla. El store se versiona; solo puede encoger.

Ojo con la palabra «duras»: trece de las diecisiete lo son porque el código ya las cumple —las tres de BE-26 entre ellas, escritas justo después de que la campaña versionara las entidades, que es el criterio normal del repo para que una regla nazca dura—. Las **cuatro de BE-COV nacieron duras con la deuda abierta**, a propósito y contra la costumbre del repo: la campaña que las motivó estaba corrigiéndose en el mismo momento en que se escribieron, así que congelarlas habría fotografiado un estado transitorio y metido al store deuda que iba a desaparecer sola —exactamente la trampa que este documento advierte sobre `allowStoreCreation`—. Mientras el contador no esté a cero, la salida es **corregir**, nunca envolverlas en `freeze(...)`.

**Son dos clases porque miran universos distintos.** `HexagonalArchitectureTest` declara `ImportOption.DoNotIncludeTests` — sus veinte reglas hablan de código de producción, incluidas las cuatro de BE-COV y las tres de BE-26: los puertos, el SQL de los adaptadores, los casos de uso y las entidades JPA son todos `src/main`, así que ninguna necesita ver el árbol de test. Las **tres** que necesitan ver `src/main` y `src/test` a la vez viven en `PiramideDeTestsTest`, con su propio `@AnalyzeClasses` sin esa opción: las dos de BE-10 (**cada adaptador con su rodaja**) y `DOBLE_DE_TEST_NO_ESCANEABLE`.

- **Ningún doble de test puede ser candidato al escaneo de producción** (`DOBLE_DE_TEST_NO_ESCANEABLE`, dura). Toda clase del árbol de test meta-anotada con `@Component` —es decir `@Configuration`, `@SpringBootConfiguration`, `@RestController`, `@Service`, `@Repository` o `@Component`— tiene que estar **meta-anotada con `@TestComponent`**. `@TestConfiguration` ya lo está, así que una configuración de test bien escrita cumple sin mencionarlo; un doble que no sea configuración —un controller de juguete, un `@SpringBootConfiguration` que sirve de raíz a un `@SpringBootTest`— se marca con `@TestComponent` a secas.
  - **El defecto que la justifica**: un test declaró su cableado como `@Configuration` simple. `target/test-classes` está en el classpath de failsafe y esa clase vive dentro de la raíz del `@ComponentScan`, así que su doble de `CreateAppointmentUseCase` se registraba en todos los contextos; `AppointmentController` encontraba dos beans y **ningún `@SpringBootTest` del repositorio arrancaba** — noventa segundos de build para morir con un `NoUniqueBeanDefinitionException` que no señala a la causa.
  - **La sutileza que la hace imprescindible**: `TestTypeExcludeFilter` sí descarta las clases anidadas dentro de una clase de test, pero solo reconoce como clase de test a la que tiene **algún `@Test` de primer nivel**. La culpable tenía todos los suyos dentro de `@Nested`. Es decir: la diferencia entre bomba y bomba desactivada era una condición invisible en revisión, que se arma sola el día que alguien mueve el último `@Test` suelto a un `@Nested`. La regla la convierte en una invariante sintáctica.
  - **`@TestComponent` no cambia cómo se registra el doble.** Los filtros de exclusión solo aplican al escaneo: lo nombrado en un `@Import`, en `@WebMvcTest(controllers = …)` o en `@SpringBootTest(classes = …)` se instala igual.

- **Toda `@Entity` decide por escrito si lleva bloqueo optimista** (`ENTIDADES_CON_BLOQUEO_OPTIMISTA`, dura). O declara un campo `@Version`, o figura en la constante `ENTIDADES_EXENTAS_DE_VERSION` con uno de seis códigos —`E1_APPEND_ONLY`, `E2_TABLA_PUENTE`, `E3_TOKEN`, `E4_VISTA`, `E5_SEMILLA`, `E6_YA_PROTEGIDO`— y el motivo **al lado del nombre**, de modo que el diff de un PR enseñe a quién se le está perdonando y por qué. El hallazgo de BE-26 fueron 104 `@Entity` con 16 `@Version`, y su conclusión no fue «póngaselo a las 88»: sin `@Version`, dos ediciones simultáneas se pisan y el dato desaparece **sin excepción y sin log**; en un asiento contable o una tabla puente, ponerlo es ruido y un 409 que el usuario no sabe resolver. Las dos cosas son ciertas — lo que no existía era el registro de cuál se aplicó a cada tabla. Hoy la cuenta cierra al dígito, y ese cuadre es la prueba de que la lista es exhaustiva y no una muestra: **104 `@Entity` = 71 versionadas + 33 exentas**.
  - **La trampa del `@SQLDelete` de dos parámetros** (`BORRADO_LOGICO_RESPETA_LA_VERSION`, dura). En cuanto una entidad lleva `@Version`, Hibernate liga **dos** parámetros al SQL de su `@SQLDelete` —primero el `id`, después la `version`—, así que el `UPDATE … SET enabled = false WHERE id = ?` que era correcto ayer **queda roto en tiempo de ejecución** hoy. Es el peor tipo de defecto para una revisión humana: la anotación se lee perfecta, el compilador calla y el error solo aparece al borrar. Con 64 entidades versionadas que borran en lógico, versionar una más sin tocar su SQL vuelve a armar la bomba; la regla exige `AND version = ?` en el `WHERE`.
  - **Y mira la condición dentro del `WHERE`, no una subcadena del SQL entero**: `employees` y `system_users` llevan `auth_version = auth_version + 1` en el `SET` —invalidación de sesión, nada que ver con el bloqueo optimista— y una comprobación ingenua las daría por buenas sin comprobar nada. Por lo mismo se fija en la columna y no en el nombre del filtro, que `unit_measure_catalog` borra por `code`.
  - **La lista de exenciones no puede pudrirse** (`EXENCIONES_DE_VERSION_AL_DIA`, dura). Toda entrada tiene que corresponder a una `@Entity` que existe y que **sigue** sin `@Version`. Versionar una entidad exenta y olvidar su línea deja el repositorio afirmando por escrito algo falso —y esa es la forma silenciosa de podrirse, la que nadie nota—; borrar la clase deja ruido que enseña a no leer la lista. Las dos rompen el build.

- **Puerto sin `@PreAuthorize`**: la única salida es anotar la interfaz con `@NoAuthorizationRequired(reason = "...")` y escribir el motivo. No hay forma silenciosa de saltarse el gate.
- **Bajar deuda congelada**: arregla el código y vuelve a correr el test; ArchUnit quita del store lo resuelto. Cuando una regla llegue a cero, quítale el `freeze(...)`.
- **Congelar una regla nueva**: no hace falta tocar nada. `allowStoreCreation=false` solo impide **crear el directorio** del store; con el directorio ya versionado, una regla nueva envuelta en `freeze(...)` registra su foto sola en la primera ejecución. Eso es cómodo y es una trampa: **la deuda entra al repo en silencio**, así que revisa el diff de `config/archunit/violation-store` y cuenta las líneas antes de commitear. Solo se pone en `true` —y se devuelve a `false` en el mismo commit— si el directorio del store no existe.
- **No toques la descripción de una regla congelada.** El store indexa por el texto completo (`stored.rules`): cambiar un predicado o el `because` huérfana la foto y la deuda reaparece entera.

## Commands

```bash
mvn clean package          # build
mvn spring-boot:run        # run
mvn test                   # all tests + informe de cobertura en target/site/jacoco/index.html
mvn test -Dtest=ClassName  # single test class
mvn verify                 # tests + suelo de cobertura (jacoco:check) + checkstyle + spotless

# El contrato OpenAPI vive en api/openapi.json y `mvn verify` falla si se quedó atrás.
# Tras un cambio deliberado de API, regenéralo y commitéalo:
mvn verify -Dit.test=OpenApiContractIT -Dopenapi.write=true
mvn clean verify sonar:sonar -Dsonar.login=<token>   # análisis de código (SonarQube 9.9 LTS en localhost:9000)
```

```bash
npm test                                          # tests de los scripts de .github/scripts
node .github/scripts/dev-version.mjs next         # qué versión de desarrollo tocaría (no escribe nada)
```

## Versionado automático de develop — qué mueve cada dígito

Cada merge a `develop` calcula su propia versión `X.Y.Z-dev.N`, la commitea en `pom.xml`,
`package.json` y `package-lock.json`, y publica la imagen ya versionada. Desplegar en dev
es escribir esa versión y nada más. Las releases (`X.Y.Z` limpias) siguen siendo territorio
exclusivo de `prepare-release.yml`.

La decisión se toma sobre el **tipo convencional** del commit, no sobre el gitmoji:

| En el commit | Bump | Ejemplo | Partiendo de `1.1.0-dev.3` |
|---|---|---|---|
| `!` tras el scope, o footer `BREAKING CHANGE:` | major | `:boom: feat(api)!: …` | `2.0.0-dev.1` |
| `feat` | minor | `:sparkles: feat(kardex): …` | `1.2.0-dev.1` |
| `fix` · `perf` | patch | `:bug: fix(audit): …` | `1.1.1-dev.1` |
| `refactor` · `docs` · `style` · `test` · `build` · `ci` · `chore` | solo N | `:memo: docs: …` | `1.1.0-dev.4` |

Dos reglas que sostienen el esquema:

- **Gana el más alto.** Se evalúan todos los commits que entraron con el merge, no solo el
  asunto del merge: un `feat` entre cuatro `chore` es un minor.
- **Cuando el dígito base se mueve, `N` vuelve a 1.** `1.1.0-dev.7` + un `feat` da
  `1.2.0-dev.1`, no `1.2.0-dev.8`.
- Un `pom.xml` limpio (el back-merge de una release) abre el ciclo siguiente en
  `X.Y.(Z+1)-dev.1` aunque no haya nada que bumpear: emitir `X.Y.Z-dev.1` daría una versión
  *anterior* a la release ya publicada.

El algoritmo vive en `.github/scripts/dev-version.mjs` y esta tabla es su contrato; el
`CHANGELOG.md` no se toca en develop.

## Configuration

MySQL en `src/main/resources/application.yml`:
- DB: `<project>_db` at `localhost:3306`
- `ddl-auto: update` — Hibernate gestiona el schema automáticamente
- Credenciales deben usar variables de entorno: `${DB_USERNAME:root}`

## El contrato de la API es un fichero versionado

`api/openapi.json` es la especificación que expone este backend, y **`mvn verify` falla si no
coincide con el código** (`OpenApiContractIT` levanta la aplicación entera y compara). No se edita
a mano: se regenera con `-Dopenapi.write=true` y se commitea en el mismo PR que el cambio de API.

Ese fichero es la única fuente de verdad de los tipos de los dos frontends, que hasta ahora
declaraban ~565 interfaces a mano sin nada que las atara aquí (TR-01). Cada front lo copia y
genera sus tipos con `openapi-typescript`; sus pruebas fallan si un DTO deja de cuadrar. En la
práctica eso significa que **renombrar un campo de un `record` de `web/response` rompe el build
del front**, que es justo lo que antes no pasaba: compilaba, desplegaba y fallaba en el navegador.

No hace falta anotar nada con `@Schema` ni `@Operation` para que esto funcione — springdoc deriva
el esquema de los tipos. Las anotaciones (BE-20) añaden descripciones y ejemplos a la
documentación publicada, no precisión al contrato.

## Package structure — Vertical Slicing (una feature por paquete raíz)

Cada entidad de negocio vive en su propio paquete raíz completamente auto-contenido.
**Nada se comparte entre features** — ni clases de dominio, ni ports, ni infraestructura.

```
com.<company>.<project>/
├── <feature>/                  ← un paquete por entidad (company, employee, animal…)
│   ├── domain/                 ← entidad, value objects, excepción de dominio
│   ├── application/
│   │   ├── command/            ← records de entrada por caso de uso de escritura
│   │   ├── dto/                ← records de salida compartidos dentro de la feature
│   │   ├── port/
│   │   │   ├── in/             ← SOLO interfaces de casos de uso
│   │   │   └── out/            ← SOLO interfaces de repositorio
│   │   └── usecase/            ← un service por caso de uso
│   └── infrastructure/
│       ├── persistence/        ← XxxJpaEntity, XxxJpaMapper, XxxJpaRepository, JpaXxxRepository
│       └── web/
│           ├── request/        ← DTOs de entrada REST
│           └── response/       ← DTOs de salida REST
├── shared/                     ← el kernel: SOLO tipos sin semántica de negocio (ver criterio)
│   ├── domain/Money.java       ← aritmética monetaria
│   ├── pagination/             ← PageResult (aplicación) + Pages (puente con Spring Data)
│   └── security/               ← @NoAuthorizationRequired
└── infrastructure/
    └── web/
        ├── GlobalExceptionHandler.java
        └── PageResponse.java   ← la página tal como sale por HTTP
```

### Regla de vertical slicing — nunca romper esto

- ❌ No crear capas horizontales top-level (`domain/`, `application/`, `infrastructure/` compartidas)
- ❌ No importar clases de dominio de otra feature (p.ej. `company.domain.Company` desde `employee`)
- ❌ No importar DTOs de aplicación ni Responses de otra feature
- ✅ Si dos features necesitan comunicarse, usar IDs primitivos (`Long companyId`), un companion VO propio (`XxxRef`), o un puerto explícito
- ✅ Excepción acotada: `<feature>/infrastructure/persistence/` puede importar `otraFeature.infrastructure.persistence.XxxJpaEntity` y `XxxJpaRepository` para asociaciones JPA (`@ManyToOne`) — ver sección "Cross-feature references"

### Qué puede entrar en `shared/` — el criterio de admisión

El vertical slicing justifica duplicar tipos **de dominio**: que `animal` y `product` tengan
cada uno su `XxxNotFoundException` es lo que mantiene las features independientes. No justifica
duplicar **infraestructura sin semántica de negocio**, y confundir las dos cosas tuvo un precio
medido: 36 declaraciones del concepto «página» (BE-21), 12 de ellas creadas en una sola semana.

Un tipo entra en `shared/` **solo si cumple las cuatro**:

1. **No significa nada distinto en dos features.** «Redondear a centavos» y «una lista con su
   total y su número de página» no cambian de sentido entre facturas y animales. «El estado de
   una cita» sí.
2. **No tiene estado ni identidad**: `record` inmutable o clase de estáticos. Nada que persista.
3. **No referencia ninguna feature**: ni entidades, ni DTOs, ni FKs, ni enums de negocio.
4. **Duplicarlo obligaría a escribir N veces la siguiente regla transversal** (paginación,
   auditoría de campo, soft delete, filtro de tenant). Ese es el coste real que se evita.

Hoy lo cumplen tres cosas y solo tres: `Money`, `pagination` y `@NoAuthorizationRequired`.
**Ante la duda, no entra**: un tipo de más en `shared/` es la grieta por la que vuelve la capa
horizontal que este documento prohíbe.

## Paginación — un solo contrato a cada lado de la frontera

Hay **dos** tipos de página en todo el proyecto, y ninguno se declara por feature:

| Tipo | Dónde | Quién lo usa |
|---|---|---|
| `shared.pagination.PageResult<T>` | dentro | `port/in`, `port/out`, `usecase`, adaptadores JPA |
| `infrastructure.web.PageResponse<T>` | la frontera | los controllers; es el nombre que ven los fronts en el OpenAPI |
| `shared.pagination.Pages` | puente | **solo** `infrastructure/persistence` |

- **Un caso de uso paginado devuelve `PageResult<XxxDto>`**, y se construye con
  `repository.findAll(...).map(XxxDto::from)`. Nunca recalcules los totales sobre el contenido
  ya paginado: son los de la consulta.
- **El adaptador JPA no construye `PageRequest` a mano.** `Pages.request(page, pageSize, sort)`
  normaliza el índice y acota el tamaño (`DEFAULT_SIZE` 20, `MAX_SIZE` 200); `Pages.result(page,
  mapper::toDomain)` convierte la página de Spring Data. El `Sort` sí lo decide cada adaptador,
  y debe ser total —con desempate por `id`—: sin él, dos páginas consecutivas repiten u omiten
  filas.
- **El controller no arrastra los cinco campos a mano.** `PageResponse.from(result,
  this::toResponse)`, y el `@RequestParam` sigue siendo `page` (base 0) + `pageSize`.
- Las tres reglas de ArchUnit que lo sostienen —`PAGINACION_CON_UN_SOLO_CONTRATO`,
  `PAGINA_ACOTADA_EN_UN_SOLO_SITIO` y `PUENTE_DE_PAGINACION_SOLO_EN_PERSISTENCIA`— son duras.
  Declarar un `PageResult` dentro de una feature, llamar a `PageRequest.of` fuera del kernel, o
  usar `Pages` desde `application`, rompe el build.

```java
// ✅ Adaptador
@Override
public PageResult<Animal> findAllByCompanyId(Long companyId, int page, int pageSize) {
    Sort order = Sort.by(Sort.Direction.ASC, "name").and(Sort.by(Sort.Direction.ASC, "id"));
    return Pages.result(jpaRepository.findAllByCompany_Id(companyId,
            Pages.request(page, pageSize, order)), mapper::toDomain);
}

// ✅ Controller
@GetMapping
public PageResponse<AnimalResponse> listAll(@RequestParam(defaultValue = "0") int page,
        @RequestParam(defaultValue = "20") int pageSize) {
    return PageResponse.from(listUseCase.listAll(authz.currentCompanyId(), page, pageSize),
            this::toResponse);
}
```

## Architecture: Hexagonal (Ports & Adapters)

### Regla de dependencias — nunca romper esta dirección

```
infrastructure → application → domain
```

- `domain`: cero imports de Spring, cero imports de infrastructure
- `application`: cero imports de infrastructure
- `infrastructure`: puede importar de `application` y `domain`

### IDs de entidad — siempre Long autogenerado por la BD

- El ID de toda entidad de dominio es `Long id` (nullable para entidades nuevas antes de persistir).
- En la entidad JPA: `@Id @GeneratedValue(strategy = GenerationType.IDENTITY)` → BIGINT AUTO_INCREMENT.
- **Nunca** usar UUID, String, ni ningún otro tipo como ID.
- El repositorio devuelve la entidad persistida tras el `save()` para capturar el ID generado.
- En Liquibase: columna `id BIGINT AUTO_INCREMENT PRIMARY KEY`.

```java
// ✅ Correcto
@Id
@GeneratedValue(strategy = GenerationType.IDENTITY)
private Long id;

// ❌ Incorrecto
@Id
private String id;           // String
private UUID id;             // UUID
@GeneratedValue(strategy = GenerationType.UUID)
```

### Columnas booleanas — siempre TINYINT pelado (nunca TINYINT(1))

Todo booleano del schema usa **`TINYINT` sin display width**. El proyecto fija
`hibernate.type.preferred_boolean_jdbc_type: TINYINT` (en `application.yml`), así que
un campo `boolean`/`Boolean` mapea a `TINYINT` y Hibernate valida contra `tinyint` pelado.

**Nunca uses `TINYINT(1)`.** El display width `(1)` hace que MySQL Connector/J
(`tinyInt1isBit=true` por defecto) reporte la columna a JDBC como `Types.BIT`, lo que
rompe la validación de schema (`ddl-auto: validate`) con:
`found [bit (Types#BIT)], but expecting [tinyint (Types#TINYINT)]`. Además el width en
enteros está deprecado en MySQL.

```java
// ✅ Correcto — sin columnDefinition; el preferred_boolean_jdbc_type hace el mapeo
@Column(name = "enabled", nullable = false)
private boolean enabled = true;

@Column(name = "rescheduled")          // nullable → Boolean
private Boolean rescheduled;

// ❌ Incorrecto — fuerza tinyint(1) → el driver lo reporta como BIT → falla validate
@Column(name = "rescheduled", columnDefinition = "TINYINT(1)")
private Boolean rescheduled;
```

En Liquibase, declara los booleanos con `type="BOOLEAN"` (MySQL lo materializa como
`tinyint` pelado) — **nunca** `type="TINYINT(1)"`:

```xml
<!-- ✅ Correcto -->
<column name="enabled" type="BOOLEAN" defaultValueBoolean="true">
    <constraints nullable="false"/>
</column>

<!-- ❌ Incorrecto — genera tinyint(1) → BIT → rompe schema-validation -->
<column name="enabled" type="TINYINT(1)" defaultValueNumeric="1"/>
```

Si una columna ya existe como `tinyint(1)` o `bit`, normalízala con un changeset nuevo
(no edites el ya aplicado — rompe el checksum): `ALTER TABLE x MODIFY col TINYINT ...`
(ver `086`/`087`).

### Capa domain

- **Entidad**: campos privados, factory method `Entity.create(...)` genera el ID internamente, método de mutación `entity.update(...)`. Sin setters públicos.
- **Value object**: Java record que envuelve UUID (e.g. `AnimalId`). Factory methods: `generate()` y `of(String)`.
- **Excepción de dominio**: siempre sufijo `Exception` (e.g. `AnimalNotFoundException extends RuntimeException`).
- **Validaciones de invariantes de dominio** van en el constructor de la entidad — nunca en el controller ni en el service:
  ```java
  public Animal(AnimalId id, String name, String species, int age) {
      if (name == null || name.isBlank()) throw new IllegalArgumentException("name is required");
      if (name.length() > 25)            throw new IllegalArgumentException("name must be 25 chars or less");
      if (age < 0)                       throw new IllegalArgumentException("age cannot be negative");
      ...
  }
  ```
- Sin anotaciones de Spring en esta capa.

### Capa application

- **Un service por caso de uso** — nunca fusionar múltiples interfaces en un solo service.
- **Un command por caso de uso de escritura** — `CreateXxxCommand`, `UpdateXxxCommand` (records en `command/`).
- **Un DTO de salida compartido** — `XxxDto` (record en `dto/`) con factory method estático `from(Entity)`.
- **Los ports de entrada (`port/in/`) no importan nada de infrastructure** — usan exclusivamente tipos de `application`.
- Constructor injection en todos los services. Sin `@Autowired`.
- `@Transactional` en cualquier método que ejecute más de una operación de repositorio (e.g. `update` hace `findById` + `save`).
- `DeleteXxxService` debe verificar que la entidad existe antes de borrar y lanzar `XxxNotFoundException` si no.
- **FK a otra feature → companion VO + `YyyQueryPort`**: si una entidad tiene una FK a otra feature, NO uses la entidad de dominio externa. El dominio guarda un companion VO (`YyyRef`) propio de esta feature. El service carga el VO vía un outbound port (`YyyQueryPort` en `port/out/`) que devuelve `Optional<YyyRef>` — esto valida y trae los datos en una sola query. El repositorio usa `getReferenceById()` sin validar. Nunca lanzar `IllegalArgumentException` por FK no encontrada desde el repositorio. Ver sección **Cross-feature references** para el patrón completo.

### Capa infrastructure

**Adaptador de entrada (`web/`):**
- El controller mapea `XxxRequest → XxxCommand`, llama al use case, mapea `XxxDto → XxxResponse`.
- El controller nunca construye entidades de dominio ni llama a `Entity.create(...)`.
- `GlobalExceptionHandler` captura excepciones de dominio:
  - `XxxNotFoundException` → HTTP 404
  - `IllegalArgumentException` → HTTP 400

**Adaptador de salida (`persistence/`):**
- `JpaXxxRepository` implementa el outbound port del dominio.
- `XxxJpaMapper` es el único lugar que conoce tanto el modelo de dominio como la entidad JPA.
- `XxxJpaEntity` tiene constructor vacío explícito: `protected XxxJpaEntity() {}`.

## Cross-feature references — patrón canónico

Cuando una entidad tiene una FK a otra feature y necesitas **datos** del agregado externo (no solo el ID), aplica este patrón en lugar de importar la entidad de dominio de la otra feature.

### Capas y representaciones

| Capa | Tipo | Responsabilidad |
|---|---|---|
| `<feature>/domain/` | `YyyRef` (record con invariantes) | Companion VO — campos del agregado externo que esta feature necesita |
| `<feature>/application/dto/` | `YyySummaryDto` (opcional) | DTO sin invariantes para outputs; si no quieres separación estricta, reusa `YyyRef` |
| `<feature>/application/port/out/` | `YyyQueryPort` | Outbound port: `Optional<YyyRef> findById(Long id)` |
| `<feature>/infrastructure/persistence/` | `JpaYyyQueryPort` | Adapter; consulta `YyyJpaRepository` de la otra feature |
| `<feature>/infrastructure/persistence/` | `@ManyToOne(LAZY) YyyJpaEntity` en `XxxJpaEntity` | Asociación JPA viva — único cruce de vertical slicing permitido |
| `<feature>/infrastructure/web/response/` | `YyySummary` | Forma del JSON expuesto |

### Componentes — ejemplo `submodule` referenciando `module`

**1. Companion VO en domain** — invariantes propias de esta feature:

```java
// submodule/domain/ModuleRef.java
public record ModuleRef(Long id, String name, String code) {
    public ModuleRef {
        if (id == null) throw new IllegalArgumentException("module id is required");
        if (name == null || name.isBlank()) throw new IllegalArgumentException("module name is required");
        if (code == null || code.isBlank()) throw new IllegalArgumentException("module code is required");
    }
}
```

**2. Entidad de dominio** — tiene la VO directamente, no `Long moduleId` colgando:

```java
public class SubModule {
    private ModuleRef module;   // ✅ companion VO
}
```

**3. Outbound port + adapter** — el adapter es el ÚNICO archivo que conoce la otra feature:

```java
public interface ModuleQueryPort {
    Optional<ModuleRef> findById(Long moduleId);
}

@Component
public class JpaModuleQueryPort implements ModuleQueryPort {
    private final ModuleJpaRepository moduleJpaRepository;   // ← cruce permitido
    @Override
    public Optional<ModuleRef> findById(Long moduleId) {
        return moduleJpaRepository.findById(moduleId)
            .map(e -> new ModuleRef(e.getId(), e.getName(), e.getCode()));
    }
}
```

**4. JPA Entity con `@ManyToOne LAZY`** — Hibernate gestiona FK y JOINs:

```java
@ManyToOne(fetch = FetchType.LAZY)
@JoinColumn(name = "module_id", nullable = false)
private ModuleJpaEntity module;
```

**5. `@EntityGraph` en el Spring Data repo** — obligatorio para evitar N+1 con LAZY:

```java
public interface SubModuleJpaRepository extends JpaRepository<SubModuleJpaEntity, Long> {

    @Override
    @EntityGraph(attributePaths = "module")
    List<SubModuleJpaEntity> findAll();

    @Override
    @EntityGraph(attributePaths = "module")
    Optional<SubModuleJpaEntity> findById(Long id);
}
```

**6. Mapper con dos overloads** — uno para reads (extrae VO desde el `@ManyToOne` ya hidratado), otro para writes (reusa la VO precargada para no disparar el proxy):

```java
@Component
public class SubModuleJpaMapper {
    public SubModuleJpaEntity toJpa(SubModule subModule, ModuleJpaEntity module) { ... }

    // Read path — el @EntityGraph ya hidrató entity.getModule()
    public SubModule toDomain(SubModuleJpaEntity entity) {
        ModuleJpaEntity m = entity.getModule();
        return toDomain(entity, new ModuleRef(m.getId(), m.getName(), m.getCode()));
    }

    // Write path — reusa el ref precargado, evita inicializar el proxy de getReferenceById
    public SubModule toDomain(SubModuleJpaEntity entity, ModuleRef ref) { ... }
}
```

**7. `JpaXxxRepository.save`** — `getReferenceById` (proxy sin SELECT) + reusa la VO:

```java
@Override
public SubModule save(SubModule subModule) {
    ModuleJpaEntity module = moduleJpaRepository.getReferenceById(subModule.getModule().id());
    SubModuleJpaEntity saved = jpaRepository.save(mapper.toJpa(subModule, module));
    return mapper.toDomain(saved, subModule.getModule());   // ← reusa el ref
}
```

**8. Service de escritura** — carga la VO antes de construir la entidad:

```java
public SubModuleDto execute(CreateSubModuleCommand command, AuthContext auth) {
    ModuleRef module = moduleQueryPort.findById(command.moduleId())
        .orElseThrow(() -> new IllegalArgumentException("Module not found: " + command.moduleId()));
    SubModule subModule = SubModule.create(command.name(), command.code(), module);
    return SubModuleDto.from(repository.save(subModule));
}
```

### Costo de SQL del patrón

| Operación | Queries |
|---|---|
| `listAll` | 1 (JOIN FETCH gracias al `@EntityGraph`) |
| `findById` | 1 (idem) |
| `create` | 1 SELECT (carga VO) + 1 INSERT |
| `update` | 1 SELECT submodule (con JOIN) + 1 SELECT modules (carga VO) + 1 UPDATE |
| `delete` | 1 SELECT (con JOIN) + 1 DELETE |

### Reglas no negociables

- ❌ El `domain` de una feature **nunca** importa el `domain` de otra feature.
- ❌ El `application` **nunca** importa nada de otra feature.
- ❌ El `web/response` **nunca** importa Responses de otra feature (`SubModuleResponse` no incluye `ModuleResponse`).
- ✅ Solo `infrastructure/persistence/` puede importar `otraFeature.infrastructure.persistence.XxxJpaEntity` y `XxxJpaRepository`.
- ✅ El `YyyQueryPort` reemplaza al antiguo `YyyValidationPort` cuando necesitas datos. Si solo validas existencia (no usas los campos), un `YyyValidationPort` con `void validateExists(Long)` sigue siendo válido.

### Cuándo NO usar este patrón

- **No necesitas datos del agregado externo, solo el ID** → usa `Long moduleId` en el dominio + `YyyValidationPort` para validar en escritura.
- **Las dos entidades son realmente el mismo agregado** (e.g. `Order` y `OrderItem`) → entonces no son features separadas; ponlas en el mismo paquete.

## Efectos externos y transacciones — lo que sale no vuelve

Una transacción protege lo que está en la base de datos. No protege un correo enviado, un
documento transmitido ni un webhook disparado: eso ya salió. Por eso hay dos reglas, y miran
el mismo salto desde lados opuestos.

**Nada de I/O síncrono dentro de la transacción.** Una llamada HTTP retiene la conexión del
pool y los locks mientras dura. Lo comprueba `SIN_IO_EXTERNO_EN_TRANSACCION`, que sigue la
cadena de llamadas —no solo la línea del método— y **se detiene** en los saltos `@Async`:
lo que cruza de hilo ya no bloquea a nadie.

**Ningún efecto `@Async` antes del commit.** Ahí empieza la otra regla,
`EFECTOS_ASINCRONOS_DESPUES_DEL_COMMIT`. El proxy de `@Async` encola la tarea **al instante**,
sin esperar al desenlace; si la transacción revierte después, el efecto ya se entregó y no hay
forma de retirarlo. Y la última línea del método **todavía no es «después del commit»**: quedan
por delante el flush (con el chequeo de `@Version`), el commit en sí y, con propagación
`REQUIRED`, el commit del caller externo que se una a la transacción. Fue BE-18: una cita que
revertía en el flush dejaba al cliente con la confirmación de una cita inexistente.

El patrón es siempre el mismo: **resolver los datos dentro de la transacción, disparar el
efecto en `afterCommit`.**

```java
private void sendAfterCommit(ConfirmationData confirmation) {
    if (confirmation == null) {
        return;
    }
    // Sin transaccion activa (test unitario, caller sin @Transactional) se envia en el
    // acto: registerSynchronization lanzaria IllegalStateException.
    if (!TransactionSynchronizationManager.isSynchronizationActive()) {
        emailSender.send(confirmation);
        return;
    }
    TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
        @Override
        public void afterCommit() {
            try {                       // una excepcion aqui se propaga al caller aunque
                emailSender.send(confirmation);   // la transaccion ya haya confirmado
            } catch (RuntimeException exception) {
                log.warn("No se pudo enviar la confirmacion: {}", exception.getMessage());
            }
        }
    });
}
```

Tres detalles que el patrón exige:

- **Las consultas del payload van fuera del callback.** Después del commit la conexión volvió
  al pool y cada `find…` abriría la suya, sumando latencia a la respuesta.
- **El callback nunca lanza.** Una excepción en `afterCommit` se propaga al caller con la
  transacción ya confirmada, y convierte una operación correcta en un 500.
- **El diferido va en su propio método**, con la clase anónima. La regla se detiene en el
  primer método que habla con `TransactionSynchronizationManager` —el envío inmediato de la
  rama de guarda no la dispara—, pero un `afterCommit` escrito con un *lambda* le queda
  atribuido al método que lo declara y da falso positivo.

Referencias en el código: `CreateAppointmentService.sendAfterCommit` y
`EmitElectronicDocumentOnCloseService.execute`. Para métricas ya existe
`AfterCommitMetricRecorder.recordAfterCommit(Runnable)`, pero **vive en `infrastructure/` y no
se puede importar desde `application/usecase`** (`APPLICATION_NO_CONOCE_INFRASTRUCTURE`).

## Autorización — `@PreAuthorize` y `Authz`

Todo recurso scoped a una `Company` (multi-tenant) se protege con permisos + ownership. El frontend **nunca** elige el `companyId`: lo deriva el backend desde el `AuthContext` que el `AuthFilter` puso en `SecurityContextHolder` al validar el JWT.

### Reglas no negociables

- ❌ El request REST de un endpoint scoped al usuario **nunca** lleva `companyId` — un cliente malicioso podría suplantar otra empresa.
- ✅ El controller obtiene `companyId` con `authz.currentCompanyId()` y lo inyecta en el command.
- ✅ El `@PreAuthorize` del puerto de entrada **siempre** valida `@authz.isMyCompany(#command.companyId)` como defensa en profundidad — protege contra otros callers o bugs futuros que pasen un `companyId` distinto.
- ✅ Endpoints globales para admin/SYSTEM que sí pueden elegir company van en otro caso de uso aparte; no se mezclan con los de employee.
- ✅ **Un listado que no filtra por empresa solo lo puede servir `hasRole('SYSTEM')` a secas.** Si el repositorio sabe filtrar por empresa —declara algún método que recibe `companyId`—, cualquier `find…` suyo que devuelva varias filas sin ese filtro devuelve filas de todos los tenants. Acotarlo por una FK ajena (`findAllByAnimalId`, `findByHospitalizationId`) **no** cuenta: el animal es de alguien. La regla `LISTADOS_SIN_EMPRESA_SOLO_SYSTEM` lo comprueba y es dura (BE-29).
- ✅ Lo que el tenant necesita va en un caso de uso hermano que sí recibe `companyId`: `listByCompany(companyId)` para el listado de la empresa, `listAvailable(companyId)` para los catálogos que mezclan filas globales y privadas.

### La familia «por id» — BE-COV, cuatro reglas duras

El permiso dice **qué** puede hacer un empleado, nunca **sobre qué filas**. Un `id` lo escribe el
cliente en la URL, así que toda operación que señala una fila concreta necesita además saber de
quién es. Una auditoría de cobertura encontró **~65 puntos en 27 de 94 features** donde faltaba
—y `LISTADOS_SIN_EMPRESA_SOLO_SYSTEM` pasaba en verde con todos ellos vivos, porque solo mira
**listados**—. Las cuatro reglas nuevas cierran el hueco desde sus cuatro ángulos, y son
**disjuntas por construcción**: ningún punto del código lo marca más de una:

| Regla | Qué exige | Cómo evita el falso positivo |
|---|---|---|
| `OPERACIONES_POR_ID_SIN_EMPRESA_SOLO_SYSTEM` | Un puerto de `port/in` que recibe un `Long` (todo id de entidad lo es) o un command con campo `id`, y **no** recibe `companyId`, solo puede estar abierto a `hasRole('SYSTEM')` a secas | Solo aplica si **alguna entidad JPA de la feature alcanza `CompanyJpaEntity`** por asociaciones. Los catálogos maestros (`countries`, `modules`, `spa_types`, `memberships`, los `system_*`) y la propia `companies` no llegan, y la regla ni los mira |
| `MUTACIONES_SQL_ACOTADAS_POR_EMPRESA` | Toda `@Query` cuyo statement empiece por `UPDATE`/`DELETE` debe nombrar la empresa: columna directa, `JOIN` contra la tabla padre o `EXISTS` correlacionado | Igual discriminador de entidad, **más** la exención de la *sobrecarga acotada del mismo nombre*: `reactivate(id)` junto a `reactivate(id, companyId)` es el camino SYSTEM declarado, y es el patrón corregido |
| `CARGA_POR_ID_ACOTADA_POR_EMPRESA` | Un `usecase` que llama a `findById(...)` sobre un `port/out` que **también** declara la variante acotada tiene que llamarla en esa misma clase | El ternario legítimo `companyId == null ? findById(id) : findByIdAndCompanyId(id, companyId)` llama a **las dos**; la fuga es la clase que solo conoce la ancha. **Exime al servicio que solo alcanza SYSTEM**: ahí la carga ancha es lo correcto, porque un principal SYSTEM no tiene empresa |
| `REFERENCIAS_CROSS_FEATURE_ACOTADAS_POR_EMPRESA` | El puerto con el que un `usecase` resuelve una referencia a **otra** feature (`animalQueryPort.findById`) tiene que ofrecer —y usar— la variante acotada | Solo servicios que **ya** usan alguna acotada (ya tienen el `companyId`), solo llamadas `find…` que devuelven un `XxxRef` (no `isOpen`/`lockForUpdate`), y solo si la entidad referida pertenece a una empresa. Si el puerto **sí** ofrece la acotada, el caso es de la regla anterior |

- **La peor es la del SQL.** En un `delete` o un `update` corriente hay una lectura previa que
  valida la propiedad; en un `reactivate` **no la hay** —el servicio decide si la fila existe
  mirando cuántas actualizó—, así que el `WHERE` es toda la seguridad. Un
  `UPDATE employee_roles SET enabled = true WHERE id = :id` devolvía el rol revocado a un
  empleado de otra empresa y le vaciaba la caché de permisos: escalada de privilegios
  cross-tenant en cuatro líneas. Modelos correctos: `RoleJpaRepository`,
  `DebtOpenAccountJpaRepository` y el `EXISTS` de `HospitalizationProcedureJpaRepository`.
- **La que ninguna revisión humana ve es la tercera.** Doce `Update…UseCase` llevaban
  `@authz.isMyCompany(#command.companyId)` y eran vulnerables igualmente: esa anotación solo
  prueba que el atacante declara *su propia* empresa —el controller siempre la inyecta desde el
  principal—, no de quién es la fila. Con `findById(command.id())` y luego
  `entidad.update(…, company)` el efecto no es un rechazo sino una **apropiación**: la fila de la
  empresa B pasa a ser de A. La anotación «se ve bien» y el defecto está debajo. Referencia del
  arreglo: `spa/application/usecase/UpdateSpaService`.
- **Cuando el puerto no ofrece vía acotada, hay que crearla.** `medicationschedule` y
  `procedureschedule` acotan hoy por `hospitalization_medication_id` /
  `hospitalization_procedure_id`, que es una FK ajena y **no** cuenta como filtro de empresa —el
  mismo criterio que en BE-29—.
- **La cuarta forma no se apropia de nada: cuelga lo tuyo de un padre ajeno.** Con la carga propia
  ya acotada, un `UpdateSurgeryService` no puede robar la cirugía de otro; lo que puede es
  **reapuntar la suya al animal de otro tenant**, porque resuelve la referencia con
  `animalQueryPort.findById(command.animalId())`. El resultado es una cirugía de tu empresa en la
  historia clínica de la vecina. `spa`, `prescription` y `consultation` declaran
  `findByIdAndCompanyId` en su `AnimalQueryPort` y son el modelo; `laboratorytest`, `surgery`,
  `diagnosticimaging`, `daycare`, `deworming`, `hospitalization` y `vaccination` solo declaran
  `findById`, así que **el arreglo empieza por añadir el método al puerto**.

### Bean `Authz`

`auth/infrastructure/security/Authz.java`, expuesto como `@authz`:

- `isMyCompany(Long companyId)` — `true` si el principal es `EmployeeContext` y `companyId` coincide.
- `currentCompanyId()` — devuelve el `companyId` del `EmployeeContext`; lanza `AccessDeniedException` si no hay contexto de empleado.

### Patrón canónico — crear recurso scoped a la company del usuario

**1. Request sin `companyId`:**
```java
public record CreateOwnerRequest(
        @NotBlank String name, ..., @NotNull Long cityId
) {}   // ← sin companyId
```

**2. Command con `companyId`** (el service lo necesita para llegar al dominio):
```java
public record CreateOwnerCommand(..., Long cityId, Long companyId) {}
```

**3. Controller inyecta `companyId` desde el contexto:**
```java
@PostMapping
public OwnerResponse create(@Valid @RequestBody CreateOwnerRequest request) {
    return toResponse(createUseCase.execute(
        new CreateOwnerCommand(..., request.cityId(), authz.currentCompanyId())));
}
```

**4. Use case con `@PreAuthorize` defensiva:**
```java
public interface CreateOwnerUseCase {
    @PreAuthorize("hasAuthority('admin.all') or " +
                  "(hasAuthority('owner.create') and @authz.isMyCompany(#command.companyId))")
    OwnerDto execute(CreateOwnerCommand command);
}
```

### SpEL en `@PreAuthorize` — referencias a parámetros

- `#paramName` debe coincidir **exactamente** con el nombre del parámetro del método. Para `execute(CreateOwnerCommand command)` es `#command.companyId`, no `#id`.
- Si referencias un parámetro inexistente, SpEL lo resuelve a `null` silenciosamente — `isMyCompany(null)` devuelve `false` y la regla siempre falla. Bug difícil de detectar; revísalo cada vez que renombres un parámetro o copies un `@PreAuthorize` entre métodos.

### Rutas públicas

Las rutas que no requieren JWT se declaran en `AuthFilter.PUBLIC_PATHS` como pares `(method, pattern)` con `AntPathMatcher` (e.g. `new PublicRoute("POST", "/auth/login/**")`). No usamos anotaciones por método (`@PublicEndpoint`) — eso requiere consultar el handler mapping desde un filter, lo cual es frágil con el `PathPatternParser` de Spring Boot 3.

### Anti-patterns auth

- ❌ Aceptar `companyId` en `XxxRequest` para recursos scoped al usuario
- ❌ `@PreAuthorize("@authz.isMyCompany(#id)")` cuando el método es `execute(XxxCommand command)` — `#id` es `null`, regla siempre falsa
- ❌ Mezclar lógica de admin global y employee-scoped en el mismo `UseCase`; separa en dos casos de uso
- ❌ Validar ownership sólo en el controller (saltable desde otro caller); mantener siempre la regla en el `@PreAuthorize` del port

## Naming conventions

| Elemento | Convención | Ejemplo |
|---|---|---|
| Entidad de dominio | `PascalCase` sustantivo | `Animal` |
| Value object | `PascalCase` + tipo | `AnimalId` |
| Companion VO (FK a otra feature) | entidad externa + `Ref` | `ModuleRef` (en `submodule.domain`) |
| Excepción de dominio | `PascalCase` + `Exception` | `AnimalNotFoundException` |
| Puerto de entrada (CRUD) | verbo + entidad + `UseCase` | `CreateAnimalUseCase` |
| Puerto de entrada (lista) | `List` + entidad plural + `UseCase` | `ListAnimalsUseCase` |
| Método de lista | `listAll()` | — |
| Puerto de salida | entidad + `Repository` | `AnimalRepository` |
| Puerto de salida (FK con datos) | entidad externa + `QueryPort` | `ModuleQueryPort` (en `submodule.application.port.out`) |
| Puerto de salida (FK solo validar) | entidad externa + `ValidationPort` | `ModuleValidationPort` |
| Adapter de QueryPort | `Jpa` + entidad externa + `QueryPort` | `JpaModuleQueryPort` |
| Command | verbo + entidad + `Command` | `CreateAnimalCommand` |
| DTO de aplicación | entidad + `Dto` | `AnimalDto` |
| DTO de aplicación (companion) | entidad externa + `SummaryDto` | `ModuleSummaryDto` |
| Service (CRUD) | verbo + entidad + `Service` | `CreateAnimalService` |
| Service (lista) | `List` + entidad plural + `Service` | `ListAnimalsService` |
| Entidad JPA | entidad + `JpaEntity` | `AnimalJpaEntity` |
| Mapper JPA | entidad + `JpaMapper` | `AnimalJpaMapper` |
| Spring Data repo | entidad + `JpaRepository` | `AnimalJpaRepository` |
| Adaptador repositorio | `Jpa` + entidad + `Repository` | `JpaAnimalRepository` |
| Controller REST | entidad + `Controller` | `AnimalController` |
| Request REST | verbo + entidad + `Request` | `CreateAnimalRequest` |
| Response REST | entidad + `Response` | `AnimalResponse` |
| Response REST (companion) | entidad externa + `Summary` | `ModuleSummary` (en `submodule.infrastructure.web.response`) |

## Testing conventions

> Reescritas el **2026-08-08**. La convención anterior —*«sin Spring context, sin Mockito,
> stubs manuales inline»*— queda **derogada**: los stubs manuales hacen que el contrato del
> puerto lo defina el propio test, que es exactamente donde vivió BE-01 durante meses. Los
> tests ya escritos con stubs manuales siguen siendo válidos y **no hay que migrarlos en masa**;
> se modernizan cuando se toque su feature.

### El stack lo fija el BOM — no declares versiones de test en el `pom.xml`

`spring-boot-starter-test` (Boot **4.1.0**, Framework **7.0.8**) ya trae todo y con estas
versiones gestionadas. Añadir una versión a mano es como se rompe un upgrade.

| Herramienta | Versión | Para qué |
|---|---|---|
| JUnit Jupiter | **6.0.3** | motor de test |
| Mockito (+ `mockito-junit-jupiter`) | **5.23.0** | dobles de los puertos |
| AssertJ | **3.27.7** | aserciones — la API por defecto |
| Testcontainers | **2.0.5** | MySQL real para `@DataJpaTest` (fase siguiente) |
| ArchUnit | 1.4.1 | reglas de este documento |
| JaCoCo | 0.8.14 | cobertura |

**Java 25 obliga a cargar el agente de Mockito explícitamente.** La JDK ya no permite
adjuntar agentes dinámicamente en silencio, así que `surefire` pasa
`-javaagent:"${org.mockito:mockito-core:jar}"`. **No toques ese `argLine`** — y si lo
tocas, mantén el `@{argLine}`, que es lo que deja entrar al agente de JaCoCo.

### Qué herramienta por capa

| Capa | Cómo se prueba | Mocks |
|---|---|---|
| `domain/` | JUnit + AssertJ puros. Instancia la entidad de verdad | ❌ **nunca** |
| `application/usecase/` | JUnit + `@ExtendWith(MockitoExtension.class)`, sin contexto de Spring | ✅ solo los puertos `port/out` |
| `application/dto/` | JUnit puro sobre `from(...)` — campo por campo | ❌ |
| `persistence/XxxJpaMapper` | JUnit puro, ida y vuelta dominio↔entidad | ❌ |
| `persistence/JpaXxxRepository` | `@DataJpaTest` + Testcontainers MySQL | ❌ — base real |
| `web/XxxController` | `@WebMvcTest` + `@MockitoBean` sobre los use cases | ✅ los puertos `port/in` |
| reglas del CLAUDE.md | ArchUnit (`HexagonalArchitectureTest`, `PiramideDeTestsTest`) | — |

### Las dos últimas filas son obligatorias, y hay una regla que lo comprueba (BE-10)

Las cuatro primeras filas de esa tabla se cubren solas: son JUnit puro y nadie se salta un
test de dominio. Las **rodajas** —`@DataJpaTest` del adaptador y `@WebMvcTest` del
controller— sí se saltaban, y ese fue el defecto BE-10: el mapper se probaba aislado y el
caso de uso con mocks, así que **el SQL y el HTTP no los ejercitaba nadie**. Un `Sort` sin
desempate que repite filas entre páginas, un `@PreAuthorize` con un `#param` inexistente, un
`@EntityGraph` que no evita el N+1 que cree evitar, un campo del JSON renombrado: nada de eso
lo ve un test de mapper ni un test de service.

`PiramideDeTestsTest` lo convierte en regla. Son dos, y **nacen congeladas** porque la deuda
que encontraron es grande: de 91 adaptadores JPA hay **74 sin rodaja**, y de 92
`@RestController` hay **78 sin rodaja**. Esas 152 líneas son el store, y solo pueden bajar:

| Regla | Qué exige | Nombre esperado |
|---|---|---|
| `ADAPTADOR_JPA_CON_RODAJA` | Todo `Jpa<Algo>Repository` de `..infrastructure.persistence` tiene en **su mismo paquete** una clase `*IT` cuyo nombre contenga `<Algo>` | `<Algo>PersistenceIT` |
| `CONTROLLER_CON_RODAJA` | Todo `@RestController` tiene en **su mismo paquete** una clase `*Test` cuyo nombre empiece por el del controller | `<Xxx>ControllerTest` |

Cuatro decisiones de criterio, que son las que evitan que la regla mienta:

- **Se cruza por el nombre, no solo por el paquete.** Un único `*IT` en
  `animal/infrastructure/persistence` taparía los dieciséis adaptadores de esa feature. La
  unidad de medida es el adaptador porque es la unidad de riesgo: cada uno tiene su consulta.
- **La rodaja web se exige por prefijo** porque en `infrastructure/web` conviven rodajas y
  tests unitarios corrientes. `RefreshTokenCookieTest`, `CashArqueoCsvTest` e
  `InventoryCsvTest` son JUnit puro sobre un helper y **no** cuentan como red de un endpoint.
- **`GlobalExceptionHandler` no es un controller**: es `@RestControllerAdvice`, anotación que
  no está meta-anotada con `@RestController`. El predicado ni lo mira.
- **Los fixtures de test no son adaptadores.** Importar `src/test` para *encontrar* las
  rodajas mete también sus dobles en el universo analizado:
  `GlobalExceptionHandlerTest.BoomController` es un `@RestController` de juguete y la regla
  llegó a exigirle su propia rodaja. `sonCodigoDeProduccion()` descarta lo que viene de
  `target/test-classes` y lo que es una clase anidada. Que no sea un adaptador no lo hace
  inofensivo: ese mismo `BoomController` va marcado con `@TestComponent` para que no pueda
  entrar en el escaneo de producción, y eso lo comprueba `DOBLE_DE_TEST_NO_ESCANEABLE` (ver
  «Las reglas de este documento se verifican solas»).

Fuera de alcance a propósito: los `JpaXxxQueryPort` / `JpaXxxValidationPort` y los adaptadores
que no siguen el naming (`NumberingAllocationAdapter`, `DianJobLeaseAdapter`). Ampliar el
alcance es ampliar el predicado de la regla, nunca relajar la condición.

**Lo que significa en la práctica**: añadir un `JpaXxxRepository` o un `@RestController` sin su
rodaja **rompe el build ese mismo día**. Lo viejo está en el store y solo puede bajar; cuando
llegue a cero, se les quita el `freeze(...)` y pasan a duras.

### Mockito — reglas duras

- **Siempre `@ExtendWith(MockitoExtension.class)`.** Nunca `Mockito.mock()` suelto en un
  campo: pierdes `STRICT_STUBS`, que es lo que detecta stubs que ya nadie usa y llamadas
  con argumentos distintos a los esperados. Ese chivato vale más que el mock.
- **No relajes la estrictez.** `lenient()` o `Strictness.LENIENT` exigen un comentario con
  el motivo en la misma línea. Casi siempre significa que el test está mal montado.
- **Nunca mockees lo que no es un puerto.** Entidades de dominio, `record`s, VOs, commands
  y DTOs se construyen de verdad. Un `Animal` mockeado no valida sus propias invariantes,
  así que el test pasa con datos que producción rechazaría.
- **Nunca mockees la clase bajo prueba**, ni `spy()` parciales sobre ella.
- **`verify` solo para efectos**, no para consultas. Si el método devuelve algo, la
  aserción es el valor devuelto; verificar además que se llamó al repositorio es acoplar el
  test a la implementación.
- **`ArgumentCaptor` para afirmar *qué* se guardó**, no solo que se guardó. `verify(repo).save(any())`
  es una aserción vacía — pasa igual si guardas el objeto equivocado.
- **`verifyNoInteractions` / `verifyNoMoreInteractions` cuando el escenario es «no debe
  escribir»**: validación que falla, tenant ajeno, entidad inexistente. Es la mitad del
  valor de estos tests.

```java
@ExtendWith(MockitoExtension.class)
class CreateAnimalServiceTest {

    @Mock private AnimalRepository repository;
    @Mock private SpecieQueryPort specieQueryPort;
    @InjectMocks private CreateAnimalService service;

    @Test
    @DisplayName("persiste el animal con la especie resuelta por el puerto")
    void persiste_el_animal_con_la_especie_resuelta() {
        when(specieQueryPort.findById(7L)).thenReturn(Optional.of(UN_PERRO));
        when(repository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        service.execute(comandoValido());

        ArgumentCaptor<Animal> guardado = ArgumentCaptor.forClass(Animal.class);
        verify(repository).save(guardado.capture());
        assertThat(guardado.getValue().getSpecie()).isEqualTo(UN_PERRO);   // ← el qué, no el que
    }

    @Test
    @DisplayName("no toca el repositorio si la especie no existe")
    void no_toca_el_repositorio_si_la_especie_no_existe() {
        when(specieQueryPort.findById(7L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.execute(comandoValido()))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Specie not found: 7");

        verifyNoInteractions(repository);
    }
}
```

### JUnit 6 y AssertJ

- **`assertThatThrownBy` de AssertJ**, no `assertThrows`: encadena la aserción sobre el
  mensaje y el tipo en una sola expresión legible.
- **`@DisplayName` en castellano** describiendo el comportamiento; el nombre del método en
  `snake_case` (se mantiene la convención anterior). El `@DisplayName` es lo que se lee en
  el informe de fallos.
- **`@Nested` para agrupar por escenario** (`class Creacion`, `class Validaciones`,
  `class Tenancy`). Un test class por service sigue vigente.
- **`@ParameterizedTest` para matrices de validación** — cada invariante de una entidad es
  un caso, no un test copiado quince veces. `@EnumSource` para recorrer enums completos:
  es lo que detecta el `switch` al que se le olvidó una rama nueva.
- **Sin `if`, `for` ni `try/catch` en el cuerpo de un test.** Si hace falta lógica, el caso
  está mal partido.
- **Sin JUnit 4.** No hay `junit-vintage` en el classpath: `org.junit.Test`,
  `@RunWith` y `Assert.*` no compilan.
- **`@Disabled` solo con motivo escrito y enlace al issue.**

### Determinismo — la regla que más fallos intermitentes evita

- **Nada de `LocalDate.now()` / `LocalDateTime.now()` / `Instant.now()` en código de
  producción que un test tenga que afirmar.** Inyecta `java.time.Clock` por constructor y
  usa `Clock.fixed(...)` en el test. Un test que compara contra `LocalDate.now()` es un
  test que se cae solo el día que el reloj cruce medianoche entre dos líneas.
- **Deuda registrada, no excusa:** `Animal.create`, `AnimalAlert.create`,
  `ConsultationType.create`, `SystemUser.create`, `CreateAnimalService`,
  `CreateWeightRecordService` y `JpaAnimalReportQueryPort.ageLabel` llaman a `now()` hoy.
  Código nuevo inyecta `Clock`; el existente se migra al tocar la feature.
- Sin `Thread.sleep`, sin aleatoriedad, sin dependencia del orden de ejecución, sin estado
  `static` mutable compartido entre tests.

### Datos de prueba — *object mother* por feature

Los fixtures viven en `src/test/java/…/<feature>/testsupport/XxxMother.java`, con un método
por variante y valores válidos por defecto. **No** se crea un paquete de fixtures
compartido entre features: el vertical slicing aplica igual en `src/test`.

```java
public final class AnimalMother {
    public static Animal perroSano()                  { … }
    public static Animal fallecido(LocalDate fecha)   { … }
}
```

### Cobertura — JaCoCo como detector, nunca como objetivo

```bash
mvn test                      # corre tests y genera target/site/jacoco/index.html
mvn verify                    # además comprueba el suelo de cobertura
```

- **Línea base al 2026-08-08: 12,73 % de líneas** (17.968/141.109) y 13,82 % de ramas,
  sobre 622 tests. Ojo: **no es el 2,3 % de la auditoría** — aquel número era el ratio
  *archivos de test / archivos de producción*, que no mide lo mismo.
- **Tras la suite del módulo `animal` (2026-08-08): 14,24 % global**, con 803 tests. Ese
  módulo es la **referencia** de lo que se espera al modernizar una feature: `domain`,
  `application/command`, `application/dto` y `application/usecase` al **100 %** de línea y
  **95,9 % de rama**; `infrastructure/persistence` al 29 % (solo los mappers — los
  adaptadores JPA necesitan `@DataJpaTest`) y `web` sin tocar (necesita `@WebMvcTest`).
- **Tras extender la suite a 17 features más (2026-08-09): 25,64 % de línea**
  (7.192/28.045) y 29,18 % de rama, sobre **2.897 tests**. El suelo del `pom.xml` sube en
  el mismo PR a `0.25`.
- El `pom.xml` fija un **trinquete**: `jacoco.line.minimum` es un suelo global que **solo
  puede subir**. Se sube a mano cuando un PR lo supera de forma estable. **Bajarlo requiere
  justificarlo en el PR** — es la única forma de que la cifra signifique algo.
- **Tras las rodajas de persistencia y web de BE-10 (2026-08-16): 53,15 % de línea**
  (14.828/27.898) y 43,32 % de rama, sobre **3.978 tests de surefire y 428 de failsafe**,
  cero fallos. El suelo global **se queda en `0.33`**: es un salto de casi 28 puntos de una
  sola vez y el trinquete solo sube cuando la cifra es estable, no en el PR que la
  produce. **Superado el 2026-08-17** — ver la entrada siguiente.
- **Tras la campaña BE-COV (2026-08-17): 98,83 % de línea** (27.883/28.214) y **94,54 % de
  rama** (7.648/8.090), sobre **10.771 tests de surefire y 1.130 de failsafe**, cero fallos y
  cero errores, con `checkstyle`, `spotless:check` y `OpenApiContractIT` en verde. Los
  ficheros de test pasan de 382 a **1.809** y las rodajas `*IT` de 27 a **93**. El suelo
  global **sube a `0.98`**: esta vez sí se mueve el trinquete, porque el número lo produce la
  suite entera ejecutándose de verdad y no una medición parcial.
  - **La cifra no se persiguió, se encontró.** Lo que se escribió fueron las rodajas que
    faltaban; el 98 % es la consecuencia. Por el camino la campaña destapó **~65 fugas de
    aislamiento entre empresas en 27 de las 94 features** —tres de escalada de privilegios y
    una de exfiltración de historia clínica— que **ninguna regla veía**, porque
    `LISTADOS_SIN_EMPRESA_SOLO_SYSTEM` solo mira listados y todas eran escrituras (y una
    lectura) **por id**. De ahí salieron las cuatro reglas nuevas de la familia «por id».
    Esto es exactamente lo que esta sección predica: **JaCoCo como detector**.
  - **Aviso que vale más que el porcentaje: una suite con una cascada de errores no está
    midiendo nada.** Los 278 errores de integración del primer `verify` eran **una sola
    causa** —`minimum-idle: 5` heredado de `application.yml` × los 32 contextos que cachea
    Spring = 160 conexiones contra un `max_connections` de 151—, y **debajo había defectos
    reales de test que nadie veía**: un `@Import` sin el mapper en tres rodajas, un
    `assertThatThrownBy` que envolvía el `flush` en vez del `save` que realmente violaba el
    índice, y un `SchemaSeed` que llevaba tiempo aparentando sembrar geografía sin sembrarla.
    Mientras la cascada tapaba el resultado, el 25 % de las `*IT` no arrancaba y la cobertura
    que se leía era honesta pero incompleta.
- **Umbrales por paquete de riesgo: evaluados con datos y NO añadidos.** La condición de
  llegar al 70 % ya se cumple si se agrega por feature —inventory 97,77 %, cashregister
  96,98 %, goodsreceipt 94,77 %, purchaseorder 89,53 %, supplierinvoice 75,61 %— pero **esa
  no es la granularidad que evalúa JaCoCo**: `<element>PACKAGE</element>` mide cada paquete
  real por separado, y ahí **8 de los 42 paquetes de esas cinco features están por debajo
  del 70 %** (`purchaseorder.infrastructure.web.response` al 0 %,
  `purchaseorder.infrastructure.web` al 22,64 %, `supplierinvoice.application.dto` al
  34,78 %, `supplierinvoice.application.usecase` al 51,91 %, y los `infrastructure/pdf` de
  6 y 9 líneas). Cualquier suelo con sentido dejaría el CI en rojo salvo que se le adjunte
  una lista de exclusiones con exactamente los agujeros de hoy, que es congelar la deuda
  llamándola umbral. **El agregado por feature engaña**: purchaseorder marca 89,53 % con su
  controller al 22,64 %.
  - Cuidado además al escribir el `<includes>`: tiene que ir anclado
    (`com/vetsoftware/app/inventory/*`). Un patrón por subcadena se traga
    `goodsreceipt.infrastructure.inventory` y `electronicdocument.infrastructure.inventory`,
    que son adaptadores de salida de **otras** features.
  - Lo que sí cubre ese riesgo es `PiramideDeTestsTest`: `purchaseorder.infrastructure.web`
    está al 22,64 % **porque `PurchaseOrderController` no tiene rodaja**, y la regla lo dice
    con nombre y apellido en vez de con un porcentaje.
- **Excluido del cómputo** y por qué: `*JpaEntity` (solo campos y accesores), `config/**`,
  `*Request`/`*Response` (forma del JSON, sin lógica), **el paquete
  `infrastructure/web/response/` entero** y la clase `main`. Medirlos infla el número sin
  decir nada del riesgo.
  - El paquete completo y no solo el sufijo `*Response`: dentro solo viven `*Response` y los
    136 `*Summary` companion —comprobado: ni un `throw`, ni un `return`, ni una factoría,
    cero ramas—, y el glob por paquete además alcanza los records **anidados**
    (`AccountsPayableAgingResponse$Bucket`) que `**/*Response.class` dejaba midiendo. Tiene
    que ir anclado al paquete: `**/*Summary*.class` se tragaría los 134 `*SummaryDto` de
    `application/dto`, que **sí** son lógica de proyección.
  - **Un glob que no casa no excluye nada, y no avisa.** La exclusión de la clase de arranque
    decía `**/VetsoftwareApplication.class` (s minúscula) y la clase es `VetSoftwareApplication`:
    estuvo sin aplicarse desde que se escribió. Al añadir un glob, comprueba que el número de
    clases medidas baja.
  - **El criterio para admitir uno nuevo**: se leen **todas** las clases que el glob captura, y
    si una sola tiene una rama, un `throw`, una factoría o una traducción, el glob se rechaza.
    Medido: las exclusiones «obvias» que se descartaron por esto —excepciones, enums,
    `*Properties`, los `*Ref` del dominio— **bajaban** la cifra, porque retiraban más línea
    cubierta que sin cubrir.
- **Prohibido escribir tests para mover el número.** Cobertura alta sobre getters es peor
  que cobertura baja honesta: entierra la señal. Lo que se mide es dónde falta red, no cuánto
  se ha trabajado.

### Anti-patterns de test — nunca hacer esto

- ❌ `@MockBean` / `@SpyBean` — **eliminados en Spring Framework 7**. Son `@MockitoBean` y
  `@MockitoSpyBean`, y viven en `org.springframework.test.context.bean.override.mockito`.
- ❌ `@SpringBootTest` para probar un service. Arranca el contexto entero para ejercitar
  una clase: minutos de CI a cambio de nada. `@SpringBootTest` solo cuando lo que se prueba
  **es** el cableado (seguridad, filtros, autoconfiguración).
- ❌ Mockear `AnimalRepository` **y además** afirmar sobre el mock en vez de sobre el
  resultado del caso de uso.
- ❌ Un test por método público del service. Se prueba **comportamiento** —incluidos los
  caminos de fallo—, no superficie de API.
- ❌ Tests sin aserción, o cuya única aserción es `verify(...)` de una consulta.
- ❌ Afirmar sobre el mensaje exacto de una excepción con `isEqualTo` — usa
  `hasMessageContaining` con la parte estable (el prefijo y el id).
- ❌ Compartir `@Mock` mutables entre `@Nested` con estado acumulado entre tests.
- ❌ Tocar la base de datos o la red desde un test unitario.
- ❌ **`INSERT IGNORE` en un seed de test.** Convierte el error en un aviso, así que la fila
  no entra y **nadie se entera**: el fallo aparece más tarde y en otro sitio, disfrazado de
  violación de clave foránea en una tabla hija. Engañó tres veces el 2026-08-17, con dos
  causas distintas bajo el mismo síntoma — columnas `NOT NULL` que el `INSERT` omitía
  (`created_date`, `enabled`), y una colisión con el índice único `countries.name` porque la
  migración `022_seed_americas_geography.sql` **ya siembra Colombia** y el seed intentaba
  meter otra con id fijo. Dejó a `SchemaSeed` aparentando sembrar geografía sin sembrar nada.
  Usa un `INSERT` normal (que falla ruidosamente) o, si necesitas idempotencia dentro del
  contenedor compartido, `INSERT … ON DUPLICATE KEY UPDATE` y **datos que no compitan con lo
  que siembra Liquibase**.

## Anti-patterns — nunca hacer esto

- ❌ Crear capas horizontales compartidas top-level (`domain/`, `application/`, `infrastructure/` fuera de una feature)
- ❌ Declarar un `PageResult` (o `PageResponse`, `PagedResult`, `Slice`…) dentro de una feature — el contrato de paginación es único y vive en `shared/pagination`; la regla `PAGINACION_CON_UN_SOLO_CONTRATO` rompe el build
- ❌ Llamar a `PageRequest.of(...)` fuera del kernel — sin acotar, `?pageSize=100000` devuelve la tabla entera; usa `Pages.request(...)`
- ❌ Copiar en el controller el bloque de cinco campos para convertir `PageResult` en `PageResponse` — es `PageResponse.from(result, this::toResponse)`
- ❌ Meter en `shared/` cualquier cosa que no cumpla las cuatro condiciones del criterio de admisión (ver "Qué puede entrar en `shared/`")
- ❌ Importar clases de dominio de otra feature (`employee` importando `company.domain.Company`)
- ❌ Usar UUID, String u otro tipo como ID de entidad (siempre `Long` autogenerado por la BD)
- ❌ Ports de entrada importando clases de `infrastructure` (rompe la dirección de dependencias)
- ❌ El controller llamando a `Animal.create(...)` directamente (lógica de dominio en infra)
- ❌ Pasar entidades de dominio (`Animal`) como parámetro de un use case (usar commands)
- ❌ Validaciones de negocio en el controller o en el service (van en el constructor de la entidad)
- ❌ `@Autowired` en campos (usar constructor injection)
- ❌ Credenciales hardcodeadas en `application.yml` (usar `${VAR:default}`)
- ❌ Excepciones sin sufijo `Exception` (`AnimalNotFound` en lugar de `AnimalNotFoundException`)
- ❌ Nombrar el adaptador de repositorio con el motor de BD (`MySqlAnimalRepository` en lugar de `JpaAnimalRepository`)
- ❌ Mezclar commands y DTOs en un mismo package (`model/` — usar `command/` y `dto/` separados)
- ❌ Usar `SearchXxxUseCase` para listar todos sin filtros (usar `ListXxxsUseCase`)
- ❌ Validar existencia de FK en el repositorio con `findById().orElseThrow()` — esa lógica va en el service via `YyyQueryPort` (o `YyyValidationPort` si solo validas)
- ❌ Importar la entidad de dominio de otra feature en el dominio propio (`submodule.domain.SubModule` con un `module.domain.Module` colgado) — usar companion VO `YyyRef`
- ❌ Importar Responses o DTOs de aplicación de otra feature (`SubModuleResponse` con `ModuleResponse` adentro) — definir un companion local (`ModuleSummary`)
- ❌ Disparar un efecto `@Async` (correo, notificación, transmisión) desde dentro de un método `@Transactional` — se entrega aunque la transacción revierta; difiérelo con `afterCommit` (ver "Efectos externos y transacciones")
- ❌ `@ManyToOne` cross-feature SIN `@EntityGraph` en `findAll`/`findById` — produce N+1
- ❌ En el mapper, leer `entity.getModule().getName()` después de `getReferenceById` en `save` — dispara una query de hidratación; reusa el `Ref` precargado vía el overload `toDomain(entity, ref)`

## How to add a new entity

Toda entidad nueva vive en su propio paquete feature `com.<company>.<project>.<feature>/`.

1. `<feature>/domain/` — `XxxId`, `Xxx` (con validaciones en constructor, ID como `Long`), `XxxNotFoundException`. **Por cada FK con datos a otra feature**: añadir `YyyRef` (companion VO).
2. `<feature>/application/command/` — `CreateXxxCommand`, `UpdateXxxCommand`
3. `<feature>/application/dto/` — `XxxDto` con `from(Xxx)`. **Opcional**: `YyySummaryDto` por cada companion VO si quieres separación estricta app↔domain.
4. `<feature>/application/port/in/` — una interfaz por caso de uso
5. `<feature>/application/port/out/` — `XxxRepository` (+ `YyyQueryPort` por cada FK con datos, o `YyyValidationPort` si solo validas existencia)
6. `<feature>/application/usecase/` — un service por caso de uso
7. `<feature>/infrastructure/persistence/` — `XxxJpaEntity` (con `@GeneratedValue(IDENTITY)`; `@ManyToOne(LAZY) YyyJpaEntity` por cada FK), `XxxJpaMapper` (con dos `toDomain` overloads cuando hay FKs), `XxxJpaRepository` (con `@EntityGraph` en `findAll`/`findById` cuando hay FKs), `JpaXxxRepository` (+ `JpaYyyQueryPort` por cada FK)
8. `<feature>/infrastructure/web/` — `XxxController`
9. `<feature>/infrastructure/web/request/` — `CreateXxxRequest`, `UpdateXxxRequest`
10. `<feature>/infrastructure/web/response/` — `XxxResponse` (+ `YyySummary` por cada FK con datos en la response)
11. `infrastructure/web/GlobalExceptionHandler` — añadir `XxxNotFoundException` al handler 404
12. `db/changelog/migrations/` — nuevo changeset Liquibase con `id BIGINT AUTO_INCREMENT PRIMARY KEY`; columnas booleanas con `type="BOOLEAN"` (nunca `TINYINT(1)` — ver "Columnas booleanas")
