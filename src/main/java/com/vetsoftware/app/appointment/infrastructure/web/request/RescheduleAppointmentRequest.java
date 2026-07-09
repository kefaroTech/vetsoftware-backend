package com.vetsoftware.app.appointment.infrastructure.web.request;

import jakarta.validation.constraints.NotNull;
import java.time.LocalDateTime;

public record RescheduleAppointmentRequest(
        @NotNull LocalDateTime startAt,
        @NotNull Long employeeId
) {}
