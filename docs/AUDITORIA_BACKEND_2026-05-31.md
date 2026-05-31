# Auditoría Integral del Backend — VetSoftware

- **Fecha:** 2026-05-31
- **Alcance:** `VetSoftware/` (Spring Boot 3, Java 21, arquitectura hexagonal + vertical slicing, MySQL, Redis, S3/LocalStack, Gotenberg, stack de observabilidad Grafana/Loki/Prometheus/Zipkin).
- **Dimensiones:** Seguridad · Fugas de memoria y recursos · Conexiones y agotamiento · Logs y observabilidad.
- **Metodología:** lectura directa del código (no solo grep) por áreas, en paralelo. Cada hallazgo incluye severidad, evidencia (`archivo:línea`), por qué es riesgo y recomendación concreta.

> ⚠️ **Titular ejecutivo:** la mecánica de logging/observabilidad está en nivel **clase mundial**, y la gestión de recursos en memoria es **buena** (sin fugas permanentes). El riesgo dominante es de **seguridad multi-tenant (IDOR)**: varios endpoints permiten leer/editar/borrar/descargar datos clínicos de **otras empresas** porque la autorización valida *permiso* pero no *ownership*. En segundo lugar, **ninguna capacidad está dimensionada** (pools de DB/Redis/Tomcat/S3 por defecto) y hay una transacción de DB que abarca I/O a S3, lo que crea una cadena de agotamiento bajo carga.

## Resumen por severidad

| Severidad | Seguridad | Memoria/Recursos | Conexiones/Agotamiento |
|---|---|---|---|
| 🔴 Crítico | C1–C4 (IDOR multi-tenant) | — | C1 (tx DB + S3), C2 (Hikari default) |
| 🟠 Alto | A1 (JWT secret default), A2 (actuator público), A3 (upload sin validar) | — | A1 (Tomcat threads), A2 (S3 sin timeouts), A3 (Redis sin timeouts), A4 (listados sin paginar) |
| 🟡 Medio | M1 (JWT sin iss/aud/revocación), M2 (CORS), M3 (rutas públicas), M4 (`@PreAuthorize` faltantes), M5 (SpEL frágil) | 1 (archivos en `byte[]`) | M1 (descarga sin streaming), M2 (Gotenberg sin pool) |
| 🟢 Bajo | B1–B3 | 2 (PDF en `byte[]`), 3 (`findAll`) | B1 (tx readOnly) |

---

# 1. Seguridad

## 🔴 CRÍTICO

### C1 — IDOR sistémico en lecturas por ID (fuga de datos cross-tenant)
**Evidencia:**
- `owner/application/port/in/FindOwnerUseCase.java:7` → `@PreAuthorize("hasAuthority('admin.all') or hasRole('SYSTEM')")`; `OwnerController.findById` llama `findUseCase.findById(id)` con `id` arbitrario del path; `FindOwnerService` hace `repository.findById(id)` **sin filtrar por company**.
- `laboratorytest/application/port/in/FindLaboratoryTestUseCase.java:7` → `hasAuthority('laboratoryTest.read')`, sin `isMyCompany`; el service no filtra company.
- `laboratorytest/application/port/in/ListLaboratoryTestsByAnimalUseCase.java:8` → recibe `animalId` del path sin verificar que el animal sea de mi company.

**Por qué es riesgo:** los IDs son `BIGINT AUTO_INCREMENT` (secuenciales). Un empleado de la Company A con el permiso de lectura puede enumerar IDs y leer recursos clínicos de cualquier otra empresa. El `@PreAuthorize` valida el *permiso* pero no el *ownership*, y el ID no porta el `companyId`.

**Recomendación:** en cada caso de uso scoped, derivar `companyId` del `AuthContext` y filtrar en la query (`findByIdAndCompanyId`) o validar el `companyId` de la entidad cargada contra `authz.currentCompanyId()`, devolviendo **404** si no coincide.

