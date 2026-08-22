package com.vetsoftware.app.procedureschedule.infrastructure.web.request;

import jakarta.validation.constraints.NotNull;
import java.time.LocalDateTime;

public record RescheduleProcedureScheduleRequest(
        @NotNull(message = "La nueva fecha y hora es obligatoria.") LocalDateTime newDateTime,
        String mode) {
}
