package com.vetsoftware.app.appointment.application.command;

import com.vetsoftware.app.appointment.domain.AppointmentType;
import java.time.LocalDateTime;

public record CreateAppointmentCommand(LocalDateTime startAt, AppointmentType type, Long employeeId,
        Long animalId, Long ownerId, String clientName, String clientPhone, String clientEmail,
        String notes, Long branchId, Long companyId) {
}