### C2 — IDOR en UPDATE/DELETE + el `companyId` lo elige el cliente
**Evidencia:**
- `owner/application/port/in/UpdateOwnerUseCase.java:8` → `@PreAuthorize("hasAuthority('admin.all') or (hasAuthority('owner.update'))")` **sin `@authz.isMyCompany(#command.companyId)`**.
- `OwnerController.java:77-80`: construye el command con `request.companyId()` (valor del cliente), no con `authz.currentCompanyId()`.
- `UpdateOwnerService.java:34-42`: carga por `id` sin verificar company y reasigna a la `CompanyRef` que venga en el command.
- Mismo patrón en `laboratorytest`: `UpdateLaboratoryTestUseCase.java:8` + `LaboratoryTestController.java:127-134`.
- `DeleteOwnerUseCase.java:6` → `owner.delete` con `id` crudo, sin company.

**Por qué es riesgo:** un empleado de Company A con `owner.update` puede (a) editar el recurso de Company B y (b) **reasignarlo** a otra company arbitraria pasando `companyId` en el body (toma de control / corrupción cross-tenant). El DELETE permite borrar recursos ajenos. Contradice directamente la regla de `CLAUDE.md`.

**Recomendación:** (1) quitar `companyId` de los requests scoped (C3); (2) inyectar `authz.currentCompanyId()` en el controller; (3) añadir `and @authz.isMyCompany(#command.companyId)` a **todos** los Update; (4) en Update/Delete verificar `entity.company == currentCompanyId` antes de mutar/borrar.

### C3 — Requests scoped que aceptan `companyId` del cliente (~35 requests)
**Evidencia:** `CreateAnimalRequest.java:29`, `UpdateAnimalRequest.java:29`, `CreateLaboratoryTestRequest.java:19`, `UpdateLaboratoryTestRequest.java:18`, `UpdateOwnerRequest.java:15`, `CreateEmployeeRequest.java:13`, y análogos en hospitalization, vaccination, deworming, prescription, spa, surgery, daycare, diagnosticimaging, consultation, role, permission…

**Matiz:** en CREATE algunos controllers (Animal, Owner) **ignoran** el `request.companyId()` y usan `authz.currentCompanyId()` — pero el campo sigue presente (confuso/peligroso). En UPDATE (Animal, Owner, LaboratoryTest) **sí** se usa el del cliente (C2). `CreateLaboratoryTestUseCase.java:8` además **no** valida `isMyCompany` → permite crear en otra company.

**Recomendación:** eliminar `companyId` de todos los requests scoped; derivarlo siempre del contexto; defensa en profundidad con `isMyCompany` en el port.

### C4 — IDOR en archivos de laboratorio (descarga/borrado cross-tenant)
**Evidencia:**
- `laboratorytestfile/application/port/in/DownloadLaboratoryTestFileUseCase.java:7` → `hasAuthority('laboratoryTest.read')` con `id` crudo.
- `DownloadLaboratoryTestFileService.java:25-29`: `repository.findById(id)` → `fileStoragePort.retrieve(file.getStorageKey())` **sin verificar company**. La storage key empieza por `{companyId}/...` (`StorageKeyFactory.java:16`), confirmando que es multi-tenant.
- Igual en `Find`/`List`/`Delete` de laboratory-test-files.

**Por qué es riesgo:** un empleado con `laboratoryTest.read` puede descargar (`GET /laboratory-test-files/{id}/download`) o borrar resultados de laboratorio (datos clínicos sensibles) de cualquier empresa enumerando IDs.

**Recomendación:** derivar el `companyId` del archivo y compararlo con `authz.currentCompanyId()`; 404 si no coincide.

## 🟠 ALTO

### A1 — JWT secret por defecto hardcodeado en el repo
**Evidencia:** `application.yml:43` → `secret: ${JWT_SECRET:c2VjcmV0...}`. Override-able por env (bien), pero el default es un secreto **conocido** (está en el repo). `JwtProvider.java:23` lo usa con `Keys.hmacShaKeyFor(...)` (HMAC-SHA, algoritmo correcto).

