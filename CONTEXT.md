# VetSoftware — Estado del proyecto

> Snapshot al 2026-05-24. Este archivo complementa al `CLAUDE.md` (que describe arquitectura y convenciones) con el estado **actual** del proyecto: qué features existen, qué falta, qué se ha hecho recientemente. Mantenerlo al día tras hitos.

## Stack y rol

Backend único del sistema VetSoftware. Spring Boot 3 + Java + MySQL + Redis. Arquitectura hexagonal + vertical slicing. Sirve en `http://localhost:8080/api/v1`. Repos hermanos:

- `../VetSoftwarePublicFront/` — Front del empleado de clínica (vet, recepcionista). Vue 3 + TS + Vuetify.
- `../VetSoftwareFront/` — Front del admin de plataforma. Vue 3 + TS + Vuetify + Pinia.

## Git state

- **Branch**: `master`
- **HEAD**: `84d7b5e` (2026-05-18) — `feat(clinicalhistory): historial clinico unificado por animal via vista`
- **Working tree limpio**
- **Sin CI configurado** (no hay `.github/`). Sólo Husky local + commitlint.

## Mapa de features

44 controllers REST. Agrupados por dominio:

### Auth e identidad
- `AuthController` — `POST /auth/login/employee`, `POST /auth/login/system`, `GET /auth/me`
- `RegistrationController` — `POST /register` (autoservicio empresa+admin+JWT)
- `EmployeeController` — CRUD + `GET /by-company`
- `SystemUserController` — CRUD
- `EmployeeRoleController` — CRUD (create/list/get/update/delete)
- `RoleController` — CRUD + `GET /by-company`
- `RolePermissionController` — CRUD + `GET /by-company` + `PUT /by-role/{roleId}` (sync atómico)
- `PermissionController` — CRUD + `GET /by-company`
- `SystemPermissionController`, `SystemUserPermissionController` — CRUD
- `BasePermissionController`, `BaseRoleController`, `BaseRolePermissionController` — CRUD (plantillas por membership)
- `AdminPermissionPublishController` — `POST /admin/admin-permissions/publish`

### Tenant / membresía
- `CompanyController`, `MembershipController`, `MembershipSubModuleController` — CRUD
- `ModuleController`, `SubModuleController` — CRUD

### Catálogos geográficos
- `CountryController`, `StateController`, `CityController` — CRUD + lookups jerárquicos

### Catálogos clínicos generales
- `SpecieController`, `BreedController`, `AnimalColorController` — CRUD
- `ConsultationTypeController` — CRUD

### Catálogos de tipo (company-scoped + flag `general`)
Todos con CRUD + `GET /available` (excepto consultation y spa):
- `VaccinationTypeController`, `LaboratoryTestTypeController`, `SurgeryTypeController`, `DiagnosticImagingTypeController`, `SpaTypeController`

### Operativo-clínico (scoped a `companyId` vía `Authz`)
- `OwnerController` — CRUD + `GET /search?q=`
- `AnimalController` — CRUD + `GET /by-owner/{id}`
- `ConsultationController` — CRUD
- `PrescriptionController`, `MedicamentPrescriptionController` — CRUD
- `VaccinationController`, `HospitalizationController`, `DewormingController` — CRUD + `GET /by-animal/{id}`
- `LaboratoryTestController`, `SurgeryController`, `DiagnosticImagingController` — CRUD + `/by-animal/{id}` + `PATCH /{id}/status`
- `SpaController` — CRUD
- **`ClinicalHistoryController`** — `GET /animals/{animalId}/clinical-history?types&from&to` (timeline unificado vía vista SQL `clinical_event_view`)

## Multi-tenancy

`Authz.currentCompanyId()` extrae el `companyId` del `EmployeeContext` (poblado por `AuthFilter` desde el JWT). Controllers scoped llaman a este método; el front nunca lo envía en path/query.

JWT claims: `sub`, `type` (`EMPLOYEE | SYSTEM_USER`), `companyId` (sólo EMPLOYEE), `iat`, `exp`. Permisos cargados como `GrantedAuthority`; `SYSTEM_USER` además recibe `ROLE_SYSTEM`.

## Migraciones recientes (Liquibase)

Path: `src/main/resources/db/changelog/migrations/`. Total: 64 changesets. Las últimas 10:

| # | Propósito |
|---|---|
| 063 | Vista `clinical_event_view` unificando 7 módulos clínicos |
| 062 | `status` en `diagnostic_imagings` |
| 061 | `status` en `surgeries` |
| 060 | `status` en `laboratory_tests` |
| 059 | Rename `test_types` → `laboratory_test_types` |
| 058 | `diagnostic_imaging_types`: company + flag general |
| 057 | `surgery_types`: company + flag general |
| 056 | `test_types`: company + flag general |
| 055 | `vaccination_types`: company + flag general |
| 054 | Crear `diagnostic_imagings` |

## Cambios desde 2026-05-10

9 commits relevantes:
1. **Clinical history unificada** (`84d7b5e`): feature `clinicalhistory/`, vista SQL `clinical_event_view`.
2. **Status en procedimientos** (`33d437c` + 060/061/062): `PATCH /{id}/status` en labs, cirugías, imágenes.
3. **List-by-animal en 6 módulos clínicos** (`c6b7028`).
4. **Permisos granulares** (`ef3fbad`): `@PreAuthorize` con `xxx.update` / `xxx.delete` separados.
5. **`/auth/me` + cache invalidation** (`afa0832`): nuevo `GetCurrentUserUseCase`, `CacheConfig`.
6. **Sync por rol + password BCrypt** (`6c2ca8d`): `PUT /role-permissions/by-role/{id}`, hash de password de empleado.
7. **Roles company-scoped** (`d36ce35`, `379f721`): `/roles/by-company`, `/permissions/by-company`, permisos embebidos.
8. **Publish admin permissions** (`e0cff09`): feature `publishadminpermissions/`.
9. **`/employees/by-company` con roles** (`ee0f248`).

## Gaps abiertos

Endpoints que el front necesita pero **no existen**:

- `POST /employees/{id}/reset-password` y `/change-password`
- `POST /employees/{id}/activate` y `/deactivate` (el cambio se hace vía PUT completo)
- `GET /employees/search?q=` (sí existe `/owners/search`)
- `/auth/me` o `/system-users/me` para `SYSTEM_USER` (sólo existe para EMPLOYEE)

Cuando se implementen, ver `../VetSoftwarePublicFront/CONTEXT.md` y el front se simplifica.

## Tests y CI

- Sólo 2 clases en `src/test/`: `PublishAdminPermissionsServiceTest`, `GetClinicalHistoryServiceTest`. Cobertura casi nula.
- Sin GitHub Actions u otro CI. Husky + commitlint sólo.

## Archivos clave

- `CLAUDE.md` — guía arquitectural completa (hexagonal + vertical slicing + cross-feature pattern).
- `src/main/java/.../auth/infrastructure/security/{JwtProvider,Authz}.java` y `filter/AuthFilter.java` — núcleo de seguridad.
- `src/main/resources/db/changelog/migrations/` — historia DB.
- `src/main/java/.../clinicalhistory/` — feature más reciente (timeline).
