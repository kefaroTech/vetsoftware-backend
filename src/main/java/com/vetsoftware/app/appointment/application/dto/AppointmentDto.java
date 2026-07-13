package com.vetsoftware.app.appointment.application.dto;

import com.vetsoftware.app.appointment.domain.Appointment;
import com.vetsoftware.app.appointment.domain.AppointmentStatus;
import com.vetsoftware.app.appointment.domain.AppointmentType;
import java.time.LocalDateTime;
import java.util.List;

public record AppointmentDto(
        Long id,
        LocalDateTime startAt,
        AppointmentType type,
        AppointmentStatus status,
        String notes,
        String cancellationReason,
        AnimalSummaryDto animal,
        OwnerSummaryDto owner,
        String clientName,
        String clientPhone,
        String clientEmail,
        EmployeeSummaryDto employee,
        BranchSummaryDto branch,
        long version,
        boolean enabled,
        LocalDateTime createdDate,
        List<Long> overlappingAppointmentIds
) {
    public static AppointmentDto from(Appointment a) {
        return from(a, List.of());
    }

    public static AppointmentDto from(Appointment a, List<Long> overlaps) {
        return new AppointmentDto(
            a.getId(), a.getStartAt(), a.getType(), a.getStatus(),
            a.getNotes(), a.getCancellationReason(),
            a.getAnimal() == null ? null
                : new AnimalSummaryDto(a.getAnimal().id(), a.getAnimal().name(), a.getAnimal().code()),
            a.getOwner() == null ? null
                : new OwnerSummaryDto(a.getOwner().id(), a.getOwner().name()),
            a.getClientName(), a.getClientPhone(), a.getClientEmail(),
            new EmployeeSummaryDto(a.getEmployee().id(), a.getEmployee().name()),
            new BranchSummaryDto(a.getBranch().id(), a.getBranch().name(), a.getBranch().code()),
            a.getVersion(), a.isEnabled(), a.getCreatedDate(),
            overlaps == null ? List.of() : overlaps);
    }
}
