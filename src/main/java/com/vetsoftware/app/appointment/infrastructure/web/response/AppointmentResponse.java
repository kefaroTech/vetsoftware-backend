package com.vetsoftware.app.appointment.infrastructure.web.response;

import io.swagger.v3.oas.annotations.media.Schema;

import com.vetsoftware.app.appointment.domain.AppointmentStatus;
import com.vetsoftware.app.appointment.domain.AppointmentType;
import java.time.LocalDateTime;
import java.util.List;

public record AppointmentResponse(@Schema(requiredMode = Schema.RequiredMode.REQUIRED) Long id,
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED) LocalDateTime startAt,
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED) AppointmentType type,
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED) AppointmentStatus status, String notes,
        String cancellationReason, AnimalSummary animal, OwnerSummary owner, String clientName,
        String clientPhone, String clientEmail,
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED) EmployeeSummary employee,
        BranchSummary branch, @Schema(requiredMode = Schema.RequiredMode.REQUIRED) long version,
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED) boolean enabled,
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED) LocalDateTime createdDate,
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED) List<Long> overlappingAppointmentIds) {
}
