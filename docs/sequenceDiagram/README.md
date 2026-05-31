# Calendario de hospitalización (MAR) — Medication & Procedure Schedule

Diagramas de secuencia de los endpoints que pueblan y operan el calendario semanal
de administración (MAR) de `/dashboard/hospital`. Renderiza los `.puml` con cualquier
visor PlantUML.

## Contexto

El MAR ya **no** se calcula ni se opera en el cliente: el backend genera, persiste y
recalcula las tomas/ejecuciones en las tablas `medication_schedules` y
`procedure_schedules` (migraciones 084/085). Esta documentación cubre la capa de
aplicación + web que se añadió sobre el dominio y la persistencia ya existentes.

## Endpoints

| Método | Ruta | Para qué |
|---|---|---|
| `POST`  | `/medication-schedules/generate/{hospitalizationMedicationId}` | Calcula y persiste las tomas de un medicamento (idempotente) |
| `GET`   | `/medication-schedules/by-hospitalization/{hospitalizationId}` | Lista las tomas de todos los medicamentos del internamiento |
| `PATCH` | `/medication-schedules/{id}/apply` | Marca una toma como aplicada (hora real = ahora) |
| `PATCH` | `/medication-schedules/{id}/reschedule` | Reprograma una toma (`mode` = `one` \| `cascade`) |
| `POST`  | `/procedure-schedules/generate/{hospitalizationProcedureId}` | Calcula y persiste las ejecuciones de un procedimiento |
| `GET`   | `/procedure-schedules/by-hospitalization/{hospitalizationId}` | Lista las ejecuciones de todos los procedimientos del internamiento |
| `PATCH` | `/procedure-schedules/{id}/apply` | Marca una ejecución como aplicada |
| `PATCH` | `/procedure-schedules/{id}/reschedule` | Reprograma una ejecución (`mode` = `one` \| `cascade`) |

Autorización: `generate` → `hospitalization.create`; `by-hospitalization` →
`hospitalization.read`; `apply`/`reschedule` → `hospitalization.update`
(siempre con `admin.all` o rol `SYSTEM` como alternativa).

## Cómo se calcula (dominio puro: `{Medication|Procedure}ScheduleGenerator`)

A partir de `frequency` + `durationMeasure`/`durationQuantity` + `startDate`/`startTime`:

- `CONTINUOUS` → 0 tomas (infusión continua, no genera chips).
- `SINGLE` → 1 toma en la fecha/hora de inicio.
- `EVERY_4H/6H/8H/12H/24H` → intervalo en horas; nº de tomas:
  - `DOSES` → `durationQuantity`
  - `DAYS` → `durationQuantity * 24 / intervalo`
  - `INDEFINITE` / sin duración → horizonte acotado (14 días), tope de seguridad 90.
- Cada toma nace con `originalDateTime == currentDateTime`, `appliedStatus = PENDING`,
  `rescheduled = false`, `realDateTime = null`.

## Aplicar y reprogramar (pauta FIJO vs INTERVALO)

- **Aplicar** (`apply`): marca `appliedStatus = APPLIED` y `realDateTime = ahora`.
  - Pauta `INTERVAL`: recalcula las tomas **pendientes posteriores** sumando el intervalo
    desde la hora real de aplicación (`currentDateTime` se desplaza, `rescheduled = true`).
  - Pauta `FIXED`: no mueve las siguientes (se quedan en sus horas de reloj).
- **Reprogramar** (`reschedule`, drag&drop): mueve `currentDateTime` de la toma.
  - `mode = one`: solo esa toma.
  - `mode = cascade` (solo `INTERVAL`): recalcula las pendientes posteriores desde la
    nueva hora.
- Ambos endpoints devuelven el **plan completo** de esa orden (todas sus tomas), para que
  el front reemplace el calendario de un golpe.

## Flujo (orquestación)

1. El front crea/edita la orden (`POST/PUT /hospitalization-medications` o
   `/hospitalization-procedures`) y acto seguido llama a `…/generate/{id}`.
2. Al abrir el detalle del paciente, el front llama a los `…/by-hospitalization/{id}` y
   pinta el calendario con datos reales.
3. Click en un chip pendiente/atrasado → modal de confirmación → `…/{id}/apply`.
4. Drag&drop de un chip → modal según pauta → `…/{id}/reschedule` (`one`/`cascade`).

## Hexagonal

`infrastructure.web` → `application.port.in` (use case, `@PreAuthorize`) → `application.usecase`
(service `@Transactional`) → `application.port.out` (`*Repository`, `*QueryPort`) → `domain`
(entidad + generador puro). Los datos de la orden (frecuencia, pauta, duración) se leen vía
un `QueryPort` (companion VO `*OrderParams`) para no acoplar features (vertical slicing).
