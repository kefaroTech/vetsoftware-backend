package com.vetsoftware.app.appointment.application.command;

import java.time.LocalDateTime;
import java.util.Set;

/**
 * @param durationMinutes
 *            nueva duración. Es un cambio parcial (PATCH): {@code null}
 *            conserva la que la cita ya tenía.
 * @param forceOverlap
 *            decisión consciente de reprogramar aunque el veterinario ya tenga
 *            otra cita cruzada. Por defecto {@code false}: el cruce bloquea con
 *            409. Requiere el permiso {@code appointment.overlap.force}, que se
 *            comprueba en el {@code @PreAuthorize} del puerto.
 * @param visibleBranchIds
 *            sedes que el caller puede consultar, puestas por el controller con
 *            {@code authz.currentBranchIdsOrEmpty()}. No viene del request:
 *            solo sirve para decidir cuánto del cruce se le revela (ids y
 *            nombre del veterinario) y falla cerrado — vacío = no se revela
 *            ninguna cita.
 */
public record RescheduleAppointmentCommand(Long id, LocalDateTime startAt, Integer durationMinutes,
        Long employeeId, Long companyId, boolean forceOverlap, Set<Long> visibleBranchIds) {
}
