package com.vetsoftware.app.medicationschedule.infrastructure.web.response;

import com.vetsoftware.app.medicationschedule.domain.CascadeSkipReason;
import io.swagger.v3.oas.annotations.media.Schema;
import java.util.List;

/**
 * Respuesta de la reprogramacion. Envuelve el plan porque el desenlace de la
 * cascada describe la operacion entera, no cada toma: colgarlo de cada elemento
 * de la lista habria contaminado tambien a generate, list, apply y
 * suspend-pending, que comparten {@link MedicationScheduleResponse}.
 */
public record RescheduleMedicationScheduleResponse(
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED, description = "Plan completo de la medicacion, ordenado por hora vigente") List<MedicationScheduleResponse> schedules,
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED, description = "true si se pidio cascada y se aplico a las tomas siguientes") boolean cascadeApplied,
        @Schema(description = "Por que la cascada pedida no se aplico; nulo si no se pidio o si se aplico") CascadeSkipReason cascadeSkippedReason) {
}