**Por qué es riesgo:** si `JWT_SECRET` no se define en algún entorno, cualquiera con acceso al repo puede **forjar tokens válidos** para cualquier `id`/`type`/`companyId` y suplantar a cualquier empleado o `SYSTEM_USER`. Combinado con C1–C4 = compromiso total.

**Recomendación:** eliminar el valor por defecto; que la app **falle al arrancar** si falta `JWT_SECRET`. Mismo criterio para `DB_PASSWORD` (`application.yml:24`, default `cronos2026*`).

### A2 — Actuator como ruta pública con `show-details: always`
**Evidencia:** `AuthFilter.java:74` → `new PublicRoute(null, "/actuator/**")`. `application.yml:50-53`: `exposure.include: health,info,metrics,prometheus` + `health.show-details: always`. `SecurityConfig.java:27` `anyRequest().permitAll()`.

**Por qué es riesgo:** `/actuator/health` filtra estado interno (DB, Redis, disco) sin auth; `/actuator/prometheus` y `/metrics` exponen métricas (URIs, throughput, nombres de beans) a internet. Reconocimiento para un atacante.

**Recomendación:** sacar `/actuator/**` de `PUBLIC_PATHS` (o exponer solo `/actuator/health` con `when-authorized`/`never`); proteger `prometheus`/`metrics` con auth o moverlos a puerto/red interna (`management.server.port`).

### A3 — Subida de archivos sin validación de tipo/tamaño (content-type confiado)
**Evidencia:** `LaboratoryTestFileController.java:53-71`: acepta cualquier `MultipartFile`, usa `file.getContentType()` (controlado por el cliente) y lo persiste en S3 (`S3StorageClient.java:32`). Sin allow-list ni validación de magic bytes. Límite global 25 MB (`application.yml:19`).

**Por qué es riesgo:** se puede subir HTML/SVG/JS con `Content-Type: text/html`. En descarga, `MediaType.parseMediaType(dto.contentType())` (`controller:91`) reusa ese tipo → si el front lo sirve inline o el bucket se expone, habilita XSS almacenado. También permite subir malware como "resultado de laboratorio".

**Recomendación:** allow-list de content-types (PDF/imágenes), validar magic bytes en servidor, forzar `Content-Disposition: attachment` (ya presente) y `X-Content-Type-Options: nosniff`.

## 🟡 MEDIO

- **M1 — JWT sin `iss`/`aud` ni revocación.** `JwtProvider.parseClaims` (`50-56`) solo verifica firma + expiración (30 min). Un token robado es válido hasta expirar, sin posibilidad de invalidar. → Añadir claims `iss`/`aud` y validarlos; lista de revocación en Redis para logout/baneo.
- **M2 — CORS con `allowedHeaders("*")` + `allowCredentials(true)`.** `CorsConfig.java:24-26`. No usa wildcard de origen (correcto), pero depende de configurar bien `CORS_ALLOWED_ORIGINS` en prod. → Orígenes explícitos en prod, acotar headers.
- **M3 — Toda la autZ depende del filtro + `@PreAuthorize`.** `SecurityConfig` usa `permitAll()`; un endpoint nuevo cuyo port olvide `@PreAuthorize` queda accesible a cualquier autenticado. → Test/ArchUnit que falle si un método de `application/port/in` no tiene `@PreAuthorize` (salvo allow-list pública).
- **M4 — Verificar cobertura de `@PreAuthorize`.** `RegisterUserUseCase` sin anotación es correcto (público). Falta una garantía automatizada de cobertura.
- **M5 — SpEL `@PreAuthorize` frágil.** Mezcla `#command.companyId`/`#query.companyId`/`#companyId`; un parámetro inexistente resuelve a null → `isMyCompany(null)` siempre falso. `Authz.isMyCompany` es fail-closed ante null (bien). → Tests de autorización por endpoint.

