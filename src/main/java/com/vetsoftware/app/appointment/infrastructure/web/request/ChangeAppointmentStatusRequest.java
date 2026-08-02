package com.vetsoftware.app.appointment.infrastructure.web.request;

import com.vetsoftware.app.appointment.domain.AppointmentStatus;
import jakarta.validation.constraints.NotNull;

public record ChangeAppointmentStatusRequest(@NotNull AppointmentStatus status) {
}
