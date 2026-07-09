package com.vetsoftware.app.appointment.application.query;

import com.vetsoftware.app.appointment.domain.AppointmentStatus;
import java.time.LocalDate;

public record ListAppointmentsQuery(
        Long companyId,
        LocalDate from,
        LocalDate to,
        Long employeeId,
        AppointmentStatus status
) {}
