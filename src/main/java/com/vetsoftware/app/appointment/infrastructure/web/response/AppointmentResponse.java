package com.vetsoftware.app.appointment.infrastructure.web.response;

import com.vetsoftware.app.appointment.domain.AppointmentStatus;
import com.vetsoftware.app.appointment.domain.AppointmentType;
import java.time.LocalDateTime;
import java.util.List;

public record AppointmentResponse(
        Long id,
        LocalDateTime startAt,
        AppointmentType type,
        AppointmentStatus status,
        String notes,
        String cancellationReason,
        AnimalSummary animal,
        OwnerSummary owner,
        String clientName,
        String clientPhone,
        EmployeeSummary employee,
        long version,
        boolean enabled,
        LocalDateTime createdDate,
        List<Long> overlappingAppointmentIds
) {}