## 🟢 BAJO
- **B1 — `SeededPasswordRehasher.java:30,35`** concatena nombres de tabla (literales internos, **no** explotable; datos sí parametrizados). Cosmético: usar constante/enum.
- **B2 — BCrypt cost por defecto (10)** en `BCryptPasswordHasher.java:9`. Correcto; considerar 12.
- **B3 — `RegisterUserRequest.java:16`**: `@Size(min=8)` sin política de complejidad.

---

# 2. Fugas de memoria y gestión de recursos

> **Veredicto:** sin fugas permanentes. Los hallazgos son de **presión de heap por request** (buffering de archivos completos), no de fugas que crezcan en el tiempo.

### 🟡 (Medio) Archivos cargados completos en `byte[]` en toda la cadena
**Evidencia:** `LaboratoryTestFileController.java:62` (`file.getBytes()`), `:93` (`new ByteArrayResource(dto.content())`), `DownloadLaboratoryTestFileService.java:28` (`byte[] content = fileStoragePort.retrieve(...)`), `S3StorageClient.java:42-49` (`getObjectAsBytes().asByteArray()` / `RequestBody.fromBytes`). Límite multipart 25 MB.

**Por qué es riesgo:** cada subida/descarga materializa el archivo entero en heap (hasta 25 MB) + copias intermedias (`getBytes()` duplica, `ByteArrayResource` referencia otra vez). N operaciones concurrentes = N×25 MB pico → riesgo de OOM bajo carga. No es fuga (se libera tras la request).

**Recomendación:** streaming. Descarga: `s3Client.getObject(...)` (`ResponseInputStream`) → `InputStreamResource`/`StreamingResponseBody` con try-with-resources. Subida: `RequestBody.fromInputStream(stream, size)` en vez de `getBytes()`.

### 🟢 (Bajo) PDF de historia clínica como `byte[]`
**Evidencia:** `GotenbergClient.java:43-49` (`.body(byte[].class)`), `HtmlPdfRenderer.java:24`, `ClinicalHistoryGotenbergAdapter.java:30-41`. → Si los reportes pueden ser grandes, considerar stream; si son pequeños, aceptable.

### 🟢 (Bajo/informativo) `findAll()` sin cota
**Evidencia:** `JpaLaboratoryTestRepository.java:76-83` y el patrón en muchos `ListXxxsService`. Para catálogos pequeños es correcto; para entidades de crecimiento ilimitado, ver **§3 A4**.

### ✅ Verificado sin hallazgos
- **Rate limiter de login:** migrado a Redis (`LoginRateLimitFilter` + `RateLimitConfig`), **sin** `ConcurrentHashMap` en memoria; buckets con TTL (`basedOnTimeForRefillingBucketUpToMax(1 min)`). Sin fuga.
- **Caché Redis** (`CacheConfig`): `entryTtl(5 min)` + `disableCachingNullValues()`; keyspace acotado por key + `@CacheEvict`. Sin caché in-memory sin cota.
- **Colecciones static:** solo constantes inmutables (`List.of`/`Set.of`). No hay mapas mutables que crezcan por request/usuario.
- **Thread pools/async/scheduled:** **no existe** ningún `ExecutorService`, `@Async`, `@Scheduled`, `CompletableFuture` propio. Sin executores sin shutdown.
- **Recursos:** sin `InputStream`/`Files.readAllBytes` abiertos manualmente; S3/Gotenberg usan API de bytes del SDK/RestClient. Único try-with-resources (`TraceContextResetFilter`) correcto.
- **ThreadLocals/MDC/SecurityContext:** limpiados en `finally` (`AuthFilter`, `AuditFilter`, `TraceContextResetFilter`, `SystemAuthRunner`). Sin fuga en threads del pool.
- **Listeners:** solo `TransactionSynchronization` (`EmployeeRoleCacheAdapter.java:32`), que Spring desregistra al cerrar la tx.

---

# 3. Conexiones, pools y agotamiento

> **Veredicto:** **ninguna capacidad está dimensionada** (todo por defecto) y hay una transacción que abarca I/O a S3. Bajo carga, estos se combinan en una cadena de agotamiento.

