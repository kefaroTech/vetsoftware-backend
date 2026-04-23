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
- ✅ Si dos features necesitan comunicarse, usar IDs primitivos (`Long companyId`) o un puerto explícito

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
- **FK a otra feature → outbound port de validación**: si una entidad tiene una FK a otra feature, el service valida la existencia a través de un outbound port (`YyyValidationPort` en `port/out/`), cuya implementación vive en `infrastructure/persistence/`. El repositorio usa `getReferenceById()` sin validar. Nunca lanzar `IllegalArgumentException` por FK no encontrada desde el repositorio.

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

## Naming conventions

| Elemento | Convención | Ejemplo |
|---|---|---|
| Entidad de dominio | `PascalCase` sustantivo | `Animal` |
| Value object | `PascalCase` + tipo | `AnimalId` |
| Excepción de dominio | `PascalCase` + `Exception` | `AnimalNotFoundException` |
| Puerto de entrada (CRUD) | verbo + entidad + `UseCase` | `CreateAnimalUseCase` |
| Puerto de entrada (lista) | `List` + entidad plural + `UseCase` | `ListAnimalsUseCase` |
| Método de lista | `listAll()` | — |
| Puerto de salida | entidad + `Repository` | `AnimalRepository` |
| Command | verbo + entidad + `Command` | `CreateAnimalCommand` |
| DTO de aplicación | entidad + `Dto` | `AnimalDto` |
| Service (CRUD) | verbo + entidad + `Service` | `CreateAnimalService` |
| Service (lista) | `List` + entidad plural + `Service` | `ListAnimalsService` |
| Entidad JPA | entidad + `JpaEntity` | `AnimalJpaEntity` |
| Mapper JPA | entidad + `JpaMapper` | `AnimalJpaMapper` |
| Spring Data repo | entidad + `JpaRepository` | `AnimalJpaRepository` |
| Adaptador repositorio | `Jpa` + entidad + `Repository` | `JpaAnimalRepository` |
| Controller REST | entidad + `Controller` | `AnimalController` |
| Request REST | verbo + entidad + `Request` | `CreateAnimalRequest` |
| Response REST | entidad + `Response` | `AnimalResponse` |

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
- ❌ Validar existencia de FK en el repositorio con `findById().orElseThrow()` — esa lógica va en el service via `YyyValidationPort`

## How to add a new entity

Toda entidad nueva vive en su propio paquete feature `com.<company>.<project>.<feature>/`.

1. `<feature>/domain/` — `XxxId`, `Xxx` (con validaciones en constructor, ID como `Long`), `XxxNotFoundException`
2. `<feature>/application/command/` — `CreateXxxCommand`, `UpdateXxxCommand`
3. `<feature>/application/dto/` — `XxxDto` con `from(Xxx)`
4. `<feature>/application/port/in/` — una interfaz por caso de uso
5. `<feature>/application/port/out/` — `XxxRepository` (+ `YyyValidationPort` por cada FK a otra feature)
6. `<feature>/application/usecase/` — un service por caso de uso
7. `<feature>/infrastructure/persistence/` — `XxxJpaEntity` (con `@GeneratedValue(IDENTITY)`), `XxxJpaMapper`, `XxxJpaRepository`, `JpaXxxRepository`
8. `<feature>/infrastructure/web/` — `XxxController`
9. `<feature>/infrastructure/web/request/` — `CreateXxxRequest`, `UpdateXxxRequest`
10. `<feature>/infrastructure/web/response/` — `XxxResponse`
11. `infrastructure/web/GlobalExceptionHandler` — añadir `XxxNotFoundException` al handler 404
12. `db/changelog/migrations/` — nuevo changeset Liquibase con `id BIGINT AUTO_INCREMENT PRIMARY KEY`
