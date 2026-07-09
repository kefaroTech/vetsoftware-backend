package com.vetsoftware.app.appointment.application.command;

import com.vetsoftware.app.appointment.domain.AppointmentStatus;

public record ChangeAppointmentStatusCommand(
        Long id,
        AppointmentStatus status,
        Long companyId
) {}