## 🔴 CRÍTICO

### C1 — Transacción DB abierta durante I/O a S3
**Evidencia:** `DeleteLaboratoryTestFileService.java:24-33` — `@Transactional` que hace `repository.delete(id)` y luego `fileStoragePort.delete(file.getStorageKey())` (llamada de red a S3 **dentro** de la tx).

**Por qué es riesgo:** la conexión Hikari queda retenida durante toda la llamada a S3. Sin timeout en el S3Client (A2) y con pool default de 10 (C2), un S3 lento + borrados concurrentes agota el pool; las peticiones esperan 30s y fallan.

**Recomendación:** sacar la llamada S3 fuera del `@Transactional` (commit primero, borrar objeto después, tolerando huérfanos limpiados por un job) o usar `TransactionSynchronization.afterCommit`. Siempre con timeouts en S3.

### C2 — HikariCP totalmente por defecto
**Evidencia:** `application.yml:21-25` define solo `url/username/password/driver`. **Cero** `spring.datasource.hikari.*` en todo `resources/`.

**Por qué es riesgo:** defaults = pool 10, `connection-timeout 30s`, `max-lifetime 30 min`, **sin** `leak-detection-threshold`. Con el patrón de C1 el pool se agota rápido y no hay alerta de conexiones no devueltas.

**Recomendación:** fijar explícitamente `maximum-pool-size`, `connection-timeout`, `max-lifetime` (< `wait_timeout` de MySQL) y `leak-detection-threshold: 60000`.

## 🟠 ALTO

### A1 — Tomcat thread pool sin configurar
**Evidencia:** ningún `server.tomcat.threads.*` en los `application*.yml`. Default `max: 200`, `accept-count: 100`.

**Por qué es riesgo:** los endpoints de PDF (Gotenberg) y archivos (S3) ocupan un thread durante todo el I/O bloqueante; un upstream lento puede saturar los 200 threads. Además 200 threads vs pool DB de 10 está descompensado.

**Recomendación:** dimensionar `server.tomcat.threads.max`/`min-spare`/`accept-count` acorde al pool DB; considerar executores separados para PDF/descarga.

### A2 — S3Client sin timeouts ni límite de conexiones
**Evidencia:** `S3Config.java:21-35` solo configura región/credenciales/endpoint; **sin** `apiCallTimeout`/`apiCallAttemptTimeout` ni connect/socket timeout ni `maxConnections`. Operaciones en `S3StorageClient.java:26-66` sin cota temporal.

**Por qué es riesgo:** combinado con C1 y A1, un S3 lento bloquea threads y conexiones DB indefinidamente.

**Recomendación:** `ClientOverrideConfiguration` con `apiCallTimeout`/`apiCallAttemptTimeout` + `ApacheHttpClient.builder()` con `connectionTimeout`/`socketTimeout`/`maxConnections`.

### A3 — Redis/Lettuce sin timeout de comando ni pool
**Evidencia:** `application.yml:35-40` define solo `host/port` + `cache.type: redis`. Sin `spring.data.redis.timeout` ni `lettuce.pool.*`.

**Por qué es riesgo:** un Redis colgado bloquea sin cota a los hilos que esperan caché o rate-limit; como el rate-limit corre en el camino de login, degrada la autenticación.

**Recomendación:** fijar `spring.data.redis.timeout` (y `lettuce.pool.*` si se habilita commons-pool2).

### A4 — Listados sin paginación sobre tablas de crecimiento ilimitado
**Evidencia:** `ListAnimalsService.java:21`, `ListConsultationsService.java:21`, `ListVaccinationsService.java:21`, `ListDewormingsService.java:21`, y análogos en surgery, diagnosticimaging, hospitalization, daycare, spa, prescription, medicamentprescription — todos con `repository.findAll()`. (`laboratorytest` **sí** tiene el patrón paginado correcto: `JpaSpecificationExecutor` + `PageResponse` + `/search`.)

