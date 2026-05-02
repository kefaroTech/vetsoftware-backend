# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Commands

```bash
mvn clean package          # build
mvn spring-boot:run        # run
mvn test                   # all tests
mvn test -Dtest=ClassName  # single test class
mvn clean verify sonar:sonar -Dsonar.login=<token>   # análisis de código (SonarQube 9.9 LTS en localhost:9000)
```

## Configuration

MySQL en `src/main/resources/application.yml`:
- DB: `<project>_db` at `localhost:3306`
- `ddl-auto: update` — Hibernate gestiona el schema automáticamente
- Credenciales deben usar variables de entorno: `${DB_USERNAME:root}`

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
└── infrastructure/
    └── web/
        └── GlobalExceptionHandler.java   ← único archivo compartido entre features
```

### Regla de vertical slicing — nunca romper esto

- ❌ No crear capas horizontales top-level (`domain/`, `application/`, `infrastructure/` compartidas)
- ❌ No importar clases de dominio de otra feature (p.ej. `company.domain.Company` desde `employee`)
- ❌ No importar DTOs de aplicación ni Responses de otra feature
- ✅ Si dos features necesitan comunicarse, usar IDs primitivos (`Long companyId`), un companion VO propio (`XxxRef`), o un puerto explícito
- ✅ Excepción acotada: `<feature>/infrastructure/persistence/` puede importar `otraFeature.infrastructure.persistence.XxxJpaEntity` y `XxxJpaRepository` para asociaciones JPA (`@ManyToOne`) — ver sección "Cross-feature references"

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

- Tests de capa application: **sin Spring context, sin Mockito** — stubs manuales inline.
- Nombre de métodos de test en `snake_case` describiendo el escenario.
- Un test class por service (`CreateAnimalServiceTest`, `FindAnimalServiceTest`, etc.).

```java
// Patrón de stub manual
private final AnimalRepository repository = new AnimalRepository() {
    Animal saved;
    public void save(Animal a)                        { saved = a; }
    public Optional<Animal> findById(AnimalId id)     { return Optional.ofNullable(saved); }
    public List<Animal> findAll()                     { return List.of(); }
    public void delete(AnimalId id)                   {}
};
```

## Anti-patterns — nunca hacer esto

- ❌ Crear capas horizontales compartidas top-level (`domain/`, `application/`, `infrastructure/` fuera de una feature)
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
12. `db/changelog/migrations/` — nuevo changeset Liquibase con `id BIGINT AUTO_INCREMENT PRIMARY KEY`
