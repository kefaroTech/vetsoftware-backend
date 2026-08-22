package com.vetsoftware.app.appointment.infrastructure.web.request;

import jakarta.validation.constraints.Size;

public record CancelAppointmentRequest(
        @Size(max = 300, message = "El motivo no puede superar los 300 caracteres.") String reason) {
}