**Por qué es riesgo:** en multi-tenant sin filtro de company ni paginación, `GET /animals` materializa **toda** la tabla (de todas las empresas) en memoria y la mapea a DTOs → pico de heap, GC, conexión DB retenida durante el scan. Crece linealmente hasta OOM. (También cruza con el IDOR de §1: además de pesado, expone datos cross-tenant.)

**Recomendación:** migrar al patrón `PageResponse<T>` + `Pageable`/Specifications de `laboratorytest` (ver `backend_pagination_pattern.md`) y filtrar por `companyId` del `AuthContext`.

## 🟡 MEDIO
- **M1 — Descarga sin streaming** (mismo `byte[]` de §2). → streaming + límite de concurrencia para endpoints de archivo.
- **M2 — Gotenberg con `SimpleClientHttpRequestFactory`** (`PdfConfig.java:14-24`): tiene timeout 30s (bien) pero **sin pool** (conexión por request). 30s de read es largo: bajo concurrencia ocupa muchos threads. → request factory con pool (`HttpComponentsClientHttpRequestFactory`) y/o bajar el timeout.

## 🟢 BAJO
- **B1 — Listados readonly sin `@Transactional(readOnly=true)`** (`ListAnimalsService`, etc.). Mejora menor (sin dirty-checking, posible enrutado a réplica).

## ✅ Bien resuelto
- **Multipart acotado** (`max-file-size`/`max-request-size` = 25 MB).
- **N+1 controlado:** todas las `@ManyToOne(LAZY)` cross-feature usan `@EntityGraph` en `findAll`/`findById` de forma consistente — no se encontró ninguna sin él.
- **`RateLimitConfig` bien gestionado:** `RedisClient`/`StatefulRedisConnection` con `destroyMethod` correcto, conexión singleton compartida, buckets con TTL.
- **Gotenberg con timeout** (a diferencia de S3).

---

# 4. Logs y observabilidad

> **Veredicto:** **clase mundial**. Esta área se auditó iterativamente y se endureció. Estado actual:

**Bien (consolidado y verificado en runtime):**
- JSON estructurado (`LogstashEncoder`), consola **profile-aware** (dev legible / prod JSON), timestamps **UTC**.
- Rotación por fecha **y** tamaño con tope total (`SizeAndTimeBasedRollingPolicy`, 100 MB / 1 GB cap / `.gz`).
- Severidad RFC 5424 correcta y sin duplicar (cliente→WARN, servidor→ERROR; `DataIntegrityViolation` en WARN).
- MDC rico: `actor.*` + `client.ip` + `user_agent.original` + `http.method`/`http.path` (logs de error autocontenidos), limpiados en `finally`.
- IP de origen **no falsificable** (`forward-headers-strategy: native` + `getRemoteAddr()`).
- Flujo de **auditoría** (logger `AUDIT`): `login_success/failure`, `login_rate_limited`, `unauthenticated`, `access_denied`, `http_mutation` — con IP+UA+traceId. Validado en ejecución.
- Auditoría con **retención separada** (`audit.log` 365 días) + appender `SYSLOG_AUDIT` listo para off-host (tamper-resistance).
- Labels de Loki sanos (`traceId`/`spanId` como campos, no labels; `level`/`logger` como labels).
- Sampling **tail-based** en el otel-collector (errores + lentas + 10% del resto) con `memory_limiter` + `batch`.
- Sin fugas de credenciales, inmune a log-injection (JSON), sin `printStackTrace`/`System.out`.

**Mejorable (bajo / opcional):**
- **`AsyncAppender`** pendiente — único ítem de mecánica (sacar el I/O de log del hilo de request con `neverBlock=true`).
- **Doble WARN en login fallido**: handler (`"Unauthorized…"`) + evento `AUDIT login_failure`. Defendible (ops vs auditoría); opcional bajar el del handler a DEBUG.
- **Tamper-resistance off-host real** (SIEM/WORM) — template listo, falta host del SIEM.
- **Trazas en Zipkin** separadas de Grafana → Tempo unificaría log↔traza↔métrica.

