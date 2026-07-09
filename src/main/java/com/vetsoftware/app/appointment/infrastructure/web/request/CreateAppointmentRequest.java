package com.vetsoftware.app.appointment.infrastructure.web.request;

import com.vetsoftware.app.appointment.domain.AppointmentType;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.time.LocalDateTime;

public record CreateAppointmentRequest(
        @NotNull LocalDateTime startAt,
        @NotNull AppointmentType type,
        @NotNull Long employeeId,
        Long animalId,
        Long ownerId,
        @Size(max = 120) String clientName,
        @Size(max = 30) String clientPhone,
        @Size(max = 1000) String notes
) {}
