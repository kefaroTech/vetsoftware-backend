package com.vetsoftware.app.appointment.application.command;

import com.vetsoftware.app.appointment.domain.AppointmentType;
import java.time.LocalDateTime;
import java.util.Set;

/**
 * @param durationMinutes
 *            duración de la cita. Es un reemplazo completo (PUT): {@code null}
 *            devuelve la cita a la duración por defecto de la empresa.
 * @param forceOverlap
 *            decisión consciente de guardar aunque el veterinario ya tenga otra
 *            cita cruzada. Por defecto {@code false}: el cruce bloquea con 409.
 *            Requiere el permiso {@code appointment.overlap.force}, que se
 *            comprueba en el {@code @PreAuthorize} del puerto.
 * @param visibleBranchIds
 *            sedes que el caller puede consultar, puestas por el controller con
 *            {@code authz.currentBranchIdsOrEmpty()}. No viene del request:
 *            solo sirve para decidir cuánto del cruce se le revela (ids y
 *            nombre del veterinario) y falla cerrado — vacío = no se revela
 *            ninguna cita.
 */
public record UpdateAppointmentCommand(Long id, LocalDateTime startAt, Integer durationMinutes,
        AppointmentType type, Long employeeId, Long animalId, Long ownerId, String clientName,
        String clientPhone, String clientEmail, String notes, Long companyId, boolean forceOverlap,
        Set<Long> visibleBranchIds) {
}
