package com.vetsoftware.app.appointment.application.command;

public record CancelAppointmentCommand(
        Long id,
        String reason,
        Long companyId
) {}
