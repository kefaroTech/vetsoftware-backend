package com.vetsoftware.app.appointment.infrastructure.web.response;

import io.swagger.v3.oas.annotations.media.Schema;

import com.vetsoftware.app.appointment.domain.AppointmentStatus;
import com.vetsoftware.app.appointment.domain.AppointmentType;
import java.time.LocalDateTime;
import java.util.List;

public record AppointmentResponse(@Schema(requiredMode = Schema.RequiredMode.REQUIRED) Long id,
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED) LocalDateTime startAt,
        @Schema(description = "Duración propia de la cita en minutos. null = hereda la duración"
                + " por defecto de la empresa (ajuste appointment.default_duration_minutes; 30"
                + " si no está configurado). El fin es derivado: startAt + duración.", example = "30") Integer durationMinutes,
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED) AppointmentType type,
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED) AppointmentStatus status, String notes,
        String cancellationReason, AnimalSummary animal, AppointmentOwnerSummary owner,
        String clientName, String clientPhone, String clientEmail,
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED) AppointmentEmployeeSummary employee,
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED) AppointmentBranchSummary branch,
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED) long version,
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED) boolean enabled,
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED) LocalDateTime createdDate,
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED, description = "Citas del mismo veterinario con las que esta se cruza. Desde BE-17"
                + " el cruce bloquea con 409 APPOINTMENT_OVERLAP, así que este array solo"
                + " llega no vacío cuando la operación se guardó con forceOverlap=true."
                + " Incluye únicamente las citas de las sedes que el usuario tiene"
                + " asignadas: los cruces en otras sedes bloquean o se fuerzan igual,"
                + " pero no se identifican. Nunca es null.") List<Long> overlappingAppointmentIds) {
}