---

# 5. Plan de remediación priorizado

### P0 — Inmediato (seguridad crítica)
1. **Cerrar el IDOR multi-tenant (C1–C4 seguridad):** quitar `companyId` de todos los requests scoped; derivarlo de `authz.currentCompanyId()`; añadir `and @authz.isMyCompany(#command.companyId)` a todos los Update; filtrar por company en reads/deletes/listados y en descarga/borrado de archivos (404 si no es mi company).
2. **Eliminar defaults de secretos (A1 seguridad):** sin valor por defecto para `JWT_SECRET` ni `DB_PASSWORD`; fallar al arrancar si faltan.

### P1 — Alto (semana 1–2)
3. **Quitar `/actuator/**` de rutas públicas** o restringir a `/actuator/health` con `when-authorized`; mover `prometheus`/`metrics` a red/puerto interno (A2 seguridad).
4. **Validar uploads** (allow-list + magic bytes + `nosniff`) (A3 seguridad).
5. **Configurar HikariCP** (pool-size, timeouts, `leak-detection-threshold`) (C2 conexiones).
6. **Sacar el I/O de S3 de la transacción** (C1 conexiones).
7. **Timeouts en S3Client** (A2 conexiones).
8. **Paginar + filtrar por company los listados** de entidades transaccionales (A4 conexiones) — cruza con el IDOR.

### P2 — Medio (mes 1)
9. Streaming de subida/descarga de archivos (M1 memoria/conexiones).
10. Dimensionar Tomcat threads (A1 conexiones) y `spring.data.redis.timeout` (A3 conexiones).
11. Gotenberg con request factory con pool (M2 conexiones).
12. JWT `iss`/`aud` + revocación en Redis (M1 seguridad); hardening CORS (M2 seguridad).
13. Test/ArchUnit de cobertura de `@PreAuthorize` (M3/M4 seguridad).

### P3 — Bajo / madurez
14. `AsyncAppender`, `@Transactional(readOnly=true)` en listados, BCrypt cost 12, política de complejidad de password, tamper-resistance off-host del audit, migración a Tempo.

---

# 6. Apéndice — Archivos clave

| Tema | Archivos |
|---|---|
| AutZ multi-tenant | `auth/infrastructure/security/Authz.java`, `*/application/port/in/Update*UseCase.java`, `*/infrastructure/web/*Controller.java`, `*/infrastructure/web/request/*Request.java` |
| JWT / auth | `auth/infrastructure/security/JwtProvider.java`, `auth/infrastructure/filter/AuthFilter.java`, `auth/infrastructure/config/SecurityConfig.java` |
| Archivos / S3 | `laboratorytestfile/.../LaboratoryTestFileController.java`, `DownloadLaboratoryTestFileService.java`, `DeleteLaboratoryTestFileService.java`, `infrastructure/storage/S3Config.java`, `S3StorageClient.java`, `StorageKeyFactory.java` |
| Config / capacidad | `src/main/resources/application.yml` (+ `-dev`/`-prod`), `auth/infrastructure/config/RateLimitConfig.java`, `CacheConfig.java`, `infrastructure/pdf/PdfConfig.java` |
| Paginación (modelo a replicar) | `laboratorytest/infrastructure/persistence/LaboratoryTestJpaRepository.java` (+ `PageResponse`) |
| Logs / observabilidad | `infrastructure/web/{GlobalExceptionHandler,AuditFilter,TraceContextResetFilter}.java`, `infrastructure/audit/AuditLogger.java`, `infrastructure/logging/MdcKeys.java`, `resources/logback-spring.xml`, `docker/{promtail,otel-collector}.yml` |

---

*Auditoría realizada mediante lectura directa del código por dimensiones (seguridad, memoria/recursos, conexiones/agotamiento) y conocimiento consolidado del subsistema de logs. Los hallazgos de seguridad multi-tenant (C1–C4) deben verificarse y corregirse con prioridad sobre el resto.*
