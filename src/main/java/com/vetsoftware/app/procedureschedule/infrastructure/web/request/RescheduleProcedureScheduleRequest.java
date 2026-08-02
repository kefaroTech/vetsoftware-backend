package com.vetsoftware.app.procedureschedule.infrastructure.web.request;

import jakarta.validation.constraints.NotNull;
import java.time.LocalDateTime;

public record RescheduleProcedureScheduleRequest(@NotNull LocalDateTime newDateTime, String mode) {
}
