package com.vetsoftware.app.appointment.application.command;

import java.time.LocalDateTime;

public record RescheduleAppointmentCommand(
        Long id,
        LocalDateTime startAt,
        Long employeeId,
        Long companyId
) {}
